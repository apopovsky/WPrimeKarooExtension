[CmdletBinding()]
param(
    [ValidatePattern('^emulator-\d+$')][string]$Serial = 'emulator-5554',
    [switch]$SkipInstall
)
$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if ($env:ANDROID_HOME) { $adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe' }
if (-not (Test-Path -LiteralPath $adb)) { throw 'ADB not found; configure ANDROID_HOME.' }
$output = Join-Path $repository 'build/simulator-smoke'
New-Item -ItemType Directory -Path $output -Force | Out-Null
function Invoke-Adb([string[]]$Command) {
    $result = & $adb -s $Serial @Command 2>&1
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: $result" }
    return ($result -join "`n")
}
function Read-Screen {
    $dump = Invoke-Adb @('shell', 'uiautomator', 'dump', '/sdcard/wprime-smoke.xml')
    if ($dump -notmatch 'dumped to') { throw "Cannot read UI: $dump" }
    Invoke-Adb @('pull', '/sdcard/wprime-smoke.xml', (Join-Path $output 'current-ui.xml')) | Out-Null
    return [xml](Get-Content -LiteralPath (Join-Path $output 'current-ui.xml') -Raw)
}
function Capture([string]$Name) {
    Invoke-Adb @('shell', 'screencap', '-p', '/sdcard/wprime-smoke.png') | Out-Null
    Invoke-Adb @('pull', '/sdcard/wprime-smoke.png', (Join-Path $output "$Name.png")) | Out-Null
}
function Get-Status([xml]$Ui) {
    $node = $Ui.SelectSingleNode('//node[starts-with(@text,"t=")]')
    if (-not $node) { throw 'Laboratory status missing.' }
    return $node.text
}
function Start-Scenario([string]$Layout, [string]$Field, [string]$Scenario = 'Intervals', [int]$Steps = 10) {
    Invoke-Adb @('shell', 'am', 'start', '-S', '-n', 'com.itl.wprimeext/.simulator.WPrimeSimulatorActivity',
        '--es', 'layout', ('"' + $Layout + '"'), '--es', 'field', $Field,
        '--es', 'scenario', ('"' + $Scenario + '"'), '--ei', 'steps', "$Steps") | Out-Null
    Start-Sleep -Seconds 2
    $ui = Read-Screen
    if ((Get-Status $ui) -notmatch "^t=${Steps}s") { throw 'Requested replay steps were reset or not executed.' }
    if ($ui.SelectSingleNode('//node[starts-with(@text,"Render failed:")]')) { throw 'Actual Glance rendering failed.' }
    return $ui
}
function Tap-Text([xml]$Ui, [string]$Text) {
    $node = $Ui.SelectNodes('//node[@clickable="true"]') | Where-Object { $_.text -eq $Text } | Select-Object -First 1
    if (-not $node) { throw "Control not visible: $Text" }
    $match = [regex]::Match($node.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
    $x = [int](($match.Groups[1].Value -as [int]) + ($match.Groups[3].Value -as [int])) / 2
    $y = [int](($match.Groups[2].Value -as [int]) + ($match.Groups[4].Value -as [int])) / 2
    Invoke-Adb @('shell', 'input', 'tap', "$([int]$x)", "$([int]$y)") | Out-Null
}
if (-not $SkipInstall) { & (Join-Path $PSScriptRoot 'start-simulator.ps1') -Serial $Serial -SkipBuild }
foreach ($layout in @('Full-width row', 'Half-row', 'Full-screen')) {
    foreach ($field in @('%', 'kJ')) {
        $ui = Start-Scenario $layout $field
        $status = Get-Status $ui
        $match = [regex]::Match($status, '([\d.]+) J\s*\n.*\nCP ([\d.]+) W / W′ ([\d.]+) J')
        if (-not $match.Success) { throw "Cannot parse diagnostics: $status" }
        $culture = [Globalization.CultureInfo]::InvariantCulture
        $balance = [double]::Parse($match.Groups[1].Value, $culture)
        $cp = [double]::Parse($match.Groups[2].Value, $culture)
        $capacity = [double]::Parse($match.Groups[3].Value, $culture)
        $expected = [Math]::Max(0, $capacity - [Math]::Max(0, 400 - $cp) * 10)
        if ([Math]::Abs($balance - $expected) -gt 5.6) { throw "Effort mismatch: expected $expected J, got $balance J" }
        Capture (($layout -replace ' ', '-') + '-' + ($field -replace '%', 'percent'))
        Write-Host "$layout / ${field}: $balance J; actual RemoteViews rendered."
    }
}
$ui = Start-Scenario 'Full-width row' '%' 'Manual' 10
Tap-Text $ui '+10'
$ui = Read-Screen
if ((Get-Status $ui) -notmatch '410 W') { throw '+10 did not update manual power.' }
Tap-Text $ui 'CP'
$ui = Read-Screen
$status = Get-Status $ui
$cp = [regex]::Match($status, 'CP (\d+) W').Groups[1].Value
if ($status -notmatch "t=10s · $cp W") { throw 'CP button did not apply effective CP.' }
Tap-Text $ui 'PAUSE RIDE'
Start-Sleep -Seconds 2
Tap-Text $ui 'FREEZE CLOCK'
$ui = Read-Screen
if ((Get-Status $ui) -notmatch 'PAUSED' -or (Get-Status $ui) -match '^t=10s') { throw 'Pause must keep virtual recovery time running.' }
Tap-Text $ui 'FREEZE CLOCK'
$ui = Read-Screen
$frozen = [regex]::Match((Get-Status $ui), '^t=(\d+)s').Value
Start-Sleep -Seconds 2
$ui = Read-Screen
if ((Get-Status $ui) -notmatch ([regex]::Escape($frozen))) { throw 'Freeze advanced virtual time.' }
Tap-Text $ui 'RESET'
$ui = Read-Screen
if ((Get-Status $ui) -notmatch '^t=0s' -or (Get-Status $ui) -notmatch '100.0%') { throw 'Reset did not restore full balance/time.' }
$ui = Start-Scenario 'Full-width row' '%' 'Sensor loss' 120
if ((Get-Status $ui) -notmatch 'sensor=false') { throw 'Loss scenario must hold unavailable sensor.' }
if ($ui.SelectSingleNode('//node[@content-desc="W'' Trend"]')) { throw 'Lost sensor must hide trend arrow.' }
Capture 'sensor-loss'
Invoke-Adb @('shell', 'rm', '/sdcard/wprime-smoke.xml', '/sdcard/wprime-smoke.png') | Out-Null
Write-Host "Simulator smoke passed. Screenshots: $output"

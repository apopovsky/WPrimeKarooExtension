param(
    [string]$Avd = 'Medium_Phone_API_35',
    [string]$Serial,
    [ValidateSet('Manual', 'Effort + recovery', 'Intervals', 'Exhaustion', 'Sensor loss')]
    [string]$Scenario = 'Manual',
    [ValidateSet(1, 5, 20, 60)]
    [int]$Speed = 1,
    [switch]$AutoStart,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot

function Invoke-Checked {
    param([string]$Executable, [string[]]$Arguments)
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed ($LASTEXITCODE): $Executable"
    }
}

$sdkDirectory = $env:ANDROID_HOME
if (-not $sdkDirectory) { $sdkDirectory = $env:ANDROID_SDK_ROOT }
if (-not $sdkDirectory) {
    $propertiesFile = Join-Path $repository 'local.properties'
    if (Test-Path -LiteralPath $propertiesFile) {
        $sdkEntry = Get-Content -LiteralPath $propertiesFile |
            Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkEntry) {
            $sdkDirectory = $sdkEntry.Substring(8).Replace('\:', ':').Replace('\\', '\')
        }
    }
}
if (-not $sdkDirectory) {
    $sdkDirectory = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
}
$adb = Join-Path $sdkDirectory 'platform-tools/adb.exe'
$emulator = Join-Path $sdkDirectory 'emulator/emulator.exe'
if (-not (Test-Path -LiteralPath $adb)) {
    throw 'Android platform-tools not found. Set ANDROID_HOME or sdk.dir in local.properties.'
}

function Get-EmulatorSerial {
    $devices = & $adb devices
    if ($LASTEXITCODE -ne 0) { throw 'Unable to list Android devices.' }
    @($devices | ForEach-Object {
        if ($_ -match '^(emulator-\d+)\s+device$') { $Matches[1] }
    })
}

if ($Serial -and $Serial -notmatch '^emulator-\d+$') {
    throw 'This script installs only to Android emulators. A physical Karoo serial is not allowed.'
}
if (-not $Serial) {
    $runningEmulators = @(Get-EmulatorSerial)
    if ($runningEmulators.Count -gt 1) {
        throw 'Multiple emulators are running. Select one with -Serial emulator-NNNN.'
    }
    if ($runningEmulators.Count -eq 1) {
        $Serial = $runningEmulators[0]
    } else {
        if (-not (Test-Path -LiteralPath $emulator)) { throw 'Android Emulator is not installed.' }
        $availableAvds = @(& $emulator -list-avds)
        if ($LASTEXITCODE -ne 0 -or $Avd -notin $availableAvds) {
            throw "AVD '$Avd' not found. Create it in Android Studio or pass -Avd."
        }
        # The emulator is an interactive testing surface, so its window is visible.
        Start-Process -FilePath $emulator -ArgumentList @('-avd', $Avd, '-no-snapshot-save', '-no-boot-anim') | Out-Null
        $deadline = [DateTime]::UtcNow.AddMinutes(3)
        do {
            Start-Sleep -Seconds 2
            $runningEmulators = @(Get-EmulatorSerial)
        } until ($runningEmulators.Count -eq 1 -or [DateTime]::UtcNow -gt $deadline)
        if ($runningEmulators.Count -ne 1) { throw 'Emulator did not become available within three minutes.' }
        $Serial = $runningEmulators[0]
    }
}

$deadline = [DateTime]::UtcNow.AddMinutes(3)
do {
    $booted = & $adb -s $Serial shell getprop sys.boot_completed 2>$null
    if ($LASTEXITCODE -eq 0 -and "$booted".Trim() -eq '1') { break }
    if ([DateTime]::UtcNow -gt $deadline) { throw "Emulator $Serial did not finish booting." }
    Start-Sleep -Seconds 2
} while ($true)

$hardware = & $adb -s $Serial shell getprop ro.hardware
if ($LASTEXITCODE -ne 0 -or "$hardware".Trim() -notin @('ranchu', 'goldfish')) {
    throw 'Target is not an Android SDK emulator. No APK was installed.'
}

Push-Location $repository
try {
    if (-not $SkipBuild) {
        Invoke-Checked (Join-Path $repository 'gradlew.bat') @(':simulator:assembleDebug', '--console=plain')
    }
    $outputMetadata = Join-Path $repository 'simulator/build/outputs/apk/debug/output-metadata.json'
    if (-not (Test-Path -LiteralPath $outputMetadata)) { throw 'Debug APK metadata not found. Run without -SkipBuild.' }
    $apkMetadata = Get-Content -LiteralPath $outputMetadata -Raw | ConvertFrom-Json
    if ($apkMetadata.applicationId -ne 'com.itl.wprimeext.simulator') {
        throw 'Refusing to install an APK whose package is not the standalone simulator.'
    }
    $apkFile = Join-Path (Split-Path -Parent $outputMetadata) $apkMetadata.elements[0].outputFile
    Invoke-Checked $adb @('-s', $Serial, 'install', '-r', '-t', $apkFile)
    Invoke-Checked $adb @('-s', $Serial, 'shell', 'am', 'start', '-S', '-n',
        'com.itl.wprimeext.simulator/.WPrimeSimulatorActivity',
        '--es', 'scenario', ('"' + $Scenario + '"'), '--ei', 'speed', "$Speed", '--ez', 'autoStart', "$($AutoStart.IsPresent)".ToLowerInvariant())
    Write-Host "W Prime simulator running on $Serial. No physical Karoo was modified."
} finally {
    Pop-Location
}

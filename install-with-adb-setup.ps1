# W Prime Karoo Extension - Installation with ADB Setup
# This script will download ADB if needed and install the extension

Write-Host "🔧 W Prime Karoo Extension - Instalación Completa" -ForegroundColor Cyan
Write-Host ""

# Check if ADB is available
$adbPath = ""
try {
    $adbTest = adb version 2>$null
    if ($LASTEXITCODE -eq 0) {
        $adbPath = "adb"
        Write-Host "✅ ADB ya está instalado" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  ADB no encontrado en PATH" -ForegroundColor Yellow
}

# If ADB not found, try to use platform-tools if available
if ($adbPath -eq "") {
    $platformToolsPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $platformToolsPath) {
        $adbPath = $platformToolsPath
        Write-Host "✅ ADB encontrado en Android SDK: $platformToolsPath" -ForegroundColor Green
    }
}

# If still not found, download minimal ADB
if ($adbPath -eq "") {
    Write-Host "📥 Descargando ADB..." -ForegroundColor Yellow

    $adbDir = ".\tools\adb"
    if (!(Test-Path $adbDir)) {
        New-Item -ItemType Directory -Path $adbDir -Force | Out-Null
    }

    # Download minimal ADB (platform-tools)
    $adbUrl = "https://dl.google.com/android/repository/platform-tools_r35.0.1-windows.zip"
    $adbZip = ".\tools\platform-tools.zip"

    try {
        Invoke-WebRequest -Uri $adbUrl -OutFile $adbZip -UseBasicParsing
        Expand-Archive -Path $adbZip -DestinationPath ".\tools" -Force
        $adbPath = ".\tools\platform-tools\adb.exe"
        Remove-Item $adbZip -Force
        Write-Host "✅ ADB descargado correctamente" -ForegroundColor Green
    } catch {
        Write-Host "❌ Error descargando ADB: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host ""
        Write-Host "📋 Instrucciones manuales:" -ForegroundColor Yellow
        Write-Host "1. Instala Android Studio o descarga platform-tools"
        Write-Host "2. Conecta el Karoo via USB"
        Write-Host "3. Habilita 'Opciones de desarrollador' en el Karoo"
        Write-Host "4. Ejecuta: adb install app\build\outputs\apk\debug\WPrimeKarooExtension-v1.0.1-debug.apk"
        exit 1
    }
}

Write-Host ""
Write-Host "🔍 Verificando conexión del Karoo..." -ForegroundColor Cyan

# Check devices
$devices = & $adbPath devices 2>&1
$deviceLines = $devices | Where-Object { $_ -match "\t" -and $_ -notmatch "List of devices" }

if ($deviceLines.Count -eq 0) {
    Write-Host "❌ Karoo no conectado" -ForegroundColor Red
    Write-Host ""
    Write-Host "📋 Pasos para conectar el Karoo:" -ForegroundColor Yellow
    Write-Host "1. Conecta el Karoo al PC via USB"
    Write-Host "2. En el Karoo, ve a Settings > Device > About"
    Write-Host "3. Toca 'Build number' 7 veces para habilitar opciones de desarrollador"
    Write-Host "4. Ve a Settings > Developer options"
    Write-Host "5. Habilita 'USB debugging'"
    Write-Host "6. Acepta el diálogo de autorización en el Karoo"
    Write-Host ""
    Write-Host "Luego ejecuta este script nuevamente."
    exit 1
}

Write-Host "✅ Karoo conectado:" -ForegroundColor Green
$deviceLines | ForEach-Object {
    Write-Host "   $_" -ForegroundColor Gray
}

Write-Host ""
Write-Host "📦 Instalando W Prime Extension..." -ForegroundColor Cyan

# Find the APK
$apkPath = "app\build\outputs\apk\debug\WPrimeKarooExtension-v1.0.1-debug.apk"

if (!(Test-Path $apkPath)) {
    Write-Host "❌ APK no encontrado: $apkPath" -ForegroundColor Red
    Write-Host "🔨 Compilando primero..." -ForegroundColor Yellow

    # Build the APK
    .\gradlew assembleDebug

    if (!(Test-Path $apkPath)) {
        Write-Host "❌ Error: No se pudo compilar el APK" -ForegroundColor Red
        exit 1
    }
}

# Install the APK
Write-Host "📲 Instalando APK..." -ForegroundColor Yellow
$installResult = & $adbPath install -r $apkPath 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ ¡W Prime Extension instalada correctamente!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📱 Para usar la extensión:" -ForegroundColor Cyan
    Write-Host "1. Abre la app Karoo en tu dispositivo"
    Write-Host "2. Ve a Settings > Extensions"
    Write-Host "3. Habilita 'W Prime Monitor'"
    Write-Host "4. Configura tus campos de datos en una pantalla de entrenamiento"
    Write-Host ""
    Write-Host "🎯 Campos disponibles:" -ForegroundColor Yellow
    Write-Host "   • W Prime Balance - Energía anaeróbica restante"
    Write-Host "   • W Prime Used - Energía anaeróbica utilizada"
    Write-Host "   • W Prime Percentage - Porcentaje de energía restante"
} else {
    Write-Host "❌ Error instalando APK:" -ForegroundColor Red
    Write-Host $installResult -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Posibles soluciones:" -ForegroundColor Yellow
    Write-Host "1. Desinstala versiones anteriores de la extensión"
    Write-Host "2. Verifica que USB debugging esté habilitado"
    Write-Host "3. Intenta desconectar y reconectar el Karoo"
}

Write-Host ""
Write-Host "🏁 Instalación completada" -ForegroundColor Cyan

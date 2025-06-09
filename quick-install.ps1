# Script simple de instalación para Karoo
Write-Host "🔍 Instalación Simple - W Prime Karoo Extension" -ForegroundColor Cyan

$packageName = "com.itl.wprimeextension"
$apkPath = "app\build\outputs\apk\debug\WPrimeKarooExtension-v1.0.1-debug.apk"
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# 1. Verificar ADB
Write-Host "`n1. Verificando ADB..." -ForegroundColor Yellow
$devices = & $adbPath devices 2>&1
if ($devices -match "device") {
    Write-Host "✅ Karoo conectado" -ForegroundColor Green
} else {
    Write-Host "❌ Karoo no conectado" -ForegroundColor Red
    Write-Host $devices
    exit 1
}

# 2. Verificar APK
Write-Host "`n2. Verificando APK..." -ForegroundColor Yellow
if (Test-Path $apkPath) {
    $size = (Get-Item $apkPath).Length / 1MB
    Write-Host "✅ APK encontrado: $([math]::Round($size, 1)) MB" -ForegroundColor Green
} else {
    Write-Host "❌ APK no encontrado" -ForegroundColor Red
    Write-Host "💡 Ejecuta: .\gradlew assembleDebug" -ForegroundColor Blue
    exit 1
}

# 3. Limpiar instalación anterior
Write-Host "`n3. Limpiando instalación previa..." -ForegroundColor Yellow
& $adbPath uninstall $packageName 2>$null
& $adbPath uninstall "$packageName.debug" 2>$null
Write-Host "✅ Limpieza completada" -ForegroundColor Green

# 4. Instalar APK
Write-Host "`n4. Instalando APK..." -ForegroundColor Yellow
Write-Host "⏳ Esto puede tomar 30-60 segundos..." -ForegroundColor Blue

$result = & $adbPath install -r $apkPath 2>&1
Write-Host $result

if ($result -match "Success") {
    Write-Host "✅ ¡Instalación exitosa!" -ForegroundColor Green

    # Verificar instalación
    $packages = & $adbPath shell pm list packages | Select-String "wprimeextension"
    if ($packages) {
        Write-Host "✅ Package confirmado: $packages" -ForegroundColor Green
    }

    Write-Host "`n🎯 Próximos pasos:" -ForegroundColor Cyan
    Write-Host "1. Reinicia tu Karoo 3" -ForegroundColor Blue
    Write-Host "2. Ve a Settings → Data Fields" -ForegroundColor Blue
    Write-Host "3. Busca 'W Prime Balance'" -ForegroundColor Blue

} else {
    Write-Host "❌ Error en instalación:" -ForegroundColor Red
    Write-Host $result -ForegroundColor Red

    Write-Host "`n🔧 Soluciones:" -ForegroundColor Yellow
    Write-Host "1. Verifica USB Debugging habilitado" -ForegroundColor Gray
    Write-Host "2. Acepta autorización ADB en Karoo" -ForegroundColor Gray
    Write-Host "3. Reinicia Karoo y reintenta" -ForegroundColor Gray    Write-Host "4. Prueba instalación manual:" -ForegroundColor Gray
    Write-Host "   & `"$adbPath`" push $apkPath /sdcard/" -ForegroundColor DarkGray
    Write-Host "   Luego instala desde file manager" -ForegroundColor DarkGray
}

Write-Host "`n✨ Completado" -ForegroundColor Cyan

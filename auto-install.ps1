# Auto-install W Prime Extension when Karoo is authorized
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apkPath = "app\build\outputs\apk\debug\WPrimeKarooExtension-v1.0.1-debug.apk"

Write-Host "🔄 Esperando autorización del Karoo..." -ForegroundColor Yellow
Write-Host "📱 Acepta el diálogo de USB debugging en tu Karoo" -ForegroundColor Cyan
Write-Host ""

# Monitor for device authorization
$maxAttempts = 30
$attempt = 0

while ($attempt -lt $maxAttempts) {
    $devices = & $adbPath devices 2>&1
    $authorizedDevice = $devices | Where-Object { $_ -match "device$" }

    if ($authorizedDevice) {
        Write-Host "✅ Karoo autorizado!" -ForegroundColor Green
        Write-Host "📦 Instalando W Prime Extension..." -ForegroundColor Cyan

        # Install the APK
        $installResult = & $adbPath install -r $apkPath 2>&1

        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "🎉 ¡W Prime Extension instalada correctamente!" -ForegroundColor Green
            Write-Host ""
            Write-Host "📱 Para usar la extensión:" -ForegroundColor Cyan
            Write-Host "1. Abre la app Karoo en tu dispositivo" -ForegroundColor White
            Write-Host "2. Ve a Settings > Extensions" -ForegroundColor White
            Write-Host "3. Habilita 'W Prime Monitor'" -ForegroundColor White
            Write-Host "4. Configura los campos en una pantalla de entrenamiento" -ForegroundColor White
            Write-Host ""
            Write-Host "🎯 Campos disponibles:" -ForegroundColor Yellow
            Write-Host "   • W Prime Balance - Energía anaeróbica restante" -ForegroundColor Gray
            Write-Host "   • W Prime Used - Energía anaeróbica utilizada" -ForegroundColor Gray
            Write-Host "   • W Prime Percentage - Porcentaje de energía restante" -ForegroundColor Gray
        } else {
            Write-Host "❌ Error instalando APK:" -ForegroundColor Red
            Write-Host $installResult -ForegroundColor Red
        }
        break
    }

    $attempt++
    Write-Host "⏳ Intento $attempt/$maxAttempts - Esperando autorización..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
}

if ($attempt -ge $maxAttempts) {
    Write-Host "⏰ Tiempo agotado esperando autorización" -ForegroundColor Red
    Write-Host "💡 Intenta desconectar y reconectar el Karoo" -ForegroundColor Yellow
}

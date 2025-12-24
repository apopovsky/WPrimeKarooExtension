# Correcciones del Manifest y URLs del APK

## Problema Principal
El APK no se podía instalar en el dispositivo Karoo 3 porque el manifest apuntaba a URLs incorrectas en GitHub (`itl/WPrimeExtension` en lugar de `apopovsky/WPrimeKarooExtension`).

## Cambios Realizados

### 1. AndroidManifest.xml
**Archivo:** `app/src/main/AndroidManifest.xml`

**Cambio:** Actualizada la URL del manifest en el meta-data
```xml
<!-- ANTES -->
android:value="https://github.com/itl/WPrimeExtension/releases/latest/download/manifest.json"

<!-- DESPUÉS -->
android:value="https://github.com/apopovsky/WPrimeKarooExtension/releases/latest/download/manifest.json"
```

### 2. manifest.json
**Archivo:** `app/manifest.json`

**Cambios:**
- URL del icono corregida
- URL del APK corregida con el nombre exacto que genera el build

```json
{
  "iconUrl": "https://github.com/apopovsky/WPrimeKarooExtension/releases/latest/download/wprime-icon.png",
  "latestApkUrl": "https://github.com/apopovsky/WPrimeKarooExtension/releases/latest/download/WPrimeExtension-v1.0-release.apk"
}
```

### 3. CI/CD Workflow
**Archivo:** `.github/workflows/ci.yml`

**Cambios:** Actualizado el job de release para incluir manifest.json e icono
```yaml
- name: Upload Release APK to GitHub Release
  uses: softprops/action-gh-release@v2
  if: startsWith(github.ref, 'refs/tags/')
  with:
    files: |
      app/build/outputs/apk/release/WPrimeExtension-*.apk
      app/manifest.json
      assets/wprime-icon.png
    token: ${{ secrets.GITHUB_TOKEN }}
```

### 4. Icono PNG
**Archivo:** `assets/wprime-icon.png`

**Acción:** Creado placeholder para el icono PNG (necesita ser reemplazado con una versión real exportada del XML)

## Verificación Realizada

✅ No hay errores de sintaxis en los archivos modificados
✅ Las URLs apuntan al repositorio correcto
✅ El nombre del APK coincide con lo que genera el build (`WPrimeExtension-v1.0-release.apk`)
✅ CodeQL ya estaba deshabilitado (no soporta Kotlin 2.3.0)

## Próximos Pasos

1. **Generar icono PNG real:**
   - Exportar `ic_wprime.xml` a PNG de 128x200px
   - Reemplazar el placeholder en `assets/wprime-icon.png`

2. **Crear nuevo release:**
   ```bash
   git add .
   git commit -m "Fix manifest URLs to point to correct repository"
   git push origin master
   git tag v1.0.1
   git push origin v1.0.1
   ```

3. **Verificar que el release incluya:**
   - WPrimeExtension-v1.0-release.apk
   - manifest.json
   - wprime-icon.png

4. **Probar instalación en Karoo 3:**
   - Instalar desde el APK del release
   - Verificar que el dispositivo pueda actualizar automáticamente

## Notas Técnicas

- El build genera APKs con el formato: `WPrimeExtension-v{versionName}-{buildType}.apk`
- Esto está definido en `app/build.gradle.kts` línea 20: `base.archivesName.set("WPrimeExtension-v${versionName}")`
- El versionName actual es "1.0" y versionCode es 8

## Problema del CodeQL

El workflow de code-quality ya tiene CodeQL deshabilitado con `if: false` porque CodeQL solo soporta Kotlin < 2.2.30 y el proyecto usa 2.3.0. Esto está correcto y no afecta el build.


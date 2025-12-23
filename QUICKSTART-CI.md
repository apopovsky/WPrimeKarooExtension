# 🚀 Quick Start - GitHub Actions CI/CD

## Configuración Inicial (Solo una vez)

### 1. Crear Personal Access Token
1. Ve a: https://github.com/settings/tokens
2. "Generate new token (classic)"
3. Nombre: `W Prime Extension CI`
4. Scope: ✅ `read:packages`
5. Copiar el token

### 2. Configurar Secretos del Repositorio
1. Tu repo → Settings → Secrets and variables → Actions
2. "New repository secret":
   - **Name:** `GPR_USER` → **Value:** `tu-username-github`
   - **Name:** `GPR_TOKEN` → **Value:** `el-token-que-copiaste`

### 3. Habilitar GitHub Actions
Settings → Actions → General:
- ✅ "Allow all actions and reusable workflows"
- ✅ "Read and write permissions"
- ✅ "Allow GitHub Actions to create and approve pull requests"

### 4. Push de la Configuración
```bash
git add .github/ docs/ci-cd-setup*.md CONTRIBUTING.md
git commit -m "ci: configurar GitHub Actions para CI/CD"
git push origin main
```

### 5. Verificar
- Ve a la pestaña "Actions" en GitHub
- Deberías ver el workflow ejecutándose

## Uso Diario

### Desarrollo Normal
```bash
git add .
git commit -m "feat: nueva funcionalidad"
git push
```
→ GitHub Actions compila y genera APK automáticamente

### Descargar APK Compilado
1. Actions → Click en el workflow
2. Scroll a "Artifacts"
3. Descargar `wprime-debug-apk`

### Crear un Release
```bash
git tag v1.2.0
git push origin v1.2.0
gh release create v1.2.0 --title "Version 1.2.0" --notes "..."
```
→ GitHub Actions genera APK y lo sube al release

## Documentación Completa
- Español: `docs/ci-cd-setup-es.md`
- English: `docs/ci-cd-setup.md`
- Contributing: `CONTRIBUTING.md`

## Troubleshooting
- Build falla → Verifica secretos GPR_USER y GPR_TOKEN
- No aparece en Actions → Habilita GitHub Actions en Settings
- Dudas → Lee `docs/ci-cd-setup-es.md`


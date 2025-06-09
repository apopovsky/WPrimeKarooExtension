# Setup Instructions for W Prime Karoo Extension

## Prerequisites

1. **Android Studio** - Latest version recommended
2. **GitHub Personal Access Token** - For accessing Karoo SDK dependencies

## Initial Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/apopovsky/WPrimeKarooExtension.git
   cd WPrimeKarooExtension
   ```

2. **Configure GitHub Access**:
   - Copy `gradle.properties.template` to `gradle.properties`
   - Replace `YOUR_GITHUB_USERNAME` with your GitHub username
   - Replace `YOUR_GITHUB_TOKEN_HERE` with your GitHub Personal Access Token

   **To create a GitHub token**:
   - Go to GitHub Settings → Developer settings → Personal access tokens
   - Generate new token with `read:packages` permission
   - Copy the token to your `gradle.properties` file

3. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

## Building the Extension

### Debug Build
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## Installation on Karoo 3

1. Copy the APK file to your Karoo 3 device
2. Enable "Install from Unknown Sources" in Karoo settings
3. Install the APK using a file manager
4. The extension will appear in your Karoo data fields

## Security Notes

- **Never commit `gradle.properties`** - It contains your personal GitHub token
- The `.gitignore` file is configured to exclude this file
- Always use the template file for sharing configuration examples

## Troubleshooting

- **Build fails with authentication error**: Check your GitHub token in `gradle.properties`
- **SDK not found**: Ensure your GitHub token has `read:packages` permission
- **Extension not visible on Karoo**: Make sure APK was installed successfully and restart the Karoo device

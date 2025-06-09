# GitHub Copilot Instructions for W Prime Karoo Extension

## Project Overview
This is a Karoo Extension that calculates and displays W Prime (anaerobic capacity) data in real-time on Hammerhead Karoo devices. The extension uses the **karoo-ext framework** (NOT the old Karoo SDK) and follows modern Android development practices.

## Key Development Context

### 🔧 Framework & Architecture
- **Framework**: Uses `karoo-ext` framework v1.1.5 (io.hammerhead.karooext)
- **NOT**: Old Karoo SDK (deprecated)
- **Pattern**: Self-contained DataType implementation following PowerHrDataType example
- **Architecture**: Flow-based reactive programming with coroutines
- **Language**: Kotlin 2.0.0 with Android target
- **UI**: Jetpack Compose (modern declarative UI)
- **Build**: Android Gradle Plugin 8.2.2

### 📁 Project Structure
```
WPrimeKarooExtension/
├── app/src/main/kotlin/com/itl/WPrimeExtension/
│   ├── WPrimeExtension.kt              # Main extension entry point
│   ├── datatypes/WPrimeDataType.kt     # Core data calculation logic
│   ├── data/WPrimeConfiguration.kt     # Configuration data class
│   ├── wprime/WPrimeCalculator.kt      # W Prime calculation algorithm
│   └── Extensions.kt                   # karoo-ext framework utilities
├── gradle/libs.versions.toml           # Centralized dependency versions
├── quick-install.ps1                   # PowerShell installation script
├── auto-install.ps1                    # Automated installation script
├── INSTALLATION_GUIDE.md               # User installation instructions
└── .github/copilot-instructions.md     # This development context file
```

### 💻 PowerShell Commands & Environment

#### ADB Path
Always use environment variables for ADB path:
```powershell
# Correct way - using environment variable
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Example usage
& $adbPath devices
& $adbPath install -r "app/build/outputs/apk/debug/app-debug.apk"
```

#### Build Commands
```powershell
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean
```

#### Installation Commands
```powershell
# Quick install (PowerShell script)
./quick-install.ps1

# Manual installation
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adbPath install -r "app/build/outputs/apk/debug/app-debug.apk"
```

### 🏗️ karoo-ext Framework Usage

#### Core Concepts
- **DataTypeImpl**: Base class for custom data types
- **KarooSystemService**: Service for accessing Karoo system data
- **StreamState**: Represents data streaming states
- **Flow-based**: Use Kotlin coroutines and Flow for reactive programming

#### Essential Extension Function
```kotlin
// Required extension function for streaming data
fun KarooSystemService.streamDataFlow(dataTypeId: String): Flow<StreamState> {
    return callbackFlow {
        val listenerId = addConsumer(OnStreamState.StartStreaming(dataTypeId)) { event: OnStreamState ->
            trySendBlocking(event.state)
        }
        awaitClose {
            removeConsumer(listenerId)
        }
    }
}
```

This function is located in `Extensions.kt` and is required for the Framework to work properly. It bridges the callback-based Karoo system with Flow-based reactive programming.

#### Data Type Implementation Pattern
```kotlin
class WPrimeDataType(
    private val karooSystem: KarooSystemService,
    private val configurationFlow: Flow<WPrimeConfiguration>,
    extension: String,
) : DataTypeImpl(extension, "wprime") {

    override fun startStream(emitter: Emitter<StreamState>) {
        // Stream power data and calculate W Prime
        karooSystem.streamDataFlow(DataType.Type.POWER).collect { powerState ->
            // Process power data and emit W Prime values
        }
    }
}
```

### 📚 Reference Examples
- **Official Examples**: [karoo-ext examples repository](https://github.com/hammerheadnav/karoo-ext/tree/main/examples)
- **PowerHr Example**: Reference implementation for power-based data types
- **This Project**: `WPrimeDataType.kt` follows the self-contained pattern

### 🚫 What NOT to Use
- **Old Karoo SDK**: Any references to old SDK patterns
- **Callback-based APIs**: Use Flow-based reactive patterns instead
- **Hardcoded paths**: Always use environment variables
- **cmd.exe syntax**: Use PowerShell syntax for commands

### 🔍 Key Implementation Details

#### W Prime Calculation
- **Formula**: Based on critical power, anaerobic capacity, and recovery time constant
- **Real-time**: Updates continuously with power data stream
- **Configuration**: User-configurable parameters (CP, W', Tau)

#### Data Flow
1. Power data streams from Karoo system
2. W Prime calculator processes power values
3. Calculated W Prime values emitted to UI
4. Configuration changes update calculator parameters

#### Error Handling
- Graceful fallback when power data unavailable
- Default values when calculator not initialized
- Comprehensive logging for debugging

### 📦 Build Outputs
- **Debug APK**: `app/build/outputs/apk/debug/WPrimeKarooExtension-v1.0.1-debug.apk` (~11.8MB)
- **Release APK**: `app/build/outputs/apk/release/WPrimeKarooExtension-v1.0.1-release.apk` (~8.2MB)

### 🛠️ Development Guidelines

#### Code Style
- Minimal comments - make code self-explanatory
- Use meaningful variable and function names
- Follow Kotlin conventions
- Prefer extension functions for clean APIs

#### Testing
- Use PowerShell scripts for installation testing
- Test on actual Karoo device when possible
- Verify power data streaming works correctly

#### Debugging
- Check ADB connection: `& $adbPath devices`
- Monitor logs: `& $adbPath logcat | Select-String "WPrime"`
- Verify APK installation: `& $adbPath shell pm list packages | Select-String "wprime"`

### 🔄 Migration Notes
This project was successfully migrated from the old Karoo SDK to karoo-ext framework:
- Eliminated circular dependencies
- Implemented self-contained DataType pattern
- Added proper Flow-based data streaming
- Fixed all compilation errors

### 📝 Documentation Files
- `REFACTORING_COMPLETION_SUMMARY.md`: Technical migration summary
- `INSTALLATION_GUIDE.md`: User installation instructions
- `README.md`: Project overview and setup
- `CLEANUP_SUMMARY.md`: Dependency and plugin cleanup details
- `UPDATE_SUMMARY.md`: Latest framework updates (January 2025)

### 🔗 Dependencies & Versions
Current versions from `gradle/libs.versions.toml`:
- **karoo-ext**: v1.1.5 (latest as of January 2025)
- **Android Gradle Plugin**: 8.2.2
- **Kotlin**: 2.0.0
- **Compose BOM**: 2024.12.01
- **AndroidX Core**: 1.13.1
- **AndroidX Lifecycle**: 2.8.6
- **AndroidX Activity**: 1.9.3
- **Kotlinx Coroutines**: 1.9.0

### 📋 GitHub Token Configuration
**Required for building**: GitHub Personal Access Token with `read:packages` permission
- Copy `gradle.properties.template` to `gradle.properties`
- Add your GitHub username and token
- **Never commit `gradle.properties`** - contains sensitive credentials

---

**Remember**: Always use karoo-ext framework, PowerShell syntax, and environment variables for paths. Refer to official examples and this project's implementation patterns for consistency.

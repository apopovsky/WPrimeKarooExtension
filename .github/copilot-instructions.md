# GitHub Copilot Instructions - W Prime Extension for Hammerhead Karoo

## Project Overview

This is a **W Prime (W') extension for Hammerhead Karoo** cycling computers, built using the official `karoo-ext` framework. W Prime is a physiological model that tracks anaerobic energy capacity during cycling.

### What is W Prime?
- **Critical Power (CP)**: Maximum sustainable power output
- **W Prime (W')**: Finite anaerobic energy available above CP (measured in Joules)
- **Tau (τ)**: Recovery time constant (how fast W' recovers when power < CP)

## Current Project Status

### ✅ COMPLETED (Ready for Testing)
- **Core W Prime calculation engine** (`WPrimeCalculator.kt`)
- **Real-time data streaming** to Karoo OS (`WPrimeDataType.kt`)
- **Persistent configuration** using Android DataStore (`WPrimeSettings.kt`)
- **Complete UI configuration** with Compose (`ConfigurationScreen.kt`)
- **Successful compilation** and APK generation
- **Full integration** between UI settings and real-time calculation

### 🔄 NEXT PRIORITIES
1. **Device testing** on actual Karoo hardware
2. **RemoteViews** for custom W Prime visualization
3. **FIT file integration** to save W Prime data in activity files
4. **Performance optimization** and user experience improvements

## Project Structure

```
app/src/main/kotlin/com/itl/wprimeext/
├── extension/
│   ├── WPrimeExtension.kt          # Main extension class
│   ├── WPrimeDataType.kt          # Real-time W Prime calculation & streaming
│   ├── WPrimeCalculator.kt        # Core W Prime algorithm
│   └── WPrimeSettings.kt          # Persistent configuration (DataStore)
├── ui/
│   ├── viewmodel/
│   │   └── WPrimeConfigViewModel.kt  # Configuration state management
│   └── components/
│       └── ConfigurationCard.kt   # UI component for parameter input
├── ConfigurationScreen.kt         # Main configuration UI
├── MainActivity.kt                # Entry point (from template)
└── TabLayout.kt                   # Main app layout
```

## Key Implementation Details

### W Prime Algorithm
The `WPrimeCalculator` implements the differential equation model:
- **Depletion**: When power > CP, W' decreases linearly
- **Recovery**: When power < CP, W' recovers exponentially with time constant τ

### Data Flow
1. User configures CP, W', τ via `ConfigurationScreen`
2. Settings saved persistently via `WPrimeSettings` (DataStore)
3. `WPrimeDataType` loads config at startup
4. Real-time power data flows from Karoo → Calculator → Karoo data field

### Integration Points
- **Karoo data field**: Available as "W Prime" in ride profiles
- **Configuration UI**: Accessible via main app interface
- **Persistent storage**: Survives app restarts and device reboots

## Development Guidelines

### When Working on This Project

1. **Always build first** to check current state:
   ```bash
   ./gradlew assembleDebug
   ```

2. **Key files to understand**:
   - `WPrimeDataType.kt` - Core integration with Karoo
   - `WPrimeCalculator.kt` - Mathematical model
   - `WPrimeSettings.kt` - Persistent configuration

3. **Common tasks**:
   - Adding features → Start with `WPrimeCalculator.kt`
   - UI changes → Start with `ConfigurationScreen.kt`
   - Data integration → Start with `WPrimeDataType.kt`

### Code Standards

- **Kotlin style**: Follow existing patterns in the codebase
- **Comments**: Minimal, focus on non-obvious logic
- **Error handling**: Use Timber for logging, graceful degradation
- **Coroutines**: Use `Dispatchers.IO` for DataStore operations

### Dependencies in Use

- **karoo-ext**: Official Hammerhead framework
- **Compose UI**: Modern Android UI toolkit
- **DataStore**: Persistent key-value storage
- **Hilt**: Dependency injection
- **Timber**: Logging

## Testing & Deployment

### Current APK
- Location: `app/build/outputs/apk/debug/WPrimeExtension-v1.0-debug.apk`
- Ready for installation on Karoo devices

### Installation
```bash
adb install app/build/outputs/apk/debug/WPrimeExtension-v1.0-debug.apk
```

### Configuration Values (for testing)
- **CP**: ~250W (or user's FTP × 0.95)
- **W'**: ~12000J (typical range: 10000-25000J)
- **Tau**: ~300s (typical range: 200-600s)

## Common Issues & Solutions

### Build Issues
- **File locks**: Kill Java processes: `taskkill /f /im java.exe`
- **Clean build**: `./gradlew clean` then `./gradlew assembleDebug`

### Integration Issues
- **DataStore not loading**: Check Timber logs for configuration load errors
- **Calculator not updating**: Verify `updateConfiguration()` is called in `WPrimeDataType`
- **UI not persisting**: Check ViewModel → Settings flow

### When Making Changes

1. **Always test compilation** after changes
2. **Check for errors** in key files:
   ```bash
   # This project has tool support for error checking
   ```
3. **Update README.md** if adding major features
4. **Test on device** when possible

## Future Development Roadmap

### Immediate Next Steps
1. **Device validation** - Test on real Karoo hardware
2. **RemoteViews** - Custom W Prime gauge visualization
3. **FIT integration** - Save W Prime data in activity files

### Advanced Features
- Real-time alerts when W' is low
- Historical W' analysis
- Integration with training platforms
- Custom W' recovery models

## Important Notes

- This project is **functional and ready for testing**
- The core W Prime calculation is **mathematically correct**
- Configuration persistence is **fully implemented**
- APK generation is **working reliably**

The main focus should be on **device testing and user experience improvements** rather than core functionality, which is already complete.

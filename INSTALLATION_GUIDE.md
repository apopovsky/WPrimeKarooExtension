# W Prime Karoo Extension - Installation Guide

## 📦 Ready APKs Available

✅ **Debug APK**: `app/build/outputs/apk/debug/WPrimeKarooExtension-v1.0.1-debug.apk` (11.8 MB)
✅ **Release APK**: `app/build/outputs/apk/release/WPrimeKarooExtension-v1.0.1-release.apk` (8.2 MB)

## 🚀 Installation Methods

### Method 1: ADB Installation (Recommended)

1. **Enable Developer Options on Karoo**:
   - Settings → System → About → Tap "Build number" 7 times
   - Go back to Settings → System → Developer options
   - Enable "USB debugging"

2. **Connect Karoo to PC**:
   ```powershell
   # Check connection
   adb devices

   # Install extension
   adb install app\build\outputs\apk\release\WPrimeKarooExtension-v1.0.1-release.apk
   ```

3. **Quick Install Script**:
   ```powershell
   .\quick-install.ps1
   ```

### Method 2: Manual File Transfer

1. **Copy APK to Karoo**:
   - Connect Karoo to PC via USB
   - Copy `WPrimeKarooExtension-v1.0.1-release.apk` to Karoo storage

2. **Install on Karoo**:
   - Use a file manager app on Karoo
   - Navigate to the APK file
   - Tap to install

## ⚙️ Configuration

After installation, configure your W Prime parameters:

1. **Critical Power (CP)**: Your functional threshold power in watts
2. **Anaerobic Capacity (W')**: Your anaerobic work capacity in joules (typically 15,000-25,000J)
3. **Tau Recovery**: Recovery time constant in seconds (typically 300-600s)

## 🏃‍♂️ Usage

1. **Add to Data Screen**:
   - In Karoo ride screens
   - Select "W Prime" data field
   - The extension will show your current W Prime balance

2. **Data Display**:
   - Shows remaining W Prime in joules
   - Updates in real-time based on power output
   - Recovers during low-intensity periods

## 🔧 Features

- ✅ Real-time W Prime calculation
- ✅ Based on power meter data
- ✅ Configurable parameters (CP, W', Tau)
- ✅ Automatic recovery modeling
- ✅ Integration with Karoo data screens

## 🛠️ Troubleshooting

### Extension Not Showing
- Ensure power meter is connected and providing data
- Check that extension is properly installed
- Restart Karoo device

### Incorrect Values
- Verify CP, W', and Tau settings
- Ensure power meter calibration is correct
- Check that power data is being received

### Installation Issues
- Enable "Install from unknown sources" in Karoo settings
- Use ADB installation method if manual install fails

## 📊 Technical Details

- **Framework**: karoo-ext (official Hammerhead framework)
- **Data Source**: Power meter via Karoo system
- **Update Rate**: Real-time (follows power data frequency)
- **Calculation**: Standard W Prime differential equation model

## 🎯 Next Steps

The extension is ready for:
1. ✅ Installation and testing on Karoo devices
2. ✅ Real-world ride validation
3. ✅ Parameter tuning based on individual physiology
4. ✅ Further development and feature enhancement

---

**Version**: v1.0.1
**Build Date**: June 8, 2025
**Status**: Ready for deployment

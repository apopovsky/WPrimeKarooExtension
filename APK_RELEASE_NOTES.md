# W Prime Karoo Extension - APK Release Notes

## Version 1.0.0 - Initial Release

### Installation Instructions

#### Method 1: Direct Install (Recommended)
1. Download the APK file to your computer
2. Connect your Karoo 3 to your computer via USB
3. Copy the APK file to your Karoo 3 device storage
4. On your Karoo 3:
   - Enable "Install from Unknown Sources" in Settings
   - Use a file manager app to navigate to the APK file
   - Tap the APK file to install

#### Method 2: ADB Install (Advanced Users)
```bash
adb install app-release.apk
```

### Available Builds

#### Release APK (`app-release.apk`)
- **Size**: ~8.2 MB
- **Optimized**: Yes (ProGuard enabled)
- **Debug Info**: Removed
- **Recommended for**: General use and distribution

#### Debug APK (`app-debug.apk`)
- **Size**: ~11.7 MB
- **Optimized**: No
- **Debug Info**: Included
- **Recommended for**: Development and troubleshooting

### What's Included

✅ **W Prime Balance Data Field**: Real-time W Prime balance calculation and display
✅ **Power Integration**: Automatic power data collection from connected sensors
✅ **Configurable Settings**: Customizable W Prime parameters (CP, W Prime capacity)
✅ **Karoo 3 Optimized**: Native integration with Hammerhead Karoo 3 platform

### Compatibility

- **Device**: Hammerhead Karoo 3 only
- **Android Version**: Android 9.0+ (API 28+)
- **Power Sensors**: ANT+ and Bluetooth power meters supported
- **Dependencies**: No additional apps required

### Usage Instructions

1. **After Installation**:
   - Restart your Karoo 3 device
   - Go to Settings → Data Fields
   - Find "W Prime Balance" in the available fields

2. **Configuration**:
   - Open the W Prime Extension app
   - Set your Critical Power (CP) value
   - Set your W Prime capacity (typically 15,000-25,000 joules)
   - Save settings

3. **During Rides**:
   - Add W Prime Balance to your desired data screens
   - The field will show your remaining W Prime capacity
   - Values update in real-time based on power output

### Known Issues

- First installation may require device restart to appear in data fields
- W Prime calculations require valid power data - ensure power sensor is connected
- Settings changes require app restart to take effect

### Support

For issues, feature requests, or questions:
- GitHub Issues: [WPrimeKarooExtension/issues](https://github.com/apopovsky/WPrimeKarooExtension/issues)
- Ensure you include device model, Android version, and detailed problem description

### Changelog

**v1.0.0** (Initial Release)
- Complete W Prime balance calculation implementation
- Fixed Karoo SDK integration and compilation errors
- Native Karoo 3 data field support
- Configurable CP and W Prime settings
- Real-time power data processing
- Optimized APK builds for distribution

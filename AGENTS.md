# AGENTS.md – WPrimeExtension Codebase Guide

## Project Overview
Kotlin Android extension for the **Hammerhead Karoo 3** cycling computer using the **karoo-ext** library. Displays real-time W′ (anaerobic energy) balance as two custom data fields (% and kJ). Built with MVVM + Hilt + Jetpack Compose/Glance.

## Key Source Paths
```
app/src/main/kotlin/com/itl/wprimeext/
├── extension/
│   ├── WPrimeExtension.kt        # KarooExtension service; FIT recording; broadcast receivers
│   ├── WPrimeDataTypeBase.kt     # Abstract Glance view + power stream logic shared by both fields
│   ├── WPrimeDataType.kt         # % display field
│   ├── WPrimeKjDataType.kt       # kJ display field
│   ├── WPrimeCalculator.kt       # IWPrimeModel interface + 6 model classes + WPrimeFactory
│   ├── WPrimeSettings.kt         # DataStore persistence; WPrimeConfiguration data class
│   └── WPrimeAlertManager.kt     # Threshold crossing alerts with 30s cooldown
├── ui/WPrimeGlanceViews.kt       # Glance composables for the data field UI
└── utils/WPrimeLogger.kt         # Timber wrapper with module tags (WPrime:Extension, etc.)
```

## Architecture & Data Flow
1. **Power stream** → `karooSystem.streamDataFlow(DataType.Type.SMOOTHED_3S_AVERAGE_POWER)` (view) or `DataType.Type.POWER` (FIT/extension)
2. **WPrimeCalculator** delegates to an `IWPrimeModel` (selected via `WPrimeFactory`); returns Joules
3. **WPrimeDataTypeBase.startStream()** emits `StreamState.Streaming(DataPoint(...))` to Karoo every update
4. **WPrimeDataTypeBase.startView()** renders `WPrimeGlanceView` via `GlanceRemoteViews`, pushed via `emitter.updateView()`
5. **WPrimeExtension.startFit()** runs a parallel calculator and writes `WriteToRecordMesg` / `WriteToSessionMesg` FIT developer fields (`WPrimeJ`, `WPrimePct`)
6. **Two independent** `WPrimeCalculator` instances exist: one per active DataType and one in the extension for FIT – they stay in sync via `wprimeSettings.configuration` Flow

## Karoo-Specific Patterns

### Service Registration (already done – don't change structure)
- `WPrimeExtension` is a `KarooExtension("wprime-id", "1.0")` service registered in `AndroidManifest.xml` with `io.hammerhead.karooext.KAROO_EXTENSION` intent filter
- `karooSystem.connect()` is called in `onCreate()`; `disconnect()` in `onDestroy()`
- Always call `removeConsumer(listenerId)` inside `awaitClose {}` – see `Extensions.kt` helpers

### Glance Layout Rules (critical – Karoo differs from standard Android)
- **Use `config.viewSize` (pixels) NOT `LocalSize.current`** – `LocalSize.current` returns `NaN` on Karoo
- Convert pixels to dp: `widthDp = (pixels / 2.0f).dp`
- Breakpoints: small field ≈ 480×240 px, large ≈ 960×480 px
- **No custom `weight(Xf)` values** – use `defaultWeight()` for flex items and fixed `width()` for fixed columns
- Wide mode detection: `val wideMode = config.gridSize.first == 60`

### KarooSystemService Extension Helpers (`Extensions.kt`)
```kotlin
// Wrap karoo callbacks as Flows:
karooSystem.streamDataFlow("dataTypeId")   // Flow<StreamState>
karooSystem.consumerFlow<RideState>()      // Flow<T: KarooEvent>
```

## Calculator: Adding a New Algorithm
1. Implement `IWPrimeModel` (or extend `BaseWPrimeModel`)
2. Add entry to `WPrimeModelType` enum
3. Add a `create(...)` case to `WPrimeFactory`
4. Add UI option in `ConfigurationScreen.kt` + persist via `WPrimeSettings.updateModelType()`

## Build & Developer Commands
```bash
./gradlew clean assembleDebug                      # Build debug APK
./gradlew installDebug                             # Install to connected device/Karoo
./gradlew test                                     # Run unit tests
adb connect <KAROO_IP>:5555                        # Connect to Karoo over Wi-Fi
adb install app/build/outputs/apk/debug/*.apk      # Manual install
adb logcat | grep WPrime                           # All extension logs
adb logcat | grep WPRIME_SIZE                      # Glance layout/sizing logs
# Test in-ride actions without a real ride:
adb shell am broadcast -a io.hammerhead.wprime.IN_RIDE_ACTION --es action io.hammerhead.karooext.models.MarkLap
# Test an alert from config screen:
adb shell am broadcast -a io.hammerhead.wprime.TEST_ALERT --es alertId test1 --ei threshold 25 --ez soundEnabled true
```

## Logging Convention
All logs use `WPrimeLogger` (never `Log.d` directly):
```kotlin
WPrimeLogger.d(WPrimeLogger.Module.CALCULATOR, "message")
// Tags: WPrime:Extension | WPrime:DataType | WPrime:Calculator | WPrime:Settings | WPrime:UI | WPrime:ViewModel
```

## Settings Persistence
`WPrimeSettings` wraps DataStore. The `configuration: Flow<WPrimeConfiguration>` is collected in both DataType stream coroutines and the FIT coroutine to hot-reload parameters without restarting the ride.

## Never Do
- **Do not create separate `.md` files for bug fixes or resolved issues** – fix the code directly
- Do not use `LocalSize.current` in Glance composables
- Do not block the main thread; always use `Dispatchers.IO` for stream/FIT coroutines and `Dispatchers.Main` for `glance.compose()` / `emitter.updateView()` calls


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
│   ├── WPrimeSettings.kt         # DataStore persistence; WPrimeConfiguration + WPrimeAlert data classes
│   ├── WPrimeAlertManager.kt     # Threshold crossing alerts with 5-minute cooldown; AlertType enum
│   ├── Extensions.kt             # karooSystem.streamDataFlow / consumerFlow extension helpers
│   └── ServiceModule.kt          # Hilt @ServiceScoped KarooSystemService provider
├── ui/
│   ├── WPrimeGlanceViews.kt      # Glance composables for the data field UI
│   ├── WPrimeColors.kt           # calculateWPrimeColors() – power-ratio color bands
│   ├── viewmodel/
│   │   └── WPrimeConfigViewModel.kt  # MVVM ViewModel + WPrimeConfigViewModelFactory
│   ├── components/
│   │   ├── AlertComponents.kt    # AlertItem / NewAlertDialog composables
│   │   ├── CompactSettingField.kt
│   │   └── ConfigurationCard.kt
│   └── theme/Theme.kt
├── ConfigurationScreen.kt        # Main settings UI (Jetpack Compose)
├── MainActivity.kt               # App entry point
├── ViewModelModule.kt            # Hilt @ViewModelScoped KarooSystemService provider
├── WPrimeApplication.kt          # Application class (Hilt entry point)
├── WPrimeRemoteViewActivity.kt   # Stand-alone preview activity for data-field RemoteViews (dev use)
└── utils/
    ├── WPrimeLogger.kt           # Timber wrapper with module tags (WPrime:Extension, etc.)
    └── LogConstants.kt           # String constants for structured log messages
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

## Taking Screenshots from Karoo via ADB

**Device specs (Karoo 3 / k24):** 480×800 px physical, 300 dpi, Android 12.

```bash
# Single screenshot → pull to media/ folder
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png media/screen.png
adb shell rm /sdcard/screen.png

# One-liner (capture + pull + cleanup)
adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png media/screen.png && adb shell rm /sdcard/screen.png

# PowerShell one-liner (Windows)
adb shell screencap -p /sdcard/screen.png; adb pull /sdcard/screen.png media/screen.png; adb shell rm /sdcard/screen.png

# Timestamped capture (useful for session comparisons)
$ts = Get-Date -Format "yyyyMMdd_HHmmss"; adb shell screencap -p /sdcard/screen.png; adb pull /sdcard/screen.png "media/screen_$ts.png"; adb shell rm /sdcard/screen.png

# Record a short video (max 180 s, Ctrl+C to stop early)
adb shell screenrecord /sdcard/karoo.mp4
adb pull /sdcard/karoo.mp4 media/karoo.mp4
adb shell rm /sdcard/karoo.mp4
```

**Notes:**
- `screencap -p` writes a PNG directly (no intermediate raw format needed).
- Screenshots land in `media/` at the repo root – already tracked by git for UI documentation.
- The Karoo screen is portrait 480×800 but data fields render in landscape sub-regions; field pixel sizes reported by `config.viewSize` will differ from full-screen dimensions.
- Use `adb shell wm size` / `adb shell wm density` to confirm current override values if you have applied `wm size` overrides during testing.

## Logging Convention
All logs use `WPrimeLogger` (never `Log.d` directly):
```kotlin
WPrimeLogger.d(WPrimeLogger.Module.CALCULATOR, "message")
// Tags: WPrime:Extension | WPrime:DataType | WPrime:Calculator | WPrime:Settings | WPrime:UI | WPrime:ViewModel
```

## Settings Persistence
`WPrimeSettings` wraps DataStore. The `configuration: Flow<WPrimeConfiguration>` is collected in both DataType stream coroutines and the FIT coroutine to hot-reload parameters without restarting the ride.

`WPrimeConfiguration` fields (all persisted, all hot-reloaded):
- `criticalPower: Double` (default 250.0)
- `anaerobicCapacity: Double` (default 12000.0)
- `tauRecovery: Double` (default 300.0) – Bartram model only
- `kIn: Double` (default 0.002) – Weigend model only
- `recordFit: Boolean` (default true)
- `modelType: WPrimeModelType` (default `SKIBA_DIFFERENTIAL`)
- `showArrow: Boolean` (default true) – trend arrow in Glance view
- `useColors: Boolean` (default true) – power-ratio color coding
- `alerts: List<WPrimeAlert>` (default empty) – threshold alerts

`WPrimeAlert` is `@Serializable`, stored as JSON in DataStore:
```kotlin
data class WPrimeAlert(id: String, thresholdPercentage: Int, soundEnabled: Boolean, alertType: AlertType)
enum class AlertType { DROP, REPLENISH }  // DROP = W' falls through; REPLENISH = W' rises through
```
Alert cooldown is **5 minutes** (`ALERT_COOLDOWN_MS = 300_000L`); auto-dismiss after 10 seconds.

## Glance Color Coding
`WPrimeColors.kt` provides `calculateWPrimeColors(currentPower, criticalPower, wPrimePercentage)` → `WPrimeColors(backgroundColor, textColor)`. Power-ratio bands (power / CP):
- At 100% W' with power < CP → light blue `#94D8E0` / black text (stable state)
- < 0.90 → recovery green `#109C77` / white
- < 1.00 → light green `#59C496` / black
- < 1.10 → yellow `#E6DE26` / black
- < 1.25 → mid orange `#E48F73` / black
- < 1.40 → orange `#E5683C` / white
- < 1.60 → red `#C7292A` / white
- ≥ 1.60 → violet `#AF26A0` / white

## Hilt DI Structure
Two Hilt modules provide `KarooSystemService` at different scopes:
- `ServiceModule` – `@ServiceScoped`, installed in `ServiceComponent` (used by the extension service)
- `ViewModelModule` – `@ViewModelScoped`, installed in `ViewModelComponent` (used by config UI)

## Development Preview
`WPrimeRemoteViewActivity` renders a data field's `RemoteViews` directly on-screen without a real Karoo ride. Launch via intent with `typeId = "wprime"` (%) or `"wprime-kj"`. Uses a mock `IHandler` + `ViewEmitter` with a fixed `ViewConfig(gridSize = Pair(60,15), viewSize = Pair(800,200))`.

## Never Do
- **Do not create separate `.md` files for bug fixes or resolved issues** – fix the code directly
- Do not use `LocalSize.current` in Glance composables
- Do not block the main thread; always use `Dispatchers.IO` for stream/FIT coroutines and `Dispatchers.Main` for `glance.compose()` / `emitter.updateView()` calls


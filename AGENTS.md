# AGENTS.md – WPrimeExtension codebase guide

## Scope and sources of truth

Kotlin Android extension for Hammerhead Karoo 3. Graphical fields `wprime` and `wprime-kj` expose W′ balance; FIT stores Joules and percent. Preserve these IDs and service registration.

- `README.md`: rider setup and limitations.
- `CONTRIBUTING.md`: development, simulator, verification and prioritized improvement plan.
- `docs/wprime-algorithms.md`: actual implemented equations; scientific certification remains pending.
- Gradle catalog, wrapper, manifests and source are authoritative for versions and contracts.

Keep documents aligned with implementation. Do not create Markdown files for individual fixes. Preserve unrelated changes; do not stage or commit unless requested.

## Source map

Production paths are relative to `app/src/main/kotlin/com/itl/wprimeext/`.

| Path | Responsibility |
| --- | --- |
| `extension/WPrimeExtension.kt` | Hilt service, shared runtime, fields, FIT and receivers |
| `extension/WPrimeRuntime.kt` | One raw POWER subscription, settings/profile/ride lifecycle, serialized publication and alert dispatch |
| `extension/WPrimeEngine.kt` | Deterministic state owner, ride/sensor policy, configuration and threshold crossings |
| `extension/WPrimeCalculator.kt` | Six model implementations and elapsed-time integration |
| `extension/WPrimeDataTypeBase.kt` | Immutable numeric/view projections and isolated host preview |
| `extension/WPrimePresentation.kt` | Shared field colors/arrow projection, including sensor loss |
| `extension/WPrimeSettings.kt`, `CriticalPowerResolver.kt` | Validated DataStore settings, atomic alert CRUD, manual CP / FTP × 0.95 fallback |
| `extension/Extensions.kt`, `ServiceModule.kt` | Cancellable SDK callback flows and service-scoped DI |
| `extension/WPrimeAlertManager.kt` | InRideAlert overlay and optional sound |
| `ConfigurationScreen.kt`, `ui/viewmodel/` | Real Compose settings, lifecycle collection, authoritative persistence and errors |
| `ui/WPrimeGlanceViews.kt`, `WPrimeColors.kt` | Actual field layout and palette |
| `WPrimeApplication.kt`, `utils/` | Hilt application and debug-gated logging |

Debug-only laboratory: `app/src/debug/kotlin/com/itl/wprimeext/simulator/`, debug manifest and CSV asset. It renders the actual Glance RemoteViews with a separate engine and real persisted settings. It has no Karoo host, FIT writer or real sensor dependency. Tests are under `src/test` and `src/testDebug`. The obsolete unregistered preview and unused ViewModelModule/ConfigurationCard were removed.

## Architecture and policy

- Both fields, alerts and FIT consume one immutable runtime snapshot. Raw `DataType.Type.POWER` is authoritative; do not introduce a second smoothed-power calculator or reset balance on field/page lifecycle.
- Model updates and publication are serialized. Production elapsed time uses `SystemClock.elapsedRealtime`; laboratory time is deterministic.
- IDLE does not integrate and resets the ride. RECORDING integrates accepted samples. PAUSED recovers at 0 W. Silent samples recover after >5 s; explicit unavailable sensor holds RECORDING balance and excludes the unknown interval on reconnect. Paused recovery continues despite sensor loss.
- Cosmetic settings and equivalent profile updates preserve balance. Physiological changes preserve the remaining fraction and rebase integration. Document equation changes separately.
- One recovery ticker runs at 3 s only when riding, depleted and able to recover; none at Idle/full capacity/explicit loss while recording. Preserve complete elapsed recovery when suspending work.
- Alerts cross in both directions independently of visible fields, once per alert ID with 300000 ms cooldown. Reset clears cooldowns. DROP prioritizes the lowest crossed threshold; REPLENISH the highest.
- Render keys filter visible state before composition; updates are conflated and spaced at least 1 s apart. Do not throttle power integration to reduce rendering.
- Settings UI retains a manual ViewModel factory. Do not document it as an injected Hilt ViewModel. DataStore is authoritative; save failures must remain visible.

## Host and UI constraints

- Service `KarooExtension("wprime-id", BuildConfig.VERSION_NAME)` connects in onCreate and disconnects in onDestroy; runtime stops and all consumers/receivers/jobs clean up.
- SDK is an external authenticated GitHub Packages dependency. Check SDK/KOS compatibility before upgrading; declared 1.1.9 requires KOS 1.634.2440 or later.
- Always remove consumers in `awaitClose`. Never block callback/Main threads. Use IO for calculation and Main for `GlanceRemoteViews.compose` / `emitter.updateView`.
- Use `config.viewSize` pixels, never `LocalSize.current`. This project uses pixels / 2 for layout dp; this is not a universal Android density rule.
- Canonical wide/narrow breakpoint is `viewSize.first > 400`; use `defaultWeight()` and fixed widths in Glance.
- Header icon/row/text: wide 30 dp / 32 dp / 20 sp; narrow 26 dp / 28 dp / 18 sp. Keep composition and sizing helper aligned.
- Palette function expects W′ fraction 0–1. Explicit loss while recording hides the arrow and uses neutral colors via the shared presentation helper.
- FIT definitions remain field 1 `WPrimeJ`, UInt32 (134), J; field 2 `WPrimePct`, UInt16 (132), %. kJ display units differ from FIT Joules.
- Use `WPrimeLogger`, not direct Android logs. Expensive high-frequency messages must use lazy debug logging. Release has no DebugTree.
- Defaults: CP250 W, capacity12000 J, tau300 s, kIn0.002, Skiba Differential, FIT/arrow/colors on, alerts empty. KAROO_FTP resolves valid FTP ×0.95 or manual fallback. The factor is an app heuristic.

## Verification workflow

```powershell
.\gradlew.bat spotlessCheck :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease --console=plain
git diff --check
.\scripts\start-simulator.ps1 -Serial emulator-5554
```

The launcher script only targets Android emulators. Choose full-width row, half-row or full-screen; percent/kJ; open Settings to change actual CP/model; use manual power buttons and activity/playback controls or CSV replay. Inspect screenshots from the actual rendered RemoteViews. Test persistence on returning from settings and loss/reconnect/paused recovery. Report executed test counts; NO-SOURCE is not successful test coverage.

For final host certification, install on a connected Karoo only when that testing is intended, verify both fields/alerts/FIT through recording, pause, resume, Idle/new ride and page navigation. Compare decoded FIT and displayed values. Local emulator evidence does not certify Karoo service lifecycle, FIT IPC, alert sound, KOS layout quirks or battery savings. Never commit visual changes before device verification or the working local RemoteViews preview.

```powershell
adb -s <serial> shell screencap -p /sdcard/screen.png
adb -s <serial> pull /sdcard/screen.png media/screen.png
adb -s <serial> shell rm /sdcard/screen.png
```

Karoo 3 reference: 480×800 px, Android12. Use explicit serials when multiple devices exist. Historical media are not new validation. Keep receiver hardening, scientific model validation, library migrations, signing/R8 and measured battery comparisons in the existing improvement plan. Never use `flow {}` with concurrent producers; use `channelFlow`.

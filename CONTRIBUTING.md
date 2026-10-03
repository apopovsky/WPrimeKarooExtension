# Contributing and technical improvement plan

## Development setup

Use [AGENTS.md](AGENTS.md) for project constraints and [README.md](README.md) for rider setup. Only `:app` is included in `settings.gradle.kts`; the SDK is an external GitHub Packages dependency.

The current build declares AGP 9.2.1, Gradle 9.5.1, Kotlin plugin aliases 2.4.0, compile/target SDK 37 and min SDK 23. AGP supplies built-in Kotlin; the application does not apply `org.jetbrains.kotlin.android`. Java source/target compatibility is 11, which is distinct from the Gradle daemon JDK. `gradle/gradle-daemon-jvm.properties` requests Oracle JDK 24. CI starts with Temurin 17 but daemon criteria can select/download JDK 24. Use Android Studio compatible with the declared AGP, not the old Hedgehog prerequisite.

Install Android SDK 37 and platform tools. Configure GitHub Packages credentials in ignored `local.properties` or user-level Gradle properties:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_READ_PACKAGES_TOKEN
```

`settings.gradle.kts` also supports `GPR_USER` and `GPR_API_KEY`, then GitHub/environment fallbacks. CI maps the repository secret `GPR_TOKEN` to `gpr.key`; `GPR_TOKEN` alone is not a local Gradle environment variable supported by settings. Never commit credentials. `mavenLocal()` currently precedes remote repositories: check provenance when reproducing a dependency problem.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug spotlessCheck --console=plain
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath --console=plain
git diff --check
```

Spotless wraps ktlint; neither `lintKotlin` nor `detekt` is configured. Avoid routine `clean` builds. Unit sources now cover the pure engine, real DataStore persistence and debug CSV parser. There are no instrumentation test sources; report actual executed counts rather than treating NO-SOURCE as coverage.

## Device verification and contributions

Preserve the user's working tree and choose a branch from the actual repository state rather than assuming `develop` exists. For Codex-created branches the default prefix is `ariel/`. Use descriptive conventional commits only when a commit is requested.

For visual or ride behavior changes, install with `installDebug`, wait about four seconds, capture and inspect a screenshot as described in AGENTS, then test recording, pause, resume, Idle/new ride, missing power, long stops and two simultaneous fields. Compare decoded FIT values to displayed values. Check CP source fallback, parameter persistence, alert CRUD, both crossing directions and optional sound. Report hardware/KOS and what was actually observed.

The old unregistered preview was replaced by the debug-only WPrimeSimulatorActivity described below. It renders actual RemoteViews and uses the production engine; emulator evidence still does not verify Karoo host lifecycle, FIT IPC or battery use.

When changing equations, add deterministic tests with explicit timestamps and reference vectors, update [the implemented model reference](docs/wprime-algorithms.md), and validate the scientific interpretation separately from reproducing the current code.

## Actual CI and release behavior

Source: `.github/workflows/ci.yml`, `code-quality.yml` and `dependabot.yml`.

- CI runs required `spotlessCheck`, `:app:testDebugUnitTest`, `:app:lintDebug` and `:app:assembleDebug` in one build. Test and lint reports upload even after failure; APK artifacts retain 30 days, verification reports seven.
- The old masked detekt/lintKotlin job was removed. Required quality checks now run in ci.yml. code-quality.yml retains only disabled CodeQL pending compatibility verification; its badge was removed from README. No active security scan is claimed.
- Dependabot proposes Gradle and Actions updates weekly; it does not certify compatibility. Authentication for the external SDK must also work in update/PR contexts.
- CI credentials fall back to `github.actor`/`GITHUB_TOKEN`, or use GPR_USER/GPR_TOKEN secrets. Package access must be verified; no blanket instruction to enable PR approval or global write permissions is necessary.
- Creating a GitHub release triggers assembleRelease and uploads the APK, `app/manifest.json` and the icon. Publishing a tag alone does not trigger this release job.
- Release currently uses the debug signing configuration and has minification disabled. Preserve the installed signing identity when planning proper release-key management.
- Build and app manifest currently agree on 1.1.2 / code 12. Update APK name/URL, manifest version/code and release notes together; release notes must describe the final change. CI currently replaces release body with generic text.

## Local execution laboratory

```powershell
.\scripts\start-simulator.ps1 -Serial emulator-5554
# Start another configured AVD when no emulator is running:
.\scripts\start-simulator.ps1 -Avd Medium_Phone_API_35
# Reuse the built APK and play a predefined scenario:
.\scripts\start-simulator.ps1 -Serial emulator-5554 -SkipBuild -Scenario 'Intervals' -Speed 20 -AutoStart
```

Android Emulator and an AVD must be installed. The script discovers the SDK, validates emulator-only serials, waits for boot, builds the debug APK and starts its debug launcher. It will not install on a physical Karoo.

The lab renders `WPrimeGlanceView` through real Glance RemoteViews, using a logical 480×800 Karoo canvas and selectable field sizes: full-width row 480×240, half-row 240×240 and full-screen 480×800. Select percent/kJ. Settings opens `MainActivity`, uses actual DataStore configuration and hot-loads changes on return. Start records/resumes, Pause ride recovers at 0 W while virtual time advances, Reset restores full balance/time, Freeze clock stops time, and Step advances manually. Power controls are −10,−1,+1,+10,0,CP plus direct input. Playback speed is independent of modeled elapsed time.

CSV format is `time_s,power_w,event`, starts at 0 and uses strictly increasing times; blank power means silence. Events: RECORDING, PAUSED, IDLE, LOST, FOUND. Import is bounded to 5 MB/100000 samples. A bundled example and deterministic scenarios cover efforts, recovery, exhaustion and loss. Diagnostics expose balance, virtual time, settings, ride/sensor state, simulated threshold alerts and FIT-value preview. No FIT file or Karoo alert is emitted.

For repeatable smoke checks the debug Activity accepts intent extras `scenario`, `speed`, `steps`, `autoStart`, `layout` (selector labels) and `field` (`%`/`kJ`). It is absent from the release manifest. Emulator rendering is useful evidence for visual/engine changes; service startup, KOS specifics, audible alerts, exported FIT and real battery usage remain device checks.

## Implementation status on this feature branch

- A1–A4: shared serialized engine/runtime, raw-power authority, explicit ride boundaries, monotonic time, isolated preview, preserved cosmetic balance and shared alerts implemented. Explicit sensor loss holds recording balance; pauses retain recovery.
- E1–E3: shared/gated subscription and recovery timer, visible render-key filtering before composition, on-demand ≤1 Hz updates, cached configuration and debug-gated/lazy logging implemented. Battery savings require measurement.
- E4: SDK callbacks use nonblocking send; queue is currently unlimited to preserve integration samples. Stress-test callback rates and define a bounded lossless policy before introducing higher-rate sources. FIT projects shared state; confirm host write cadence and exported files on device.
- E5/N2/N4: lifecycle-aware UI collection, debounced parameter writes with focus/IME flush, atomic settings and alert CRUD, validated finite inputs, safe enum/JSON/IO defaults and save errors implemented. Verify fast exit/persistence and FTP on actual host.
- Phase 5: confirmed unused components, model hooks, old preview, unused resources and direct Nordic/Mapbox/Rx/navigation/ConstraintLayout dependencies removed; irrelevant constraints pruned. AppCompat theme and service DI retained.
- N1/N3: integration/lifecycle regression tests added; six scientific model equations deliberately retained. Primary-reference validation and cadence/reference vectors remain separate work.
- CI: required real tasks and test artifacts implemented. Workflow execution on GitHub and CodeQL compatibility remain unverified locally.
- Library version migrations, R8/resource shrinking, release signing and receiver hardening remain pending. The official candidate inventory below is preserved.

### Local verification of the implementation — 2026-10-03

- `spotlessCheck`, `:app:testDebugUnitTest`, `:app:lintDebug`, debug and release assembly succeeded. 23 unit tests executed: 15 engine, 5 DataStore, 3 CSV; no failures. Lint:0 errors/29 warnings, mainly pending dependency versions and debug-lab localization.
- Android 35 AVD `Medium_Phone_API_35`, serial `emulator-5554`: real Glance rendering inspected for all six layout/unit combinations. Ten-second 400 W replay matched the shared engine balance. Corrected clipped glyphs by reserving the TextView line box and remaining space below the fixed header.
- Actual configuration UI changed CP 250→ 300 W and Skiba Differential→Bartram; both persisted and appeared in lab diagnostics on return. No physical Karoo was installed or modified.
- `scripts/test-simulator.ps1 -Serial emulator-5554` passed actual UI/RemoteViews smoke checks: six views, +10/CP, continuing paused clock, freeze, reset and unavailable-sensor arrow suppression. Captures land under ignored `build/simulator-smoke/`. It uses an already built debug APK; run the launcher first when rebuilding.
- APK manifest inspection confirmed the laboratory Activity exists only in debug. Debug APK decreased from 26177263 to 24042731 bytes (about 8.2%) despite adding the lab. This measures package size, not energy savings.
- GitHub workflow execution, Karoo host integration, exported FIT, audio, scientific reference accuracy and battery discharge remain unverified. Use the device protocol below before release certification.

## Technical audit — 2026-10-03

Scope: Kotlin sources, Android resources/manifest, Gradle/catalog, resolved debug dependency graph, workflows, tracked and locally ignored Markdown. Findings below are static code evidence unless explicitly marked as measured. The findings below describe the initial baseline before this feature branch. Completed implementation and remaining work are distinguished below; they are not claims that the original defects still exist.

### Calculation ownership and lifecycle (P1)

**A1 — Shared mutable field calculator has concurrent writers.** In `WPrimeDataTypeBase.startStream`, the numeric producer and recovery ticker mutate the field calculator; `streamRealWPrimeData` adds a raw-power producer and a second ticker. The timestamp is volatile, but `updatePower`, model balance and configuration replacement are not atomic. Two callers can read the same previous timestamp and integrate overlapping intervals. Smoothed and raw sources also compete. With both fields and FIT active there are three calculator instances, potentially five power subscriptions and five recovery tickers; this is not a single consistent ride state. Exact active counts depend on host requests. Preview mutates the same field calculator too.

**A2 — Cosmetic changes reset physiological balance.** `WPrimeCalculator.updateConfiguration` always constructs a full model, even when only alerts, colors, arrows or recordFit change, or an equivalent user profile is emitted. It preserves the old timestamp. A harmless settings change can therefore restore W′ to full capacity. Initial FTP null/fallback then real profile can cause another reset. Views alone do not collect model configuration; they rely on numeric streaming having configured the shared instance.

**A3 — Ride boundaries and missing power have incomplete policies.** Field reset follows `startStream`, not RideState; page/stream lifecycle may reset balance mid-ride. FIT updates its model before checking Idle, does not explicitly reset on new rides, and keeps consumers while recordFit is false. Re-enabling FIT can integrate a gap using the new sample. `combine(power, rideState)` also reuses the last power on a ride-state change. Wall-clock timestamps permit clock corrections; use a monotonic clock for durations. Keep zero-power recovery during genuine stops, but explicitly distinguish pause, sensor loss and Idle rather than removing recovery wholesale.

**A4 — Alerts depend on views and miss synthetic recovery.** `checkAlerts` runs only in the graphical real-power collector. Both fields can fire the same alert with independent cooldowns; numeric-only/FIT-only usage does not run alerts. Recovery tickers do not detect REPLENISH crossings. The settings test broadcast omits `alertType`, so REPLENISH tests default to DROP. Sorting thresholds descending does not always choose the most critical downward alert, despite the comment. Choose severity and session reset rules explicitly.

### Battery and CPU work (P1/P2)

**E1 — Unnecessary producers and wakeups.** Each active context polls every three seconds even before a sample or at full capacity; after silence it can repeatedly calculate/emit/log unchanged full W′. FIT-disabled polling still wakes and checks a Boolean. There are no application wake locks or direct scan loops in the reviewed sources; timer overhead does not by itself prove the device stays awake. Reduce work by shared ownership, lifecycle gating and an event-driven/adaptive recovery deadline. Keep enough timing information to integrate a complete elapsed interval after a stop.

**E2 — Rendering recomposes every display emission.** `startView` composes RemoteViews and pushes them for every item without equality filtering, conflation or an explicit rate limit. The SDK only accepts graphical updates at 1 Hz ([ViewEmitter contract](https://hammerheadnav.github.io/karoo-ext/karoo-ext/io.hammerhead.karooext.internal/-view-emitter/update-view.html)). Filter a render key containing formatted value, color band, arrow state, label/layout and enabled options before composition. A changing raw watt number need not trigger a frame if the visible field is identical. Do not throttle the model's power integration.

**E3 — Repeated configuration decoding and logging.** Views call `configuration.first()` per sample and recovery tick. DataStore can cache storage; this is not proof of a disk read every second, but the cold mapping reparses alerts JSON, allocates a configuration and formats logs every collection. Share latest validated settings in memory. `WPrimeApplication` plants DebugTree in release; color processing emits two formatted debug messages per sample, and layout logs run during composition. Gate high-frequency logging and construct expensive messages lazily. A release filter alone still pays eager string-formatting costs.

**E4 — FIT IPC and callback backpressure.** Session writes occur per combined event while Paused, though session summaries need an explicit write policy. FIT's stale ticker changes balance but does not itself emit a FIT message. `streamDataFlow` uses `trySendBlocking`, potentially blocking the SDK callback if its buffer fills. Evaluate buffering and backpressure with realistic sample rates; do not drop integration samples blindly. Batch field values (already done) and measure messages/second before changing cadence.

**E5 — UI activity work.** `ConfigurationScreen` uses collectAsState rather than lifecycle-aware collection. The ViewModel retains a profile subscription until cleared. `CompactSettingField` persists each valid intermediate keystroke; this creates repeated DataStore edits and, currently, model resets. Use an explicit apply/focus-loss/debounce policy with pending text and truthful save/error feedback. Confirm the last edit survives closing the screen.

### Numerical correctness and persistence (P1)

**N1 — Model names/comments overstate their implementations.** Skiba2012 never computes its documented tau and matches the differential recovery for valid positive capacity. Bartram's tau fallback exists but the app always passes a non-null value. Bartram and Caen recovery divide a CP/capacity recovery rate by another tau; units/parameters need scientific review. Chorley repartitions the remaining deficit 30/70 on every update, rather than preserving two recovery reservoirs, making results dependent on step partitioning. These are model accuracy questions, not evidence that exponentials are the battery bottleneck. See the exact implemented equations in the model reference.

**N2 — Domain input validation is incomplete.** Settings setters do not validate finite values/ranges. Model enum deserialization uses unguarded valueOf; unknown persisted names can cancel a collector. Configuration accepts capacity zero, but several models divide by capacity. NaN power is not rejected by `<`/`>` bounds. Positive Infinity can pass UI positivity checks. Validate constructor/update/persistence inputs consistently, tolerate unknown enum names, handle DataStore IO failures, and distinguish invalid samples from intentional 0 W recovery.

**N3 — Timestamp integration approximations require tests.** The calculator applies the current sample over the elapsed interval, caps gaps at one hour and has no replay tests. Define interval ownership and sensor-silence policy; test timestamp repeats/backwards, long gaps, CP boundary, invalid power, capacity clamps and variable sample cadence. Make any equation correction independently reviewable from the runtime refactor.

**N4 — UI settings state can race.** The ViewModel reads configuration once, then mutates local copies after independent asynchronous writes. Rapid alert operations can compute lists from stale state; disabling FTP performs two writes, publishing an intermediate configuration. Observe DataStore as the authoritative state and apply related edits atomically; serialize alert CRUD and expose loading/persistence errors.

### Dead code, duplication and packaging (P2)

Search evidence is repository-local; confirm generated/resource consumers before removal.

| Candidate | Evidence and proposed action |
| --- | --- |
| `ui/components/ConfigurationCard.kt` | Declaration only, no callers; obsolete editor duplicates CompactSettingField. Remove after build checks. |
| `ViewModelModule.kt` | No Hilt ViewModel or injection consumer; manual service/factory used by ConfigurationScreen. Remove or adopt Hilt deliberately. ServiceModule is used and must remain. |
| `getFormatDataTypeId`, `getUnitText`, `getTargetHeightFraction` | Base declarations and overrides without callers; numeric fields use SINGLE and graphical labels directly. Remove unused hooks/associated imports. |
| kJ `getSizeScale()` override | Returns the base default 1.0; redundant. |
| `logDataTypeUpdate`, `logStateChange`, `logDataFlow`, `logError` | No callers. Several LogConstants also have only declarations. Trim by symbol usage, preserving active logging. |
| `WPrimeRemoteViewActivity` | Unregistered scaffold with uncancelled emitter/local service. Either make a debug-only lifecycle-safe preview or remove it; do not treat preview composables as dead code. |
| `attrs.xml` LineChart, empty `dimens.xml`, `res/pic1.png` | No corresponding chart/use found. Run resource lint and check historical tooling before removal. Keep actually referenced drawable variants. |
| raw-power unavailable/else view branches | Identical display-state construction; centralize projection instead of copying blocks. |
| repeated stream/view/FIT setup | Recovery intervals, settings resolution and calculation plumbing diverge; shared runtime addresses duplication and correctness together. |
| unused direct dependencies | No imports/usages for Nordic BLE, Mapbox Turf, coroutines-rx2, navigation-compose, hilt-navigation-compose or ConstraintLayout. Resolved graph includes Nordic alpha modules, RxJava 2.2.8 and Mapbox → Gson 2.8.9. Prefer removal to upgrades if unused; confirm SDK transitive needs. AppCompat remains referenced by the theme. |
| build residue | viewBinding enabled without layouts/binding usage; Dokka/android-library plugins declared without a module/task consumer; review before pruning. |

Unused dependencies affect APK size, method/resource footprint and maintenance. Their mere presence does not establish active Bluetooth/network battery consumption. BLE permissions are declared but `scansDevices=false` and there is no app scan code; remove unnecessary permissions after checking merged manifest and host requirements.

Release lacks R8 shrinking. After dead-code/dependency cleanup, trial minification/resource shrinking separately and verify Glance, Hilt, serialization, SDK AIDL and FIT on device. Release IN_RIDE_ACTION uses Class.forName/createInstance on broadcast input; replace with a debug-only allowlist or remove test-only control paths. Receivers use the old registration overload; review target-37 export requirements on newer Android, while distinguishing Karoo's Android 12 behavior.

### Dependency updates checked against official sources

Snapshot checked 2026-10-03. These are candidates, not validated upgrades. Match minSdk, AGP built-in Kotlin, Compose compiler/plugin, KSP and Hilt constraints before changing versions.

| Component | Declared | Verified candidate / action |
| --- | --- | --- |
| Kotlin plugin aliases / metadata constraints | 2.4.0 | 2.4.20; verify AGP's actual compiler separately ([Kotlin releases](https://kotlinlang.org/docs/releases.html)). |
| AGP | 9.2.1 | 9.3.0; official minimum Gradle 9.5.0, JDK 17. Wrapper is already 9.5.1 ([release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes)). |
| KSP | 2.3.9 | 2.3.12; processor compatibility and backing-field changes need checking ([releases](https://github.com/google/ksp/releases)). |
| Hilt | 2.59.2 | 2.60.1; upgrade plugin/runtime/compiler together ([releases](https://github.com/google/dagger/releases)). |
| Core KTX | 1.19.0 | 1.19.1 |
| Lifecycle | 2.10.0 | 2.11.0 |
| Glance | 1.1.1 | 1.2.0; certify actual Karoo RemoteViews behavior |
| AppCompat | 1.7.1 | 1.8.0; still needed by theme |
| ConstraintLayout | 2.2.1 | 2.2.2 if retained; removal preferred |
| Navigation | 2.9.8 | 2.10.2 if retained; removal preferred |
| Compose BOM / ui-tooling | 2026.05.01 / 1.11.2 | Refresh BOM as a coherent group; official AndroidX table lists Compose UI 1.12.1. Remove independent tooling pin where appropriate. Exact target BOM not established here. |
| karoo-ext | 1.1.9 | Current official release; KOS minimum 1.634.2440 ([releases](https://github.com/hammerheadnav/karoo-ext/releases)). |
| Coroutines | 1.11.0 | Current official stable release reviewed ([releases](https://github.com/Kotlin/kotlinx.coroutines/releases)); remove unused Rx2 bridge. |
| Serialization | 1.11.0 | Stable reviewed; 1.12.0-RC is a prerelease, not a routine production upgrade ([releases](https://github.com/Kotlin/kotlinx.serialization/releases)). |

AndroidX candidates above come from the [official release table](https://developer.android.com/jetpack/androidx/versions). Newer libraries can increase minSdk; the declared minSdk 23 requires deliberate compatibility decisions even though Karoo 3 runs Android 12.

The local lint report suggests older candidates for some components (for example Hilt 2.60 and Compose BOM 2026.06.00); it is not authoritative for the latest release. Official sources and the resolved local graph are recorded separately.

Netty, Commons Lang, HttpClient, BouncyCastle, Jackson, jose4j and JDOM entries are dependency constraints, not automatic runtime inclusions. None of those families appears in the resolved debugRuntimeClasspath report reviewed here. Their security-fix comments are not a vulnerability scan and the listed GHSA claims were not verified. Check advisory IDs against actual resolved artifacts, remove irrelevant overrides or annotate justified ones. No blanket claim that the APK is vulnerability-free is warranted. Spotless, Dokka, Gradle, Nordic, Mapbox and every transitive patch were not exhaustively checked for latest versions; unused entries should be removed first and remaining updates resolved through authenticated Dependabot/metadata checks.

### Initial baseline verification (before implementation)

- `:app:assembleDebug` and `spotlessCheck`: successful, largely UP-TO-DATE.
- `:app:testDebugUnitTest`: NO-SOURCE; zero project unit tests executed.
- `:app:lintDebug`: successful; report shows 0 errors / 10 dependency-version warnings, largely cached. This does not invalidate runtime findings.
- `:app:dependencies --configuration debugRuntimeClasspath`: resolved successfully. Local report saved under ignored `build/audit-validation.log`; lint report is `app/build/reports/lint-results-debug.txt`.
- Initial sandbox invocation could not read the JDK security configuration; the same tasks succeeded outside the sandbox. This was environment access, not a source/build failure.
- No installation, ride, FIT export or battery measurement performed for this documentation-only audit. Existing screenshots are not new evidence.

## Prioritized improvement plan

Phases1–3 and the persistence/dead-code portions of4–5 are implemented in this branch with the local laboratory. Remaining priorities are scientific model/reference validation (4), packaging/dependency migrations (6), receiver hardening and device/battery certification (7). Keep equation and toolchain changes separately reviewable.

| Phase | Work and dependencies | Completion criteria |
| --- | --- | --- |
| 1 — Establish trustworthy checks (P1) | Replace masked nonexistent CI tasks with spotlessCheck; make actual tests/lint required, upload test reports, remove repeated builds. Add deterministic domain tests and disclose NO-SOURCE until replaced. Check CodeQL compatibility before re-enabling. | A deliberate test/style failure fails CI; test artifacts show nonzero executed tests. No claim of active scanners while disabled. |
| 2 — Fix ownership and resets (P1) | Define raw/smoothed source semantics; implement one serialized ride runtime with immutable state shared by both fields, FIT and one alert manager. Isolate preview. Deduplicate physiological configuration; define state preservation for CP/model/capacity changes. | Same source/timestamps produce consistent fields and FIT; no cosmetic reset, overlapping integrations or duplicate alerts. Page changes preserve ride state; new ride reset is explicit. |
| 3 — Reduce continuous work (P1/P2) | After phase 2, gate consumers/recovery by ride/consumer demand, share latest settings, deduplicate render keys before composition, enforce ≤1 Hz graphics, gate lazy logs and define FIT write cadence. Keep genuine stop recovery and alert crossings. | One owner per model, bounded frame/IPC rate, no unchanged full-capacity renders, no repeated settings parsing per sample; idle/FIT-disabled work matches agreed policy. |
| 4 — Validate models and settings (P1) | In parallel with test groundwork, validate equations against primary scientific references; fix Skiba2012 and other discrepancies in dedicated changes. Reject nonfinite/invalid domain inputs; safe enum/IO fallback; atomic settings and alert CRUD; fix REPLENISH test intent. | Reference vectors, CP boundaries, zero/NaN/Infinity, cadence and corrupted settings tests pass; last UI edit persists and failures are visible. Model behavior changes documented. |
| 5 — Remove residue (P2) | Delete confirmed unused components/hooks/resources/dependencies; keep or replace preview intentionally. Prune irrelevant security constraints and build settings only after resolved graph checks. | Build/lint/style pass, host registration/themes/preview references intact, before/after APK and dependency inventory captured. |
| 6 — Upgrade and release hygiene (P2) | Upgrade compatible toolchain/KSP/Hilt group, then AndroidX/Compose/Glance separately; maintain minSdk/KOS decisions. Trial R8, manage stable signing keys, harden receivers and synchronize release metadata. | Debug/release builds, required CI and device matrix pass; existing installation upgrades with the correct signature, FIT definitions stay compatible. |
| 7 — Certify battery and ride behavior (P1 validation) | Capture a baseline before phase 2/3 and repeat after them and dependency updates. Use the protocol below. | Measured CPU/frame/IPC improvements and repeatable battery results reported with conditions; no regression in balance, alerts, recovery, FIT or visual states. |

### Battery measurement protocol

Use the same Karoo/KOS, brightness, screen state, sensors, ride profile, settings, thermal conditions and initial charge; disconnect charging/ADB cable during timed runs. Repeat comparable runs and alternate baseline/candidate order. Do not quote a battery-saving percentage from static code analysis.

Exercise no active field, numeric-only requests where reproducible, one visible field, both fields, FIT on/off, sensor loss, autopause/long stop, recovery to full, page navigation, settings UI open/closed and next ride. Record active power/profile consumers, model updates/s, timer wakes, configuration decodes/s, RemoteViews compositions/accepted updates, FIT IPC/s, log volume, process CPU/memory and battery discharge over equal intervals.

Capture `adb shell dumpsys batterystats com.itl.wprimeext` before/after comparable sessions and `adb shell dumpsys cpuinfo`; use a supported profiler/Perfetto for scheduling and CPU detail if available. Avoid resetting global battery statistics without explicit authorization. Save timestamps, APK/version, KOS, scenario durations and screenshots/FIT samples needed to reproduce the comparison. Prefer low-overhead counters over per-sample logging, which itself changes the measurement.

### Documentation consolidation performed

- AGENTS now states shared ownership, lifecycle/sensor policy, FTP source, actual DI, local laboratory, units and verification requirements.
- README retains rider guidance but describes the actual model limitations and links to this plan.
- CI setup is consolidated here. QUICKSTART-CI and the locally ignored English/Spanish CI guides were obsolete duplicates and removed.
- The algorithm reference is retained, rewritten against current code and made visible to Git by removing the blanket docs ignore.
- Copilot guidance points to AGENTS rather than repeating contradictory service/layout rules. PR template requests concrete verification and limitations.
- The original documentation audit was followed by the implementation summarized above; library version upgrades remain pending.

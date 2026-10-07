# Contributing and improvement plan

## Development setup

Use [AGENTS.md](AGENTS.md) for constraints and [README.md](README.md) for rider setup. `:app` is the production Karoo APK; `:shared` is an Android library containing calculation, settings and UI; `:simulator` is a standalone testOnly emulator application. The simulator package/configuration store is independent from the installed Karoo app. The SDK remains an external authenticated GitHub Packages dependency, including shared UI types.

AGP 9.4.1 supplies built-in Kotlin; plugin aliases/metadata use Kotlin 2.4.0. Wrapper 9.8.0, compile/target 37, min 23 and Java compatibility 11 are declared. Daemon criteria request OracleJDK 24; CI installs OracleJDK 24.0.2 directly from its archive to satisfy daemon criteria without the obsolete Foojay redirect. Check actual Gradle compiler/daemon versions rather than treating aliases as the effective compiler.

Configure ignored `local.properties` or user Gradle properties:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_READ_PACKAGES_TOKEN
```

Settings also accepts GPR_USER/GPR_API_KEY and GitHub fallbacks. CI maps GPR_TOKEN to gpr.key. Never commit credentials.

```powershell
.\gradlew.bat spotlessCheck :shared:testDebugUnitTest :simulator:testDebugUnitTest :shared:lintDebug :app:lintDebug :simulator:lintDebug :app:assembleDebug :simulator:assembleDebug :app:assembleRelease --console=plain
git diff --check
.\scripts\start-simulator.ps1 -Serial emulator-5554
```

Spotless uses ktlint; detekt/lintKotlin are not configured. Report executed counts; NO-SOURCE is not test coverage. Avoid routine clean builds. Preserve unrelated changes; commits require authorization.

## Local execution laboratory

The emulator-only launcher builds `:simulator:assembleDebug`, checks APK identity and installs with testOnly support. Gradle simulator install tasks are disabled. Pass `-Avd Medium_Phone_API_35`, `-SkipBuild`, `-Scenario Intervals`, `-Speed 20` or `-AutoStart` as needed.

The laboratory renders shared WPrimeGlanceView RemoteViews in full-width 480×240, half-row 240×240, compact 480×148 / 240×148 or full-screen 480×800 layouts. A local 320 dpi configuration keeps preview dimensions in actual pixels regardless of emulator density. The emulator substitutes fonts absent from stock Android; certify Relative/IBM Plex typography on Karoo. The optional launch extra `alignment` accepts LEFT/CENTER/RIGHT for layout checks. Choose percent/kJ, open actual settings and return to verify persisted CP/model. Start/resume, pause ride, reset, freeze/step virtual time, manual power and CSV replay cover depletion/recovery/loss. Paused activity continues zero-power recovery; freezing the clock stops virtual time.

CSV uses time_s,power_w,event, starts at 0 and requires increasing times; blank power means silence. Events include RECORDING,PAUSED,IDLE,LOST,FOUND. Import limits are 5 MB/100000 samples. Alerts and FIT values are diagnostics, not Karoo IPC or an exported FIT file. The simulator cannot certify host lifecycle, KOS layout quirks, audible alerts or real battery use.

## CI, releases and signing

`ci.yml` requires Spotless, shared/simulator unit tests, all module debug lint, Karoo debug and simulator debug builds. Reports upload after failures. CodeQL remains disabled pending compatibility validation; no active security scan is claimed. Dependabot proposals require authenticated resolution and compatibility testing.

Publish a stable release through `workflow_dispatch`, selecting the release tag as the workflow ref and passing that same tag as the `tag` input. Both jobs check out the immutable `github.sha`; the tag must point to that exact verified workflow commit and match Android/manifest versions. Integrate workflow changes into the default branch before creating the release tag: GitHub requires `Workflows: write` when the target's workflow files differ from the default branch, and `GITHUB_TOKEN` cannot receive that permission ([GitHub API contract](https://docs.github.com/en/rest/releases/releases#create-a-release)). Dispatch without a tag runs checks only. Only the signed Karoo APK, `app/manifest.json` and icon are release assets; simulator APKs are excluded.

The release job alone grants `contents: write`; build/check jobs keep read access. CI uses the runner's GitHub CLI with `GITHUB_TOKEN` to create or reuse a draft, upload all three verified assets, then publish as stable/latest. It updates an existing draft's target to the verified commit without moving the tag. Retries skip assets only when their SHA-256 digests match; conflicting assets and incomplete published releases stop publication. API/authentication errors stop the job rather than being treated as a missing release. Keep the four signing secrets separate from GitHub authentication; a successful certificate check followed by a release API 403 is a publication problem, not a keystore failure. No publication PAT is required for the default release flow.

For each new release, update Android versionName/versionCode and manifest versions/download URLs together, integrate and verify the release commit on the default branch, then create and push the matching tag. Dispatch that tag explicitly, for example `gh workflow run ci.yml --repo apopovsky/WPrimeKarooExtension --ref v1.2.2 -f tag=v1.2.2`. Changing a branch's workflow does not update an existing tag or an old run: use a fresh dispatch after preparing the correct ref. Never move a published release tag. Preserve the same persistent signing key across subsequent versions.

Version 1.2.2/code 16 and manifest download URLs must match the APK and concrete tag. Release notes are read from `app/manifest.json` so the Karoo manifest and GitHub release describe the same changes. Release enables R8/resource shrinking with conservative extension/SDK keeps. Successful minification is build evidence; Glance/Hilt/serialization/FIT host certification still requires device testing.

Configure repository Actions secrets `RELEASE_STORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` using the persistent release keystore introduced in 1.2.0. The pipeline restores it in the runner's temporary directory, signs without Gradle configuration caching, verifies the APK package/version and checks its certificate against the pinned release identity before publication. Missing credentials or a different signer stop publication. The temporary key is removed after the build; never commit a private key or password.

Local `:app:assembleRelease` builds an unsigned APK when signing credentials are absent. To sign locally, provide `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`, or an ignored root `keystore.properties` containing `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Relative keystore paths resolve from the repository root. Environment variables override local properties. Use `--no-configuration-cache` for signed builds. Keep a secure backup of the keystore and credentials; GitHub secrets cannot be downloaded. Do not use the debug keystore for production distribution. The public SHA-256 certificate fingerprint for 1.2.0 and subsequent releases is `426ef0b5e196bc3088112ce7dec780526218ba23723e1b24a44211a30044ff4a`.

## Technical audit — 2026-10-03

Implemented improvements:

- One serialized raw-power owner publishes immutable state to both fields, alerts and FIT; preview/simulator are isolated. Cosmetic changes preserve balance; physiological updates retain its fraction.
- Monotonic acquisition timestamps and sample sequence numbers are captured at the callback. A 64-entry DROP_OLDEST buffer bounds callback backlog; sequence gaps expose overload. Dropped samples can lose short effort details under overload, so verify diagnostics on hardware.
- IDLE resets; PAUSED recovers; explicit sensor loss holds recording balance and excludes the unknown interval. Silent streams retain >5-second recovery. A single 3-second ticker is suspended when unnecessary.
- Unchanged render keys are filtered before composition; conflated RemoteViews updates are spaced at least 1 second. Integration/alert detection retains its own cadence.
- FIT recording retains distinct timestamps while suppressing duplicate same-time records; paused session writes skip unchanged rounded values. Definitions/units remain compatible.
- Finite settings validation, safe enum/JSON/IO reads, authoritative UI state, atomic FTP-source/alert edits, deferred numeric writes and visible save failures.
- Release TEST_ALERT is non-exported; validation/rate limiting apply. Ride-action tests are debug-only with an allowlist; reflective arbitrary class construction is removed.
- Debug logging is gated/lazy. Unused provider/editor/preview/resources, direct BLE/Mapbox/Rx/navigation/ConstraintLayout dependencies and irrelevant transitive constraints were removed.
- Release shrinking is enabled; standalone simulator separation removes its service/control paths from Karoo packaging.

Scientific model limitations remain described in [the algorithm reference](docs/wprime-algorithms.md). No named model has been scientifically certified, and no battery-saving percentage has been measured.

## Dependency updates

Applied: [Core 1.19.1](https://developer.android.com/jetpack/androidx/releases/core#1.19.1), [Hilt 2.60.1](https://github.com/google/dagger/releases/tag/dagger-2.60.1) and [KSP 2.3.12](https://github.com/google/ksp/releases/tag/2.3.12). Hilt's minSdk 23 and KSP's minAGP 8.12 are compatible with declared settings; KSP includes fixes for AGP 9 built-in Kotlin. Full toolchain/Compose/Glance migrations remain separate work. Catalog/build files are authoritative; confirm resolved runtime/processor artifacts in build reports.

## Prioritized improvement plan

| Priority | Remaining work | Evidence required |
| --- | --- | --- |
| P 1 | Certify installation and subsequent updates using the persistent release key | Device installation and same-certificate update preserving settings |
| P 1 | Karoo certification: both fields, recording/pause/resume/Idle, page navigation, loss, alerts and decoded FIT | Screenshots, sound observation, decoded values and lifecycle logs from target KOS |
| P 1 | Scientific equations/reference vectors | Primary publications, CP boundaries, cadence partitioning and numerical expectations |
| P 1 | Measure energy changes against baseline | Controlled CPU/frame/IPC/battery runs using protocol below |
| P 2 | Further Kotlin/AGP/AndroidX/Glance migrations | Compatible SDK/minSdk, coherent version groups and regression matrix |
| P 2 | Narrow conservative R 8 keeps after host certification | Release package inventory plus lifecycle/RemoteViews/FIT tests |

### Battery measurement protocol

Use the same Karoo/KOS, brightness, screen state, sensors, ride profile, settings, thermal conditions and initial charge; disconnect charging/ADB cable during timed runs. Repeat comparable runs and alternate baseline/candidate order. Do not quote a battery-saving percentage from static code analysis.

Exercise no active field, numeric-only requests where reproducible, one visible field, both fields, FIT on/off, sensor loss, autopause/long stop, recovery to full, page navigation, settings UI open/closed and next ride. Record active power/profile consumers, model updates/s, timer wakes, configuration decodes/s, RemoteViews compositions/accepted updates, FIT IPC/s, log volume, process CPU/memory and battery discharge over equal intervals.

Capture `adb shell dumpsys batterystats com.itl.wprimeext` before/after comparable sessions and `adb shell dumpsys cpuinfo`; use a supported profiler/Perfetto for scheduling and CPU detail if available. Avoid resetting global battery statistics without explicit authorization. Save timestamps, APK/version, KOS, scenario durations and screenshots/FIT samples needed to reproduce the comparison. Prefer low-overhead counters over per-sample logging, which itself changes the measurement.

## Current local verification

Verification on 2026-10-03 after integrating master and local AGP 9.4.1 / Gradle 9.8.0 updates: 36 unit tests passed with zero failures (Engine 15, Settings 5, Input 5, FIT 6, Commands 2, Scenario 3). Spotless, all module debug lint, production debug/signed release and simulator debug builds passed. Lint has zero errors and 1 shared / 9 app / 11 simulator warnings. Workflow YAML, Bash/Python syntax, version/URL checks and missing-secret rejection were verified locally. The new persistent signing key is provisioned in Actions. The signed APK passed apksigner verification and its certificate matches the pinned release identity. The maintainer reported basic operation on Karoo; full lifecycle, alert sound, decoded FIT and battery certification remain pending.

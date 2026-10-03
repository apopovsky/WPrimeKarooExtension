# W′ model implementation reference

Reviewed against `app/src/main/kotlin/com/itl/wprimeext/extension/WPrimeCalculator.kt` on 2026-10-03. This describes the current code, not scientific certification of the named models. See [the technical audit and plan](../CONTRIBUTING.md#technical-audit--2026-10-03) before changing equations.

## Units and integration

Let P = input power (W), C = Critical Power (W), A = configured W′ capacity (J), B = current balance (J), D = A − B and dt = elapsed seconds. For valid positive capacity the reported percentage is 100 B/A. The model starts at B=A.

`WPrimeCalculator.updatePower(power, timestamp)` accepts milliseconds. The first update initializes time; later calls use current power over elapsed dt, with a 3600 s gap cap. Calculator calls at repeated/backwards timestamps do not integrate. The engine rejects backwards time, nonfinite power and samples outside 0–2000 W. Production uses monotonic elapsedRealtime; the lab supplies virtual timestamps.

Configuration validates finite positive CP, capacity and tau, and finite nonnegative kIn. Equivalent/cosmetic updates preserve balance and integration time; physiological changes preserve the remaining fraction and rebase the interval. These protections do not correct the scientific equations below.
## Implemented equations

All models deplete above CP by `(P − C) × dt`, with the balance bounded at zero. Recovery differences are below. The enum names are persisted API identifiers; renaming requires migration.

| Enum / class | Recovery implemented at P ≤ C | Important limitations |
| --- | --- | --- |
| `SKIBA_DIFFERENTIAL` / SkibaDifferentialModel | `B ← clamp(B + ((C−P)/A) D dt, 0, A)`; rate is zero when A≤0 | Default. Explicit Euler update can vary with timestep. |
| `SKIBA_2012` / Skiba2012Model | `B ← min(A, B + ((C−P)/A) D dt)` | Same recovery as Differential for valid positive inputs. The tau expression in KDoc is not computed; no classic monoexponential-history integration is implemented. |
| `BARTRAM` / BartramModel | `B ← clamp(B + ((C−P)/A) D dt/tau, 0, A)` | tau is the supplied override, otherwise `2287 × max(C−min(P,C), 1)^(-0.688)`. The app supplies a non-null tau (default 300), so the fallback is not used by current application callers. Dimensional/scientific interpretation needs validation. |
| `CAEN_LIEVENS` / CaenLievensModel | Same rate as Bartram, using tau=350 when P/C<0.6, 700 when P/C<0.9, otherwise 1000 | Hardcoded domain constants; CP≤0 returns tau=1000. Scientific basis and rate units need validation. |
| `CHORLEY` / ChorleyModel | `B ← min(A, B + 0.3D(1−exp(−dt/60)) + 0.7D(1−exp(−dt/400)))` | Constants and component weights are fixed. Repartitions the remaining deficit every step; no independent fast/slow reservoir states. Recovery also occurs at P=C. |
| `WEIGEND` / WeigendHydraulicModel | For P<C: `B ← clamp(B + kIn(C−P)(1−B/A)dt, 0, A)`; no inflow at P=C | Default kIn=0.002. Simplified single-balance hydraulic implementation; capacity validation and parameter interpretation need review. |

Bartram, Caen, Differential and Weigend clamp balance to [0,A]. Skiba2012 and Chorley clamp depletion at zero and recovery at A. Do not duplicate replacement Kotlin implementations here; change the source and focused tests together.

## Configuration and power sources

DataStore persists manual CP (250 W), CP source (MANUAL by default), capacity (12000 J), tauRecovery (300 s), kIn (0.002), model (SKIBA_DIFFERENTIAL), recordFit/showArrow/useColors (true) and alerts (empty). Settings are Preferences with an alerts JSON list; the old proposed nested model/params JSON schema is not used.

`CriticalPowerResolver.kt` uses valid Karoo FTP × 0.95 only when KAROO_FTP is selected; otherwise it falls back to stored manual CP. That factor is the application's heuristic, not proof that FTP and CP are physiologically interchangeable.

One shared runtime consumes raw POWER and exposes the same immutable balance to numeric fields, graphics, alerts and FIT. Idle resets and does not integrate; paused activity recovers at 0 W. A gated 3 s ticker retains zero-power recovery after >5 s of silence. Explicit unavailable sensor during recording holds balance and excludes the unknown interval on reconnect; paused recovery remains enabled. The local debug lab uses this engine and the actual field rendering with synthetic power.
## Required validation before model fixes

Existing deterministic engine tests cover integration and lifecycle. Extend them for scientific reference validation: above/below/equal CP, full/empty balance, variable intervals, duplicate/backwards timestamps, long stops, zero/nonfinite parameters and equivalent elapsed-time partitioning. Add reference vectors derived from primary scientific publications before claiming fidelity to the model names. Preserve released enum IDs and explain numerical changes to users. Measure before optimizing exp/pow; repeated subscriptions, rendering and logging are stronger current battery candidates than a few arithmetic operations per sample.

# W Prime Extension for Hammerhead Karoo 3

[![CI/CD - Build and Release](https://github.com/apopovsky/WPrimeKarooExtension/actions/workflows/ci.yml/badge.svg)](https://github.com/apopovsky/WPrimeKarooExtension/actions/workflows/ci.yml)
[![Code Quality](https://github.com/apopovsky/WPrimeKarooExtension/actions/workflows/code-quality.yml/badge.svg)](https://github.com/apopovsky/WPrimeKarooExtension/actions/workflows/code-quality.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Karoo%202%20%26%203-orange.svg)](https://www.hammerhead.io/)

> **Track your anaerobic capacity in real-time and never blow up on climbs or sprints again.**

<p align="center">
  <img src="media/screencap.gif" width="540" alt="W Prime Extension in Action"/>
</p>

---

## Table of Contents

- [What is This Extension?](#what-is-this-extension)
- [Quick Start](#quick-start-for-karoo-users)
- [Screenshots](#screenshots)
- [Understanding W Prime](#understanding-w-prime)
- [Choose Your Algorithm](#choose-your-algorithm)
- [Configuration Guide](#configuration-guide)
- [FAQ](#frequently-asked-questions)
- [Installation & Setup](#installation--setup)
- [Technical Details](#technical-details-for-developers)
- [Contributing](#contributing)
- [Support & Community](#support--community)

---

## What is This Extension?

This extension brings **W Prime (W') balance tracking** to your Karoo 2 & 3 cycling computers. W Prime represents your finite anaerobic energy reserve - think of it as a battery that depletes when you push hard above your threshold and recharges when you ease off.

### Why Should You Care?

If you're a cyclist who:
- **Races or does group rides** with surges and attacks
- **Climbs steep hills** and wants to pace efforts optimally
- **Does interval training** and wants to track recovery between efforts
- **Competes in criteriums, cyclocross, or mountain biking** with repeated hard efforts

...then W Prime tracking can help you **avoid "blowing up"** by showing exactly how much anaerobic energy you have left in the tank.

### What Makes This Extension Different?

✅ **Six scientific algorithms** - Choose the model that fits your training style  
✅ **Real-time tracking** - Updated every second during your ride  
✅ **Dual display options** - View as percentage (%) or absolute energy (Joules)  
✅ **FIT file integration** - Your W Prime data is saved in every activity  
✅ **Fully configurable** - Set your Critical Power, W', and algorithm parameters  
✅ **Zero subscriptions** - Free and open source


## Quick Start for Karoo Users

### 1. Install the Extension

Download the latest APK from the [Releases](https://github.com/apopovsky/WPrimeExtension/releases) page.

**Option 1: Companion App (Recommended - Easiest)**

The easiest way to install is using the Hammerhead Companion App on your smartphone:

1. On your phone, open the [Releases page](https://github.com/apopovsky/WPrimeExtension/releases) in your browser
2. Find the latest release APK file link
3. Long-press the APK download URL and select "Share" → **Hammerhead Companion App**
4. The APK will transfer to your Karoo
5. Tap **Install** on your Karoo when prompted

*Prerequisites:* Karoo firmware 1.538.2049+, Companion App (Android 1.36.0+ or iOS 1.12.0+), Wi-Fi enabled on Karoo.

For detailed instructions, see the [official Hammerhead guide](https://support.hammerhead.io/hc/en-us/articles/31576497036827-Companion-App-Sideloading).

**Option 2: ADB (Advanced Users)**

If you prefer command-line installation:

```bash
adb install WPrimeExtension-vX.X.X.apk
```

### 2. Configure Your Settings

1. Open the **W Prime** app from your Karoo's app drawer
2. Enter your physiological parameters:
   - **Critical Power (CP)**: Your functional threshold power × 0.95 (e.g., 250W for FTP of 263W)
   - **Anaerobic Capacity (W')**: Typically 12,000-20,000 J (start with 15,000 if unsure)
   - **Algorithm**: Start with "Skiba 2014 Differential" (the default)
3. Save your settings

### 3. Add Data Fields to Your Ride Profile

1. Go to **Settings → Profiles** on your Karoo
2. Select your ride profile (e.g., "Road Cycling")
3. Edit a data page
4. Add one or both W Prime fields:
   - **W Prime (%)** - Shows 0-100% remaining (most intuitive)
   - **W Prime (kJ)** - Shows absolute energy in kilojoules

### 4. Start Riding!

- **100%** = Full anaerobic capacity (fresh)
- **50%** = Half depleted (approach hard efforts cautiously)
- **0%** = Fully depleted (recovery mode - back off!)

Watch your W Prime drop during climbs or sprints, then recover during easy pedaling.

---

## Screenshots

### Configuration Interface

The extension provides an intuitive configuration screen where you can set all parameters:

<p align="center">
  <img src="media/config1.png" width="480" alt="W Prime Configuration Screen - Parameters"/>
  &nbsp;&nbsp;&nbsp;
  <img src="media/config2.png" width="480" alt="W Prime Configuration Screen - Algorithm Selection"/>
</p>

### In-Ride Display

See your W Prime balance in real-time during your rides:

<p align="center">
  <img src="media/main1.png" width="480" alt="W Prime Data Field on Karoo"/>
  &nbsp;&nbsp;&nbsp;
  <img src="media/screencap.gif" width="540" alt="W Prime in Action"/>
</p>

The data field updates every second, showing your remaining anaerobic capacity as you ride.

---

## Understanding W Prime

### The Science Made Simple

Your body has two main energy systems for cycling:

1. **Aerobic (sustainable)**: Powered by oxygen, can run indefinitely at Critical Power
2. **Anaerobic (limited)**: Used above Critical Power, finite capacity measured as W Prime

When you ride **above CP**, you drain your W Prime battery. When you ride **below CP**, it recharges - but recovery is slower than depletion.

### Example: The Sprint Scenario

Imagine you have:
- **CP**: 250W
- **W'**: 15,000 J (15 kJ)

**Sprint Attack (500W for 30 seconds):**
- Power above CP: 500W - 250W = 250W deficit
- Energy used: 250W × 30s = **7,500 J** (50% of W')
- Time to recover 50%: ~3-5 minutes at easy pace

**The Extension shows this in real-time**, so you know when you can attack again!


## Choose Your Algorithm

The extension includes **six scientific W Prime models**. Each has different characteristics for depletion and recovery.

### Recommended Algorithms

| Algorithm | Best For | Recovery Speed | Complexity |
|-----------|----------|----------------|------------|
| **Skiba 2014 Differential** | Most riders, general use | Moderate | Simple ⭐ |
| **Caen/Lievens** | Criteriums, short intervals | Domain-dependent | Moderate ⭐⭐ |
| **Bartram 2018** | Elite athletes, lab-tested | Individualized | Advanced ⭐⭐⭐ |

### All Available Models

#### 1. **Skiba 2014 Differential** (Default) ⭐ RECOMMENDED
- **Parameters**: CP, W'
- **Best for**: Road racing, gran fondos, general training
- **Recovery**: Exponential recovery based on power deficit
- **Why use it**: Most tested model, works well for most athletes

#### 2. **Skiba 2012 Monoexponential**
- **Parameters**: CP, W'
- **Best for**: Structured ERG workouts
- **Recovery**: Classic exponential with tau function
- **Why use it**: Original W Prime model, very conservative recovery

#### 3. **Bartram 2018** (Advanced)
- **Parameters**: CP, W', Tau (τ)
- **Best for**: Athletes with lab-tested recovery constants
- **Recovery**: Personalized tau calculation (τ = 2287 × D_CP^-0.688)
- **Why use it**: Most accurate if you know your specific recovery profile

#### 4. **Caen/Lievens Domain Model**
- **Parameters**: CP, W'
- **Best for**: Criteriums, cyclocross, mountain biking
- **Recovery**: Different recovery rates by intensity zone:
  - <60% CP: Fast recovery (τ=350s)
  - 60-90% CP: Moderate recovery (τ=700s)
  - \>90% CP: Slow recovery (τ=1000s)
- **Why use it**: Better models repeated surges and varied intensity

#### 5. **Chorley 2023 Bi-Exponential**
- **Parameters**: CP, W'
- **Best for**: Analysis and post-ride review
- **Recovery**: Fast (30%) + slow (70%) recovery components
- **Why use it**: Most physiologically accurate, computationally intensive

#### 6. **Weigend 2022 Hydraulic**
- **Parameters**: CP, W', kIn (inflow constant)
- **Best for**: Experimental use
- **Recovery**: Adaptive "filling tank" model
- **Why use it**: Research and experimentation


## Configuration Guide

### Finding Your Critical Power (CP)

**Option 1: From FTP**
```
CP ≈ FTP × 0.95
Example: 260W FTP → 247W CP
```

**Option 2: Power Duration Curve**
- Do maximal efforts of 3, 5, and 12 minutes
- Use online CP calculator (e.g., Cycling Analytics)
- Most accurate method

**Option 3: 20-Minute Test**
```
CP ≈ 20-min average power × 0.95
```

### Finding Your W Prime (W')

**Starting Point by Rider Type:**
- **Sprinter/Track**: 18,000-25,000 J
- **All-rounder**: 12,000-18,000 J
- **Endurance/Climber**: 10,000-15,000 J

**To Refine:**
1. Start with estimate (e.g., 15,000 J)
2. Do a maximal effort to exhaustion above CP
3. Time to exhaustion = W' ÷ (Power - CP)
4. Adjust W' based on actual performance

### Advanced Parameters

**Tau (τ)** - Only for Bartram model:
- Typical range: 200-600 seconds
- Higher = slower recovery (endurance athletes)
- Lower = faster recovery (sprinters)

**kIn** - Only for Weigend model:
- Default: 0.002
- Controls recovery rate responsiveness
- Experimental parameter

### FIT File Recording

The extension automatically saves two custom fields to every activity:
- **WPrimeJ** - W Prime balance in Joules
- **WPrimePct** - W Prime balance as percentage

This data can be analyzed in:
- **WKO5** (TrainingPeaks)
- **Golden Cheetah**
- **Intervals.icu**
- Any platform supporting FIT developer fields

**To disable FIT recording**: Toggle "Record to FIT File" off in settings.


## Frequently Asked Questions

### Does this replace power-based training?
No - it complements it! W Prime helps you understand **short-term energy availability** for efforts above threshold, while FTP/power zones guide overall pacing.

### Why does my W Prime recover so slowly?
Recovery is physiologically slower than depletion. The Skiba 2014 model typically shows ~5-10 minutes for full recovery after moderate depletion. This matches real physiology.

### Which algorithm should I use?
Start with **Skiba 2014 Differential**. Switch to **Caen/Lievens** if you do lots of short, repeated efforts.

### Can I use this for races?
Absolutely! That's the primary use case. Add W Prime % to your race profile to manage efforts strategically.

### My W Prime shows 0% but I can still sprint?
The model is conservative. 0% means you've depleted your calculated W', but you might have more if your parameters are off. Refine your CP and W' values.

### Does this work with ERG mode on trainers?
Yes, but W Prime is most useful for **variable power** rides (races, group rides, outdoor). For steady ERG intervals, it's less critical.


## Installation & Setup

### Requirements
- Hammerhead Karoo 3 (firmware 1.538.2049 or later for Companion App install)
- Power meter connected to Karoo
- Hammerhead Companion App (optional, for easy installation)
- ADB access (optional, for advanced installation)

### Step-by-Step Installation

#### Option 1: Install via Companion App (Recommended)

This method requires no cables or developer options.

1. **Prepare your devices**
   - Ensure Karoo is connected to Wi-Fi
   - Ensure phone has internet connection
   - Update Companion App to latest version

2. **Sideload the APK**
   - On your phone, go to the [Releases page](https://github.com/apopovsky/WPrimeExtension/releases)
   - Long-press the APK link (or share the downloaded file)
   - Select **Hammerhead Companion App** from the share menu
   - Watch for the "Transferring" screen on your phone

3. **Install on Karoo**
   - When the transfer completes, an "Install" prompt appears on Karoo
   - Tap **Install** to finish

#### Option 2: Install via ADB (Advanced)

1. **Enable Developer Options on Karoo**
   - Go to Settings → About
   - Tap "Build Number" 7 times
   - Return to Settings → Developer Options
   - Enable "USB Debugging"

2. **Install via ADB**
   ```bash
   adb connect <KAROO_IP>:5555
   adb install WPrimeExtension-vX.X.X.apk
   ```

3. **Configure Extension**
   - Open "W Prime" app from Karoo app drawer
   - Enter your CP, W', and select algorithm
   - Save settings

4. **Add to Ride Profile**
   - Settings → Profiles → [Your Profile]
   - Edit data page
   - Add "W Prime (%)" or "W Prime (kJ)"

### Troubleshooting

**Extension not showing in app drawer**
- Reboot Karoo
- Check Logcat for errors: `adb logcat | grep WPrime`

**Data field shows "--" during ride**
- Ensure power meter is connected
- Check that CP and W' are configured (open app)
- Verify extension is enabled in Settings → Extensions

**Values seem incorrect**
- Verify your CP is accurate (should be ~FTP × 0.95)
- Check W' is in reasonable range (10,000-20,000 J)
- Try a different algorithm (Caen/Lievens vs Skiba 2014)


## Technical Details (For Developers)

### Project Structure

```
app/src/main/kotlin/com/itl/wprimeext/
├── extension/
│   ├── WPrimeExtension.kt          # Main extension service, FIT integration
│   ├── WPrimeDataType.kt           # Percentage (%) data field
│   ├── WPrimeKjDataType.kt         # Joules (J) data field
│   ├── WPrimeDataTypeBase.kt       # Shared logic for both data fields
│   ├── WPrimeCalculator.kt         # Core algorithms (all 6 models)
│   └── WPrimeSettings.kt           # DataStore persistence
├── ui/
│   ├── viewmodel/
│   │   └── WPrimeConfigViewModel.kt  # MVVM state management
│   └── components/
│       └── ConfigurationCard.kt    # Reusable UI components
├── ConfigurationScreen.kt          # Main settings UI
├── MainActivity.kt                 # App entry point
└── utils/
    └── WPrimeLogger.kt            # Structured logging
```

### Architecture

**Data Flow:**
1. User configures parameters via Jetpack Compose UI
2. Settings persisted using DataStore (survives app restarts)
3. `WPrimeDataType` subscribes to power stream from Karoo
4. Calculator updates W' balance every second
5. Value streamed to Karoo data field system
6. Simultaneously written to FIT file as developer fields

**Algorithm Implementation:**
- **Interface-based design**: `IWPrimeModel` interface
- **Six concrete implementations**: One per scientific model
- **Factory pattern**: `WPrimeFactory` for model selection
- **State management**: Each model maintains internal W' balance
- **Performance optimized**: Minimal CPU usage per update

### Key Features

✅ **Multiple Display Formats**
- Percentage (0-100%) for intuitive understanding
- Absolute Joules for scientific analysis
- Both available simultaneously on data pages

✅ **FIT File Integration**
- Two custom developer fields per activity:
  - `WPrimeJ`: Raw Joules value
  - `WPrimePct`: Percentage value
- Written to both Record and Session messages
- Compatible with WKO5, Golden Cheetah, intervals.icu
- Optional (can be disabled in settings)

✅ **Algorithm Flexibility**
- Six scientifically validated models
- Runtime switching (no restart required)
- Each model with specific use case optimization
- Shared interface for consistent behavior

✅ **Robust Configuration**
- Persistent storage via DataStore
- Real-time updates without ride interruption
- Validation and bounds checking
- Default values for new installations

### Building from Source

**Requirements:**
- Android Studio Hedgehog or later
- JDK 17+
- Gradle 8.x
- karoo-ext library (included)

**Build Commands:**
```bash
# Clean build
./gradlew clean assembleDebug

# Install to connected device
./gradlew installDebug

# Run tests
./gradlew test
```

**Output:**
- APK: `app/build/outputs/apk/debug/WPrimeExtension-v1.0-debug.apk`

### Testing

**Unit Tests:**
```bash
./gradlew test
```

**On-Device Testing:**
1. Install debug APK
2. Configure test values (CP=200W, W'=10000J)
3. Start indoor ride with ERG intervals:
   - 5min @ 150W (below CP, should recover)
   - 2min @ 300W (above CP, should deplete)
   - 5min @ 150W (recovery)
4. Observe W' balance behavior

**Validation:**
- At 150W (50W below CP), W' should gradually recover
- At 300W (100W above CP), W' should deplete ~100J/sec
- Full recovery typically takes 5-10 minutes

### Dependencies

**Core:**
- `karoo-ext:1.x` - Official Hammerhead extension framework
- `androidx.compose.ui` - Modern UI toolkit
- `androidx.datastore` - Persistent storage
- `hilt-android` - Dependency injection

**Development:**
- `timber` - Structured logging
- `kotlin-reflect` - Runtime reflection for effects
- `kotlinx-coroutines` - Async operations


## Contributing

This is an open-source project. Contributions welcome!

### Areas for Contribution
- Additional W Prime algorithms
- UI/UX improvements
- Translation to other languages
- Testing on real-world rides
- Documentation improvements

### How to Contribute
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-algorithm`)
3. Make your changes
4. Test on Karoo hardware
5. Submit a pull request

### Coding Standards
- Follow Kotlin style guide
- Use existing architecture patterns (MVVM, DI)
- Add unit tests for new algorithms
- Document algorithm sources (scientific papers)
- Follow karoo-ext best practices


## Scientific References

The algorithms implemented are based on peer-reviewed research:

1. **Skiba et al. (2012)**: "Modeling the Expenditure and Reconstitution of Work Capacity above Critical Power"
2. **Skiba et al. (2014)**: "Simplified differential equation model of W′ balance"
3. **Bartram et al. (2018)**: "Accuracy of W′ recovery kinetics in high-performance cyclists"
4. **Caen & Lievens**: Domain-specific recovery constants
5. **Chorley et al. (2023)**: "Bi-exponential recovery model for W′"
6. **Weigend et al. (2022)**: "Hydraulic model of anaerobic energy"

See `docs/wprime-algorithms.md` for detailed formulas and implementation notes.


## Support & Community

### Getting Help
- **Issues**: [GitHub Issues](https://github.com/apopovsky/WPrimeExtension/issues)
- **Discussions**: [GitHub Discussions](https://github.com/apopovsky/WPrimeExtension/discussions)
- **Karoo Community**: [r/Karoo on Reddit](https://reddit.com/r/Karoo)
- **Hammerhead Forum**: [Extensions Developers](https://support.hammerhead.io/hc/en-us/community/topics/31298804001435-Hammerhead-Extensions-Developers)

### Reporting Bugs
Please include:
- Karoo firmware version
- Extension version
- CP and W' values configured
- Algorithm selected
- Description of unexpected behavior
- Logcat output if possible: `adb logcat | grep WPrime`


## Roadmap

### Completed ✅
- [x] Core W Prime calculation (6 algorithms)
- [x] Real-time data field integration
- [x] Dual display formats (% and kJ)
- [x] FIT file recording
- [x] Persistent configuration
- [x] Jetpack Compose UI
- [x] Algorithm selection

### Planned Features
- [ ] Visual gauge/graph on data field
- [ ] Low W' alerts during ride
- [ ] Post-ride W' analysis chart
- [ ] Integration with training platforms
- [ ] Automatic CP/W' estimation from ride data
- [ ] Multi-athlete profiles


## License

```
Copyright (c) 2024-2025 Ariel Popovsky

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```


## Acknowledgments

- **Hammerhead Navigation** for the excellent karoo-ext framework
- **Dr. Philip Skiba** for pioneering W Prime balance modeling
- The **cycling science community** for continued research
- **Beta testers** from r/Karoo and Hammerhead forums


## Disclaimer

This extension is provided as-is for educational and training purposes. Always ride safely and use your own judgment. W Prime calculations are estimates based on models and may not perfectly reflect your individual physiology. Consult with a coach or sports scientist for personalized training advice.

---

**Made with ❤️ by cyclists, for cyclists.**

For questions, feature requests, or bug reports, please [open an issue](https://github.com/apopovsky/WPrimeExtension/issues).

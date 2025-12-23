# Contributing to W Prime Extension

Thank you for your interest in contributing to the W Prime Extension for Hammerhead Karoo 3! This document provides guidelines and instructions for contributors.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Coding Standards](#coding-standards)
- [Adding New Algorithms](#adding-new-algorithms)

## Code of Conduct

This project follows a code of conduct. By participating, you are expected to:
- Be respectful and inclusive
- Accept constructive criticism gracefully
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/WPrimeExtension.git
   cd WPrimeExtension
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/WPrimeExtension.git
   ```

## Development Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Hammerhead Karoo 3 device (for testing)
- ADB for device communication

### Configure GitHub Package Registry Access

The project uses the `karoo-ext` library from GitHub Packages. You need to configure authentication:

1. Create a GitHub Personal Access Token with `read:packages` scope
2. Add to `~/.gradle/gradle.properties`:
   ```properties
   gpr.user=YOUR_GITHUB_USERNAME
   gpr.key=YOUR_GITHUB_TOKEN
   ```

### Build the Project

```bash
./gradlew clean build
```

### Install on Karoo

```bash
./gradlew installDebug
# Or manually:
adb install app/build/outputs/apk/debug/WPrimeExtension-v1.0-debug.apk
```

## Making Changes

### Branching Strategy

- `main` - Production-ready code
- `develop` - Development branch
- Feature branches: `feature/your-feature-name`
- Bug fixes: `bugfix/issue-description`
- Hotfixes: `hotfix/critical-fix`

### Creating a Feature Branch

```bash
git checkout develop
git pull upstream develop
git checkout -b feature/your-feature-name
```

### Commit Messages

Follow conventional commit format:

```
type(scope): subject

body

footer
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Example:**
```
feat(calculator): add Chorley 2023 bi-exponential model

Implement the bi-exponential recovery model from Chorley et al. 2023
with fast and slow recovery components.

Closes #42
```

## Testing

### Unit Tests

Run unit tests before submitting:

```bash
./gradlew test
```

### On-Device Testing

1. Install the APK on your Karoo 3
2. Configure test parameters (CP, W', algorithm)
3. Test with both indoor and outdoor rides
4. Verify data appears in FIT files

### Test Checklist

- [ ] Extension appears in app drawer
- [ ] Configuration UI works correctly
- [ ] Settings persist after restart
- [ ] Data field displays during ride
- [ ] W Prime depletes correctly above CP
- [ ] W Prime recovers correctly below CP
- [ ] FIT file contains W Prime data
- [ ] Multiple algorithms tested
- [ ] No crashes or errors in logcat

## Submitting Changes

### Before Submitting

1. **Update from upstream**:
   ```bash
   git checkout develop
   git pull upstream develop
   git checkout your-feature-branch
   git rebase develop
   ```

2. **Run tests**:
   ```bash
   ./gradlew test
   ./gradlew lint
   ```

3. **Build successfully**:
   ```bash
   ./gradlew build
   ```

4. **Update documentation** if needed

### Create Pull Request

1. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

2. Open a Pull Request on GitHub:
   - Target: `develop` branch
   - Fill out the PR template completely
   - Link related issues
   - Add screenshots/videos if applicable

3. Wait for review and address feedback

## Coding Standards

### Kotlin Style

- Follow official [Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html)
- Use `ktlint` for formatting
- Prefer explicit types when it improves readability
- Use meaningful variable names

### Architecture

- **MVVM** pattern for UI components
- **Dependency Injection** with Hilt
- **Repository pattern** for data access
- **Single responsibility** principle

### File Organization

Example of proper Kotlin file structure:

```text
// 1. Package declaration
package com.itl.wprimeext.extension

// 2. Imports (grouped and sorted)
import android.content.Context
import androidx.compose.runtime.Composable
import io.hammerhead.karooext.KarooSystemService

// 3. Constants
private const val TAG = "WPrimeCalculator"
private const val DEFAULT_CP = 250.0

// 4. Class/Interface definition
class WPrimeCalculator(
    private val cp: Double,
    private val wPrime: Double,
) {
    private var wBal: Double = wPrime
    
    fun update(power: Double): Double {
        // Implementation
        return wBal
    }
}
```

### Documentation

- Add KDoc for public APIs:
  ```text
  /**
   * Calculates W Prime balance using the specified algorithm.
   *
   * @param power Current power output in watts
   * @param dt Time delta in seconds
   * @return Updated W Prime balance in joules
   */
  fun update(power: Double, dt: Double): Double {
      // Implementation
      return 0.0
  }
  ```

- Comment complex algorithms with references:
  ```kotlin
  // Skiba et al. 2014 differential model
  // τ = 2287 * D_CP^(-0.688)
  val tau = 2287 * dcp.pow(-0.688)
  ```

## Adding New Algorithms

If you're adding a new W Prime algorithm:

### 1. Research and Validation

- Provide scientific reference (paper, author, year)
- Explain the model's unique characteristics
- Document when it should be used

### 2. Implementation Steps

1. **Create model class** implementing `IWPrimeModel`:
   ```kotlin
   class YourNewModel(
       cp: Double,
       wPrime: Double,
       // Additional parameters
   ) : BaseWPrimeModel(cp, wPrime) {
       override fun update(power: Double, dt: Double): Double {
           // Your implementation
       }
   }
   ```

2. **Add to enum**:
   ```kotlin
   enum class WPrimeModelType {
       // ...existing
       YOUR_NEW_MODEL,
   }
   ```

3. **Update factory**:
   ```kotlin
   object WPrimeFactory {
       fun create(
           type: WPrimeModelType,
           cp: Double,
           wPrime: Double,
           tauOverride: Double?,
           kIn: Double
       ): IWPrimeModel = when (type) {
           // ...existing models...
           WPrimeModelType.YOUR_NEW_MODEL -> YourNewModel(cp, wPrime, additionalParam)
       }
   }
   ```

4. **Update UI** in `ConfigurationScreen.kt`

5. **Add documentation** to `docs/wprime-algorithms.md`

6. **Update README.md** with the new algorithm

### 3. Testing Requirements

- Unit tests for the model
- Comparison with reference implementation (if available)
- Real-world ride data validation
- Edge case handling (negative power, zero CP, etc.)

## Areas for Contribution

We welcome contributions in these areas:

### High Priority
- [ ] Additional W Prime algorithms
- [ ] Visual gauge/graph for data field
- [ ] Low W Prime alerts during ride
- [ ] Post-ride W Prime analysis

### Medium Priority
- [ ] UI/UX improvements
- [ ] Localization/translations
- [ ] Performance optimizations
- [ ] Better documentation

### Future Features
- [ ] Integration with training platforms
- [ ] Automatic CP/W' estimation
- [ ] Multi-athlete profiles
- [ ] Advanced data export

## Questions?

- **Issues**: Open an issue for bugs or feature requests
- **Discussions**: Use GitHub Discussions for questions
- **Community**: Join r/Karoo on Reddit
- **Forum**: Hammerhead Extensions Developers forum

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

---

**Thank you for contributing to the W Prime Extension!**


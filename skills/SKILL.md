---
name: Karoo Extension Development
description: Best practices and guidelines for developing the W Prime Karoo Extension.
---

# Karoo Extension Development

## Project Overview
This project implements a W Prime (W') Extension for Hammerhead Karoo cycling computers.
It uses the `karoo-ext` library.

## Architecture
- **Pattern**: MVVM (Model-View-ViewModel)
- **UI**: Jetpack Compose
- **DI**: Hilt
- **Persistence**: Android DataStore

## Key Components
- **Extension Service**: `WPrimeExtension.kt` (integrates with Karoo system)
- **Data Types**: `WPrimeDataType.kt` (%) and `WPrimeKjDataType.kt` (Joules)
- **Logic**: `WPrimeCalculator.kt` (implements the 6 scientific algorithms)
- **Configuration**: `ConfigurationScreen.kt` (Settings UI)

## Development Workflow
1.  **Build**: `./gradlew clean assembleDebug`
2.  **Install**: `./gradlew installDebug` (requires ADB)
3.  **Test**: `./gradlew test` (Unit tests)
4.  **Logs**: `adb logcat | grep WPrime`

## Coding Standards
- Follow Kotlin coding conventions.
- Use `WPrimeLogger` for logging.
- Ensure new algorithms implement `IWPrimeModel`.

# Module karoo-ext (W Prime Extension)

Karoo Extensions (karoo-ext library) is an [Android library](https://developer.android.com/studio/projects/android-library) for use on [Hammerhead](https://www.hammerhead.io/)
cycling computers.

This project implements a **W Prime (W') Extension** that provides real-time anaerobic capacity tracking during cycling activities.

## W Prime Extension Features

### Core Functionality
- **Real-time W Prime calculation** based on Critical Power model
- **Persistent configuration** for Critical Power (CP), Anaerobic Capacity (W'), and Recovery Time Constant (Tau)
- **Native Karoo integration** as a data field available in ride profiles
- **Power zone visualization** with background color coding
- **Data smoothing** for stable and reliable calculations

### Technical Implementation
- **WPrimeCalculator**: Robust mathematical model with adaptive recovery
- **WPrimeDataType**: Native Karoo data field implementation
- **WPrimeSettings**: Persistent configuration using Android DataStore
- **TestPowerDataSource**: Realistic cycling power simulation for testing

# Package io.hammerhead.karooext

Top-level package for the library providing the foundation for W Prime extension implementation.

# Package io.hammerhead.karooext.extension

Implementation classes for creating the W Prime extension, including:
- **KarooExtension** base class for W Prime extension registration
- **DataTypeImpl** for W Prime data field integration
- **Device** and **DeviceEvent** for power data source simulation

# Package io.hammerhead.karooext.models

Data models which are used to interact with [KarooSystemService](io.hammerhead.karooext.KarooSystemService) or as part of a [KarooExtension](io.hammerhead.karooext.extension.KarooExtension) implementation.

Essential models for W Prime implementation:
- **StreamState** for real-time power data streaming
- **DataPoint** for W Prime percentage values
- **ViewConfig** for custom visualization
- **ShowCustomStreamState** for power zone color display

## Key Components

1. **[KarooEvent]** - System events for ride state monitoring
2. **[KarooEffect]** - Effects for system interaction
3. **[DataType]** - Data type definitions including W Prime field

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Expo Superwall SDK is an official Expo integration for Superwall, providing in-app paywall functionality for React Native applications. The project offers two SDK approaches:
- **Hooks-based SDK** (recommended for new projects): Modern React hooks interface
- **Compat SDK** (`expo-superwall/compat`): Legacy compatibility layer for migrating from React Native Superwall SDK

**Requirements**: Expo SDK 53+ only. For older versions, users should use the legacy React Native SDK.

## Development Commands

### Core Development
```bash
# Build the module
expo-module build

# Clean build artifacts
expo-module clean

# Run linting
expo-module lint

# Run tests
expo-module test

# Format code with Biome
biome check --write .

# Prepare for publishing
expo-module prepare
expo-module prepublishOnly
```

### Native Development
```bash
# Open iOS project in Xcode
xed example/ios

# Open Android project in Android Studio
open -a "Android Studio" example/android

# Run on iOS
expo run:ios

# Run on Android
expo run:android
```

### Versioning & Release
```bash
# Version bump using changesets
changeset version

# Build and publish
yarn build && changeset publish
```

## Architecture Overview

### Multi-Platform Native Bridge
The SDK bridges TypeScript/React Native with native iOS (Swift) and Android (Kotlin) SDKs:

**iOS Bridge Structure:**
- `ios/SuperwallExpoModule.swift`: Main Expo module
- `ios/Bridges/`: Bridge classes for purchase controller and delegate
- `ios/Json/`: JSON serialization extensions for native types

**Android Bridge Structure:**
- `android/src/main/java/expo/modules/superwallexpo/SuperwallExpoModule.kt`: Main module
- `android/src/main/java/expo/modules/superwallexpo/bridges/`: Bridge classes
- `android/src/main/java/expo/modules/superwallexpo/json/`: JSON serialization extensions

### TypeScript Architecture

**Hooks-Based SDK (`src/`):**
- `SuperwallProvider.tsx`: React context provider for SDK initialization
- `useSuperwall.ts`: Core store and SDK access hook
- `useUser.ts`: User management and subscription status
- `usePlacement.ts`: Paywall registration and presentation
- `useSuperwallEvents.ts`: Low-level event subscription

**Compat SDK (`src/compat/`):**
- `index.ts`: Legacy Superwall class API
- `lib/`: Type definitions and classes matching React Native SDK

**Type System:**
- `SuperwallExpoModule.types.ts`: Comprehensive TypeScript definitions
- All types extensively documented with JSDoc
- Bridge serialization ensures type safety across platforms

### State Management
Uses Zustand for lightweight, type-safe state management:
- Centralized store in `useSuperwall.ts`
- Event-driven updates from native modules
- Shallow equality checking for performance

### Native Type Serialization
Critical pattern for extending native types:

**When adding properties to native types:**
1. Update Swift JSON extension in `ios/Json/[Type]+Json.swift`
2. Update Kotlin JSON extension in `android/src/main/java/expo/modules/superwallexpo/json/[Type].kt`
3. Update TypeScript interface in `SuperwallExpoModule.types.ts`
4. Update compat class in `src/compat/lib/[Type].ts` if applicable

**Example Pattern:**
```swift
// iOS: TransactionProduct+Json.swift
extension TransactionProduct {
  func toJson() -> [String: Any] {
    return [
      "id": id,
      "price": price.toJson(),
      // ... other properties
    ]
  }
}
```

```kotlin
// Android: TransactionProduct.kt
fun TransactionProduct.toJson(): Map<String, Any> {
  return mapOf(
    "id" to id,
    "price" to price.toJson(),
    // ... other properties
  )
}
```

### Event System
Comprehensive event bridge between native SDKs and TypeScript:
- Native events → TypeScript via `SuperwallExpoModule.addListener`
- Type-safe event payloads defined in `SuperwallExpoModule.types.ts`
- Automatic cleanup on component unmount

### Dual SDK Pattern
The project supports both modern hooks and legacy class-based APIs:
- Hooks SDK: `import { usePlacement } from "expo-superwall"`
- Compat SDK: `import Superwall from "expo-superwall/compat"`

## Key Files to Understand

- `src/SuperwallExpoModule.types.ts`: Single source of truth for all TypeScript types
- `src/useSuperwall.ts`: Core store and SDK operations
- `src/compat/index.ts`: Legacy API implementation
- `ios/SuperwallExpoModule.swift` & `android/.../SuperwallExpoModule.kt`: Native module entry points

## Native Dependencies

**iOS:** SuperwallKit (CocoaPods)
**Android:** Superwall SDK (Gradle)

Both are automatically managed through the respective native package managers.

## Testing Strategy

The SDK relies heavily on integration testing through the example app rather than unit tests, due to the native bridge dependencies and paywall presentation logic.
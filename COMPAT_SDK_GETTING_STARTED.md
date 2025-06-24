# Getting Started with Expo Superwall Compat SDK

This guide will help you integrate the Expo Superwall Compat SDK into your React Native application. This SDK is intended for projects migrating from the legacy `react-native-superwall` SDK.

**Warning**: This SDK only supports Expo SDK Version >= 53. If you'd like to use older versions, please use our [legacy react-native sdk](https://github.com/superwall/react-native-superwall).

## Installation

First, you need to install the `expo-superwall` package. You can do this using npm or yarn:

```bash
npx expo install expo-superwall
# or
bunx expo install expo-superwall
```

## Basic Setup

```tsx
import Superwall from "expo-superwall/compat"
import { useEffect, useState } from "react"
import { Platform } from "react-native"

// Initialize Superwall
useEffect(()=> {
	const apiKey = Platform.OS === "ios"
        ? "yourSuperwall_iOSKey"
        : "yourSuperwall_androidKey";
	Superwall.configure({ // `await` is optional here if not chaining or needing immediate confirmation
		apiKey,
	});
})
```

## Identify User

```tsx
// Identify a user
await Superwall.shared.identify({ userId })

// Set User Attributes
await Superwall.shared.setUserAttributes({
        someCustomVal: "abc",
        platform: Platform.OS,
        timestamp: new Date().toISOString(),
})
```

## Present a Paywall

```tsx
// Present a paywall
Superwall.shared.register({
      placement: "yourPlacementName",
      feature() {
        console.log(`Feature called!`)
      },
})
```

## Listen to Events

```tsx
// 1. Define your superwall Delegat
import {
  EventType,
  type PaywallInfo,
  type RedemptionResult,
  type SubscriptionStatus,
  SuperwallDelegate,
  type SuperwallEventInfo,
} from "expo-superwall/compat"

export class MyDelegate extends SuperwallDelegate {
  handleSuperwallEvent(eventInfo) {
    switch (eventInfo.event.type) {
      case EventType.paywallOpen:
        console.log("Paywall opened");
        break;
      case EventType.paywallClose:
        console.log("Paywall closed");
        break;
    }
  }
}

// 2. Simply set the delegate
const delegate = new MyDelegate()
await Superwall.shared.setDelegate(delegate)

```

## Resources

- 📖 [Full Documentation](https://superwall.com/docs/home)
- 🎮 [Example App](./example)
- 💬 [Discord Community](https://discord.gg/superwall)
- 📧 [Support](mailto:jake@superwall.com)
- 🐛 [Report Issues](https://github.com/superwall/superwall-expo/issues)

<p align="center">
  <br />
  <img src="https://sdmntprpolandcentral.oaiusercontent.com/files/00000000-2d34-620a-9ff1-08fd49c7d10c/raw?se=2025-06-04T09%3A55%3A26Z&sp=r&sv=2024-08-04&sr=b&scid=d726d727-00ef-592a-a050-441e2a1bb43c&skoid=b928fb90-500a-412f-a661-1ece57a7c318&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2025-06-04T05%3A39%3A10Z&ske=2025-06-05T05%3A39%3A10Z&sks=b&skv=2024-08-04&sig=XEiCPtCJd/tW%2BwYghFuJHCuB9ZkuetUmfEcvnfGZY9g%3D" alt="logo" height="100px" />
  <h3 style="font-size:26" align="center">In-App Paywalls Made Easy 💸</h3>
  <br />
</p>

[Superwall](https://superwall.com/) lets you remotely configure every aspect of your paywall — helping you find winners quickly.

## Superwall

**Superwall** is an open source framework that provides a wrapper for presenting and creating paywalls. It interacts with the Superwall backend letting you easily iterate paywalls on the fly in `React Native`!

## Features
|   | Superwall |
| --- | --- |
✅ | Server-side paywall iteration
🎯 | Paywall conversion rate tracking - know whether a user converted after seeing a paywall
🆓 | Trial start rate tracking - know and measure your trial start rate out of the box
📊 | Analytics - automatic calculation of metrics like conversion and views
✏️ | A/B Testing - automatically calculate metrics for different paywalls
📝 | [Online documentation](https://superwall.com/docs/home) up to date
🔀 | [Integrations](https://superwall.com/docs/home) - over a dozen integrations to easily send conversion data where you need it
💯 | Well maintained - [frequent releases](https://superwall.com/docs/home)
📮 | Great support - email a founder: jake@superwall.com


## Getting Started

[Sign up for a free account on Superwall](https://superwall.com/sign-up) and [read our docs](https://superwall.com/docs/home).


## Installation

```bash
npm install superwall-expo
# or
bun add superwall-expo
```

### Basic Setup

```tsx
import Superwall from "expo-superwall/compat"
import { useEffect, useState } from "react"
import { Platform } from "react-native"

// Initialize Superwall
useEffect(()=> {
	const apiKey = Platform.OS === "ios"
        ? "yourSuperwall_iOSKey"
        : "yourSuperwall_androidKey"
	await Superwall.configure({
		apiKey,
	})
})
```

### Identify User

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


### Present a Paywall

```tsx
// Present a paywall
Superwall.shared.register({
      "yourPlacementName",
      feature() {
        console.log(`Feature called!`)
      },
})
```


### Listen to Events

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
const delegate = new MySuperwallDelegate()
await Superwall.shared.setDelegate(delegate)

```


## Resources

- 📖 [Full Documentation](https://superwall.com/docs/home)
- 🎮 [Example App](./example)
- 💬 [Discord Community](https://discord.gg/superwall)
- 📧 [Support](mailto:jake@superwall.com)
- 🐛 [Report Issues](https://github.com/superwall/superwall-expo/issues)


---

## Contributing

Please see the [CONTRIBUTING](.github/CONTRIBUTING.md) file for how to help.
<p align="center">
  <br />
  <img src=https://user-images.githubusercontent.com/3296904/158817914-144c66d0-572d-43a4-9d47-d7d0b711c6d7.png alt="logo" height="100px" />
  <h3 style="font-size:26" align="center">In-App Paywalls Made Easy 💸</h3>
  <br />
</p>

> [!WARNING] 
> **Important: Expo SDK 53+ Required**
>
> This SDK is exclusively compatible with Expo SDK version 53 and newer.
> For projects using older Expo versions, please use our [legacy React Native SDK](https://github.com/superwall/react-native-superwall).

## Choosing the Right SDK

This repository contains two SDKs for integrating Superwall with your Expo app:

*   **For new projects:** We recommend using our new **Hooks-based SDK**. See the guide below.
*   **For migrating from the Superwall React Native SDK:** Get setup in minutes with the [legacy/compat SDK (`expo-superwall/compat`) guide](./COMPAT_SDK_GETTING_STARTED.md).
*   **For older Expo SDKs:** Please refer to our [legacy React Native SDK](https://github.com/superwall/react-native-superwall).

---

# Getting Started with Expo Superwall SDK

This guide will help you integrate the Expo Superwall Hooks SDK into your React Native application, allowing you to easily manage users and display paywalls.

**Warning**: This SDK only supports Expo SDK Version >= 53. If you'd like to use older versions, please use our [legacy react-native sdk](https://github.com/superwall/react-native-superwall).

## Installation

First, you need to install the `expo-superwall` package. You can do this using npm or yarn:

```bash
npx expo install expo-superwall
# or
bunx expo install expo-superwall
```

## Setup

To use the Superwall SDK, you need to wrap your application (or the relevant part of it) with the `<SuperwallProvider />`. This provider initializes the SDK with your API key.

```tsx
import { SuperwallProvider } from "expo-superwall";

// Replace with your actual Superwall API key
export default function App() {
  return (
    <SuperwallProvider apiKeys={{ ios: "YOUR_SUPERWALL_API_KEY" /* android: API_KEY */ }}>
      {/* Your app content goes here */}
    </SuperwallProvider>
  );
}
```

**Note:** You can find your API key in your Superwall dashboard.

## Basic Usage

The SDK provides hooks to interact with Superwall's features.

### Handling Loading States

While the SDK is initializing, you can display a loading indicator. The `<SuperwallLoading />` and `<SuperwallLoaded />` components help manage this.

```tsx
import {
  SuperwallProvider,
  SuperwallLoading,
  SuperwallLoaded,
} from "expo-superwall";
import { ActivityIndicator, View, Text } from "react-native";

const API_KEY = "YOUR_SUPERWALL_API_KEY";

export default function App() {
  return (
    <SuperwallProvider apiKeys={{ ios: API_KEY }}>
      <SuperwallLoading>
        <ActivityIndicator style={{ flex: 1 }} />
      </SuperwallLoading>
      <SuperwallLoaded>
        {/* Your main app screen or component */}
        <MainAppScreen />
      </SuperwallLoaded>
    </SuperwallProvider>
  );
}

function MainAppScreen() {
  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
      <Text>Superwall SDK is ready!</Text>
      {/* Rest of your app's UI */}
    </View>
  );
}
```

### Managing Users with `useUser`

The `useUser` hook provides functions to identify users, sign them out, update their attributes, and access user and subscription status information.

```tsx
import { useUser } from "expo-superwall";
import { Button, Text, View } from "react-native";

function UserManagementScreen() {
  const { identify, user, signOut, update, subscriptionStatus } = useUser();

  const handleLogin = async () => {
    // Identify the user with a unique ID
    await identify(`user_${Date.now()}`);
  };

  const handleSignOut = async () => {
    await signOut();
  };

  const handleUpdateUserAttributes = async () => {
    // Update custom user attributes
    await update((oldAttributes) => ({
      ...oldAttributes,
      customProperty: "new_value",
      counter: (oldAttributes.counter || 0) + 1,
    }));
  };

  return (
    <View style={{ padding: 20 }}>
      <Text>Subscription Status: {subscriptionStatus?.status ?? "unknown"}</Text>
      {user && <Text>User ID: {user.appUserId}</Text>}
      {user && <Text>User Attributes: {JSON.stringify(user, null, 2)}</Text>}

      <Button title="Login" onPress={handleLogin} />
      <Button title="Sign Out" onPress={handleSignOut} />
      <Button title="Update Attributes" onPress={handleUpdateUserAttributes} />
    </View>
  );
}
```

Key functions from `useUser`:
- `identify(userId)`: Identifies the current user with Superwall. This is typically called when a user logs in.
- `signOut()`: Signs the current user out. Call this when a user logs out.
- `update(attributesUpdater)`: Updates the user's attributes. You can provide a function that receives the old attributes and returns the new ones.
- `user`: An object containing the user's `appUserId` and other attributes.
- `subscriptionStatus`: An object indicating the user's subscription status (e.g., `active`, `inactive`).

### Triggering Paywalls with `usePlacement`

The `usePlacement` hook allows you to register and trigger paywalls (placements) that you've configured in your Superwall dashboard.

```tsx
import { usePlacement, useUser } from "expo-superwall";
import { Alert, Button, Text, View } from "react-native";

function PaywallScreen() {
  const { registerPlacement, state: placementState } = usePlacement({
    onError: (err) => console.error("Placement Error:", err),
    onPresent: (info) => console.log("Paywall Presented:", info),
    onDismiss: (info, result) =>
      console.log("Paywall Dismissed:", info, "Result:", result),
  });

  const handleTriggerPlacement = async () => {
    await registerPlacement({
      placement: "your_placement_id", // Replace with your actual placement ID
      feature() {
        // This function is called if the user is already subscribed
        // or successfully subscribes through the paywall.
        console.log("Feature unlocked!");
        Alert.alert("Feature Unlocked!", "You can now access this feature.");
      },
    });
  };

  return (
    <View style={{ padding: 20 }}>
      <Button title="Show Paywall for 'your_placement_id'" onPress={handleTriggerPlacement} />
      {placementState && (
        <Text>Last Paywall Result: {JSON.stringify(placementState)}</Text>
      )}
    </View>
  );
}
```

Key aspects of `usePlacement`:
- `registerPlacement(options)`: Registers and potentially presents a paywall.
    - `options.placement`: The ID of the placement you want to show (e.g., "fishing" from the example, or "onboarding_paywall").
    - `options.feature()`: A callback function that is executed if the user has access to the feature (either by already being subscribed or by purchasing through the presented paywall).
- Callbacks (passed as options to `usePlacement`):
    - `onError`: Handles errors during paywall presentation.
    - `onPresent`: Called when a paywall is presented.
    - `onDismiss`: Called when a paywall is dismissed.
- `state`: An object returned by the hook, containing information about the last paywall presentation attempt (e.g., `presented`, `purchased`, `cancelled`, `skipped`).

This covers the basic setup and usage of the `expo-superwall` Hooks SDK. For more advanced scenarios and detailed API information, refer to the official Superwall documentation.

---

## Hooks API Reference

This section provides a detailed reference for each hook available in the `expo-superwall` SDK.

### `useSuperwall`

**Purpose:**
The `useSuperwall` hook is the core hook that provides access to the Superwall store and underlying SDK functionality. It's generally used internally by other more specific hooks like `useUser` and `usePlacement`, but can be used directly for advanced scenarios. It ensures that native event listeners are set up on first use.

**Returned Values (Store State and Actions):**

The hook returns an object representing the Superwall store. If a `selector` function is provided, it returns the selected slice of the store.

-   **State:**
    -   `isConfigured: boolean`: True if the Superwall SDK has been configured with an API key.
    -   `isLoading: boolean`: True when the SDK is performing an asynchronous operation like configuration.
    -   `listenersInitialized: boolean`: True if native event listeners have been initialized.
    -   `user?: UserAttributes | null`: An object containing the current user's attributes.
        -   `UserAttributes`:
            -   `aliasId: string`: The alias ID of the user.
            -   `appUserId: string`: The application-specific user ID.
            -   `applicationInstalledAt: string`: ISO date string of when the application was installed.
            -   `seed: number`: A seed value for the user.
            -   `[key: string]: any`: Other custom user attributes.
    -   `subscriptionStatus?: SubscriptionStatus`: The current subscription status of the user.
        -   `SubscriptionStatus` (see `SuperwallExpoModule.types.ts` for full details):
            -   `status: "UNKNOWN" | "INACTIVE" | "ACTIVE"`
            -   `entitlements?: Entitlement[]` (if status is "ACTIVE")
                -   `Entitlement`: `{ id: string, type: EntitlementType }`

-   **Actions (Functions):**
    -   `configure: (apiKey: string, options?: Record<string, any>) => Promise<void>`: Initializes the Superwall SDK with the provided API key and optional configuration options.
    -   `identify: (userId: string, options?: IdentifyOptions) => Promise<void>`: Identifies the user with the given `userId`.
        -   `IdentifyOptions`:
            -   `restorePaywallAssignments?: boolean`: If true, restores paywall assignments from a previous session.
    -   `reset: () => Promise<void>`: Resets the user's identity and clears any stored user-specific data. This is equivalent to logging out the user.
    -   `registerPlacement: (placement: string, params?: Record<string, any>, handlerId?: string | null) => Promise<void>`: Registers a placement. This may or may not present a paywall depending on campaign rules. `handlerId` is used internally by `usePlacement` to associate events.
    -   `getPresentationResult: (placement: string, params?: Record<string, any>) => Promise<any>`: Gets the presentation result for a given placement.
    -   `dismiss: () => Promise<void>`: Dismisses any currently presented paywall.
    -   `preloadAllPaywalls: () => Promise<void>`: Preloads all paywalls.
    -   `preloadPaywalls: (placements: string[]) => Promise<void>`: Preloads paywalls for the specified placement IDs.
    -   `setUserAttributes: (attrs: Record<string, any>) => Promise<void>`: Sets custom attributes for the current user.
    -   `getUserAttributes: () => Promise<Record<string, any>>`: Retrieves the attributes of the current user.
    -   `setLogLevel: (level: string) => Promise<void>`: Sets the log level for the SDK. `level` can be one of: `"debug"`, `"info"`, `"warn"`, `"error"`, `"none"`.

**Selector (Optional Parameter):**

-   `selector?: (state: SuperwallStore) => T`: A function that receives the entire `SuperwallStore` state and returns a selected part of it. Useful for performance optimization by only re-rendering components when the selected state changes. Uses `zustand/shallow` for shallow equality checking.

**Example (Direct Usage - Advanced):**

```tsx
import { useSuperwall } from 'expo-superwall';

function MyAdvancedComponent() {
  const { isConfigured, configure, setUserAttributes } = useSuperwall();

  if (!isConfigured) {
    return <Text>SDK not configured yet.</Text>;
  }

  const handleSetCustomAttribute = () => {
    setUserAttributes({ myCustomFlag: true });
  };

  return <Button title="Set Custom Flag" onPress={handleSetCustomAttribute} />;
}
```

### `useUser`

**Purpose:**
The `useUser` hook provides a convenient way to manage user identity and attributes, and access user-specific information like subscription status.

**Returned Values:**

An object containing:

-   `identify: (userId: string, options?: IdentifyOptions) => Promise<void>`: Identifies the user with Superwall.
    -   `userId: string`: The unique identifier for the user.
    -   `options?: IdentifyOptions`: Optional parameters for identification.
        -   `restorePaywallAssignments?: boolean`: If true, attempts to restore paywall assignments for this user.
-   `update: (attributes: Record<string, any> | ((old: Record<string, any>) => Record<string, any>)) => Promise<void>`: Updates the current user's attributes.
    -   `attributes`: Either an object of attributes to set/update, or a function that takes the old attributes and returns the new attributes.
-   `signOut: () => void`: Resets the user's identity, effectively signing them out from Superwall's perspective.
-   `refresh: () => Promise<Record<string, any>>`: Manually refreshes the user's attributes and subscription status from the Superwall servers. Returns the refreshed user attributes.
-   `subscriptionStatus?: SubscriptionStatus`: The current subscription status of the user.
    -   `SubscriptionStatus`: (As defined in `useSuperwall` and `SuperwallExpoModule.types.ts`)
        -   `status: "UNKNOWN" | "INACTIVE" | "ACTIVE"`
        -   `entitlements?: Entitlement[]` (if status is "ACTIVE")
-   `user?: UserAttributes | null`: An object containing the current user's attributes (e.g., `appUserId`, `aliasId`, custom attributes).
    -   `UserAttributes`: (As defined in `useSuperwall`)

**Example:**
(Covered in the Basic Usage section)

### `usePlacement`

**Purpose:**
The `usePlacement` hook is designed to handle the presentation of paywalls based on placements configured in your Superwall dashboard. It manages the state of paywall presentation and provides callbacks for various events.

**Parameters:**

-   `callbacks?: usePlacementCallbacks`: An optional object containing callback functions for different paywall events.
    -   `usePlacementCallbacks`:
        -   `onPresent?: (paywallInfo: PaywallInfo) => void`: Called when a paywall is presented.
            -   `paywallInfo: PaywallInfo`: Detailed information about the presented paywall (see `SuperwallExpoModule.types.ts`).
        -   `onDismiss?: (paywallInfo: PaywallInfo, result: PaywallResult) => void`: Called when a paywall is dismissed.
            -   `paywallInfo: PaywallInfo`: Information about the dismissed paywall.
            -   `result: PaywallResult`: The result of the paywall interaction (e.g., `purchased`, `declined`, `restored`). See `SuperwallExpoModule.types.ts`.
        -   `onSkip?: (reason: PaywallSkippedReason) => void`: Called when a paywall is skipped (e.g., due to holdout group, no audience match).
            -   `reason: PaywallSkippedReason`: The reason why the paywall was skipped. See `SuperwallExpoModule.types.ts`.
        -   `onError?: (error: string) => void`: Called when an error occurs during paywall presentation or other SDK operations related to this placement.
            -   `error: string`: The error message.

**Returned Values:**

An object containing:

-   `registerPlacement: (args: RegisterPlacementArgs) => Promise<void>`: A function to register a placement and potentially trigger a paywall.
    -   `RegisterPlacementArgs`:
        -   `placement: string`: The placement name as defined on the Superwall dashboard.
        -   `params?: Record<string, any>`: Optional parameters to pass to the placement.
        -   `feature?: () => void`: An optional function to execute if the placement does *not* result in a paywall presentation (i.e., the user is allowed through, or is already subscribed).
-   `state: PaywallState`: An object representing the current state of the paywall associated with this hook instance.
    -   `PaywallState`:
        -   `{ status: "idle" }`
        -   `{ status: "presented"; paywallInfo: PaywallInfo }`
        -   `{ status: "dismissed"; result: PaywallResult }`
        -   `{ status: "skipped"; reason: PaywallSkippedReason }`
        -   `{ status: "error"; error: string }`

**Example:**
(Covered in the Basic Usage section)

### `useSuperwallEvents`

**Purpose:**
The `useSuperwallEvents` hook provides a low-level way to subscribe to *any* native Superwall event. This is useful for advanced use cases or for events not covered by the more specific hooks. Listeners are automatically cleaned up when the component using this hook unmounts.

**Parameters:**

-   `callbacks?: SuperwallEventCallbacks`: An object where keys are event names and values are the corresponding callback functions.
    -   `SuperwallEventCallbacks`: (Refer to `src/useSuperwallEvents.ts` and `src/SuperwallExpoModule.types.ts` for a full list of subscribable events and their payload types. Common events include):
        -   `onPaywallPresent?: (info: PaywallInfo) => void`
        -   `onPaywallDismiss?: (info: PaywallInfo, result: PaywallResult) => void`
        -   `onPaywallSkip?: (reason: PaywallSkippedReason) => void`
        -   `onPaywallError?: (error: string) => void`
        -   `onSubscriptionStatusChange?: (status: SubscriptionStatus) => void`
        -   `onSuperwallEvent?: (eventInfo: SuperwallEventInfo) => void`: For generic Superwall events.
            -   `SuperwallEventInfo`: `{ event: SuperwallEvent, params: Record<string, any> }`
        -   `onCustomPaywallAction?: (name: string) => void`: When a custom action is triggered from a paywall.
        -   `willDismissPaywall?: (info: PaywallInfo) => void`
        -   `willPresentPaywall?: (info: PaywallInfo) => void`
        -   `didDismissPaywall?: (info: PaywallInfo) => void`
        -   `didPresentPaywall?: (info: PaywallInfo) => void`
        -   `onPaywallWillOpenURL?: (url: string) => void`
        -   `onPaywallWillOpenDeepLink?: (url: string) => void`
        -   `onLog?: (params: { level: LogLevel, scope: LogScope, message: string | null, info: Record<string, any> | null, error: string | null }) => void`
        -   `handlerId?: string`: (Optional) If provided, some events like `onPaywallPresent`, `onPaywallDismiss`, `onPaywallSkip` will only be triggered if the event originated from a `registerPlacement` call associated with the same `handlerId`. This is used internally by `usePlacement`.

**Returned Values:**

This hook does not return any values (`void`). Its purpose is to set up and tear down event listeners.

**Example:**

```tsx
import { useSuperwallEvents } from 'expo-superwall';

function EventLogger() {
  useSuperwallEvents({
    onSuperwallEvent: (eventInfo) => {
      console.log('Superwall Event:', eventInfo.event.event, eventInfo.params);
    },
    onSubscriptionStatusChange: (newStatus) => {
      console.log('Subscription Status Changed:', newStatus.status);
    },
    onPaywallPresent: (info) => {
      console.log('Paywall Presented (via useSuperwallEvents):', info.name);
    }
  });

}
```

For detailed type information on `PaywallInfo`, `PaywallResult`, `PaywallSkippedReason`, `SubscriptionStatus`, `SuperwallEventInfo`, and other types, please refer to the `SuperwallExpoModule.types.ts` file in the `expo-superwall` package.

## Resources

- 📖 [Full Documentation](https://superwall.com/docs/home)
- 🎮 [Example App](./example)
- 💬 [Discord Community](https://discord.gg/superwall)
- 📧 [Support](mailto:jake@superwall.com)
- 🐛 [Report Issues](https://github.com/superwall/superwall-expo/issues)


---

## Contributing

Please see the [CONTRIBUTING](.github/CONTRIBUTING.md) file for how to help.

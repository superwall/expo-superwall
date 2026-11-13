---
"expo-superwall": minor
---

Adds the remaining native SDK API, and an option to build the iOS SDK from Swift Package Manager.

**iOS: SuperwallKit from Swift Package Manager.** Add `"expo-superwall"` to the `plugins` in your app config and prebuild again to build against SuperwallKit 4.19.1 from Swift Package Manager instead of 4.18.3 from CocoaPods; 4.19 and later aren't published to CocoaPods. The plugin switches this module's podspec to the Swift package and links the package into your app target. It needs React Native 0.75 or later and Xcode 26. Without the plugin nothing changes. See the README for apps that don't use prebuild.

- Adds `refreshCustomerInfo()`, which fetches the user's latest entitlements from Superwall. It needs the Swift package; with CocoaPods, and on Android, it returns the customer info the SDK already has.
- iOS: products include `customTransactionId` when built with the Swift package.

**New methods**, on the hooks store and on the compat `Superwall.shared`:

- `setIntegrationAttribute(attribute, value)` sets one integration attribute and leaves the others.
- `purchase()` also takes a product returned by `products()`, or a compat `StoreProduct`. On Android it's bought with its own base plan and offer. Compat `StoreProduct` gains `fullIdentifier`.
- iOS only: `getGrantedEntitlements()` and `setGrantedEntitlements()`, for entitlements your own backend grants, and `removeAllPendingNonSuperwallNotificationRequests()`, `removeAllDeliveredSuperwallNotifications()` and `removeAllDeliveredNonSuperwallNotifications()`. They do nothing on Android.
- Android only: `showAlert()`, `openRestoreOnWeb()`, `getExternalAccountId()`, `dismissForNextPaywall()`, `teardown()` and `observe()`, which reports purchases made outside of Superwall in observer mode. They do nothing on iOS.

**Configuration status changes.** The hooks store has a `configurationStatus` state and `useSuperwallEvents` an `onConfigurationStatusChange` callback. Compat has `Superwall.shared.addConfigurationStatusListener()`.

**Embedded views.**

- `PaywallView` gains `paywallOverrides` (products by name), `feature`, and on iOS `onCustomCallback` and `onLoadingStateChange`.
- Adds `CustomerCenterView`, which embeds the Customer Center on iOS. On Android it renders its fallback, as the Android SDK can only present the Customer Center.
- Both are now exported from `expo-superwall/compat` too.

**Options.**

- `networkEnvironment` accepts `"local"` (iOS) and a custom environment, such as `{ custom: "example.com" }`.
- iOS: `devServer` connects to a running `superwall dev` server.
- Android: `paywalls.preloadDeviceOverrides` and `paywalls.loadingColor`, the name of a color resource.
- A redeemed web checkout's result includes `stripeSubscriptionIds` for Stripe purchases.

**Behaviour changes.**

- iOS: an option that's missing or has an unexpected value now keeps its native default. Previously the native module discarded every option if a single required one was missing, which only affected apps calling `SuperwallExpoModule.configure` directly.
- Android: `getCustomerInfo()` no longer waits forever when the SDK can't load the customer info. After 5 seconds it resolves with what the SDK has.
- Compat: the static `Superwall.purchase()` and `Superwall.products()` now wait for `configure` to finish, like the other methods, instead of failing when called early.

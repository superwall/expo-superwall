---
"expo-superwall": minor
---

Updates the native iOS SDK to 4.19.1, now installed from Swift Package Manager, and adds the remaining native SDK API. [View iOS SDK release notes](https://github.com/superwall/Superwall-iOS/releases/tag/4.19.1).

**Breaking install change on iOS: SuperwallKit now comes from Swift Package Manager.** SuperwallKit is no longer published to CocoaPods, so this package no longer depends on the `SuperwallKit` pod. `pod install` adds the Swift package to the Pods project instead. You don't need a config plugin, Podfile changes or anything in Xcode, but:

- You need Xcode 26 or later and React Native 0.75 or later (any Expo SDK this package supports, 53 and up). `pod install` fails with a message saying so on an older React Native.
- Run `pod install` again after upgrading, or `npx expo prebuild --clean`.
- Nothing else in the app may install the `SuperwallKit` pod or add the `Superwall-iOS` package to the app target, or the build fails with duplicate symbols.
- React Native warns during `pod install` that the pod uses a Swift package with static linking. That's expected.

With SuperwallKit 4.19.1:

- Adds `refreshCustomerInfo()`, which fetches the user's latest entitlements from Superwall. On Android, where the SDK can't force a refresh, it returns the customer info the SDK already has.
- iOS: products include `customTransactionId`.
- iOS: `getCustomerInfo()` no longer waits forever when the SDK can't load its configuration.

**New methods**, on the hooks store and on the compat `Superwall.shared`:

- `setIntegrationAttribute(attribute, value)` sets one integration attribute and leaves the others.
- `purchase()` also takes a product returned by `products()`, or a compat `StoreProduct`. On Android it's bought with its own base plan and offer. Compat `StoreProduct` gains `fullIdentifier`.
- iOS only: `getGrantedEntitlements()` and `setGrantedEntitlements()`, for entitlements your own backend grants, and `removeAllPendingNonSuperwallNotificationRequests()`, `removeAllDeliveredSuperwallNotifications()` and `removeAllDeliveredNonSuperwallNotifications()`. They do nothing on Android.
- Android only: `showAlert()`, `openRestoreOnWeb()`, `getExternalAccountId()`, `dismissForNextPaywall()`, `teardown()` and `observe()`, which reports purchases made outside of Superwall in observer mode. They do nothing on iOS. After `teardown()`, `SuperwallProvider` leaves the SDK unconfigured until you call `configure` with the API key to use.

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

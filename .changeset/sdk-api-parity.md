---
"expo-superwall": minor
---

Adds the native SDK methods, callbacks and options that were missing from the hooks and compat SDKs.

- Hooks: `useSuperwall()` now has `getAssignments`, `confirmAllAssignments`, `setInterfaceStyle`, `getConfigurationStatus` and `handleDeepLink`, which were only on the compat SDK.
- Adds `refreshConfiguration()`, and the getters `getUserId()`, `getIsLoggedIn()`, `getIsPaywallPresented()`, `getLatestPaywallInfo()`, `getEventTrackingBehavior()`, `getAdConsent()`, `getLocaleIdentifier()` and `getLogLevel()`, on both SDKs.
- Adds `overrideProductsByName`, which replaces products on every paywall by product name, as a paywall option and at runtime with `setOverrideProductsByName()` and `getOverrideProductsByName()`.
- Adds the `shouldShowWebRestorationAlert` and `notificationPermissionsDenied` (iOS only) and `optimisticLoading` and `timeoutAfter` (Android only) paywall options.
- Adds `cancelAllScheduledNotifications()`, which cancels the local notifications Superwall scheduled.
- Android: adds `setPushToken()`, `optOutOfNotifications()`, `optInToNotifications()` and `handleNotificationOpen()` for the push notifications Superwall sends. They do nothing on iOS.
- Android: when a paywall's purchase replaces the user's existing subscription, `onPurchase` now has a `replacement` (`ProductReplacement`, with the replacement `mode` and the old product's ID and purchase token). A compat purchase controller gets it through the new `purchaseFromGooglePlayWithReplacement()`, which by default calls `purchaseFromGooglePlay()`.
- Adds the `userAttributesDidChange` and, on iOS, `handleSuperwallDeepLink` delegate callbacks: `onUserAttributesChange` and `onSuperwallDeepLink` in `useSuperwallEvents`, and methods of the compat `SuperwallDelegate`.
- iOS: adds `onWillDismiss` to `usePlacement` and to the compat `PaywallPresentationHandler`, called when a paywall is about to be dismissed.
- Compat: adds `getDeviceAttributes()` and the `localResources` configure option, which the hooks SDK already had.
- Adds the `firebaseInstallationId` (iOS only) and `meta`, `amplitude`, `mixpanel`, `googleAds`, `googleAppSetId` and `custom` (Android only) integration attributes. A `null` value passed to `setIntegrationAttributes()` now removes the attribute.
- Adds the `analytics`, `grantedEntitlements` (iOS only), `transactions`, `webEntitlements` and `customerCenter`, and `customerInfo`, `jsEvaluator`, `nativePurchaseController` and `deepLinks` (Android only) log scopes. The compat `LogScope` also gains `billing`.
- **Behaviour change:** on Android, `setIntegrationAttributes()` now keeps the attributes you leave out, as on iOS. It used to replace them all.

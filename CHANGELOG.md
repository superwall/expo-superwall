# CHANGELOG

The changelog for `Superwall`. Also see the [releases](https://github.com/superwall/Superwall-Android/releases) on GitHub.

## 2.5.7

## Fixes
- Fix `demandScore` and `demandTier` getting removed from some events
- Fixes paywall navigation resetting after backgrounding
- Removes webview flags which can cause off-screen render issues

## 2.5.6

## Enhancements

- Add support for rerouting back button if enabled in paywall settings
  - Handled by `SuperwallOptions.PaywallOptions.onBackPressed`, which enables you to consume the back press or let the SDK consume it
- Add support for redeeming web entitlements with Paddle

## Fixes
- Fix binary .so file regression to ensure 16kb page size compatibility
- Fix potential issue with paywall not dismissing due to paywall_decline concurrency issue

## 2.5.5

## Enhancements
- Expose `signature` on `StoreTransaction`

## Fixes
- ⚠️ Important - in the recent versions we have added usage of Google AppSetId and AdID to enable automatic attribution with Google's ad networks.
- Due to issues with Google's detection of the usage, they have been removed temporarily and will be added back once the issue is resolved.
- As an alternative, you can set those attribution identifiers using `AttributionProvider.GOOGLE_APP_SET | AttributionProvider.GOOGLE_ADS`

## 2.5.4

## Enhancements
- `PaywallComposable` now reapplies theme on system change and `PaywallView` now exposes an `onThemeChanged()` method
- `DeepLink` event now exposes query params

## 2.5.3

## Enhancements
- Adds ability to specify a custom height and corner radius for the drawer presentation style.
- Adds ability to display a `Popup` presentation style
- Adds ability to request reviews from paywall actions, including:
  - Value `device.reviewRequestCount` that returns total request counts
  - Method `device.reviewRequestsInHour|Day|Week|Year|reviewRequestsSinceInstall` computed methods for granular targeting
- Adds `Superwall.instance.setIntegrationAttributes` method enabling you to set integration identifiers for the users from different platforms (Adjust, Mixpanel, Meta, etc.)
- Adds `Superwall.instance.showAlert` method enabling you to easily show an alert over the current paywall
- Adds `PaywallOptions.timeoutAfter` to easily control timeout of paywalls when not using fallback loading
- Adds `Superwall.instance.setIntegrationAttributes` and `Superwall.instance.integrationAttributes` to set integration identifiers (i.e. Mixpanel, Appsflyer, etc)
- Adds user attributes to `TransactionComplete` and `PaywallOpen` events
- Adds device ID to device attributes

## Fixes

- Fixes memory leak issues in `PaywallBuilder` would keep the paywall view alive in some cases
- Fixes issue where some events would be missing properties

## 2.5.1

## Enhancements
- Improves subscribed user experience in cases with slow configuration 

## 2.5.0

## Enhancements
- Updates Google Play billing library to v8. Unfortunately, Google has broken backwards compatibility with previous versions, so if you're using the standalone library too ensure it is compatible with v8.
- Adds kotlin version to device variables

## Fixes
- Reduces noisy logging when product is missing an offer
- Ensures that getting experimental properties works for consumable products


## 2.4.1

## Fixes
- Google Play Billing integration with newer libraries (such as RC 9.*)
- Reduces noisy logging when product is missing an offer
- Ensures that getting experimental properties works for consumable products

## 2.4.0

## Enhancements
- Increases superscript version to 1.0.2 with improved type and null safety

## 2.3.3

## Fixes
- Ensure properties are always properly serialized

## 2.3.2

## Enhancements
- Adds new properties to count placement occurrences in specific time: `placementsInHours`, `placementsInDay`, `placementsInWeek`, `placementsInMonth`, `placementsSinceInstall`

## Fixes
- Fixes an issue where a redemption could succeed but throw an error
- Fixes an issue with receipt manager when there is no purchases and experimental properties are enabled

## 2.3.1

## Fixes
- Fixes an issue where entitlements would not be reset on time
- Ensures `redeem` is only done on initial config not on config refresh

## Enhancements

- Adds `overrideProductsByName` property to allow globally overriding products on paywalls. This property accepts a map of product names to product identifiers (strings). Local overrides provided via `PaywallOverrides` take precedence over global overrides.
- Adds `ProductOverride` sealed class to provide flexible product override handling with support for both product IDs and `StoreProduct` objects.

## 2.3.0

## Enhancements

- Deprecated `Superwall.instance.handleDeepLink` in favor of static `Superwall.handleDeepLink` to ensure links received before `configure` completion are handled properly
- Adds `externalAcountId`, provided to Google Play billing upon purchase as a SHA256 of the userId or the userId itself if `passIdentifiersToPlayStore` option is provided.
- Adds a `SuperwallOption` named `enableExperimentalDeviceVariables`. When set to true, this enables additional device-level variables: `latestSubscriptionPeriodType`, `latestSubscriptionState`, and `latestSubscriptionWillAutoRenew`. These properties provide information about the most recent Google Play subscription on the device and can be used in audience filters. Note that due to their experimental nature, they are subject to change in future updates.
- Update `com.android.billingclient` to version 7.1.1 to align with Google's latest requirements

## Fixes
- Fixes issues with paywall destruction when activity performs a hot reload (i.e. during update)
- Fixes issue where the feature block would be triggered on non-gated paywalls when the app is minimised

## 2.2.3

## Fixes
- Fix potential issue with device enrichment and attributes synchronisation causing a lock

## 2.2.2

## Enhancements

- Adds `demandScore` and `demandTier` to device attributes using an off-device advanced machine learning model. A user is assigned these based on a variety of factors to determine whether they're more or less likely to convert and can be used within audience filters.
- Adds `deviceTier` to the device attributes using an on-device scoring system to place the device in a tier based on it's capabilities. This can be used in audience filters to display different paywalls based on the user's device capabilities. The value can be one of the following values `ultraLow`, `low`, `mid`, `high`, `ultra_high`, `unknown`. NOTE: This property is still experimental

## Fix

- Fixes potential issue in `web2app` with subscription being overriden by the polling due to extra API calls 

## 2.2.0

## Enhancements
- Updates binaries to work on 16kb page sizes

## 2.1.2

## Fixes
- Fix issue with deep link referrer throwing a DeadObjectException

## 2.1.1

## Enhancements

- Add optimisticLoading paywall option that hides the shimmer when HTML is loaded
- Prevent stopping the paywall handler listening with onDismiss when reason is None
- Improve PaywallBuilder API for non-kotlin and non-coroutine users
- Expose `deviceAttributes()` function to retrieve session device attributes 


## 2.1.0

## Enhancements

- Updates kotlin version to 2.0.21
- Updates `compileSDK` to 35
- Adds web checkout and redemption support
- Adds SuperwallDelegate methods `willRedeemLink` and `didRedeemLink`

## Fixes
- Removes lock while reading cache that could cause ANR on the main thread
- Fixes issue where experiment and variant ID would be missing due to concurrency issues

## 2.1.0-beta.1

## Enhancements

- Updates kotlin version to 2.0.21
- Updates `compileSDK` to 35
- Adds web checkout and redemption support
- Adds SuperwallDelegate methods `willRedeemLink` and `didRedeemLink`

## 2.0.8

## Fixes
- Fixes the serialization issue with Kotlin 2.0 

## 2.0.7

## Enhancements
- Improves how errors are handled when loading, improving the UX and reloading in real failure cases
- Added `device.subscriptionStatus` to the device object 

## Fixes
- Fixes an issue where users of kotlin 2.0 would experience a `NoAudienceMatch` when evaluating rules

## 2.0.6

## Fixes
- Fix potential crash while setting render priority

## 2.0.5

## Fixes
- Fix issue with `original_transaction_id` missing when using a `PurchaseController`

# 2.0.4

## Enhancements
- Provide overloads for Java interop
- Provide utility functions for Java interop with `PurchaseController`

# 2.0.3

## Enhancements

- Renames `SuperwallPlacement` back to `SuperwallEvent`

# 2.0.2

## Fixes

- Fixes issue with `NoAudienceMatch` appearing on some devices and issues with certain campaign rules

# 2.0.1

## Enhancements
- Changes back to `handleSuperwallEvent` naming with a deprecation notice and a typealias for previous methods

## Fixes
- Removes extra failure logging when displaying alerts
- Finds nearest activity instead of relying just on Context in `PaywallComposable`
- Improves cleanup in `PaywallComposable`

# 2.0.0

Our 2.0.0 release brings some major and minor changes to both our API's and core features. For more information, please look at our [migration docs](https://superwall.com/docs/migrating-to-v2-android)

## Enhancements

- Adds `PaywallBuilder` class as an alternative to existing `getPaywallView` method. This provides a cleaner API and an ability to change purchase loading bar and shimmer view.
- Ensure safety of static webview calls that are known to fail randomly due to Webview's internal issues
- Adds `purchase` method to `Superwall` you can use to purchase products without having to resort on paywalls. To purchase a product you can pass it in one of the following objects:
  - Google Play's `ProductDetails`
  - Superwall's `StoreProduct` object
  - Or a string containing the product identifier, i.e. `Superwall.instance.purchase("product_id:base_plan:offer")`
- Adds `restorePurchases` method to `Superwall` you can use to handle restoring purchases
- Adds `getProducts` method to `Superwall` you can use to retrieve a list of `ProductDetails` given the product ID, i.e.  i.e. `Superwall.instance.purchase("product_id:base_plan:offer")`
- Adds support for observing purchases done outside of Superwall paywalls. You can now observe purchases done outside of Superwall paywalls by setting the `shouldObservePurchases` option to true and either:
  - Manually by calling `Superwall.instance.observe(PurchasingObserverState)` or utility methods `Superwall.instance.observePurchaseStart/observePurchaseError/observePurchaseResult`
  - Automatically by replacing `launchBillingFlow` with `launchBillingFlowWithSuperwall`. This will automatically observe purchases done outside of Superwall paywalls.
- Adds consumer proguard rules to enable consumer minification
- `Superwall.instance` now provides blocking or callback based version of multiple calls, suffixed with `*Sync`
- Improves preloading performance and reduces impact on the main thread
- Reduces minSDK to 22


## Breaking Changes

- `SuperwallPaywallActivity` and `PaywallView` have been moved into `com.superwall.sdk.paywall.view` package from `com.superwall.sdk.paywall.vc` package.
- Removes `PaywallComposable` and Jetpack Compose support from the main SDK artifact in favor of `Superwall-Compose` module for Jetpack Compose support:
  - You can find it at `com.superwall.sdk:superwall-compose:2.0.0-alpha`
  - Usage remains the same as before, but now you need to include the `superwall-compose` module in your project.
- Removed methods previously marked as Deprecated
- `SubscriptionStatus.Active` now takes in a set of `Entitlements`, while `Inactive` and `Active` have been turned into objects.
- `Superwall.instance.register` now uses `placement` instead of `event` as the argument name
- `preloadPaywalls` now uses `placementNames` instead of `eventNames` as the argument name
- `PaywallPresentationHandler.onDismiss` now has two arguments, `PaywallInfo` and `PaywallResult`
- `PaywallComposable` now uses `placement` argument instead of `event`
- `TriggerResult.NoRuleMatch` and `TriggerResult.EventNotFound` have been renamed to `TriggerResult.NoAudienceMatch` and `TriggerResult.PlacementNotFound`
- `PresentationResult.NoRuleMatch` and `PresentationResult.EventNotFound` have been renamed to `PresentationResult.NoAudienceMatch` and `PresentationResult.PlacementNotFound`
- `SuperwallEvent` has been renamed to `SuperwallPlacement`, belonging properties with `eventName` have been renamed to `placementName`
- `SuperwallEventInfo` has been renamed to `SuperwallPlacementInfo`
- `ComputedPropertyRequest.eventName` has been renamed to `ComputedPropertyRequest.placementName`
- `Superwall.instance.events` has been renamed to `Superwall.instance.placements`
- `LogScope.events` has been renamed to `LogScope.placements`
- `PaywallPresentationRequestStatusReason.EventNotFound` has been renamed to `PaywallPresentationRequestStatusReason.PlacementNotFound`
- `PaywallSkippedReason.EventNotFound` has been renamed to `PaywallSkippedReason.PlacementNotFound`
- `SuperwallDelegate.handleSuperwallEvent` method has been renamed to `SuperwallDelegate.handleSuperwallPlacement`
- Removed `PurchaseResult.Restored`

# 2.0.0-beta.5

## Breaking changes
- `Superwall.instance.register` now uses `placement` instead of `event` as the argument name
- `preloadPaywalls` now uses `placementNames` instead of `eventNames` as the argument name
- Superwall's `PaywallPresentationHandler.onDismiss` now has two arguments, `PaywallInfo` and `PaywallResult`
- `PaywallComposable` now uses `placement` argument instead of `event`
- Remove `PurchaseResult.Restored`

# 2.0.0-beta.4

## Breaking changes
- `SuperwallEvents.entitlementStatusDidChange` has been renamed to `SuperwallEvents.subscriptionStatusDidChange`
- `SuperwallDelegate.entitlementStatusDidChange` has been renamed to `SuperwallEvents.entitlementStatusDidChange`
- `TriggerResult.NoRuleMatch` and `TriggerResult.EventNotFound` have been renamed to `TriggerResult.NoAudienceMatch` and `TriggerResult.PlacementNotFound`
- `PresentationResult.NoRuleMatch` and `PresentationResult.EventNotFound` have been renamed to `PresentationResult.NoAudienceMatch` and `PresentationResult.PlacementNotFound`

## 2.0.0-beta.3

### Breaking changes

- `SuperwallEvent` has been renamed to `SuperwallPlacement`, belonging properties with `eventName` have been renamed to `placementName`
- `SuperwallEventInfo` has been renamed to `SuperwallPlacementInfo`
- `ComputedPropertyRequest.eventName` has been renamed to `ComputedPropertyRequest.placementName`
- `Superwall.instance.events` has been renamed to `Superwall.instance.placements`
- `LogScope.events` has been renamed to `LogScope.placements`
- `PaywallPresentationRequestStatusReason.EventNotFound` has been renamed to `PaywallPresentationRequestStatusReason.PlacementNotFound`
- `PaywallSkippedReason.EventNotFound` has been renamed to `PaywallSkippedReason.PlacementNotFound`
- `SuperwallDelegate.handleSuperwallEvent` method has been renamed to `SuperwallDelegate.handleSuperwallPlacement`

## 2.0.0-beta.2

### Breaking Changes

- API Changes:
  - Migration of `setEntitlementStatus` to `setSubscriptionStatus`
  - Exposing `Superwall.instance.entitlementsStatus`
  - Migration of `SuperwallDelegate.entitlementStatusDidChange` to `SuperwallDelegate.subscriptionStatusDidChange`

## 2.0.0-beta.1

## Enhancements

- Add `PaywallBuilder` class as an alternative to existing `getPaywallView` method. This provides a cleaner API and an ability to change purchase loading bar and shimmer view.
- Add callback versions of new 2.0 methods
- Ensure safety of static webview calls that are known to fail randomly due to Webview's internal issues


## 2.0.0-Alpha.1

### Breaking Changes

- `SuperwallPaywallActivity` and `PaywallView` have been moved into `com.superwall.sdk.paywall.view` package from `com.superwall.sdk.paywall.vc` package.
- Removes `PaywallComposable` and Jetpack Compose support from the main SDK artifact in favor of `Superwall-Compose` module for Jetpack Compose support:
  - You can find it at `com.superwall.sdk:superwall-compose:2.0.0-alpha`
  - Usage remains the same as before, but now you need to include the `superwall-compose` module in your project.
- Removed methods previously marked as Deprecated
- Removes `SubscriptionStatus`, together with belonging update methods and `subscriptionStatusDidChange` callback.
- These are replaced with `EntitlementStatus` and `entitlementStatusDidChange` callback. You can find more details on this migration in our docs.

### Enhancements
- Adds `purchase` method to `Superwall` you can use to purchase products without having to resort on paywalls. To purchase a product you can pass it in one of the following objects:
  - Google Play's `ProductDetails`
  - Superwall's `StoreProduct` object
  - Or a string containing the product identifier, i.e. `Superwall.instance.purchase("product_id:base_plan:offer")`

- Adds `restorePurchases` method to `Superwall` you can use to handle restoring purchases
- Adds `getProducts` method to `Superwall` you can use to retrieve a list of `ProductDetails` given the product ID, i.e.  i.e. `Superwall.instance.purchase("product_id:base_plan:offer")`

- Adds support for observing purchases done outside of Superwall paywalls. You can now observe purchases done outside of Superwall paywalls by setting the `shouldObservePurchases` option to true and either:
  - Manually by calling `Superwall.instance.observe(PurchasingObserverState)` or utility methods `Superwall.instance.observePurchaseStart/observePurchaseError/observePurchaseResult`
  - Automatically by replacing `launchBillingFlow` with `launchBillingFlowWithSuperwall`. This will automatically observe purchases done outside of Superwall paywalls.
- Adds consumer proguard rules to enable consumer minification
- Reduces minSDK to 22
- Adds `purchaseToken` to purchase events
- `Superwall.instance` now provides blocking or callback based version of multiple calls, suffixed with `*Sync`
- Improves preloading performance and reduces impact on the main thread

## 1.5.5

## Fixes
- Fixes potential distribution issues for variant selection in edge cases
- Fixes potential memory leaks of paywall calling activity


## 1.5.4

## Fixes
- Fixes issue when a paywall would dismiss with `ForNextPaywall` but next paywall could not be shown due to triggers or limits. Now it resolves into the proper dismiss status.

## 1.5.3

### Enhancements
- Add `purchaseToken` to TransactionComplete

## 1.5.2

### Fixes
- Fix chromium crashes caused by race conditions in webview's implementation

## 1.5.1

### Enhancements
- Updates superscript dependencies to reduce minSDK version

### Fixes
- Adds consumer proguard rules to avoid minifying JNA classes during minification

## 1.5.0

### Enhancements

- Adds `shimmerView_start` and `shimmerView_complete` events to track the loading of the shimmer animation.
- Makes `hasFreeTrial` match iOS SDK behavior by returning `true` for both free trials and non-free introductory offers
- Adds `Superwall.instance.events` - A SharedFlow instance emitting all Superwall events as `SuperwallEventInfo`. This can be used as an alternative to a delegate for listening to events.
- Adds a new shimmer animation
- Adds support for SuperScript expression evaluator

### Fixes

- Fixes concurrency issues with subscriptions triggered in Cordova apps 

## 1.5.0-beta.2

## Enhancements
- Adds `shimmerView_start` and `shimmerView_complete` events to track the loading of the shimmer animation.
- Makes `hasFreeTrial` match iOS SDK behavior by returning `true` for both free trials and non-free introductory offers

## 1.5.0-beta.1

## Enhancements
- Adds `Superwall.instance.events` - A SharedFlow instance emitting all Superwall events as `SuperwallEventInfo`. This can be used as an alternative to a delegate for listening to events.
- Adds a new shimmer animation
- Adds support for SuperScript expression evaluator

## 1.4.1

### Enhancements
- Adds `appVersionPadded` attribute

### Fixes
- Fixes issue where `PaywallPresentationHandler.onError` would be skipped in case of `BillingError`s

## 1.4.0

## Enhancements

- Improves paywall loading and preloading performance
- Reduces impact of preloading on render performance
- Updates methods to return `kotlin.Result` instead of relying on throwing exceptions
- This introduces some minor breaking changes:
  - `configure` completion block now provides a `Result<Unit>` that can be used to check for success or failure
  - `handleDeepLink` now returns a `Result<Boolean>`
  - `getAssignments` now returns a `Result<List<ConfirmedAssignments>>`
  - `confirmAllAssignments` now returns a `Result<List<ConfirmedAssignments>>`
  - `getPresentationResult` now returns a `Result<PresentationResult>`
  - `getPaywallComponents` now returns a `Result<PaywallComponents>`
- Removes Gson dependency
- Adds `isScrollEnabled` flag to enable remote control of Paywall scrollability
- Adds `PaywallResourceLoadFail` event to enable tracking of failed resources in Paywall
- Improves bottom navigation bar color handling

## Fixes
- Fixes issue where paywalls without fallback would fail to load and missing resource would cause a failure event
- Fixes issue with `trialPeriodDays` rounding to the higher value instead of lower, i.e. where `P4W2D` would return 28 days instead of 30, it now returns 30.
- Fixes issue with system navigation bar not respecting paywall color
- Fixes issues with cursor allocation in Room transaction
- Improves handling of chromium render process crash

## 1.4.0-beta.3

- Fixes issue where paywalls without fallback would fail to load and missing resource would cause a failure event

## 1.4.0-beta.2

## Enhancements

- Removes Gson dependency
- Adds `isScrollEnabled` flag to enable remote controll of Paywall scrollability

## Fixes
- Fixes issue with `trialPeriodDays` rounding to the higher value instead of lower, i.e. where `P4W2D` would return 28 days instead of 30, it now returns 30.
- Fixes issue with system navigation bar not respecting paywall color
- Reduces impact of preloading on render performance
- Fixes issues with cursor allocation in Room transaction


## 1.4.0-beta.1

- Updates methods to return `kotlin.Result` instead of relying on throwing exceptions
- This introduces some minor breaking changes:
  - `configure` completion block now provides a `Result<Unit>` that can be used to check for success or failure
  - `handleDeepLink` now returns a `Result<Boolean>`
  - `getAssignments` now returns a `Result<List<ConfirmedAssignments>>`
  - `confirmAllAssignments` now returns a `Result<List<ConfirmedAssignments>>`
  - `getPresentationResult` now returns a `Result<PresentationResult>`
  - `getPaywallComponents` now returns a `Result<PaywallComponents>`
  
## 1.3.1

### Fixes
- Fixes issue when destroying activities during sleep would cause a background crash
- Fixes issue when using Superwall with some SDK's would cause a crash (i.e. Smartlook SDK)

## 1.3.0

### Enhancements
- The existing `getPaywall` method has been deprecated and renamed to `getPaywallOrThrow`. The new `getPaywall` method now returns a `kotlin.Result<PaywallView>` instead of throwing an exception.
- Adds a new option to `SuperwallOptions` - `passIdentifiersToPlayStore` which allows you to pass the user's identifiers (from `Superwall.instance.identify(userId: String, ...)`) to the Play Store when making a purchase. Note: When passing in identifiers to use with the play store, please make sure to follow their [guidelines](https://developer.android.com/reference/com/android/billingclient/api/BillingFlowParams.Builder#setObfuscatedAccountId(java.lang.String).
- Adds `Superwall.instance.confirmAllAssignments()`, which confirms assignments for all placements and returns an array of all confirmed experiment assignments. Note that the assignments may be different when a placement is registered due to changes in user, placement, or device parameters used in audience filters.

### Fixes

- Fixes issues with Paywall sometimes not displaying when returning from background 
- Fixes issue with SDK crashing when WebView is not available
- Fixes issue with `SuperwallPaywallActivity` NPE
- Update visibility of internal `getPaywall` methods to `internal` to prevent misuse

## 1.2.9

### Fixes

- Fixes issues with `MODAL` presentation style and scrolling containers
- Fixes issues with `FULLSCREEN` presentation style rendering behind navigation

## 1.2.8

### Fixes
- Fixes issues with Paywall presentation styles not being properly passed

## 1.2.7

### Enhancements
- Exposes current configuration status via `Superwall.instance.configurationStatus`

### Fixes
- Fixes issues with Paywall previews not loading

## 1.2.6

### Fixes
- Fixes issue where the paywall would not show in some cases when using `minutes_since`
- Fixes issue with wrong URL being logged when a paywall fails to load

## 1.2.5

### Enhancements
- Adds a `Modifier` to `PaywallComposable` to allow for more control
- Adds a `PaywallView.setup(...)` method to allow for easy setup when using `PaywallView` directly
- Adds support for `MODAL` presentation style

### Fixes
- Fixes issue with displaying `PaywallComposable`
- Resolves issue where users would get `UninitializedPropertyAccessException` when calling `Superwall.instance`

## 1.2.4

### Enhancements
- For users who are not able to upgrade their AGP or Gradle versions, we have added a new artifact `superwall-android-agp-7` which keeps compatibility.

### Enhancements

- Fixes issue with decoding custom placements from paywalls.

## 1.2.3

### Enhancements

- Expose `placementName`, `paywallInfo` and `params` on a `custom_placement` event

## 1.2.2

### Enhancements

- Adds support for multiple paywall URLs, in case one CDN provider fails.
- `ActivityEncapsulatable` now uses a WeakReference instead of a reference
- SW-2900: Adds Superwall.instance.localeIdentifier as a convenience variable that you can use to dynamically update the locale used for evaluating rules and getting localized paywalls.
- SW-2919: Adds a `custom_placement` event that you can attach to any element in the paywall with a dictionary of parameters. When the element is tapped, the event will be tracked. The name of the placement can be used to trigger a paywall and its params used in audience filters.
- Adds support for bottom sheet presentation style (DRAWER), no animation style and default animation.
- Adds `build_id` and `cache_key` to `PaywallInfo`.
- SW-2917: Tracks a `config_attributes` event after calling `Superwall.configure`, which contains info about the configuration of the SDK. This gets tracked whenever you set the delegate.
- Adds in device attributes tracking after setting the interface style override.
- To comply with new Google Play Billing requirements we now avoid setting empty `offerToken` for one-time purchases

## 1.2.1

### Enhancements

- Adds the ability for the SDK to refresh the Superwall configuration every session start, subject to a feature flag.
- Tracks a `config_refresh` Superwall event when the configuration is refreshed.
- SW-2890: Adds `capabilities` to device attributes. This is a comma-separated list of capabilities the SDK has that you can target in audience filters. This release adds the `paywall_event_receiver` capability. This indicates that the paywall can receive transaction from the SDK.
- SW-2902: Adds `abandoned_product_id` to a `transaction_abandon` event to use in audience filters. You can use this to show a paywall if a user abandons the transaction for a specific product.

## 1.2.0

### Enhancements
- Adds DSL methods for configuring the SDK. You can now use a configuration block:
  ```kotlin
  fun Application.configureSuperwall(
        apiKey: String,
        configure: SuperwallBuilder.() -> Unit,
  )
  ```

This allows you to configure the SDK in a more idiomatic way:
  ```kotlin
     configureSuperwall(CONSTANT_API_KEY){
        options {
          logging {
            level = LogLevel.debug
          }
          paywalls {
            shouldPreload = false
          }
        }
    }
  ```

### Deprecations

This release includes multiple deprecations that will be removed in upcoming versions.
Most are internal and will not affect the public API, those that will are marked as such and a simple migration
path is provided. The notable ones in the public API are as follows:

- Deprecated `DebugViewControllerActivity` in favor of `DebugViewActivity`
- Deprecated `PaywallViewController` in favor of `PaywallView`
  - Deprecated belonging methods:
    - `viewWillAppear` in favor of `beforeViewCreated`
    - `viewDidAppear` in favor of `onViewCreated`
    - `viewWillDisappear` in favor of `beforeOnDestroy`
    - `viewDidDisappear` in favor of `destroyed`
    - `presentAlert` in favor of `showAlert`
- Deprecated `PaywallViewControllerDelegate` in favor of `PaywallViewCallback`
  - Deprecated belonging methods:
    -  `didFinish` in favor of `onFinished`
- Deprecated `PaywallViewControllerEventDelegate` in favor of `PaywallViewEventCallback`
  - Users might also note deprecation of `PaywallWebEvent.OpenedUrlInSafari` in favor of `PaywallWebEvent.OpenedUrlInChrome`
    -  `didFinish` in favor of `onFinished`

- In `Superwall`, the following methods were deprecated:
  - `Superwall.paywallViewController` in favor of `Superwall.paywallView`
  - `Superwall.eventDidOccur` argument `paywallViewController` in favor of `paywallView`
  - `Superwall.dismiss` in favor of `Superwall.presentPaywallView
  - `Superwall.presentPaywallViewController` in favor of `Superwall.presentPaywallView`
- Deprecated `Paywallmanager.getPaywallViewController` in favor of `PaywallManager.getPaywallView`
- Deprecated `DebugManager.viewController` in favor of `DebugManager.view`
- Deprecated `DebugViewController` in favor of `DebugView`
- Deprecated `LogScope.debugViewController` in favor of `LogScope.debugView`
- Deprecated `PaywallPresentationRequestStatus.NoPaywallViewController` in favor of `NoPaywallView`

## 1.1.9

### Deprecations

- Deprecated configuration method `Superwall.configure(applicationContext: Context, ...)` in favor of `Superwall.configure(applicationContext: Application, ...)` to enforce type safety. The rest of the method signature remains the same.

### Fixes

- SW-2878: and it's related leaks. The `PaywallViewController` was not being properly detached when activity was stopped, causing memory leaks.
- SW-2872: Fixes issue where `deviceAttributes` event and fetching would not await for IP geo to complete.
- Fixes issues on tablet devices where the paywall would close after rotation/configuration change.

## 1.1.8

### Enhancements

- SW-2859: Adds error message to `paywallWebviewLoad_fail`.
- SW-2866: Logs error when trying to purchase a product that has failed to load.
- SW-2869: Add `Reset` event to track when `Superwall.instance.reset` is called.
- SW-2867: Prevents Geo api from being called when app is in the background
- SW-2431: Improves coroutine scope usages & threading limits
- Toolchain and dependency updates

### Fixes

- SW-2863: Fixed a `NullPointerException` some users on Android 12 & 13 would experience when calling `configure`.

## 1.1.7

### Enhancements

- SW-2805: Exposes a `presentation` property on the `PaywallInfo` object. This contains information about the presentation of the paywall.
- SW-2855: Adds `restore_start`, `restore_complete`, and `restore_fail` events.

### Fixes

- SW-2854: Fixed issue where abandoning the transaction by pressing back would prevent the user from restarting the transaction.

## 1.1.6

### Enhancements

- SW-2833: Adds support for dark mode paywall background color.
-  Adds ability to target devices based on their IP address location. Use `device.ipRegion`, 
`device.ipRegionCode`, `device.ipCountry`, `device.ipCity`, `device.ipContinent`, or `device.ipTimezone`.
- Adds `event_name` to the event params for use with audience filters.

### Fixes

- Fixes issue with products whose labels weren't primary/secondary/tertiary.

## 1.1.5

### Fixes

- Fixes thread safety crash when multiple threads attempted to initialize the `JavaScriptSandbox`
  internally.

## 1.1.3

### Enhancements

- Tracks an `identity_alias` event whenever identify is called to alias Superwall's anonymous ID with a
developer provided id.
- Adds `setInterfaceStyle(interfaceStyle:)` which can be used to override the system interface style.
- Adds `device.interfaceStyleMode` to the device template, which can be `automatic` or `manual` if 
overriding the interface style.

### Fixes

- Uses `JavascriptSandbox` when available for filter expression evaluation on a background thread 
instead of running code on the main thread in a webview.
- Fixes crash where the loading spinner inside the `PaywallViewController` was being updated outside
  the main thread.

## 1.1.2

### Enhancements

- Updates build.gradle configuration which means we can now upload the SDK to maven central. You no 
longer need to specify our custom repo in your build.gradle to get our SDK and therefore installation 
should be easier.

### Fixes

- Fixes `ConcurrentModificationException` crash that sometimes happened when identifying a user.
- Fixes crash on purchasing a free trial when using `getPaywall`.

## 1.1.1

### Fixes

- Fixes an issue loading products with offers.

## 1.1.0

### Enhancements

- SW-2768: Adds `device.regionCode` and `device.preferredRegionCode`, which returns the `regionCode` 
of the locale. For example, if a locale is `en_GB`, the `regionCode` will be `GB`. You can use this
in the filters of your campaign.
- Adds support for unlimited products in a paywall.
- SW-2785: Adds internal feature flag to disable verbose events like `paywallResponseLoad_start`.

### Fixes

- SW-2732: User attributes weren't being sent on app open until identify was called. Now they are 
sent every time there's a new session.
- SW-2733: Fixes issue where the spinner would still show on a paywall if a user had previously
  purchased on it.
- SW-2744: Fixes issue where using the back button to dismiss a paywall presented via `getPaywall`
would call `didFinish` in the `PaywallViewControllerDelegate` with the incorrect values.
- Fixes issue where an invalid paywall background color would prevent the paywall from opening. If 
this happens, it will now default to white.
- SW-2748: Exposes `viewWillAppear`, `viewDidAppear`, `viewWillDisappear` and `viewDidDisappear` 
methods of `PaywallViewController` which you must call when using `getPaywall`.
- Stops `Superwall.configure` from being called multiple times.
- `getPresentationResult` now confirms assignments for holdouts.
- Gracefully handles unknown local notification types if new ones are added in the future.
- SW-2761: Fixes issue where "other" responses in paywall surveys weren't showing in the dashboard.

## 1.0.2

### Fixes

- Prevents a paywall from opening in a separate activity inside a task manager when using `taskAffinity` 
within your app.

## 1.0.1

### Fixes

- Fixes serialization of `feature_gating` in `SuperwallEvents`.
- Changes the product loading so that if preloading is enabled, it makes one API request to get all 
products available in paywalls. This results in fewer API requests. Also, it adds retry logic on failure. 
If billing isn't available on device, the `onError` handler will be called.

## 1.0.0

### Breaking Changes

- Changes the import path for the `LogScope`, and `LogLevel`.

### Fixes

- Fixes rare thread-safety crash when sending events back to Superwall's servers.
- Calls the `onError` presentation handler block when there's no activity to present a paywall on.
- Fixes issue where the wrong product may be presented to purchase if a free trial had already been 
used and you were letting Superwall handle purchases.
- Fixes `IllegalStateException` on Samsung devices when letting Superwall handle purchases.
- Keeps the text zoom of paywalls to 100% rather than respecting the accessibility settings text zoom,
which caused unexpected UI issues.
- Fixes rare `UninitializedPropertyAccessException` crash caused by a threading issue.
- Fixes crash when the user has disabled the Android System WebView.

## 1.0.0-alpha.45

### Fixes

- Fixes issue where the `paywallProductsLoad_fail` event wasn't correctly being logged. This is a
"soft fail", meaning that even though it gets logged, your paywall will still show. The error message
with the event has been updated to include all product subscription IDs that are failing to be retrieved.

## 1.0.0-alpha.44

### Fixes

- Fixes rare issue where paywall preloading was preventing paywalls from showing.

## 1.0.0-alpha.43

### Enhancements

- Adds `handleLog` to the `SuperwallDelegate`.

## 1.0.0-alpha.42

### Fixes

- Makes sure client apps use our proguard file.

## 1.0.0-alpha.41

### Fixes

- Removes need for `SCHEDULED_EXACT_ALARM` permission in manifest.

## 1.0.0-alpha.40

### Fixes

- Fixes issue presenting paywalls to users who had their device language set to Russian, Polish or 
Czech.

## 1.0.0-alpha.39

### Fixes

- Adds missing `presentationSourceType` to `PaywallInfo`.
- Fixes issue where the status bar color was always dark regardless of paywall color.
- Adds `TypeToken` to proguard rules to prevent r8 from 'optimizing' our code and causing a crash.

## 1.0.0-alpha.38

### Enhancements

- SW-2682: Adds `Superwall.instance.latestPaywallInfo`, which you can use to get the `PaywallInfo` of
the most recently presented view controller.
- SW-2683: Adds `Superwall.instance.isLoggedIn`, which you can use to check if the user is logged in.

### Fixes

- Removes use of `USE_EXACT_ALARM` permission that was getting apps rejected.
- Fixes issue with scheduling notifications. The paywall wasn't waiting to schedule notifications
  before dismissal so the permissions wasn't always showing.

## 1.0.0-alpha.37

### Enhancements

- SW-2684: Adds error logging if the `currentActivity` is `null` when trying to present a paywall.

### Fixes

- Fixes bug where paywalls might not present on first app install.
- Fixes bug where all `paywallResponseLoad_` events were being counted as `paywallResponseLoad_start`.
- Adds ProGuard rule to prevent `DefaultLifecycleObserver` from being removed.

## 1.0.0-alpha.36

### Enhancements

- Adds `X-Subscription-Status` header to all requests.
- Caches the last `subscriptionStatus`.
- Adds `subscriptionStatus_didChange` event that is fired whenever the subscription status changes.
- Calls the delegate method `subscriptionStatusDidChange` whenever the subscription status changes.
- SW-2676: Adds a completion block to the `configure` method.

### Fixes

- Fixes issue where the main thread was blocked when accessing some properties internally.
- SW-2679: Fixes issue where the `subscription_start` event was being fired even if a non-recurring product was
purchased.

## 1.0.0-alpha.35

### Fixes

- Fixes issue where `transaction_complete` events were being rejected by the server.

## 1.0.0-alpha.34

### Breaking Changes

- Changes `Superwall.instance.getUserAttributes()` to `Superwall.instance.userAttributes`.
- `SuperwallOptions.logging.logLevel` is now non-optional. Set it to `LogLevel.none` to prevent
logs from being printed to the console.

### Enhancements

- SW-2663: Adds `preloadAllPaywalls()` and `preloadPaywalls(eventNames:)` to be able to manually 
preload paywalls.
- SW-2665: Adds `Superwall.instance.userId` so you can access the current user's id.
- SW-2668: Adds `preferredLocale` and `preferredLanguageLocale` to the device attributes for use in rules.
- Adds `Superwall.instance.logLevel` as a convenience variable to set and get the log level.

### Fixes

- SW-2664: Fixes race condition between resetting and presenting paywalls.

## 1.0.0-alpha.33

### Fixes

- Fixes issue where a user would be asked to enable notifications even if there weren't any attached 
to the paywall.

## 1.0.0-alpha.32

### Enhancements

- SW-2214: Adds ability to use free trial notifications with a paywall.
- Adds `cancelAllScheduledNotifications()` to cancel any scheduled free trial notifications.
- SW-2640: Adds `computedPropertyRequests` to `PaywallInfo`.
- SW-2641: Makes `closeReason` in `PaywallInfo` non-optional.

### Fixes

- Fixes issue where thrown exceptions weren't always being caught.

## 1.0.0-alpha.31

### Enhancements

- SW-2638: Adds `Restored` to `PurchaseResult`.
- SW-2644: Adds `RestoreType` to `SuperwallEvent.TransactionRestore`.
- SW-2643: Makes `storePayment` non-optional for a `StoreTransaction`.
- SW-2642: Adds `productIdentifier` to `StorePayment`.

### Fixes

- SW-2635: Fixes crash that sometimes occurred if an app was trying to get Superwall's paywall 
configuration in the background.

## 1.0.0-alpha.30

### Enhancements

- SW-2154: The SDK now includes a paywall debugger, meaning you can scan the QR code of any paywall in the 
editor to preview it on device. You can change its localization, view product attributes, and view
the paywall with or without a trial.

### Fixes

- More bug fixes relating to the loading of products.

## 1.0.0-alpha.29

### Fixes

- SW-2631: Fixes issue where paywalls weren't showing if the products within them had a base plan or
offer ID set.

## 1.0.0-alpha.28

### Fixes

- SW-2615: Fixes crash on Android versions <8 when accessing the Android 8+ only class Period.
- SW-2616: Fixes crash where the `PaywallViewController` was sometimes being added to a new parent view before
being removed from it's existing parent view.

## 1.0.0-alpha.27

### Breaking Changes

- #SW-2218: Changes the `PurchaseController` purchase function to `purchase(activity:productDetails:basePlanId:offerId:)`. 
This adds support for purchasing offers and base plans.

### Enhancements

- #SW-2600: Backport device attributes
- Adds support for localized paywalls.
- Paywalls are only preloaded if their associated rules can match.
- Adds status bar to full screen paywalls.

### Fixes

- Fixes issue where holdouts were still matching even if the limit set for their corresponding rules were exceeded.
- #SW-2584: Fixes issue where prices with non-terminating decimals were causing products to fail to load.

## 1.0.0-alpha.26

### Fixes
- Additional fixes to make Google billing more robust.
- Fixes an issue that causes `transaction_complete` events to fail.

## 1.0.0-alpha.25

### Fixes 

- Fixes [Google Billing Crash on Samsung devices](https://community.revenuecat.com/sdks-51/how-to-fix-crash-too-many-bind-requests-999-for-service-intent-inappbillingservice-3317). 

## 1.0.0-alpha.24

### Fixes

- Fixes an issue that could cause "n/a" to be displayed on a paywall in place of the proper subscription period string.

## 1.0.0-alpha.23

### Fixes

- Fixes an issue where calling `identify` right after `configure` would hang b/c network requests need to access the user id to add to headers. 
- Fixes a potential crash when loading data from disk 

## 1.0.0-alpha.22

### Fixes

- Fixes threading issue

## 1.0.0-alpha.21

### Fixes

- Changes Activity to AppCompatActivity

## 1.0.0-alpha.20

### Enhancements

### Fixes

- Fixes `app_open` race condition
- Prevents calling purchase twice
- Disables interactivity during purchase

## 1.0.0-alpha.19

### Fixes

- Fixes `app_launch` event not triggering

## 1.0.0-alpha.18

### Enhancements

- Adds the ability to provide an `ActivityProvider` when configuring the SDK. This is an interface 
containing the function `getCurrentActivity()`, which Superwall will use to present paywalls from. 
You would typically conform an `ActivityLifecycleTracker` to this interface.

### Fixes

- Fixes a crash when storing a `List` to user attributes and if that List or a Map had a null value.

## 1.0.0-alpha.17

### Enhancements

- Adds automatic purchase controller
- Improves memory handling for webviews
- Hides the loading indicator on a paywall if transactionBackgroundView is set to NONE

## 1.0.0-alpha.14

### Enhancements

- Adds `trigger_session_id` to Superwall Events.
- Resets the scroll position of the paywall on close.

### Fixes

- Fixes issue where an invalid currency code on a device would crash the app when trying to retrieve products.

## 1.0.0-alpha.13

### Fixes

- Fixes concurrency issues when setting and retrieving values like the appUserId and seed.

## 1.0.0-alpha.11

### Fixes

- Can now use both non-recurring products and subscription products in paywalls.
- Fixes a crash issue that was caused by a lazy variable being accessed before it was initialized.

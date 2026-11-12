---
"expo-superwall": patch
---

Fixes purchase controllers and two compat SDK methods.

- Android: paywall purchases of custom products now reach your purchase controller's `onPurchase`, with `store: "CUSTOM"`, instead of failing in the native SDK. `onPurchase` now has `store` on Android too (`"PLAY_STORE"` or `"CUSTOM"`).
- Compat: `PurchaseController.purchaseFromAppStore` and `purchaseFromGooglePlay` now receive the product's `store` as a last argument, so a compat purchase controller can tell custom products from store ones.
- Compat: a presentation handler's `onPresent` is now called once per presentation. It used to be called twice.
- **Behaviour change:** compat `getPresentationResult()` now returns a `PresentationResult` instance, as typed, such as `PresentationResultPaywall`, instead of the raw native object.
- Compat: `getEntitlements()` now fills in `status` from the subscription status. It used to always be `UNKNOWN`.

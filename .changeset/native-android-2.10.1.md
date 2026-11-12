---
"expo-superwall": patch
---

Update native Android SDK to 2.10.1. [View Android SDK release notes](https://github.com/superwall/Superwall-Android/releases/tag/2.10.1).

- Android: products using `:sw-none` are no longer bought with a free trial or discount offer when you don't use a custom purchase controller.
- Android: if you use a custom `PurchaseController`, an absent `offerId` in `onPurchase` / `purchaseFromGooglePlay` means buy the base plan with no offer. Don't pick a free trial or other offer yourself.
- Android: paywall content stays clear of side navigation bars in landscape, and test mode now keeps entitlements bought during the session.

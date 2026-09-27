---
"expo-superwall": minor
---

Update native SDKs to iOS 4.17.0 and Android 2.8.4.

⚠️ **Android: Google Play Billing Library is updated from 8.0.0 to 9.1.0.** See the [Play Billing Library 9 migration guide](https://developer.android.com/google/play/billing/migrate-gpblv9) for the full list of changes.

- **Minimum Android SDK is now 23** (previously 21), as required by Billing Library 9.
- If your app still calls Billing Library APIs removed in 9 (`SkuDetails`, `SkuDetailsParams`, `querySkuDetailsAsync`, `queryPurchaseHistoryAsync`, `BillingClient.SkuType`, or the no-arg `enablePendingPurchases()`), it will no longer compile. Migrate those call sites to the `ProductDetails` APIs before upgrading.
- **Please test your billing and purchasing flows before shipping this upgrade.** The Billing Library resolves to a single version across your app, so upgrading Superwall also upgrades Billing for everything else that depends on it. If you use Google Play Billing directly, or another subscription provider such as RevenueCat, Adapty or Purchasely, make sure that provider's SDK supports Billing 9 and run through purchase, restore and subscription-status flows end to end.
- **If another subscription library you use does not support Play Billing 9**, you can pin the Billing Client to 8. Add this to your app module's `android/app/build.gradle`, outside the `android { }` block (for managed Expo projects, apply it via a config plugin):

  ```groovy
  configurations.all {
      resolutionStrategy.force 'com.android.billingclient:billing:8.3.0'
  }
  ```

  Confirm the resolved version with `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` — look for `com.android.billingclient:billing:9.1.0 -> 8.3.0`. Billing 8 still satisfies Google's August 31, 2026 requirement, and the Superwall SDK works on both 8.x and 9.x. Note that a `-dontwarn com.android.billingclient.api.QueryPurchaseHistoryParams` ProGuard rule is **not** a fix: it lets the build through but throws at runtime.

Android: system back presses are now forwarded into the paywall instead of dismissing it directly. Multi-page paywalls navigate back one page; single-page paywalls dismiss as before.

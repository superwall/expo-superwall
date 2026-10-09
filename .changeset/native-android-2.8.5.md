---
"expo-superwall": minor
---

Update native Android SDK to 2.8.5. [View Android SDK release notes](https://github.com/superwall/Superwall-Android/releases/tag/2.8.5).

- Add the Customer Center on Android: a self-service screen where users can view and restore their purchases, cancel or change a Google Play subscription, request a refund, manage a web subscription and contact support. Present it with `presentCustomerCenter()` and dismiss it with `dismissCustomerCenter()`, available on `useSuperwall()` and on the compat `Superwall.shared`. `presentCustomerCenter()` resolves once the Customer Center is dismissed, and takes an optional per-presentation `configuration` plus `onAction`, `onSurveyComplete`, `onRefundRequestComplete` and `shouldRestorePurchases` callbacks. Set the default configuration with the new `customerCenter` option. Android only: on iOS, `presentCustomerCenter()` rejects and `dismissCustomerCenter()` does nothing.
- Add the `customerCenterOpen`, `customerCenterClose`, `customerCenterAction`, `customerCenterSurveyResponse` and `customerCenterRefundRequest` Superwall events (Android only).
- Android fixes from the native SDK: subscribers are no longer reported as inactive when Google Play fails to answer a purchase query or a product no longer maps to an entitlement; unacknowledged purchases are now acknowledged on every subscription sync, so Google Play no longer refunds them; `register()` no longer stalls when a product query at launch is dropped; and a paywall whose webview can't load is now dismissed as declined, so the placement's `onDismiss` fires and then the feature block runs (non-gated) or `onError` fires (gated).

---
"expo-superwall": major
---

Add missing SDK configuration options from native iOS and Android SDKs:

- `shouldObservePurchases` (iOS & Android): Observe purchases made outside of Superwall
- `shouldBypassAppTransactionCheck` (iOS only): Disables app transaction check on SDK launch
- `maxConfigRetryCount` (iOS only): Number of retry attempts for config fetch (default: 6)
- `useMockReviews` (Android only): Enable mock review functionality

Also fixes `enableExperimentalDeviceVariables` not being passed to the Android native SDK.

**Breaking change**: Removed deprecated `collectAdServicesAttribution` option (AdServices attribution is now collected automatically by the native iOS SDK).

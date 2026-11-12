---
"expo-superwall": minor
---

Adds Android support for the `adConsent` configure option and `setAdConsent()`, previously iOS only, which report the user's ad consent with the conversions Superwall uploads to Google Ads and Meta.

Fixes the `debugViewController`, `localizationViewController` and `paywallViewController` log scopes being ignored on Android, and adds the `billing` log scope, which the Android SDK now uses for Google Play Billing logs. `storeKitManager` also still enables them on Android.

---
"expo-superwall": minor
---

Update native Android SDK to 2.9.0. [View Android SDK release notes](https://github.com/superwall/Superwall-Android/releases/tag/2.9.0).

- Android: The `adConsent` configure option and `setAdConsent()` (hooks and compat SDKs) now work on Android, not just iOS. The consent is sent as the `adUserDataConsent` and `adPersonalizationConsent` device attributes and forwarded with the conversions Superwall uploads to Google Ads and Meta. If you don't set it, the SDK uses the consent stored by an IAB TCF consent banner when GDPR applies, and otherwise defaults to granted. Where the values came from is reported in the new `adConsentSource` device attribute.
- Android: Adds subscription replacement. When a paywall's Purchase action is set to replace an existing subscription, the purchase upgrades, downgrades or switches the user's active Google Play subscription instead of buying a second one. With a custom purchase controller, the Android `onPurchase` params now include an optional `replacement` (`{ mode, oldProductId, oldPurchaseToken }`), and the compat `PurchaseController.purchaseFromGooglePlay` gets it as a new optional fourth argument. Pass it to your billing library to perform the replacement.
- Android: Adds install attribution matching. The new `attributionMatch` Superwall event (with `info: AttributionMatchInfo`) is emitted when an install is matched, or fails to match, to the ad click that led to it. The resulting `acquisition_*` attributes are added to user attributes.
- Android: Adds best-effort public IPv4 and IPv6 device attributes for apps using the Superwall MMP, and the IPv4 attribute for apps that set an AppsFlyer ID. These apps should declare IP address collection in their Play Console data safety form.
- Android: Web checkout codes passed through the Play install referrer are now redeemed, once, on the first launch after install.
- Android: Paywalls now run post-purchase actions after a one-time product purchase.
- Android: Fixes a band of the host window showing beside the display cutout around in-window (`presentationHost: "inWindow"`) paywalls.
- Android: Fixes `getProducts()` failing with `NetworkOnMainThreadException` when it fell back to the network on the main thread.

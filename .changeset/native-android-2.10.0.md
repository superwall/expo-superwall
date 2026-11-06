---
"expo-superwall": minor
---

Update native Android SDK to 2.10.0. [View Android SDK release notes](https://github.com/superwall/Superwall-Android/releases/tag/2.10.0).

- Adds the `billing` log scope (`LogScope.Billing` in `expo-superwall/compat`) for Google Play Billing logs. On Android, billing logs passed to `onLog` / `handleLog` now have the scope `"billing"` instead of `"storeKitManager"`, so update any log filter that matches `"storeKitManager"`. Setting `storeKitManager` in `logging.scopes` still enables the billing logs on Android. On iOS, `billing` enables the StoreKit (`storeKitManager`) logs.
- Android: custom store product transaction events now report `store: "CUSTOM"` instead of `"PLAY_STORE"`, and their entitlements are refreshed in the background after a successful purchase.
- Android: fixes `register()` failing with "Paywall Already Presented" for the rest of the session after the paywall activity was finished in the background (e.g. by opening a `singleTask` activity from a deep link or notification). The paywall is now dismissed and `onDismiss` is called.
- Android: observer mode (`shouldObservePurchases`) now tracks `transaction_complete` / `transaction_abandon` for single in-progress purchases, and no longer stops tracking after Play reports a cancelled or failed purchase that wasn't being observed. Observer-mode charts may show more transactions.
- Android: for Google Play subscriptions with several base plans, `CustomerInfo` and entitlements now use the base plan that was bought. When the plan can't be known (e.g. after a reinstall, or for purchases made before this release), `productId` / `latestProductId` are the Play subscription ID and expiry dates are `null`. Those users keep their access.
- Android: preloaded paywalls are paused while off screen, and a misleading `Error Evaluating JS` log after paywall messages has been removed.

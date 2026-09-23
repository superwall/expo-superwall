---
"expo-superwall": minor
---

Add Android-only `queryInAppPurchases()` to the hooks and compat APIs. It returns the one-time products the user owns in the `PURCHASED` state, with their product IDs and purchase tokens, so the app can grant the benefit and then call `consume()`. `consume()` is now also available from `useSuperwall()`.

Fix `products()` and `purchase()` result handling:
- `products()` returns products in the requested order and omits unknown identifiers. On Android, an identifier the store failed to return earlier no longer makes later calls reject.
- `purchase()` resolves with `{ type: "failed", error }` instead of rejecting when the product can't be found or the native purchase call fails.
- `consume()` on iOS resolves with the given token instead of `null`.

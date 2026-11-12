---
"expo-superwall": minor
---

Adds the details the native SDKs have for entitlements, paywalls, products and transactions, on both the hooks and compat SDKs.

- `Entitlement` now has `isActive`, `productIds`, `latestProductId`, `store`, `startsAt`, `renewedAt`, `expiresAt`, `isLifetime`, `willRenew`, `renewsAt`, `isRevoked` (iOS only), `isInGracePeriod`, `state` and `offerType`, wherever entitlements appear: `getEntitlements()`, `CustomerInfo`, `subscriptionStatus`, products and redemption results. A detail the SDK doesn't know is left out.
- `getEntitlements()` now includes `web`, the active entitlements redeemed through web checkout. Adds `getEntitlementsByProductIds(productIds)`, which returns the entitlements that products unlock.
- `PaywallInfo` now has `cacheKey`, `buildId`, `shimmerLoadStartTime`, `shimmerLoadCompleteTime`, `presentation` (a `PaywallPresentationInfo` with the style, delay and drawer or popup dimensions) and `isScrollEnabled`, plus the iOS-only `isLocal` and `introOfferEligibility`.
- Products from `products()` and in transaction events now have `attributes`, plus the iOS-only `subscriptionPeriod`, `introductoryDiscount`, `discounts`, `billingPlanType`, `isBillingPlanAvailable` and `entitlements`, and the Android-only `productType`, `storeFrontCountryCode`, `isCustomProduct` and `customTransactionId`. The App Store products in `PaywallInfo.products` now have `billingPlanType` (iOS).
- `StoreTransaction` now has `state` and `payment`.
- `CustomerInfo` now has `activeSubscriptionProductIds`.
- `transactionFail` events now have `transactionError`, which says whether the transaction is pending or failed and, for a failure, the product. Android `customerInfoDidChange` events now have the `from` and `to` customer info. The compat `SuperwallEvent` has them as `transactionError`, `fromCustomerInfo` and `toCustomerInfo`, plus `transactionType` for `transactionComplete` (iOS).
- iOS: a redeemed web checkout's `redemptionInfo` now has `userAttributes`. Android: an unknown store's details are now included in a redemption's `storeIdentifiers`, as on iOS.
- Compat: the entitlements in `getEntitlements()` are now `Entitlement` instances, and a list the bridge doesn't send is empty rather than `undefined`. The compat `Entitlement` and `EntitlementState` are now exported.

---
"expo-superwall": patch
---

`onPurchase` now has `customTransactionId` for custom products, on iOS and Android. It's the transaction ID Superwall generates for each purchase of a custom product. Pass it to your checkout and have your server send it back as `custom_transaction_id` when it reports the purchase, so Superwall attributes the revenue to the paywall. Compat purchase controllers receive it as a new last argument of `purchaseFromAppStore`, `purchaseFromGooglePlay` and `purchaseFromGooglePlayWithReplacement`.

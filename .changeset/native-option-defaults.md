---
"expo-superwall": minor
---

**Behaviour change:** the `shouldPreload` and `shouldShowWebPurchaseConfirmationAlert` paywall options now default to `true`, as in the native SDKs. Paywalls are preloaded after configuring, and on iOS a confirmation alert is shown after a web purchase. Set them to `false` in the `paywalls` options to keep the previous behaviour.

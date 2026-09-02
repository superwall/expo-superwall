---
"expo-superwall": patch
---

Bridge `togglePaywallSpinner` to Expo. Both native SDKs expose it as the companion to `handleCustomPaywallAction`, but it was never wired up, so there was no way to show the paywall's spinner while a custom action did async work. Available on the `useSuperwall` store and on the compat `Superwall` class.

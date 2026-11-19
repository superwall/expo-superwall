---
"expo-superwall": patch
---

- iOS: an embedded `PaywallView` or `CustomerCenterView` is no longer torn down when its screen is only covered, such as by a pushed screen or a full-screen modal. The paywall isn't reloaded when the screen comes back, and a custom callback or restore confirmation that was waiting on your handler can still be answered.
- Hooks SDK: `undefined` values nested inside the configure options (for example in `logging`, `paywalls.restoreFailed` or `paywalls.preloadDeviceOverrides`) and inside `PaywallView`'s `params` and `paywallOverrides` no longer reach the native module. An option left `undefined` falls back to its default.

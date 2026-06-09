---
"expo-superwall": patch
---

Fix purchase events being dropped on cold start when using a custom purchase controller, which left the paywall spinner stuck forever. Native events are now emitted to every live module instance instead of only the most recently created one (more than one app context can exist, e.g. with expo-dev-client), and `onPurchase`/`onPurchaseRestore` are buffered and replayed if they arrive before a handler subscribes.

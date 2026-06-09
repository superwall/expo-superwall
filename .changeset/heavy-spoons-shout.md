---
"expo-superwall": patch
---

Fix purchase events being dropped on cold start when using a custom purchase controller, which left the paywall spinner stuck forever. The native module emitted events through a single static reference that was overwritten by every module instance, so when more than one app context exists (e.g. expo-dev-client's launcher plus the app) the reference could point at an instance whose JS runtime never subscribed and `onPurchase`/`onPurchaseRestore` were silently dropped. Native events are now emitted to every live module instance (tracked weakly) instead of only the most recently created one.

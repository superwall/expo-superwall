---
"expo-superwall": patch
---

In the compat SDK, calls such as `identify`, `register` and `setSubscriptionStatus` no longer wait forever when `configure()` fails. Calls made before or after a failed native configure now reject with its error, until a later `configure()` succeeds. A `completion` callback that throws no longer leaves the SDK marked as unconfigured.

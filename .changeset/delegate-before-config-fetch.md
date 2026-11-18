---
"expo-superwall": patch
---

Attach the native delegate as soon as `configure()` creates the SDK instance instead of in its completion, which only runs after the first config fetch. Events the SDK reports before then (app launch, session start, the first subscription status, a paywall for an early placement) no longer go missing from `useSuperwallEvents` and the compat `SuperwallDelegate`. In the compat SDK, `Superwall.shared.setDelegate()` no longer waits for `configure()`, so a delegate set before `configure()` receives those events too. Fixes #186.

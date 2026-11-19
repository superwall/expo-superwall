---
"expo-superwall": patch
---

A purchase or restore started from a paywall no longer hangs when the purchase controller's answer can't be used. In the compat SDK, a `purchaseFromAppStore`, `purchaseFromGooglePlay` or `restorePurchases` that throws or returns nothing is now reported to the paywall as failed, and `RestorationResult.failed()` without an error sends "Unknown error" instead of `null`. On iOS, a failed result without a readable message, or an unknown result type, is now treated as failed instead of being ignored, matching Android. On Android, a non-string error message no longer throws.

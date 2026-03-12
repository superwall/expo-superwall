---
"expo-superwall": patch
---

Prevent `configure()` from settling its Expo promise more than once during
native setup on iOS and Android. This avoids crashes such as
`PromiseAlreadySettledException` if the native SDK completion handler is
invoked more than once.

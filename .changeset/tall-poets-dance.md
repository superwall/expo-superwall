---
"expo-superwall": patch
---

Fix `setUserAttributes` silently failing when JavaScript attribute values are
`null` by making the bridge value types nullable on iOS and Android, and update
TypeScript signatures to explicitly allow nullable user attribute values.

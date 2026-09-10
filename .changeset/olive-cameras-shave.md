---
"expo-superwall": patch
---

Fix web checkout redemption codes being dropped on cold start (iOS)

`handleDeepLink` called the deprecated instance overload `Superwall.shared.handleDeepLink`,
which routes immediately with no configuration guard and no replay. Because
`SuperwallProvider` hands over the URL in the same effect that starts `configure()`, a
cold-start deep link always arrived before the SDK was configured and was silently
discarded. Switched to the static `Superwall.handleDeepLink`, which stores the URL and
replays it once configuration completes.

---
"expo-superwall": minor
---

feat: Remove the export of the internal SuperwallExpoModule Class, 
this class should have not been used since it's an internal class and could break the state of the internal SuperwallStore.
If you have used in prior for a usecase that the current SDK doesn't support, please open an issue.


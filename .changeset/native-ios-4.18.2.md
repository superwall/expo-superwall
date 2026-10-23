---
"expo-superwall": minor
---

Update native iOS SDK to 4.18.2. [View iOS SDK release notes](https://github.com/superwall/Superwall-iOS/releases/tag/4.18.2).

`airbridgeId` integration attributes passed to `setIntegrationAttributes()` are now forwarded on iOS as well as Android (previously iOS ignored the key). On iOS, the Airbridge Device ID is kept when you reset or identify a different user.

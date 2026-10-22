---
"expo-superwall": minor
---

Update native Android SDK to 2.8.8. [View Android SDK release notes](https://github.com/superwall/Superwall-Android/releases/tag/2.8.8).

Adds `airbridgeId` as an integration attribute for the Airbridge Device ID (from `Airbridge.fetchDeviceUUID`). Pass it to `setIntegrationAttributes()` in the hooks SDK or `Superwall.shared.setIntegrationAttributes()` in the compat SDK. Android only for now; on iOS the key is ignored.

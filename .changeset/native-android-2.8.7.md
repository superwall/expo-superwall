---
"expo-superwall": minor
---

Update native Android SDK to 2.8.7. [View Android SDK release notes](https://github.com/superwall/Superwall-Android/releases/tag/2.8.7).

- Add the experimental `paywalls.presentationHost` option (`PresentationHost` in the compat SDK). Set it to `"inWindow"` to show paywalls as an overlay in the presenting activity's own window instead of launching a separate paywall activity. All presentation styles, back handling, surveys, trial reminder notifications, permission requests and web checkout work in this mode. The default stays `"activity"`. Android only: it's ignored on iOS.
- Android fix from the native SDK: prioritized preloading now works. The first preload batch holds only the prioritized campaign's paywalls, and the rest load afterwards.

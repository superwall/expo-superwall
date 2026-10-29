---
"expo-superwall": patch
---

Update native iOS SDK to 4.18.3. [View iOS SDK release notes](https://github.com/superwall/Superwall-iOS/releases/tag/4.18.3).

- iOS: Entitlements now stay active while an App Store subscription is in a billing grace period when using StoreKit 2. During the grace period, `SubscriptionTransaction.isActive` is `true` and `expirationDate` is the date the grace period ends.
- iOS: The native SDK's database moves out of the root of Application Support into its own folder. Existing data is migrated automatically on first launch.

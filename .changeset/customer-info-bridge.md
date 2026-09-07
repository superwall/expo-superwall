---
"expo-superwall": minor
---

Bridge `getCustomerInfo()` and the `customerInfoDidChange` event from the native SDKs.

- `getCustomerInfo()` resolves with the customer's subscription transactions, non-subscription transactions and entitlements, waiting until real data has loaded on both platforms.
- New `customerInfoDidChange` native event with `{ from, to }` snapshots, exposed as `onCustomerInfoChange` in `useSuperwallEvents` and as a `SuperwallDelegate` method in the compat SDK.
- Reactive `customerInfo` state on `useSuperwall`/`useUser`, seeded after configuration, cleared and reseeded across `identify`/`reset`, and kept in sync via the change event.

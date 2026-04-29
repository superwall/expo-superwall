---
"expo-superwall": minor
---

Bump native SDKs and expose new APIs.

**iOS — SuperwallKit 4.14.1 → 4.15.1**

- New `paywallPageView` event for multi-page paywall navigation tracking (with `PageViewData` payload).
- `PaywallInfo.presentationId` is now bridged so events within a single presentation can be correlated.
- Custom store products are fully bridged: `Product` now carries `store` (`APP_STORE` | `STRIPE` | `PADDLE` | `PLAY_STORE` | `SUPERWALL` | `CUSTOM` | `OTHER`) plus per-store identifier objects (`appStoreProduct`, `stripeProduct`, `paddleProduct`, `customProduct`). `onPurchase` also receives `store` so JS can route `CUSTOM` products to its own purchase logic instead of StoreKit.

**Android — Superwall-Android 2.7.11 → 2.7.12**

- Bridges the new `customerInfo` field on `PaywallInfo` (subscriptions, non-subscriptions, entitlements, userId).
- Picks up new intro-offer eligibility logic for Stripe/Paddle products and bottom-sheet dismiss fix on newer Samsung devices.

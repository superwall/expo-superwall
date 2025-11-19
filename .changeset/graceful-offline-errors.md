---
"expo-superwall": minor
---

feat: comprehensive error handling for SDK configuration failures

Added robust error handling to prevent apps from hanging indefinitely when SDK configuration fails (e.g., during offline scenarios). This introduces three new ways for developers to handle configuration errors:

**New Features:**
- Added `configurationError` state to store for programmatic error access
- Added `onConfigurationError` callback prop to `SuperwallProvider` for error tracking/analytics
- Added `SuperwallError` component for declarative error UI rendering
- Listen to native `configFail` events to capture configuration failures
- Improved `SuperwallLoading` and `SuperwallLoaded` to respect error states

**Breaking Changes:** None - all changes are backward compatible

**Fixes:**
- Fixed app hanging in loading state when offline or configuration fails
- Fixed unhandled promise rejections in deep link initialization
- Fixed loading state not resetting on configuration failure

Developers can now gracefully handle offline scenarios and provide better UX when SDK initialization fails.

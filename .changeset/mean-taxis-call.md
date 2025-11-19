---
"expo-superwall": patch
---

fix: resolve Android handleDeepLink promise consistently with iOS

Fixed Android crash on app launch caused by "Not a superwall link" error. The Android implementation now resolves the handleDeepLink promise with a boolean value (matching iOS behavior) instead of rejecting it for non-Superwall links. This prevents unhandled promise rejections that were causing production app crashes.

Additionally added error handling in TypeScript as a safety net for any future edge cases.

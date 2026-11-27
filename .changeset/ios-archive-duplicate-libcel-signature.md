---
"expo-superwall": patch
---

iOS: fixes archives (EAS device builds, TestFlight and App Store builds) failing since 1.9.0 with `"libcel.xcframework-ios.signature" couldn't be copied to "Signatures" because an item with the same name already exists`. Fixes #254.

---
"expo-superwall": patch
---

fix(android): handle nullable properties in RedemptionResult JSON serialization

Fixed a Kotlin compilation error where nullable properties (`variantId`, `experimentId`, `productIdentifier`) were being assigned directly to a Map<String, Any>. Now using the null-safe let operator to conditionally add these properties only when they have values.
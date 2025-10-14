## RevenueCat integration Example

This application flavor serves as an introduction to integrating Superwall with Revenuecat.
It demonstrates how to write a Superwall Purchase Controller with a RevenueCat integration.
The flavor is the same as the default one, with the exception of passing in a `RevenueCatPurchaseController` during Superwall configuration


## Setup Note
To enable purchasing:

- Set your Superwall API key in `MainApplication.kt`
- Change `applicationId` in `build.gradle.kts` to match your `applicationId`
- Ensure you are added as a [License Tester](https://developer.android.com/google/play/billing/test) to your app on Google Play Store
- Change the trigger used to display a paywall




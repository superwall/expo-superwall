## GetProducts & Purchase API Example

This application flavor serves as an introduction to Superwall's GetProducts & Purchase APIs.
It demonstrates fetching products with 3 different types:

1. With offer autoselection
2. With no offer
3. With a specific offer

Due to Google Play's limitations, you might not be able to purchase the pre-defined products, so you will
need to replace them with your own. For that, follow the setup.

## Setup

- Set your Superwall API key in `MainApplication.kt`
- Change `applicationId` in `build.gradle.kts` to match your `applicationId`
- Change product names in `./com/superwall/superapp/HomeActivity.kt` to match your products
- Ensure you are added as a [License Tester](https://developer.android.com/google/play/billing/test) to your app on Google Play Store





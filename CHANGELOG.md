# Changelog

## 0.6.1

### Patch Changes

- 7920773: fix(android): handle nullable properties in RedemptionResult JSON serialization

  Fixed a Kotlin compilation error where nullable properties (`variantId`, `experimentId`, `productIdentifier`) were being assigned directly to a Map<String, Any>. Now using the null-safe let operator to conditionally add these properties only when they have values.

## 0.6.0

### Minor Changes

- b816292: # Custom Purchase Controller API Improvement

  Changed `CustomPurchaseControllerContext` return types from `Promise<PurchaseResult | undefined>` to `Promise<PurchaseResult | void>` for cleaner success handling.

  Now you can simply not return anything for success instead of `return undefined`:

  ```tsx
  import Purchases, { PURCHASES_ERROR_CODE } from "react-native-purchases";

  <CustomPurchaseControllerProvider
    controller={{
      onPurchase: async (params) => {
        try {
          const products = await Purchases.getProducts([params.productId]);
          const product = products[0];

          if (!product) {
            return { type: "failed", error: "Product not found" };
          }

          await Purchases.purchaseStoreProduct(product);
          // Success - no return needed ✨
        } catch (error: any) {
          if (error.code === PURCHASES_ERROR_CODE.PURCHASE_CANCELLED_ERROR) {
            return { type: "cancelled" };
          }
          return { type: "failed", error: error.message };
        }
      },

      onPurchaseRestore: async () => {
        try {
          await Purchases.restorePurchases();
          // Success - no return needed ✨
        } catch (error: any) {
          return { type: "failed", error: error.message };
        }
      },
    }}
  >
    {/* Your app */}
  </CustomPurchaseControllerProvider>;
  ```

### Patch Changes

- acb9956: feature: add StoreProduct Type to exports

## 0.5.1

### Patch Changes

- 889aaf7: fix: improve custom purchase type handling
- 56a72c9: Bump Android version to 2.6.3
- 465a215: Exposes Product identifier in Redemption Info

## 0.5.0

### Minor Changes

- 8c2c14f: User identification and attribute operations are now non-blocking async calls, preventing UI freezes while ensuring proper state synchronization

  Thanks to @gursheyss for the PR #90

## 0.4.1

### Patch Changes

- 86a3b28: Update Android version to 2.6.1 adding app2web support
- 6df6cc4: Adds paddle store identifiers

## 0.4.0

### Minor Changes

- 6d3e625: bump ios to fix critical webview bug

## 0.3.2

### Patch Changes

- 5555e8e: make error handling more defensive

## 0.3.1

### Patch Changes

- 4a3f540: fix: compat typeissues

## 0.3.0

### Minor Changes

- 9ed73eb: feat: improve error handling of Custom Purchase Controller

## 0.2.9

### Patch Changes

- bd460a7: Expose signature in android StoreTransaction
- e9eeff8: Expose appAcounttoken and purchaseToken on Android StoreTransaction

## 0.2.8

### Patch Changes

- e0b57bc: fix(compat): none nullable access
- 314be3c: bump deps

## 0.2.7

### Patch Changes

- adccfe4: Update Android SDK to 2.5.4 and iOS to 4.8.2

## 0.2.6

### Patch Changes

- 10bb039: force release?

## 0.2.5

### Patch Changes

- 95636a6: Bump internal android sdk to 2.5.1

## 0.2.4

### Patch Changes

- c274e6a: Add typed SuperwallOptions and fix mispelled option name

## 0.2.3

### Patch Changes

- f9372f1: Exposes StoreTransaction in /compat

## 0.2.2

### Patch Changes

- 3f832c7: Updates Android SDK to 2.3.2

## 0.2.1

### Patch Changes

- 4327c59: expose internal types

## 0.2.0

### Minor Changes

- 3b58ea4: Updates Android SDK to 2.3.1 (with Google Play Billing library 7)

## 0.1.3

### Patch Changes

- d273d2a: bump expo module

## 0.1.2

### Patch Changes

- f243226: fix: type issues

## 0.1.1

### Patch Changes

- 707e513: temp fix swift types

## 0.1.0

### Minor Changes

- b39e98e: feat: Remove the export of the internal SuperwallExpoModule Class,
  this class should have not been used since it's an internal class and could break the state of the internal SuperwallStore.
  If you have used in prior for a usecase that the current SDK doesn't support, please open an issue.

### Patch Changes

- 32112a6: feat: handle deeplink automatically, no need for manual handling

## 0.0.18

### Patch Changes

- 3a93b2b: feat: fix inital loading state

## 0.0.17

### Patch Changes

- db980b6: fix missing types on native

## 0.0.16

### Patch Changes

- e19e626: require Expo 53+

## 0.0.15

### Patch Changes

- 020c22a: fix: old exports

## 0.0.14

### Patch Changes

- 6153163: mark things as internal
- 6fbaa94: add types to TransactionProductIdentifier

## 0.0.13

### Patch Changes

- 2ead245: feat: add getDeviceAttributes
- efbd9d5: feat: add getDeviceAttributes to ios

## 0.0.12

### Patch Changes

- 4751b75: Fixes issues with identify on Android, updates Android SDK to 2.2.3

## 0.0.11

### Patch Changes

- 9c053b3: feat: add experimentalDeviceVariables for ios

## 0.0.10

### Patch Changes

- d5beb70: fix(compat): subscription event emitter not firing

## 0.0.9

### Patch Changes

- 67edd16: feat: export internal SuperwallExpoModule for advance usage

## 0.0.8

### Patch Changes

- 0175478: feat: set subscription status to UNKNOWN on startup
- d8390ab: feat: bump ios SDK version

## 0.0.7

### Patch Changes

- f5a1d9a: fix: signout state changes
- 4df7557: fix: ios getSubscriptionStatus

## 0.0.6

### Patch Changes

- fc22062: fix: android getSubscriptionStatus returning undefined

## 0.0.5

### Patch Changes

- 8f4d758: fix compat subscriptionStatus access failing
- 9d98a30: fix: android sdk version not being passed correctly

## 0.0.4

### Patch Changes

- eb98aeb: feat: add ability to use CustomPurchaseController

  Just wrap your app with CustomPurchaseControllerProvider and pass your own handler functions to it.
  It will await the result of these handler functions to continue the purchase/restore flow.

  ```tsx
  <CustomPurchaseControllerProvider
    controller={{
      onPurchase: async (params) => {
        // Set stuff in ur system here
        if (params.platform === "ios") {
          console.log("onPurchase", params);
        } else {
          console.log("onPurchase", params.productId);
        }
        return;
      },
      onPurchaseRestore: async () => {
        console.log("onPurchaseRestore");
        // Set stuff in ur system here
        return;
      },
    }}
  >
    <SuperwallProvider apiKeys={{ ios: API_KEY }}>
      <SuperwallLoading>
        <ActivityIndicator style={{ flex: 1 }} />
      </SuperwallLoading>
      <SuperwallLoaded>
        <ScreenContent />
      </SuperwallLoaded>
    </SuperwallProvider>
  </CustomPurchaseControllerProvider>
  ```

## 0.0.3

### Patch Changes

- 72d9879: fix: adding ability to let superwall manage subscriptions

## 0.0.2

### Patch Changes

- 8914f05: Initialize new experimental Hook based SDK.

## 0.0.1

### Patch Changes

- 0cd5243: Inital Release
- 0cd5243: Change Delegate class to normal class from abstract

## Unpublished

### 🛠 Breaking changes

### 🎉 New features

### 🐛 Bug fixes

### 💡 Others

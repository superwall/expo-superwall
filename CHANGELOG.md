# Changelog

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

# Changelog

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

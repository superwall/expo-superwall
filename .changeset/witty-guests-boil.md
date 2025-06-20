---
"expo-superwall": patch
---

feat: add ability to use CustomPurchaseController


Just wrap your app with CustomPurchaseControllerProvider and pass your own handler functions to it.
It will await the result of these handler functions to continue the purchase/restore flow.
```tsx
 <CustomPurchaseControllerProvider
      controller={{
        onPurchase: async (params) => {
          // Set stuff in ur system here
          if (params.platform === "ios") {
            console.log("onPurchase", params)
          } else {
            console.log("onPurchase", params.productId)
          }
          return
        },
        onPurchaseRestore: async () => {
          console.log("onPurchaseRestore")
          // Set stuff in ur system here
          return
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

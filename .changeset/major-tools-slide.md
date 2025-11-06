---
"expo-superwall": minor
---

# Custom Purchase Controller API Improvement

Changed `CustomPurchaseControllerContext` return types from `Promise<PurchaseResult | undefined>` to `Promise<PurchaseResult | void>` for cleaner success handling.

Now you can simply not return anything for success instead of `return undefined`:

```tsx
import Purchases, { PURCHASES_ERROR_CODE } from "react-native-purchases"

<CustomPurchaseControllerProvider
  controller={{
    onPurchase: async (params) => {
      try {
        const products = await Purchases.getProducts([params.productId])
        const product = products[0]

        if (!product) {
          return { type: "failed", error: "Product not found" }
        }

        await Purchases.purchaseStoreProduct(product)
        // Success - no return needed ✨
      } catch (error: any) {
        if (error.code === PURCHASES_ERROR_CODE.PURCHASE_CANCELLED_ERROR) {
          return { type: "cancelled" }
        }
        return { type: "failed", error: error.message }
      }
    },

    onPurchaseRestore: async () => {
      try {
        await Purchases.restorePurchases()
        // Success - no return needed ✨
      } catch (error: any) {
        return { type: "failed", error: error.message }
      }
    },
  }}
>
  {/* Your app */}
</CustomPurchaseControllerProvider>
```

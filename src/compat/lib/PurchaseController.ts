import type { ProductStore } from "./Product"
import type { PurchaseResult } from "./PurchaseResult"
import type { RestorationResult } from "./RestorationResult"

/**
 * @category Purchase Controller
 * @since 0.0.15
 * Abstract class that defines the contract for a purchase controller.
 * This is used for custom purchase handling.
 */
export abstract class PurchaseController {
  /**
   * Purchase a product from the App Store.
   * @param productId The product id of the product the user would like to purchase.
   * @param store The store that backs the product. When it's `ProductStore.custom`, the
   * product isn't on the App Store and you must purchase it yourself (e.g., through your
   * backend). `undefined` if the product couldn't be matched to the latest paywall.
   * @returns A promise that resolves with the result of the purchase logic.
   */
  abstract purchaseFromAppStore(productId: string, store?: ProductStore): Promise<PurchaseResult>

  /**
   * Purchase a product from Google Play.
   * @param productId The product id of the product the user would like to purchase.
   * @param basePlanId An optional base plan identifier of the product that's being purchased.
   * @param offerId An optional offer identifier of the product that's being purchased.
   * @param store The store that backs the product. When it's `ProductStore.custom`, the
   * product isn't on Google Play and you must purchase it yourself (e.g., through your
   * backend).
   * @returns A promise that resolves with the result of the purchase logic.
   */
  abstract purchaseFromGooglePlay(
    productId: string,
    basePlanId?: string,
    offerId?: string,
    store?: ProductStore,
  ): Promise<PurchaseResult>

  /**
   * Restore purchases.
   * @returns A promise that resolves with the restoration result.
   */
  abstract restorePurchases(): Promise<RestorationResult>
}

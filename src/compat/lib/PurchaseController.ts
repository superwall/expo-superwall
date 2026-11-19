import type { ProductReplacement } from "../../SuperwallExpoModule.types"
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
   * @param customTransactionId The transaction ID Superwall generated for this purchase of a
   * custom product, or `undefined` for other products. Pass it to your checkout and have your
   * server send it back as `custom_transaction_id` when it reports the purchase to Superwall.
   * @returns A promise that resolves with the result of the purchase logic.
   */
  abstract purchaseFromAppStore(
    productId: string,
    store?: ProductStore,
    customTransactionId?: string,
  ): Promise<PurchaseResult>

  /**
   * Purchase a product from Google Play.
   * @param productId The product id of the product the user would like to purchase.
   * @param basePlanId An optional base plan identifier of the product that's being purchased.
   * @param offerId The identifier of the offer to buy, already chosen by the SDK from the paywall's
   * product (including `sw-auto` resolution). Undefined or `null` means buy the base plan with no offer:
   * don't pick a free trial or other offer yourself.
   * @param replacement The existing Google Play subscription this purchase replaces (an upgrade,
   * downgrade or plan switch), if any. Pass its `oldPurchaseToken` and `mode` to your billing
   * library's subscription update params. Undefined for a normal purchase. Since 1.8.0.
   * @param store The store that backs the product. When it's `ProductStore.custom`, the
   * product isn't on Google Play and you must purchase it yourself (e.g., through your
   * backend).
   * @param customTransactionId The transaction ID Superwall generated for this purchase of a
   * custom product, or `undefined` for other products. Pass it to your checkout and have your
   * server send it back as `custom_transaction_id` when it reports the purchase to Superwall.
   * @returns A promise that resolves with the result of the purchase logic.
   */
  abstract purchaseFromGooglePlay(
    productId: string,
    basePlanId?: string,
    offerId?: string,
    replacement?: ProductReplacement,
    store?: ProductStore,
    customTransactionId?: string,
  ): Promise<PurchaseResult>

  /**
   * Restore purchases.
   * @returns A promise that resolves with the restoration result.
   */
  abstract restorePurchases(): Promise<RestorationResult>
}

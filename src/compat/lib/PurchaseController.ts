import type { ProductReplacement } from "../../SuperwallExpoModule.types"
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
   * @returns A promise that resolves with the result of the purchase logic.
   */
  abstract purchaseFromAppStore(productId: string): Promise<PurchaseResult>

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
   * @returns A promise that resolves with the result of the purchase logic.
   */
  abstract purchaseFromGooglePlay(
    productId: string,
    basePlanId?: string,
    offerId?: string,
    replacement?: ProductReplacement,
  ): Promise<PurchaseResult>

  /**
   * Restore purchases.
   * @returns A promise that resolves with the restoration result.
   */
  abstract restorePurchases(): Promise<RestorationResult>
}

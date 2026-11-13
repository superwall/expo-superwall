/**
 * The identifier `purchase()` sends to the native SDK for a product ID or a product object.
 * A product object's `fullIdentifier` (Android's `productId:basePlanId:offerId`) wins over its
 * `productIdentifier`, so the base plan and offer of the product you were given are the ones
 * purchased.
 */
export function productIdentifierForPurchase(
  product: string | { productIdentifier: string; fullIdentifier?: string },
): string {
  if (typeof product === "string") return product
  return product.fullIdentifier || product.productIdentifier
}

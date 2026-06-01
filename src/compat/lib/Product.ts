import { Entitlement } from "./Entitlement"

/**
 * @category Enums
 * The store that backs a {@link Product}.
 *
 * - `APP_STORE`: Apple App Store product (iOS).
 * - `PLAY_STORE`: Google Play Store product (Android).
 * - `STRIPE`: Stripe-managed product.
 * - `PADDLE`: Paddle-managed product.
 * - `SUPERWALL`: Manually granted entitlement from the Superwall dashboard.
 * - `CUSTOM`: Purchased via your own `PurchaseController` (no storefront).
 * - `OTHER`: Unknown store.
 */
export enum ProductStore {
  appStore = "APP_STORE",
  playStore = "PLAY_STORE",
  stripe = "STRIPE",
  paddle = "PADDLE",
  superwall = "SUPERWALL",
  custom = "CUSTOM",
  other = "OTHER",
}

/**
 * The Apple App Store-specific data for a {@link Product}.
 */
export interface AppStoreProductIdentifier {
  id: string
}

/**
 * The Stripe-specific data for a {@link Product}.
 */
export interface StripeProductIdentifier {
  id: string
  trialDays?: number
}

/**
 * The Paddle-specific data for a {@link Product}.
 */
export interface PaddleProductIdentifier {
  id: string
}

/**
 * The custom-product data for a {@link Product}.
 * Custom products are purchased via your `PurchaseController` rather than through a storefront.
 */
export interface CustomStoreProductIdentifier {
  id: string
}

/**
 * @category Models
 * @since 0.0.15
 * Represents a product that can be purchased.
 */
export class Product {
  name?: string
  id: string
  entitlements: Set<Entitlement>
  /**
   * The store that backs this product. May be `undefined` on older native SDK
   * versions that don't bridge the field.
   */
  store?: ProductStore
  appStoreProduct?: AppStoreProductIdentifier
  stripeProduct?: StripeProductIdentifier
  paddleProduct?: PaddleProductIdentifier
  customProduct?: CustomStoreProductIdentifier

  constructor({
    id,
    name,
    entitlements,
    store,
    appStoreProduct,
    stripeProduct,
    paddleProduct,
    customProduct,
  }: {
    id: string
    name?: string
    entitlements: Set<Entitlement>
    store?: ProductStore
    appStoreProduct?: AppStoreProductIdentifier
    stripeProduct?: StripeProductIdentifier
    paddleProduct?: PaddleProductIdentifier
    customProduct?: CustomStoreProductIdentifier
  }) {
    this.id = id
    this.name = name
    this.entitlements = entitlements
    this.store = store
    this.appStoreProduct = appStoreProduct
    this.stripeProduct = stripeProduct
    this.paddleProduct = paddleProduct
    this.customProduct = customProduct
  }

  // Factory method to create a Product instance from a JSON object
  static fromJson(json: { [key: string]: any }): Product {
    return new Product({
      id: json.id,
      name: json.name,
      entitlements: new Set<Entitlement>(
        Array.isArray(json.entitlements)
          ? json.entitlements.map((item: any) => Entitlement.fromJson(item))
          : [],
      ),
      store: json.store as ProductStore | undefined,
      appStoreProduct: json.appStoreProduct,
      stripeProduct: json.stripeProduct,
      paddleProduct: json.paddleProduct,
      customProduct: json.customProduct,
    })
  }
}

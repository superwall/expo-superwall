import { Entitlement } from "./Entitlement"

/**
 * @category Models
 * Offer type that applied to a {@link SubscriptionTransaction}.
 *
 * iOS only delivers `"trial"`, `"code"`, `"promotional"` and `"winback"`;
 * `"subscription"` and `"revoked"` are Android-only values.
 */
export type SubscriptionOfferType =
  | "trial"
  | "code"
  | "subscription"
  | "promotional"
  | "winback"
  | "revoked"

/**
 * @category Models
 * A subscription transaction in the customer's purchase history.
 */
export interface SubscriptionTransaction {
  transactionId: string
  productId: string
  /** ISO-8601 string. */
  purchaseDate: string
  willRenew: boolean
  isRevoked: boolean
  isInGracePeriod: boolean
  isInBillingRetryPeriod: boolean
  isActive: boolean
  /** ISO-8601 string, or `null` if non-renewing. */
  expirationDate?: string | null
  store: string
  offerType?: SubscriptionOfferType
  subscriptionGroupId?: string
}

/**
 * @category Models
 * A non-subscription (one-time / consumable) transaction.
 */
export interface NonSubscriptionTransaction {
  transactionId: string
  productId: string
  /** ISO-8601 string. */
  purchaseDate: string
  isConsumable: boolean
  isRevoked: boolean
  store: string
}

/**
 * @category Models
 * Snapshot of the customer's subscription and entitlement state.
 * Snapshots are immutable and don't auto-update — fetch a fresh one with
 * `Superwall.shared.getCustomerInfo()` or implement the
 * `customerInfoDidChange` delegate method to stay current.
 */
export class CustomerInfo {
  userId: string
  subscriptions: SubscriptionTransaction[]
  nonSubscriptions: NonSubscriptionTransaction[]
  entitlements: Entitlement[]

  constructor({
    userId,
    subscriptions,
    nonSubscriptions,
    entitlements,
  }: {
    userId: string
    subscriptions: SubscriptionTransaction[]
    nonSubscriptions: NonSubscriptionTransaction[]
    entitlements: Entitlement[]
  }) {
    this.userId = userId
    this.subscriptions = subscriptions
    this.nonSubscriptions = nonSubscriptions
    this.entitlements = entitlements
  }

  static fromJson(json: any): CustomerInfo {
    return new CustomerInfo({
      userId: json.userId ?? "",
      subscriptions: Array.isArray(json.subscriptions) ? json.subscriptions : [],
      nonSubscriptions: Array.isArray(json.nonSubscriptions) ? json.nonSubscriptions : [],
      entitlements: Array.isArray(json.entitlements)
        ? json.entitlements.map((e: any) => Entitlement.fromJson(e))
        : [],
    })
  }
}

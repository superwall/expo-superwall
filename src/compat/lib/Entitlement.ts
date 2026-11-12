import type { SubscriptionOfferType } from "./CustomerInfo"

/**
 * @category Models
 * The state of the last subscription transaction that unlocks an {@link Entitlement}.
 * `"unknown"` is Android only.
 */
export type EntitlementState =
  | "inGracePeriod"
  | "subscribed"
  | "expired"
  | "inBillingRetryPeriod"
  | "revoked"
  | "unknown"

const DETAIL_KEYS = [
  "isActive",
  "productIds",
  "latestProductId",
  "store",
  "startsAt",
  "renewedAt",
  "expiresAt",
  "isLifetime",
  "willRenew",
  "renewsAt",
  "isRevoked",
  "isInGracePeriod",
  "state",
  "offerType",
] as const

/**
 * @category Models
 * @since 0.0.15
 * Represents a user entitlement.
 *
 * Everything but `id` and `type` is filled in on the device from the user's transactions,
 * and is absent when no transaction unlocks the entitlement.
 */
export class Entitlement {
  // The entitlement's identifier.
  id: string
  // The entitlement's type.
  type: string
  /** Whether there is any active, non-revoked transaction for this entitlement. */
  isActive?: boolean
  /** All product identifiers that map to the entitlement. */
  productIds?: string[]
  /** The product identifier of the latest transaction to unlock this entitlement. */
  latestProductId?: string
  /** The store the entitlement was granted through, such as `"APP_STORE"`. */
  store?: string
  /** ISO-8601 purchase date of the first transaction that unlocked this entitlement. */
  startsAt?: string
  /** ISO-8601 date the entitlement was last renewed. */
  renewedAt?: string
  /** ISO-8601 expiry date of the last transaction that unlocked this entitlement. */
  expiresAt?: string
  /** Whether a non-consumable purchase unlocks the entitlement for a lifetime. */
  isLifetime?: boolean
  /** Whether the last subscription transaction for this entitlement will auto-renew. */
  willRenew?: boolean
  /** ISO-8601 date at which the subscription renews, if it's active and will renew. */
  renewsAt?: string
  /** Whether the last subscription transaction was revoked. Always absent on Android. */
  isRevoked?: boolean
  /** Whether the last subscription transaction is in a billing grace period. */
  isInGracePeriod?: boolean
  /** The state of the last subscription transaction. */
  state?: EntitlementState
  /** The type of offer applied to the last subscription transaction. */
  offerType?: SubscriptionOfferType

  /**
   * Creates an instance of Entitlement.
   * @param id - The entitlement's identifier.
   */
  constructor(id: string) {
    this.id = id
    this.type = "SERVICE_LEVEL"
  }

  static fromJson(json: any): Entitlement {
    const e = Entitlement.create(json.id, json.type)
    // Absent and null details stay undefined.
    for (const key of DETAIL_KEYS) {
      if (json[key] !== undefined && json[key] !== null) {
        ;(e as any)[key] = json[key]
      }
    }
    return e
  }

  /**
   * Creates an Entitlement instance from id and type strings.
   * @param id - The entitlement's identifier.
   * @param type - The entitlement's type.
   * @returns A new Entitlement instance.
   */
  static create(id: string, type: string): Entitlement {
    const e = new Entitlement(id)
    e.type = type
    return e
  }
}

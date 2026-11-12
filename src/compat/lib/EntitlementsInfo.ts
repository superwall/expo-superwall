import { Entitlement } from "./Entitlement"
import { SubscriptionStatus } from "./SubscriptionStatus"

/**
 * @category Models
 * @since 0.0.15
 * Interface representing information about user entitlements.
 */
export interface EntitlementsInfo {
  status: SubscriptionStatus
  active: Entitlement[]
  all: Entitlement[]
  inactive: Entitlement[]
  /** The active entitlements redeemed through web checkout. */
  web: Entitlement[]
}

// Utility functions for EntitlementsInfo
export namespace EntitlementsInfo {
  export function fromObject(obj: any): EntitlementsInfo {
    const toEntitlements = (items: any): Entitlement[] =>
      Array.isArray(items) ? items.map((item: any) => Entitlement.fromJson(item)) : []
    const active = toEntitlements(obj.active)
    return {
      status: SubscriptionStatus.fromString(obj.status, active),
      active,
      all: toEntitlements(obj.all),
      inactive: toEntitlements(obj.inactive),
      web: toEntitlements(obj.web),
    }
  }
}

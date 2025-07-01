import type { Entitlement } from "./Entitlement"
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
}

// Utility functions for EntitlementsInfo
export namespace EntitlementsInfo {
  export function fromObject(obj: any): EntitlementsInfo {
    return {
      status: SubscriptionStatus.fromString(obj.status, obj.active),
      active: obj.active,
      all: obj.all,
      inactive: obj.inactive,
    }
  }
}

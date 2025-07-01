import { Entitlement } from "./Entitlement"

/**
 * @category Types
 * @since 0.0.15
 * Represents the subscription status of a user.
 */
export type SubscriptionStatus =
  | SubscriptionStatus.Active
  | SubscriptionStatus.Inactive
  | SubscriptionStatus.Unknown

export namespace SubscriptionStatus {
  /**
   * @category Types
   * @since 0.0.15
   * Represents an active subscription status.
   */
  export type Active = {
    status: `ACTIVE`
    entitlements: Entitlement[]
  }

  /**
   * @category Types
   * @since 0.0.15
   * Represents an inactive subscription status.
   */
  export type Inactive = {
    status: `INACTIVE`
  }

  /**
   * @category Types
   * @since 0.0.15
   * Represents an unknown subscription status.
   */
  export type Unknown = {
    status: `UNKNOWN`
  }

  export function Active(input: Entitlement[] | string[]): Active {
    return {
      status: "ACTIVE",
      entitlements:
        input.length === 0
          ? []
          : typeof input[0] === "string"
            ? (input as string[]).map((id) => new Entitlement(id))
            : (input as Entitlement[]),
    }
  }

  export function Inactive(): Inactive {
    return {
      status: "INACTIVE",
    }
  }

  export function Unknown(): Unknown {
    return {
      status: "UNKNOWN",
    }
  }

  export function fromString(value: string, entitlements: Entitlement[]): SubscriptionStatus {
    switch (value) {
      case "ACTIVE":
        return Active(entitlements)
      case "INACTIVE":
        return Inactive()
      default:
        return Unknown()
    }
  }

  export function fromJson(json: any): SubscriptionStatus {
    switch (json.status) {
      case "ACTIVE":
        return {
          status: "ACTIVE",
          entitlements: json.entitlements.map((entitlement: any) =>
            Entitlement.fromJson(entitlement),
          ),
        }
      case "INACTIVE":
        return {
          status: "INACTIVE",
        }
      default:
        return {
          status: "UNKNOWN",
        }
    }
  }
}

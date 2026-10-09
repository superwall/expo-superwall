import SuperwallExpoModule from "../SuperwallExpoModule"
import type {
  CustomerCenterAction,
  CustomerCenterConfiguration,
  CustomerCenterPurchase,
  CustomerCenterRefundStatus,
} from "../SuperwallExpoModule.types"

/**
 * @category Models
 * @since 1.6.0
 * Options for presenting the Customer Center. Android only.
 */
export interface PresentCustomerCenterOptions {
  /**
   * Overrides the `customerCenter` option set at configure time for this presentation.
   * Omit to use the configured value.
   */
  configuration?: CustomerCenterConfiguration
  /**
   * Called before purchases are restored. Return `false` (or resolve to it) to cancel, for
   * example after the user declines to sign in. The restore waits until this settles. Omit
   * to always restore. If it throws, the restore goes ahead.
   */
  shouldRestorePurchases?: () => boolean | Promise<boolean>
  /**
   * Called whenever the user taps a path, including custom and URL paths, before the action
   * runs. `purchase` is the purchase the action applies to, or `null` for a screen-level
   * action such as restore.
   */
  onAction?: (
    action: CustomerCenterAction,
    pathId: string,
    purchase: CustomerCenterPurchase | null,
  ) => void
  /** Called when the user answers a survey attached to a path. */
  onSurveyComplete?: (
    surveyId: string,
    optionId: string,
    action: CustomerCenterAction,
    pathId: string,
  ) => void
  /** Called when a refund request finishes. */
  onRefundRequestComplete?: (productId: string, status: CustomerCenterRefundStatus) => void
}

// Drops `undefined` at every level: the Android bridge can't convert it.
function stripUndefined(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(stripUndefined)
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value)
        .filter(([, v]) => v !== undefined)
        .map(([k, v]) => [k, stripUndefined(v)]),
    )
  }
  return value
}

/**
 * Serializes a {@link CustomerCenterConfiguration} for the native bridge.
 * @internal
 */
export function customerCenterConfigurationToJson(
  configuration: CustomerCenterConfiguration,
): Record<string, any> {
  return stripUndefined(configuration) as Record<string, any>
}

let nextPresentationId = 1

/**
 * Presents the Customer Center and resolves once it is dismissed. Rejects on iOS natively.
 * @internal
 */
export async function presentCustomerCenter(options: PresentCustomerCenterOptions = {}) {
  const handlerId = `customer-center-${nextPresentationId++}`
  const {
    configuration,
    shouldRestorePurchases,
    onAction,
    onSurveyComplete,
    onRefundRequestComplete,
  } = options

  const subscriptions = [
    SuperwallExpoModule.addListener("onCustomerCenterAction", (data) => {
      if (data.handlerId !== handlerId) return
      onAction?.(data.action, data.pathId, data.purchase ?? null)
    }),
    SuperwallExpoModule.addListener("onCustomerCenterSurveyComplete", (data) => {
      if (data.handlerId !== handlerId) return
      onSurveyComplete?.(data.surveyId, data.optionId, data.action, data.pathId)
    }),
    SuperwallExpoModule.addListener("onCustomerCenterRefundRequestComplete", (data) => {
      if (data.handlerId !== handlerId) return
      onRefundRequestComplete?.(data.productId, data.status)
    }),
    SuperwallExpoModule.addListener("onCustomerCenterShouldRestorePurchases", async (data) => {
      if (data.handlerId !== handlerId || !shouldRestorePurchases) return
      let proceed = true
      try {
        proceed = await shouldRestorePurchases()
      } catch (error) {
        console.error("[Superwall] shouldRestorePurchases threw; restoring anyway.", error)
      }
      SuperwallExpoModule.didHandleCustomerCenterShouldRestorePurchases(data.requestId, proceed)
    }),
  ]

  try {
    await SuperwallExpoModule.presentCustomerCenter(
      configuration ? customerCenterConfigurationToJson(configuration) : null,
      handlerId,
      shouldRestorePurchases != null,
    )
  } finally {
    for (const subscription of subscriptions) subscription.remove()
  }
}

/**
 * Dismisses the Customer Center, if presented. A no-op on iOS natively.
 * @internal
 */
export async function dismissCustomerCenter() {
  await SuperwallExpoModule.dismissCustomerCenter()
}

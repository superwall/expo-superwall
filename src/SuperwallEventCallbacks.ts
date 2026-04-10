import type {
  CustomCallback,
  CustomCallbackResult,
  LogLevel,
  LogScope,
  PaywallInfo,
  PaywallResult,
  PaywallSkippedReason,
  RedemptionResult,
  SubscriptionStatus,
  SuperwallEventInfo,
} from "./SuperwallExpoModule.types"

/**
 * @category Events
 * @since 0.0.15
 * Defines the available callbacks for subscribing to Superwall native events.
 * Each property is an optional callback function that will be invoked when the corresponding event occurs.
 * If a callback is not provided, no listener will be registered for that event.
 */
export interface SuperwallEventCallbacks {
  /**
   * Called when a paywall is presented.
   * @param paywallInfo - Information about the presented paywall. See {@link PaywallInfo}.
   */
  onPaywallPresent?: (paywallInfo: PaywallInfo) => void
  /**
   * Called when a paywall is dismissed.
   * @param paywallInfo - Information about the dismissed paywall. See {@link PaywallInfo}.
   * @param result - The result of the paywall interaction. See {@link PaywallResult}.
   */
  onPaywallDismiss?: (paywallInfo: PaywallInfo, result: PaywallResult) => void
  /**
   * Called when a paywall is skipped.
   * @param reason - The reason why the paywall was skipped. See {@link PaywallSkippedReason}.
   */
  onPaywallSkip?: (reason: PaywallSkippedReason) => void
  /**
   * Called when an error occurs during paywall presentation or other SDK operations.
   * @param error - A string describing the error.
   */
  onPaywallError?: (error: string) => void

  /**
   * Called when the user's subscription status changes.
   * @param status - The new subscription status. See {@link SubscriptionStatus}.
   */
  onSubscriptionStatusChange?: (status: SubscriptionStatus) => void

  /**
   * Called for all Superwall internal events. This is a general-purpose event handler.
   * @param eventInfo - Information about the Superwall event. See {@link SuperwallEventInfo}.
   */
  onSuperwallEvent?: (eventInfo: SuperwallEventInfo) => void
  /**
   * Called when a custom action is triggered from a paywall (e.g., via `superwall.triggerCustomPaywallAction('myAction')`).
   * @param name - The name of the custom action.
   */
  onCustomPaywallAction?: (name: string) => void

  /**
   * Called just before a paywall is dismissed.
   * @param paywallInfo - Information about the paywall that will be dismissed. See {@link PaywallInfo}.
   */
  willDismissPaywall?: (paywallInfo: PaywallInfo) => void
  /**
   * Called just before a paywall is presented.
   * @param paywallInfo - Information about the paywall that will be presented. See {@link PaywallInfo}.
   */
  willPresentPaywall?: (paywallInfo: PaywallInfo) => void
  /**
   * Called after a paywall has been dismissed.
   * @param paywallInfo - Information about the paywall that was dismissed. See {@link PaywallInfo}.
   */
  didDismissPaywall?: (paywallInfo: PaywallInfo) => void
  /**
   * Called after a paywall has been presented.
   * @param paywallInfo - Information about the paywall that was presented. See {@link PaywallInfo}.
   */
  didPresentPaywall?: (paywallInfo: PaywallInfo) => void

  /**
   * Called when the paywall attempts to open a URL.
   * @param url - The URL that the paywall will attempt to open.
   */
  onPaywallWillOpenURL?: (url: string) => void
  /**
   * Called when the paywall attempts to open a deep link.
   * @param url - The deep link URL that the paywall will attempt to open.
   */
  onPaywallWillOpenDeepLink?: (url: string) => void

  /**
   * Called for logging messages from the SDK.
   * @param params - An object containing log details.
   * @param params.level - The log level. See {@link LogLevel}.
   * @param params.scope - The scope of the log. See {@link LogScope}.
   * @param params.message - The log message.
   * @param params.info - Additional info associated with the log.
   * @param params.error - Error message if applicable.
   */
  onLog?: (params: {
    level: LogLevel
    scope: LogScope
    message: string | null
    info: Record<string, any> | null
    error: string | null
  }) => void

  /**
   * Called before the SDK attempts to redeem a promotional link.
   */
  willRedeemLink?: () => void
  /**
   * Called after the SDK has attempted to redeem a promotional link.
   * @param result - The result of the redemption attempt. See {@link RedemptionResult}.
   */
  didRedeemLink?: (result: RedemptionResult) => void

  /**
   * Called when a purchase is initiated.
   * @param params - Parameters related to the purchase. For iOS, this includes `productId` and `platform`.
   *                 For Android, this includes `productId`, `platform`, `basePlanId`, and `offerId`.
   */
  onPurchase?: (params: any) => void
  /**
   * Called when a purchase restoration is initiated.
   */
  onPurchaseRestore?: () => void

  /**
   * Called when the back button is pressed while a paywall is displayed (Android only).
   * This is only triggered when `rerouteBackButton` is enabled in the paywall settings.
   *
   * @param paywallInfo - Information about the currently displayed paywall
   * @returns `true` to consume the back press and prevent default dismiss behavior, `false` to allow normal dismiss
   *
   * @platform Android
   * @internal This is used internally by SuperwallProvider and should not be called directly.
   */
  onBackPressed?: (paywallInfo: PaywallInfo) => boolean

  /**
   * Called when a custom callback is invoked from a paywall.
   * Custom callbacks allow paywalls to communicate with the app to perform
   * operations like validation, data fetching, etc.
   *
   * @param callback - The custom callback info containing name and optional variables.
   * @returns A promise or value with the result to send back to the paywall.
   */
  onCustomCallback?: (callback: CustomCallback) => Promise<CustomCallbackResult> | CustomCallbackResult

  /**
   * An optional identifier used to scope certain events (like `onPaywallPresent`, `onPaywallDismiss`, `onPaywallSkip`)
   * to a specific `usePlacement` hook instance. If provided, these events will only be triggered
   * if the event originated from a `registerPlacement` call associated with the same `handlerId`.
   * This is primarily used internally by the `usePlacement` hook.
   * @internal
   */
  handlerId?: string
}

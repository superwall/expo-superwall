import { useEffect, useRef } from "react"
import SuperwallExpoModule from "./SuperwallExpoModule"
import type {
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
  onPaywallPresent?: (paywallInfo: PaywallInfo) => void;
  /**
   * Called when a paywall is dismissed.
   * @param paywallInfo - Information about the dismissed paywall. See {@link PaywallInfo}.
   * @param result - The result of the paywall interaction. See {@link PaywallResult}.
   */
  onPaywallDismiss?: (paywallInfo: PaywallInfo, result: PaywallResult) => void;
  /**
   * Called when a paywall is skipped.
   * @param reason - The reason why the paywall was skipped. See {@link PaywallSkippedReason}.
   */
  onPaywallSkip?: (reason: PaywallSkippedReason) => void;
  /**
   * Called when an error occurs during paywall presentation or other SDK operations.
   * @param error - A string describing the error.
   */
  onPaywallError?: (error: string) => void;

  /**
   * Called when the user's subscription status changes.
   * @param status - The new subscription status. See {@link SubscriptionStatus}.
   */
  onSubscriptionStatusChange?: (status: SubscriptionStatus) => void;

  /**
   * Called for all Superwall internal events. This is a general-purpose event handler.
   * @param eventInfo - Information about the Superwall event. See {@link SuperwallEventInfo}.
   */
  onSuperwallEvent?: (eventInfo: SuperwallEventInfo) => void;
  /**
   * Called when a custom action is triggered from a paywall (e.g., via `superwall.triggerCustomPaywallAction('myAction')`).
   * @param name - The name of the custom action.
   */
  onCustomPaywallAction?: (name: string) => void;

  /**
   * Called just before a paywall is dismissed.
   * @param paywallInfo - Information about the paywall that will be dismissed. See {@link PaywallInfo}.
   */
  willDismissPaywall?: (paywallInfo: PaywallInfo) => void;
  /**
   * Called just before a paywall is presented.
   * @param paywallInfo - Information about the paywall that will be presented. See {@link PaywallInfo}.
   */
  willPresentPaywall?: (paywallInfo: PaywallInfo) => void;
  /**
   * Called after a paywall has been dismissed.
   * @param paywallInfo - Information about the paywall that was dismissed. See {@link PaywallInfo}.
   */
  didDismissPaywall?: (paywallInfo: PaywallInfo) => void;
  /**
   * Called after a paywall has been presented.
   * @param paywallInfo - Information about the paywall that was presented. See {@link PaywallInfo}.
   */
  didPresentPaywall?: (paywallInfo: PaywallInfo) => void;

  /**
   * Called when the paywall attempts to open a URL.
   * @param url - The URL that the paywall will attempt to open.
   */
  onPaywallWillOpenURL?: (url: string) => void;
  /**
   * Called when the paywall attempts to open a deep link.
   * @param url - The deep link URL that the paywall will attempt to open.
   */
  onPaywallWillOpenDeepLink?: (url: string) => void;

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
    level: LogLevel;
    scope: LogScope;
    message: string | null;
    info: Record<string, any> | null;
    error: string | null;
  }) => void;

  /**
   * Called before the SDK attempts to redeem a promotional link.
   */
  willRedeemLink?: () => void;
  /**
   * Called after the SDK has attempted to redeem a promotional link.
   * @param result - The result of the redemption attempt. See {@link RedemptionResult}.
   */
  didRedeemLink?: (result: RedemptionResult) => void;

  /**
   * Called when a purchase is initiated.
   * @param params - Parameters related to the purchase. For iOS, this includes `productId` and `platform`.
   *                 For Android, this includes `productId`, `platform`, `basePlanId`, and `offerId`.
   */
  onPurchase?: (params: any) => void; // Type any due to platform differences, specific types in SuperwallExpoModule.types.ts
  /**
   * Called when a purchase restoration is initiated.
   */
  onPurchaseRestore?: () => void;

  /**
   * An optional identifier used to scope certain events (like `onPaywallPresent`, `onPaywallDismiss`, `onPaywallSkip`)
   * to a specific `usePlacement` hook instance. If provided, these events will only be triggered
   * if the event originated from a `registerPlacement` call associated with the same `handlerId`.
   * This is primarily used internally by the `usePlacement` hook.
   * @internal
   */
  handlerId?: string;
}

/**
 * @category Hooks
 * @since 0.0.15
 * React hook for subscribing to a wide range of native Superwall events.
 *
 * This hook allows you to define callback functions for various events emitted by the
 * Superwall SDK, such as paywall presentation, dismissal, subscription status changes,
 * and more. Event listeners are automatically added when the component mounts
 * and removed when it unmounts.
 *
 * It must be used within a component that is a descendant of `<SuperwallProvider />`.
 *
 * @param callbacks - An object where keys are event names (from `SuperwallEventCallbacks`)
 *                    and values are the corresponding callback functions.
 *                    If `callbacks.handlerId` is provided, core paywall events (`onPaywallPresent`,
 *                    `onPaywallDismiss`, `onPaywallSkip`) will be scoped to that handler. This is
 *                    mainly for internal use by `usePlacement`.
 *
 * @example
 * useSuperwallEvents({
 *   onPaywallPresent: (info) => console.log("Paywall presented:", info.name),
 *   onSubscriptionStatusChange: (status) => console.log("New sub status:", status),
 *   onLog: (logParams) => {
 *     if (logParams.level === 'error') {
 *       console.error(`[Superwall SDK Error] ${logParams.scope}: ${logParams.message}`);
 *     }
 *   },
 * });
 *
 * // This component will now log specific Superwall events.
 * // Return null or your component's JSX.
 */
export function useSuperwallEvents({
  handlerId: trackedHandlerId,
  ...callbacks
}: SuperwallEventCallbacks = {}): void {
  // Keep the latest callbacks in a ref so event handlers always use the latest
  const callbacksRef = useRef<SuperwallEventCallbacks>(callbacks)
  callbacksRef.current = callbacks

  useEffect(() => {
    const subs: { remove: () => void }[] = []

    /* ---------------- Core events ---------------- */
    if (callbacksRef.current.onPaywallPresent) {
      subs.push(
        SuperwallExpoModule.addListener("onPaywallPresent", ({ paywallInfoJson, handlerId }) => {
          if (trackedHandlerId && handlerId !== trackedHandlerId) return

          callbacksRef.current.onPaywallPresent?.(paywallInfoJson)
        }),
      )
    }

    if (callbacksRef.current.onPaywallDismiss) {
      subs.push(
        SuperwallExpoModule.addListener(
          "onPaywallDismiss",
          ({ paywallInfoJson, result, handlerId }) => {
            if (trackedHandlerId && handlerId !== trackedHandlerId) return

            callbacksRef.current.onPaywallDismiss?.(paywallInfoJson, result)
          },
        ),
      )
    }

    if (callbacksRef.current.onPaywallSkip) {
      subs.push(
        SuperwallExpoModule.addListener("onPaywallSkip", ({ skippedReason, handlerId }) => {
          if (trackedHandlerId && handlerId !== trackedHandlerId) return
          callbacksRef.current.onPaywallSkip?.(skippedReason)
        }),
      )
    }

    if (callbacksRef.current.onPaywallError) {
      subs.push(
        SuperwallExpoModule.addListener("onPaywallError", ({ errorString }) => {
          callbacksRef.current.onPaywallError?.(errorString)
        }),
      )
    }

    /* ---------------- Subscription ---------------- */
    if (callbacksRef.current.onSubscriptionStatusChange) {
      subs.push(
        SuperwallExpoModule.addListener("subscriptionStatusDidChange", ({ to }) => {
          callbacksRef.current.onSubscriptionStatusChange?.(to)
        }),
      )
    }

    /* ---------------- Delegate bridge ---------------- */
    if (callbacksRef.current.onSuperwallEvent) {
      subs.push(
        SuperwallExpoModule.addListener("handleSuperwallEvent", ({ eventInfo }) => {
          callbacksRef.current.onSuperwallEvent?.(eventInfo)
        }),
      )
    }

    if (callbacksRef.current.onCustomPaywallAction) {
      subs.push(
        SuperwallExpoModule.addListener(
          "handleCustomPaywallAction",
          ({ name }: { name: string }) => {
            callbacksRef.current.onCustomPaywallAction?.(name)
          },
        ),
      )
    }

    /* ---------------- Paywall lifecycle ---------------- */
    const lifecycleMap: [keyof SuperwallEventCallbacks, string][] = [
      ["willDismissPaywall", "willDismissPaywall"],
      ["willPresentPaywall", "willPresentPaywall"],
      ["didDismissPaywall", "didDismissPaywall"],
      ["didPresentPaywall", "didPresentPaywall"],
    ]

    lifecycleMap.forEach(([key, event]) => {
      const cb = callbacksRef.current[key]
      if (!cb) return
      subs.push(
        SuperwallExpoModule.addListener(event as any, ({ info }: { info: PaywallInfo }) => {
          // @ts-expect-error – key narrowed above
          callbacksRef.current[key]?.(info)
        }),
      )
    })

    /* ---------------- Links ---------------- */
    if (callbacksRef.current.onPaywallWillOpenURL) {
      subs.push(
        SuperwallExpoModule.addListener("paywallWillOpenURL", ({ url }: { url: string }) => {
          callbacksRef.current.onPaywallWillOpenURL?.(url)
        }),
      )
    }

    if (callbacksRef.current.onPaywallWillOpenDeepLink) {
      subs.push(
        SuperwallExpoModule.addListener("paywallWillOpenDeepLink", ({ url }: { url: string }) => {
          callbacksRef.current.onPaywallWillOpenDeepLink?.(url)
        }),
      )
    }

    /* ---------------- Logs ---------------- */
    if (callbacksRef.current.onLog) {
      subs.push(
        SuperwallExpoModule.addListener("handleLog", (params) => {
          callbacksRef.current.onLog?.(params)
        }),
      )
    }

    /* ---------------- Promotional links ---------------- */
    if (callbacksRef.current.willRedeemLink) {
      subs.push(
        SuperwallExpoModule.addListener("willRedeemLink", () => {
          callbacksRef.current.willRedeemLink?.()
        }),
      )
    }

    if (callbacksRef.current.didRedeemLink) {
      subs.push(
        SuperwallExpoModule.addListener("didRedeemLink", (result: RedemptionResult) => {
          callbacksRef.current.didRedeemLink?.(result)
        }),
      )
    }

    /* ---------------- Purchases ---------------- */
    if (callbacksRef.current.onPurchase) {
      subs.push(
        SuperwallExpoModule.addListener("onPurchase", (params: any) => {
          callbacksRef.current.onPurchase?.(params)
        }),
      )
    }

    if (callbacksRef.current.onPurchaseRestore) {
      subs.push(
        SuperwallExpoModule.addListener("onPurchaseRestore", () => {
          callbacksRef.current.onPurchaseRestore?.()
        }),
      )
    }

    return () => subs.forEach((s) => s.remove())
  }, [trackedHandlerId])
}

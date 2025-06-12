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
 * A mapping of Superwall native events to optional callback handlers.
 * Only the callbacks you provide will be subscribed to under the hood, so
 * passing an empty object results in no listeners being registered.
 */
export interface SuperwallEventCallbacks {
  // Core events already surfaced by `usePlacement`
  onPaywallPresent?: (info: PaywallInfo) => void
  onPaywallDismiss?: (info: PaywallInfo, result: PaywallResult) => void
  onPaywallSkip?: (reason: PaywallSkippedReason) => void
  onPaywallError?: (error: string) => void

  // Subscription status
  onSubscriptionStatusChange?: (status: SubscriptionStatus) => void

  // Delegate bridge events
  onSuperwallEvent?: (eventInfo: SuperwallEventInfo) => void
  onCustomPaywallAction?: (name: string) => void

  // Paywall lifecycle
  willDismissPaywall?: (info: PaywallInfo) => void
  willPresentPaywall?: (info: PaywallInfo) => void
  didDismissPaywall?: (info: PaywallInfo) => void
  didPresentPaywall?: (info: PaywallInfo) => void

  // URL / Deeplink
  onPaywallWillOpenURL?: (url: string) => void
  onPaywallWillOpenDeepLink?: (url: string) => void

  // Logs
  onLog?: (params: {
    level: LogLevel
    scope: LogScope
    message: string | null
    info: Record<string, any> | null
    error: string | null
  }) => void

  // Promotional links
  willRedeemLink?: () => void
  didRedeemLink?: (result: RedemptionResult) => void

  // Purchases
  onPurchase?: (params: any) => void
  onPurchaseRestore?: () => void

  handlerId?: string
}

/**
 * React hook that lets you subscribe to **any** native Superwall event using
 * a simple callback object:
 *
 * ```tsx
 * useSuperwallEvents({
 *   onPaywallPresent: (info) => console.log("Presented", info),
 *   handleLog: (log) => console.log(log.scope, log.message),
 * })
 * ```
 *
 * All listeners are automatically cleaned up when the component using this
 * hook unmounts.
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

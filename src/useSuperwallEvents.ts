import { useEffect, useRef } from "react"
import type { SuperwallEventCallbacks } from "./SuperwallEventCallbacks"
import { subscribeToSuperwallEvents } from "./internal/superwallEventBridge"

export type { SuperwallEventCallbacks } from "./SuperwallEventCallbacks"

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
    return subscribeToSuperwallEvents(trackedHandlerId, () => callbacksRef.current)
  }, [trackedHandlerId])
}

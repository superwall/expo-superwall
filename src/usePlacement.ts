import { useCallback, useId, useState } from "react"

import type { PaywallInfo, PaywallResult, PaywallSkippedReason } from "./SuperwallExpoModule.types"
import { useSuperwall } from "./useSuperwall"
import { useSuperwallEvents } from "./useSuperwallEvents"

// -------------------- Types --------------------
/**
 * Possible states returned by `usePlacement`.
 */
export type PaywallState =
  | { status: "idle" }
  | { status: "presented"; paywallInfo: PaywallInfo }
  | { status: "dismissed"; result: PaywallResult }
  | { status: "skipped"; reason: PaywallSkippedReason }
  | { status: "error"; error: string }

export interface usePlacementCallbacks {
  /** Called when a paywall is presented. */
  onPresent?: (paywallInfo: PaywallInfo) => void
  /** Called when a paywall is dismissed. */
  onDismiss?: (paywallInfo: PaywallInfo, result: PaywallResult) => void
  /** Called when a paywall is skipped. */
  onSkip?: (reason: PaywallSkippedReason) => void
  /** Called when a paywall fails to present or another SDK error occurs. */
  onError?: (error: string) => void
}

export interface RegisterPlacementArgs {
  /** The placement name defined on the Superwall dashboard. */
  placement: string
  /** Optional parameters passed to the placement. */
  params?: Record<string, any>
  /**
   * An optional feature/function to execute if the placement does **not** result
   * in a paywall presentation – i.e. the user is allowed through.
   */
  feature?: () => void
}

// -------------------- Hook --------------------
/**
 * React hook for managing paywall presentation based on placements.
 *
 * This hook provides a way to register placements configured on the Superwall dashboard.
 * It handles the state of paywall presentation (`PaywallState`) and allows you
 * to define callbacks for various paywall lifecycle events (`usePlacementCallbacks`).
 *
 * It must be used within a component that is a descendant of `<SuperwallProvider />`.
 *
 * @param callbacks - An optional object containing callback functions for paywall events
 *                    like `onPresent`, `onDismiss`, `onSkip`, and `onError`.
 * @returns An object containing:
 *   - `registerPlacement: (args: RegisterPlacementArgs) => Promise<void>`:
 *     A function to register a placement and potentially trigger a paywall.
 *     - `args.placement`: The placement name defined on the Superwall dashboard.
 *     - `args.params`: Optional parameters to pass to the placement.
 *     - `args.feature`: An optional function to execute if the placement does not result
 *                       in a paywall presentation (e.g., user is already subscribed or part of a holdout).
 *   - `state: PaywallState`: The current state of the paywall interaction for this hook instance.
 *     This includes states like "idle", "presented", "dismissed", "skipped", or "error".
 *
 * @example
 * const { registerPlacement, state } = usePlacement({
 *   onPresent: (paywallInfo) => console.log("Paywall presented:", paywallInfo.name),
 *   onDismiss: (paywallInfo, result) => console.log("Paywall dismissed:", result.type),
 *   onSkip: (reason) => console.log("Paywall skipped:", reason.type),
 *   onError: (error) => console.error("Paywall error:", error),
 * });
 *
 * const showMyFeaturePaywall = async () => {
 *   await registerPlacement({
 *     placement: "MyFeaturePlacement",
 *     feature: () => {
 *       // Logic to execute if paywall is not shown (e.g., navigate to feature)
 *       console.log("Accessing feature directly.");
 *     }
 *   });
 * };
 *
 * // In your component:
 * // <Button title="Unlock Feature" onPress={showMyFeaturePaywall} />
 * // <Text>Current paywall state: {state.status}</Text>
 */
export function usePlacement(callbacks: usePlacementCallbacks = {}) {
  const id = useId()

  const [state, setState] = useState<PaywallState>({ status: "idle" })

  useSuperwallEvents({
    handlerId: id,
    onPaywallDismiss(info, result) {
      setState({ status: "dismissed", result })

      callbacks.onDismiss?.(info, result)
    },
    onPaywallSkip(reason) {
      setState({ status: "skipped", reason })
      callbacks.onSkip?.(reason)
    },
    onPaywallError(error) {
      setState({ status: "error", error })
      callbacks.onError?.(error)
    },
    onPaywallPresent(info) {
      setState({ status: "presented", paywallInfo: info })
      callbacks.onPresent?.(info)
    },
  })

  const { registerPlacement: storeRegisterPlacement } = useSuperwall((state) => ({
    registerPlacement: state.registerPlacement,
  }))

  /* -------------------- Helpers -------------------- */
  const registerPlacement = useCallback(
    async ({ placement, params, feature }: RegisterPlacementArgs) => {
      await storeRegisterPlacement(placement, params, id)
      // Execute feature if provided (called when the user is allowed access)
      feature?.()
    },
    [storeRegisterPlacement, id],
  )

  return {
    registerPlacement,
    state,
  } as const
}

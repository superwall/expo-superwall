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
 * React hook that surfaces paywall presentation events and a convenient
 * `registerPlacement` helper.
 *
 * ```tsx
 * const { registerPlacement, state, error } = usePlacement({
 *   onPresent: (info) => console.log("Paywall presented", info),
 *   onError: (err) => console.warn(err),
 * })
 *
 * // ...
 * await registerPlacement({ placement: "StartWorkout", feature: navigation.startWorkout })
 * ```
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

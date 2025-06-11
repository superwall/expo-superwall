import { useCallback, useEffect, useMemo, useRef } from "react"

import type { PaywallInfo, PaywallResult, PaywallSkippedReason } from "./SuperwallExpoModule.types"
import { useSuperwall } from "./useSuperwall"

// -------------------- Types --------------------
/**
 * Possible states returned by `usePaywall`.
 */
export type PaywallState =
  | { status: "idle" }
  | { status: "presented"; paywallInfo: PaywallInfo }
  | { status: "dismissed"; result: PaywallResult }
  | { status: "skipped"; reason: PaywallSkippedReason }
  | { status: "error"; error: string }

export interface UsePaywallCallbacks {
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
 * const { registerPlacement, state, error } = usePaywall({
 *   onPresent: (info) => console.log("Paywall presented", info),
 *   onError: (err) => console.warn(err),
 * })
 *
 * // ...
 * await registerPlacement({ placement: "StartWorkout", feature: navigation.startWorkout })
 * ```
 */
export function usePaywall(callbacks: UsePaywallCallbacks = {}) {
  const {
    registerPlacement: storeRegisterPlacement,
    activePaywallInfo,
    lastPaywallResult,
    lastSkippedReason,
    lastError,
  } = useSuperwall((state) => ({
    registerPlacement: state.registerPlacement,
    activePaywallInfo: state.activePaywallInfo,
    lastPaywallResult: state.lastPaywallResult,
    lastSkippedReason: state.lastSkippedReason,
    lastError: state.lastError,
  }))

  /* -------------------- Callback handling -------------------- */
  const prevPaywallInfoRef = useRef<PaywallInfo | undefined>(undefined)
  const prevResultRef = useRef<PaywallResult | undefined>(undefined)
  const prevSkipRef = useRef<PaywallSkippedReason | undefined>(undefined)
  const prevErrorRef = useRef<string | null>(null)

  // Paywall present
  useEffect(() => {
    if (activePaywallInfo && activePaywallInfo !== prevPaywallInfoRef.current) {
      callbacks.onPresent?.(activePaywallInfo)
      prevPaywallInfoRef.current = activePaywallInfo
    }
  }, [activePaywallInfo, callbacks])

  // Paywall dismissed
  useEffect(() => {
    if (lastPaywallResult && lastPaywallResult !== prevResultRef.current) {
      // The SDK does not resend the PaywallInfo on dismiss, so use the last known one.
      if (prevPaywallInfoRef.current) {
        callbacks.onDismiss?.(prevPaywallInfoRef.current, lastPaywallResult)
      }
      prevResultRef.current = lastPaywallResult
    }
  }, [lastPaywallResult, callbacks])

  // Paywall skipped
  useEffect(() => {
    if (lastSkippedReason && lastSkippedReason !== prevSkipRef.current) {
      callbacks.onSkip?.(lastSkippedReason)
      prevSkipRef.current = lastSkippedReason
    }
  }, [lastSkippedReason, callbacks])

  // Error
  useEffect(() => {
    if (lastError && lastError !== prevErrorRef.current) {
      callbacks.onError?.(lastError)
      prevErrorRef.current = lastError
    }
  }, [lastError, callbacks])

  /* -------------------- Derived state -------------------- */
  const state: PaywallState = useMemo(() => {
    if (activePaywallInfo) {
      return { status: "presented", paywallInfo: activePaywallInfo }
    }
    if (lastPaywallResult) {
      return { status: "dismissed", result: lastPaywallResult }
    }
    if (lastSkippedReason) {
      return { status: "skipped", reason: lastSkippedReason }
    }
    if (lastError) {
      return { status: "error", error: lastError }
    }
    return { status: "idle" }
  }, [activePaywallInfo, lastPaywallResult, lastSkippedReason, lastError])

  /* -------------------- Helpers -------------------- */
  const registerPlacement = useCallback(
    async ({ placement, params, feature }: RegisterPlacementArgs) => {
      await storeRegisterPlacement(placement, params)
      // Execute feature if provided (called when the user is allowed access)
      if (feature) {
        feature()
      }
    },
    [storeRegisterPlacement],
  )

  return {
    registerPlacement,
    state,
    error: lastError,
  } as const
}

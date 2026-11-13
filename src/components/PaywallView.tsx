import { requireNativeView } from "expo"
import { type ComponentType, type ReactNode, useEffect, useRef, useState } from "react"
import { type NativeSyntheticEvent, StyleSheet, View, type ViewProps } from "react-native"
import SuperwallExpoModule from "../SuperwallExpoModule"
import type {
  CustomCallback,
  CustomCallbackResult,
  PaywallInfo,
  PaywallLoadingState,
  PaywallOverrides,
  PaywallResult,
  PaywallSkippedReason,
} from "../SuperwallExpoModule.types"
import { filterUndefined } from "../utils/filterUndefined"

type LoadStartEvent = NativeSyntheticEvent<Record<string, never>>
type PresentEvent = NativeSyntheticEvent<{ paywallInfo: PaywallInfo }>
type DismissEvent = NativeSyntheticEvent<{
  paywallInfo: PaywallInfo
  result: PaywallResult
  shouldDismiss: boolean
}>
type SkipEvent = NativeSyntheticEvent<{ reason: PaywallSkippedReason }>
type ErrorEvent = NativeSyntheticEvent<{ message: string }>
type CustomCallbackEvent = NativeSyntheticEvent<CustomCallback & { callbackId: string }>
type LoadingStateEvent = NativeSyntheticEvent<{ state: PaywallLoadingState }>

type NativePaywallViewProps = {
  placement: string
  params?: Record<string, any>
  paywallOverrides?: Record<string, any>
  style?: ViewProps["style"]
  onPaywallCustomCallback?: (event: CustomCallbackEvent) => void
  onPaywallLoadingStateChange?: (event: LoadingStateEvent) => void
  onPaywallLoadStart?: (event: LoadStartEvent) => void
  onPaywallPresent?: (event: PresentEvent) => void
  onPaywallDismiss?: (event: DismissEvent) => void
  onPaywallSkip?: (event: SkipEvent) => void
  onPaywallError?: (event: ErrorEvent) => void
}

// Looked up on first render rather than at import, so importing the SDK for its methods alone
// doesn't need the native view.
let NativePaywallViewComponent: ComponentType<NativePaywallViewProps> | undefined
const getNativePaywallView = () => {
  NativePaywallViewComponent ??= requireNativeView<NativePaywallViewProps>(
    "SuperwallExpo",
    "PaywallView",
  )
  return NativePaywallViewComponent
}

export type PaywallFallbackState =
  | { status: "loading" }
  | { status: "skipped"; reason: PaywallSkippedReason }
  | { status: "error"; error: string }
  | { status: "dismissed"; paywallInfo: PaywallInfo; result: PaywallResult }

type PaywallViewState = PaywallFallbackState | { status: "presented"; paywallInfo: PaywallInfo }

/**
 * Whether finishing a paywall with `result` gives the user access to the feature behind it,
 * following the native SwiftUI `PaywallView`: a purchase or restore always does, and declining
 * does when the paywall isn't gated, unless it closed to make way for the next paywall.
 * @internal
 */
export function paywallResultUnlocksFeature(paywallInfo: PaywallInfo, result: PaywallResult) {
  if (result.type === "purchased" || result.type === "restored") return true
  return (
    result.type === "declined" &&
    paywallInfo.closeReason !== "forNextPaywall" &&
    paywallInfo.featureGatingBehavior === "nonGated"
  )
}

export interface PaywallViewProps extends Omit<ViewProps, "children"> {
  /** The placement whose paywall should be retrieved. */
  placement: string
  /** Optional parameters used to evaluate and render the placement. */
  params?: Record<string, any>
  /**
   * Replaces products on the paywall by name, or its presentation style. Changing it retrieves
   * the paywall again.
   */
  paywallOverrides?: PaywallOverrides
  /**
   * Called when the user gets access to the feature behind the paywall: after a purchase or
   * restore, or when they decline a paywall that isn't gated. It isn't called when the paywall
   * is skipped; use `onSkip` for that.
   */
  feature?: () => void
  /**
   * iOS only. Called when the paywall invokes a custom callback. Return a result to send back
   * to the paywall; without this prop the paywall is told the callback failed. On Android the
   * native SDK can't deliver custom callbacks from an embedded paywall, so this is never called.
   */
  onCustomCallback?: (
    callback: CustomCallback,
  ) => Promise<CustomCallbackResult> | CustomCallbackResult
  /**
   * iOS only. Called when the paywall's loading state changes, for example while a purchase is
   * in progress. Never called on Android, where the native SDK doesn't report it.
   */
  onLoadingStateChange?: (state: PaywallLoadingState) => void
  /**
   * Renders UI while the paywall is loading or after it is skipped, fails, or dismisses.
   * Returning `null` for a terminal state removes the PaywallView from the layout.
   */
  renderFallback?: (state: PaywallFallbackState) => ReactNode
  /** Called once the paywall has been retrieved and attached to the inline host. */
  onPresent?: (paywallInfo: PaywallInfo) => void
  /** Called when the embedded paywall finishes an interaction. */
  onDismiss?: (paywallInfo: PaywallInfo, result: PaywallResult, shouldDismiss: boolean) => void
  /** Called when campaign rules intentionally skip the requested paywall. */
  onSkip?: (reason: PaywallSkippedReason) => void
  /** Called when the paywall cannot be retrieved or attached. */
  onError?: (error: string) => void
}

/**
 * Embeds a Superwall paywall inside a React Native layout.
 *
 * Give the view an explicit width/height, and unmount it when the surrounding
 * content no longer needs it. Each mounted component owns its native paywall;
 * use different placements for paywalls mounted at the same time.
 */
export function PaywallView({
  placement,
  params,
  paywallOverrides,
  feature,
  onCustomCallback,
  onLoadingStateChange,
  renderFallback,
  onPresent,
  onDismiss,
  onSkip,
  onError,
  style,
  ...viewProps
}: PaywallViewProps) {
  const [state, setState] = useState<PaywallViewState>({ status: "loading" })
  const configurationKey = `${placement}:${JSON.stringify(params ?? null)}:${JSON.stringify(
    paywallOverrides ?? null,
  )}`
  const previousConfigurationKey = useRef(configurationKey)

  useEffect(() => {
    if (previousConfigurationKey.current !== configurationKey) {
      previousConfigurationKey.current = configurationKey
      setState({ status: "loading" })
    }
  }, [configurationKey])

  const fallback = state.status === "presented" ? null : renderFallback?.(state)
  const isTerminal =
    state.status === "skipped" || state.status === "error" || state.status === "dismissed"

  if (isTerminal) {
    if (fallback === null || fallback === undefined || fallback === false) return null
    return (
      <View {...viewProps} style={style}>
        {fallback}
      </View>
    )
  }

  const NativePaywallView = getNativePaywallView()
  return (
    <View {...viewProps} style={style}>
      <NativePaywallView
        placement={placement}
        params={params}
        // The Android bridge can't convert `undefined`, so pass only what's set.
        paywallOverrides={paywallOverrides ? filterUndefined(paywallOverrides) : undefined}
        style={StyleSheet.absoluteFill}
        onPaywallCustomCallback={(event) => {
          const { callbackId, name, variables } = event.nativeEvent
          const fail = () =>
            void SuperwallExpoModule.didHandleCustomCallback(callbackId, "failure", undefined)
          if (!onCustomCallback) {
            fail()
            return
          }
          // Wrapped so a handler that throws synchronously still answers the paywall.
          new Promise<CustomCallbackResult>((resolve) =>
            resolve(onCustomCallback({ name, variables })),
          ).then(
            (result) =>
              void SuperwallExpoModule.didHandleCustomCallback(
                callbackId,
                result.status,
                result.data,
              ),
            fail,
          )
        }}
        onPaywallLoadingStateChange={(event) => onLoadingStateChange?.(event.nativeEvent.state)}
        onPaywallLoadStart={() => setState({ status: "loading" })}
        onPaywallPresent={(event) => {
          const { paywallInfo } = event.nativeEvent
          setState({ status: "presented", paywallInfo })
          onPresent?.(paywallInfo)
        }}
        onPaywallDismiss={(event) => {
          const { paywallInfo, result, shouldDismiss } = event.nativeEvent
          if (shouldDismiss) setState({ status: "dismissed", paywallInfo, result })
          onDismiss?.(paywallInfo, result, shouldDismiss)
          if (paywallResultUnlocksFeature(paywallInfo, result)) feature?.()
        }}
        onPaywallSkip={(event) => {
          const { reason } = event.nativeEvent
          setState({ status: "skipped", reason })
          onSkip?.(reason)
        }}
        onPaywallError={(event) => {
          const { message } = event.nativeEvent
          setState({ status: "error", error: message })
          onError?.(message)
        }}
      />
      {fallback}
    </View>
  )
}

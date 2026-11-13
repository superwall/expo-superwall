import { requireNativeView } from "expo"
import { type ComponentType, type ReactNode, useEffect, useRef, useState } from "react"
import { type NativeSyntheticEvent, Platform, StyleSheet, View, type ViewProps } from "react-native"
import {
  customerCenterConfigurationToJson,
  type PresentCustomerCenterOptions,
} from "../internal/customerCenter"
import SuperwallExpoModule from "../SuperwallExpoModule"

type NativeCustomerCenterViewProps = {
  configuration?: Record<string, any>
  handlerId: string
  asksBeforeRestoring: boolean
  style?: ViewProps["style"]
  onCustomerCenterError?: (event: NativeSyntheticEvent<{ message: string }>) => void
}

// Only the iOS module registers this view, so it's looked up on first use rather than at import.
let NativeCustomerCenterView: ComponentType<NativeCustomerCenterViewProps> | undefined
const getNativeCustomerCenterView = () => {
  NativeCustomerCenterView ??= requireNativeView<NativeCustomerCenterViewProps>(
    "SuperwallExpo",
    "CustomerCenterView",
  )
  return NativeCustomerCenterView
}

let nextEmbeddedId = 1

export interface CustomerCenterViewProps
  extends Omit<ViewProps, "children">,
    PresentCustomerCenterOptions {
  /** Called when the Customer Center can't be shown, such as before Superwall is configured. */
  onError?: (error: string) => void
  /** Renders in place of the Customer Center on Android, where it can't be embedded. */
  renderFallback?: () => ReactNode
}

/**
 * iOS only. Embeds the Customer Center inside a React Native layout, instead of presenting it
 * as a sheet with `presentCustomerCenter()`.
 *
 * Give the view an explicit width/height and mount it once Superwall is configured. It has its
 * own navigation bar, which its screens push onto, and no close button: unmount it to close it.
 * `configuration` is read when the view mounts; remount the view (for example with a new `key`)
 * to apply a different one.
 *
 * The Android SDK can only present the Customer Center as its own activity, so on Android this
 * renders `renderFallback`, or nothing. Use `presentCustomerCenter()` there.
 */
export function CustomerCenterView({
  configuration,
  shouldRestorePurchases,
  onAction,
  onSurveyComplete,
  onRefundRequestComplete,
  onError,
  renderFallback,
  style,
  ...viewProps
}: CustomerCenterViewProps) {
  const [handlerId] = useState(() => `customer-center-view-${nextEmbeddedId++}`)
  const callbacks = useRef({
    shouldRestorePurchases,
    onAction,
    onSurveyComplete,
    onRefundRequestComplete,
  })
  callbacks.current = {
    shouldRestorePurchases,
    onAction,
    onSurveyComplete,
    onRefundRequestComplete,
  }

  useEffect(() => {
    if (Platform.OS !== "ios") return
    const subscriptions = [
      SuperwallExpoModule.addListener("onCustomerCenterAction", (data) => {
        if (data.handlerId !== handlerId) return
        callbacks.current.onAction?.(data.action, data.pathId, data.purchase ?? null)
      }),
      SuperwallExpoModule.addListener("onCustomerCenterSurveyComplete", (data) => {
        if (data.handlerId !== handlerId) return
        callbacks.current.onSurveyComplete?.(data.surveyId, data.optionId, data.action, data.pathId)
      }),
      SuperwallExpoModule.addListener("onCustomerCenterRefundRequestComplete", (data) => {
        if (data.handlerId !== handlerId) return
        callbacks.current.onRefundRequestComplete?.(data.productId, data.status)
      }),
      SuperwallExpoModule.addListener("onCustomerCenterShouldRestorePurchases", async (data) => {
        if (data.handlerId !== handlerId) return
        let proceed = true
        try {
          proceed = (await callbacks.current.shouldRestorePurchases?.()) ?? true
        } catch (error) {
          console.error("[Superwall] shouldRestorePurchases threw; restoring anyway.", error)
        }
        SuperwallExpoModule.didHandleCustomerCenterShouldRestorePurchases(data.requestId, proceed)
      }),
    ]
    return () => {
      for (const subscription of subscriptions) subscription.remove()
    }
  }, [handlerId])

  if (Platform.OS !== "ios") {
    const fallback = renderFallback?.()
    if (fallback === null || fallback === undefined || fallback === false) return null
    return (
      <View {...viewProps} style={style}>
        {fallback}
      </View>
    )
  }

  const NativeView = getNativeCustomerCenterView()
  return (
    <View {...viewProps} style={style}>
      <NativeView
        configuration={configuration ? customerCenterConfigurationToJson(configuration) : undefined}
        handlerId={handlerId}
        // The native delegate always asks; without a handler the answer is to go ahead.
        asksBeforeRestoring
        style={StyleSheet.absoluteFill}
        onCustomerCenterError={(event) => onError?.(event.nativeEvent.message)}
      />
    </View>
  )
}

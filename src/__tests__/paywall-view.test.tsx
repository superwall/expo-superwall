import { View } from "react-native"
import TestRenderer, { act } from "react-test-renderer"
import type { PaywallInfo, PaywallResult, PaywallSkippedReason } from "../SuperwallExpoModule.types"

jest.mock("expo", () => ({
  requireNativeView: () => "NativePaywallView",
}))

const mockDidHandleCustomCallback = jest.fn().mockResolvedValue(undefined)

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: { didHandleCustomCallback: mockDidHandleCustomCallback },
}))

jest.mock("react-native", () => ({
  StyleSheet: {
    absoluteFill: { bottom: 0, left: 0, position: "absolute", right: 0, top: 0 },
  },
  View: "View",
}))

;(globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true

const {
  PaywallView,
  paywallResultUnlocksFeature,
}: typeof import("../components/PaywallView") = require("../components/PaywallView")

const paywallInfo = {
  identifier: "paywall-id",
  name: "Article Paywall",
} as PaywallInfo
const declined = { type: "declined" } satisfies PaywallResult
const skipped = { type: "PlacementNotFound" } satisfies PaywallSkippedReason

function fallbackFor(state: { status: string }) {
  return <View testID={`fallback-${state.status}`} />
}

describe("PaywallView", () => {
  it("declaratively renders each fallback state and preserves lifecycle callbacks", () => {
    const onPresent = jest.fn()
    const onDismiss = jest.fn()
    const onSkip = jest.fn()
    const onError = jest.fn()

    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(
        <PaywallView
          placement="article"
          renderFallback={fallbackFor}
          onPresent={onPresent}
          onDismiss={onDismiss}
          onSkip={onSkip}
          onError={onError}
        />,
      )
    })

    expect(renderer!.root.findByProps({ testID: "fallback-loading" })).toBeDefined()
    let nativeView = renderer!.root.findByType("NativePaywallView" as never)

    act(() => {
      nativeView.props.onPaywallPresent({ nativeEvent: { paywallInfo } })
    })
    expect(onPresent).toHaveBeenCalledWith(paywallInfo)
    expect(renderer!.root.findAllByProps({ testID: "fallback-loading" })).toHaveLength(0)

    nativeView = renderer!.root.findByType("NativePaywallView" as never)
    act(() => {
      nativeView.props.onPaywallDismiss({
        nativeEvent: { paywallInfo, result: declined, shouldDismiss: false },
      })
    })
    expect(onDismiss).toHaveBeenLastCalledWith(paywallInfo, declined, false)
    expect(renderer!.root.findByType("NativePaywallView" as never)).toBeDefined()

    act(() => {
      nativeView.props.onPaywallDismiss({
        nativeEvent: { paywallInfo, result: declined, shouldDismiss: true },
      })
    })
    expect(renderer!.root.findByProps({ testID: "fallback-dismissed" })).toBeDefined()
    expect(renderer!.root.findAllByType("NativePaywallView" as never)).toHaveLength(0)

    act(() => {
      renderer!.update(
        <PaywallView
          placement="another-article"
          renderFallback={fallbackFor}
          onSkip={onSkip}
          onError={onError}
        />,
      )
    })
    expect(renderer!.root.findByProps({ testID: "fallback-loading" })).toBeDefined()
    nativeView = renderer!.root.findByType("NativePaywallView" as never)

    act(() => {
      nativeView.props.onPaywallSkip({ nativeEvent: { reason: skipped } })
    })
    expect(onSkip).toHaveBeenCalledWith(skipped)
    expect(renderer!.root.findByProps({ testID: "fallback-skipped" })).toBeDefined()

    act(() => {
      renderer!.update(
        <PaywallView
          placement="error-article"
          renderFallback={fallbackFor}
          onSkip={onSkip}
          onError={onError}
        />,
      )
    })
    nativeView = renderer!.root.findByType("NativePaywallView" as never)
    act(() => {
      nativeView.props.onPaywallError({ nativeEvent: { message: "Network unavailable" } })
    })
    expect(onError).toHaveBeenCalledWith("Network unavailable")
    expect(renderer!.root.findByProps({ testID: "fallback-error" })).toBeDefined()
  })

  it("collapses terminal states when no fallback is provided", () => {
    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(<PaywallView placement="article" />)
    })

    const nativeView = renderer!.root.findByType("NativePaywallView" as never)
    act(() => {
      nativeView.props.onPaywallSkip({ nativeEvent: { reason: skipped } })
    })

    expect(renderer!.toJSON()).toBeNull()
  })

  it("passes paywall overrides to the native view and reloads when they change", () => {
    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(
        <PaywallView
          placement="article"
          renderFallback={fallbackFor}
          paywallOverrides={{ productsByName: { primary: "yearly" }, presentationStyle: undefined }}
        />,
      )
    })

    let nativeView = renderer!.root.findByType("NativePaywallView" as never)
    // `undefined` is stripped: the Android bridge can't convert it.
    expect(nativeView.props.paywallOverrides).toEqual({ productsByName: { primary: "yearly" } })

    act(() => {
      nativeView.props.onPaywallPresent({ nativeEvent: { paywallInfo } })
    })
    expect(renderer!.root.findAllByProps({ testID: "fallback-loading" })).toHaveLength(0)

    act(() => {
      renderer!.update(
        <PaywallView
          placement="article"
          renderFallback={fallbackFor}
          paywallOverrides={{ productsByName: { primary: "monthly" } }}
        />,
      )
    })
    expect(renderer!.root.findByProps({ testID: "fallback-loading" })).toBeDefined()
    nativeView = renderer!.root.findByType("NativePaywallView" as never)
    expect(nativeView.props.paywallOverrides).toEqual({ productsByName: { primary: "monthly" } })
  })

  it("runs the feature when the result gives access, as the native PaywallView does", () => {
    const gated = { ...paywallInfo, featureGatingBehavior: "gated", closeReason: "manualClose" }
    const nonGated = { ...gated, featureGatingBehavior: "nonGated" } as PaywallInfo
    const purchased = { type: "purchased", productId: "monthly" } satisfies PaywallResult

    expect(paywallResultUnlocksFeature(gated as PaywallInfo, purchased)).toBe(true)
    expect(paywallResultUnlocksFeature(gated as PaywallInfo, { type: "restored" })).toBe(true)
    expect(paywallResultUnlocksFeature(gated as PaywallInfo, declined)).toBe(false)
    expect(paywallResultUnlocksFeature(nonGated, declined)).toBe(true)
    expect(
      paywallResultUnlocksFeature({ ...nonGated, closeReason: "forNextPaywall" }, declined),
    ).toBe(false)

    const feature = jest.fn()
    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(<PaywallView placement="article" feature={feature} />)
    })
    const nativeView = renderer!.root.findByType("NativePaywallView" as never)

    act(() => {
      nativeView.props.onPaywallDismiss({
        nativeEvent: { paywallInfo: gated, result: declined, shouldDismiss: false },
      })
    })
    expect(feature).not.toHaveBeenCalled()

    act(() => {
      nativeView.props.onPaywallDismiss({
        nativeEvent: { paywallInfo: nonGated, result: declined, shouldDismiss: true },
      })
    })
    expect(feature).toHaveBeenCalledTimes(1)
  })

  it("answers custom callbacks and reports loading state changes", async () => {
    const onLoadingStateChange = jest.fn()
    const onCustomCallback = jest
      .fn()
      .mockResolvedValueOnce({ status: "success", data: { valid: true } })
      .mockImplementationOnce(() => {
        throw new Error("boom")
      })

    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(
        <PaywallView
          placement="article"
          onCustomCallback={onCustomCallback}
          onLoadingStateChange={onLoadingStateChange}
        />,
      )
    })
    const nativeView = renderer!.root.findByType("NativePaywallView" as never)

    act(() => {
      nativeView.props.onPaywallLoadingStateChange({ nativeEvent: { state: "loadingPurchase" } })
    })
    expect(onLoadingStateChange).toHaveBeenCalledWith("loadingPurchase")

    await act(async () => {
      nativeView.props.onPaywallCustomCallback({
        nativeEvent: { callbackId: "one", name: "validate", variables: { email: "a@b.c" } },
      })
    })
    expect(onCustomCallback).toHaveBeenCalledWith({
      name: "validate",
      variables: { email: "a@b.c" },
    })
    expect(mockDidHandleCustomCallback).toHaveBeenLastCalledWith("one", "success", { valid: true })

    // A handler that throws still answers, so the paywall isn't left waiting.
    await act(async () => {
      nativeView.props.onPaywallCustomCallback({ nativeEvent: { callbackId: "two", name: "x" } })
    })
    expect(mockDidHandleCustomCallback).toHaveBeenLastCalledWith("two", "failure", undefined)

    // Without a handler the paywall is told the callback failed.
    act(() => {
      renderer!.update(<PaywallView placement="article" />)
    })
    await act(async () => {
      renderer!.root
        .findByType("NativePaywallView" as never)
        .props.onPaywallCustomCallback({ nativeEvent: { callbackId: "three", name: "x" } })
    })
    expect(mockDidHandleCustomCallback).toHaveBeenLastCalledWith("three", "failure", undefined)
  })
})

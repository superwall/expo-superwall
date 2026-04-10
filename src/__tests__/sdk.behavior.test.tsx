import TestRenderer, { act } from "react-test-renderer"

const mockListeners = new Map<string, Set<(payload: any) => void>>()
const mockHandleDeepLink = jest.fn().mockResolvedValue(false)
const mockRefreshConfiguration = jest.fn().mockResolvedValue(undefined)
const mockDidHandleBackPressed = jest.fn()
const mockDidHandleCustomCallback = jest.fn().mockResolvedValue(undefined)
const mockAddListener = jest.fn(
  (eventName: string, listener: (payload: any) => void): { remove: () => void } => {
    const listeners = mockListeners.get(eventName) ?? new Set()
    listeners.add(listener)
    mockListeners.set(eventName, listeners)

    return {
      remove: () => {
        listeners.delete(listener)
        if (listeners.size === 0) {
          mockListeners.delete(eventName)
        }
      },
    }
  },
)

const emit = (eventName: string, payload: any) => {
  const listeners = mockListeners.get(eventName)
  if (!listeners) return
  listeners.forEach((listener) => {
    listener(payload)
  })
}

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: mockAddListener,
    handleDeepLink: mockHandleDeepLink,
    refreshConfiguration: mockRefreshConfiguration,
    didHandleBackPressed: mockDidHandleBackPressed,
    didHandleCustomCallback: mockDidHandleCustomCallback,
  },
}))

const mockGetInitialURL = jest.fn().mockResolvedValue(null)
const mockAddLinkingListener = jest.fn(() => ({ remove: jest.fn() }))

jest.mock("react-native", () => {
  return {
    Platform: {
      OS: "ios",
    },
    Linking: {
      getInitialURL: mockGetInitialURL,
      addEventListener: mockAddLinkingListener,
    },
  }
})

;(globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true

const { SuperwallProvider }: typeof import("../SuperwallProvider") = require("../SuperwallProvider")
const {
  SuperwallContext,
  useSuperwallStore,
}: typeof import("../useSuperwall") = require("../useSuperwall")
const { usePlacement }: typeof import("../usePlacement") = require("../usePlacement")
const { useSuperwallEvents }: typeof import("../useSuperwallEvents") =
  require("../useSuperwallEvents")
const {
  __resetSuperwallEventBridgeForTests,
}: typeof import("../internal/superwallEventBridge") = require("../internal/superwallEventBridge")

describe("SDK behavior regressions", () => {
  let consoleErrorSpy: jest.SpyInstance
  let consoleLogSpy: jest.SpyInstance

  beforeEach(() => {
    jest.clearAllMocks()
    __resetSuperwallEventBridgeForTests()
    ;(globalThis as { __DEV__?: boolean }).__DEV__ = true
    consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {})
    consoleLogSpy = jest.spyOn(console, "log").mockImplementation(() => {})
    useSuperwallStore.setState({
      isConfigured: false,
      isLoading: false,
      listenersInitialized: false,
      configurationError: null,
      user: null,
      subscriptionStatus: { status: "UNKNOWN" },
    })
  })

  afterEach(() => {
    consoleErrorSpy.mockRestore()
    consoleLogSpy.mockRestore()
  })

  it("scopes onPaywallError by handlerId while keeping global listeners unscoped", () => {
    const scopedError = jest.fn()
    const globalError = jest.fn()

    function ScopedHarness() {
      useSuperwallEvents({
        handlerId: "placement-a",
        onPaywallError: scopedError,
      })
      return null
    }

    function GlobalHarness() {
      useSuperwallEvents({
        onPaywallError: globalError,
      })
      return null
    }

    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(
        <>
          <ScopedHarness />
          <GlobalHarness />
        </>,
      )
    })

    act(() => {
      emit("onPaywallError", {
        errorString: "wrong placement",
        handlerId: "placement-b",
      })
    })

    expect(scopedError).not.toHaveBeenCalled()
    expect(globalError).toHaveBeenCalledWith("wrong placement")

    act(() => {
      emit("onPaywallError", {
        errorString: "matching placement",
        handlerId: "placement-a",
      })
    })

    expect(scopedError).toHaveBeenCalledWith("matching placement")
    act(() => {
      renderer!.unmount()
    })
  })

  it("does not run the placement feature immediately and runs it on skip", async () => {
    const registerPlacement = jest.fn().mockResolvedValue(undefined)
    const feature = jest.fn()
    let placementApi: ReturnType<typeof usePlacement> | undefined

    useSuperwallStore.setState({ registerPlacement })

    function Harness() {
      placementApi = usePlacement()
      return null
    }

    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(
        <SuperwallContext.Provider value={true}>
          <Harness />
        </SuperwallContext.Provider>,
      )
    })

    await act(async () => {
      await placementApi!.registerPlacement({
        placement: "test-placement",
        feature,
      })
    })

    expect(feature).not.toHaveBeenCalled()

    const handlerId = registerPlacement.mock.calls[0][2]

    act(() => {
      emit("onPaywallSkip", {
        skippedReason: { type: "NoAudienceMatch" },
        handlerId,
      })
    })

    expect(feature).toHaveBeenCalledTimes(1)
    act(() => {
      renderer!.unmount()
    })
  })

  it("runs the placement feature only for purchased/restored dismissals", async () => {
    const registerPlacement = jest.fn().mockResolvedValue(undefined)
    const feature = jest.fn()
    let placementApi: ReturnType<typeof usePlacement> | undefined

    useSuperwallStore.setState({ registerPlacement })

    function Harness() {
      placementApi = usePlacement()
      return null
    }

    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(
        <SuperwallContext.Provider value={true}>
          <Harness />
        </SuperwallContext.Provider>,
      )
    })

    await act(async () => {
      await placementApi!.registerPlacement({
        placement: "declined-placement",
        feature,
      })
    })

    let handlerId = registerPlacement.mock.calls[0][2]

    act(() => {
      emit("onPaywallDismiss", {
        paywallInfoJson: { name: "declined" },
        result: { type: "declined" },
        handlerId,
      })
    })

    expect(feature).not.toHaveBeenCalled()

    await act(async () => {
      await placementApi!.registerPlacement({
        placement: "purchased-placement",
        feature,
      })
    })

    handlerId = registerPlacement.mock.calls[1][2]

    act(() => {
      emit("onPaywallDismiss", {
        paywallInfoJson: { name: "purchased" },
        result: { type: "purchased", productId: "pro" },
        handlerId,
      })
    })

    expect(feature).toHaveBeenCalledTimes(1)

    await act(async () => {
      await placementApi!.registerPlacement({
        placement: "errored-placement",
        feature,
      })
    })

    handlerId = registerPlacement.mock.calls[2][2]

    act(() => {
      emit("onPaywallError", {
        errorString: "boom",
        handlerId,
      })
    })

    act(() => {
      emit("onPaywallDismiss", {
        paywallInfoJson: { name: "post-error" },
        result: { type: "restored" },
        handlerId,
      })
    })

    expect(feature).toHaveBeenCalledTimes(1)
    act(() => {
      renderer!.unmount()
    })
  })

  it("stores missing platform API key failures and reports them once", async () => {
    const onConfigurationError = jest.fn()
    const configure = jest.fn().mockResolvedValue(undefined)

    useSuperwallStore.setState({ configure })

    let renderer: TestRenderer.ReactTestRenderer
    await act(async () => {
      renderer = TestRenderer.create(
        <SuperwallProvider
          apiKeys={{ android: "android-key" }}
          onConfigurationError={onConfigurationError}
        >
          {null}
        </SuperwallProvider>,
      )

      await Promise.resolve()
    })

    expect(configure).not.toHaveBeenCalled()
    expect(useSuperwallStore.getState().configurationError).toBe(
      "No API key provided for platform ios",
    )
    expect(onConfigurationError).toHaveBeenCalledTimes(1)
    expect(onConfigurationError.mock.calls[0][0]).toBeInstanceOf(Error)
    expect(onConfigurationError.mock.calls[0][0].message).toBe(
      "No API key provided for platform ios",
    )

    act(() => {
      renderer!.unmount()
    })
  })
  it("configures once and does not immediately refresh after the first successful configuration", async () => {
    const configure = jest.fn().mockResolvedValue(undefined)

    useSuperwallStore.setState({ configure })

    let renderer: TestRenderer.ReactTestRenderer
    await act(async () => {
      renderer = TestRenderer.create(
        <SuperwallProvider apiKeys={{ ios: "ios-key" }}>{null}</SuperwallProvider>,
      )

      await Promise.resolve()
    })

    expect(configure).toHaveBeenCalledTimes(1)
    expect(mockRefreshConfiguration).not.toHaveBeenCalled()

    act(() => {
      useSuperwallStore.setState({
        isConfigured: true,
        isLoading: false,
        configurationError: null,
      })
    })

    expect(mockRefreshConfiguration).not.toHaveBeenCalled()

    act(() => {
      renderer!.unmount()
    })
  })

  it("refreshes configuration on a later dev rerun once configuration was already observed", async () => {
    useSuperwallStore.setState({
      isConfigured: true,
      isLoading: false,
      configurationError: null,
    })

    let renderer: TestRenderer.ReactTestRenderer
    await act(async () => {
      renderer = TestRenderer.create(
        <SuperwallProvider apiKeys={{ ios: "ios-key" }}>{null}</SuperwallProvider>,
      )

      await Promise.resolve()
    })

    expect(mockRefreshConfiguration).not.toHaveBeenCalled()

    act(() => {
      useSuperwallStore.setState({ isConfigured: false })
    })

    await act(async () => {
      useSuperwallStore.setState({
        isConfigured: true,
        isLoading: false,
        configurationError: null,
      })
      await Promise.resolve()
    })

    expect(mockRefreshConfiguration).toHaveBeenCalledTimes(1)

    act(() => {
      renderer!.unmount()
    })
  })

  it("does not refresh configuration outside development", async () => {
    ;(globalThis as { __DEV__?: boolean }).__DEV__ = false

    useSuperwallStore.setState({
      isConfigured: true,
      isLoading: false,
      configurationError: null,
    })

    let renderer: TestRenderer.ReactTestRenderer
    await act(async () => {
      renderer = TestRenderer.create(
        <SuperwallProvider apiKeys={{ ios: "ios-key" }}>{null}</SuperwallProvider>,
      )

      await Promise.resolve()
    })

    act(() => {
      useSuperwallStore.setState({ isConfigured: false })
    })

    await act(async () => {
      useSuperwallStore.setState({
        isConfigured: true,
        isLoading: false,
        configurationError: null,
      })
      await Promise.resolve()
    })

    expect(mockRefreshConfiguration).not.toHaveBeenCalled()

    act(() => {
      renderer!.unmount()
    })
  })

  it("catches refresh failures locally", async () => {
    const refreshError = new Error("refresh failed")
    mockRefreshConfiguration.mockRejectedValueOnce(refreshError)

    useSuperwallStore.setState({
      isConfigured: true,
      isLoading: false,
      configurationError: null,
    })

    let renderer: TestRenderer.ReactTestRenderer
    await act(async () => {
      renderer = TestRenderer.create(
        <SuperwallProvider apiKeys={{ ios: "ios-key" }}>{null}</SuperwallProvider>,
      )

      await Promise.resolve()
    })

    act(() => {
      useSuperwallStore.setState({ isConfigured: false })
    })

    await act(async () => {
      useSuperwallStore.setState({
        isConfigured: true,
        isLoading: false,
        configurationError: null,
      })
      await Promise.resolve()
    })

    await act(async () => {
      await Promise.resolve()
    })

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      "[Superwall] Failed to refresh configuration after Metro refresh",
      refreshError,
    )

    act(() => {
      renderer!.unmount()
    })
  })

  it("updates configuration error state from config refresh and fail events", async () => {
    useSuperwallStore.setState({
      isConfigured: true,
      isLoading: false,
      configurationError: "stale error",
    })

    let renderer: TestRenderer.ReactTestRenderer
    await act(async () => {
      renderer = TestRenderer.create(
        <SuperwallProvider apiKeys={{ ios: "ios-key" }}>{null}</SuperwallProvider>,
      )

      await Promise.resolve()
    })

    act(() => {
      emit("handleSuperwallEvent", {
        eventInfo: {
          event: { event: "configRefresh" },
          params: null,
        },
      })
    })

    expect(useSuperwallStore.getState().configurationError).toBeNull()

    act(() => {
      emit("handleSuperwallEvent", {
        eventInfo: {
          event: { event: "configFail" },
          params: null,
        },
      })
    })

    expect(useSuperwallStore.getState().configurationError).toBe(
      "Failed to load Superwall configuration",
    )

    act(() => {
      renderer!.unmount()
    })
  })

  it("replays buffered log and superwall events once after hooks mount", () => {
    const onLog = jest.fn()
    const onSuperwallEvent = jest.fn()

    emit("handleLog", {
      level: "debug",
      scope: "paywallPresentation",
      message: "buffered log",
      info: null,
      error: null,
    })
    emit("handleSuperwallEvent", {
      eventInfo: {
        event: { event: "appLaunch" },
        params: null,
      },
    })

    function Harness() {
      useSuperwallEvents({
        onLog,
        onSuperwallEvent,
      })
      return null
    }

    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(<Harness />)
    })

    expect(onLog).toHaveBeenCalledTimes(1)
    expect(onLog).toHaveBeenCalledWith({
      level: "debug",
      scope: "paywallPresentation",
      message: "buffered log",
      info: null,
      error: null,
    })
    expect(onSuperwallEvent).toHaveBeenCalledTimes(1)
    expect(onSuperwallEvent).toHaveBeenCalledWith({
      event: { event: "appLaunch" },
      params: null,
    })

    act(() => {
      renderer!.unmount()
    })

    const lateOnLog = jest.fn()
    const lateOnSuperwallEvent = jest.fn()

    function LateHarness() {
      useSuperwallEvents({
        onLog: lateOnLog,
        onSuperwallEvent: lateOnSuperwallEvent,
      })
      return null
    }

    act(() => {
      renderer = TestRenderer.create(<LateHarness />)
    })

    expect(lateOnLog).not.toHaveBeenCalled()
    expect(lateOnSuperwallEvent).not.toHaveBeenCalled()

    act(() => {
      renderer!.unmount()
    })
  })

  it("replays buffered scoped events only to the matching handler", () => {
    const matchingDismiss = jest.fn()
    const globalDismiss = jest.fn()

    emit("onPaywallDismiss", {
      paywallInfoJson: { name: "buffered-paywall" },
      result: { type: "declined" },
      handlerId: "placement-a",
    })

    function GlobalHarness() {
      useSuperwallEvents({
        onPaywallDismiss: globalDismiss,
      })
      return null
    }

    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(<GlobalHarness />)
    })

    expect(globalDismiss).not.toHaveBeenCalled()

    function MatchingHarness() {
      useSuperwallEvents({
        handlerId: "placement-a",
        onPaywallDismiss: matchingDismiss,
      })
      return null
    }

    act(() => {
      renderer.update(
        <>
          <GlobalHarness />
          <MatchingHarness />
        </>,
      )
    })

    expect(matchingDismiss).toHaveBeenCalledTimes(1)
    expect(matchingDismiss).toHaveBeenCalledWith({ name: "buffered-paywall" }, { type: "declined" })
    expect(globalDismiss).not.toHaveBeenCalled()

    act(() => {
      renderer!.unmount()
    })
  })

  it("does not buffer back press, custom callback, or purchase events", () => {
    emit("onBackPressed", {
      paywallInfo: { name: "buffered-back-press" },
    })
    emit("onCustomCallback", {
      callbackId: "callback-1",
      name: "validate",
      variables: { foo: "bar" },
      handlerId: "placement-a",
    })
    emit("onPurchase", {
      productId: "pro_monthly",
      platform: "ios",
    })

    const onBackPressed = jest.fn(() => true)
    const onCustomCallback = jest.fn(() => ({
      status: "success" as const,
      data: { ok: true },
    }))
    const onPurchase = jest.fn()

    function Harness() {
      useSuperwallEvents({
        handlerId: "placement-a",
        onBackPressed,
        onCustomCallback,
        onPurchase,
      })
      return null
    }

    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(<Harness />)
    })

    expect(onBackPressed).not.toHaveBeenCalled()
    expect(onCustomCallback).not.toHaveBeenCalled()
    expect(onPurchase).not.toHaveBeenCalled()
    expect(mockDidHandleBackPressed).toHaveBeenCalledWith(false)
    expect(mockDidHandleCustomCallback).toHaveBeenCalledWith("callback-1", "failure", undefined)

    act(() => {
      renderer!.unmount()
    })
  })
})

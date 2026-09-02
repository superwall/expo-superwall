import TestRenderer, { act } from "react-test-renderer"

const mockListeners = new Map<string, Set<(payload: any) => void>>()
const mockHandleDeepLink = jest.fn().mockResolvedValue(false)
const mockDidHandleBackPressed = jest.fn()
const mockDidHandleCustomCallback = jest.fn().mockResolvedValue(undefined)
const mockConfigure = jest.fn().mockResolvedValue(true)
const mockIdentify = jest.fn().mockResolvedValue(undefined)
const mockGetUserAttributes = jest.fn().mockResolvedValue({})
const mockGetSubscriptionStatus = jest.fn().mockResolvedValue({ status: "INACTIVE" })
const mockGetCustomerInfo = jest.fn().mockResolvedValue({
  userId: "",
  subscriptions: [],
  nonSubscriptions: [],
  entitlements: [],
})
const mockSetSubscriptionStatus = jest.fn().mockResolvedValue(undefined)
const mockSetIntegrationAttributes = jest.fn().mockResolvedValue(undefined)
const mockTogglePaywallSpinner = jest.fn()
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
  for (const listener of listeners) {
    listener(payload)
  }
}

// Mocked so the suite doesn't load the real ESM module, which jest can't
// require on Node < 24.9. Nothing here exercises real asset resolution.
jest.mock("expo-asset", () => ({
  Asset: {
    fromModule: jest.fn(() => ({ localUri: "file:///mock", uri: "file:///mock" })),
  },
}))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: mockAddListener,
    configure: mockConfigure,
    identify: mockIdentify,
    getUserAttributes: mockGetUserAttributes,
    getSubscriptionStatus: mockGetSubscriptionStatus,
    getCustomerInfo: mockGetCustomerInfo,
    setSubscriptionStatus: mockSetSubscriptionStatus,
    setIntegrationAttributes: mockSetIntegrationAttributes,
    handleDeepLink: mockHandleDeepLink,
    didHandleBackPressed: mockDidHandleBackPressed,
    didHandleCustomCallback: mockDidHandleCustomCallback,
    togglePaywallSpinner: mockTogglePaywallSpinner,
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

const originalConfigure = useSuperwallStore.getState().configure

describe("SDK behavior regressions", () => {
  let consoleErrorSpy: jest.SpyInstance
  let consoleLogSpy: jest.SpyInstance

  beforeEach(() => {
    jest.clearAllMocks()
    __resetSuperwallEventBridgeForTests()
    consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {})
    consoleLogSpy = jest.spyOn(console, "log").mockImplementation(() => {})
    useSuperwallStore.setState({
      isConfigured: false,
      isLoading: false,
      listenersInitialized: false,
      configurationError: null,
      user: null,
      subscriptionStatus: { status: "UNKNOWN" },
      customerInfo: null,
      configure: originalConfigure,
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

  it("runs the placement feature when the native register promise resolves (Non-Gated dismiss, skip, purchased)", async () => {
    // Mirror native semantics: the promise only resolves when the SDK invokes
    // its feature closure — i.e. when access should be granted. Gated declines
    // never resolve the promise.
    let resolveRegister: (() => void) | undefined
    const registerPlacement = jest.fn(
      () =>
        new Promise<void>((resolve) => {
          resolveRegister = resolve
        }),
    )
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

    // Non-Gated dismiss (regression for #185): native fires feature closure on
    // dismiss-without-purchase, so the promise resolves and feature must run.
    let pending = act(async () => {
      await placementApi!.registerPlacement({
        placement: "non-gated-placement",
        feature,
      })
    })

    expect(feature).not.toHaveBeenCalled()
    act(() => {
      resolveRegister?.()
    })
    await pending
    expect(feature).toHaveBeenCalledTimes(1)

    // Skip: native also resolves the promise via its feature closure.
    pending = act(async () => {
      await placementApi!.registerPlacement({
        placement: "skipped-placement",
        feature,
      })
    })
    act(() => {
      resolveRegister?.()
    })
    await pending
    expect(feature).toHaveBeenCalledTimes(2)

    // Gated decline: native never invokes the feature closure, so the promise
    // never resolves and feature never runs. We assert by ticking the event
    // loop without resolving the pending promise.
    let unresolved = true
    placementApi!
      .registerPlacement({
        placement: "gated-placement",
        feature,
      })
      .then(() => {
        unresolved = false
      })
    await act(async () => {
      await Promise.resolve()
    })
    expect(unresolved).toBe(true)
    expect(feature).toHaveBeenCalledTimes(2)

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

  it("waits for configure before setting subscription status", async () => {
    let resolveConfigure: ((value: boolean) => void) | undefined
    mockConfigure.mockReturnValueOnce(
      new Promise<boolean>((resolve) => {
        resolveConfigure = resolve
      }),
    )

    const pendingSet = useSuperwallStore.getState().setSubscriptionStatus({ status: "INACTIVE" })

    const pendingConfigure = useSuperwallStore.getState().configure("api-key")

    await act(async () => {
      await Promise.resolve()
    })

    expect(mockSetSubscriptionStatus).not.toHaveBeenCalled()

    await act(async () => {
      resolveConfigure?.(true)
      await pendingConfigure
      await pendingSet
    })

    expect(mockSetSubscriptionStatus).toHaveBeenCalledWith({ status: "INACTIVE" })
  })

  it("waits for configure before setting integration attributes", async () => {
    let resolveConfigure: ((value: boolean) => void) | undefined
    mockConfigure.mockReturnValueOnce(
      new Promise<boolean>((resolve) => {
        resolveConfigure = resolve
      }),
    )

    const pendingAttributes = useSuperwallStore
      .getState()
      .setIntegrationAttributes({ adjustId: "adjust-123" })

    const pendingConfigure = useSuperwallStore.getState().configure("api-key")

    await act(async () => {
      await Promise.resolve()
    })

    expect(mockSetIntegrationAttributes).not.toHaveBeenCalled()

    await act(async () => {
      resolveConfigure?.(true)
      await pendingConfigure
      await pendingAttributes
    })

    expect(mockSetIntegrationAttributes).toHaveBeenCalledWith({ adjustId: "adjust-123" })
  })

  it("waits for configure before toggling the paywall spinner", async () => {
    let resolveConfigure: ((value: boolean) => void) | undefined
    mockConfigure.mockReturnValueOnce(
      new Promise<boolean>((resolve) => {
        resolveConfigure = resolve
      }),
    )

    const pendingToggle = useSuperwallStore.getState().togglePaywallSpinner(false)

    const pendingConfigure = useSuperwallStore.getState().configure("api-key")

    await act(async () => {
      await Promise.resolve()
    })

    expect(mockTogglePaywallSpinner).not.toHaveBeenCalled()

    await act(async () => {
      resolveConfigure?.(true)
      await pendingConfigure
      await pendingToggle
    })

    expect(mockTogglePaywallSpinner).toHaveBeenCalledWith(false)

    await act(async () => {
      await useSuperwallStore.getState().togglePaywallSpinner(true)
    })

    expect(mockTogglePaywallSpinner).toHaveBeenLastCalledWith(true)
  })

  it("seeds customerInfo after configure and applies customerInfoDidChange updates", async () => {
    const seeded = {
      userId: "user-1",
      subscriptions: [],
      nonSubscriptions: [],
      entitlements: [],
    }
    mockGetCustomerInfo.mockResolvedValueOnce(seeded)

    const cleanupListeners = useSuperwallStore.getState()._initListeners()

    await act(async () => {
      await useSuperwallStore.getState().configure("api-key")
      // Let the non-blocking customer info seed settle.
      await Promise.resolve()
      await Promise.resolve()
    })

    expect(mockGetCustomerInfo).toHaveBeenCalledTimes(1)
    expect(useSuperwallStore.getState().customerInfo).toEqual(seeded)

    const updated = {
      userId: "user-1",
      subscriptions: [
        {
          transactionId: "txn-1",
          productId: "premium_annual",
          purchaseDate: "2026-09-06T12:00:00Z",
          willRenew: true,
          isRevoked: false,
          isInGracePeriod: false,
          isInBillingRetryPeriod: false,
          isActive: true,
          expirationDate: "2027-09-06T12:00:00Z",
          store: "APP_STORE",
        },
      ],
      nonSubscriptions: [],
      entitlements: [{ id: "premium", type: "SERVICE_LEVEL" }],
    }

    act(() => {
      emit("customerInfoDidChange", { from: seeded, to: updated })
    })

    expect(useSuperwallStore.getState().customerInfo).toEqual(updated)

    cleanupListeners()
  })

  it("clears customerInfo during identify and reseeds it for the new identity", async () => {
    const previous = {
      userId: "user-a",
      subscriptions: [],
      nonSubscriptions: [],
      entitlements: [{ id: "premium", type: "SERVICE_LEVEL" }],
    }
    useSuperwallStore.setState({ isConfigured: true, customerInfo: previous })

    const reseeded = {
      userId: "user-b",
      subscriptions: [],
      nonSubscriptions: [],
      entitlements: [],
    }
    let resolveFetch: ((value: unknown) => void) | undefined
    mockGetCustomerInfo.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveFetch = resolve
      }),
    )

    const pendingIdentify = useSuperwallStore.getState().identify("user-b")

    await act(async () => {
      await Promise.resolve()
    })

    // The previous identity's snapshot must be gone for the whole transition.
    expect(useSuperwallStore.getState().customerInfo).toBeNull()

    await act(async () => {
      await pendingIdentify
    })

    // Still null: identify resolved but the reseed fetch is in flight.
    expect(useSuperwallStore.getState().customerInfo).toBeNull()

    await act(async () => {
      resolveFetch?.(reseeded)
      await Promise.resolve()
      await Promise.resolve()
    })

    expect(useSuperwallStore.getState().customerInfo).toEqual(reseeded)
  })

  it("routes customerInfoDidChange through useSuperwallEvents with from/to snapshots", () => {
    const onCustomerInfoChange = jest.fn()
    const from = {
      userId: "user-1",
      subscriptions: [],
      nonSubscriptions: [],
      entitlements: [],
    }
    const to = {
      ...from,
      entitlements: [{ id: "premium", type: "SERVICE_LEVEL" }],
    }

    // Emitted before any subscriber mounts, so it should buffer and replay.
    emit("customerInfoDidChange", { from, to })

    function Harness() {
      useSuperwallEvents({ onCustomerInfoChange })
      return null
    }

    let renderer: TestRenderer.ReactTestRenderer
    act(() => {
      renderer = TestRenderer.create(<Harness />)
    })

    expect(onCustomerInfoChange).toHaveBeenCalledTimes(1)
    expect(onCustomerInfoChange).toHaveBeenCalledWith(from, to)

    act(() => {
      emit("customerInfoDidChange", { from: to, to: from })
    })

    expect(onCustomerInfoChange).toHaveBeenCalledTimes(2)
    expect(onCustomerInfoChange).toHaveBeenLastCalledWith(to, from)

    act(() => {
      renderer!.unmount()
    })
  })

  it("rejects queued native calls when configure fails", async () => {
    mockConfigure.mockRejectedValueOnce(new Error("native configure failed"))

    const pendingSet = useSuperwallStore.getState().setSubscriptionStatus({ status: "INACTIVE" })

    const pendingConfigure = useSuperwallStore.getState().configure("api-key")

    await expect(pendingConfigure).resolves.toBeUndefined()
    await expect(pendingSet).rejects.toThrow("native configure failed")
    expect(mockSetSubscriptionStatus).not.toHaveBeenCalled()
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

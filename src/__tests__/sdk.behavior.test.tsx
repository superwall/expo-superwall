import React from "react"
import TestRenderer, { act } from "react-test-renderer"

const mockListeners = new Map<string, Set<(payload: any) => void>>()
const mockHandleDeepLink = jest.fn().mockResolvedValue(false)
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
  listeners.forEach((listener) => listener(payload))
}

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: mockAddListener,
    handleDeepLink: mockHandleDeepLink,
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
const { SuperwallContext, useSuperwallStore }: typeof import("../useSuperwall") = require("../useSuperwall")
const { usePlacement }: typeof import("../usePlacement") = require("../usePlacement")
const { useSuperwallEvents }: typeof import("../useSuperwallEvents") =
  require("../useSuperwallEvents")

describe("SDK behavior regressions", () => {
  let consoleErrorSpy: jest.SpyInstance
  let consoleLogSpy: jest.SpyInstance

  beforeEach(() => {
    mockListeners.clear()
    jest.clearAllMocks()
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
        <SuperwallProvider apiKeys={{ android: "android-key" }} onConfigurationError={onConfigurationError}>
          <></>
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
})

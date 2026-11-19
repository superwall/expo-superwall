const mockListeners = new Map<string, Set<(payload: any) => void>>()
const mockModule: Record<string, jest.Mock> = {
  addListener: jest.fn((eventName: string, listener: (payload: any) => void) => {
    const listeners = mockListeners.get(eventName) ?? new Set()
    listeners.add(listener)
    mockListeners.set(eventName, listeners)
    return { remove: () => listeners.delete(listener) }
  }),
  purchase: jest.fn().mockResolvedValue({ type: "purchased" }),
  getUserAttributes: jest.fn().mockResolvedValue({}),
  setIntegrationAttribute: jest.fn().mockResolvedValue(undefined),
  getGrantedEntitlements: jest.fn().mockResolvedValue([{ id: "pro" }]),
  setGrantedEntitlements: jest.fn().mockResolvedValue(undefined),
  showAlert: jest.fn().mockResolvedValue(true),
  openRestoreOnWeb: jest.fn(),
  getExternalAccountId: jest.fn().mockResolvedValue(undefined),
  observe: jest.fn().mockResolvedValue(undefined),
  teardown: jest.fn().mockResolvedValue(true),
  dismissForNextPaywall: jest.fn().mockResolvedValue(undefined),
  refreshCustomerInfo: jest.fn().mockResolvedValue({ userId: "user", entitlements: [] }),
  removeAllPendingNonSuperwallNotificationRequests: jest.fn(),
  removeAllDeliveredSuperwallNotifications: jest.fn(),
  removeAllDeliveredNonSuperwallNotifications: jest.fn(),
}

const emit = (eventName: string, payload: any) => {
  for (const listener of mockListeners.get(eventName) ?? []) {
    listener(payload)
  }
}

// Mocked so the suite doesn't load the real ESM module, which jest can't require.
jest.mock("expo-asset", () => ({ Asset: { fromModule: jest.fn() } }))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: mockModule,
}))

const { useSuperwallStore }: typeof import("../useSuperwall") = require("../useSuperwall")
const {
  __resetSuperwallEventBridgeForTests,
  subscribeToSuperwallEvents,
}: typeof import("../internal/superwallEventBridge") = require("../internal/superwallEventBridge")
const {
  productIdentifierForPurchase,
}: typeof import("../utils/productIdentifier") = require("../utils/productIdentifier")

describe("hooks native helpers", () => {
  beforeEach(() => {
    jest.clearAllMocks()
    __resetSuperwallEventBridgeForTests()
    useSuperwallStore.setState({ isConfigured: true, configurationError: null })
  })

  it("purchases a product object by its full identifier, and an ID as it is", async () => {
    const store = useSuperwallStore.getState()

    await store.purchase("monthly")
    expect(mockModule.purchase).toHaveBeenLastCalledWith("monthly")

    await store.purchase({
      productIdentifier: "pro",
      fullIdentifier: "pro:monthly:intro",
    } as never)
    expect(mockModule.purchase).toHaveBeenLastCalledWith("pro:monthly:intro")

    // iOS products have no full identifier.
    expect(productIdentifierForPurchase({ productIdentifier: "pro" })).toBe("pro")
  })

  it("sets a single integration attribute and granted entitlements", async () => {
    const store = useSuperwallStore.getState()

    await store.setIntegrationAttribute("adjustId", null)
    expect(mockModule.setIntegrationAttribute).toHaveBeenCalledWith("adjustId", null)

    await store.setGrantedEntitlements(["pro", { id: "gold", isActive: true } as never])
    expect(mockModule.setGrantedEntitlements).toHaveBeenCalledWith([{ id: "pro" }, { id: "gold" }])
    await expect(store.getGrantedEntitlements()).resolves.toEqual([{ id: "pro" }])
  })

  it("passes the platform-only helpers through", async () => {
    const store = useSuperwallStore.getState()

    await expect(store.showAlert({ title: "Hi", actionTitle: "OK" })).resolves.toBe(true)
    expect(mockModule.showAlert).toHaveBeenCalledWith("Hi", null, "OK", null)

    await store.openRestoreOnWeb()
    expect(mockModule.openRestoreOnWeb).toHaveBeenCalled()
    await expect(store.getExternalAccountId()).resolves.toBeNull()

    await store.observe({ type: "purchaseResult", responseCode: 0, debugMessage: undefined })
    expect(mockModule.observe).toHaveBeenCalledWith({ type: "purchaseResult", responseCode: 0 })

    await store.observe({
      type: "purchaseResult",
      responseCode: 0,
      purchases: [{ originalJson: "{}", signature: undefined }],
    })
    expect(mockModule.observe).toHaveBeenLastCalledWith({
      type: "purchaseResult",
      responseCode: 0,
      purchases: [{ originalJson: "{}" }],
    })

    await store.dismissForNextPaywall()
    expect(mockModule.dismissForNextPaywall).toHaveBeenCalled()

    await store.removeAllPendingNonSuperwallNotificationRequests()
    await store.removeAllDeliveredSuperwallNotifications()
    await store.removeAllDeliveredNonSuperwallNotifications()
    expect(mockModule.removeAllPendingNonSuperwallNotificationRequests).toHaveBeenCalled()
    expect(mockModule.removeAllDeliveredSuperwallNotifications).toHaveBeenCalled()
    expect(mockModule.removeAllDeliveredNonSuperwallNotifications).toHaveBeenCalled()
  })

  it("stores refreshed customer info", async () => {
    await useSuperwallStore.getState().refreshCustomerInfo()
    expect(useSuperwallStore.getState().customerInfo).toEqual({ userId: "user", entitlements: [] })
  })

  it("goes back to not configured after a teardown, but not when nothing was torn down", async () => {
    mockModule.teardown!.mockResolvedValueOnce(false)
    await expect(useSuperwallStore.getState().teardown()).resolves.toBe(false)
    expect(useSuperwallStore.getState().isConfigured).toBe(true)

    await expect(useSuperwallStore.getState().teardown()).resolves.toBe(true)
    expect(useSuperwallStore.getState()).toMatchObject({
      isConfigured: false,
      isTornDown: true,
      configurationStatus: "PENDING",
      customerInfo: null,
    })
  })

  it("answers false straight away when there is nothing left to tear down", async () => {
    await expect(useSuperwallStore.getState().teardown()).resolves.toBe(true)
    mockModule.teardown!.mockClear()

    // Not a ten-second wait for a configure that isn't coming, ending in a rejection.
    await expect(useSuperwallStore.getState().teardown()).resolves.toBe(false)
    expect(mockModule.teardown).not.toHaveBeenCalled()
  })

  it("keeps undefined out of the options sent to the native module, however deeply nested", async () => {
    mockModule.configure = jest.fn().mockResolvedValue(undefined)
    mockModule.getSubscriptionStatus = jest.fn().mockResolvedValue({ status: "UNKNOWN" })
    useSuperwallStore.setState({ isConfigured: false })

    await useSuperwallStore.getState().configure("api-key", {
      logging: { level: undefined, scopes: ["all"] },
      paywalls: {
        shouldPreload: undefined,
        preloadDeviceOverrides: { low: false, mid: undefined },
        restoreFailed: { title: "Nothing to restore", message: undefined },
      },
    } as never)

    const options = mockModule.configure.mock.calls[0][1]
    const undefinedPaths: string[] = []
    const walk = (value: unknown, path: string) => {
      if (value === undefined) undefinedPaths.push(path)
      else if (value && typeof value === "object") {
        for (const [key, child] of Object.entries(value)) walk(child, `${path}.${key}`)
      }
    }
    walk(options, "options")
    expect(undefinedPaths).toEqual([])

    // What was set is kept, and what was left undefined falls back to the default.
    expect(options.paywalls.preloadDeviceOverrides).toStrictEqual({ low: false })
    expect(options.paywalls.restoreFailed.title).toBe("Nothing to restore")
    expect(typeof options.paywalls.restoreFailed.message).toBe("string")
    expect(options.paywalls.shouldPreload).toBe(true)
    expect(typeof options.logging.level).toBe("string")
    expect(options.logging.scopes).toEqual(["all"])
  })

  it("tracks the configuration status in the store and tells subscribers", () => {
    const cleanup = useSuperwallStore.getState()._initListeners()
    const onConfigurationStatusChange = jest.fn()
    subscribeToSuperwallEvents(undefined, () => ({ onConfigurationStatusChange }))

    emit("configurationStatusDidChange", { status: "FAILED" })

    expect(useSuperwallStore.getState().configurationStatus).toBe("FAILED")
    expect(onConfigurationStatusChange).toHaveBeenCalledWith("FAILED")
    cleanup()
  })
})

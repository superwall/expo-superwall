const mockListeners = new Map<string, Set<(payload: any) => void>>()
const mockModule: Record<string, jest.Mock> = {
  addListener: jest.fn((eventName: string, listener: (payload: any) => void) => {
    const listeners = mockListeners.get(eventName) ?? new Set()
    listeners.add(listener)
    mockListeners.set(eventName, listeners)
    return { remove: () => listeners.delete(listener) }
  }),
  getEntitlementsByProductIds: jest.fn().mockResolvedValue([{ id: "pro", type: "SERVICE_LEVEL" }]),
  getAssignments: jest.fn().mockResolvedValue([]),
  confirmAllAssignments: jest.fn().mockResolvedValue([]),
  setInterfaceStyle: jest.fn(),
  getConfigurationStatus: jest.fn().mockResolvedValue("CONFIGURED"),
  handleDeepLink: jest.fn().mockResolvedValue(true),
  refreshConfiguration: jest.fn().mockResolvedValue(undefined),
  getLatestPaywallInfo: jest.fn().mockResolvedValue(null),
  getLocaleIdentifier: jest.fn().mockResolvedValue(null),
  setIntegrationAttributes: jest.fn().mockResolvedValue(undefined),
  getUserAttributes: jest.fn().mockResolvedValue({}),
  setOverrideProductsByName: jest.fn(),
  setPushToken: jest.fn(),
  optOutOfNotifications: jest.fn(),
  handleNotificationOpen: jest.fn(),
  cancelAllScheduledNotifications: jest.fn(),
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

describe("hooks API parity", () => {
  beforeEach(() => {
    jest.clearAllMocks()
    __resetSuperwallEventBridgeForTests()
    useSuperwallStore.setState({ isConfigured: true, configurationError: null })
  })

  it("passes the new store methods through to the native module", async () => {
    const store = useSuperwallStore.getState()

    await expect(store.getEntitlementsByProductIds(["monthly"])).resolves.toEqual([
      { id: "pro", type: "SERVICE_LEVEL" },
    ])
    expect(mockModule.getEntitlementsByProductIds).toHaveBeenCalledWith(["monthly"])

    await store.getAssignments()
    await store.confirmAllAssignments()
    expect(mockModule.getAssignments).toHaveBeenCalled()
    expect(mockModule.confirmAllAssignments).toHaveBeenCalled()

    await store.setInterfaceStyle("DARK")
    await store.setInterfaceStyle(null)
    expect(mockModule.setInterfaceStyle).toHaveBeenNthCalledWith(1, "DARK")
    expect(mockModule.setInterfaceStyle).toHaveBeenNthCalledWith(2, undefined)

    await expect(store.handleDeepLink("myapp://superwall")).resolves.toBe(true)
    await store.refreshConfiguration()
    expect(mockModule.refreshConfiguration).toHaveBeenCalled()

    await expect(store.getLatestPaywallInfo()).resolves.toBeNull()
    await expect(store.getLocaleIdentifier()).resolves.toBeNull()

    await store.setOverrideProductsByName({ primary: "yearly" })
    expect(mockModule.setOverrideProductsByName).toHaveBeenCalledWith({ primary: "yearly" })

    await store.setPushToken("fcm-token")
    await store.optOutOfNotifications()
    await store.handleNotificationOpen({ sw: "payload" })
    await store.cancelAllScheduledNotifications()
    expect(mockModule.setPushToken).toHaveBeenCalledWith("fcm-token")
    expect(mockModule.optOutOfNotifications).toHaveBeenCalled()
    expect(mockModule.handleNotificationOpen).toHaveBeenCalledWith({ sw: "payload" })
    expect(mockModule.cancelAllScheduledNotifications).toHaveBeenCalled()
  })

  it("reads the configuration status without waiting for configure", async () => {
    useSuperwallStore.setState({ isConfigured: false })
    mockModule.getConfigurationStatus!.mockResolvedValueOnce("PENDING")

    await expect(useSuperwallStore.getState().getConfigurationStatus()).resolves.toBe("PENDING")
  })

  it("passes null integration attributes through so native removes them", async () => {
    await useSuperwallStore.getState().setIntegrationAttributes({ adjustId: null, meta: "meta-id" })

    expect(mockModule.setIntegrationAttributes).toHaveBeenCalledWith({
      adjustId: null,
      meta: "meta-id",
    })
  })

  it("delivers user attribute changes and Superwall deep links to every subscriber", () => {
    const onUserAttributesChange = jest.fn()
    const onSuperwallDeepLink = jest.fn()
    subscribeToSuperwallEvents(undefined, () => ({ onUserAttributesChange, onSuperwallDeepLink }))

    emit("userAttributesDidChange", { newAttributes: { name: "Jane" } })
    const deepLink = {
      fullURL: "myapp://superwall/paywall?id=1",
      pathComponents: ["paywall"],
      queryParameters: { id: "1" },
    }
    emit("handleSuperwallDeepLink", deepLink)

    expect(onUserAttributesChange).toHaveBeenCalledWith({ name: "Jane" })
    expect(onSuperwallDeepLink).toHaveBeenCalledWith(deepLink)
  })

  it("scopes onPaywallWillDismiss to the registering handler", () => {
    const mine = jest.fn()
    const theirs = jest.fn()
    subscribeToSuperwallEvents("mine", () => ({ onPaywallWillDismiss: mine }))
    subscribeToSuperwallEvents("theirs", () => ({ onPaywallWillDismiss: theirs }))

    const paywallInfo = { identifier: "paywall" }
    emit("onPaywallWillDismiss", {
      paywallInfoJson: paywallInfo,
      result: { type: "purchased", productId: "monthly" },
      handlerId: "mine",
    })

    expect(mine).toHaveBeenCalledWith(paywallInfo, { type: "purchased", productId: "monthly" })
    expect(theirs).not.toHaveBeenCalled()
  })
})

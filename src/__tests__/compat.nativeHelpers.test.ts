const mockConfigure = jest.fn()
const mockPurchase = jest.fn().mockResolvedValue({ type: "purchased" })
const mockProducts = jest.fn().mockResolvedValue([])
const mockTeardown = jest.fn().mockResolvedValue(true)
const mockSetGrantedEntitlements = jest.fn().mockResolvedValue(undefined)
const mockShowAlert = jest.fn().mockResolvedValue(false)
const mockListeners: Record<string, ((data: any) => unknown)[]> = {}

// The compat configuration gate only needs an in-process event emitter.
jest.mock("expo", () => ({ EventEmitter: require("node:events").EventEmitter }))
// The compat SDK resolves local resources through `expo-asset`, whose ESM build jest can't require.
jest.mock("expo-asset", () => ({ Asset: { fromModule: jest.fn() } }))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: jest.fn((name: string, listener: (data: any) => unknown) => {
      if (!mockListeners[name]) mockListeners[name] = []
      mockListeners[name].push(listener)
      return { remove: jest.fn() }
    }),
    configure: mockConfigure,
    purchase: mockPurchase,
    products: mockProducts,
    teardown: mockTeardown,
    setGrantedEntitlements: mockSetGrantedEntitlements,
    showAlert: mockShowAlert,
  },
}))

const {
  default: Superwall,
  ConfigurationStatus,
  NetworkEnvironment,
  PaywallView,
  CustomerCenterView,
  RedemptionResults,
  StoreProduct,
  SuperwallOptions,
}: typeof import("../compat") = require("../compat")

const flush = () => new Promise((resolve) => setImmediate(resolve))

describe("compat native helpers", () => {
  it("waits for configure before the static purchase and products calls", async () => {
    let finishConfigure: () => void = () => {}
    mockConfigure.mockReturnValueOnce(
      new Promise<void>((resolve) => {
        finishConfigure = resolve
      }),
    )

    const purchase = Superwall.purchase(
      StoreProduct.fromJson({ productIdentifier: "pro", fullIdentifier: "pro:monthly" }),
    )
    const products = Superwall.products(["pro"])
    const configured = Superwall.configure({ apiKey: "api-key" })
    await flush()
    expect(mockPurchase).not.toHaveBeenCalled()
    expect(mockProducts).not.toHaveBeenCalled()

    finishConfigure()
    await configured
    await purchase
    await products
    expect(mockPurchase).toHaveBeenCalledWith("pro:monthly")
    expect(mockProducts).toHaveBeenCalledWith(["pro"])
  })

  it("tells configuration status listeners until they're removed", async () => {
    const listener = jest.fn()
    const subscription = Superwall.shared.addConfigurationStatusListener(listener)

    for (const nativeListener of mockListeners.configurationStatusDidChange ?? []) {
      await nativeListener({ status: "CONFIGURED" })
    }
    expect(listener).toHaveBeenCalledWith(ConfigurationStatus.CONFIGURED)

    subscription.remove()
    for (const nativeListener of mockListeners.configurationStatusDidChange ?? []) {
      await nativeListener({ status: "FAILED" })
    }
    expect(listener).toHaveBeenCalledTimes(1)
  })

  it("sends granted entitlements as IDs and alert options in order", async () => {
    await Superwall.shared.setGrantedEntitlements(["pro", { id: "gold" }])
    expect(mockSetGrantedEntitlements).toHaveBeenCalledWith([{ id: "pro" }, { id: "gold" }])

    await expect(Superwall.shared.showAlert({ message: "Sure?" })).resolves.toBe(false)
    expect(mockShowAlert).toHaveBeenCalledWith(null, "Sure?", null, null)
  })

  it("serializes the new options", () => {
    const json = new SuperwallOptions({
      networkEnvironment: { custom: "example.com", port: undefined },
      devServer: "http://192.168.1.10:6100",
      paywalls: { preloadDeviceOverrides: { low: false }, loadingColor: "paywall_loading" },
    }).toJson() as any

    expect(json.networkEnvironment).toEqual({ custom: "example.com" })
    expect(json.devServer).toBe("http://192.168.1.10:6100")
    expect(json.paywalls.preloadDeviceOverrides).toEqual({ low: false })
    expect(json.paywalls.loadingColor).toBe("paywall_loading")

    const defaults = new SuperwallOptions({ networkEnvironment: NetworkEnvironment.Local })
    expect((defaults.toJson() as any).networkEnvironment).toBe("local")
    expect(defaults.toJson()).not.toHaveProperty("devServer")
  })

  it("keeps the top-level Stripe subscription IDs of a redemption", () => {
    const redemptionInfo = {
      ownership: { type: "APP_USER", appUserId: "user" },
      purchaserInfo: {
        appUserId: "user",
        storeIdentifiers: {
          store: "STRIPE",
          stripeCustomerId: "cus",
          stripeSubscriptionIds: ["sub"],
        },
      },
      entitlements: [],
    }
    expect(
      RedemptionResults.fromJson({
        status: "SUCCESS",
        code: "code",
        redemptionInfo,
        stripeSubscriptionIds: ["sub"],
      }),
    ).toMatchObject({ status: "SUCCESS", stripeSubscriptionIds: ["sub"] })
    expect(
      RedemptionResults.fromJson({ status: "SUCCESS", code: "code", redemptionInfo }),
    ).not.toHaveProperty("stripeSubscriptionIds")
  })

  it("exports the embedded views", () => {
    expect(typeof PaywallView).toBe("function")
    expect(typeof CustomerCenterView).toBe("function")
  })

  // Last: it leaves the SDK not configured.
  it("waits for configure again after a teardown", async () => {
    await expect(Superwall.teardown()).resolves.toBe(true)
    expect(mockTeardown).toHaveBeenCalledTimes(1)

    mockPurchase.mockClear()
    void Superwall.purchase("pro")
    await flush()
    expect(mockPurchase).not.toHaveBeenCalled()

    // Nothing to tear down now.
    await expect(Superwall.teardown()).resolves.toBe(false)
    expect(mockTeardown).toHaveBeenCalledTimes(1)
  })
})

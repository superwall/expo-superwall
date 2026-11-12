const mockConfigure = jest.fn().mockResolvedValue(undefined)
const mockSetSubscriptionStatus = jest.fn().mockResolvedValue(undefined)
const mockGetEntitlementsByProductIds = jest.fn()
const mockGetLatestPaywallInfo = jest.fn()
const mockGetLogLevel = jest.fn()
const mockDidPurchase = jest.fn().mockResolvedValue(undefined)
const mockListeners: Record<string, ((data: any) => unknown)[]> = {}

// The compat configuration gate only needs an in-process event emitter.
jest.mock("expo", () => ({ EventEmitter: require("node:events").EventEmitter }))

// Mocked so the suite doesn't load the real ESM module, which jest can't require.
jest.mock("expo-asset", () => ({
  Asset: {
    fromModule: jest.fn(() => ({ localUri: "file:///bundle/hero.png", uri: "asset:/hero" })),
  },
}))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: jest.fn((name: string, listener: (data: any) => unknown) => {
      if (!mockListeners[name]) mockListeners[name] = []
      mockListeners[name].push(listener)
      return { remove: jest.fn() }
    }),
    configure: mockConfigure,
    setSubscriptionStatus: mockSetSubscriptionStatus,
    getEntitlementsByProductIds: mockGetEntitlementsByProductIds,
    getLatestPaywallInfo: mockGetLatestPaywallInfo,
    getLogLevel: mockGetLogLevel,
    didPurchase: mockDidPurchase,
  },
}))

const {
  default: Superwall,
  Entitlement,
  EntitlementsInfo,
  EventType,
  LogLevel,
  PaywallInfo,
  PaywallOptions,
  PaywallPresentationHandler,
  PurchaseController,
  PurchaseResultPurchased,
  StoreProduct,
  StoreTransaction,
  SubscriptionStatus,
  SuperwallDelegate,
  SuperwallEventInfo,
}: typeof import("../compat") = require("../compat")

async function emit(name: string, data: any) {
  await Promise.all((mockListeners[name] ?? []).map((listener) => listener(data)))
}

const entitlementJson = {
  id: "pro",
  type: "SERVICE_LEVEL",
  isActive: true,
  productIds: ["monthly", "yearly"],
  latestProductId: "monthly",
  store: "APP_STORE",
  startsAt: "2026-01-01T00:00:00Z",
  expiresAt: "2026-12-01T00:00:00Z",
  willRenew: true,
  renewsAt: "2026-12-01T00:00:00Z",
  isInGracePeriod: false,
  state: "subscribed",
  offerType: "trial",
}

const paywallInfoJson = {
  identifier: "paywall",
  name: "Paywall",
  url: "https://example.com",
  products: [],
  productIds: [],
  presentedBy: "register",
  isFreeTrialAvailable: false,
  featureGatingBehavior: "nonGated",
  closeReason: "none",
  computedPropertyRequests: [],
  surveys: [],
  localNotifications: [],
  state: {},
  cacheKey: "cache-key",
  buildId: "build",
  shimmerLoadStartTime: "2026-11-12T10:00:00.000Z",
  presentation: { style: "DRAWER", delay: 300, height: 70, cornerRadius: 16 },
  isScrollEnabled: true,
  isLocal: false,
  introOfferEligibility: "automatic",
}

const productJson = {
  productIdentifier: "monthly",
  localizedPrice: "$9.99",
  price: 9.99,
  subscriptionPeriod: { value: 1, unit: "month" },
  discounts: [],
  billingPlanType: "MONTHLY",
  isBillingPlanAvailable: true,
  entitlements: [entitlementJson],
  attributes: { price: "$9.99" },
  productType: "subs",
  storeFrontCountryCode: "US",
  isCustomProduct: false,
  customTransactionId: null,
}

describe("compat API parity", () => {
  beforeAll(async () => {
    await Superwall.configure({ apiKey: "api-key" })
  })

  beforeEach(() => {
    jest.clearAllMocks()
  })

  it("keeps an entitlement's details and leaves out the ones the bridge didn't send", () => {
    const entitlement = Entitlement.fromJson({ ...entitlementJson, renewedAt: null })

    expect(entitlement).toBeInstanceOf(Entitlement)
    expect(entitlement.isActive).toBe(true)
    expect(entitlement.productIds).toEqual(["monthly", "yearly"])
    expect(entitlement.store).toBe("APP_STORE")
    expect(entitlement.state).toBe("subscribed")
    expect(entitlement.offerType).toBe("trial")
    expect(entitlement.renewedAt).toBeUndefined()
    expect(entitlement.isRevoked).toBeUndefined()
  })

  it("parses web entitlements into Entitlement instances", () => {
    const info = EntitlementsInfo.fromObject({
      status: "ACTIVE",
      all: [entitlementJson],
      active: [entitlementJson],
      inactive: [],
      web: [{ id: "web", type: "SERVICE_LEVEL", isActive: true }],
    })

    expect(info.web).toHaveLength(1)
    expect(info.web[0]).toBeInstanceOf(Entitlement)
    expect(info.web[0]?.id).toBe("web")
    expect(info.active[0]?.latestProductId).toBe("monthly")
  })

  it("returns the entitlements that products unlock", async () => {
    mockGetEntitlementsByProductIds.mockResolvedValue([entitlementJson])

    const entitlements = await Superwall.shared.getEntitlementsByProductIds(["monthly"])

    expect(mockGetEntitlementsByProductIds).toHaveBeenCalledWith(["monthly"])
    expect(entitlements[0]).toBeInstanceOf(Entitlement)
    expect(entitlements[0]?.willRenew).toBe(true)
  })

  it("sends only each entitlement's id and type when setting the subscription status", async () => {
    const entitlement = Entitlement.fromJson(entitlementJson)

    await Superwall.shared.setSubscriptionStatus(SubscriptionStatus.Active([entitlement]))

    expect(mockSetSubscriptionStatus).toHaveBeenCalledWith({
      status: "ACTIVE",
      entitlements: [{ id: "pro", type: "SERVICE_LEVEL" }],
    })
  })

  it("parses the new PaywallInfo fields", () => {
    const info = PaywallInfo.fromJson(paywallInfoJson)

    expect(info.cacheKey).toBe("cache-key")
    expect(info.buildId).toBe("build")
    expect(info.shimmerLoadStartTime).toBe("2026-11-12T10:00:00.000Z")
    expect(info.presentation).toEqual({
      style: "DRAWER",
      delay: 300,
      height: 70,
      cornerRadius: 16,
    })
    expect(info.isScrollEnabled).toBe(true)
    expect(info.introOfferEligibility).toBe("automatic")
  })

  it("returns null when no paywall has been presented", async () => {
    mockGetLatestPaywallInfo.mockResolvedValue(null)
    await expect(Superwall.shared.getLatestPaywallInfo()).resolves.toBeNull()

    mockGetLatestPaywallInfo.mockResolvedValue(paywallInfoJson)
    const info = await Superwall.shared.getLatestPaywallInfo()
    expect(info).toBeInstanceOf(PaywallInfo)
  })

  it("maps the native log level to the LogLevel enum", async () => {
    mockGetLogLevel.mockResolvedValue("warn")
    await expect(Superwall.shared.getLogLevel()).resolves.toBe(LogLevel.Warn)
  })

  it("parses the new StoreProduct and StoreTransaction fields", () => {
    const product = StoreProduct.fromJson(productJson)
    expect(product.subscriptionPeriod).toEqual({ value: 1, unit: "month" })
    expect(product.billingPlanType).toBe("MONTHLY")
    expect(product.entitlements?.[0]).toBeInstanceOf(Entitlement)
    expect(product.storeFrontCountryCode).toBe("US")
    expect(product.attributes).toEqual({ price: "$9.99" })

    const transaction = StoreTransaction.fromJson({
      configRequestId: "config",
      appSessionId: "session",
      originalTransactionIdentifier: "original",
      state: "PURCHASED",
      payment: { productIdentifier: "monthly", quantity: 1, discountIdentifier: null },
    })
    expect(transaction.state).toBe("PURCHASED")
    expect(transaction.payment).toEqual({
      productIdentifier: "monthly",
      quantity: 1,
      discountIdentifier: null,
    })
  })

  it("parses transactionFail details, the transactionComplete type and customer info changes", () => {
    const fail = SuperwallEventInfo.fromJson({
      event: {
        event: "transactionFail",
        error: "Pending approval",
        transactionError: { type: "pending", message: "Pending approval" },
        paywallInfo: paywallInfoJson,
      },
      params: {},
    })
    expect(fail.event.type).toBe(EventType.transactionFail)
    expect(fail.event.transactionError).toEqual({ type: "pending", message: "Pending approval" })

    const complete = SuperwallEventInfo.fromJson({
      event: {
        event: "transactionComplete",
        type: "FREE_TRIAL_START",
        product: productJson,
        paywallInfo: paywallInfoJson,
      },
      params: {},
    })
    expect(complete.event.transactionType).toBe("FREE_TRIAL_START")

    const customerInfo = {
      userId: "user",
      subscriptions: [],
      nonSubscriptions: [],
      entitlements: [entitlementJson],
      activeSubscriptionProductIds: ["monthly"],
    }
    const change = SuperwallEventInfo.fromJson({
      event: { event: "customerInfoDidChange", from: { ...customerInfo }, to: customerInfo },
      params: {},
    })
    expect(change.event.type).toBe(EventType.customerInfoDidChange)
    expect(change.event.toCustomerInfo?.activeSubscriptionProductIds).toEqual(["monthly"])
    expect(change.event.toCustomerInfo?.entitlements[0]).toBeInstanceOf(Entitlement)
  })

  it("routes a replacement purchase to purchaseFromGooglePlayWithReplacement", async () => {
    const replacement = {
      mode: "chargeProrated",
      oldProductId: "monthly",
      oldPurchaseToken: "token",
    }

    class Controller extends PurchaseController {
      purchaseFromAppStore = jest.fn()
      purchaseFromGooglePlay = jest.fn().mockResolvedValue(new PurchaseResultPurchased())
      restorePurchases = jest.fn()
    }

    // The default implementation buys the product as a new subscription.
    const plain = new Controller()
    await Superwall.configure({ apiKey: "api-key", purchaseController: plain })
    await emit("onPurchase", {
      platform: "android",
      productId: "yearly",
      basePlanId: "base",
      store: "PLAY_STORE",
      replacement,
    })
    expect(plain.purchaseFromGooglePlay).toHaveBeenCalledWith(
      "yearly",
      "base",
      undefined,
      "PLAY_STORE",
    )
    expect(mockDidPurchase).toHaveBeenCalledWith({ type: "purchased" })

    class ReplacingController extends Controller {
      purchaseFromGooglePlayWithReplacement = jest
        .fn()
        .mockResolvedValue(new PurchaseResultPurchased())
    }
    const replacing = new ReplacingController()
    await Superwall.configure({ apiKey: "api-key", purchaseController: replacing })
    await emit("onPurchase", {
      platform: "android",
      productId: "yearly",
      basePlanId: "base",
      store: "PLAY_STORE",
      replacement,
    })
    expect(replacing.purchaseFromGooglePlayWithReplacement).toHaveBeenCalledWith(
      "yearly",
      "base",
      undefined,
      replacement,
      "PLAY_STORE",
    )
    expect(replacing.purchaseFromGooglePlay).not.toHaveBeenCalled()

    await Superwall.configure({ apiKey: "api-key" })
  })

  it("forwards userAttributesDidChange and handleSuperwallDeepLink to the delegate", async () => {
    const delegate = new SuperwallDelegate()
    delegate.userAttributesDidChange = jest.fn()
    delegate.handleSuperwallDeepLink = jest.fn()
    await Superwall.shared.setDelegate(delegate)

    await emit("userAttributesDidChange", { newAttributes: { name: "Jane" } })
    await emit("handleSuperwallDeepLink", {
      fullURL: "myapp://superwall/paywall?id=1",
      pathComponents: ["paywall"],
      queryParameters: { id: "1" },
    })

    expect(delegate.userAttributesDidChange).toHaveBeenCalledWith({ name: "Jane" })
    const [url, pathComponents, queryParameters] = (delegate.handleSuperwallDeepLink as jest.Mock)
      .mock.calls[0]
    expect(url).toBeInstanceOf(URL)
    expect(url.toString()).toBe("myapp://superwall/paywall?id=1")
    expect(pathComponents).toEqual(["paywall"])
    expect(queryParameters).toEqual({ id: "1" })

    await Superwall.shared.setDelegate(undefined)
  })

  it("calls a presentation handler's onWillDismiss for its own registration only", async () => {
    const registerPlacement = jest.fn().mockResolvedValue(undefined)
    const module = require("../SuperwallExpoModule").default
    module.registerPlacement = registerPlacement

    const handler = new PaywallPresentationHandler()
    const onWillDismiss = jest.fn()
    handler.onWillDismiss(onWillDismiss)
    await Superwall.shared.register({ placement: "campaign", handler })
    const handlerId = registerPlacement.mock.calls[0][2]

    await emit("onPaywallWillDismiss", {
      paywallInfoJson: paywallInfoJson,
      result: { type: "declined" },
      handlerId: "someone-else",
    })
    expect(onWillDismiss).not.toHaveBeenCalled()

    await emit("onPaywallWillDismiss", {
      paywallInfoJson: paywallInfoJson,
      result: { type: "declined" },
      handlerId,
    })
    expect(onWillDismiss).toHaveBeenCalledTimes(1)
    expect(onWillDismiss.mock.calls[0][0]).toBeInstanceOf(PaywallInfo)
  })

  it("serializes the new paywall options and leaves out the unset ones", () => {
    const options = new PaywallOptions({
      overrideProductsByName: { primary: "yearly" },
      timeoutAfter: 5000,
      notificationPermissionsDenied: { title: "Notifications are off", message: undefined },
    }).toJson() as Record<string, unknown>

    expect(options.overrideProductsByName).toEqual({ primary: "yearly" })
    expect(options.timeoutAfter).toBe(5000)
    expect(options.notificationPermissionsDenied).toEqual({ title: "Notifications are off" })
    expect("optimisticLoading" in options).toBe(false)
    expect("shouldShowWebRestorationAlert" in options).toBe(false)
  })

  it("resolves local resources when configuring", async () => {
    await Superwall.configure({
      apiKey: "api-key",
      options: {
        localResources: { hero: 42, logo: "file:///logo.png", icon: { uri: "file:///icon.png" } },
      },
    })

    const options = mockConfigure.mock.calls[0][1]
    expect(options.localResources).toEqual({
      hero: "file:///bundle/hero.png",
      logo: "file:///logo.png",
      icon: "file:///icon.png",
    })
  })
})

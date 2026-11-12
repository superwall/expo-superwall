const mockConfigure = jest.fn().mockResolvedValue(undefined)
const mockGetPresentationResult = jest.fn()
const mockGetEntitlements = jest.fn()
const mockGetSubscriptionStatus = jest.fn()
const mockDidPurchase = jest.fn().mockResolvedValue(undefined)
const mockListeners: Record<string, ((data: any) => unknown)[]> = {}

// The compat configuration gate only needs an in-process event emitter.
jest.mock("expo", () => ({ EventEmitter: require("node:events").EventEmitter }))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: jest.fn((name: string, listener: (data: any) => unknown) => {
      if (!mockListeners[name]) mockListeners[name] = []
      mockListeners[name].push(listener)
      return { remove: jest.fn() }
    }),
    configure: mockConfigure,
    getPresentationResult: mockGetPresentationResult,
    getEntitlements: mockGetEntitlements,
    getSubscriptionStatus: mockGetSubscriptionStatus,
    didPurchase: mockDidPurchase,
  },
}))

const {
  default: Superwall,
  EventType,
  PaywallInfo,
  PaywallOptions,
  PresentationResultPaywall,
  ProductStore,
  PurchaseController,
  PurchaseResultPurchased,
  SuperwallEventInfo,
}: typeof import("../compat") = require("../compat")
const {
  ComputedPropertyRequestType,
}: typeof import("../compat/lib/ComputedPropertyRequest") = require("../compat/lib/ComputedPropertyRequest")
const {
  DefaultSuperwallOptions,
}: typeof import("../SuperwallOptions") = require("../SuperwallOptions")

async function emit(name: string, data: any) {
  await Promise.all((mockListeners[name] ?? []).map((listener) => listener(data)))
}

// The shape both native bridges send now: Android used to send `{ type, id }` products and
// SCREAMING_CASE computed property types.
const paywallInfoJson = {
  identifier: "paywall",
  name: "Paywall",
  url: "https://example.com",
  presentationId: "presentation",
  products: [
    {
      id: "monthly:base:sw-auto",
      name: "primary",
      store: "PLAY_STORE",
      entitlements: [{ id: "pro", type: "SERVICE_LEVEL" }],
    },
    {
      id: "custom_coins",
      name: "secondary",
      store: "CUSTOM",
      entitlements: [],
      customProduct: { id: "custom_coins" },
    },
  ],
  productIds: ["monthly:base:sw-auto", "custom_coins"],
  presentedBy: "register",
  presentedByEventAt: "2026-11-12T10:00:00.000Z",
  isFreeTrialAvailable: false,
  featureGatingBehavior: "nonGated",
  closeReason: "none",
  computedPropertyRequests: [
    { type: "minutesSince", placementName: "a" },
    { type: "placementsInDay", placementName: "b" },
  ],
  surveys: [],
  localNotifications: [
    { type: "trialStarted", title: "Trial", subtitle: "Ends soon", body: "Body", delay: 1000 },
  ],
  state: {},
}

describe("compat paywall info", () => {
  it("parses products, presentation ID and every computed property type", () => {
    const info = PaywallInfo.fromJson(paywallInfoJson)
    expect(info.presentationId).toBe("presentation")
    expect(info.products.map((product) => product.store)).toEqual([
      ProductStore.playStore,
      ProductStore.custom,
    ])
    expect(info.products[0].name).toBe("primary")
    expect([...info.products[0].entitlements].map((entitlement) => entitlement.id)).toEqual(["pro"])
    expect(info.products[1].customProduct).toEqual({ id: "custom_coins" })
    expect(info.computedPropertyRequests.map((request) => request.type)).toEqual([
      ComputedPropertyRequestType.minutesSince,
      ComputedPropertyRequestType.placementsInDay,
    ])
    expect(info.presentedByEventAt).toBe("2026-11-12T10:00:00.000Z")
  })
})

describe("compat event parsing", () => {
  function parse(event: Record<string, any>) {
    return SuperwallEventInfo.fromJson({ event, params: {} }).event
  }

  it("reads the trigger's placement name, not the event type", () => {
    const event = parse({
      event: "triggerFire",
      placementName: "campaign_trigger",
      result: { result: "noAudienceMatch" },
    })
    expect(event.placementName).toBe("campaign_trigger")
  })

  it("reads integration attributes from `attributes`", () => {
    const event = parse({ event: "integrationAttributes", attributes: { adjustId: "abc" } })
    expect(event.audienceFilterParams).toEqual({ adjustId: "abc" })
  })

  it("keeps the restore type of transactionRestore", () => {
    const event = parse({
      event: "transactionRestore",
      restoreType: { type: "viaRestore" },
      paywallInfo: paywallInfoJson,
    })
    expect(event.restoreType?.type).toBeDefined()
    expect(event.paywallInfo?.identifier).toBe("paywall")
  })

  it("keeps the paywall of product and response load events", () => {
    for (const name of [
      "paywallResponseLoadComplete",
      "paywallProductsLoadStart",
      "paywallProductsLoadFail",
      "paywallProductsLoadComplete",
    ]) {
      const event = parse({
        event: name,
        triggeredEventName: "placement",
        paywallInfo: paywallInfoJson,
      })
      expect(event.triggeredEventName).toBe("placement")
      expect(event.paywallInfo?.identifier).toBe("paywall")
    }
    // iOS sends no paywall for paywallProductsLoadComplete.
    expect(
      parse({ event: "paywallProductsLoadComplete", triggeredEventName: "placement" }).paywallInfo,
    ).toBeUndefined()
  })

  it("recognizes events the bridges send that had no compat type", () => {
    expect(parse({ event: "customerInfoDidChange" }).type).toBe(EventType.customerInfoDidChange)

    const permission = parse({
      event: "permissionGranted",
      permissionName: "notifications",
      paywallIdentifier: "paywall",
    })
    expect(permission.type).toBe(EventType.permissionGranted)
    expect(permission.permissionName).toBe("notifications")
    expect(permission.paywallIdentifier).toBe("paywall")

    const info = { provider: "mmp", matched: true, source: "meta", confidence: "high" }
    const attribution = parse({ event: "attributionMatch", info })
    expect(attribution.type).toBe(EventType.attributionMatch)
    expect(attribution.attributionMatchInfo).toEqual(info)

    const review = parse({ event: "reviewGranted", count: 2 })
    expect(review.type).toBe(EventType.reviewGranted)
    expect(review.count).toBe(2)
  })
})

describe("compat Superwall methods", () => {
  beforeAll(async () => {
    await Superwall.configure({ apiKey: "api-key" })
  })

  it("returns a PresentationResult instance from getPresentationResult", async () => {
    mockGetPresentationResult.mockResolvedValueOnce({
      type: "Paywall",
      experiment: { id: "exp", groupId: "group", variant: { id: "variant", type: "TREATMENT" } },
    })
    const result = await Superwall.shared.getPresentationResult({ placement: "placement" })
    expect(result).toBeInstanceOf(PresentationResultPaywall)
  })

  it("fills the entitlements' status from the subscription status", async () => {
    const active = [{ id: "pro", type: "SERVICE_LEVEL" }]
    mockGetEntitlements.mockResolvedValueOnce({ all: active, active, inactive: [] })
    mockGetSubscriptionStatus.mockResolvedValueOnce({ status: "ACTIVE", entitlements: active })
    const entitlements = await Superwall.shared.getEntitlements()
    expect(entitlements.status.status).toBe("ACTIVE")
  })

  it("passes the product's store to the purchase controller on both platforms", async () => {
    class Controller extends PurchaseController {
      purchaseFromAppStore = jest.fn(async () => new PurchaseResultPurchased())
      purchaseFromGooglePlay = jest.fn(async () => new PurchaseResultPurchased())
      restorePurchases = jest.fn()
    }
    const controller = new Controller()
    await Superwall.configure({ apiKey: "api-key", purchaseController: controller })

    await emit("onPurchase", { platform: "ios", productId: "custom_coins", store: "CUSTOM" })
    expect(controller.purchaseFromAppStore).toHaveBeenCalledWith(
      "custom_coins",
      ProductStore.custom,
    )

    await emit("onPurchase", {
      platform: "android",
      productId: "custom_coins",
      basePlanId: null,
      offerId: null,
      store: "CUSTOM",
    })
    expect(controller.purchaseFromGooglePlay).toHaveBeenCalledWith(
      "custom_coins",
      null,
      null,
      ProductStore.custom,
    )
  })

  it("calls a presentation handler's onPresent once per presentation", async () => {
    const onPresent = jest.fn()
    const handlerId = "handler"
    // Registering needs the native module; the handler map is what the listener reads.
    ;(Superwall.shared as any).presentationHandlers.set(handlerId, { onPresentHandler: onPresent })

    await emit("onPaywallPresent", { handlerId, paywallInfoJson })
    expect(onPresent).toHaveBeenCalledTimes(1)
  })
})

describe("paywall option defaults", () => {
  it("preload paywalls and confirm web purchases, as the native SDKs do", () => {
    expect(DefaultSuperwallOptions.paywalls.shouldPreload).toBe(true)
    expect(DefaultSuperwallOptions.paywalls.shouldShowWebPurchaseConfirmationAlert).toBe(true)

    const compatOptions = new PaywallOptions().toJson()
    expect(compatOptions.shouldPreload).toBe(true)
    expect(compatOptions.shouldShowWebPurchaseConfirmationAlert).toBe(true)
  })
})

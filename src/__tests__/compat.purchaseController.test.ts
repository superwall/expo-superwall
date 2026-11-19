const mockConfigure = jest.fn().mockResolvedValue(undefined)
const mockDidPurchase = jest.fn()
const mockDidRestore = jest.fn()
const mockListeners = new Map<string, ((data: any) => unknown)[]>()

// The compat configuration gate only needs an in-process event emitter.
jest.mock("expo", () => ({ EventEmitter: require("node:events").EventEmitter }))
// The compat SDK resolves local resources through `expo-asset`, whose ESM build jest can't require.
jest.mock("expo-asset", () => ({ Asset: { fromModule: jest.fn() } }))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: jest.fn((name: string, listener: (data: any) => unknown) => {
      mockListeners.set(name, [...(mockListeners.get(name) ?? []), listener])
      return { remove: jest.fn() }
    }),
    configure: mockConfigure,
    didPurchase: mockDidPurchase,
    didRestore: mockDidRestore,
  },
}))

const {
  default: Superwall,
  PurchaseResultPurchased,
  RestorationResult,
}: typeof import("../compat") = require("../compat")

// Resolves once every listener for the event has finished its async work.
const emit = async (name: string, data: any) => {
  await Promise.allSettled((mockListeners.get(name) ?? []).map((listener) => listener(data)))
}

const configureWith = async (purchaseController: any) => {
  await Superwall.configure({ apiKey: "api-key", purchaseController })
}

describe("compat purchase controller bridge", () => {
  beforeEach(() => {
    mockDidPurchase.mockClear()
    mockDidRestore.mockClear()
  })

  it("answers a Google Play purchase as failed when the controller throws", async () => {
    await configureWith({
      purchaseFromAppStore: jest.fn(),
      purchaseFromGooglePlay: jest.fn().mockRejectedValue(new Error("Billing unavailable")),
      restorePurchases: jest.fn(),
    })

    await emit("onPurchase", { platform: "android", productId: "pro_monthly" })

    expect(mockDidPurchase).toHaveBeenCalledWith({ type: "failed", error: "Billing unavailable" })
  })

  it("answers an App Store purchase as failed when the controller throws", async () => {
    await configureWith({
      purchaseFromAppStore: jest.fn().mockRejectedValue(new Error("User cancelled")),
      purchaseFromGooglePlay: jest.fn(),
      restorePurchases: jest.fn(),
    })

    await emit("onPurchase", { platform: "ios", productId: "pro_monthly" })

    expect(mockDidPurchase).toHaveBeenCalledWith({ type: "failed", error: "User cancelled" })
  })

  it("answers a purchase as failed when the controller resolves with no result", async () => {
    await configureWith({
      purchaseFromAppStore: jest.fn(),
      purchaseFromGooglePlay: jest.fn().mockResolvedValue(undefined),
      restorePurchases: jest.fn(),
    })

    await emit("onPurchase", { platform: "android", productId: "pro_monthly" })

    expect(mockDidPurchase).toHaveBeenCalledTimes(1)
    expect(mockDidPurchase.mock.calls[0][0].type).toBe("failed")
  })

  it("answers a restore as failed when the controller throws", async () => {
    await configureWith({
      purchaseFromAppStore: jest.fn(),
      purchaseFromGooglePlay: jest.fn(),
      restorePurchases: jest.fn().mockRejectedValue(new Error("Network down")),
    })

    await emit("onPurchaseRestore", undefined)

    expect(mockDidRestore).toHaveBeenCalledWith({ result: "failed", errorMessage: "Network down" })
  })

  it("still passes a successful purchase through", async () => {
    await configureWith({
      purchaseFromAppStore: jest.fn(),
      purchaseFromGooglePlay: jest.fn().mockResolvedValue(new PurchaseResultPurchased()),
      restorePurchases: jest.fn().mockResolvedValue(RestorationResult.restored()),
    })

    await emit("onPurchase", { platform: "android", productId: "pro_monthly" })
    await emit("onPurchaseRestore", undefined)

    expect(mockDidPurchase).toHaveBeenCalledWith({ type: "purchased" })
    expect(mockDidRestore).toHaveBeenCalledWith({ result: "restored" })
  })

  it("sends an error message for a restore that failed without an error", async () => {
    await configureWith({
      purchaseFromAppStore: jest.fn(),
      purchaseFromGooglePlay: jest.fn(),
      restorePurchases: jest.fn().mockResolvedValue(RestorationResult.failed()),
    })

    await emit("onPurchaseRestore", undefined)

    // iOS cannot read a null `errorMessage` and drops the answer, so a message is always sent.
    expect(mockDidRestore).toHaveBeenCalledWith({
      result: "failed",
      errorMessage: expect.any(String),
    })
  })

  it("leaves the purchase to another layer when no compat controller is set", async () => {
    await configureWith(undefined)

    await emit("onPurchase", { platform: "android", productId: "pro_monthly" })
    await emit("onPurchaseRestore", undefined)

    expect(mockDidPurchase).not.toHaveBeenCalled()
    expect(mockDidRestore).not.toHaveBeenCalled()
  })
})

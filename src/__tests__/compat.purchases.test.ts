const mockConfigure = jest.fn().mockResolvedValue(undefined)
const mockProducts = jest.fn()
const mockPurchase = jest.fn()
const mockQueryInAppPurchases = jest.fn()
const mockConsume = jest.fn()

// The compat configuration gate only needs an in-process event emitter.
jest.mock("expo", () => ({ EventEmitter: require("node:events").EventEmitter }))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: jest.fn(() => ({ remove: jest.fn() })),
    configure: mockConfigure,
    products: mockProducts,
    purchase: mockPurchase,
    queryInAppPurchases: mockQueryInAppPurchases,
    consume: mockConsume,
  },
}))

const { default: Superwall }: typeof import("../compat") = require("../compat")

describe("compat products and purchases", () => {
  beforeAll(async () => {
    await Superwall.configure({ apiKey: "api-key" })
  })

  it("returns products from the native module", async () => {
    const products = [
      { productIdentifier: "coins_500", price: 3.99, localizedPrice: "$3.99", currencyCode: "USD" },
      { productIdentifier: "coins_100", price: 0.99, localizedPrice: "$0.99", currencyCode: "USD" },
    ]
    mockProducts.mockResolvedValueOnce(products)

    await expect(Superwall.products(["coins_500", "unknown", "coins_100"])).resolves.toEqual(
      products,
    )
    expect(mockProducts).toHaveBeenCalledWith(["coins_500", "unknown", "coins_100"])
  })

  it("passes a failed purchase result through with its error", async () => {
    mockPurchase.mockResolvedValueOnce({
      type: "failed",
      error: "Product not found for identifier: missing",
    })

    await expect(Superwall.purchase("missing")).resolves.toEqual({
      type: "failed",
      error: "Product not found for identifier: missing",
    })
  })

  it("queries owned purchases, then consumes each token after granting", async () => {
    mockQueryInAppPurchases.mockResolvedValueOnce([
      {
        productIds: ["coins_100"],
        purchaseToken: "token-1",
        orderId: "GPA.1",
        purchaseTime: 1700000000000,
        quantity: 2,
        isAcknowledged: false,
      },
    ])
    mockConsume.mockImplementation(async (token: string) => token)

    const owned = await Superwall.shared.queryInAppPurchases()
    const granted: string[] = []
    for (const purchase of owned) {
      granted.push(...purchase.productIds)
      await expect(Superwall.shared.consume(purchase.purchaseToken)).resolves.toBe("token-1")
    }

    expect(granted).toEqual(["coins_100"])
    expect(mockConsume).toHaveBeenCalledWith("token-1")
  })

  it("surfaces native query errors", async () => {
    mockQueryInAppPurchases.mockRejectedValueOnce(
      new Error("queryInAppPurchases is only available on Android."),
    )

    await expect(Superwall.shared.queryInAppPurchases()).rejects.toThrow(
      "queryInAppPurchases is only available on Android.",
    )
  })
})

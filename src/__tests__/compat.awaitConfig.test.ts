const mockConfigure = jest.fn()
const mockIdentify = jest.fn().mockResolvedValue(undefined)

// The compat configuration gate only needs an in-process event emitter.
jest.mock("expo", () => ({ EventEmitter: require("node:events").EventEmitter }))
// The compat SDK resolves local resources through `expo-asset`, whose ESM build jest can't require.
jest.mock("expo-asset", () => ({ Asset: { fromModule: jest.fn() } }))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: jest.fn(() => ({ remove: jest.fn() })),
    configure: mockConfigure,
    identify: mockIdentify,
  },
}))

const { default: Superwall }: typeof import("../compat") = require("../compat")

// Settles a promise against a deadline so a call that never settles shows up as "pending".
const settleWithin = (promise: Promise<unknown>, ms = 50) =>
  Promise.race([
    promise.then(
      () => "resolved",
      (error: Error) => `rejected: ${error.message}`,
    ),
    new Promise((resolve) => setTimeout(() => resolve("pending"), ms)),
  ])

describe("compat configuration gate", () => {
  beforeEach(() => {
    mockIdentify.mockClear()
  })

  it("rejects calls waiting on configure when the native configure fails", async () => {
    mockConfigure.mockRejectedValueOnce(new Error("React context is not available"))
    const pendingIdentify = Superwall.shared.identify({ userId: "user_1" })

    await expect(Superwall.configure({ apiKey: "api-key" })).rejects.toThrow(
      "React context is not available",
    )

    expect(await settleWithin(pendingIdentify)).toBe("rejected: React context is not available")
    expect(await settleWithin(Superwall.shared.identify({ userId: "user_1" }))).toBe(
      "rejected: React context is not available",
    )
    expect(mockIdentify).not.toHaveBeenCalled()
  })

  it("lets a later configure succeed after a failed one", async () => {
    mockConfigure.mockResolvedValueOnce(undefined)

    await Superwall.configure({ apiKey: "api-key" })

    expect(await settleWithin(Superwall.shared.identify({ userId: "user_1" }))).toBe("resolved")
    expect(mockIdentify).toHaveBeenCalledTimes(1)
  })
})

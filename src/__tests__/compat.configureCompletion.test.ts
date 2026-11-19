const mockConfigure = jest.fn().mockResolvedValue(undefined)
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

const settleWithin = (promise: Promise<unknown>, ms = 50) =>
  Promise.race([
    promise.then(
      () => "resolved",
      (error: Error) => `rejected: ${error.message}`,
    ),
    new Promise((resolve) => setTimeout(() => resolve("pending"), ms)),
  ])

describe("compat configure completion", () => {
  it("still marks the SDK configured when the completion throws", async () => {
    await expect(
      Superwall.configure({
        apiKey: "api-key",
        completion: () => {
          throw new Error("analytics not ready")
        },
      }),
    ).rejects.toThrow("analytics not ready")

    // Native configure succeeded, so calls must not wait forever on the gate.
    expect(await settleWithin(Superwall.shared.identify({ userId: "user_1" }))).toBe("resolved")
  })
})

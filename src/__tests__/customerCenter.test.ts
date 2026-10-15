const mockListeners = new Map<string, ((data: any) => unknown)[]>()
const mockPresentCustomerCenter = jest.fn()
const mockDismissCustomerCenter = jest.fn().mockResolvedValue(undefined)
const mockDidHandleShouldRestore = jest.fn()

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: jest.fn((name: string, listener: (data: any) => unknown) => {
      const listeners = mockListeners.get(name) ?? []
      listeners.push(listener)
      mockListeners.set(name, listeners)
      return {
        remove: () => {
          mockListeners.set(
            name,
            (mockListeners.get(name) ?? []).filter((l) => l !== listener),
          )
        },
      }
    }),
    presentCustomerCenter: mockPresentCustomerCenter,
    dismissCustomerCenter: mockDismissCustomerCenter,
    didHandleCustomerCenterShouldRestorePurchases: mockDidHandleShouldRestore,
  },
}))

const {
  customerCenterConfigurationToJson,
  presentCustomerCenter,
}: typeof import("../internal/customerCenter") = require("../internal/customerCenter")

const emit = async (name: string, data: any) => {
  await Promise.all((mockListeners.get(name) ?? []).map((listener) => listener(data)))
}

describe("Customer Center", () => {
  beforeEach(() => {
    mockListeners.clear()
    jest.clearAllMocks()
  })

  it("strips undefined values from the configuration at every level", () => {
    expect(
      customerCenterConfigurationToJson({
        managementScreen: {
          title: undefined,
          paths: [{ type: "refund", windowMillis: undefined }, { type: "restore" }],
        },
        support: { email: "help@example.com", latestAppVersion: undefined },
      }),
    ).toEqual({
      managementScreen: { paths: [{ type: "refund" }, { type: "restore" }] },
      support: { email: "help@example.com" },
    })
  })

  it("routes callbacks for its own presentation and resolves on dismiss", async () => {
    let dismiss!: () => void
    mockPresentCustomerCenter.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          dismiss = resolve
        }),
    )
    const onAction = jest.fn()
    const onRefundRequestComplete = jest.fn()

    const presented = presentCustomerCenter({
      configuration: { showsAccountDetails: false },
      onAction,
      onRefundRequestComplete,
    })
    await Promise.resolve()

    const [configuration, handlerId, asksBeforeRestoring] = mockPresentCustomerCenter.mock.calls[0]
    expect(configuration).toEqual({ showsAccountDetails: false })
    expect(asksBeforeRestoring).toBe(false)

    await emit("onCustomerCenterAction", {
      handlerId: "someone-else",
      action: { type: "restore" },
      pathId: "restore",
      purchase: null,
    })
    await emit("onCustomerCenterAction", {
      handlerId,
      action: { type: "custom", identifier: "faq" },
      pathId: "faq",
      purchase: null,
    })
    await emit("onCustomerCenterRefundRequestComplete", {
      handlerId,
      productId: "pro_monthly",
      status: "success",
    })

    expect(onAction).toHaveBeenCalledTimes(1)
    expect(onAction).toHaveBeenCalledWith({ type: "custom", identifier: "faq" }, "faq", null)
    expect(onRefundRequestComplete).toHaveBeenCalledWith("pro_monthly", "success")

    dismiss()
    await presented
    expect([...mockListeners.values()].every((listeners) => listeners.length === 0)).toBe(true)
  })

  it("answers shouldRestorePurchases with the callback's result", async () => {
    let dismiss!: () => void
    mockPresentCustomerCenter.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          dismiss = resolve
        }),
    )

    const presented = presentCustomerCenter({ shouldRestorePurchases: async () => false })
    await Promise.resolve()
    const [, handlerId, asksBeforeRestoring] = mockPresentCustomerCenter.mock.calls[0]
    expect(asksBeforeRestoring).toBe(true)

    await emit("onCustomerCenterShouldRestorePurchases", { handlerId, requestId: "request-1" })
    expect(mockDidHandleShouldRestore).toHaveBeenCalledWith("request-1", false)

    dismiss()
    await presented
  })
})

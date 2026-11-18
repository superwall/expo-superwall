const mockConfigure = jest.fn().mockResolvedValue(undefined)
const mockListeners = new Map<string, ((data: any) => void)[]>()

// The compat configuration gate only needs an in-process event emitter.
jest.mock("expo", () => ({ EventEmitter: require("node:events").EventEmitter }))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: jest.fn((name: string, listener: (data: any) => void) => {
      mockListeners.set(name, [...(mockListeners.get(name) ?? []), listener])
      return { remove: jest.fn() }
    }),
    configure: mockConfigure,
  },
}))

const { default: Superwall }: typeof import("../compat") = require("../compat")

const emit = (name: string, data: any) => {
  for (const listener of mockListeners.get(name) ?? []) {
    listener(data)
  }
}

describe("compat delegate", () => {
  it("receives the events the SDK reports while configure is still pending", async () => {
    let resolveConfigure: (() => void) | undefined
    mockConfigure.mockReturnValueOnce(
      new Promise<void>((resolve) => {
        resolveConfigure = resolve
      }),
    )
    const handleSuperwallEvent = jest.fn()

    // The native configure call only resolves after the first config fetch. A delegate set
    // before it must already be in place for what the SDK reports in the meantime.
    await Superwall.shared.setDelegate({ handleSuperwallEvent } as any)
    const pendingConfigure = Superwall.configure({ apiKey: "api-key" })
    await Promise.resolve()

    emit("handleSuperwallEvent", { eventInfo: { event: { event: "appOpen" }, params: {} } })
    await Promise.resolve()

    expect(handleSuperwallEvent).toHaveBeenCalledTimes(1)
    expect(handleSuperwallEvent.mock.calls[0][0].event.type).toBe("appOpen")

    resolveConfigure?.()
    await pendingConfigure
  })
})

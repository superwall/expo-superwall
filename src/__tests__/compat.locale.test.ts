const mockConfigure = jest.fn().mockResolvedValue(undefined)
const mockSetLocaleIdentifier = jest.fn()

// The compat configuration gate only needs an in-process event emitter.
jest.mock("expo", () => ({ EventEmitter: require("node:events").EventEmitter }))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: {
    addListener: jest.fn(() => ({ remove: jest.fn() })),
    configure: mockConfigure,
    setLocaleIdentifier: mockSetLocaleIdentifier,
  },
}))

const { default: Superwall }: typeof import("../compat") = require("../compat")

describe("compat runtime locale", () => {
  it("waits for configure, changes the locale, and resets without reconfiguring", async () => {
    let resolveConfigure: (() => void) | undefined
    mockConfigure.mockReturnValueOnce(
      new Promise<void>((resolve) => {
        resolveConfigure = resolve
      }),
    )

    const pendingConfigure = Superwall.configure({ apiKey: "api-key" })
    const pendingLocale = Superwall.shared.setLocaleIdentifier("es_ES")
    await Promise.resolve()
    expect(mockSetLocaleIdentifier).not.toHaveBeenCalled()

    resolveConfigure?.()
    await pendingConfigure
    await pendingLocale
    expect(mockSetLocaleIdentifier).toHaveBeenCalledWith("es_ES")

    await Superwall.shared.setLocaleIdentifier(null)
    expect(mockSetLocaleIdentifier.mock.calls).toEqual([["es_ES"], [null]])
    expect(mockConfigure).toHaveBeenCalledTimes(1)
  })
})

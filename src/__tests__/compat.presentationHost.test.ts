const mockConfigure = jest.fn().mockResolvedValue(undefined)
const mockModule: Record<string, unknown> = {
  addListener: jest.fn(() => ({ remove: jest.fn() })),
  configure: mockConfigure,
}

// The compat configuration gate only needs an in-process event emitter.
jest.mock("expo", () => ({ EventEmitter: require("node:events").EventEmitter }))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: mockModule,
}))

const { PaywallOptions, PresentationHost }: typeof import("../compat") = require("../compat")

describe("compat presentationHost", () => {
  it("leaves presentationHost out of the JSON when unset", () => {
    expect(new PaywallOptions().toJson()).not.toHaveProperty("presentationHost")
  })

  it("serializes presentationHost when set", () => {
    const options = new PaywallOptions({ presentationHost: PresentationHost.inWindow })
    expect(options.toJson()).toMatchObject({ presentationHost: "inWindow" })
  })
})

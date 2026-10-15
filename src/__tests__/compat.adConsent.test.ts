const mockConfigure = jest.fn().mockResolvedValue(undefined)
const mockSetAdConsent = jest.fn()
const mockModule: Record<string, unknown> = {
  addListener: jest.fn(() => ({ remove: jest.fn() })),
  configure: mockConfigure,
  setAdConsent: mockSetAdConsent,
}

// The compat configuration gate only needs an in-process event emitter.
jest.mock("expo", () => ({ EventEmitter: require("node:events").EventEmitter }))

jest.mock("../SuperwallExpoModule", () => ({
  __esModule: true,
  default: mockModule,
}))

const {
  AdConsentStatus,
  default: Superwall,
  EventType,
  SuperwallEventInfo,
}: typeof import("../compat") = require("../compat")

describe("compat ad consent", () => {
  it("passes adConsent to configure without undefined purposes", async () => {
    await Superwall.configure({
      apiKey: "api-key",
      options: {
        adConsent: { adUserData: AdConsentStatus.Denied, adPersonalization: undefined },
      },
    })
    const options = mockConfigure.mock.calls[0][1]
    expect(options.adConsent).toEqual({ adUserData: "denied" })
  })

  it("sets ad consent where the native module supports it, and skips it otherwise", async () => {
    await Superwall.shared.setAdConsent({
      adUserData: AdConsentStatus.Granted,
      adPersonalization: AdConsentStatus.Denied,
    })
    expect(mockSetAdConsent).toHaveBeenCalledWith({
      adUserData: "granted",
      adPersonalization: "denied",
    })

    // Android doesn't define `setAdConsent`.
    delete mockModule.setAdConsent
    await expect(
      Superwall.shared.setAdConsent({ adUserData: AdConsentStatus.Denied }),
    ).resolves.toBeUndefined()
    expect(mockSetAdConsent).toHaveBeenCalledTimes(1)
  })
})

describe("compat teleport events", () => {
  const paywallInfo = { identifier: "paywall" }

  beforeAll(() => {
    // Parsing a full paywall info is covered elsewhere; only its routing matters here.
    const {
      PaywallInfo,
    }: typeof import("../compat/lib/PaywallInfo") = require("../compat/lib/PaywallInfo")
    jest.spyOn(PaywallInfo, "fromJson").mockImplementation((json: any) => json)
  })

  it("parses teleportOpen with its paywall", () => {
    const info = SuperwallEventInfo.fromJson({
      event: { event: "teleportOpen", paywallInfo },
      params: {},
    })
    expect(info.event.type).toBe(EventType.teleportOpen)
    expect(info.event.paywallInfo?.identifier).toBe("paywall")
  })

  it("parses teleportReturn with and without a presented paywall", () => {
    const withPaywall = SuperwallEventInfo.fromJson({
      event: { event: "teleportReturn", paywallInfo },
      params: {},
    })
    expect(withPaywall.event.type).toBe(EventType.teleportReturn)
    expect(withPaywall.event.paywallInfo?.identifier).toBe("paywall")

    const withoutPaywall = SuperwallEventInfo.fromJson({
      event: { event: "teleportReturn" },
      params: {},
    })
    expect(withoutPaywall.event.type).toBe(EventType.teleportReturn)
    expect(withoutPaywall.event.paywallInfo).toBeUndefined()
  })
})

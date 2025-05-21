import SuperwallExpoModule from "./SuperwallExpoModule"

export function getApiKey(): string {
  return SuperwallExpoModule.getApiKey()
}

export function registerPlacement(
  placement: string,
  params?: Map<string, any> | Record<string, any>,
  handlerId?: string,
): Promise<void> {
  return SuperwallExpoModule.registerPlacement(placement, params, handlerId)
}

export const configure = (
  apiKey: string,
  options?: Map<string, any> | Record<string, any>,
  usingPurchaseController?: boolean,
  sdkVersion?: string,
): Promise<void> => {
  return SuperwallExpoModule.configure(apiKey, options, usingPurchaseController, sdkVersion)
}

export default SuperwallExpoModule

import SuperwallExpoModule from "./SuperwallExpoModule"

export function getApiKey(): string {
  return SuperwallExpoModule.getApiKey()
}

export function registerPlacement(
  placement: string,
  params?: Map<string, any> | Record<string, any>,
  handlerId?: string, // TODO: remove this from public API
  // TODO: Add feature handler
): Promise<void> {
  return SuperwallExpoModule.registerPlacement(placement, params, handlerId)
}

export const configure = (
  apiKey: string,
  options?: Map<string, any> | Record<string, any>,
  usingPurchaseController?: boolean,
  sdkVersion?: string, // TODO: remove this
  // TODO: Add completion handler
): Promise<void> => {
  return SuperwallExpoModule.configure(apiKey, options, usingPurchaseController, sdkVersion)
}

export const getConfigurationStatus = (): Promise<string> => {
  return SuperwallExpoModule.getConfigurationStatus()
}

export default SuperwallExpoModule

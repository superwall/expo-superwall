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

export default SuperwallExpoModule

import { NativeModule, requireNativeModule } from "expo"
import type { SuperwallExpoModuleEvents } from "./SuperwallExpoModule.types"

declare class SuperwallExpoModule extends NativeModule<SuperwallExpoModuleEvents> {
  getApiKey(): string
  registerPlacement(
    placement: string,
    params?: Map<string, any> | Record<string, any>,
    handlerId?: string,
  ): Promise<void>
  configure(
    apiKey: string,
    options?: Map<string, any> | Record<string, any>,
    usingPurchaseController?: boolean,
    sdkVersion?: string,
  ): Promise<void>

  // User Management
  identify(userId: string, options?: Map<string, any> | Record<string, any>): void
  reset(): void
}

export default requireNativeModule<SuperwallExpoModule>("SuperwallExpo")

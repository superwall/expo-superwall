import { NativeModule, requireNativeModule } from "expo"
import type { SuperwallExpoModuleEvents } from "./SuperwallExpoModule.types"

declare class SuperwallExpoModule extends NativeModule<SuperwallExpoModuleEvents> {
  getApiKey(): string
  registerPlacement(
    placement: string,
    params?: Map<string, any> | Record<string, any>,
    handlerId?: string,
  ): Promise<void>
}

export default requireNativeModule<SuperwallExpoModule>("SuperwallExpo")

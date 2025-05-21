import { NativeModule, requireNativeModule } from "expo"

declare class SuperwallExpoModule extends NativeModule {
  getApiKey(): string
  registerPlacement(placement: string, params?: Map<string, any> | Record<string, any>): void
}

export default requireNativeModule<SuperwallExpoModule>("SuperwallExpo")

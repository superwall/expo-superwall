import { NativeModule, requireNativeModule } from "expo"
import type { SuperwallExpoModuleEvents } from "./SuperwallExpoModule.types"

export type SubscriptionStatus = any

declare class SuperwallExpoModule extends NativeModule<SuperwallExpoModuleEvents> {
  getApiKey(): string
  registerPlacement(
    placement: string,
    params?: Map<string, any> | Record<string, any>,
    handlerId?: string | null,
  ): Promise<void>

  configure(
    apiKey: string,
    options?: Map<string, any> | Record<string, any>,
    usingPurchaseController?: boolean,
    sdkVersion?: string,
  ): Promise<void>

  getConfigurationStatus(): Promise<string>

  identify(userId: string, options?: Map<string, any> | Record<string, any> | null): void
  reset(): void

  getAssignments(): Promise<any[]>
  getEntitlements(): Promise<any>
  getSubscriptionStatus(): Promise<SubscriptionStatus>
  setSubscriptionStatus(status: Record<string, any>): void

  setInterfaceStyle(style?: string): void

  getUserAttributes(): Promise<Record<string, any>>
  setUserAttributes(userAttributes: Record<string, any>): void

  handleDeepLink(url: string): Promise<boolean>

  didPurchase(result: Record<string, any>): void
  didRestore(result: Record<string, any>): void

  dismiss(): Promise<void>
  confirmAllAssignments(): Promise<any[]>

  getPresentationResult(
    placement: string,
    params?: Map<string, any> | Record<string, any>,
  ): Promise<any>

  preloadPaywalls(placementNames: string[]): void
  preloadAllPaywalls(): void

  setLogLevel(level: string): void
}

export default requireNativeModule<SuperwallExpoModule>("SuperwallExpo")

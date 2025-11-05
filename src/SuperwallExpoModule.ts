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

  identify(userId: string, options?: Map<string, any> | Record<string, any> | null): Promise<void>
  reset(): Promise<void>

  getAssignments(): Promise<any[]>
  getEntitlements(): Promise<any>
  getSubscriptionStatus(): Promise<SubscriptionStatus>
  setSubscriptionStatus(status: Record<string, any>): Promise<void>

  setInterfaceStyle(style?: string): void

  getUserAttributes(): Promise<Record<string, any>>
  setUserAttributes(userAttributes: Record<string, any>): Promise<void>

  handleDeepLink(url: string): Promise<boolean>

  didPurchase(
    result:
      | { type: "cancelled" | "purchased" | "pending" }
      | {
          type: "failed"
          error?: string
        },
  ): void
  didRestore(result: Record<string, any>): void

  dismiss(): Promise<void>
  confirmAllAssignments(): Promise<any[]>

  getPresentationResult(
    placement: string,
    params?: Map<string, any> | Record<string, any>,
  ): Promise<any>

  getDeviceAttributes(): Promise<Record<string, any>>

  preloadPaywalls(placementNames: string[]): void
  preloadAllPaywalls(): void

  consume(purchaseToken: string): Promise<string>

  setLogLevel(level: string): void
}

export default requireNativeModule<SuperwallExpoModule>("SuperwallExpo")

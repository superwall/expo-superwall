import { NativeModule, requireNativeModule } from "expo"
import type { PresentationResult } from "./compat/lib/PresentationResult"
import type {
  Assignment,
  CustomerInfo,
  Entitlement,
  EntitlementsInfo,
  IntegrationAttribute,
  IntegrationAttributes,
  OwnedInAppPurchase,
  PaywallInfo,
  ProductResponse,
  PurchaseResultResponse,
  PurchasingObserverState,
  RestorationResultResponse,
  SuperwallExpoModuleEvents,
} from "./SuperwallExpoModule.types"

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

  getAssignments(): Promise<Assignment[]>
  getCustomerInfo(): Promise<CustomerInfo>
  /** Forces a refresh on iOS builds with SuperwallKit 4.19+; elsewhere the same as `getCustomerInfo`. */
  refreshCustomerInfo(): Promise<CustomerInfo>
  getEntitlements(): Promise<EntitlementsInfo>
  getEntitlementsByProductIds(productIds: string[]): Promise<Entitlement[]>
  getSubscriptionStatus(): Promise<SubscriptionStatus>
  setSubscriptionStatus(status: Record<string, any>): Promise<void>

  setInterfaceStyle(style?: string): void

  getUserAttributes(): Promise<Record<string, any>>
  setUserAttributes(userAttributes: Record<string, any | null>): Promise<void>

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
  didHandleBackPressed(shouldConsume: boolean): void
  didHandleCustomCallback(
    callbackId: string,
    status: string,
    data?: Record<string, any>,
  ): Promise<void>

  restorePurchases(): Promise<RestorationResultResponse>

  dismiss(): Promise<void>
  togglePaywallSpinner(isHidden: boolean): void
  confirmAllAssignments(): Promise<Assignment[]>

  getPresentationResult(
    placement: string,
    params?: Map<string, any> | Record<string, any>,
  ): Promise<PresentationResult>

  getDeviceAttributes(): Promise<Record<string, any>>

  preloadPaywalls(placementNames: string[]): void
  preloadAllPaywalls(): void

  setLogLevel(level: string): void
  setLocaleIdentifier(localeIdentifier: string | null): void
  setEventTrackingBehavior(behavior: string): void

  setIntegrationAttributes(attributes: IntegrationAttributes): Promise<void>
  setIntegrationAttribute(attribute: IntegrationAttribute, value: string | null): Promise<void>
  getIntegrationAttributes(): Promise<Record<string, string>>

  consume(purchaseToken: string): Promise<string>

  /** On Android the identifier can be a full one: `productId:basePlanId:offerId`. */
  purchase(productId: string): Promise<PurchaseResultResponse>
  products(productIds: string[]): Promise<ProductResponse[]>
  queryInAppPurchases(): Promise<OwnedInAppPurchase[]>

  /** Resolves once the Customer Center is dismissed. */
  presentCustomerCenter(
    configuration: Record<string, any> | null,
    handlerId: string,
    asksBeforeRestoring: boolean,
  ): Promise<void>
  dismissCustomerCenter(): Promise<void>
  didHandleCustomerCenterShouldRestorePurchases(requestId: string, proceed: boolean): void

  /** Both native modules define it; callers still check, so a stale native build without it is a no-op. */
  setAdConsent?: (consent: Record<string, string>) => void

  getAdConsent(): Promise<{ adUserData: string; adPersonalization: string }>
  getEventTrackingBehavior(): Promise<string>
  getLocaleIdentifier(): Promise<string | null>
  getLogLevel(): Promise<string>
  getUserId(): Promise<string>
  getIsLoggedIn(): Promise<boolean>
  getIsPaywallPresented(): Promise<boolean>
  getLatestPaywallInfo(): Promise<PaywallInfo | null>
  getOverrideProductsByName(): Promise<Record<string, string>>
  setOverrideProductsByName(overrides: Record<string, string>): void

  refreshConfiguration(): Promise<void>

  cancelAllScheduledNotifications(): void
  /** Android only; the iOS module defines them as no-ops. */
  setPushToken(token: string): void
  optOutOfNotifications(): void
  optInToNotifications(): void
  handleNotificationOpen(data: Record<string, string>): void

  /** iOS only; the Android module defines them as no-ops. */
  removeAllPendingNonSuperwallNotificationRequests(): void
  removeAllDeliveredSuperwallNotifications(): void
  removeAllDeliveredNonSuperwallNotifications(): void
  /** iOS only; the Android module resolves with an empty array. */
  getGrantedEntitlements(): Promise<Entitlement[]>
  setGrantedEntitlements(entitlements: { id: string }[]): Promise<void>

  /** Android only; the iOS module resolves with `false`. */
  showAlert(
    title: string | null,
    message: string | null,
    actionTitle: string | null,
    closeActionTitle: string | null,
  ): Promise<boolean>
  /** Android only; a no-op on iOS. */
  openRestoreOnWeb(): void
  /** Android only; the iOS module resolves with `null`. */
  getExternalAccountId(): Promise<string | null>
  /** Android only; a no-op on iOS. */
  observe(state: PurchasingObserverState): Promise<void>
  /** Android only. Resolves with whether an SDK instance was torn down; always `false` on iOS. */
  teardown(): Promise<boolean>
  /** Android only; a no-op on iOS. */
  dismissForNextPaywall(): Promise<void>
}

export default requireNativeModule<SuperwallExpoModule>("SuperwallExpo")

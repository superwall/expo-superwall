/* eslint-disable @typescript-eslint/no-unused-vars */
// @ts-nocheck TS6133: Unused variable
import { PaywallInfo } from "./PaywallInfo";
import type { RedemptionResult } from "./RedemptionResults";
import { SubscriptionStatus } from "./SubscriptionStatus";
import { SuperwallEventInfo } from "./SuperwallEventInfo";

/**
 * @category Delegate
 * @since 0.0.15
 * Delegate class for handling Superwall events.
 * Provides methods that are called at various points in the Superwall lifecycle.
 * You can override these methods to implement custom logic.
 */
export class SuperwallDelegate {
  /**
   * Called when the user's subscription status changes.
   * @param from The previous subscription status.
   * @param to The new subscription status.
   */
  subscriptionStatusDidChange(
    from: SubscriptionStatus,
    to: SubscriptionStatus,
  ): void {}
  /**
   * Called before the SDK attempts to redeem a promotional link.
   */
  willRedeemLink(): void {}
  /**
   * Called after the SDK has attempted to redeem a promotional link.
   * @param result The result of the redemption attempt.
   */
  didRedeemLink(result: RedemptionResult): void {}
  /**
   * Called for all Superwall internal events.
   * @param eventInfo Information about the Superwall event.
   */
  handleSuperwallEvent(eventInfo: SuperwallEventInfo): void {}
  /**
   * Called when a custom action is triggered from a paywall.
   * @param name The name of the custom action.
   */
  handleCustomPaywallAction(name: string): void {}
  /**
   * Called just before a paywall is dismissed.
   * @param paywallInfo Information about the paywall that will be dismissed.
   */
  willDismissPaywall(paywallInfo: PaywallInfo): void {}
  /**
   * Called just before a paywall is presented.
   * @param paywallInfo Information about the paywall that will be presented.
   */
  willPresentPaywall(paywallInfo: PaywallInfo): void {}
  /**
   * Called after a paywall has been dismissed.
   * @param paywallInfo Information about the paywall that was dismissed.
   */
  didDismissPaywall(paywallInfo: PaywallInfo): void {}
  /**
   * Called after a paywall has been presented.
   * @param paywallInfo Information about the paywall that was presented.
   */
  didPresentPaywall(paywallInfo: PaywallInfo): void {}
  /**
   * Called when the paywall attempts to open a URL.
   * @param url The URL that the paywall will attempt to open.
   */
  paywallWillOpenURL(url: URL): void {}
  /**
   * Called when the paywall attempts to open a deep link.
   * @param url The deep link URL that the paywall will attempt to open.
   */
  paywallWillOpenDeepLink(url: URL): void {}
  /**
   * Called for logging messages from the SDK.
   * @param level The log level.
   * @param scope The scope of the log.
   * @param message The log message.
   * @param info Additional info associated with the log.
   * @param error Error message if applicable.
   */
  handleLog(
    level: string,
    scope: string,
    message?: string,
    info?: Record<string, any> | null,
    error?: string | null,
  ): void {}
}

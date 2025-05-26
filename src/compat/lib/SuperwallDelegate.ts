import type {
  PaywallInfo as MainPaywallInfo,
  RedemptionResult as MainRedemptionResult,
  LogLevel,
  LogScope,
} from '../../SuperwallExpoModule.types';

// Keep local PaywallInfo import for now if it's used as a class instance internally,
// but method signatures will use MainPaywallInfo.
// If the local PaywallInfo class (from ./PaywallInfo) is what's expected by consumers
// of the delegate, this might need further adjustment. For now, aligning signatures.
import { PaywallInfo } from './PaywallInfo'; // This might be the class consumers expect.
import type { RedemptionResult } from './RedemptionResults'; // This will be replaced by MainRedemptionResult in the signature.
import { SubscriptionStatus } from './SubscriptionStatus';
import { SuperwallEventInfo } from './SuperwallEventInfo';

export abstract class SuperwallDelegate {
  abstract subscriptionStatusDidChange(
    from: SubscriptionStatus,
    to: SubscriptionStatus
  ): void;
  abstract willRedeemLink(): void;
  abstract didRedeemLink(result: MainRedemptionResult): void; // Updated
  abstract handleSuperwallEvent(eventInfo: SuperwallEventInfo): void;
  abstract handleCustomPaywallAction(name: string): void;
  abstract willDismissPaywall(paywallInfo: MainPaywallInfo): void; // Updated
  abstract willPresentPaywall(paywallInfo: MainPaywallInfo): void; // Updated
  abstract didDismissPaywall(paywallInfo: MainPaywallInfo): void; // Updated
  abstract didPresentPaywall(paywallInfo: MainPaywallInfo): void; // Updated
  abstract paywallWillOpenURL(url: URL): void;
  abstract paywallWillOpenDeepLink(url: URL): void;
  abstract handleLog(
    level: LogLevel, // Updated
    scope: LogScope, // Updated
    message?: string,
    info?: Map<string, any> | Record<string, any>, // Allow Record for easier JSON mapping
    error?: string
  ): void;
}

// Primarily re-exporting types from the main module via the lib files
import type {
  PaywallInfo as MainPaywallInfo,
  PaywallResult as MainPaywallResult,
  SubscriptionStatus as MainSubscriptionStatus,
  SuperwallEventInfo as MainSuperwallEventInfo,
  RedemptionResult as MainRedemptionResult,
  PaywallSkippedReason as MainPaywallSkippedReason,
  LogLevel as MainLogLevel,
  LogScope as MainLogScope,
  Experiment as MainExperiment,
  Variant as MainVariant,
  VariantType as MainVariantType,
  Entitlement as MainEntitlement,
  EntitlementType as MainEntitlementType,
  Product as MainProduct,
  FeatureGatingBehavior as MainFeatureGatingBehavior,
  PaywallCloseReason as MainPaywallCloseReason,
  LocalNotification as MainLocalNotification,
  LocalNotificationType as MainLocalNotificationType,
  ComputedPropertyRequest as MainComputedPropertyRequest,
  Survey as MainSurvey,
  SurveyOption as MainSurveyOption,
  SurveyShowCondition as MainSurveyShowCondition,
  TriggerResult as MainTriggerResult,
  StoreTransaction as MainStoreTransaction,
  RestoreType as MainRestoreType,
  PaywallPresentationRequestStatus as MainPaywallPresentationRequestStatus,
  PaywallPresentationRequestStatusReason as MainPaywallPresentationRequestStatusReason,
  PaywallPresentationRequestStatusType as MainPaywallPresentationRequestStatusType,
  TransactionProductIdentifier as MainTransactionProductIdentifier,
} from '../../SuperwallExpoModule.types';

// Local compat layer classes/types that are not direct re-exports of main types
// or retain specific class structures for compatibility.
import { Assignment } from "./lib/Assignments"; // Renamed file
import { ConfigurationStatus } from "./lib/ConfigurationStatus";
import { EntitlementsInfo } from "./lib/EntitlementsInfo";
import type { IdentityOptions } from "./lib/IdentityOptions";
import type { InterfaceStyle } from "./lib/InterfaceStyle";
// PaywallInfo from lib is a class that implements MainPaywallInfo and has fromJson
import { PaywallInfo } from "./lib/PaywallInfo";
import type { PaywallPresentationHandler } from "./lib/PaywallPresentationHandler";
// PaywallResult is now a type re-export, fromJson was removed from its file
// import { fromJson as paywallResultFromJson } from "./lib/PaywallResult";
// PaywallSkippedReason is now a type re-export
// import { PaywallSkippedReason } from "./lib/PaywallSkippedReason";
import type { PresentationResult } from "./lib/PresentationResult";
import type { PurchaseController } from "./lib/PurchaseController";
// RedemptionResults is now a type re-export
// import { RedemptionResults } from "./lib/RedemptionResults";
// SubscriptionStatus is now a type re-export
// import { SubscriptionStatus } from "./lib/SubscriptionStatus";
import type { SuperwallDelegate } from "./lib/SuperwallDelegate";
// SuperwallEventInfo is now a type re-export
// import { SuperwallEventInfo } from "./lib/SuperwallEventInfo";
import type { SuperwallOptions } from "./lib/SuperwallOptions";

import { EventEmitter } from "expo";
import { version } from "../../package.json";
import SuperwallExpoModule, { SuperwallExpoModuleEvents } from "../SuperwallExpoModule";

// Re-exporting types that are now sourced from SuperwallExpoModule.types via the lib files
export type { ComputedPropertyRequest } from "./lib/ComputedPropertyRequest";
export type { Experiment, Variant, VariantType } from "./lib/Experiment";
export type { FeatureGatingBehavior } from "./lib/FeatureGatingBehavior";
export type { IdentityOptions }; // Already a type
export type { LocalNotification, LocalNotificationType } from "./lib/LocalNotification";
export type { LogLevel, LogScope } from "./lib/LogLevel"; // LogScope is from its own file
export type { PaywallCloseReason } from "./lib/PaywallCloseReason";
export { PaywallInfo }; // Exporting the class from lib/PaywallInfo.ts
export type { Product, Entitlement, EntitlementType } from "./lib/Product";
export type { PurchaseController }; // Already a type
export type { SubscriptionStatus } from "./lib/SubscriptionStatus";
export type { PurchaseResult } from "./lib/PurchaseResult"; // This still seems to be a local type with sub-types
export {
  PurchaseResultPurchased,
  PurchaseResultPending,
  PurchaseResultCancelled,
  PurchaseResultFailed,
  PurchaseResultRestored,
} from "./lib/PurchaseResult";
export type { RestorationResult } from "./lib/RestorationResult";
export type { InterfaceStyle }; // Already a type
export { ConfigurationStatus }; // Exporting class
export type { SuperwallDelegate }; // Already a type
export type { SuperwallEventInfo, SuperwallEvent } from "./lib/SuperwallEventInfo"; // EventType was removed
export type { SuperwallOptions }; // Already a type
export type { Survey, SurveyOption, SurveyShowCondition } from "./lib/Survey";
export type { TriggerResult } from "./lib/TriggerResult";
export type {
  PaywallOptions,
  TransactionBackgroundView,
} from "./lib/PaywallOptions";
export type { PaywallPresentationHandler }; // Already a type
export type {
  PaywallPresentationRequestStatus,
  PaywallPresentationRequestStatusReason,
  PaywallPresentationRequestStatusType,
} from "./lib/PaywallPresentationRequestStatus";
export type { PaywallSkippedReason } from "./lib/PaywallSkippedReason";
// Individual skipped reasons might need to be handled differently if they were classes/objects
// For now, assuming PaywallSkippedReason is a discriminated union as per main types.
// export {
//   PaywallSkippedReasonPlacementNotFound,
//   PaywallSkippedReasonHoldout,
//   PaywallSkippedReasonNoAudienceMatch,
//   PaywallSkippedReasonUserIsSubscribed,
// } from "./lib/PaywallSkippedReason";
export type { RestoreType, StoreTransaction } from "./lib/RestoreType";
export { EntitlementsInfo }; // Exporting class
export * from "./lib/RedemptionResults"; // Exports types like RedemptionResult

interface UserAttributes {
  [key: string]: any;
}

export default class Superwall {
  static purchaseController?: PurchaseController;
  private static delegate?: SuperwallDelegate;
  private static _superwall = new Superwall();

  private static configEmitter = new EventEmitter<{
    configured: (isConfigured: boolean) => void;
  }>();
  private static didConfigure = false;
  private presentationHandlers: Map<string, PaywallPresentationHandler> = new Map();
  subscriptionStatusEmitter = new EventEmitter<{
    change: (status: MainSubscriptionStatus) => void; // Use main type
  }>();

  private static setDidConfigure(didConfigure: boolean) {
    Superwall.didConfigure = didConfigure;
    if (didConfigure) {
      Superwall.configEmitter.emit("configured", didConfigure);
    }
  }

  private async awaitConfig(): Promise<void> {
    if (Superwall.didConfigure) {
      return;
    }
    await new Promise<void>((resolve) => {
      Superwall.configEmitter.addListener("configured", () => {
        resolve();
      });
    });
  }

  private constructor() {
    SuperwallExpoModule.addListener("onPurchase", async (data) => {
      // This part remains largely the same as PurchaseController and PurchaseResult are local compat types
      if (data.platform === "ios") {
        const purchaseResult = await Superwall.purchaseController?.purchaseFromAppStore(
          data.productId,
        );
        if (purchaseResult == null) {
          return;
        }
        await SuperwallExpoModule.didPurchase(purchaseResult.toJSON()); // Assuming PurchaseResult has toJSON
        return;
      }

      if (data.platform === "android") {
        const purchaseResult = await Superwall.purchaseController?.purchaseFromGooglePlay(
          data.productId,
          data.basePlanId,
          data.offerId,
        );
        if (purchaseResult == null) {
          return;
        }
        await SuperwallExpoModule.didPurchase(purchaseResult.toJSON()); // Assuming PurchaseResult has toJSON
      }
    });

    SuperwallExpoModule.addListener("onPurchaseRestore", async () => {
      // This part remains largely the same as PurchaseController and RestorationResult are local compat types
      const restorationResult = await Superwall.purchaseController?.restorePurchases();
      if (restorationResult == null) {
        return;
      }
      await SuperwallExpoModule.didRestore(restorationResult.toJson()); // Assuming RestorationResult has toJson
    });

    SuperwallExpoModule.addListener("onPaywallPresent", (data: SuperwallExpoModuleEvents['onPaywallPresent']) => {
      const handler = this.presentationHandlers.get(data.handlerId);
      if (!handler) {
        return;
      }
      // The `data` param is already typed by SuperwallExpoModuleEvents
      // We still use PaywallInfo.fromJson because the compat PaywallInfo class handles timestamp conversion etc.
      // and implements MainPaywallInfo.
      const paywallInfo = PaywallInfo.fromJson(data.paywallInfoJson);

      // Check which specific handler method is being called based on `data.method` was a previous pattern.
      // The new SuperwallExpoModuleEvents directly calls specific event names.
      // This needs to be reconciled. For now, assuming onPaywallPresent only calls onPresentHandler.
      // The provided `data` for `onPaywallPresent` in `SuperwallExpoModuleEvents` is `{ paywallInfoJson: MainPaywallInfo; handlerId: string }`
      // It does not have a `method` field. This implies the structure of how handlers are called might need a broader change.
      // For now, I'll adapt based on the direct event.

      if (handler.onPresentHandler) {
         handler.onPresentHandler(paywallInfo);
      }
    });

    // This listener needs to be split or the data structure from native needs to be confirmed.
    // The old code used `data.method` to differentiate.
    // The new `SuperwallExpoModuleEvents` has distinct events: `onPaywallDismiss`, `onPaywallError`, `onPaywallSkip`.
    // I will assume these are now separate listeners.

    SuperwallExpoModule.addListener('onPaywallDismiss', (data: SuperwallExpoModuleEvents['onPaywallDismiss']) => {
      const handler = this.presentationHandlers.get(data.handlerId);
      if (handler?.onDismissHandler) {
        const paywallInfo = PaywallInfo.fromJson(data.paywallInfoJson);
        // data.result is already MainPaywallResult, no fromJson needed.
        handler.onDismissHandler(paywallInfo, data.result as MainPaywallResult);
      }
    });

    SuperwallExpoModule.addListener('onPaywallError', (data: SuperwallExpoModuleEvents['onPaywallError']) => {
      const handler = this.presentationHandlers.get(data.handlerId);
      if (handler?.onErrorHandler) {
        handler.onErrorHandler(data.errorString);
      }
    });

    SuperwallExpoModule.addListener('onPaywallSkip', (data: SuperwallExpoModuleEvents['onPaywallSkip']) => {
      const handler = this.presentationHandlers.get(data.handlerId);
      if (handler?.onSkipHandler) {
        // data.skippedReason is already MainPaywallSkippedReason
        handler.onSkipHandler(data.skippedReason as MainPaywallSkippedReason);
      }
    });


    SuperwallExpoModule.addListener("subscriptionStatusDidChange", async (data: SuperwallExpoModuleEvents['subscriptionStatusDidChange']) => {
      // data.from and data.to are already MainSubscriptionStatus
      Superwall.delegate?.subscriptionStatusDidChange(data.from, data.to);
    });

    SuperwallExpoModule.addListener("handleSuperwallEvent", async (data: SuperwallExpoModuleEvents['handleSuperwallEvent']) => {
      // data.eventInfo is already MainSuperwallEventInfo
      Superwall.delegate?.handleSuperwallEvent(data.eventInfo as MainSuperwallEventInfo);
    });

    SuperwallExpoModule.addListener("handleCustomPaywallAction", async (data: SuperwallExpoModuleEvents['handleCustomPaywallAction']) => {
      Superwall.delegate?.handleCustomPaywallAction(data.name);
    });

    SuperwallExpoModule.addListener("willDismissPaywall", async (data: SuperwallExpoModuleEvents['willDismissPaywall']) => {
      // data.info is already MainPaywallInfo, but delegate expects compat PaywallInfo class instance
      const info = PaywallInfo.fromJson(data.info);
      Superwall.delegate?.willDismissPaywall(info);
    });

    SuperwallExpoModule.addListener("willPresentPaywall", async (data: SuperwallExpoModuleEvents['willPresentPaywall']) => {
      const info = PaywallInfo.fromJson(data.info);
      Superwall.delegate?.willPresentPaywall(info);
    });

    SuperwallExpoModule.addListener("didDismissPaywall", async (data: SuperwallExpoModuleEvents['didDismissPaywall']) => {
      const info = PaywallInfo.fromJson(data.info);
      Superwall.delegate?.didDismissPaywall(info);
    });

    SuperwallExpoModule.addListener("didPresentPaywall", async (data: SuperwallExpoModuleEvents['didPresentPaywall']) => {
      const info = PaywallInfo.fromJson(data.info);
      Superwall.delegate?.didPresentPaywall(info);
    });

    SuperwallExpoModule.addListener("handleLog", async (data: SuperwallExpoModuleEvents['handleLog']) => {
      // data.level, data.scope etc are already the correct main types
      Superwall.delegate?.handleLog(data.level, data.scope, data.message, data.info, data.error);
    });

    SuperwallExpoModule.addListener("paywallWillOpenDeepLink", async (data: SuperwallExpoModuleEvents['paywallWillOpenDeepLink']) => {
      const url = new URL(data.url);
      Superwall.delegate?.paywallWillOpenDeepLink(url);
    });

    SuperwallExpoModule.addListener("paywallWillOpenURL", async (data: SuperwallExpoModuleEvents['paywallWillOpenURL']) => {
      const url = new URL(data.url);
      Superwall.delegate?.paywallWillOpenURL(url);
    });

    SuperwallExpoModule.addListener("willRedeemLink", async (data: SuperwallExpoModuleEvents['willRedeemLink']) => {
      // data is Record<string, never> | null
      Superwall.delegate?.willRedeemLink();
    });

    SuperwallExpoModule.addListener("didRedeemLink", async (data: SuperwallExpoModuleEvents['didRedeemLink']) => {
      // data is already MainRedemptionResult
      Superwall.delegate?.didRedeemLink(data as MainRedemptionResult);
    });
  }

  static get shared(): Superwall {
    return Superwall._superwall;
  }

  static async configure({
    apiKey,
    options,
    purchaseController,
    completion,
  }: {
    apiKey: string;
    options?: SuperwallOptions; // SuperwallOptions is a local compat type
    purchaseController?: PurchaseController; // PurchaseController is a local compat type
    completion?: () => void;
  }): Promise<Superwall> {
    Superwall.purchaseController = purchaseController;
    await SuperwallExpoModule.configure(
      apiKey,
      options?.toJson(), // Assuming SuperwallOptions has toJson
      !!purchaseController,
      `${version}compat`,
    ).then(() => {
      if (completion) completion();
    });
    Superwall.setDidConfigure(true);
    return Superwall._superwall;
  }

  async identify({
    userId,
    options,
  }: {
    userId: string;
    options?: IdentityOptions; // IdentityOptions is a local compat type
  }): Promise<void> {
    await this.awaitConfig();
    const serializedOptions = options ? options.toJson() : null; // Assuming IdentityOptions has toJson
    await SuperwallExpoModule.identify(userId, serializedOptions);
  }

  async reset(): Promise<void> {
    await this.awaitConfig();
    await SuperwallExpoModule.reset();
  }

  async handleDeepLink(url: string): Promise<boolean> {
    await this.awaitConfig();
    return await SuperwallExpoModule.handleDeepLink(url);
  }

  async register(params: {
    placement: string;
    params?: Map<string, any> | Record<string, any>;
    handler?: PaywallPresentationHandler; // PaywallPresentationHandler is a local compat type
    feature?: () => void;
  }): Promise<void> {
    await this.awaitConfig();
    let handlerId: string | null = null;
    if (params.handler) {
      const uuid = (+new Date() * Math.random()).toString(36);
      this.presentationHandlers.set(uuid, params.handler);
      handlerId = uuid;
    }
    let paramsObject = {};
    if (params.params) {
      paramsObject =
        params.params instanceof Map ? Object.fromEntries(params.params) : params.params;
    }
    if (params.feature) {
      return await SuperwallExpoModule.registerPlacement(
        params.placement,
        paramsObject,
        handlerId,
      ).then(() => {
        params.feature!();
      });
    }
    return SuperwallExpoModule.registerPlacement(params.placement, paramsObject, handlerId);
  }

  async confirmAllAssignments(): Promise<Assignment[]> { // Assignment is a local compat class
    await this.awaitConfig();
    const assignmentsData = await SuperwallExpoModule.confirmAllAssignments();
    return assignmentsData.map((assignment: any) => Assignment.fromJson(assignment)); // Uses Assignment.fromJson
  }

  async getAssignments(): Promise<Assignment[]> { // Assignment is a local compat class
    await this.awaitConfig();
    const assignmentsData = await SuperwallExpoModule.getAssignments();
    return assignmentsData.map((assignment: any) => Assignment.fromJson(assignment)); // Uses Assignment.fromJson
  }

  async getPresentationResult({
    placement,
    params,
  }: {
    placement: string;
    params?: Map<string, any>;
  }): Promise<PresentationResult> { // PresentationResult is a local compat type
    await this.awaitConfig();
    let paramsObject = {};
    if (params) {
      paramsObject = Object.fromEntries(params);
    }
    // Assuming the native module returns data compatible with PresentationResult (which is currently `any`)
    return await SuperwallExpoModule.getPresentationResult(placement, paramsObject) as PresentationResult;
  }

  async getConfigurationStatus(): Promise<ConfigurationStatus> { // ConfigurationStatus is a local compat class
    const configurationStatusString = await SuperwallExpoModule.getConfigurationStatus();
    return ConfigurationStatus.fromString(configurationStatusString); // Uses ConfigurationStatus.fromString
  }

  async getEntitlements(): Promise<EntitlementsInfo> { // EntitlementsInfo is a local compat class
    await this.awaitConfig();
    const entitlementsJson = await SuperwallExpoModule.getEntitlements();
    return EntitlementsInfo.fromObject(entitlementsJson); // Uses EntitlementsInfo.fromObject
  }

  async setSubscriptionStatus(status: MainSubscriptionStatus): Promise<void> { // Parameter is MainSubscriptionStatus
    await this.awaitConfig();
    // The native module expects the JSON representation of SubscriptionStatus
    await SuperwallExpoModule.setSubscriptionStatus(status);
  }

  async getSubscriptionStatus(): Promise<MainSubscriptionStatus> { // Return is MainSubscriptionStatus
    await this.awaitConfig();
    const subscriptionStatusData = await SuperwallExpoModule.getSubscriptionStatus();
    // Assuming subscriptionStatusData.subscriptionStatus is already MainSubscriptionStatus
    return subscriptionStatusData.subscriptionStatus as MainSubscriptionStatus;
  }

  async setInterfaceStyle(style: InterfaceStyle | null): Promise<void> { // InterfaceStyle is local compat
    await SuperwallExpoModule.setInterfaceStyle(style?.toString());
  }

  async setDelegate(delegate: SuperwallDelegate | undefined): Promise<void> { // SuperwallDelegate is local compat
    await this.awaitConfig();
    Superwall.delegate = delegate;
    await SuperwallExpoModule.setDelegate(delegate === undefined);
  }

  async getUserAttributes(): Promise<UserAttributes> {
    await this.awaitConfig();
    const userAttributes: UserAttributes = await SuperwallExpoModule.getUserAttributes();
    return userAttributes;
  }

  async preloadAllPaywalls(): Promise<void> {
    await this.awaitConfig();
    await SuperwallExpoModule.preloadAllPaywalls();
  }

  async preloadPaywalls(placementNames: Set<string>): Promise<void> {
    await this.awaitConfig();
    await SuperwallExpoModule.preloadPaywalls(Array.from(placementNames));
  }

  async setUserAttributes(userAttributes: UserAttributes): Promise<void> {
    await this.awaitConfig();
    await SuperwallExpoModule.setUserAttributes(userAttributes);
  }

  async dismiss(): Promise<void> {
    await SuperwallExpoModule.dismiss();
  }

  async setLogLevel(level: MainLogLevel): Promise<void> { // Parameter is MainLogLevel
    await SuperwallExpoModule.setLogLevel(level.toString());
  }
}

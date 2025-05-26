// src/SuperwallExpoModule.types.ts

/**
 * Defines the type of an experiment variant.
 * - `TREATMENT`: The user is in the treatment group and will see a paywall.
 * - `HOLDOUT`: The user is in the holdout group and will not see a paywall.
 */
export type VariantType = "TREATMENT" | "HOLDOUT"

/**
 * Represents an experiment variant.
 */
export interface Variant {
  /**
   * The identifier of the variant.
   */
  id: string
  /**
   * The type of the variant.
   */
  type: VariantType
  /**
   * The identifier of the paywall associated with this variant, if any.
   * Will be an empty string if no paywall is associated (e.g., for a holdout variant).
   */
  paywallId: string
}

/**
 * Represents a Superwall experiment.
 */
export interface Experiment {
  /**
   * The identifier of the experiment.
   */
  id: string
  /**
   * The identifier of the group the experiment belongs to.
   */
  groupId: string
  /**
   * The variant of the experiment that the user was assigned to.
   */
  variant: Variant
}

/**
 * Defines the type of an entitlement.
 * - `SERVICE_LEVEL`: Indicates a service level entitlement.
 */
export type EntitlementType = "SERVICE_LEVEL"

/**
 * Represents a user entitlement.
 */
export interface Entitlement {
  /**
   * The identifier of the entitlement.
   */
  id: string
  /**
   * The type of the entitlement.
   */
  type: EntitlementType
}

/**
 * Represents the reason why a paywall was skipped.
 */
export type PaywallSkippedReason =
  | {
      /**
       * The user was part of a holdout group in an experiment.
       */
      type: "Holdout"
      /**
       * The experiment details.
       */
      experiment: Experiment
    }
  | {
      /**
       * No audience rule matched for the user.
       */
      type: "NoAudienceMatch"
    }
  | {
      /**
       * The specified placement was not found.
       */
      type: "PlacementNotFound"
    }

/**
 * Represents the result of a paywall presentation.
 */
export type PaywallResult =
  | {
      /**
       * The user purchased a product.
       */
      type: "purchased"
      /**
       * The identifier of the product that was purchased.
       */
      productId: string
    }
  | {
      /**
       * The user declined the paywall.
       */
      type: "declined"
    }
  | {
      /**
       * The user successfully restored their purchases.
       */
      type: "restored"
    }

/**
 * Represents the subscription status of the user.
 */
export type SubscriptionStatus =
  | {
      /**
       * The subscription status is unknown.
       */
      status: "UNKNOWN"
    }
  | {
      /**
       * The user has an inactive subscription.
       */
      status: "INACTIVE"
    }
  | {
      /**
       * The user has an active subscription.
       */
      status: "ACTIVE"
      /**
       * A list of entitlements the user has.
       */
      entitlements: Entitlement[]
    }

/**
 * The reason why a paywall was closed.
 * - `systemLogic`: Closed due to system logic.
 * - `forNextPaywall`: Closed to present another paywall.
 * - `webViewFailedToLoad`: The web view failed to load the paywall content.
 * - `manualClose`: Closed by a manual action (e.g., user tapping a close button).
 * - `none`: No specific close reason.
 */
export type PaywallCloseReason =
  | "systemLogic"
  | "forNextPaywall"
  | "webViewFailedToLoad"
  | "manualClose"
  | "none"

/**
 * Defines the behavior for feature gating.
 * - `gated`: The feature is gated and requires a specific condition to be met.
 * - `nonGated`: The feature is not gated and is available to all users.
 */
export type FeatureGatingBehavior = "gated" | "nonGated"

/**
 * Conditions for when a survey should be shown.
 * - `ON_MANUAL_CLOSE`: Show the survey when the paywall is manually closed.
 * - `ON_PURCHASE`: Show the survey after a successful purchase.
 */
export type SurveyShowCondition = "ON_MANUAL_CLOSE" | "ON_PURCHASE"

/**
 * Represents an option in a survey.
 */
export interface SurveyOption {
  /**
   * The unique identifier for the survey option.
   */
  id: string
  /**
   * The text displayed for the survey option.
   */
  title: string
}

/**
 * Represents a survey that can be presented to the user.
 */
export interface Survey {
  /**
   * The unique identifier for the survey.
   */
  id: string
  /**
   * The key used for assigning the survey to a user.
   */
  assignmentKey: string
  /**
   * The title of the survey.
   */
  title: string
  /**
   * The message or description of the survey.
   */
  message: string
  /**
   * A list of options available in the survey.
   */
  options: SurveyOption[]
  /**
   * The condition under which the survey should be presented.
   */
  presentationCondition: SurveyShowCondition
  /**
   * The probability (0.0 to 1.0) of presenting the survey.
   */
  presentationProbability: number
  /**
   * Indicates whether an "Other" option should be included in the survey.
   */
  includeOtherOption: boolean
  /**
   * Indicates whether a close button/option should be included in the survey.
   */
  includeCloseOption: boolean
}

/**
 * The type of local notification.
 * - `trialStarted`: Notification for when a trial has started.
 */
export type LocalNotificationType = "trialStarted"

/**
 * Represents a local notification.
 */
export interface LocalNotification {
  /**
   * The type of the local notification.
   */
  type: LocalNotificationType
  /**
   * The title of the notification.
   */
  title: string
  /**
   * The main body text of the notification.
   */
  body: string
  /**
   * The delay in seconds before the notification is shown.
   */
  delay: number
  /**
   * The subtitle of the notification, if any.
   */
  subtitle?: string
}

/**
 * Represents a request for a computed property.
 */
export interface ComputedPropertyRequest {
  /**
   * The type of the computed property.
   */
  type: string
  /**
   * The name of the placement associated with this request, if any.
   */
  placementName: string
}

/**
 * Represents a product available for purchase on a paywall.
 * This is a simplified definition specific to PaywallInfo.
 */
export interface Product {
  /**
   * The unique identifier of the product.
   */
  id: string
  /**
   * The name of the product, if available.
   */
  name?: string
  /**
   * A list of entitlements granted by purchasing this product.
   */
  entitlements: Entitlement[]
}

/**
 * Contains information about a paywall.
 */
export interface PaywallInfo {
  /**
   * The identifier of the paywall.
   */
  identifier: string
  /**
   * The name of the paywall.
   */
  name: string
  /**
   * The URL of the paywall.
   */
  url: string
  /**
   * The experiment associated with this paywall, if any.
   */
  experiment?: Experiment
  /**
   * A list of products available on the paywall.
   */
  products: Product[]
  /**
   * A list of product identifiers available on the paywall.
   */
  productIds: string[]
  /**
   * The name of the event that triggered the presentation of this paywall, if applicable.
   * Corresponds to `presentedByPlacementWithName` in Swift.
   */
  presentedByEventWithName?: string
  /**
   * The identifier of the event that triggered the presentation of this paywall, if applicable.
   * Corresponds to `presentedByPlacementWithId` in Swift.
   */
  presentedByEventWithId?: string
  /**
   * The timestamp of when the event triggering this paywall occurred, if applicable.
   * Corresponds to `presentedByPlacementAt` in Swift.
   */
  presentedByEventAt?: number
  /**
   * The source that presented the paywall (e.g., "implicit", "explicit", "getPaywall").
   */
  presentedBy: string
  /**
   * The type of the source that led to the paywall presentation (e.g., "Register", "Track").
   */
  presentationSourceType?: string
  /**
   * The timestamp when the request to load the paywall response started.
   */
  responseLoadStartTime?: number
  /**
   * The timestamp when the paywall response successfully loaded.
   */
  responseLoadCompleteTime?: number
  /**
   * The timestamp when the paywall response failed to load.
   */
  responseLoadFailTime?: number
  /**
   * The duration (in seconds or milliseconds) it took to load the paywall response.
   */
  responseLoadDuration?: number
  /**
   * Indicates whether a free trial is available for any of the products on this paywall.
   */
  isFreeTrialAvailable: boolean
  /**
   * The feature gating behavior for this paywall.
   */
  featureGatingBehavior: FeatureGatingBehavior
  /**
   * The reason why the paywall was closed.
   */
  closeReason: PaywallCloseReason
  /**
   * The timestamp when the web view started loading the paywall content.
   */
  webViewLoadStartTime?: number
  /**
   * The timestamp when the web view successfully loaded the paywall content.
   */
  webViewLoadCompleteTime?: number
  /**
   * The timestamp when the web view failed to load the paywall content.
   */
  webViewLoadFailTime?: number
  /**
   * The duration (in seconds or milliseconds) it took for the web view to load.
   */
  webViewLoadDuration?: number
  /**
   * The timestamp when the loading of products started.
   */
  productsLoadStartTime?: number
  /**
   * The timestamp when the products successfully loaded.
   */
  productsLoadCompleteTime?: number
  /**
   * The timestamp when the products failed to load.
   */
  productsLoadFailTime?: number
  /**
   * The duration (in seconds or milliseconds) it took to load the products.
   */
  productsLoadDuration?: number
  /**
   * The version of paywall.js used, if available.
   */
  paywalljsVersion?: string
  /**
   * A list of computed property requests associated with this paywall.
   */
  computedPropertyRequests: ComputedPropertyRequest[]
  /**
   * A list of surveys associated with this paywall.
   */
  surveys: Survey[]
  /**
   * A list of local notifications associated with this paywall.
   */
  localNotifications: LocalNotification[]
}

/**
 * Information about an error during redemption.
 */
export interface RedemptionErrorInfo {
  /**
   * The error message.
   */
  message: string
}

/**
 * Information about an expired redemption code.
 */
export interface RedemptionExpiredCodeInfo {
  /**
   * Whether the code was resent.
   */
  resent: boolean
  /**
   * The obfuscated email if the code was resent.
   */
  obfuscatedEmail?: string
}

/**
 * Defines who owns the redeemed item.
 */
export type RedemptionOwnership =
  | {
      /**
       * Ownership is tied to an application user ID.
       */
      type: "APP_USER"
      /**
       * The application user ID.
       */
      appUserId: string
    }
  | {
      /**
       * Ownership is tied to a device ID.
       */
      type: "DEVICE"
      /**
       * The device ID.
       */
      deviceId: string
    }

/**
 * Store-specific identifiers for a purchaser.
 */
export type RedemptionStoreIdentifiers =
  | {
      /**
       * The store is Stripe.
       */
      store: "STRIPE"
      /**
       * The Stripe customer ID.
       */
      stripeCustomerId: string
      /**
       * The Stripe subscription IDs.
       */
      stripeSubscriptionIds: string[]
    }
  | {
      /**
       * The store is unknown or not explicitly handled.
       */
      store: string
      /**
       * Allows for additional properties for unknown stores.
       */
      [key: string]: any
    }

/**
 * Information about the purchaser during redemption.
 */
export interface RedemptionPurchaserInfo {
  /**
   * The application user ID of the purchaser.
   */
  appUserId: string
  /**
   * The email of the purchaser, if available.
   */
  email?: string
  /**
   * Store-specific identifiers for the purchaser.
   */
  storeIdentifiers: RedemptionStoreIdentifiers
}

/**
 * Information about the paywall related to the redemption.
 */
export interface RedemptionPaywallInfo {
  /**
   * The identifier of the paywall.
   */
  identifier: string
  /**
   * The name of the placement that triggered the paywall.
   */
  placementName: string
  /**
   * Parameters associated with the placement.
   */
  placementParams: Record<string, any>
  /**
   * The ID of the variant shown.
   */
  variantId: string
  /**
   * The ID of the experiment.
   */
  experimentId: string
}

/**
 * Detailed information about a successful redemption or an expired subscription with prior redemption.
 */
export interface RedemptionInfo {
  /**
   * Information about who owns the redeemed item.
   */
  ownership: RedemptionOwnership
  /**
   * Information about the purchaser.
   */
  purchaserInfo: RedemptionPurchaserInfo
  /**
   * Information about the paywall, if applicable to the redemption.
   */
  paywallInfo?: RedemptionPaywallInfo
  /**
   * A list of entitlements granted by this redemption.
   */
  entitlements: Entitlement[]
}

/**
 * Represents the result of a promotional code redemption attempt.
 */
export type RedemptionResult =
  | {
      /**
       * The redemption was successful.
       */
      status: "SUCCESS"
      /**
       * The redeemed code.
       */
      code: string
      /**
       * Detailed information about the redemption.
       */
      redemptionInfo: RedemptionInfo
    }
  | {
      /**
       * An error occurred during redemption.
       */
      status: "ERROR"
      /**
       * The code used in the attempt.
       */
      code: string
      /**
       * Information about the error.
       */
      error: RedemptionErrorInfo
    }
  | {
      /**
       * The redemption code has expired.
       */
      status: "CODE_EXPIRED"
      /**
       * The expired code.
       */
      code: string
      /**
       * Information about the expired code.
       */
      expired: RedemptionExpiredCodeInfo
    }
  | {
      /**
       * The redemption code is invalid.
       */
      status: "INVALID_CODE"
      /**
       * The invalid code.
       */
      code: string
    }
  | {
      /**
       * The subscription associated with the code has expired, but the code was previously redeemed.
       */
      status: "EXPIRED_SUBSCRIPTION"
      /**
       * The code associated with the expired subscription.
       */
      code: string
      /**
       * Detailed information about the prior redemption.
       */
      redemptionInfo: RedemptionInfo
    }

/**
 * Represents the result of a trigger evaluation.
 */
export type TriggerResult =
  | {
      /** The specified placement was not found. */
      result: "placementNotFound"
    }
  | {
      /** No audience rule matched for the user. */
      result: "noAudienceMatch"
    }
  | {
      /** A paywall will be shown. */
      result: "paywall"
      /** The experiment details. */
      experiment: Experiment
    }
  | {
      /** The user is in a holdout group. */
      result: "holdout"
      /** The experiment details. */
      experiment: Experiment
    }
  | {
      /** An error occurred during trigger evaluation. */
      result: "error"
      /** The error message. */
      error: string
    }

/**
 * Represents the identifier of a product involved in a transaction event.
 */
export interface TransactionProductIdentifier {
  /** The product ID. */
  id: string
}

/**
 * Represents a store transaction. Dates are in ISO 8601 format.
 */
export interface StoreTransaction {
  /** The request ID from the configuration. */
  configRequestId?: string
  /** The ID of the app session. */
  appSessionId?: string
  /** The date of the transaction. */
  transactionDate?: string // ISO 8601
  /** The original transaction identifier. */
  originalTransactionIdentifier?: string
  /** The store's transaction identifier. */
  storeTransactionId?: string
  /** The original purchase date of the transaction. */
  originalPurchaseDate?: string // ISO 8601
  /** The product identifier. */
  productIdentifier?: string
  /** The quantity of the product. */
  quantity?: number
  /** The web order line item ID (for auto-renewable subscriptions). */
  webOrderLineItemId?: string
  /** The promotional offer identifier. */
  promotionalOfferIdentifier?: string
  /** The subscription group identifier. */
  subscriptionGroupIdentifier?: string
  /** Indicates if it's an upsell. */
  isUpgraded?: boolean
  /** The expiration date of the transaction. */
  expirationDate?: string // ISO 8601
  /** The revocation date of the transaction, if applicable. */
  revocationDate?: string // ISO 8601
  /** The App Store account token. */
  appAccountToken?: string
  /** The purchasing storefront's country code. */
  storefrontCountryCode?: string
  /** The purchasing storefront's identifier. */
  storefrontId?: string
  /** The type of product purchased (e.g., "autoRenewable"). */
  productType?: string // e.g., "autoRenewable", "nonConsumable", "consumable", "nonRenewing"
  /** The reason for revocation, if applicable. */
  revocationReason?: string // e.g., "developerIssue", "other"
  /** The environment of the transaction (e.g., "Production", "Sandbox"). */
  environment?: string
  /** The type of ownership (e.g., "PURCHASED", "FAMILY_SHARED"). */
  ownershipType?: string
  /** The currency code of the price. */
  price?: string
  /** The currency code. */
  currency?: string
  /** The offer type (e.g., "INTRODUCTORY", "PROMOTIONAL", "CODE"). */
  offerType?: string // e.g., "INTRODUCTORY", "PROMOTIONAL", "CODE"
  /** The payment mode for an introductory offer (e.g., "PAY_AS_YOU_GO", "PAY_UP_FRONT", "FREE_TRIAL"). */
  offerPaymentMode?: string
}

/**
 * Represents the type of restoration.
 */
export type RestoreType =
  | {
      /** The restoration occurred via a purchase action. */
      type: "viaPurchase"
      /** The store transaction associated with the purchase, if available. */
      storeTransaction?: StoreTransaction
    }
  | {
      /** The restoration occurred via a manual restore action. */
      type: "viaRestore"
    }

/**
 * Possible statuses for a paywall presentation request.
 */
export type PaywallPresentationRequestStatusType = "presentation" | "noPresentation" | "timeout"

/**
 * Represents the status of a paywall presentation request.
 */
export interface PaywallPresentationRequestStatus {
  /** The status of the presentation request. */
  status: PaywallPresentationRequestStatusType
}

/**
 * Represents the reason for a paywall presentation request status.
 */
export type PaywallPresentationRequestStatusReason =
  | {
      /** The debugger is active and presented a paywall. */
      reason: "debuggerPresented"
    }
  | {
      /** A paywall was already presented. */
      reason: "paywallAlreadyPresented"
    }
  | {
      /** The user is in a holdout group. */
      reason: "holdout"
      /** The experiment details. */
      experiment: Experiment
    }
  | {
      /** No audience rule matched for the user. */
      reason: "noRuleMatch"
    }
  | {
      /** An event was not found. */
      reason: "eventNotFound"
    }
  | {
      /** A paywall is not available in the_products coordinator. */
      reason: "noPaywallViewController"
    }
  | {
      /** No view controller available to present the paywall. */
      reason: "noViewController"
    }
  | {
      /** The user is subscribed. */
      reason: "userIsSubscribed"
    }
  | {
      /** An error occurred. */
      reason: "error"
      /** The error details. */
      error: string // Assuming this will be a string representation of the error
    }
  | {
      /** The paywall is gated. */
      reason: "paywallIsGated"
      /** Information about the paywall that was gated. */
      paywallInfo: PaywallInfo // Assuming PaywallInfo is relevant here as per typical gating scenarios
    }

/**
 * Represents a Superwall event that can be tracked.
 */
export type SuperwallEvent =
  | { event: "firstSeen" }
  | { event: "appOpen" }
  | { event: "appLaunch" }
  | { event: "identityAlias" }
  | { event: "appInstall" }
  | { event: "sessionStart" }
  | { event: "reset" }
  | { event: "configRefresh" }
  | { event: "configFail" }
  | { event: "configAttributes" }
  | { event: "confirmAllAssignments" }
  | { event: "touchesBegan" }
  | { event: "surveyClose" }
  | { event: "restoreStart" }
  | { event: "restoreComplete" }
  | { event: "restoreFail"; message: string }
  | { event: "adServicesTokenRequestStart" }
  | { event: "adServicesTokenRequestFail"; error: string }
  | { event: "adServicesTokenRequestComplete"; token: string }
  | { event: "shimmerViewStart" }
  | { event: "shimmerViewComplete" }
  | { event: "redemptionStart" }
  | { event: "redemptionComplete" } // Assuming no specific payload needed beyond the event type
  | { event: "redemptionFail" } // Assuming no specific payload needed beyond the event type
  | { event: "enrichmentStart" }
  | {
      event: "enrichmentComplete"
      userEnrichment?: Record<string, any>
      deviceEnrichment?: Record<string, any>
    }
  | { event: "enrichmentFail" }
  | { event: "unknown" }
  | {
      /** Device attributes were updated. */
      event: "deviceAttributes"
      /** The updated device attributes. */
      attributes: Record<string, any>
    }
  | {
      /** Subscription status changed. Specific data might be in the dedicated listener. */
      event: "subscriptionStatusDidChange"
      /** Current subscription status. */
      subscriptionStatus: SubscriptionStatus // Added to match native SDK behavior
    }
  | { event: "appClose" }
  | {
      /** A deep link was opened. */
      event: "deepLink"
      /** The URL of the deep link. */
      url: string
    }
  | {
      /** A trigger was fired. */
      event: "triggerFire"
      /** The name of the event or placement that fired the trigger. */
      placementName: string // Changed from eventName to placementName to match Swift
      /** The result of the trigger evaluation. */
      result: TriggerResult
    }
  | {
      /** A paywall was opened. */
      event: "paywallOpen"
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** A paywall was closed. */
      event: "paywallClose"
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** User declined a paywall. */
      event: "paywallDecline"
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** A transaction was started. */
      event: "transactionStart"
      /** The product involved in the transaction. */
      product: TransactionProductIdentifier
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** A transaction failed. */
      event: "transactionFail"
      /** The error message from the transaction failure. */
      error: string
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** A transaction was abandoned. */
      event: "transactionAbandon"
      /** The product involved in the transaction. */
      product: TransactionProductIdentifier
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** A transaction was completed. */
      event: "transactionComplete"
      /** The store transaction details, if available. */
      transaction?: StoreTransaction
      /** The product involved in the transaction. */
      product: TransactionProductIdentifier
      /** The type of the transaction (e.g., "purchase", "restore"). */
      type: string // From Swift: transaction.transactionType.rawValue
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** A subscription was started. */
      event: "subscriptionStart"
      /** The product for which the subscription started. */
      product: TransactionProductIdentifier
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** A free trial was started. */
      event: "freeTrialStart"
      /** The product for which the free trial started. */
      product: TransactionProductIdentifier
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** A transaction was restored. */
      event: "transactionRestore"
      /** The type of restoration. */
      restoreType: RestoreType
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** A transaction timed out. */
      event: "transactionTimeout"
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** User attributes were updated. */
      event: "userAttributes"
      /** The updated user attributes. */
      attributes: Record<string, any>
    }
  | {
      /** A non-recurring product was purchased. */
      event: "nonRecurringProductPurchase"
      /** The purchased non-recurring product. */
      product: TransactionProductIdentifier
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** Loading of paywall response started. */
      event: "paywallResponseLoadStart"
      /** The name of the event that triggered this load. */
      triggeredEventName: string
    }
  | {
      /** Paywall response was not found. */
      event: "paywallResponseLoadNotFound"
      /** The name of the event that triggered this load. */
      triggeredEventName: string
    }
  | {
      /** Loading of paywall response failed. */
      event: "paywallResponseLoadFail"
      /** The name of the event that triggered this load. */
      triggeredEventName: string
    }
  | {
      /** Loading of paywall response completed. */
      event: "paywallResponseLoadComplete"
      /** The name of the event that triggered this load. */
      triggeredEventName: string
      /** Information about the loaded paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** Web view started loading paywall content. */
      event: "paywallWebviewLoadStart"
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** Web view failed to load paywall content. */
      event: "paywallWebviewLoadFail"
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** Web view completed loading paywall content. */
      event: "paywallWebviewLoadComplete"
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** Web view timed out loading paywall content. */
      event: "paywallWebviewLoadTimeout"
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** Web view used fallback content for paywall. */
      event: "paywallWebviewLoadFallback"
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** Loading of products for paywall started. */
      event: "paywallProductsLoadStart"
      /** The name of the event that triggered this load. */
      triggeredEventName: string
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** Loading of products for paywall failed. */
      event: "paywallProductsLoadFail"
      /** The name of the event that triggered this load. */
      triggeredEventName: string
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
    }
  | {
      /** Loading of products for paywall completed. */
      event: "paywallProductsLoadComplete"
      /** The name of the event that triggered this load. */
      triggeredEventName: string
      /** Information about the paywall. (Added based on typical needs, Swift might omit if only event name is key) */
      paywallInfo: PaywallInfo
    }
  | {
      /** Retry loading products for paywall. */
      event: "paywallProductsLoadRetry"
      /** The name of the event that triggered this load. */
      triggeredEventName: string
      /** Information about the paywall. */
      paywallInfo: PaywallInfo
      /** The attempt number for the retry. */
      attempt: number
    }
  | {
      /** A survey response was submitted. */
      event: "surveyResponse"
      /** The survey that was responded to. */
      survey: Survey
      /** The selected option in the survey. */
      selectedOption: SurveyOption
      /** The custom response text, if any. */
      customResponse?: string // Marked as optional as it might not always be present
      /** Information about the paywall where the survey was presented. */
      paywallInfo: PaywallInfo
    }
  | {
      /** A paywall presentation was requested. */
      event: "paywallPresentationRequest"
      /** The status of the presentation request. */
      status: PaywallPresentationRequestStatus
      /** The reason for the status, if applicable. */
      reason?: PaywallPresentationRequestStatusReason
    }
  | {
      /** A custom placement was executed. */
      event: "customPlacement"
      /** The name of the custom placement. */
      name: string
      /** Parameters associated with the custom placement. */
      params: Record<string, any>
      /** Information about the paywall shown for this placement, if any. */
      paywallInfo?: PaywallInfo // Marked as optional as a custom placement might not always show a paywall
    }

/**
 * Contains information about a Superwall event, including the event itself and associated parameters.
 */
export interface SuperwallEventInfo {
  /** The specific Superwall event that occurred. */
  event: SuperwallEvent
  /** Additional parameters associated with the event. */
  params: Record<string, any> // As per Swift: params: event.toJson()
}

/**
 * Defines the verbosity level for logging.
 * - `debug`: Detailed debugging information.
 * - `info`: General information about SDK activity.
 * - `warn`: Warnings about potential issues.
 * - `error`: Errors that occurred within the SDK.
 * - `none`: No logging.
 */
export type LogLevel = "debug" | "info" | "warn" | "error" | "none"

/**
 * Defines the scope of logging within the Superwall SDK.
 * This allows for targeted logging of specific SDK components.
 * Possible values include:
 * 'localizationManager', 'bounceButton', 'coreData', 'configManager', 'identityManager',
 * 'debugManager', 'debugViewController', 'localizationViewController', 'gameControllerManager',
 * 'device', 'network', 'paywallEvents', 'productsManager', 'storeKitManager', 'placements',
 * 'receipts', 'superwallCore', 'paywallPresentation', 'paywallTransactions',
 * 'paywallViewController', 'cache', 'all'.
 */
export type LogScope =
  | "localizationManager"
  | "bounceButton"
  | "coreData"
  | "configManager"
  | "identityManager"
  | "debugManager"
  | "debugViewController"
  | "localizationViewController"
  | "gameControllerManager"
  | "device"
  | "network"
  | "paywallEvents"
  | "productsManager"
  | "storeKitManager"
  | "placements"
  | "receipts"
  | "superwallCore"
  | "paywallPresentation"
  | "paywallTransactions"
  | "paywallViewController"
  | "cache"
  | "all"

/**
 * Defines the events emitted by the Superwall Expo module.
 * These events can be subscribed to using `Superwall.addListener`.
 */
export type SuperwallExpoModuleEvents = {
  /**
   * Called when a paywall is presented.
   * @param params Contains `paywallInfoJson` (PaywallInfo) and `handlerId` (string).
   */
  onPaywallPresent: (params: { paywallInfoJson: PaywallInfo; handlerId: string }) => void
  /**
   * Called when a paywall is dismissed.
   * @param params Contains `paywallInfoJson` (PaywallInfo), `result` (PaywallResult), and `handlerId` (string).
   */
  onPaywallDismiss: (params: {
    paywallInfoJson: PaywallInfo
    result: PaywallResult
    handlerId: string
  }) => void
  /**
   * Called when an error occurs during paywall presentation.
   * @param params Contains `errorString` (string) and `handlerId` (string).
   */
  onPaywallError: (params: { errorString: string; handlerId: string }) => void
  /**
   * Called when a paywall is skipped.
   * @param params Contains `skippedReason` (PaywallSkippedReason) and `handlerId` (string).
   */
  onPaywallSkip: (params: { skippedReason: PaywallSkippedReason; handlerId: string }) => void

  // --- SuperwallDelegateBridge Events ---
  /**
   * Called when the subscription status of the user changes.
   * @param params Contains `from` (SubscriptionStatus) and `to` (SubscriptionStatus).
   */
  subscriptionStatusDidChange: (params: {
    from: SubscriptionStatus
    to: SubscriptionStatus
  }) => void
  /**
   * Called for all Superwall internal events.
   * @param params Contains `eventInfo` (SuperwallEventInfo).
   */
  handleSuperwallEvent: (params: { eventInfo: SuperwallEventInfo }) => void
  /**
   * Called when a custom action is handled on the paywall.
   * @param params Contains `name` (string) of the custom action.
   */
  handleCustomPaywallAction: (params: { name: string }) => void
  /**
   * Called before a paywall is dismissed.
   * @param params Contains `info` (PaywallInfo) of the paywall.
   */
  willDismissPaywall: (params: { info: PaywallInfo }) => void
  /**
   * Called before a paywall is presented.
   * @param params Contains `info` (PaywallInfo) of the paywall.
   */
  willPresentPaywall: (params: { info: PaywallInfo }) => void
  /**
   * Called after a paywall is dismissed.
   * @param params Contains `info` (PaywallInfo) of the paywall.
   */
  didDismissPaywall: (params: { info: PaywallInfo }) => void
  /**
   * Called after a paywall is presented.
   * @param params Contains `info` (PaywallInfo) of the paywall.
   */
  didPresentPaywall: (params: { info: PaywallInfo }) => void
  /**
   * Called when the paywall will open a URL.
   * @param params Contains `url` (string) to be opened.
   */
  paywallWillOpenURL: (params: { url: string }) => void
  /**
   * Called when the paywall will open a deep link.
   * @param params Contains `url` (string) of the deep link.
   */
  paywallWillOpenDeepLink: (params: { url: string }) => void
  /**
   * Called for logging messages from the SDK.
   * @param params Contains `level` (LogLevel), `scope` (LogScope), `message` (string | null),
   * `info` (Record<string, any> | null), and `error` (string | null).
   */
  handleLog: (params: {
    level: LogLevel
    scope: LogScope
    message: string | null
    info: Record<string, any> | null
    error: string | null
  }) => void
  /**
   * Called before attempting to redeem a promotional link.
   * iOS sends an empty dictionary `[:]`.
   * @param params An empty object or null.
   */
  willRedeemLink: (params: Record<string, never> | null) => void
  /**
   * Called after a promotional link has been redeemed.
   * @param params The result of the redemption attempt (RedemptionResult).
   */
  didRedeemLink: (params: RedemptionResult) => void

  // Purchase Events
  /**
   * Called when a purchase is initiated.
   * For iOS, this directly reflects the product ID.
   * @param params Contains `productId` (string) and `platform` ("ios").
   */
  onPurchase: (
    params:
      | { productId: string; platform: "ios" }
      | { productId: string; platform: "android"; basePlanId: string; offerId: string },
  ) => void
  /**
   * Called when a restore is initiated.
   * iOS sends `nil` for this event.
   * @param params null.
   */
  onPurchaseRestore: (params: null) => void // Updated to reflect iOS sending nil
}

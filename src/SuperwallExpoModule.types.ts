// src/SuperwallExpoModule.types.ts

/**
 * @file Defines the core data types, interfaces, and event structures used by the Superwall Expo SDK.
 * These types are crucial for understanding the data flow and interactions with the native Superwall modules.
 */

/**
 * Defines the type of an experiment variant, indicating whether the user sees a paywall or is in a holdout group.
 * - `TREATMENT`: The user is assigned to a treatment group and will be presented with a paywall.
 * - `HOLDOUT`: The user is assigned to a holdout group and will not be presented with a paywall for this experiment.
 */
export type VariantType = "TREATMENT" | "HOLDOUT"

/**
 * Represents an experiment variant, detailing its identifier, type, and associated paywall.
 */
export interface Variant {
  /**
   * The unique identifier for this specific variant of an experiment.
   */
  id: string
  /**
   * The type of the variant, determining the user's experience (e.g., seeing a paywall or being in a holdout).
   * See {@link VariantType}.
   */
  type: VariantType
  /**
   * The identifier of the paywall associated with this variant.
   * This will be an empty string if no paywall is linked (e.g., for `HOLDOUT` variants).
   */
  paywallId: string
}

/**
 * Represents a Superwall experiment, including its ID, group ID, and the variant assigned to the user.
 */
export interface Experiment {
  /**
   * The unique identifier for the experiment.
   */
  id: string
  /**
   * The identifier of the group to which this experiment belongs.
   * Experiments are often grouped for organizational or analytical purposes.
   */
  groupId: string
  /**
   * The specific variant of the experiment that the current user has been assigned to.
   * See {@link Variant}.
   */
  variant: Variant
}

/**
 * Defines the type of an entitlement, typically indicating a level of service or access.
 * - `SERVICE_LEVEL`: Represents an entitlement that grants a certain service level.
 */
export type EntitlementType = "SERVICE_LEVEL" // Currently, only "SERVICE_LEVEL" is used.

/**
 * Represents a user entitlement, signifying a feature or content piece the user has access to.
 */
export interface Entitlement {
  /**
   * The unique identifier for the entitlement.
   */
  id: string
  /**
   * The type of the entitlement. See {@link EntitlementType}.
   */
  type: EntitlementType
}

/**
 * Third-party integration providers supported by Superwall.
 * Use these values with `setIntegrationAttributes()` to link user IDs from
 * attribution and analytics platforms.
 */
export type IntegrationAttribute =
  | "adjustId"
  | "amplitudeDeviceId"
  | "amplitudeUserId"
  | "appsflyerId"
  | "brazeAliasName"
  | "brazeAliasLabel"
  | "onesignalId"
  | "fbAnonId"
  | "firebaseAppInstanceId"
  | "iterableUserId"
  | "iterableCampaignId"
  | "iterableTemplateId"
  | "mixpanelDistinctId"
  | "mparticleId"
  | "clevertapId"
  | "airshipChannelId"
  | "kochavaDeviceId"
  | "tenjinId"
  | "posthogUserId"
  | "customerioId"

/**
 * Object with optional keys for third-party integration provider IDs.
 * Used with `setIntegrationAttributes()` to link attribution and analytics platforms.
 *
 * Available keys: adjustId, amplitudeDeviceId, amplitudeUserId, appsflyerId,
 * brazeAliasName, brazeAliasLabel, onesignalId, fbAnonId, firebaseAppInstanceId,
 * iterableUserId, iterableCampaignId, iterableTemplateId, mixpanelDistinctId,
 * mparticleId, clevertapId, airshipChannelId, kochavaDeviceId, tenjinId,
 * posthogUserId, customerioId
 *
 * @example
 * ```typescript
 * setIntegrationAttributes({
 *   adjustId: "adjust_123",
 *   amplitudeUserId: "user_456",
 *   airshipChannelId: "channel_789"
 * })
 * ```
 */
export type IntegrationAttributes = {
  [K in IntegrationAttribute]?: string
}

/**
 * Contains information about the user's entitlements, separated into active and inactive.
 * This is typically fetched from Superwall's servers to determine what content or features
 * the user has access to.
 */
export interface EntitlementsInfo {
  /**
   * Array of entitlements that are currently active for the user.
   * See {@link Entitlement}.
   */
  active: Entitlement[]
  /**
   * Array of entitlements that are not currently active for the user.
   * See {@link Entitlement}.
   */
  inactive: Entitlement[]
}

/**
 * Describes the reason why a paywall presentation was skipped for the user.
 * This can happen due to various conditions like being in a holdout group,
 * not matching audience rules, or if the specified placement doesn't exist.
 */
export type PaywallSkippedReason =
  | {
      /**
       * The user was assigned to a holdout group within an experiment, so the paywall was intentionally skipped.
       */
      type: "Holdout"
      /**
       * Detailed information about the experiment that led to this holdout.
       * See {@link Experiment}.
       */
      experiment: Experiment
    }
  | {
      /**
       * The user did not match any of the audience rules defined for the campaign or trigger.
       */
      type: "NoAudienceMatch"
    }
  | {
      /**
       * The placement ID specified for the paywall presentation was not found in the Superwall dashboard configuration.
       */
      type: "PlacementNotFound"
    }

/**
 * Represents the outcome of a user's interaction with a paywall.
 * This includes scenarios like purchasing a product, declining the paywall, or restoring purchases.
 */
export type PaywallResult =
  | {
      /**
       * The user successfully purchased a product through the paywall.
       */
      type: "purchased"
      /**
       * The identifier of the product that was purchased.
       */
      productId: string
    }
  | {
      /**
       * The user explicitly declined or closed the paywall without making a purchase.
       */
      type: "declined"
    }
  | {
      /**
       * The user successfully restored their previous purchases through the paywall.
       */
      type: "restored"
    }

/**
 * Represents the subscription status of the user.
 * It indicates whether the user is unknown, inactive, or active with specific entitlements.
 */
export type SubscriptionStatus =
  | {
      /**
       * The subscription status has not yet been determined or is unavailable.
       * This can be an initial state before the SDK fetches the status.
       */
      status: "UNKNOWN"
    }
  | {
      /**
       * The user does not have an active subscription.
       * They are not currently entitled to any subscription-based features.
       */
      status: "INACTIVE"
    }
  | {
      /**
       * The user has an active subscription.
       */
      status: "ACTIVE"
      /**
       * A list of entitlements the user has access to due to their active subscription.
       * This array is only present when the status is "ACTIVE".
       * See {@link Entitlement}.
       */
      entitlements: Entitlement[]
    }

/**
 * Specifies the reason why a paywall was closed.
 * This helps in understanding the flow of paywall presentations and user interactions.
 * - `systemLogic`: Closed automatically by the SDK due to internal logic (e.g., another paywall is about to open).
 * - `forNextPaywall`: Closed because another paywall is scheduled to be presented immediately after.
 * - `webViewFailedToLoad`: Closed because the web view component failed to load the paywall content.
 * - `manualClose`: Closed due to a direct user action (e.g., tapping a close button on the paywall).
 * - `none`: No specific reason, or the reason is unknown.
 */
export type PaywallCloseReason =
  | "systemLogic"
  | "forNextPaywall"
  | "webViewFailedToLoad"
  | "manualClose"
  | "none"

/**
 * Defines the feature gating behavior for a paywall or feature.
 * - `gated`: The feature or content is gated and requires a specific condition (e.g., active subscription) to be met for access.
 * - `nonGated`: The feature or content is not gated and is available to all users, regardless of subscription status.
 */
export type FeatureGatingBehavior = "gated" | "nonGated"

/**
 * Specifies the conditions under which a survey should be presented to the user.
 * - `ON_MANUAL_CLOSE`: Present the survey when the user manually closes the paywall.
 * - `ON_PURCHASE`: Present the survey after the user completes a purchase.
 */
export type SurveyShowCondition = "ON_MANUAL_CLOSE" | "ON_PURCHASE"

/**
 * Represents a single option within a survey.
 */
export interface SurveyOption {
  /**
   * The unique identifier for this survey option.
   */
  id: string
  /**
   * The text displayed to the user for this option.
   */
  title: string
}

/**
 * Represents a survey that can be presented to the user, typically after a paywall interaction.
 */
export interface Survey {
  /**
   * The unique identifier for the survey.
   */
  id: string
  /**
   * A key used for assigning this survey to a user, often for A/B testing or specific targeting.
   */
  assignmentKey: string
  /**
   * The title of the survey, displayed to the user.
   */
  title: string
  /**
   * The main message or question of the survey.
   */
  message: string
  /**
   * An array of options available for the user to choose from in the survey.
   * See {@link SurveyOption}.
   */
  options: SurveyOption[]
  /**
   * The condition that triggers the presentation of this survey.
   * See {@link SurveyShowCondition}.
   */
  presentationCondition: SurveyShowCondition
  /**
   * The probability (ranging from 0.0 to 1.0) that this survey will be presented if its condition is met.
   */
  presentationProbability: number
  /**
   * If true, an "Other" option with a free-text input field will be included in the survey.
   */
  includeOtherOption: boolean
  /**
   * If true, a close button or option will be available on the survey, allowing the user to dismiss it.
   */
  includeCloseOption: boolean
}

/**
 * Defines the types of local notifications that Superwall can schedule.
 * - `trialStarted`: A notification to inform the user that their free trial has started.
 */
export type LocalNotificationType = "trialStarted" // Currently, only "trialStarted" is used.

/**
 * Represents a local notification that can be scheduled by the Superwall SDK.
 */
export interface LocalNotification {
  /**
   * The type of the local notification. See {@link LocalNotificationType}.
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
   * The delay in seconds before the notification is shown to the user, relative to the scheduling time.
   */
  delay: number
  /**
   * An optional subtitle for the notification.
   */
  subtitle?: string
}

/**
 * Represents a request for a computed property that the Superwall SDK needs to evaluate.
 * Computed properties are dynamic values based on user attributes or other data.
 */
export interface ComputedPropertyRequest {
  /**
   * The type or name of the computed property being requested.
   */
  type: string
  /**
   * The name of the placement associated with this computed property request, if applicable.
   */
  placementName: string
}

/**
 * Represents a product available for purchase on a paywall, as defined within {@link PaywallInfo}.
 * This provides a simplified view of a product, focusing on its ID, name, and granted entitlements.
 */
export interface Product {
  /**
   * The unique identifier of the product (e.g., from the App Store or Google Play).
   */
  id: string
  /**
   * The name of the product, if available.
   */
  name?: string
  /**
   * A list of entitlements that are granted to the user upon purchasing this product.
   * See {@link Entitlement}.
   */
  entitlements: Entitlement[]
}

/**
 * Contains comprehensive information about a paywall, including its configuration, associated experiment,
 * products, loading times, and other metadata. This object is central to understanding paywall behavior.
 */
export interface PaywallInfo {
  /** The unique identifier of the paywall, as configured in the Superwall dashboard. */
  identifier: string
  /** The name of the paywall, as configured in the Superwall dashboard. */
  name: string
  /** The URL where the paywall's web content is hosted. */
  url: string
  /**
   * The experiment associated with this paywall presentation, if applicable.
   * See {@link Experiment}.
   */
  experiment?: Experiment
  /**
   * A list of products available for purchase on this paywall.
   * See {@link Product}.
   */
  products: Product[]
  /** A list of product identifiers (SKUs) available on this paywall. */
  productIds: string[]
  /**
   * The name of the event or placement that triggered the presentation of this paywall, if applicable.
   * Corresponds to `presentedByPlacementWithName` in the native Swift SDK.
   */
  presentedByEventWithName?: string
  /**
   * The identifier of the event or placement that triggered the presentation of this paywall, if applicable.
   * Corresponds to `presentedByPlacementWithId` in the native Swift SDK.
   */
  presentedByEventWithId?: string
  /**
   * The Unix timestamp (in seconds or milliseconds) of when the event triggering this paywall occurred, if applicable.
   * Corresponds to `presentedByPlacementAt` in the native Swift SDK.
   */
  presentedByEventAt?: number
  /**
   * The source that initiated the paywall presentation (e.g., "implicit", "explicit", "getPaywall").
   */
  presentedBy: string
  /**
   * The type of the source that led to the paywall presentation (e.g., "Register" for `Superwall.shared.register(event:)`, "Track" for `Superwall.shared.track(event:)`).
   */
  presentationSourceType?: string
  /** The Unix timestamp when the request to load the paywall configuration and rules (response) started. */
  responseLoadStartTime?: number
  /** The Unix timestamp when the paywall response successfully loaded. */
  responseLoadCompleteTime?: number
  /** The Unix timestamp if the paywall response failed to load. */
  responseLoadFailTime?: number
  /** The duration (typically in milliseconds) it took to load the paywall response. */
  responseLoadDuration?: number
  /** Indicates whether a free trial is available for any of the products on this paywall. */
  isFreeTrialAvailable: boolean
  /**
   * The feature gating behavior for this paywall.
   * See {@link FeatureGatingBehavior}.
   */
  featureGatingBehavior: FeatureGatingBehavior
  /**
   * The reason why the paywall was closed.
   * See {@link PaywallCloseReason}.
   */
  closeReason: PaywallCloseReason
  /** The Unix timestamp when the web view started loading the paywall's HTML content. */
  webViewLoadStartTime?: number
  /** The Unix timestamp when the web view successfully loaded the paywall's HTML content. */
  webViewLoadCompleteTime?: number
  /** The Unix timestamp if the web view failed to load the paywall's HTML content. */
  webViewLoadFailTime?: number
  /** The duration (typically in milliseconds) it took for the web view to load the paywall's content. */
  webViewLoadDuration?: number
  /** The Unix timestamp when the loading of product information (from App Store/Google Play) started. */
  productsLoadStartTime?: number
  /** The Unix timestamp when the product information successfully loaded. */
  productsLoadCompleteTime?: number
  /** The Unix timestamp if loading product information failed. */
  productsLoadFailTime?: number
  /** The duration (typically in milliseconds) it took to load the product information. */
  productsLoadDuration?: number
  /** The version of the `paywall.js` script used in the paywall, if available. */
  paywalljsVersion?: string
  /**
   * A list of computed property requests associated with this paywall presentation.
   * See {@link ComputedPropertyRequest}.
   */
  computedPropertyRequests: ComputedPropertyRequest[]
  /**
   * A list of surveys associated with this paywall that may be presented.
   * See {@link Survey}.
   */
  surveys: Survey[]
  /**
   * A list of local notifications that may be scheduled as a result of this paywall presentation.
   * See {@link LocalNotification}.
   */
  localNotifications: LocalNotification[]
}

/**
 * Provides information about an error that occurred during a promotional code redemption attempt.
 */
export interface RedemptionErrorInfo {
  /**
   * A developer-readable message describing the error.
   */
  message: string
}

/**
 * Provides information about an expired promotional code during a redemption attempt.
 */
export interface RedemptionExpiredCodeInfo {
  /**
   * Indicates whether a new code was resent to the user (e.g., via email) if applicable.
   */
  resent: boolean
  /**
   * The obfuscated email address to which a new code was resent, if applicable.
   */
  obfuscatedEmail?: string
}

/**
 * Defines who owns the item or subscription obtained through a promotional code redemption.
 */
export type RedemptionOwnership =
  | {
      /**
       * Ownership of the redeemed item is tied to a specific application user ID.
       */
      type: "APP_USER"
      /**
       * The application user ID of the owner.
       */
      appUserId: string
    }
  | {
      /**
       * Ownership of the redeemed item is tied to a specific device ID.
       */
      type: "DEVICE"
      /**
       * The device ID of the owner.
       */
      deviceId: string
    }

/**
 * Contains store-specific identifiers for a purchaser, relevant for tracking redemptions across different billing systems.
 */
export type RedemptionStoreIdentifiers =
  | {
      /**
       * The purchase or subscription is managed by Stripe.
       */
      store: "STRIPE"
      /**
       * The Stripe customer ID.
       */
      stripeCustomerId: string
      /**
       * The Stripe subscription IDs associated with the customer.
       */
      stripeSubscriptionIds: string[]
    }
  | {
      /**
       * The purchase or subscription is managed by Paddle.
       */
      store: "PADDLE"
      /**
       * The Paddle customer ID.
       */
      paddleCustomerId: string
      /**
       * The Paddle subscription IDs associated with the customer.
       */
      paddleSubscriptionIds: string[]
    }
  | {
      /**
       * The store is unknown or not explicitly handled by this type definition.
       */
      store: string // Allows for other store names not explicitly defined
      /**
       * Allows for additional properties specific to other or future store integrations.
       */
      [key: string]: any
    }

/**
 * Information about the purchaser involved in a promotional code redemption.
 */
export interface RedemptionPurchaserInfo {
  /**
   * The application user ID of the purchaser.
   */
  appUserId: string
  /**
   * The email address of the purchaser, if available.
   */
  email?: string
  /**
   * Store-specific identifiers for the purchaser.
   * See {@link RedemptionStoreIdentifiers}.
   */
  storeIdentifiers: RedemptionStoreIdentifiers
}

/**
 * Represents a product involved in a redemption transaction with comprehensive pricing and localization information.
 */
export interface PaywallProduct {
  /** The unique identifier of the product */
  identifier: string
  /** Language code for localization */
  languageCode: string
  /** Locale string */
  locale: string
  /** Currency code (e.g., "USD") */
  currencyCode: string
  /** Currency symbol (e.g., "$") */
  currencySymbol: string
  /** Subscription period */
  period: string
  /** Periodly description */
  periodly: string
  /** Localized period description */
  localizedPeriod: string
  /** Alternative period description */
  periodAlt: string
  /** Period length in days */
  periodDays: number
  /** Period length in weeks */
  periodWeeks: number
  /** Period length in months */
  periodMonths: number
  /** Period length in years */
  periodYears: number
  /** Raw price as a number */
  rawPrice: number
  /** Formatted price string */
  price: string
  /** Daily equivalent price */
  dailyPrice: string
  /** Weekly equivalent price */
  weeklyPrice: string
  /** Monthly equivalent price */
  monthlyPrice: string
  /** Yearly equivalent price */
  yearlyPrice: string
  /** Raw trial period price */
  rawTrialPeriodPrice: number
  /** Formatted trial period price */
  trialPeriodPrice: string
  /** Trial period daily price */
  trialPeriodDailyPrice: string
  /** Trial period weekly price */
  trialPeriodWeeklyPrice: string
  /** Trial period monthly price */
  trialPeriodMonthlyPrice: string
  /** Trial period yearly price */
  trialPeriodYearlyPrice: string
  /** Trial period length in days */
  trialPeriodDays: number
  /** Trial period length in weeks */
  trialPeriodWeeks: number
  /** Trial period length in months */
  trialPeriodMonths: number
  /** Trial period length in years */
  trialPeriodYears: number
  /** Trial period description text */
  trialPeriodText: string
  /** Trial period end date string */
  trialPeriodEndDate: string
}

/**
 * Information about the paywall that was involved in or led to a promotional code redemption.
 */
export interface RedemptionPaywallInfo {
  /**
   * The identifier of the paywall.
   */
  identifier: string
  /**
   * The name of the placement that triggered the paywall presentation.
   */
  placementName: string
  /**
   * Parameters associated with the placement.
   */
  placementParams: Record<string, any>
  /**
   * The ID of the experiment variant shown to the user.
   */
  variantId: string
  /**
   * The ID of the experiment the user was part of.
   */
  experimentId: string
  /**
   * The product identifier associated with the paywall, if any.
   * @deprecated Use `product.identifier` instead. This property will be removed in a future version.
   */
  productIdentifier?: string
  /**
   * The product associated with the paywall, if any.
   */
  product?: PaywallProduct
}

/**
 * Contains detailed information about a successful promotional code redemption
 * or an expired subscription that was previously redeemed.
 */
export interface RedemptionInfo {
  /**
   * Information about who owns the redeemed item or subscription.
   * See {@link RedemptionOwnership}.
   */
  ownership: RedemptionOwnership
  /**
   * Information about the purchaser.
   * See {@link RedemptionPurchaserInfo}.
   */
  purchaserInfo: RedemptionPurchaserInfo
  /**
   * Information about the paywall related to this redemption, if applicable.
   * See {@link RedemptionPaywallInfo}.
   */
  paywallInfo?: RedemptionPaywallInfo
  /**
   * A list of entitlements granted by this redemption.
   * See {@link Entitlement}.
   */
  entitlements: Entitlement[]
}

/**
 * Represents the result of an attempt to redeem a promotional code.
 * This is a discriminated union type based on the `status` property.
 */
export type RedemptionResult =
  | {
      /**
       * The promotional code was successfully redeemed.
       */
      status: "SUCCESS"
      /**
       * The promotional code that was redeemed.
       */
      code: string
      /**
       * Detailed information about the successful redemption.
       * See {@link RedemptionInfo}.
       */
      redemptionInfo: RedemptionInfo
    }
  | {
      /**
       * An error occurred during the redemption attempt.
       */
      status: "ERROR"
      /**
       * The promotional code that was attempted.
       */
      code: string
      /**
       * Information about the error that occurred.
       * See {@link RedemptionErrorInfo}.
       */
      error: RedemptionErrorInfo
    }
  | {
      /**
       * The promotional code has expired and cannot be redeemed.
       */
      status: "CODE_EXPIRED"
      /**
       * The expired promotional code.
       */
      code: string
      /**
       * Information related to the expired code, such as whether a new one was resent.
       * See {@link RedemptionExpiredCodeInfo}.
       */
      expired: RedemptionExpiredCodeInfo
    }
  | {
      /**
       * The promotional code is invalid or does not exist.
       */
      status: "INVALID_CODE"
      /**
       * The invalid promotional code that was attempted.
       */
      code: string
    }
  | {
      /**
       * The subscription associated with the promotional code has expired,
       * but the code was successfully redeemed in the past.
       */
      status: "EXPIRED_SUBSCRIPTION"
      /**
       * The promotional code associated with the expired subscription.
       */
      code: string
      /**
       * Detailed information about the prior redemption.
       * See {@link RedemptionInfo}.
       */
      redemptionInfo: RedemptionInfo
    }

/**
 * Represents the result of a trigger evaluation.
 * This determines what action Superwall will take in response to an event or placement registration.
 */
export type TriggerResult =
  | {
      /** The specified placement ID was not found in the Superwall dashboard configuration. */
      result: "placementNotFound"
    }
  | {
      /** The user did not match any audience rules for the trigger. */
      result: "noAudienceMatch"
    }
  | {
      /** A paywall will be presented to the user as a result of this trigger. */
      result: "paywall"
      /**
       * Information about the experiment that led to this paywall presentation.
       * See {@link Experiment}.
       */
      experiment: Experiment
    }
  | {
      /** The user was assigned to a holdout group, and no paywall will be shown. */
      result: "holdout"
      /**
       * Information about the experiment that led to this holdout assignment.
       * See {@link Experiment}.
       */
      experiment: Experiment
    }
  | {
      /** An error occurred during the trigger evaluation process. */
      result: "error"
      /** A string describing the error. */
      error: string
    }

/**
 * Represents comprehensive information about a product involved in a transaction event.
 * This interface includes detailed pricing, subscription, trial, and localization information
 * from both the native iOS (StoreProduct) and Android (Product) implementations.
 */
export interface TransactionProductIdentifier {
  /** The unique identifier of the product (e.g., SKU). */
  id: string

  /** The product identifier from the store. */
  productIdentifier: string

  /** The full identifier including any additional qualifiers. */
  fullIdentifier: string

  /** The raw price of the product as a number. */
  price: number

  /** The price formatted according to the device's locale and currency. */
  localizedPrice: string

  /** The subscription period formatted for display (e.g., "1 month"). */
  localizedSubscriptionPeriod: string

  /** The subscription period type (e.g., "month", "year"). */
  period: string

  /** The subscription period with "ly" suffix (e.g., "monthly", "yearly"). */
  periodly: string

  /** The number of weeks in the subscription period. */
  periodWeeks: number

  /** The weeks duration as a formatted string. */
  periodWeeksString: string

  /** The number of months in the subscription period. */
  periodMonths: number

  /** The months duration as a formatted string. */
  periodMonthsString: string

  /** The number of years in the subscription period. */
  periodYears: number

  /** The years duration as a formatted string. */
  periodYearsString: string

  /** The number of days in the subscription period. */
  periodDays: number

  /** The days duration as a formatted string. */
  periodDaysString: string

  /** The calculated daily price of the product. */
  dailyPrice: string

  /** The calculated weekly price of the product. */
  weeklyPrice: string

  /** The calculated monthly price of the product. */
  monthlyPrice: string

  /** The calculated yearly price of the product. */
  yearlyPrice: string

  /** Whether the product includes a free trial period. */
  hasFreeTrial: boolean

  /** The trial period price formatted for display. */
  localizedTrialPeriodPrice: string

  /** The trial period price as a number. */
  trialPeriodPrice: number

  /** The end date of the trial period as an ISO string, or null if no trial. */
  trialPeriodEndDate: string | null

  /** The trial period end date formatted as a display string. */
  trialPeriodEndDateString: string

  /** The number of days in the trial period. */
  trialPeriodDays: number

  /** The trial days duration as a formatted string. */
  trialPeriodDaysString: string

  /** The number of weeks in the trial period. */
  trialPeriodWeeks: number

  /** The trial weeks duration as a formatted string. */
  trialPeriodWeeksString: string

  /** The number of months in the trial period. */
  trialPeriodMonths: number

  /** The trial months duration as a formatted string. */
  trialPeriodMonthsString: string

  /** The number of years in the trial period. */
  trialPeriodYears: number

  /** The trial years duration as a formatted string. */
  trialPeriodYearsString: string

  /** The trial period formatted as descriptive text (e.g., "7-day free trial"). */
  trialPeriodText: string

  /** The locale identifier for the product (e.g., "en_US"). */
  locale: string

  /** The language code extracted from the locale, or null if unavailable. */
  languageCode: string | null

  /** The currency code for the product's price (e.g., "USD"). */
  currencyCode: string | null

  /** The currency symbol for the product's price (e.g., "$"). */
  currencySymbol: string | null

  /** The region code extracted from the locale, or null if unavailable. */
  regionCode: string | null

  /**
   * The structured subscription period information.
   * Contains the unit of time and the value for that unit.
   */
  subscriptionPeriod: {
    /** The time unit for the subscription period. */
    unit: "day" | "week" | "month" | "year"
    /** The number of units for the subscription period. */
    value: number
  } | null

  /** The identifier for the subscription group this product belongs to, if applicable. */
  subscriptionGroupIdentifier?: string | null

  /** Whether this product can be shared with family members. */
  isFamilyShareable?: boolean

  /** The token for the introductory offer (iOS 18+, Android). Used for purchase verification. */
  introOfferToken?: string | null

  /** Additional attributes and metadata associated with the product. */
  attributes: Record<string, any>
}

/**
 * Represents a store transaction, providing detailed information about a purchase or subscription event.
 * Dates are typically in ISO 8601 format.
 */
export interface StoreTransaction {
  /** The request ID from the Superwall configuration that initiated this transaction, if applicable. */
  configRequestId?: string
  /** The ID of the app session during which this transaction occurred. */
  appSessionId?: string
  /** The ISO 8601 date string of when the transaction occurred. */
  transactionDate?: string
  /** The original transaction identifier, useful for linking related transactions (e.g., subscriptions). */
  originalTransactionIdentifier?: string
  /** The transaction identifier provided by the respective app store (App Store, Google Play). */
  storeTransactionId?: string
  /** The ISO 8601 date string of the original transaction date, especially relevant for subscriptions. */
  originalTransactionDate?: string
  /** The web order line item ID, used for auto-renewable subscriptions on some platforms. */
  webOrderLineItemID?: string
  /** The app bundle ID associated with this transaction. */
  appBundleId?: string
  /** The identifier for the subscription group this transaction belongs to. */
  subscriptionGroupId?: string
  /** Indicates if this transaction represents an upgrade from a previous subscription. */
  isUpgraded?: boolean
  /** The ISO 8601 date string of when the subscription or product access expires. */
  expirationDate?: string
  /** The offer ID associated with this transaction, if any. */
  offerId?: string
  /** The ISO 8601 date string if the transaction was revoked by the store or developer. */
  revocationDate?: string
  /** The App Store account token, specific to Apple's App Store. */
  appAccountToken?: string
  /** The purchase token for Android transactions (Google Play). */
  purchaseToken?: string
  /** The signature for Android transactions for verification (Google Play). */
  signature?: string
}

/**
 * Represents the type of restoration process that occurred.
 */
export type RestoreType =
  | {
      /** Indicates that the restoration occurred as part of a purchase attempt. */
      type: "viaPurchase"
      /**
       * The store transaction associated with the purchase that led to the restoration, if available.
       * See {@link StoreTransaction}.
       */
      storeTransaction?: StoreTransaction
    }
  | {
      /** Indicates that the restoration was initiated manually by the user (e.g., by tapping a "Restore Purchases" button). */
      type: "viaRestore"
    }

/**
 * Defines the possible statuses of a paywall presentation request.
 * - `presentation`: A paywall is being or will be presented.
 * - `noPresentation`: No paywall will be presented.
 * - `timeout`: The request to determine presentation timed out.
 */
export type PaywallPresentationRequestStatusType = "presentation" | "noPresentation" | "timeout"

/**
 * Represents the status of a request to present a paywall.
 */
export interface PaywallPresentationRequestStatus {
  /** The overall status of the paywall presentation request. See {@link PaywallPresentationRequestStatusType}. */
  status: PaywallPresentationRequestStatusType
}

/**
 * Provides the reason behind the status of a paywall presentation request, especially when no paywall is shown.
 */
export type PaywallPresentationRequestStatusReason =
  | {
      /** A paywall was presented because the debugger is active. */
      reason: "debuggerPresented"
    }
  | {
      /** No paywall was presented because one is already being displayed. */
      reason: "paywallAlreadyPresented"
    }
  | {
      /** No paywall was presented because the user is in a holdout group of an experiment. */
      reason: "holdout"
      /**
       * Information about the experiment that led to this holdout.
       * See {@link Experiment}.
       */
      experiment: Experiment
    }
  | {
      /** No paywall was presented because the user did not match any audience rules. */
      reason: "noRuleMatch"
    }
  | {
      /** No paywall was presented because the triggering event was not found. */
      reason: "eventNotFound"
    }
  | {
      /** No paywall was presented because the paywall view controller is not available (native SDK specific). */
      reason: "noPaywallViewController"
    }
  | {
      /** No paywall was presented because there is no view controller available to present it on (native SDK specific). */
      reason: "noViewController"
    }
  | {
      /** No paywall was presented because the user is already subscribed. */
      reason: "userIsSubscribed"
    }
  | {
      /** No paywall was presented due to an error. */
      reason: "error"
      /** A string describing the error. */
      error: string
    }
  | {
      /** No paywall was presented because it is gated by some condition. */
      reason: "paywallIsGated"
      /**
       * Information about the paywall that was gated.
       * See {@link PaywallInfo}.
       */
      paywallInfo: PaywallInfo
    }

/**
 * Represents a Superwall event that can be tracked by the SDK.
 * This is a discriminated union type where the `event` property determines the specific event and its associated payload.
 * These events provide insights into user behavior, SDK operations, and paywall interactions.
 */

// Individual Superwall Event Interfaces/Types

/**
 * User's first time seeing a Superwall-related element or paywall.
 */
export interface FirstSeenEvent {
  /** User's first time seeing a Superwall-related element or paywall. */
  event: "firstSeen"
}

/**
 * The application was opened.
 */
export interface AppOpenEvent {
  /** The application was opened. */
  event: "appOpen"
}

/**
 * The application was launched.
 */
export interface AppLaunchEvent {
  /** The application was launched. */
  event: "appLaunch"
}

/**
 * An alias was set for the user's identity.
 */
export interface IdentityAliasEvent {
  /** An alias was set for the user's identity. */
  event: "identityAlias"
}

/**
 * The application was installed.
 */
export interface AppInstallEvent {
  /** The application was installed. */
  event: "appInstall"
}

/**
 * A new user session started.
 */
export interface SessionStartEvent {
  /** A new user session started. */
  event: "sessionStart"
}

/**
 * The user's identity was reset (logged out).
 */
export interface ResetEvent {
  /** The user's identity was reset (logged out). */
  event: "reset"
}

/**
 * The Superwall configuration was refreshed.
 */
export interface ConfigRefreshEvent {
  /** The Superwall configuration was refreshed. */
  event: "configRefresh"
}

/**
 * Failed to refresh the Superwall configuration.
 */
export interface ConfigFailEvent {
  /** Failed to refresh the Superwall configuration. */
  event: "configFail"
}

/**
 * Attributes were passed with the Superwall configuration.
 */
export interface ConfigAttributesEvent {
  /** Attributes were passed with the Superwall configuration. */
  event: "configAttributes"
}

/**
 * All pending paywall assignments have been confirmed.
 */
export interface ConfirmAllAssignmentsEvent {
  /** All pending paywall assignments have been confirmed. */
  event: "confirmAllAssignments"
}

/**
 * A touch event began on the screen (generic).
 */
export interface TouchesBeganEvent {
  /** A touch event began on the screen (generic). */
  event: "touchesBegan"
}

/**
 * A survey was closed.
 */
export interface SurveyCloseEvent {
  /** A survey was closed. */
  event: "surveyClose"
}

/**
 * Restoration of purchases started.
 */
export interface RestoreStartEvent {
  /** Restoration of purchases started. */
  event: "restoreStart"
}

/**
 * Restoration of purchases completed successfully.
 */
export interface RestoreCompleteEvent {
  /** Restoration of purchases completed successfully. */
  event: "restoreComplete"
}

/**
 * Restoration of purchases failed.
 */
export interface RestoreFailEvent {
  /** Restoration of purchases failed. */
  event: "restoreFail"
  /** The error message. */
  message: string
}

/**
 * Request for AdServices token started.
 */
export interface AdServicesTokenRequestStartEvent {
  /** Request for AdServices token started. */
  event: "adServicesTokenRequestStart"
}

/**
 * Request for AdServices token failed.
 */
export interface AdServicesTokenRequestFailEvent {
  /** Request for AdServices token failed. */
  event: "adServicesTokenRequestFail"
  /** The error message. */
  error: string
}

/**
 * Request for AdServices token completed.
 */
export interface AdServicesTokenRequestCompleteEvent {
  /** Request for AdServices token completed. */
  event: "adServicesTokenRequestComplete"
  /** The AdServices token. */
  token: string
}

/**
 * Shimmer view (placeholder loading) started.
 */
export interface ShimmerViewStartEvent {
  /** Shimmer view (placeholder loading) started. */
  event: "shimmerViewStart"
}

/**
 * Shimmer view (placeholder loading) completed.
 */
export interface ShimmerViewCompleteEvent {
  /** Shimmer view (placeholder loading) completed. */
  event: "shimmerViewComplete"
}

/**
 * Redemption of a promotional code started.
 */
export interface RedemptionStartEvent {
  /** Redemption of a promotional code started. */
  event: "redemptionStart"
}

/**
 * Redemption of a promotional code completed.
 */
export interface RedemptionCompleteEvent {
  /** Redemption of a promotional code completed. */
  event: "redemptionComplete"
}

/**
 * Redemption of a promotional code failed.
 */
export interface RedemptionFailEvent {
  /** Redemption of a promotional code failed. */
  event: "redemptionFail"
}

/**
 * Data enrichment process started.
 */
export interface EnrichmentStartEvent {
  /** Data enrichment process started. */
  event: "enrichmentStart"
}

/**
 * Data enrichment process completed.
 */
export interface EnrichmentCompleteEvent {
  /** Data enrichment process completed. */
  event: "enrichmentComplete"
  /** Enriched user data, if any. */
  userEnrichment?: Record<string, any>
  /** Enriched device data, if any. */
  deviceEnrichment?: Record<string, any>
}

/**
 * Data enrichment process failed.
 */
export interface EnrichmentFailEvent {
  /** Data enrichment process failed. */
  event: "enrichmentFail"
}

/**
 * An unknown or uncategorized event occurred.
 */
export interface UnknownEvent {
  /** An unknown or uncategorized event occurred. */
  event: "unknown"
}

/**
 * Device attributes were updated.
 */
export interface DeviceAttributesEvent {
  /** Device attributes were updated. */
  event: "deviceAttributes"
  /** The updated device attributes. */
  attributes: Record<string, any>
}

/**
 * The user's subscription status changed.
 */
export interface SubscriptionStatusDidChangeEvent {
  /** The user's subscription status changed. */
  event: "subscriptionStatusDidChange"
  /**
   * The new subscription status.
   * See {@link SubscriptionStatus}.
   */
  subscriptionStatus: SubscriptionStatus
}

/**
 * The application was closed.
 */
export interface AppCloseEvent {
  /** The application was closed. */
  event: "appClose"
}

/**
 * A deep link was opened by the application, potentially via Superwall.
 */
export interface DeepLinkEvent {
  /** A deep link was opened by the application, potentially via Superwall. */
  event: "deepLink"
  /** The URL of the deep link. */
  url: string
}

/**
 * A trigger was fired based on an event or placement.
 */
export interface TriggerFireEvent {
  /** A trigger was fired based on an event or placement. */
  event: "triggerFire"
  /** The name of the event or placement that caused the trigger to fire. */
  placementName: string
  /**
   * The result of the trigger evaluation, determining what action Superwall will take.
   * See {@link TriggerResult}.
   */
  result: TriggerResult
}

/**
 * A paywall was opened.
 */
export interface PaywallOpenEvent {
  /** A paywall was opened. */
  event: "paywallOpen"
  /**
   * Information about the paywall that was opened.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * A paywall was closed.
 */
export interface PaywallCloseEvent {
  /** A paywall was closed. */
  event: "paywallClose"
  /**
   * Information about the paywall that was closed.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * The user explicitly declined a paywall.
 */
export interface PaywallDeclineEvent {
  /** The user explicitly declined a paywall. */
  event: "paywallDecline"
  /**
   * Information about the paywall that was declined.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * A transaction (purchase, restore) was initiated.
 */
export interface TransactionStartEvent {
  /** A transaction (purchase, restore) was initiated. */
  event: "transactionStart"
  /**
   * Identifier of the product involved in the transaction.
   * See {@link TransactionProductIdentifier}.
   */
  product: TransactionProductIdentifier
  /**
   * Information about the paywall from which the transaction was initiated.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * A transaction failed.
 */
export interface TransactionFailEvent {
  /** A transaction failed. */
  event: "transactionFail"
  /** The error message from the transaction failure. */
  error: string
  /**
   * Information about the paywall associated with the failed transaction.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * A transaction was abandoned by the user.
 */
export interface TransactionAbandonEvent {
  /** A transaction was abandoned by the user. */
  event: "transactionAbandon"
  /**
   * Identifier of the product involved in the abandoned transaction.
   * See {@link TransactionProductIdentifier}.
   */
  product: TransactionProductIdentifier
  /**
   * Information about the paywall associated with the abandoned transaction.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * A transaction was successfully completed.
 */
export interface TransactionCompleteEvent {
  /** A transaction was successfully completed. */
  event: "transactionComplete"
  /**
   * Detailed information about the store transaction, if available.
   * See {@link StoreTransaction}.
   */
  transaction?: StoreTransaction
  /**
   * Identifier of the product involved in the completed transaction.
   * See {@link TransactionProductIdentifier}.
   */
  product: TransactionProductIdentifier
  /** The type of the transaction (e.g., "purchase", "restore"). */
  type: string
  /**
   * Information about the paywall associated with the completed transaction.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * A new subscription was started.
 */
export interface SubscriptionStartEvent {
  /** A new subscription was started. */
  event: "subscriptionStart"
  /**
   * Identifier of the product for which the subscription started.
   * See {@link TransactionProductIdentifier}.
   */
  product: TransactionProductIdentifier
  /**
   * Information about the paywall associated with the subscription start.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * A free trial was started.
 */
export interface FreeTrialStartEvent {
  /** A free trial was started. */
  event: "freeTrialStart"
  /**
   * Identifier of the product for which the free trial started.
   * See {@link TransactionProductIdentifier}.
   */
  product: TransactionProductIdentifier
  /**
   * Information about the paywall associated with the free trial start.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * A transaction was restored.
 */
export interface TransactionRestoreEvent {
  /** A transaction was restored. */
  event: "transactionRestore"
  /**
   * The type of restoration process.
   * See {@link RestoreType}.
   */
  restoreType: RestoreType
  /**
   * Information about the paywall associated with the restoration.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * A transaction timed out.
 */
export interface TransactionTimeoutEvent {
  /** A transaction timed out. */
  event: "transactionTimeout"
  /**
   * Information about the paywall associated with the timed-out transaction.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * User attributes were updated.
 */
export interface UserAttributesEvent {
  /** User attributes were updated. */
  event: "userAttributes"
  /** The updated user attributes. */
  attributes: Record<string, any>
}

/**
 * A non-recurring product was purchased.
 */
export interface NonRecurringProductPurchaseEvent {
  /** A non-recurring product was purchased. */
  event: "nonRecurringProductPurchase"
  /**
   * Identifier of the purchased non-recurring product.
   * See {@link TransactionProductIdentifier}.
   */
  product: TransactionProductIdentifier
  /**
   * Information about the paywall associated with the purchase.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * Loading of the paywall response from the server started.
 */
export interface PaywallResponseLoadStartEvent {
  /** Loading of the paywall response from the server started. */
  event: "paywallResponseLoadStart"
  /** The name of the event or placement that triggered this load. */
  triggeredEventName: string
}

/**
 * The paywall response was not found on the server.
 */
export interface PaywallResponseLoadNotFoundEvent {
  /** The paywall response was not found on the server. */
  event: "paywallResponseLoadNotFound"
  /** The name of the event or placement that triggered this load. */
  triggeredEventName: string
}

/**
 * Loading of the paywall response from the server failed.
 */
export interface PaywallResponseLoadFailEvent {
  /** Loading of the paywall response from the server failed. */
  event: "paywallResponseLoadFail"
  /** The name of the event or placement that triggered this load. */
  triggeredEventName: string
}

/**
 * Loading of the paywall response from the server completed successfully.
 */
export interface PaywallResponseLoadCompleteEvent {
  /** Loading of the paywall response from the server completed successfully. */
  event: "paywallResponseLoadComplete"
  /** The name of the event or placement that triggered this load. */
  triggeredEventName: string
  /**
   * Information about the loaded paywall.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * The web view started loading the paywall's content.
 */
export interface PaywallWebviewLoadStartEvent {
  /** The web view started loading the paywall's content. */
  event: "paywallWebviewLoadStart"
  /**
   * Information about the paywall whose web view is loading.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * The web view failed to load the paywall's content.
 */
export interface PaywallWebviewLoadFailEvent {
  /** The web view failed to load the paywall's content. */
  event: "paywallWebviewLoadFail"
  /**
   * Information about the paywall whose web view failed to load.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * The web view completed loading the paywall's content.
 */
export interface PaywallWebviewLoadCompleteEvent {
  /** The web view completed loading the paywall's content. */
  event: "paywallWebviewLoadComplete"
  /**
   * Information about the paywall whose web view completed loading.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * The web view timed out while loading the paywall's content.
 */
export interface PaywallWebviewLoadTimeoutEvent {
  /** The web view timed out while loading the paywall's content. */
  event: "paywallWebviewLoadTimeout"
  /**
   * Information about the paywall whose web view timed out.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * The web view used fallback content for the paywall due to a loading issue.
 */
export interface PaywallWebviewLoadFallbackEvent {
  /** The web view used fallback content for the paywall due to a loading issue. */
  event: "paywallWebviewLoadFallback"
  /**
   * Information about the paywall that used fallback content.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * Loading of products for the paywall started.
 */
export interface PaywallProductsLoadStartEvent {
  /** Loading of products for the paywall started. */
  event: "paywallProductsLoadStart"
  /** The name of the event or placement that triggered this load. */
  triggeredEventName: string
  /**
   * Information about the paywall for which products are loading.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * Loading of products for the paywall failed.
 */
export interface PaywallProductsLoadFailEvent {
  /** Loading of products for the paywall failed. */
  event: "paywallProductsLoadFail"
  /** The name of the event or placement that triggered this load. */
  triggeredEventName: string
  /**
   * Information about the paywall for which product loading failed.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * Loading of products for the paywall completed successfully.
 */
export interface PaywallProductsLoadCompleteEvent {
  /** Loading of products for the paywall completed successfully. */
  event: "paywallProductsLoadComplete"
  /** The name of the event or placement that triggered this load. */
  triggeredEventName: string
  /**
   * Information about the paywall for which products completed loading.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * Retrying the loading of products for the paywall.
 */
export interface PaywallProductsLoadRetryEvent {
  /** Retrying the loading of products for the paywall. */
  event: "paywallProductsLoadRetry"
  /** The name of the event or placement that triggered this load. */
  triggeredEventName: string
  /**
   * Information about the paywall for which product loading is being retried.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
  /** The attempt number for this retry. */
  attempt: number
}

/**
 * A response to a survey was submitted by the user.
 */
export interface SurveyResponseEvent {
  /** A response to a survey was submitted by the user. */
  event: "surveyResponse"
  /**
   * The survey that was responded to.
   * See {@link Survey}.
   */
  survey: Survey
  /**
   * The option selected by the user in the survey.
   * See {@link SurveyOption}.
   */
  selectedOption: SurveyOption
  /** The custom response text entered by the user, if any. */
  customResponse?: string
  /**
   * Information about the paywall where the survey was presented.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * A request to present a paywall was made.
 */
export interface PaywallPresentationRequestEvent {
  /** A request to present a paywall was made. */
  event: "paywallPresentationRequest"
  /**
   * The status of the presentation request.
   * See {@link PaywallPresentationRequestStatus}.
   */
  status: PaywallPresentationRequestStatus
  /**
   * The reason for the status, if applicable (e.g., why a paywall was not presented).
   * See {@link PaywallPresentationRequestStatusReason}.
   */
  reason?: PaywallPresentationRequestStatusReason
}

/**
 * A custom placement (defined by the developer) was executed.
 */
export interface CustomPlacementEvent {
  /** A custom placement (defined by the developer) was executed. */
  event: "customPlacement"
  /** The name of the custom placement. */
  name: string
  /** Parameters associated with the custom placement. */
  params: Record<string, any>
  /**
   * Information about the paywall shown for this placement, if any.
   * See {@link PaywallInfo}.
   */
  paywallInfo?: PaywallInfo
}

/**
 * The paywall's web view content process was terminated.
 */
export interface PaywallWebviewProcessTerminatedEvent {
  /** The paywall's web view content process was terminated. */
  event: "paywallWebviewProcessTerminated"
  /**
   * Information about the paywall whose web view process was terminated.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
}

/**
 * Products for the paywall are missing from the store.
 */
export interface PaywallProductsLoadMissingProductsEvent {
  /** Products for the paywall are missing from the store. */
  event: "paywallProductsLoadMissingProducts"
  /** The name of the event or placement that triggered this load. */
  triggeredEventName: string
  /**
   * Information about the paywall with missing products.
   * See {@link PaywallInfo}.
   */
  paywallInfo: PaywallInfo
  /** The product identifiers that are missing. */
  identifiers: string[]
}

/**
 * A network response failed to decode.
 */
export interface NetworkDecodingFailEvent {
  /** A network response failed to decode. */
  event: "networkDecodingFail"
}

/**
 * The customer info did change.
 */
export interface CustomerInfoDidChangeEvent {
  /** The customer info did change. */
  event: "customerInfoDidChange"
}

/**
 * Integration attributes were set.
 */
export interface IntegrationAttributesEvent {
  /** Integration attributes were set. */
  event: "integrationAttributes"
  /** The integration attributes. */
  attributes: Record<string, any>
}

/**
 * A review was requested from the user.
 */
export interface ReviewRequestedEvent {
  /** A review was requested from the user. */
  event: "reviewRequested"
  /** The number of times a review has been requested. */
  count: number
}

/**
 * A permission was requested from a paywall.
 */
export interface PermissionRequestedEvent {
  /** A permission was requested from a paywall. */
  event: "permissionRequested"
  /** The name of the permission that was requested. */
  permissionName: string
  /** The identifier of the paywall that requested the permission. */
  paywallIdentifier: string
}

/**
 * A permission was granted after being requested from a paywall.
 */
export interface PermissionGrantedEvent {
  /** A permission was granted after being requested from a paywall. */
  event: "permissionGranted"
  /** The name of the permission that was granted. */
  permissionName: string
  /** The identifier of the paywall that requested the permission. */
  paywallIdentifier: string
}

/**
 * A permission was denied after being requested from a paywall.
 */
export interface PermissionDeniedEvent {
  /** A permission was denied after being requested from a paywall. */
  event: "permissionDenied"
  /** The name of the permission that was denied. */
  permissionName: string
  /** The identifier of the paywall that requested the permission. */
  paywallIdentifier: string
}

/**
 * Paywall preloading has started.
 */
export interface PaywallPreloadStartEvent {
  /** Paywall preloading has started. */
  event: "paywallPreloadStart"
  /** The number of paywalls being preloaded. */
  paywallCount: number
}

/**
 * Paywall preloading has completed.
 */
export interface PaywallPreloadCompleteEvent {
  /** Paywall preloading has completed. */
  event: "paywallPreloadComplete"
  /** The number of paywalls that were preloaded. */
  paywallCount: number
}

/**
 * A union of all possible string literal values for the `event` property from all specific Superwall event types.
 * This type can be used when you need to refer to an event type name itself.
 */
export type SuperwallEventType =
  | FirstSeenEvent["event"]
  | AppOpenEvent["event"]
  | AppLaunchEvent["event"]
  | IdentityAliasEvent["event"]
  | AppInstallEvent["event"]
  | SessionStartEvent["event"]
  | ResetEvent["event"]
  | ConfigRefreshEvent["event"]
  | ConfigFailEvent["event"]
  | ConfigAttributesEvent["event"]
  | ConfirmAllAssignmentsEvent["event"]
  | TouchesBeganEvent["event"]
  | SurveyCloseEvent["event"]
  | RestoreStartEvent["event"]
  | RestoreCompleteEvent["event"]
  | RestoreFailEvent["event"]
  | AdServicesTokenRequestStartEvent["event"]
  | AdServicesTokenRequestFailEvent["event"]
  | AdServicesTokenRequestCompleteEvent["event"]
  | ShimmerViewStartEvent["event"]
  | ShimmerViewCompleteEvent["event"]
  | RedemptionStartEvent["event"]
  | RedemptionCompleteEvent["event"]
  | RedemptionFailEvent["event"]
  | EnrichmentStartEvent["event"]
  | EnrichmentCompleteEvent["event"]
  | EnrichmentFailEvent["event"]
  | UnknownEvent["event"]
  | DeviceAttributesEvent["event"]
  | SubscriptionStatusDidChangeEvent["event"]
  | AppCloseEvent["event"]
  | DeepLinkEvent["event"]
  | TriggerFireEvent["event"]
  | PaywallOpenEvent["event"]
  | PaywallCloseEvent["event"]
  | PaywallDeclineEvent["event"]
  | TransactionStartEvent["event"]
  | TransactionFailEvent["event"]
  | TransactionAbandonEvent["event"]
  | TransactionCompleteEvent["event"]
  | SubscriptionStartEvent["event"]
  | FreeTrialStartEvent["event"]
  | TransactionRestoreEvent["event"]
  | TransactionTimeoutEvent["event"]
  | UserAttributesEvent["event"]
  | NonRecurringProductPurchaseEvent["event"]
  | PaywallResponseLoadStartEvent["event"]
  | PaywallResponseLoadNotFoundEvent["event"]
  | PaywallResponseLoadFailEvent["event"]
  | PaywallResponseLoadCompleteEvent["event"]
  | PaywallWebviewLoadStartEvent["event"]
  | PaywallWebviewLoadFailEvent["event"]
  | PaywallWebviewLoadCompleteEvent["event"]
  | PaywallWebviewLoadTimeoutEvent["event"]
  | PaywallWebviewLoadFallbackEvent["event"]
  | PaywallProductsLoadStartEvent["event"]
  | PaywallProductsLoadFailEvent["event"]
  | PaywallProductsLoadCompleteEvent["event"]
  | PaywallProductsLoadRetryEvent["event"]
  | SurveyResponseEvent["event"]
  | PaywallPresentationRequestEvent["event"]
  | CustomPlacementEvent["event"]
  | PaywallWebviewProcessTerminatedEvent["event"]
  | PaywallProductsLoadMissingProductsEvent["event"]
  | NetworkDecodingFailEvent["event"]
  | CustomerInfoDidChangeEvent["event"]
  | IntegrationAttributesEvent["event"]
  | ReviewRequestedEvent["event"]
  | PermissionRequestedEvent["event"]
  | PermissionGrantedEvent["event"]
  | PermissionDeniedEvent["event"]
  | PaywallPreloadStartEvent["event"]
  | PaywallPreloadCompleteEvent["event"]

/**
 * Represents a Superwall event that can be tracked by the SDK.
 * This is a discriminated union type where the `event` property determines the specific event and its associated payload.
 * These events provide insights into user behavior, SDK operations, and paywall interactions.
 */
export type SuperwallEvent =
  | FirstSeenEvent
  | AppOpenEvent
  | AppLaunchEvent
  | IdentityAliasEvent
  | AppInstallEvent
  | SessionStartEvent
  | ResetEvent
  | ConfigRefreshEvent
  | ConfigFailEvent
  | ConfigAttributesEvent
  | ConfirmAllAssignmentsEvent
  | TouchesBeganEvent
  | SurveyCloseEvent
  | RestoreStartEvent
  | RestoreCompleteEvent
  | RestoreFailEvent
  | AdServicesTokenRequestStartEvent
  | AdServicesTokenRequestFailEvent
  | AdServicesTokenRequestCompleteEvent
  | ShimmerViewStartEvent
  | ShimmerViewCompleteEvent
  | RedemptionStartEvent
  | RedemptionCompleteEvent
  | RedemptionFailEvent
  | EnrichmentStartEvent
  | EnrichmentCompleteEvent
  | EnrichmentFailEvent
  | UnknownEvent
  | DeviceAttributesEvent
  | SubscriptionStatusDidChangeEvent
  | AppCloseEvent
  | DeepLinkEvent
  | TriggerFireEvent
  | PaywallOpenEvent
  | PaywallCloseEvent
  | PaywallDeclineEvent
  | TransactionStartEvent
  | TransactionFailEvent
  | TransactionAbandonEvent
  | TransactionCompleteEvent
  | SubscriptionStartEvent
  | FreeTrialStartEvent
  | TransactionRestoreEvent
  | TransactionTimeoutEvent
  | UserAttributesEvent
  | NonRecurringProductPurchaseEvent
  | PaywallResponseLoadStartEvent
  | PaywallResponseLoadNotFoundEvent
  | PaywallResponseLoadFailEvent
  | PaywallResponseLoadCompleteEvent
  | PaywallWebviewLoadStartEvent
  | PaywallWebviewLoadFailEvent
  | PaywallWebviewLoadCompleteEvent
  | PaywallWebviewLoadTimeoutEvent
  | PaywallWebviewLoadFallbackEvent
  | PaywallProductsLoadStartEvent
  | PaywallProductsLoadFailEvent
  | PaywallProductsLoadCompleteEvent
  | PaywallProductsLoadRetryEvent
  | SurveyResponseEvent
  | PaywallPresentationRequestEvent
  | CustomPlacementEvent
  | PaywallWebviewProcessTerminatedEvent
  | PaywallProductsLoadMissingProductsEvent
  | NetworkDecodingFailEvent
  | CustomerInfoDidChangeEvent
  | IntegrationAttributesEvent
  | ReviewRequestedEvent
  | PermissionRequestedEvent
  | PermissionGrantedEvent
  | PermissionDeniedEvent
  | PaywallPreloadStartEvent
  | PaywallPreloadCompleteEvent

/**
 * Contains information about a Superwall event, including the specific {@link SuperwallEvent}
 * that occurred and any associated parameters. This structure is used by the `onSuperwallEvent` callback
 * in {@link SuperwallEventCallbacks} and the `handleSuperwallEvent` in {@link SuperwallExpoModuleEvents}.
 */
export interface SuperwallEventInfo {
  /** The specific Superwall event that occurred. See {@link SuperwallEvent}. */
  event: SuperwallEvent
  /**
   * Additional parameters associated with the event. The structure of these parameters
   * depends on the specific event type and often mirrors the properties defined within
   * the {@link SuperwallEvent} union members.
   */
  params: Record<string, any>
}

/**
 * Defines the verbosity level for logging within the Superwall SDK.
 * - `debug`: Outputs detailed debugging information, useful for development and troubleshooting.
 * - `info`: Outputs general information about SDK activity and state changes.
 * - `warn`: Outputs warnings about potential issues or deprecated usage.
 * - `error`: Outputs error messages for issues encountered by the SDK.
 * - `none`: Disables all logging from the SDK.
 */
export type LogLevel = "debug" | "info" | "warn" | "error" | "none"

/**
 * Defines the scope of logging within the Superwall SDK, allowing for targeted logging of specific SDK components or features.
 * Using these scopes can help narrow down logs to relevant areas when debugging.
 *
 * Possible values include:
 * - `localizationManager`: Logs related to localization and language settings.
 * - `bounceButton`: Logs related to the animated bounce button feature.
 * - `coreData`: Logs related to internal data storage (native specific).
 * - `configManager`: Logs related to fetching and managing Superwall configurations.
 * - `identityManager`: Logs related to user identification and session management.
 * - `debugManager`: Logs related to the Superwall debugger.
 * - `debugViewController`: Logs related to the debugger's view controller (native specific).
 * - `localizationViewController`: Logs related to localization view controllers (native specific).
 * - `gameControllerManager`: Logs related to game controller interactions (native specific).
 * - `device`: Logs related to device information and properties.
 * - `network`: Logs related to network requests made by the SDK.
 * - `paywallEvents`: Logs related to events occurring on the paywall itself.
 * - `productsManager`: Logs related to fetching and managing product information from app stores.
 * - `storeKitManager`: Logs related to StoreKit interactions (iOS specific).
 * - `placements`: Logs related to placements and their evaluation.
 * - `receipts`: Logs related to App Store receipt validation and processing.
 * - `superwallCore`: Logs related to the core functionalities of the Superwall SDK.
 * - `paywallPresentation`: Logs related to the presentation logic of paywalls.
 * - `paywallTransactions`: Logs related to transactions initiated from paywalls.
 * - `paywallViewController`: Logs related to the paywall view controller (native specific).
 * - `cache`: Logs related to caching mechanisms within the SDK.
 * - `all`: Enables logging for all scopes.
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

export type OnPurchaseParamsIOS = { productId: string; platform: "ios" }

export type OnPurchaseParamsAndroid = {
  productId: string
  platform: "android"
  basePlanId: string
  offerId?: string
}

export type OnPurchaseParams = OnPurchaseParamsIOS | OnPurchaseParamsAndroid

/**
 * Defines the events emitted by the native Superwall Expo module that can be listened to.
 * These events provide a way to react to various SDK activities and user interactions.
 * Use `SuperwallExpoModule.addListener(eventName, callback)` to subscribe.
 */
export type SuperwallExpoModuleEvents = {
  /**
   * Emitted when a paywall is presented to the user.
   * @param params - Event parameters.
   * @param params.paywallInfoJson - JSON representation of {@link PaywallInfo} for the presented paywall.
   * @param params.handlerId - Identifier for the handler that triggered this presentation (e.g., from `usePlacement`).
   */
  onPaywallPresent: (params: { paywallInfoJson: PaywallInfo; handlerId: string }) => void
  /**
   * Emitted when a paywall is dismissed by the user or programmatically.
   * @param params - Event parameters.
   * @param params.paywallInfoJson - JSON representation of {@link PaywallInfo} for the dismissed paywall.
   * @param params.result - The result of the paywall interaction. See {@link PaywallResult}.
   * @param params.handlerId - Identifier for the handler associated with this paywall.
   */
  onPaywallDismiss: (params: {
    paywallInfoJson: PaywallInfo
    result: PaywallResult
    handlerId: string
  }) => void
  /**
   * Emitted when an error occurs during the paywall presentation process.
   * @param params - Event parameters.
   * @param params.errorString - A string describing the error.
   * @param params.handlerId - Identifier for the handler associated with this error, if applicable.
   */
  onPaywallError: (params: { errorString: string; handlerId: string }) => void
  /**
   * Emitted when a paywall presentation is skipped (e.g., user is in a holdout group, no rule match).
   * @param params - Event parameters.
   * @param params.skippedReason - The reason why the paywall was skipped. See {@link PaywallSkippedReason}.
   * @param params.handlerId - Identifier for the handler associated with this skipped paywall.
   */
  onPaywallSkip: (params: { skippedReason: PaywallSkippedReason; handlerId: string }) => void

  // --- SuperwallDelegateBridge Events ---
  /**
   * Emitted when the user's subscription status changes.
   * @param params - Event parameters.
   * @param params.from - The previous subscription status. See {@link SubscriptionStatus}.
   * @param params.to - The new subscription status. See {@link SubscriptionStatus}.
   */
  subscriptionStatusDidChange: (params: {
    from: SubscriptionStatus
    to: SubscriptionStatus
  }) => void
  /**
   * Emitted for various internal Superwall events, providing a detailed stream of SDK activity.
   * @param params - Event parameters.
   * @param params.eventInfo - Detailed information about the Superwall event. See {@link SuperwallEventInfo}.
   */
  handleSuperwallEvent: (params: { eventInfo: SuperwallEventInfo }) => void
  /**
   * Emitted when a custom action is invoked from a paywall's JavaScript.
   * @param params - Event parameters.
   * @param params.name - The name of the custom action that was invoked.
   */
  handleCustomPaywallAction: (params: { name: string }) => void
  /**
   * Emitted just before a paywall is dismissed.
   * @param params - Event parameters.
   * @param params.info - Information about the paywall that will be dismissed. See {@link PaywallInfo}.
   */
  willDismissPaywall: (params: { info: PaywallInfo }) => void
  /**
   * Emitted just before a paywall is presented.
   * @param params - Event parameters.
   * @param params.info - Information about the paywall that will be presented. See {@link PaywallInfo}.
   */
  willPresentPaywall: (params: { info: PaywallInfo }) => void
  /**
   * Emitted after a paywall has been dismissed.
   * @param params - Event parameters.
   * @param params.info - Information about the paywall that was dismissed. See {@link PaywallInfo}.
   */
  didDismissPaywall: (params: { info: PaywallInfo }) => void
  /**
   * Emitted after a paywall has been presented.
   * @param params - Event parameters.
   * @param params.info - Information about the paywall that was presented. See {@link PaywallInfo}.
   */
  didPresentPaywall: (params: { info: PaywallInfo }) => void
  /**
   * Emitted when the paywall intends to open an external URL.
   * @param params - Event parameters.
   * @param params.url - The URL that the paywall intends to open.
   */
  paywallWillOpenURL: (params: { url: string }) => void
  /**
   * Emitted when the paywall intends to open a deep link.
   * @param params - Event parameters.
   * @param params.url - The deep link URL that the paywall intends to open.
   */
  paywallWillOpenDeepLink: (params: { url: string }) => void
  /**
   * Emitted for logging messages generated by the native SDK.
   * @param params - Log parameters.
   * @param params.level - The log level. See {@link LogLevel}.
   * @param params.scope - The scope of the log. See {@link LogScope}.
   * @param params.message - The log message (can be null).
   * @param params.info - Additional structured information for the log (can be null).
   * @param params.error - Error message if the log represents an error (can be null).
   */
  handleLog: (params: {
    level: LogLevel
    scope: LogScope
    message: string | null
    info: Record<string, any> | null
    error: string | null
  }) => void
  /**
   * Emitted before the SDK attempts to redeem a promotional link or code.
   * @param params - Event parameters. For iOS, this is typically an empty dictionary.
   */
  willRedeemLink: (params: Record<string, never> | null) => void
  /**
   * Emitted after the SDK has attempted to redeem a promotional link or code.
   * @param params - The result of the redemption attempt. See {@link RedemptionResult}.
   */
  didRedeemLink: (params: RedemptionResult) => void

  // Purchase Events
  /**
   * Emitted when a purchase is initiated by the user from a paywall.
   * @param params - Parameters related to the purchase.
   *   - For iOS: `{ productId: string; platform: "ios" }`
   *   - For Android: `{ productId: string; platform: "android"; basePlanId: string; offerId?: string }`
   *     (offerId is optional for Android).
   */
  onPurchase: (params: OnPurchaseParams) => void
  /**
   * Emitted when a purchase restoration process is initiated by the user.
   * @param params - Event parameters. For iOS, this is `null`.
   */
  onPurchaseRestore: () => void
  /**
   * Emitted when the back button is pressed while a paywall is displayed (Android only).
   * This is only triggered when `rerouteBackButton` is enabled in the paywall settings.
   * @param params - Event parameters.
   * @param params.paywallInfo - Information about the paywall that is currently displayed. See {@link PaywallInfo}.
   * @platform Android
   */
  onBackPressed: (params: { paywallInfo: PaywallInfo }) => void
}

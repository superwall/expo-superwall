/**
 * RedemptionResult and related types
 * Corresponds to the Swift RedemptionResult enum and its associated types
 */

import { Entitlement } from "./Entitlement"

/**
 * @category Models
 * @since 1.0.0
 * Represents a product involved in a redemption transaction with comprehensive pricing and localization information
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
 * @category Models
 * @since 0.0.15
 * Information about an error that occurred during code redemption
 */
export interface ErrorInfo {
  /** The error message */
  message: string
}

/**
 * @category Models
 * @since 0.0.15
 * Information about an expired redemption code
 */
export interface ExpiredCodeInfo {
  /** Whether the redemption email was resent */
  resent: boolean
  /** Optional obfuscated email address that the redemption email was sent to */
  obfuscatedEmail?: string
}

/**
 * @category Types
 * @since 0.0.15
 * Represents the ownership of a redemption code
 */
export type Ownership =
  | { type: "APP_USER"; appUserId: string }
  | { type: "DEVICE"; deviceId: string }

/**
 * @category Types
 * @since 0.0.15
 * Store identifiers for the purchase
 */
export type StoreIdentifiers =
  | {
      store: "STRIPE"
      stripeCustomerId: string
      stripeSubscriptionIds: string[]
    }
  | {
      store: "PADDLE"
      paddleCustomerId: string
      paddleSubscriptionIds: string[]
    }
  | { store: string; [key: string]: any } // For unknown store types

/**
 * @category Models
 * @since 0.0.15
 * Information about the purchaser
 */
export interface PurchaserInfo {
  /** The app user ID of the purchaser */
  appUserId: string
  /** The email address of the purchaser (optional) */
  email?: string
  /** The identifiers for the store the purchase was made from */
  storeIdentifiers: StoreIdentifiers
}

/**
 * @category Models
 * @since 0.0.15
 * Information about the paywall the purchase was made from
 */
export interface PaywallInfo {
  /** The identifier of the paywall */
  identifier: string
  /** The name of the placement */
  placementName: string
  /** The params of the placement */
  placementParams: Record<string, any>
  /** The ID of the paywall variant */
  variantId: string
  /** The ID of the experiment that the paywall belongs to */
  experimentId: string
  /** 
   * The product identifier that the user purchased 
   * @deprecated Use `product.identifier` instead. This property will be removed in a future version.
   */
  productIdentifier?: string
  /** The product that the user purchased */
  product?: PaywallProduct 
}

/**
 * @category Models
 * @since 0.0.15
 * Information about a successful redemption
 */
export interface RedemptionInfo {
  /** The ownership of the code */
  ownership: Ownership
  /** Information about the purchaser */
  purchaserInfo: PurchaserInfo
  /** Information about the paywall the purchase was made from (optional) */
  paywallInfo?: PaywallInfo
  /** The entitlements granted by the redemption */
  entitlements: Entitlement[]
}

/**
 * @category Types
 * @since 0.0.15
 * The result of redeeming a code via web checkout
 */
export type RedemptionResult =
  | { status: "SUCCESS"; code: string; redemptionInfo: RedemptionInfo }
  | { status: "ERROR"; code: string; error: ErrorInfo }
  | { status: "CODE_EXPIRED"; code: string; expired: ExpiredCodeInfo }
  | { status: "INVALID_CODE"; code: string }
  | {
      status: "EXPIRED_SUBSCRIPTION"
      code: string
      redemptionInfo: RedemptionInfo
    }

/**
 * @category Utils
 * @since 0.0.15
 * Static methods for working with RedemptionResult
 */
export class RedemptionResults {
  static fromJson(json: any): RedemptionResult {
    const { status, code } = json

    switch (status) {
      case "SUCCESS":
        return {
          status,
          code,
          redemptionInfo: RedemptionResults.parseRedemptionInfo(json.redemptionInfo),
        }

      case "ERROR":
        return {
          status,
          code,
          error: {
            message: json.error.message,
          },
        }

      case "CODE_EXPIRED":
        return {
          status,
          code,
          expired: {
            resent: json.expired.resent,
            obfuscatedEmail: json.expired.obfuscatedEmail,
          },
        }

      case "INVALID_CODE":
        return {
          status,
          code,
        }

      case "EXPIRED_SUBSCRIPTION":
        return {
          status,
          code,
          redemptionInfo: RedemptionResults.parseRedemptionInfo(json.redemptionInfo),
        }

      default:
        throw new Error(`Unknown RedemptionResult status: ${status}`)
    }
  }

  private static parseRedemptionInfo(json: any): RedemptionInfo {
    const result: RedemptionInfo = {
      ownership: RedemptionResults.parseOwnership(json.ownership),
      purchaserInfo: RedemptionResults.parsePurchaserInfo(json.purchaserInfo),
      entitlements: Array.isArray(json.entitlements)
        ? json.entitlements.map((e: any) => Entitlement.fromJson(e))
        : [],
    }

    if (json.paywallInfo) {
      result.paywallInfo = {
        identifier: json.paywallInfo.identifier,
        placementName: json.paywallInfo.placementName,
        placementParams: json.paywallInfo.placementParams || {},
        variantId: json.paywallInfo.variantId,
        experimentId: json.paywallInfo.experimentId,
        productIdentifier: json.paywallInfo.productIdentifier || undefined,
        product: json.paywallInfo.product ? {
          identifier: json.paywallInfo.product.identifier,
          languageCode: json.paywallInfo.product.languageCode,
          locale: json.paywallInfo.product.locale,
          currencyCode: json.paywallInfo.product.currencyCode,
          currencySymbol: json.paywallInfo.product.currencySymbol,
          period: json.paywallInfo.product.period,
          periodly: json.paywallInfo.product.periodly,
          localizedPeriod: json.paywallInfo.product.localizedPeriod,
          periodAlt: json.paywallInfo.product.periodAlt,
          periodDays: json.paywallInfo.product.periodDays,
          periodWeeks: json.paywallInfo.product.periodWeeks,
          periodMonths: json.paywallInfo.product.periodMonths,
          periodYears: json.paywallInfo.product.periodYears,
          rawPrice: json.paywallInfo.product.rawPrice,
          price: json.paywallInfo.product.price,
          dailyPrice: json.paywallInfo.product.dailyPrice,
          weeklyPrice: json.paywallInfo.product.weeklyPrice,
          monthlyPrice: json.paywallInfo.product.monthlyPrice,
          yearlyPrice: json.paywallInfo.product.yearlyPrice,
          rawTrialPeriodPrice: json.paywallInfo.product.rawTrialPeriodPrice,
          trialPeriodPrice: json.paywallInfo.product.trialPeriodPrice,
          trialPeriodDailyPrice: json.paywallInfo.product.trialPeriodDailyPrice,
          trialPeriodWeeklyPrice: json.paywallInfo.product.trialPeriodWeeklyPrice,
          trialPeriodMonthlyPrice: json.paywallInfo.product.trialPeriodMonthlyPrice,
          trialPeriodYearlyPrice: json.paywallInfo.product.trialPeriodYearlyPrice,
          trialPeriodDays: json.paywallInfo.product.trialPeriodDays,
          trialPeriodWeeks: json.paywallInfo.product.trialPeriodWeeks,
          trialPeriodMonths: json.paywallInfo.product.trialPeriodMonths,
          trialPeriodYears: json.paywallInfo.product.trialPeriodYears,
          trialPeriodText: json.paywallInfo.product.trialPeriodText,
          trialPeriodEndDate: json.paywallInfo.product.trialPeriodEndDate
        } : undefined
      }
    }

    return result
  }
  private static parseOwnership(json: any): Ownership {
    switch (json.type) {
      case "APP_USER":
        return {
          type: "APP_USER",
          appUserId: json.appUserId,
        }

      case "DEVICE":
        return {
          type: "DEVICE",
          deviceId: json.deviceId,
        }

      default:
        throw new Error(`Unknown ownership type: ${json.type}`)
    }
  }

  private static parsePurchaserInfo(json: any): PurchaserInfo {
    const result: PurchaserInfo = {
      appUserId: json.appUserId,
      storeIdentifiers: RedemptionResults.parseStoreIdentifiers(json.storeIdentifiers),
    }

    if (json.email) {
      result.email = json.email
    }

    return result
  }

  private static parseStoreIdentifiers(json: any): StoreIdentifiers {
    if (json.store === "STRIPE") {
      return {
        store: "STRIPE",
        stripeCustomerId: json.stripeCustomerId,
        stripeSubscriptionIds: json.stripeSubscriptionIds || [],
      }
    }

    return { ...json }
  }
}

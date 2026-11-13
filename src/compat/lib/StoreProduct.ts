import type { StoreProductPlatformDetails } from "../../SuperwallExpoModule.types"
import { Entitlement } from "./Entitlement"

/**
 * @category Models
 * @since 0.0.15
 * A wrapper around a store product.
 */
export class StoreProduct {
  // The product identifier
  productIdentifier: string

  // The localized price.
  localizedPrice: string

  // The localized subscription period.
  localizedSubscriptionPeriod: string

  // The subscription period unit, e.g., week.
  period: string

  // The number of weeks in the product's subscription period.
  periodWeeks: number

  // The string value of the number of weeks in the product's subscription period.
  periodWeeksString: string

  // The number of months in the product's subscription period.
  periodMonths: number

  // The string value of the number of months in the product's subscription period.
  periodMonthsString: string

  // The number of years in the product's subscription period.
  periodYears: number

  // The string value of the number of years in the product's subscription period.
  periodYearsString: string

  // The number of days in the product's subscription period.
  periodDays: number

  // The string value of the number of days in the product's subscription period.
  periodDaysString: string

  // The product's localized daily price.
  dailyPrice: string

  // The product's localized weekly price.
  weeklyPrice: string

  // The product's localized monthly price.
  monthlyPrice: string

  // The product's localized yearly price.
  yearlyPrice: string

  // A boolean indicating whether the product has an introductory price.
  hasFreeTrial: boolean

  // The product's trial period end date.
  trialPeriodEndDate?: Date | null

  // The product's trial period end date formatted using `DateFormatter.Style.medium`
  trialPeriodEndDateString: string

  // The product's introductory price duration in days.
  localizedTrialPeriodPrice: string

  // The product's introductory price duration in days.
  trialPeriodPrice: number

  // The product's introductory price duration in days.
  trialPeriodDays: number

  // The product's string value of the introductory price duration in days.
  trialPeriodDaysString: string

  // The product's introductory price duration in weeks.
  trialPeriodWeeks: number

  // The product's string value of the introductory price duration in weeks.
  trialPeriodWeeksString: string

  // The product's introductory price duration in months.
  trialPeriodMonths: number

  // The product's string value of the introductory price duration in months.
  trialPeriodMonthsString: string

  // The product's introductory price duration in years.
  trialPeriodYears: number

  // The product's string value of the introductory price duration in years.
  trialPeriodYearsString: string

  // The product's introductory price duration in days, e.g., 7-day.
  trialPeriodText: string

  // The product's locale.
  locale: string

  // The language code of the product's locale.
  languageCode?: string | null

  // The currency code of the product's locale.
  currencyCode?: string | null

  // The currency symbol of the product's locale.
  currencySymbol?: string | null

  // A boolean that indicates whether the product is family shareable.
  isFamilyShareable: boolean

  // The region code of the product's price locale.
  regionCode?: string | null

  // The price of the product in the local currency.
  price: number

  /** The length of the product's subscription period, or `null` if it isn't a subscription. */
  subscriptionPeriod?: { unit: "day" | "week" | "month" | "year"; value: number } | null

  /** iOS only. The product's introductory offer, if it has one. */
  introductoryDiscount?: StoreProductPlatformDetails["introductoryDiscount"]

  /** iOS only. The product's promotional offers. */
  discounts?: StoreProductPlatformDetails["discounts"]

  /** iOS only. The App Store billing plan the product is sold on (iOS 26.4+). */
  billingPlanType?: StoreProductPlatformDetails["billingPlanType"]

  /** iOS only. Whether a billing plan is configured for the product and available on the device. */
  isBillingPlanAvailable?: boolean

  /** iOS only. The entitlements the product unlocks. */
  entitlements?: Entitlement[]

  /** Android only. The Google Play product type, such as `"subs"` or `"inapp"`. */
  productType?: string

  /** Android only. The country code of the Google Play storefront. */
  storeFrontCountryCode?: string | null

  /** Android only. Whether it's a custom product, bought through your purchase controller. */
  isCustomProduct?: boolean

  /**
   * The transaction ID the SDK generated for a purchase of a custom product. Send it back
   * from your server with the purchase's events to attribute the revenue to the paywall. On iOS
   * it's only sent when the app builds SuperwallKit 4.19 or later (see the README).
   */
  customTransactionId?: string | null

  /** The product's attributes, as referenced in audience filters and paywalls. */
  attributes?: Record<string, string>

  /**
   * Android only. The product's full identifier, `productId:basePlanId:offerId`, which
   * `Superwall.purchase()` uses to buy this base plan and offer.
   */
  fullIdentifier?: string

  constructor({
    productIdentifier,
    localizedPrice,
    localizedSubscriptionPeriod,
    period,
    periodWeeks,
    periodWeeksString,
    periodMonths,
    periodMonthsString,
    periodYears,
    periodYearsString,
    periodDays,
    periodDaysString,
    dailyPrice,
    weeklyPrice,
    monthlyPrice,
    yearlyPrice,
    hasFreeTrial,
    trialPeriodEndDate,
    trialPeriodEndDateString,
    localizedTrialPeriodPrice,
    trialPeriodPrice,
    trialPeriodDays,
    trialPeriodDaysString,
    trialPeriodWeeks,
    trialPeriodWeeksString,
    trialPeriodMonths,
    trialPeriodMonthsString,
    trialPeriodYears,
    trialPeriodYearsString,
    trialPeriodText,
    locale,
    languageCode,
    currencyCode,
    currencySymbol,
    isFamilyShareable,
    regionCode,
    price,
    subscriptionPeriod,
    introductoryDiscount,
    discounts,
    billingPlanType,
    isBillingPlanAvailable,
    entitlements,
    productType,
    storeFrontCountryCode,
    isCustomProduct,
    customTransactionId,
    attributes,
    fullIdentifier,
  }: {
    productIdentifier: string
    localizedPrice: string
    localizedSubscriptionPeriod: string
    period: string
    periodWeeks: number
    periodWeeksString: string
    periodMonths: number
    periodMonthsString: string
    periodYears: number
    periodYearsString: string
    periodDays: number
    periodDaysString: string
    dailyPrice: string
    weeklyPrice: string
    monthlyPrice: string
    yearlyPrice: string
    hasFreeTrial: boolean
    trialPeriodEndDate?: Date | null
    trialPeriodEndDateString: string
    localizedTrialPeriodPrice: string
    trialPeriodPrice: number
    trialPeriodDays: number
    trialPeriodDaysString: string
    trialPeriodWeeks: number
    trialPeriodWeeksString: string
    trialPeriodMonths: number
    trialPeriodMonthsString: string
    trialPeriodYears: number
    trialPeriodYearsString: string
    trialPeriodText: string
    locale: string
    languageCode?: string | null
    currencyCode?: string | null
    currencySymbol?: string | null
    isFamilyShareable: boolean
    regionCode?: string | null
    price: number
    subscriptionPeriod?: { unit: "day" | "week" | "month" | "year"; value: number } | null
    introductoryDiscount?: StoreProductPlatformDetails["introductoryDiscount"]
    discounts?: StoreProductPlatformDetails["discounts"]
    billingPlanType?: StoreProductPlatformDetails["billingPlanType"]
    isBillingPlanAvailable?: boolean
    entitlements?: Entitlement[]
    productType?: string
    storeFrontCountryCode?: string | null
    isCustomProduct?: boolean
    customTransactionId?: string | null
    attributes?: Record<string, string>
    fullIdentifier?: string
  }) {
    this.productIdentifier = productIdentifier
    this.localizedPrice = localizedPrice
    this.localizedSubscriptionPeriod = localizedSubscriptionPeriod
    this.period = period
    this.periodWeeks = periodWeeks
    this.periodWeeksString = periodWeeksString
    this.periodMonths = periodMonths
    this.periodMonthsString = periodMonthsString
    this.periodYears = periodYears
    this.periodYearsString = periodYearsString
    this.periodDays = periodDays
    this.periodDaysString = periodDaysString
    this.dailyPrice = dailyPrice
    this.weeklyPrice = weeklyPrice
    this.monthlyPrice = monthlyPrice
    this.yearlyPrice = yearlyPrice
    this.hasFreeTrial = hasFreeTrial
    this.trialPeriodEndDate = trialPeriodEndDate
    this.trialPeriodEndDateString = trialPeriodEndDateString
    this.localizedTrialPeriodPrice = localizedTrialPeriodPrice
    this.trialPeriodPrice = trialPeriodPrice
    this.trialPeriodDays = trialPeriodDays
    this.trialPeriodDaysString = trialPeriodDaysString
    this.trialPeriodWeeks = trialPeriodWeeks
    this.trialPeriodWeeksString = trialPeriodWeeksString
    this.trialPeriodMonths = trialPeriodMonths
    this.trialPeriodMonthsString = trialPeriodMonthsString
    this.trialPeriodYears = trialPeriodYears
    this.trialPeriodYearsString = trialPeriodYearsString
    this.trialPeriodText = trialPeriodText
    this.locale = locale
    this.languageCode = languageCode
    this.currencyCode = currencyCode
    this.currencySymbol = currencySymbol
    this.isFamilyShareable = isFamilyShareable
    this.regionCode = regionCode
    this.price = price
    this.subscriptionPeriod = subscriptionPeriod
    this.introductoryDiscount = introductoryDiscount
    this.discounts = discounts
    this.billingPlanType = billingPlanType
    this.isBillingPlanAvailable = isBillingPlanAvailable
    this.entitlements = entitlements
    this.productType = productType
    this.storeFrontCountryCode = storeFrontCountryCode
    this.isCustomProduct = isCustomProduct
    this.customTransactionId = customTransactionId
    this.attributes = attributes
    this.fullIdentifier = fullIdentifier
  }

  static fromJson(json: any): StoreProduct {
    return new StoreProduct({
      productIdentifier: json.productIdentifier,
      localizedPrice: json.localizedPrice,
      localizedSubscriptionPeriod: json.localizedSubscriptionPeriod,
      period: json.period,
      periodWeeks: json.periodWeeks,
      periodWeeksString: json.periodWeeksString,
      periodMonths: json.periodMonths,
      periodMonthsString: json.periodMonthsString,
      periodYears: json.periodYears,
      periodYearsString: json.periodYearsString,
      periodDays: json.periodDays,
      periodDaysString: json.periodDaysString,
      dailyPrice: json.dailyPrice,
      weeklyPrice: json.weeklyPrice,
      monthlyPrice: json.monthlyPrice,
      yearlyPrice: json.yearlyPrice,
      hasFreeTrial: json.hasFreeTrial,
      trialPeriodEndDate: json.trialPeriodEndDate ? new Date(json.trialPeriodEndDate) : null,
      trialPeriodEndDateString: json.trialPeriodEndDateString,
      localizedTrialPeriodPrice: json.localizedTrialPeriodPrice,
      trialPeriodPrice: json.trialPeriodPrice,
      trialPeriodDays: json.trialPeriodDays,
      trialPeriodDaysString: json.trialPeriodDaysString,
      trialPeriodWeeks: json.trialPeriodWeeks,
      trialPeriodWeeksString: json.trialPeriodWeeksString,
      trialPeriodMonths: json.trialPeriodMonths,
      trialPeriodMonthsString: json.trialPeriodMonthsString,
      trialPeriodYears: json.trialPeriodYears,
      trialPeriodYearsString: json.trialPeriodYearsString,
      trialPeriodText: json.trialPeriodText,
      locale: json.locale,
      languageCode: json.languageCode || null,
      currencyCode: json.currencyCode || null,
      currencySymbol: json.currencySymbol || null,
      isFamilyShareable: json.isFamilyShareable ?? false,
      regionCode: json.regionCode || null,
      price: json.price,
      subscriptionPeriod: json.subscriptionPeriod,
      introductoryDiscount: json.introductoryDiscount,
      discounts: json.discounts,
      billingPlanType: json.billingPlanType,
      isBillingPlanAvailable: json.isBillingPlanAvailable,
      entitlements: Array.isArray(json.entitlements)
        ? json.entitlements.map((e: any) => Entitlement.fromJson(e))
        : undefined,
      productType: json.productType,
      storeFrontCountryCode: json.storeFrontCountryCode,
      isCustomProduct: json.isCustomProduct,
      customTransactionId: json.customTransactionId,
      attributes: json.attributes,
      fullIdentifier: json.fullIdentifier,
    })
  }
}

export interface TransactionProductPrice {
  /** The raw price of the product. For example, 29.99. */
  raw: number
  /** The price localized to the currency of the device. For example, "£29.99". */
  localized: string
  /** The daily cost of the product. For example, "£0.99". */
  daily: string
  /** The weekly cost of the product. For example, "£1.99". */
  weekly: string
  /** The monthly cost of the product. For example, "£2.99". */
  monthly: string
  /** The yearly cost of the product. For example, "£29.99". */
  yearly: string
}

export interface TransactionProductTrialPeriod {
  /** The number of days the trial period lasts. */
  days: number
  /** The number of weeks the trial period lasts. */
  weeks: number
  /** The number of months the trial period lasts. */
  months: number
  /** The number of years the trial period lasts. */
  years: number
  /** The number of day the trial period lasts in the format "X-day". For example, for a seven day trial, it would be "7-day". */
  text: string
  /** The end date of the trial period as a timestamp in milliseconds. */
  endAt?: number
}

export interface TransactionProductPeriod {
  /** The shortened representation of the duration of the subscription. For example, "1 year". */
  alt: string
  /** The subscription period with -ly added on the end. For example, "weekly". */
  ly: string
  /** The value representing the duration of the product interval, from a day up to a year. */
  unit: string
  /** The number of days the subscription lasts. */
  days: number
  /** The number of weeks the subscription lasts. */
  weeks: number
  /** The number of months the subscription lasts. */
  months: number
  /** The number of years the subscription lasts. */
  years: number
}

export interface TransactionProductCurrency {
  /** The currency code of the product. For example, for "zh-Hant-HK", returns "HKD". */
  code?: string
  /** The currency symbol of the product. For example, for "zh-Hant-HK", returns "HK$". */
  symbol?: string
}

export class TransactionProduct {
  /** The unique identifier of the product (e.g., SKU). */
  id: string
  /** The identifier of the locale of the product. */
  locale: string
  /** The language code of the product locale, or undefined if has none. For example, for the locale "zh-Hant-HK", returns "zh". */
  languageCode?: string
  /** Attributes associated with the price of the product. */
  price: TransactionProductPrice
  /** The currency of the product. */
  currency: TransactionProductCurrency
  /** The trial period of the product, if available. */
  trialPeriod?: TransactionProductTrialPeriod
  /** The subscription period of the product, if available. */
  period?: TransactionProductPeriod

  constructor(
    id: string,
    locale: string,
    price: TransactionProductPrice,
    currency: TransactionProductCurrency,
    languageCode?: string,
    trialPeriod?: TransactionProductTrialPeriod,
    period?: TransactionProductPeriod
  ) {
    this.id = id
    this.locale = locale
    this.price = price
    this.currency = currency
    this.languageCode = languageCode
    this.trialPeriod = trialPeriod
    this.period = period
  }

  static fromJson(json: any): TransactionProduct {
    const price: TransactionProductPrice = {
      raw: json.price?.raw ?? 0,
      localized: json.price?.localized ?? '',
      daily: json.price?.daily ?? '',
      weekly: json.price?.weekly ?? '',
      monthly: json.price?.monthly ?? '',
      yearly: json.price?.yearly ?? ''
    }

    const currency: TransactionProductCurrency = {
      code: json.currency?.code,
      symbol: json.currency?.symbol
    }

    let trialPeriod: TransactionProductTrialPeriod | undefined
    if (json.trialPeriod) {
      trialPeriod = {
        days: json.trialPeriod.days ?? 0,
        weeks: json.trialPeriod.weeks ?? 0,
        months: json.trialPeriod.months ?? 0,
        years: json.trialPeriod.years ?? 0,
        text: json.trialPeriod.text ?? '',
        endAt: json.trialPeriod.endAt
      }
    }

    let period: TransactionProductPeriod | undefined
    if (json.period) {
      period = {
        alt: json.period.alt ?? '',
        ly: json.period.ly ?? '',
        unit: json.period.unit ?? '',
        days: json.period.days ?? 0,
        weeks: json.period.weeks ?? 0,
        months: json.period.months ?? 0,
        years: json.period.years ?? 0
      }
    }

    return new TransactionProduct(
      json.id ?? '',
      json.locale ?? '',
      price,
      currency,
      json.languageCode,
      trialPeriod,
      period
    )
  }

  toJson(): { [key: string]: any } {
    const json: { [key: string]: any } = {
      id: this.id,
      locale: this.locale,
      price: this.price,
      currency: this.currency
    }

    if (this.languageCode) {
      json.languageCode = this.languageCode
    }

    if (this.trialPeriod) {
      json.trialPeriod = this.trialPeriod
    }

    if (this.period) {
      json.period = this.period
    }

    return json
  }
}

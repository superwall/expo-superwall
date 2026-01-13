import SuperwallKit

extension TransactionProduct {
  func toJson() -> [String: Any?] {
    let dateFormatter = ISO8601DateFormatter()

    var json: [String: Any?] = [
      // Core identifiers
      "id": id,
      "productIdentifier": id,
      "fullIdentifier": id,

      // Price
      "price": NSDecimalNumber(decimal: price.raw).doubleValue,
      "localizedPrice": price.localized,
      "dailyPrice": price.daily,
      "weeklyPrice": price.weekly,
      "monthlyPrice": price.monthly,
      "yearlyPrice": price.yearly,

      // Locale and currency
      "locale": locale,
      "languageCode": languageCode,
      "currencyCode": currency.code,
      "currencySymbol": currency.symbol,

      // Attributes
      "attributes": [String: Any](),
    ]

    // Period properties
    if let period = period {
      json["localizedSubscriptionPeriod"] = period.alt
      json["period"] = String(describing: period.unit).lowercased()
      json["periodly"] = period.ly
      json["periodDays"] = period.days
      json["periodDaysString"] = String(period.days)
      json["periodWeeks"] = period.weeks
      json["periodWeeksString"] = String(period.weeks)
      json["periodMonths"] = period.months
      json["periodMonthsString"] = String(period.months)
      json["periodYears"] = period.years
      json["periodYearsString"] = String(period.years)

      // Subscription period object
      let unitString: String
      switch period.unit {
      case .day:
        unitString = "day"
      case .week:
        unitString = "week"
      case .month:
        unitString = "month"
      case .year:
        unitString = "year"
      @unknown default:
        unitString = "unknown"
      }

      // Calculate value based on which period property is non-zero
      let value: Int
      switch period.unit {
      case .day:
        value = period.days
      case .week:
        value = period.weeks
      case .month:
        value = period.months
      case .year:
        value = period.years
      @unknown default:
        value = 1
      }

      json["subscriptionPeriod"] = [
        "unit": unitString,
        "value": value,
      ]
    } else {
      json["localizedSubscriptionPeriod"] = ""
      json["period"] = ""
      json["periodly"] = ""
      json["periodDays"] = 0
      json["periodDaysString"] = "0"
      json["periodWeeks"] = 0
      json["periodWeeksString"] = "0"
      json["periodMonths"] = 0
      json["periodMonthsString"] = "0"
      json["periodYears"] = 0
      json["periodYearsString"] = "0"
      json["subscriptionPeriod"] = nil
    }

    // Trial period properties
    if let trialPeriod = trialPeriod {
      json["hasFreeTrial"] = true
      json["trialPeriodDays"] = trialPeriod.days
      json["trialPeriodDaysString"] = String(trialPeriod.days)
      json["trialPeriodWeeks"] = trialPeriod.weeks
      json["trialPeriodWeeksString"] = String(trialPeriod.weeks)
      json["trialPeriodMonths"] = trialPeriod.months
      json["trialPeriodMonthsString"] = String(trialPeriod.months)
      json["trialPeriodYears"] = trialPeriod.years
      json["trialPeriodYearsString"] = String(trialPeriod.years)
      json["trialPeriodText"] = trialPeriod.text

      if let endAt = trialPeriod.endAt {
        json["trialPeriodEndDate"] = dateFormatter.string(from: endAt)
        json["trialPeriodEndDateString"] = dateFormatter.string(from: endAt)
      } else {
        json["trialPeriodEndDate"] = nil
        json["trialPeriodEndDateString"] = ""
      }

      // Trial period price (free trial, so 0)
      json["localizedTrialPeriodPrice"] = ""
      json["trialPeriodPrice"] = 0
    } else {
      json["hasFreeTrial"] = false
      json["trialPeriodDays"] = 0
      json["trialPeriodDaysString"] = "0"
      json["trialPeriodWeeks"] = 0
      json["trialPeriodWeeksString"] = "0"
      json["trialPeriodMonths"] = 0
      json["trialPeriodMonthsString"] = "0"
      json["trialPeriodYears"] = 0
      json["trialPeriodYearsString"] = "0"
      json["trialPeriodText"] = ""
      json["trialPeriodEndDate"] = nil
      json["trialPeriodEndDateString"] = ""
      json["localizedTrialPeriodPrice"] = ""
      json["trialPeriodPrice"] = 0
    }

    return json
  }
}

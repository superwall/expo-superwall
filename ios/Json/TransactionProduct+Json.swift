import SuperwallKit

extension TransactionProduct {
  func toJson() -> [String: Any] {
    var json: [String: Any] = [
      "id": id,
      "productIdentifier": productIdentifier,
      "fullIdentifier": fullIdentifier,
      "price": price,
      "localizedPrice": localizedPrice,
      "localizedSubscriptionPeriod": localizedSubscriptionPeriod,
      "period": period,
      "periodly": periodly,
      "periodWeeks": periodWeeks,
      "periodWeeksString": periodWeeksString,
      "periodMonths": periodMonths,
      "periodMonthsString": periodMonthsString,
      "periodYears": periodYears,
      "periodYearsString": periodYearsString,
      "periodDays": periodDays,
      "periodDaysString": periodDaysString,
      "dailyPrice": dailyPrice,
      "weeklyPrice": weeklyPrice,
      "monthlyPrice": monthlyPrice,
      "yearlyPrice": yearlyPrice,
      "hasFreeTrial": hasFreeTrial,
      "localizedTrialPeriodPrice": localizedTrialPeriodPrice,
      "trialPeriodPrice": trialPeriodPrice,
      "trialPeriodEndDateString": trialPeriodEndDateString,
      "trialPeriodDays": trialPeriodDays,
      "trialPeriodDaysString": trialPeriodDaysString,
      "trialPeriodWeeks": trialPeriodWeeks,
      "trialPeriodWeeksString": trialPeriodWeeksString,
      "trialPeriodMonths": trialPeriodMonths,
      "trialPeriodMonthsString": trialPeriodMonthsString,
      "trialPeriodYears": trialPeriodYears,
      "trialPeriodYearsString": trialPeriodYearsString,
      "trialPeriodText": trialPeriodText,
      "locale": locale,
      "attributes": attributes,
    ]

    // Handle optional properties
    if let trialPeriodEndDate = trialPeriodEndDate {
      json["trialPeriodEndDate"] = ISO8601DateFormatter().string(from: trialPeriodEndDate)
    } else {
      json["trialPeriodEndDate"] = NSNull()
    }

    if let languageCode = languageCode {
      json["languageCode"] = languageCode
    } else {
      json["languageCode"] = NSNull()
    }

    if let currencyCode = currencyCode {
      json["currencyCode"] = currencyCode
    } else {
      json["currencyCode"] = NSNull()
    }

    if let currencySymbol = currencySymbol {
      json["currencySymbol"] = currencySymbol
    } else {
      json["currencySymbol"] = NSNull()
    }

    if let regionCode = regionCode {
      json["regionCode"] = regionCode
    } else {
      json["regionCode"] = NSNull()
    }

    if let subscriptionPeriod = subscriptionPeriod {
      json["subscriptionPeriod"] = [
        "unit": subscriptionPeriod.unit.rawValue,
        "value": subscriptionPeriod.value,
      ]
    } else {
      json["subscriptionPeriod"] = NSNull()
    }

    if let subscriptionGroupIdentifier = subscriptionGroupIdentifier {
      json["subscriptionGroupIdentifier"] = subscriptionGroupIdentifier
    } else {
      json["subscriptionGroupIdentifier"] = NSNull()
    }

    if #available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 8.0, *) {
      json["isFamilyShareable"] = isFamilyShareable
    } else {
      json["isFamilyShareable"] = false
    }

    return json
  }
}

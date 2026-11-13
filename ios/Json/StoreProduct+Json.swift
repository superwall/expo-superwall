import SuperwallKit

extension StoreProduct {
  func toJson() -> [String: Any?] {
    var json: [String: Any?] = [
      "productIdentifier": productIdentifier,
      "subscriptionGroupIdentifier": subscriptionGroupIdentifier,
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
      "trialPeriodEndDateString": trialPeriodEndDateString,
      "localizedTrialPeriodPrice": localizedTrialPeriodPrice,
      "trialPeriodPrice": trialPeriodPrice,
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
      "languageCode": languageCode,
      "currencyCode": currencyCode,
      "currencySymbol": currencySymbol,
      "regionCode": regionCode,
      "price": price,
    ]

    if #available(iOS 14.0, macOS 11.0, tvOS 14.0, watchOS 8.0, *) {
      json["isFamilyShareable"] = isFamilyShareable
    }

    // The JWS string, as typed in JS; the struct itself can't cross the bridge.
    json["introOfferToken"] = introOfferToken?.token

    if let trialPeriodEndDate = trialPeriodEndDate {
      let dateFormatter = ISO8601DateFormatter()
      json["trialPeriodEndDate"] = dateFormatter.string(from: trialPeriodEndDate)
    } else {
      json["trialPeriodEndDate"] = nil
    }

    // `.some` keeps the key with a null value for a product with no period, as Android
    // sends and the JS type says. Assigning the optional directly would drop the key.
    json["subscriptionPeriod"] = .some(subscriptionPeriod?.toJson())
    json["introductoryDiscount"] = introductoryDiscount?.toJson()
    json["discounts"] = discounts.map { $0.toJson() }
    json["billingPlanType"] = billingPlanType?.toJson()
    json["isBillingPlanAvailable"] = isBillingPlanAvailable
    json["attributes"] = attributes
    #if SUPERWALL_SPM
    // Only SuperwallKit 4.19 and later has it; CocoaPods builds use 4.18.3.
    if let customTransactionId = customTransactionId {
      json["customTransactionId"] = customTransactionId
    }
    #endif
    json["entitlements"] = entitlements.map { $0.toJson() }

    return json
  }
}

extension SubscriptionPeriod {
  // Same shape as the Android bridge.
  func toJson() -> [String: Any] {
    return [
      "value": value,
      "unit": unit.toJson(),
    ]
  }
}

extension SubscriptionPeriod.Unit {
  func toJson() -> String {
    switch self {
    case .day:
      return "day"
    case .week:
      return "week"
    case .month:
      return "month"
    case .year:
      return "year"
    @unknown default:
      return "day"
    }
  }
}

extension StoreProductDiscount {
  func toJson() -> [String: Any?] {
    return [
      "offerIdentifier": offerIdentifier,
      "currencyCode": currencyCode,
      "price": NSDecimalNumber(decimal: price).doubleValue,
      "localizedPriceString": localizedPriceString,
      "paymentMode": paymentMode.toJson(),
      "subscriptionPeriod": subscriptionPeriod.toJson(),
      "numberOfPeriods": numberOfPeriods,
      "type": type.toJson(),
    ]
  }
}

extension StoreProductDiscount.PaymentMode {
  func toJson() -> String {
    switch self {
    case .payAsYouGo:
      return "payAsYouGo"
    case .payUpFront:
      return "payUpFront"
    case .freeTrial:
      return "freeTrial"
    @unknown default:
      return "payAsYouGo"
    }
  }
}

extension StoreProductDiscount.DiscountType {
  func toJson() -> String {
    switch self {
    case .introductory:
      return "introductory"
    case .promotional:
      return "promotional"
    @unknown default:
      return "introductory"
    }
  }
}

import SuperwallKit

extension RedemptionResult.RedemptionInfo.PaywallInfo.PaywallProduct {
  func toJson() -> [String: Any] {
    return [
      "identifier": identifier,
      "languageCode": languageCode,
      "locale": locale,
      "currencyCode": currencyCode,
      "currencySymbol": currencySymbol,
      "period": period,
      "periodly": periodly,
      "localizedPeriod": localizedPeriod,
      "periodAlt": periodAlt,
      "periodDays": periodDays,
      "periodWeeks": periodWeeks,
      "periodMonths": periodMonths,
      "periodYears": periodYears,
      "rawPrice": rawPrice,
      "price": price,
      "dailyPrice": dailyPrice,
      "weeklyPrice": weeklyPrice,
      "monthlyPrice": monthlyPrice,
      "yearlyPrice": yearlyPrice,
      "rawTrialPeriodPrice": rawTrialPeriodPrice,
      "trialPeriodPrice": trialPeriodPrice,
      "trialPeriodDailyPrice": trialPeriodDailyPrice,
      "trialPeriodWeeklyPrice": trialPeriodWeeklyPrice,
      "trialPeriodMonthlyPrice": trialPeriodMonthlyPrice,
      "trialPeriodYearlyPrice": trialPeriodYearlyPrice,
      "trialPeriodDays": trialPeriodDays,
      "trialPeriodWeeks": trialPeriodWeeks,
      "trialPeriodMonths": trialPeriodMonths,
      "trialPeriodYears": trialPeriodYears,
      "trialPeriodText": trialPeriodText,
      "trialPeriodEndDate": trialPeriodEndDate
    ]
  }
}

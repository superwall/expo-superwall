import SuperwallKit

extension TransactionProduct {
  func toJson() -> [String: Any] {
    var json: [String: Any] = [
      "id": id,
      "locale": locale
    ]
    
    // Price information
    json["price"] = [
      "raw": price.raw,
      "localized": price.localized,
      "daily": price.daily,
      "weekly": price.weekly,
      "monthly": price.monthly,
      "yearly": price.yearly
    ]
    
    // Currency information
    json["currency"] = [
      "code": currency.code,
      "symbol": currency.symbol
    ]
    
    // Language code
    if let languageCode = languageCode {
      json["languageCode"] = languageCode
    }
    
    // Trial period
    if let trialPeriod = trialPeriod {
      var trialJson: [String: Any] = [
        "days": trialPeriod.days,
        "weeks": trialPeriod.weeks,
        "months": trialPeriod.months,
        "years": trialPeriod.years,
        "text": trialPeriod.text
      ]
      if let endAt = trialPeriod.endAt {
        trialJson["endAt"] = endAt.timeIntervalSince1970 * 1000 // Convert to milliseconds
      }
      json["trialPeriod"] = trialJson
    }
    
    // Subscription period
    if let period = period {
      json["period"] = [
        "alt": period.alt,
        "ly": period.ly,
        "unit": period.unit.rawValue,
        "days": period.days,
        "weeks": period.weeks,
        "months": period.months,
        "years": period.years
      ]
    }
    
    return json
  }
}

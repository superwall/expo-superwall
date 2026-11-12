//
//  PaywallInfo+Json.swift
//  Superwall
//
//  Created by Yusuf Tör on 21/02/2024.
//

import SuperwallKit

extension PaywallInfo {
  func toJson() -> [String: Any] {
    var map: [String: Any] = [:]

    map["identifier"] = self.identifier
    map["name"] = self.name
    map["url"] = self.url.absoluteString
    if let presentationId = self.presentationId {
      map["presentationId"] = presentationId
    }
    if let experiment = self.experiment {
      map["experiment"] = experiment.toJson()
    }

    map["products"] = products.map { $0.toJson() }

    let productIdsArray = self.productIds
    map["productIds"] = productIdsArray

    if let presentedByPlacementWithName = self.presentedByPlacementWithName {
      map["presentedByEventWithName"] = presentedByPlacementWithName
    }
    if let presentedByPlacementWithId = self.presentedByPlacementWithId {
      map["presentedByEventWithId"] = presentedByPlacementWithId
    }
    if let presentedByPlacementAt = self.presentedByPlacementAt {
      map["presentedByEventAt"] = presentedByPlacementAt
    }
    map["presentedBy"] = self.presentedBy
    if let presentationSourceType = self.presentationSourceType {
      map["presentationSourceType"] = presentationSourceType
    }
    if let responseLoadStartTime = self.responseLoadStartTime {
      map["responseLoadStartTime"] = responseLoadStartTime
    }
    if let responseLoadCompleteTime = self.responseLoadCompleteTime {
      map["responseLoadCompleteTime"] = responseLoadCompleteTime
    }
    if let responseLoadFailTime = self.responseLoadFailTime {
      map["responseLoadFailTime"] = responseLoadFailTime
    }
    if let responseLoadDuration = self.responseLoadDuration {
      map["responseLoadDuration"] = responseLoadDuration
    }

    map["isFreeTrialAvailable"] = self.isFreeTrialAvailable
    map["featureGatingBehavior"] = self.featureGatingBehavior.toJson()
    map["closeReason"] = self.closeReason.toJson()

    if let webViewLoadStartTime = self.webViewLoadStartTime {
      map["webViewLoadStartTime"] = webViewLoadStartTime
    }
    if let webViewLoadCompleteTime = self.webViewLoadCompleteTime {
      map["webViewLoadCompleteTime"] = webViewLoadCompleteTime
    }
    if let webViewLoadFailTime = self.webViewLoadFailTime {
      map["webViewLoadFailTime"] = webViewLoadFailTime
    }
    if let webViewLoadDuration = self.webViewLoadDuration {
      map["webViewLoadDuration"] = webViewLoadDuration
    }

    if let productsLoadStartTime = self.productsLoadStartTime {
      map["productsLoadStartTime"] = productsLoadStartTime
    }
    if let productsLoadCompleteTime = self.productsLoadCompleteTime {
      map["productsLoadCompleteTime"] = productsLoadCompleteTime
    }
    if let productsLoadFailTime = self.productsLoadFailTime {
      map["productsLoadFailTime"] = productsLoadFailTime
    }
    if let productsLoadDuration = self.productsLoadDuration {
      map["productsLoadDuration"] = productsLoadDuration
    }

    if let paywalljsVersion = self.paywalljsVersion {
      map["paywalljsVersion"] = paywalljsVersion
    }

    // Sends the type's name (e.g. `minutesSince`), as Android does, rather than its raw value.
    map["computedPropertyRequests"] = self.computedPropertyRequests.map { $0.toJson() }

    let surveysArray: [[String: Any]] = self.surveys.map { survey in
      var surveyMap: [String: Any] = [
        "id": survey.id,
        "message": survey.message,
        "title": survey.title,
        "assignmentKey": survey.assignmentKey,
        "includeCloseOption": survey.includeCloseOption,
        "includeOtherOption": survey.includeOtherOption,
        "presentationProbability": survey.presentationProbability,
        "presentationCondition": survey.presentationCondition.toJson(),
      ]

      let optionsArray: [[String: Any]] = survey.options.map { option in
        [
          "id": option.id,
          "title": option.title,
        ]
      }
      surveyMap["options"] = optionsArray

      return surveyMap
    }
    map["surveys"] = surveysArray

    map["localNotifications"] = self.localNotifications.map { $0.toJson() }

    map["state"] = self.state

    map["cacheKey"] = self.cacheKey
    map["buildId"] = self.buildId
    if let shimmerLoadStartTime = self.shimmerLoadStartTime {
      map["shimmerLoadStartTime"] = shimmerLoadStartTime
    }
    if let shimmerLoadCompleteTime = self.shimmerLoadCompleteTime {
      map["shimmerLoadCompleteTime"] = shimmerLoadCompleteTime
    }
    map["presentation"] = self.presentation.toJson()
    map["isScrollEnabled"] = self.isScrollEnabled
    map["isLocal"] = self.isLocal
    map["introOfferEligibility"] = self.introOfferEligibility.toJson()

    return map
  }
}

extension PaywallPresentationInfo {
  // The dashboard's style names, which Android uses too.
  func toJson() -> [String: Any] {
    var map: [String: Any] = ["delay": delay]
    switch style {
    case .modal:
      map["style"] = "MODAL"
    case .fullscreen:
      map["style"] = "FULLSCREEN"
    case .fullscreenNoAnimation:
      map["style"] = "NO_ANIMATION"
    case .push:
      map["style"] = "PUSH"
    case let .popup(height, width, cornerRadius):
      map["style"] = "POPUP"
      map["height"] = height
      map["width"] = width
      map["cornerRadius"] = cornerRadius
    case let .drawer(height, cornerRadius):
      map["style"] = "DRAWER"
      map["height"] = height
      map["cornerRadius"] = cornerRadius
    case .none:
      map["style"] = "NONE"
    }
    return map
  }
}

extension IntroOfferEligibility {
  func toJson() -> String {
    switch self {
    case .eligible:
      return "eligible"
    case .ineligible:
      return "ineligible"
    case .automatic:
      return "automatic"
    @unknown default:
      return "automatic"
    }
  }
}

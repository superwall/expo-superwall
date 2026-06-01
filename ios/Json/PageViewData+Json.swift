import SuperwallKit

extension PageViewData {
  func toJson() -> [String: Any] {
    var map: [String: Any] = [
      "pageNodeId": self.pageNodeId,
      "flowPosition": self.flowPosition,
      "pageName": self.pageName,
      "navigationNodeId": self.navigationNodeId,
      "navigationType": self.navigationType
    ]

    if let previousPageNodeId = self.previousPageNodeId {
      map["previousPageNodeId"] = previousPageNodeId
    }
    if let previousFlowPosition = self.previousFlowPosition {
      map["previousFlowPosition"] = previousFlowPosition
    }
    if let timeOnPreviousPageMs = self.timeOnPreviousPageMs {
      map["timeOnPreviousPageMs"] = timeOnPreviousPageMs
    }

    return map
  }
}

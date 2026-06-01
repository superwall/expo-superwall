package expo.modules.superwallexpo.json

import com.superwall.sdk.paywall.view.webview.messaging.PageViewData

fun PageViewData.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>(
    "pageNodeId" to pageNodeId,
    "flowPosition" to flowPosition,
    "pageName" to pageName,
    "navigationNodeId" to navigationNodeId,
    "navigationType" to navigationType,
  )
  previousPageNodeId?.let { map["previousPageNodeId"] = it }
  previousFlowPosition?.let { map["previousFlowPosition"] = it }
  timeOnPreviousPageMs?.let { map["timeOnPreviousPageMs"] = it }
  return map
}

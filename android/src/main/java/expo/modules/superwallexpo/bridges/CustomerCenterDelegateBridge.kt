package expo.modules.superwallexpo.bridges

import com.superwall.sdk.customercenter.CustomerCenterAction
import com.superwall.sdk.customercenter.CustomerCenterDelegate
import com.superwall.sdk.customercenter.CustomerCenterPurchase
import com.superwall.sdk.customercenter.CustomerCenterRefundStatus
import expo.modules.superwallexpo.SuperwallExpoModule
import expo.modules.superwallexpo.json.toJson
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Forwards Customer Center delegate callbacks for one presentation to JS, tagged with the
// presentation's `handlerId`. Dismissal is reported through the `presentCustomerCenter` promise.
class CustomerCenterDelegateBridge(
  private val handlerId: String,
  // Only ask JS before restoring when it registered a handler; otherwise restore straight away.
  private val asksBeforeRestoring: Boolean,
) : CustomerCenterDelegate {

  companion object {
    // Restores waiting on `didHandleCustomerCenterShouldRestorePurchases`, keyed by request ID.
    val pendingRestores = ConcurrentHashMap<String, (Boolean) -> Unit>()
  }

  override fun customerCenterShouldRestorePurchases(proceed: (Boolean) -> Unit) {
    if (!asksBeforeRestoring) {
      proceed(true)
      return
    }
    val requestId = UUID.randomUUID().toString()
    pendingRestores[requestId] = proceed
    sendEvent(
      "onCustomerCenterShouldRestorePurchases",
      mapOf("handlerId" to handlerId, "requestId" to requestId)
    )
  }

  override fun customerCenterDidSelectAction(
    action: CustomerCenterAction,
    pathId: String,
    purchase: CustomerCenterPurchase?,
  ) {
    sendEvent(
      "onCustomerCenterAction",
      mapOf(
        "handlerId" to handlerId,
        "action" to action.toJson(),
        "pathId" to pathId,
        "purchase" to purchase?.toJson(),
      )
    )
  }

  override fun customerCenterDidCompleteSurvey(
    surveyId: String,
    optionId: String,
    action: CustomerCenterAction,
    pathId: String,
  ) {
    sendEvent(
      "onCustomerCenterSurveyComplete",
      mapOf(
        "handlerId" to handlerId,
        "surveyId" to surveyId,
        "optionId" to optionId,
        "action" to action.toJson(),
        "pathId" to pathId,
      )
    )
  }

  override fun customerCenterDidCompleteRefundRequest(
    productId: String,
    status: CustomerCenterRefundStatus,
  ) {
    sendEvent(
      "onCustomerCenterRefundRequestComplete",
      mapOf(
        "handlerId" to handlerId,
        "productId" to productId,
        "status" to status.toJson(),
      )
    )
  }

  private fun sendEvent(name: String, body: Map<String, Any?>) {
    SuperwallExpoModule.emitEvent(name, body)
  }
}

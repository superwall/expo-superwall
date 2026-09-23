package expo.modules.superwallexpo

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class InAppPurchaseQueryException(
  val responseCode: Int,
  message: String,
) : Exception(message)

/**
 * Queries Google Play Billing for one-time (INAPP) purchases the user currently owns.
 *
 * Superwall Android does not expose owned purchases publicly, so this uses a short-lived
 * [BillingClient] that only reads purchases. It never launches a billing flow and never
 * acknowledges or consumes purchases; consumption is left to `Superwall.consume`.
 */
object InAppPurchaseQuery {
  suspend fun queryPurchased(context: Context): List<Map<String, Any?>> =
    suspendCancellableCoroutine { cont ->
      val client = BillingClient.newBuilder(context)
        // Purchases are made through Superwall; this client only reads them.
        .setListener { _, _ -> }
        .enablePendingPurchases(
          PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

      fun fail(billingResult: BillingResult, stage: String) {
        if (cont.isActive) {
          cont.resumeWith(
            Result.failure(
              InAppPurchaseQueryException(
                billingResult.responseCode,
                "Google Play Billing $stage failed: " +
                  "${billingResult.responseCode} - ${billingResult.debugMessage}"
              )
            )
          )
        }
        client.endConnection()
      }

      cont.invokeOnCancellation { client.endConnection() }

      client.startConnection(object : BillingClientStateListener {
        override fun onBillingSetupFinished(setupResult: BillingResult) {
          if (setupResult.responseCode != BillingClient.BillingResponseCode.OK) {
            fail(setupResult, "setup")
            return
          }
          val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
          client.queryPurchasesAsync(params) { queryResult, purchases ->
            if (queryResult.responseCode != BillingClient.BillingResponseCode.OK) {
              fail(queryResult, "purchase query")
            } else {
              if (cont.isActive) {
                cont.resume(purchases.toOwnedPurchasesJson())
              }
              client.endConnection()
            }
          }
        }

        override fun onBillingServiceDisconnected() {
          if (cont.isActive) {
            cont.resumeWith(
              Result.failure(
                InAppPurchaseQueryException(
                  BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                  "Google Play Billing service disconnected"
                )
              )
            )
          }
        }
      })
    }
}

private fun List<Purchase>.toOwnedPurchasesJson(): List<Map<String, Any?>> =
  filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
    .map {
      mapOf(
        "productIds" to it.products,
        "purchaseToken" to it.purchaseToken,
        "orderId" to it.orderId,
        "purchaseTime" to it.purchaseTime.toDouble(),
        "quantity" to it.quantity,
        "isAcknowledged" to it.isAcknowledged,
      )
    }

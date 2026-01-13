package com.superwall.sdk.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ConsumeParams
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger

internal data class ConsumePurchaseUseCaseParams(
    val purchaseToken: String,
    override val appInBackground: Boolean,
) : UseCaseParams

internal class ConsumePurchaseUseCase(
    private val useCaseParams: ConsumePurchaseUseCaseParams,
    private val onReceive: (String) -> Unit,
    onError: (BillingError) -> Unit,
    private val withConnectedClient: (BillingClient.() -> Unit) -> Unit,
    executeRequestOnUIThread: ExecuteRequestOnUIThreadFunction,
) : BillingClientUseCase<String>(useCaseParams, onError, executeRequestOnUIThread) {
    override fun executeAsync() {
        withConnectedClient {
            val params =
                ConsumeParams
                    .newBuilder()
                    .setPurchaseToken(useCaseParams.purchaseToken)
                    .build()

            consumeAsync(params) { billingResult, purchaseToken ->
                processResult(billingResult, purchaseToken)
            }
        }
    }

    override fun onOk(received: String) {
        Logger.debug(
            logLevel = LogLevel.debug,
            scope = LogScope.productsManager,
            message = "Successfully consumed purchase with token ${useCaseParams.purchaseToken}",
        )
        onReceive(received)
    }
}

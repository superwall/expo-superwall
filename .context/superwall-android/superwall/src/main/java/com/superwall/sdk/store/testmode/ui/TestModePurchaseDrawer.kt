package com.superwall.sdk.store.testmode.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.superwall.sdk.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class PurchaseSimulationResult {
    data object Purchased : PurchaseSimulationResult()

    data object Abandoned : PurchaseSimulationResult()

    data object Failed : PurchaseSimulationResult()
}

internal object TestModePurchaseDrawer {
    suspend fun show(
        activity: Activity,
        productIdentifier: String,
        localizedPrice: String,
        period: String,
        hasFreeTrial: Boolean,
        trialPeriodText: String,
        entitlementNames: List<String>,
    ): PurchaseSimulationResult =
        withContext(Dispatchers.Main) {
            val result = CompletableDeferred<PurchaseSimulationResult>()

            val dialog = BottomSheetDialog(activity)
            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.test_mode_purchase_drawer, null)

            view.background =
                ContextCompat.getDrawable(activity, R.drawable.test_mode_sheet_bg)

            // Populate product card
            view.findViewById<TextView>(R.id.product_id_text).text = productIdentifier

            if (hasFreeTrial && trialPeriodText.isNotEmpty()) {
                view.findViewById<TextView>(R.id.trial_text).apply {
                    text = "$trialPeriodText free trial"
                    visibility = View.VISIBLE
                }
            }

            view.findViewById<TextView>(R.id.price_text).text =
                if (period.isNotEmpty()) "$localizedPrice / $period" else localizedPrice

            if (entitlementNames.isNotEmpty()) {
                view.findViewById<TextView>(R.id.entitlements_text).apply {
                    text = "Entitlements: ${entitlementNames.joinToString(", ")}"
                    visibility = View.VISIBLE
                }
            }

            // Confirm button text depends on trial
            view.findViewById<TextView>(R.id.confirm_button).apply {
                text = if (hasFreeTrial) "Start Free Trial" else "Confirm Purchase"
                setOnClickListener {
                    result.complete(PurchaseSimulationResult.Purchased)
                    dialog.dismiss()
                }
            }

            // Close button
            view.findViewById<TextView>(R.id.close_button).setOnClickListener {
                result.complete(PurchaseSimulationResult.Abandoned)
                dialog.dismiss()
            }

            // Fail button
            view.findViewById<TextView>(R.id.fail_button).setOnClickListener {
                result.complete(PurchaseSimulationResult.Failed)
                dialog.dismiss()
            }

            dialog.setContentView(view)
            dialog.setOnDismissListener {
                if (!result.isCompleted) {
                    result.complete(PurchaseSimulationResult.Abandoned)
                }
            }

            dialog.show()

            result.await()
        }
}

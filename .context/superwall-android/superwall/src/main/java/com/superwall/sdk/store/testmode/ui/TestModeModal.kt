package com.superwall.sdk.store.testmode.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.superwall.sdk.R
import com.superwall.sdk.Superwall
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.models.serialization.AnyMapSerializer
import com.superwall.sdk.storage.TestModeSettings
import com.superwall.sdk.store.testmode.FreeTrialOverride
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal data class TestModeModalResult(
    val entitlements: List<EntitlementSelection>,
    val freeTrialOverride: FreeTrialOverride,
)

internal object TestModeModal {
    suspend fun show(
        activity: Activity,
        reason: String,
        hasPurchaseController: Boolean,
        availableEntitlements: List<String>,
        apiKey: String = "",
        dashboardBaseUrl: String = "",
        savedSettings: TestModeSettings? = null,
    ): TestModeModalResult {
        // Fetch these OFF the main thread — their getters use runBlocking internally,
        // which would cause an ANR if called inside withContext(Dispatchers.Main).
        val userId = Superwall.instance.userId
        val userAttributes = Json.encodeToString(AnyMapSerializer, Superwall.instance.userAttributes)

        return withContext(Dispatchers.Main) {
            val result = CompletableDeferred<TestModeModalResult>()
            val ioScope = IOScope()

            val dialog = BottomSheetDialog(activity)
            dialog.setCancelable(false)

            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.test_mode_modal, null)

            // Apply background
            view.background = ContextCompat.getDrawable(activity, R.drawable.test_mode_sheet_bg)

            val savedSelectionsByName =
                savedSettings?.entitlementSelections?.associateBy { it.identifier } ?: emptyMap()

            var selectedOverride = savedSettings?.freeTrialOverride ?: FreeTrialOverride.UseDefault

            // Populate static fields
            view.findViewById<TextView>(R.id.reason_text).text = reason
            view.findViewById<TextView>(R.id.user_id_value).text = userId ?: "(anonymous)"
            view.findViewById<TextView>(R.id.purchase_controller_value).text =
                if (hasPurchaseController) "Provided" else "Not provided"

            // Copy User ID button
            view.findViewById<TextView>(R.id.copy_user_id_button).setOnClickListener {
                copyToClipboard(activity, "User ID", userId ?: "")
            }

            // View on Dashboard button
            view.findViewById<TextView>(R.id.view_dashboard_button).setOnClickListener {
                if (dashboardBaseUrl.isNotEmpty() && apiKey.isNotEmpty() && !userId.isNullOrEmpty()) {
                    val url = "$dashboardBaseUrl/sdk-link/applications/$apiKey/users/$userId"
                    try {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: Exception) {
                        Toast
                            .makeText(activity, "Could not open browser", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

            // Device Attributes row
            ioScope.launch {
                val deviceAttributes =
                    Json.encodeToString(AnyMapSerializer, Superwall.instance.deviceAttributes())
                withContext(Dispatchers.Main) {
                    view.findViewById<View>(R.id.device_attributes_row).setOnClickListener {
                        TestModeAttributesViewer.show(activity, "Device Attributes", deviceAttributes)
                    }
                }
            }

            // User Attributes row
            view.findViewById<View>(R.id.user_attributes_row).setOnClickListener {
                TestModeAttributesViewer.show(activity, "User Attributes", userAttributes)
            }

            // Free trial override spinner
            val freeTrialSpinner = view.findViewById<Spinner>(R.id.free_trial_spinner)
            val overrideOptions = FreeTrialOverride.entries.toList()
            val overrideAdapter =
                ArrayAdapter(
                    activity,
                    R.layout.test_mode_spinner_item,
                    overrideOptions.map { it.displayName },
                )
            overrideAdapter.setDropDownViewResource(R.layout.test_mode_spinner_dropdown_item)
            freeTrialSpinner.adapter = overrideAdapter
            freeTrialSpinner.setSelection(
                overrideOptions.indexOf(selectedOverride).coerceAtLeast(0),
            )
            freeTrialSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        itemView: View?,
                        position: Int,
                        id: Long,
                    ) {
                        selectedOverride = overrideOptions[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

            // Entitlements section
            val rowHolders = mutableListOf<EntitlementRowViewHolder>()
            if (availableEntitlements.isNotEmpty()) {
                val section = view.findViewById<LinearLayout>(R.id.entitlements_section)
                section.visibility = View.VISIBLE
                val entitlementsContainer =
                    view.findViewById<LinearLayout>(R.id.entitlements_container)

                for (entitlement in availableEntitlements) {
                    val initialSelection =
                        savedSelectionsByName[entitlement]
                            ?: EntitlementSelection(identifier = entitlement)
                    val holder =
                        EntitlementRowViewHolder(
                            context = activity,
                            parent = entitlementsContainer,
                            entitlementName = entitlement,
                            initialSelection = initialSelection,
                            onSelectionChanged = {},
                        )
                    rowHolders.add(holder)
                    entitlementsContainer.addView(holder.view)
                }
            }

            // Continue button
            view.findViewById<TextView>(R.id.continue_button).setOnClickListener {
                val selections = rowHolders.map { it.currentSelection }
                result.complete(
                    TestModeModalResult(
                        entitlements = selections,
                        freeTrialOverride = selectedOverride,
                    ),
                )
                dialog.dismiss()
            }

            dialog.setContentView(view)
            dialog.setOnDismissListener {
                ioScope.cancel()
                if (!result.isCompleted) {
                    result.complete(
                        TestModeModalResult(
                            entitlements = emptyList(),
                            freeTrialOverride = FreeTrialOverride.UseDefault,
                        ),
                    )
                }
            }

            dialog.show()

            result.await()
        }
    }

    private fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
    ) {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }
}

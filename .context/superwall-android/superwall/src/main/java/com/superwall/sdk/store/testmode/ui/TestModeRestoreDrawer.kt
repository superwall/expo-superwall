package com.superwall.sdk.store.testmode.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.superwall.sdk.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal sealed class RestoreSimulationResult {
    data class Restored(
        val selectedEntitlements: List<EntitlementSelection>,
    ) : RestoreSimulationResult()

    data object Cancelled : RestoreSimulationResult()
}

internal object TestModeRestoreDrawer {
    suspend fun show(
        activity: Activity,
        availableEntitlements: List<String>,
        currentSelections: List<EntitlementSelection>,
    ): RestoreSimulationResult =
        withContext(Dispatchers.Main) {
            val result = CompletableDeferred<RestoreSimulationResult>()

            val dialog = BottomSheetDialog(activity)
            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.test_mode_restore_drawer, null)

            view.background =
                ContextCompat.getDrawable(activity, R.drawable.test_mode_sheet_bg)

            val currentByName = currentSelections.associateBy { it.identifier }

            // Close button
            view.findViewById<TextView>(R.id.close_button).setOnClickListener {
                result.complete(RestoreSimulationResult.Cancelled)
                dialog.dismiss()
            }

            // Dynamic entitlement rows
            val entitlementsContainer = view.findViewById<LinearLayout>(R.id.entitlements_container)
            val rowHolders = mutableListOf<EntitlementRowViewHolder>()
            if (availableEntitlements.isNotEmpty()) {
                for (entitlement in availableEntitlements) {
                    val initialSelection =
                        currentByName[entitlement]
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
            } else {
                view.findViewById<TextView>(R.id.no_entitlements_text).visibility = View.VISIBLE
            }

            // Restore button
            view.findViewById<TextView>(R.id.restore_button).setOnClickListener {
                val selections = rowHolders.map { it.currentSelection }
                result.complete(RestoreSimulationResult.Restored(selections))
                dialog.dismiss()
            }

            dialog.setContentView(view)
            dialog.setOnDismissListener {
                if (!result.isCompleted) {
                    result.complete(RestoreSimulationResult.Cancelled)
                }
            }

            dialog.show()

            result.await()
        }
}

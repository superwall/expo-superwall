package com.superwall.sdk.store.testmode.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.superwall.sdk.R

internal class EntitlementRowViewHolder(
    private val context: Context,
    parent: ViewGroup,
    entitlementName: String,
    initialSelection: EntitlementSelection,
    private val onSelectionChanged: (EntitlementSelection) -> Unit,
) {
    val view: View = LayoutInflater.from(context).inflate(R.layout.test_mode_entitlement_row, parent, false)

    private val nameText: TextView = view.findViewById(R.id.entitlement_name)
    private val stateSpinner: Spinner = view.findViewById(R.id.state_spinner)
    private val offerRow: View = view.findViewById(R.id.offer_type_row)
    private val offerSpinner: Spinner = view.findViewById(R.id.offer_spinner)

    private var currentState: EntitlementStateOption = initialSelection.state
    private var currentOfferType: OfferTypeOption = initialSelection.offerType

    val currentSelection: EntitlementSelection
        get() =
            EntitlementSelection(
                identifier = nameText.text.toString(),
                state = currentState,
                offerType = if (currentState.isActive) currentOfferType else OfferTypeOption.None,
            )

    init {
        nameText.text = entitlementName

        val stateOptions = EntitlementStateOption.entries.toList()
        val stateAdapter =
            ArrayAdapter(
                context,
                R.layout.test_mode_spinner_item,
                stateOptions.map { it.displayName },
            )
        stateAdapter.setDropDownViewResource(R.layout.test_mode_spinner_dropdown_item)
        stateSpinner.adapter = stateAdapter
        stateSpinner.setSelection(stateOptions.indexOf(initialSelection.state).coerceAtLeast(0))

        val offerOptions = OfferTypeOption.entries.toList()
        val offerAdapter =
            ArrayAdapter(
                context,
                R.layout.test_mode_spinner_item,
                offerOptions.map { it.displayName },
            )
        offerAdapter.setDropDownViewResource(R.layout.test_mode_spinner_dropdown_item)
        offerSpinner.adapter = offerAdapter
        offerSpinner.setSelection(offerOptions.indexOf(initialSelection.offerType).coerceAtLeast(0))

        updateAppearance()

        stateSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    currentState = stateOptions[position]
                    updateAppearance()
                    notifyChanged()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        offerSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    currentOfferType = offerOptions[position]
                    notifyChanged()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        view.layoutParams =
            LinearLayout
                .LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dpToPx(8)
                }
    }

    private fun updateAppearance() {
        offerRow.visibility = if (currentState.isActive) View.VISIBLE else View.GONE
        val bgRes =
            if (currentState.isActive) {
                R.drawable.test_mode_card_active_bg
            } else {
                R.drawable.test_mode_card_bg
            }
        view.background = ContextCompat.getDrawable(context, bgRes)
    }

    private fun notifyChanged() {
        onSelectionChanged(currentSelection)
    }

    fun resetToDefaults() {
        currentState = EntitlementStateOption.Inactive
        currentOfferType = OfferTypeOption.None
        stateSpinner.setSelection(0)
        offerSpinner.setSelection(0)
        updateAppearance()
    }

    private fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()
}

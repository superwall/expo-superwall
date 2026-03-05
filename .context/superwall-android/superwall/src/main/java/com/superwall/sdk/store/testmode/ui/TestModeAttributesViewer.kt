package com.superwall.sdk.store.testmode.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.superwall.sdk.R
import org.json.JSONObject

internal object TestModeAttributesViewer {
    fun show(
        context: Context,
        title: String,
        attributesJson: String,
    ) {
        val dialog = Dialog(context, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.test_mode_attributes_viewer, null)

        view.findViewById<TextView>(R.id.viewer_title).text = title
        view.findViewById<View>(R.id.back_button).setOnClickListener {
            dialog.dismiss()
        }

        val container = view.findViewById<LinearLayout>(R.id.attributes_container)
        val attributes = parseAttributes(attributesJson)

        for ((key, value) in attributes) {
            val row = inflater.inflate(R.layout.test_mode_attribute_row, container, false)
            row.findViewById<TextView>(R.id.attribute_key).text = key
            row.findViewById<TextView>(R.id.attribute_value).text = value
            row.setOnClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(key, value))
                Toast.makeText(context, "$key copied", Toast.LENGTH_SHORT).show()
            }
            container.addView(row)
        }

        if (attributes.isEmpty()) {
            val empty = TextView(context)
            empty.text = "No attributes available."
            empty.textSize = 14f
            empty.setTextColor(0xFF999999.toInt())
            container.addView(empty)
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun parseAttributes(json: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            val obj = JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result.add(key to obj.get(key).toString())
            }
        } catch (_: Exception) {
        }
        return result.sortedBy { it.first }
    }
}

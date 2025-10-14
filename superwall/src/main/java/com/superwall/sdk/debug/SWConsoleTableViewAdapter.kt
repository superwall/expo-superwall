package com.superwall.sdk.debug

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.superwall.sdk.R

class TableViewAdapter(
    private var data: MutableList<Map.Entry<String, String>>,
    private var pickerRow: Int,
) : RecyclerView.Adapter<TableViewAdapter.ViewHolder>() {
    fun updateData(
        newData: Map<String, String>,
        pickerRow: Int,
    ) {
        this.pickerRow = pickerRow
        data.clear()

        newData.forEach { attribute ->
            data.add(attribute)
        }

        data.sortWith(compareBy { it.key })

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.custom_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val productLevels = listOf("primary", "secondary", "tertiary")
        var selectedProduct: String? = null

        if (pickerRow < productLevels.size) {
            selectedProduct = productLevels[pickerRow]
        }

        val (key, value) = data[position]

        holder.text1.text = value
        val text: String = selectedProduct?.let { "$it.$key" } ?: key
        holder.text2.text = "{{ $text }}"
    }

    override fun getItemCount() = data.size

    class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        var text1: TextView = view.findViewById(R.id.text1)
        var text2: TextView = view.findViewById(R.id.text2)
    }
}

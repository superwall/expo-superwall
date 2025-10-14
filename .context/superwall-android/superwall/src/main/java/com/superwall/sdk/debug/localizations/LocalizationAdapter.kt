package com.superwall.sdk.debug.localizations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superwall.sdk.R

class LocalizationAdapter(
    private var data: List<LocalizationGrouping>,
    private val onLocaleSelected: (String) -> Unit,
) : RecyclerView.Adapter<LocalizationAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_localization, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val grouping = data[position]
        holder.bind(grouping, onLocaleSelected)
    }

    override fun getItemCount() = data.size

    fun updateData(newData: List<LocalizationGrouping>) {
        data = newData
        notifyDataSetChanged()
    }

    class ViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)
        private val localeRecyclerView: RecyclerView = itemView.findViewById(R.id.locale_recycler_view)

        init {
            localeRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
        }

        fun bind(
            grouping: LocalizationGrouping,
            onLocaleSelected: (String) -> Unit,
        ) {
            titleTextView.text = grouping.title
            localeRecyclerView.adapter = LocaleAdapter(grouping.localizations, onLocaleSelected)
        }
    }
}

class LocaleAdapter(
    private val localizations: List<LocalizationOption>,
    private val onLocaleSelected: (String) -> Unit,
) : RecyclerView.Adapter<LocaleAdapter.LocaleViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): LocaleViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_locale, parent, false)
        return LocaleViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: LocaleViewHolder,
        position: Int,
    ) {
        val locale = localizations[position]
        holder.bind(locale, onLocaleSelected)
    }

    override fun getItemCount() = localizations.size

    class LocaleViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val languageTextView: TextView = itemView.findViewById(R.id.language_text_view)
        private val countryTextView: TextView = itemView.findViewById(R.id.country_text_view)

        fun bind(
            localizationOption: LocalizationOption,
            onLocaleSelected: (String) -> Unit,
        ) {
            languageTextView.text = localizationOption.description
            countryTextView.text = localizationOption.locale
            itemView.setOnClickListener { onLocaleSelected(localizationOption.locale) }
        }
    }
}

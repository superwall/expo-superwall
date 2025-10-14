package com.superwall.sdk.debug.localizations

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superwall.sdk.R

class SWLocalizationActivity :
    AppCompatActivity(),
    SearchView.OnQueryTextListener {
    companion object {
        var completion: ((String) -> Unit)? = null
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LocalizationAdapter
    private lateinit var localizationManager: LocalizationManager
    private val allRowModels by lazy { localizationManager.localizationGroupings }
    private var rowModels: List<LocalizationGrouping> = listOf()

    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_localization)

        localizationManager = LocalizationManager()
        rowModels = allRowModels

        recyclerView = findViewById(R.id.recycler_view)
        setupRecyclerView()

        setupSearchView()
        setupActionBar()
    }

    private fun setupRecyclerView() {
        adapter =
            LocalizationAdapter(rowModels) { locale ->
                finish()
                completion?.invoke(locale)
            }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        searchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(this)
    }

    private fun setupActionBar() {
        supportActionBar?.title = "Localization"
        // Customize your action bar further if needed
    }

    private fun completion(locale: String) {
        // Return the result to the caller
        val data =
            Intent().apply {
                putExtra("locale", locale)
            }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        query?.let {
            rowModels = localizationManager.localizationGroupings(it)
            adapter.updateData(rowModels)
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let {
            rowModels = localizationManager.localizationGroupings(it)
            adapter.updateData(rowModels)
        }
        return true
    }
}

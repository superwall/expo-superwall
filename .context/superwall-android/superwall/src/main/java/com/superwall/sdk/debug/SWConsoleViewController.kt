package com.superwall.sdk.debug

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.superwall.sdk.R
import com.superwall.sdk.store.abstractions.product.StoreProduct

class SWConsoleActivity : AppCompatActivity() {
    private lateinit var productPicker: Spinner
    private lateinit var tableView: RecyclerView
    private lateinit var productAdapter: ArrayAdapter<String>
    private lateinit var tableViewAdapter: TableViewAdapter

    companion object {
        var products: List<StoreProduct> = listOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_console)
        supportActionBar?.title = "Template Variables"
        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        productPicker = findViewById(R.id.productPicker)
        tableView = findViewById(R.id.console_recycler_view) // Replace with your actual RecyclerView ID

        // Set up the Spinner with the product names or identifiers
        productAdapter =
            ArrayAdapter(
                this,
                R.layout.spinner_item,
                products.map { it.productIdentifier },
            )
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        productPicker.adapter = productAdapter
        productPicker.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long,
                ) {
                    // When a product is selected, update the RecyclerView with its attributes
                    tableViewAdapter.updateData(products[position].attributes, position)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // This can be left empty
                }
            }

        // Set up the RecyclerView to display the attributes of the selected product
        setupTableView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_console, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_done -> {
                finish() // Close the activity
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun setupTableView() {
        tableView.layoutManager = LinearLayoutManager(this)
        tableViewAdapter = TableViewAdapter(mutableListOf(), 0)
        tableView.adapter = tableViewAdapter
    }
}

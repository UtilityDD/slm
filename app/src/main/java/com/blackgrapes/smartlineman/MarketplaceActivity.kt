package com.blackgrapes.smartlineman

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.json.JSONObject

class MarketplaceActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MarketplaceAdapter
    private lateinit var chipGroup: ChipGroup
    private lateinit var emptyState: LinearLayout
    
    private var allItems = mutableListOf<MarketplaceItem>()
    private val categories = mutableMapOf<String, String>() // id to name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_marketplace)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_marketplace)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupViews()
        loadMarketplaceData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.equipment_recycler_view)
        chipGroup = findViewById(R.id.category_chip_group)
        emptyState = findViewById(R.id.empty_state)
        
        adapter = MarketplaceAdapter(emptyList()) { item ->
            openItemDetail(item)
        }
        recyclerView.adapter = adapter
    }

    private fun loadMarketplaceData() {
        try {
            val jsonString = com.blackgrapes.smartlineman.util.JsonHelper.loadJSON(this, "marketplace_items.json")
            if (jsonString == null) {
                showEmptyState()
                return
            }
            val jsonObject = JSONObject(jsonString)
            val categoriesArray = jsonObject.getJSONArray("categories")

            for (i in 0 until categoriesArray.length()) {
                val categoryObj = categoriesArray.getJSONObject(i)
                val categoryId = categoryObj.getString("id")
                val categoryName = categoryObj.getString("name")
                categories[categoryId] = categoryName

                val itemsArray = categoryObj.getJSONArray("items")
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    val item = MarketplaceItem(
                        id = itemObj.getString("id"),
                        name = itemObj.getString("name"),
                        description = itemObj.getString("description"),
                        priceRange = itemObj.getString("price_range"),
                        imageName = if (itemObj.isNull("image_name")) null else itemObj.getString("image_name"),
                        category = itemObj.getString("category"),
                        safetyStandards = if (itemObj.isNull("safety_standards")) null else itemObj.getString("safety_standards"),
                        supplierContact = if (itemObj.isNull("supplier_contact")) null else itemObj.getString("supplier_contact")
                    )
                    allItems.add(item)
                }
            }

            setupCategoryChips()
            updateItemsList(allItems)

        } catch (e: Exception) {
            e.printStackTrace()
            showEmptyState()
        }
    }

    private fun setupCategoryChips() {
        // Add category chips dynamically
        categories.forEach { (id, name) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                
                // Set color selectors for checked/unchecked states
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(getColorStateList(R.color.chip_text_selector))
                
                // Set checked icon tint
                checkedIconTint = getColorStateList(R.color.white)
                
                tag = id
            }
            chipGroup.addView(chip)
        }

        // Set up chip selection listener
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                // No chip selected, show all items
                updateItemsList(allItems)
            } else {
                val selectedChip = group.findViewById<Chip>(checkedIds.first())
                if (selectedChip.id == R.id.chip_all) {
                    updateItemsList(allItems)
                } else {
                    val categoryId = selectedChip.tag as? String
                    val categoryName = categories[categoryId]
                    val filteredItems = allItems.filter { it.category == categoryName }
                    updateItemsList(filteredItems)
                }
            }
        }
    }

    private fun updateItemsList(items: List<MarketplaceItem>) {
        if (items.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            adapter.updateItems(items)
        }
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun openItemDetail(item: MarketplaceItem) {
        val intent = Intent(this, MarketplaceDetailActivity::class.java).apply {
            putExtra("item_id", item.id)
            putExtra("item_name", item.name)
            putExtra("item_description", item.description)
            putExtra("item_price", item.priceRange)
            putExtra("item_category", item.category)
            putExtra("item_standards", item.safetyStandards)
            putExtra("item_contact", item.supplierContact)
        }
        startActivity(intent)
    }
}

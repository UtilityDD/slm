package com.blackgrapes.smartlineman

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ResourcesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_resources)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_resources)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val recyclerView: RecyclerView = findViewById(R.id.resources_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns

        val resourceSections = listOf(
            ResourceSection("Act and Law", R.drawable.ic_law),
            ResourceSection("Regulation", R.drawable.ic_regulation),
            ResourceSection("Safety", R.drawable.ic_safety),
            ResourceSection("Construction Standard", R.drawable.ic_construction),
            ResourceSection("Accident", R.drawable.ic_accident),
            ResourceSection("First Aid", R.drawable.ic_first_aid),
            ResourceSection("Most Useful Equipments", R.drawable.ic_equipment),
            ResourceSection("Operation Manuals", R.drawable.ic_manuals)
        )

        val adapter = ResourceSectionAdapter(resourceSections) { section ->
            // Handle click for each resource section
            showToast("${section.title} Clicked")
            // Here you would typically launch a new activity or fragment
            // to display the content for the selected section.
            // Example: startActivity(Intent(this, DetailActivity::class.java).putExtra("SECTION_TITLE", section.title))
        }
        recyclerView.adapter = adapter
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
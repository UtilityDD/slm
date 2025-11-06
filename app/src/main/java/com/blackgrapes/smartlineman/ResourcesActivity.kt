package com.blackgrapes.smartlineman

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

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

        val resourceSections = loadResourceSectionsFromJson()
        val adapter = ResourceSectionAdapter(resourceSections) { section ->
            // Handle click for each resource section
            showToast("${section.title} Clicked")
            // TODO: Launch detail view for other sections
        }
        recyclerView.adapter = adapter
    }

    private fun loadResourceSectionsFromJson(): List<ResourceSection> {
        val sections = mutableListOf<ResourceSection>()
        val fileName = "resource_sections.json"
        try {
            val jsonString: String = assets.open(fileName).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val sectionObject = jsonArray.getJSONObject(i)
                val title = sectionObject.getString("title")
                val iconName = sectionObject.getString("iconName")
                val iconResId = resources.getIdentifier(iconName, "drawable", packageName)

                // Only add if the icon exists
                if (iconResId != 0) {
                    sections.add(ResourceSection(title, iconResId))
                }
            }
        } catch (e: IOException) {
            Log.e("ResourcesActivity", "IOException: Error reading $fileName", e)
        } catch (e: JSONException) {
            Log.e("ResourcesActivity", "JSONException: Error parsing $fileName", e)
        }
        return sections
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
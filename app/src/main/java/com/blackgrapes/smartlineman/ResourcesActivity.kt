package com.blackgrapes.smartlineman

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import com.blackgrapes.smartlineman.util.JsonHelper

class ResourcesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_resources)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_resources)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // We only need padding for the bottom to prevent overlap with navigation bar
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Knowledge Base"
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.white))
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val recyclerView: RecyclerView = findViewById(R.id.resources_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns

        val resourceSections = loadResourceSectionsFromJson()
        val adapter = ResourceSectionAdapter(resourceSections) { section ->
            if (section.contentFile != null) {
                val intent = Intent(this, ChapterDetailActivity::class.java).apply {
                    putExtra(ChapterDetailActivity.EXTRA_TITLE, section.title)
                    putExtra(ChapterDetailActivity.EXTRA_CONTENT_FILE_NAME, section.contentFile)
                }
                startActivity(intent)
            } else {
                showToast("${section.title} content is not available yet.")
            }
        }
        recyclerView.adapter = adapter
    }

    private fun loadResourceSectionsFromJson(): List<ResourceSection> {
        val sections = mutableListOf<ResourceSection>()
        val fileName = "resource_sections.json"
        try {
            val jsonString = JsonHelper.loadJSON(this, fileName)
            if (jsonString != null) {
                val jsonArray = JSONArray(jsonString)

                for (i in 0 until jsonArray.length()) {
                    val sectionObject = jsonArray.getJSONObject(i)
                    val title = sectionObject.getString("title")
                    val id = sectionObject.getString("id")
                    val iconName = sectionObject.getString("iconName")
                    val contentFile = sectionObject.optString("contentFile", null)

                    var iconResId = resources.getIdentifier(iconName, "drawable", packageName)
                    if (iconResId == 0) {
                        Log.w("ResourcesActivity", "Icon not found for: $iconName. Using default.")
                        iconResId = R.drawable.ic_resources // Fallback to a default icon
                    }

                    sections.add(ResourceSection(id, title, iconResId, contentFile))
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
package com.blackgrapes.smartlineman

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import android.content.Intent
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import org.json.JSONObject
import org.json.JSONException
import android.widget.Toast
import java.io.IOException

class ChapterDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT_FILE_NAME = "EXTRA_CONTENT_FILE_NAME"
    }

    private lateinit var sections: MutableList<ChapterSection>
    private lateinit var adapter: ChapterSectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chapter_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_chapter_detail)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val title = intent.getStringExtra(EXTRA_TITLE)
        val contentFileName = intent.getStringExtra(EXTRA_CONTENT_FILE_NAME)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (contentFileName != null) {
            sections = loadSectionsFromJson(contentFileName).toMutableList()
            setupRecyclerView()
        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.chapter_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val markwon = Markwon.builder(this).usePlugin(HtmlPlugin.create()).build()

        adapter = ChapterSectionAdapter(sections, markwon) { section ->
            if (section.contentFile != null) {
                val intent = Intent(this, ChapterDetailActivity::class.java).apply {
                    putExtra(EXTRA_TITLE, section.title)
                    putExtra(EXTRA_CONTENT_FILE_NAME, section.contentFile)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "No further details available.", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter
    }

    private fun loadSectionsFromJson(fileName: String): List<ChapterSection> {
        val sectionList = mutableListOf<ChapterSection>()
        try {
            val jsonString: String = assets.open(fileName).bufferedReader().use { it.readText() }
            // The file contains a single chapter object, not an array.
            val chapterObject = JSONObject(jsonString)
            // Get the array of levels from within the chapter object.
            val levelsArray = chapterObject.getJSONArray("levels")

            for (i in 0 until levelsArray.length()) {
                val levelObject = levelsArray.getJSONObject(i)
                val title = levelObject.getString("level_title")
                val summary = levelObject.getString("level_summary")
                val status = levelObject.getString("level_status")
                val contentFile = levelObject.optString("contentFile", null)

                // Use the status to determine the emoji for the list item.
                val emoji = if (status == "unlocked") "🔓" else "🔒"

                // Map the level data to a ChapterSection object.
                sectionList.add(ChapterSection(emoji, title, summary, false, null, contentFile))
            }
        } catch (e: IOException) {
            Log.e("ChapterDetailActivity", "IOException: Error reading $fileName", e)
        } catch (e: JSONException) {
            Log.e("ChapterDetailActivity", "JSONException: Error parsing $fileName", e)
        }
        return sectionList
    }
}
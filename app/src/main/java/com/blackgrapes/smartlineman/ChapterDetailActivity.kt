package com.blackgrapes.smartlineman

import android.os.Bundle
import android.graphics.Rect
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import org.json.JSONArray
import org.json.JSONException
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
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            findViewById<Toolbar>(R.id.toolbar).setPadding(0, systemBars.top, 0, 0)
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

        adapter = ChapterSectionAdapter(sections, markwon) { position ->
            val isExpanding = !sections[position].isExpanded
            sections[position].isExpanded = !sections[position].isExpanded
            adapter.notifyItemChanged(position)

            if (isExpanding) {
                // After the layout has been updated, scroll if necessary to show the full card
                recyclerView.post {
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                    viewHolder?.itemView?.let { itemView ->
                        val rect = Rect()
                        itemView.getGlobalVisibleRect(rect)
                        if (rect.bottom > recyclerView.height) {
                            recyclerView.smoothScrollBy(0, rect.bottom - recyclerView.height + 20) // 20px padding
                        }
                    }
                }
            }
        }
        recyclerView.adapter = adapter
    }

    private fun loadSectionsFromJson(fileName: String): List<ChapterSection> {
        val sectionList = mutableListOf<ChapterSection>()
        try {
            val jsonString: String = assets.open(fileName).use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val sectionObject = jsonArray.getJSONObject(i)
                val emoji = sectionObject.getString("emoji")
                val title = sectionObject.getString("title")
                val content = sectionObject.getString("content")
                sectionList.add(ChapterSection(emoji, title, content))
            }
        } catch (e: IOException) {
            Log.e("ChapterDetailActivity", "IOException: Error reading $fileName", e)
        } catch (e: JSONException) {
            Log.e("ChapterDetailActivity", "JSONException: Error parsing $fileName", e)
        }
        return sectionList
    }
}
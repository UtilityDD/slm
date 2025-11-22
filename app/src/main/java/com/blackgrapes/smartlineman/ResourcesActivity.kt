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
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin

class ResourcesActivity : AppCompatActivity() {

    private lateinit var adapter: ResourceSectionAdapter
    private lateinit var allSections: List<ResourceSection>
    private val chapterContentMap = mutableMapOf<String, String>()

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

        allSections = loadResourceSectionsFromJson()
        val markwon = Markwon.builder(this).usePlugin(HtmlPlugin.create()).build()
        
        adapter = ResourceSectionAdapter(allSections, markwon) { section ->
            if (section.contentFile != null) {
                val intent = Intent(this, ChapterDetailActivity::class.java).apply {
                    putExtra(ChapterDetailActivity.EXTRA_TITLE, section.title)
                    putExtra(ChapterDetailActivity.EXTRA_CONTENT_FILE_NAME, section.contentFile)
                    // Pass the search query if available (optional, but good for consistency)
                }
                startActivity(intent)
            } else {
                showToast("${section.title} content is not available yet.")
            }
        }
        recyclerView.adapter = adapter
        
        preloadChapterContent()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView
        searchView.queryHint = "Search..."
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return true
            }
        })
        return true
    }

    private fun filter(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            allSections
        } else {
            val searchQuery = query.lowercase(java.util.Locale.getDefault())
            val resultList = mutableListOf<ResourceSection>()
            val breakIterator = java.text.BreakIterator.getWordInstance(java.util.Locale("bn", "BD"))

            for (section in allSections) {
                val titleMatch = section.title.lowercase(java.util.Locale.getDefault()).contains(searchQuery)
                
                if (titleMatch) {
                    resultList.add(section)
                } else {
                    val contentFileName = section.contentFile
                    if (contentFileName != null && chapterContentMap.containsKey(contentFileName)) {
                        val content = chapterContentMap[contentFileName] ?: ""
                        val lowerContent = content.lowercase(java.util.Locale.getDefault())
                        val matchIndex = lowerContent.indexOf(searchQuery)
                        
                        if (matchIndex != -1) {
                            breakIterator.setText(content)
                            
                            var start = (matchIndex - 40).coerceAtLeast(0)
                            if (start > 0) {
                                val boundary = breakIterator.preceding(start)
                                if (boundary != java.text.BreakIterator.DONE) {
                                    start = boundary
                                }
                            }

                            var end = (matchIndex + searchQuery.length + 60).coerceAtMost(content.length)
                            if (end < content.length) {
                                val boundary = breakIterator.following(end)
                                if (boundary != java.text.BreakIterator.DONE) {
                                    end = boundary
                                }
                            }
                            
                            val rawSnippet = content.substring(start, end).trim()
                            val prefix = if (start > 0) "..." else ""
                            val suffix = if (end < content.length) "..." else ""
                            var snippet = "$prefix$rawSnippet$suffix"

                            val regex = searchQuery.toRegex(RegexOption.IGNORE_CASE)
                            snippet = regex.replace(snippet) { "**${it.value}**" }

                            val newSection = section.copy(summary = snippet)
                            resultList.add(newSection)
                        }
                    }
                }
            }
            resultList
        }
        adapter.updateSections(filteredList)
    }

    private fun preloadChapterContent() {
        Thread {
            allSections.forEach { section ->
                section.contentFile?.let { fileName ->
                    try {
                        val jsonString = JsonHelper.loadJSON(this, fileName)
                        if (jsonString != null) {
                            val fullText = extractTextFromChapterJson(jsonString)
                            chapterContentMap[fileName] = fullText
                        }
                    } catch (e: Exception) {
                        Log.e("ResourcesActivity", "Error preloading content for $fileName", e)
                    }
                }
            }
        }.start()
    }

    private fun extractTextFromChapterJson(jsonString: String): String {
        val sb = StringBuilder()
        try {
            val json = org.json.JSONObject(jsonString)

            // Handle "levels" structure (e.g., B1.json)
            if (json.has("levels")) {
                sb.append(json.optString("title", "")).append(" ")
                sb.append(json.optString("description", "")).append(" ")
                
                val levelsArray = json.getJSONArray("levels")
                for (i in 0 until levelsArray.length()) {
                    val level = levelsArray.getJSONObject(i)
                    sb.append(level.optString("level_title", "")).append(" ")
                    sb.append(level.optString("level_summary", "")).append(" ")
                }
            } else {
                // Handle detailed chapter structure (e.g., chapter_1_1.json) - fallback or if reused
                sb.append(json.optString("mission_briefing", "")).append(" ")

                val sectionsArray = json.optJSONArray("sections")
                if (sectionsArray != null) {
                    for (i in 0 until sectionsArray.length()) {
                        val section = sectionsArray.getJSONObject(i)
                        sb.append(section.optString("title", "")).append(" ")
                        
                        val pointsArray = section.optJSONArray("points")
                        if (pointsArray != null) {
                            for (j in 0 until pointsArray.length()) {
                                val point = pointsArray.getJSONObject(j)
                                sb.append(point.optString("item_name", "")).append(" ")
                                sb.append(point.optString("specifications", "")).append(" ")
                                sb.append(point.optString("importance", "")).append(" ")
                                sb.append(point.optString("daily_check", "")).append(" ")
                                sb.append(point.optString("golden_rule", "")).append(" ")
                                sb.append(point.optString("safety_tip", "")).append(" ")
                            }
                        }
                    }
                }

                val proTip = json.optJSONObject("pro_tip")
                if (proTip != null) {
                    sb.append(proTip.optString("title", "")).append(" ")
                    val contentArray = proTip.optJSONArray("content")
                    if (contentArray != null) {
                        for (i in 0 until contentArray.length()) {
                            sb.append(contentArray.getString(i)).append(" ")
                        }
                    }
                }

                val mythBuster = json.optJSONObject("myth_buster")
                if (mythBuster != null) {
                    sb.append(mythBuster.optString("title", "")).append(" ")
                    val mythsArray = mythBuster.optJSONArray("myths")
                    if (mythsArray != null) {
                        for (i in 0 until mythsArray.length()) {
                            val myth = mythsArray.getJSONObject(i)
                            sb.append(myth.optString("myth", "")).append(" ")
                            sb.append(myth.optString("reality", "")).append(" ")
                        }
                    }
                }

                val advancedSection = json.optJSONObject("advanced_section")
                if (advancedSection != null) {
                    sb.append(advancedSection.optString("title", "")).append(" ")
                    val factsArray = advancedSection.optJSONArray("facts")
                    if (factsArray != null) {
                        for (i in 0 until factsArray.length()) {
                            val fact = factsArray.getJSONObject(i)
                            sb.append(fact.optString("title", "")).append(" ")
                            sb.append(fact.optString("content", "")).append(" ")
                        }
                    }
                }
            }

        } catch (e: JSONException) {
            Log.e("ResourcesActivity", "Error extracting text from JSON", e)
        }
        return sb.toString().lowercase(java.util.Locale.getDefault())
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
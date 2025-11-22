package com.blackgrapes.smartlineman

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import android.view.View
import android.content.Intent
import androidx.core.view.WindowInsetsCompat
import android.view.Menu
import androidx.appcompat.widget.SearchView
import android.content.Context
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import io.noties.markwon.html.HtmlPlugin
import org.json.JSONObject
import org.json.JSONException
import android.widget.Toast
import android.widget.Button
import android.widget.TextView
import java.util.Locale
import kotlin.math.min
import android.widget.ImageView
import java.io.IOException
import com.blackgrapes.smartlineman.util.JsonHelper

class ChapterDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IS_KNOWLEDGE_BASE = "EXTRA_IS_KNOWLEDGE_BASE"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT_FILE_NAME = "EXTRA_CONTENT_FILE_NAME"
        const val EXTRA_SEARCH_QUERY = "EXTRA_SEARCH_QUERY"
    }

    private lateinit var sections: MutableList<ChapterSection>
    private lateinit var adapter: ChapterSectionAdapter
    private lateinit var startQuizButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var noResultsTextView: TextView
    private lateinit var allSections: List<ChapterSection> // To hold the original, unfiltered list
    private var chapterLevelId: String? = null
    private var isQuizButtonActive = false
    private var isChapterListView = false // To distinguish between chapter list and chapter detail
    private val chapterContentMap = mutableMapOf<String, String>() // Map to store content file name -> full text content
    private var currentSearchQuery: String? = null

    private val chapterQuizResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            finish() // Quiz was completed, finish this detail view to go back to the list.
        }
    }

    private val chapterContentResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // This is called when we return from a chapter detail screen (after a quiz).
        // We need to reload the list to reflect the new "completed" status.
        val contentFileName = intent.getStringExtra(EXTRA_CONTENT_FILE_NAME)!!
        // Re-load all sections to get completion status, but don't immediately update the adapter
        allSections = loadSectionsFromJson(contentFileName, false)
        // Re-apply the current filter to the newly loaded data
        filter(currentSearchQuery)
        sections = allSections.toMutableList()
        adapter.updateSections(sections)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chapter_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_chapter_detail)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // We only need padding for the bottom to prevent overlap with navigation bar
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Chapter Details"
        val contentFileName = intent.getStringExtra(EXTRA_CONTENT_FILE_NAME) ?: "chapter_1_1.json" // Default to chapter_1_1.json

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = intent.getStringExtra(EXTRA_TITLE) ?: "প্রথম দিনের ইউনিফর্ম ও পিপিই" // Use a specific title for the default case
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        startQuizButton = findViewById(R.id.start_quiz_button)
        recyclerView = findViewById(R.id.chapter_recycler_view)
        noResultsTextView = findViewById(R.id.no_results_in_page_text)


        if (contentFileName != null) {
            allSections = loadSectionsFromJson(contentFileName)
            sections = allSections.toMutableList()
            setupRecyclerView()

            // If this is a chapter list, preload the content for deep search
            if (isChapterListView) {
                preloadChapterContent()
            } else {
                // Check if we have a search query to highlight and scroll to
                val searchQuery = intent.getStringExtra(EXTRA_SEARCH_QUERY)
                if (!searchQuery.isNullOrEmpty()) {
                    highlightAndScrollToQuery(searchQuery)
                }
            }
        }
    }

    private fun highlightAndScrollToQuery(query: String) {
        val lowerQuery = query.lowercase(Locale.getDefault())
        var scrollPosition = -1

        // Create a new list to avoid concurrent modification if we were iterating directly
        val updatedSections = sections.toMutableList()

        for (i in updatedSections.indices) {
            val section = updatedSections[i]
            val titleMatch = section.title.lowercase(Locale.getDefault()).contains(lowerQuery)
            val summaryMatch = section.summary.lowercase(Locale.getDefault()).contains(lowerQuery)

            if (titleMatch || summaryMatch) {
                scrollPosition = i
                
                // Highlight the match in the summary
                if (summaryMatch) {
                    val regex = query.toRegex(RegexOption.IGNORE_CASE)
                    val highlightedSummary = regex.replace(section.summary) { 
                        // Using bold and italic for highlighting as it is standard markdown
                        "***${it.value}***" 
                    }
                    
                    updatedSections[i] = section.copy(summary = highlightedSummary)
                }
                
                // We found the first match, so we can stop looking for the *scroll position*
                break 
            }
        }

        if (scrollPosition != -1) {
            sections = updatedSections
            adapter.updateSections(sections)
            
            this.recyclerView.post {
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(scrollPosition, 0)
            }
        }
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
                        Log.e("ChapterDetailActivity", "Error preloading content for $fileName", e)
                    }
                }
            }
        }.start()
    }

    private fun extractTextFromChapterJson(jsonString: String): String {
        val sb = StringBuilder()
        try {
            val json = JSONObject(jsonString)

            // 1. Mission Briefing
            sb.append(json.optString("mission_briefing", "")).append(" ")

            // 2. Sections & Points
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

            // 3. Pro Tip
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

            // 4. Myth Buster
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

            // 5. Advanced Section
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

        } catch (e: JSONException) {
            Log.e("ChapterDetailActivity", "Error extracting text from JSON", e)
        }
        return sb.toString().lowercase(Locale.getDefault())
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        val markwon = Markwon.builder(this).usePlugin(HtmlPlugin.create()).build()

        adapter = ChapterSectionAdapter(sections, markwon) { section ->
            if (section.contentFile != null) {
                val intent = Intent(this, ChapterDetailActivity::class.java).apply {
                    putExtra(EXTRA_TITLE, section.title)
                    putExtra(EXTRA_CONTENT_FILE_NAME, section.contentFile)
                    // Pass the search query if available
                    if (!currentSearchQuery.isNullOrEmpty()) {
                        putExtra(EXTRA_SEARCH_QUERY, currentSearchQuery)
                    }
                }
                chapterContentResultLauncher.launch(intent)
            } else {
                // This is a non-clickable item. Start the shake animation.
                val itemView = recyclerView.findViewHolderForAdapterPosition(sections.indexOf(section))?.itemView
                itemView?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_animation))

                if (isChapterListView) {
                    // If it's a locked chapter in a list, also show the toast message.
                    Toast.makeText(this, "আরে বন্ধু! 🚦 আগের পাঠটি ভালো করে না বুঝলে সামনে এগোনো মুশকিল। আগে ওটা শেষ করুন! 😉", Toast.LENGTH_LONG).show()
                }
                // For non-interactive cards inside a chapter, only the animation will play.
            }
        }
        recyclerView.adapter = adapter

        // Add a scroll listener to enable the button at the end.
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Check if the user can't scroll down anymore (is at the bottom)
                if (!recyclerView.canScrollVertically(1) && dy > 0) {
                    if (!isQuizButtonActive) {
                        isQuizButtonActive = true
                        startQuizButton.backgroundTintList = ContextCompat.getColorStateList(this@ChapterDetailActivity, R.color.purple_500)
                        startQuizButton.setTextColor(ContextCompat.getColor(this@ChapterDetailActivity, R.color.white))
                        startQuizButton.alpha = 1.0f
                    }
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_search, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val searchItem = menu.findItem(R.id.action_search)
        // Only show the search menu if this is a chapter list view (Knowledge Base)
        if (isChapterListView) {
            searchItem.isVisible = true
            val searchView = searchItem.actionView as SearchView
            searchView.queryHint = "Search..."
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filter(newText)
                    return true
                }
            })
        } else {
            searchItem.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun filter(query: String?) {
        currentSearchQuery = query // Update the current query
        val filteredList = if (query.isNullOrEmpty()) {
            allSections
        } else {
            val searchQuery = query.lowercase(Locale.getDefault())
            val resultList = mutableListOf<ChapterSection>()
            // Use Bengali locale for better word breaking
            val breakIterator = java.text.BreakIterator.getWordInstance(Locale("bn", "BD"))

            for (section in allSections) {
                // Fuzzy search on title and summary
                val titleWords = section.title.split(Regex("\\s+"))
                val summaryWords = section.summary.split(Regex("\\s+"))
                var matchFound = false
                val threshold = if (searchQuery.length > 5) 2 else 1

                for (word in titleWords + summaryWords) {
                    val distance = levenshteinDistance(searchQuery, word.lowercase(Locale.getDefault()).trim(' ', ',', '.', '!', '?'))
                    if (distance <= threshold) {
                        matchFound = true
                        break
                    }
                }
                
                if (matchFound) {
                    // Direct match in title or summary, add as is
                    resultList.add(section)
                }
                
                if (resultList.none { it.levelId == section.levelId && it.title == section.title }) {
                    // Check deep content
                    val contentFileName = section.contentFile
                    if (contentFileName != null && chapterContentMap.containsKey(contentFileName)) {
                        val content = chapterContentMap[contentFileName] ?: ""
                        val lowerContent = content.lowercase(Locale.getDefault())
                        val matchIndex = lowerContent.indexOf(searchQuery)
                        
                        if (matchIndex != -1) {
                            // Found in content! Create a smart snippet.
                            breakIterator.setText(content)
                            
                            // Find a good start position (approx 40 chars before)
                            var start = (matchIndex - 40).coerceAtLeast(0)
                            if (start > 0) {
                                // Align to word boundary
                                val boundary = breakIterator.preceding(start)
                                if (boundary != java.text.BreakIterator.DONE) {
                                    start = boundary
                                }
                            }

                            // Find a good end position (approx 60 chars after match end)
                            var end = (matchIndex + searchQuery.length + 60).coerceAtMost(content.length)
                            if (end < content.length) {
                                // Align to word boundary
                                val boundary = breakIterator.following(end)
                                if (boundary != java.text.BreakIterator.DONE) {
                                    end = boundary
                                }
                            }
                            
                            val rawSnippet = content.substring(start, end).trim()
                            
                            // Add ellipsis if needed
                            val prefix = if (start > 0) "..." else ""
                            val suffix = if (end < content.length) "..." else ""
                            
                            var snippet = "$prefix$rawSnippet$suffix"

                            // Highlight the match. 
                            val regex = searchQuery.toRegex(RegexOption.IGNORE_CASE)
                            snippet = regex.replace(snippet) { "**${it.value}**" }

                            // Create a new section object with the snippet as summary.
                            // We hide the image to make it more compact.
                            val newSection = section.copy(
                                summary = snippet,
                                imageName = null, // Hide image for compact look
                                imageCaption = null
                            )
                            resultList.add(newSection)
                        }
                    }
                }
            }
            resultList
        }
        adapter.updateSections(filteredList)

        if (filteredList.isEmpty()) {
            recyclerView.visibility = View.GONE
            noResultsTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noResultsTextView.visibility = View.GONE
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val s1Len = s1.length
        val s2Len = s2.length
        val dp = Array(s1Len + 1) { IntArray(s2Len + 1) }

        for (i in 0..s1Len) {
            dp[i][0] = i
        }
        for (j in 0..s2Len) {
            dp[0][j] = j
        }

        for (i in 1..s1Len) {
            for (j in 1..s2Len) {
                val cost = if (s1[i - 1].equals(s2[j - 1], ignoreCase = true)) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,          // Deletion
                    dp[i][j - 1] + 1,          // Insertion
                    dp[i - 1][j - 1] + cost    // Substitution
                )
            }
        }
        // Return distance, but cap it to avoid overly dissimilar words matching
        val distance = dp[s1Len][s2Len]
        return if (distance > 3) 99 else distance
    }

    private fun loadSectionsFromJson(fileName: String, updateUi: Boolean = true): List<ChapterSection> {
        val sectionList = mutableListOf<ChapterSection>()
        try {
            val jsonString = JsonHelper.loadJSON(this, fileName)
            if (jsonString == null) {
                throw IOException("File not found: $fileName")
            }
            val chapterJson = JSONObject(jsonString)

            // Extract level_id to use for the quiz button
            chapterLevelId = chapterJson.optString("level_id", null)

            // Check if a quiz exists for this chapter and set up the button
            if (updateUi) {
                chapterLevelId?.let { levelId ->
                    if (hasQuizForLevel(levelId)) {
                        startQuizButton.visibility = View.VISIBLE
                        isQuizButtonActive = false
                        // Set a distinct "disabled" look
                        startQuizButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.disabled_gray)
                        startQuizButton.alpha = 1.0f // Keep it fully opaque

                        startQuizButton.setOnClickListener {
                            if (isQuizButtonActive) {
                                val intent = Intent(this, ChapterQuizActivity::class.java).apply {
                                    putExtra(ChapterQuizActivity.EXTRA_LEVEL_ID, chapterLevelId)
                                }
                                chapterQuizResultLauncher.launch(intent)
                            } else {
                                Toast.makeText(this, "অধ্যায়টি শেষ পর্যন্ত পড়ে কুইজটি আনলক করুন!", Toast.LENGTH_SHORT).show()
                                startQuizButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_animation))
                            }
                        }
                    } else {
                        startQuizButton.visibility = View.GONE
                    }
                } ?: run {
                    // If there's no level_id (e.g., it's a list of chapters), hide the button.
                    startQuizButton.visibility = View.GONE
                }
            }

            // If the content is not scrollable from the start, enable the button immediately.
            if (updateUi) {
                this.recyclerView.post {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                    val totalItemCount = recyclerView.adapter?.itemCount ?: 0

                    // Check if all items are visible (i.e., not scrollable)
                    if (totalItemCount > 0 && lastVisibleItemPosition == totalItemCount - 1) {
                        if (!isQuizButtonActive) {
                            isQuizButtonActive = true
                            startQuizButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.purple_500)
                            startQuizButton.setTextColor(ContextCompat.getColor(this, R.color.white))
                        }
                    }
                }
            }

            // Check if this is a chapter list (old format) or detailed content (new format)
            if (chapterJson.has("levels")) {
                isChapterListView = true // This is a list of chapters
                // --- PARSE OLD FORMAT (List of chapters/levels) ---
                val levelsArray = chapterJson.getJSONArray("levels")
                for (i in 0 until levelsArray.length()) {
                    val levelObject = levelsArray.getJSONObject(i)
                    val title = levelObject.getString("level_title")
                    val levelId = levelObject.optString("level_id", null)
                    val summary = levelObject.getString("level_summary")

                    val isUnlocked = isLevelUnlocked(levelId)
                    val isCompleted = isChapterQuizCompleted(levelId)

                    val contentFile = if (isUnlocked) {
                        levelId?.let { "chapter_${it.replace('.', '_')}.json" }
                    } else {
                        null // This will prevent the click from opening a new activity
                    }

                    val emoji = when {
                        isCompleted -> "✅"
                        isUnlocked -> "📖"
                        else -> "🔒"
                    }

                    sectionList.add(ChapterSection(emoji, title, summary, isCompleted, null, contentFile, null, null, levelId))
                }
            } else {
                isChapterListView = false // This is a detailed chapter view
                // --- PARSE NEW FORMAT (Detailed single chapter content) ---
                // 1. Mission Briefing Card
                chapterJson.optString("mission_briefing").takeIf { it.isNotEmpty() }?.let {
                    sectionList.add(ChapterSection("🎯", "মিশন ব্রিফিং", it))
                }

                // 2. PPE Arsenal Card (from "sections" array)
                chapterJson.optJSONArray("sections")?.let { ppeSectionsArray ->
                    for (j in 0 until ppeSectionsArray.length()) {
                        val ppeSection = ppeSectionsArray.getJSONObject(j)
                        // This is the main image for the whole section, if it exists
                        val sectionImage = ppeSection.optString("image_name", null)
                        val sectionImageCaption = ppeSection.optString("image_caption", null)
                        val sectionTitle = ppeSection.getString("title")
                        // Add a card for the main section title and its image
                        sectionList.add(ChapterSection("🛡️", sectionTitle, "", false, sectionImage, null, sectionImageCaption))

                        val ppePointsArray = ppeSection.getJSONArray("points")
                        for (k in 0 until ppePointsArray.length()) {
                            val point = ppePointsArray.getJSONObject(k)
                            var pointTitle = point.getString("item_name")
                            val pointImage = point.optString("image_name", null)
                            val pointImageCaption = point.optString("image_caption", null)

                            // Extract serial number from title to use as the "emoji"
                            var serialNumber = (k + 1).toString() // Fallback to loop index
                            val regex = "^(\\d+)\\.\\s*".toRegex()
                            val matchResult = regex.find(pointTitle)
                            if (matchResult != null) {
                                serialNumber = matchResult.groupValues[1]
                                pointTitle = regex.replace(pointTitle, "") // Remove serial from title
                            }

                            // Define a map of extra fields to their Bengali labels.
                            // To support a new field, just add it to this map.
                            val extraFieldLabels = mapOf(
                                "daily_check" to "দৈনিক পরীক্ষা",
                                "golden_rule" to "গোল্ডেন রুল",
                                "safety_tip" to "নিরাপত্তা টিপ"
                            )

                            // Format specifications to have line breaks instead of sub-bullets.
                            val specifications = point.optString("specifications").replace("\n", "<br>")
                            val importance = point.optString("importance").replace("\n", "<br>")

                            val pointContent = StringBuilder()
                                .append("- **স্পেসিফিকেশন:** $specifications\n")
                                .append("- **গুরুত্ব:** $importance\n")

                            // Loop through the map to handle any extra fields generically.
                            extraFieldLabels.forEach { (key, label) ->
                                point.optString(key).takeIf { it.isNotEmpty() }?.let {
                                    pointContent.append("- **$label:** $it\n")
                                }
                            }

                            // Create a single ChapterSection for the entire point, passing null for sourceLink
                            sectionList.add(ChapterSection(serialNumber, pointTitle, pointContent.toString(), false, pointImage, null, pointImageCaption, null))
                        }
                    }
                }

                // 3. Pro Tips Card
                chapterJson.optJSONObject("pro_tip")?.let { proTipObject ->
                    val proTipTitle = proTipObject.getString("title")
                    val proTipContentArray = proTipObject.getJSONArray("content")
                    val sourceLink = proTipObject.optString("source_link", null)
                    val proTipContent = StringBuilder()
                    for (i in 0 until proTipContentArray.length()) {
                        proTipContent.append("- ${proTipContentArray.getString(i)}\n")
                    }
                    sectionList.add(ChapterSection("💡", proTipTitle, proTipContent.toString(), false, null, null, null, sourceLink))
                }

                // 4. Myth Buster Card
                chapterJson.optJSONObject("myth_buster")?.let { mythBusterObject ->
                    val mythBusterTitle = mythBusterObject.getString("title")
                    val mythsArray = mythBusterObject.getJSONArray("myths")
                    val mythBusterContent = StringBuilder()
                    val sourceLink = mythBusterObject.optString("source_link", null)
                    for (i in 0 until mythsArray.length()) {
                        val myth = mythsArray.getJSONObject(i)
                        mythBusterContent.append("**মিথ:** ${myth.getString("myth")}\n")
                        mythBusterContent.append("> **বাস্তবতা:** ${myth.getString("reality")}\n\n")
                    }
                    sectionList.add(ChapterSection("👻", mythBusterTitle, mythBusterContent.toString(), false, null, null, null, sourceLink))
                }

                // 5. Advanced Facts Card
                chapterJson.optJSONObject("advanced_section")?.let { advancedSectionObject ->
                    val sourceLink = advancedSectionObject.optString("source_link", null)
                    val advancedTitle = advancedSectionObject.getString("title")
                    val factsArray = advancedSectionObject.getJSONArray("facts")
                    val advancedContent = StringBuilder()
                    for (i in 0 until factsArray.length()) {
                        val fact = factsArray.getJSONObject(i)
                        advancedContent.append("#### ${fact.getString("title")}\n")
                        advancedContent.append("${fact.getString("content")}\n\n")
                    }
                    sectionList.add(ChapterSection("🔬", advancedTitle, advancedContent.toString(), false, null, null, null, sourceLink))
                }
            }

            // Add a "Learn More" button at the end of every chapter detail view.
            // We use a special emoji to identify it in the adapter.
            if (!isChapterListView) sectionList.add(ChapterSection("🔗", "আরও জানুন", ""))

        } catch (e: IOException) {
            Log.e("ChapterDetailActivity", "IOException: Error reading $fileName", e)
            // Optionally, show an error message to the user
            Toast.makeText(this, "Could not load chapter data.", Toast.LENGTH_LONG).show()
        } catch (e: JSONException) {
            Log.e("ChapterDetailActivity", "JSONException: Error parsing $fileName", e)
            Toast.makeText(this, "Error parsing chapter data.", Toast.LENGTH_LONG).show()
        }
        return sectionList
    }

    private fun hasQuizForLevel(levelId: String): Boolean {
        val quizFileName = "questions_${levelId.replace('.', '_')}.json"
        return try {
            assets.open(quizFileName).close() // Check if file exists
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun isLevelUnlocked(levelId: String?): Boolean {
        if (levelId == null) return false

        val levelParts = levelId.split('.')
        if (levelParts.size < 2) return false // Invalid level_id format
        val majorVersion = levelParts[0]
        val minorVersion = levelParts[1].toIntOrNull() ?: return false

        // The first level is always unlocked
        if (minorVersion <= 1) return true

        // For subsequent levels, check if the previous chapter's quiz was completed
        val previousMinorVersion = minorVersion - 1
        val previousLevelId = "$majorVersion.$previousMinorVersion"

        // The level is unlocked if the previous chapter's quiz was completed
        return isChapterQuizCompleted(previousLevelId)
    }

    private fun isChapterQuizCompleted(levelId: String?): Boolean {
        if (levelId == null) return false
        val sharedPref = getSharedPreferences("ChapterProgress", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("chapter_quiz_completed_$levelId", false)
    }
}
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
import android.widget.ImageView
import java.io.IOException

class ChapterDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT_FILE_NAME = "EXTRA_CONTENT_FILE_NAME"
    }

    private lateinit var sections: MutableList<ChapterSection>
    private lateinit var adapter: ChapterSectionAdapter
    private lateinit var startQuizButton: Button
    private var chapterLevelId: String? = null
    private var isQuizButtonActive = false

    private val chapterQuizResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            finish() // Quiz was completed, finish this detail view to go back to the list.
        }
    }

    private val chapterContentResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // This is called when we return from a chapter detail screen (after a quiz).
        // We need to reload the list to reflect the new "completed" status.
        val contentFileName = intent.getStringExtra(EXTRA_CONTENT_FILE_NAME)
        sections = loadSectionsFromJson(contentFileName!!).toMutableList()
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
                chapterContentResultLauncher.launch(intent)
            } else {
                Toast.makeText(this, "আগের লেভেলের কুইজটি সম্পূর্ণ করে এটি আনলক করুন!", Toast.LENGTH_LONG).show()
                recyclerView.findViewHolderForAdapterPosition(sections.indexOf(section))?.itemView?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_animation))
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

    private fun loadSectionsFromJson(fileName: String): List<ChapterSection> {
        val sectionList = mutableListOf<ChapterSection>()
        try {
            val jsonString = assets.open(fileName).bufferedReader().use { it.readText() }
            val chapterJson = JSONObject(jsonString)

            // Extract level_id to use for the quiz button
            chapterLevelId = chapterJson.optString("level_id", null)

            // Check if a quiz exists for this chapter and set up the button
            chapterLevelId?.let { levelId ->
                val levelNumber = levelId.split('.').lastOrNull()?.toIntOrNull()
                if (levelNumber != null && hasQuizForLevel(levelNumber)) {
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

            // If the content is not scrollable from the start, enable the button immediately.
            val recyclerView: RecyclerView = findViewById(R.id.chapter_recycler_view)
            recyclerView.post {
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




            // Check if this is a chapter list (old format) or detailed content (new format)
            if (chapterJson.has("levels")) {
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

                    sectionList.add(ChapterSection(emoji, title, summary, isCompleted, null, contentFile))
                }
            } else {
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
                            val pointTitle = point.getString("item_name")
                            val pointImage = point.optString("image_name", null)
                            val pointImageCaption = point.optString("image_caption", null)

                            val pointContent = StringBuilder()
                                .append("- **স্পেসিফিকেশন:** ${point.optString("specifications")}\n")
                                .append("- **গুরুত্ব:** ${point.optString("importance")}\n")

                            // Handle both "daily_check" and "golden_rule" gracefully
                            point.optString("daily_check").takeIf { it.isNotEmpty() }?.let {
                                pointContent.append("- **দৈনিক পরীক্ষা:** $it\n")
                            }
                            point.optString("golden_rule").takeIf { it.isNotEmpty() }?.let {
                                pointContent.append("- **গোল্ডেন রুল:** $it\n")
                            }

                            // Create a single ChapterSection for the entire point, passing null for sourceLink
                            sectionList.add(ChapterSection("🔹", pointTitle, pointContent.toString(), false, pointImage, null, pointImageCaption, null))
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

    private fun hasQuizForLevel(level: Int): Boolean {
        val levelId = "1.$level"
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
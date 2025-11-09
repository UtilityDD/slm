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
import android.graphics.BitmapFactory
import io.noties.markwon.html.HtmlPlugin
import org.json.JSONObject
import org.json.JSONException
import android.widget.Toast
import android.widget.ImageView
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
            val jsonString = assets.open(fileName).bufferedReader().use { it.readText() }
            val chapterJson = JSONObject(jsonString)

            // Check if this is a chapter list (old format) or detailed content (new format)
            if (chapterJson.has("levels")) {
                // --- PARSE OLD FORMAT (List of chapters/levels) ---
                val levelsArray = chapterJson.getJSONArray("levels")
                for (i in 0 until levelsArray.length()) {
                    val levelObject = levelsArray.getJSONObject(i)
                    val title = levelObject.getString("level_title")
                    val summary = levelObject.getString("level_summary")
                    val status = levelObject.getString("level_status")
                    val contentFile = levelObject.optString("level_id", null)?.let { "chapter_${it.replace('.', '_')}.json" }
                    val emoji = "${i + 1}." // Use serial number instead of lock icon
                    sectionList.add(ChapterSection(emoji, title, summary, false, null, contentFile))
                }
            } else {
                // --- PARSE NEW FORMAT (Detailed single chapter content) ---
                // 0. Chapter Image
                chapterJson.optString("image_name").takeIf { it.isNotEmpty() }?.let { imageName ->
                    val imageView: ImageView = findViewById(R.id.chapter_image)
                    try {
                        val inputStream = assets.open(imageName)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = ImageView.VISIBLE
                        inputStream.close()
                    } catch (e: IOException) {
                        Log.e("ChapterDetailActivity", "Error loading image from assets: $imageName", e)
                        imageView.visibility = ImageView.GONE
                    }
                }

                // 1. Mission Briefing Card
                chapterJson.optString("mission_briefing").takeIf { it.isNotEmpty() }?.let {
                    sectionList.add(ChapterSection("🎯", "মিশন ব্রিফিং", it))
                }

                // 2. PPE Arsenal Card (from "sections" array)
                chapterJson.optJSONArray("sections")?.let { ppeSectionsArray ->
                    if (ppeSectionsArray.length() > 0) {
                        val ppeSection = ppeSectionsArray.getJSONObject(0)
                        val ppeTitle = ppeSection.getString("title")
                        val ppePointsArray = ppeSection.getJSONArray("points")
                        val ppeContent = StringBuilder()
                        for (i in 0 until ppePointsArray.length()) {
                            val point = ppePointsArray.getJSONObject(i)
                            ppeContent.append("### ${point.getString("item_name")}\n")
                            ppeContent.append("- **স্পেসিফিকেশন:** ${point.getString("specifications")}\n")
                            ppeContent.append("- **গুরুত্ব:** ${point.getString("importance")}\n")
                            ppeContent.append("- **দৈনিক পরীক্ষা:** ${point.getString("daily_check")}\n\n")
                        }
                        sectionList.add(ChapterSection("🛡️", ppeTitle, ppeContent.toString()))
                    }
                }

                // 3. Pro Tips Card
                chapterJson.optJSONObject("pro_tip")?.let { proTipObject ->
                    val proTipTitle = proTipObject.getString("title")
                    val proTipContentArray = proTipObject.getJSONArray("content")
                    val proTipContent = StringBuilder()
                    for (i in 0 until proTipContentArray.length()) {
                        proTipContent.append("- ${proTipContentArray.getString(i)}\n")
                    }
                    sectionList.add(ChapterSection("💡", proTipTitle, proTipContent.toString()))
                }

                // 4. Myth Buster Card
                chapterJson.optJSONObject("myth_buster")?.let { mythBusterObject ->
                    val mythBusterTitle = mythBusterObject.getString("title")
                    val mythsArray = mythBusterObject.getJSONArray("myths")
                    val mythBusterContent = StringBuilder()
                    for (i in 0 until mythsArray.length()) {
                        val myth = mythsArray.getJSONObject(i)
                        mythBusterContent.append("**মিথ:** ${myth.getString("myth")}\n")
                        mythBusterContent.append("> **বাস্তবতা:** ${myth.getString("reality")}\n\n")
                    }
                    sectionList.add(ChapterSection("👻", mythBusterTitle, mythBusterContent.toString()))
                }

                // 5. Advanced Facts Card
                chapterJson.optJSONObject("advanced_section")?.let { advancedSectionObject ->
                    val advancedTitle = advancedSectionObject.getString("title")
                    val factsArray = advancedSectionObject.getJSONArray("facts")
                    val advancedContent = StringBuilder()
                    for (i in 0 until factsArray.length()) {
                        val fact = factsArray.getJSONObject(i)
                        advancedContent.append("#### ${fact.getString("title")}\n")
                        advancedContent.append("${fact.getString("content")}\n\n")
                    }
                    sectionList.add(ChapterSection("🔬", advancedTitle, advancedContent.toString()))
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
}
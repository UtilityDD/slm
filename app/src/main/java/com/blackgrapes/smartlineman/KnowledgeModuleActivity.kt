package com.blackgrapes.smartlineman

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.blackgrapes.smartlineman.util.JsonHelper
import com.google.android.material.card.MaterialCardView
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class KnowledgeModuleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_KM_FILE_NAME = "EXTRA_KM_FILE_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_knowledge_module)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_km)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val fileName = intent.getStringExtra(EXTRA_KM_FILE_NAME)
        if (fileName == null) {
            Toast.makeText(this, "Could not load content.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadContentFromJson(fileName)
    }

    private fun loadContentFromJson(fileName: String) {
        try {
            val jsonString = JsonHelper.loadJSON(this, fileName)
            if (jsonString == null) {
                throw IOException("File not found: $fileName")
            }
            val kmJson = JSONObject(jsonString)

            // Set main title and briefing
            val title = kmJson.getString("title")
            val briefing = kmJson.getString("briefing")
            supportActionBar?.title = title
            findViewById<TextView>(R.id.km_title).text = title
            findViewById<TextView>(R.id.km_briefing).text = briefing

            // Inflate sections
            val sectionsContainer = findViewById<LinearLayout>(R.id.km_sections_container)
            val sectionsArray = kmJson.getJSONArray("sections")
            val inflater = LayoutInflater.from(this)

            for (i in 0 until sectionsArray.length()) {
                val sectionObject = sectionsArray.getJSONObject(i)
                val sectionCard = inflater.inflate(R.layout.knowledge_module_section_card, sectionsContainer, false) as MaterialCardView

                val sectionTitle = sectionObject.getString("title")
                sectionCard.findViewById<TextView>(R.id.km_section_title).text = sectionTitle

                val pointsContainer = sectionCard.findViewById<LinearLayout>(R.id.km_points_container)
                val pointsArray = sectionObject.getJSONArray("points")

                for (j in 0 until pointsArray.length()) {
                    val pointObject = pointsArray.getJSONObject(j)
                    val pointItemView = inflater.inflate(R.layout.knowledge_module_point_item, pointsContainer, false)

                    val subtitle = pointObject.getString("subtitle")
                    pointItemView.findViewById<TextView>(R.id.km_point_subtitle).text = subtitle

                    val detailsArray = pointObject.getJSONArray("details")
                    val detailsStringBuilder = StringBuilder()
                    for (k in 0 until detailsArray.length()) {
                        val detailObject = detailsArray.getJSONObject(k)
                        val key = detailObject.getString("key")
                        val value = detailObject.getString("value")
                        detailsStringBuilder.append("• **$key:** $value\n")
                    }
                    pointItemView.findViewById<TextView>(R.id.km_point_details).text = detailsStringBuilder.toString().trim()

                    pointsContainer.addView(pointItemView)
                }
                sectionsContainer.addView(sectionCard)
            }

            // Inflate stat box if it exists
            kmJson.optJSONObject("stat_box")?.let { statBoxObject ->
                val statBoxCard = findViewById<MaterialCardView>(R.id.stat_box_card)
                val statBoxTitle = statBoxObject.getString("title")
                val statBoxContent = statBoxObject.getString("content")

                statBoxCard.findViewById<TextView>(R.id.stat_box_title).text = statBoxTitle
                statBoxCard.findViewById<TextView>(R.id.stat_box_content).text = statBoxContent
                statBoxCard.visibility = View.VISIBLE
            }

        } catch (e: IOException) {
            Log.e("KnowledgeModuleActivity", "IOException: Error reading $fileName", e)
            Toast.makeText(this, "Could not load content data.", Toast.LENGTH_LONG).show()
        } catch (e: JSONException) {
            Log.e("KnowledgeModuleActivity", "JSONException: Error parsing $fileName", e)
            Toast.makeText(this, "Error parsing content data.", Toast.LENGTH_LONG).show()
        }
    }
}
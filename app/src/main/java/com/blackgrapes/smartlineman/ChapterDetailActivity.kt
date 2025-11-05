package com.blackgrapes.smartlineman

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin

class ChapterDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT_RES_ID = "EXTRA_CONTENT_RES_ID"
    }

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
        val contentResId = intent.getIntExtra(EXTRA_CONTENT_RES_ID, 0)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (contentResId != 0) {
            val contentTextView: TextView = findViewById(R.id.chapter_content_textview)
            val markdown = getString(contentResId)

            val markwon = Markwon.builder(this).usePlugin(HtmlPlugin.create()).build()
            markwon.setMarkdown(contentTextView, markdown)
        }
    }
}
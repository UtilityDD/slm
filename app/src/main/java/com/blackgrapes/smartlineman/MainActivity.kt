package com.blackgrapes.smartlineman

import android.content.Intent
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var scrollView: NestedScrollView
    private val scrollCheckRunnable = Runnable { onScrollIdle() }
    private var isScrolling = false
    private var lastScrollY = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Apply padding for the status bar at the top and the navigation bar at the bottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply top padding to the header container to avoid overlap with status bar
            findViewById<View>(R.id.header_container).setPadding(0, systemBars.top, 0, 16)
            // Apply bottom padding to the footer text to avoid overlap with navigation bar
            findViewById<View>(R.id.footer_text).setPadding(0, 0, 0, systemBars.bottom + 24)
            insets
        }

        // Scroll to the bottom to show the lineman
        scrollView = findViewById(R.id.scroll_view)
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }

        // Add a scroll listener to implement the elastic snap-back effect
        scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val totalHeight = scrollView.getChildAt(0).height - scrollView.height

            // If scrolling and not at the top or bottom, handle snap-back logic
            if (scrollY > 0 && scrollY < totalHeight) {
                isScrolling = true
                lastScrollY = scrollY
                scrollView.removeCallbacks(scrollCheckRunnable)
                scrollView.postDelayed(scrollCheckRunnable, 150)
            } else {
                isScrolling = false
                scrollView.removeCallbacks(scrollCheckRunnable)
            }
        })

        // Apply pulsating animation to level buttons
        val pulseAnimation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_out)

        for (i in 1..100) {
            val levelId = resources.getIdentifier("level_${i}_button", "id", packageName)
            // Check if the view exists before trying to access it
            if (levelId != 0) {
                val levelButton = findViewById<View>(levelId)
                levelButton?.let {
                    it.startAnimation(pulseAnimation)
                    it.setOnClickListener {
                        startGame(i)
                    }
                }
            }
        }
    }

    private fun startGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL", level)
        startActivity(intent)
    }

    private fun onScrollIdle() {
        if (isScrolling) {
            isScrolling = false
            // Smoothly scroll back to the bottom (lineman's position)
            val bottomY = scrollView.getChildAt(0).height - scrollView.height
            ObjectAnimator.ofInt(scrollView, "scrollY", lastScrollY, bottomY).setDuration(400).start()
        }
    }
}
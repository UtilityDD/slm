package com.blackgrapes.smartlineman

import android.app.Activity
import android.content.Intent
import android.animation.Animator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.core.view.ViewCompat
import android.util.DisplayMetrics
import androidx.core.view.WindowInsetsCompat
import android.content.Context
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    private lateinit var scrollView: NestedScrollView
    private lateinit var linemanCharacter: ImageView
    private val scrollCheckRunnable = Runnable { onScrollIdle() }
    private var isScrolling = false
    private var lastScrollY = 0
    private var currentLevel = 1
    private var targetScrollY = 0

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
        val mainView = findViewById<View>(R.id.main)
        scrollView = findViewById(R.id.scroll_view)
        linemanCharacter = findViewById(R.id.lineman_character)

        mainView.post {
            loadProgress()
        }

        // Add a scroll listener to implement the elastic snap-back effect
        scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val totalHeight = scrollView.getChildAt(0).height - scrollView.height

            // If scrolling and not at the top or bottom, handle snap-back logic
            if (scrollY > 0 && scrollY < totalHeight) {
                isScrolling = true // Disable snap-back for now as it conflicts with climbing
                lastScrollY = scrollY
                scrollView.removeCallbacks(scrollCheckRunnable)
                scrollView.postDelayed(scrollCheckRunnable, 150)
            } else {
                isScrolling = false
                scrollView.removeCallbacks(scrollCheckRunnable)
            }
        })

        for (i in 1..100) {
            val levelId = resources.getIdentifier("level_${i}_button", "id", packageName)
            // Check if the view exists before trying to access it
            if (levelId != 0) {
                val levelButton = findViewById<View>(levelId)
                levelButton?.let {
                    it.setOnClickListener {
                        startGame(i)
                    }
                }
            }
        }
    }

    private val gameActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val levelPassed = result.data?.getIntExtra("level_passed", -1) ?: -1
            if (levelPassed != -1 && levelPassed == currentLevel) {
                currentLevel++
                saveProgress()
                updateLinemanPosition(currentLevel, true)
            }
        }
    }

    private fun startGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL", level)
        gameActivityResultLauncher.launch(intent)
    }

    private fun updateLinemanPosition(level: Int, animate: Boolean) {
        // The lineman should be on the rung of the previously completed level.
        // If currentLevel is 1, he is on the ground (level 0).
        val linemanOnLevel = level - 1

        if (linemanOnLevel == 0) {
            // Position on the ground
            // The lineman is already in the correct ground position via XML.
            // We just need to scroll to the bottom to make him visible.
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
                // Set the target for snap-back
                val bottomY = scrollView.getChildAt(0).height - scrollView.height
                targetScrollY = bottomY
            }
        } else {
            val levelButtonId = resources.getIdentifier("level_${linemanOnLevel}_button", "id", packageName)

            if (levelButtonId != 0) {
                val levelButton = findViewById<View>(levelButtonId)
                levelButton.post {
                    // Position the lineman's feet on the rung
                    val targetY = levelButton.y + levelButton.height - linemanCharacter.height
                    targetScrollY = (targetY - (scrollView.height / 2) + (linemanCharacter.height / 2)).toInt()

                    if (animate) {
                        linemanCharacter.animate()
                            .y(targetY)
                            .setDuration(1000)
                            .withEndAction { scrollView.smoothScrollTo(0, targetScrollY) }
                            .start()
                    } else {
                        linemanCharacter.y = targetY
                        scrollView.post { scrollView.scrollTo(0, targetScrollY) }
                    }
                }
            }
        }
    }

    private fun saveProgress() {
        val sharedPref = getSharedPreferences("GameProgress", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("currentLevel", currentLevel)
            apply()
        }
    }

    private fun loadProgress() {
        val sharedPref = getSharedPreferences("GameProgress", Context.MODE_PRIVATE)
        currentLevel = sharedPref.getInt("currentLevel", 1)
        updateLinemanPosition(currentLevel, false)
    }

    private fun onScrollIdle() {
        if (isScrolling) {
            isScrolling = false
            // Smoothly scroll back to the bottom (lineman's position)
            ObjectAnimator.ofInt(scrollView, "scrollY", lastScrollY, targetScrollY).setDuration(400).start()
        }
    }
}
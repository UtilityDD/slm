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
import android.widget.TextView
import android.content.Context
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var scrollView: NestedScrollView
    private lateinit var linemanCharacter: ImageView
    private val scrollCheckRunnable = Runnable { onScrollIdle() }
    private var isScrolling = false
    private var lastScrollY = 0
    private var currentLevel = 1
    private var targetScrollY = 0

    // Floating Action Button menu state
    private lateinit var fabMain: FloatingActionButton

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
            loadProgress(false)
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
                val levelToStart = i // Capture the level number for the listener
                levelButton?.setOnClickListener {
                    startGame(levelToStart)
                }

            }
        }

        setupFabMenu()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the progress and UI in case it was reset in another activity
        loadProgress(false) // Don't animate on a regular resume
    }

    private val menuActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Progress was reset in MenuActivity, reload with animation
            loadProgress(true)
        }
    }

    private fun setupFabMenu() {
        fabMain = findViewById(R.id.fab_main)

        fabMain.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            menuActivityResultLauncher.launch(intent)
        }
    }

    private val gameActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val levelPassed = result.data?.getIntExtra("level_passed", -1) ?: -1
            if (levelPassed != -1 && levelPassed == currentLevel) {
                // Level was passed, advance to the next one
                currentLevel++
                saveProgress()
                updateLinemanPosition(currentLevel - 1, true)
            }
        } else {
            // Even if the level wasn't passed, refresh the colors and scores
            updateLevelColors()
        }
    }

    private fun startGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL", level)
        gameActivityResultLauncher.launch(intent)
    }

    private fun updateLinemanPosition(linemanOnLevel: Int, animate: Boolean) {
        // The lineman should be on the rung of the previously completed level.

        updateLevelColors()

        if (linemanOnLevel == 0) {
            // Position on the ground
            // The lineman is already in the correct ground position via XML.
            scrollView.post {
                // Reset lineman's Y position to the bottom of the ladder
                val groundY = (scrollView.getChildAt(0).height - linemanCharacter.height - findViewById<View>(R.id.ground_base).height).toFloat()

                if (animate) {
                    linemanCharacter.animate()
                        .y(groundY)
                        .setDuration(1000)
                        .withEndAction { scrollView.fullScroll(View.FOCUS_DOWN) }
                        .start()
                } else {
                    linemanCharacter.y = groundY
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
                // Set the target for snap-back
                val bottomY = scrollView.getChildAt(0).height - scrollView.height
                targetScrollY = bottomY
            }
        } else {
            val levelButtonId = resources.getIdentifier("level_${linemanOnLevel}_button", "id", packageName)

            if (levelButtonId != 0) {
                val levelButton = findViewById<View>(levelButtonId)
                val futureLevelButtonId = resources.getIdentifier("level_${linemanOnLevel + 2}_button", "id", packageName)
                val futureLevelButton = if (futureLevelButtonId != 0) findViewById<View>(futureLevelButtonId) else null

                levelButton.post {
                    // Position the lineman's feet on the current rung
                    val linemanTargetY = levelButton.y + (levelButton.height / 2) - linemanCharacter.height

                    // Calculate the scroll position.
                    // If a future level button exists, scroll to show it. Otherwise, center the lineman.
                    targetScrollY = if (futureLevelButton != null && animate) { // Only auto-scroll on animation
                        (futureLevelButton.y - (scrollView.height / 2) + (futureLevelButton.height / 2)).toInt()
                    } else {
                        (linemanTargetY - (scrollView.height / 2) + (linemanCharacter.height / 2)).toInt()
                    }

                    if (animate) {
                        linemanCharacter.animate()
                            .y(linemanTargetY)
                            .setDuration(1000)
                            .withEndAction { scrollView.smoothScrollTo(0, targetScrollY) }
                            .start()
                    } else {
                        linemanCharacter.y = linemanTargetY
                        scrollView.post { scrollView.scrollTo(0, targetScrollY) }
                    }
                }
            }
        }
    }

    private fun updateLevelColors() {
        for (i in 1..100) {
            val levelId = resources.getIdentifier("level_${i}_button", "id", packageName)
            if (levelId != 0) {
                findViewById<View>(levelId)?.let { levelButton ->
                    val scoreTextViewId = resources.getIdentifier("level_${i}_score_text", "id", packageName)
                    val scoreTextView = if (scoreTextViewId != 0) findViewById<TextView>(scoreTextViewId) else null

                    val sharedPref = getSharedPreferences("GameProgress", Context.MODE_PRIVATE)
                    val highScore = sharedPref.getInt("high_score_level_$i", -1)
                    val totalQuestions = sharedPref.getInt("total_questions_level_$i", -1)

                    if (highScore != -1 && totalQuestions != -1) {
                        scoreTextView?.text = getString(R.string.score_display, highScore, totalQuestions)
                        scoreTextView?.visibility = View.VISIBLE
                    } else {
                        scoreTextView?.visibility = View.GONE
                    }

                    // For now, all levels are unlocked. Highlight the current one.
                    if (i == currentLevel) {
                        levelButton.setBackgroundResource(R.drawable.level_marker_active_background)
                    } else {
                        levelButton.setBackgroundResource(R.drawable.level_marker_background)
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

    private fun loadProgress(animate: Boolean) {
        val sharedPref = getSharedPreferences("GameProgress", Context.MODE_PRIVATE)
        var highestLevelPlayed = 0
        for (i in 1..100) {
            if (sharedPref.contains("high_score_level_$i")) {
                highestLevelPlayed = i
            } else {
                break // Stop when we find a level without a high score
            }
        }
        currentLevel = highestLevelPlayed + 1
        updateLinemanPosition(highestLevelPlayed, animate) // Position based on the highest level played
    }

    private fun onScrollIdle() {
        if (isScrolling) {
            isScrolling = false
            // Smoothly scroll back to the bottom (lineman's position)
            ObjectAnimator.ofInt(scrollView, "scrollY", lastScrollY, targetScrollY).setDuration(400).start()
        }
    }
}
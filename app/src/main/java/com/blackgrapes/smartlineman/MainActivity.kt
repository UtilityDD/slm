package com.blackgrapes.smartlineman

import android.app.Activity
import android.content.Intent
import android.animation.Animator
import android.animation.ObjectAnimator
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.text.Html
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView
import android.content.Context
import android.media.SoundPool
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

    // Background music player
    private var mediaPlayer: MediaPlayer? = null

    // Mute buttons and state
    private lateinit var muteMusicButton: ImageView
    private lateinit var muteSfxButton: ImageView
    private var isMusicMuted = false
    private var isSfxMuted = false

    // UI Sound Effects
    private var uiSoundPool: SoundPool? = null
    private var unlockSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Force system icons to be dark, making them invisible on a dark background
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true

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

        // Set the colorful title text
        val titleTextView = findViewById<TextView>(R.id.title_lineman_quest)
        val styledText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(getString(R.string.title_lineman_quest), Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(getString(R.string.title_lineman_quest))
        }
        titleTextView.text = styledText

        val swingAnimation = AnimationUtils.loadAnimation(this, R.anim.swing_animation)
        linemanCharacter.startAnimation(swingAnimation)

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
                    if (levelToStart <= currentLevel) {
                        startGame(levelToStart)
                    } else {
                        // Optionally, you can show a message that the level is locked
                        Toast.makeText(this, "Complete previous levels to unlock!", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

        setupFabMenu()
        setupMuteButtons()

        // Initialize and start background music
        mediaPlayer = MediaPlayer.create(this, R.raw.background)
        mediaPlayer?.isLooping = true

        // Initialize SoundPool for UI sound effects
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        uiSoundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        unlockSoundId = uiSoundPool!!.load(this, R.raw.unlock, 1)
    }

    override fun onResume() {
        super.onResume()
        // Refresh the progress and UI in case it was reset in another activity
        loadProgress(false) // Don't animate on a regular resume

        // Check mute state and manage music
        val prefs = getSharedPreferences("GameSettings", Context.MODE_PRIVATE)
        isMusicMuted = prefs.getBoolean("music_muted", false)
        isSfxMuted = prefs.getBoolean("sfx_muted", false)
        updateMuteButtonIcons()

        if (!isMusicMuted) {
            mediaPlayer?.takeIf { !it.isPlaying }?.start()
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
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

    private fun setupMuteButtons() {
        muteMusicButton = findViewById(R.id.mute_music_button)
        muteSfxButton = findViewById(R.id.mute_sfx_button)

        muteMusicButton.setOnClickListener {
            isMusicMuted = !isMusicMuted
            getSharedPreferences("GameSettings", Context.MODE_PRIVATE).edit()
                .putBoolean("music_muted", isMusicMuted)
                .apply()
            updateMuteButtonIcons()

            if (isMusicMuted) {
                mediaPlayer?.pause()
            } else {
                mediaPlayer?.start()
            }
        }

        muteSfxButton.setOnClickListener {
            isSfxMuted = !isSfxMuted
            getSharedPreferences("GameSettings", Context.MODE_PRIVATE).edit()
                .putBoolean("sfx_muted", isSfxMuted)
                .apply()
            updateMuteButtonIcons()
            val message = if (isSfxMuted) "Sound effects muted" else "Sound effects unmuted"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMuteButtonIcons() {
        runOnUiThread {
            val musicIcon = if (isMusicMuted) R.drawable.ic_music_off else R.drawable.ic_music_on
            muteMusicButton.setImageResource(musicIcon)

            val sfxIcon = if (isSfxMuted) R.drawable.ic_sfx_off else R.drawable.ic_sfx_on
            muteSfxButton.setImageResource(sfxIcon)
        }
    }

    private val gameActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Regardless of the result (pass or fail), we need to reload the progress.
        // loadProgress will determine the correct currentLevel and update the UI,
        // including lineman position and score badges.
        // Animate the lineman's movement if the level was passed.
        val animate = result.resultCode == Activity.RESULT_OK
        loadProgress(animate)
    }

    private fun startGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(GameActivity.EXTRA_LEVEL, level)

        // Use the LevelManager to get the dynamic level ID
        val levelId = LevelManager.getLevelId(level)

        intent.putExtra(GameActivity.EXTRA_LEVEL_ID, levelId)
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
                    val distance = Math.abs(linemanCharacter.y - groundY)
                    // Slower speed: 500ms per 100 pixels, with a min of 1.5s and max of 5s
                    val duration = (distance / 100 * 500).toLong().coerceIn(1500, 5000)
                    linemanCharacter.animate()
                        .y(groundY)
                        .setDuration(duration)
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
                        val distance = Math.abs(linemanCharacter.y - linemanTargetY)
                        // Slower speed: 500ms per 100 pixels, with a min of 1.5s and max of 5s
                        val duration = (distance / 100 * 500).toLong().coerceIn(1500, 5000)
                        linemanCharacter.animate()
                            .y(linemanTargetY)
                            .setDuration(duration)
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

                    if (highScore != -1 && totalQuestions > 0) {
                        if (highScore == totalQuestions) {
                            // Perfect score, show time
                            val totalTime = sharedPref.getLong("total_time_level_$i", 0)
                            val avgTime = totalTime.toFloat() / totalQuestions.toFloat() / 1000f
                            scoreTextView?.text = getString(R.string.time_display, avgTime)
                        } else {
                            // Not a perfect score, show score
                            scoreTextView?.text = getString(R.string.score_display, highScore, totalQuestions)
                        }
                        scoreTextView?.visibility = View.VISIBLE
                    } else {
                        scoreTextView?.visibility = View.GONE
                    }

                    // Set the background based on the level's state
                    when {
                        i < currentLevel -> levelButton.setBackgroundResource(R.drawable.level_marker_completed_background)
                        i == currentLevel -> levelButton.setBackgroundResource(R.drawable.level_marker_active_background)
                        else -> levelButton.setBackgroundResource(R.drawable.level_marker_disabled_background)
                    }
                }
            }
        }
    }

    private fun saveProgress(levelToSave: Int) {
        val sharedPref = getSharedPreferences("GameProgress", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("currentLevel", levelToSave)
            apply()
        }
    }

    private fun loadProgress(animate: Boolean) {
        val sharedPref = getSharedPreferences("GameProgress", Context.MODE_PRIVATE)
        val previousLevel = currentLevel
        var highestLevelCompleted = 0
        // A level is only considered "completed" if a perfect score was achieved.
        for (i in 1..100) { // Check up to 100 levels
            val highScore = sharedPref.getInt("high_score_level_$i", -1)
            val totalQuestions = sharedPref.getInt("total_questions_level_$i", 0)
            if (highScore != -1 && highScore == totalQuestions) {
                highestLevelCompleted = i
            } else {
                break // Stop when we find a level that wasn't completed perfectly.
            }
        }
        currentLevel = highestLevelCompleted + 1

        if (currentLevel > previousLevel && previousLevel > 0) { // Play sound if a level was unlocked
            if (!isSfxMuted) uiSoundPool?.play(unlockSoundId, 0.8f, 0.8f, 0, 0, 1f)
        }

        updateLinemanPosition(highestLevelCompleted, animate) // Position based on the highest level completed
    }

    private fun onScrollIdle() {
        if (isScrolling) {
            isScrolling = false
            // Smoothly scroll back to the bottom (lineman's position)
            ObjectAnimator.ofInt(scrollView, "scrollY", lastScrollY, targetScrollY).setDuration(400).start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        uiSoundPool?.release()
        uiSoundPool = null
    }
}
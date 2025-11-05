package com.blackgrapes.smartlineman

import android.content.Context
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.io.IOException
import java.util.concurrent.TimeUnit

class ScoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_score)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_score)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val score = intent.getIntExtra(GameActivity.EXTRA_SCORE, 0)
        val totalQuestions = intent.getIntExtra(GameActivity.EXTRA_TOTAL_QUESTIONS, 0)
        val level = intent.getIntExtra(GameActivity.EXTRA_LEVEL, 1)
        val totalTime = intent.getLongExtra(GameActivity.EXTRA_TOTAL_TIME, 0)

        val scoreTextView = findViewById<TextView>(R.id.score_text)
        val feedbackEmojiTextView = findViewById<TextView>(R.id.feedback_emoji)
        val wellDoneTextView = findViewById<TextView>(R.id.well_done_text)

        scoreTextView.text = "$score / $totalQuestions"

        saveHighScore(level, score, totalQuestions)

        if (totalQuestions > 0) {
            val avgTime = totalTime.toFloat() / totalQuestions.toFloat() / 1000f
            animateAverageTime(avgTime)
        }
        val percentage = if (totalQuestions > 0) (score * 100) / totalQuestions else 0

        val (feedbackMessage, emoji) = when {
            percentage == 100 -> {
                startConfetti()
                "Perfect!" to "🏆"
            }
            percentage >= 75 -> "Great Job!" to "👍"
            percentage >= 50 -> "Good Effort!" to "😊"
            else -> "Keep Trying!" to "💪"
        }

        wellDoneTextView.text = feedbackMessage
        feedbackEmojiTextView.text = emoji


        val nextLevelButton = findViewById<Button>(R.id.next_level_button)
        val playAgainButton = findViewById<Button>(R.id.play_again_button)

        // Show "Next Level" button if the player scored perfectly
        // Also, check if there is a next level to go to.

        if (score == totalQuestions && hasNextLevel(level)) {
            nextLevelButton.visibility = View.VISIBLE
            // When the level is passed, set a result for MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("level_passed", level)
            setResult(RESULT_OK, resultIntent)
        } else {
            setResult(RESULT_CANCELED)
            nextLevelButton.visibility = View.GONE
        }




        playAgainButton.setOnClickListener {
            // Simply finish and go back to MainActivity
            finish() // Finish the ScoreActivity
        }

        nextLevelButton.setOnClickListener {
            finish() // Finish the ScoreActivity
        }
    }

    private fun startConfetti() {
        val konfettiView = findViewById<KonfettiView>(R.id.konfetti_view)
        konfettiView.start(
            Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
            )
        )
    }

    private fun animateAverageTime(avgTime: Float) {
        val avgTimeTextView = findViewById<TextView>(R.id.avg_time_text)
        val animator = ValueAnimator.ofFloat(0f, avgTime)
        animator.duration = 1500 // Animation duration in milliseconds
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            avgTimeTextView.text = "%.1fs".format(animatedValue)
        }
        animator.start()
    }

    private fun saveHighScore(level: Int, score: Int, totalQuestions: Int) {
        val sharedPref = getSharedPreferences("GameProgress", Context.MODE_PRIVATE)
        val highScoreKey = "high_score_level_$level"
        val totalQuestionsKey = "total_questions_level_$level"
        val currentHighScore = sharedPref.getInt(highScoreKey, 0)

        if (score > currentHighScore) {
            with(sharedPref.edit()) {
                putInt(highScoreKey, score)
                putInt(totalQuestionsKey, totalQuestions)
                apply()
            }
        }
    }

    private fun hasNextLevel(currentLevel: Int): Boolean {
        val nextLevelFileName = "questions_level${currentLevel + 1}.json"
        return try {
            assets.open(nextLevelFileName).close() // Check if file exists and can be opened
            true
        } catch (e: IOException) {
            false
        }
    }
}
package com.blackgrapes.smartlineman

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

        val scoreTextView = findViewById<TextView>(R.id.score_text)
        val feedbackEmojiTextView = findViewById<TextView>(R.id.feedback_emoji)
        val wellDoneTextView = findViewById<TextView>(R.id.well_done_text)

        scoreTextView.text = "$score / $totalQuestions"

        val percentage = if (totalQuestions > 0) (score * 100) / totalQuestions else 0

        val (feedbackMessage, emoji) = when {
            percentage == 100 -> "Perfect!" to "🏆"
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
        // We'll assume for now there are only 2 levels.
        val isLastLevel = level >= 2 // You can make this more dynamic if you have more levels

        if (score == totalQuestions && !isLastLevel) {
            nextLevelButton.visibility = View.VISIBLE
            nextLevelButton.setOnClickListener {
                // Start the next level
                val nextLevelIntent = Intent(this, GameActivity::class.java).apply {
                    putExtra(GameActivity.EXTRA_LEVEL, level + 1)
                }
                startActivity(nextLevelIntent)
                finish()
            }
        } else {
            nextLevelButton.visibility = View.GONE
        }




        playAgainButton.setOnClickListener {
            // Create an intent to go back to the MainActivity
            val intent = Intent(this, MainActivity::class.java)
            // Clear the activity stack and start a new task
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Finish the ScoreActivity
        }
    }
}
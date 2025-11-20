package com.blackgrapes.smartlineman

import android.content.Context
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class ChapterScoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chapter_score)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_chapter_score)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val score = intent.getIntExtra(GameActivity.EXTRA_SCORE, 0)
        val totalQuestions = intent.getIntExtra(GameActivity.EXTRA_TOTAL_QUESTIONS, 0)
        val levelId = intent.getStringExtra(ChapterQuizActivity.EXTRA_LEVEL_ID)

        val scoreTextView = findViewById<TextView>(R.id.score_text)
        val feedbackEmojiTextView = findViewById<TextView>(R.id.feedback_emoji)
        val feedbackMessageTextView = findViewById<TextView>(R.id.feedback_message_text)
        val finishButton = findViewById<Button>(R.id.finish_button)

        scoreTextView.text = "$score / $totalQuestions"

        val percentage = if (totalQuestions > 0) (score * 100) / totalQuestions else 0

        val (feedbackMessage, emoji) = when {
            percentage == 100 -> {
                startConfetti()
                if (levelId != null) {
                    saveChapterQuizCompletion(levelId)
                }
                finishButton.text = "Finish"
                finishButton.setOnClickListener {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                "চমৎকার! আপনি এই অধ্যায়টি আয়ত্ত করেছেন। এবার পরের অধ্যায়ে যেতে পারেন।" to "🏆"
            }
            percentage >= 50 -> {
                finishButton.text = "Try Again"
                finishButton.setOnClickListener {
                    if (levelId != null) {
                        val intent = Intent(this, ChapterQuizActivity::class.java)
                        intent.putExtra(ChapterQuizActivity.EXTRA_LEVEL_ID, levelId)
                        startActivity(intent)
                    }
                    finish()
                }
                "ভালো প্রচেষ্টা! অধ্যায়টি আবার পর্যালোচনা করে দেখুন।" to "👍"
            }
            else -> {
                finishButton.text = "Try Again"
                finishButton.setOnClickListener {
                    if (levelId != null) {
                        val intent = Intent(this, ChapterQuizActivity::class.java)
                        intent.putExtra(ChapterQuizActivity.EXTRA_LEVEL_ID, levelId)
                        startActivity(intent)
                    }
                    finish()
                }
                "অধ্যায়টি মনোযোগ দিয়ে আবার পড়ুন এবং পুনরায় চেষ্টা করুন।" to "💪"
            }
        }

        feedbackMessageTextView.text = feedbackMessage
        feedbackEmojiTextView.text = emoji
    }

    private fun saveChapterQuizCompletion(levelId: String) {
        val sharedPref = getSharedPreferences("ChapterProgress", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("chapter_quiz_completed_$levelId", true)
            apply()
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
}
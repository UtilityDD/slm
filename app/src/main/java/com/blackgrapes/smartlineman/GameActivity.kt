package com.blackgrapes.smartlineman

import android.content.Intent
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

data class Question(
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LEVEL = "LEVEL"
        const val EXTRA_SCORE = "SCORE"
        const val EXTRA_TOTAL_QUESTIONS = "TOTAL_QUESTIONS"
    }

    private lateinit var questionCounterText: TextView
    private lateinit var questionText: TextView
    private lateinit var answerButtons: List<Button>
    private lateinit var submitButton: Button
    private lateinit var feedbackIcon: ImageView

    private var currentQuestionIndex = 0
    private var selectedAnswerIndex: Int? = null
    private var score = 0
    private var isAnswerSubmitted = false
    private var level = 1

    private lateinit var questions: List<Question>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_game)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        level = intent.getIntExtra(EXTRA_LEVEL, 1)
        questions = loadQuestionsFromJson(level)

        initializeViews()
        if (questions.isNotEmpty()) {
            setupQuestion()
            setupClickListeners()
        } else {
            Toast.makeText(this, "Failed to load questions for level $level.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        questionCounterText = findViewById(R.id.question_counter_text)
        questionText = findViewById(R.id.question_text)
        submitButton = findViewById(R.id.submit_button)
        answerButtons = listOf(
            findViewById(R.id.answer_button_a),
            findViewById(R.id.answer_button_b),
            findViewById(R.id.answer_button_c),
            findViewById(R.id.answer_button_d)
        )
        feedbackIcon = findViewById(R.id.feedback_icon)
    }

    private fun setupQuestion() {
        isAnswerSubmitted = false
        selectedAnswerIndex = null
        val currentQuestion = questions[currentQuestionIndex]
        questionCounterText.text = "Question ${currentQuestionIndex + 1}/${questions.size}"
        questionText.text = currentQuestion.questionText
        submitButton.visibility = View.INVISIBLE
        feedbackIcon.visibility = View.GONE

        answerButtons.forEachIndexed { index, button ->
            button.text = currentQuestion.options[index]
            resetButtonState(button)
        }
    }

    private fun setupClickListeners() {
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (!isAnswerSubmitted) {
                    checkAnswer(index)
                }
            }
        }

        submitButton.setOnClickListener {
            if (isAnswerSubmitted) {
                // "NEXT QUESTION" was clicked
                currentQuestionIndex++
                if (currentQuestionIndex < questions.size) {
                    setupQuestion()
                } else {
                    // End of the quiz
                    val scoreIntent = Intent(this, ScoreActivity::class.java).apply {
                        putExtra(EXTRA_SCORE, score)
                        putExtra(EXTRA_TOTAL_QUESTIONS, questions.size)
                        putExtra(EXTRA_LEVEL, level)
                    }
                    startActivity(scoreIntent)
                    finish() // Finish GameActivity so it's not on the back stack
                }
            } else {
                // This block is no longer needed as we check the answer on button click
            }
        }
    }

    private fun checkAnswer(index: Int) {
        isAnswerSubmitted = true
        selectedAnswerIndex = index
        val correctIndex = questions[currentQuestionIndex].correctAnswerIndex
        val selectedButton = answerButtons[selectedAnswerIndex!!]

        if (selectedAnswerIndex == correctIndex) {
            score++
            selectedButton.background = ContextCompat.getDrawable(this, R.drawable.answer_button_correct)
            animateFeedback(true)
        } else {
            // Mark the incorrect answer red
            selectedButton.background = ContextCompat.getDrawable(this, R.drawable.answer_button_incorrect)
            selectedButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_animation))

            // Mark the correct answer green
            val correctButton = answerButtons[correctIndex]
            correctButton.background = ContextCompat.getDrawable(this, R.drawable.answer_button_correct)
            animateFeedback(false)
        }

        // Disable all answer buttons after submission
        answerButtons.forEach {
            it.isEnabled = false
            it.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        submitButton.text = "NEXT QUESTION"
        submitButton.visibility = View.VISIBLE
    }

    private fun animateFeedback(isCorrect: Boolean) {
        feedbackIcon.setImageResource(if (isCorrect) R.drawable.ic_correct else R.drawable.ic_incorrect)
        feedbackIcon.visibility = View.VISIBLE

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.scale_fade_in)
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.scale_fade_out)
        fadeOut.startOffset = 600 // Keep the icon visible for a moment

        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                feedbackIcon.startAnimation(fadeOut)
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                feedbackIcon.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        feedbackIcon.startAnimation(fadeIn)
    }

    private fun resetButtonState(button: Button) {
        // Reset to the default outlined button style using our new drawable
        button.background = ContextCompat.getDrawable(this, R.drawable.answer_button_default)
        button.isEnabled = true
        button.setTextColor(ContextCompat.getColor(this, R.color.pearl_white))
    }

    private fun loadQuestionsFromJson(level: Int): List<Question> {
        val questionList = mutableListOf<Question>()
        val fileName = "questions_level$level.json"
        try {
            // A more robust way to read from the assets folder
            val jsonString: String = application.assets.open(fileName).use { inputStream ->
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                String(buffer, Charsets.UTF_8)
            }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val questionObject = jsonArray.getJSONObject(i)
                val questionText = questionObject.getString("questionText")
                val optionsArray = questionObject.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }
                val correctAnswerIndex = questionObject.getInt("correctAnswerIndex")

                questionList.add(Question(questionText, options, correctAnswerIndex))
            }
        } catch (e: IOException) {
            Log.e("GameActivity", "IOException: Error reading $fileName", e)
            // You can optionally show an error message to the user
        } catch (e: JSONException) {
            Log.e("GameActivity", "JSONException: Error parsing $fileName", e)
            // You can optionally show an error message to the user
        }
        return questionList
    }
}

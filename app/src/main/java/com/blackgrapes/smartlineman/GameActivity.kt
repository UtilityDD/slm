package com.blackgrapes.smartlineman

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import android.graphics.BitmapFactory
import org.json.JSONException
import java.io.IOException

data class Question(
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val imageName: String? = null
)

class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LEVEL = "LEVEL"
        const val EXTRA_SCORE = "SCORE"
        const val EXTRA_TOTAL_QUESTIONS = "TOTAL_QUESTIONS"
        const val EXTRA_TOTAL_TIME = "TOTAL_TIME"
    }

    private lateinit var questionCounterText: TextView
    private lateinit var questionText: TextView
    private lateinit var answerButtons: List<Button>
    private lateinit var submitButton: Button
    private lateinit var timerProgressBar: ProgressBar
    private lateinit var questionCard: View
    private lateinit var feedbackIcon: ImageView
    private lateinit var questionImage: ImageView

    private var currentQuestionIndex = 0
    private var selectedAnswerIndex: Int? = null
    private var score = 0
    private var currentCorrectAnswerIndex: Int = 0
    private var isAnswerSubmitted = false
    private var level = 1
    private var totalTimeTakenInMillis: Long = 0
    private var countDownTimer: CountDownTimer? = null
    private val questionTimeInMillis: Long = 15000 // 15 seconds per question


    private lateinit var questions: List<Question>
    private val timerProgressDrawable by lazy { ContextCompat.getDrawable(this, R.drawable.timer_progress_background) }
    private val timerWarningDrawable by lazy { ContextCompat.getDrawable(this, R.drawable.timer_progress_background_warning) }


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
        questions = loadQuestionsFromJson(level).shuffled()

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
        questionCard = findViewById(R.id.question_card)
        answerButtons = listOf(
            findViewById(R.id.answer_button_a),
            findViewById(R.id.answer_button_b),
            findViewById(R.id.answer_button_c),
            findViewById(R.id.answer_button_d)
        )
        feedbackIcon = findViewById(R.id.feedback_icon)
        timerProgressBar = findViewById(R.id.timer_progress_bar)
        questionImage = findViewById(R.id.question_image)
    }

    private fun setupQuestion() {
        isAnswerSubmitted = false
        selectedAnswerIndex = null
        val currentQuestion = questions[currentQuestionIndex]
        questionCounterText.text = "Question ${currentQuestionIndex + 1}/${questions.size}"
        questionText.text = currentQuestion.questionText
        submitButton.visibility = View.INVISIBLE
        feedbackIcon.visibility = View.GONE

        // Handle the question image
        if (currentQuestion.imageName != null) {
            try {
                val inputStream = assets.open(currentQuestion.imageName)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                questionImage.setImageBitmap(bitmap)
                questionImage.visibility = View.VISIBLE
                inputStream.close()
            } catch (e: IOException) {
                Log.e("GameActivity", "Error loading image from assets: ${currentQuestion.imageName}", e)
                questionImage.visibility = View.GONE
            }
        } else {
            questionImage.visibility = View.GONE
        }

        // Get the correct answer text before shuffling
        val correctAnswerText = currentQuestion.options[currentQuestion.correctAnswerIndex]
        // Shuffle the options
        val shuffledOptions = currentQuestion.options.shuffled()
        // Find the new index of the correct answer
        currentCorrectAnswerIndex = shuffledOptions.indexOf(correctAnswerText)

        animateQuestionIn()

        answerButtons.forEachIndexed { index, button ->
            button.text = shuffledOptions[index]
            resetButtonState(button)
        }

        startTimer()
    }
    private fun animateQuestionIn() {
        // Question card slides in from top with a bounce
        questionCard.translationY = -300f
        questionCard.animate()
            .translationY(0f)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .setDuration(600)
            .start()

        // Answer buttons fade in from bottom with a stagger
        answerButtons.forEachIndexed { index, button ->
            button.alpha = 0f
            button.translationY = 100f
            button.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 100).toLong())
                .setDuration(400)
                .start()
        }
    }

    private fun setupClickListeners() {
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                handleAnswerSelection(index)
            }
        }

        submitButton.setOnClickListener {
            countDownTimer?.cancel()
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
                        putExtra(EXTRA_TOTAL_TIME, totalTimeTakenInMillis)
                    }
                    scoreActivityResultLauncher.launch(scoreIntent)
                }
            }
        }
    }

    private fun handleAnswerSelection(index: Int) {
        if (isAnswerSubmitted) {
            return
        }

        countDownTimer?.cancel()
        isAnswerSubmitted = true
        selectedAnswerIndex = index
        val selectedButton = answerButtons[selectedAnswerIndex!!]

        val progress = timerProgressBar.progress
        val timeRemaining = (progress.toLong() * questionTimeInMillis) / 100L
        totalTimeTakenInMillis += (questionTimeInMillis - timeRemaining)

        if (selectedAnswerIndex == currentCorrectAnswerIndex) {
            score++
            selectedButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.correct_green)
            animateFeedback(true)
        } else {
            // Mark the incorrect answer red
            selectedButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.incorrect_red)
            selectedButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_animation))

            // Mark the correct answer green
            val correctButton = answerButtons[currentCorrectAnswerIndex]
            correctButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.correct_green)
            animateFeedback(false)
        }

        // Animate and show the next button
        showNextButton()
        // Disable all answer buttons after submission
        answerButtons.forEach {
            it.isEnabled = false
            it.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        submitButton.text = "NEXT QUESTION"
    }

    private fun showNextButton() {
        submitButton.translationY = 150f
        submitButton.visibility = View.VISIBLE
        submitButton.animate()
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(600) // Delay to allow feedback animation to play
            .start()
    }

    private fun animateFeedback(isCorrect: Boolean) {
        feedbackIcon.setImageResource(if (isCorrect) R.drawable.ic_correct else R.drawable.ic_incorrect)
        feedbackIcon.visibility = View.VISIBLE

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.scale_fade_in)
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.scale_fade_out)
        fadeOut.startOffset = 600 // Keep the icon visible for a moment before fading out
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

    private fun startTimer() {
        timerProgressBar.progress = 100
        countDownTimer = object : CountDownTimer(questionTimeInMillis, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = (millisUntilFinished * 100 / questionTimeInMillis).toInt()
                if (progress < 25) { // Turns orange when 25% time is left
                    timerProgressBar.progressDrawable = timerWarningDrawable
                } else {
                    timerProgressBar.progressDrawable = timerProgressDrawable
                }
                timerProgressBar.progress = progress
            }

            override fun onFinish() {
                timerProgressBar.progress = 0
                handleTimeUp()
            }
        }.start()
    }

    private fun handleTimeUp() {
        isAnswerSubmitted = true
        totalTimeTakenInMillis += questionTimeInMillis
        Toast.makeText(this, "Time's up!", Toast.LENGTH_SHORT).show()
        animateFeedback(false) // Show incorrect feedback
        answerButtons[currentCorrectAnswerIndex].backgroundTintList = ContextCompat.getColorStateList(this, R.color.correct_green)
        answerButtons.forEach {
            it.isEnabled = false
            it.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
        showNextButton()
    }
    private fun resetButtonState(button: Button) {
        // Reset background tint based on the button's ID

        button.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.answer_option_default)
        button.isEnabled = true
        button.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the timer to prevent memory leaks
        countDownTimer?.cancel()
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
                val imageName = questionObject.optString("imageName", null)

                questionList.add(Question(questionText, options, correctAnswerIndex, imageName))
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

    private val scoreActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Level was passed, pass the result back to MainActivity
            setResult(Activity.RESULT_OK, result.data)
        } else {
            // Level was not passed or user hit "Play Again"
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }
}

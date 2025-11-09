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

class ChapterQuizActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LEVEL_ID = "LEVEL_ID"
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
    private var levelId: String? = null
    private var totalTimeTakenInMillis: Long = 0
    private var countDownTimer: CountDownTimer? = null
    private val questionTimeInMillis: Long = 15000 // 15 seconds per question

    private lateinit var questions: List<Question>
    private val timerProgressDrawable by lazy { ContextCompat.getDrawable(this, R.drawable.timer_progress_background) }
    private val timerWarningDrawable by lazy { ContextCompat.getDrawable(this, R.drawable.timer_progress_background_warning) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game) // We can reuse the same layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_game)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        levelId = intent.getStringExtra(EXTRA_LEVEL_ID)
        if (levelId == null) {
            Toast.makeText(this, "Could not load quiz. Level ID is missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        questions = loadQuestionsFromJson(levelId!!).shuffled()

        initializeViews()
        if (questions.isNotEmpty()) {
            setupQuestion()
            setupClickListeners()
        } else {
            Toast.makeText(this, "Failed to load questions for level $levelId.", Toast.LENGTH_LONG).show()
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

        if (currentQuestion.imageName != null) {
            try {
                val inputStream = assets.open(currentQuestion.imageName)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                questionImage.setImageBitmap(bitmap)
                questionImage.visibility = View.VISIBLE
                inputStream.close()
            } catch (e: IOException) {
                Log.e("ChapterQuizActivity", "Error loading image: ${currentQuestion.imageName}", e)
                questionImage.visibility = View.GONE
            }
        } else {
            questionImage.visibility = View.GONE
        }

        val correctAnswerText = currentQuestion.options[currentQuestion.correctAnswerIndex]
        val shuffledOptions = currentQuestion.options.shuffled()
        currentCorrectAnswerIndex = shuffledOptions.indexOf(correctAnswerText)

        animateQuestionIn()

        answerButtons.forEachIndexed { index, button ->
            button.text = shuffledOptions[index]
            resetButtonState(button)
        }

        startTimer()
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
                currentQuestionIndex++
                if (currentQuestionIndex < questions.size) {
                    setupQuestion()
                } else {
                    // End of the quiz, launch ChapterScoreActivity
                    val scoreIntent = Intent(this, ChapterScoreActivity::class.java).apply {
                        putExtra(GameActivity.EXTRA_SCORE, score)
                        putExtra(GameActivity.EXTRA_TOTAL_QUESTIONS, questions.size)
                    }
                    startActivity(scoreIntent)
                    finish() // Finish this quiz activity
                }
            }
        }
    }

    private fun handleAnswerSelection(index: Int) {
        if (isAnswerSubmitted) return

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
            selectedButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.incorrect_red)
            selectedButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_animation))
            val correctButton = answerButtons[currentCorrectAnswerIndex]
            correctButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.correct_green)
            animateFeedback(false)
        }

        showNextButton()
        answerButtons.forEach {
            it.isEnabled = false
            it.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        submitButton.text = "NEXT QUESTION"
    }

    private fun startTimer() {
        timerProgressBar.progress = 100
        countDownTimer = object : CountDownTimer(questionTimeInMillis, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = (millisUntilFinished * 100 / questionTimeInMillis).toInt()
                timerProgressBar.progressDrawable = if (progress < 25) timerWarningDrawable else timerProgressDrawable
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
        animateFeedback(false)
        answerButtons[currentCorrectAnswerIndex].backgroundTintList = ContextCompat.getColorStateList(this, R.color.correct_green)
        answerButtons.forEach {
            it.isEnabled = false
            it.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
        showNextButton()
    }

    private fun loadQuestionsFromJson(levelId: String): List<Question> {
        val fileName = "questions_${levelId.replace('.', '_')}.json"
        val questionList = mutableListOf<Question>()
        try {
            val jsonString: String = application.assets.open(fileName).use { it.reader().readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val questionObject = jsonArray.getJSONObject(i)
                val questionText = questionObject.getString("questionText")
                val optionsArray = questionObject.getJSONArray("options")
                val options = (0 until optionsArray.length()).map { optionsArray.getString(it) }
                val correctAnswerIndex = questionObject.getInt("correctAnswerIndex")
                val imageName = questionObject.optString("imageName", null)

                questionList.add(Question(questionText, options, correctAnswerIndex, imageName))
            }
        } catch (e: IOException) {
            Log.e("ChapterQuizActivity", "IOException: Error reading $fileName", e)
        } catch (e: JSONException) {
            Log.e("ChapterQuizActivity", "JSONException: Error parsing $fileName", e)
        }
        return questionList
    }

    // region Animations and UI Helpers
    private fun animateQuestionIn() {
        questionCard.translationY = -300f
        questionCard.animate().translationY(0f).setInterpolator(android.view.animation.OvershootInterpolator(1.2f)).setDuration(600).start()
        answerButtons.forEachIndexed { index, button ->
            button.alpha = 0f
            button.translationY = 100f
            button.animate().alpha(1f).translationY(0f).setStartDelay((index * 100).toLong()).setDuration(400).start()
        }
    }

    private fun showNextButton() {
        submitButton.translationY = 150f
        submitButton.visibility = View.VISIBLE
        submitButton.animate().translationY(0f).setDuration(400).setStartDelay(600).start()
    }

    private fun animateFeedback(isCorrect: Boolean) {
        feedbackIcon.setImageResource(if (isCorrect) R.drawable.ic_correct else R.drawable.ic_incorrect)
        feedbackIcon.visibility = View.VISIBLE
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.scale_fade_in)
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.scale_fade_out)
        fadeOut.startOffset = 600
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) { feedbackIcon.startAnimation(fadeOut) }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) { feedbackIcon.visibility = View.GONE }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        feedbackIcon.startAnimation(fadeIn)
    }

    private fun resetButtonState(button: Button) {
        button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.answer_option_default)
        button.isEnabled = true
        button.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
    // endregion
}
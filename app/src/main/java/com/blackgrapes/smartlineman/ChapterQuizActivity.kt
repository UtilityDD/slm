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
import android.media.AudioAttributes
import android.media.SoundPool
import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import android.graphics.BitmapFactory
import org.json.JSONException
import java.io.IOException
import com.google.android.material.button.MaterialButton
import android.os.Handler
import android.os.Looper

class ChapterQuizActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LEVEL_ID = "LEVEL_ID"
    }

    private lateinit var questionCounterText: TextView
    private lateinit var scoreText: TextView
    private lateinit var levelText: TextView
    private lateinit var questionText: TextView
    private lateinit var answerButtons: List<com.google.android.material.button.MaterialButton>
    private lateinit var submitButton: com.google.android.material.button.MaterialButton
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
    // Use a different color for the chapter quiz timer
    private val timerWarningDrawable by lazy { ContextCompat.getDrawable(this, R.drawable.timer_progress_background_warning_cyan) }

    // Sound effects
    private lateinit var soundPool: SoundPool
    private var buttonTapSoundId: Int = 0
    private var isSfxMuted = false


    private val chapterScoreResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // When ChapterScoreActivity finishes, pass its result back to ChapterDetailActivity.
        setResult(result.resultCode)
        finish()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game) // We can reuse the same layout

        // Set the custom background for the chapter quiz
        findViewById<View>(R.id.main_game).setBackgroundResource(R.drawable.chapter_quiz_background_gradient)

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

        val allQuestions = loadQuestionsFromJson(levelId!!).shuffled()
        questions = allQuestions.take(10)

        // Load mute preferences
        val prefs = getSharedPreferences("GameSettings", Context.MODE_PRIVATE)
        isSfxMuted = prefs.getBoolean("sfx_muted", false)

        // Initialize SoundPool for sound effects
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        buttonTapSoundId = soundPool.load(this, R.raw.button_tap, 1)


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
        scoreText = findViewById(R.id.score_text)
        levelText = findViewById(R.id.level_text)
        levelText.text = "Chapter $levelId"
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
        questionCounterText.text = "${currentQuestionIndex + 1}/${questions.size}"
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
                    navigateToScoreActivity()
                }
            }
        }
    }

    private fun handleAnswerSelection(index: Int) {
        if (isAnswerSubmitted) return

        playSfx(buttonTapSoundId)

        countDownTimer?.cancel()
        isAnswerSubmitted = true
        selectedAnswerIndex = index
        val selectedButton = answerButtons[selectedAnswerIndex!!]

        val progress = timerProgressBar.progress
        val timeRemaining = (progress.toLong() * questionTimeInMillis) / 100L
        totalTimeTakenInMillis += (questionTimeInMillis - timeRemaining)

        if (selectedAnswerIndex == currentCorrectAnswerIndex) {
            score++
            scoreText.text = "Score: ${score * 10}"
            setButtonState(selectedButton, R.color.correct_green, R.color.white, R.color.correct_green)
            animateFeedback(true)
            
            showNextButton()
            answerButtons.forEach {
                it.isEnabled = false
            }
            
            submitButton.text = "" // Remove text
            val nextDrawable = ContextCompat.getDrawable(this, R.drawable.next)
            submitButton.setCompoundDrawablesWithIntrinsicBounds(nextDrawable, null, null, null)
            // Post a runnable to calculate padding after the button is laid out
            submitButton.post {
                val padding = (submitButton.width - (nextDrawable?.intrinsicWidth ?: 0)) / 2
                submitButton.setPadding(padding, 0, 0, 0)
            }
        } else {
            setButtonState(selectedButton, R.color.incorrect_red, R.color.white, R.color.incorrect_red)
            selectedButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_animation))
            val correctButton = answerButtons[currentCorrectAnswerIndex]
            setButtonState(correctButton, R.color.correct_green, R.color.white, R.color.correct_green)
            animateFeedback(false)
            
            answerButtons.forEach {
                it.isEnabled = false
            }
            
            // Delay and then fail
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToScoreActivity()
            }, 1500)
        }
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
        
        val correctButton = answerButtons[currentCorrectAnswerIndex]
        setButtonState(correctButton, R.color.correct_green, R.color.white, R.color.correct_green)
        
        answerButtons.forEach {
            it.isEnabled = false
        }
        
        // Delay and then fail
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToScoreActivity()
        }, 1500)
    }

    private fun navigateToScoreActivity() {
        val scoreIntent = Intent(this, ChapterScoreActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_SCORE, score)
            putExtra(GameActivity.EXTRA_TOTAL_QUESTIONS, questions.size)
            putExtra(EXTRA_LEVEL_ID, levelId)
        }
        chapterScoreResultLauncher.launch(scoreIntent)
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
    private fun playSfx(soundId: Int) {
        if (!isSfxMuted) {
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

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

    private fun resetButtonState(button: com.google.android.material.button.MaterialButton) {
        // Default state: Outlined, Midnight Blue text/stroke, Transparent background
        button.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.transparent)
        button.strokeColor = ContextCompat.getColorStateList(this, R.color.midnight_blue)
        button.strokeWidth = (1 * resources.displayMetrics.density).toInt() // 1dp
        button.setTextColor(ContextCompat.getColor(this, R.color.midnight_blue))
        button.isEnabled = true
    }

    private fun setButtonState(button: com.google.android.material.button.MaterialButton, backgroundColorRes: Int, textColorRes: Int, strokeColorRes: Int) {
        button.backgroundTintList = ContextCompat.getColorStateList(this, backgroundColorRes)
        button.strokeColor = ContextCompat.getColorStateList(this, strokeColorRes)
        button.setTextColor(ContextCompat.getColor(this, textColorRes))
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
        countDownTimer?.cancel()
    }
    // endregion
}
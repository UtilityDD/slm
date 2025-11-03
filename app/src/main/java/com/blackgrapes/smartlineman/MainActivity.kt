package com.blackgrapes.smartlineman

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Apply padding for the status bar at the top and the navigation bar at the bottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply top padding to the main layout
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            // Apply bottom padding to the footer text to avoid overlap with navigation bar
            findViewById<View>(R.id.footer_text).setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val scrollView = findViewById<NestedScrollView>(R.id.scroll_view)
        val climberImage = findViewById<ImageView>(R.id.climber_image)
        val climberAnimation = climberImage.drawable as AnimationDrawable
        val level1Button = findViewById<Button>(R.id.level_1_button)
        val level2Button = findViewById<Button>(R.id.level_2_button)
        val level3Button = findViewById<Button>(R.id.level_3_button)

        // Apply pulsating animation to level buttons
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_out)
        level1Button.startAnimation(pulseAnimation)
        level2Button.startAnimation(pulseAnimation)
        level3Button.startAnimation(pulseAnimation)

        scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            // Move the climber up or down based on scroll
            climberImage.translationY = -scrollY.toFloat()

            // Start animation on scroll, stop when idle
            if (scrollY != oldScrollY) {
                if (!climberAnimation.isRunning) {
                    climberAnimation.start()
                }
            } else {
                // Optional: A delay could be added here before stopping
                if (climberAnimation.isRunning) {
                    climberAnimation.stop()
                }
            }
        })

        level1Button.setOnClickListener {
            startGame(1)
        }

        level2Button.setOnClickListener {
            startGame(2)
        }
        level3Button.setOnClickListener {
            startGame(3)
        }
    }

    private fun startGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL", level)
        startActivity(intent)
    }
}
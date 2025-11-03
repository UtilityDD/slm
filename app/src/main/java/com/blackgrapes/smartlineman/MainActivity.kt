package com.blackgrapes.smartlineman

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
            // Apply top padding to the header container to avoid overlap with status bar
            findViewById<View>(R.id.header_container).setPadding(0, systemBars.top, 0, 16)
            // Apply bottom padding to the footer text to avoid overlap with navigation bar
            findViewById<View>(R.id.footer_text).setPadding(0, 0, 0, systemBars.bottom + 24)
            insets
        }

        val level1Button = findViewById<View>(R.id.level_1_button)
        val level2Button = findViewById<View>(R.id.level_2_button)
        val level3Button = findViewById<View>(R.id.level_3_button)
        val level4Button = findViewById<View>(R.id.level_4_button)
        val level5Button = findViewById<View>(R.id.level_5_button)
        val level6Button = findViewById<View>(R.id.level_6_button)
        val level7Button = findViewById<View>(R.id.level_7_button)
        val level8Button = findViewById<View>(R.id.level_8_button)
        val level9Button = findViewById<View>(R.id.level_9_button)
        val level10Button = findViewById<View>(R.id.level_10_button)
        val level11Button = findViewById<View>(R.id.level_11_button)
        val level12Button = findViewById<View>(R.id.level_12_button)
        val level13Button = findViewById<View>(R.id.level_13_button)
        val level14Button = findViewById<View>(R.id.level_14_button)

        // Apply pulsating animation to level buttons
        val pulseAnimation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_out)
        val allLevels = listOf(level1Button, level2Button, level3Button, level4Button,
            level5Button, level6Button, level7Button, level8Button, level9Button,
            level10Button, level11Button, level12Button, level13Button, level14Button)

        allLevels.forEach {
            it.startAnimation(pulseAnimation)
        }

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
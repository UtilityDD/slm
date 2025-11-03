package com.blackgrapes.smartlineman

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
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
            // Apply top padding to the main layout
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            // Apply bottom padding to the footer text to avoid overlap with navigation bar
            findViewById<View>(R.id.footer_text).setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Find the emoji ladder and apply the climbing animation
        val emojiLadder = findViewById<LinearLayout>(R.id.emoji_ladder)
        val climbAnimation = AnimationUtils.loadAnimation(this, R.anim.climb_animation)
        emojiLadder.startAnimation(climbAnimation)

        val level1Button = findViewById<Button>(R.id.level_1_button)
        val level2Button = findViewById<Button>(R.id.level_2_button)

        level1Button.setOnClickListener {
            startGame(1)
        }

        level2Button.setOnClickListener {
            startGame(2)
        }
    }

    private fun startGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL", level)
        startActivity(intent)
    }
}
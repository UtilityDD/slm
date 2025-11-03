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

        // Apply pulsating animation to level buttons
        val pulseAnimation: Animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_out)

        for (i in 1..100) {
            val levelId = resources.getIdentifier("level_${i}_button", "id", packageName)
            // Check if the view exists before trying to access it
            if (levelId != 0) {
                val levelButton = findViewById<View>(levelId)
                levelButton?.let {
                    it.startAnimation(pulseAnimation)
                    it.setOnClickListener {
                        startGame(i)
                    }
                }
            }
        }
    }

    private fun startGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL", level)
        startActivity(intent)
    }
}
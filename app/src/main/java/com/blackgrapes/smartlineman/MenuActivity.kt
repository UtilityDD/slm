package com.blackgrapes.smartlineman

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_menu)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set click listeners for each card - for now, they just show a toast.
        findViewById<View>(R.id.card_resources).setOnClickListener { showToast("Resources Clicked") }
        findViewById<View>(R.id.card_market_place).setOnClickListener { showToast("Market Place Clicked") }
        findViewById<View>(R.id.card_update).setOnClickListener { showToast("Update Clicked") }
        findViewById<View>(R.id.card_notice).setOnClickListener { showToast("Notice Clicked") }
        findViewById<View>(R.id.card_help).setOnClickListener { showToast("Help Clicked") }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
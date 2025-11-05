package com.blackgrapes.smartlineman

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
        findViewById<View>(R.id.card_resources).setOnClickListener { startActivity(Intent(this, ResourcesActivity::class.java)) }
        findViewById<View>(R.id.card_market_place).setOnClickListener { showToast("Market Place Clicked") }
        findViewById<View>(R.id.card_update).setOnClickListener { showToast("Update Clicked") }
        findViewById<View>(R.id.card_notice).setOnClickListener { showToast("Notice Clicked") }
        findViewById<View>(R.id.card_help).setOnClickListener { showToast("Help Clicked") }
        findViewById<View>(R.id.card_reset_progress).setOnClickListener { showResetConfirmationDialog() }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Progress")
            .setMessage("Are you sure you want to erase all your scores and progress? This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                // User clicked Reset button
                val sharedPref = getSharedPreferences("GameProgress", Context.MODE_PRIVATE)
                sharedPref.edit().clear().apply()
                showToast("Progress has been reset!")
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_reset)
            .show()
    }
}
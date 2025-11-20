package com.blackgrapes.smartlineman

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

class MarketplaceDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_marketplace_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_marketplace)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        loadItemDetails()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadItemDetails() {
        val name = intent.getStringExtra("item_name") ?: "Unknown Item"
        val description = intent.getStringExtra("item_description") ?: ""
        val price = intent.getStringExtra("item_price") ?: ""
        val category = intent.getStringExtra("item_category") ?: ""
        val standards = intent.getStringExtra("item_standards")
        val contact = intent.getStringExtra("item_contact")

        // Set icon based on category
        val icon = findViewById<TextView>(R.id.equipment_icon)
        icon.text = when (category) {
            "Safety Gear" -> "⛑️"
            "Protective Gloves" -> "🧤"
            "Insulated Tools" -> "🔧"
            "Arc Flash Protection" -> "🦺"
            "Testing Equipment" -> "📊"
            else -> "🛠️"
        }

        findViewById<TextView>(R.id.equipment_name).text = name
        findViewById<TextView>(R.id.equipment_category).text = category
        findViewById<TextView>(R.id.equipment_description).text = description
        findViewById<TextView>(R.id.equipment_price).text = price

        // Handle safety standards
        val standardsLabel = findViewById<TextView>(R.id.standards_label)
        val standardsText = findViewById<TextView>(R.id.equipment_standards)
        if (!standards.isNullOrEmpty()) {
            standardsText.text = standards
            standardsLabel.visibility = View.VISIBLE
            standardsText.visibility = View.VISIBLE
        } else {
            standardsLabel.visibility = View.GONE
            standardsText.visibility = View.GONE
        }

        // Set up contact button
        val contactButton = findViewById<Button>(R.id.contact_supplier_button)
        contactButton.setOnClickListener {
            if (!contact.isNullOrEmpty()) {
                sendEmail(contact, name)
            } else {
                Toast.makeText(this, "Supplier contact not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmail(email: String, itemName: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "Inquiry about $itemName")
            putExtra(Intent.EXTRA_TEXT, "Hello,\n\nI am interested in learning more about the $itemName.\n\nThank you.")
        }
        
        try {
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.blackgrapes.smartlineman

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EquipmentListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_equipment_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_equipment_list)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val recyclerView: RecyclerView = findViewById(R.id.equipment_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val equipmentChapters = listOf(
            Equipment("Transformers (Distribution, Power)", R.drawable.transformer_image),
            Equipment("Circuit Breakers (VCB, SF6)", R.drawable.circuit_breaker_image),
            Equipment("Insulators (Pin, Suspension, Strain)", R.drawable.ic_construction),
            Equipment("Conductors (ACSR, AAAC)", R.drawable.ic_bolt_white),
            Equipment("Lightning Arresters", R.drawable.ic_bolt_white),
            Equipment("Fuses (Dropout, HRC)", R.drawable.ic_safety),
            Equipment("Underground Cables", R.drawable.ic_construction),
            Equipment("Utility Poles (PCC, Steel)", R.drawable.ic_construction),
            Equipment("Hot Sticks", R.drawable.ic_equipment),
            Equipment("Personal Protective Equipment (PPE)", R.drawable.ic_safety),
            Equipment("Bucket Trucks / Aerial Lifts", R.drawable.ic_equipment)
        )

        val adapter = EquipmentListAdapter(equipmentChapters) { equipment ->
            if (equipment.name.startsWith("Transformers")) {
                val intent = Intent(this, ChapterDetailActivity::class.java).apply {
                    putExtra(ChapterDetailActivity.EXTRA_TITLE, "Distribution Transformer")
                    putExtra(ChapterDetailActivity.EXTRA_CONTENT_FILE_NAME, "chapter_transformer.json")
                }
                startActivity(intent)
            } else {
                showToast("${equipment.name} Clicked")
            }
        }
        recyclerView.adapter = adapter
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
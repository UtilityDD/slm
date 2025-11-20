package com.blackgrapes.smartlineman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MarketplaceAdapter(
    private var items: List<MarketplaceItem>,
    private val onItemClick: (MarketplaceItem) -> Unit
) : RecyclerView.Adapter<MarketplaceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.equipment_icon)
        val name: TextView = view.findViewById(R.id.equipment_name)
        val category: TextView = view.findViewById(R.id.equipment_category)
        val description: TextView = view.findViewById(R.id.equipment_description)
        val price: TextView = view.findViewById(R.id.equipment_price)
        val standards: TextView = view.findViewById(R.id.equipment_standards)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marketplace_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        // Set icon based on category
        holder.icon.text = when (item.category) {
            "Safety Gear" -> "⛑️"
            "Protective Gloves" -> "🧤"
            "Insulated Tools" -> "🔧"
            "Arc Flash Protection" -> "🦺"
            "Testing Equipment" -> "📊"
            else -> "🛠️"
        }
        
        holder.name.text = item.name
        holder.category.text = item.category
        holder.description.text = item.description
        holder.price.text = item.priceRange
        
        // Show only first safety standard if available
        if (!item.safetyStandards.isNullOrEmpty()) {
            val firstStandard = item.safetyStandards.split(",").firstOrNull()?.trim()
            holder.standards.text = firstStandard
            holder.standards.visibility = View.VISIBLE
        } else {
            holder.standards.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<MarketplaceItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

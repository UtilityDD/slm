package com.blackgrapes.smartlineman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EquipmentListAdapter(
    private val equipmentList: List<Equipment>,
    private val onItemClick: (Equipment) -> Unit
) : RecyclerView.Adapter<EquipmentListAdapter.EquipmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.equipment_list_item, parent, false)
        return EquipmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: EquipmentViewHolder, position: Int) {
        val equipment = equipmentList[position]
        holder.bind(equipment)
    }

    override fun getItemCount(): Int = equipmentList.size

    inner class EquipmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.equipment_icon)
        private val name: TextView = itemView.findViewById(R.id.equipment_name)

        fun bind(equipment: Equipment) {
            icon.setImageResource(equipment.iconResId)
            name.text = equipment.name
            itemView.setOnClickListener {
                onItemClick(equipment)
            }
        }
    }
}
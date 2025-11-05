package com.blackgrapes.smartlineman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ResourceSectionAdapter(
    private val sections: List<ResourceSection>,
    private val onItemClick: (ResourceSection) -> Unit
) : RecyclerView.Adapter<ResourceSectionAdapter.ResourceSectionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceSectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.resource_section_item, parent, false)
        return ResourceSectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResourceSectionViewHolder, position: Int) {
        val section = sections[position]
        holder.bind(section)
    }

    override fun getItemCount(): Int = sections.size

    inner class ResourceSectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.section_icon)
        private val title: TextView = itemView.findViewById(R.id.section_title)

        fun bind(section: ResourceSection) {
            icon.setImageResource(section.iconResId)
            title.text = section.title
            itemView.setOnClickListener {
                onItemClick(section)
            }
        }
    }
}
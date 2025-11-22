
package com.blackgrapes.smartlineman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ResourceSectionAdapter(
    private var sections: List<ResourceSection>,
    private val markwon: io.noties.markwon.Markwon,
    private val onItemClicked: (ResourceSection) -> Unit
) : RecyclerView.Adapter<ResourceSectionAdapter.ViewHolder>() {

    private val pastelColors = listOf(
        R.color.pastel_lavender, R.color.pastel_baby_blue,
        R.color.pastel_mint, R.color.pastel_peach,
        R.color.pastel_butter, R.color.pastel_sky
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.section_title)
        val summary: TextView = view.findViewById(R.id.section_summary)
        val icon: ImageView = view.findViewById(R.id.section_icon)
        val cardView: CardView = view as CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.resource_section_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val section = sections[position]
        val context = holder.itemView.context
        
        holder.title.text = section.title

        if (!section.summary.isNullOrEmpty()) {
            holder.summary.visibility = View.VISIBLE
            markwon.setMarkdown(holder.summary, section.summary)
        } else {
            holder.summary.visibility = View.GONE
        }
        
        val colorRes = pastelColors[position % pastelColors.size]
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
        
        holder.icon.setImageResource(section.iconResId)
        // Icon tint removed to restore original colors
        holder.icon.clearColorFilter()
        
        holder.itemView.setOnClickListener { onItemClicked(section) }
        
        // Simple fade in animation
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
    }

    override fun getItemCount(): Int = sections.size
    
    fun updateSections(newSections: List<ResourceSection>) {
        sections = newSections
        notifyDataSetChanged()
    }
}
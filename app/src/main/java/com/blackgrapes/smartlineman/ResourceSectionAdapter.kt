package com.blackgrapes.smartlineman

import android.content.Context
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
    private val sections: List<ResourceSection>,
    private val onItemClicked: (ResourceSection) -> Unit
) : RecyclerView.Adapter<ResourceSectionAdapter.ViewHolder>() {

    private lateinit var context: Context
    private val pastelColors by lazy {
        listOf(
            R.color.pastel_lavender, R.color.pastel_baby_blue,
            R.color.pastel_mint, R.color.pastel_peach,
            R.color.pastel_butter, R.color.pastel_sky
        )
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.section_title)
        val icon: ImageView = view.findViewById(R.id.section_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.resource_section_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val section = sections[position]
        holder.title.text = section.title
        val colorRes = pastelColors[position % pastelColors.size]
        (holder.itemView as CardView).setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
        holder.icon.setImageResource(section.iconResId)
        holder.itemView.setOnClickListener { onItemClicked(section) }
        holder.itemView.animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
    }

    override fun getItemCount() = sections.size
}
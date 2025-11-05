package com.blackgrapes.smartlineman

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon

class ChapterSectionAdapter(
    private val sections: List<ChapterSection>,
    private val markwon: Markwon,
    private val onSectionClick: (Int) -> Unit
) : RecyclerView.Adapter<ChapterSectionAdapter.ChapterSectionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterSectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chapter_section_card, parent, false)
        return ChapterSectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterSectionViewHolder, position: Int) {
        val section = sections[position]
        holder.bind(section)
    }

    override fun getItemCount(): Int = sections.size

    inner class ChapterSectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emojiTextView: TextView = itemView.findViewById(R.id.section_emoji)
        private val titleTextView: TextView = itemView.findViewById(R.id.section_title)
        private val contentTextView: TextView = itemView.findViewById(R.id.section_content)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expand_icon)

        init {
            itemView.setOnClickListener {
                onSectionClick(adapterPosition)
            }
        }

        fun bind(section: ChapterSection) {
            emojiTextView.text = section.emoji
            titleTextView.text = section.title

            if (section.isExpanded) {
                contentTextView.visibility = View.VISIBLE
                markwon.setMarkdown(contentTextView, section.content)
                contentTextView.movementMethod = LinkMovementMethod.getInstance()
                expandIcon.setImageResource(R.drawable.ic_arrow_up)
            } else {
                contentTextView.visibility = View.GONE
                expandIcon.setImageResource(R.drawable.ic_arrow_down)
            }
        }
    }
}
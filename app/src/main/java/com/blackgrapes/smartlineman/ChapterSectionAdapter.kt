package com.blackgrapes.smartlineman

import android.graphics.BitmapFactory
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import java.io.IOException

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
        private val sectionImageView: ImageView = itemView.findViewById(R.id.section_image)
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
                contentTextView.movementMethod = LinkMovementMethod.getInstance()
                markwon.setMarkdown(contentTextView, section.summary)
                expandIcon.setImageResource(R.drawable.ic_arrow_up)

                if (section.imageName != null) {
                    try {
                        val inputStream = itemView.context.assets.open(section.imageName)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        sectionImageView.setImageBitmap(bitmap)
                        sectionImageView.visibility = View.VISIBLE
                        inputStream.close()
                    } catch (e: IOException) {
                        Log.e("ChapterAdapter", "Error loading image: ${section.imageName}", e)
                        sectionImageView.visibility = View.GONE
                    }
                } else {
                    sectionImageView.visibility = View.GONE
                }
            } else {
                contentTextView.visibility = View.GONE
                sectionImageView.visibility = View.GONE
                expandIcon.setImageResource(R.drawable.ic_arrow_down)
            }
        }
    }
}
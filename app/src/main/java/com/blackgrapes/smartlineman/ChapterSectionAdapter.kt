package com.blackgrapes.smartlineman

import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import java.io.IOException

class ChapterSectionAdapter(
    private var sections: List<ChapterSection>,
    private val markwon: Markwon,
    private val onSectionClick: (ChapterSection) -> Unit
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

    fun updateSections(newSections: List<ChapterSection>) {
        this.sections = newSections
        notifyDataSetChanged()
    }

    inner class ChapterSectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emojiTextView: TextView = itemView.findViewById(R.id.section_emoji)
        private val titleTextView: TextView = itemView.findViewById(R.id.section_title)
        private val contentTextView: TextView = itemView.findViewById(R.id.section_content)
        private val sectionImageView: ImageView = itemView.findViewById(R.id.section_image)
        private val captionTextView: TextView = itemView.findViewById(R.id.section_image_caption)
        private val cardView: CardView = itemView as CardView
        private val imageContainer: View = itemView.findViewById(R.id.image_container)
        private val linkButton: View = itemView.findViewById(R.id.section_link_button)
        private val chapterNumberBadge: TextView = itemView.findViewById(R.id.chapter_number_badge)

        init {
            itemView.setOnClickListener {
                onSectionClick(sections[adapterPosition])
            }
        }

        fun bind(section: ChapterSection) {
            emojiTextView.text = section.emoji
            markwon.setMarkdown(titleTextView, section.title)

            // Check if the emoji is a number (our serial number)
            if (section.emoji.all { it.isDigit() }) {
                // It's a serial number, let's style it to match the theme.
                emojiTextView.setTextColor(itemView.context.getColor(R.color.purple_700))
            } else {
                // It's a real emoji, reset to default text color in case of view recycling.
                emojiTextView.setTextColor(itemView.context.getColor(android.R.color.black))
            }

            // Display the chapter number badge if it exists
            if (section.levelId != null) {
                chapterNumberBadge.visibility = View.VISIBLE
                chapterNumberBadge.text = section.levelId
            } else {
                chapterNumberBadge.visibility = View.GONE
            }

            // Only show content if there is summary text
            if (section.summary.isNotBlank()) {
                contentTextView.visibility = View.VISIBLE
                markwon.setMarkdown(contentTextView, section.summary)
                // Ensure the text view itself isn't clickable, so the card's listener is used.
                contentTextView.setOnClickListener(null)
                contentTextView.isClickable = false // Redundant but safe
                contentTextView.movementMethod = null // Explicitly remove movement method
            } else {
                contentTextView.visibility = View.GONE
            }

            // Set card background color based on its type
            when {
                section.emoji == "👻" -> { // Myth Buster card
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.myth_buster_bg))
                }
                section.isCompleted -> { // Completed chapter card
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.pastel_mint))
                }
                section.emoji == "🔗" -> { // "Learn More" button
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.purple_200))
                    titleTextView.setTextColor(itemView.context.getColor(R.color.white))
                    emojiTextView.setTextColor(itemView.context.getColor(R.color.white))
                }
                else -> { // Default card color
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.card_bg_light))
                }
            }

            if (section.imageName != null) {
                imageContainer.visibility = View.VISIBLE
                sectionImageView.adjustViewBounds = true
                sectionImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                try {
                    val inputStream = itemView.context.assets.open(section.imageName)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    sectionImageView.setImageBitmap(bitmap)
                    inputStream.close()
                } catch (e: IOException) {
                    Log.e("ChapterAdapter", "Error loading image: ${section.imageName}", e)
                    imageContainer.visibility = View.GONE
                }

                imageContainer.setOnClickListener {
                    val intent = Intent(itemView.context, ImageZoomActivity::class.java).apply {
                        putExtra(ImageZoomActivity.EXTRA_IMAGE_NAME, section.imageName)
                    }
                    itemView.context.startActivity(intent)
                }

                // Handle the caption
                if (section.imageCaption != null) {
                    captionTextView.visibility = View.VISIBLE
                    captionTextView.text = section.imageCaption
                } else {
                    captionTextView.visibility = View.GONE
                }

            } else {
                imageContainer.visibility = View.GONE
                imageContainer.setOnClickListener(null) // Remove listener if no image
            }

            // Handle the source link button
            if (section.sourceLink != null) {
                linkButton.visibility = View.VISIBLE
                linkButton.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(section.sourceLink))
                    itemView.context.startActivity(intent)
                }
            } else {
                linkButton.visibility = View.GONE
                linkButton.setOnClickListener(null)
            }
        }
    }
}
package com.blackgrapes.smartlineman

import android.content.Intent
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

    inner class ChapterSectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emojiTextView: TextView = itemView.findViewById(R.id.section_emoji)
        private val titleTextView: TextView = itemView.findViewById(R.id.section_title)
        private val contentTextView: TextView = itemView.findViewById(R.id.section_content)
        private val sectionImageView: ImageView = itemView.findViewById(R.id.section_image)
        private val imageContainer: View = itemView.findViewById(R.id.image_container)

        init {
            itemView.setOnClickListener {
                onSectionClick(sections[adapterPosition])
            }
        }

        fun bind(section: ChapterSection) {
            emojiTextView.text = section.emoji
            titleTextView.text = section.title

            contentTextView.movementMethod = LinkMovementMethod.getInstance()
            markwon.setMarkdown(contentTextView, section.summary)

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

            } else {
                imageContainer.visibility = View.GONE
                imageContainer.setOnClickListener(null) // Remove listener if no image
            }
        }
    }
}
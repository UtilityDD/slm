package com.blackgrapes.smartlineman

data class ChapterSection(
    val emoji: String,
    val title: String,
    val summary: String,
    val isCompleted: Boolean = false,
    val imageName: String? = null,
    val contentFile: String? = null,
    val imageCaption: String? = null,
    val sourceLink: String? = null
)
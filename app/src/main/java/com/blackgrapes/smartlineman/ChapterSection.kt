package com.blackgrapes.smartlineman

data class ChapterSection(
    val emoji: String,
    val title: String,
    val summary: String,
    var isExpanded: Boolean = false,
    val imageName: String? = null
)
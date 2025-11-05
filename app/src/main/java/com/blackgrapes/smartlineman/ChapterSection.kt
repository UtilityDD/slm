package com.blackgrapes.smartlineman

data class ChapterSection(
    val emoji: String,
    val title: String,
    val content: String,
    var isExpanded: Boolean = false
)
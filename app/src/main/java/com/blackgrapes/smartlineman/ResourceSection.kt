package com.blackgrapes.smartlineman

data class ResourceSection(
    val id: String,
    val title: String,
    val iconResId: Int,
    val contentFile: String?,
    val summary: String? = null
)
package com.blackgrapes.smartlineman

data class MarketplaceItem(
    val id: String,
    val name: String,
    val description: String,
    val priceRange: String,
    val imageName: String?,
    val category: String,
    val safetyStandards: String?,
    val supplierContact: String?
)

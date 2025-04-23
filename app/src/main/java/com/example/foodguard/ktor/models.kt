package com.example.foodguard.ktor

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    val product: Product? = null
)

@Serializable
data class Product(
    val product_name: String? = null,
    val brands: String? = null,
    val expiration_date: String? = null,
    val packaging_date: String? = null,
    val image_url: String? = null,
    val nutriments: Nutriments? = null,
//    val allergens: String? = null
)

@Serializable
data class Nutriments(
    val fat: Double? = null,
    val carbohydrates: Double? = null,
    val proteins: Double? = null
)

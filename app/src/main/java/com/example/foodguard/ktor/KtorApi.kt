package com.example.foodguard.ktor

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*

object KtorApi {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun fetchProduct(barcode: String): Product? {
        val url = "https://world.openfoodfacts.org/api/v0/product/$barcode.json"
        return try {
            val response: ProductResponse = client.get(url).body()
            response.product
        } catch (e: Exception) {
            println("Feil ved henting: ${e.localizedMessage}")
            null
        }
    }
}

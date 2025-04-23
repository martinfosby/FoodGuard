package com.example.foodguard.ktor

import android.util.Log
import com.example.foodguard.room.ScannedItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*

object KtorApi {
    private const val AI_API_URL = "https://api.openai.com/v1/chat/completions"
    private const val AI_API_KEY = com.example.foodguard.BuildConfig.OPENAI_API_KEY

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun fetchProduct(scannedItem: ScannedItem?): Product? {
        val url = "https://world.openfoodfacts.org/api/v2/product/${scannedItem?.barcode}"
        return try {
            val response: ProductResponse = client.get(url).body()
            response.product
        } catch (e: Exception) {
            println("Feil ved henting: ${e.localizedMessage}")
            null
        }
    }


    suspend fun chatWithAI(prompt: String): String {
        val request = AIRequest(
            model = "gpt-4o-mini",
            messages = listOf(Message(role = "user", content = prompt))
        )

        return try {
            val response: AIResponse = client.post(AI_API_URL) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $AI_API_KEY")
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(request)
            }.body()
            response.choices.firstOrNull()?.message?.content ?: "No content"

        } catch (e: ClientRequestException) { // 4xx errors
            val errorBody = e.response.bodyAsText()
            Log.e("KtorApi", "Client error: $errorBody")
            "Client error: ${e.localizedMessage}"

        } catch (e: ServerResponseException) { // 5xx errors
            val errorBody = e.response.bodyAsText()
            Log.e("KtorApi", "Server error: $errorBody")
            "Server error: ${e.localizedMessage}"

        } catch (e: ResponseException) { // Generic HTTP error
            val errorBody = e.response.bodyAsText()
            Log.e("KtorApi", "HTTP error: $errorBody")
            "HTTP error: ${e.localizedMessage}"

        } catch (e: Exception) {
            // Log error or handle accordingly
            Log.e("KtorApi","Error interacting with AI: ${e.localizedMessage}")
            "Error occurred: ${e.localizedMessage}"
        }

    }

}


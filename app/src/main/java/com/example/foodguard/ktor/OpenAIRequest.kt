package com.example.foodguard.ktor

@kotlinx.serialization.Serializable
data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val temperature: Double = 0.7
)

@kotlinx.serialization.Serializable
data class Message(
    val role: String, // "user", "assistant", "system"
    val content: String
)

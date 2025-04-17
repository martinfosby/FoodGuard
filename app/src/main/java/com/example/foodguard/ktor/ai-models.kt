package com.example.foodguard.ktor
import kotlinx.serialization.Serializable

@Serializable
data class AIRequest(
    val model: String,
    val messages: List<Message>
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class AIResponse(
    val choices: List<Choice>,
)

@Serializable
data class ErrorResponse(
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val message: String,
    val type: String
)


@Serializable
data class Choice(
    val message: Message
)

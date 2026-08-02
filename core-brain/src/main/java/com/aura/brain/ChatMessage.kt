package com.aura.brain

data class ChatMessage(
    val role: String, // "user" | "assistant"
    val content: String
)

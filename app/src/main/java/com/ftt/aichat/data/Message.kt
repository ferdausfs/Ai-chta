package com.ftt.aichat.data

import java.util.UUID

/**
 * Represents a single chat message.
 * role: "user" or "assistant"
 * isStreaming: true while AI is still generating the response
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}

/** Available Anthropic models with display names */
data class ModelOption(
    val id: String,
    val displayName: String,
    val description: String
)

val AVAILABLE_MODELS = listOf(
    ModelOption(
        id = "claude-opus-4-6",
        displayName = "Claude Opus 4.6",
        description = "Most powerful — best for complex tasks"
    ),
    ModelOption(
        id = "claude-sonnet-4-6",
        displayName = "Claude Sonnet 4.6",
        description = "Smart & fast — best for everyday use"
    ),
    ModelOption(
        id = "claude-haiku-4-5-20251001",
        displayName = "Claude Haiku 4.5",
        description = "Fastest & cheapest — quick tasks"
    ),
    ModelOption(
        id = "claude-opus-4-5",
        displayName = "Claude Opus 4.5",
        description = "Previous gen powerful model"
    ),
    ModelOption(
        id = "claude-sonnet-4-5",
        displayName = "Claude Sonnet 4.5",
        description = "Previous gen balanced model"
    )
)

val DEFAULT_MODEL = AVAILABLE_MODELS[1] // Sonnet 4.6

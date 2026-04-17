package com.peihua.genai.primitives

import com.peihua.genai.primitives.parts.Parts
import com.peihua.genai.primitives.parts.ToolPart

data class ChatMessage(
    val role: ChatMessageRole,
    val parts: Parts = Parts(),
    val metadata: Map<String, Any> = emptyMap(),
    val finishStatus: FinishStatus? = null,
) {

    val text: String
        get() = parts.text

    /**
     * Whether this message contains any tool calls.
     */
    val hasToolCalls: Boolean
        get() = parts.toolCalls.isNotEmpty();

    /**
     * Gets all tool calls in this message.
     */
    val toolCalls: List<ToolPart> get() = parts.toolCalls;

    /**
     * Whether this message contains any tool results.
     */
    val hasToolResults: Boolean get() = parts.toolResults.isNotEmpty();

    /**
     * Gets all tool results in this message.
     */
    val toolResults: List<ToolPart> get() = parts.toolResults

    /**
     * Concatenates this message with another message.
     *
     * Throws [IllegalArgumentException] if:
     * - Roles are different.
     * - Finish statuses are both not null and different.
     * - Metadata sets are different.
     */
    fun concatenate(other: ChatMessage): ChatMessage {
        if (role != other.role) {
            throw IllegalArgumentException("Roles must match for concatenation")
        }

        if (finishStatus != null && other.finishStatus != null && finishStatus != other.finishStatus) {
            throw IllegalArgumentException("Finish statuses must match for concatenation")
        }

        if (metadata != other.metadata) {
            throw IllegalArgumentException("Metadata sets should be equal, but found $metadata and ${other.metadata}")
        }
        return copy(parts = parts.addAll(other.parts), finishStatus = finishStatus ?: other.finishStatus)
    }
}

/** The role of a message author.
 *
 * The role indicates the source of the message or the intended perspective.
 * For example, a system message is sent to the model to set context,
 * a user message is sent to the model as a request,
 * and a model message is a response to the user request. */
enum class ChatMessageRole {
    /** A message from the system that sets context or instructions for the model.
     *
     * System messages are typically sent to the model to define its behavior
     * or persona ("system prompt"). They are not usually shown to the end user. */
    system,

    /** A message from the end user to the model ("user prompt"). */
    user,

    /** A message from the model to the user ("model response"). */
    model,
}

package com.peihua.genui.a2a.core

import kotlinx.serialization.Serializable

@Serializable
data class AgentCapabilities(
    /**
     * Indicates if the agent supports streaming responses, typically via
     * Server-Sent Events (SSE).
     *
     * A value of `true` means the client can use methods like `message/stream`.
     **/
    val streaming: Boolean,
    /**
     * Indicates if the agent supports sending push notifications for
     * asynchronous task updates to a client-specified endpoint.
     **/
    val pushNotifications: Boolean,
    /**
     * Indicates if the agent maintains and can provide a history of state
     * transitions for tasks.
     **/
    val stateTransitionHistory: Boolean,
    /**
     * A list of non-standard protocol extensions supported by the agent.
     *
     * See [AgentExtension] for more details.
     **/
    val extensions: List<AgentExtension>?,
) {
}
package com.peihua.genui.a2a.core

import kotlinx.serialization.Serializable

@Serializable
data class AgentExtension(
    /**
     * The unique URI identifying the extension.
     **/
    val uri: String,
    /**
     * A human-readable description of the extension.
     **/
    val description: String?,
    /**
     * If true, the client must understand and comply with the extension's
     * requirements to interact with the agent.
     **/
    val required: Boolean,
    /**
     * Optional, extension-specific configuration parameters.
     **/
    val params: Map<String, Any?>?,
) {
}
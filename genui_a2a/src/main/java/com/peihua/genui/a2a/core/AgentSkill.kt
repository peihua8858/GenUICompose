package com.peihua.genui.a2a.core

import kotlinx.serialization.Serializable

@Serializable
data class AgentSkill(
    /** A unique identifier for the agent's skill (e.g., "weather-forecast").**/
    val id: String,

    /** A human-readable name for the skill (e.g., "Weather Forecast").**/
    val name: String,

    /**  A detailed description of the skill, intended to help clients or users
     * understand its purpose and functionality.**/
    val description: String,

    /** A set of keywords describing the skill's capabilities.**/
    val tags: List<String>,

    /** Example prompts or scenarios that this skill can handle, providing a
     * hint to the client on how to use the skill.**/
    val examples: List<String>?,

    /** The set of supported input MIME types for this skill, overriding the
     * agent's defaults.**/
    val inputModes: List<String>?,

    /** The set of supported output MIME types for this skill, overriding the
     * agent's defaults.**/
    val outputModes: List<String>?,

    /** Security schemes necessary for the agent to leverage this skill.**/
    val security: List<Map<String, List<String>>>?,
) {
}
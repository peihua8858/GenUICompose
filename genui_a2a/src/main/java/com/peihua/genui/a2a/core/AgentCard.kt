package com.peihua.genui.a2a.core

import com.peihua.genui.a2a.a2aJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class AgentCard(
    /**
     * The version of the A2A protocol that this agent implements.
     *
     * Example: "0.1.0".
     **/
    val protocolVersion: String,
    /**
     * A human-readable name for the agent.
     *
     * Example: "Recipe Assistant".
     **/
    val name: String,
    /**
     * A concise, human-readable description of the agent's purpose and
     * functionality.
     **/
    val description: String,
    /**
     * The primary endpoint URL for interacting with the agent.
     **/
    val url: String,
    /**
     * The transport protocol used by the primary endpoint specified in [url].
     *
     * Defaults to [TransportProtocol.JSONRPC] if not specified.
     **/
    val preferredTransport: TransportProtocol?,
    /**
     * A list of alternative interfaces the agent supports.
     *
     * This allows an agent to expose its API via multiple transport protocols
     * or at different URLs.
     **/
    val additionalInterfaces: List<AgentInterface>?,
    /**
     * An optional URL pointing to an icon representing the agent.
     **/
    var iconUrl: String?,
    /**
     * Information about the entity providing the agent service.
     **/
    var provider: AgentProvider?,
    /**
     * The version string of the agent implementation itself.
     *
     * The format is specific to the agent provider.
     **/
    val version: String,
    /**
     * An optional URL pointing to human-readable documentation for the agent.
     **/
    var documentationUrl: String?,
    /**
     * A declaration of optional A2A protocol features and extensions
     * supported by the agent.
     **/
    val capabilities: AgentCapabilities,
    /**
     * A map of security schemes supported by the agent for authorization.
     *
     * The keys are scheme names (e.g., "apiKey", "bearerAuth") which can be
     * referenced in security requirements. The values define the scheme
     * details, following the OpenAPI 3.0 Security Scheme Object structure.
     **/
    val securitySchemes: Map<String,SecurityScheme>?,
    /**
     * A list of security requirements that apply globally to all interactions
     * with this agent, unless overridden by a specific skill or method.
     *
     * Each item in the list is a map representing a disjunction (OR) of
     * security schemes. Within each map, the keys are scheme names from
     * [securitySchemes], and the values are lists of required scopes (AND).
     **/
    val security: List<Map<String, List<String>>>?,
    /**
     * Default set of supported input MIME types (e.g., "text/plain") for all
     * skills.
     *
     * This can be overridden on a per-skill basis in [AgentSkill].
     **/
    val defaultInputModes: List<String>,
    /**
     * Default set of supported output MIME types (e.g., "application/json") for
     * all skills.
     *
     * This can be overridden on a per-skill basis in [AgentSkill].
     **/
    val defaultOutputModes: List<String>,
    /**
     * The set of skills (distinct functionalities) that the agent can perform.
     **/
    val skills: List<AgentSkill>,
    /**
     * Indicates whether the agent can provide an extended agent card with
     * potentially more details to authenticated users.
     *
     * Defaults to `false` if not specified.
     **/
    val supportsAuthenticatedExtendedCard: Boolean,
) {
    companion object {
        fun fromJson(json: JsonElement): AgentCard {
            return a2aJson.decodeFromJsonElement(json)
        }
    }
}

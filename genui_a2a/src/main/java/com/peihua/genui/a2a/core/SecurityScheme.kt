package com.peihua.genui.a2a.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface SecurityScheme {
    /** The type discriminator, always 'apiKey'.**/
    val type: String

    /** An optional description of the API key security scheme.**/
    val description: String?

    companion object {
        /**
         *  Represents an API key-based security scheme.
         */
        fun apiKey(
            // The type discriminator, always 'apiKey'.
            type: String = "apiKey",
            // An optional description of the API key security scheme.
            description: String? = null,
            // The name of the header, query, or cookie parameter used to transmit
            // the API key.
            name: String,
            // Specifies the location of the API key.
            //
            // Valid values are "query", "header", or "cookie".
            _in: String,
        ) = APIKeySecurityScheme(type, description, name, _in)

        /**
         * Represents an HTTP authentication scheme (e.g., Basic, Bearer).
         */
        fun http(
            // The type discriminator, always 'http'.
            type: String = "http",
            // An optional description of the HTTP security scheme.
            description: String? = null,
            // The name of the HTTP Authorization scheme, e.g., "Bearer", "Basic".
            //
            // Values should be registered in the IANA "Hypertext Transfer Protocol
            // (HTTP) Authentication Scheme Registry".
            scheme: String,
            // An optional hint about the format of the bearer token (e.g., "JWT").
            //
            // Only relevant when `scheme` is "Bearer".
            bearerFormat: String? = null,
        ) = HttpAuthSecurityScheme(type, description, scheme, bearerFormat)

        /**
         * epresents an OAuth 2.0 security scheme.
         */
        fun oauth2(
            // The type discriminator, always 'oauth2'.
            type: String = "oauth2",
            // An optional description of the OAuth 2.0 security scheme.
            description: String? = null,
            // Configuration details for the supported OAuth 2.0 flows.
            flows: OAuthFlows,
        ) = OAuth2SecurityScheme(type, description, flows)

        /**
         * Represents an OpenID Connect security scheme.
         */
        fun openIdConnect(
            // The type discriminator, always 'openIdConnect'.
            type: String = "openIdConnect",
            //An optional description of the OpenID Connect security scheme.
            description: String? = null,
            //The OpenID Connect Discovery URL (e.g., ending in `.well-known/openid-configuration`).
            openIdConnectUrl: String,
        ) = OpenIdConnectSecurityScheme(type, description, openIdConnectUrl)

        /**
         * Represents a mutual TLS authentication scheme.
         */
        fun mutualTls(
            //The type discriminator, always 'mutualTls'.
            type: String = "mutualTls",
            //An optional description of the mutual TLS security scheme.
            description: String? = null,
        ) = MutualTlsSecurityScheme(type, description)

//        /**
//         *  Deserializes a [SecurityScheme] instance from a JSON object.
//         */
//        fun fromJson(json: JsonObject) = _SecuritySchemeFromJson(json);
    }
}
//
//fun _SecuritySchemeFromJson(json: JsonObject): SecurityScheme {
//    val type = json["type"].toString()
//    return when (type) {
//        "apiKey" -> Json.decodeFromJsonElement<APIKeySecurityScheme>(json) //APIKeySecurityScheme.fromJson(json)
//        "http" ->  Json.decodeFromJsonElement<HttpAuthSecurityScheme>(json)//HttpAuthSecurityScheme.fromJson(json)
//        "oauth2" -> Json.decodeFromJsonElement<OAuth2SecurityScheme>(json)//OAuth2SecurityScheme.fromJson(json)
//        "openIdConnect" -> Json.decodeFromJsonElement<OpenIdConnectSecurityScheme>(json)//OpenIdConnectSecurityScheme.fromJson(json)
//        "mutualTls" -> Json.decodeFromJsonElement<MutualTlsSecurityScheme>(json)//MutualTlsSecurityScheme.fromJson(json)
//        else -> throw Exception("Invalid union type \"${json["type"]}")
//    }
//}

/**
 *  Container for the OAuth 2.0 flows supported by a [SecurityScheme.oauth2].
 *
 *  Each property represents a different OAuth 2.0 grant type.
 */
@Serializable
data class OAuthFlows(
    val implicit: OAuthFlow,
    val password: OAuthFlow,
    val clientCredentials: OAuthFlow,
    val authorizationCode: OAuthFlow,
)

/** Configuration details for a single OAuth 2.0 flow. **/
@Serializable
data class OAuthFlow(
    val authorizationUrl: String,
    val tokenUrl: String,
    val refreshUrl: String,
    val scopes: Map<String, String>,
)

@Serializable
@SerialName("apiKey")
data class APIKeySecurityScheme(
    override val type: String,
    override val description: String?,
    /** The name of the header, query, or cookie parameter used to transmit the API key. */
    val name: String,
    /** Specifies the location of the API key. Valid values are "query", "header", or "cookie". */
    @SerialName("in")
    val _in: String,
) : SecurityScheme
@Serializable
@SerialName("http")
data class HttpAuthSecurityScheme(
    override val type: String,
    override val description: String?,
    /** The name of the header, query, or cookie parameter used to transmit the API key. */
    val scheme: String,
    /** Specifies the location of the API key. Valid values are "query", "header", or "cookie". */
    val bearerFormat: String?,
) : SecurityScheme
@Serializable
@SerialName("oauth2")
data class OAuth2SecurityScheme(
    override val type: String,
    override val description: String?,
    /** The name of the header, query, or cookie parameter used to transmit the API key. */
    val flows: OAuthFlows,
) : SecurityScheme
@Serializable
@SerialName("openIdConnect")
data class OpenIdConnectSecurityScheme(
    override val type: String,
    override val description: String?,
    /** The name of the header, query, or cookie parameter used to transmit the API key. */
    val openIdConnectUrl: String,
) : SecurityScheme
@Serializable
@SerialName("mutualTls")
data class MutualTlsSecurityScheme(
    override val type: String,
    override val description: String?,
) : SecurityScheme
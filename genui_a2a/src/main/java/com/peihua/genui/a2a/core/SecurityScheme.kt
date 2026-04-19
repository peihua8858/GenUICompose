package com.peihua.genui.a2a.core

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

@Serializable(with = SecuritySchemeSerializer::class)
sealed interface SecurityScheme {
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


class SecuritySchemeSerializer : KSerializer<SecurityScheme> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("com.peihua.genui.a2a.core.SecurityScheme") {
            element("type", serialDescriptor<String>())
            element("description", serialDescriptor<String>())
            element("name", serialDescriptor<String>())
            element("in", serialDescriptor<String>())
            element("scheme", serialDescriptor<String>())
            element("bearerFormat", serialDescriptor<String>())
            element("openIdConnectUrl", serialDescriptor<String>())
            element("flows", serialDescriptor<OAuthFlows>())
        }

    override fun serialize(encoder: Encoder, value: SecurityScheme) {
        when (value.type) {
            "apiKey" -> encoder.encodeSerializableValue(
                APIKeySecurityScheme.serializer(),
                value as APIKeySecurityScheme
            )

            "http" -> encoder.encodeSerializableValue(
                HttpAuthSecurityScheme.serializer(),
                value as HttpAuthSecurityScheme
            )

            "oauth2" -> encoder.encodeSerializableValue(
                OAuth2SecurityScheme.serializer(),
                value as OAuth2SecurityScheme
            )

            "openIdConnect" -> encoder.encodeSerializableValue(
                OpenIdConnectSecurityScheme.serializer(),
                value as OpenIdConnectSecurityScheme
            )

            "mutualTls" -> encoder.encodeSerializableValue(
                MutualTlsSecurityScheme.serializer(),
                value as MutualTlsSecurityScheme
            )
        }
    }

    override fun deserialize(decoder: Decoder): SecurityScheme =
        decoder.decodeStructure(descriptor) {
            var type: String = ""
            var description: String? = null
            var name: String = ""
            var _in: String = ""
            var scheme: String = ""
            var bearerFormat: String? = null
            var openIdConnectUrl: String = ""
            var flows: OAuthFlows? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> type = decodeStringElement(descriptor, 0)
                    1 -> description = decodeStringElement(descriptor, 1)
                    2 -> name = decodeStringElement(descriptor, 2)
                    3 -> _in = decodeStringElement(descriptor, 3)
                    4 -> scheme = decodeStringElement(descriptor, 4)
                    5 -> bearerFormat = decodeStringElement(descriptor, 5)
                    6 -> openIdConnectUrl = decodeStringElement(descriptor, 6)
                    7 -> flows = decodeSerializableElement(descriptor, 7, OAuthFlows.serializer())
                    CompositeDecoder.DECODE_DONE -> break
                    CompositeDecoder.UNKNOWN_NAME -> {
                        decodeUnknownElement(descriptor, index)
                        continue
                    }
                    else -> error("Unexpected index: $index")
                }
            }
            when (type) {
                "apiKey" -> APIKeySecurityScheme(type, description, name, _in)
                "http" -> HttpAuthSecurityScheme(type, description, scheme, bearerFormat)
                "oauth2" -> OAuth2SecurityScheme(type, description, flows!!)
                "openIdConnect" -> OpenIdConnectSecurityScheme(type, description, openIdConnectUrl)
                "mutualTls" -> MutualTlsSecurityScheme(type, description)
                else -> error("Unknown type: $type")
            }
        }
}

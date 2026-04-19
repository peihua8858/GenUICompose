package com.peihua.genui.model

import com.peihua.genui.ILogger
import com.peihua.genui.Logger
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.primitives.surfaceIdKey
import com.peihua.json.utils.toJsonObject
import com.peihua.json.AnyValueSerializer
import com.peihua.json.schema.S
import com.peihua.json.schema.Schema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

sealed interface A2uiMessage {
    fun toJson(): JsonElement

    companion object {
        /** Creates an [A2uiMessage] from a JSON map.*/
        fun fromJson(json: JsonObject): A2uiMessage {
            try {
                val version = json["version"].toString()
                if (version != "v0.9") {
                    throw A2uiValidationException(
                        "A2UI message must have version \"v0.9\"",
                        json = json,
                    );
                }
                val createSurface = json["createSurface"] as? JsonObject
                if (createSurface != null) {
                    try {
                        return CreateSurface.fromJson(createSurface);
                    } catch (e: Exception) {
                        throw A2uiValidationException(
                            "Failed to parse CreateSurface message",
                            json = json,
                            cause = e,
                        );
                    }
                }
                val updateComponents = json["updateComponents"] as? JsonObject
                if (updateComponents != null) {
                    try {
                        return UpdateComponents.fromJson(updateComponents);
                    } catch (e: Exception) {
                        throw A2uiValidationException(
                            "Failed to parse UpdateComponents message",
                            json = json,
                            cause = e,
                        );
                    }
                }
                val updateDataModel = json["updateDataModel"] as? JsonObject
                if (updateDataModel != null) {
                    try {
                        return UpdateDataModel.fromJson(updateDataModel);
                    } catch (e: Exception) {
                        throw A2uiValidationException(
                            "Failed to parse UpdateDataModel message",
                            json = json,
                            cause = e,
                        );
                    }
                }
                val deleteSurface = json["deleteSurface"] as? JsonObject
                if (deleteSurface != null) {
                    try {
                        return DeleteSurface.fromJson(deleteSurface);
                    } catch (e: Exception) {
                        throw A2uiValidationException(
                            "Failed to parse DeleteSurface message",
                            json = json,
                            cause = e,
                        );
                    }
                }
            } catch (e: Exception) {
                Logger.eLog("Failed to parse A2UI message from JSON: $json", e)
                throw e
            }
            throw A2uiValidationException("Unknown A2UI message type: ${json.keys}", json = json)
        }

        /**
         * Returns the JSON schema for an A2UI message.
         */
        fun a2uiMessageSchema(catalog: Catalog): Schema {
            return S.combined(
                allOf = listOf(
                    S.obj(
                        title = "A2UI Message Schema",
                        description = "Describes a JSON payload for an A2UI (Agent to UI) message. A message MUST contain exactly ONE of the action properties.",
                        properties = mapOf(
                            "version" to S.string(constValue = "v0.9"),
                            "createSurface" to A2uiSchemas.createSurfaceSchema(),
                            "updateComponents" to A2uiSchemas.updateComponentsSchema(catalog),
                            "updateDataModel" to A2uiSchemas.updateDataModelSchema(),
                            "deleteSurface" to A2uiSchemas.deleteSurfaceSchema(),
                        ),
                        required = listOf("version"),
                    ),
                ),
                anyOf = listOf(
                    mapOf(
                        "required" to listOf("createSurface")
                    ),
                    mapOf(
                        "required" to listOf("updateComponents")
                    ),
                    mapOf(
                        "required" to listOf("updateDataModel")
                    ),
                    mapOf(
                        "required" to listOf("deleteSurface")
                    ),
                ),
            )
        }
    }
}

@Serializable
data class CreateSurface(
    // The ID of the surface that this message applies to.
    val surfaceId: String,

    // The ID of the catalog to use for rendering this surface.
    val catalogId: String,

    // The theme parameters for this surface.
    val theme: JsonMap?,

    // If true, the client sends the full data model in A2A metadata.
    val sendDataModel: Boolean = false,
) : A2uiMessage {
    override fun toJson(): JsonElement {
        return this.toJsonObject()
    }

    companion object {
        fun fromJson(json: JsonObject): CreateSurface {
            return CreateSurface(
                surfaceId = json[surfaceIdKey] as String,
                catalogId = json["catalogId"] as String,
                theme = json["theme"] as JsonMap?,
                sendDataModel = json["sendDataModel"] as Boolean? ?: false,
            );
        }
    }
}

@Serializable
data class UpdateComponents(
    // The ID of the surface that this message applies to.
    val surfaceId: String,

    // The list of components to add or update.
    val components: List<Component>
) : A2uiMessage {
    override fun toJson(): JsonElement {
        return this.toJsonObject()
    }

    companion object {
        /// Creates a [UpdateComponents] message from a JSON map.
        fun fromJson(json: JsonObject): UpdateComponents {
            return UpdateComponents(
                surfaceId = json[surfaceIdKey] as String,
                components = (json["components"] as List<*>)
                    .map { Component.fromJson(it.toJsonObject()) }
                    .toList(),
            )
        }
    }
}

@Serializable
data class UpdateDataModel(
    // The ID of the surface that this message applies to.
    val surfaceId: String,

    // The path in the data model to update. Defaults to root '/'.
    val path: DataPath,

    // The new value to write to the data model.
    // If null (and the key is present in the JSON), it implies deletion of the
    // key at the path.
    @Serializable(with = AnyValueSerializer::class) val value: Any? = null,

    ) : A2uiMessage {
    override fun toJson(): JsonElement {
        return this.toJsonObject()
    }

    companion object {
        /**
         * Creates a [UpdateDataModel] message from a JSON map.
         */
        fun fromJson(json: JsonObject): UpdateDataModel {
            return UpdateDataModel(
                surfaceId = json[surfaceIdKey] as String,
                path = DataPath(json["path"] as? String ?: "/"),
                value = json["value"],
            );
        }
    }
}

@Serializable
data class DeleteSurface(
    // The ID of the surface that this message applies to.
    val surfaceId: String
) : A2uiMessage {
    override fun toJson(): JsonElement {
        return this.toJsonObject()
    }

    companion object {
        /**
         * Creates a [DeleteSurface] message from a JSON map.
         */
        fun fromJson(json: JsonObject): DeleteSurface {
            return DeleteSurface(surfaceId = json[surfaceIdKey] as String);
        }
    }
}
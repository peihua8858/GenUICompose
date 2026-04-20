package com.peihua.genui.model.parts

import com.peihua.genai.primitives.parts.DataPart
import com.peihua.genai.primitives.parts.Part
import com.peihua.genai.primitives.parts.StandardPart
import com.peihua.genui.model.SurfaceDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

object JsonKey {
    const val definition = "definition";
    const val surfaceId = "surfaceId";
    const val interaction = "interaction"
}

/// Constants for UI related parts.
object UiPartConstants {
    /// MIME type for UI definition parts.
    const val uiMimeType = "application/vnd.genui.ui+json";

    /// MIME type for UI interaction parts.
    const val interactionMimeType = "application/vnd.genui.interaction+json";
}

//Whether this part is a UI part.
val Part.isUiPart: Boolean
    get() {
        if (this is DataPart) {
            return this.mimeType == UiPartConstants.uiMimeType
        }
        return false
    }

val Part.isUiInteractionPart: Boolean
    get() {
        if (this is DataPart) {
            return this.mimeType == UiPartConstants.interactionMimeType
        }
        return false
    }
val Part.asUiPart: UiPart?
    get() {
        if (!isUiPart) return null
        return UiPart.fromDataPart(this as DataPart)
    }
val Part.asUiInteractionPart: UiInteractionPart?
    get() {
        if (!isUiInteractionPart) return null
        return UiInteractionPart.fromDataPart(this as DataPart)
    }

/**
 * Filters the list for UI parts and returns them as [UiPart] views.
 */
val Iterable<StandardPart>.uiParts: Iterable<UiPart>
    get() = this.filter { it.isUiPart }.mapNotNull { it.asUiPart }

/**
 * Filters the list for UI interaction parts.
 */
val Iterable<StandardPart>.uiInteractionParts: Iterable<UiInteractionPart>
    get() = this.filter { it.isUiInteractionPart }.mapNotNull { it.asUiInteractionPart }

@Serializable
class UiPart private constructor(
    // The JSON definition of the UI.
    val definition: SurfaceDefinition,
    // The unique ID for this UI surface.
    private val surfaceId: String?,
) {
    companion object {
        /// Creates a view from a [DataPart].
        fun fromDataPart(part: DataPart): UiPart {
            if (part.mimeType != UiPartConstants.uiMimeType) {
                throw IllegalArgumentException("Part is not a UI part")
            }
            val uiPart = Json.decodeFromString<UiPart>(String(part.bytes))
            return UiPart(definition = uiPart.definition, surfaceId = uiPart.surfaceId)
        }
    }
}

@Serializable
class UiInteractionPart private constructor(
    //The interaction data (JSON string).
    val interaction: String,
) {
    companion object {
        /// Creates a [DataPart] representing a UI interaction.
        fun create(interaction: String): DataPart {
            val json = buildJsonObject {
                put(JsonKey.interaction, JsonPrimitive(interaction))
            }
            return DataPart(Json.encodeToString(json).toByteArray(Charsets.UTF_8), mimeType = UiPartConstants.interactionMimeType)
        }

        /// Creates a view from a [DataPart].
        fun fromDataPart(part: DataPart): UiInteractionPart {
            if (part.mimeType != UiPartConstants.interactionMimeType) {
                throw IllegalArgumentException("Part is not a UI interaction part");
            }
            val json = Json.decodeFromString<JsonObject>(String(part.bytes))
            return UiInteractionPart(json[JsonKey.interaction].toString())
        }
    }
}

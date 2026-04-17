package com.peihua.genui.model.parts

import com.peihua.genai.primitives.parts.DataPart
import com.peihua.genai.primitives.parts.Part
import com.peihua.genai.primitives.parts.StandardPart
import com.peihua.genui.model.SurfaceDefinition

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

class UiPart private constructor(val definition: SurfaceDefinition, private val surfaceId: String?) {
    //  const UiPart._({required this.definition, required this.surfaceId});
    //
    //  /// The JSON definition of the UI.
    //  final SurfaceDefinition definition;
    //
    //  /// The unique ID for this UI surface.
    //  final String? surfaceId;
    companion object {
        /// Creates a view from a [DataPart].
        fun fromDataPart(part: DataPart): UiPart {
            if (part.mimeType != UiPartConstants.uiMimeType) {
                throw IllegalArgumentException("Part is not a UI part");
            }
            val json = jsonDecode(utf8.decode(part.bytes)) as Map<String, Any>;
            return UiPart(
                definition = SurfaceDefinition.fromJson(
                    json[_Json.definition] as Map<String, Object?>,
                ),
                surfaceId = json[_Json.surfaceId] as String?,
            );
        }
    }
}

final class UiInteractionPart private constructor(
    //The interaction data (JSON string).
    val interaction: String,
) {
    companion object {
        /// Creates a [DataPart] representing a UI interaction.
        fun create(interaction: String): DataPart {
            final val json = { _Json.interaction = interaction };
            return DataPart(
                utf8.encode(jsonEncode(json)),
                mimeType = UiPartConstants.interactionMimeType,
            );
        }

        /// Creates a view from a [DataPart].
        fun fromDataPart(part: DataPart): UiInteractionPart {
            if (part.mimeType != UiPartConstants.interactionMimeType) {
                throw IllegalArgumentException("Part is not a UI interaction part");
            }
            val json = jsonDecode(utf8.decode(part.bytes)) as Map<String, Object?>;
            return UiInteractionPart(json[_Json.interaction] as String);
        }
    }
}

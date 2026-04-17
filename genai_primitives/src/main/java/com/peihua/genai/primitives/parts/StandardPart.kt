package com.peihua.genai.primitives.parts

import android.net.Uri
import com.peihua.json.UriSerializer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

final object JsonKey {
    const val content = "content";
    const val mimeType = "mimeType";
    const val name = "name";
    const val bytes = "bytes";
    const val url = "url";
    const val id = "id";
    const val arguments = "arguments";
    const val result = "result";
}

sealed class StandardPart : Part() {
    companion object {
        const val typeKey = "type";
    }
}

@Serializable
final class TextPart(var text: StateFlow<String>) : StandardPart() {
    companion object {
        const val TYPE = "text"
    }

    /**
     * 当前文本值，便于像 ValueNotifier.value 一样访问
     */
    val textValue: String
        get() = text.value
    override val type: String
        get() = TYPE

    override fun toJson(): JsonElement {
        return Json.encodeToJsonElement(this)
    }

}

@Serializable
class DataPart(
    val bytes: ByteArray,
    val mimeType: String,
    val name: String? = nameFromMimeType(mimeType),
) : StandardPart() {
    override val type: String
        get() = TYPE

    override fun toJson(): JsonElement {
        return Json.encodeToJsonElement(this)
    }

    companion object {
        const val TYPE = "Data";
        const val defaultMimeType = "application/octet-stream";

        //Gets the name for a MIME type.
        fun nameFromMimeType(mimeType: String): String {
            val ext = extensionFromMimeType(mimeType) ?: "bin";
            return if (mimeType.startsWith("image/")) "image.$ext" else "file.$ext";
        }

        //Gets the extension for a MIME type.
        fun extensionFromMimeType(mimeType: String): String? {
            return defaultExtensionMap.entries.firstOrNull { it.value == mimeType }?.key
        }

        //Gets the MIME type for a file.
        fun mimeTypeForFile(path: String, headerBytes: ByteArray): String {
//            lookupMimeType(path, headerBytes= headerBytes) ?: defaultMimeType;
            return defaultMimeType;
        }

    }


}

@Serializable
final class LinkPart(
    // The URL of the external content.
    @Serializable(with = UriSerializer::class)
    val url: Uri,
    // Optional MIME type of the linked content.
    val mimeType: String? = null,
    // Optional name for the link.
    val name: String? = null,
) : StandardPart() {
    companion object {
        const val TYPE = "Link";

        /// Creates a link part from a JSON-compatible map.
        fun fromJson(json: JsonElement): LinkPart {
            val content = json.jsonObject[JsonKey.content]
            if (content == null) {
                throw IllegalArgumentException("LinkPart.fromJson: content property is null");
            }
            return Json.decodeFromJsonElement(content)
        }
    }


    override val type: String
        get() = TYPE

    override fun toJson(): JsonElement {
        return Json.encodeToJsonElement(this)
    }

}

/** The kind of tool interaction. */
enum class ToolPartKind {
    /** A request to call a tool. */
    CALL,

    /** The result of a tool execution. */
    RESULT,
}

/// A tool interaction part of a message.
data class ToolPart(
    // The kind of tool interaction.
    val kind: ToolPartKind,
    // The unique identifier for this tool interaction.
    val callId: String,
    // The name of the tool.
    val toolName: String,
    // The arguments for a tool call (null for results).
    val arguments: Map<String, Any>?,
    // The result of a tool execution (null for calls).
    val result: Map<String, Any>?,
) : StandardPart() {
    companion object {
        const val TYPE = "Tool";

        /** Creates a tool call part. */
        fun call(callId: String, toolName: String, arguments: Map<String, Any>): ToolPart {
            return ToolPart(kind = ToolPartKind.CALL, callId = callId, toolName = toolName, arguments = arguments, result = null);
        }

        /** Creates a tool result part. */
        fun result(callId: String, toolName: String, result: Map<String, Any>): ToolPart {
            return ToolPart(kind = ToolPartKind.RESULT, callId = callId, toolName = toolName, arguments = null, result = result);
        }
    }

    /**
     * The arguments as a JSON string.
     */
    val argumentsRaw: String get() = if (arguments == null) "" else Json.encodeToString(arguments);
    override val type: String
        get() = TYPE

    override fun toJson(): JsonElement {
        return Json.encodeToJsonElement(this)
    }
}

data class ThinkingPart(
    // The thinking content.
    val text: String,
) : StandardPart() {
    override val type: String
        get() = TYPE

    override fun toJson(): JsonElement {
        return Json.encodeToJsonElement(this)
    }

    companion object {
        const val TYPE = "Thinking";
    }
}

/**
 * 简单扩展名 -> MIME 映射
 * 可按需扩充
 */
val defaultExtensionMap: Map<String, String> = mapOf(
    "png" to "image/png",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "gif" to "image/gif",
    "webp" to "image/webp",
    "pdf" to "application/pdf",
    "txt" to "text/plain",
    "json" to "application/json",
    "bin" to "application/octet-stream",
    "mp3" to "audio/mpeg",
    "wav" to "audio/wav",
    "mp4" to "video/mp4"
)
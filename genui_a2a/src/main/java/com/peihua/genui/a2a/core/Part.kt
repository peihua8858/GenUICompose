package com.peihua.genui.a2a.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

@Serializable(with = PartSerializer::class)
interface Part {
    /// The type discriminator, always 'text'.
    val kind: String

    /// Optional metadata associated with this text part.
    val metadata: JsonObject?

    companion object {
        fun text(
            // The type discriminator, always 'text'.
            kind: String = "text",
            text: String,
            // Optional metadata associated with this text part.
            metadata: JsonObject? = null,
        ): TextPart {
            return TextPart(kind, text, metadata)
        }

        fun file(
            // The type discriminator, always 'file'.
            kind: String = "file",
            file: FileType,
            // Optional metadata associated with this file part.
            metadata: JsonObject? = null,
        ): FilePart {
            return FilePart(kind, file, metadata)
        }

        fun data(
            // The type discriminator, always 'data'.
            kind: String = "data",
            data: JsonObject,
            // Optional metadata associated with this data part.
            metadata: JsonObject? = null,
        ): DataPart {
            return DataPart(kind, data, metadata)
        }
    }
}

interface FileType {
    val name: String?
    val mimeType: String?

    companion object {
        fun uri(uri: String, name: String? = null, mimeType: String? = null): FileType {
            return FileWithUri(uri, name, mimeType)
        }

        fun bytes(bytes: String, name: String?=null, mimeType: String?=null): FileType {
            return FileWithBytes(bytes, name, mimeType)
        }
    }
}

@Serializable
data class FileWithUri(val uri: String, override val name: String?, override val mimeType: String?) : FileType

@Serializable
data class FileWithBytes(val bytes: String, override val name: String?, override val mimeType: String?) : FileType

@Serializable
data class TextPart(override val kind: String, val text: String, override val metadata: JsonObject?=null) : Part

@Serializable
data class FilePart(override val kind: String, val file: FileType, override val metadata: JsonObject?=null) : Part

@Serializable
data class DataPart(override val kind: String, val data: JsonObject, override val metadata: JsonObject?=null) : Part

class PartSerializer : KSerializer<Part> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(encoder: Encoder, value: Part) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): Part {
        TODO("Not yet implemented")
    }

}
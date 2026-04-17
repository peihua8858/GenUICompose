package com.peihua.json

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

object AnyValueSerializer : KSerializer<Any> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("AnyValue", SerialKind.CONTEXTUAL)

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer only works with JSON")

        val element = when (value) {
            is String -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> buildJsonObject {
                value.forEach { (k, v) ->
                    if (k is String && v != null) {
                        put(k, toJsonElement(v))
                    }
                }
            }

            is List<*> -> buildJsonArray {
                value.forEach { v ->
                    add(v?.let { toJsonElement(it) } ?: JsonNull)
                }
            }

            else -> throw SerializationException("Unsupported type: ${value::class}")
        }

        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer only works with JSON")

        return fromJsonElement(jsonDecoder.decodeJsonElement())
    }

    private fun toJsonElement(value: Any): JsonElement = when (value) {
        is String -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Float -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> buildJsonObject {
            value.forEach { (k, v) ->
                if (k is String && v != null) {
                    put(k, toJsonElement(v))
                }
            }
        }

        is List<*> -> buildJsonArray {
            value.forEach { v ->
                add(v?.let { toJsonElement(it) } ?: JsonNull)
            }
        }

        else -> throw SerializationException("Unsupported type: ${value::class}")
    }

    private fun fromJsonElement(element: JsonElement): Any = when (element) {
        is JsonPrimitive -> {
            element.booleanOrNull
                ?: element.longOrNull
                ?: element.doubleOrNull
                ?: element.content
        }

        is JsonObject -> element.mapValues { fromJsonElement(it.value) }
        is JsonArray -> element.map { fromJsonElement(it) }
        JsonNull -> "null"
    }
}
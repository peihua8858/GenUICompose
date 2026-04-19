package com.peihua.json.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlin.collections.iterator

/**
 * 将对象转成Map
 */
fun Any?.toMapOrNull(): Map<String, Any?>? {
    if (this == null) {
        return null
    }
    val result = mutableMapOf<String, Any?>()
    this.javaClass.declaredFields.forEach { field ->
        field.isAccessible = true
        val value = field.get(this)
        result[field.name] = value
    }
    return result
}

/**
 * 将对象转成Map
 */
fun Any?.toMap(): Map<String, Any> {
    if (this == null) {
        return emptyMap()
    }
    val result = mutableMapOf<String, Any>()
    this.javaClass.declaredFields.forEach { field ->
        field.isAccessible = true
        val value = field.get(this)
        if (value != null) {
            result[field.name] = value
        }
    }
    return result
}

fun Any?.toJsonString(): String {
    if (this == null) {
        return ""
    }
    val json = Json.encodeToString(this)
    return json
}


fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is ULong -> JsonPrimitive(this)
    is UInt -> JsonPrimitive(this)
    is UByte -> JsonPrimitive(this)
    is UShort -> JsonPrimitive(this)
    is JsonElement -> this
    is List<*> -> JsonArray(this.map { it.toJsonElement() })
    is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST")
        (this as Map<String, Any?>).toJsonObject()
    }

    is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
    is Array<*> -> JsonArray(this.map { it.toJsonElement() })
    else -> JsonPrimitive(this.toString()) // 或者抛出异常，根据需求处理
}

fun Map<String, Any?>.toJsonObject(): JsonObject = buildJsonObject {
    for ((key, value) in this@toJsonObject) {
        put(key, value.toJsonElement())
    }
}

fun <T> T?.toJsonObject(): JsonObject = when (this) {
    null -> JsonObject(emptyMap())
    is JsonObject -> this
    else -> this.toMap().toJsonObject()
}

fun List<Any?>.toJsonArray(): JsonArray = buildJsonArray {
    for (value in this@toJsonArray) {
      add(value.toJsonElement())
    }
}

fun <T> T?.toJsonArray(): JsonArray = when (this) {
    null -> JsonArray(listOf())
    is JsonArray -> this
    is List<*> -> JsonArray(this.map { it.toJsonElement() })
    else -> throw IllegalArgumentException("Unsupported type: ${this::class.simpleName}")
}
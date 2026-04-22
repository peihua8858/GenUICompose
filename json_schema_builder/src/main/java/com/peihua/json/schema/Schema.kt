package com.peihua.json.schema

import com.peihua.json.JsonType
import com.peihua.json.kAnchor
import com.peihua.json.kDefs
import com.peihua.json.kDynamicAnchor
import com.peihua.json.kDynamicRef
import com.peihua.json.kRef
import com.peihua.json.utils.toJsonArray
import com.peihua.json.utils.toJsonElement
import com.peihua.json.utils.toJsonObject
import com.peihua.json.utils.toMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

typealias S = Schema

open class Schema(val value: JsonObject) {

    companion object {
        fun fromMap(map: Map<String, Any?>): Schema {
            return Schema(map.toJsonObject())
        }

        fun combined(
            type: Any? = null,
            enumValues: List<Any?>? = null,
            constValue: Any? = null,
            title: String? = null,
            description: String? = null,
            comment: String? = null,
            defaultValue: Any? = null,
            examples: List<Any?>? = null,
            deprecated: Boolean? = null,
            readOnly: Boolean? = null,
            writeOnly: Boolean? = null,
            defs: Map<String, Schema>? = null,
            ref: String? = null,
            anchor: String? = null,
            dynamicAnchor: String? = null,
            id: String? = null,
            schema: String? = null,
            allOf: List<Any?>? = null,
            anyOf: List<Any?>? = null,
            oneOf: List<Any?>? = null,
            not: Any? = null,
            ifSchema: Any? = null,
            thenSchema: Any? = null,
            elseSchema: Any? = null,
            dependentSchemas: Map<String, Schema>? = null,
        ): Schema {
            val typeValue = when (type) {
                is JsonType -> type.typeName
                is List<*> -> type.mapNotNull {
                    (it as? JsonType)?.typeName
                }.toList()

                else -> null
            }

            return Schema(buildJsonObject {
                put("type", typeValue.toJsonElement())
                put("title", title)
                put("description", description)
                put("enum", enumValues.toJsonArray())
                put("const", constValue.toJsonElement())
                put("\$comment", comment)
                put("default", defaultValue.toJsonElement())
                put("examples", examples.toJsonArray())
                put("deprecated", deprecated)
                put("readOnly", readOnly)
                put("writeOnly", writeOnly)
                put(kDefs, defs.toJsonElement())
                put(kRef, ref)
                put(kAnchor, anchor)
                put(kDynamicAnchor, dynamicAnchor)
                put("\$id", id)
                put("\$schema", schema)
                put("allOf", allOf.toJsonArray())
                put("anyOf", anyOf.toJsonArray())
                put("oneOf", oneOf.toJsonArray())
                put("not", not.toJsonElement())
                put("if", ifSchema.toJsonElement())
                put("then", thenSchema.toJsonElement())
                put("else", elseSchema.toJsonElement())
                put("dependentSchemas", dependentSchemas.toJsonElement())
            })
        }

        fun string(
            title: String? = null,
            description: String? = null,
            enumValues: List<Any?>? = null,
            constValue: Any? = null,
            minLength: Int? = null,
            maxLength: Int? = null,
            pattern: String? = null,
            format: String? = null,
        ): Schema {
            return Schema(buildJsonObject {
                put("type", JsonType.BOOLEAN.typeName)
                put("title", title)
                put("description", description)
                put("enum", enumValues.toJsonArray())
                put("const", constValue.toJsonElement())
                put("minLength", minLength)
                put("maxLength", maxLength)
                put("pattern", pattern)
                put("format", format)
            })
        }

        fun boolean(
            title: String? = null,
            description: String? = null,
        ): Schema {
            return Schema(buildJsonObject {
                put("type", JsonType.BOOLEAN.typeName)
                put("title", title)
                put("description", description)
            })
        }

        fun number(
            title: String? = null,
            description: String? = null,
            minimum: Number? = null,
            maximum: Number? = null,
            exclusiveMinimum: Number? = null,
            exclusiveMaximum: Number? = null,
            multipleOf: Number? = null,
        ): Schema {
            return Schema(buildJsonObject {
                put("type", JsonType.NUM.typeName)
                put("title", title)
                put("description", description)
                put("minimum", minimum)
                put("maximum", maximum)
                put("exclusiveMinimum", exclusiveMinimum)
                put("exclusiveMaximum", exclusiveMaximum)
                put("multipleOf", multipleOf)
            })
        }

        fun integer(
            title: String? = null,
            description: String? = null,
            minimum: Int? = null,
            maximum: Int? = null,
            exclusiveMinimum: Int? = null,
            exclusiveMaximum: Int? = null,
            multipleOf: Number? = null,
        ): Schema {
            return Schema(buildJsonObject {
                put("type", JsonType.INT.typeName)
                put("title", title)
                put("description", description)
                put("minimum", minimum)
                put("maximum", maximum)
                put("exclusiveMinimum", exclusiveMinimum)
                put("exclusiveMaximum", exclusiveMaximum)
                put("multipleOf", multipleOf)
            })
        }

        fun list(
            title: String? = null,
            description: String? = null,
            items: Schema? = null,
            prefixItems: List<Schema>? = null,
            unevaluatedItems: Any? = null,
            contains: Schema? = null,
            minContains: Int? = null,
            maxContains: Int? = null,
            minItems: Int? = null,
            maxItems: Int? = null,
            uniqueItems: Boolean? = null,
        ): Schema {
            return Schema(buildJsonObject {
                put("type", JsonType.LIST.typeName)
                put("title", title)
                put("description", description)
                put("items", items?.value ?: JsonNull)
                put("prefixItems", prefixItems.toSchemaJsonArray())
                put("unevaluatedItems", unevaluatedItems.toJsonElement())
                put("contains", contains?.value ?: JsonNull)
                put("minContains", minContains)
                put("maxContains", maxContains)
                put("minItems", minItems)
                put("maxItems", maxItems)
                put("uniqueItems", uniqueItems)

            })
        }

        fun obj(
            title: String? = null,
            description: String? = null,
            properties: Map<String, Schema>? = null,
            patternProperties: Map<String, Schema>? = null,
            required: List<String>? = null,
            dependentRequired: Map<String, List<String>>? = null,
            additionalProperties: Any? = null,
            unevaluatedProperties: Any? = null,
            propertyNames: Schema? = null,
            minProperties: Int? = null,
            maxProperties: Int? = null,
        ): Schema {
            return Schema(buildJsonObject {
                put("type", JsonType.OBJECT.typeName)
                put("title", title)
                put("description", description)
                put("properties", properties.toJsonObject())
                put("patternProperties", patternProperties.toJsonObject())
                put("required", required.toJsonArray())
                put("dependentRequired", dependentRequired.toJsonObject())
                put("additionalProperties", additionalProperties.toJsonElement())
                put("unevaluatedProperties", unevaluatedProperties.toJsonElement())
                put("propertyNames", propertyNames?.value ?: JsonNull)
                put("minProperties", minProperties)
                put("maxProperties", maxProperties)
            })
        }

        fun nil(
            title: String? = null,
            description: String? = null,
        ): Schema {
            return Schema(buildJsonObject {
                put("type", JsonType.NIL.typeName)
                put("title", title)
                put("description", description)
            })
        }

        fun any(
            title: String? = null,
            description: String? = null,
        ): Schema {
            return Schema(buildJsonObject {
                put("title", title)
                put("description", description)
            })
        }

        fun fromBoolean(
            value: Boolean,
            jsonPath: List<String> = emptyList(),
        ): Schema {
            return if (value) {
                Schema(JsonObject(mapOf()))
            } else {
                Schema(
                    buildJsonObject {
                        put("not", JsonObject(mapOf()))
                    })
            }
        }

        private fun putIfNotNull(
            map: MutableMap<String, Any>,
            key: String,
            value: Any?,
        ) {
            if (value != null) {
                map[key] = value
            }
        }

        private fun normalizeSchemaLike(value: Any?): Any? {
            return when (value) {
                null -> null
                is Schema -> value.toMap()
                else -> value
            }
        }
    }

    operator fun get(key: String): Any? = value[key]

    fun schemaOrBool(key: String): Schema? {
        val v = value[key] ?: return null
        return when {
            v is JsonPrimitive && (v.booleanOrNull != null) -> fromBoolean(v.booleanOrNull!!)
            v is JsonObject -> Schema(v)
            v is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                fromMap(v as Map<String, Any>)
            }

            else -> null
        }
    }

    fun mapToSchemaOrBool(key: String): Map<String, Schema>? {
        val v = value[key]
        if (v is Map<*, *>) {
            return v.entries.associate { entry ->
                val k = entry.key as String
                val schema = when (val item = entry.value) {
                    is Boolean -> fromBoolean(item)
                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        fromMap(item as Map<String, Any>)
                    }

                    else -> error("Invalid schema value for key: $k")
                }
                k to schema
            }
        }
        return null
    }

    val type: Any?
        get() = value["type"]

    val enumValues: List<Any?>?
        get() = value["enum"] as? List<Any?>

    val constValue: Any?
        get() = value["const"]

    val title: String?
        get() = value["title"] as? String

    val description: String?
        get() = value["description"] as? String

    val comment: String?
        get() = value["\$comment"] as? String

    val defaultValue: Any?
        get() = value["default"]

    val examples: List<Any?>?
        get() = value["examples"] as? List<Any?>

    val deprecated: Boolean?
        get() = value["deprecated"] as? Boolean

    val readOnly: Boolean?
        get() = value["readOnly"] as? Boolean

    val writeOnly: Boolean?
        get() = value["writeOnly"] as? Boolean

    val defs: Map<String, Schema>?
        get() = mapToSchemaOrBool(kDefs)

    val ref: String?
        get() = value[kRef] as? String

    val dynamicRef: String?
        get() = value[kDynamicRef] as? String

    val anchor: String?
        get() = value[kAnchor] as? String

    val dynamicAnchor: String?
        get() = value[kDynamicAnchor] as? String

    val id: String?
        get() = value["\$id"] as? String

    val schema: String?
        get() = value["\$schema"] as? String

    val allOf: List<Any?>?
        get() = value["allOf"] as? List<Any?>

    val anyOf: List<Any?>?
        get() = value["anyOf"] as? List<Any?>

    val oneOf: List<Any?>?
        get() = value["oneOf"] as? List<Any?>

    val not: Any?
        get() = value["not"]

    val ifSchema: Any?
        get() = value["if"]

    val thenSchema: Any?
        get() = value["then"]

    val elseSchema: Any?
        get() = value["else"]

    val dependentSchemas: Map<String, Schema>?
        get() = mapToSchemaOrBool("dependentSchemas")

    fun toJson(indent: String? = null): String {
        val jsonElement = value
        return if (indent != null) {
            Json {
                prettyPrint = true
                prettyPrintIndent = indent
            }.encodeToString(JsonElement.serializer(), jsonElement)
        } else {
            Json.encodeToString(JsonElement.serializer(), jsonElement)
        }
    }
}

fun Map<String, Schema>?.toJsonElement(): JsonElement {
    if (this == null) {
        return JsonNull
    }
    return buildJsonObject {
        for ((key, value) in this@toJsonElement) {
            put(key, value.value)
        }
    }
}

fun List<Schema>?.toSchemaJsonArray(): JsonElement {
    if (this == null) {
        return JsonNull
    }
    return buildJsonArray {
        for (value in this@toSchemaJsonArray) {
            add(value.value)
        }
    }
}
package com.peihua.json.schema

import com.peihua.json.JsonType
import com.peihua.json.kAnchor
import com.peihua.json.kDefs
import com.peihua.json.kDynamicAnchor
import com.peihua.json.kDynamicRef
import com.peihua.json.kRef
import com.peihua.json.utils.toMap
import kotlinx.serialization.json.*

typealias S = Schema

class Schema private constructor(
    val value: MutableMap<String, Any>
) {

    companion object {
        fun fromMap(map: Map<String, Any>): Schema {
            return Schema(map.toMutableMap())
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
                }

                else -> null
            }

            val map = mutableMapOf<String, Any>()

            putIfNotNull(map, "type", typeValue)
            putIfNotNull(map, "enum", enumValues)
            putIfNotNull(map, "const", constValue)
            putIfNotNull(map, "title", title)
            putIfNotNull(map, "description", description)
            putIfNotNull(map, "\$comment", comment)
            putIfNotNull(map, "default", defaultValue)
            putIfNotNull(map, "examples", examples)
            putIfNotNull(map, "deprecated", deprecated)
            putIfNotNull(map, "readOnly", readOnly)
            putIfNotNull(map, "writeOnly", writeOnly)
            putIfNotNull(map, kDefs, defs?.mapValues { it.value.toMap() })
            putIfNotNull(map, kRef, ref)
            putIfNotNull(map, kAnchor, anchor)
            putIfNotNull(map, kDynamicAnchor, dynamicAnchor)
            putIfNotNull(map, "\$id", id)
            putIfNotNull(map, "\$schema", schema)
            putIfNotNull(map, "allOf", allOf?.map { normalizeSchemaLike(it) })
            putIfNotNull(map, "anyOf", anyOf?.map { normalizeSchemaLike(it) })
            putIfNotNull(map, "oneOf", oneOf?.map { normalizeSchemaLike(it) })
            putIfNotNull(map, "not", normalizeSchemaLike(not))
            putIfNotNull(map, "if", normalizeSchemaLike(ifSchema))
            putIfNotNull(map, "then", normalizeSchemaLike(thenSchema))
            putIfNotNull(map, "else", normalizeSchemaLike(elseSchema))
            putIfNotNull(
                map,
                "dependentSchemas",
                dependentSchemas?.mapValues { it.value.toMap() }
            )
            return Schema(map)
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
            val map = mutableMapOf<String, Any>(
                "type" to "string"
            )
            putIfNotNull(map, "title", title)
            putIfNotNull(map, "description", description)
            putIfNotNull(map, "enum", enumValues)
            putIfNotNull(map, "const", constValue)
            putIfNotNull(map, "minLength", minLength)
            putIfNotNull(map, "maxLength", maxLength)
            putIfNotNull(map, "pattern", pattern)
            putIfNotNull(map, "format", format)
            return Schema(map)
        }

        fun boolean(
            title: String? = null,
            description: String? = null,
        ): Schema {
            val map = mutableMapOf<String, Any>(
                "type" to "boolean"
            )
            putIfNotNull(map, "title", title)
            putIfNotNull(map, "description", description)
            return Schema(map)
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
            val map = mutableMapOf<String, Any>(
                "type" to "number"
            )
            putIfNotNull(map, "title", title)
            putIfNotNull(map, "description", description)
            putIfNotNull(map, "minimum", minimum)
            putIfNotNull(map, "maximum", maximum)
            putIfNotNull(map, "exclusiveMinimum", exclusiveMinimum)
            putIfNotNull(map, "exclusiveMaximum", exclusiveMaximum)
            putIfNotNull(map, "multipleOf", multipleOf)
            return Schema(map)
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
            val map = mutableMapOf<String, Any>(
                "type" to "integer"
            )
            putIfNotNull(map, "title", title)
            putIfNotNull(map, "description", description)
            putIfNotNull(map, "minimum", minimum)
            putIfNotNull(map, "maximum", maximum)
            putIfNotNull(map, "exclusiveMinimum", exclusiveMinimum)
            putIfNotNull(map, "exclusiveMaximum", exclusiveMaximum)
            putIfNotNull(map, "multipleOf", multipleOf)
            return Schema(map)
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
            val map = mutableMapOf<String, Any>(
                "type" to "array"
            )
            putIfNotNull(map, "title", title)
            putIfNotNull(map, "description", description)
            putIfNotNull(map, "items", items?.toMap())
            putIfNotNull(map, "prefixItems", prefixItems?.map { it.toMap() })
            putIfNotNull(map, "unevaluatedItems", normalizeSchemaLike(unevaluatedItems))
            putIfNotNull(map, "contains", contains?.toMap())
            putIfNotNull(map, "minContains", minContains)
            putIfNotNull(map, "maxContains", maxContains)
            putIfNotNull(map, "minItems", minItems)
            putIfNotNull(map, "maxItems", maxItems)
            putIfNotNull(map, "uniqueItems", uniqueItems)
            return Schema(map)
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
            val map = mutableMapOf<String, Any>(
                "type" to "object"
            )
            putIfNotNull(map, "title", title)
            putIfNotNull(map, "description", description)
            putIfNotNull(map, "properties", properties?.mapValues { it.value.toMap() })
            putIfNotNull(map, "patternProperties", patternProperties?.mapValues { it.value.toMap() })
            putIfNotNull(map, "required", required)
            putIfNotNull(map, "dependentRequired", dependentRequired)
            putIfNotNull(map, "additionalProperties", normalizeSchemaLike(additionalProperties))
            putIfNotNull(map, "unevaluatedProperties", normalizeSchemaLike(unevaluatedProperties))
            putIfNotNull(map, "propertyNames", propertyNames?.toMap())
            putIfNotNull(map, "minProperties", minProperties)
            putIfNotNull(map, "maxProperties", maxProperties)
            return Schema(map)
        }

        fun nil(
            title: String? = null,
            description: String? = null,
        ): Schema {
            val map = mutableMapOf<String, Any>(
                "type" to "null"
            )
            putIfNotNull(map, "title", title)
            putIfNotNull(map, "description", description)
            return Schema(map)
        }

        fun any(
            title: String? = null,
            description: String? = null,
        ): Schema {
            val map = mutableMapOf<String, Any>()
            putIfNotNull(map, "title", title)
            putIfNotNull(map, "description", description)
            return Schema(map)
        }

        fun fromBoolean(
            value: Boolean,
            jsonPath: List<String> = emptyList(),
        ): Schema {
            return if (value) {
                Schema(mutableMapOf())
            } else {
                Schema(mutableMapOf("not" to emptyMap<String, Any?>()))
            }
        }

        private fun putIfNotNull(
            map: MutableMap<String, Any>,
            key: String,
            value: Any?
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
        return when (v) {
            is Boolean -> fromBoolean(v, listOf(key))
            is Map<*, *> -> {
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
                val item = entry.value
                val schema = when (item) {
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
        val jsonElement = toJsonElement(value)
        return if (indent != null) {
            Json {
                prettyPrint = true
                prettyPrintIndent = indent
            }.encodeToString(JsonElement.serializer(), jsonElement)
        } else {
            Json.encodeToString(JsonElement.serializer(), jsonElement)
        }
    }

    private fun toJsonElement(any: Any?): JsonElement {
        return when (any) {
            null -> JsonNull
            is String -> JsonPrimitive(any)
            is Boolean -> JsonPrimitive(any)
            is Int -> JsonPrimitive(any)
            is Long -> JsonPrimitive(any)
            is Float -> JsonPrimitive(any)
            is Double -> JsonPrimitive(any)
            is Number -> JsonPrimitive(any)
            is Schema -> toJsonElement(any.toMap())
            is Map<*, *> -> JsonObject(
                any.entries.associate { (k, v) ->
                    k.toString() to toJsonElement(v)
                }
            )

            is List<*> -> JsonArray(any.map { toJsonElement(it) })
            else -> JsonPrimitive(any.toString())
        }
    }
}

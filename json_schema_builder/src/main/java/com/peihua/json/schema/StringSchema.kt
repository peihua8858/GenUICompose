package com.peihua.json.schema

import com.peihua.json.kMaxLength
import com.peihua.json.kMinLength
import com.peihua.json.utils.toInteger
import com.peihua.json.utils.toJsonArray
import com.peihua.json.utils.toJsonElement
import com.peihua.json.utils.toString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class StringSchema(value: JsonObject) : Schema(value) {
    constructor(
        title: String? = null,
        description: String? = null,
        enumValues: List<Any?>? = null,
        constValue: Any? = null,
        minLength: Int? = null,
        maxLength: Int? = null,
        pattern: String? = null,
        format: String? = null,
    ) : this(buildJsonObject {
        put("type", "string")
        put("title", title)
        put("description", description)
        put("enum", enumValues.toJsonArray())
        put("const", constValue.toJsonElement())
        put("minLength", minLength)
        put("maxLength", maxLength)
        put("pattern", pattern)
        put("format", format)
    })

    /// The minimum length of the string.
    val minLength: Int
        get() = value[kMinLength].toInteger()

    /// The maximum length of the string.
    val maxLength: Int
        get() = value[kMaxLength].toInteger()

    /// A regular expression that the string must match.
    val pattern: String
        get() = value["pattern"].toString()

    /// A pre-defined format that the string must match.
    ///
    /// See https://json-schema.org/understanding-json-schema/reference/string.html#format
    /// for a list of supported formats.
    val format: String
        get() = value["format"].toString()
}
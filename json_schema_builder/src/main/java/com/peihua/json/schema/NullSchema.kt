package com.peihua.json.schema

import com.peihua.json.JsonType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class NullSchema(value: JsonObject) : Schema(value) {
    constructor(title: String?, description: String?) : this(
        buildJsonObject {
            put("type", JsonType.NIL.typeName)
            put("title", title)
            put("description", description)
        }
    )
}
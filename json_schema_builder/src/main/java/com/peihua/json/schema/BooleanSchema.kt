package com.peihua.json.schema

import com.peihua.json.JsonType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BooleanSchema(title: String?, description: String?) : Schema(
    buildJsonObject {
        put("type", JsonType.BOOLEAN.typeName)
        put("title", title)
        put("description", description)

    }
) {
}
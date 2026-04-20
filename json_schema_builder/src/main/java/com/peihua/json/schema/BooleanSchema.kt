package com.peihua.json.schema

import com.peihua.json.JsonType

class BooleanSchema(title: String?, description: String?) : Schema(
    mutableMapOf(
        "type" to JsonType.BOOLEAN.typeName,
        "title" to (title?:""),
        "description" to (description?:"")
    )
) {
}
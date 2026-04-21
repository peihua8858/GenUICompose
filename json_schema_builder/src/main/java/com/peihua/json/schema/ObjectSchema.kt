package com.peihua.json.schema

import com.peihua.json.utils.toJsonObject
import kotlinx.serialization.json.JsonObject

class ObjectSchema(value: JsonObject) : Schema(value) {
    companion object {
        fun fromMap(map: Map<String, Any>): ObjectSchema {
            return ObjectSchema(map.toJsonObject())
        }
    }
}
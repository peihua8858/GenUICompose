package com.peihua.json.schema

class ObjectSchema(value: MutableMap<String, Any>) : Schema(value) {
    companion object {
        fun fromMap(map: Map<String, Any>): ObjectSchema {
            return ObjectSchema(map.toMutableMap())
        }
    }
}
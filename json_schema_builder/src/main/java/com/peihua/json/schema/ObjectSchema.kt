package com.peihua.json.schema

class ObjectSchema( val value: MutableMap<String, Any?>) {
    companion object {
        fun fromMap(map: Map<String, Any?>): ObjectSchema {
            return ObjectSchema(map.toMutableMap())
        }
    }
}
package com.peihua.genai.primitives.parts

import kotlinx.serialization.json.JsonElement

abstract class Part {
    companion object {
        const val typeKey = "type";
    }

    abstract val type: String
    abstract fun toJson(): JsonElement
}
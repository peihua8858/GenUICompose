package com.peihua.genai.primitives.parts

import java.util.Collections

fun <V> List<V>.toImmutableList(): List<V> {
    return if (isEmpty()) {
        emptyList()
    } else {
        Collections.unmodifiableList(this)
    }
}

class Parts(parts: List<Part>) : AbstractList<Part>() {
    constructor() : this(emptyList())

    private val parts: MutableList<Part> = parts.toMutableList()

    override val size: Int
        get() = parts.size

    override operator fun get(index: Int): Part {
        return parts[index]
    }

    fun addAll(parts: Parts): Parts {
        this.parts.addAll(parts)
        return this
    }

    /**
     * Extracts and concatenates all text content from TextPart instances.
     *
     * Returns a single string with all text content concatenated together
     * without any separators. Empty text parts are included in the result.
     */
    val text: String
        get() = filterIsInstance<TextPart>().map { it.text }.joinToString("")

    /**
     * Extracts all tool call parts from the list.
     *
     * Returns only ToolPart instances where kind == ToolPartKind.call.
     */
    val toolCalls: List<ToolPart>
        get() = parts.filterIsInstance<ToolPart>()
            .filter { it.kind == ToolPartKind.CALL }

    /**
     *  Extracts all tool result parts from the list.
     *
     *  Returns only ToolPart instances where kind == ToolPartKind.result.
     */
    val toolResults: List<ToolPart>
        get() = parts.filterIsInstance<ToolPart>()
            .filter { it.kind == ToolPartKind.RESULT }
}
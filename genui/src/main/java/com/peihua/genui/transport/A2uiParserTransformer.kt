package com.peihua.genui.transport

import com.peihua.genui.Logger
import com.peihua.genui.model.A2uiMessage
import com.peihua.genui.model.A2uiMessageEvent
import com.peihua.genui.model.A2uiValidationException
import com.peihua.genui.model.GenerationEvent
import com.peihua.genui.model.TextEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class A2uiParserTransformer(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
    private val logger: Logger = Logger,
) {

    fun transform(stream: Flow<String>): Flow<GenerationEvent> = channelFlow {
        val parser = A2uiParserStream(
            json = json,
            logger = logger,
            emit = { event -> send(event) },
            emitError = { throwable -> throw throwable }
        )

        stream.collect { chunk ->
            parser.onData(chunk)
        }
        parser.onDone()
    }
}

private class A2uiParserStream(
    private val json: Json,
    private val logger: Logger = Logger,
    private val emit: suspend (GenerationEvent) -> Unit,
    private val emitError: suspend (Throwable) -> Unit,
) {
    private var buffer: String = ""

    suspend fun onData(chunk: String) {
        logger.dLog("_onData>>stream.chunk:>>>$chunk")
        logger.dLog("_onData>before>stream.buffer:>>>$buffer")
        buffer += chunk
        logger.dLog("_onData>after>stream.buffer:>>>$buffer")
        processBuffer()
    }

    suspend fun onDone() {
        if (buffer.isNotEmpty()) {
            emitText(buffer)
            buffer = ""
        }
    }

    private suspend fun processBuffer() {
        while (buffer.isNotEmpty()) {
            // 1. Check for Markdown JSON block
            val markdownMatch = findMarkdownJson(buffer)
            if (markdownMatch != null) {
                try {
                    logger.dLog("_processBuffer>after>markdownMatch.content:>>>${markdownMatch.content}")
                    val decoded = json.parseToJsonElement(markdownMatch.content)
                    logger.dLog("_processBuffer>after>decoded:>>>$decoded")

                    emitBefore(markdownMatch.start)
                    emitMessage(decoded)
                    buffer = buffer.substring(markdownMatch.end)
                    continue
                } catch (_: Exception) {
                    emitBefore(markdownMatch.start)
                    emitText(markdownMatch.original)
                    buffer = buffer.substring(markdownMatch.end)
                    continue
                }
            }

            // 2. Check for Balanced JSON
            logger.dLog("_processBuffer>after>decoded:>>>$buffer")
            val jsonMatch = findBalancedJson(buffer)
            if (jsonMatch != null) {
                if (markdownMatch != null && markdownMatch.start <= jsonMatch.start) {
                    // already tried markdown and failed, fall through
                }

                logger.dLog("_processBuffer>after>decoded:>>>${jsonMatch.content}")

                try {
                    val decoded = json.parseToJsonElement(jsonMatch.content)
                    emitBefore(jsonMatch.start)
                    emitMessage(decoded)
                    buffer = buffer.substring(jsonMatch.end)
                    continue
                } catch (_: Exception) {
                    emitBefore(jsonMatch.start)
                    emitText(jsonMatch.original)
                    buffer = buffer.substring(jsonMatch.end)
                    continue
                }
            }

            // 3. Fallback / Wait logic
            val markdownStart = buffer.indexOf("```")
            val braceStart = buffer.indexOf("{")

            val firstPotentialStart = when {
                markdownStart != -1 && braceStart != -1 -> minOf(markdownStart, braceStart)
                markdownStart != -1 -> markdownStart
                else -> braceStart
            }

            if (firstPotentialStart == -1) {
                if (buffer.isNotEmpty()) {
                    emitText(buffer)
                    buffer = ""
                }
                break
            } else {
                if (firstPotentialStart > 0) {
                    emitText(buffer.substring(0, firstPotentialStart))
                    buffer = buffer.substring(firstPotentialStart)
                }
                break
            }
        }
    }

    private suspend fun emitBefore(index: Int) {
        if (index > 0) {
            emitText(buffer.substring(0, index))
        }
    }

    private suspend fun emitText(text: String) {
        logger.dLog("_emitText>>stream.text:>>>$text")
        val cleanText = text
            .replace("<a2ui_message>", "")
            .replace("</a2ui_message>", "")

        if (cleanText.isNotEmpty()) {
            emit(TextEvent(cleanText))
        }
    }

    private suspend fun emitMessage(jsonElement: JsonElement) {
        logger.dLog("_emitMessage>>stream.json:>>>$jsonElement")
        when (jsonElement) {
            is JsonObject -> {
                try {
                    emit(A2uiMessageEvent(A2uiMessage.fromJson(jsonElement)))
                } catch (e: A2uiValidationException) {
                    emitError(e)
                } catch (_: Exception) {
                    emit(TextEvent(jsonElement.toString()))
                }
            }

            is JsonArray -> {
                for (item in jsonElement) {
                    if (item is JsonObject) {
                        try {
                            emit(A2uiMessageEvent(A2uiMessage.fromJson(item)))
                        } catch (e: A2uiValidationException) {
                            emitError(e)
                        } catch (_: Exception) {
                            emit(TextEvent(item.toString()))
                        }
                    }
                }
            }

            else -> {
                emit(TextEvent(jsonElement.toString()))
            }
        }
    }

    private fun findMarkdownJson(text: String): Match? {
        val regex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        val match = regex.find(text)
        return if (match != null) {
            Match(
                start = match.range.first,
                end = match.range.last + 1,
                content = match.groupValues.getOrElse(1) { "" },
                original = match.value
            )
        } else {
            null
        }
    }

    private fun findBalancedJson(input: String): Match? {
        if (!input.startsWith("{")) return null

        var balance = 0
        var inString = false
        var isEscaped = false

        for (i in input.indices) {
            val char = input[i]

            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (char == '\\') {
                isEscaped = true
                continue
            }
            if (char == '"') {
                inString = !inString
                continue
            }

            if (!inString) {
                if (char == '{') {
                    balance++
                } else if (char == '}') {
                    balance--
                    if (balance == 0) {
                        val text = input.substring(0, i + 1)
                        return Match(0, i + 1, text, text)
                    }
                }
            }
        }
        return null
    }
}

private data class Match(
    val start: Int,
    val end: Int,
    val content: String,
    val original: String,
)

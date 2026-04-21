package com.peihua.genui.transport

import com.peihua.genai.primitives.ChatMessage
import com.peihua.genui.Logger
import com.peihua.genui.interfaces.Transport
import com.peihua.genui.model.A2uiMessage
import com.peihua.genui.model.A2uiMessageEvent
import com.peihua.genui.model.GenerationEvent
import com.peihua.genui.model.TextEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/// A manual sender callback.
typealias ManualSendCallback = (message: ChatMessage) -> Unit;

class A2uiTransportAdapter(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
    private val logger: Logger = Logger,
    private val onSend: ManualSendCallback? = null,
) : Transport {
    private val inputChannel = Channel<String>(Channel.UNLIMITED)
    private val messageFlowInternal = MutableSharedFlow<A2uiMessage>(
        extraBufferCapacity = Int.MAX_VALUE
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pipeline: SharedFlow<GenerationEvent> by lazy {
        A2uiParserTransformer(json = json, logger = logger)
            .transform(inputChannel.receiveAsFlow())
            .shareIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                replay = 0
            )
    }
    private var pipelineJob: Job? = null
    override val incomingText: Flow<String>
        get() = pipeline
            .filterIsInstance<TextEvent>()
            .map { it.text.trim() }
            .filter { it.isNotEmpty() }
    override val incomingMessages: Flow<A2uiMessage>
        get() = messageFlowInternal.asSharedFlow()

    override suspend fun sendRequest(message: ChatMessage) {
        val callback = onSend
            ?: throw IllegalStateException(
                "A2uiTransportAdapter.onSend must be provided to use sendRequest."
            )
        callback(message)
    }

    suspend fun flush() {
        inputChannel.close()
        pipelineJob?.join()
    }

    override fun dispose() {
        inputChannel.close()
        pipelineJob?.cancel()
        scope.cancel()
    }

    override fun addChunk(text: String) {
        logger.dLog("addChunk>>>>>text:$text")

        if (pipelineJob == null) {
            pipelineJob = scope.launch {
                pipeline.collect { event ->
                    if (event is A2uiMessageEvent) {
                        logger.dLog("addChunk>>>>>event.message:${event.message}")
                        messageFlowInternal.emit(event.message)
                    }
                }
            }
        }

        scope.launch {
            inputChannel.send(text)
        }
    }

    override fun addMessage(message: A2uiMessage) {
        logger.dLog("addMessage>>>>>message:$message")
        scope.launch {
            messageFlowInternal.emit(message)
        }
    }
}
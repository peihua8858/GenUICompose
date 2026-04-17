package com.peihua.genui.a2a

import com.peihua.genai.primitives.ChatMessage
import com.peihua.genai.primitives.parts.DataPart
import com.peihua.genai.primitives.parts.LinkPart
import com.peihua.genai.primitives.parts.TextPart
import com.peihua.genui.a2a.client.A2AClient
import com.peihua.genui.a2a.client.SseTransport
import com.peihua.genui.a2a.core.AgentCard
import com.peihua.genui.a2a.core.FileType
import com.peihua.genui.a2a.core.Message
import com.peihua.genui.a2a.core.Part
import com.peihua.genui.a2a.core.Role
import com.peihua.genui.model.A2UiClientCapabilities
import com.peihua.genui.model.A2uiMessage
import com.peihua.genui.model.parts.asUiInteractionPart
import com.peihua.genui.model.parts.asUiPart
import com.peihua.genui.model.parts.isUiInteractionPart
import com.peihua.genui.model.parts.isUiPart
import com.peihua.genui.primitives.CancellationSignal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.net.URI
import java.util.Base64
import java.util.UUID

val a2uiExtensionUri: URI = URI.create(
    "https://a2ui.org/a2a-extension/a2ui/v0.9"
)

class A2uiAgentConnector(
    val url: URI,
    client: A2AClient? = null,
    contextId: String? = null,
) {
    private val _controller = MutableSharedFlow<A2uiMessage>(extraBufferCapacity = 64)
    private val _textController = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private val _errorController = MutableSharedFlow<Any>(extraBufferCapacity = 64)
    val client: A2AClient =
        client ?: A2AClient(
            url = url.toString(),
            transport = SseTransport(
                url = url.toString(),
                authHeaders = mapOf("X-A2A-Extensions" to a2uiExtensionUri.toString())
            )
        )

    /**
     * The current task ID from the A2A server.
     */
    var taskId: String? = null

    private var _contextId: String? = contextId

    /**
     * The current context ID from the A2A server.
     */
    val contextId: String?
        get() = _contextId

    /**
     * The stream of A2UI messages.
     */
    val stream: SharedFlow<A2uiMessage> = _controller.asSharedFlow()

    /**
     * The stream of text responses.
     */
    val textStream: SharedFlow<String> = _textController.asSharedFlow()

    /**
     * A stream of errors from the A2A connection.
     */
    val errorStream: SharedFlow<Any> = _errorController.asSharedFlow()

    /**
     * Fetches the agent card.
     *
     * The agent card contains metadata about the agent, such as its name,
     * description, and version.
     */
    suspend fun getAgentCard(): AgentCard {
        return client.getAgentCard()
    }
    /**
     * Connects to the agent and sends a message.
     *
     * The [clientCapabilities] describe the UI capabilities of the client,
     * specifically determining which component catalogs are supported.
     *
     * The [clientDataModel] allows passing the current state of client-side
     * data to the agent, enabling context-aware responses.
     *
     * Returns the text response from the agent, if any.
     */
    suspend fun connectAndSend(
        chatMessage: ChatMessage,
        clientCapabilities: A2UiClientCapabilities? = null,
        clientDataModel: Map<String, Any?>? = null,
        cancellationSignal: CancellationSignal? = null,
    ): String? {
        cancellationSignal?.addListener {
            taskId?.let {
                client.cancelTask(it)
            }
        }

        val message = Message(
            messageId = UUID.randomUUID().toString(),
            role = Role.USER,
            parts = chatMessage.parts.map { part ->
                when {
                    part is TextPart -> {
                        Part.text(text = part.text.value)
                    }

                    part.isUiInteractionPart -> {
                        val uiPart = part.asUiInteractionPart!!
                        try {
                            val json: JsonObject? = Json.decodeFromString(uiPart.interaction)
                            if (json != null) {
                                Part.data(data = json)
                            } else {
                                Part.text(text = uiPart.interaction)
                            }
                        } catch (e: Exception) {
                            Part.text(text = uiPart.interaction)
                        }
                    }

                    part.isUiPart -> {
                        val uiPart = part.asUiPart!!
                        Part.data(data = Json.decodeFromString(uiPart.definition))
                    }

                    part is DataPart -> {
                        Part.file(
                            file = FileType.bytes(
                                bytes = Base64.getEncoder().encodeToString(part.bytes),
                                mimeType = part.mimeType
                            )
                        )
                    }

                    part is LinkPart -> {
                        Part.file(
                            file = FileType.uri(
                                uri = part.url.toString(),
                                mimeType = part.mimeType ?: "application/octet-stream"
                            )
                        )
                    }

                    else -> {
                        Part.text(text = "")
                    }
                }
            }.toList()
        )

        var messageToSend = message

        if (taskId != null) {
            messageToSend = messageToSend.copy(
                referenceTaskIds = listOf(taskId!!)
            )
        }

        if (contextId != null) {
            messageToSend = messageToSend.copy(
                contextId = contextId
            )
        }

        val metadata = mutableMapOf<String, Any?>()
        if (clientCapabilities != null) {
            metadata["a2uiClientCapabilities"] = clientCapabilities.toJson()
        }
        if (clientDataModel != null) {
            metadata["a2uiClientDataModel"] = clientDataModel
        }
        if (metadata.isNotEmpty()) {
            messageToSend = messageToSend.copy(metadata = metadata)
        }

        log.info("--- OUTGOING REQUEST ---")
        log.info("URL: $url")
        log.info("Method: message/stream")
        try {
            val payload = toPrettyJson(sanitizeLogData(messageToSend.toJson()))
            log.info("Payload: $payload")
        } catch (e: Exception) {
            log.warning("Error logging payload: $e")
        }
        log.info("----------------------")

        val events = client.messageStream(messageToSend)

        var responseText: String? = null

        try {
            var finalResponse: Message? = null

            events.collect { event ->
                log.info("Received raw A2A event: ${event.toJson()}")
                val prettyJson = toPrettyJson(event.toJson())
                log.info("Received A2A event:\n$prettyJson")

                when (event) {
                    is TaskStatusUpdate -> {
                        taskId = event.taskId
                        _contextId = event.contextId
                        val messageResp = event.status.message

                        when (event.status.state) {
                            TaskState.FAILED,
                            TaskState.CANCELED,
                            TaskState.REJECTED -> {
                                val errorMessage =
                                    "A2A Error: ${event.status.state}: ${event.status.message}"
                                log.severe(errorMessage)
                                _errorController.tryEmit(errorMessage)
                            }

                            else -> {}
                        }

                        if (messageResp != null) {
                            finalResponse = messageResp
                            log.info(
                                "Received A2A Message:\n${toPrettyJson(messageResp.toJson())}"
                            )
                            for (part in messageResp.parts) {
                                when (part) {
                                    is Part.Data -> {
                                        processA2uiMessages(part.data)
                                    }

                                    is Part.Text -> {
                                        val trimmedText = part.text.trim()
                                        if (trimmedText.isNotEmpty()) {
                                            _textController.tryEmit(trimmedText)
                                        }
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }

                    is StatusUpdate -> {
                        taskId = event.taskId
                        _contextId = event.contextId
                        val messageResp = event.status.message

                        when (event.status.state) {
                            TaskState.FAILED,
                            TaskState.CANCELED,
                            TaskState.REJECTED -> {
                                val errorMessage =
                                    "A2A Error: ${event.status.state}: ${event.status.message}"
                                log.severe(errorMessage)
                                _errorController.tryEmit(errorMessage)
                            }

                            else -> {}
                        }

                        if (messageResp != null) {
                            finalResponse = messageResp
                            log.info(
                                "Received A2A Message:\n${toPrettyJson(messageResp.toJson())}"
                            )
                            for (part in messageResp.parts) {
                                when (part) {
                                    is Part.Data -> {
                                        processA2uiMessages(part.data)
                                    }

                                    is Part.Text -> {
                                        val trimmedText = part.text.trim()
                                        if (trimmedText.isNotEmpty()) {
                                            _textController.tryEmit(trimmedText)
                                        }
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }

            finalResponse?.let { response ->
                for (part in response.parts) {
                    if (part is Part.Text) {
                        responseText = part.text
                    }
                }
            }
        } catch (e: Exception) {
            log.severe("Error parsing A2A response: $e")
        }

        return responseText
    }
    /**
     * Sends an event to the agent.
     *
     * This is used to send user interaction events to the agent, such as
     * button clicks or form submissions.
     */
    suspend fun sendEvent(event: Map<String, Any?>) {
        if (taskId == null) {
            log.severe("Cannot send event, no active task ID.")
            return
        }

        val clientEvent = mutableMapOf<String, Any?>(
            "version" to "v0.9",
            "action" to mutableMapOf<String, Any?>(
                "name" to event["action"],
                "sourceComponentId" to event["sourceComponentId"],
                "timestamp" to Instant.now().toString(),
                "context" to event["context"]
            ).apply {
                if (event.containsKey("surfaceId")) {
                    this["surfaceId"] = event["surfaceId"]
                }
            }
        )

        log.fine("Sending client event: $clientEvent")

        val dataPart = Part.Data(data = clientEvent)
        val message = Message(
            role = Role.USER,
            parts = listOf(dataPart),
            contextId = contextId,
            referenceTaskIds = listOf(taskId!!),
            messageId = UUID.randomUUID().toString(),
            extensions = listOf(a2uiExtensionUri.toString())
        )

        try {
            val response: Task = client.messageSend(message)
            log.fine("Response: ${toPrettyJson(response.toJson())}")
            log.fine("Successfully sent event for task $taskId (context $contextId)")
        } catch (e: Exception) {
            log.severe("Error sending event: $e")
        }
    }

    private suspend fun processA2uiMessages(data: Map<String, Any?>) {
        var prettyJson = "(Error sanitizing log data)"
        try {
            prettyJson = toPrettyJson(sanitizeLogData(data))
            log.finest("Processing a2ui messages from data part:\n$prettyJson")
        } catch (e: Exception) {
            log.warning("Error logging a2ui messages: $e")
        }

        if (
            data.containsKey("updateComponents") ||
            data.containsKey("updateDataModel") ||
            data.containsKey("createSurface") ||
            data.containsKey("deleteSurface")
        ) {
            log.finest("Adding message to stream: $prettyJson")
            _controller.emit(GenUiA2uiMessage.fromJson(data))
        } else {
            log.warning("A2A data part did not contain any known A2UI messages.")
        }
    }

    /**
     * Closes the connection to the agent.
     *
     * This should be called when the connector is no longer needed to release
     * resources.
     */
    fun dispose() {
        // If your A2AClient or transport supports closing, do it here.
        // Example:
        // client.close()
    }
}
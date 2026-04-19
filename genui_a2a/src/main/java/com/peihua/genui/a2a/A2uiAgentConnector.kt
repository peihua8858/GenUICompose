package com.peihua.genui.a2a

import com.peihua.genai.primitives.ChatMessage
import com.peihua.genui.ILogger
import com.peihua.genui.a2a.client.A2AClient
import com.peihua.genui.a2a.client.SseTransport
import com.peihua.genui.a2a.core.AgentCard
import com.peihua.genui.a2a.core.DataPart
import com.peihua.genui.a2a.core.FileType
import com.peihua.genui.a2a.core.Message
import com.peihua.genui.a2a.core.Part
import com.peihua.genui.a2a.core.Role
import com.peihua.genui.a2a.core.StatusUpdate
import com.peihua.genui.a2a.core.Task
import com.peihua.genui.a2a.core.TaskState
import com.peihua.genui.a2a.core.TaskStatusUpdate
import com.peihua.genui.a2a.core.TextPart
import com.peihua.genui.model.A2UiClientCapabilities
import com.peihua.genui.model.A2uiMessage
import com.peihua.genui.model.parts.asUiInteractionPart
import com.peihua.genui.model.parts.asUiPart
import com.peihua.genui.model.parts.isUiInteractionPart
import com.peihua.genui.model.parts.isUiPart
import com.peihua.json.utils.toJsonString
import com.peihua.json.utils.toJsonObject
import com.peihua.json.utils.toMapOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.net.URI
import java.util.Base64
import java.util.UUID
import com.peihua.genai.primitives.parts.DataPart as GenUiDataPart
import com.peihua.genai.primitives.parts.LinkPart as GenUiLinkPart
import com.peihua.genai.primitives.parts.TextPart as GenUiTextPart

val a2uiExtensionUri: URI = URI.create(
    "https://a2ui.org/a2a-extension/a2ui/v0.9"
)

class A2uiAgentConnector(
    val url: URI,
    val logger: ILogger,
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
    ): String? {
        val message = Message(
            messageId = UUID.randomUUID().toString(),
            role = Role.USER,
            parts = chatMessage.parts.map { part ->
                when {
                    part is GenUiTextPart -> {
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
                        Part.data(data = Json.decodeFromJsonElement(uiPart.definition.toJson()))
                    }

                    part is GenUiDataPart -> {
                        Part.file(
                            file = FileType.bytes(
                                bytes = Base64.getEncoder().encodeToString(part.bytes),
                                mimeType = part.mimeType
                            )
                        )
                    }

                    part is GenUiLinkPart -> {
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
        val metadata = mutableMapOf<String, Any>()
        if (clientCapabilities != null) {
            metadata["a2uiClientCapabilities"] = clientCapabilities.toJson()
        }
        if (clientDataModel != null) {
            metadata["a2uiClientDataModel"] = clientDataModel
        }
        if (metadata.isNotEmpty()) {
            messageToSend = messageToSend.copy(metadata = metadata)
        }
        logger.iLog("--- OUTGOING REQUEST ---")
        logger.iLog("URL: $url")
        logger.iLog("Method: message/stream")
        try {
            val payload = Json.encodeToString(sanitizeLogData(messageToSend.toMapOrNull()))
            logger.iLog("Payload: $payload")
        } catch (e: Exception) {
            logger.wLog("Error logging payload: $e")
        }
        logger.iLog("----------------------")

        val events = client.messageStream(messageToSend)

        var responseText: String? = null

        try {
            var finalResponse: Message? = null

            events.collect { event ->
                logger.iLog("Received raw A2A event: ${event.toJsonString()}")
                val prettyJson = event.toJsonString()
                logger.iLog("Received A2A event:\n$prettyJson")

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
                                logger.iLog(errorMessage)
                                _errorController.tryEmit(errorMessage)
                            }

                            else -> {}
                        }

                        if (messageResp != null) {
                            finalResponse = messageResp
                            logger.iLog("Received A2A Message:\n${messageResp.toJsonString()}")
                            for (part in messageResp.parts) {
                                when (part) {
                                    is DataPart -> {
                                        processA2uiMessages(part.data)
                                    }

                                    is TextPart -> {
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
                                logger.iLog(errorMessage)
                                _errorController.tryEmit(errorMessage)
                            }

                            else -> {}
                        }

                        if (messageResp != null) {
                            finalResponse = messageResp
                            logger.iLog("Received A2A Message:\n${(messageResp.toJsonString())}")
                            for (part in messageResp.parts) {
                                when (part) {
                                    is DataPart -> {
                                        processA2uiMessages(part.data)
                                    }

                                    is TextPart -> {
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

                    else -> {}
                }
            }

            finalResponse?.let { response ->
                for (part in response.parts) {
                    if (part is TextPart) {
                        responseText = part.text
                    }
                }
            }
        } catch (e: Exception) {
            logger.eLog("Error parsing A2A response: $e")
        }
        return responseText
    }

    /**
     * Sends an event to the agent.
     *
     * This is used to send user interaction events to the agent, such as
     * button clicks or form submissions.
     */
    suspend fun sendEvent(event: Map<String, Any>) {
        if (taskId == null) {
            logger.iLog("Cannot send event, no active task ID.")
            return
        }

        val eventParams = mutableMapOf(
            "version" to "v0.9",
            "action" to mutableMapOf(
                "name" to event["action"],
                "sourceComponentId" to event["sourceComponentId"],
                "timestamp" to java.time.Instant.now().toString(),
                "context" to event["context"]
            ).apply {
                if (event.containsKey("surfaceId")) {
                    this["surfaceId"] = event["surfaceId"]
                }
            }
        )
        val clientEvent =  eventParams.toJsonObject()
        logger.iLog("Sending client event: $clientEvent")
        val dataPart = Part.data(data = clientEvent)
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
            logger.iLog("Response: ${(response.toJsonString())}")
            logger.iLog("Successfully sent event for task $taskId (context $contextId)")
        } catch (e: Exception) {
            logger.eLog("Error sending event: $e")
        }
    }

    private suspend fun processA2uiMessages(data: JsonObject) {
        var prettyJson = "(Error sanitizing log data)"
        try {
            prettyJson = (sanitizeLogData(data)).toJsonString()
            logger.iLog("Processing a2ui messages from data part:\n$prettyJson")
        } catch (e: Exception) {
            logger.eLog("Error logging a2ui messages: $e")
        }

        if (
            data.containsKey("updateComponents") ||
            data.containsKey("updateDataModel") ||
            data.containsKey("createSurface") ||
            data.containsKey("deleteSurface")
        ) {
            logger.iLog("Adding message to stream: $prettyJson")
            _controller.emit(a2aJson.decodeFromJsonElement(data))
        } else {
            logger.eLog("A2A data part did not contain any known A2UI messages.")
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
        client.close()
    }
}
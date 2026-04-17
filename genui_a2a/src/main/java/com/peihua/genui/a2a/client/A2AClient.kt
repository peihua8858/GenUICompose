package com.peihua.genui.a2a.client

import android.util.Log
import com.peihua.genui.a2a.DefaultJson
import com.peihua.genui.a2a.client.A2AClient.Companion.agentCardPath
import com.peihua.genui.a2a.core.AgentCard
import com.peihua.genui.a2a.core.Event
import com.peihua.genui.a2a.core.Message
import com.peihua.genui.a2a.core.Task
import com.peihua.genui.a2a.sanitizeLogData
import com.peihua.genui.utils.toInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMap
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A client for interacting with an A2A (Agent-to-Agent) server.
 *
 * This class provides methods for all the RPC calls defined in the A2A
 * specification. It handles the JSON-RPC 2.0 protocol and uses a [Transport]
 * instance to communicate with the server, which defaults to [HttpTransport].
 **/
fun _exceptionFrom(error: JsonObject): A2AException {
    val code = error["code"].toInteger()
    val message = error["message"].toString()
    val data = error["data"]?.jsonObject

    return when (code) {
        -32001 -> A2AException.taskNotFound(message = message, data = data)
        -32002 -> A2AException.taskNotCancelable(message = message, data = data)
        -32006 -> A2AException.pushNotificationNotSupported(message = message, data = data)
        -32007 -> A2AException.pushNotificationConfigNotFound(message = message, data = data)
        else -> A2AException.jsonRpc(code = code, message = message, data = data)
    }
}

class A2AClient(private val url: String, private val transport: Transport) {
    private var _requestId: Int = 0

    /**
     * Fetches the public agent card from the server.
     *
     * The agent card contains metadata about the agent, such as its capabilities
     * and security schemes. This method typically requests the card from the
     * [agentCardPath] endpoint on the server.
     *
     * Returns an [AgentCard] object.
     * Throws an [A2AException] if the request fails or the response is invalid.
     **/
    suspend fun getAgentCard(): AgentCard {
        Log.d("A2AClient", "Fetching agent card...");
        val response = transport[agentCardPath];
        Log.d("A2AClient", "Received agent card: $response");
        return AgentCard.fromJson(response);
    }

    /**
     * Fetches the authenticated extended agent card from the server.
     *
     * This method retrieves a potentially more detailed [AgentCard] that is only
     * available to authenticated users. It includes an `Authorization` header
     * with the provided Bearer [token] in the request to [agentCardPath].
     *
     * Returns an [AgentCard] object.
     * Throws an [A2AException] if the request fails or the response is invalid.
     **/
    suspend fun getAuthenticatedExtendedCard(token: String): AgentCard {
        Log.d("A2AClient", "Fetching authenticated agent card...");
        val response = transport.get(agentCardPath, headers = mapOf("Authorization" to "Bearer $token"))
        Log.d("A2AClient", "Received authenticated agent card: $response");
        return AgentCard.fromJson(response);
    }

    /**
     * Sends a message to the agent for a single-shot interaction via
     * `message/send`.
     *
     * This method is used for synchronous request/response interactions. The
     * server is expected to process the [message] and return a result relatively
     * quickly. The returned [Task] contains the initial state of the task as
     * reported by the server.
     *
     * For operations that are expected to take longer, consider using
     * [messageStream] or polling the task status using [getTask].
     *
     * Returns the initial [Task] state. Throws an [A2AException] if the server
     * returns a JSON-RPC error.
     **/
    suspend fun messageSend(message: Message): Task {
        Log.d("A2AClient", "Sending message: ${message.messageId}");
        val params = mutableMapOf<String, Any>("message" to Json.encodeToString(message))
        if (message.extensions != null) {
            params["extensions"] = message.extensions
        }
        val messageMap = mutableMapOf(
            "jsonrpc" to "2.0",
            "method" to "message/send",
            "params" to params,
            "id" to _requestId++,
        )
        val headers = mutableMapOf<String, String>()
        if (message.extensions != null) {
            headers["X-A2A-Extensions"] = message.extensions.joinToString(",")
        }
        val response = transport.send(messageMap, headers = headers);
        Log.d("A2AClient", "Received response from message/send: $response");
        if (response.containsKey("error")) {
            throw _exceptionFrom(response["error"]!!.jsonObject);
        }
        return DefaultJson.decodeFromJsonElement(response["result"]!!);
    }

    suspend fun messageStream(message: Message): Flow<Event> {
        Log.i("", "Sending message for stream: ${message.messageId}");
        val params = mutableMapOf<String, Any?>(
            "configuration" to null,
            "metadata" to null,
            "message" to Json.encodeToString(message),
        )
        if (message.extensions != null) {
            params["extensions"] = message.extensions;
        }
        val messageMap = mutableMapOf<String, Any?>(
            "jsonrpc" to "2.0",
            "method" to "message/stream",
            "params" to params,
            "id" to _requestId++,
        );
        val headers = mutableMapOf<String, String>();
        if (message.extensions != null) {
            headers["X-A2A-Extensions"] = message.extensions.joinToString(",");
        }
        val stream = transport.sendStream(messageMap, headers = headers)
        return stream.transform { data ->
            try {
                Log.d("", "Received event from stream: ${sanitizeLogData(data)}")
            } catch (e: Exception) {
                Log.w("", "Error logging event from stream: $e")
            }
            if ("error" in data) {
                val error = data["error"]?.jsonObject
                    ?: throw IllegalStateException("Invalid error payload")
                throw _exceptionFrom(error)
            } else {
                val kind = data["kind"]?.jsonPrimitive?.contentOrNull
                if (kind != null) {
                    if (kind == "task") {
                        val task = Json.decodeFromJsonElement<Task>(data)
                        emit(
                            Event.statusUpdate(
                                taskId = task.id,
                                contextId = task.contextId,
                                status = task.status,
                                final = false
                            )
                        )
                    } else {
                        emit(Event.fromJson(data))
                    }
                }
            }
        }
    }

    /// Retrieves the current state of a task from the server using `tasks/get`.
    ///
    /// This method is used to poll the status of a task, identified by [taskId],
    /// that was previously initiated (e.g., via [messageSend]).
    ///
    /// Returns the current [Task] state. Throws an [A2AException] if the server
    /// returns a JSON-RPC error (e.g., task not found).
    suspend fun getTask(taskId: String): Task {
        Log.i("", "Getting task: $taskId");
        val params = mapOf(
            "jsonrpc" to "2.0",
            "method" to "tasks/get",
            "params" to mapOf("id" to taskId),
            "id" to _requestId++,
        )
        val response = transport.send(params);
        Log.i("", "Received response from tasks/get: $response");
        if (response.containsKey("error")) {
            throw _exceptionFrom(response["error"]!!.jsonObject);
        }
        return Json.decodeFromJsonElement(response["result"]!!);
    }

    companion object {
        /**
         * Creates an [A2AClient] by fetching an [AgentCard] and selecting the best
         * transport.
         *
         * Fetches the agent card from [agentCardUrl], determines the best transport
         * based on the card's capabilities (preferring streaming if available),
         * and returns a new [A2AClient] instance.
         **/
        suspend fun fromAgentCardUrl(agentCardUrl: String): A2AClient {
            val tempTransport = HttpTransport(url = agentCardUrl);
            val response = tempTransport[""]
            val agentCard = AgentCard.fromJson(response)
            val transport = if (agentCard.capabilities.streaming == true)
                SseTransport(url = agentCard.url)
            else HttpTransport(url = agentCard.url)
            return A2AClient(url = agentCard.url, transport = transport)
        }

        const val agentCardPath: String = "/.well-known/agent-card.json"
    }

}
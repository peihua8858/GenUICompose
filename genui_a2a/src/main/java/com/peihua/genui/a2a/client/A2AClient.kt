package com.peihua.genui.a2a.client

import android.util.Log
import com.peihua.genui.ILogger
import com.peihua.genui.Logger
import com.peihua.genui.a2a.a2aJson
import com.peihua.genui.a2a.client.A2AClient.Companion.agentCardPath
import com.peihua.genui.a2a.core.AgentCard
import com.peihua.genui.a2a.core.Event
import com.peihua.genui.a2a.core.ListTasksParams
import com.peihua.genui.a2a.core.ListTasksResult
import com.peihua.genui.a2a.core.Message
import com.peihua.genui.a2a.core.PushNotificationConfig
import com.peihua.genui.a2a.core.Task
import com.peihua.genui.a2a.core.TaskPushNotificationConfig
import com.peihua.genui.a2a.sanitizeLogData
import com.peihua.genui.utils.toInteger
import com.peihua.json.utils.toMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.serialization.json.Json
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

class A2AClient(
    private val url: String,
    private val transport: Transport,
    private val logger: ILogger = Logger
) {
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
        logger.dLog("Fetching agent card...");
        val response = transport[agentCardPath];
        logger.dLog("Received agent card: $response");
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
        logger.dLog("Fetching authenticated agent card...");
        val response = transport.get(agentCardPath, headers = mapOf("Authorization" to "Bearer $token"))
        logger.dLog("Received authenticated agent card: $response");
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
        logger.dLog("Sending message: ${message.messageId}");
        val params = mutableMapOf<String, Any>(
            "message" to message
        )
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
        logger.dLog("Received response from message/send: $response");
        if (response.containsKey("error")) {
            throw _exceptionFrom(response["error"]!!.jsonObject);
        }
        return Json.decodeFromJsonElement(response["result"]!!);
    }

    suspend fun messageStream(message: Message): Flow<Event> {
        logger.dLog("Sending message for stream: ${message.messageId}")
        val params = mutableMapOf<String, Any?>(
            "configuration" to null,
            "metadata" to null,
            "message" to message,
        )
        if (message.extensions != null) {
            params["extensions"] = message.extensions;
        }
        val messageMap = mutableMapOf(
            "jsonrpc" to "2.0",
            "method" to "message/stream",
            "params" to params,
            "id" to _requestId++,
        )
        val headers = mutableMapOf<String, String>()
        if (message.extensions != null) {
            headers["X-A2A-Extensions"] = message.extensions.joinToString(",")
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
                        val task = a2aJson.decodeFromJsonElement<Task>(data)
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

    /**
     * Retrieves the current state of a task from the server using `tasks/get`.
     *
     * This method is used to poll the status of a task, identified by [taskId],
     * that was previously initiated (e.g., via [messageSend]).
     *
     * Returns the current [Task] state. Throws an [A2AException] if the server
     * returns a JSON-RPC error (e.g., task not found).
     */
    suspend fun getTask(taskId: String): Task {
        logger.dLog("Getting task: $taskId")
        val params = mapOf(
            "jsonrpc" to "2.0",
            "method" to "tasks/get",
            "params" to mapOf("id" to taskId),
            "id" to _requestId++,
        )
        val response = transport.send(params);
        logger.dLog("Received response from tasks/get: $response");
        if (response.containsKey("error")) {
            throw _exceptionFrom(response["error"]!!.jsonObject)
        }
        return a2aJson.decodeFromJsonElement(response["result"]!!)
    }

    /**
     * Retrieves a list of tasks from the server using `tasks/list`.
     *
     * The optional [params] of type [ListTasksParams] can be provided to filter,
     * sort, and paginate the task list.
     *
     * Returns a [ListTasksResult] containing the list of tasks and pagination
     * info. Throws an [A2AException] if the server returns a JSON-RPC error.
     */
    suspend fun listTasks(params: ListTasksParams?): ListTasksResult {
        logger.dLog("Listing tasks...")
        val response = transport.send(
            mapOf(
                "jsonrpc" to "2.0",
                "method" to "tasks/list",
                "params" to params.toMap(),
                "id" to _requestId++,
            )
        )
        logger.dLog("Received response from tasks/list: $response")
        if (response.containsKey("error")) {
            throw _exceptionFrom(response["error"]!!.jsonObject)
        }
        return a2aJson.decodeFromJsonElement(response["result"]!!)
    }

    /**
     * Requests the cancellation of an ongoing task using `tasks/cancel`.
     *
     * The server will attempt to cancel the task identified by [taskId].
     * Success is not guaranteed, as the task might have already completed or may
     * not support cancellation.
     *
     * Returns the updated [Task] state after the cancellation request.
     * Throws an [A2AException] if the server returns a JSON-RPC error.
     */
    suspend fun cancelTask(taskId: String): Task {
        logger.dLog("Canceling task: $taskId");
        val response = transport.send(
            mapOf(
                "jsonrpc" to "2.0",
                "method" to "tasks/cancel",
                "params" to mapOf("id" to taskId),
                "id" to _requestId++,
            )
        );
        logger.dLog("Received response from tasks/cancel: $response")
        if (response.containsKey("error")) {
            throw _exceptionFrom(response["error"]!!.jsonObject)
        }
        return a2aJson.decodeFromJsonElement(response["result"]!!)
    }

    /**
     * Resubscribes to an SSE stream for an ongoing task using
     * `tasks/resubscribe`.
     *
     * This method allows a client to reconnect to the event stream of a task
     * identified by [taskId], for instance, after a network interruption. The
     * returned stream will emit subsequent [Event] objects for the task.
     *
     * Returns a [Flow] of [Event] objects. The stream will emit an
     * [A2AException] if the server returns a JSON-RPC error.
     */
    fun resubscribeToTask(taskId: String): Flow<Event> {
        logger.dLog("Resubscribing to task: $taskId");
        return transport
            .sendStream(
                mapOf(
                    "jsonrpc" to "2.0",
                    "method" to "tasks/resubscribe",
                    "params" to mapOf("id" to taskId),
                    "id" to _requestId++,
                )
            ).map {
                try {
                    logger.dLog("Received event from stream: ${sanitizeLogData(it)}");
                } catch (e: Exception) {
                    logger.wLog("Error logging event from stream: $e");
                }
                if (it.containsKey("error")) {
                    throw _exceptionFrom(it["error"]!!.jsonObject);
                }
                Event.fromJson(it);
            }
    }

    /** Closes the underlying transport connection.
     *
     * This should be called when the client is no longer needed to release
     * resources. */
    fun close() {
        transport.close()
    }

    /**
     * Sets or updates the push notification configuration for a task.
     *
     * Uses the `tasks/pushNotificationConfig/set` method.
     *
     * Returns the updated [TaskPushNotificationConfig].
     * Throws an [A2AException] if the server returns a JSON-RPC error.
     */
    suspend fun setPushNotificationConfig(params: TaskPushNotificationConfig): TaskPushNotificationConfig {
        logger.dLog("Setting push notification config for task: ${params.taskId}");
        val response = transport.send(
            mapOf(
                "jsonrpc" to "2.0",
                "method" to "tasks/pushNotificationConfig/set",
                "params" to params.toMap(),
                "id" to _requestId++,
            )
        );
        logger.dLog("Received response from tasks/pushNotificationConfig/set: $response")
        if (response.containsKey("error")) {
            throw _exceptionFrom(response["error"]!!.jsonObject)
        }
        return a2aJson.decodeFromJsonElement(response["result"]!!)
    }

    /** Retrieves a specific push notification configuration for a task.
     *
     * Uses the `tasks/pushNotificationConfig/get` method, identified by [taskId]
     * and [configId].
     *
     * Returns the requested [TaskPushNotificationConfig].
     * Throws an [A2AException] if the server returns a JSON-RPC error.
     */
    suspend fun getPushNotificationConfig(taskId: String, configId: String): TaskPushNotificationConfig {
        logger.dLog("Getting push notification config $configId for task: $taskId")
        val response = transport.send(
            mapOf(
                "jsonrpc" to "2.0",
                "method" to "tasks/pushNotificationConfig/get",
                "params" to mapOf("id" to taskId, "pushNotificationConfigId" to configId),
                "id" to _requestId++,
            )
        );
        logger.dLog("Received response from tasks/pushNotificationConfig/get: $response")
        if (response.containsKey("error")) {
            throw _exceptionFrom(response["error"]!!.jsonObject)
        }
        return a2aJson.decodeFromJsonElement(response["result"]!!)
    }

    /**
     * Lists all push notification configurations for a given task.
     *
     * Uses the `tasks/pushNotificationConfig/list` method, identified by [taskId].
     *
     * Returns a List of [PushNotificationConfig] objects.
     * Throws an [A2AException] if the server returns a JSON-RPC error.
     */
    suspend fun listPushNotificationConfigs(taskId: String): List<PushNotificationConfig> {
        logger.dLog("Listing push notification configs for task: $taskId");
        val response = transport.send(
            mapOf(
                "jsonrpc" to "2.0",
                "method" to "tasks/pushNotificationConfig/list",
                "params" to mapOf("id" to taskId),
                "id" to _requestId++,
            )
        );
        logger.dLog("Received response from tasks/pushNotificationConfig/list: $response");
        if (response.containsKey("error")) {
            throw _exceptionFrom(response["error"]!!.jsonObject)
        }
        val result = response["result"]!!.jsonObject
        val configs = result["configs"]!!.jsonObject
        return configs.map {
            a2aJson.decodeFromJsonElement<PushNotificationConfig>(it.value)
        }.toList()
    }

    /**
     * Deletes a specific push notification configuration for a task.
     *
     * Uses the `tasks/pushNotificationConfig/delete` method, identified by [taskId]
     * and [configId].
     *
     * Throws an [A2AException] if the server returns a JSON-RPC error.
     */
    suspend fun deletePushNotificationConfig(taskId: String, configId: String) {
        logger.dLog("Deleting push notification config $configId for task: $taskId");
        val response = transport.send(
            mapOf(
                "jsonrpc" to "2.0",
                "method" to "tasks/pushNotificationConfig/delete",
                "params" to mapOf("id" to taskId, "pushNotificationConfigId" to configId),
                "id" to _requestId++,
            )
        );
        logger.dLog(
            "Received response from tasks/pushNotificationConfig/delete: $response",
        );
        if (response.containsKey("error")) {
            throw _exceptionFrom(response["error"]!!.jsonObject);
        }
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
            val tempTransport = HttpTransport(url = agentCardUrl, logger = Logger)
            val response = tempTransport[""]
            val agentCard = AgentCard.fromJson(response)
            val transport = if (agentCard.capabilities.streaming)
                SseTransport(url = agentCard.url)
            else HttpTransport(url = agentCard.url, logger = Logger)
            return A2AClient(url = agentCard.url, logger = Logger, transport = transport)
        }

        const val agentCardPath: String = "/.well-known/agent-card.json"
    }

}
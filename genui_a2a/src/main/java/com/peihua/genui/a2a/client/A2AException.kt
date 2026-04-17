package com.peihua.genui.a2a.client

import kotlinx.serialization.json.JsonObject

abstract class A2AException(message: String? = "") : Exception(message) {
    companion object {
        /**
         *  code, and the issue is not a specific JSON-RPC error.
         */
        fun http(
            //The HTTP status code (e.g., 404, 500).
            statusCode: Int,
            //An optional human-readable reason phrase associated with the status code.
            reason: String?,
        ): A2AException {
            return A2AHttpException(statusCode, reason)
        }

        fun network(message: String): A2ANetworkException = A2ANetworkException(message)
        fun unsupported(message: String): A2AUnsupportedException = A2AUnsupportedException(message)
        fun parsing(message: String): A2AParsingException = A2AParsingException(message)
        fun taskNotFound(message: String, data: JsonObject?): A2ATaskNotFoundException = A2ATaskNotFoundException(message, data)
        fun taskNotCancelable(message: String, data: JsonObject?): A2ATaskNotCancelableException =
            A2ATaskNotCancelableException(message, data)

        fun pushNotificationNotSupported(message: String, data: JsonObject?): A2APushNotificationNotSupportedException =
            A2APushNotificationNotSupportedException(message, data)

        fun pushNotificationConfigNotFound(message: String, data: JsonObject?): A2APushNotificationConfigNotFoundException =
            A2APushNotificationConfigNotFoundException(message, data)

        fun jsonRpc(code: Int, message: String, data: JsonObject?): A2AJsonRpcException = A2AJsonRpcException(code, message, data)

    }
}

class A2AHttpException(val statusCode: Int, val reason: String?, type: String? = "http") : A2AException() {
    override val message: String?
        get() = super.message

    override fun toString(): String {
        return "A2AException.http(statusCode: $statusCode, reason: $reason)"
    }
}

class A2ANetworkException(message: String) : A2AException(message)
class A2AUnsupportedException(message: String) : A2AException(message)
class A2AParsingException(message: String) : A2AException(message)
class A2ATaskNotFoundException(message: String, val _data: Map<String, Any?>?) : A2AException(message) {
    val data: Map<String, Any?>?
        get() {
            val value = _data;
            if (value == null) return null;
            // ignore: implicit_dynamic_type
            return value.toMap()
        }
}

class A2ATaskNotCancelableException(message: String, val data: Map<String, Any?>?) : A2AException(message)
class A2APushNotificationNotSupportedException(message: String, val data: Map<String, Any?>?) : A2AException(message)
class A2APushNotificationConfigNotFoundException(message: String, val data: Map<String, Any?>?) : A2AException(message)
class A2AJsonRpcException(val code: Int, message: String, val data: Map<String, Any?>?) : A2AException(message)

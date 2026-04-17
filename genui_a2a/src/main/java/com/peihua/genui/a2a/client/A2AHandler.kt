package com.peihua.genui.a2a.client

import kotlinx.coroutines.Deferred
import java.util.concurrent.Future

interface A2AHandler {
    /** Handles the request and can modify it before it is sent.*/
    suspend fun handleRequest(request: Map<String, Any?>): Deferred<Map<String, Any>>;

    /**
     * Handles the response and can modify it before it is returned to the
     * caller.
     */
    suspend fun handleResponse(response: Map<String, Any>): Deferred<Map<String, Any>>
}

/**
 * A pipeline for executing a series of [A2AHandler]s.
 */
class A2AHandlerPipeline(
    //The list of handlers to execute.
    val handlers: List<A2AHandler>,
) {

    /**
    * Executes the request handlers in order.
     */
    suspend fun handleRequest(request: Map<String, Any>): Deferred<Map<String, Any>> {
        var currentRequest = request;
        for (handler in handlers) {
            currentRequest = handler.handleRequest(currentRequest).await()
        }
        return currentRequest;
    }

    /**
     * Executes the response handlers in reverse order.
     */
    suspend fun handleResponse(response: Map<String, Any>): Future<Map<String, Any>> {
        var currentResponse = response;
        for (handler in handlers.reversed()) {
            currentResponse = handler.handleResponse(currentResponse).await()
        }
        return currentResponse;
    }
}
package com.peihua.genui.a2a.client

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.path
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.IOException

/**
 * A [Transport] implementation using Server-Sent Events (SSE) for streaming.
 *
 * This class extends [HttpTransport] to add support for streaming responses
 * from the server via an SSE connection. It should be used for methods like
 * `message/stream` where the server pushes multiple events over time.
 *    Creates an [SseTransport] instance.
 *
 * Inherits parameters from [HttpTransport]:
 *   - [url]: The base URL of the A2A server.
 *   - [authHeaders]: Optional additional authorization headers.
 *   - [client]: Optional [http.Client] for custom configurations or testing.
 *   - [log]: Optional [Logger] instance.
 **/
class SseTransport(url: String, authHeaders: Map<String, String> = mapOf(), client: HttpClient? = null) : HttpTransport(url, authHeaders, client) {
    override  fun sendStream(request: Map<String, Any?>, headers: Map<String, String>): Flow<Map<String, Any?>> = flow {
        val thisUrl = url
        val allHeaders = Headers.build {
            authHeaders.forEach { append(it.key, it.value) }
            headers.forEach { append(it.key, it.value) }
        }
        try {
            val response = client.request {
                this.headers.appendAll(allHeaders)
                this.url.path(thisUrl)
                this.setBody(request)
                this.method = HttpMethod.Post
            }
            if (response.statusCode >= 400) {
                val responseBody = response.bodyAsText()
                Log.e("HttpTransport", "Received error response: ${response.statusCode} $responseBody")
                throw A2AException.http(
                    statusCode = response.statusCode,
                    reason = "${response.message} $responseBody",
                );
            }
            SseParser().parse(response.readLine()).collect {
                emit(it)
            }
        } catch (e: IOException) {
            throw A2AException.network(message = e.toString());
        } catch (e: Exception) {
            if (e is A2AException) {
                throw e;
            }
            // Catch any other unexpected errors during stream processing.
            throw A2AException.network(message = "SSE stream error: $e");
        }
    }
}
package com.peihua.genui.a2a.client

import android.util.Log
import com.peihua.genui.a2a.DefaultJson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.path
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import java.io.IOException

/**
 * An implementation of the [Transport] interface using standard HTTP requests.
 *
 * This transport is suitable for single-shot GET requests and POST requests
 * for non-streaming JSON-RPC calls. It does not support [sendStream].
 * Creates an [HttpTransport] instance.
 *
 * Parameters:
 *   - [url]: The base URL of the A2A server.
 *   - [authHeaders]: Optional additional headers.
 *   - [client]: Optional [http.Client] for custom configurations or testing.
 *   - [log]: Optional [Logger] instance.
 **/
open class HttpTransport(val url: String, override val authHeaders: Map<String, String> = mapOf(), client: HttpClient? = null) : Transport {
    protected val client: HttpClient = client ?: HttpClient()

    @OptIn(InternalAPI::class)
    override suspend fun get(
        path: String,
        headers: Map<String, String>,
    ): JsonObject {
        val uri = "$url$path"
        val allHeaders = Headers.build {
            authHeaders.forEach { append(it.key, it.value) }
            headers.forEach { append(it.key, it.value) }
        }
        Log.d("HttpTransport", "Sending GET request to $uri with headers: $allHeaders");
        try {
            val response = client.get {
                this.headers.appendAll(allHeaders)
                this.url.path(uri)
            }
            Log.d("HttpTransport", "Received response from GET $uri: ${response.rawContent}")
            if (response.statusCode >= 400) {
                throw A2AException.http(
                    statusCode = response.statusCode,
                    reason = response.message,
                )
            }
            return DefaultJson.decodeFromString(response.bodyAsText())
        } catch (e: IOException) {
            throw A2AException.network(e.toString())
        }
    }

    override suspend fun send(
        request: Map<String, Any?>,
        path: String,
        headers: Map<String, String>,
    ): JsonObject {
        val uri = "$url$path"
        Log.d("HttpTransport", "Sending POST request to $uri with body: $request");
        val allHeaders = Headers.build {
            authHeaders.forEach { append(it.key, it.value) }
            headers.forEach { append(it.key, it.value) }
            append("Content-Type", "application/json")

        }
        try {
            val response = client.request {
                this.headers.appendAll(allHeaders)
                this.url.path(uri)
                this.setBody(request)
                this.method = HttpMethod.Post
            }
            Log.d("HttpTransport", "Received response from POST $uri: ${response.readText()}");
            if (response.statusCode >= 400) {
                throw A2AException.http(
                    statusCode = response.statusCode,
                    reason = response.message,
                )
            }
            return DefaultJson.decodeFromString(response.bodyAsText())
        } catch (e: IOException) {
            throw A2AException.network(e.toString())
        } catch (e: SerializationException) {
            throw A2AException.parsing(e.toString())
        }
    }

    override fun sendStream(
        request: Map<String, Any?>,
        headers: Map<String, String>,
    ): Flow<JsonObject> {
        throw A2AException.unsupported("Streaming is not supported by HttpTransport. Use SseTransport instead.")
    }

    override fun close() {
        client.close()
    }
}
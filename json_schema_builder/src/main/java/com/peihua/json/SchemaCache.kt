package com.peihua.json

import com.peihua.json.schema.Schema
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.Json
import java.net.URI

class SchemaCache(
    val externalHttpClient: HttpClient? = null,
    val internalHttpClient: HttpClient? = null,
    val cache: MutableMap<String, Schema> = mutableMapOf(),
    val loggingContext: LoggingContext? = null,
    val fileLoader: SchemaCacheFileLoader = SchemaCacheFileLoader(),
) {


    val httpClient: HttpClient?
        get() = externalHttpClient ?: internalHttpClient

    fun close() {
        internalHttpClient?.close()
    }

   suspend fun get(uri: URI): Schema? {
        val uriString = uri.toString()
       if (cache.containsKey(uriString)) {
            return cache[uriString]
        }

        try {
            var content: String
            when (uri.scheme) {
                "file" -> {
                    content = fileLoader.getFile(uri)
                }
                "http", "https" -> {
                    val response: HttpResponse? = httpClient?.get(uri.toURL())
                    val statusCode = response?.status?.value
                    if (statusCode != 200) {
                        throw SchemaFetchException(uri, Exception("Failed to fetch schema: $statusCode"))
                    }
                    content = response.body()
                }
                else -> {
                    // Unsupported scheme
                    throw SchemaFetchException(uri, Exception("Unsupported scheme: ${uri.scheme}"))
                }
            }
            val schema = Schema.fromMap(Json.decodeFromString(content))
            cache[uriString] = schema
            return schema
        } catch (e: Exception) {
            loggingContext?.log("Error fetching remote schema from $uri: $e")
            throw SchemaFetchException(uri, e)
        }
    }
}
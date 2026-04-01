package com.peihua.genui.a2a.client

import android.util.Log
import com.peihua.genui.a2a.sanitizeLogData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class SseParser {

    /// Parses a stream of SSE lines and returns a stream of JSON objects.
    fun parse(lines: Flow<String>): Flow<Map<String, Any?>> = flow {
        var data = mutableListOf<String>();
        try {
            lines.collect { line ->
                val lineData = if (line.length < 300) line else line.substring(0, 300)
                Log.d("", "Received SSE line: ${line.length} $lineData...")
                when {
                    line.startsWith("data:") -> data.add(line.substring(5).trim())
                    line.startsWith(":") -> {
                        // Ignore comments (used for keepalives)
                        Log.w("", "Ignoring SSE comment: $line");
                    }

                    line.isEmpty() -> {
                        // Event boundary
                        if (data.isNotEmpty()) {
                            val result = _parseData(data)
                            data = mutableListOf<String>() // Clear for next event
                            if (result != null) {
                                emit(result);
                            }
                        }
                    }

                    else -> {
                        Log.w("", "Ignoring unexpected SSE line: $line");
                    }
                }
            }

            if (data.isNotEmpty()) {
                Log.w("", "End of stream reached with ${data.size} lines of data pending.");
                val result = _parseData(data);
                if (result != null) {
                    emit(result)
                }
            }
            // ignore: avoid_catching_errors
        } catch (e: Error) {
            throw A2AException.parsing(message = "Stream closed unexpectedly.");
        }
    }

    fun _parseData(data: List<String>): Map<String, Any?>? {
        val dataString = data.joinToString(separator = "\n");
        if (dataString.isNotEmpty()) {
            try {
                val jsonData = Json.decodeFromString<Map<String, Any?>>(dataString) as Map<String, Any?>;
                try {
                    Log.w("", "Parsed JSON: ${sanitizeLogData(jsonData)}")
                } catch (e: Exception) {
                    Log.w("", "Error logging parsed JSON: $e");
                }
                if (jsonData.containsKey("result")) {
                    val result = jsonData["result"]
                    if (result != null) {
                        return result as Map<String, Any?>;
                    } else {
                        Log.w("", "Received a null result in the SSE stream.");
                    }
                } else if (jsonData.containsKey("error")) {
                    val error = jsonData["error"] as Map<String, Any?>;
                    throw A2AException.jsonRpc(
                        code = error["code"] as Int,
                        message = error["message"] as String,
                        data = error["data"] as Map<String, Any?>?,
                    );
                }
            } catch (e: Exception) {
                if (e is A2AException) throw e;
                throw A2AException.parsing(message = e.toString());
            }
        }
        return null;
    }
}
package com.peihua.genui.a2a

/**
 * Sanitizes data for logging purposes.
 *
 * This function recursively traverses the given [data] and replaces the value
 * of any key named "bytes" with the string `"&lt;binary bytes&gt;"`. This is
 * useful for preventing large binary data from cluttering log output.
 **/
fun sanitizeLogData(data: Any?): Any? {
    if (data is Map<*, *>) {
        val sanitized = mutableMapOf<String, Any?>()
        for (entry in data.entries) {
            val key = entry.key.toString()
            if (key == "bytes") {
                sanitized[key] = "<binary bytes>"
            } else {
                sanitized[key] = sanitizeLogData(entry.value)
            }
        }
        return sanitized
    } else if (data is List<*>) {
        return data.map {
            sanitizeLogData(it)
        }.toList()
    }
    return data
}

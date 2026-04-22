package com.peihua.json

import java.time.LocalTime

/// A function that validates a string against a format.
///
/// Returns `true` if the string is valid, and `false` otherwise.
typealias FormatValidator = (String) -> Boolean;

/// A map of format names to their validation functions.
///
/// This is used to validate string formats like 'date-time', 'email', etc.
///
/// Note: the field `Duration` is not supported.
val formatValidators: Map<String, FormatValidator>
    get(){
        return mapOf(
            "date-time" to { value -> _isTime(value) || _isDate(value) },
            "date" to ::_isDate,
            "time" to ::_isTime,
            "email" to { value ->
                android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()
            },
            "email" to { value -> value.contains("@") },
            "ipv4" to { value ->
                val parts = value.split(".")
                if (parts.size != 4) {
                    false
                } else {
                    parts.all { part ->
                        val n = part.toIntOrNull()
                        n != null && n in 0..255
                    }
                }
            },
            "ipv6" to { value ->
                try {
                    java.net.InetAddress.getByName(value) is java.net.Inet6Address
                } catch (e: Exception) {
                    false
                }
            }
        )
    }

fun _isTime(value: String): Boolean {
    return LocalTime.parse("0000-01-01T$value") != null;
}


fun _isDate(value: String): Boolean {
    return LocalTime.parse("${value}T00:00:00Z") != null;
}

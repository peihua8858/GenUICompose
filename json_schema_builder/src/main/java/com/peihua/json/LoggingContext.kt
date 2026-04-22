package com.peihua.json

/// A context for logging validation steps.
///
/// This is used for debugging purposes to trace the validation process.
class LoggingContext(
    // Whether logging is enabled.
    val enabled: Boolean,
) {
    /// The buffer that accumulates the log messages.
    val buffer = StringBuffer();

    /// Logs a message to the buffer if logging is enabled.
    fun log(message: String) {
        if (enabled) {
            buffer.append(message).append("\n");
        }
    }
}
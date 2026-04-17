package com.peihua.genui.primitives


typealias CancellationListener = () -> Unit

class CancellationSignal {
    private var _isCancelled: Boolean = false
    private val _listeners: MutableList<CancellationListener> = mutableListOf()
    val isCancelled: Boolean get() = _isCancelled
    fun cancel() {
        if (_isCancelled) return;
        _isCancelled = true;
        for (listener in _listeners) {
            listener()
        }
    }

    fun addListener(listener: CancellationListener) {
        if (_isCancelled) {
            listener();
        } else {
            _listeners.add(listener);
        }
    }

    fun removeListener(listener: CancellationListener) {
        _listeners.remove(listener);
    }
}

/// An exception thrown when an operation is cancelled.
class CancellationException(override val message: String? = null) : Exception(message) {
    /// Creates a [CancellationException].
    override fun toString(): String {
        return if (message.isNullOrEmpty()) {
            "CancellationException"
        } else {
            "CancellationException: $message"
        }
    }
}

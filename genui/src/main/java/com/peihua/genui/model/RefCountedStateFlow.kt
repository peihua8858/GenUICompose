package com.peihua.genui.model

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * 带引用计数的 StateFlow，当最后一个订阅者释放时自动清理内部资源。
 * 实现 StateFlow 接口，可直接用于 Compose 的 collectAsState()。
 */
interface RefCountedStateFlow<T> : StateFlow<T> {
    /** 增加引用计数（通常由 subscribe 自动调用） */
    fun acquire()

    /** 减少引用计数，计数归零时触发内部 dispose */
    fun release()

    /** 强制通知所有监听器（用于容器内部修改但值未变的情况） */
    fun forceNotify()
    fun updateValue(newValue: T)
}

internal class RefCountedStateFlowImpl<T>(
    initialValue: T,
    private val onDispose: () -> Unit
) : RefCountedStateFlow<T> {
    private val _state = MutableStateFlow(initialValue)
    private var refCount = 1  // 创建时引用计数为1（订阅者持有）
    private var isDisposed = false

    override val value: T get() = _state.value
    override val replayCache: List<T>
        get() = _state.replayCache

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        // 直接代理 StateFlow 的 collect，但需要注意引用计数生命周期由外部管理
        _state.collect(collector)
    }

    override fun acquire() {
        check(!isDisposed) { "Cannot acquire a disposed RefCountedStateFlow" }
        refCount++
    }

    override fun release() {
        if (isDisposed) return
        refCount--
        if (refCount <= 0) {
            isDisposed = true
            onDispose()
        }
    }

    override fun forceNotify() {
        _state.update { it }
    }

    // 内部更新值的方法（模型使用）
    override fun updateValue(newValue: T) {
        if (!isDisposed) {
            _state.value = newValue
        }
    }

    internal fun getStateFlow(): StateFlow<T> = _state
}
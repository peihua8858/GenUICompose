package com.peihua.genui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import com.peihua.genui.ILogger
import com.peihua.genui.Logger
import com.peihua.json.utils.toInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

typealias Function<T> = (path: DataPath, source: State<T>, twoWay: Boolean) -> Unit
typealias VoidCallback = () -> Unit

interface DataModel {
    /**
     * Updates the data model at a specific absolute path and notifies all
     * relevant subscribers.
     *
     * If [absolutePath] is root, the entire data model is replaced
     * (if contents is a Map).
     */
    fun update(absolutePath: DataPath, contents: Any?)
    /** Subscribes to a specific absolute path in the data model.*/
    fun <T> subscribe(path: DataPath): RefCountedStateFlow<T?>
    /**
     * Binds an external state [source] to a [path] in the DataModel.
     *
     * **Side Effect:** Calling this method immediately performs a synchronous
     * `update()` on the DataModel at the specified [path] using the current
     * value of the [source].
     *
     * If [twoWay] is true, changes in the DataModel at [path] will also
     * update the [source] (assuming [source] is a [ValueNotifier]).
     *
     * Returns a function that disposes the binding.
     */
    suspend fun <T> bindExternalState(
        scope: CoroutineScope,
        path: DataPath,
        source: MutableStateFlow<T>,
        twoWay: Boolean
    ): VoidCallback
    /**
     * Retrieves a static, one-time value from the data model at the
     * specified absolute path without creating a subscription.
     */
    fun <T> getValue(path: DataPath): T?

    /** Disposes resources and bindings.*/
    fun dispose()
}

// 订阅缓存：路径 -> 引用计数 StateFlow
private val _subscriptions = ConcurrentHashMap<DataPath, RefCountedStateFlow<Any?>>()

// 外部绑定清理函数集合
private val _externalCleanups = mutableListOf<() -> Unit>()

// 内部数据存储（可变 Map，支持路径修改）
private var _data: MutableMap<String, Any?> = mutableMapOf()

/** Standard in-memory implementation of [DataModel].*/
internal class InMemoryDataModel(private val logger: ILogger = Logger) : DataModel {


    // 订阅缓存：路径 -> 引用计数 StateFlow
    private val _subscriptions = ConcurrentHashMap<DataPath, RefCountedStateFlow<Any?>>()

    // 外部绑定清理函数集合
    private val _externalCleanups = mutableListOf<() -> Unit>()

    private val _cleanupCallbacks: MutableList<VoidCallback> = mutableListOf()
    private val _externalSubscriptions: MutableList<VoidCallback> = mutableListOf();
    override fun update(absolutePath: DataPath, contents: Any?) {
        logger.dLog("DataModel.update: path=$absolutePath, contents=${contents}")
        if (absolutePath.isRoot) {
            _data = when (contents) {
                is Map<*, *> -> contents.mapKeys { it.key.toString() }.toMutableMap()
                null -> mutableMapOf()
                else -> {
                    logger.wLog("DataModel.update: contents for root path is not a Map: $contents")
                    mutableMapOf()
                }
            }
            notifySubscribers(DataPath.root)
            return
        }

        updateValue(_data, absolutePath.segments, contents)
        notifySubscribers(absolutePath)
    }

    override fun <T> subscribe(path: DataPath): RefCountedStateFlow<T?> {
        logger.dLog("DataModel.subscribe: path=$path")
        val existing = _subscriptions[path] as? RefCountedStateFlowImpl<T?>
        if (existing != null) {
            existing.acquire()
            return existing
        }
        val initialValue = getValue<T>(path)
        val notifier = RefCountedStateFlowImpl(initialValue) {
            _subscriptions.remove(path)
        }
        _subscriptions[path] = notifier as RefCountedStateFlowImpl<Any?>
        return notifier
    }

    override suspend fun <T> bindExternalState(
        scope: CoroutineScope,
        path: DataPath,
        source: MutableStateFlow<T>,
        twoWay: Boolean
    ): VoidCallback {
        // 初始同步
        update(path, source.value)
        // 单向：源 -> 模型
        val sourceJob = source.onEach { newValue ->
            val currentValue = getValue<T>(path)
            if (currentValue != newValue) {
                update(path, newValue)
            }
        }.launchIn(scope)  // 实际应传入 CoroutineScope，为简化使用 GlobalScope
        val removeSourceListener = { sourceJob.cancel() }
        _externalCleanups.add(removeSourceListener)
        var removeModelListener: (() -> Unit)? = null
        if (twoWay) {
            // 双向：模型变化时同步回源
            val modelFlow = subscribe<T>(path)
            val modelJob = modelFlow.onEach { modelValue ->
                if (modelValue != null && modelValue != source.value) {
                    source.value = modelValue
                }
            }.launchIn(scope)
            removeModelListener = { modelJob.cancel() }
            _externalCleanups.add(removeModelListener)
        }
        return {
            removeSourceListener()
            _externalCleanups.remove(removeSourceListener)
            removeModelListener?.let {
                it()
                _externalCleanups.remove(it)
            }
        }
    }

    override fun dispose() {
        // 执行所有外部清理
        _externalCleanups.forEach { it() }
        _externalCleanups.clear()

        // 释放所有订阅（通知每个 notifier 释放其引用，最终清理缓存）
        _subscriptions.values.forEach { it.release() }
        _subscriptions.clear()
    }

    override fun <T> getValue(path: DataPath): T? {
        try {
            return if (path.isRoot) {
                _data as T?
            } else {
                val value = getValueAtPath(_data, path.segments)
                value as T?
            }
        } catch (e: Exception) {
            throw DataModelTypeException(path)
        }
    }

    private fun getValueAtPath(current: Any?, segments: List<String>): Any? {
        if (segments.isEmpty()) return current
        val segment = segments.first()
        val remaining = segments.drop(1)

        return when (current) {
            is Map<*, *> -> {
                val next = current[segment]
                getValueAtPath(next, remaining)
            }

            is List<*> -> {
                val index = segment.toIntOrNull()
                if (index != null && index in current.indices) {
                    getValueAtPath(current[index], remaining)
                } else null
            }

            else -> null
        }
    }

    private fun updateValue(current: Any?, segments: List<String>, value: Any?) {
        if (segments.isEmpty()) return
        val segment = segments.first()
        val remaining = segments.drop(1)

        when (current) {
            is MutableMap<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val map = current as MutableMap<String, Any?>
                if (remaining.isEmpty()) {
                    if (value == null) map.remove(segment)
                    else map[segment] = value
                    return
                }

                var nextNode = map[segment]
                if (nextNode == null && value != null) {
                    val nextSegment = remaining.first()
                    nextNode =
                        if (nextSegment.toIntOrNull() != null) mutableListOf<Any?>() else mutableMapOf<String, Any?>()
                    map[segment] = nextNode
                }
                updateValue(nextNode, remaining, value)
            }

            is MutableList<*> -> {
                @Suppress("UNCHECKED_CAST")
                val list = current as MutableList<Any?>
                val index = segment.toIntOrNull() ?: return
                if (index < 0) return

                if (remaining.isEmpty()) {
                    if (index < list.size) {
                        list[index] = value
                    } else if (index == list.size && value != null) {
                        list.add(value)
                    }
                } else {
                    if (index < list.size) {
                        updateValue(list[index], remaining, value)
                    } else if (index == list.size) {
                        val nextSegment = remaining.first()
                        val newItem =
                            if (nextSegment.toIntOrNull() != null) mutableListOf<Any?>() else mutableMapOf<String, Any?>()
                        list.add(newItem)
                        updateValue(newItem, remaining, value)
                    }
                }
            }

            else -> {
                // 无法更新，忽略
            }
        }
    }

    private fun notifySubscribers(path: DataPath) {
        // 通知直接订阅此路径的监听器
        _subscriptions[path]?.let { notifier ->
            val newValue = getValue<Any?>(path)
            notifier.updateValue(newValue)
        }

        // 通知所有祖先路径（因为容器内容可能变化，但引用未变，需要强制通知）
        var parent = path
        while (parent.segments.isNotEmpty()) {
            parent = parent.dirname
            _subscriptions[parent]?.let { notifier ->
                val newValue = getValue<Any?>(parent)
                if (newValue != notifier.value) {
                    notifier.updateValue(newValue)
                } else {
                    notifier.forceNotify()  // 容器内部修改，值引用相同但内容已变
                }
            }
            if (parent.isRoot) break
        }

        // 通知所有子孙路径（因为子路径数据可能变化）
        _subscriptions.keys.toList().forEach { subPath ->
            if (subPath != path && subPath.startsWith(path)) {
                _subscriptions[subPath]?.let { notifier ->
                    notifier.updateValue(getValue(subPath))
                }
            }
        }
    }

    private fun _getValue(current: Any?, segments: List<String>): Any? {
        if (segments.isEmpty()) {
            return current;
        }

        val segment = segments.first()
        val remaining = segments.subList(1, segments.size);

        if (current is Map<*, *>) {
            return _getValue(current[segment], remaining);
        } else if (current is List<*>) {
            val index = segment.toInteger();
            if (index >= 0 && index < current.size) {
                return _getValue(current[index], remaining);
            }
        }
        return null;
    }
}

/**
 * 在 Composable 中安全订阅数据，自动处理生命周期释放。
 */
@Composable
fun <T : Any> DataModel.observeAsState(path: DataPath): T? {
    val flow = remember(this, path) { subscribe<T>(path) }
    DisposableEffect(flow) {
        onDispose { flow.release() }
    }
    return flow.collectAsState().value
}

/**
 * 双向绑定：将 Compose 的 MutableState 绑定到数据模型。
 */
@Composable
fun <T : Any> DataModel.BindState(
    path: DataPath,
    state: MutableState<T>,
    twoWay: Boolean = true
) {
    val modelFlow = remember(this, path) { subscribe<T>(path) }
    val scope = rememberCoroutineScope()
    DisposableEffect(modelFlow, state) {
        // 1. 模型 -> UI
        val modelJob = scope.launch {
            modelFlow.collect { modelValue ->
                if (modelValue != null && modelValue != state.value) {
                    state.value = modelValue
                }
            }
        }
        // 2. UI -> 模型（双向）
        val uiJob = if (twoWay) {
            scope.launch {
                // 使用 snapshotFlow 监听 state 的变化
                snapshotFlow { state.value }.collect { uiValue ->
                    val currentModelValue = getValue<T>(path)
                    if (uiValue != currentModelValue) {
                        update(path, uiValue)
                    }
                }
            }
        } else null
        onDispose {
            modelJob.cancel()
            uiJob?.cancel()
            modelFlow.release()
        }
    }
}

/**
 * Exception thrown when a value in the [DataModel] is not of the expected
 * type.
 */
class DataModelTypeException(
    /** The path where the type mismatch occurred.*/
    final val path: DataPath,

    /** The expected type.*/
    val expectedType: Type? = null,

    /** The actual type found.*/
    val actualType: Type? = null
) : Exception() {


    override fun toString(): String {
        return "DataModelTypeException: Expected $expectedType at $path, but found $actualType"
    }
}
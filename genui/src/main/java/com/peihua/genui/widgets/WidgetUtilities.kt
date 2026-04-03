package com.peihua.genui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.peihua.genui.model.DataContext
import com.peihua.genui.model.DataPath
import com.peihua.genui.primitives.JsonMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class WidgetUtilities {
}

/**
 * Resolves a context map definition against a [DataContext].
 */
suspend fun resolveContext(dataContext: DataContext, contextDefinition: JsonMap? = null): JsonMap {
    val resolved = mutableMapOf<String, Any?>()
    if (contextDefinition == null) return resolved;

    for (entry in contextDefinition) {
        val key = entry.key;
        val value = entry.value;
        resolved[key] = dataContext.resolve(value).first();
    }
    return resolved;
}

@Composable
fun <T> BoundValue(
    dataContext: DataContext,
    value: Any?,
    resolveFlow: (DataContext, Any?) -> Flow<T?>,
    builder: @Composable (T?) -> Unit,
) {
    val flow = remember(dataContext, value) {
        resolveFlow(dataContext, value)
    }

    val boundValue = flow.collectAsState(initial = null)
    builder(boundValue.value)
}

@Composable
fun BoundString(
    dataContext: DataContext,
    value: Any?,
    builder: @Composable (String?) -> Unit,
) {
    BoundValue(
        dataContext = dataContext,
        value = value,
        resolveFlow = { context, input ->
            when (input) {
                is Map<*, *> -> {
                    when {
                        input["path"] is String -> {
                            val path = input["path"] as String
                            context.subscribe<Any?>(DataPath(path)).map { it?.toString() }
                        }

                        input.containsKey("call") -> {
                            context.resolve(input)
                                .map { it?.toString() }
                        }

                        else -> {
                            flowOf(input.toString())
                        }
                    }
                }

                else -> {
                    flowOf(input?.toString())
                }
            }
        },
        builder = builder
    )
}

@Composable
fun BoundBool(
    dataContext: DataContext,
    value: Any?,
    builder: @Composable (Boolean?) -> Unit,
) {
    BoundValue(
        dataContext = dataContext,
        value = value,
        resolveFlow = { context, input ->
            when (input) {
                is Map<*, *> -> {
                    when {
                        input["path"] is String -> {
                            val path = input["path"] as String
                            dataContext.subscribe<Any?>(DataPath(path))
                                .map { v: Any? -> v as? Boolean }
                        }

                        input.containsKey("call") -> {
                            dataContext.resolve(value)
                                .map { v: Any? -> v as? Boolean }
                        }

                        else -> flowOf(value as? Boolean)
                    }
                }

                else -> flowOf(value as? Boolean)
            }
        },
        builder = builder
    )
}

@Composable
fun BoundList(
    dataContext: DataContext,
    value: Any?,
    builder: @Composable (List<Any?>?) -> Unit,
) {
    BoundValue(
        dataContext = dataContext,
        value = value,
        resolveFlow = { context, input ->
            return@BoundValue if (input is Map<*, *>) {
                when {
                    input["path"] is String -> {
                        val path = input["path"] as String
                        dataContext.subscribe<List<Any?>>(DataPath(path))
                    }

                    input.containsKey("call") -> {
                        dataContext.resolve(input)
                            .map { it as? List<*> }
                    }

                    else -> flowOf(input as? List<Any?>)
                }
            } else if (input is List<*>) {
                flowOf(input)
            } else {
                flowOf(null)
            }
        },
        builder = builder
    )
}

@Composable
fun BoundObject(
    dataContext: DataContext,
    value: Any?,
    builder: @Composable (Any?) -> Unit,
) {
    BoundValue(
        dataContext = dataContext,
        value = value,
        resolveFlow = { context, input ->
            return@BoundValue if (input is Map<*, *>) {
                when {
                    input["path"] is String -> {
                        val path = input["path"] as String
                        dataContext.subscribe<Any?>(DataPath(path))
                    }

                    input.containsKey("call") -> {
                        dataContext.resolve(input)
                            .map { it as? Any }
                    }

                    else -> flowOf(input as? Any?)
                }
            } else {
                flowOf(null)
            }
        },
        builder = builder
    )
}
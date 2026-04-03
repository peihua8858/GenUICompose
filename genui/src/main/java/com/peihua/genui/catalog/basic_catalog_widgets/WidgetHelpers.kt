package com.peihua.genui.catalog.basic_catalog_widgets

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.peihua.genui.model.ChildBuilderCallback
import com.peihua.genui.model.DataContext
import com.peihua.genui.model.GetComponentCallback
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.widgets.BoundObject

/**
 * Builder function for creating a widget from a template and a list of data.
 *
 * This is used by [ComponentChildrenBuilder] when children are defined by a
 * `template` which includes a `dataBinding` to a list in the [DataContext].
 */
typealias TemplateListWidgetBuilder = @Composable (data: Any?, componentId: String, path: String) -> Unit


/**
 * Builder function for creating a parent widget given a list of pre-built
 * [childIds].
 *
 * This is used by [ComponentChildrenBuilder] when children are defined as an
 * explicit list of component IDs.
 */
typealias ExplicitListWidgetBuilder = @Composable (
    childIds: List<String>,
    buildChild: ChildBuilderCallback,
    getComponent: GetComponentCallback,
    dataContext: DataContext,
) -> Unit

@Composable
fun ComponentChildrenBuilder(
    childrenData: Any?,
    dataContext: DataContext,
    buildChild: ChildBuilderCallback,
    getComponent: GetComponentCallback,
    explicitListBuilder: ExplicitListWidgetBuilder,
    templateListWidgetBuilder: TemplateListWidgetBuilder,
) {
    if (childrenData is List<*>) {
        val explicitList: List<String> = childrenData.map { e -> e as? String ?: e.toString() }.toList()
        explicitListBuilder.invoke(explicitList, buildChild, getComponent, dataContext)
        return
    }
    if (childrenData is Map<*, *>) {
        val childrenMap = childrenData as Map<*, *>;
        if (childrenMap.containsKey("path") &&
            childrenMap.containsKey("componentId")
        ) {
            val path = childrenMap["path"] as String;
            val componentId = childrenMap["componentId"] as String;
            Log.d("", "Widget $componentId subscribing to ${dataContext.path}");
            return BoundObject(
                dataContext = dataContext,
                value = { "path" to path },
                builder = { data ->
                    if (data != null) {
                        templateListWidgetBuilder(data, componentId, path);
                    }
                },
            );
        }
    }
}

/// Builds a child widget, wrapping it in a [Flexible] if a weight is provided
/// in the component.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowColumnScope.buildWeightedChild(
    modifier: Modifier,
    componentId: String,
    dataContext: DataContext,
    buildChild: ChildBuilderCallback,
    weight: Int?,
    fit: Boolean = false,
    key: Any? = null,
) {
    val child: @Composable () -> Unit = {
        if (weight != null) {
            Box(modifier = Modifier.weight(weight.toFloat(), fill = fit)) {
                buildChild(Modifier, componentId, dataContext)
            }
        } else {
            buildChild(modifier, componentId, dataContext)
        }
    }
    if (key != null) {
        key(key) { child() }
    } else {
        child()
    }
}

/**
 * Converts a list of validation checks into a single expression that evaluates
 * to true if all checks pass.
 */
fun checksToExpression(checks: List<JsonMap>?): Any? {
    if (checks == null || checks.isEmpty()) {
        return true;
    }
    // Combine all checks into a single 'and' condition
    return mapOf(
        "functionCall" to mapOf(
            "call" to "and",
            "args" to mapOf("values" to checks.map { c -> c["condition"] }.toList())
        ),
    );
}
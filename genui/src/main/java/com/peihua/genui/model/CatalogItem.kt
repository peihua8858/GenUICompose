package com.peihua.genui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peihua.json.schema.ObjectSchema
import com.peihua.json.schema.Schema

/**
 * A callback to get a component definition by its ID.
 */
typealias GetComponentCallback = (componentId: String) -> Component?;
/**
 * A callback that builds a child widget for a catalog item.
 */
typealias ChildBuilderCallback = @Composable (modifier: Modifier, id: String, dataContext: DataContext?) -> Unit

typealias ExampleBuilderCallback = () -> String;
typealias CatalogWidgetBuilder = @Composable (itemContext: CatalogItemContext) -> Unit;

data class CatalogItem(
    val name: String,
    private val schema: Schema,
    val widgetBuilder: CatalogWidgetBuilder,
    val exampleData: List<ExampleBuilderCallback> = listOf(),
    val isImplicitlyFlexible: Boolean = false,
) {
    val dataSchema: ObjectSchema
        get() {
            val originalMap = schema.value;
            val properties = originalMap["properties"] as? Map<String, Any> ?: mutableMapOf()
            val requiredProps = originalMap["required"] as? List<Any> ?: listOf()
            val result = mutableMapOf<String, Any>()
            result.putAll(originalMap)
            result["properties"] = mutableMapOf<String, Any>().apply {
                putAll(properties)
                put("component", mapOf("type" to "string", "enum" to listOf(name)))
            }
            val component = mutableListOf<Any>()
            component.add("component")
            component.addAll(requiredProps)
            result["required"] = component
            return ObjectSchema.fromMap(result)
        }
}

/**
 * Context provided to a [CatalogItem]'s widget builder.
 *
 * This class encapsulates all the information and callbacks needed to build
 * a catalog widget, including access to the widget's data, its position in
 * the component tree, and mechanisms for building children and dispatching
 * events.

 * Creates a [CatalogItemContext] with the required parameters.
 *
 * All parameters are required to ensure the widget builder has complete
 * context for rendering and interaction.
 **/
data class CatalogItemContext(
    /** The parsed data for this component from the AI-generated definition.**/
    val data: Any,
    /** The unique identifier for this component instance.**/
    val id: String,
    /** The type of this component.**/
    val type: String,
    /** Callback to build a child widget by its component ID.**/
    val buildChild: ChildBuilderCallback,
    /** Callback to dispatch UI events (e.g., button taps) back to the system.**/
    val dispatchEvent: DispatchEventCallback,
//    /** The Flutter [BuildContext] for this widget.**/
//    val buildContext: BuildContext,
    /** The [DataContext] for accessing and modifying the data model.**/
    val dataContext: DataContext,
    /** Callback to retrieve a component definition by its ID.**/
    val getComponent: GetComponentCallback,
    /** Callback to retrieve a catalog item definition by its type name.**/
    val getCatalogItem: (type: String) -> CatalogItem?,
    /** The ID of the surface this component belongs to.**/
    val surfaceId: String,
    /** Callback to report an error that occurred within this component.**/
    val reportError: (error: Throwable, stack: Array<StackTraceElement?>?) -> Unit,
    val animateTyping: Boolean = false,
) {

}
package com.peihua.genui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.peihua.genui.ILogger
import com.peihua.genui.Logger
import com.peihua.genui.primitives.JsonMap
import com.peihua.json.schema.S
import com.peihua.json.schema.Schema

data class Catalog(
    val items: List<CatalogItem>,
    val logger: ILogger = Logger,
    val functions: List<ClientFunction> = listOf(),
    val catalogId: String? = null,
    val systemPromptFragments: kotlin.collections.List<String> = listOf(),
) {
    /**
     * Builds a Flutter widget from a JSON-like data structure.
     */
    @Composable
    fun BuildWidget(modifier: Modifier, itemContext: CatalogItemContext) {
        val widgetType = itemContext.type
        val item = items.firstOrNull { it.name == widgetType }
        if (item == null) {
            throw CatalogItemNotFoundException(widgetType, catalogId = catalogId)
        }
        logger.iLog("Building widget ${item.name} with id ${itemContext.id}")
        return key(itemContext.id) {
            item.widgetBuilder(
                CatalogItemContext(
                    data = itemContext.data,
                    id = itemContext.id,
                    type = widgetType,
                    buildChild = { modifier, childId, childDataContext ->
                        itemContext.buildChild(
                            modifier,
                            childId,
                            childDataContext ?: itemContext.dataContext
                        )
                    },
                    dispatchEvent = itemContext.dispatchEvent,
                    dataContext = itemContext.dataContext,
                    getComponent = itemContext.getComponent,
                    getCatalogItem = { type -> items.firstOrNull { it.name == type } },
                    surfaceId = itemContext.surfaceId,
                    reportError = itemContext.reportError
                )
            )

        }
    }

    /**
     * Generates a JSON map suitable for inclusion in an inline catalog array
     * within `A2UiClientCapabilities`.
     *
     * This differs from [definition] because `a2ui_client_capabilities.json`
     * expects `components` to be a direct map of name to schema, and `functions`
     * to be an array of objects.
     */
    fun toCapabilitiesJson(): JsonMap {
        val json = mutableMapOf<String, Any>()
        if (catalogId != null) json["catalogId"] = catalogId
        json["components"] = items.associate {
            it.name to it.dataSchema.value
        }
        json["functions"] = functions.map {
            mapOf(
                "name" to it.name,
                "description" to it.description,
                "parameters" to it.argumentSchema.value,
                "returnType" to it.returnType.value,
            )
        }
        return json
    }

    /**
     * A dynamically generated [Schema] that describes all widgets in the
     * catalog.
     *
     * This schema is a "one-of" object, where the `widget` property can be one
     * of the schemas from the [items] in the catalog. This is used to inform
     * the generative AI model about the available UI components and their
     * expected data structures.
     */
    val definition: Schema
        get() {
            val componentProperties = items.associate {
                it.name to it.dataSchema
            }
            val functionProperties = functions.associate {
                it.name to it.argumentSchema
            }
            return S.obj(
                title = "A2UI Catalog Description Schema",
                description =
                    "A schema for a custom Catalog Description including A2UI components and styles.",
                properties = mapOf(
                    "components" to S.obj(
                        title = "A2UI Components",
                        description =
                            "A schema that defines a catalog of A2UI components. Each key is a component name, and each value is the JSON schema for that component's properties.",
                        properties = componentProperties,
                    ),
                    "styles" to S.obj(
                        title = "A2UI Styles",
                        description =
                            "A schema that defines a catalog of A2UI styles. Each key is a style name, and each value is the JSON schema for that style's properties.",
                        properties = mapOf(),
                    ),
                    "functions" to S.obj(
                        title = "A2UI Functions",
                        description = "A schema that defines a catalog of A2UI functions. Each key is a function name, and each value is the JSON schema for that function's arguments.",
                        properties = functionProperties,
                    ),
                ),
                required = listOf("components", "styles", "functions"),
            );
        }
}

/// An exception thrown when a requested item is not found in the [Catalog].
class CatalogItemNotFoundException(
    // The type of the widget that was not found.
    val widgetType: String,
    // The ID of the catalog that was searched.
    val catalogId: String? = null
) : Exception() {


    override fun toString(): String {
        val buffer = StringBuffer();
        buffer.append("CatalogItemNotFoundException: Item \"$widgetType\" was not found in catalog")
        if (catalogId != null) {
            buffer.append(" \"$catalogId\"");
        }
        return buffer.toString();
    }
}
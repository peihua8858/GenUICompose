package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.DataPath
import com.peihua.genui.primitives.JsonMap
import com.peihua.json.schema.S
import kotlin.collections.List
data class ColumnData(private val _json: JsonMap) {
    val children: Any? = _json["children"]
    val justify: String?
        get() {
            val value = _json["justify"]
            if (value is String) return value;
            return null;
        }
    val align: String?
        get() {
            val value = _json["align"]
            if (value is String) return value;
            return null;
        }
}

/**
 * A layout widget that arranges its children vertically.
 */

object Column {
    const val _verticalColumnSpacing = 8.0;

    val _schema = S.obj(
        description = "A layout widget that arranges its children vertically.",
        properties = mapOf(
            "justify" to S.string(
                description = "How children are aligned on the main axis. ",
                enumValues = listOf(
                    "start",
                    "center",
                    "end",
                    "spaceBetween",
                    "spaceAround",
                    "spaceEvenly",
                    "stretch", // Added stretch to match keys
                ),
            ),
            "align" to S.string(
                description = "How children are aligned on the cross axis. ",
                enumValues = listOf("start", "center", "end", "stretch"),
            ),
            "children" to A2uiSchemas.componentArrayReference(
                description = "Either an explicit list of widget IDs for the children, or a template with a data binding to the list of children.",
            ),
        ),
        required = listOf("children"),
    )

    fun _parseMainAxisAlignment(alignment: String?) =
        when (alignment) {
            "start" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.Top)
            "center" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.CenterVertically)
            "end" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.Bottom)
            "spaceBetween" -> Arrangement.SpaceBetween
            "spaceAround" -> Arrangement.SpaceAround
            "spaceEvenly" -> Arrangement.SpaceEvenly
            else -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.Top)
        };

    fun _parseCrossAxisAlignment(alignment: String?) =
        when (alignment) {
            "start" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.Start)
            "center" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.CenterHorizontally)
            "end" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.End)
            "stretch" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.Start)
            else -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.Start)
        };

    @OptIn(ExperimentalLayoutApi::class)
    val column = CatalogItem(
        name = "Column",
        dataSchema = _schema,
        widgetBuilder = { itemContext ->
            val columnData = ColumnData(itemContext.data as JsonMap);
            return@CatalogItem ComponentChildrenBuilder(
                childrenData = columnData.children,
                dataContext = itemContext.dataContext,
                buildChild = itemContext.buildChild,
                getComponent = itemContext.getComponent,
                explicitListBuilder = { childIds, buildChild, getComponent, dataContext ->
                    val isStretch = columnData.align == "stretch";
                    FlowColumn(
                        verticalArrangement = _parseMainAxisAlignment(columnData.justify),
                        horizontalArrangement = _parseCrossAxisAlignment(columnData.align),
                    ) {
                        val modifier = if (isStretch) {
                            Modifier.fillMaxColumnWidth()
                        } else {
                            Modifier
                        }
                        for ((index, childId) in childIds.withIndex()) {
                            val explicitWeight = getComponent(childId)?.properties["weight"] as? Int
                            val isImplicitlyFlexible = itemContext.getCatalogItem(getComponent(childId)?.type ?: "")?.isImplicitlyFlexible == true
                            val weight = explicitWeight ?: (if (isImplicitlyFlexible) 1 else null)
                            val fit = explicitWeight != null
                            BuildWeightedChild(modifier, childId, dataContext, buildChild, weight, fit)
                        }
                    }
                },
                templateListWidgetBuilder = { data, componentId, dataBinding ->
                    val values: List<Any?>
                    val keys: List<String>
                    if (data is List<*>) {
                        values = data;
                        keys = List(data.size) { index -> index.toString() }
                    } else if (data is Map<*, *>) {
                        values = data.values.toList();
                        keys = data.keys.map { it.toString() }
                    } else {
                        return@ComponentChildrenBuilder
                    }
                    val component = itemContext.getComponent(componentId);
                    val explicitWeight = component?.properties["weight"] as Int?
                    val isImplicitlyFlexible = itemContext.getCatalogItem(component?.type ?: "")?.isImplicitlyFlexible == true
                    val weight = explicitWeight ?: (if (isImplicitlyFlexible) 1 else null)
                    val fit = explicitWeight != null
                    val isStretch = columnData.align == "stretch";
                    FlowColumn(
                        verticalArrangement = _parseMainAxisAlignment(columnData.justify),
                        horizontalArrangement = _parseCrossAxisAlignment(columnData.align),
                    ) {
                        val modifier = if (isStretch) {
                            Modifier.fillMaxColumnWidth()
                        } else {
                            Modifier
                        }
                        for ((index, value) in values.withIndex()) {
                            BuildWeightedChild(
                                modifier = modifier,
                                componentId = componentId,
                                dataContext = itemContext.dataContext.nested(DataPath("$dataBinding/${keys[index]}")),
                                buildChild = itemContext.buildChild,
                                weight = weight,
                                fit = fit
                            )
                        }
                    }
                }
            )
        },
        exampleData = listOf(
            {
                """
                [
                  {
                      "id": "root",
                      "component": "Column",
                      "children": [
                             "advice_text",
                             "advice_options",
                             "submit_button"
                      ]
                  },
                  {
                        "id": "advice_text",
                        "component": "Text",
                        "text": "What kind of advice are you looking for?"
                  },
                   {
                        "id": "advice_options",
                        "component": "Text",
                        "text": "Some advice options."
                   },
                   {
                        "id": "submit_button",
                        "component": "Button",
                        "child": "submit_button_text",
                        "action": {
                            "event": {
                                 "name": "submit"
                            }
                        }
                   },
                   {
                        "id": "submit_button_text",
                        "component": "Text",
                        "text": "Submit"
                   }
                ]
                """.trimIndent()
            }
        )
    )
}
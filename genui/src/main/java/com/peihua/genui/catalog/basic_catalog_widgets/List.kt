package com.peihua.genui.catalog.basic_catalog_widgets

import android.util.Log
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peihua.genui.catalog.basic_catalog_widgets.Column._verticalColumnSpacing
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.DataPath
import com.peihua.genui.primitives.JsonMap
import com.peihua.json.schema.S
import kotlin.collections.List

data class ListData(private val _json: JsonMap) {
    val children: Any? = _json["children"]
    val direction: String? = _json["direction"] as? String
    val align: String? = _json["align"] as? String

    companion object {
        fun fromMap(json: JsonMap): ListData {
            return ListData(json)
        }

        fun create(child: Any?, direction: String? = null, align: String? = null): ListData {
            return fromMap(
                mapOf(
                    "children" to child,
                    "direction" to direction,
                    "align" to align
                )
            )
        }
    }
}

object List {
    val _schema = S.obj(
        description = "A scrollable list of child widgets.",
        properties = mapOf(
            "children" to A2uiSchemas.componentArrayReference(),
            "direction" to S.string(enumValues = listOf("vertical", "horizontal")),
            "align" to S.string(enumValues = listOf("start", "center", "end", "stretch")),
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

    private fun _parseCrossAxisAlignment(alignment: String?) =
        when (alignment) {
            "start" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.Start)
            "center" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.CenterHorizontally)
            "end" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.End)
            "stretch" -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.Start)
            else -> Arrangement.spacedBy(_verticalColumnSpacing.dp, Alignment.Start)
        };

    /**
     * A scrollable list of child widgets.
     *
     * This widget is analogous to Flutter's [ListView] widget. It can display
     * children in either a vertical or horizontal direction.
     *
     * ## Parameters:
     *
     * - `children`: A list of child widget IDs to display in the list.
     * - `direction`: The direction of the list. Can be `vertical` or `horizontal`.
     *   Defaults to `vertical`.
     * - `align`: The alignment of children along the cross axis. One of `start`,
     *   `center`, `end`, `stretch`.
     */
    @OptIn(ExperimentalLayoutApi::class)
    val list = CatalogItem(
        name = "List",
        dataSchema = _schema,
        widgetBuilder = { itemContext ->
            val listData = ListData.fromMap(itemContext.data as JsonMap)
            val direction = when (listData.direction) {
                "horizontal" -> Orientation.Horizontal
                "vertical" -> Orientation.Vertical
                else -> Orientation.Vertical
            }
            val crossAxisAlignment = when (listData.align) {
                "start" -> if (direction == Orientation.Vertical) Arrangement.Top else Arrangement.Start
                "center" -> Arrangement.Center
                "end" -> Arrangement.End
                "stretch" -> Arrangement.Start
                else -> Arrangement.Center
            }
            ComponentChildrenBuilder(
                listData.children,
                itemContext.dataContext,
                itemContext.buildChild,
                itemContext.getComponent,
                explicitListBuilder = { childIds, buildChild, getComponent, dataContext ->
                    for ((index, childId) in childIds.withIndex()) {
                        buildChild(Modifier, childId, dataContext)
                    }
                },
                templateListWidgetBuilder = { data, componentId, dataBinding ->
                    val values: List<Any?>
                    val keys: List<String>
                    when (data) {
                        is List<*> -> {
                            values = data
                            keys = List(data.size) { index -> index.toString() }
                        }

                        is Map<*, *> -> {
                            values = data.values.toList();
                            keys = data.keys.map { it.toString() }
                        }

                        else -> {
                            Log.w("", "List: invalid data type for template list: ${data?.javaClass?.name}")
                            return@ComponentChildrenBuilder
                        }
                    }
                    if (direction == Orientation.Vertical) {
                        FlowColumn(
                            Modifier,
                            verticalArrangement = _parseMainAxisAlignment(listData.align),
                        ) {
                            for ((index, value) in values.withIndex()) {
                                val nestedPath = "$dataBinding/${keys[index]}";
                                val itemDataContext = itemContext.dataContext
                                    .nested(DataPath(nestedPath));
                                itemContext.buildChild(Modifier, componentId, itemDataContext)
                            }
                        }
                    } else {
                        FlowRow(
                            Modifier,
                            horizontalArrangement = _parseCrossAxisAlignment(listData.align)
                        ) {
                            for ((index, value) in values.withIndex()) {
                                val nestedPath = "$dataBinding/${keys[index]}";
                                val itemDataContext = itemContext.dataContext
                                    .nested(DataPath(nestedPath));
                                itemContext.buildChild(Modifier, componentId, itemDataContext)
                            }
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
                        "component": "List",
                        "children": [
                            "text1",
                            "text2"
                        ]
                    },
                    {
                        "id": "text1",
                        "component": "Text",
                        "text": "First"
                    },
                    {
                        "id": "text2",
                        "component": "Text",
                        "text": "Second"
                    }
                ]
               """
            }
        ),
        isImplicitlyFlexible = true,
    )
}
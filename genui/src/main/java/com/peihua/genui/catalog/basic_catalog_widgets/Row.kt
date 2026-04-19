package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import com.peihua.genui.catalog.basic_catalog_widgets.Column._verticalColumnSpacing
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.DataPath
import com.peihua.genui.primitives.JsonMap
import com.peihua.json.schema.S

//extension type _RowData.fromMap(JsonMap _json) {
//  factory _RowData({Object? children, String? justify, String? align}) =>
//      _RowData.fromMap({
//        'children': children,
//        'justify': justify,
//        'align': align,
//      });
//
//  Object? get children => _json['children'];
//  String? get justify => _json['justify'] as String?;
//  String? get align => _json['align'] as String?;
//}
data class RowData(private val _json: JsonMap) {
    val children: Any? = _json["children"]
    val justify: String? = _json["justify"] as String?
    val align: String? = _json["align"] as String?

    companion object {
        fun fromMap(json: JsonMap): RowData {
            return RowData(json)
        }

        fun create(children: Any?, justify: String?, align: String?): RowData {
            return RowData(
                mapOf(
                    ("children" to children),
                    ("justify" to justify),
                    ("align" to align)
                )
            )
        }
    }
}

object Row {
    const val _horizontalRowSpacing = 16.0;

    val _schema = S.obj(
        description = "A layout widget that arranges its children horizontally.",
        properties = mapOf(
            "children" to A2uiSchemas.componentArrayReference(
                description = "Either an explicit list of widget IDs for the children, or a template with a data binding to the list of children.",
            ),
            "justify" to S.string(
                enumValues = listOf(
                    "start",
                    "center",
                    "end",
                    "spaceBetween",
                    "spaceAround",
                    "spaceEvenly",
                    "stretch",
                ),
            ),
            "align" to S.string(
                enumValues = listOf("start", "center", "end", "stretch")
            ),
        ),
        required = listOf("children"),
    )

    fun _parseMainAxisAlignment(alignment: String?) =
        when (alignment) {
            "start" -> Arrangement.spacedBy(_horizontalRowSpacing.dp, Alignment.Top)
            "center" -> Arrangement.spacedBy(_horizontalRowSpacing.dp, Alignment.CenterVertically)
            "end" -> Arrangement.spacedBy(_horizontalRowSpacing.dp, Alignment.Bottom)
            "spaceBetween" -> Arrangement.SpaceBetween
            "spaceAround" -> Arrangement.SpaceAround
            "spaceEvenly" -> Arrangement.SpaceEvenly
            else -> Arrangement.spacedBy(_horizontalRowSpacing.dp, Alignment.Top)
        };

    fun _parseCrossAxisAlignment(alignment: String?) =
        when (alignment) {
            "start" -> Arrangement.spacedBy(_horizontalRowSpacing.dp, Alignment.Start)
            "center" -> Arrangement.spacedBy(_horizontalRowSpacing.dp, Alignment.CenterHorizontally)
            "end" -> Arrangement.spacedBy(_horizontalRowSpacing.dp, Alignment.End)
            "stretch" -> Arrangement.spacedBy(_horizontalRowSpacing.dp, Alignment.Start)
            else -> Arrangement.spacedBy(_horizontalRowSpacing.dp, Alignment.Start)
        };

    /// A layout widget that arranges its children in a horizontal sequence.
///
/// This widget is analogous to Flutter's [Row] widget. It arranges a list of
/// child components from left to right.
///
/// ## Parameters:
///
/// - `children`: A list of child widget IDs to display in the row.
/// - `justify`: How the children should be placed along the main axis. Can
///   be `start`, `center`, `end`, `spaceBetween`, `spaceAround`, or
///   `spaceEvenly`. Defaults to `start`.
/// - `align`: How the children should be aligned on the cross axis. Can
///   be `start`, `center`, `end`, or `stretch`. Defaults to
///   `start`.
    @OptIn(ExperimentalLayoutApi::class)
    val row = CatalogItem(
        name = "Row",
        schema = _schema,
        widgetBuilder = { itemContext ->
            val rowData = RowData.fromMap(itemContext.data as JsonMap);
            ComponentChildrenBuilder(
                childrenData = rowData.children,
                dataContext = itemContext.dataContext,
                buildChild = itemContext.buildChild,
                getComponent = itemContext.getComponent,
                explicitListBuilder = { childIds, buildChild, getComponent, dataContext ->
                    FlowRow(
                        verticalArrangement = _parseMainAxisAlignment(rowData.justify),
                        horizontalArrangement = _parseCrossAxisAlignment(rowData.align),
                    ) {
                        for ((index, childId) in childIds.withIndex()) {
                            val explicitWeight = getComponent(childId)?.properties["weight"] as Int?;
                            val isImplicitlyFlexible =
                                itemContext.getCatalogItem((getComponent(childId)?.type ?: ""))?.isImplicitlyFlexible
                                    ?: false
                            val weight = explicitWeight ?: (if (isImplicitlyFlexible) 1 else null);
                            val fit = explicitWeight != null
                            BuildWeightedChild(
                                componentId = childId,
                                dataContext = dataContext,
                                buildChild = buildChild,
                                weight = weight,
                                fit = fit
                            )
                        }
                    }
                },
                templateListWidgetBuilder = { data, componentId, dataBinding ->
                    val values: kotlin.collections.List<Any?>
                    val keys: kotlin.collections.List<String>
                    if (data is kotlin.collections.List<*>) {
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
                    val isImplicitlyFlexible =
                        itemContext.getCatalogItem(component?.type ?: "")?.isImplicitlyFlexible == true
                    val weight = explicitWeight ?: (if (isImplicitlyFlexible) 1 else null)
                    val fit = explicitWeight != null
                    FlowRow(
                        verticalArrangement = _parseMainAxisAlignment(rowData.justify),
                        horizontalArrangement = _parseCrossAxisAlignment(rowData.align),
                    ) {
                        for ((index, key) in values.withIndex()) {
                            BuildWeightedChild(
                                componentId = componentId,
                                dataContext = itemContext.dataContext.nested(
                                    DataPath("$dataBinding/${keys[index]}"),
                                ),
                                buildChild = itemContext.buildChild,
                                weight = weight,
                                fit = fit,
                                key = keys[index],
                            )
                        }
                    }
                },
            )
        })
}
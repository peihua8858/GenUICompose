package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.primitives.JsonMap
import com.peihua.json.schema.S

data class DividerData(private val _json: JsonMap) {
    val axis: String?
        get() {
            val value = _json["axis"]
            if (value is String) return value;
            return null;
        }

    companion object {
        fun fromMap(json: JsonMap): DividerData {
            return DividerData(json);
        }

        fun create(axis: String?): DividerData {
            return DividerData(mapOf("axis" to axis));
        }
    }
}

object Divider {
    val _schema = S.obj(
        description = "A thin horizontal or vertical line used to separate content.",
        properties = mapOf(
            "axis" to S.string(enumValues = listOf("horizontal", "vertical")),
        ),
    );

    /**
     * A thin horizontal or vertical line used to separate content.
     *
     * This widget displays a thin line to separate content, either horizontally
     * or vertically.
     *
     * ## Parameters:
     *
     * - `axis`: The direction of the divider. Can be `horizontal` or `vertical`.
     *   Defaults to `horizontal`.
     */
    val divider = CatalogItem(
        name = "Divider",
        schema = _schema,
        widgetBuilder = { itemContext ->
            val dividerData = DividerData.fromMap(itemContext.data as JsonMap);
            if (dividerData.axis == "vertical") {
                VerticalDivider()
            } else {
                HorizontalDivider()
            }
        },
        exampleData = listOf(
            {
                """
                {
                   "id": "root",
                   "component": "Divider"
                   "axis": "horizontal"
                }
                """
            }
        ),
    )
}
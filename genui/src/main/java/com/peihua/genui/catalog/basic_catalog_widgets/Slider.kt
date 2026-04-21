package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.DataPath
import com.peihua.genui.primitives.JsonMap
import com.peihua.json.utils.toFloat
import com.peihua.json.utils.toStringAsFixed
import com.peihua.genui.widgets.BoundNumber
import com.peihua.genui.widgets.BoundString
import com.peihua.json.schema.S
import kotlin.collections.List

data class SliderData(private val _json: JsonMap) {
    val value: Any? = _json["value"]
    val min: Double = (_json["min"] as Number?)?.toDouble() ?: 0.0
    val max: Double = (_json["max"] as Number?)?.toDouble() ?: 1.0
    val label: String? = when (val label = _json["label"]) {
        is String -> label
        is Map<*, *> -> label["value"] as String?
        else -> null
    }
    val checks: List<JsonMap>?
        get() {
            val checks = _json["checks"] as List<*>?
            return checks?.mapNotNull { it as? JsonMap }
        }

    companion object {
        fun fromMap(json: JsonMap): SliderData {
            return SliderData(json)
        }

        fun create(value: JsonMap, min: Double, max: Double, label: String?): SliderData {
            return SliderData(
                mapOf(
                    "value" to value,
                    "min" to min,
                    "max" to max,
                    "label" to label,
                )
            )
        }
    }
}

object Slider {
    val _schema = S.obj(
        description = "A slider for selecting a value from a range.",
        properties = mapOf(
            "value" to A2uiSchemas.numberReference(),
            "min" to S.number(description = "The minimum value. Defaults to 0.0."),
            "max" to S.number(description = "The maximum value. Defaults to 1.0."),
            "label" to A2uiSchemas.stringReference(
                description = "The label for the slider.",
            ),
            "checks" to A2uiSchemas.checkable(),
        ),
        required = listOf("value"),
    )

    /**
     * A Material Design slider.
     *
     * This widget allows the user to select a value from a range by sliding a
     * thumb along a track. The `value` is bidirectionally bound to the data model.
     * This is analogous to Flutter's [Slider] widget.
     *
     * ## Parameters:
     *
     * - `value`: The current value of the slider.
     * - `min`: The minimum value of the slider. Defaults to 0.0.
     * - `max`: The maximum value of the slider. Defaults to 1.0.
     * - `label`: The label for the slider.
     */
    @OptIn(ExperimentalLayoutApi::class)
    val slider = CatalogItem(
        name = "Slider",
        schema = _schema,
        widgetBuilder = { itemContext ->
            val sliderData = SliderData.fromMap(itemContext.data as JsonMap);
            val valueRef = sliderData.value;
            val path = if (valueRef is Map<*, *> && valueRef.containsKey("path")) valueRef["path"] as String
            else "${itemContext.id}.value"
            BoundNumber(
                dataContext = itemContext.dataContext,
                value = mapOf("path" to path),
                builder = { value ->
                    // If value is null (nothing in DataContext yet), fall back to
                    // literal value if provided.
                    var effectiveValue = value
                    if (effectiveValue == null) {
                        if (valueRef is Number) {
                            effectiveValue = valueRef;
                        }
                    }
                    BoundString(
                        dataContext = itemContext.dataContext,
                        value = sliderData.label,
                        builder = { label ->
                            val validationFlow = remember {
                                itemContext.dataContext.evaluateConditionStream(checksToExpression(sliderData.checks))
                            }
                            val isValid = validationFlow.collectAsState(initial = true)
                            val isError = !isValid.value
                            FlowColumn(
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(text = label ?: "", style = MaterialTheme.typography.titleSmall)
                                Row(
                                    modifier = Modifier.padding(end = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Slider(
                                        value = (effectiveValue ?: sliderData.min).toFloat(),
                                        onValueChange = {
                                            itemContext.dataContext.update(DataPath(path), it);
                                        },
                                        valueRange = sliderData.min.toFloat()..sliderData.max.toFloat(),
                                        enabled = isValid.value
                                    )
                                    Text(
                                        value.toFloat()?.toStringAsFixed(0)
                                            ?: sliderData.min.toStringAsFixed(0),
                                    )
                                }
                                if (isError) {
                                    Box(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
                                        Text(
                                            text = "Invalid value",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            )
        },
        exampleData = listOf(
            {
                """
                 [
                    {
                        "id": "root",
                        "component": "Slider",
                        "min": 0,
                        "max": 10,
                        "value": {
                            "path": "/myValue"
                        }
                    }
                ]
                """
            }
        ),
    )
}
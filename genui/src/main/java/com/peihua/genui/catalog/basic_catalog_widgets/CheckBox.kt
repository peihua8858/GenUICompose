package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.peihua.genui.widgets.BoundBool
import com.peihua.genui.widgets.BoundString
import com.peihua.json.schema.S
import kotlin.collections.List
fun CheckBoxData(label: JsonMap, value: JsonMap, checks: List<JsonMap>? = null): JsonMap {
    return mapOf(
        "label" to label,
        "value" to value,
        "checks" to checks,
    )
}

data class _CheckBoxData(private val _json: JsonMap) {
    val label: Any? = _json["label"]
    val value: Any? = _json["value"]
    val checks: List<JsonMap>? = _json["checks"] as List<JsonMap>?
}


object CheckBox {
    val _schema = S.obj(
        description = "A selectable checkbox used for boolean toggles with a label.",
        properties = mapOf(
            "label" to A2uiSchemas.stringReference(),
            "value" to A2uiSchemas.booleanReference(),
            "checks" to A2uiSchemas.checkable(),
        ),
        required = listOf("label", "value"),
    )

    /**
     * A Material Design checkbox with a label.
     *
     * This widget displays a checkbox with a [Text] label. The checkbox's state
     * is bidirectionally bound to the data model path specified in the `value`
     * parameter.
     *
     * ## Parameters:
     *
     * - `label`: The text to display next to the checkbox.
     * - `value`: The boolean value of the checkbox.
     */
    val checkBox = CatalogItem(
        name = "CheckBox",
        schema = _schema,
        widgetBuilder = { itemContext ->
            val checkBoxData = _CheckBoxData(itemContext.data as JsonMap)
            val valueRef = checkBoxData.value;
            val path = if (valueRef is Map<*, *> && valueRef.containsKey("path")) valueRef["path"] as String
            else "${itemContext.id}.value";
            BoundString(
                dataContext = itemContext.dataContext,
                value = checkBoxData.label,
                builder = { label ->
                    val validationFlow = remember {
                        itemContext.dataContext.evaluateConditionStream(checksToExpression(checkBoxData.checks))
                    }
                    val isValid = validationFlow.collectAsState(initial = true)
                    val isError = !isValid.value
                    BoundBool(
                        dataContext = itemContext.dataContext,
                        value = mapOf("path" to path),
                    ) { value ->
                        CheckBoxListTile(
                            label = label ?: "",
                            checked = value ?: false,
                            isError = isError,
                            errorText = if (isError) "Invalid value" else null,
                        ) { newValue ->
                            itemContext.dataContext.update(
                                DataPath(path),
                                newValue,
                            );
                        }
                    }

                }
            )

        }
    )
}

@Composable
fun CheckBoxListTile(
    label: String,
    checked: Boolean,
    isError: Boolean,
    errorText: String?,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = if (isError) {
                    CheckboxDefaults.colors(
                        checkedColor = colorScheme.error,
                        uncheckedColor = colorScheme.error
                    )
                } else {
                    CheckboxDefaults.colors()
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                color = if (isError) colorScheme.error else colorScheme.onSurface
            )
        }

        if (errorText != null) {
            Text(
                text = errorText,
                color = colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

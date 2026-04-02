package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.CatalogItemContext
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.widgets.BoundList
import com.peihua.json.schema.S

data class ChoicePickerData(private val _json: JsonMap) {
    val label: Any?
        get() = _json["label"]
    val variant: String?
        get() = _json["variant"] as String?
    val options: Any?
        get() = _json["options"]
    val value: Any
        get() = _json["value"] as Any
    val displayStyle: String?
        get() = _json["displayStyle"] as String?
    val filterable: Boolean
        get() = _json["filterable"] as Boolean
    val checks: List<JsonMap>
        get() = (_json["checks"] as List<JsonMap>)
}

object ChoicePicker {
    val _schema = S.obj(
        description = "A component that allows selecting one or more options from a list.",
        properties = mapOf(
            "label" to A2uiSchemas.stringReference(
                description = "The label for the group of options.",
            ),
            "options" to A2uiSchemas.listOrReference(
                description = "The list of available options to choose from.",
                items = S.obj(
                    properties = mapOf(
                        "label" to A2uiSchemas.stringReference(
                            description = "The text to display for this option.",
                        ),
                        "value" to S.string(
                            description = "The stable value associated with this option.",
                        ),
                    ),
                    required = listOf("label", "value"),
                )
            ),
            "value" to S.combined(
                oneOf = listOf(
                    S.string(),
                    S.list(items = S.string()),
                    A2uiSchemas.dataBindingSchema(),
                    A2uiSchemas.functionCall(),
                ),
                description = "The list of currently selected values (or single value)."
            ),
            "displayStyle" to S.string(
                description = "The display style of the component.",
                enumValues = listOf("checkbox", "chips"),
            ),
            "variant" to S.string(
                description =
                    "A hint for how the choice picker should be displayed and behave.",
                enumValues = listOf("multipleSelection", "mutuallyExclusive"),
            ),
            "filterable" to S.boolean(
                description = "Whether the options can be filtered by the user.",
            ),
            "checks" to A2uiSchemas.checkable(),
        ),
        required = listOf("options", "value"),
    )

    /// A component that allows selecting one or more options from a list.
    val choicePicker = CatalogItem(
        name = "ChoicePicker",
        dataSchema = _schema,
        widgetBuilder = @Composable { itemContext ->
            val data = ChoicePickerData(itemContext.data as JsonMap)
            val valueRef = data.value;
            val path = if (valueRef is Map<*, *> && valueRef.containsKey("path"))
                valueRef["path"] as String
            else "${itemContext.id}.value";
            val isMutuallyExclusive = data.variant == "mutuallyExclusive";
            val isChips = data.displayStyle == "chips";
            val validationFlow = remember {
                itemContext.dataContext.evaluateConditionStream(checksToExpression(data.checks))
            }
            val isValid = validationFlow.collectAsState(initial = true)
            val isError = !isValid.value
            BoundList(
                dataContext = itemContext.dataContext,
                value = data.options,
                builder = { options ->
                    if (isError) {
                        Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
                            Text(
                                text = "Invalid selection",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        _ChoicePicker(
                            label = data.label,
                            options = options,
                            valueRef = valueRef,
                            path = path,
                            itemContext = itemContext,
                            isMutuallyExclusive = isMutuallyExclusive,
                            isChips = isChips,
                            filterable = data.filterable
                        )
                    }
                }
            )
        }
    )

}

@Composable
fun _ChoicePicker(
    label: Any?,
    options: List<Any?>?,
    valueRef: Any,
    path: String,
    itemContext: CatalogItemContext,
    isMutuallyExclusive: Boolean,
    isChips: Boolean,
    filterable: Boolean
) {

}
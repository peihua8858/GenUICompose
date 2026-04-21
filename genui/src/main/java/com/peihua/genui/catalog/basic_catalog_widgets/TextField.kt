package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.DataContext
import com.peihua.genui.model.DataPath
import com.peihua.genui.model.UserActionEvent
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.widgets.BoundString
import com.peihua.genui.widgets.resolveContext
import com.peihua.json.schema.S
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.collections.List
import kotlin.collections.listOf
import kotlin.collections.mapNotNull
import kotlin.collections.mapOf

data class TextFieldData(private val jsonMap: JsonMap) {
    val value: Any? = jsonMap["value"]
    val label: Any? = jsonMap["label"]
    val checks: List<JsonMap>? = (jsonMap["checks"] as? List<*>)?.mapNotNull { it as JsonMap }
    val variant: String? = jsonMap["variant"] as? String
    val validationRegexp: String? = jsonMap["validationRegexp"] as? String
    val onSubmittedAction: JsonMap? = jsonMap["onSubmittedAction"] as? JsonMap

    companion object {
        fun fromMap(jsonMap: JsonMap): TextFieldData {
            return TextFieldData(jsonMap)
        }

        fun create(
            value: Any?,
            label: Any?,
            checks: List<JsonMap>?,
            variant: String?,
            validationRegexp: String?,
            onSubmittedAction: JsonMap?
        ): TextFieldData {
            return TextFieldData(
                mapOf(
                    "value" to value,
                    "label" to label,
                    "checks" to checks,
                    "variant" to variant,
                    "validationRegexp" to validationRegexp,
                    "onSubmittedAction" to onSubmittedAction,
                )
            )
        }
    }
}

object TextField {
    val _schema = S.obj(
        description = "A text input field.",
        properties = mapOf(
            "value" to A2uiSchemas.stringReference(
                description = "The value of the text field.",
            ),
            "label" to A2uiSchemas.stringReference(),
            "variant" to S.string(
                enumValues = listOf("shortText", "longText", "number", "obscured"),
            ),
            "checks" to A2uiSchemas.checkable(),
            "validationRegexp" to S.string(),
            "onSubmittedAction" to A2uiSchemas.action(),
        ),
    );

    /**
     * A Material Design text field.
     *
     * This widget allows the user to enter and edit text. The `text` parameter
     * bidirectionally binds the field's content to the data model. This is
     * analogous to Flutter's [TextField] widget.
     *
     * ## Parameters:
     *
     * - `text`: The initial value of the text field.
     * - `label`: The text to display as the label for the text field.
     * - `textFieldType`: The type of text field. Can be `shortText`, `longText`,
     *   `number`, `date`, or `obscured`.
     * - `validationRegexp`: A regular expression to validate the input.
     * - `onSubmittedAction`: The action to perform when the user submits the
     *   text field.
     */
    val textField = CatalogItem(
        name = "TextField",
        isImplicitlyFlexible = true,
        schema = _schema,
        widgetBuilder = { itemContext ->
            val textFieldData = TextFieldData.fromMap(itemContext.data as JsonMap);
            val valueRef = textFieldData.value;
            val path = if (valueRef is Map<*, *> && valueRef.containsKey("path"))
                valueRef["path"] as String
            else "${itemContext.id}.value";
            BoundString(
                dataContext = itemContext.dataContext,
                value = mapOf("path" to path),
                builder = { currentValue ->
                    BoundString(
                        dataContext = itemContext.dataContext,
                        value = textFieldData.label,
                        builder = { label ->
                            val effectiveValue = currentValue ?: valueRef as? String
                            val scope = rememberCoroutineScope()
                            _TextField(
                                initialValue = effectiveValue ?: "",
                                label = label,
                                checks = textFieldData.checks,
                                context = itemContext.dataContext,
                                textFieldType = textFieldData.variant,
                                validationRegexp = textFieldData.validationRegexp,
                                onChanged = { newValue ->
                                    if (textFieldData.variant == "number") {
                                        val numberValue = newValue.toIntOrNull()
                                        if (numberValue != null) {
                                            itemContext.dataContext.update(DataPath(path), numberValue)
                                            return@_TextField
                                        }
                                    }
                                    itemContext.dataContext.update(DataPath(path), newValue);
                                },
                                onSubmitted = { newValue ->
                                    val actionData = textFieldData.onSubmittedAction ?: return@_TextField
                                    scope.launch {
                                        if (actionData.containsKey("event")) {

                                            val eventMap = actionData["event"] as JsonMap;
                                            val actionName = eventMap["name"] as String;
                                            val contextDefinition = eventMap["context"] as JsonMap?;
                                            val resolvedContext = resolveContext(
                                                itemContext.dataContext,
                                                contextDefinition,
                                            );
                                            itemContext.dispatchEvent(
                                                UserActionEvent(
                                                    name = actionName,
                                                    sourceComponentId = itemContext.id,
                                                    context = resolvedContext,
                                                ),
                                            );
                                        } else if (actionData.containsKey("functionCall")) {
                                            val funcMap = actionData["functionCall"] as JsonMap;
                                            val callName = funcMap["call"] as String;
                                            if (callName == "closeModal") {
//                                                if (itemContext.buildContext.mounted) {
//                                                    Navigator.of(itemContext.buildContext).pop();
//                                                }
                                                return@launch
                                            }
                                            val resultStream = itemContext.dataContext
                                                .resolve(funcMap);
                                            resultStream.first();
                                        }
                                    }
                                }
                            )
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
                        "component": "TextField",
                        "value": "Hello World",
                        "label": "Greeting"
                    }
                ]
               """
            },
            {
                """
                [
                    {
                        "id": "root",
                        "component": "TextField",
                        "value": "password123",
                        "label": "Password",
                        "textFieldType": "obscured"
                    }
                ]
               """
            },
        ),
    )
}

@Composable
fun _TextField(
    initialValue: String,
    label: String?,
    checks: List<JsonMap>?,
    context: DataContext?,
    textFieldType: String?,
    validationRegexp: String?,
    onChanged: (String) -> Unit,
    onSubmitted: (String) -> Unit,
) {

}
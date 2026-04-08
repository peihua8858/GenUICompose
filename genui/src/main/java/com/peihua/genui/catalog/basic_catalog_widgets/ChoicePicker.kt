package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.CatalogItemContext
import com.peihua.genui.model.DataPath
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.widgets.BoundList
import com.peihua.genui.widgets.BoundObject
import com.peihua.genui.widgets.BoundString
import com.peihua.genui.widgets.CheckboxListTile
import com.peihua.genui.widgets.ListTileControlAffinity
import com.peihua.genui.widgets.RadioListTile
import com.peihua.json.schema.S
import kotlin.collections.List
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
                        ChoicePicker(
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
fun ChoicePicker(
    label: Any?,
    options: List<Any?>?,
    valueRef: Any,
    path: String,
    itemContext: CatalogItemContext,
    isMutuallyExclusive: Boolean,
    isChips: Boolean,
    filterable: Boolean,
) {
    val _filter = remember { mutableStateOf("") }
    if (label != null) {
        Column(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
            BoundString(itemContext.dataContext, label) { label ->
                if (label == null || label.isEmpty()) {
                    Box(modifier = Modifier)
                } else {
                    Text(text = label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    if (filterable) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            TextField(
                value = _filter.value,
                trailingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Filter options")
                },
                onValueChange = { _filter.value = it })
        }
    }
    BoundObject(
        itemContext.dataContext,
        value = { "path" to path }
    ) { currentSelections ->
        var effectiveSelections = currentSelections;
        if (effectiveSelections == null) {
            if (valueRef is List<*>) {
                effectiveSelections = valueRef;
            } else if (valueRef is String) {
                effectiveSelections = listOf(valueRef);
            }
        } else if (effectiveSelections !is List<*>) {
            effectiveSelections = listOf(effectiveSelections);
        }
        val currentStrings = (effectiveSelections as? List<*>)?.map { e -> e.toString() }?.toList() ?: listOf<String>()
        if (options.isNullOrEmpty()) {
            Box(modifier = Modifier)
        } else {
            val castOptions = options as List<JsonMap>
            LazyColumn {
                items(castOptions) { item ->
                    val optionValue = item["value"] as String;
                    BoundString(
                        itemContext.dataContext,
                        value = item["label"]
                    ) { label ->
                        if (filterable &&
                            _filter.value.isNotEmpty() &&
                            label != null &&
                            !label.lowercase().contains(_filter.value.lowercase())
                        ) {
                            Box(modifier = Modifier)
                        } else if (isChips) {
                            val selected = currentStrings.contains(optionValue);
                            Box(modifier = Modifier.padding(4.dp)) {
                                FilterChip(
                                    selected = selected,
                                    label = { Text(label ?: "") },
                                    onClick = {
                                        _updateSelection(
                                            itemContext,
                                            path,
                                            isMutuallyExclusive,
                                            selected,
                                            optionValue,
                                            currentStrings,
                                        );
                                    }
                                )
                            }
                        } else if (isMutuallyExclusive) {
                            val groupValue = if (currentStrings.isNotEmpty()) currentStrings.first() else null
                            RadioListTile<String>(
                                checked = currentStrings.contains(optionValue),
                                controlAffinity = ListTileControlAffinity.leading,
                                dense = true,
                                title = { Text(label ?: "") },
                                value = optionValue,
                                // ignore: deprecated_member_use
                                groupValue = groupValue,
                                // ignore: deprecated_member_use
                                onChanged = { newValue ->
                                    if (newValue != null) {
                                        itemContext.dataContext.update(
                                            DataPath(path),
                                            listOf(newValue),
                                        );
                                    }
                                },
                            );
                        } else {
                            CheckboxListTile(
                                checked = currentStrings.contains(optionValue),
                                controlAffinity = ListTileControlAffinity.leading,
                                onCheckedChange = { newValue ->
                                    _updateSelection(
                                        itemContext,
                                        path,
                                        isMutuallyExclusive,
                                        newValue == true,
                                        optionValue,
                                        currentStrings,
                                    );
                                }
                            ) {
                                Text(label ?: "")
                            }
                        }
                    }
                }
            }
        }
    }

}

fun _updateSelection(
    itemContext: CatalogItemContext,
    path: String,
    isMutuallyExclusive: Boolean = false,
    selected: Boolean,
    optionValue: String, currentStrings: List<String>,
) {
    if (isMutuallyExclusive) {
        if (selected) {
            itemContext.dataContext.update(
                DataPath(path), listOf(
                    optionValue,
                )
            )
        }
    } else {
        val newSelections = currentStrings.toMutableList();
        if (selected) {
            if (!newSelections.contains(optionValue)) {
                newSelections.add(optionValue);
            }
        } else {
            newSelections.remove(optionValue);
        }
        itemContext.dataContext.update(
            DataPath(path),
            newSelections,
        );
    }
}
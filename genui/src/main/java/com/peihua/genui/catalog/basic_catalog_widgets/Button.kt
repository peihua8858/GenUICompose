package com.peihua.genui.catalog.basic_catalog_widgets

import android.util.Log
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.CatalogItemContext
import com.peihua.genui.model.UserActionEvent
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.utils.ValidationHelper
import com.peihua.genui.widgets.resolveContext
import com.peihua.json.schema.S
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

fun ButtonData(child: String, action: JsonMap, variant: String? = null, checks: List<JsonMap>? = null): ButtonData {
    return ButtonData(mapOf("child" to child, "action" to action, "variant" to variant, "checks" to checks));
}

data class ButtonData(private val _json: JsonMap) {
    val child: String
        get() {
            val value = _json["child"]
            if (value is String) return value;
            throw IllegalArgumentException("Invalid child: $value");
        }
    val action: JsonMap
        get() {
            val value = _json["action"]
            if (value is Map<*, *>) return value as JsonMap;
            throw IllegalArgumentException("Invalid action: $value");
        }
    val variant: String?
        get() {
            val value = _json["variant"]
            if (value is String) return value;
            return null;
        }
    val checks: List<JsonMap>?
        get() {
            val value = _json["checks"]
            if (value is List<*>) {
                return value.map { it as JsonMap }
            }
            return null;
        }
}

object Button {
    val _schema = S.obj(
        description = "An interactive button that triggers an action when pressed.",
        properties = mapOf(
            "child" to A2uiSchemas.componentReference(
                description = "The ID of a child widget. This should always be set, e.g. to the ID  of a `Text` widget.",
            ),
            "action" to A2uiSchemas.action(),
            "variant" to S.string(
                description = "A hint for the button style.",
                enumValues = listOf("primary", "borderless"),
            ),
            "checks" to A2uiSchemas.checkable(),
        ),
        required = listOf("child", "action"),
    );

    /**
     * A Material Design elevated button.
     *
     * This widget displays an interactive button. When pressed, it dispatches
     * the specified `action` event. The button's appearance can be styled as
     * a primary action.
     *
     * ## Parameters:
     *
     * - `child`: The ID of a child widget to display inside the button.
     * - `action`: The action to perform when the button is pressed.
     * - `variant`: A hint for the button style ('primary' or 'borderless').
     */
    val button = CatalogItem(
        name = "Button",
        dataSchema = _schema,
        widgetBuilder = { itemContext ->
            val scope = rememberCoroutineScope()
            val buttonData = Json.decodeFromString<ButtonData>(itemContext.data.toString())
            val colorScheme = MaterialTheme.colorScheme
            val variant = buttonData.variant ?: ""
            val primary = variant == "primary"
            val borderless = variant == "borderless"
            val textStyle = MaterialTheme.typography.bodyLarge
                .copy(
                    color = when (primary) {
                        true -> colorScheme.onPrimary
                        else -> colorScheme.onSurface
                    }
                )
            val buttonColors = when (variant) {
                "primary" -> ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )

                "borderless" -> ButtonDefaults.buttonColors(
                    contentColor = colorScheme.onSurface
                )

                else -> ButtonDefaults.buttonColors(
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.onSurface
                )
            }
            val validationFlow = remember {
                ValidationHelper.validateFlow(buttonData.checks, itemContext.dataContext)
            }
            val validationMessage = validationFlow.collectAsState(initial = null)
            val enabled = validationMessage.value == null
            if (borderless) {
                TextButton(
                    onClick = {
                        scope.launch {
                            _handlePress(itemContext, buttonData)
                        }
                    },
                    enabled = enabled,
                    colors = buttonColors,
                ) {
                    ProvideTextStyle(textStyle) {
                        itemContext.buildChild(Modifier,buttonData.child, null)
                    }
                }
            } else {
                ElevatedButton(
                    onClick = {
                        scope.launch {
                            _handlePress(itemContext, buttonData)
                        }
                    },
                    enabled = enabled,
                    colors = buttonColors,
                ) {
                    ProvideTextStyle(textStyle) {
                        itemContext.buildChild(Modifier,buttonData.child, null)
                    }
                }
            }
        },
        exampleData = listOf(
            {
                """
                [
                  {
                     "id": "root",
                     "component": "Button",
                     "child": "text",
                     "action": {
                          "event": {
                                "name": "button_pressed"
                                }
                          }
                    }
                  },
                  {
                    "id": "text",
                     "component": "Text",
                     "text": "Hello World"
                  }
                ]
                """
            },
            {
                """
                 [
                    {
                        "id": "root",
                         "component": "Column",
                         "children": ["primaryButton", "secondaryButton"]
                    },
                    {
                        "id": "primaryButton",
                        "component": "Button",
                         "child": "primaryText",
                         "primary": true,
                         "action": {
                                "event": {
                                    "name": "primary_pressed"
                                }
                         }   
                    },
                    {
                         "id": "secondaryButton",
                         "component": "Button",
                         "child": "secondaryText",
                         "action": {
                                "event": {
                                    "name": "secondary_pressed"
                                }
                         }
                    },
                    {
                        "id": "primaryText",
                        "component": "Text",
                        "text": "Primary Button"
                    },
                    {
                         "id": "secondaryText",
                         "component": "Text",
                         "text": "Secondary Button"
                    }
                ]
                """
            }
        )
    );

    suspend fun _handlePress(
        itemContext: CatalogItemContext,
        buttonData: ButtonData,
    ) {
        val actionData = buttonData.action;
        if (actionData.containsKey("event")) {
            val eventMap = actionData["event"] as JsonMap;
            val actionName = eventMap["name"] as String;
            val contextDefinition = eventMap["context"] as JsonMap?

            val resolvedContext = resolveContext(itemContext.dataContext, contextDefinition);
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
                //此处关闭页面或者弹窗
//                if (itemContext.buildContext.mounted) {
//                    Navigator.of(itemContext.buildContext).pop();
//                }
                return;
            }

            val resultStream = itemContext.dataContext.resolve(funcMap);
            try {
                resultStream.first();
            } catch (e: Exception) {
                itemContext.reportError(e, e.stackTrace);
            }
        } else {
            Log.w("Button", "Button action missing event or functionCall: $actionData");
        }
    }
}

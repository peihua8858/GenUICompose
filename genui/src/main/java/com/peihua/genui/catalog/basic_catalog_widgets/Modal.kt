package com.peihua.genui.catalog.basic_catalog_widgets

import android.content.Context
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.primitives.JsonMap
import com.peihua.json.schema.S

data class ModalData(private val _json: JsonMap) {
    val trigger: String
        get() {
            val value = _json["trigger"]
            if (value is String) {
                return value
            }
            if (value == null) {
                return ""
            }
            throw IllegalArgumentException("Invalid trigger: $value")
        }
    val content: String
        get() {
            val value = _json["content"]
            if (value is String) {
                return value
            }
            if (value == null) {
                return ""
            }
            throw IllegalArgumentException("Invalid content: $value")
        }

    companion object {
        fun fromMap(json: JsonMap): ModalData {
            return ModalData(json)
        }

        fun create(trigger: String, content: String): ModalData {
            return ModalData(
                mapOf(
                    "trigger" to trigger,
                    "content" to content
                )
            )
        }
    }
}

object Modal {
    val _schema = S.obj(
        description = "A modal overlay that slides up from the bottom of the screen.",
        properties = mapOf(
            "trigger" to A2uiSchemas.componentReference(
                description = "The widget that opens the modal.",
            ),
            "content" to A2uiSchemas.componentReference(
                description = "The widget to display in the modal.",
            )
        ),
        required = listOf("trigger", "content"),
    )

    /**
     * A modal overlay that slides up from the bottom of the screen.
     *
     * This component doesn't render the modal content directly. Instead, it
     * renders the `trigger` widget. The `trigger` is expected to
     * trigger an action (e.g., on button press) that causes the `content` to
     * be displayed within a modal bottom sheet by the [Surface].
     *
     * ## Parameters:
     *
     * - `trigger`: The ID of the widget that opens the modal.
     * - `content`: The ID of the widget to display in the modal.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    val modal = CatalogItem(
        name = "Modal",
        dataSchema = _schema,
        widgetBuilder = { itemContext ->
            val context = LocalContext.current
            val showModalDialog= remember { mutableStateOf( false) }
            val modalData = ModalData.fromMap(itemContext.data as JsonMap)
            ElevatedButton(
                modifier = Modifier,
                onClick = {
                    showModalDialog.value = true
                }
            ) {
                itemContext.buildChild(Modifier, modalData.trigger, null)
            }
            if(showModalDialog.value){
                ModalBottomSheet(
                    onDismissRequest = { showModalDialog.value = false },
                ) {
                    itemContext.buildChild(Modifier, modalData.content, null)
                }
            }
        },
        exampleData = listOf(
            {
                """
                 [
                    {
                        "id": "root",
                        "component": "Modal",
                        "trigger": "trigger_text",
                        "content": "modal_content"
                    },
                    {
                        "id": "trigger_text",
                        "component": "Text",
                        "text": "Open Modal"
                    },
                    {
                        "id": "modal_content",
                        "component": "Text",
                        "text": "This is a modal."
                    }
                ]   
                """
            }
        )
    )
}
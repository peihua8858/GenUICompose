package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.widgets.BoundString
import com.peihua.json.schema.S

object AudioPlayer {
    val _schema = S.obj(
        description = "An audio player component that plays audio from a given URL.",
        properties = mapOf(
            "url" to A2uiSchemas.stringReference(
                description = "The URL of the audio to play.",
            ),
            "description" to A2uiSchemas.stringReference(
                description = "A description of the audio, such as a title or summary.",
            ),
        ),
        required = listOf("url"),
    );

    /** A catalog item for an audio player.
     *
     * This widget displays a placeholder for an audio player, used to represent
     * a component capable of playing audio from a given URL.
     *
     * ## Parameters:
     *
     * - `url`: The URL of the audio to play.
     **/
    val audioPlayer = CatalogItem(
        name = "AudioPlayer",
        schema = _schema,
        widgetBuilder = @Composable { itemContext ->
            val json = itemContext.data as JsonMap
            val description = json["description"].toString()
            BoundString(
                dataContext = itemContext.dataContext,
                value = description
            ) { value ->
                Box(modifier = Modifier.semantics { contentDescription = value ?: "AudioPlayer" }) {
                    Icon(painter = rememberVectorPainter(Icons.Default.Audiotrack), contentDescription = value)
                }
            }
        },
        exampleData = listOf(
            {
                """
            [
              {
                "id": "root",
                "component": "AudioPlayer",
                "url": "https://example.com/audio.mp3"
              }
            ]
            """.trimIndent()
            }
        )
    )
}

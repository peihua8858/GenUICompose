package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.json.schema.S

object Video {

    val _schema = S.obj(
        description = "A video player.",
        properties = mapOf(
            "url" to A2uiSchemas.stringReference(
                description = "The URL of the video to play.",
            ),
        ),
        required = listOf("url"),
    );

    /**
     * A video player.
     *
     * This widget currently displays a placeholder for a video player. It is
     * intended to play video content from the given `url`.
     *
     * ## Parameters:
     *
     * - `url`: The URL of the video to play.
     */
    val video = CatalogItem(
        name = "Video",
        schema = _schema,
        widgetBuilder = { itemContext ->
            BoxWithConstraints(modifier = Modifier.sizeIn(maxWidth = 200.dp, maxHeight = 100.dp)) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(this.maxWidth, maxHeight)
                        .padding(16.dp)
                )
            }
        },
        exampleData = listOf(
            {
                """
            {
                "id": "root",
                "component": "Video",
                "url": "https://example.com/video.mp4"
            }
            """
            }
        ),
    );
}

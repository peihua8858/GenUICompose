package com.peihua.genui.catalog.basic_catalog_widgets

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.DataContext
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.widgets.BoundString
import com.peihua.json.schema.S

data class ImageData(private val jsonMap: JsonMap) {
    val url: Any = jsonMap["url"] as Any
    val fit: String? = null
    val variant: String? = null

    companion object {
        fun fromMap(json: JsonMap): ImageData {
            return ImageData(json)
        }

        fun create(url: String, fit: String? = null, variant: String? = null): ImageData {
            return ImageData(mapOf("url" to url, "fit" to fit, "variant" to variant))
        }
    }
}

object Image {
    val _schema = S.obj(
        description = "A UI element for displaying image data from a URL or asset path.",
        properties = mapOf(
            "url" to A2uiSchemas.stringReference(
                description = "Asset path (e.g. assets/...) or network URL (e.g. https://...)",
            ),
            "fit" to S.string(
                description = "How the image should be inscribed into the box.",
                enumValues = listOf(
                    "Fit",
                    "Crop",
                    "FillWidth",
                    "FillHeight",
                    "Inside",
                    "FillBounds",
                    "None",
                ),
            ),
            "variant" to S.string(
                description = """'"A hint for the image size and style. One of:
        - icon: Small square icon.
        - avatar: Circular avatar image.
        - smallFeature: Small feature image.
        - mediumFeature: Medium feature image.
        - largeFeature: Large feature image.
        - header: Full-width, full bleed, header image.""",
                enumValues = listOf(
                    "icon",
                    "avatar",
                    "smallFeature",
                    "mediumFeature",
                    "largeFeature",
                    "header",
                ),
            )
        ),
    )

    val image = CatalogItem(
        name = "Image",
        dataSchema = _schema,
        exampleData = listOf(
            {
                """
                [
                    {
                        "id": "root",
                        "component": "Image",
                        "url": "https://storage.googleapis.com/cms-storage-bucket/lockup_flutter_horizontal.c823e53b3a1a7b0d36a9.png",
                        "variant": "mediumFeature"
                    }
                ]
            """.trimIndent()
            }
        ),
        widgetBuilder = { itemContext ->
            val imageData = ImageData.fromMap(itemContext.data as JsonMap);
            CatalogImage(
                imageData = imageData,
                dataContext = itemContext.dataContext
            )
        }
    )
}

@Composable
private fun CatalogImage(
    imageData: ImageData,
    dataContext: DataContext,
    modifier: Modifier = Modifier,
) {
    BoundString(
        dataContext = dataContext,
        value = imageData.url
    ) { value ->
        if (value.isNullOrEmpty()) {
            Log.d("", "Image widget created with no URL at path: ${dataContext.path}")
            return@BoundString
        }

        val sizeModifier = when (imageData.variant) {
            "header" -> Modifier.fillMaxWidth()
            "icon", "avatar" -> Modifier.size(32.dp)
            "smallFeature" -> Modifier.size(50.dp)
            "mediumFeature" -> Modifier.size(150.dp)
            "largeFeature" -> Modifier.size(400.dp)
            else -> Modifier.size(150.dp)
        }.then(modifier)

        val shapedModifier = if (imageData.variant == "avatar") {
            sizeModifier.clip(CircleShape)
        } else {
            sizeModifier
        }

        if (value.startsWith("http")) {
            NetworkImage(
                url = value,
                contentScale = imageData.fit.toContentScale(),
                modifier = shapedModifier
            )
        } else {
            LocalImage(
                path = value,
                contentScale = imageData.fit.toContentScale(),
                modifier = shapedModifier
            )
        }
    }

}

fun String?.toContentScale(): ContentScale {
    return when (this) {
        "Fit" -> ContentScale.Fit
        "Crop" -> ContentScale.Crop
        "FillBounds" -> ContentScale.FillBounds
        "FillWidth" -> ContentScale.FillWidth
        "FillHeight" -> ContentScale.FillHeight
        "none" -> ContentScale.None
        "Inside" -> ContentScale.Inside
        else -> ContentScale.Fit
    }
}

@Composable
private fun NetworkImage(
    url: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        error = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "broken image"
                )
            }
        },
        success = { state ->
            Crossfade(targetState = state.painter, label = "image_fade") { painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}

@Composable
private fun LocalImage(
    path: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resourceName = remember(path) {
        path.substringAfterLast("/").substringBeforeLast(".")
    }

    @DrawableRes
    val resId = remember(resourceName) {
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "broken image"
            )
        }
    }
}

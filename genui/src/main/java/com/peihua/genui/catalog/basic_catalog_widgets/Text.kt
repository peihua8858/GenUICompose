package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.widgets.BoundString
import com.peihua.json.schema.S
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes

data class TextData(val jsonMap: JsonMap) {
    val text: Any? = jsonMap["text"]
    val variant: String? = jsonMap["variant"] as? String

    companion object {
        fun fromMap(jsonMap: JsonMap): TextData {
            return TextData(jsonMap)
        }

        fun create(text: Any, variant: String?): TextData {
            return TextData(jsonMap = mapOf("text" to text, "variant" to variant))
        }
    }
}

object Text {
    val _schema = S.obj(
        description = "A block of styled text.",
        properties = mapOf(
            "text" to S.string(
                description = """While simple Markdown is supported (without HTML or image references), utilizing dedicated UI components is generally preferred for a richer and more structured presentation.""",
            ),
            "variant" to S.string(
                description = "A hint for the base text style.",
                enumValues = listOf("h1", "h2", "h3", "h4", "h5", "h6", "caption", "body"),
            ),
        ),
        required = listOf("text"),
    )
    val text = CatalogItem(
        name = "Text",
        dataSchema = _schema,
        widgetBuilder = @Composable { itemContext ->
            val textData = TextData.fromMap(itemContext.data as JsonMap)
            BoundString(
                dataContext = itemContext.dataContext,
                value = textData.text,
                builder = { value ->
                    val variant = textData.variant
                    val baseStyle = when (variant) {
                        "h1" -> MaterialTheme.typography.headlineLarge
                        "h2" -> MaterialTheme.typography.headlineMedium
                        "h3" -> MaterialTheme.typography.headlineSmall
                        "h4" -> MaterialTheme.typography.titleLarge
                        "h5" -> MaterialTheme.typography.titleMedium
                        "caption" -> MaterialTheme.typography.bodySmall
                        "body" -> MaterialTheme.typography.bodyLarge
                        else -> MaterialTheme.typography.bodyLarge
                    }
                    val verticalPadding = when (variant) {
                        "h1" -> 20.0
                        "h2" -> 16.0
                        "h3" -> 12.0
                        "h4" -> 8.0
                        "h5" -> 4.0
                        else -> 0.0
                    };
                    Box(modifier = Modifier.padding(vertical = verticalPadding.dp)) {
                        val isDarkTheme = isSystemInDarkTheme()
                        val highlightsBuilder = remember(isDarkTheme) {
                            Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
                        }
                        Markdown(
                            content = value ?: "",
                            colors = markdownColor(text = MaterialTheme.colorScheme.primary),
                            typography = markdownTypography(text = baseStyle),
                            imageTransformer = Coil3ImageTransformerImpl,
                            components = markdownComponents(
                                codeBlock = {
                                    MarkdownHighlightedCodeBlock(
                                        content = it.content,
                                        node = it.node,
                                        highlightsBuilder = highlightsBuilder
                                    )
                                },
                                codeFence = {
                                    MarkdownHighlightedCodeFence(
                                        content = it.content,
                                        node = it.node,
                                        highlightsBuilder = highlightsBuilder
                                    )
                                },
                            )
                        )
                    }
                }
            )
        },
        exampleData=listOf(
            {
                """
                [
                    {
                        "id": "root",
                        "component": "Text",
                        "text": "This is a sample text.",
                        "variant":"h1"
                    }
                ]
                """
            }
        )
    )
}
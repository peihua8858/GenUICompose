package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.primitives.JsonMap
import com.peihua.json.schema.S

fun CardData(child: String): CardData {
    return CardData(mapOf("child" to child));
}

data class CardData(private val _json: JsonMap) {
    val child: String
        get() {
            val value = _json["child"]
            if (value is String) return value;
            throw IllegalArgumentException("Invalid child: $value");
        }
}

object Card {
    val _schema = S.obj(
        description = "A visual container (card) that groups a single child widget.",
        properties = mapOf("child" to A2uiSchemas.componentReference()),
        required = listOf("child"),
    )

    /**
     * A Material Design card.
     *
     * This widget displays a card, which is a container for a single `child`
     * widget. Cards often have rounded corners and a shadow, and are used to group
     * related content.
     *
     * ## Parameters:
     *
     * - `child`: The ID of a child widget to display inside the card.
     */
    val card = CatalogItem(
        name = "Card",
        dataSchema = _schema,
        widgetBuilder = { itemContext ->
            val cardData = CardData(itemContext.data as JsonMap)
            Card(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            ) {
                itemContext.buildChild(cardData.child, null)
            }
        },
        exampleData = listOf(
            {
                """
                 [
                      {
                        "id": "root",
                        "component": "Card",
                        "child": "text"
                    },
                    {
                        "id": "text",
                        "component": "Text",
                         "text": "This is a card."
                    }
                ] 
                """.trimIndent()
            }
        )
    )
}
package com.peihua.genui.catalog

import com.peihua.genui.catalog.basic_catalog_widgets.AudioPlayer
import com.peihua.genui.catalog.basic_catalog_widgets.Button
import com.peihua.genui.catalog.basic_catalog_widgets.Card
import com.peihua.genui.catalog.basic_catalog_widgets.CheckBox
import com.peihua.genui.catalog.basic_catalog_widgets.ChoicePicker
import com.peihua.genui.catalog.basic_catalog_widgets.Column
import com.peihua.genui.catalog.basic_catalog_widgets.DateTimeInput
import com.peihua.genui.catalog.basic_catalog_widgets.Divider
import com.peihua.genui.catalog.basic_catalog_widgets.Icon
import com.peihua.genui.catalog.basic_catalog_widgets.Image
import com.peihua.genui.catalog.basic_catalog_widgets.List
import com.peihua.genui.catalog.basic_catalog_widgets.Modal
import com.peihua.genui.catalog.basic_catalog_widgets.Row
import com.peihua.genui.catalog.basic_catalog_widgets.Slider
import com.peihua.genui.catalog.basic_catalog_widgets.Tabs
import com.peihua.genui.catalog.basic_catalog_widgets.Text
import com.peihua.genui.catalog.basic_catalog_widgets.TextField
import com.peihua.genui.catalog.basic_catalog_widgets.Video
import com.peihua.genui.model.Catalog
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.primitives.basicCatalogId

object BasicCatalogItems {

    /// A UI element for playing audio content.
    ///
    /// This typically includes controls like play/pause, seek, and volume.
    val audioPlayer: CatalogItem = AudioPlayer.audioPlayer;

    /// An interactive button that triggers an action when pressed.
    ///
    /// Conforms to Material Design guidelines for elevated buttons.
    val button: CatalogItem = Button.button;

    /// A Material Design card, a container for related information and
    /// actions.
    ///
    /// Often used to group content visually.
    val card: CatalogItem = Card.card;

    /// A checkbox that allows the user to toggle a boolean state.
    val checkBox: CatalogItem = CheckBox.checkBox;

    /// A layout widget that arranges its children in a vertical
    /// sequence.
    val column: CatalogItem = Column.column;

    /// A widget for selecting a date and/or time.
    val dateTimeInput: CatalogItem = DateTimeInput.dateTimeInput;

    /// A thin horizontal line used to separate content.
    val divider: CatalogItem = Divider.divider;

    /// An icon.
    val icon: CatalogItem = Icon.icon;

    /// A UI element for displaying image data from a URL or other
    /// source.
    val image: CatalogItem = Image.image;

    /// A scrollable list of child widgets.
    ///
    /// Can be configured to lay out items linearly.
    val list: CatalogItem = List.list;

    /// A modal overlay that slides up from the bottom of the screen.
    ///
    /// Used to present a set of options or a piece of content requiring user
    /// interaction.
    val modal: CatalogItem = Modal.modal;

    /// A widget allowing the user to select one or more options from a
    /// list.
    val choicePicker: CatalogItem = ChoicePicker.choicePicker;

    /// A layout widget that arranges its children in a horizontal
    /// sequence.
    val row: CatalogItem = Row.row;

    /// A slider control for selecting a value from a range.
    val slider: CatalogItem = Slider.slider;

    /// A set of tabs for navigating between different views or
    /// sections.
    val tabs: CatalogItem = Tabs.tabs;

    /// A block of styled text.
    val text: CatalogItem = Text.text;

    /// An input field where the user can enter `text.
    val textField: CatalogItem = TextField.textField;

    /// A UI element for playing video content.
    ///
    /// This typically includes controls like play/pause, seek, and volume.
    val video: CatalogItem = Video.video;

    val basicCatalogRules: String = _basicCatalogRules

    /**
     * Creates a catalog containing all core catalog items.
     */
    fun asCatalog(systemPromptFragments: kotlin.collections.List<String>): Catalog {
        return Catalog(
            listOf(
                audioPlayer,
                button,
                card,
                checkBox,
                column,
                dateTimeInput,
                divider,
                icon,
                image,
                list,
                modal,
                choicePicker,
                row,
                slider,
                tabs,
                text,
                textField,
                video,
            ),
            functions = BasicFunctions.all,
            catalogId = basicCatalogId,
            systemPromptFragments = listOf(basicCatalogRules, *systemPromptFragments.toTypedArray())
        )
    }
}

/// The text content of basic_catalog_rules.txt.
const val _basicCatalogRules = """
**REQUIRED PROPERTIES:** You MUST include ALL required properties for every component, even if they are inside a template or will be bound to data .
- For 'Text', you MUST provide 'text'. If dynamic, use { "path": "..." }.
- For 'Image', you MUST provide 'url'. If dynamic, use { "path": "..." }.
- For 'Button', you MUST provide 'action'.
- For 'TextField', 'CheckBox', etc., you MUST provide 'label'.

**EXAMPLES:**

1. Create a surface:
```json
{
    "version": "v0.9",
    "createSurface": {
    "surfaceId": "main",
    "catalogId": "https://a2ui.org/specification/v0_9/standard_catalog.json",
    "sendDataModel": true
}
}
```

2. Update components:
```json
{
    "version": "v0.9",
    "updateComponents": {
    "surfaceId": "main",
    "components": [
    {
        // The root component MUST have id "root"
        "id": "root",
        "component": "Column",
        "justify": "start",
        "children": [
        "headerText",
        "content"
        ]
    }
    ]
}
}
```

**IMPORTANT:**
- One of the components sent in one of the `updateComponents` MUST have id "root", or nothing will be displayed.
- Do NOT nest `components` inside `createSurface`. Use `updateComponents` to add components to a surface.
- `createSurface` ONLY sets up the surface (ID and catalog). It does NOT take content.
- To show a UI, you typically send a `createSurface` message (if the surface doesn't exist), followed by an `updateComponents` message.
"""
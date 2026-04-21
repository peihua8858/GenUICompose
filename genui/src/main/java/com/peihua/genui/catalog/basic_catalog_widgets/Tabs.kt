package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.CatalogItemContext
import com.peihua.genui.model.DataPath
import com.peihua.genui.primitives.JsonMap
import com.peihua.json.utils.toInteger
import com.peihua.genui.widgets.BoundNumber
import com.peihua.genui.widgets.BoundString
import com.peihua.json.schema.S
import kotlin.collections.List

data class TabsData(private val _json: JsonMap) {
    val tabs: List<JsonMap> = (_json["tabs"] as List<*>).mapNotNull { it as? JsonMap }
    val activeTab: Any? = _json["activeTab"]

    companion object {
        fun fromMap(json: JsonMap): TabsData {
            return TabsData(json)
        }

        fun create(tabs: List<JsonMap>, activeTab: Int): TabsData {
            return TabsData(mapOf("tabs" to tabs, "activeTab" to activeTab))
        }
    }
}

object Tabs {
    val _schema = S.obj(
        description = "A tab layout to navigate between different child components.",
        properties = mapOf(
            "tabs" to S.list(
                items = S.obj(
                    properties = mapOf(
                        "label" to A2uiSchemas.stringReference(
                            description = "The label for the tab.",
                        ),
                        "content" to A2uiSchemas.componentReference(
                            description = "The content (widget ID) to display when this tab is active.",
                        ),
                    ),
                    required = listOf("label", "content"),
                ),
            ),
            "activeTab" to A2uiSchemas.numberReference(
                description = "The index of the currently active tab.",
            ),
        ),
        required = listOf("tabs"),
    )

    /**
     * A Material Design tab layout.
     *
     * This widget displays a [TabBar] and a view area to allow navigation
     * between different child components. Each tab in `tabs` has a label and
     * a corresponding child component ID to display when selected.
     *
     * ## Parameters:
     *
     * - `tabs`: A list of tabs to display, each with a `label` and a `content`
     *   widget ID.
     * - `activeTab`: (Optional) Binding to the current tab index.
     */
    val tabs = CatalogItem(
        name = "Tabs",
        schema = _schema,
        widgetBuilder = { itemContext ->
            val tabsData = TabsData.fromMap(itemContext.data as JsonMap);
            val activeTabRef = tabsData.activeTab;
            val path = if (activeTabRef is Map<*, *> && activeTabRef.containsKey("path"))
                activeTabRef["path"] as String
            else "${itemContext.id}.activeTab"
            BoundNumber(
                dataContext = itemContext.dataContext,
                value = mapOf("path" to path)
            ) { value ->
                _TabsWidget(
                    tabs = tabsData.tabs,
                    itemContext = itemContext,
                    activeTab = value?.toInteger(),
                    initialTab = if (activeTabRef is Number) activeTabRef.toInt() else 0,
                    onTabChanged = { newIndex ->
                        itemContext.dataContext.update(DataPath(path), newIndex);
                    },
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun _TabsWidget(
    tabs: List<JsonMap>,
    itemContext: CatalogItemContext,
    activeTab: Int?,
    initialTab: Int,
    onTabChanged: (Int) -> Unit
) {
    val pagerState: PagerState = rememberPagerState() {
        tabs.size
    }
    FlowColumn {
        PrimaryTabRow(selectedTabIndex = activeTab ?: initialTab) {
            for ((index, tabItem) in tabs.withIndex()) {
                val labelRef = tabItem["label"] ?: tabItem["title"];
                BoundString(
                    dataContext = itemContext.dataContext,
                    value = labelRef,
                    builder = { value ->
                        Tab(
                            text = { Text(value ?: "") },
                            selected = index == activeTab,
                            onClick = {
                                onTabChanged(index)
                            }
                        )
                    }
                )

            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier) { page ->
            val tabItem = tabs[page]
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                val contentId = (tabItem["content"] ?: tabItem["child"]) as String;
                itemContext.buildChild(Modifier, contentId, null)
            }
        }
    }
}

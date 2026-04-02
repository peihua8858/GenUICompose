package com.peihua.genui.model

data class Catalog(
    val items: List<CatalogItem>,
    val functions: List<ClientFunction> = listOf(),
    val catalogId: String? = null,
    val systemPromptFragments: List<String> = listOf(),
) {
}
package com.peihua.genui.model

import com.peihua.genui.primitives.JsonMap

/** Defines how catalogs should be handled when generating client capabilities. */
enum class InlineCatalogHandling {
    /** Do not inline any catalogs. If a catalog is missing a `catalogId`, an
     * exception is thrown.
     */
    NONE,

    /** Inline only catalogs that do not have a `catalogId`. Send the rest as
     * supported IDs.
     */
    MISSING_IDS,

    /** Inline all provided catalogs, regardless of whether they have a
     * `catalogId`.
     */
    ALL,
}

class A2UiClientCapabilities(
    val supportedCatalogIds: List<String>,
    val inlineCatalogs: List<JsonMap>? = null
) {
    companion object {
        /**
         * Creates client capabilities from a collection of catalogs.
         *
         * This is used to create an [A2UiClientCapabilities] instance from a
         * collection of [Catalog] objects to send to an A2A server that supports
         * the [A2UI extension](https://a2ui.org).
         *
         * [inlineHandling] determines how catalogs without a `catalogId` are
         * handled. See [InlineCatalogHandling] for more information.
         */
        fun fromCatalogs(
            catalogs: Iterable<Catalog>,
            inlineHandling: InlineCatalogHandling = InlineCatalogHandling.MISSING_IDS,
        ): A2UiClientCapabilities {
            val supportedIds = mutableListOf<String>()
            val inlineDefinitions = mutableListOf<JsonMap>();

            for (catalog in catalogs) {
                if (inlineHandling == InlineCatalogHandling.ALL) {
                    inlineDefinitions.add(catalog.toCapabilitiesJson());
                    continue;
                }

                if (catalog.catalogId != null) {
                    supportedIds.add(catalog.catalogId);
                } else {
                    if (inlineHandling == InlineCatalogHandling.NONE) {
                        throw StateError(
                            "Catalog provided without a catalogId, but inlineHandling is set to InlineCatalogHandling.none",
                        );
                    }
                    inlineDefinitions.add(catalog.toCapabilitiesJson());
                }
            }

            return A2UiClientCapabilities(
                supportedCatalogIds = supportedIds,
                inlineCatalogs = inlineDefinitions.ifEmpty { null },
            );
        }
    }

    fun toJson(): JsonMap {
        val json: MutableMap<String, Any> = mutableMapOf("supportedCatalogIds" to supportedCatalogIds);
        if (inlineCatalogs != null) {
            json["inlineCatalogs"] = inlineCatalogs;
        }
        return mapOf("v0.9" to json);
    }
}
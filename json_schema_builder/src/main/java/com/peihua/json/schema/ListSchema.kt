package com.peihua.json.schema

import com.peihua.json.JsonType
import com.peihua.json.kContains
import com.peihua.json.kItems
import com.peihua.json.kMaxContains
import com.peihua.json.kMaxItems
import com.peihua.json.kMinContains
import com.peihua.json.kMinItems
import com.peihua.json.kPrefixItems
import com.peihua.json.kUnevaluatedItems
import com.peihua.json.kUniqueItems
import com.peihua.json.utils.toBoolean
import com.peihua.json.utils.toInteger
import com.peihua.json.utils.toJsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ListSchema(value: JsonObject) : Schema(value) {
    constructor(
        title: String?,
        description: String?,
        items: Schema? = null,
        prefixItems: List<Schema>? = null,
        unevaluatedItems: Any? = null,
        contains: Schema? = null,
        minContains: Int? = null,
        maxContains: Int? = null,
        minItems: Int? = null,
        maxItems: Int? = null,
        uniqueItems: Boolean? = null,
    ) : this(buildJsonObject {
        put("type", JsonType.LIST.typeName)
        put("title", title)
        put("description", description)
        put("items", items?.value ?: JsonNull)
        put("prefixItems", prefixItems.toSchemaJsonArray())
        put("unevaluatedItems", unevaluatedItems.toJsonObject())
        put("contains", contains?.value ?: JsonNull)
        put("minContains", minContains)
        put("maxContains", maxContains)
        put("minItems", minItems)
        put("maxItems", maxItems)
        put("uniqueItems", uniqueItems)

    })

    /// The schema that all items in the list must match.
    ///
    /// If [prefixItems] is also present, this schema will only apply to items
    /// after the ones matched by [prefixItems].
    val items: Schema?
        get() = schemaOrBool(kItems)

    /// A list of schemas that must match the items in the list at the same
    /// index.
    val prefixItems: List<Any?>?
        get() {
            val items: List<Any?> = value[kPrefixItems] as? List<*> ?: return null
            return items.map { item ->
                if (item is Boolean) {
                    return@map item
                }
                return@map fromMap(item as Map<String, Any>)
            }.toList()
        }

    /// A schema that will be applied to all items that are not matched by
    /// [items], [prefixItems], or [contains].
    val unevaluatedItems: Schema?
        get() = schemaOrBool(kUnevaluatedItems);

    /// The schema that at least one item in the list must match.
    val contains: Schema?
        get() = schemaOrBool(kContains);

    /// The minimum number of items that must match the [contains] schema.
    ///
    /// Defaults to 1.
    val minContains: Int
        get() = value[kMinContains].toInteger();

    /// The maximum number of items that can match the [contains] schema.
    val maxContains: Int
        get() = value[kMaxContains].toInteger();

    /// The minimum number of items that the list must have.
    val minItems: Int
        get() = value[kMinItems].toInteger()

    /// The maximum number of items that the list can have.
    val maxItems: Int
        get() = value[kMaxItems].toInteger();

    /// Whether all items in the list must be unique.
    val uniqueItems: Boolean
        get() = value[kUniqueItems].toBoolean();
}
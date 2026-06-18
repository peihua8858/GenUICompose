package com.peihua.json

import com.peihua.json.schema.Schema
import java.net.URI
import kotlin.String
import kotlin.collections.Map
import kotlin.collections.forEach

/**
 * A registry for managing and resolving JSON schemas.
 *
 * This class is responsible for storing schemas, resolving `$ref` and
 * `$dynamicRef` references, and handling schema identifiers (`$id`).
 */
class SchemaRegistry(
    schemaCache: SchemaCache? = null,
    loggingContext: LoggingContext? = null
) {
    private val schemas = mutableMapOf<URI, Schema>()
    private val schemaCache: SchemaCache = schemaCache ?: SchemaCache(loggingContext = loggingContext)

    /**
     * Adds a schema to the registry with a given [uri].
     *
     * The schema is stored in the registry and can be resolved later using its
     * URI. This method also registers any `$id`s found within the schema.
     */
    fun addSchema(uri: URI, schema: Schema) {
        val uriWithoutFragment = uri.withoutFragment()
        schemas[uriWithoutFragment] = schema
        registerIds(schema, uriWithoutFragment)
    }

    /**
     * Resolves a schema from the given [uri].
     *
     * If the schema is already in the registry, it is returned directly.
     * Otherwise, it is fetched using the [SchemaCache], stored in the registry,
     * and then returned.
     *
     * This method can also resolve fragments and JSON pointers within a schema.
     */
    suspend fun resolve(uri: URI): Schema? {
        val uriWithoutFragment = uri.withoutFragment()
        schemas[uriWithoutFragment]?.let {
            return getSchemaFromFragment(uri, it)
        }

        try {
            val schema = schemaCache.get(uriWithoutFragment) ?: return null;
            schemas[uriWithoutFragment] = schema;
            registerIds(schema, uriWithoutFragment);

            return getSchemaFromFragment(uri, schema);
        } catch (e: SchemaFetchException) {
            throw e;
        }
    }

    /**
     * Gets the URI for a given schema, if it has been registered.
     *
     * This method performs a deep comparison to find a matching schema in the
     * registry.
     */
    fun getUriForSchema(schema: Schema): URI? {
        for ((uri, registeredSchema) in schemas) {
            if (deepEquals(registeredSchema.value, schema.value)) {
                return uri
            }
        }
        return null
    }

    fun dispose() {
        schemaCache.close()
    }

    private fun registerIds(schema: Schema, baseUri: URI) {
        var currentBaseUri = baseUri
        val id = schema.id
        if (id != null) {
            // This is a heuristic to avoid re-resolving a relative path that has
            // already been applied to the base URI.
            if (id.endsWith("/") && currentBaseUri.path.endsWith("/$id")) {
                schemas[currentBaseUri.withoutFragment()] = schema;
            } else {
                val newUri = currentBaseUri.resolve(id);
                schemas[newUri.withoutFragment()] = schema;
                currentBaseUri = newUri;
            }
        }

        fun recurseOnMap(map: Map<String, Any?>) {
            registerIds(Schema.fromMap(map), currentBaseUri);
        }

        fun recurseOnList(list: List<Any?>) {
            for (item in list) {
                if (item is Map<*, *>) {
                    recurseOnMap(item as Map<String, Any?>);
                }
            }
        }

        // Keywords with map-of-schemas values
        val mapOfSchemasKeywords = listOf(
            "properties",
            "patternProperties",
            "dependentSchemas",
            "\$defs"
        )
        for (keyword in mapOfSchemasKeywords) {
            val value = schema.value[keyword]
            if (value is Map<*, *>) {
                value.values.forEach { value ->
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        recurseOnMap(value as Map<String, Any?>)
                    }
                }
            }

            // Keywords with schema values
            val schemaKeywords = listOf(
                "additionalProperties",
                "unevaluatedProperties",
                "items",
                "unevaluatedItems",
                "contains",
                "propertyNames",
                "not",
                "if",
                "then",
                "else"
            )
            for (keyword in schemaKeywords) {
                (schema.value[keyword] as? Map<String, Any?>)?.let {
                    recurseOnMap(it)
                }
            }

            // Keywords with list-of-schemas values
            val listOfSchemasKeywords = listOf("allOf", "anyOf", "oneOf", "prefixItems")
            for (keyword in listOfSchemasKeywords) {
                (schema.value[keyword] as? List<Any?>)?.let {
                    recurseOnList(it)
                }
            }
        }
    }

    private fun getSchemaFromFragment(uri: URI, schema: Schema): Schema? {
        val fragment = uri.fragment
        if (fragment.isNullOrEmpty()) {
            return schema
        }

        return if (fragment.startsWith("/")) {
            resolveJsonPointer(schema, fragment)
        } else {
            findAnchor(fragment, schema)
        }
    }

    private fun resolveJsonPointer(schema: Schema, pointer: String): Schema? {
        val parts = pointer.substring(1).split('/')
        var current: Any? = schema
        for (part in parts) {
            val decodedPart = URI(part).toString()
                .replace("~1", "/")
                .replace("~0", "~")
            current = when (current) {
                is Schema -> {
                    if (!current.value.containsKey(decodedPart)) return null
                    current.value[decodedPart]
                }

                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val map = current as Map<String, Any?>
                    if (!map.containsKey(decodedPart)) return null
                    map[decodedPart]
                }

                is List<*> -> {
                    val index = decodedPart.toIntOrNull() ?: return null
                    if (index !in current.indices) return null
                    current[index]
                }

                else -> return null
            }
        }
        return when (current) {
            is Schema -> current
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                Schema.fromMap(current as Map<String, Any?>)
            }

            is Boolean -> Schema.fromBoolean(current)
            else -> null
        }
    }

    private fun findAnchor(anchorName: String, schema: Schema): Schema? {
        var result: Schema? = null
        val visited = mutableSetOf<Map<String, Any?>>()

        fun visit(current: Any?, isRootOfResource: Boolean) {
            if (result != null) return
            when (current) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val map = current as Map<String, Any?>
                    if (map in visited) return
                    visited.add(map)

                    val currentSchema = Schema.fromMap(map)

                    if (!isRootOfResource && currentSchema.id != null) {
                        // This is a new schema resource, so we don't look for anchors for
                        // the parent resource inside it.
                        return
                    }

                    if (currentSchema.anchor == anchorName || currentSchema.dynamicAnchor == anchorName) {
                        result = currentSchema
                        return
                    }

                    for (value in map.values) {
                        visit(value, isRootOfResource = false)
                    }
                }

                is List<*> -> {
                    for (item in current) {
                        visit(item, isRootOfResource = false)
                    }
                }
            }
        }

        visit(schema.value, isRootOfResource = true)
        return result
    }

    private fun URI.withoutFragment(): URI =
        URI(scheme, userInfo, host, port, path, query, null)
}
// These declarations are assumed to exist in the project.
// They are provided here for completeness and to satisfy the compiler.

//data class Schema(
//    val value: Map<String, Any?>,
//    val $id: String? = null,
//    val $anchor: String? = null,
//    val $dynamicAnchor: String? = null
//) {
//    companion object {
//        fun fromMap(map: Map<String, Any?>): Schema = Schema(map)
//        fun fromBoolean(boolean: Boolean): Schema = Schema(mapOf())
//    }
//}
//
//class SchemaCache(loggingContext: LoggingContext? = null) {
//    suspend fun get(uri: URI): Schema? = TODO()
//    fun close() = TODO()
//}

//class LoggingContext
//
//class SchemaFetchException : Exception()

fun deepEquals(a: Any?, b: Any?): Boolean = TODO()
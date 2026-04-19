package com.peihua.genui.model

import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.primitives.basicCatalogId
import com.peihua.genui.primitives.surfaceIdKey
import com.peihua.json.schema.Schema
import com.peihua.json.utils.toMap
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.time.Instant

final object JsonKey {
    const val catalogId = "catalogId";
    const val components = "components";
    const val theme = "theme";
}
/**
 * A callback that is called when events are sent.
 */
typealias SendEventsCallback = (surfaceId: String, events: List<UiEvent>) -> Unit;

/**
 * A callback that is called when an event is dispatched.
 */
typealias DispatchEventCallback = (event: UiEvent) -> Unit;

abstract class UiEvent(private val _json: JsonMap) {
    /**
     * The ID of the surface that this event originated from.
     */
    val surfaceId: String = _json[surfaceIdKey] as String;

    /**
     * The ID of the widget that triggered the event.
     */
    val widgetId: String = _json["widgetId"] as String;

    /**
     * The type of event that was triggered (e.g., 'onChanged', 'onTap').
     */
    val eventType: String = _json["eventType"] as String;

    /**
     * The value associated with the event, if any.
     *
     * For example, the text in a `TextField`, or the value of a `Checkbox`.*/
    val value: Any? = _json["value"];

    /**
     * The timestamp of when the event occurred.
     */
    val timestamp: Instant = Instant.parse(_json["timestamp"] as String);

    /**
     * Converts this event to a map, suitable for JSON serialization.
     */
    fun toMap(): JsonMap = _json;
}

/**
 * A UI event that represents a user action.
 * Triggers a submission to the AI, such as tapping a button.
 */
class UserActionEvent(json: JsonMap) : UiEvent(json) {
    constructor(
        surfaceId: String? = null,
        name: String,
        sourceComponentId: String,
        timestamp: Instant? = null,
        context: JsonMap? = null,
    ) : this(
        mapOf(
            "name" to name,
            "timestamp" to (timestamp ?: Instant.now()),
            "surfaceId" to surfaceId,
            "sourceComponentId" to sourceComponentId,
            "context" to (context ?: mapOf()),
        )
    )

    /**
     * The name of the action.
     */
    val name: String = json["name"] as String;

    /**
     * The ID of the component that triggered the action.
     */
    val sourceComponentId: String = json["sourceComponentId"] as String;

    /**
     * Context associated with the action.
     */
    val context: JsonMap = json["context"] as JsonMap;
}

/** A contextual view of the main DataModel, used by widgets to resolve
 * relative and absolute paths.*/
class DataContext : ExecutionContext {
    override val path: DataPath
        get() = TODO("Not yet implemented")

    override fun getFunction(name: String): ClientFunction? {
        TODO("Not yet implemented")
    }

    override fun <T> subscribe(path: DataPath): Flow<T?> {
        TODO("Not yet implemented")
    }

    override fun <T> subscribeStream(path: DataPath): Flow<T?> {
        TODO("Not yet implemented")
    }

    override fun <T> getValue(path: DataPath): T? {
        TODO("Not yet implemented")
    }

    override fun update(path: DataPath, contents: Any?) {
        TODO("Not yet implemented")
    }

    override fun nested(relativePath: DataPath): DataContext {
        TODO("Not yet implemented")
    }

    override fun resolvePath(pathToResolve: DataPath): DataPath {
        TODO("Not yet implemented")
    }

    override fun resolve(value: Any?): Flow<Any?> {
        TODO("Not yet implemented")
    }

    override fun evaluateConditionStream(condition: Any?): Flow<Boolean> {
        TODO("Not yet implemented")
    }

}

/**
 * A component in the UI.
 */
@Serializable
data class Component(
    /** The unique ID of the component.*/
    val id: String,
    /** The type of the component (e.g. 'Text', 'Button').*/
    val type: String,
    /** The properties of the component.*/
    val properties: JsonMap,
) {
    companion object {
        /**
         * Creates a [Component] from a JSON map.
         */
        fun fromJson(json: JsonObject): Component {
            if (json["component"] == null) {
                throw IllegalArgumentException("Component.fromJson: component property is null");
            }
            val rawType = json["component"] as String;
            val id = json["id"] as String;

            val properties = json.toMutableMap()
            properties.remove("id");
            properties.remove("component");

            return Component(id = id, type = rawType, properties = properties);
        }
    }


    /**
     * Converts this object to a JSON map.
     */
    fun toJson(): JsonMap {
        return mapOf("id" to id, "component" to type, "properties" to properties);
    }
}

@Serializable
data class SurfaceDefinition(
    // The ID of the surface that this UI belongs to.
    val surfaceId: String,
    // The ID of the catalog to use for rendering this surface.
    val catalogId: String = basicCatalogId,
    // A map of all widget definitions in the UI, keyed by their ID.
    val components: Map<String, Component> = mapOf(),
    // The theme for this surface.
    val theme: JsonMap?,
) {
    companion object {
        /** Creates a [SurfaceDefinition] from a JSON map.*/
        fun fromJson(json: JsonObject): SurfaceDefinition {
            return Json.decodeFromJsonElement(json)
        }
    }

    /** Converts this object to a JSON map.*/
    fun toJson(): JsonElement {
        return Json.encodeToJsonElement(this)
    }

    /** Converts a UI definition into a blob of text.*/
    fun asContextDescriptionText(): String {
        val text = Json.encodeToString(this);
        return "A user interface is shown with the following content:\n$text.";
    }

    /**
     * Validates the UI definition against a schema.
     *
     * Throws [A2uiValidationException] if validation fails.
     */
    fun validate(schema: Schema) {
        val schemaMap = schema.toMap();
        var allowedSchemas = mutableListOf<Map<String, Any>>();
        when {
            schemaMap.containsKey("oneOf") -> {
                allowedSchemas.addAll((schemaMap["oneOf"] as List<*>).map { it.toMap() })
            }

            schemaMap.containsKey("properties") -> {
                val properties = (schemaMap["properties"] as Map<*, *>)
                if (properties.containsKey("components")) {
                    val componentsProp = properties["components"] as Map<*, *>
                    when {
                        componentsProp.containsKey("items") -> {
                            val items = componentsProp["items"] as Map<*, *>
                            if (items.containsKey("oneOf")) {
                                allowedSchemas.addAll((items["oneOf"] as List<*>).map { it.toMap() })
                            } else {
                                allowedSchemas.add(items.toMap())
                            }
                        }

                        componentsProp.containsKey("properties") -> {
                            val props = (componentsProp["properties"] as Map<*, *>)
                            allowedSchemas.addAll(props.values.map { it.toMap() })
                        }
                    }
                }
            }
        }
        if (allowedSchemas.isEmpty()) return
        for (component in components.values) {
            var matched = false;
            val errors = mutableListOf<String>()
            val instanceJson = component.toJson()
            for (s in allowedSchemas) {
                if (schemaMatchesType(s, component.type)) {
                    try {
                        validateInstance(instanceJson, s, "/components/${component.id}")
                        matched = true
                        break
                    } catch (e: Exception) {
                        errors.add(e.toString())
                    }
                }
            }

            if (!matched) {
                if (errors.isNotEmpty()) {
                    throw A2uiValidationException(
                        "Validation failed for component ${component.id} " +
                                "(${component.type}): ${errors.joinToString("; ")}",
                        surfaceId = surfaceId,
                        path = "/components/${component.id}",
                    )
                }
                throw A2uiValidationException(
                    "Unknown component type: ${component.type}",
                    surfaceId = surfaceId,
                    path = "/components/${component.id}",
                )

            }
        }
    }

    fun schemaMatchesType(schema: Map<String, *>, type: String): Boolean {
        val properties = schema["properties"] as? Map<*, *> ?: return false
        val compProp = properties["component"] as? Map<*, *> ?: return false
        return when {
            compProp.containsKey("const") -> {
                compProp["const"] == type
            }

            compProp.containsKey("enum") -> {
                (compProp["enum"] as List<*>).contains(type)
            }

            else -> false
        }
    }

    fun validateInstance(instance: Any?, schema: Map<String, *>, path: String) {
        if (instance == null) {
            return
        }
        if (schema.containsKey("const")) {
            if (instance != schema["const"]) {
                throw A2uiValidationException(
                    "Value mismatch. Expected ${schema["const"]}, got $instance",
                    surfaceId = surfaceId,
                    path = path,
                )
            }
        }

        val enumList = schema["const"] as? List<*>?
        if (enumList != null && !enumList.contains(instance)) {
            throw A2uiValidationException(
                "Value not in enum: $instance",
                surfaceId = surfaceId,
                path = path,
            )
        }
        if (instance is Map<*, *>) {
            val required = schema["required"] as? List<*>
            if (required != null) {
                for (key in required) {
                    if (key !is String) continue // 或者抛出类型错误
                    if (!instance.containsKey(key)) {
                        throw A2uiValidationException("Missing required property: $key", surfaceId, path)
                    }
                }
            }
            val props = schema["properties"] as? Map<*, *>
            if (props != null) {
                for ((key, propSchema) in props) {
                    if (instance.containsKey(key)) {
                        val propSchemaMap = propSchema as? Map<String, *>
                        if (propSchemaMap != null) {
                            validateInstance(instance[key], propSchema, "$path/$key")
                        }
                    }
                }
            }
        }
        // items (only if instance is List)
        if (instance is List<*>) {
            val itemsSchema = schema["items"] as? Map<String, Any?>
            if (itemsSchema != null) {
                for ((i, item) in instance.withIndex()) {
                    validateInstance(item, itemsSchema, "$path/$i")
                }
            }
        }
        // oneOf
        val oneOfs = schema["oneOf"] as? List<*>
        if (oneOfs != null) {
            var oneMatched = false
            for (item in oneOfs) {
                val subSchema = item as? Map<String, Any?> ?: continue
                try {
                    validateInstance(instance, subSchema, path)
                    oneMatched = true
                    break
                } catch (_: Exception) {
                    // continue
                }
            }
            if (!oneMatched) {
                throw A2uiValidationException("Value did not match any oneOf schema", surfaceId, path)
            }
        }
    }

}


/// Exception thrown when validation fails.
class A2uiValidationException(
    /// The error message.
    override val message: String,

    /// The ID of the surface where the validation error occurred.
    val surfaceId: String? = null,

    /// The path in the data/component model where the error occurred.
    final val path: String? = null,

    /// The JSON that caused the error.
    final val json: Any? = null,

    /// The underlying cause of the error.
    final override val cause: Throwable? = null,


    ) : Exception() {


    override fun toString(): String {
        val buffer = StringBuffer("A2uiValidationException: $message");
        if (surfaceId != null) buffer.append(" (surface: $surfaceId)");
        if (path != null) buffer.append(" (path: $path)");
        if (cause != null) buffer.append("\nCause: $cause");
        if (json != null) buffer.append("\nJSON: $json");
        return buffer.toString();
    }
}
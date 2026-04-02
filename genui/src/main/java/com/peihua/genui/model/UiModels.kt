package com.peihua.genui.model

import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.primitives.surfaceIdKey
import kotlinx.coroutines.flow.Flow
import java.util.Date

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
    val timestamp: Date = Date(Date.parse(_json["timestamp"] as String));

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
    constructor(surfaceId: String? = null, name: String, sourceComponentId: String, timestamp: Date? = null, context: JsonMap? = null) : this(
        mapOf(
            "name" to name,
            "timestamp" to (timestamp?.time ?: Date().time),
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

    override fun nested(relativePath: DataPath): ExecutionContext {
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
        fun fromJson(json: JsonMap): Component {
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
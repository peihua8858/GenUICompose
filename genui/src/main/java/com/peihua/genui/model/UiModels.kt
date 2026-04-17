package com.peihua.genui.model

import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.primitives.basicCatalogId
import com.peihua.genui.primitives.surfaceIdKey
import com.peihua.json.schema.Schema
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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
  /// Converts this object to a JSON map.
  fun toJson() : JsonElement {
      return Json.encodeToJsonElement(this)
  }
      /// Converts a UI definition into a blob of text.
  fun asContextDescriptionText():String {
    val text = Json.encodeToString(this);
    return "A user interface is shown with the following content:\n$text.";
  }

  /// Validates the UI definition against a schema.
  ///
  /// Throws [A2uiValidationException] if validation fails.
  fun validate( schema: Schema) {
    val jsonOutput = schema.toJson();
    val schemaMap = Json.decodeFromString<Map<String, Any>>(jsonOutput)

    var allowedSchemas = mutableListOf<Map<String, Any>>();
    if (schemaMap.containsKey("oneOf")) {
      allowedSchemas = (schemaMap["oneOf"] as MutableList<Map<String, Any>>);
    } else if (schemaMap.containsKey("properties") &&
        (schemaMap["properties"] as Map).containsKey("components")) {
      val componentsProp =
          (schemaMap["properties"] as Map)["components"]
              as Map<String, dynamic>;
      if (componentsProp.containsKey("items")) {
        final items = componentsProp['items'] as Map<String, dynamic>;
        if (items.containsKey('oneOf')) {
          allowedSchemas = (items['oneOf'] as List)
              .cast<Map<String, dynamic>>();
        } else {
          allowedSchemas = [items];
        }
      } else if (componentsProp.containsKey('properties')) {
        allowedSchemas = (componentsProp['properties'] as Map).values
            .cast<Map<String, dynamic>>()
            .toList();
      }
    }

    if (allowedSchemas.isEmpty) {
      return;
    }

    for (final Component component in components.values) {
      var matched = false;
      List<String> errors = [];
      final JsonMap instanceJson = component.toJson();

      for (final s in allowedSchemas) {
        if (_schemaMatchesType(s, component.type)) {
          try {
            _validateInstance(instanceJson, s, '/components/${component.id}');
            matched = true;
            break;
          } catch (e) {
            errors.add(e.toString());
          }
        }
      }

      if (!matched) {
        if (errors.isNotEmpty) {
          throw A2uiValidationException(
            'Validation failed for component ${component.id} '
            '(${component.type}): ${errors.join("; ")}',
            surfaceId: surfaceId,
            path: '/components/${component.id}',
          );
        }
        throw A2uiValidationException(
          'Unknown component type: ${component.type}',
          surfaceId: surfaceId,
          path: '/components/${component.id}',
        );
      }
    }
  }
}
//class SurfaceDefinition {
//


//
//  bool _schemaMatchesType(Map<String, dynamic> schema, String type) {
//    if (schema case {
//      'properties': {'component': Map<String, dynamic> compProp},
//    }) {
//      return switch (compProp) {
//        {'const': String constType} when constType == type => true,
//        {'enum': List<dynamic> enums} when enums.contains(type) => true,
//        _ => false,
//      };
//    }
//    return false;
//  }
//
//  void _validateInstance(
//    Object? instance,
//    Map<String, dynamic> schema,
//    String path,
//  ) {
//    if (instance == null) {
//      return;
//    }
//
//    if (schema case {'const': Object? constVal} when instance != constVal) {
//      throw A2uiValidationException(
//        'Value mismatch. Expected $constVal, got $instance',
//        surfaceId: surfaceId,
//        path: path,
//      );
//    }
//
//    if (schema case {
//      'enum': List<dynamic> enums,
//    } when !enums.contains(instance)) {
//      throw A2uiValidationException(
//        'Value not in enum: $instance',
//        surfaceId: surfaceId,
//        path: path,
//      );
//    }
//
//    if (schema case {'required': List<dynamic> required} when instance is Map) {
//      for (final String key in required.cast<String>()) {
//        if (!instance.containsKey(key)) {
//          throw A2uiValidationException(
//            'Missing required property: $key',
//            surfaceId: surfaceId,
//            path: path,
//          );
//        }
//      }
//    }
//
//    if (schema case {
//      'properties': Map<String, dynamic> props,
//    } when instance is Map) {
//      for (final MapEntry<String, dynamic> entry in props.entries) {
//        final String key = entry.key;
//        final propSchema = entry.value as Map<String, dynamic>;
//        if (instance.containsKey(key)) {
//          _validateInstance(instance[key], propSchema, '$path/$key');
//        }
//      }
//    }
//
//    if (schema case {
//      'items': Map<String, dynamic> itemsSchema,
//    } when instance is List) {
//      for (var i = 0; i < instance.length; i++) {
//        _validateInstance(instance[i], itemsSchema, '$path/$i');
//      }
//    }
//
//    if (schema case {'oneOf': List<dynamic> oneOfs}) {
//      var oneMatched = false;
//      for (final Map<String, dynamic> s
//          in oneOfs.cast<Map<String, dynamic>>()) {
//        try {
//          _validateInstance(instance, s, path);
//          oneMatched = true;
//          break;
//        } catch (_) {}
//      }
//      if (!oneMatched) {
//        throw A2uiValidationException(
//          'Value did not match any oneOf schema',
//          surfaceId: surfaceId,
//          path: path,
//        );
//      }
//    }
//  }
//}
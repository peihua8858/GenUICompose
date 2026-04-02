package com.peihua.genui.model

import com.peihua.genui.primitives.surfaceIdKey
import com.peihua.json.JsonType
import com.peihua.json.schema.S
import com.peihua.json.schema.Schema
import kotlin.collections.map

object A2uiSchemas {
    fun clientFunctions(): Schema {
        return S.list(
            title = "A2UI Client Functions",
            description = "A list of functions available for use in the client.",
            items = S.combined(
                oneOf = listOf(
                    _requiredFunction(),
                    _regexFunction(),
                    _lengthFunction(),
                    _numericFunction(),
                    _emailFunction(),
                    _formatStringFunction(),
                    _formatNumberFunction(),
                    _formatCurrencyFunction(),
                    _formatDateFunction(),
                    _andFunction(),
                    _orFunction(),
                    _notFunction(),
                ),
            ),
        );
    }

    fun _functionDefinition(
        name: String,
        description: String,
        returnType: String,
        args: Schema,
    ): Schema {
        return S.obj(
            description = description,
            properties = mapOf(
                "call" to S.string(constValue = name),
                "args" to args,
                "returnType" to S.string(constValue = returnType),
            ),
            required = listOf("call", "args"),
        );
    }

    fun _requiredFunction(): Schema {
        return _functionDefinition(
            name = "required",
            description = "Checks that the value is not null, undefined, or empty.",
            returnType = "boolean",
            args = S.obj(
                properties = mapOf("value" to S.any(description = "The value to check.")),
                required = listOf("value"),
            ),
        );
    }

    fun _regexFunction(): Schema {
        return _functionDefinition(
            name = "regex",
            description = "Checks that the value matches a regular expression string.",
            returnType = "boolean",
            args = S.obj(
                properties = mapOf(
                    "value" to S.any(), // DynamicString
                    "pattern" to S.string(
                        description = "The regex pattern to match against.",
                    ),
                ),
                required = listOf("value", "pattern"),
            ),
        );
    }

    fun _lengthFunction(): Schema {
        return _functionDefinition(
            name = "length",
            description = "Checks string length constraints.",
            returnType = "boolean",
            args = S.obj(
                properties = mapOf(
                    "value" to S.any(), // DynamicString
                    "min" to S.integer(
                        minimum = 0,
                        description = "The minimum allowed length.",
                    ),
                    "max" to S.integer(
                        minimum = 0,
                        description = "The maximum allowed length.",
                    ),
                ),
                required = listOf("value"),
            ),
        );
    }

    fun _numericFunction(): Schema {
        return _functionDefinition(
            name = "numeric",
            description = "Checks numeric range constraints.",
            returnType = "boolean",
            args = S.obj(
                properties = mapOf(
                    "value" to S.any(), // DynamicNumber
                    "min" to S.number(description = "The minimum allowed value."),
                    "max" to S.number(description = "The maximum allowed value."),
                ),
                required = listOf("value"),
            ),
        );
    }

    fun _emailFunction(): Schema {
        return _functionDefinition(
            name = "email",
            description = "Checks that the value is a valid email address.",
            returnType = "boolean",
            args = S.obj(
                properties = mapOf(
                    "value" to S.any(), // DynamicString
                ),
                required = listOf("value"),
            ),
        )
    }

    fun _formatStringFunction(): Schema {
        return _functionDefinition(
            name = "formatString",
            description =
                """Performs string interpolation of data model values and other functions.""",
            returnType = "string",
            args = S.obj(
                properties = mapOf("value" to S.any(description = "The string to format.")),
                required = listOf("value"),
                additionalProperties = true, // Allow other interpolation args
            ),
        );
    }

    fun _formatNumberFunction(): Schema {
        return _functionDefinition(
            name = "formatNumber",
            description =
                "Formats a number with the specified grouping and decimal precision.",
            returnType = "string",
            args = S.obj(
                properties = mapOf(
                    "value" to S.number(description = "The number to format."),
                    "decimalPlaces" to S.integer(
                        description = "Optional. The number of decimal places to show.",
                    ),
                    "useGrouping" to S.boolean(
                        description = """Optional. If true, uses locale-specific grouping separators.""",
                    ),
                ),
                required = listOf("value"),
            ),
        )
    }

    fun _formatCurrencyFunction(): Schema {
        return _functionDefinition(
            name = "formatCurrency",
            description = "Formats a number as a currency string.",
            returnType = "string",
            args = S.obj(
                properties = mapOf(
                    "value" to S.number(description = "The monetary amount."),
                    "currencyCode" to S.string(
                        description = "The ISO 4217 currency code (e.g., 'USD', 'EUR').",
                    ),
                ),
                required = listOf("value", "currencyCode"),
            ),
        );
    }

    fun _formatDateFunction(): Schema {
        return _functionDefinition(
            name = "formatDate",
            description = "Formats a timestamp into a string using a pattern.",
            returnType = "string",
            args = S.obj(
                properties = mapOf(
                    "value" to S.any(description = "The date to format."),
                    "pattern" to S.string(
                        description = "The format pattern (e.g. 'MM/dd/yyyy').",
                    ),
                ),
                required = listOf("value", "pattern"),
            ),
        );
    }

    fun _andFunction(): Schema {
        return _functionDefinition(
            name = "and",
            description = "Performs logical AND on a list of values.",
            returnType = "boolean",
            args = S.obj(
                properties = mapOf("values" to S.list(items = S.any(), minItems = 2)),
                required = listOf("values"),
            ),
        );
    }

    fun _orFunction(): Schema {
        return _functionDefinition(
            name = "or",
            description = "Performs logical OR on a list of values.",
            returnType = "boolean",
            args = S.obj(
                properties = mapOf("values" to S.list(items = S.any(), minItems = 2)),
                required = listOf("values"),
            ),
        );
    }

    fun _notFunction(): Schema {
        return _functionDefinition(
            name = "not",
            description = "Performs logical NOT on a value.",
            returnType = "boolean",
            args = S.obj(
                properties = mapOf("value" to S.any()),
                required = listOf("value")
            ),
        );
    }

    /// Schema for a function call.
    fun functionCall(): Schema = S.obj(
        properties = mapOf(
            "call" to S.string(description = "The name of the function to call."),
            "args" to S.obj(
                description = "Arguments to pass to the function.",
                additionalProperties = true,
            ),
        ),
        required = listOf("call"),
    );

    /// Schema for a validation check, including logic and an error message.
    fun validationCheck(description: String? = null): Schema {
        return S.obj(
            description = description,
            properties = mapOf(
                "message" to S.string(description = "Error message if validation fails."),
                "condition" to S.any(
                    description =
                        "DynamicBoolean condition (FunctionCall, DataBinding, or literal).",
                ),
            ),
            required = listOf("message", "condition"),
        );
    }

    /**
     * Schema for a value that can be either a literal string or a
     * data-bound path to a string in the DataModel.
     **/
    fun stringReference(description: String? = null, enumValues: List<String>? = null): Schema {
        val literal = S.string(
            description = "A literal string value.",
            enumValues = enumValues,
        );
        val binding = dataBindingSchema(
            description = "A path to a string.",
        );
        val function = functionCall()
        return S.combined(
            oneOf = listOf(literal, binding, function),
            description = description,
        );
    }

    /// Schema for a value that can be either a literal number or a
    /// data-bound path to a number in the DataModel.
    fun numberReference(description: String? = null): Schema {
        val literal = S.number(description = "A literal number value.");
        val binding = dataBindingSchema(
            description = "A path to a number.",
        );
        val function = functionCall();
        return S.combined(
            oneOf = listOf(literal, binding, function),
            description = description,
        );
    }

    /// Schema for a value that can be either a literal boolean or a
    /// data-bound path to a boolean in the DataModel.
    fun booleanReference(description: String? = null): Schema {
        val literal = S.boolean(description = "A literal boolean value.");
        val binding = dataBindingSchema(
            description = "A path to a boolean.",
        );
        val function = functionCall();
        return S.combined(
            oneOf = listOf(literal, binding, function),
            description = description,
        );
    }

    /// Helper to create a DataBinding schema.
    fun dataBindingSchema(description: String? = null): Schema {
        return S.obj(
            description = description,
            properties = mapOf(
                "path" to S.string(
                    description = "A relative or absolute path in the data model.",
                ),
            ),
            required = listOf("path"),
        );
    }

    /// Schema for a property that holds a list of child components,
    /// either as an explicit list of IDs or a data-bound template.
    fun componentArrayReference(description: String? = null): Schema {
        val idList = S.list(items = S.string(description = "Component ID"));
        val template = S.obj(
            properties = mapOf(
                "componentId" to componentReference(),
                "path" to S.string(
                    description = "A relative or absolute path in the data model.",
                ),
            ),
            required = listOf("componentId", "path"),
        );
        return S.combined(oneOf = listOf(idList, template), description = description);
    }

    /// Schema for a list of validation checks.
    fun checkable(description: String? = null): Schema {
        return S.list(
            description = description ?: "Validation rules for this component.",
            items = validationCheck(),
        );
    }

    /// Schema for a user-initiated action.
    ///
    /// Can be either a server-side event or a client-side function call.
    fun action(description: String? = null): Schema {
        val eventSchema = S.obj(
            properties = mapOf(
                "event" to S.obj(
                    properties = mapOf(
                        "name" to S.string(description = "The name of the action to be dispatched to the server."),
                        "context" to S.obj(
                            description = "Arbitrary context data to send with the action.",
                            additionalProperties = true,
                        ),
                    ),
                    required = listOf("name"),
                ),
            ),
            required = listOf("event"),
        );

        val functionCallSchema = S.obj(
            properties = mapOf("functionCall" to functionCall()),
            required = listOf("functionCall"),
        );

        return S.combined(
            description = description,
            oneOf = listOf(eventSchema, functionCallSchema),
        );
    }

    /// Schema for a value that can be either a literal array of strings or a
    /// data-bound path to an array of strings.
    fun stringArrayReference(description: String?): Schema {
        val literal = S.list(items = S.string());
        val binding = dataBindingSchema(
            description = "A path to a string list.",
        );
        val function = functionCall();
        return S.combined(
            oneOf = listOf(literal, binding, function),
            description = description,
        );
    }

    /// Schema for a createSurface message.
    fun createSurfaceSchema(): Schema = S.obj(
        properties = mapOf(
            surfaceIdKey to S.string(description = "The unique ID for the surface."),
            "catalogId" to S.string(description = "The URI of the component catalog."),
            "theme" to S.obj(
                description = "Theme parameters for the surface.",
                additionalProperties = true,
            ),
            "sendDataModel" to S.boolean(
                description = "Whether to send the data model to every client request.",
            ),
        ),
        required = listOf(surfaceIdKey, "catalogId"),
    );

    /// Schema for a deleteSurface message.
    fun deleteSurfaceSchema() = S.obj(
        properties = mapOf(surfaceIdKey to S.string()),
        required = listOf(surfaceIdKey),
    );

    /// Schema for a updateDataModel message.
    fun updateDataModelSchema() = S.obj(
        properties = mapOf(
            surfaceIdKey to S.string(),
            "path" to S.combined(type = JsonType.STRING, defaultValue = "/"),
            "value" to S.any(
                description =
                    "The new value to write to the data model. If null/omitted, the key is removed.",
            ),
        ),
        required = listOf(surfaceIdKey),
    );

    /// Schema for a component reference (ID).
    fun componentReference(description: String? = null): Schema {
        return S.string(description = description ?: "The ID of a component.");
    }

    /// Schema for a updateComponents message.
    fun updateComponentsSchema(catalog: Catalog): Schema {
        // Collect specific component schemas from the catalog.
        // We assume catalog items have updated schemas (flattened).
        val componentSchemas = catalog.items.map { item -> item.dataSchema }.toList()

        return S.obj(
            properties = mapOf(
                surfaceIdKey to S.string(
                    description = "The unique identifier for the UI surface.",
                ),
                "components" to S.list(
                    description = "A flat list of component definitions.",
                    minItems = 1,
                    items = if (componentSchemas.isEmpty())
                        S.obj(description = "No components in catalog.") else S.combined(
                        oneOf = componentSchemas,
                        description = "Must match one of the component definitions in the catalog.",
                    ),
                ),
            ),
            required = listOf(surfaceIdKey, "components"),
        );
    }

    /// Schema for a value that can be either a literal list or a reference.
    fun listOrReference(items: Schema, description: String? = null): Schema {
        val literal = S.list(items = items);
        val binding = dataBindingSchema(description = "A path to a list.");
        val function = functionCall();
        return S.combined(
            oneOf = listOf(literal, binding, function),
            description = description,
        );
    }

    /// Schema for a generic property value (literal, binding, or function).
    fun propertyReference(description: String? = null): Schema {
        val binding = dataBindingSchema(description = "A path to a value.");
        val function = functionCall();
        // We allow any type for the literal value since we don't know it here.
        // Ideally usage would be more specific if possible.
        return S.combined(
            oneOf = listOf(S.any(), binding, function),
            description = description,
        );
    }

}
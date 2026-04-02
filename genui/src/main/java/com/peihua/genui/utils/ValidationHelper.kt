package com.peihua.genui.utils

import com.peihua.genui.model.DataContext
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.widgets.resolveContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * A validation error with a message.
 */
data class ValidationError(val message: String)

/**
 * A helper class for handling reactive validation logic.
 */
class ValidationHelper {
    companion object {
        /**
         * Validates a value against a list of checks.
         *
         * Returns a [Stream] that emits an error message if any check fails, or null
         * if all checks pass.
         *
         * The [checks] list should contain maps with 'condition' and optional
         * 'message' keys.
         */
        fun validateFlow(checks: List<JsonMap>?, context: DataContext?): Flow<String?> {
            if (checks == null || checks.isEmpty() || context == null) {
                return flowOf(null)
            }

            val streams = mutableListOf<Flow<Pair<Boolean, String>>>();
            for (check in checks) {
                val message = check["message"] as String? ?: "Invalid value";
                streams.add(context.evaluateConditionStream(check["condition"]).map { isValid -> isValid to message })
            }

            return combine(streams) { results ->
                results.firstOrNull { !it.first }?.second
            }
        }
    }

    /**
     * Validates a value against a schema, resolving any expressions in the
     * schema.
     */
    suspend fun validate(value: Any?, schema: JsonMap, dataContext: DataContext): List<ValidationError> {
        val errors = mutableListOf<ValidationError>();

        // Resolve schema constraints that might be expressions
        val resolvedSchema = resolveContext(dataContext, schema);

        // simple validation for now, delegating to json_schema_builder would be
        // ideal, but for now we just check basic constraints we support in genui
        val actualType = value?.let { it::class.simpleName } ?: "null"
        if (resolvedSchema.containsKey("type")) {
            val type = resolvedSchema["type"];
            if (type == "string" && value !is String) {
                errors.add(
                    ValidationError("Expected string, got $actualType"),
                );
            } else if (type == "number" && value !is Number) {
                errors.add(
                    ValidationError("Expected number, got $actualType"),
                );
            } else if (type == "boolean" && value !is Boolean) {
                errors.add(
                    ValidationError("Expected boolean, got $actualType"),
                );
            }
        }

        if (resolvedSchema.containsKey("required") && value is Map<*, *>) {
            val required = resolvedSchema["required"] as List<*>;
            for (key in required) {
                if (!value.containsKey(key)) {
                    errors.add(ValidationError("Missing required key: $key"));
                }
            }
        }
        // TODO: Add more validation logic as needed, potentially using a library
        return errors;
    }
}
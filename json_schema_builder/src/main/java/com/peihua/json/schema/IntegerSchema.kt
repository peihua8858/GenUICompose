package com.peihua.json.schema

import com.peihua.json.JsonType
import com.peihua.json.ValidationError
import com.peihua.json.ValidationErrorType

data class IntegerSchema(value: MutableMap<String, Any>) : Schema(value) {
    constructor(
        // Core keywords
        title: String? = null,
        description: String? = null,
        //Number-specific keywords The inclusive lower bound of the integer.
        minimum: Int? = null,
        // The inclusive upper bound of the integer.
        maximum: Int? = null,
        // The exclusive lower bound of the integer.
        exclusiveMinimum: Int? = null,
        // The exclusive upper bound of the integer.
        exclusiveMaximum: Int? = null,
        /// The integer must be a multiple of this number.
        multipleOf: Number? = null,
    ) : this(
        mutableMapOf<String, Any>(
            "type" to JsonType.INT.typeName,
            "title" to (title ?: ""),
            "description" to (description ?: ""),
            "minimum" to (minimum ?: 0),
            "maximum" to (maximum ?: 0),
            "exclusiveMinimum" to (exclusiveMinimum ?: 0),
            "exclusiveMaximum" to (exclusiveMaximum ?: 0),
            "multipleOf" to (multipleOf ?: 0),
        )
    )
    /// Validates the given integer against the schema constraints.
    ///
    /// This is a helper method used by the main validation logic.
    fun validateInteger(
        data: Int,
        currentPath:List<String>,
        accumulatedFailures:HashSet<ValidationError>,
    ) {
        if (minimum case final min? when data < min) {
            accumulatedFailures.add(
                ValidationError(
                    ValidationErrorType.minimumNotMet,
                    path: currentPath,
                    details: 'Value $data is less than the minimum of $min',
            ),
            );
        }
        if (maximum case final max? when data > max) {
            accumulatedFailures.add(
                ValidationError(
                    ValidationErrorType.maximumExceeded,
                    path: currentPath,
                    details: 'Value $data is more than the maximum of $max',
            ),
            );
        }
        if (exclusiveMinimum case final exclusiveMin? when data <= exclusiveMin) {
            accumulatedFailures.add(
                ValidationError(
                    ValidationErrorType.exclusiveMinimumNotMet,
                    path: currentPath,
                    details: 'Value $data is not greater than $exclusiveMin',
            ),
            );
        }
        if (exclusiveMaximum case final exclusiveMax? when data >= exclusiveMax) {
            accumulatedFailures.add(
                ValidationError(
                    ValidationErrorType.exclusiveMaximumExceeded,
                    path: currentPath,
                    details: 'Value $data is not less than $exclusiveMax',
            ),
            );
        }
        if (multipleOf case final multOf? when multOf != 0) {
            final double remainder = data / multOf;
            if (remainder.isInfinite || remainder.isNaN) {
                accumulatedFailures.add(
                    ValidationError(
                        ValidationErrorType.multipleOfInvalid,
                        path: currentPath,
                        details: 'Value $data is not a multiple of $multOf',
                ),
                );
            } else if ((remainder - remainder.truncate()).abs() > 1e-9) {
                accumulatedFailures.add(
                    ValidationError(
                        ValidationErrorType.multipleOfInvalid,
                        path: currentPath,
                        details: 'Value $data is not a multiple of $multOf',
                ),
                );
            }
        }
    }
}
package com.peihua.json

/** Enum representing the types of validation failures when checking data
 * against a schema. */
enum class ValidationErrorType {
    // For custom validation.
    custom,

    // General
    typeMismatch,
    constMismatch,
    enumValueNotAllowed,
    formatInvalid,
    refResolutionError,

    // Schema combinators
    allOfNotMet,
    anyOfNotMet,
    oneOfNotMet,
    notConditionViolated,
    ifThenElseInvalid,

    // Object specific
    requiredPropertyMissing,
    dependentRequiredMissing,
    additionalPropertyNotAllowed,
    minPropertiesNotMet,
    maxPropertiesExceeded,
    propertyNamesInvalid,
    patternPropertyValueInvalid,
    unevaluatedPropertyNotAllowed,

    // Array/List specific
    minItemsNotMet,
    maxItemsExceeded,
    uniqueItemsViolated,
    containsInvalid,
    minContainsNotMet,
    maxContainsExceeded,
    itemInvalid,
    prefixItemInvalid,
    unevaluatedItemNotAllowed,

    // String specific
    minLengthNotMet,
    maxLengthExceeded,
    patternMismatch,

    // Number/Integer specific
    minimumNotMet,
    maximumExceeded,
    exclusiveMinimumNotMet,
    exclusiveMaximumExceeded,
    multipleOfInvalid,
}

@JvmInline
value class ValidationError(private val value: Map<String, Any?>) {
    companion object {
        operator fun invoke(
            error: ValidationErrorType,
            path: List<String>,
            details: String? = null,
        ): ValidationError {
            return ValidationError(
                mapOf(
                    "error" to error.name,
                    "path" to path.toList(),
                    "details" to details
                )
            )
        }

        fun typeMismatch(
            path: List<String>,
            expectedType: Any,
            actualValue: Any?,
        ): ValidationError {
            return ValidationError(
                error = ValidationErrorType.typeMismatch,
                path = path,
                details = "Value `$actualValue` is not of type `$expectedType`"
            )
        }

        fun fromMap(map: Map<String, Any>): ValidationError {
            return ValidationError(map.toMutableMap())
        }
    }
}
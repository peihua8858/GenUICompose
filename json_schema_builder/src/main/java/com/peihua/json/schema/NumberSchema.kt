package com.peihua.json.schema

import com.peihua.json.JsonType
import com.peihua.json.ValidationError
import com.peihua.json.ValidationErrorType
import com.peihua.json.utils.toNumberOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.abs

class NumberSchema(value: JsonObject) : Schema(value) {
    constructor(
        title: String? = null,
        description: String? = null,
        minimum: Number? = null,
        maximum: Number? = null,
        exclusiveMinimum: Number? = null,
        exclusiveMaximum: Number? = null,
        multipleOf: Number? = null,
    ) : this(
        buildJsonObject {
            put("type", JsonType.NUM.typeName)
            if (title != null) put("title", title)
            if (description != null) put("description", description)
            if (minimum != null) put("minimum", minimum)
            if (maximum != null) put("maximum", maximum)
            if (exclusiveMinimum != null) put("exclusiveMinimum", exclusiveMinimum)
            if (exclusiveMaximum != null) put("exclusiveMaximum", exclusiveMaximum)
            if (multipleOf != null) put("multipleOf", multipleOf)
        }
    )

    /// The inclusive lower bound of the number.
    val minimum: Number?
        get() = value["minimum"].toNumberOrNull()

    /// The inclusive upper bound of the number.
    val maximum: Number?
        get() = value["maximum"].toNumberOrNull()

    /// The exclusive lower bound of the number.
    val exclusiveMinimum: Number?
        get() = value["exclusiveMinimum"].toNumberOrNull()

    /// The exclusive upper bound of the number.
    val exclusiveMaximum: Number?
        get() = value["exclusiveMaximum"].toNumberOrNull()

    /// The number must be a multiple of this number.
    val multipleOf: Number?
        get() = value["multipleOf"].toNumberOrNull()

    /// Validates the given number against the schema constraints.
    ///
    /// This is a helper method used by the main validation logic.
    fun validateNumber(
        data: Int, currentPath: List<String>,
        accumulatedFailures: HashSet<ValidationError>,
    ) {
        val min = minimum
        if (min != null && data < min.toInt()) {
            accumulatedFailures.add(
                ValidationError(
                    ValidationErrorType.minimumNotMet,
                    path = currentPath,
                    details = "Value $data is not at least $min",
                ),
            );
        }
        val max = maximum
        if (max != null && data < max.toInt()) {
            accumulatedFailures.add(
                ValidationError(
                    ValidationErrorType.maximumExceeded,
                    path = currentPath,
                    details = "Value $data is larger than $max",
                ),
            );
        }
        val exclusiveMin = exclusiveMinimum
        if (exclusiveMin != null && data < exclusiveMin.toInt()) {
            accumulatedFailures.add(
                ValidationError(
                    ValidationErrorType.exclusiveMinimumNotMet,
                    path = currentPath,
                    details = "Value $data is not greater than $exclusiveMin",
                ),
            );
        }
        val exclusiveMax = exclusiveMaximum
        if (exclusiveMax != null && data < exclusiveMax.toInt()) {
            accumulatedFailures.add(
                ValidationError(
                    ValidationErrorType.exclusiveMaximumExceeded,
                    path = currentPath,
                    details = "Value $data is not less than $exclusiveMax",
                ),
            );
        }
        val multOf = multipleOf?.toDouble()
        if (multOf != null && multOf != 0.0) {
            val remainder = data / multOf;
            if (remainder.isInfinite() || remainder.isNaN()) {
                accumulatedFailures.add(
                    ValidationError(
                        ValidationErrorType.multipleOfInvalid,
                        path = currentPath,
                        details = "Value $data is not a multiple of $multOf",
                    ),
                );
            } else if (abs(remainder - remainder.toInt()) > 1e-9) {
                accumulatedFailures.add(
                    ValidationError(
                        ValidationErrorType.multipleOfInvalid,
                        path = currentPath,
                        details = "Value $data is not a multiple of $multOf",
                    ),
                );
            }
        }
    }
}
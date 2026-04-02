package com.peihua.genui.catalog.basic_catalog_widgets

import com.peihua.genui.primitives.JsonMap

class WidgetHelpers {
}


/**
 * Converts a list of validation checks into a single expression that evaluates
 * to true if all checks pass.
 */
fun checksToExpression(checks: List<JsonMap>?): Any? {
    if (checks == null || checks.isEmpty()) {
        return true;
    }
    // Combine all checks into a single 'and' condition
    return mapOf(
        "functionCall" to mapOf(
            "call" to "and",
            "args" to mapOf("values" to checks.map { c -> c["condition"] }.toList())
        ),
    );
}
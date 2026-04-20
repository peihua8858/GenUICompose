package com.peihua.genui.catalog

import android.net.Uri
import android.nfc.FormatException
import android.telecom.Call.Details.can
import com.peihua.genui.model.ClientFunctionReturnType
import com.peihua.genui.model.ExecutionContext
import com.peihua.genui.model.SynchronousClientFunction
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.utils.toInteger
import com.peihua.json.schema.S
import com.peihua.json.schema.Schema
import kotlin.collections.containsKey
import androidx.core.net.toUri
import com.peihua.genui.functions.FormatStringFunction
import com.peihua.genui.model.ClientFunction
import com.peihua.genui.utils.format
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.collections.containsKey
import kotlin.text.get

/**
 * ignore: avoid_classes_with_only_static_members
 * A collection of basic client-side functions.
 */
object BasicFunctions {
    val requiredFunction = RequiredFunction()
    val regexFunction = RegexFunction()
    val lengthFunction = LengthFunction()
    val numericFunction = NumericFunction()
    val emailFunction = EmailFunction()
    val formatStringFunction = FormatStringFunction()
    val openUrlFunction = OpenUrlFunction()
    val formatNumberFunction = FormatNumberFunction()
    val formatCurrencyFunction = FormatCurrencyFunction()
    val formatDateFunction = FormatDateFunction()
    val pluralizeFunction = PluralizeFunction()
    val andFunction = AndFunction()
    val orFunction = OrFunction()
    val notFunction = NotFunction()

    /** Returns a list of all basic functions.*/
    val all: List<ClientFunction>
        get() = listOf(
            requiredFunction,
            regexFunction,
            lengthFunction,
            numericFunction,
            emailFunction,
            formatStringFunction,
            openUrlFunction,
            formatNumberFunction,
            formatCurrencyFunction,
            formatDateFunction,
            pluralizeFunction,
            andFunction,
            orFunction,
            notFunction,
        )
}

/// Helper to check for truthiness.
fun _isTruthy(value: Any?): Boolean {
    if (value is Boolean) return value;
    if (value == null) return false;
    return true;
}

/// Checks if all values in a list are truthy.
class AndFunction : SynchronousClientFunction() {
    override val name: String
        get() = "and";
    override val description: String
        get() = "Performs a logical AND operation on a list of boolean values.";
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.BOOLEAN
    override val argumentSchema: Schema
        get() = S.obj(properties = mapOf("values" to S.list(items = S.any())));

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any? {
        if (!args.containsKey("values")) return false;
        val values = args["values"];
        if (values !is List<*>) return false;
        for (element in values) {
            if (!_isTruthy(element)) return false;
        }
        return true;
    }
}

/// Checks if any value in a list is truthy.
class OrFunction : SynchronousClientFunction() {
    override val name: String
        get() = "or";
    override val description: String
        get() = "Performs a logical OR operation on a list of boolean values.";
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.BOOLEAN
    override val argumentSchema: Schema
        get() = S.obj(properties = mapOf("values" to S.list(items = S.any())));

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any? {
        if (!args.containsKey("values")) return false;
        val values = args["values"];
        if (values !is List<*>) return false;
        for (element in values) {
            if (_isTruthy(element)) return true;
        }
        return false;
    }
}

/// Negates a boolean value.
class NotFunction : SynchronousClientFunction() {
    override val name: String
        get() = "not";

    //
    override val description: String
        get() = "Performs a logical NOT operation on a boolean value.";
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.BOOLEAN
    override val argumentSchema: Schema
        get() = S.obj(properties = mapOf("value" to S.any()));

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any? {
        if (!args.containsKey("value")) return false;
        return !_isTruthy(args["value"]);
    }
}

/**
 * Checks if a value is present and not empty.
 */
class RequiredFunction : SynchronousClientFunction() {
    override val name: String = "required"
    override val description: String
        get() = "Checks that the value is not null, undefined, or empty."
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.BOOLEAN
    override val argumentSchema: Schema
        get() = S.obj(properties = mapOf("value" to S.any()))

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any {
        if (!args.containsKey("value")) return false
        val value = args["value"] ?: return false
        if (value is String) return value.isNotEmpty()
        if (value is List<*>) return value.isNotEmpty()
        if (value is Map<*, *>) return value.isNotEmpty()
        return true
    }
}

/**
 * Checks if a string matches a regex pattern.
 */
class RegexFunction : SynchronousClientFunction() {

    override val name: String
        get() = "regex"
    override val description: String
        get() = "Checks that the value matches a regular expression string."
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.BOOLEAN
    override val argumentSchema: Schema
        get() = S.obj(properties = mapOf("value" to S.string(), "pattern" to S.string()))

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any {
        val value = args["value"]
        val pattern = args["pattern"]
        if (value !is String || pattern !is String) return false
        try {
            return Regex(pattern).matches(value)
        } catch (exception: FormatException) {
            throw FormatException("Invalid regex pattern: $pattern. $exception")
        }
    }
}

/**
 * Returns the length of a string, list, or map.
 */
class LengthFunction : SynchronousClientFunction() {
    override val name: String
        get() = "length"
    override val description: String
        get() = "Checks string length constraints."
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.ANY
    override val argumentSchema: Schema
        get() = S.obj(
            properties = mapOf("value" to S.any(), "min" to S.integer(), "max" to S.integer()),
        )

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any {
        val value = args["value"]
        var length = 0
        if (value == null) {
            length = 0
        } else if (value is String) {
            length = value.length
        } else if (value is List<*>) {
            length = value.size
        } else if (value is Map<*, *>) {
            length = value.size
        } else {
            length = 0
        }

        if (args.containsKey("min") || args.containsKey("max")) {
            if (args.containsKey("min")) {
                val min = args["min"].toInteger()
                if (length < min) return false
            }
            if (args.containsKey("max")) {
                val max = args["max"].toInteger()
                if (length > max) return false
            }
            return true
        }
        return length
    }
}

/**
 * Checks if a value is numeric and optionally within a range.
 */
class NumericFunction : SynchronousClientFunction() {

    override val name: String
        get() = "numeric"
    override val description: String
        get() = "Checks numeric range constraints."
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.BOOLEAN
    override val argumentSchema: Schema
        get() = S.obj(
            properties = mapOf("value" to S.number(), "min" to S.number(), "max" to S.number()),
        )

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any {
        val value = args["value"]
        if (value !is Number) return false

        if (args.containsKey("min")) {
            val min = args["min"].toInteger()
            if (value.toInt() < min) return false
        }
        if (args.containsKey("max")) {
            val max = args["max"].toInteger()
            if (value.toInt() > max) return false
        }
        return true
    }
}


/**
 * Checks if a string is a valid email.
 */
class EmailFunction : SynchronousClientFunction() {

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any {
        val value = args["value"]
        if (value !is String) return false
        val emailRegex = Regex("^[^@]+@[^@]+\\.[^@]+\$")
        return emailRegex.matches(value)
    }

    override val name: String
        get() = "email"
    override val description: String
        get() = "Checks that the value is a valid email address."
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.BOOLEAN
    override val argumentSchema: Schema
        get() = S.obj(properties = mapOf("value" to S.string()))
}

/**
 * Opens a URL.
 */
class OpenUrlFunction : SynchronousClientFunction() {

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any {
        val urlStr = args["url"]
        if (urlStr !is String) return false
        val uri = urlStr.toUri()
        //val uriHandler = LocalUriHandler.current
        //uriHandler.openUri("https://example.com")
        canLaunchUrl(uri).then((can) {
            if (can) launchUrl(uri)
        })
        return true
    }

    override val name: String
        get() = "openUrl"
    override val description: String
        get() = "Opens the specified URL in a browser or handler. This function has no return value."
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.EMPTY
    override val argumentSchema: Schema
        get() = S.obj(properties = mapOf("url" to S.string()))
}

/**
 * Formats a number.
 */
class FormatNumberFunction : SynchronousClientFunction() {


    override val name: String
        get() = "formatNumber"
    override val description: String
        get() = "Formats a number with the specified grouping and decimal precision."
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.STRING
    override val argumentSchema: Schema
        get() = S.obj(
            properties = mapOf(
                "value" to S.number(),
                "decimalPlaces" to S.integer(),
                "useGrouping" to S.boolean(),
            ),
        )

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any {
        val number = args["value"]
        if (number !is Number) return number?.toString() ?: ""

        var decimalPlaces: Int? = null
        if (args["decimalPlaces"] is Number) {
            decimalPlaces = (args["decimalPlaces"] as Number).toInt()
        }

        var useGrouping = true
        if (args["useGrouping"] is Boolean) {
            useGrouping = args["useGrouping"] as Boolean
        }
        val formatter = NumberFormat.getNumberInstance()
        formatter.isGroupingUsed = useGrouping
        if (decimalPlaces != null) {
            formatter.minimumFractionDigits = decimalPlaces
            formatter.maximumFractionDigits = decimalPlaces
        }

        return formatter.format(number)
    }
}

/**
 * Formats a currency value.
 */
class FormatCurrencyFunction : SynchronousClientFunction() {


    override fun executeSync(args: JsonMap, context: ExecutionContext): Any {
        val amount = args["value"]
        val currencyCode = args["currencyCode"]
        if (amount !is Number || currencyCode !is String) {
            return amount?.toString() ?: ""
        }
        val formatter = NumberFormat.getCurrencyInstance()
        formatter.currency = Currency.getInstance(currencyCode)
        return formatter.format(amount)
    }

    override val name: String
        get() = "formatCurrency"
    override val description: String
        get() = "Formats a number as a currency string."
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.STRING
    override val argumentSchema: Schema
        get() = S.obj(properties = mapOf("value" to S.number(), "currencyCode" to S.string()))
}

/**
 * Formats a date.
 */
class FormatDateFunction : SynchronousClientFunction() {
    private fun parseIsoDate(value: String): Date? {
        return try {
            DateFormat.getDateInstance().parse(value)
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(value)
            } catch (e2: Exception) {
                null
            }
        }
    }
    override fun executeSync(args: JsonMap, context: ExecutionContext): Any? {
        val dateVal = args["value"]
        val pattern = args["pattern"] as? String ?: return dateVal?.toString()
        val date: Date? = when (dateVal) {
            is String -> {
                parseIsoDate(dateVal)
            }
            is Int -> Date(dateVal.toLong())
            is Long -> Date(dateVal)
            is Number -> Date(dateVal.toLong())
            else -> null
        }

        if (date == null) return dateVal?.toString()

        return try {
            date.format(pattern)
        } catch (e: Exception) {
            date.toString()
        }
//        var date: LocalDateTime? = null
//        if (dateVal is String) {
//            date = LocalDateTime.parse(dateVal)
//        } else if (dateVal is Long) {
//            date = LocalDateTime.ofEpochSecond(dateVal, 0, ZoneOffset.UTC)
//        }
//
//        if (date == null || pattern !is String) return dateVal?.toString()
//
//        try {
//            //pattern
//            return date.format(DateTimeFormatter.ofPattern(pattern))
//        } catch (e: Exception) {
//            return date.toString()
//        }
    }

    override val name: String
        get() = "formatDate"
    override val description: String
        get() = "Formats a timestamp into a string using a pattern."
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.STRING
    override val argumentSchema: Schema
        get() = S.obj(
            properties = mapOf(
                "value" to S.any(), // String or int (millis)
                "pattern" to S.string(),
            ),
        )
}

/**
 * Pluralizes a word based on a count.
 */
class PluralizeFunction : SynchronousClientFunction() {

    override fun executeSync(args: JsonMap, context: ExecutionContext): Any? {
        val count = args["count"]
        if (count !is Number) return ""

        if (count == 0 && args.containsKey("zero")) return args["zero"]
        if (count == 1 && args.containsKey("one")) return args["one"]
        return args["other"] ?: ""
    }

    override val name: String
        get() = "pluralize"
    override val description: String
        get() = "Returns a localized string based on the Common Locale Data Repository " +
                "(CLDR) plural category of the count (zero, one, two, few, many, other). " +
                "Requires an 'other' fallback. For English, just use 'one' and 'other'."
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.STRING
    override val argumentSchema: Schema
        get() = S.obj(
            properties = mapOf(
                "count" to S.number(),
                "zero" to S.string(),
                "one" to S.string(),
                "other" to S.string(),
            ),
        )
}
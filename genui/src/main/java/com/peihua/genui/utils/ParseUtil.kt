package com.peihua.genui.utils

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes

/**
 * 将Object对象转成boolean类型
 *
 * @param [this]
 * @return 如果[this]不能转成boolean，则默认false
 */
fun Any?.toBoolean(): Boolean {
    return toBoolean(this, false)
}

/**
 * 将Object对象转成boolean类型
 *
 * @param [this]
 * @return 如果[this]不能转成boolean，则默认false
 */
fun Any?.toBoolean(defaultValue: Boolean = false): Boolean {
    return toBoolean(this, defaultValue)
}

/**
 * 将Object对象转成boolean类型
 *
 * @param value
 * @return 如果[this]不能转成boolean，则默认defaultValue
 */
fun Any?.toBoolean(value: Any?, defaultValue: Boolean = false): Boolean {
    if (value is Boolean) {
        return value
    } else if (value is String) {
        return "true".equals(value as String?, ignoreCase = true)
    }
    return defaultValue
}

/**
 * 将Object对象转成Double类型
 *
 * @return 如果[this]不能转成Double，则默认0.00
 */
fun Any?.toDouble(): Double {
    return toDouble(0.00)
}

/**
 * 将Object对象转成Double类型
 *
 * @param [this]
 * @return 如果[this]不能转成Double，则默认0.00
 */
fun Any?.toDouble(defaultValue: Double = 0.00): Double {
    when (this) {
        is Double -> {
            return this
        }

        is Number -> {
            return this.toDouble()
        }

        is String -> {
            try {
                return this.toDoubleOrNull() ?: defaultValue
            } catch (ignored: Exception) {
            }
        }

        else -> {
            try {
                return this.toString().toDoubleOrNull() ?: defaultValue
            } catch (ignored: NumberFormatException) {
            }
        }
    }
    return defaultValue
}

/**
 * 将Object对象转成Float类型
 *
 * @return 如果[this]不能转成Float，则默认0.00
 */
fun Any?.toFloat(): Float {
    return toFloat(0.00f)
}

/**
 * 将Object对象转成Float类型
 *
 * @param [this]
 * @return 如果[this]不能转成Float，则默认0.00
 */
fun Any?.toFloat(defaultValue: Float = 0.00f): Float {
    when (this) {
        is Double -> {
            return this as Float
        }

        is Number -> {
            return this.toFloat()
        }

        is String -> {
            try {
                return this.toFloatOrNull() ?: defaultValue
            } catch (ignored: Exception) {
            }
        }

        else -> {
            try {
                return this.toString().toFloatOrNull() ?: defaultValue
            } catch (ignored: NumberFormatException) {
            }
        }
    }
    return defaultValue
}

/**
 * 将Object对象转成Integer类型
 *
 * @param [this]
 * @return 如果[this]不能转成Integer，则默认0
 */
fun Any?.toInteger(): Int {
    return toInteger(0)
}

/**
 * 将Object对象转成Integer类型
 *
 * @param [this]
 * @return 如果[this]不能转成Integer，则默认0
 */
fun Any?.toInteger(defaultValue: Int = 0): Int {
    when (this) {
        is Int -> {
            return this
        }

        is Number -> {
            return this.toInt()
        }

        is String -> {
            try {
                return this.toIntOrNull() ?: defaultValue
            } catch (ignored: Exception) {
            }
        }

        else -> {
            try {
                return this.toString().toIntOrNull() ?: defaultValue
            } catch (ignored: NumberFormatException) {
            }
        }
    }
    return defaultValue
}

/**
 * 将Object对象转成Long类型
 *
 * @param [this]
 * @return 如果[this]不能转成Long，则默认0
 */
fun Any?.toLong(): Long {
    return toLong(0L)
}

/**
 * 将Object对象转成Long类型
 *
 * @param [this]
 * @return 如果[this]不能转成Long，则默认0
 */
fun Any?.toLong(defaultValue: Long = 0L): Long {
    when (this) {
        is Long -> {
            return this
        }

        is Number -> {
            return this.toLong()
        }

        is String -> {
            try {
                return this.toLongOrNull() ?: defaultValue
            } catch (ignored: NumberFormatException) {
            }
        }

        else -> {
            try {
                return this.toString().toLongOrNull() ?: defaultValue
            } catch (ignored: NumberFormatException) {
            }
        }
    }
    return defaultValue
}


/**
 * 将Object对象转成String类型
 *
 * @return 如果[this]不能转成String，则默认""
 */
fun Any?.toString(): String = toString("")

/**
 * 将Object对象转成String类型
 *
 * @param [this]
 * @return 如果[this]不能转成String，则默认""
 */
fun Any?.toString(defaultValue: String = ""): String {
    return when {
        this is String -> {
            this
        }

        this is EditText -> {
            text.toString()
        }

        this is TextView -> {
            text.toString()
        }

        this != null -> {
            this.toString()
        }

        else -> defaultValue
    }
}

/**
 * 将Object对象转成String类型
 *
 * @param [this]
 * @return 如果[this]不能转成String，则默认""
 */
fun Any?.toString(context: Context?, value: Any?, @StringRes resId: Int): String {
    return value.toString(context?.getString(resId) ?: "")
}

/**
 * 将Object对象转成String类型
 *
 * @param [this]
 * @return 如果[this]不能转成String，则默认""
 */
fun Any?.toString(context: Context?, @StringRes resId: Int): String {
    return toString(context?.getString(resId) ?: "")
}


/**
 * 将数字格式化为指定小数位数的字符串
 *
 * @param fractionDigits 小数位数,默认为0
 * @return 格式化后的字符串
 *
 * 示例:
 * 3.14159.toStringAsFixed(2) -> "3.14"
 * 3.0.toStringAsFixed(0) -> "3"
 * 3.1.toStringAsFixed(0) -> "3"
 */
fun Number?.toStringAsFixed(fractionDigits: Int = 0): String {
    if (this == null) {
        return ""
    }
    return when (fractionDigits) {
        0 -> this.toInt().toString()
        else -> String.format("%.${fractionDigits}f", this.toDouble())
    }
}


fun Any?.toNumberOrNull(): Number? {
    return when (this) {
        is Number -> {
            this
        }

        is String -> {
            this.toDoubleOrNull()
        }

        else -> null
    }
}

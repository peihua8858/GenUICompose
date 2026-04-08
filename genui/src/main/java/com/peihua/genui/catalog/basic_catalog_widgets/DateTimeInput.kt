package com.peihua.genui.catalog.basic_catalog_widgets

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.model.DataContext
import com.peihua.genui.model.DataPath
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.utils.ValidationHelper
import com.peihua.genui.widgets.BoundString
import com.peihua.json.schema.S
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.collections.List
data class DateTimeInputData(private val jsonMap: JsonMap) {
    val value: Any = jsonMap["value"] as Any
    val variant: String? = jsonMap["variant"] as String?
    val label: Any? = jsonMap["label"]
    val checks: List<JsonMap>? = (jsonMap["checks"] as? List<*>)?.mapNotNull { it as? JsonMap }
    val enableDate: Boolean
        get() {
            val v = variant
            if (v == null) {
                if (jsonMap.containsKey("enableDate")) return jsonMap["enableDate"] as Boolean;
                return true;
            }
            return v == "date" || v == "datetime";
        }
    val enableTime: Boolean
        get() {
            val v = variant
            if (v == null) {
                if (jsonMap.containsKey("enableTime")) return jsonMap["enableTime"] as Boolean
                return true;
            }
            return v == "time" || v == "datetime";
        }
    val firstDate: LocalDate =
        parseDate((jsonMap["min"] as String?) ?: "") ?: LocalDate.of(-999999999, 1, 1)
    val lastDate: LocalDate =
        parseDate((jsonMap["max"] as String?) ?: "") ?: LocalDate.of(999999999, 12, 31)

    companion object {
        fun fromMap(json: JsonMap): DateTimeInputData = DateTimeInputData(json)

        fun create(
            value: JsonMap,
            variant: String? = null,
            min: String? = null,
            max: String? = null,
            label: Any? = null,
            checks: List<JsonMap>? = null,
            enableDate: Boolean? = null,
            enableTime: Boolean? = null,
        ): DateTimeInputData {
            return DateTimeInputData(
                buildMap {
                    put("value", value)
                    put("variant", variant)
                    put("min", min)
                    put("max", max)
                    put("label", label)
                    put("checks", checks)
                    if (enableDate != null) put("enableDate", enableDate)
                    if (enableTime != null) put("enableTime", enableTime)
                }
            )
        }
    }

}

private fun parseDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(value).toLocalDate()
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(value).toLocalDate()
            } catch (_: DateTimeParseException) {
                try {
                    Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate()
                } catch (_: DateTimeParseException) {
                    null
                }
            }
        }
    }
}

object DateTimeInput {
    val _schema = S.obj(
        description = "A widget for selecting a date and/or time.",
        properties = mapOf(
            "value" to A2uiSchemas.stringReference(
                description = "The selected date and/or time.",
            ),
            "variant" to S.string(
                description = "The input type: date, time, or datetime.",
                enumValues = listOf("date", "time", "datetime"),
            ),
            "min" to S.string(
                description = "The earliest selectable date (YYYY-MM-DD). Defaults to -9999-01-01.",
            ),
            "max" to S.string(
                description = "The latest selectable date (YYYY-MM-DD). Defaults to 9999-12-31.",
            ),
            "label" to A2uiSchemas.stringReference(
                description = "The text label for the input field.",
            ),
            "checks" to S.list(items = A2uiSchemas.validationCheck()),
        ),
        required = listOf("value"),
    );

    /**
     * A widget for selecting a date and/or time.
     *
     * This widget displays a field that, when tapped, opens the native date and/or
     * time pickers. The selected value is stored as a string in the data model
     * path specified by the `value` parameter.
     *
     * ## Parameters:
     *
     * - `value`: The selected date and/or time, as a string.
     * - `enableDate`: Whether to allow the user to select a date. Defaults to
     *   `true`.
     * - `enableTime`: Whether to allow the user to select a time. Defaults to
     *   `true`.
     * - `min`: The minimum allowed date.
     * - `max`: The maximum allowed date.
     * - `label`: The label text.
     * - `checks`: Validation checks.
     */
    val dateTimeInput = CatalogItem(
        name = "DateTimeInput",
        dataSchema = _schema,
        widgetBuilder = { itemContext ->
            val dateTimeInputData = DateTimeInputData.fromMap(itemContext.data as JsonMap)
            val valueRef = dateTimeInputData.value;
            val path = if (valueRef is Map<*, *> && valueRef.containsKey("path"))
                valueRef["path"] as String
            else "${itemContext.id}.value"
            BoundString(
                itemContext.dataContext,
                value = mapOf("path" to path),
                builder = { value ->
                    var effectiveValue = value;
                    if (effectiveValue == null) {
                        val dateValue = dateTimeInputData.value;
                        if (dateValue !is Map<*, *> || !dateValue.containsKey("path")) {
                            effectiveValue = dateValue as String?
                        }
                    }
                    BoundString(
                        dataContext = itemContext.dataContext,
                        value = dateTimeInputData.label,
                        builder = { label ->
                            DateTimeInputWidget(
                                id = itemContext.id,
                                value = effectiveValue,
                                path = path,
                                data = dateTimeInputData,
                                dataContext = itemContext.dataContext,
                                onChanged = {},
                                label = label,
                                checks = dateTimeInputData.checks,
                            )
                        })
                }
            )
        },
        exampleData = listOf(
            {
                """
                [
                    {
                        "id": "root",
                        "component": "DateTimeInput",
                        "value": {
                            "path": "/myDateTime"
                        },
                         "variant": "datetime"
                    }
                ]
                """.trimIndent()
            },
            {
                """
                [
                    {
                        "id": "root",
                        "component": "DateTimeInput",
                        "value": {
                            "path": "/myDate"
                        },
                        "enableTime": "false"
                    }
                ]
                """.trimIndent()
            },
            {
                """
                [
                    {
                        "id": "root",
                        "component": "DateTimeInput",
                        "value": {
                            "path": "/myTime"
                        },
                         "enableDate": "false"
                    }
                ]
                """.trimIndent()
            }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeInputWidget(
    modifier: Modifier = Modifier,
    id: String,
    value: String?,
    path: String,
    data: DateTimeInputData,
    dataContext: DataContext,
    onChanged: () -> Unit,
    label: String? = null,
    checks: List<JsonMap>? = null,
) {
    var errorText by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(value, checks, dataContext) {
        if (checks.isNullOrEmpty()) {
            errorText = null
        } else {
            ValidationHelper.validateFlow(checks, dataContext).collectLatest { newError ->
                errorText = newError
            }
        }
    }

    val parsedInitial = remember(value) {
        parseInputDateTime(value) ?: LocalDateTime.now()
    }

    var pendingDate by remember(value) {
        mutableStateOf(parsedInitial.toLocalDate())
    }
    var pendingTime by remember(value) {
        mutableStateOf(parsedInitial.toLocalTime().truncatedTo(ChronoUnit.MINUTES))
    }

    var showDateDialog by remember { mutableStateOf(false) }

    val displayText = remember(value, data.enableDate, data.enableTime) {
        getDisplayText(
            value = value,
            enableDate = data.enableDate,
            enableTime = data.enableTime
        )
    }

    if (showDateDialog) {
        val zoneId = ZoneId.systemDefault()
        val initialMillis = pendingDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val minMillis = data.firstDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val maxMillis = data.lastDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis in minMillis..maxMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = {
                showDateDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(zoneId)
                                .toLocalDate()

                            pendingDate = selectedDate
                            showDateDialog = false

                            if (data.enableTime) {
                                showTimePickerDialog(
                                    context = context,
                                    initialTime = pendingTime,
                                    onTimePicked = { pickedTime ->
                                        pendingTime = pickedTime
                                        val formattedValue = formatResultValue(
                                            date = pendingDate,
                                            time = pendingTime,
                                            enableDate = data.enableDate,
                                            enableTime = data.enableTime
                                        )
                                        dataContext.update(DataPath(path), formattedValue)
                                        onChanged()
                                    }
                                )
                            } else {
                                val formattedValue = formatResultValue(
                                    date = pendingDate,
                                    time = pendingTime,
                                    enableDate = data.enableDate,
                                    enableTime = data.enableTime
                                )
                                dataContext.update(DataPath(path), formattedValue)
                                onChanged()
                            }
                        } else {
                            showDateDialog = false
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDateDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                when {
                    data.enableDate -> {
                        showDateDialog = true
                    }

                    data.enableTime -> {
                        showTimePickerDialog(
                            context = context,
                            initialTime = pendingTime,
                            onTimePicked = { pickedTime ->
                                pendingTime = pickedTime
                                val formattedValue = formatResultValue(
                                    date = pendingDate,
                                    time = pendingTime,
                                    enableDate = data.enableDate,
                                    enableTime = data.enableTime
                                )
                                dataContext.update(DataPath(path), formattedValue)
                                onChanged()
                            }
                        )
                    }
                }
            }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = {
                if (label != null) {
                    Text(label)
                }
            },
            supportingText = {
                if (errorText != null) {
                    Text(errorText!!)
                }
            },
            isError = errorText != null,
            readOnly = true,
            enabled = false,
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true
        )
    }
}

private fun parseInputDateTime(value: String?): LocalDateTime? {
    if (value.isNullOrBlank()) return null

    return tryParseLocalDateTime(value)
        ?: tryParseLocalTime(value)?.let { LocalDate.of(1970, 1, 1).atTime(it) }
}

private fun tryParseLocalDateTime(value: String): LocalDateTime? {
    return try {
        LocalDateTime.parse(value)
    } catch (_: DateTimeParseException) {
        try {
            OffsetDateTime.parse(value).toLocalDateTime()
        } catch (_: DateTimeParseException) {
            try {
                Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime()
            } catch (_: DateTimeParseException) {
                try {
                    LocalDate.parse(value).atStartOfDay()
                } catch (_: DateTimeParseException) {
                    null
                }
            }
        }
    }
}

private fun tryParseLocalTime(value: String): LocalTime? {
    return try {
        LocalTime.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun formatResultValue(
    date: LocalDate,
    time: LocalTime,
    enableDate: Boolean,
    enableTime: Boolean,
): String {
    return when {
        enableDate && !enableTime -> {
            date.toString()
        }

        !enableDate && enableTime -> {
            "%02d:%02d:00".format(time.hour, time.minute)
        }

        else -> {
            LocalDateTime.of(
                date,
                time.withSecond(0).withNano(0)
            ).toString()
        }
    }
}

private fun getDisplayText(
    value: String?,
    enableDate: Boolean,
    enableTime: Boolean,
): String {
    if (value == null) {
        return getPlaceholderText(enableDate, enableTime)
    }

    val dateTime = parseInputDateTime(value) ?: return value
    val parts = mutableListOf<String>()

    if (enableDate) {
        val dateFormatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.FULL)
            .withLocale(Locale.getDefault())
        parts += dateTime.toLocalDate().format(dateFormatter)
    }

    if (enableTime) {
        val timeFormatter = DateTimeFormatter
            .ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
        parts += dateTime.toLocalTime().format(timeFormatter)
    }

    return parts.joinToString(" ")
}

private fun getPlaceholderText(
    enableDate: Boolean,
    enableTime: Boolean,
): String {
    return when {
        enableDate && enableTime -> "Select a date and time"
        enableDate -> "Select a date"
        enableTime -> "Select a time"
        else -> "Select a date/time"
    }
}

private fun showTimePickerDialog(
    context: Context,
    initialTime: LocalTime,
    onTimePicked: (LocalTime) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimePicked(LocalTime.of(hourOfDay, minute))
        },
        initialTime.hour,
        initialTime.minute,
        true
    ).show()
}
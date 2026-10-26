package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

private val calendarYears = 1900..2100

/** UTC is only the calendar's date encoding; business today is supplied by the owner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectDateInput(
    value: String,
    onValueChange: (String) -> Unit,
    businessToday: LocalDate,
    label: String,
    tagPrefix: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    labelOutside: Boolean = false,
) {
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var calendarMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val parsed = try { LocalDate.parse(value) } catch (_: DateTimeParseException) { null }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
        if (labelOutside) Text(label, style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = value, onValueChange = { calendarMessage = null; onValueChange(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("$tagPrefix-input")
                .semantics { if (labelOutside) contentDescription = label; error?.let { semanticsError(it) } },
            label = if (labelOutside) null else ({ Text(label) }), enabled = enabled, singleLine = true,
            isError = error != null,
            supportingText = { Text(error ?: "年-月-日，例如2026-01-01", style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        )
        OutlinedButton(
            onClick = {
                if (parsed != null && parsed.year !in calendarYears) {
                    calendarMessage = "该日期超出日历范围，请直接修改日期文字"
                } else if (parsed == null && businessToday.year !in calendarYears) {
                    calendarMessage = "该日期超出日历范围，请直接修改日期文字"
                } else {
                    calendarMessage = null
                    showCalendar = true
                }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$tagPrefix-picker"),
        ) { Text("选择$label", style = MaterialTheme.typography.bodyLarge) }
        calendarMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (showCalendar && enabled) {
        val initial = parsed ?: businessToday
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = calendarYears,
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        onValueChange(Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    showCalendar = false
                }, enabled = picker.selectedDateMillis != null,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("date-picker-confirm")) {
                    Text("确定日期", style = MaterialTheme.typography.bodyLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("date-picker-cancel")) {
                    Text("取消", style = MaterialTheme.typography.bodyLarge)
                }
            },
        ) {
            DatePicker(picker, Modifier.testTag("date-picker"),
                title = { Text(label, Modifier.padding(LithicSpacing.medium), style = MaterialTheme.typography.bodyLarge) })
        }
    }
}

@Composable
internal fun ProjectCategoryPicker(
    categories: List<CategoryEntity>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
) {
    var showCategories by rememberSaveable { mutableStateOf(false) }
    val options = categories.filter { it.type == CategoryType.PROJECT }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
        OutlinedButton(onClick = { showCategories = true }, enabled = enabled && options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-category")
                .semantics { error?.let { semanticsError(it) } }) {
            Column(Modifier.fillMaxWidth()) {
                Text("类别", style = MaterialTheme.typography.bodyMedium)
                Text(options.firstOrNull { it.id == selectedId }?.name ?: "请选择类别",
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
    if (showCategories && enabled) {
        AlertDialog(onDismissRequest = { showCategories = false },
            modifier = Modifier.testTag("category-picker"),
            title = { Text("选择类别") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { category ->
                        TextButton(onClick = { onSelect(category.id); showCategories = false },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("category-${category.id}")) {
                            Text(category.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategories = false }, Modifier.heightIn(min = 48.dp)) { Text("取消") }
            },
        )
    }
}

@Composable
internal fun ProjectNoteInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    enabled: Boolean,
    error: String? = null,
    labelOutside: Boolean = false,
) {
    val field: @Composable () -> Unit = {
        OutlinedTextField(value, onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag(tag)
                .semantics { if (labelOutside) contentDescription = label; error?.let { semanticsError(it) } },
            label = if (labelOutside) null else ({ Text(label) }), enabled = enabled, minLines = 2, isError = error != null,
            supportingText = if (error == null) null else ({ Text(error, style = MaterialTheme.typography.bodyMedium) }),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default))
    }
    if (labelOutside) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        field()
    } else field()
}

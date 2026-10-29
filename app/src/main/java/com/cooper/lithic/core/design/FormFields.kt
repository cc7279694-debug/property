package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import com.cooper.lithic.core.design.LithicOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "名称",
    error: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    labelOutside: Boolean = false,
) {
    ControlledTextField(value, onValueChange, modifier, label, error, enabled,
        KeyboardOptions(capitalization = KeyboardCapitalization.Words, keyboardType = KeyboardType.Text, imeAction = imeAction), onImeAction, labelOutside)
}

@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "金额（元）",
    error: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    labelOutside: Boolean = false,
) {
    ControlledTextField(value, onValueChange, modifier, label, error, enabled,
        KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction), onImeAction, labelOutside)
}

@Composable
fun DateField(
    value: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "日期",
    error: String? = null,
    enabled: Boolean = true,
) {
    Column {
        OutlinedButton(onClick = onClick,
            modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                error?.let { semanticsError(it) }
            }, enabled = enabled) {
            Column(Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(value?.toString() ?: "选择日期", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (error != null) {
            Text(error, Modifier.padding(horizontal = LithicSpacing.medium),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ControlledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    label: String,
    error: String?,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    onImeAction: (() -> Unit)?,
    labelOutside: Boolean,
) {
    val field: @Composable () -> Unit = {
        OutlinedTextField(value = value, onValueChange = onValueChange,
            colors = lithicInputColors(),
            modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).semantics {
                if (labelOutside) contentDescription = label
                error?.let { semanticsError(it) }
            }, label = if (labelOutside) null else ({ Text(label) }), enabled = enabled, isError = error != null,
            supportingText = if (error == null) null else ({ Text(error, style = MaterialTheme.typography.bodyMedium) }),
            textStyle = MaterialTheme.typography.bodyLarge, singleLine = true,
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions(onImeAction))
    }
    if (labelOutside) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        field()
    } else field()
}

@Composable
internal fun lithicInputColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledBorderColor = MaterialTheme.colorScheme.outline,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

private fun keyboardActions(onImeAction: (() -> Unit)?): KeyboardActions =
    if (onImeAction == null) KeyboardActions.Default else KeyboardActions(
        onDone = { onImeAction() }, onGo = { onImeAction() }, onNext = { onImeAction() },
        onPrevious = { onImeAction() }, onSearch = { onImeAction() }, onSend = { onImeAction() },
    )

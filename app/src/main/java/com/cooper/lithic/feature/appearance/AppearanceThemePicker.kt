package com.cooper.lithic.feature.appearance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.*

internal data class AppearanceActions(
    val state: AppearanceUiState, val select: (LithicAppearance) -> Unit, val retryRead: () -> Unit,
)
internal val LocalAppearanceActions = staticCompositionLocalOf<AppearanceActions?> { null }

/** A local presentation choice, never a ledger operation or a navigation destination. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceThemePicker() {
    val actions = LocalAppearanceActions.current ?: return
    val state = actions.state
    var expanded by rememberSaveable { mutableStateOf(false) }
    C2SettingRow("外观与主题", C2Glyph.Settings, Modifier.testTag("appearance-open"),
        trailing = if (state.loading) "正在读取外观" else if (!state.readable) "外观尚未确认" else state.selected.label,
        onClick = { expanded = true })
    state.error?.let { UiFeedbackText(it, UiMessageTone.ERROR, "appearance-error") }
    if (expanded) ModalBottomSheet(onDismissRequest = { expanded = false },
        containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.testTag("appearance-sheet")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = LithicSpacing.page).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("外观与主题", style = MaterialTheme.typography.headlineMedium)
            if (state.saving) Text("正在保存外观…", Modifier.testTag("appearance-saving"),
                style = MaterialTheme.typography.bodyMedium)
            LithicAppearance.entries.forEach { theme ->
                Row(Modifier.fillMaxWidth().heightIn(min = 80.dp)
                    .selectable(selected = state.readable && state.selected == theme, enabled = !state.loading,
                        role = Role.RadioButton, onClick = { actions.select(theme) })
                    .testTag("appearance-${theme.storageId}").semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppearanceSwatch(theme)
                    Text(theme.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    RadioButton(selected = state.readable && state.selected == theme, onClick = null,
                        modifier = Modifier.clearAndSetSemantics {})
                }
            }
            state.error?.let { UiFeedbackText(it, UiMessageTone.ERROR, "appearance-sheet-error") }
            if (!state.readable && !state.loading) TextButton(actions.retryRead, Modifier.heightIn(min = 48.dp)
                .testTag("appearance-retry")) { Text("重新读取外观") }
            Text("仅保存在本机，不随完整备份导出或恢复。", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The same abstract layout for all choices; no sample business record or commercial image. */
@Composable
private fun AppearanceSwatch(theme: LithicAppearance) {
    val colors = lithicColorScheme(theme)
    Canvas(Modifier.size(96.dp, 62.dp).clearAndSetSemantics {}) {
        drawRoundRect(colors.background, cornerRadius = CornerRadius(8.dp.toPx()))
        drawRoundRect(colors.onSurface, Offset(size.width * .12f, size.height * .18f),
            Size(size.width * .5f, size.height * .08f), CornerRadius(2.dp.toPx()))
        drawRoundRect(colors.onSurfaceVariant, Offset(size.width * .12f, size.height * .38f),
            Size(size.width * .7f, size.height * .04f), CornerRadius(2.dp.toPx()))
        drawRoundRect(colors.surfaceContainer, Offset(size.width * .12f, size.height * .56f),
            Size(size.width * .5f, size.height * .24f), CornerRadius(4.dp.toPx()))
        drawCircle(colors.primary, size.height * .10f, Offset(size.width * .8f, size.height * .70f))
    }
}

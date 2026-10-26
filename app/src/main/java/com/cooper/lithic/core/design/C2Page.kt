package com.cooper.lithic.core.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Presentation only: callbacks still enter the original navigation and write guards. */
@Composable
internal fun C2PageHeader(title: String, onBack: () -> Unit, backTag: String,
    modifier: Modifier = Modifier, backLabel: String = "返回", subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (LocalDensity.current.fontScale > 1.3f) {
            TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag(backTag)) {
                C2Icon(C2Glyph.Back); Spacer(Modifier.width(8.dp)); Text(backLabel)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
        } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag(backTag)) {
                C2Icon(C2Glyph.Back); Spacer(Modifier.width(8.dp)); Text(backLabel)
            }
            Spacer(Modifier.weight(1f))
            actions()
        }
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun C2StatusChip(label: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun C2Section(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

/** Read-only rows have no chevron or click semantics. Action eligibility belongs to the caller. */
@Composable
internal fun C2SettingRow(title: String, icon: C2Glyph, modifier: Modifier = Modifier, subtitle: String? = null,
    trailing: String? = null, enabled: Boolean = true, onClick: (() -> Unit)? = null, trailingTag: String? = null) {
    Row(modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick))
        .heightIn(min = 56.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { C2Icon(icon) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            trailing?.let { Text(it, if (trailingTag == null) Modifier else Modifier.testTag(trailingTag), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (onClick != null) C2Icon(C2Glyph.Chevron)
    }
}

/** The footer consumes real measured height; scrolling content can never extend under it. */
@Composable
internal fun C2FormScaffold(title: String, onBack: () -> Unit, backTag: String,
    modifier: Modifier = Modifier, backLabel: String = "返回",
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().imePadding()) {
        C2PageHeader(title, onBack, backTag, Modifier.padding(horizontal = 16.dp), backLabel)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = footer)
    }
}

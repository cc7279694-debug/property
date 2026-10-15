package com.cooper.lithic.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicIcon

/** Opening or dismissing this menu has no business effect. Route guards belong to its callbacks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddRecordSheet(onDismiss: () -> Unit, onCreateProject: () -> Unit, onCreateSubscription: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.testTag("add-record-sheet")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("新增记录", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                IconButton(onDismiss, Modifier.testTag("add-record-dismiss")) {
                    LithicIcon(LithicIcon.Close, "关闭新增菜单")
                }
            }
            AddOption(LithicIcon.Project, "长期项目", "记录买断物品、永久会员和长期投入", "home-create-project") {
                onDismiss()
                onCreateProject()
            }
            AddOption(LithicIcon.Subscription, "订阅服务", "记录月付、季付和年付的真实付款", "home-create-subscription") {
                onDismiss()
                onCreateSubscription()
            }
        }
    }
}

@Composable
private fun AddOption(icon: LithicIcon, title: String, description: String, tag: String, onClick: () -> Unit) {
    Surface(onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag(tag),
        color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LithicIcon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LithicIcon(LithicIcon.ChevronRight, null)
        }
    }
}

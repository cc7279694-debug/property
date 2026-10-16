package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Presentation only: callers supply the original guarded back callback. */
@Composable
fun FormHeader(title: String, onBack: () -> Unit, backTag: String, backLabel: String = "返回") {
    Row(Modifier.fillMaxWidth().testTag("$backTag-header"),
        verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onBack, Modifier.size(48.dp).testTag(backTag)) {
            LithicIcon(LithicIcon.Back, backLabel)
        }
        Text(title, Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.headlineMedium, softWrap = true)
    }
}

@Composable
fun lithicFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
)

package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Ordinary explanations only. Errors, uncertainty and destructive consequences stay inline. */
@Composable
internal fun C5InformationButton(
    title: String,
    modifier: Modifier = Modifier,
    buttonLabel: String = "说明",
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    TextButton({ open = true }, modifier.heightIn(min = 48.dp).semantics { contentDescription = title }) {
        C2Icon(C2Glyph.Info)
        Spacer(Modifier.width(4.dp))
        Text(buttonLabel, style = MaterialTheme.typography.bodyMedium)
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        },
        confirmButton = { TextButton({ open = false }, Modifier.heightIn(min = 48.dp)) { Text("知道了") } })
}

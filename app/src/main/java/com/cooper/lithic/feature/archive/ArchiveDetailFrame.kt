package com.cooper.lithic.feature.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.LithicTextButton

internal val ArchivePageInset = 16.dp
internal val ArchiveContentWidth = 560.dp

/** The header, scrolling body and fixed action share the same bounded column. */
@Composable
internal fun ArchiveDetailFrame(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = ArchiveContentWidth).fillMaxSize(), content = content)
    }
}

@Composable
internal fun ArchiveDetailDisclosure(label: String, expanded: Boolean, tag: String, onToggle: () -> Unit) {
    LithicTextButton(onToggle, Modifier.fillMaxWidth().testTag(tag).semantics {
        stateDescription = if (expanded) "已展开" else "已收起"
    }) {
        Text(label, Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        C2Icon(C2Glyph.Chevron, Modifier.rotate(if (expanded) -90f else 90f))
    }
}

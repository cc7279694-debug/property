package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** Presentation of the supplied category only; never infer a brand or item type from its name. */
internal fun c2IdentityGlyph(category: String): C2Glyph? = when (category.trim()) {
    "电子设备", "数码", "电脑" -> C2Glyph.Computer
    "书籍", "阅读", "学习" -> C2Glyph.Book
    "音乐", "影音", "娱乐" -> C2Glyph.Audio
    "健身", "运动", "健康" -> C2Glyph.Wellness
    else -> null
}

internal fun c2IdentityInitial(name: String): String? = name.trim().takeIf { it.isNotEmpty() }
    ?.let { it.substring(0, it.offsetByCodePoints(0, 1)) }

/** No files, media loader, storage owner or cache: the enclosing row supplies its full accessible name. */
@Composable
internal fun C2RecordIdentity(name: String, category: String, kind: C2Glyph, modifier: Modifier = Modifier) {
    val glyph = c2IdentityGlyph(category)
    val initial = c2IdentityInitial(name)
    Surface(modifier.size(64.dp).clearAndSetSemantics {}, shape = MaterialTheme.shapes.small,
        color = if (kind == C2Glyph.Subscription) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            if (glyph != null) C2Icon(glyph)
            else if (initial != null) Text(initial, style = MaterialTheme.typography.headlineMedium)
            else C2Icon(kind)
        }
    }
}

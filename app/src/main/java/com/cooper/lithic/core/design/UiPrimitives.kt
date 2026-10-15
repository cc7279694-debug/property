package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cooper.lithic.R

/** Original project vectors: simple geometric strokes, without third-party logo assets. */
enum class LithicIcon(val drawableRes: Int) {
    Now(R.drawable.ic_ui_now),
    Review(R.drawable.ic_ui_review),
    Settings(R.drawable.ic_ui_settings),
    Add(R.drawable.ic_ui_add),
    Project(R.drawable.ic_ui_project),
    Subscription(R.drawable.ic_ui_subscription),
    Back(R.drawable.ic_ui_back),
    More(R.drawable.ic_ui_more),
    ChevronRight(R.drawable.ic_ui_chevron_right),
    Image(R.drawable.ic_ui_image),
    ImageError(R.drawable.ic_ui_image_error),
    Close(R.drawable.ic_ui_close),
}

@Composable
fun LithicIcon(
    icon: LithicIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Icon(painterResource(icon.drawableRes), contentDescription, modifier.size(24.dp), tint = tint)
}

@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
            Text(title, Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.headlineMedium, softWrap = true)
            actions?.invoke(this)
        }
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = true)
        }
    }
}

@Composable
fun StatusBadge(label: String, modifier: Modifier = Modifier, subdued: Boolean = false) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp),
        color = if (subdued) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (subdued) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium, softWrap = true)
    }
}

/** Shared surface only; callers retain their own business wording and actions. */
@Composable
fun LithicCard(
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier.fillMaxWidth(),
        shape = if (emphasis) MaterialTheme.shapes.large else MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.fillMaxWidth().padding(LithicSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.small), content = content)
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: LithicIcon = LithicIcon.Project,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = LithicSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                LithicIcon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, softWrap = true)
        Text(message, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, softWrap = true)
        if (actionLabel != null && onAction != null) {
            Button(onAction, Modifier.heightIn(min = 48.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.bodyLarge, softWrap = true)
            }
        }
    }
}

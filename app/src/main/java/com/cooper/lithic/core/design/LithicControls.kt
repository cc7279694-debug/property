package com.cooper.lithic.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Existing Material behavior, with explicit readable inactive colors and a 48dp target. */
@Composable
internal fun LithicButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(), border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit) =
    Button(onClick, modifier.heightIn(min = 48.dp), enabled, shape, colors, elevation, border,
        contentPadding, interactionSource, content)

@Composable
internal fun LithicOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    elevation: ButtonElevation? = null, border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit) =
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape, colors, elevation, border,
        contentPadding, interactionSource, content)

@Composable
internal fun LithicTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.textButtonColors(disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    elevation: ButtonElevation? = null, border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding, interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit) =
    TextButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape, colors, elevation, border,
        contentPadding, interactionSource, content)

@Composable
internal fun LithicIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) =
    IconButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
        colors = colors, interactionSource = interactionSource, content = content)


@Composable
internal fun lithicCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = MaterialTheme.colorScheme.primary,
    uncheckedColor = MaterialTheme.colorScheme.outline,
    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
    disabledCheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledUncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledIndeterminateColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

package com.cooper.lithic.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.backup.RestorePreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pure presentation fixtures; this does not perform a storage restore or open an Owner. */
@RunWith(AndroidJUnit4::class)
class P03RestorePreviewSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ten_table_preview_shows_both_sides_and_explicit_settings_overwrite_scope() {
        compose.setContent { LithicTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            RestorePreviewScreen(preview(), onConfirm = {}, onDiscard = {})
        } } }
        compose.onNodeWithText("记录数量对照：当前账本 → 备份内容").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("restore-count-notification_receipts").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("当前 4 · 备份 11").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("restore-count-milestone_watermarks").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("当前 7 · 备份 8").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("完整覆盖当前记录，不合并。当前记录、图片和设置将被备份内容替换。").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("restore-overwrite-scope").performScrollTo().assertIsDisplayed()
    }

    @Test fun a_busy_preview_does_not_offer_a_second_confirmation_or_discard() {
        var confirmations = 0
        var discards = 0
        compose.setContent { LithicTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            RestorePreviewScreen(preview(), onConfirm = { confirmations += 1 }, onDiscard = { discards += 1 }, busy = true)
        } } }
        compose.onNodeWithTag("confirm-backup-restore").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("discard-backup-restore").performScrollTo().assertIsNotEnabled()
        assertEquals(0, confirmations)
        assertEquals(0, discards)
    }

    @Test fun viewing_a_preview_never_confirms_until_the_explicit_action() {
        var confirmations = 0
        var discards = 0
        compose.setContent { LithicTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            RestorePreviewScreen(preview(), onConfirm = { confirmations += 1 }, onDiscard = { discards += 1 })
        } } }
        assertEquals(0, confirmations)
        assertEquals(0, discards)
        compose.onNodeWithTag("confirm-backup-restore").performScrollTo().performClick()
        assertEquals(1, confirmations)
        assertEquals(0, discards)
    }

    @Test fun local360dp_large_text_retains_all_ten_pairs_unknown_counts_and_overwrite_scope() {
        val original = preview()
        val keys = original.backupCounts.keys.toList()
        val current = keys.mapIndexed { index, key -> key to index.toLong() }.toMap() - "local_media_assets"
        val backup = keys.mapIndexed { index, key -> key to (index + 20L) }.toMap() +
            ("long_term_projects" to Long.MAX_VALUE)
        val candidate = original.copy(currentCounts = current, backupCounts = backup)
        compose.setContent { LithicTheme {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                Box(Modifier.width(360.dp)) { Column(Modifier.verticalScroll(rememberScrollState())) {
                    RestorePreviewScreen(candidate, onConfirm = {}, onDiscard = {})
                } }
            }
        } }
        keys.forEach { key ->
            compose.onNodeWithTag("restore-count-$key").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("当前 ${current[key] ?: "未知"} · 备份 ${backup[key] ?: "未知"}").assertIsDisplayed()
        }
        compose.onNodeWithTag("restore-count-snapshot-note").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("restore-overwrite-scope").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("confirm-backup-restore").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("discard-backup-restore").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(candidate.token).assertDoesNotExist()
    }

    private fun preview(): RestorePreview {
        val keys = listOf("long_term_projects", "project_money_events", "subscriptions", "subscription_renewals",
            "subscription_state_changes", "categories", "local_media_assets", "replacements",
            "notification_receipts", "milestone_watermarks")
        return RestorePreview("p03-private-preview-token",
            backupCounts = keys.associateWith { 1L } + mapOf("notification_receipts" to 11L, "milestone_watermarks" to 8L),
            currentCounts = keys.associateWith { 0L } + mapOf("notification_receipts" to 4L, "milestone_watermarks" to 7L),
            createdAt = "2026-10-20T00:00:00Z", recordZoneId = "Asia/Shanghai")
    }
}

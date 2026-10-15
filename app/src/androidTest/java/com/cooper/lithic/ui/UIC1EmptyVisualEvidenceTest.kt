package com.cooper.lithic.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.HomePreview
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LithicNavigation
import com.cooper.lithic.app.readHomeOverview
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionRepository
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Actual Android test-host screenshots, never the active ledger or a production fixture. */
class UIC1EmptyVisualEvidenceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun isolated_empty_home_is_real_in_both_themes_and_cancel_add_does_not_write() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.noBackupFilesDir, "ui-c1-empty-visual-${UUID.randomUUID()}")
        val generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        try {
            val clock = FixedBusinessClock(LocalDate.of(2026, 10, 15))
            val projects = ProjectRepository(generation.database, generation.coordinator, clock)
            val subscriptions = SubscriptionRepository(generation.database, generation.coordinator, clock)
            val read = readHomeOverview(projects, subscriptions, clock.today())
            val state = LedgerUiState.Ready(read.projectCount, generation.settings.recordZone(), projects,
                clock, subscriptions, read.subscriptionCount, homePreview = read.preview)
            assertEquals(0L, state.projectCount)
            assertEquals(0L, state.subscriptionCount)
            var dark by mutableStateOf(false)
            compose.setContent { LithicTheme(darkTheme = dark) { Surface { LithicNavigation(state) } } }
            for (theme in listOf("light", "dark")) {
                compose.runOnIdle { dark = theme == "dark" }
                compose.onNodeWithTag("home-first-record").assertIsDisplayed()
                compose.onNodeWithTag("home-add-record").assertDoesNotExist()
                compose.waitForIdle()
                val image = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                val output = File(context.noBackupFilesDir, "ui-c1-visual/empty-home-$theme.png")
                check(output.parentFile!!.mkdirs() || output.parentFile!!.isDirectory)
                output.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                compose.onNodeWithTag("home-first-record").performClick()
                compose.onNodeWithTag("add-record-sheet").assertIsDisplayed()
                compose.onNodeWithTag("add-record-dismiss").performClick()
                compose.onNodeWithTag("add-record-sheet").assertDoesNotExist()
                assertEquals(0L, projects.loadSummary().projectCount)
                assertEquals(0L, subscriptions.loadSubscriptionCount())
            }
        } finally {
            generation.close()
            check(root.deleteRecursively())
        }
    }
}

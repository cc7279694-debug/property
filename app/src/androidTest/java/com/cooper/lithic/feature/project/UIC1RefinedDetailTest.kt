package com.cooper.lithic.feature.project

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.media.CoverBaseline
import com.cooper.lithic.data.media.CoverCommand
import com.cooper.lithic.data.media.M10ImageSourceFixtures
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.media.MediaResult
import com.cooper.lithic.data.media.SyntheticImageSource
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.data.repository.ProjectRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Real isolated facts, real media import and real cost engine; never the active ledger. */
class UIC1RefinedDetailTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val date = LocalDate.of(2026, 10, 15)
    private val clock = FixedBusinessClock(date)
    private val time = Instant.parse("2026-10-15T00:00:00Z")
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var media: MediaRepository
    private var source: SyntheticImageSource? = null

    @Before fun setUp() = runBlocking {
        root = File(context.noBackupFilesDir, "ui-c1-refined-detail-${UUID.randomUUID()}")
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        media = MediaRepository(generation.database, generation.coordinator, generation.images,
            clock, context.contentResolver)
    }

    @After fun tearDown() = runBlocking {
        source?.delete()
        if (::generation.isInitialized) generation.close()
        if (::root.isInitialized) {
            check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            check(root.deleteRecursively())
        }
    }

    @Test fun saved_cover_precedes_cost_and_both_daily_cost_and_days_fit_normal_first_viewport() {
        val detail = runBlocking { detailWithCover() }
        compose.setContent { LithicTheme { ProjectDetailScreen(ProjectDetailUiState(detail, loading = false),
            {}, {}, {}, {}, mediaRepository = media) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("project-cover-image").fetchSemanticsNodes().isNotEmpty() }
        val cover = compose.onNodeWithTag("project-cover-image").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val daily = compose.onNodeWithTag("project-detail-daily").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("The saved image should introduce the item before its current cost", cover.bottom <= daily.top)
        assertTrue("A wide compact cover must not consume the whole first viewport", cover.width / cover.height >= 2f)
        compose.onNodeWithText("已持有10天").assertIsDisplayed()
        assertEquals("An engine result is used instead of hand-written UI math", 10L, detail.cost.heldDays)
        assertEquals(0, detail.cost.dailyRate!!.compareTo(java.math.BigDecimal("100")))
    }

    @Test fun long_return_name_at_200_percent_uses_non_overlapping_48dp_controls_with_full_accessible_names() {
        val detail = runBlocking { detailWithCover() }
        val fullReturn = "返回长期项目列表并保留原始页面来源"
        var backs = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                LithicTheme { Box(Modifier.width(360.dp).fillMaxSize()) {
                    ProjectDetailScreen(ProjectDetailUiState(detail, loading = false), { backs++ }, {}, {}, {},
                        backLabel = fullReturn, mediaRepository = media)
                } }
            }
        }
        val back = compose.onNodeWithTag("project-detail-back").assertIsDisplayed().fetchSemanticsNode()
        val more = compose.onNodeWithTag("project-more").assertIsDisplayed().fetchSemanticsNode()
        assertEquals(listOf(fullReturn), back.config[SemanticsProperties.ContentDescription])
        assertEquals(listOf("更多操作"), more.config[SemanticsProperties.ContentDescription])
        val density = context.resources.displayMetrics.density
        assertTrue(back.boundsInRoot.width >= 48f * density - 1)
        assertTrue(back.boundsInRoot.height >= 48f * density - 1)
        assertTrue(more.boundsInRoot.width >= 48f * density - 1)
        assertTrue(more.boundsInRoot.height >= 48f * density - 1)
        assertTrue(back.boundsInRoot.right <= more.boundsInRoot.left)
        assertTrue("A long spoken return destination must not become a multi-line toolbar title",
            back.boundsInRoot.height <= 64f * density)
        compose.onNodeWithTag("project-detail-back").performClick()
        assertEquals(1, backs)
    }

    @Test fun large_text_more_menu_keeps_all_actions_readable_and_money_facts_remain_reachable() {
        val detail = runBlocking { detailWithCover() }
        var ending = 0
        var deletion = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                LithicTheme { Box(Modifier.width(360.dp).fillMaxSize()) {
                    ProjectDetailScreen(ProjectDetailUiState(detail, loading = false), {}, {}, {}, {},
                        onEnd = { ending++ }, onDelete = { deletion++ }, mediaRepository = media)
                } }
            }
        }
        compose.onNodeWithTag("project-more").performClick()
        val diagnosticDirectory = File(context.noBackupFilesDir, "ui-c1-refined-diagnostics")
        check(diagnosticDirectory.mkdirs() || diagnosticDirectory.isDirectory)
        val diagnosticId = "menu-layout-${System.nanoTime()}"
        val diagnostic = File(diagnosticDirectory, "$diagnosticId.txt")
        diagnostic.writeText("fontScale=2.0; hostWidth=360dp; displayDensity=${context.resources.displayMetrics.density}\n")
        for (name in listOf("编辑项目资料", "管理封面", "结束项目", "删除项目")) {
            val node = compose.onNodeWithText(name, useUnmergedTree = true).assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val tag = when (name) {
                "编辑项目资料" -> "project-edit"
                "管理封面" -> "project-cover-edit"
                "结束项目" -> "project-end"
                else -> "project-delete"
            }
            val labelBounds = node.fetchSemanticsNode().boundsInRoot
            val itemBounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            diagnostic.appendText("label=$name; labelBounds=$labelBounds; itemBounds=$itemBounds\n")
            layouts.forEach { layout ->
                diagnostic.appendText("size=${layout.size}; overflowWidth=${layout.didOverflowWidth}; " +
                    "overflowHeight=${layout.didOverflowHeight}; hasVisualOverflow=${layout.hasVisualOverflow}; " +
                    "lineCount=${layout.lineCount}; didExceedMaxLines=${layout.multiParagraph.didExceedMaxLines}; " +
                    "constraints=${layout.layoutInput.constraints}; density=${layout.layoutInput.density}; " +
                    "densityValue=${layout.layoutInput.density.density}; fontScale=${layout.layoutInput.density.fontScale}; " +
                    "fontSize=${layout.layoutInput.style.fontSize}; lineHeight=${layout.layoutInput.style.lineHeight}; " +
                    "maxLines=${layout.layoutInput.maxLines}; softWrap=${layout.layoutInput.softWrap}; " +
                    "overflow=${layout.layoutInput.overflow}\n")
                diagnostic.appendText("paragraphWidth=${String.format(java.util.Locale.ROOT, "%.9f", layout.multiParagraph.width)}; " +
                    "paragraphWidthHex=${java.lang.Float.toHexString(layout.multiParagraph.width)}; " +
                    "paragraphHeight=${String.format(java.util.Locale.ROOT, "%.9f", layout.multiParagraph.height)}; " +
                    "paragraphHeightHex=${java.lang.Float.toHexString(layout.multiParagraph.height)}\n")
                repeat(layout.lineCount) { line ->
                    diagnostic.appendText("line[$line]: left=${layout.getLineLeft(line)}; right=${layout.getLineRight(line)}; " +
                        "top=${layout.getLineTop(line)}; bottom=${layout.getLineBottom(line)}; " +
                        "ellipsized=${layout.isLineEllipsized(line)}\n")
                }
            }
            if (layouts.single().hasVisualOverflow) {
                val screenshot = checkNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .uiAutomation.takeScreenshot())
                File(diagnosticDirectory, "$diagnosticId-$tag.png").outputStream().use {
                    check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            }
            val layout = layouts.single()
            val tolerance = 1f // One physical pixel for text-layout rounding, independent of font scale.
            assertEquals("The menu must retain the complete original label", name, layout.layoutInput.text.text)
            assertFalse("The menu must not truncate lines: $name", layout.multiParagraph.didExceedMaxLines)
            assertTrue("The menu label needs at least one visible line: $name", layout.lineCount > 0)
            assertEquals("All label characters must remain present: $name", layout.layoutInput.text.length,
                layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
            repeat(layout.lineCount) { line ->
                assertFalse("A menu line must not ellipsize: $name", layout.isLineEllipsized(line))
                assertTrue("Menu line left edge must remain visible: $name", layout.getLineLeft(line) >= -tolerance)
                assertTrue("Menu line right edge must remain visible: $name", layout.getLineRight(line) <= layout.size.width + tolerance)
                assertTrue("Menu line top edge must remain visible: $name", layout.getLineTop(line) >= -tolerance)
                assertTrue("Menu line bottom edge must remain visible: $name", layout.getLineBottom(line) <= layout.size.height + tolerance)
            }
            for (offset in name.indices) {
                val character = layout.getBoundingBox(offset)
                assertTrue("Every menu character must fit the visible text frame: $name[$offset]",
                    character.left >= -tolerance && character.top >= -tolerance &&
                        character.right <= layout.size.width + tolerance && character.bottom <= layout.size.height + tolerance)
            }
            assertTrue("The menu must not clip the text frame: $name",
                labelBounds.width + tolerance >= layout.size.width && labelBounds.height + tolerance >= layout.size.height)
            assertTrue("The complete label must remain inside its action row: $name",
                labelBounds.left >= itemBounds.left - tolerance && labelBounds.top >= itemBounds.top - tolerance &&
                    labelBounds.right <= itemBounds.right + tolerance && labelBounds.bottom <= itemBounds.bottom + tolerance)
        }
        assertEquals(0, ending)
        assertEquals(0, deletion)
        // Opening the menu is read-only. Dismiss without accidentally executing an action.
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        compose.waitForIdle()
        for (tag in listOf("project-detail-outlay", "project-detail-recovery", "project-detail-net", "project-detail-note")) {
            compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).assertIsDisplayed()
        }
        assertEquals(detail, runBlocking { ProjectRepository(generation.database, generation.coordinator, clock)
            .loadProjectDetail(detail.project.id) })
    }

    private suspend fun detailWithCover(): ProjectDetail {
        val category = generation.database.categoryDao().list().single { it.type == CategoryType.PROJECT }
        val project = LongTermProjectEntity(uuid(), "随身相机", category.id, date.minusDays(9),
            note = "记录每一次旅途", createdAt = time, updatedAt = time)
        val event = ProjectMoneyEventEntity(uuid(), project.id, MoneyDirection.OUTLAY, 100_000,
            project.startedOn, "真实测试投入", ProjectMoneyEventRole.INITIAL, uuid())
        generation.database.projectDao().insertProject(project)
        generation.database.projectDao().insertEvent(event)
        val bitmap = Bitmap.createBitmap(600, 360, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xff657f72.toInt())
        val bytes = ByteArrayOutputStream().also { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray()
        bitmap.recycle()
        source = M10ImageSourceFixtures.source(bytes)
        val prepared = success(media.prepareImport(source!!.uri, uuid()))
        success(media.commit(CoverCommand(uuid(), CoverBaseline.Project(project, listOf(event)), prepared.asset, time.plusSeconds(1))))
        return checkNotNull(ProjectRepository(generation.database, generation.coordinator, clock).loadProjectDetail(project.id))
    }

    private fun uuid() = UUID.randomUUID().toString()
    private fun <T> success(result: MediaResult<T>): T {
        assertTrue("Expected real import success: $result", result is MediaResult.Success)
        return (result as MediaResult.Success<T>).value
    }
}

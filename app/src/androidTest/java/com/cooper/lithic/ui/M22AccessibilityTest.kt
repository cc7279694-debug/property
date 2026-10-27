package com.cooper.lithic.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.core.design.EntryCard
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.data.backup.RestorePreview
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.feature.project.ProjectDetailScreen
import com.cooper.lithic.feature.project.ProjectDetailUiState
import com.cooper.lithic.feature.project.ProjectEditorScreen
import com.cooper.lithic.feature.project.ProjectEditorUiState
import com.cooper.lithic.feature.project.ProjectListScreen
import com.cooper.lithic.feature.project.ProjectListUiState
import com.cooper.lithic.feature.settings.BackupRestoreUiState
import com.cooper.lithic.feature.settings.NotificationPermissionState
import com.cooper.lithic.feature.settings.NotificationSettingsContent
import com.cooper.lithic.feature.settings.NotificationSettingsUiState
import com.cooper.lithic.feature.settings.RestoreUiPhase
import com.cooper.lithic.feature.settings.SettingsShell
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real production composables in the plain-Application isolated host. No ledger or system tasks. */
@RunWith(AndroidJUnit4::class)
class M22AccessibilityTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun isolatedHostOnly() {
        assertEquals(Application::class.java, ApplicationProvider.getApplicationContext<Context>().applicationContext.javaClass)
    }

    @Test fun narrow_large_font_project_header_scrolls_with_records_and_keeps_both_touch_dimensions() {
        val record = detail()
        var opened: String? = null
        compose.setContent { LocalNarrowHost {
            ProjectListScreen(ProjectListUiState(listOf(record), 1, false), {}, {}, { opened = it }, {}, {})
        } }
        compose.onNodeWithTag("project-list-back").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("project-list-create").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("project-list-content").performScrollToNode(hasTestTag("project-row-$PROJECT_ID"))
        compose.onNodeWithTag("project-row-$PROJECT_ID").assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        // A fixed header formerly consumed the narrow 200% viewport instead of scrolling away.
        compose.onNodeWithTag("project-list-back").assertIsNotDisplayed()
        capture("01-project-list-local360-200", local200 = true)
        compose.onNodeWithTag("project-row-$PROJECT_ID").performClick()
        assertEquals(PROJECT_ID, opened)
    }

    @Test fun failed_project_list_retains_records_with_an_explicit_stale_result_notice() {
        compose.setContent { LithicTheme {
            ProjectListScreen(ProjectListUiState(listOf(detail()), 1, false, error = "读取失败"), {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("project-list-error").assertTextContains("读取失败")
        compose.onNodeWithTag("project-list-stale").assertTextEquals("暂时无法刷新，以下为上次读取记录。")
        compose.onNodeWithText("暂无长期项目").assertDoesNotExist()
        compose.onNodeWithTag("project-list-content").performScrollToNode(hasTestTag("project-row-$PROJECT_ID"))
        compose.onNodeWithTag("project-row-$PROJECT_ID").assertIsDisplayed()
    }

    @Test fun failed_project_detail_explains_its_retained_result_without_claiming_fresh_data() {
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail(), false, error = "读取失败"), {}, {}, {}, {})
        } }
        compose.onNodeWithTag("project-detail-stale").assertTextEquals("暂时无法刷新，以下为上次读取记录。")
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-name"))
        compose.onNodeWithTag("project-detail-name").assertTextContains(PROJECT_NAME)
        compose.onNodeWithText("项目不存在，返回列表查看当前记录。").assertDoesNotExist()
    }

    @Test fun negative_subcent_and_large_amounts_keep_their_complete_value_in_light_and_dark() {
        var dark by mutableStateOf(false)
        compose.setContent { LocalNarrowHost(dark = dark) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                EntryCard(PROJECT_NAME, "已结束", "结果截至今天；持有天数不再增加。", BigDecimal("-0.0005"))
                AmountText(BigDecimal("-92233720368547758.07"), Modifier.testTag("m22-large-amount"))
            }
        } }
        compose.onNodeWithText(PROJECT_NAME, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        assertTextFits(PROJECT_NAME)
        compose.onNodeWithText("−<¥0.01/天", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("m22-large-amount").performScrollTo()
            .assertContentDescriptionEquals("负92233720368547758.07元/天")
        assertTextFits("−¥92233720368547758.07/天")
        capture("02-negative-amount-light-local360-200", local200 = true)
        compose.runOnIdle { dark = true }
        compose.onNodeWithTag("m22-large-amount").assertIsDisplayed()
        assertTextFits("−¥92233720368547758.07/天")
        capture("03-negative-amount-dark-local360-200", local200 = true)
    }

    @Test fun real_project_form_keeps_input_errors_keyboard_save_and_back_reachable_at_local200() {
        var state by mutableStateOf(editor())
        val saves = AtomicInteger()
        val backs = AtomicInteger()
        compose.setContent { LocalNarrowHost { Editor(state, { state = it }, { saves.incrementAndGet() }, { backs.incrementAndGet() }) } }
        compose.onNodeWithTag("project-name").performScrollTo().performClick().performTextInput("保留长名称")
        compose.onNodeWithTag("project-initial-amount").performScrollTo().assertTextContains("1.234")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, AMOUNT_ERROR))
        compose.onNodeWithText(AMOUNT_ERROR, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        assertTextFits(AMOUNT_ERROR)
        val decor = compose.activity.window.decorView
        val beforeIme = checkNotNull(ViewCompat.getRootWindowInsets(decor)).isVisible(WindowInsetsCompat.Type.ime())
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("M22_IME_BEFORE_HIDE", "visible=$beforeIme;windowFocus=${decor.hasWindowFocus()};attached=${decor.isAttachedToWindow};lifecycle=${compose.activity.lifecycle.currentState}")
        })
        compose.runOnUiThread { WindowCompat.getInsetsController(compose.activity.window, decor).hide(WindowInsetsCompat.Type.ime()) }
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(decor)?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("M22_IME_AFTER_HIDE", "visible=${checkNotNull(ViewCompat.getRootWindowInsets(decor)).isVisible(WindowInsetsCompat.Type.ime())};windowFocus=${decor.hasWindowFocus()}")
        })
        compose.onNodeWithTag("project-save").performScrollTo().assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, saves.get())
        compose.onNodeWithTag("project-form-error").performScrollTo().assertIsDisplayed()
        capture("04-project-form-local360-200", local200 = true)
        compose.onNodeWithTag("project-editor-back").performScrollTo().assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, backs.get())
        assertEquals("保留长名称", state.name)
        assertEquals("1.234", state.initialAmount)
    }

    /** Run separately only after the authorized supervisor sets the actual system font scale. */
    @Test fun system200_project_form_uses_the_real_configuration_without_a_font_override() {
        val configuration = compose.activity.resources.configuration
        assertTrue("Requires the authorized system font_scale=2.0 phase", configuration.fontScale >= 1.95f)
        var state by mutableStateOf(editor())
        compose.setContent { LithicTheme { Surface(Modifier.fillMaxSize().testTag("m22-capture-root")) {
            Editor(state, { state = it }, {}, {})
        } } }
        compose.onNodeWithTag("project-name").performScrollTo().performTextInput("系统大字体合成项目")
        compose.onNodeWithTag("project-save").performScrollTo().assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        capture("05-project-form-system200", local200 = false)
        compose.onNodeWithTag("project-editor-back").performScrollTo().assertIsDisplayed()
        assertEquals("系统大字体合成项目", state.name)
    }

    /** System settings and a real IME are prerequisites; no LocalDensity override or Room writes. */
    @Test fun system200_real_ime_keeps_production_form_input_and_save_reachable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = compose.activity.resources.configuration
        assertEquals("Requires the authorized actual system font_scale=2.0",
            2f, Settings.System.getFloat(context.contentResolver, Settings.System.FONT_SCALE), 0.001f)
        assertEquals(2f, configuration.fontScale, 0.001f)
        assertTrue("Requires the authorized 360dp system display phase",
            configuration.screenWidthDp in 359..361)
        var state by mutableStateOf(editor().copy(initialAmount = "100", errors = emptyMap(), message = null))
        val saves = AtomicInteger()
        var saved: ProjectEditorUiState? = null
        compose.setContent { LithicTheme { Surface(Modifier.fillMaxSize()) {
            Editor(state, { state = it }, { saved = state; saves.incrementAndGet() }, {})
        } } }
        val decor = compose.activity.window.decorView
        compose.waitUntil(5_000) { decor.hasWindowFocus() && decor.isAttachedToWindow }
        compose.onNodeWithTag("project-name").performScrollTo().performClick().assertIsFocused()
            .performTextInput("真实系统字号与键盘合成项目")
        compose.runOnUiThread {
            WindowCompat.getInsetsController(compose.activity.window, decor).show(WindowInsetsCompat.Type.ime())
        }
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(decor)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNodeWithTag("project-name").assertIsDisplayed().assertIsFocused()
            .assertTextContains("真实系统字号与键盘合成项目")
        captureSystemImeFrame("08-system200-ime-visible-input", expectedVisible = true)
        compose.onNodeWithTag("project-save").performScrollTo().assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        assertTrue("IME must still be visible when the real save action is reachable",
            checkNotNull(ViewCompat.getRootWindowInsets(decor)).isVisible(WindowInsetsCompat.Type.ime()))
        captureSystemImeFrame("09-system200-ime-visible-save", expectedVisible = true)
        compose.onNodeWithTag("project-save").performClick()
        compose.runOnIdle {
            assertEquals(1, saves.get())
            assertEquals("真实系统字号与键盘合成项目", saved?.name)
            assertEquals("100", saved?.initialAmount)
        }
        compose.runOnUiThread {
            WindowCompat.getInsetsController(compose.activity.window, decor).hide(WindowInsetsCompat.Type.ime())
        }
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(decor)?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
        compose.onNodeWithTag("project-name").performScrollTo().assertIsDisplayed()
            .assertTextContains("真实系统字号与键盘合成项目")
        compose.onNodeWithTag("project-initial-amount").performScrollTo().assertTextContains("100")
        captureSystemImeFrame("10-system200-ime-hidden-input-retained", expectedVisible = false)
        compose.onNodeWithTag("project-editor-back").performScrollTo().assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    @Test fun denied_notification_settings_keep_labeled_switches_and_nonoverlapping_full_size_actions() {
        var state by mutableStateOf(NotificationSettingsUiState(false, ReminderPreferences(true, false)))
        compose.setContent { LocalNarrowHost {
            NotificationSettingsContent(state, NotificationPermissionState(true, false, false),
                { state = state.copy(preferences = state.preferences!!.copy(renewalEnabled = it)) },
                { state = state.copy(preferences = state.preferences!!.copy(milestoneEnabled = it)) }, {}, {}, {})
        } }
        compose.onNodeWithTag("renewal-reminder-switch").performScrollTo().assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(false, state.preferences!!.renewalEnabled)
        compose.onNodeWithTag("milestone-reminder-switch").performScrollTo().assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(true, state.preferences!!.milestoneEnabled)
        compose.onNodeWithTag("notification-permission-status").performScrollTo().assertTextEquals("系统通知未授权")
        compose.onNodeWithTag("request-notification-permission").performScrollTo().assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        val request = compose.onNodeWithTag("request-notification-permission").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("open-system-notification-settings").performScrollTo().assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        val settings = compose.onNodeWithTag("open-system-notification-settings").fetchSemanticsNode().boundsInRoot
        // Re-read both bounds in the same viewport; scroll positions above may differ.
        val requestNow = compose.onNodeWithTag("request-notification-permission").fetchSemanticsNode().boundsInRoot
        assertTrue("Adjacent actions must not overlap", requestNow.bottom <= settings.top || requestNow.top >= settings.bottom)
        assertTrue(request.width > 0)
        capture("06-reminder-denied-local360-200", local200 = true)
    }

    @Test fun restore_preview_at_large_font_preserves_explicit_confirmation_and_independent_cancel() {
        val confirms = AtomicInteger()
        val discards = AtomicInteger()
        val preview = RestorePreview("synthetic-validated-token", mapOf("long_term_projects" to 2L),
            mapOf("long_term_projects" to 1L), "2026-10-23T00:00:00Z", "Asia/Shanghai")
        compose.setContent { LocalNarrowHost {
            SettingsShell(ZoneId.of("Asia/Shanghai"), onExportBackup = {}, onRestoreBackup = {},
                restoreState = BackupRestoreUiState(RestoreUiPhase.PREVIEW, preview),
                onConfirmRestore = { confirms.incrementAndGet() }, onDiscardRestore = { discards.incrementAndGet() })
        } }
        compose.onNodeWithTag("confirm-backup-restore").performScrollTo().assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        assertEquals(0, confirms.get())
        compose.onNodeWithTag("discard-backup-restore").performScrollTo().assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        capture("07-restore-preview-local360-200", local200 = true)
        compose.onNodeWithTag("discard-backup-restore").performClick()
        assertEquals(1, discards.get())
        assertEquals(0, confirms.get())
    }

    @Composable private fun Editor(state: ProjectEditorUiState, change: (ProjectEditorUiState) -> Unit,
        save: () -> Unit, back: () -> Unit) {
        ProjectEditorScreen(state, back, { change(state.copy(name = it)) }, { change(state.copy(startedOn = it)) },
            { change(state.copy(initialAmount = it)) }, { change(state.copy(initialPaidOn = it)) },
            { change(state.copy(categoryId = it)) }, { change(state.copy(note = it)) }, save, {})
    }

    @Composable private fun LocalNarrowHost(dark: Boolean = false, content: @Composable () -> Unit) {
        val density = LocalDensity.current.density
        CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
            LithicTheme(darkTheme = dark) { Surface(Modifier.fillMaxSize()) {
                Box(Modifier.width(360.dp).height(560.dp).testTag("m22-capture-root")) { content() }
            } }
        }
    }

    private fun editor() = ProjectEditorUiState(projectId = PROJECT_ID, startedOn = "2026-10-01", initialAmount = "1.234",
        initialPaidOn = "2026-10-01", categoryId = FALLBACK.id, categories = listOf(FALLBACK),
        today = LocalDate.of(2026, 10, 23), errors = mapOf("initialAmount" to AMOUNT_ERROR),
        message = "尚未保存；输入已保留，请核对金额。")

    private fun detail(): ProjectDetail {
        val day = LocalDate.of(2026, 10, 23)
        val start = day.minusDays(99)
        val instant = Instant.parse("2026-10-23T00:00:00Z")
        val event = ProjectMoneyEventEntity(UUID(22, 2).toString(), PROJECT_ID, MoneyDirection.OUTLAY, 12000,
            start, role = ProjectMoneyEventRole.INITIAL, operationId = UUID(22, 3).toString())
        val cost = ProjectCostEngine.evaluate(ProjectSnapshot(LongTermProject(start),
            listOf(ProjectMoneyEvent(event.direction, Money(event.amountCents), event.occurredOn))), day)
        return ProjectDetail(LongTermProjectEntity(PROJECT_ID, PROJECT_NAME, FALLBACK.id, start,
            createdAt = instant, updatedAt = instant), FALLBACK, listOf(event), cost, day)
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse("Complete text must wrap without clipping", layouts.single().hasVisualOverflow)
    }

    private fun capture(name: String, local200: Boolean) {
        val raw = InstrumentationRegistry.getArguments().getString("m22CaptureRun") ?: return
        val run = UUID.fromString(raw).toString()
        check(raw == run)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = context.cacheDir.canonicalFile
        val root = File(cache, "m22-ui-captures-$run").absoluteFile
        check(root.canonicalFile == root && root.parentFile == cache)
        if (!root.exists()) {
            check(root.mkdir())
            val owner = File(root, "owner.txt").apply { check(createNewFile()) }
            FileOutputStream(owner).use { it.write(run.toByteArray()); it.fd.sync() }
        }
        check(File(root, "owner.txt").readText() == run)
        compose.waitForIdle()
        val png = File(root, "$name.png").apply { check(createNewFile()) }
        val bitmap = compose.onNodeWithTag("m22-capture-root").captureToImage().asAndroidBitmap()
        val width = bitmap.width
        val height = bitmap.height
        try { FileOutputStream(png).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)); it.fd.sync() } }
        finally { bitmap.recycle() }
        val digest = MessageDigest.getInstance("SHA-256")
        png.inputStream().use { input -> val buffer = ByteArray(8192); while (true) {
            val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count)
        } }
        val source = JSONObject().put("runId", run).put("host", "ISOLATED_COMPONENT_ACTIVITY")
            .put("activity", compose.activity.javaClass.name).put("package", context.packageName)
            .put("capturedAtUtc", Instant.now().toString())
            .put("mainActivity", false).put("screenshot", png.name).put("api", Build.VERSION.SDK_INT)
            .put("pid", Process.myPid()).put("uid", Process.myUid()).put("widthPx", width).put("heightPx", height)
            .put("bytes", png.length()).put("sha256", digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) })
            .put("localDensityOverride", local200).put("localFontScale", if (local200) 2 else JSONObject.NULL)
            .put("systemFontScale", compose.activity.resources.configuration.fontScale.toDouble())
            .put("systemScreenWidthDp", compose.activity.resources.configuration.screenWidthDp)
            .put("source", "PRODUCTION_COMPOSABLES_SYNTHETIC_STATE_NO_LEDGER")
        val json = File(root, "$name.json").apply { check(createNewFile()) }
        FileOutputStream(json).use { it.write(source.toString(2).toByteArray()); it.fd.sync() }
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("M22_SCREENSHOT", png.absolutePath); putString("M22_SCREENSHOT_SOURCE", json.absolutePath)
        })
    }

    /** Full display bytes include the real keyboard/SystemUI, unlike captureToImage. */
    private fun captureSystemImeFrame(name: String, expectedVisible: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val raw = checkNotNull(InstrumentationRegistry.getArguments().getString("m22CaptureRun"))
        val run = UUID.fromString(raw).toString()
        check(raw == run)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = context.cacheDir.canonicalFile
        val root = File(cache, "m22-system-ime-captures-$run").absoluteFile
        check(root.canonicalFile == root && root.parentFile == cache)
        if (!root.exists()) {
            check(root.mkdir())
            val owner = File(root, "owner.txt").apply { check(createNewFile()) }
            FileOutputStream(owner).use { it.write(run.toByteArray()); it.fd.sync() }
        }
        check(File(root, "owner.txt").readText() == run)
        compose.waitForIdle()
        val decor = compose.activity.window.decorView
        val insets = checkNotNull(ViewCompat.getRootWindowInsets(decor))
        assertEquals(expectedVisible, insets.isVisible(WindowInsetsCompat.Type.ime()))
        assertTrue("A system modal must not have taken activity window focus", decor.hasWindowFocus())
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val width = bitmap.width
        val height = bitmap.height
        val png = File(root, "$name.png").apply { check(createNewFile()) }
        try { FileOutputStream(png).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)); it.fd.sync() } }
        finally { bitmap.recycle() }
        val sha = MessageDigest.getInstance("SHA-256").digest(png.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val source = JSONObject().put("runId", run).put("host", "ISOLATED_COMPONENT_ACTIVITY")
            .put("mainActivity", false).put("source", "UIAUTOMATION_FULL_DISPLAY_PRODUCTION_PROJECT_FORM_SYNTHETIC_STATE_NO_LEDGER")
            .put("savedToRoom", false).put("capturedAtUtc", Instant.now().toString())
            .put("elapsedRealtimeMs", SystemClock.elapsedRealtime()).put("pid", Process.myPid()).put("uid", Process.myUid())
            .put("localDensityOverride", false).put("systemFontScale", compose.activity.resources.configuration.fontScale.toDouble())
            .put("systemScreenWidthDp", compose.activity.resources.configuration.screenWidthDp)
            .put("imeVisible", expectedVisible).put("imeInsetBottomPx", insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
            .put("windowFocus", decor.hasWindowFocus()).put("bytes", png.length()).put("sha256", sha)
            .put("widthPx", width).put("heightPx", height).put("screenshot", png.name)
        val json = File(root, "$name.json").apply { check(createNewFile()) }
        FileOutputStream(json).use { it.write(source.toString(2).toByteArray()); it.fd.sync() }
        instrumentation.sendStatus(0, Bundle().apply {
            putString("M22_SYSTEM_IME_EVIDENCE", json.absolutePath)
            putString("M22_SYSTEM_IME_STATE", "stage=$name;visible=$expectedVisible;sha256=$sha;windowFocus=${decor.hasWindowFocus()}")
        })
    }

    private companion object {
        val PROJECT_ID = UUID(22, 1).toString()
        const val PROJECT_NAME = "长期使用的合成电脑与必要配件完整名称"
        const val AMOUNT_ERROR = "金额最多保留两位小数；其他输入保持不变。"
        val FALLBACK = CategoryEntity(LithicDatabase.PROJECT_FALLBACK_ID, CategoryType.PROJECT, "其他", "其他", true)
    }
}

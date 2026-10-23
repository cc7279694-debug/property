package com.cooper.lithic.feature.reminder

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.repository.MilestoneNoticeRef
import com.cooper.lithic.domain.reminder.MilestoneCandidate
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.domain.reminder.MilestonePlanner
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.emptyFlow
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated Compose host and synthetic resolver only; not real notification delivery. */
@RunWith(AndroidJUnit4::class)
class M21MilestoneSourcesUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val day = LocalDate.of(2026, 10, 22)
    private fun ref(count: Int) = MilestoneNoticeRef("50c79a10-bc1e-4a99-9d7a-314a5e757865", day, "a".repeat(64), count)
    private fun row(number: Int, type: MilestoneOwnerType = MilestoneOwnerType.PROJECT): MilestoneCandidate {
        val id = UUID(0L, number.toLong()).toString()
        return MilestoneCandidate(type, id, "D100", 100, day,
            MilestonePlanner.logicalKey(type, id, "D100"), "合成来源$number", "于${day}达到100日节点")
    }

    @Before fun plainApplicationOnly() {
        assertEquals(Application::class.java, ApplicationProvider.getApplicationContext<Context>().applicationContext.javaClass)
    }

    @Test fun summary_keeps_more_than_50_sources_and_expands_its_display_window() {
        val rows = (1..51).map { row(it) }
        compose.setContent { LithicTheme {
            MilestoneSourcesScreen(ref(51), emptyFlow(), { rows }, false, {}, {}, {})
        } }
        compose.waitUntil { compose.onAllNodes(hasTestTag("milestone-sources-loading")).fetchSemanticsNodes().isEmpty() }
        capture("01-summary-sources")
        compose.onNodeWithTag("milestone-source-${rows.last().logicalKey}").assertDoesNotExist()
        compose.onNodeWithTag("milestone-sources").performScrollToNode(hasText("继续查看"))
        compose.onNode(hasText("继续查看")).performClick()
        compose.onNodeWithTag("milestone-sources").performScrollToNode(hasTestTag("milestone-source-${rows.last().logicalKey}"))
        compose.onNodeWithTag("milestone-source-${rows.last().logicalKey}").assertIsDisplayed()
    }

    @Test fun same_uuid_cross_type_row_revalidates_and_opens_the_exact_type() {
        val rows = listOf(row(1), row(1, MilestoneOwnerType.SUBSCRIPTION))
        val calls = AtomicInteger()
        val selected = AtomicReference<MilestoneCandidate?>()
        compose.setContent { LithicTheme {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                Box(Modifier.width(360.dp).fillMaxHeight()) {
                    MilestoneSourcesScreen(ref(2), emptyFlow(), { calls.incrementAndGet(); rows }, false, {}, selected::set, {})
                }
            }
        } }
        compose.waitUntil { calls.get() >= 1 && compose.onAllNodes(hasTestTag("milestone-sources-loading")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("milestone-sources").performScrollToNode(hasTestTag("milestone-source-${rows.first().logicalKey}"))
        capture("02-typed-sources-local360-200", local200 = true)
        compose.onNodeWithTag("milestone-sources").performScrollToNode(hasTestTag("milestone-source-${rows.last().logicalKey}"))
        capture("02-subscription-local360-200", local200 = true)
        compose.onNodeWithTag("milestone-source-${rows.last().logicalKey}").performClick()
        compose.waitUntil { selected.get() != null }
        assertEquals(MilestoneOwnerType.SUBSCRIPTION, selected.get()?.ownerType)
        assertEquals(rows.last().ownerId, selected.get()?.ownerId)
        assertTrue(calls.get() >= 2)
    }

    @Test fun stale_or_deleted_summary_is_an_explicit_error_and_never_an_empty_success() {
        compose.setContent { LithicTheme {
            MilestoneSourcesScreen(ref(1), emptyFlow(), { error("Old generation or corrected source") }, false, {}, {}, {})
        } }
        compose.waitUntil { compose.onAllNodes(hasTestTag("milestone-sources-error")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("milestone-sources-error").assertIsDisplayed()
        capture("03-stale-source")
    }

    @Test fun single_source_opens_once_after_revalidation_and_does_not_repeat_on_recomposition() {
        val opens = AtomicInteger()
        compose.setContent { LithicTheme {
            var automatic by remember { mutableStateOf(true) }
            MilestoneSourcesScreen(ref(1), emptyFlow(), { listOf(row(1)) }, automatic,
                { automatic = false }, { opens.incrementAndGet() }, {})
        } }
        compose.waitUntil { opens.get() == 1 }
        compose.waitForIdle()
        assertEquals(1, opens.get())
    }

    /** Opt-in evidence only: never opens an owner, starts WorkManager, or sends a notification. */
    private fun capture(name: String, local200: Boolean = false) {
        val rawRun = InstrumentationRegistry.getArguments().getString("m21CaptureRun") ?: return
        val run = UUID.fromString(rawRun).toString()
        check(rawRun == run) { "Screenshot run must be a canonical UUID" }
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        val root = synchronized(CAPTURE_LOCK) {
            val cache = context.cacheDir.canonicalFile
            val expected = File(cache, "m21-captures-$run").absoluteFile
            check(expected.canonicalFile == expected && expected.parentFile == cache)
            captureRoot?.let {
                check(captureRun == run && it == expected && it.isDirectory)
                check(File(it, "owner.txt").readText() == run)
                it
            } ?: expected.also {
                check(!it.exists() && it.mkdir()) { "Screenshot root must be new; never overwrite an earlier run" }
                val owner = File(it, "owner.txt").apply { check(createNewFile()) }
                FileOutputStream(owner).use { output -> output.write(run.toByteArray(Charsets.UTF_8)); output.fd.sync() }
                captureRoot = it
                captureRun = run
            }
        }
        compose.waitForIdle()
        val png = File(root, "$name.png").apply { check(createNewFile()) }
        val bitmap = compose.onNodeWithTag("milestone-sources").captureToImage().asAndroidBitmap()
        val width = bitmap.width
        val height = bitmap.height
        try {
            FileOutputStream(png).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.fd.sync()
            }
        } finally { bitmap.recycle() }
        check(png.length() > 0)
        val digest = MessageDigest.getInstance("SHA-256")
        png.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val source = JSONObject().put("runId", run).put("source", "REAL_ISOLATED_COMPOSE_COMPONENT_ACTIVITY")
            .put("activity", compose.activity.javaClass.name).put("mainActivity", false)
            .put("package", context.packageName).put("api", Build.VERSION.SDK_INT)
            .put("pid", Process.myPid()).put("uid", Process.myUid())
            .put("widthPx", width).put("heightPx", height).put("bytes", png.length())
            .put("sha256", digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) })
            .put("canvasDp", if (local200) 360 else JSONObject.NULL)
            .put("fontScaleSource", if (local200) "LOCAL_DENSITY" else "DEVICE_DEFAULT")
            .put("fontScale", if (local200) 2 else JSONObject.NULL)
            .put("systemFontScaleModified", false).put("resolver", "SYNTHETIC_NO_LEDGER")
            .put("systemNotificationsSent", 0)
        val metadata = File(root, "$name.json").apply { check(createNewFile()) }
        FileOutputStream(metadata).use { output -> output.write(source.toString(2).toByteArray(Charsets.UTF_8)); output.fd.sync() }
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("M21_COMPONENT_ACTIVITY_SCREENSHOT", png.absolutePath)
            putString("M21_COMPONENT_ACTIVITY_SCREENSHOT_SOURCE", metadata.absolutePath)
        })
    }

    private companion object {
        val CAPTURE_LOCK = Any()
        var captureRoot: File? = null
        var captureRun: String? = null
    }
}

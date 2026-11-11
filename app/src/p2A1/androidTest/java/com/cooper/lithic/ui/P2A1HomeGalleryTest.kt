package com.cooper.lithic.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.C2PrimaryNavigationBar
import com.cooper.lithic.app.Home
import com.cooper.lithic.app.M13IsolatedRunner
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.media.CoverOwnerType
import com.cooper.lithic.data.media.CoverReadResult
import com.cooper.lithic.data.media.CoverReader
import com.cooper.lithic.data.media.CoverReference
import com.cooper.lithic.data.media.VerifiedCoverImage
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.feature.home.HomeShell
import com.cooper.lithic.feature.media.ProvideCoverPreviews
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val APP_PACKAGE = "com.cooper.lithic.p2a1.debug"
private const val TEST_PACKAGE = "com.cooper.lithic.p2a1.debug.test"
private const val CANVAS = "p2a1-home-canvas"

private fun assertP2A1Identity() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val target = instrumentation.targetContext
    val test = instrumentation.context
    assertEquals(APP_PACKAGE, target.packageName)
    assertEquals(TEST_PACKAGE, test.packageName)
    assertEquals(M13IsolatedRunner::class.java, instrumentation.javaClass)
    assertEquals(Application::class.java, target.applicationContext.javaClass)
    assertEquals(target.applicationInfo.uid, Process.myUid())
    assertNotEquals(target.applicationInfo.uid, test.applicationInfo.uid)
}

@RunWith(AndroidJUnit4::class)
class P2A1StartupTest {
    @Test fun plainApplicationStartsUnderDedicatedIdentity() {
        assertP2A1Identity()
    }
}

@RunWith(AndroidJUnit4::class)
class P2A1HomeGalleryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun verifyHost() {
        assertP2A1Identity()
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
    }

    @Test fun stoneMist() = capture(LithicAppearance.STONE_MIST)
    @Test fun pureWhite() = capture(LithicAppearance.PURE_WHITE)
    @Test fun charcoal() = capture(LithicAppearance.CHARCOAL)

    @Test fun stoneMistLargeFontSubscription() = capture(LithicAppearance.STONE_MIST, focusSubscription = true)

    private fun capture(theme: LithicAppearance, focusSubscription: Boolean = false) {
        val facts = C2VisualFixtures(populated = true)
        val project = facts.projects.items.single()
        val subscription = facts.subscriptions.items.single()
        val projectAsset = "b1111111-0000-0000-0000-000000000001"
        val subscriptionAsset = "b1111111-0000-0000-0000-000000000002"
        val covers = mapOf(
            CoverReference(CoverOwnerType.PROJECT, project.project.id, projectAsset) to
                syntheticPng(projectAsset, 48, 24),
            CoverReference(CoverOwnerType.SUBSCRIPTION, subscription.subscription.id, subscriptionAsset) to
                syntheticPng(subscriptionAsset, 24, 48),
        )
        val reader = object : CoverReader {
            override val generationId = "p2a1-synthetic"
            override val ownerKey = Any()
            override fun isCurrent() = true
            override suspend fun read(reference: CoverReference): CoverReadResult =
                CoverReadResult.Ready(checkNotNull(covers[reference]))
        }
        val projects = facts.projects.copy(
            items = listOf(project.copy(project = project.project.copy(coverAssetId = projectAsset))),
        )
        val subscriptions = facts.subscriptions.copy(
            items = listOf(subscription.copy(subscription = subscription.subscription.copy(coverAssetId = subscriptionAsset))),
        )

        compose.setContent {
            LithicTheme(theme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize().testTag(CANVAS),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = { C2PrimaryNavigationBar(Home) {} },
                ) { padding ->
                    Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                        ProvideCoverPreviews(reader) {
                            HomeShell(
                                projectCount = facts.overview.projectCount,
                                onProjectList = {},
                                onCreateProject = {},
                                subscriptionCount = facts.overview.subscriptionCount,
                                onSubscriptionList = {},
                                onCreateSubscription = {},
                                overview = facts.overview,
                                onSearch = {},
                                projectPreview = projects,
                                subscriptionPreview = subscriptions,
                                onOpenProject = {},
                                onOpenSubscription = {},
                            )
                        }
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("record-cover-image", useUnmergedTree = true)
                .fetchSemanticsNodes().size == 2
        }
        compose.onNodeWithTag("home-current-cost-summary").assertIsDisplayed()
        compose.onNodeWithTag("tab-now").assertIsSelected()
        val add = compose.onNodeWithTag("home-open-add").assertIsDisplayed()
        if (focusSubscription) {
            check(compose.activity.resources.configuration.fontScale >= 2f)
            val subscriptionList = compose.onNodeWithTag("open-subscription-list").performScrollTo().assertIsDisplayed()
            check(subscriptionList.fetchSemanticsNode().boundsInRoot.bottom <= add.fetchSemanticsNode().boundsInRoot.top)
        }

        val image = compose.onNodeWithTag(CANVAS).captureToImage().asAndroidBitmap()
        check(image.width.toLong() * image.height in 1..5_000_000)
        val activity = compose.activity
        val directory = File(activity.filesDir, "p2a1-capture-" + UUID.randomUUID())
        check(directory.mkdir())
        val png = File(directory, "home.png")
        check(png.createNewFile())
        FileOutputStream(png).use { output ->
            check(image.compress(Bitmap.CompressFormat.PNG, 100, output))
            output.fd.sync()
        }
        val config = activity.resources.configuration
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("p2a1_directory", directory.name)
            putString("p2a1_theme", theme.name)
            putString("p2a1_dimensions", "${image.width}x${image.height}")
            putString("p2a1_config", "${config.screenWidthDp}dp, ${config.densityDpi}dpi, fontScale=${config.fontScale}")
        })
    }

    private fun syntheticPng(assetId: String, width: Int, height: Int): VerifiedCoverImage {
        val pixels = IntArray(width * height) { index ->
            when {
                index % width < width / 6 -> Color.rgb(75, 104, 124)
                index / width < height / 6 -> Color.rgb(193, 154, 97)
                else -> Color.rgb(130, 147, 139)
            }
        }
        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val bytes = try {
            ByteArrayOutputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
        return VerifiedCoverImage(assetId, "image/png", width, height, bytes)
    }
}

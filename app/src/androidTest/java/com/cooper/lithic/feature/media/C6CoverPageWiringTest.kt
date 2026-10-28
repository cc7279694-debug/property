package com.cooper.lithic.feature.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2RecordIdentity
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.media.*
import com.cooper.lithic.feature.home.HomeShell
import com.cooper.lithic.feature.project.*
import com.cooper.lithic.feature.subscription.*
import com.cooper.lithic.ui.C2VisualFixtures
import java.io.IOException
import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production page wiring with immutable synthetic facts: no Room, Owner, files or user media. */
@RunWith(AndroidJUnit4::class)
class C6CoverPageWiringTest {
    @get:Rule val compose = createComposeRule()
    private enum class Page { HOME, PROJECT_LIST, PROJECT_HISTORY, SUBSCRIPTION_LIST, SUBSCRIPTION_HISTORY, PROJECT_DETAIL, SUBSCRIPTION_DETAIL }

    @Test fun eight_cover_slots_keep_exact_type_record_and_asset_identity_and_failure_semantics() {
        val facts = C2VisualFixtures(true)
        val reader = ObservedReader()
        val project = facts.projects.items.single().let { it.copy(project = it.project.copy(coverAssetId = ASSET)) }
        val subscription = facts.subscriptions.items.single().let { it.copy(subscription = it.subscription.copy(coverAssetId = ASSET)) }
        val projectRef = CoverReference(CoverOwnerType.PROJECT, project.project.id, ASSET)
        val subscriptionRef = CoverReference(CoverOwnerType.SUBSCRIPTION, subscription.subscription.id, ASSET)
        var page by mutableStateOf(Page.HOME)
        compose.setContent { Host(reader) { key(page) { PageContent(page, facts, project, subscription) } } }
        Page.entries.forEach { target ->
            if (target != Page.HOME) compose.runOnIdle { reader.requests.clear(); page = target }
            val expected = when (target) {
                Page.HOME -> listOf(projectRef, subscriptionRef)
                Page.PROJECT_LIST, Page.PROJECT_HISTORY, Page.PROJECT_DETAIL -> listOf(projectRef)
                else -> listOf(subscriptionRef)
            }
            compose.waitUntil(5_000) { expected.all { reader.requests.contains(it) } }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithContentDescription("封面暂不可用", substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes().size == expected.size
            }
            compose.waitForIdle()
            assertEquals(expected.toSet(), synchronized(reader.requests) { reader.requests.toSet() })
            compose.onAllNodesWithContentDescription("封面暂不可用", substring = true, useUnmergedTree = true).assertCountEquals(expected.size)
        }
    }

    @Test fun absent_cover_references_keep_decorative_fallback_and_do_not_call_reader() {
        val facts = C2VisualFixtures(true)
        val reader = ObservedReader()
        var page by mutableStateOf(Page.HOME)
        compose.setContent { Host(reader) { key(page) {
            PageContent(page, facts, facts.projects.items.single(), facts.subscriptions.items.single())
        } } }
        Page.entries.forEach { target ->
            compose.runOnIdle { page = target }
            compose.waitForIdle()
            assertEquals(emptyList<CoverReference>(), synchronized(reader.requests) { reader.requests.toList() })
            compose.onAllNodesWithContentDescription("封面暂不可用", substring = true, useUnmergedTree = true).assertCountEquals(0)
        }
    }

    @Composable private fun Host(reader: CoverReader, content: @Composable () -> Unit) {
        val actual = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(actual.density, 1f)) {
            LithicTheme { ProvideCoverPreviews(reader) { Box(Modifier.width(400.dp).fillMaxHeight()) { content() } } }
        }
    }

    @Composable private fun PageContent(page: Page, facts: C2VisualFixtures,
        project: com.cooper.lithic.data.repository.ProjectDetail,
        subscription: com.cooper.lithic.data.repository.SubscriptionDetail) {
        val projects = facts.projects.copy(items = listOf(project))
        val subscriptions = facts.subscriptions.copy(items = listOf(subscription))
        when (page) {
            Page.HOME -> HomeShell(1, {}, {}, 1, {}, {}, overview = facts.overview,
                projectPreview = projects, subscriptionPreview = subscriptions)
            Page.PROJECT_LIST -> ProjectListScreen(projects, {}, {}, {}, {}, {})
            Page.PROJECT_HISTORY -> ProjectHistoryScreen(projects, {}, {}, {}, {})
            Page.SUBSCRIPTION_LIST -> SubscriptionListScreen(subscriptions, {}, {}, {}, {}, {})
            Page.SUBSCRIPTION_HISTORY -> SubscriptionHistoryScreen(subscriptions, {}, {}, {}, {})
            Page.PROJECT_DETAIL -> ProjectDetailScreen(ProjectDetailUiState(project, loading = false), {}, {}, {}, {},
                coverContent = { RecordCover(CoverReference(CoverOwnerType.PROJECT, project.project.id, project.project.coverAssetId), Modifier.fillMaxSize()) {
                    C2RecordIdentity(project.project.name, project.category.name, C2Glyph.Project)
                } })
            Page.SUBSCRIPTION_DETAIL -> SubscriptionDetailScreen(SubscriptionDetailUiState(subscription, loading = false), {}, {}, {}, {},
                coverContent = { RecordCover(CoverReference(CoverOwnerType.SUBSCRIPTION, subscription.subscription.id, subscription.subscription.coverAssetId), Modifier.fillMaxSize()) {
                    C2RecordIdentity(subscription.subscription.name, subscription.category.name, C2Glyph.Subscription)
                } })
        }
    }

    private class ObservedReader : CoverReader {
        override val generationId = "9b000000-0000-0000-0000-000000000001"
        override val ownerKey: Any = Any()
        val requests = Collections.synchronizedList(mutableListOf<CoverReference>())
        override fun isCurrent() = true
        override suspend fun read(reference: CoverReference): CoverReadResult {
            requests.add(reference)
            throw IOException("Synthetic read failure; no media file exists or is opened")
        }
    }
    private companion object { const val ASSET = "9b000000-0000-0000-0000-000000000999" }
}

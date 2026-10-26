package com.cooper.lithic.feature.subscription

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LedgerUiStateHolder
import com.cooper.lithic.app.LithicApp
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.feature.project.ProjectViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Formal Nav3 entry points over the actual Room owner, rather than a synthetic router. */
@RunWith(AndroidJUnit4::class)
class M09SubscriptionNavigationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val fixture = M09SubscriptionRoomFixture()
    private lateinit var holder: LedgerUiStateHolder
    private lateinit var project: ProjectViewModel
    private lateinit var subscription: SubscriptionViewModel

    @Before fun before() = runBlocking { fixture.open() }
    @After fun after() = runBlocking { fixture.close() }

    @Test fun formal_current_detail_opens_pause_confirmation_without_a_database_write() = runBlocking {
        val facts = M09SubscriptionFixtures.facts()
        val id = fixture.seed(facts)
        attachAndRender()
        compose.onNodeWithTag("open-subscription-list").performScrollTo().performClick()
        compose.waitUntil(10_000) { !subscription.state.value.list.loading }
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(hasTestTag("subscription-row-$id"))
        compose.onNodeWithTag("subscription-row-$id").performClick()
        compose.waitUntil(10_000) { subscription.state.value.detail.detail?.subscription?.id == id }
        compose.onNodeWithTag("subscription-detail-open-management").performClick()
        compose.onNodeWithTag("subscription-detail-pause").assertIsEnabled().performClick()
        compose.onNodeWithTag("subscription-state-screen").assertIsDisplayed()
        assertEquals(facts.renewals, fixture.repository.loadSubscriptionDetail(id)!!.renewals)
        assertEquals(emptyList<Any>(), fixture.generation.database.subscriptionDao().statesForSubscriptions(listOf(id)))
    }

    @Test fun formal_review_history_opens_original_stopped_identity_with_reopen_entry() = runBlocking {
        val facts = M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.STOPPED)
        val id = fixture.seed(facts)
        attachAndRender()
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("review-subscription-history").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("subscription-history-screen").assertIsDisplayed()
        compose.onNodeWithTag("subscription-history-content").performScrollToNode(hasTestTag("subscription-history-row-$id"))
        compose.onNodeWithTag("subscription-history-row-$id").performClick()
        compose.waitUntil(10_000) { subscription.state.value.detail.detail?.subscription?.id == id }
        compose.onNodeWithTag("subscription-detail-open-management").performClick()
        compose.onNodeWithTag("subscription-detail-reopen").assertIsEnabled()
        assertEquals(id, subscription.state.value.detail.detail!!.subscription.id)
        assertEquals(1L, fixture.generation.database.subscriptionDao().countSubscriptions())
    }

    private suspend fun attachAndRender() {
        val storage = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(fixture.generation))
        fixture.onMain {
            holder = LedgerUiStateHolder(storage) { fixture.clock }; fixture.retain("holder", holder)
        }
        val ready = withTimeout(10_000) { holder.state.filterIsInstance<LedgerUiState.Ready>().first() }
        fixture.onMain {
            project = ProjectViewModel(ready.repository, SavedStateHandle(), fixture.clock); fixture.retain("project", project)
        }
        subscription = fixture.model()
        compose.setContent { LithicTheme {
            val ledger by holder.state.collectAsState()
            LithicApp(ledger, {}, project, subscriptionViewModel = subscription)
        } }
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }
}

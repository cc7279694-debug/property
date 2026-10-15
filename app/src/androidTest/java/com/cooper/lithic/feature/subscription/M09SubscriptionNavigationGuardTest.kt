package com.cooper.lithic.feature.subscription

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LedgerUiStateHolder
import com.cooper.lithic.app.LithicApp
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.ui.openHomeCreate
import com.cooper.lithic.data.repository.SubscriptionFacts
import com.cooper.lithic.feature.project.ProjectViewModel
import com.cooper.lithic.feature.project.SubmissionPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Hidden restored lifecycle forms must receive the same global protection as M08 payments. */
@RunWith(AndroidJUnit4::class)
class M09SubscriptionNavigationGuardTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val fixture = M09SubscriptionRoomFixture()
    private lateinit var holder: LedgerUiStateHolder
    private lateinit var project: ProjectViewModel
    private lateinit var subscription: SubscriptionViewModel
    private lateinit var saved: SavedStateHandle

    @Before fun before() = runBlocking { fixture.open() }
    @After fun after() = runBlocking { fixture.close() }

    @Test fun hidden_pending_state_blocks_project_entry_and_returns_to_original_confirmation() = runBlocking {
        val facts = M09SubscriptionFixtures.facts()
        fixture.seed(facts)
        attachAndRender(facts, SubmissionPhase.UNCONFIRMED)
        compose.openHomeCreate("home-create-project")
        compose.onNodeWithTag("pending-submit-dialog").assertIsDisplayed()
        compose.onNodeWithTag("discard-dialog").assertDoesNotExist()
        compose.onNodeWithTag("pending-submit-return-to-form").performClick()
        compose.onNodeWithTag("subscription-state-screen").assertIsDisplayed()
        assertNotNull(saved.get<Bundle>(M09SubscriptionFixtures.FORM_KEY)?.getBundle("submitted"))
        assertEquals(0L, fixture.generation.database.projectDao().countProjects())
        assertEquals(facts.renewals, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.renewals)
    }

    @Test fun hidden_pending_state_blocks_root_system_back_without_finishing_host() = runBlocking {
        val facts = M09SubscriptionFixtures.facts()
        fixture.seed(facts)
        attachAndRender(facts, SubmissionPhase.UNCONFIRMED)
        val host = compose.activity
        Espresso.pressBackUnconditionally()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        fixture.onMain {
            assertFalse("Root Back cannot finish a host with an unconfirmed lifecycle command", host.isFinishing)
            assertFalse("The original lifecycle form must stay recoverable", host.isDestroyed)
        }
        compose.onNodeWithTag("pending-submit-dialog").assertIsDisplayed()
        assertNotNull(saved.get<Bundle>(M09SubscriptionFixtures.FORM_KEY)?.getBundle("submitted"))
    }

    @Test fun hidden_dirty_state_protects_tab_reselection_until_explicit_discard() = runBlocking {
        val facts = M09SubscriptionFixtures.facts()
        fixture.seed(facts)
        attachAndRender(facts, SubmissionPhase.IDLE)
        compose.onNodeWithTag("tab-now").performClick()
        compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
        compose.onNodeWithTag("discard-keep").performClick()
        assertNotNull(saved.get<Bundle>(M09SubscriptionFixtures.FORM_KEY))
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("discard-confirm").performClick()
        compose.onNodeWithTag("review-screen").assertIsDisplayed()
        assertEquals(emptyList<Any>(), fixture.generation.database.subscriptionDao().statesForSubscriptions(listOf(facts.subscription.id)))
        assertEquals(1L, fixture.generation.database.subscriptionDao().countRenewals(facts.subscription.id))
    }

    private suspend fun attachAndRender(facts: SubscriptionFacts, phase: SubmissionPhase) {
        val storage = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(fixture.generation))
        fixture.onMain {
            holder = LedgerUiStateHolder(storage) { fixture.clock }; fixture.retain("holder", holder)
        }
        val ready = withTimeout(10_000) { holder.state.filterIsInstance<LedgerUiState.Ready>().first() }
        fixture.onMain {
            project = ProjectViewModel(ready.repository, SavedStateHandle(), fixture.clock); fixture.retain("project", project)
        }
        saved = SavedStateHandle(mapOf(M09SubscriptionFixtures.FORM_KEY to M09SubscriptionFixtures.stateForm(facts, phase)))
        subscription = fixture.model(saved)
        compose.setContent { LithicTheme {
            val ledger by holder.state.collectAsState()
            LithicApp(ledger, {}, project, subscriptionViewModel = subscription)
        } }
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }
}

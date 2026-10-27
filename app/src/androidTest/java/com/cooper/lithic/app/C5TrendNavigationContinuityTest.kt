package com.cooper.lithic.app

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.ReportRepository
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.data.repository.TotalCostTimelineRepository
import com.cooper.lithic.domain.report.LedgerSnapshot
import com.cooper.lithic.feature.review.TotalCostTrendViewModel
import com.cooper.lithic.ui.C2VisualFixtures
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Real navigation and retained UI scope with an in-memory synthetic database only.
 * No default ledger, file owner, restore or physical-device operation is exercised. */
class C5TrendNavigationContinuityTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val facts = C2VisualFixtures(populated = false)
    private val clock = FixedBusinessClock(facts.day)
    private val coordinator = LocalWriteCoordinator()
    private lateinit var database: LithicDatabase
    private var currentScope: GenerationUiScopeViewModel? = null
    private val shown = mutableStateOf(true)
    private var releaseRead: CompletableDeferred<Unit>? = null

    @Before fun before(): Unit = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),
            LithicDatabase::class.java).build()
        database.initializeDefaults()
    }

    @After fun after(): Unit = runBlocking {
        releaseRead?.complete(Unit)
        val jobs = mutableListOf<Job>()
        compose.runOnIdle {
            shown.value = false
            currentScope?.viewModelStore?.let { store ->
                store.keys().mapNotNullTo(jobs) { store[it]?.viewModelScope?.coroutineContext?.get(Job) }
            }
            compose.activity.viewModelStore.clear()
        }
        compose.waitForIdle()
        jobs.forEach { it.join() }
        if (::database.isInitialized) database.close()
    }

    @Test fun trend_is_lazy_retains_its_existing_flow_while_inactive_and_drops_it_on_generation_change() {
        val reports = ReportRepository(database, coordinator, clock)
        val reads = AtomicInteger()
        val timeline = object : TotalCostTimelineRepository(reports, clock) {
            override val changes = emptyFlow<Unit>()
            override suspend fun loadSnapshot(asOf: LocalDate): LedgerSnapshot {
                reads.incrementAndGet()
                releaseRead?.await()
                return facts.ledger
            }
        }
        var ready by mutableStateOf(LedgerUiState.Ready(0, ZoneId.of("Asia/Shanghai"),
            ProjectRepository(database, coordinator, clock), clock,
            SubscriptionRepository(database, coordinator, clock), 0,
            generationId = "c5-synthetic-first", overview = facts.overview,
            totalCostTimelineRepository = timeline))
        compose.setContent { LithicTheme { if (shown.value) LithicNavigation(ready) } }
        lateinit var scope: GenerationUiScopeViewModel
        compose.runOnIdle {
            scope = ViewModelProvider(compose.activity)[GenerationUiScopeViewModel::class.java]
            currentScope = scope
            assertNull(existingTrend(scope))
            assertEquals(0, reads.get())
        }
        compose.onNodeWithTag("tab-review").performClick()
        lateinit var first: TotalCostTrendViewModel
        compose.runOnIdle { first = checkNotNull(existingTrend(scope)) }
        compose.waitUntil(15_000) { first.state.value.snapshot != null }
        val flow = first.state
        compose.onNodeWithTag("tab-now").performClick()
        compose.runOnIdle {
            assertSame(first, existingTrend(scope))
            assertSame(flow, existingTrend(scope)?.state)
            val inactiveReads = reads.get()
            val inactiveState = first.state.value
            first.refresh()
            assertSame(inactiveState, first.state.value)
            assertEquals(inactiveReads, reads.get())
        }

        // A return still refreshes, while the established snapshot stays in the same Flow.
        releaseRead = CompletableDeferred()
        compose.onNodeWithTag("tab-review").performClick()
        compose.waitUntil(15_000) { first.state.value.loading }
        compose.runOnIdle {
            assertSame(first, existingTrend(scope))
            assertSame(flow, existingTrend(scope)?.state)
            assertSame(facts.ledger, first.state.value.snapshot)
        }
        releaseRead?.complete(Unit)
        compose.waitUntil(15_000) { !first.state.value.loading }
        compose.onNodeWithTag("tab-now").performClick()

        val nextSnapshot = C2VisualFixtures(populated = false).ledger
        val nextTimeline = object : TotalCostTimelineRepository(reports, clock) {
            override val changes = emptyFlow<Unit>()
            override suspend fun loadSnapshot(asOf: LocalDate): LedgerSnapshot = nextSnapshot
        }
        compose.runOnIdle {
            ready = ready.copy(generationId = "c5-synthetic-second",
                repository = ProjectRepository(database, coordinator, clock),
                subscriptionRepository = SubscriptionRepository(database, coordinator, clock),
                totalCostTimelineRepository = nextTimeline)
        }
        compose.runOnIdle { assertNull(existingTrend(scope)) }
        compose.onNodeWithTag("tab-review").performClick()
        lateinit var second: TotalCostTrendViewModel
        compose.runOnIdle { second = checkNotNull(existingTrend(scope)) }
        compose.waitUntil(15_000) { second.state.value.snapshot != null }
        compose.runOnIdle {
            assertNotSame(first, second)
            assertNotSame(flow, second.state)
            assertSame(nextSnapshot, second.state.value.snapshot)
        }
    }

    private fun existingTrend(scope: GenerationUiScopeViewModel): TotalCostTrendViewModel? =
        scope.viewModelStore.keys().mapNotNull { scope.viewModelStore[it] as? TotalCostTrendViewModel }.singleOrNull()
}

package com.cooper.lithic.app

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.repository.MilestoneNoticeRef
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.ui.C2VisualFixtures
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Synthetic in-memory data only; execution still requires an isolated Android test host. */
class CoreLegacyReminderNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val facts = C2VisualFixtures(populated = false)
    private val clock = FixedBusinessClock(facts.day)
    private val coordinator = LocalWriteCoordinator()
    private lateinit var database: LithicDatabase
    private val shown = mutableStateOf(true)
    private var currentScope: GenerationUiScopeViewModel? = null

    @Before fun before(): Unit = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),
            LithicDatabase::class.java).build()
        database.initializeDefaults()
    }

    @After fun after(): Unit = runBlocking {
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

    @Test fun valid_and_invalid_legacy_requests_do_not_resolve_or_navigate() {
        val ready = LedgerUiState.Ready(0, ZoneId.of("Asia/Shanghai"),
            ProjectRepository(database, coordinator, clock), clock,
            SubscriptionRepository(database, coordinator, clock), 0,
            generationId = "synthetic-only", overview = facts.overview)
        val resolutions = AtomicInteger()
        var request by mutableStateOf(MilestoneOpenRequest(1,
            MilestoneNoticeRef("synthetic-only", facts.day, "a".repeat(64), 1), false))
        compose.setContent {
            LithicTheme {
                if (shown.value) LithicNavigation(ready, milestoneRequest = request,
                    resolveMilestone = { resolutions.incrementAndGet(); emptyList() })
            }
        }
        compose.runOnIdle {
            currentScope = ViewModelProvider(compose.activity)[GenerationUiScopeViewModel::class.java]
        }
        compose.onNodeWithTag("tab-now").assertExists()
        compose.onNodeWithTag("milestone-sources").assertDoesNotExist()
        compose.runOnIdle { request = MilestoneOpenRequest(2, null, true) }
        compose.onNodeWithTag("tab-now").assertExists()
        compose.onNodeWithTag("notification-deferred-screen").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, resolutions.get()) }
    }
}

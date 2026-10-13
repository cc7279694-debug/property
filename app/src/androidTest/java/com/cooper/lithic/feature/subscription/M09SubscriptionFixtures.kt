package com.cooper.lithic.feature.subscription

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.data.repository.SubscriptionFacts
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import com.cooper.lithic.feature.project.SubmissionPhase
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Job

/** Synthetic facts use the frozen engine; no test substitutes a display status or cost. */
internal object M09SubscriptionFixtures {
    val today: LocalDate = LocalDate.of(2026, 1, 25)
    val timestamp: Instant = Instant.parse("2026-01-25T12:00:00Z")
    const val FORM_KEY = "lithic.subscription.activeForm"

    fun facts(name: String = "真实月付服务", operation: SubscriptionOperationState? = null,
        stateOn: LocalDate = today, futurePayment: Boolean = false): SubscriptionFacts {
        val id = UUID.randomUUID().toString()
        val subscription = SubscriptionEntity(id, name, LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            createdAt = timestamp, updatedAt = timestamp)
        val first = SubscriptionRenewalEntity(UUID.randomUUID().toString(), id, LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 29),
            15000, BillingCycle.MONTHLY, 30, UUID.randomUUID().toString())
        val renewals = if (futurePayment) listOf(first, first.copy(id = UUID.randomUUID().toString(),
            paidOn = LocalDate.of(2026, 1, 20), effectiveFrom = LocalDate.of(2026, 2, 1),
            paidUntilExclusive = LocalDate.of(2026, 3, 1), reminderDueOn = LocalDate.of(2026, 2, 26),
            operationId = UUID.randomUUID().toString())) else listOf(first)
        val states = operation?.let { listOf(SubscriptionStateChangeEntity(UUID.randomUUID().toString(), id,
            stateOn, it, reason = "用户核对的原因", operationId = UUID.randomUUID().toString())) }.orEmpty()
        return SubscriptionFacts(subscription, renewals, states)
    }

    fun detail(facts: SubscriptionFacts, asOf: LocalDate = today): SubscriptionDetail {
        val snapshot = SubscriptionSnapshot(Subscription(facts.subscription.id), facts.renewals.map {
            Renewal(Money(it.amountCents), it.cycle, it.paidOn, it.effectiveFrom, it.paidUntilExclusive,
                it.reminderDueOn, it.standardDays)
        }, facts.stateChanges.map { SubscriptionStateChange(it.effectiveOn, it.state) })
        val cost = SubscriptionCostEngine.evaluate(snapshot, asOf)
        val current = facts.renewals.singleOrNull { !it.paidOn.isAfter(asOf) && !it.effectiveFrom.isAfter(asOf) && asOf.isBefore(it.paidUntilExclusive) }
            ?.takeIf { cost.currentPrice != null }
        return SubscriptionDetail(facts.subscription,
            CategoryEntity(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, CategoryType.SUBSCRIPTION, "其他", "其他", true),
            facts.renewals, facts.stateChanges, cost, current, asOf)
    }

    /** Complete proposed persisted form contract, independent of any production encoder. */
    fun stateForm(facts: SubscriptionFacts, phase: SubmissionPhase = SubmissionPhase.UNCONFIRMED,
        kind: String = "STATE", dirty: Boolean = true): Bundle {
        val command = UUID.randomUUID().toString()
        val target = SubscriptionStateChangeEntity(UUID.randomUUID().toString(), facts.subscription.id,
            today, SubscriptionOperationState.PAUSED, "保留原始原因", "保留原始备注", command)
        return Bundle().apply {
            putString("kind", kind); putString("subscriptionId", facts.subscription.id)
            putString("commandId", command); putString("renewalId", UUID.randomUUID().toString())
            putString("stateId", target.id); putString("action", "PAUSE"); putString("origin", "CURRENT")
            putString("serviceName", facts.subscription.name); putString("phase", phase.name)
            putString("effectiveOn", "2026-01-25"); putString("reason", "保留原始原因"); putString("stateNote", "保留原始备注")
            putBoolean("dirty", dirty); putBoolean("initialReadComplete", true)
            putBundle("factsBaseline", encodeFacts(facts)); putBundle("targetState", encodeState(target))
            if (phase != SubmissionPhase.IDLE) putBundle("submitted", Bundle(this).apply {
                putString("writtenAt", timestamp.toString())
            })
        }
    }

    private fun encodeFacts(facts: SubscriptionFacts) = Bundle().apply {
        putBundle("subscription", Bundle().apply {
            putString("id", facts.subscription.id); putString("name", facts.subscription.name)
            putString("categoryId", facts.subscription.categoryId); putString("coverAssetId", facts.subscription.coverAssetId)
            putString("note", facts.subscription.note); putString("createdAt", facts.subscription.createdAt.toString())
            putString("updatedAt", facts.subscription.updatedAt.toString())
        })
        putParcelableArrayList("renewals", ArrayList(facts.renewals.map { renewal -> Bundle().apply {
            putString("id", renewal.id); putString("subscriptionId", renewal.subscriptionId)
            putString("paidOn", renewal.paidOn.toString()); putString("effectiveFrom", renewal.effectiveFrom.toString())
            putString("paidUntilExclusive", renewal.paidUntilExclusive.toString()); putString("reminderDueOn", renewal.reminderDueOn.toString())
            putLong("amountCents", renewal.amountCents); putString("cycle", renewal.cycle.name)
            putInt("standardDays", renewal.standardDays); putString("operationId", renewal.operationId)
        } }))
        putParcelableArrayList("stateChanges", ArrayList(facts.stateChanges.map(::encodeState)))
    }

    private fun encodeState(state: SubscriptionStateChangeEntity) = Bundle().apply {
        putString("id", state.id); putString("subscriptionId", state.subscriptionId)
        putString("effectiveOn", state.effectiveOn.toString()); putString("state", state.state.name)
        putString("reason", state.reason); putString("note", state.note); putString("operationId", state.operationId)
    }
}

/** Test-only temporary generation and owner cleanup; production has no additional owner. */
internal class M09SubscriptionRoomFixture {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    val clock = M09MutableBusinessClock(M09SubscriptionFixtures.today)
    private val store = ViewModelStore()
    private val jobs = mutableListOf<Job>()
    private lateinit var root: File
    lateinit var generation: ManagedGeneration
    lateinit var repository: SubscriptionRepository

    suspend fun open() {
        root = File(context.noBackupFilesDir, "m09-ui-${UUID.randomUUID()}")
        check(root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        repository = SubscriptionRepository(generation.database, generation.coordinator, clock)
    }

    suspend fun seed(facts: SubscriptionFacts): String {
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                generation.database.subscriptionDao().apply {
                    insertSubscription(facts.subscription)
                    facts.renewals.forEach { insertRenewal(it) }
                    facts.stateChanges.forEach { insertStateChange(it) }
                }
            }
        }
        return facts.subscription.id
    }

    fun model(handle: SavedStateHandle = SavedStateHandle(), repo: SubscriptionRepository = repository): SubscriptionViewModel {
        lateinit var model: SubscriptionViewModel
        onMain {
            model = SubscriptionViewModel(repo, handle, clock) { M09SubscriptionFixtures.timestamp }
            retain("subscription-${UUID.randomUUID()}", model)
        }
        return model
    }

    fun retain(key: String, model: ViewModel) {
        store.put(key, model); model.viewModelScope.coroutineContext[Job]?.let(jobs::add)
    }

    suspend fun close() {
        onMain { store.clear() }; jobs.forEach { it.join() }
        generation.close()
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(root.deleteRecursively())
    }

    fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
}

internal class M09MutableBusinessClock(@Volatile var date: LocalDate) : BusinessClock {
    override fun today() = date
}

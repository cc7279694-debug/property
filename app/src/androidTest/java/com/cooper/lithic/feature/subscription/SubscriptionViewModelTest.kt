package com.cooper.lithic.feature.subscription

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.repository.CreateSubscriptionCommand
import com.cooper.lithic.data.repository.ConfirmRenewalCommand
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.data.repository.SubscriptionMutationResult
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.project.SubmissionPhase
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Room facts and actual saved task state; no in-memory payment substitute. */
@RunWith(AndroidJUnit4::class)
class SubscriptionViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = FixedBusinessClock(LocalDate.of(2026, 1, 20))
    private val timestamp = Instant.parse("2026-01-20T12:00:00Z")
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var repository: SubscriptionRepository
    private val models = mutableListOf<SubscriptionViewModel>()
    private val stores = mutableListOf<ViewModelStore>()

    @Before fun before() = runBlocking {
        root = File(context.noBackupFilesDir, "m08-vm-${UUID.randomUUID()}")
        check(root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        repository = SubscriptionRepository(generation.database, generation.coordinator, clock)
    }

    @After fun after() = runBlocking {
        val jobs = models.mapNotNull { it.viewModelScope.coroutineContext[Job] }
        onMain { stores.forEach { it.clear() } }
        jobs.forEach { it.join() }
        generation.close()
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(root.deleteRecursively())
    }

    @Test fun raw_four_dates_and_cycle_survive_saved_state_recreation() = runBlocking {
        val handle = SavedStateHandle()
        val first = vm(handle)
        fillCreate(first, "未保存订阅", "123.456")
        onMain { first.changeCycle(BillingCycle.YEARLY); first.changeNote("准确日期不能被预填覆盖") }
        val restored = vm(restoredHandle(handle))
        assertNotNull("The saved task must restore its original active subscription form", restored.state.value.editor)
        val editor = restored.state.value.editor!!
        assertEquals("未保存订阅", editor.serviceName)
        assertEquals("123.456", editor.amount)
        assertEquals(BillingCycle.YEARLY, editor.cycle)
        assertEquals("2026-01-02", editor.paidOn)
        assertEquals("2026-01-03", editor.effectiveFrom)
        assertEquals("2026-02-03", editor.paidUntilExclusive)
        assertEquals("2026-02-05", editor.reminderDueOn)
        assertEquals("准确日期不能被预填覆盖", editor.note)
        assertEquals(LeaveDecision.CONFIRM_DISCARD, restored.leaveDecision())
        assertEquals(0L, generation.database.subscriptionDao().countSubscriptions())
    }

    @Test fun first_confirmed_payment_persists_identity_and_four_dates() = runBlocking {
        val model = vm()
        fillCreate(model, "真实首付款", "150.00")
        onMain { model.save() }
        await(model) { it.committedSubscriptionId != null }
        val id = model.state.value.committedSubscriptionId!!
        val detail = repository.loadSubscriptionDetail(id)!!
        assertEquals("真实首付款", detail.subscription.name)
        val renewal = detail.renewals.single()
        assertEquals(15000L, renewal.amountCents)
        assertEquals(BillingCycle.MONTHLY, renewal.cycle)
        assertEquals(30, renewal.standardDays)
        assertEquals(LocalDate.of(2026, 1, 2), renewal.paidOn)
        assertEquals(LocalDate.of(2026, 1, 3), renewal.effectiveFrom)
        assertEquals(LocalDate.of(2026, 2, 3), renewal.paidUntilExclusive)
        assertEquals(LocalDate.of(2026, 2, 5), renewal.reminderDueOn)
        onMain { model.save() }
        assertEquals(1L, generation.database.subscriptionDao().countSubscriptions())
        assertEquals(listOf(renewal), repository.loadSubscriptionDetail(id)!!.renewals)
    }

    @Test fun invalid_real_payment_preserves_raw_fields_and_reports_amount_error() = runBlocking {
        val model = vm()
        fillCreate(model, "保留未确认输入", "0")
        onMain { model.save() }
        val editor = model.state.value.editor!!
        assertTrue("Zero cannot be accepted as a real renewal payment", editor.errors.containsKey("amount"))
        assertEquals("0", editor.amount)
        assertEquals("2026-01-02", editor.paidOn)
        assertEquals("2026-01-03", editor.effectiveFrom)
        assertEquals("2026-02-03", editor.paidUntilExclusive)
        assertEquals("2026-02-05", editor.reminderDueOn)
        assertNull(model.state.value.committedSubscriptionId)
        assertEquals(0L, generation.database.subscriptionDao().countSubscriptions())
    }

    @Test fun opening_and_discarding_form_never_creates_a_real_payment() = runBlocking {
        val model = vm()
        onMain { model.startCreate() }
        assertNotNull(model.state.value.editor)
        assertEquals(0L, generation.database.subscriptionDao().countSubscriptions())
        fillCreate(model, "只是填写", "150")
        onMain { model.discardForm() }
        assertNull(model.state.value.editor)
        assertEquals(0L, generation.database.subscriptionDao().countSubscriptions())
        assertEquals(LeaveDecision.ALLOW, model.leaveDecision())
    }

    @Test fun in_flight_double_save_and_changes_cannot_change_frozen_payment() = runBlocking {
        val released = CompletableDeferred<Unit>()
        val held = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun createWithRenewal(command: CreateSubscriptionCommand): SubscriptionMutationResult {
                released.await()
                return super.createWithRenewal(command)
            }
        }
        val model = vm(repo = held)
        fillCreate(model, "单次确认", "150")
        onMain { model.save() }
        await(model) { it.editor?.phase == SubmissionPhase.IN_FLIGHT }
        onMain { model.changeAmount("999"); model.changePaidOn("2026-01-19"); model.save() }
        assertEquals("150", model.state.value.editor!!.amount)
        assertEquals("2026-01-02", model.state.value.editor!!.paidOn)
        released.complete(Unit)
        await(model) { it.committedSubscriptionId != null }
        val detail = repository.loadSubscriptionDetail(model.state.value.committedSubscriptionId!!)!!
        assertEquals(15000L, detail.renewals.single().amountCents)
        assertEquals(LocalDate.of(2026, 1, 2), detail.renewals.single().paidOn)
        assertEquals(1L, generation.database.subscriptionDao().countSubscriptions())
    }

    @Test fun lost_create_result_restores_and_replays_the_same_real_payment() = runBlocking {
        val handle = SavedStateHandle()
        val lossy = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun createWithRenewal(command: CreateSubscriptionCommand): SubscriptionMutationResult {
                super.createWithRenewal(command)
                throw IOException("Test result delivery interrupted after actual commit")
            }
        }
        val first = vm(handle, lossy)
        fillCreate(first, "首次结果丢失", "150.00")
        onMain { first.save() }
        await(first) { it.editor?.phase == SubmissionPhase.UNCONFIRMED }
        val before = repository.loadSubscriptionPage().items.single()
        val restored = vm(restoredHandle(handle))
        assertEquals(SubmissionPhase.UNCONFIRMED, restored.state.value.editor!!.phase)
        assertEquals(LeaveDecision.BLOCK_PENDING, restored.leaveDecision())
        onMain { restored.changeAmount("180"); restored.save() }
        await(restored) { it.committedSubscriptionId != null }
        val after = repository.loadSubscriptionDetail(restored.state.value.committedSubscriptionId!!)!!
        assertEquals(before.subscription, after.subscription)
        assertEquals(before.renewals, after.renewals)
        assertEquals("150.00", restored.state.value.editor!!.amount)
        assertEquals(1L, generation.database.subscriptionDao().countSubscriptions())
    }

    @Test fun lost_renewal_result_restores_and_replays_same_operation_identity() = runBlocking {
        val id = seedSubscription()
        val handle = SavedStateHandle()
        val lossy = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun confirmRenewal(command: ConfirmRenewalCommand): SubscriptionMutationResult {
                super.confirmRenewal(command)
                throw IOException("Test renewal result delivery interrupted after actual commit")
            }
        }
        val first = vm(handle, lossy)
        onMain { first.startRenewal(id) }
        await(first) { it.editor?.canEdit == true }
        onMain {
            first.changeAmount("180.00")
            first.changePaidOn("2026-01-20")
            first.changeEffectiveFrom("2026-02-01")
            first.changePaidUntil("2026-03-01")
            first.changeReminderDue("2026-03-04")
            first.save()
        }
        await(first) { it.editor?.phase == SubmissionPhase.UNCONFIRMED }
        val before = repository.loadSubscriptionDetail(id)!!.renewals
        assertEquals(2, before.size)
        val restored = vm(restoredHandle(handle))
        onMain { restored.save() }
        await(restored) { it.committedSubscriptionId == id }
        assertEquals(before, repository.loadSubscriptionDetail(id)!!.renewals)
        assertEquals(2L, generation.database.subscriptionDao().countRenewals(id))
    }

    @Test fun committed_read_failure_only_rereads_and_never_confirms_payment_again() = runBlocking {
        val handle = SavedStateHandle()
        var failRead = true
        val unreadable = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadSubscriptionDetail(id: String, asOf: LocalDate): SubscriptionDetail? {
                if (failRead) throw IOException("Test reading failed after actual saved payment")
                return super.loadSubscriptionDetail(id, asOf)
            }
        }
        val first = vm(handle, unreadable)
        fillCreate(first, "已保存读取失败", "150")
        onMain { first.save() }
        await(first) { it.editor?.phase == SubmissionPhase.COMMITTED && it.editor.message?.contains("已保存") == true }
        assertNull(first.state.value.committedSubscriptionId)
        assertEquals(1L, generation.database.subscriptionDao().countSubscriptions())
        val restored = vm(restoredHandle(handle), unreadable)
        await(restored) { it.editor?.message?.contains("已保存") == true }
        onMain { restored.save(); restored.retryRead() }
        assertEquals(1L, generation.database.subscriptionDao().countSubscriptions())
        assertEquals(SubmissionPhase.COMMITTED, restored.state.value.editor!!.phase)
        failRead = false
        onMain { restored.retryRead() }
        await(restored) { it.committedSubscriptionId != null }
        val id = restored.state.value.committedSubscriptionId!!
        assertEquals(1L, generation.database.subscriptionDao().countRenewals(id))
    }

    @Test fun mismatched_success_identity_stays_pending_until_same_command_replay() = runBlocking {
        var wrongOnce = true
        val wrong = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun createWithRenewal(command: CreateSubscriptionCommand): SubscriptionMutationResult {
                val result = super.createWithRenewal(command)
                if (wrongOnce && result is SubscriptionMutationResult.Success) {
                    wrongOnce = false
                    return result.copy(subscriptionId = UUID.randomUUID().toString())
                }
                return result
            }
        }
        val model = vm(repo = wrong)
        fillCreate(model, "身份必须核对", "150")
        onMain { model.save() }
        await(model) { it.editor?.phase == SubmissionPhase.UNCONFIRMED }
        assertNull(model.state.value.committedSubscriptionId)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        onMain { model.save() }
        await(model) { it.committedSubscriptionId != null }
        assertEquals(1L, generation.database.subscriptionDao().countSubscriptions())
    }

    @Test fun slow_previous_detail_cannot_publish_after_another_identity_is_opened() = runBlocking {
        val firstId = seedSubscription("先前服务")
        val secondId = seedSubscription("当前服务")
        val entered = CompletableDeferred<Unit>()
        val released = CompletableDeferred<Unit>()
        val slow = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadSubscriptionDetail(id: String, asOf: LocalDate): SubscriptionDetail? {
                if (id == firstId) { entered.complete(Unit); released.await() }
                return super.loadSubscriptionDetail(id, asOf)
            }
        }
        val model = vm(repo = slow)
        onMain { model.openDetail(firstId) }
        withTimeout(5_000) { entered.await() }
        onMain { model.openDetail(secondId) }
        assertNull(model.state.value.detail.detail)
        released.complete(Unit)
        await(model) { it.detail.detail?.subscription?.id == secondId }
        assertEquals("当前服务", model.state.value.detail.detail!!.subscription.name)
    }

    @Test fun failed_list_refresh_retains_real_rows_and_an_explicit_read_error() = runBlocking {
        seedSubscription()
        var failRead = false
        val flaky = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadSubscriptionWindow(pageCount: Int, asOf: LocalDate): com.cooper.lithic.data.repository.SubscriptionPage {
                if (failRead) throw IOException("Test refresh failure")
                return super.loadSubscriptionWindow(pageCount, asOf)
            }
        }
        val model = vm(repo = flaky)
        onMain { model.loadList() }
        await(model) { it.list.items.size == 1 }
        val before = model.state.value.list.items
        failRead = true
        onMain { model.refresh() }
        await(model) { it.list.error != null }
        assertEquals(before, model.state.value.list.items)
        assertEquals(1L, model.state.value.list.totalCount)
    }

    @Test fun verified_missing_renewal_target_disables_editing_and_can_leave_without_payment() = runBlocking {
        val missingId = UUID.randomUUID().toString()
        assertNull(repository.loadSubscriptionDetail(missingId))
        val model = vm()
        onMain { model.startRenewal(missingId) }
        await(model) { it.editor?.loading == false && it.editor.message?.contains("不存在") == true }
        val before = model.state.value.editor!!
        assertFalse("A verified missing service cannot offer an editable payment form", before.canEdit)
        assertTrue("The screen must receive the verified write prohibition", before.verifiedCannotWrite)
        assertEquals(LeaveDecision.ALLOW, model.leaveDecision())
        assertNull(model.state.value.committedSubscriptionId)
        onMain {
            model.changeAmount("180.00")
            model.changePaidOn("2026-01-20")
            model.save()
        }
        assertEquals(before, model.state.value.editor)
        assertEquals(0L, generation.database.subscriptionDao().countSubscriptions())
        assertEquals(0L, generation.database.subscriptionDao().countRenewals(missingId))
        onMain { model.discardForm() }
        assertNull(model.state.value.editor)
        assertEquals(LeaveDecision.ALLOW, model.leaveDecision())
    }

    private fun vm(handle: SavedStateHandle = SavedStateHandle(), repo: SubscriptionRepository = repository): SubscriptionViewModel {
        lateinit var model: SubscriptionViewModel
        onMain {
            model = SubscriptionViewModel(repo, handle, clock) { timestamp }
            models += model
            ViewModelStore().also { it.put("subscription", model); stores += it }
        }
        return model
    }

    private suspend fun seedSubscription(name: String = "原有月付服务"): String {
        val id = UUID.randomUUID().toString()
        val commandId = UUID.randomUUID().toString()
        val renewal = SubscriptionRenewalEntity(UUID.randomUUID().toString(), id, LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
            15000, BillingCycle.MONTHLY, 30, commandId)
        val result = repository.createWithRenewal(CreateSubscriptionCommand(commandId,
            SubscriptionEntity(id, name, LithicDatabase.SUBSCRIPTION_FALLBACK_ID, createdAt = timestamp, updatedAt = timestamp), renewal))
        assertTrue(result is SubscriptionMutationResult.Success)
        return id
    }

    private fun fillCreate(model: SubscriptionViewModel, name: String, amount: String) {
        onMain {
            model.startCreate()
            model.changeName(name)
            model.changeCategory(LithicDatabase.SUBSCRIPTION_FALLBACK_ID)
            model.changeAmount(amount)
            model.changePaidOn("2026-01-02")
            model.changeEffectiveFrom("2026-01-03")
            model.changePaidUntil("2026-02-03")
            model.changeReminderDue("2026-02-05")
        }
        assertNotNull("The real editor must exist before the confirm action", model.state.value.editor)
    }

    private fun restoredHandle(source: SavedStateHandle): SavedStateHandle = SavedStateHandle(source.keys().associateWith { key ->
        val value = source.get<Any?>(key)
        if (value is Bundle) Bundle(value) else value
    })

    private suspend fun await(model: SubscriptionViewModel, predicate: (SubscriptionUiState) -> Boolean) {
        val result = withTimeoutOrNull(5_000) { model.state.first(predicate) }
        assertNotNull("Expected actual subscription state was not reached: phase=${model.state.value.editor?.phase}", result)
    }

    private fun onMain(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
}

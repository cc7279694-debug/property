package com.cooper.lithic.feature.subscription

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.core.model.MoneyParseError
import com.cooper.lithic.core.model.MoneyParseResult
import com.cooper.lithic.core.model.MoneyParser
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.repository.ConfirmRenewalCommand
import com.cooper.lithic.data.repository.CreateSubscriptionCommand
import com.cooper.lithic.data.repository.RenewalCursor
import com.cooper.lithic.data.repository.StorageFailureKind
import com.cooper.lithic.data.repository.SubscriptionCursor
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.data.repository.SubscriptionMutationResult
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.data.repository.SubscriptionStateAction
import com.cooper.lithic.data.repository.SubscriptionFacts
import com.cooper.lithic.data.repository.SubscriptionLifecycleResult
import com.cooper.lithic.data.repository.SetSubscriptionStateCommand
import com.cooper.lithic.data.repository.UpdateSubscriptionStateCommand
import com.cooper.lithic.data.repository.CancelScheduledStopCommand
import com.cooper.lithic.data.repository.UpdateSubscriptionRenewalCommand
import com.cooper.lithic.data.repository.DeleteSubscriptionRenewalCommand
import com.cooper.lithic.data.repository.ReenableSubscriptionWithRenewalCommand
import com.cooper.lithic.data.repository.facts
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.project.SubmissionPhase
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Task state and derived reads only; AppContainer owns persistent storage. */
class SubscriptionViewModel(
    private val repository: SubscriptionRepository,
    private val savedState: SavedStateHandle,
    private val clock: BusinessClock,
    private val instant: () -> Instant = Instant::now,
) : ViewModel() {
    private var form = savedState.get<Bundle>(FORM_KEY)?.let(::Bundle)
    private var categories: List<CategoryEntity> = emptyList()
    private var errors: Map<String, String> = emptyMap()
    private var message: String? = null
    private var loadingEditor = false
    private var listRequested = false
    private var loadedPages = 1
    private var cursor: SubscriptionCursor? = null
    private var stoppedRequested = false
    private var stoppedPages = 1
    private var stoppedCursor: SubscriptionCursor? = null
    private var detailId: String? = null
    private var detailVersion = 0L
    private var loadedHistoryPages = 1
    private var historyCursor: RenewalCursor? = null
    private val reads = Mutex()
    private val mutableState = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = mutableState.asStateFlow()

    init {
        form?.let { active ->
            if (!validIdentity(active)) form = null
            else if (active.getString("phase") == SubmissionPhase.IN_FLIGHT.name) active.putString("phase", SubmissionPhase.UNCONFIRMED.name)
        }
        publishForm()
        if (form?.getString("kind") == SubscriptionEditorKind.CREATE.name) loadCategories()
        form?.takeIf(::needsInitialRead)?.let(::loadInitialForm)
        if (phase() == SubmissionPhase.COMMITTED) viewModelScope.launch { form?.let { readCommitted(it) } }
        viewModelScope.launch {
            try { repository.changes.collectLatest { refreshReads() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutableState.value = state.value.copy(list = state.value.list.copy(loading = false, loadingMore = false, error = LIST_ERROR),
                    detail = state.value.detail.copy(loading = false, error = DETAIL_ERROR))
            }
        }
    }
    fun loadList() { listRequested = true; refresh() }
    fun loadStoppedHistory() { stoppedRequested = true; refresh() }
    fun loadMoreStoppedHistory() {
        if (!stoppedRequested || state.value.stoppedHistory.loading || state.value.stoppedHistory.loadingMore || stoppedCursor == null) return
        stoppedPages += 1
        mutableState.value = state.value.copy(stoppedHistory = state.value.stoppedHistory.copy(loadingMore = true)); refresh()
    }
    fun loadMore() {
        if (!listRequested || state.value.list.loading || state.value.list.loadingMore || cursor == null) return
        loadedPages += 1
        mutableState.value = state.value.copy(list = state.value.list.copy(loadingMore = true))
        refresh()
    }
    fun openDetail(subscriptionId: String) {
        detailVersion += 1
        detailId = subscriptionId.takeIf(StoredValueValidation::isUuid)
        loadedHistoryPages = 1; historyCursor = null
        mutableState.value = state.value.copy(detail = if (detailId == null) SubscriptionDetailUiState(loading = false, error = "订阅身份无效，请返回列表。") else SubscriptionDetailUiState())
        refresh()
    }
    fun loadMoreHistory() {
        if (detailId == null || state.value.detail.history.loading || state.value.detail.history.loadingMore || historyCursor == null) return
        loadedHistoryPages += 1
        mutableState.value = state.value.copy(detail = state.value.detail.copy(history = state.value.detail.history.copy(loadingMore = true)))
        refresh()
    }
    fun startCreate() {
        if (form != null) return
        val today = clock.today()
        form = newForm(SubscriptionEditorKind.CREATE, UUID.randomUUID().toString()).apply {
            putString("categoryId", LithicDatabase.SUBSCRIPTION_FALLBACK_ID)
            putString("paidOn", today.toString()); putString("effectiveFrom", today.toString())
            putString("paidUntilExclusive", calendarEnd(today, BillingCycle.MONTHLY))
            putString("reminderDueOn", getString("paidUntilExclusive"))
        }
        publishForm(); loadCategories()
    }
    fun startRenewal(subscriptionId: String, origin: String = "CURRENT") {
        if (form != null || !StoredValueValidation.isUuid(subscriptionId)) return
        form = newForm(SubscriptionEditorKind.RENEWAL, subscriptionId).apply { putString("origin", origin) }
        loadInitialForm(requireNotNull(form))
    }
    fun changeName(value: String) { if (form?.getString("kind") == SubscriptionEditorKind.CREATE.name) change("serviceName", value) }
    fun changeCategory(value: String) { if (form?.getString("kind") == SubscriptionEditorKind.CREATE.name) change("categoryId", value) }
    fun changeNote(value: String) { if (form?.getString("kind") == SubscriptionEditorKind.CREATE.name) change("note", value) }
    fun changeAmount(value: String) = change("amount", value)
    fun changeCycle(value: BillingCycle) = change("cycle", value.name)
    fun changePaidOn(value: String) = change("paidOn", value)
    fun changeEffectiveFrom(value: String) = change("effectiveFrom", value)
    fun changePaidUntil(value: String) = change("paidUntilExclusive", value)
    fun changeReminderDue(value: String) = change("reminderDueOn", value)

    fun startStateAction(subscriptionId: String, action: SubscriptionStateAction, origin: String = "CURRENT") =
        startOperation("STATE", subscriptionId, origin, action = action.name)
    fun startCorrectState(subscriptionId: String, stateId: String, origin: String = "CURRENT") =
        startOperation("CORRECT_STATE", subscriptionId, origin, stateId = stateId)
    fun startCancelStop(subscriptionId: String, stateId: String, origin: String = "CURRENT") =
        startOperation("CANCEL_STOP", subscriptionId, origin, stateId = stateId)
    fun startCorrectRenewal(subscriptionId: String, renewalId: String, origin: String = "CURRENT") =
        startOperation("CORRECT_RENEWAL", subscriptionId, origin, renewalId = renewalId)
    fun startDeleteRenewal(subscriptionId: String, renewalId: String, origin: String = "CURRENT") =
        startOperation("DELETE_RENEWAL", subscriptionId, origin, renewalId = renewalId)
    fun changeStateEffectiveOn(value: String) = change("effectiveOn", value)
    fun changeStateReason(value: String) = change("reason", value)
    fun changeStateNote(value: String) = change("stateNote", value)
    fun changeStateValue(value: SubscriptionOperationState) = change("stateValue", value.name)
    fun changeStopEarly(value: Boolean) {
        val active = form ?: return
        if (!canChange(active) || text(active, "kind") != "STATE" || text(active, "action") !in listOf("STOP_AT_END", "STOP_EARLY")) return
        change("action", if (value) "STOP_EARLY" else "STOP_AT_END")
        change("effectiveOn", if (value) clock.today().toString() else text(active, "fixedStopOn"))
    }

    fun save() {
        val active = form ?: return
        if (text(active, "kind") !in listOf("CREATE", "RENEWAL")) { saveLifecycle(active); return }
        if (phase() in listOf(SubmissionPhase.IN_FLIGHT, SubmissionPhase.COMMITTED) || loadingEditor || needsInitialRead(active) || active.getBoolean("conflict") || active.getBoolean("verifiedNoWrite")) return
        if (active.getBundle("submitted") == null) {
            errors = validate(active)
            if (errors.isNotEmpty()) { publishForm(); return }
            active.putBundle("submitted", Bundle(active).apply { putString("writtenAt", instant().toString()) })
        }
        active.putString("phase", SubmissionPhase.IN_FLIGHT.name); message = null
        publishForm() // Save the complete immutable command before entering the real transaction.
        viewModelScope.launch {
            try {
                val envelope = requireNotNull(active.getBundle("submitted"))
                val result = withContext(Dispatchers.IO) { applyEnvelope(envelope) }
                if (form !== active) return@launch
                when (result) {
                    is SubscriptionMutationResult.Success -> {
                        if (result.subscriptionId != text(envelope, "subscriptionId") || result.renewalId != text(envelope, "renewalId")) {
                            unconfirmed(active, "返回的付款身份无法核对，原始提交已保留，请重试同一付款。")
                        } else {
                            active.putString("phase", SubmissionPhase.COMMITTED.name)
                            savedState[LAST_CONFIRMED_KEY] = result.subscriptionId
                            message = null; publishForm(); readCommitted(active)
                        }
                    }
                    is SubscriptionMutationResult.ValidationFailure -> {
                        active.remove("submitted"); active.putString("phase", SubmissionPhase.IDLE.name)
                        message = "付款未保存，请核对金额、类别和完整覆盖日期。${result.reason}"; publishForm()
                    }
                    is SubscriptionMutationResult.Conflict -> { active.putBoolean("conflict", true); unconfirmed(active, "提交身份或付款内容发生冲突，请读取当前记录核对；应用不会覆盖已有付款。") }
                    is SubscriptionMutationResult.StorageFailure -> unconfirmed(active, when (result.kind) {
                        StorageFailureKind.DISK_FULL -> "设备空间不足，输入已保留。腾出空间后核对并重试同一付款。"
                        StorageFailureKind.CORRUPT -> "本地数据异常，原始输入已保留。请不要清除应用数据。"
                        StorageFailureKind.DATABASE -> "暂时无法确认付款保存结果，原始输入已保留，请重试同一付款。"
                    })
                }
            } catch (cancelled: CancellationException) {
                if (phase() != SubmissionPhase.COMMITTED && form === active) unconfirmed(active, "付款保存结果尚未确认，原始提交已保留。")
                throw cancelled
            } catch (_: Exception) {
                if (form === active) {
                    if (phase() != SubmissionPhase.COMMITTED) unconfirmed(active, "暂时无法确认付款保存结果，请核对并重试同一付款。")
                    else { message = COMMITTED_ERROR; publishForm() }
                }
            }
        }
    }
    fun retryRead() {
        publishForm()
        val active = form
        when {
            active != null && phase() == SubmissionPhase.COMMITTED -> viewModelScope.launch {
                readCommitted(active, allowChangedExit = active.getBoolean("conflict"))
            }
            active != null && needsInitialRead(active) -> loadInitialForm(active)
            active?.getBoolean("conflict") == true -> viewModelScope.launch { resolveConflict(active) }
            else -> refresh()
        }
    }
    fun refresh() {
        publishForm(); form?.takeIf(::needsInitialRead)?.let(::loadInitialForm)
        viewModelScope.launch { refreshReads() }
    }
    fun leaveDecision(): LeaveDecision = when {
        form?.getBoolean("verifiedNoWrite") == true -> LeaveDecision.ALLOW
        phase() in listOf(SubmissionPhase.IN_FLIGHT, SubmissionPhase.UNCONFIRMED) -> LeaveDecision.BLOCK_PENDING
        phase() == SubmissionPhase.COMMITTED && state.value.committedSubscriptionId == null -> LeaveDecision.BLOCK_PENDING
        phase() == SubmissionPhase.IDLE && form?.getBoolean("dirty") == true -> LeaveDecision.CONFIRM_DISCARD
        else -> LeaveDecision.ALLOW
    }
    fun discardForm() {
        if (leaveDecision() == LeaveDecision.BLOCK_PENDING) return
        form = null; loadingEditor = false; errors = emptyMap(); message = null; publishForm()
        mutableState.value = state.value.copy(committedSubscriptionId = null, committedOrigin = null)
    }
    fun consumeCommitted() {
        if (phase() != SubmissionPhase.COMMITTED || state.value.committedSubscriptionId != form?.getString("subscriptionId")) return
        form = null; loadingEditor = false; errors = emptyMap(); message = null
        mutableState.value = state.value.copy(committedSubscriptionId = null, committedOrigin = null); publishForm()
    }
    private fun loadInitialForm(requested: Bundle) {
        if (loadingEditor || !needsInitialRead(requested)) return
        loadingEditor = true; publishForm()
        viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { repository.loadSubscriptionDetail(text(requested, "subscriptionId"), clock.today()) }
                if (form !== requested || !needsInitialRead(requested)) return@launch
                if (detail == null) {
                    requested.putBoolean("verifiedNoWrite", true)
                    message = "已核对当前订阅不存在，无法新增付款。输入已保留，可以返回列表。"
                } else {
                    require(detail.subscription.id == text(requested, "subscriptionId")) { "Initial read identity changed" }
                    if (text(requested, "kind") != "RENEWAL") {
                        hydrateLifecycle(requested, detail)
                        return@launch
                    }
                    fun raw(key: String, value: String) { if (!requested.containsKey(key)) requested.putString(key, value) }
                    val latest = detail.renewals.sortedWith(compareByDescending<SubscriptionRenewalEntity> { it.paidOn }.thenBy { it.id }).firstOrNull()
                    val cycle = latest?.cycle ?: BillingCycle.MONTHLY
                    val from = maxOf(clock.today(), detail.renewals.maxOfOrNull { it.paidUntilExclusive } ?: clock.today())
                    requested.putString("serviceName", detail.subscription.name)
                    raw("amount", latest?.let { BigDecimal.valueOf(it.amountCents, 2).toPlainString() }.orEmpty())
                    raw("cycle", cycle.name); raw("paidOn", clock.today().toString()); raw("effectiveFrom", from.toString())
                    raw("paidUntilExclusive", calendarEnd(from, cycle)); raw("reminderDueOn", text(requested, "paidUntilExclusive"))
                    requested.putBoolean("initialReadComplete", true); message = null
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (form === requested) message = "订阅资料暂时无法读取，原始输入不会被替换。" }
            finally { if (form === requested) { loadingEditor = false; publishForm() } }
        }
    }
    private suspend fun readCommitted(active: Bundle, allowChangedExit: Boolean = false) {
        if (form !== active || phase() != SubmissionPhase.COMMITTED || loadingEditor) return
        val requestedVersion = detailVersion
        loadingEditor = true; publishForm()
        try {
            val id = text(active, "subscriptionId")
            val detail = withContext(Dispatchers.IO) { repository.loadSubscriptionDetail(id, clock.today()) }
            if (form !== active || phase() != SubmissionPhase.COMMITTED) return
            if (detailVersion != requestedVersion) {
                message = "详情已切换，原提交保持待核对，不会重定向当前页面。请返回原表单明确读取核对。"
                return
            }
            require(detail == null || detail.subscription.id == id) { "Committed read identity changed" }
            val envelope = requireNotNull(active.getBundle("submitted"))
            if (detail == null || !matchesStored(detail, envelope)) {
                if (allowChangedExit) {
                    active.putBoolean("verifiedNoWrite", true)
                    message = "已明确核对当前事实：记录已经变化，原始提交不会覆盖当前记录，也不会以旧结果导航。原输入保留，可以返回当前记录查看。"
                } else {
                    active.putBoolean("conflict", true)
                    message = "已读取当前事实，记录在原提交后已有变化。原信封保留；请明确核对当前事实后安全返回，不会自动重新写入或以旧结果导航。"
                }
            } else {
                active.putBoolean("conflict", false)
                message = null
                mutableState.value = state.value.copy(committedSubscriptionId = id,
                    committedOrigin = committedSubscriptionOrigin(text(active, "origin"), detail.cost.displayStatus == SubscriptionDisplayStatus.STOPPED))
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (form === active) message = COMMITTED_ERROR }
        finally { if (form === active) { loadingEditor = false; publishForm() } }
    }
    /** Explicit conflict reads do not overwrite history or silently allocate a fresh payment. */
    private suspend fun resolveConflict(active: Bundle) {
        if (form !== active || loadingEditor) return
        val requestedVersion = detailVersion
        loadingEditor = true; publishForm()
        try {
            val id = text(active, "subscriptionId")
            val detail = withContext(Dispatchers.IO) { repository.loadSubscriptionDetail(id, clock.today()) }
            if (form !== active) return
            if (detailVersion != requestedVersion) {
                message = "详情已切换，原提交和待核对状态保留；迟到的读取不会重定向当前页面或解除原提交保护。请返回原表单明确核对。"
                return
            }
            val envelope = requireNotNull(active.getBundle("submitted"))
            require(detail == null || detail.subscription.id == id) { "Conflict read identity changed" }
            if (detail != null && matchesStored(detail, envelope)) {
                active.putString("phase", SubmissionPhase.COMMITTED.name); active.putBoolean("conflict", false)
                savedState[LAST_CONFIRMED_KEY] = id; message = null
                mutableState.value = state.value.copy(committedSubscriptionId = id,
                    committedOrigin = committedSubscriptionOrigin(text(active, "origin"), detail.cost.displayStatus == SubscriptionDisplayStatus.STOPPED))
            } else {
                active.putBoolean("verifiedNoWrite", true)
                message = "已核对当前记录：事实已经变化，原始提交不会覆盖当前记录。原始输入已保留，可以返回查看当前记录。"
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (form === active) message = "当前付款暂时无法读取，原始提交与输入仍保留，请重试核对。" }
        finally { if (form === active) { loadingEditor = false; publishForm() } }
    }
    private suspend fun refreshReads() = reads.withLock {
        val asOf = clock.today()
        if (listRequested) {
            try {
                val page = withContext(Dispatchers.IO) { repository.loadManagementWindow(false, loadedPages, asOf) }
                cursor = page.nextCursor
                mutableState.value = state.value.copy(list = SubscriptionListUiState(page.items, page.totalCount, loading = false, hasMore = page.nextCursor != null))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.value.copy(list = state.value.list.copy(loading = false, loadingMore = false, error = LIST_ERROR)) }
        }
        if (stoppedRequested) {
            try {
                val page = withContext(Dispatchers.IO) { repository.loadManagementWindow(true, stoppedPages, asOf) }
                stoppedCursor = page.nextCursor
                mutableState.value = state.value.copy(stoppedHistory = SubscriptionListUiState(page.items, page.totalCount,
                    loading = false, hasMore = page.nextCursor != null))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.value.copy(stoppedHistory = state.value.stoppedHistory.copy(
                loading = false, loadingMore = false, error = "已停止订阅暂时无法读取，已有历史不会被当作空记录。")) }
        }
        detailId?.let { id ->
            val version = detailVersion
            fun stillCurrent() = detailId == id && detailVersion == version
            try {
                val detail = withContext(Dispatchers.IO) { repository.loadSubscriptionDetail(id, asOf) }
                require(detail == null || detail.subscription.id == id) { "Detail read identity changed" }
                if (!stillCurrent()) return@let
                mutableState.value = state.value.copy(detail = state.value.detail.copy(detail = detail, loading = false, missing = detail == null, error = null))
                if (detail != null) {
                    try {
                        val page = withContext(Dispatchers.IO) { repository.loadRenewalHistoryWindow(id, loadedHistoryPages, asOf) }
                        require(page.items.all { it.subscriptionId == id }) { "History read identity changed" }
                        if (stillCurrent()) {
                            historyCursor = page.nextCursor
                            mutableState.value = state.value.copy(detail = state.value.detail.copy(history = RenewalHistoryUiState(page.items, page.totalCount, loading = false, hasMore = page.nextCursor != null)))
                        }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { if (stillCurrent()) mutableState.value = state.value.copy(detail = state.value.detail.copy(history = state.value.detail.history.copy(loading = false, loadingMore = false, error = HISTORY_ERROR))) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (stillCurrent()) mutableState.value = state.value.copy(detail = state.value.detail.copy(loading = false, missing = false,
                error = if (savedState.get<String>(LAST_CONFIRMED_KEY) == id) COMMITTED_ERROR else DETAIL_ERROR)) }
        }
    }
    private fun startOperation(kind: String, id: String, origin: String, action: String = "",
        stateId: String? = null, renewalId: String? = null) {
        if (form != null || !StoredValueValidation.isUuid(id) || !isSubscriptionOriginSupported(origin) ||
            stateId?.let { !StoredValueValidation.isUuid(it) } == true || renewalId?.let { !StoredValueValidation.isUuid(it) } == true) return
        form = newForm(SubscriptionEditorKind.RENEWAL, id).apply {
            putString("kind", kind); putString("origin", origin); putString("action", action)
            val allocated = UUID.randomUUID().toString()
            putString("stateId", stateId ?: allocated); putString("newStateId", allocated)
            renewalId?.let { putString("renewalId", it) }
        }
        publishForm(); loadInitialForm(requireNotNull(form))
    }

    private fun hydrateLifecycle(active: Bundle, detail: SubscriptionDetail) {
        message = null
        val facts = detail.facts()
        active.putBundle("factsBaseline", SubscriptionFactsCodec.encode(facts))
        active.putString("serviceName", detail.subscription.name)
        val today = clock.today()
        val lastPaidBoundary = maxOf(today, detail.renewals.maxOf { it.paidUntilExclusive })
        val scheduled = detail.stateChanges.filter { it.state == SubscriptionOperationState.STOPPED && it.effectiveOn.isAfter(today) }
            .minByOrNull { it.effectiveOn }
        if (text(active, "kind") == "STATE" && text(active, "action") in listOf("STOP_AT_END", "STOP_EARLY")) {
            active.putString("fixedStopOn", lastPaidBoundary.toString())
        } else scheduled?.let { active.putString("fixedStopOn", it.effectiveOn.toString()) }
        fun raw(key: String, value: String) { if (!active.containsKey(key)) active.putString(key, value) }
        when (text(active, "kind")) {
            "STATE" -> {
                val action = SubscriptionStateAction.valueOf(text(active, "action"))
                if (action in listOf(SubscriptionStateAction.STOP_AT_END, SubscriptionStateAction.STOP_EARLY) && scheduled != null) {
                    active.putBoolean("verifiedNoWrite", true)
                    message = "已有 ${scheduled.effectiveOn} 的停止计划，请返回详情明确修正或取消原计划；不会追加另一条计划。"
                }
                val enabling = action in listOf(SubscriptionStateAction.RESUME, SubscriptionStateAction.REOPEN)
                val targetValue = when (action) {
                    SubscriptionStateAction.PAUSE -> SubscriptionOperationState.PAUSED
                    SubscriptionStateAction.RESUME, SubscriptionStateAction.REOPEN -> SubscriptionOperationState.ENABLED
                    else -> SubscriptionOperationState.STOPPED
                }
                val effective = if (action == SubscriptionStateAction.STOP_AT_END) lastPaidBoundary else today
                raw("effectiveOn", effective.toString()); raw("stateValue", targetValue.name)
                raw("reason", ""); raw("stateNote", "")
                if (enabling && detail.renewals.none { !it.paidOn.isAfter(today) && it.paidUntilExclusive.isAfter(today) }) {
                    active.putString("kind", "REENABLE"); active.putString("reenableAction", action.name)
                    raw("amount", ""); raw("paidOn", ""); raw("effectiveFrom", ""); raw("paidUntilExclusive", ""); raw("reminderDueOn", "")
                    raw("cycle", detail.renewals.maxBy { it.paidOn }.cycle.name)
                }
            }
            "CORRECT_STATE", "CANCEL_STOP" -> {
                val original = detail.stateChanges.singleOrNull { it.id == text(active, "stateId") }
                if (original == null) {
                    active.putBoolean("verifiedNoWrite", true); message = "已核对目标状态事实不存在，不能覆盖别的记录。"
                } else {
                    active.putBundle("stateBaseline", SubscriptionFactsCodec.encodeState(original))
                    raw("effectiveOn", original.effectiveOn.toString()); raw("stateValue", original.state.name)
                    raw("reason", original.reason.orEmpty()); raw("stateNote", original.note.orEmpty())
                    if (text(active, "kind") == "CANCEL_STOP" && (original.state != SubscriptionOperationState.STOPPED || !original.effectiveOn.isAfter(today))) {
                        active.putBoolean("verifiedNoWrite", true); message = "这条停止已生效或不是停止计划，请从原订阅明确重新开通或修正状态事实。"
                    }
                }
            }
            "CORRECT_RENEWAL", "DELETE_RENEWAL" -> {
                val original = detail.renewals.singleOrNull { it.id == text(active, "renewalId") }
                if (original == null) {
                    active.putBoolean("verifiedNoWrite", true); message = "已核对目标付款不存在，不能改写别的付款。"
                } else {
                    active.putBundle("renewalBaseline", SubscriptionFactsCodec.encodeRenewal(original))
                    raw("amount", BigDecimal.valueOf(original.amountCents, 2).toPlainString()); raw("cycle", original.cycle.name)
                    raw("paidOn", original.paidOn.toString()); raw("effectiveFrom", original.effectiveFrom.toString())
                    raw("paidUntilExclusive", original.paidUntilExclusive.toString()); raw("reminderDueOn", original.reminderDueOn.toString())
                    if (text(active, "kind") == "DELETE_RENEWAL" && detail.renewals.size == 1) {
                        active.putBoolean("lastPayment", true); message = "最后一笔真实付款不能单独删除；订阅身份和原始记录会保留。"
                    }
                }
            }
        }
        active.putBoolean("initialReadComplete", true)
    }

    private fun validateLifecycle(active: Bundle): Map<String, String> = buildMap {
        if (active.getBundle("factsBaseline") == null) put("form", "请先成功读取原始事实。")
        val kind = text(active, "kind")
        if (kind in listOf("CORRECT_RENEWAL", "REENABLE")) putAll(validate(active))
        if (kind in listOf("STATE", "CORRECT_STATE", "REENABLE")) {
            val effective = date(active, "effectiveOn")
            if (effective == null) put("effectiveOn", "请输入有效的真实状态日期。")
            if (kind in listOf("STATE", "REENABLE")) {
                val action = text(active, "action")
                if (action in listOf("PAUSE", "RESUME", "REOPEN") && effective != clock.today())
                    put("form", "记录时区的日期已变化，请返回后重新核对；原提交日期不会自动替换。")
                if (action == "STOP_EARLY" && (effective == null || effective.isBefore(clock.today()) ||
                        !effective.isBefore(LocalDate.parse(text(active, "fixedStopOn")))))
                    put("effectiveOn", "提前停止请选择今天或早于最后已付边界的未来日期。")
            }
        }
    }

    private fun freezeLifecycle(active: Bundle) {
        val facts = SubscriptionFactsCodec.decode(requireNotNull(active.getBundle("factsBaseline")))
        val kind = text(active, "kind")
        if (kind in listOf("STATE", "CORRECT_STATE", "REENABLE")) {
            val effective = requireNotNull(date(active, "effectiveOn"))
            val original = if (kind == "CORRECT_STATE") SubscriptionFactsCodec.decodeState(requireNotNull(active.getBundle("stateBaseline")))
                else facts.stateChanges.singleOrNull { it.effectiveOn == effective }
            val target = SubscriptionStateChangeEntity(original?.id ?: text(active, "newStateId"), facts.subscription.id, effective,
                SubscriptionOperationState.valueOf(text(active, "stateValue")), text(active, "reason").trim().ifEmpty { null },
                text(active, "stateNote").trim().ifEmpty { null }, original?.operationId ?: text(active, "commandId"))
            active.putString("stateId", target.id); active.putBundle("targetState", SubscriptionFactsCodec.encodeState(target))
        }
        if (kind in listOf("CORRECT_RENEWAL", "REENABLE")) {
            val original = active.getBundle("renewalBaseline")?.let(SubscriptionFactsCodec::decodeRenewal)
            val target = decodeRenewal(active).copy(operationId = original?.operationId ?: text(active, "commandId"))
            active.putBundle("targetRenewal", SubscriptionFactsCodec.encodeRenewal(target))
        }
        active.putBundle("submitted", Bundle(active).apply { putString("writtenAt", instant().toString()) })
    }

    private fun saveLifecycle(active: Bundle) {
        if (phase() in listOf(SubmissionPhase.IN_FLIGHT, SubmissionPhase.COMMITTED) || loadingEditor || needsInitialRead(active) ||
            active.getBoolean("conflict") || active.getBoolean("verifiedNoWrite") || active.getBoolean("lastPayment")) return
        if (active.getBundle("submitted") == null) {
            errors = validateLifecycle(active)
            if (errors.isNotEmpty()) { publishForm(); return }
            freezeLifecycle(active)
        }
        active.putString("phase", SubmissionPhase.IN_FLIGHT.name); message = null; publishForm()
        viewModelScope.launch {
            try {
                val envelope = requireNotNull(active.getBundle("submitted"))
                val result = withContext(Dispatchers.IO) { applyLifecycle(envelope) }
                if (form !== active) return@launch
                when (result) {
                    is SubscriptionLifecycleResult.Saved, is SubscriptionLifecycleResult.CurrentAbsence -> {
                        if (!matchesLifecycleResult(result, envelope)) unconfirmed(active, "返回的事实身份无法核对，原始提交已保留，请核对并重试同一提交。")
                        else {
                            active.putString("phase", SubmissionPhase.COMMITTED.name); savedState[LAST_CONFIRMED_KEY] = text(envelope, "subscriptionId")
                            message = null; publishForm(); readCommitted(active)
                        }
                    }
                    is SubscriptionLifecycleResult.ValidationFailure -> {
                        active.remove("submitted"); active.putString("phase", SubmissionPhase.IDLE.name)
                        message = "未保存，原始输入保持。请核对原事实及日期。${result.reason}"; publishForm()
                    }
                    is SubscriptionLifecycleResult.Conflict -> {
                        active.putBoolean("conflict", true); unconfirmed(active, "原始事实发生冲突，请读取核对。不会覆盖新事实或自动调整停止计划。${result.reason}")
                    }
                    is SubscriptionLifecycleResult.StorageFailure -> unconfirmed(active, when (result.kind) {
                        StorageFailureKind.DISK_FULL -> "设备空间不足，原始提交保留。腾出空间后核对并重试同一提交。"
                        StorageFailureKind.CORRUPT -> "本地数据异常，原始事实和输入保留，请不要清除应用数据。"
                        StorageFailureKind.DATABASE -> "保存结果暂时无法确认，请核对并重试同一提交。"
                    })
                }
            } catch (cancelled: CancellationException) {
                if (form === active && phase() != SubmissionPhase.COMMITTED) unconfirmed(active, "保存结果尚未确认，原始提交保留。")
                throw cancelled
            } catch (_: Exception) {
                if (form === active) {
                    if (phase() != SubmissionPhase.COMMITTED) unconfirmed(active, "保存结果尚未确认，请核对并重试同一提交。")
                    else { message = COMMITTED_ERROR; publishForm() }
                }
            }
        }
    }

    private suspend fun applyLifecycle(envelope: Bundle): SubscriptionLifecycleResult {
        val facts = SubscriptionFactsCodec.decode(requireNotNull(envelope.getBundle("factsBaseline")))
        val command = text(envelope, "commandId")
        fun oldState() = SubscriptionFactsCodec.decodeState(requireNotNull(envelope.getBundle("stateBaseline")))
        fun targetState() = SubscriptionFactsCodec.decodeState(requireNotNull(envelope.getBundle("targetState")))
        fun oldRenewal() = SubscriptionFactsCodec.decodeRenewal(requireNotNull(envelope.getBundle("renewalBaseline")))
        fun targetRenewal() = SubscriptionFactsCodec.decodeRenewal(requireNotNull(envelope.getBundle("targetRenewal")))
        return when (text(envelope, "kind")) {
            "STATE" -> repository.setState(SetSubscriptionStateCommand(command, facts, targetState(), SubscriptionStateAction.valueOf(text(envelope, "action"))))
            "CORRECT_STATE" -> repository.updateState(UpdateSubscriptionStateCommand(command, facts, oldState(), targetState()))
            "CANCEL_STOP" -> repository.cancelScheduledStop(CancelScheduledStopCommand(command, facts, oldState()))
            "CORRECT_RENEWAL" -> repository.updateRenewal(UpdateSubscriptionRenewalCommand(command, facts, oldRenewal(), targetRenewal()))
            "DELETE_RENEWAL" -> repository.deleteRenewal(DeleteSubscriptionRenewalCommand(command, facts, oldRenewal()))
            "REENABLE" -> repository.reenableWithRenewal(ReenableSubscriptionWithRenewalCommand(command, facts, targetRenewal(), targetState()))
            else -> error("Unknown lifecycle command")
        }
    }

    private fun matchesLifecycleResult(result: SubscriptionLifecycleResult, envelope: Bundle): Boolean {
        val kind = text(envelope, "kind")
        val id = text(envelope, "subscriptionId")
        return when (result) {
            is SubscriptionLifecycleResult.Saved -> result.subscriptionId == id &&
                result.stateId == (if (kind in listOf("STATE", "CORRECT_STATE", "REENABLE")) text(envelope, "stateId") else null) &&
                result.renewalId == (if (kind in listOf("CORRECT_RENEWAL", "REENABLE")) text(envelope, "renewalId") else null)
            is SubscriptionLifecycleResult.CurrentAbsence -> result.subscriptionId == id &&
                ((kind == "CANCEL_STOP" && result.factId == text(envelope, "stateId")) ||
                    (kind == "DELETE_RENEWAL" && result.factId == text(envelope, "renewalId")))
            else -> false
        }
    }

    private fun matchesStored(detail: SubscriptionDetail, envelope: Bundle): Boolean {
        val kind = text(envelope, "kind")
        if (kind in listOf("CREATE", "RENEWAL")) return matchesStoredPayment(detail, envelope)
        val expected = SubscriptionFactsCodec.decode(requireNotNull(envelope.getBundle("factsBaseline")))
        var renewals = expected.renewals
        var states = expected.stateChanges
        if (kind in listOf("STATE", "CORRECT_STATE", "REENABLE")) {
            val target = SubscriptionFactsCodec.decodeState(requireNotNull(envelope.getBundle("targetState")))
            states = states.filterNot { it.id == target.id } + target
        }
        if (kind == "CANCEL_STOP") states = states.filterNot { it.id == text(envelope, "stateId") }
        if (kind in listOf("CORRECT_RENEWAL", "REENABLE")) {
            val target = SubscriptionFactsCodec.decodeRenewal(requireNotNull(envelope.getBundle("targetRenewal")))
            renewals = renewals.filterNot { it.id == target.id } + target
        }
        if (kind == "DELETE_RENEWAL") renewals = renewals.filterNot { it.id == text(envelope, "renewalId") }
        return detail.subscription == expected.subscription && detail.renewals.toSet() == renewals.toSet() &&
            detail.stateChanges.toSet() == states.toSet()
    }

    private fun loadCategories() = viewModelScope.launch {
        try { categories = withContext(Dispatchers.IO) { repository.loadSubscriptionCategories() } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { message = "类别暂时无法读取，原始输入已保留。" }
        publishForm()
    }
    private suspend fun applyEnvelope(envelope: Bundle): SubscriptionMutationResult {
        val renewal = decodeRenewal(envelope)
        val commandId = text(envelope, "commandId")
        return if (text(envelope, "kind") == SubscriptionEditorKind.CREATE.name) repository.createWithRenewal(CreateSubscriptionCommand(commandId, decodeSubscription(envelope), renewal))
        else repository.confirmRenewal(ConfirmRenewalCommand(commandId, renewal))
    }
    private fun matchesStoredPayment(detail: SubscriptionDetail, envelope: Bundle): Boolean =
        detail.renewals.any { it == decodeRenewal(envelope) } &&
            (text(envelope, "kind") != SubscriptionEditorKind.CREATE.name || detail.subscription == decodeSubscription(envelope))
    private fun decodeSubscription(envelope: Bundle): SubscriptionEntity {
        val writtenAt = Instant.parse(text(envelope, "writtenAt"))
        return SubscriptionEntity(text(envelope, "subscriptionId"), text(envelope, "serviceName").trim(), text(envelope, "categoryId"),
            note = text(envelope, "note").trim().ifEmpty { null }, createdAt = writtenAt, updatedAt = writtenAt)
    }
    private fun decodeRenewal(envelope: Bundle): SubscriptionRenewalEntity {
        val cycle = BillingCycle.valueOf(text(envelope, "cycle"))
        val cents = (MoneyParser.parseCny(text(envelope, "amount")) as MoneyParseResult.Success).money.cents
        return SubscriptionRenewalEntity(text(envelope, "renewalId"), text(envelope, "subscriptionId"),
            LocalDate.parse(text(envelope, "paidOn").trim()), LocalDate.parse(text(envelope, "effectiveFrom").trim()),
            LocalDate.parse(text(envelope, "paidUntilExclusive").trim()), LocalDate.parse(text(envelope, "reminderDueOn").trim()),
            cents, cycle, cycle.standardDays, text(envelope, "commandId"))
    }
    private fun validate(active: Bundle): Map<String, String> = buildMap {
        if (text(active, "kind") == SubscriptionEditorKind.CREATE.name) {
            if (text(active, "serviceName").isBlank()) put("serviceName", "请输入服务名称。")
            if (!StoredValueValidation.isUuid(text(active, "categoryId"))) put("categoryId", "请选择订阅类别。")
        }
        when (val money = MoneyParser.parseCny(text(active, "amount"))) {
            is MoneyParseResult.Success -> if (money.money.cents <= 0) put("amount", "真实付款金额必须大于 0；免费期不生成付款。")
            is MoneyParseResult.Failure -> put("amount", when (money.reason) {
                MoneyParseError.EXCESS_PRECISION -> "金额最多保留两位小数。"
                MoneyParseError.OUT_OF_RANGE -> "单笔金额超过允许范围。"
                MoneyParseError.INVALID_FORMAT -> "请输入正数人民币金额。"
            })
        }
        for (key in listOf("paidOn", "effectiveFrom", "paidUntilExclusive", "reminderDueOn")) if (date(active, key) == null) put(key, "请输入有效的真实日期，例如 2026-01-01。")
        date(active, "paidOn")?.let { if (it.isAfter(clock.today())) put("paidOn", "实际付款日期不能晚于记录时区的今天。") }
        val from = date(active, "effectiveFrom"); val until = date(active, "paidUntilExclusive")
        if (from != null && until != null && !from.isBefore(until)) put("paidUntilExclusive", "付费到期边界必须晚于实际生效日期。")
    }
    private fun newForm(kind: SubscriptionEditorKind, id: String) = Bundle().apply {
        putString("kind", kind.name); putString("subscriptionId", id)
        putString("commandId", UUID.randomUUID().toString()); putString("renewalId", UUID.randomUUID().toString())
        if (kind == SubscriptionEditorKind.CREATE) putString("cycle", BillingCycle.MONTHLY.name)
        putString("phase", SubmissionPhase.IDLE.name)
    }
    private fun phase(): SubmissionPhase = form?.getString("phase")?.let { raw -> SubmissionPhase.entries.firstOrNull { it.name == raw } } ?: SubmissionPhase.IDLE
    private fun needsInitialRead(active: Bundle) = active.getString("kind") != SubscriptionEditorKind.CREATE.name && !active.getBoolean("initialReadComplete") &&
        active.getBundle("submitted") == null && active.getString("phase") == SubmissionPhase.IDLE.name && !active.getBoolean("verifiedNoWrite")
    private fun change(key: String, value: String) {
        val active = form ?: return
        if (!canChange(active) || text(active, key) == value) return
        active.putString(key, value); active.putBoolean("dirty", true); errors = errors - key; message = null; publishForm()
    }
    private fun canChange(active: Bundle) = phase() == SubmissionPhase.IDLE && !loadingEditor && !needsInitialRead(active) &&
        !active.getBoolean("verifiedNoWrite") && !active.getBoolean("conflict") && !active.getBoolean("lastPayment")
    private fun publishForm() {
        savedState[FORM_KEY] = form?.let(::Bundle)
        val editor = form?.takeIf { active -> SubscriptionEditorKind.entries.any { it.name == text(active, "kind") } }?.let { active ->
            SubscriptionEditorUiState(SubscriptionEditorKind.valueOf(text(active, "kind")), text(active, "subscriptionId"), text(active, "serviceName"),
                text(active, "categoryId"), text(active, "note"), text(active, "amount"),
                BillingCycle.entries.firstOrNull { it.name == text(active, "cycle") } ?: BillingCycle.MONTHLY,
                text(active, "paidOn"), text(active, "effectiveFrom"), text(active, "paidUntilExclusive"), text(active, "reminderDueOn"),
                categories, clock.today(), phase(), active.getBoolean("dirty"), loadingEditor, errors, message, active.getBoolean("conflict"), needsInitialRead(active),
                verifiedCannotWrite = active.getBoolean("verifiedNoWrite"), origin = text(active, "origin").ifEmpty { "CURRENT" },
                renewalId = text(active, "renewalId"), fixedStopOn = active.getString("fixedStopOn"), reenableAction = active.getString("reenableAction"))
        }
        val stateEditor = form?.takeIf { active -> SubscriptionStateEditorKind.entries.any { it.name == text(active, "kind") } }?.let { active ->
            SubscriptionStateEditorUiState(SubscriptionStateEditorKind.valueOf(text(active, "kind")), text(active, "subscriptionId"),
                text(active, "serviceName"), text(active, "action"), text(active, "origin").ifEmpty { "CURRENT" }, text(active, "effectiveOn"),
                text(active, "stateValue").ifEmpty { active.getBundle("targetState")?.getString("state").orEmpty() },
                text(active, "reason"), text(active, "stateNote"), active.getString("fixedStopOn"),
                if (text(active, "kind") == "DELETE_RENEWAL") text(active, "renewalId") else text(active, "stateId"),
                text(active, "amount"), text(active, "paidOn"), clock.today(), phase(), active.getBoolean("dirty"), loadingEditor,
                errors, message, active.getBoolean("conflict"), needsInitialRead(active), active.getBoolean("verifiedNoWrite"), active.getBoolean("lastPayment"))
        }
        mutableState.value = state.value.copy(editor = editor, stateEditor = stateEditor)
    }
    private fun unconfirmed(active: Bundle, reason: String) { active.putString("phase", SubmissionPhase.UNCONFIRMED.name); message = reason; publishForm() }
    private fun validIdentity(active: Bundle): Boolean = try {
        fun known(bundle: Bundle) = (SubscriptionEditorKind.entries.any { it.name == text(bundle, "kind") } ||
            SubscriptionStateEditorKind.entries.any { it.name == text(bundle, "kind") }) &&
            listOf("subscriptionId", "renewalId", "commandId").all { StoredValueValidation.isUuid(text(bundle, it)) }
        val submitted = active.getBundle("submitted")
        fun complete(bundle: Bundle): Boolean {
            val kind = text(bundle, "kind")
            if (kind in listOf("CREATE", "RENEWAL")) return decodeRenewal(bundle).amountCents > 0
            val facts = SubscriptionFactsCodec.decode(requireNotNull(bundle.getBundle("factsBaseline")))
            require(facts.subscription.id == text(bundle, "subscriptionId"))
            bundle.getBundle("targetState")?.let { target ->
                require(SubscriptionFactsCodec.decodeState(target).subscriptionId == facts.subscription.id)
            }
            return true
        }
        known(active) && SubmissionPhase.entries.any { it.name == text(active, "phase") } &&
            (active.getBundle("factsBaseline") == null || SubscriptionFactsCodec.decode(requireNotNull(active.getBundle("factsBaseline"))).subscription.id == text(active, "subscriptionId")) &&
            (submitted == null || (known(submitted) && listOf("kind", "subscriptionId", "renewalId", "commandId").all { text(active, it) == text(submitted, it) } && complete(submitted))) &&
            (active.getString("phase") !in listOf(SubmissionPhase.IN_FLIGHT.name, SubmissionPhase.UNCONFIRMED.name, SubmissionPhase.COMMITTED.name) || submitted != null)
    } catch (_: Exception) { false }
    private fun text(bundle: Bundle, key: String) = bundle.getString(key).orEmpty()
    private fun date(bundle: Bundle, key: String): LocalDate? = try { LocalDate.parse(text(bundle, key).trim()) } catch (_: Exception) { null }
    private fun calendarEnd(from: LocalDate, cycle: BillingCycle): String = try {
        when (cycle) { BillingCycle.MONTHLY -> from.plusMonths(1); BillingCycle.QUARTERLY -> from.plusMonths(3); BillingCycle.YEARLY -> from.plusYears(1) }.toString()
    } catch (_: Exception) { "" }
    companion object {
        private const val FORM_KEY = "lithic.subscription.activeForm"
        private const val LAST_CONFIRMED_KEY = "lithic.subscription.lastConfirmedId"
        private const val LIST_ERROR = "订阅服务暂时无法读取，当前记录不会被当作空账本。"
        private const val DETAIL_ERROR = "订阅资料暂时无法读取，请重试读取。"
        private const val HISTORY_ERROR = "真实付款历史暂时无法读取，当前仍显示上次结果。"
        private const val COMMITTED_ERROR = "已保存，暂时无法读取。请重试读取，不必重新付款。"
    }
}

package com.cooper.lithic.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.core.design.ChartEvent
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.repository.ItemTimelineRepository
import com.cooper.lithic.data.repository.ProjectTimelineData
import com.cooper.lithic.data.repository.SubscriptionTimelineData
import com.cooper.lithic.domain.report.ProjectTimelineEngine
import com.cooper.lithic.domain.report.ProjectTimelinePoint
import com.cooper.lithic.domain.report.SubscriptionTimelineEngine
import com.cooper.lithic.domain.report.SubscriptionTimelinePoint
import java.time.LocalDate
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ItemChartKind { PROJECT, SUBSCRIPTION }

data class ItemTimelineUiState(
    val kind: ItemChartKind? = null, val id: String = "", val title: String = "",
    val project: ProjectTimelineData? = null, val subscription: SubscriptionTimelineData? = null,
    val selectedDate: LocalDate? = null,
    val selectedProject: ProjectTimelinePoint? = null,
    val selectedSubscription: SubscriptionTimelinePoint? = null,
    val events: List<ChartEvent> = emptyList(),
    val loading: Boolean = false, val error: String? = null, val missing: Boolean = false,
    /** A selected-day calculation uses retained facts, rather than completing a new read. */
    val readError: String? = null,
) {
    internal fun onReadFailed(): ItemTimelineUiState {
        val failure = "读取失败。已有图表为上次读取结果，请重试。"
        return copy(loading = false, error = failure, readError = failure)
    }

    internal fun onSelectingDate(date: LocalDate) = copy(selectedDate = date, selectedProject = null,
        selectedSubscription = null, error = readError)

    internal fun onPointFailed() = copy(error = listOfNotNull(readError, "该日计算失败，请重新读取。").joinToString(" "))

    /** A completed read replaces the retained dataset and its already calculated selection. */
    internal fun onReadSucceeded(fresh: ItemTimelineUiState) = fresh.copy(error = null, readError = null)
}

/** Owned by the existing generation UI scope. Tickets reject late CPU/IO results even if a
 * cooperative cancellation cannot interrupt them; no database read is made per display point. */
internal class ItemTimelineViewModel(
    private val repository: ItemTimelineRepository, private val clock: BusinessClock,
) : ViewModel() {
    private val mutable = MutableStateFlow(ItemTimelineUiState())
    val state = mutable.asStateFlow()
    private var active = false
    private var readTicket = 0L
    private var pointTicket = 0L
    private var readJob: Job? = null
    private var pointJob: Job? = null

    init { viewModelScope.launch { repository.changes.collect { if (active) refresh() } } }
    fun openProject(id: String) = open(ItemChartKind.PROJECT, id)
    fun openSubscription(id: String) = open(ItemChartKind.SUBSCRIPTION, id)
    private fun open(kind: ItemChartKind, id: String) {
        if (state.value.kind != kind || state.value.id != id) {
            readTicket++; pointTicket++; readJob?.cancel(); pointJob?.cancel()
            mutable.value = ItemTimelineUiState(kind = kind, id = id)
        }
        active = true
        refresh()
    }
    fun pause() { active = false; readTicket++; pointTicket++; readJob?.cancel(); pointJob?.cancel() }

    fun refresh() {
        if (!active) return
        val current = state.value
        val kind = current.kind ?: return
        val id = current.id
        val ticket = ++readTicket
        pointTicket++; pointJob?.cancel(); readJob?.cancel()
        val asOf = clock.today()
        mutable.value = current.copy(loading = true, error = null)
        readJob = viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    when (kind) {
                        ItemChartKind.PROJECT -> repository.loadProject(id, asOf)?.let {
                            ItemTimelineUiState(kind, id, it.detail.project.name, project = it, events = projectEvents(it))
                        }
                        ItemChartKind.SUBSCRIPTION -> repository.loadSubscription(id, asOf)?.let {
                            ItemTimelineUiState(kind, id, it.detail.subscription.name, subscription = it, events = subscriptionEvents(it))
                        }
                    }
                }
                if (ticket != readTicket || !active || state.value.kind != kind || state.value.id != id) return@launch
                if (loaded == null) {
                    pointTicket++; pointJob?.cancel()
                    mutable.value = ItemTimelineUiState(kind = kind, id = id, missing = true)
                    return@launch
                }
                val selected = state.value.selectedDate?.coerceAtMost(asOf) ?: asOf
                val selectionTicket = pointTicket
                val complete = withContext(Dispatchers.Default) {
                    coroutineContext.ensureActive()
                    val project = loaded.project?.let { ProjectTimelineEngine.point(it.snapshot, asOf, selected) }
                    coroutineContext.ensureActive()
                    val subscription = loaded.subscription?.let { SubscriptionTimelineEngine.point(it.snapshot, asOf, selected) }
                    coroutineContext.ensureActive()
                    loaded.copy(selectedDate = selected, selectedProject = project, selectedSubscription = subscription)
                }
                if (ticket != readTicket || !active || state.value.kind != kind || state.value.id != id) return@launch
                if (selectionTicket == pointTicket) {
                    pointTicket++; pointJob?.cancel()
                    // Dataset, date and exact result publish together; a completed refresh must
                    // never transiently forget the selection or label an old amount with a new day.
                    mutable.value = state.value.onReadSucceeded(complete)
                } else {
                    // A user chose another date during computation. Keep that newer intent,
                    // clear the previous dataset's point and calculate against the fresh snapshot.
                    val latest = state.value.selectedDate?.coerceAtMost(asOf) ?: asOf
                    mutable.value = state.value.onReadSucceeded(loaded.copy(selectedDate = latest))
                    selectDate(latest)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (ticket == readTicket && active) mutable.value = state.value.onReadFailed()
            }
        }
    }

    fun selectDate(date: LocalDate) {
        if (!active) return
        val captured = state.value
        val asOf = captured.project?.timeline?.asOf ?: captured.subscription?.timeline?.asOf ?: return
        if (date.isAfter(asOf)) {
            mutable.value = captured.copy(error = "只能观察截至 $asOf 的日期；未来计划不属于历史曲线。")
            return
        }
        if (captured.selectedDate == date && captured.error == null &&
            (captured.selectedProject?.date == date || captured.selectedSubscription?.date == date || pointJob?.isActive == true)) return
        val ticket = ++pointTicket
        val datasetTicket = readTicket
        pointJob?.cancel()
        mutable.value = captured.onSelectingDate(date)
        pointJob = viewModelScope.launch {
            try {
                val point = withContext(Dispatchers.Default) {
                    coroutineContext.ensureActive()
                    val project = captured.project?.let { ProjectTimelineEngine.point(it.snapshot, asOf, date) }
                    coroutineContext.ensureActive()
                    val subscription = captured.subscription?.let { SubscriptionTimelineEngine.point(it.snapshot, asOf, date) }
                    coroutineContext.ensureActive()
                    project to subscription
                }
                if (ticket == pointTicket && datasetTicket == readTicket && active &&
                    state.value.kind == captured.kind && state.value.id == captured.id &&
                    !state.value.missing && state.value.project === captured.project && state.value.subscription === captured.subscription) {
                    mutable.value = state.value.copy(selectedProject = point.first, selectedSubscription = point.second)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (ticket == pointTicket && datasetTicket == readTicket && active && !state.value.missing &&
                    state.value.project === captured.project && state.value.subscription === captured.subscription) mutable.value = state.value.onPointFailed()
            }
        }
    }

    private fun projectEvents(data: ProjectTimelineData): List<ChartEvent> = buildList {
        val detail = data.detail
        add(ChartEvent("acquired", detail.project.startedOn, "开始拥有", "取得日；付款以真实资金事件为准。" + if (detail.replacementLinks.isNotEmpty()) " 此项目存在替换关系；关系本身不计现金。" else ""))
        detail.project.endedOn?.let { add(ChartEvent("ended", it, "结束持有", "此后不增加持有天数；后续真实结算单列。")) }
        detail.events.forEach { event ->
            val role = when (event.role) {
                com.cooper.lithic.data.local.ProjectMoneyEventRole.INITIAL -> "初始投入"
                com.cooper.lithic.data.local.ProjectMoneyEventRole.REGULAR -> "后续资金事件"
                com.cooper.lithic.data.local.ProjectMoneyEventRole.END_SETTLEMENT -> "结束结算"
            }
            add(ChartEvent(event.id, event.occurredOn,
                if (event.direction == com.cooper.lithic.core.model.MoneyDirection.OUTLAY) "实际投入" else "实际收回",
                listOfNotNull(role, event.description?.takeIf(String::isNotBlank)).joinToString("；"),
                event.amountCents, nonCash = event.basis == com.cooper.lithic.data.local.MoneyBasis.TRADE_IN_CREDIT))
        }

    }.sortedWith(compareBy<ChartEvent> { it.date }.thenBy { it.id })

    private fun subscriptionEvents(data: SubscriptionTimelineData): List<ChartEvent> = buildList {
        data.detail.renewals.forEach { r ->
            add(ChartEvent("${r.id}-paid", r.paidOn, "真实付款", "${when (r.cycle) { com.cooper.lithic.domain.subscription.BillingCycle.MONTHLY -> "月付"; com.cooper.lithic.domain.subscription.BillingCycle.QUARTERLY -> "季付"; com.cooper.lithic.domain.subscription.BillingCycle.YEARLY -> "年付" }}；折算${r.standardDays}天；覆盖[${r.effectiveFrom}, ${r.paidUntilExclusive})", r.amountCents))
            add(ChartEvent("${r.id}-from", r.effectiveFrom, "付费覆盖生效", "按该期已存周期和标准天数计算；不是新增付款。"))
            add(ChartEvent("${r.id}-until", r.paidUntilExclusive, "付费覆盖结束", "排他边界；当日不属于上一期。"))
            if (r.reminderDueOn != r.paidUntilExclusive) add(ChartEvent("${r.id}-reminder", r.reminderDueOn,
                "提醒日期边界", "提醒边界不等于真实付款或自动续费。"))
        }
        data.detail.stateChanges.forEach { change ->
            val label = when (change.state) {
                com.cooper.lithic.domain.subscription.SubscriptionOperationState.ENABLED -> "启用"
                com.cooper.lithic.domain.subscription.SubscriptionOperationState.PAUSED -> "暂停"
                com.cooper.lithic.domain.subscription.SubscriptionOperationState.STOPPED -> "停止"
            }
            add(ChartEvent(change.id, change.effectiveOn, "状态变更：$label",
                listOfNotNull("按实际生效日影响状态；不产生付款。", change.reason?.takeIf(String::isNotBlank),
                    change.note?.takeIf(String::isNotBlank)).joinToString("；")))
        }
    }.sortedWith(compareBy<ChartEvent> { it.date }.thenBy { it.id })
}

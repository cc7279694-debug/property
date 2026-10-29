package com.cooper.lithic.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.repository.TotalCostTimelineRepository
import com.cooper.lithic.domain.report.CostTimeline
import com.cooper.lithic.domain.report.CostTimelineEngine
import com.cooper.lithic.domain.report.CostTimelinePoint
import com.cooper.lithic.domain.report.LedgerSnapshot
import com.cooper.lithic.domain.report.TimelineSampler
import com.cooper.lithic.domain.report.TotalCostEvent
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

data class TotalCostTrendUiState(
    val snapshot: LedgerSnapshot? = null,
    val timeline: CostTimeline? = null,
    val selectedDate: LocalDate? = null,
    val selectedPoint: CostTimelinePoint? = null,
    val events: List<TotalCostEvent> = emptyList(),
    val showBreakdown: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    /** Recalculating a date from retained facts cannot resolve a failed complete read. */
    val readError: String? = null,
) {
    internal fun onReadFailed(): TotalCostTrendUiState {
        val failure = "尚未刷新：读取或计算失败。已有图表仍为上次截止日的结果，请重试。"
        return copy(loading = false, error = failure, readError = failure)
    }

    internal fun onSelectingDate(date: LocalDate) = copy(selectedDate = date, selectedPoint = null,
        events = emptyList(), error = readError, notice = null)

    internal fun onPointFailed() = copy(error = listOfNotNull(readError, "该日计算失败，请重新读取。").joinToString(" "))

    internal fun onReadSucceeded(input: LedgerSnapshot, timeline: CostTimeline, selected: LocalDate,
        point: CostTimelinePoint, notice: String?) = TotalCostTrendUiState(input, timeline, selected, point,
        timeline.events.filter { it.date == selected }, showBreakdown, notice = notice, readError = null)
}

/** One existing generation owns this model. Read and selection tickets fence stale IO/CPU
 * completions; cancellation is cooperative and is never treated as proof of stopped work. */
internal class TotalCostTrendViewModel(
    private val repository: TotalCostTimelineRepository,
    private val clock: BusinessClock,
) : ViewModel() {
    private val mutable = MutableStateFlow(TotalCostTrendUiState())
    val state = mutable.asStateFlow()
    private var active = false
    private var readTicket = 0L
    private var pointTicket = 0L
    private var readJob: Job? = null
    private var pointJob: Job? = null

    init { viewModelScope.launch { repository.changes.collect { if (active) refresh() } } }

    fun open() { active = true; refresh() }
    fun pause() {
        active = false; readTicket++; pointTicket++
        readJob?.cancel(); pointJob?.cancel()
    }
    fun toggleBreakdown() { mutable.value = state.value.copy(showBreakdown = !state.value.showBreakdown) }

    fun refresh() {
        if (!active) return
        val ticket = ++readTicket
        pointTicket++; pointJob?.cancel(); readJob?.cancel()
        val asOf = clock.today()
        mutable.value = state.value.copy(loading = true, error = null, notice = null)
        readJob = viewModelScope.launch {
            try {
                val input = withContext(Dispatchers.IO) { repository.loadSnapshot(asOf) }
                val timeline = withContext(Dispatchers.Default) {
                    val context = coroutineContext
                    TimelineSampler.sample(input, asOf, cancellationCheck = { context.ensureActive() })
                }
                // A new selected date during either IO or CPU work is intent, not an old point
                // to copy. Re-evaluate against this captured ledger before publishing anything.
                while (active && ticket == readTicket) {
                    val requested = state.value.selectedDate
                    val selected = requested?.coerceAtMost(asOf) ?: asOf
                    val selection = pointTicket
                    val point = withContext(Dispatchers.Default) {
                        val context = coroutineContext
                        CostTimelineEngine.point(input, selected) { context.ensureActive() }
                    }
                    if (!active || ticket != readTicket) return@launch
                    if (selection != pointTicket) continue
                    mutable.value = state.value.onReadSucceeded(input, timeline, selected, point,
                        notice = if (requested != null && requested.isAfter(asOf))
                            "所选日期晚于当前截止日，已调整为 $asOf，并重新计算。" else null)
                    return@launch
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (active && ticket == readTicket) mutable.value = state.value.onReadFailed()
            }
        }
    }

    fun selectDate(date: LocalDate) {
        if (!active) return
        val captured = state.value
        val input = captured.snapshot ?: return
        val timeline = captured.timeline ?: return
        if (date.isAfter(timeline.asOf)) {
            mutable.value = captured.copy(notice = "只能观察截至 ${timeline.asOf} 的日期；未来计划不属于历史曲线。")
            return
        }
        if (captured.selectedDate == date && captured.error == null && captured.notice == null &&
            (captured.selectedPoint?.date == date || pointJob?.isActive == true)) return
        val ticket = ++pointTicket
        val dataset = readTicket
        pointJob?.cancel()
        mutable.value = captured.onSelectingDate(date)
        pointJob = viewModelScope.launch {
            try {
                val point = withContext(Dispatchers.Default) {
                    val context = coroutineContext
                    CostTimelineEngine.point(input, date) { context.ensureActive() }
                }
                if (active && ticket == pointTicket && dataset == readTicket && state.value.snapshot === input &&
                    state.value.timeline === timeline) {
                    mutable.value = state.value.copy(selectedPoint = point,
                        events = timeline.events.filter { it.date == date })
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (active && ticket == pointTicket && dataset == readTicket && state.value.snapshot === input &&
                    state.value.timeline === timeline) mutable.value = state.value.onPointFailed()
            }
        }
    }

    override fun onCleared() { pause(); super.onCleared() }
}

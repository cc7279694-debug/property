package com.cooper.lithic.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.repository.TotalCostTimelineRepository
import com.cooper.lithic.domain.report.*
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReportAnalysisUiState(
    val snapshot: LedgerSnapshot? = null,
    val asOf: LocalDate? = null,
    val selectedYear: Int? = null,
    val years: List<Int> = emptyList(),
    val annual: AnnualReport? = null,
    val categories: List<CategoryReport> = emptyList(),
    val annualTrend: List<AnnualReport> = emptyList(),
    val selectedKind: OverviewKind = OverviewKind.PROJECT,
    val selectedCategoryId: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
) {
    val selectedCategory: CategoryReport? get() = categories.firstOrNull {
        it.category.id == selectedCategoryId && it.category.kind == selectedKind
    }
}

/** One generation owns the model; selection intent is separate from published reports.
 * A year, category, observation date and amounts are only published as one captured result. */
internal class ReportAnalysisViewModel(
    private val repository: TotalCostTimelineRepository,
    private val clock: BusinessClock,
) : ViewModel() {
    private val mutable = MutableStateFlow(ReportAnalysisUiState())
    val state = mutable.asStateFlow()
    private var active = false
    private var readTicket = 0L
    private var selectionTicket = 0L
    private var requestedYear: Int? = null
    private var requestedKind = OverviewKind.PROJECT
    private var requestedCategoryId: String? = null
    private var selectionNotice: String? = null
    private var readJob: Job? = null
    private var selectionJob: Job? = null

    init { viewModelScope.launch { repository.changes.collect { if (active) refresh() } } }
    fun open() { active = true; refresh() }
    fun pause() {
        active = false; readTicket++; selectionTicket++
        readJob?.cancel(); selectionJob?.cancel()
    }
    fun refresh() {
        if (!active) return
        val ticket = ++readTicket
        selectionTicket++; readJob?.cancel(); selectionJob?.cancel()
        val asOf = clock.today()
        mutable.value = state.value.copy(loading = true)
        readJob = viewModelScope.launch {
            try {
                val input = withContext(Dispatchers.IO) { repository.loadSnapshot(asOf) }
                while (active && ticket == readTicket) {
                    val selection = selectionTicket
                    val intent = intent(asOf)
                    val result = withContext(Dispatchers.Default) { calculate(input, asOf, intent) }
                    if (!active || ticket != readTicket) return@launch
                    if (selection != selectionTicket) continue
                    publish(result)
                    return@launch
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (active && ticket == readTicket) mutable.value = state.value.copy(loading = false,
                    error = "尚未刷新：读取或计算失败。下方保留上次截止日和选择的结果，请重试。")
            }
        }
    }
    fun selectYear(year: Int) {
        val ceiling = state.value.asOf?.year ?: clock.today().year
        if (year !in 1..ceiling) {
            mutable.value = state.value.copy(notice = "请选择1至${ceiling}年的已过去年度。")
            return
        }
        if (requestedYear == year) return
        requestedYear = year; selectionNotice = null; changedSelection()
    }
    fun selectCategory(kind: OverviewKind, id: String?) {
        if (requestedKind == kind && requestedCategoryId == id) return
        requestedKind = kind; requestedCategoryId = id; selectionNotice = null; changedSelection()
    }
    private fun changedSelection() {
        val selection = ++selectionTicket
        selectionJob?.cancel()
        if (!active) return
        // A pending complete read must finish; selection never cancels it in favour of stale facts.
        if (readJob?.isActive == true) return
        val input = state.value.snapshot ?: return
        val observed = state.value.asOf ?: return
        val read = readTicket
        val intent = intent(observed)
        // Recomputing a selection does not make the retained database snapshot fresh.
        val retainedError = state.value.error
        mutable.value = state.value.copy(loading = true)
        selectionJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) { calculate(input, observed, intent) }
                if (active && read == readTicket && selection == selectionTicket && state.value.snapshot === input) {
                    publish(result.copy(error = retainedError))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (active && read == readTicket && selection == selectionTicket) mutable.value = state.value.copy(
                    loading = false, error = "所选年度或类别尚未刷新，请重新读取。")
            }
        }
    }
    private data class Intent(val year: Int, val kind: OverviewKind, val categoryId: String?, val notice: String?)
    private fun intent(asOf: LocalDate) = Intent((requestedYear ?: asOf.year).coerceAtMost(asOf.year), requestedKind, requestedCategoryId, selectionNotice)
    private fun calculate(input: LedgerSnapshot, asOf: LocalDate, intent: Intent): ReportAnalysisUiState {
        val categories = CategoryReportEngine.evaluate(input, asOf)
        val missing = intent.categoryId != null && categories.none { it.category.id == intent.categoryId && it.category.kind == intent.kind }
        val years = (AnnualReportEngine.years(input, asOf) + asOf.year + intent.year).distinct().sortedDescending()
        val trend = years.map { AnnualReportEngine.evaluate(input, it, asOf) }
        return ReportAnalysisUiState(snapshot = input, asOf = asOf, selectedYear = intent.year, years = years,
            annual = trend.single { it.year == intent.year }, categories = categories, annualTrend = trend,
            selectedKind = intent.kind, selectedCategoryId = if (missing) null else intent.categoryId,
            notice = if (missing) "所选类别已合并或删除，已返回该类型类别列表；历史按当前分类重算。" else intent.notice)
    }

    private fun publish(result: ReportAnalysisUiState) {
        requestedYear = result.selectedYear
        requestedKind = result.selectedKind
        requestedCategoryId = result.selectedCategoryId
        selectionNotice = result.notice
        mutable.value = result
    }
    override fun onCleared() { pause(); super.onCleared() }
}

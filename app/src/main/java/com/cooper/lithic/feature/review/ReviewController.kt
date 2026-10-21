package com.cooper.lithic.feature.review

import com.cooper.lithic.data.repository.ReviewPage
import com.cooper.lithic.data.repository.ReviewRanking
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Keeps one whole ranked window; stale reads never append to or replace a newer snapshot. */
internal class ReviewController(
    private val scope: CoroutineScope,
    changes: Flow<Unit>,
    private val loadWindow: suspend (ReviewRanking, Int) -> ReviewPage,
) {
    private val mutableState = MutableStateFlow(ReviewUiState())
    val state = mutableState.asStateFlow()
    private var requestSequence = 0L
    private var loadedPages = 1

    init {
        scope.launch {
            try { changes.collect { refresh() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                requestSequence++
                mutableState.value = state.value.copy(loading = false, error = READ_ERROR)
            }
        }
        refresh()
    }

    fun changeRanking(ranking: ReviewRanking) {
        if (ranking == state.value.ranking) return
        loadedPages = 1
        requestSequence++
        mutableState.value = ReviewUiState(ranking = ranking)
        request()
    }

    fun refresh() = request()

    fun loadMore() {
        if (state.value.loading || !state.value.hasMore) return
        loadedPages++
        request()
    }

    private fun request() {
        val ranking = state.value.ranking
        val pages = loadedPages
        val ticket = ++requestSequence
        mutableState.value = state.value.copy(loading = true, error = null)
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { loadWindow(ranking, pages) }
                if (ticket != requestSequence || state.value.ranking != ranking) return@launch
                check(result.ranking == ranking) { "A ranked window must match the requested view" }
                mutableState.value = state.value.copy(items = result.items, totalCount = result.totalCount,
                    rankableCount = result.rankableCount, asOf = result.asOf, loading = false,
                    hasMore = result.hasMore, error = null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (ticket == requestSequence && state.value.ranking == ranking) {
                    mutableState.value = state.value.copy(loading = false, error = READ_ERROR)
                }
            }
        }
    }

    companion object { private const val READ_ERROR = "长期复盘暂时无法读取，请重试。" }
}

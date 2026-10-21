package com.cooper.lithic.feature.home

import com.cooper.lithic.data.repository.SearchPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Rebuilds a requested window; no UI-level append can mix two database snapshots. */
internal class SearchController(
    private val scope: CoroutineScope,
    changes: Flow<Unit>,
    private val loadWindow: suspend (String, Int) -> SearchPage,
) {
    private val mutableState = MutableStateFlow(SearchUiState())
    val state = mutableState.asStateFlow()
    private var requestSequence = 0L
    private var loadedPages = 1

    init {
        scope.launch {
            try { changes.collect { refresh() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.value.copy(loading = false, error = READ_ERROR) }
        }
    }

    fun changeQuery(value: String) {
        if (value == state.value.query) return
        loadedPages = 1
        requestSequence++ // Invalidate an old read before the debounce or an empty query.
        mutableState.value = SearchUiState(query = value)
        if (value.isNotBlank()) request(debounce = true)
    }

    fun refresh() { if (state.value.query.isNotBlank()) request(debounce = false) }

    fun loadMore() {
        if (state.value.loading || !state.value.hasMore) return
        loadedPages++
        request(debounce = false)
    }

    private fun request(debounce: Boolean) {
        val query = state.value.query
        val pages = loadedPages
        val ticket = ++requestSequence
        mutableState.value = state.value.copy(loading = true, error = null)
        scope.launch {
            try {
                if (debounce) delay(180)
                if (ticket != requestSequence || state.value.query != query) return@launch
                val result = withContext(Dispatchers.IO) { loadWindow(query, pages) }
                if (ticket != requestSequence || state.value.query != query) return@launch
                mutableState.value = state.value.copy(items = result.items, totalCount = result.totalCount,
                    asOf = result.asOf, loading = false, hasMore = result.nextCursor != null, error = null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: IllegalArgumentException) {
                if (ticket == requestSequence && state.value.query == query) {
                    mutableState.value = state.value.copy(loading = false, error = "搜索名称最多 200 个字符，不能包含换行或控制字符。")
                }
            } catch (_: Exception) {
                if (ticket == requestSequence && state.value.query == query) {
                    mutableState.value = state.value.copy(loading = false, error = READ_ERROR)
                }
            }
        }
    }

    companion object { private const val READ_ERROR = "搜索结果暂时无法读取，请重新查找。" }
}

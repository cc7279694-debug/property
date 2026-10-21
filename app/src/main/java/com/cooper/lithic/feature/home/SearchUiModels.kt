package com.cooper.lithic.feature.home

import com.cooper.lithic.data.repository.SearchItem
import com.cooper.lithic.data.repository.SearchStatus
import java.time.LocalDate

internal data class SearchUiState(
    val query: String = "",
    val items: List<SearchItem> = emptyList(),
    val totalCount: Long = 0,
    val asOf: LocalDate? = null,
    val loading: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

internal fun searchStatusLabel(status: SearchStatus): String = when (status) {
    SearchStatus.PROJECT_NOT_STARTED -> "尚未开始"
    SearchStatus.PROJECT_CURRENT -> "持有中"
    SearchStatus.PROJECT_ENDED -> "已结束"
    SearchStatus.ACTIVE -> "订阅中"
    SearchStatus.PAUSED -> "已暂停"
    SearchStatus.EXPIRED -> "已到期"
    SearchStatus.STOPPED -> "已停止"
}

package com.cooper.lithic.feature.home

import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.SearchItem
import com.cooper.lithic.data.repository.SearchStatus
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class P02SearchPresentationTest {
    private val day = LocalDate.of(2026, 10, 25)
    @Test fun unqueried_or_pending_input_is_not_a_confirmed_empty_result() {
        assertFalse(SearchUiState(query = "名字").hasReadSnapshot)
        assertFalse(SearchUiState(query = "名字").isConfirmedEmptyResult)
        assertFalse(SearchUiState(query = "名字", loading = true).isConfirmedEmptyResult)
    }
    @Test fun successful_empty_read_is_confirmed_using_its_existing_business_date() {
        val state = SearchUiState(query = "名字", asOf = day)
        assertTrue(state.hasReadSnapshot)
        assertTrue(state.isConfirmedEmptyResult)
    }
    @Test fun refreshing_or_failed_zero_count_is_not_a_new_empty_result() {
        assertFalse(SearchUiState(query = "名字", asOf = day, loading = true).isConfirmedEmptyResult)
        assertFalse(SearchUiState(query = "名字", asOf = day, error = "无法读取").isConfirmedEmptyResult)
    }
    @Test fun retained_rows_are_a_snapshot_without_inventing_a_calculation_date() {
        val row = SearchItem(CategoryType.SUBSCRIPTION, "72000000-0000-0000-0000-000000000005",
            "合成订阅", "其他", SearchStatus.PAUSED, false)
        val state = SearchUiState(query = "订阅", items = listOf(row), totalCount = 1)
        assertTrue(state.hasReadSnapshot)
        assertFalse(state.isConfirmedEmptyResult)
        assertNull(state.asOf)
    }
}

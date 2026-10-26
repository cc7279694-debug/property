package com.cooper.lithic.feature.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.SearchItem
import com.cooper.lithic.data.repository.SearchStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** No runtime claim: this class is compiled, not executed in UI-C2 B. */
class C2B1SearchPresentationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun historical_result_keeps_type_status_and_exact_record_callback() {
        val row = SearchItem(CategoryType.SUBSCRIPTION, "same-id", "阅读服务", "阅读", SearchStatus.STOPPED, true)
        var opened: SearchItem? = null
        compose.setContent { LithicTheme { SearchScreen(SearchUiState(query = "阅读", items = listOf(row), totalCount = 1), {}, {}, {}, {}, { opened = it }) } }
        compose.onNodeWithTag("search-row-same-id").performClick()
        compose.runOnIdle { assertEquals(row, opened) }
        compose.onNodeWithText("订阅服务 · 阅读").assertExists()
        compose.onNodeWithTag("search-status-same-id", useUnmergedTree = true).assertTextEquals("已停止 · 历史记录")
    }
    @Test fun search_read_failure_stays_an_error_instead_of_empty_results() {
        compose.setContent { LithicTheme { SearchScreen(SearchUiState(query = "笔记本", error = "无法读取"), {}, {}, {}, {}, {}) } }
        compose.onNodeWithTag("search-error").assertTextEquals("无法读取")
        compose.onNodeWithTag("search-no-results").assertDoesNotExist()
        compose.onNodeWithTag("search-result-count").assertDoesNotExist()
    }
}

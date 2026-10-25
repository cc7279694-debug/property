package com.cooper.lithic.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Controlled component inputs only; no ledger Owner. Compilation is not an Android run. */
@RunWith(AndroidJUnit4::class)
class P02SearchExperienceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun pending_input_does_not_claim_a_successful_zero_match() {
        compose.setContent { LithicTheme { SearchScreen(SearchUiState(query = "名称"), {}, {}, {}, {}, {}) } }
        compose.onNodeWithTag("search-awaiting-read").assertExists()
        compose.onNodeWithTag("search-result-count").assertDoesNotExist()
        compose.onNodeWithTag("search-no-results").assertDoesNotExist()
    }
    @Test fun verified_empty_result_and_failed_zero_snapshot_are_distinct() {
        var state by mutableStateOf(SearchUiState(query = "名称", asOf = LocalDate.of(2026, 10, 25)))
        compose.setContent { LithicTheme { SearchScreen(state, {}, {}, {}, {}, {}) } }
        compose.onNodeWithTag("search-no-results").assertExists()
        compose.onNodeWithTag("search-result-count").assertTextContains("0", substring = true)
        compose.runOnIdle { state = state.copy(error = "读取失败") }
        compose.onNodeWithTag("search-no-results").assertDoesNotExist()
        compose.onNodeWithTag("search-result-count").assertDoesNotExist()
        compose.onNodeWithTag("search-stale").assertExists()
        compose.onNodeWithTag("search-retry").assertExists()
    }
    @Test fun loading_a_retained_snapshot_labels_it_and_blocks_duplicate_retry() {
        compose.setContent { LithicTheme {
            SearchScreen(SearchUiState(query = "名称", asOf = LocalDate.of(2026, 10, 25),
                loading = true, error = "旧错误", hasMore = true), {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("search-stale").assertExists()
        compose.onNodeWithTag("search-retry").assertIsNotEnabled()
        compose.onNodeWithTag("search-result-count").assertDoesNotExist()
        compose.onNodeWithTag("search-load-more").performScrollTo().assertIsNotEnabled()
    }
}

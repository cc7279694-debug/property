package com.cooper.lithic.feature.home

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.SearchItem
import com.cooper.lithic.data.repository.SearchStatus
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun result_status_and_history_are_visible_and_open_the_exact_record() {
        val item = SearchItem(CategoryType.SUBSCRIPTION, UUID.randomUUID().toString(), "已停测试服务", "数字服务", SearchStatus.STOPPED, true)
        var opened: SearchItem? = null
        compose.setContent { LithicTheme {
            SearchScreen(SearchUiState(query = "服务", items = listOf(item), totalCount = 1), {}, {}, {}, {}, { opened = it })
        } }
        compose.onNodeWithTag("search-status-${item.id}").assertTextContains("已停止").assertTextContains("历史记录")
        compose.onNodeWithTag("search-row-${item.id}").performClick()
        assertEquals(item, opened)
    }
}

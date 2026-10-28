package com.cooper.lithic.feature.project

import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.repository.ProjectDetail
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Read-only presentation facts; no pagination query, database, or Android host is started. */
class RecordingProjectListPresentationTest {
    @Test fun a_complete_single_page_has_no_end_paragraph_but_keeps_its_total() {
        listOf(1, 50).forEach { count ->
            val state = ready(count, count.toLong())
            assertNull(projectListPageStatus(state))
            assertEquals("长期项目共 $count 条，已显示 $count 条。", projectListCountDescription(state))
        }
    }

    @Test fun unknown_count_is_not_reported_as_an_empty_or_zero_total() {
        val unknown = ProjectListUiState(loading = false)
        assertTrue(projectListCountDescription(unknown).contains("总数量尚未取得"))
        assertFalse(projectListCountDescription(unknown).contains("共 0 条"))
        assertNull(projectListPageStatus(unknown))
        val empty = unknown.copy(totalCount = 0)
        assertEquals("长期项目共 0 条，已显示 0 条。", projectListCountDescription(empty))
        assertNull(projectListPageStatus(empty))
    }

    @Test fun real_continuation_and_next_page_loading_keep_precise_progress() {
        val state = ready(50, 51).copy(hasMore = true)
        assertEquals("已显示 50 / 51 条，可继续加载", projectListPageStatus(state))
        val busy = state.copy(loadingMore = true)
        assertEquals("正在读取下一页；已显示 50 / 51 条", projectListPageStatus(busy))
        assertTrue(projectListCountDescription(busy).contains("数量为上次读取结果"))
        assertEquals(true, state.hasMore)
        assertEquals(false, state.loadingMore)
    }

    @Test fun failed_or_refreshing_reads_qualify_old_counts_without_claiming_completion() {
        val state = ready(1, 1)
        listOf(state.copy(error = "合成读取失败"), state.copy(loading = true)).forEach { pending ->
            assertNull(projectListPageStatus(pending))
            assertEquals("长期项目共 1 条，已显示 1 条。 数量为上次读取结果。", projectListCountDescription(pending))
        }
        assertTrue(projectListCountDescription(ProjectListUiState(totalCount = 0, loading = true))
            .contains("数量为上次读取结果"))
    }

    @Test fun a_finished_multiple_page_list_keeps_its_real_end_and_total() {
        val state = ready(51, 51)
        assertEquals("已显示 51 条，已到列表末尾", projectListPageStatus(state))
        assertEquals("长期项目共 51 条，已显示 51 条。", projectListCountDescription(state))
    }

    @Test fun mismatched_counts_cannot_authorize_a_single_page_completion_claim() {
        val state = ready(1, 51)
        assertEquals("已显示 1 / 51 条，数量尚待核对", projectListPageStatus(state))
        assertFalse(requireNotNull(projectListPageStatus(state)).contains("末尾"))
        assertFalse(projectListCountDescription(state).contains("共 1 条"))
    }

    private fun ready(count: Int, total: Long): ProjectListUiState {
        val start = LocalDate.of(2026, 1, 1)
        val stamp = Instant.parse("2026-01-01T00:00:00Z")
        val category = CategoryEntity("73000000-0000-0000-0000-000000000999", CategoryType.PROJECT, "其他", "其他", true)
        return ProjectListUiState((1..count).map { index ->
            ProjectDetail(LongTermProjectEntity("73000000-0000-0000-0000-${index.toString().padStart(12, '0')}",
                "合成长期项目", category.id, start, createdAt = stamp, updatedAt = stamp), category,
                emptyList(), ProjectCostResult(10, 10_000, 0, 10_000, BigDecimal.TEN, true, null), start.plusDays(9))
        }, totalCount = total, loading = false)
    }
}

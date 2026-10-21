package com.cooper.lithic.feature.home

import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.SearchItem
import com.cooper.lithic.data.repository.SearchPage
import com.cooper.lithic.data.repository.SearchStatus
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class SearchControllerTest {
    @Test fun delayed_old_query_cannot_replace_new_query_results() = runBlocking {
        val oldRequest = CompletableDeferred<Unit>()
        val oldResult = CompletableDeferred<SearchPage>()
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = SearchController(scope, emptyFlow()) { query, _ ->
                if (query == "旧") { oldRequest.complete(Unit); oldResult.await() }
                else page(query, listOf(item("新结果")))
            }
            model.changeQuery("旧")
            withTimeout(3_000) { oldRequest.await() }
            model.changeQuery("新")
            withTimeout(3_000) { model.state.first { it.items.singleOrNull()?.name == "新结果" } }
            oldResult.complete(page("旧", listOf(item("旧结果"))))
            kotlinx.coroutines.yield()
            assertEquals("新", model.state.value.query)
            assertEquals("新结果", model.state.value.items.single().name)
        } finally { scope.cancel() }
    }

    @Test fun clearing_query_immediately_clears_old_results_without_browsing_everything() = runBlocking {
        var calls = 0
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = SearchController(scope, emptyFlow()) { query, _ -> calls++; page(query, listOf(item("相机"))) }
            model.changeQuery("相机")
            withTimeout(3_000) { model.state.first { it.items.isNotEmpty() } }
            model.changeQuery("   ")
            assertTrue(model.state.value.items.isEmpty())
            assertFalse(model.state.value.loading)
            assertEquals(1, calls)
        } finally { scope.cancel() }
    }

    @Test fun read_failure_keeps_last_same_query_snapshot_with_visible_error() = runBlocking {
        var fail = false
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = SearchController(scope, emptyFlow()) { query, _ ->
                if (fail) error("storage unavailable")
                page(query, listOf(item("相机")))
            }
            model.changeQuery("相机")
            withTimeout(3_000) { model.state.first { it.items.isNotEmpty() } }
            fail = true; model.refresh()
            withTimeout(3_000) { model.state.first { it.error != null } }
            assertEquals("相机", model.state.value.items.single().name)
            assertFalse(model.state.value.loading)
        } finally { scope.cancel() }
    }

    @Test fun invalidation_replaces_results_instead_of_appending_an_old_snapshot() = runBlocking {
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        var latest = listOf(item("原名称"))
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = SearchController(scope, changes) { query, _ -> page(query, latest) }
            model.changeQuery("名")
            withTimeout(3_000) { model.state.first { it.items.singleOrNull()?.name == "原名称" } }
            withTimeout(3_000) { changes.subscriptionCount.first { it > 0 } }
            latest = listOf(item("新名称"))
            changes.emit(Unit)
            withTimeout(3_000) { model.state.first { it.items.singleOrNull()?.name == "新名称" } }
            assertEquals(1, model.state.value.items.size)
        } finally { scope.cancel() }
    }

    private fun item(name: String) = SearchItem(CategoryType.PROJECT, UUID.randomUUID().toString(), name,
        "设备", SearchStatus.PROJECT_CURRENT, false)
    private fun page(query: String, items: List<SearchItem>) = SearchPage(items, null, items.size.toLong(), LocalDate.of(2026, 10, 21), query)
}

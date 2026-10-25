package com.cooper.lithic.feature.home

import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.SearchItem
import com.cooper.lithic.data.repository.SearchPage
import com.cooper.lithic.data.repository.SearchStatus
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class P02SearchStateTest {
    private val day = LocalDate.of(2026, 10, 25)
    private val row = SearchItem(CategoryType.PROJECT, "72000000-0000-0000-0000-000000000004",
        "旧名称", "其他", SearchStatus.PROJECT_CURRENT, false)
    private fun page(query: String, items: List<SearchItem>) = SearchPage(items, null, items.size.toLong(), day, query)

    @Test fun new_query_immediately_drops_previous_query_rows_and_count() = runBlocking {
        val pending = CompletableDeferred<SearchPage>()
        val started = CompletableDeferred<Unit>()
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val c = SearchController(scope, emptyFlow()) { query, _ ->
                if (query == "新") { started.complete(Unit); pending.await() } else page(query, listOf(row))
            }
            c.changeQuery("旧")
            withTimeout(3_000) { c.state.first { !it.loading && it.items.isNotEmpty() } }
            c.changeQuery("新")
            assertEquals("新", c.state.value.query)
            assertTrue(c.state.value.items.isEmpty())
            assertEquals(0L, c.state.value.totalCount)
            assertNull(c.state.value.asOf)
            assertTrue(c.state.value.loading)
            withTimeout(3_000) { started.await() }
            pending.complete(page("新", emptyList()))
            withTimeout(3_000) { c.state.first { !it.loading } }
            assertEquals(day, c.state.value.asOf)
        } finally { scope.cancel() }
    }
    @Test fun same_query_refresh_keeps_its_snapshot_until_the_read_finishes() = runBlocking {
        val pending = CompletableDeferred<SearchPage>()
        var calls = 0
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val c = SearchController(scope, emptyFlow()) { query, _ ->
                if (++calls == 1) page(query, listOf(row)) else pending.await()
            }
            c.changeQuery("旧")
            withTimeout(3_000) { c.state.first { !it.loading && it.items.isNotEmpty() } }
            c.refresh()
            assertTrue(c.state.value.loading)
            assertEquals(listOf(row), c.state.value.items)
            assertEquals(day, c.state.value.asOf)
            assertNull(c.state.value.error)
            pending.complete(page("旧", emptyList()))
            withTimeout(3_000) { c.state.first { !it.loading } }
            assertTrue(c.state.value.items.isEmpty())
            assertEquals(0L, c.state.value.totalCount)
        } finally { scope.cancel() }
    }
    @Test fun cleared_query_cannot_receive_a_late_previous_completion() = runBlocking {
        val pending = CompletableDeferred<SearchPage>()
        val started = CompletableDeferred<Unit>()
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val c = SearchController(scope, emptyFlow()) { _, _ -> started.complete(Unit); pending.await() }
            c.changeQuery("旧")
            withTimeout(3_000) { started.await() }
            c.changeQuery("")
            pending.complete(page("旧", listOf(row)))
            withTimeout(3_000) { scope.coroutineContext[Job]!!.children.toList().joinAll() }
            assertEquals("", c.state.value.query)
            assertTrue(c.state.value.items.isEmpty())
            assertFalse(c.state.value.loading)
            assertNull(c.state.value.asOf)
        } finally { scope.cancel() }
    }
}

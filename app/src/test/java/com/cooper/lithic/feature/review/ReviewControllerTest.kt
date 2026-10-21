package com.cooper.lithic.feature.review

import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.ReviewPage
import com.cooper.lithic.data.repository.ReviewRankItem
import com.cooper.lithic.data.repository.ReviewRanking
import com.cooper.lithic.domain.report.RankedProject
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
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

class ReviewControllerTest {
    private val today = LocalDate.of(2026, 10, 21)

    @Test fun initial_read_distinguishes_a_real_empty_snapshot_from_an_unread_screen() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = ReviewController(scope, emptyFlow()) { ranking, pages ->
                assertEquals(1, pages)
                page(ranking, emptyList())
            }
            val result = withTimeout(3_000) { model.state.first { it.totalCount == 0L && !it.loading } }
            assertEquals(today, result.asOf)
            assertEquals(0L, result.rankableCount)
            assertNull(result.error)
        } finally { scope.cancel() }
    }

    @Test fun switching_view_fences_a_late_previous_view_and_resets_the_window() = runBlocking {
        val oldStarted = CompletableDeferred<Unit>()
        val oldResult = CompletableDeferred<ReviewPage>()
        val oldReturned = CompletableDeferred<Unit>()
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = ReviewController(scope, emptyFlow()) { ranking, pages ->
                if (ranking == ReviewRanking.PROJECT_TIME) {
                    oldStarted.complete(Unit)
                    oldResult.await().also { oldReturned.complete(Unit) }
                } else {
                    assertEquals(1, pages)
                    page(ranking, listOf(item("new", "新榜记录")))
                }
            }
            withTimeout(3_000) { oldStarted.await() }
            model.changeRanking(ReviewRanking.PROJECT_COST)
            assertTrue(model.state.value.items.isEmpty())
            withTimeout(3_000) { model.state.first { !it.loading && it.items.singleOrNull()?.id == "new" } }
            oldResult.complete(page(ReviewRanking.PROJECT_TIME, listOf(item("old", "旧榜记录"))))
            withTimeout(3_000) { oldReturned.await() }
            kotlinx.coroutines.yield()
            assertEquals(ReviewRanking.PROJECT_COST, model.state.value.ranking)
            assertEquals("new", model.state.value.items.single().id)
        } finally { scope.cancel() }
    }

    @Test fun load_more_and_refresh_replace_the_entire_loaded_window() = runBlocking {
        val phase = AtomicInteger(0)
        val requestedPages = AtomicInteger(0)
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = ReviewController(scope, emptyFlow()) { ranking, pages ->
                requestedPages.set(pages)
                val rows = (1..minOf(pages * 50, 75)).map { item("$it", "版本${phase.get()}-$it") }
                page(ranking, rows).copy(totalCount = 75, rankableCount = 75, hasMore = pages == 1)
            }
            withTimeout(3_000) { model.state.first { it.items.size == 50 && !it.loading } }
            model.loadMore()
            withTimeout(3_000) { model.state.first { it.items.size == 75 && !it.loading } }
            phase.set(1); model.refresh()
            withTimeout(3_000) { model.state.first { it.items.firstOrNull()?.name == "版本1-1" && !it.loading } }
            assertEquals(2, requestedPages.get())
            assertEquals(75, model.state.value.items.map { it.id }.distinct().size)
            assertFalse(model.state.value.hasMore)
        } finally { scope.cancel() }
    }

    @Test fun read_failure_preserves_the_previous_snapshot_and_an_explicit_error() = runBlocking {
        val fail = AtomicBoolean(false)
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = ReviewController(scope, emptyFlow()) { ranking, _ ->
                if (fail.get()) error("read failed")
                page(ranking, listOf(item("camera", "相机")))
            }
            withTimeout(3_000) { model.state.first { it.items.size == 1 && !it.loading } }
            fail.set(true); model.refresh()
            val result = withTimeout(3_000) { model.state.first { it.error != null } }
            assertEquals("camera", result.items.single().id)
            assertEquals(1L, result.totalCount)
            assertEquals(today, result.asOf)
            assertFalse(result.loading)
        } finally { scope.cancel() }
    }

    @Test fun invalidation_recomputes_a_same_view_instead_of_appending_stale_rows() = runBlocking {
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val rows = AtomicReference(listOf(item("first", "更正前")))
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = ReviewController(scope, changes) { ranking, _ -> page(ranking, rows.get()) }
            withTimeout(3_000) { model.state.first { it.items.singleOrNull()?.name == "更正前" } }
            withTimeout(3_000) { changes.subscriptionCount.first { it > 0 } }
            rows.set(listOf(item("second", "更正后")))
            changes.emit(Unit)
            withTimeout(3_000) { model.state.first { it.items.singleOrNull()?.name == "更正后" } }
            assertEquals(listOf("second"), model.state.value.items.map { it.id })
        } finally { scope.cancel() }
    }

    @Test fun a_late_same_view_read_cannot_replace_a_new_business_day() = runBlocking {
        val calls = AtomicInteger(0)
        val delayedStarted = CompletableDeferred<Unit>()
        val delayedResult = CompletableDeferred<ReviewPage>()
        val delayedReturned = CompletableDeferred<Unit>()
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = ReviewController(scope, emptyFlow()) { ranking, _ -> when (calls.incrementAndGet()) {
                1 -> page(ranking, listOf(item("camera", "相机")))
                2 -> { delayedStarted.complete(Unit); delayedResult.await().also { delayedReturned.complete(Unit) } }
                else -> page(ranking, listOf(item("camera", "相机"))).copy(asOf = today.plusDays(1))
            } }
            withTimeout(3_000) { model.state.first { it.asOf == today && !it.loading } }
            model.refresh(); withTimeout(3_000) { delayedStarted.await() }
            model.refresh()
            withTimeout(3_000) { model.state.first { it.asOf == today.plusDays(1) && !it.loading } }
            delayedResult.complete(page(ReviewRanking.PROJECT_TIME, listOf(item("old", "旧观察日"))))
            withTimeout(3_000) { delayedReturned.await() }
            kotlinx.coroutines.yield()
            assertEquals(today.plusDays(1), model.state.value.asOf)
            assertEquals("camera", model.state.value.items.single().id)
        } finally { scope.cancel() }
    }

    private fun item(id: String, name: String): ReviewRankItem {
        val cost = ProjectCostResult(100, 10_000, 0, 10_000, BigDecimal.ONE, true, null)
        return ReviewRankItem(CategoryType.PROJECT, id, name, "设备", RankedProject(id, "devices", cost, false))
    }
    private fun page(ranking: ReviewRanking, items: List<ReviewRankItem>) =
        ReviewPage(items, items.size.toLong(), items.size.toLong(), today, ranking, false)
}

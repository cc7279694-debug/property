package com.cooper.lithic.feature.category

import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryNames
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.CategoryEntry
import com.cooper.lithic.data.repository.CategoryMergePreview
import com.cooper.lithic.data.repository.CategoryMutationResult
import com.cooper.lithic.feature.project.LeaveDecision
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class CategoryControllerTest {
    @Test fun pending_write_blocks_leave_and_cannot_be_discarded_or_duplicated() = runBlocking {
        val writing = CompletableDeferred<Unit>()
        val result = CompletableDeferred<CategoryMutationResult>()
        val calls = mutableListOf<String>()
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = model(scope, create = { _, _, id -> calls += id; writing.complete(Unit); result.await() })
            model.startCreate(); model.changeName("设备"); model.save()
            withTimeout(3_000) { writing.await() }
            assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
            model.save(); model.dismissEditor(); model.discardForm()
            assertNotNull(model.state.value.editor)
            assertEquals(1, calls.size)
            result.complete(CategoryMutationResult.Applied(calls.single()))
            withTimeout(3_000) { model.state.first { it.editor == null } }
            assertEquals(LeaveDecision.ALLOW, model.leaveDecision())
        } finally { scope.cancel() }
    }

    @Test fun unknown_write_keeps_original_command_and_id_for_safe_retry() = runBlocking {
        val attempts = mutableListOf<Triple<CategoryType, String, String>>()
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = model(scope, create = { type, name, id ->
                attempts += Triple(type, name, id)
                if (attempts.size == 1) CategoryMutationResult.StorageFailure("uncertain")
                else CategoryMutationResult.Applied(id, replayed = true)
            })
            model.startCreate(); model.changeName("设备"); model.save()
            withTimeout(3_000) { model.state.first { it.editor?.uncertain == true } }
            assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
            model.changeName("另一个名字"); model.selectType(CategoryType.SUBSCRIPTION); model.dismissEditor()
            assertEquals("设备", model.state.value.editor?.name)
            assertEquals(CategoryType.PROJECT, model.state.value.type)
            model.save()
            withTimeout(3_000) { model.state.first { it.editor == null } }
            assertEquals(2, attempts.size)
            assertEquals(attempts[0], attempts[1])
        } finally { scope.cancel() }
    }

    @Test fun fallback_cannot_open_a_rename_or_merge_source_editor() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = model(scope)
            val fallback = category("其他", fallback = true)
            model.startRename(fallback)
            assertNull(model.state.value.editor)
            model.startMerge(fallback)
            assertNull(model.state.value.editor)
        } finally { scope.cancel() }
    }

    @Test fun merge_requires_read_preview_then_explicit_confirmation() = runBlocking {
        val source = category("设备")
        val target = category("数码")
        val expected = CategoryMergePreview(source, target, 7)
        val merged = mutableListOf<CategoryMergePreview>()
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = model(scope, read = { listOf(CategoryEntry(source, 7), CategoryEntry(target, 2)) },
                preview = { _, _ -> expected }, merge = { merged += it; CategoryMutationResult.Applied(target.id, 7) })
            withTimeout(3_000) { model.state.first { !it.loading } }
            model.startMerge(source); model.selectTarget(target.id)
            model.confirmMerge()
            assertTrue(merged.isEmpty())
            model.previewMerge()
            withTimeout(3_000) { model.state.first { it.editor?.preview != null } }
            assertEquals(7L, model.state.value.editor?.preview?.affectedCount)
            assertTrue(merged.isEmpty())
            model.confirmMerge()
            withTimeout(3_000) { model.state.first { it.editor == null } }
            assertEquals(listOf(expected), merged)
        } finally { scope.cancel() }
    }

    @Test fun stale_type_read_does_not_replace_current_category_type() = runBlocking {
        val oldRead = CompletableDeferred<List<CategoryEntry>>()
        val requested = CompletableDeferred<Unit>()
        val subscription = category("服务", CategoryType.SUBSCRIPTION)
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val model = model(scope, read = { type ->
                if (type == CategoryType.PROJECT) { requested.complete(Unit); oldRead.await() }
                else listOf(CategoryEntry(subscription, 1))
            })
            withTimeout(3_000) { requested.await() }
            model.selectType(CategoryType.SUBSCRIPTION)
            withTimeout(3_000) { model.state.first { it.items.singleOrNull()?.category?.id == subscription.id } }
            oldRead.complete(listOf(CategoryEntry(category("旧结果"), 4)))
            kotlinx.coroutines.yield()
            assertEquals(CategoryType.SUBSCRIPTION, model.state.value.type)
            assertEquals(subscription.id, model.state.value.items.single().category.id)
        } finally { scope.cancel() }
    }

    private fun model(
        scope: CoroutineScope,
        read: suspend (CategoryType) -> List<CategoryEntry> = { emptyList() },
        create: suspend (CategoryType, String, String) -> CategoryMutationResult = { _, _, id -> CategoryMutationResult.Applied(id) },
        preview: suspend (String, String) -> CategoryMergePreview = { _, _ -> error("unexpected preview") },
        merge: suspend (CategoryMergePreview) -> CategoryMutationResult = { error("unexpected merge") },
    ) = CategoryController(scope, emptyFlow(), read, create,
        { expected, _ -> CategoryMutationResult.Applied(expected.id) }, preview, merge)

    private fun category(name: String, type: CategoryType = CategoryType.PROJECT, fallback: Boolean = false) =
        CategoryEntity(UUID.randomUUID().toString(), type, name, CategoryNames.normalized(name), fallback)
}

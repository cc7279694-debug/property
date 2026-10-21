package com.cooper.lithic.feature.category

import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.CategoryEntry
import com.cooper.lithic.data.repository.CategoryMergePreview
import com.cooper.lithic.data.repository.CategoryMutationResult
import com.cooper.lithic.feature.project.LeaveDecision
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Generation-scoped UI requests; all writes still run through the owning Repository. */
internal class CategoryController(
    private val scope: CoroutineScope,
    changes: Flow<Unit>,
    private val read: suspend (CategoryType) -> List<CategoryEntry>,
    private val create: suspend (CategoryType, String, String) -> CategoryMutationResult,
    private val rename: suspend (CategoryEntity, String) -> CategoryMutationResult,
    private val preview: suspend (String, String) -> CategoryMergePreview,
    private val merge: suspend (CategoryMergePreview) -> CategoryMutationResult,
) {
    private val mutableState = MutableStateFlow(CategoryManagementUiState())
    val state = mutableState.asStateFlow()
    private var readSequence = 0L
    private var editorEpoch = 0L

    init {
        refresh()
        scope.launch {
            try { changes.collect { refresh() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.value.copy(loading = false, error = READ_ERROR) }
        }
    }

    fun refresh() {
        val requestedType = state.value.type
        val request = ++readSequence
        mutableState.value = state.value.copy(loading = true, error = null)
        scope.launch {
            try {
                val items = withContext(Dispatchers.IO) { read(requestedType) }
                if (request != readSequence || state.value.type != requestedType) return@launch
                mutableState.value = state.value.copy(items = items, loading = false, error = null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (request == readSequence && state.value.type == requestedType) {
                    mutableState.value = state.value.copy(loading = false, error = READ_ERROR)
                }
            }
        }
    }

    fun selectType(type: CategoryType) {
        if (state.value.editor != null) return
        mutableState.value = state.value.copy(type = type, items = emptyList(), message = null)
        refresh()
    }

    fun startCreate() = openEditor(CategoryEditorUiState(CategoryEditMode.CREATE, UUID.randomUUID().toString()))

    fun startRename(category: CategoryEntity) {
        if (category.isFallback || category.type != state.value.type) return
        openEditor(CategoryEditorUiState(CategoryEditMode.RENAME, category.id, source = category, name = category.name))
    }

    fun startMerge(category: CategoryEntity) {
        if (category.isFallback || category.type != state.value.type) return
        openEditor(CategoryEditorUiState(CategoryEditMode.MERGE, category.id, source = category))
    }

    private fun openEditor(editor: CategoryEditorUiState) {
        if (state.value.editor != null) return
        editorEpoch++
        mutableState.value = state.value.copy(editor = editor, message = null)
    }

    fun changeName(value: String) {
        val editor = state.value.editor ?: return
        if (!editor.canChange || editor.mode == CategoryEditMode.MERGE) return
        mutableState.value = state.value.copy(editor = editor.copy(name = value, message = null))
    }

    fun selectTarget(id: String) {
        val editor = state.value.editor ?: return
        if (!editor.canChange || editor.mode != CategoryEditMode.MERGE) return
        if (id == editor.categoryId || state.value.items.none { it.category.id == id && it.category.type == state.value.type }) return
        mutableState.value = state.value.copy(editor = editor.copy(targetId = id, preview = null, message = null))
    }

    fun previewMerge() {
        val editor = state.value.editor ?: return
        val target = editor.targetId ?: return
        if (!editor.canChange || editor.mode != CategoryEditMode.MERGE) return
        val ticket = editorEpoch
        mutableState.value = state.value.copy(editor = editor.copy(busy = true, message = null))
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { preview(editor.categoryId, target) }
                if (ticket != editorEpoch) return@launch
                mutableState.value = state.value.copy(editor = editor.copy(preview = result, busy = false))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (invalid: IllegalArgumentException) {
                if (ticket == editorEpoch) mutableState.value = state.value.copy(editor = editor.copy(message = invalid.message ?: "请重新选择合并范围。"))
            } catch (_: Exception) {
                if (ticket == editorEpoch) mutableState.value = state.value.copy(editor = editor.copy(message = "无法读取合并范围，请重试。"))
            }
        }
    }

    fun save() {
        val editor = state.value.editor ?: return
        if (editor.busy || editor.conflict || editor.mode == CategoryEditMode.MERGE) return
        val type = state.value.type
        submit(editor) {
            if (editor.mode == CategoryEditMode.CREATE) create(type, editor.name, editor.categoryId)
            else rename(requireNotNull(editor.source), editor.name)
        }
    }

    fun confirmMerge() {
        val editor = state.value.editor ?: return
        val confirmation = editor.preview ?: return
        if (editor.busy || editor.conflict || editor.mode != CategoryEditMode.MERGE) return
        submit(editor) { merge(confirmation) }
    }

    private fun submit(editor: CategoryEditorUiState, operation: suspend () -> CategoryMutationResult) {
        val ticket = editorEpoch
        mutableState.value = state.value.copy(editor = editor.copy(busy = true, message = null))
        scope.launch {
            val result = try { withContext(Dispatchers.IO) { operation() } }
            catch (cancelled: CancellationException) {
                if (ticket == editorEpoch) mutableState.value = state.value.copy(editor = editor.copy(busy = false, uncertain = true,
                    message = "提交已中断，结果尚未确认。请重新确认同一次提交。"))
                throw cancelled
            } catch (_: Exception) { CategoryMutationResult.StorageFailure("提交结果尚未确认，请重新确认同一次提交。") }
            if (ticket != editorEpoch) return@launch
            when (result) {
                is CategoryMutationResult.Applied -> {
                    editorEpoch++
                    mutableState.value = state.value.copy(editor = null,
                        message = if (editor.mode == CategoryEditMode.MERGE) "已合并 ${result.affectedCount} 条记录，金额保持不变。" else "类别已保存。")
                    refresh()
                }
                is CategoryMutationResult.ValidationFailure -> mutableState.value = state.value.copy(
                    editor = editor.copy(busy = false, uncertain = false, message = result.reason))
                is CategoryMutationResult.Conflict -> mutableState.value = state.value.copy(
                    editor = editor.copy(busy = false, uncertain = false, conflict = true, message = result.reason + " 请取消后重新读取类别。"))
                is CategoryMutationResult.StorageFailure -> mutableState.value = state.value.copy(
                    editor = editor.copy(busy = false, uncertain = true, message = result.message))
            }
        }
    }

    fun dismissEditor() {
        val editor = state.value.editor ?: return
        if (editor.busy || editor.uncertain) return
        editorEpoch++
        mutableState.value = state.value.copy(editor = null)
        refresh()
    }

    fun discardForm() = dismissEditor()

    fun leaveDecision(): LeaveDecision {
        val editor = state.value.editor ?: return LeaveDecision.ALLOW
        return if (editor.busy || editor.uncertain) LeaveDecision.BLOCK_PENDING else LeaveDecision.CONFIRM_DISCARD
    }

    companion object { private const val READ_ERROR = "类别暂时无法读取，请重试。原有记录未被清空。" }
}

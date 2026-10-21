package com.cooper.lithic.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryNames
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.StoredValueValidation
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CategoryEntry(val category: CategoryEntity, val recordCount: Long)

/** A confirmation refers to these exact category identities and current affected row count. */
data class CategoryMergePreview(val source: CategoryEntity, val target: CategoryEntity, val affectedCount: Long)

sealed interface CategoryMutationResult {
    data class Applied(val categoryId: String, val affectedCount: Long = 0, val replayed: Boolean = false) : CategoryMutationResult
    data class ValidationFailure(val reason: String) : CategoryMutationResult
    data class Conflict(val reason: String) : CategoryMutationResult
    data class StorageFailure(val message: String) : CategoryMutationResult
}

/** Non-owning facade: category changes share the existing generation write and backup boundary. */
open class CategoryRepository(private val db: LithicDatabase, private val coordinator: LocalWriteCoordinator) {
    open val changes: Flow<Unit> = db.invalidationTracker.createFlow(
        "categories", "long_term_projects", "subscriptions",
    ).map { Unit }

    open suspend fun list(type: CategoryType): List<CategoryEntry> = coordinator.withSnapshot {
        db.withTransaction {
            db.requireDefaults()
            db.categoryDao().listWithUsage(type).map { CategoryEntry(it.category, it.recordCount) }
        }
    }

    /** Reusing the supplied identity is safe only when the exact persisted row still matches. */
    open suspend fun create(type: CategoryType, name: String, categoryId: String = UUID.randomUUID().toString()): CategoryMutationResult = mutate {
        StoredValueValidation.requireUuid(categoryId)
        val displayName = validatedName(name)
        val category = CategoryEntity(categoryId, type, displayName, CategoryNames.normalized(displayName), false)
        val dao = db.categoryDao()
        val sameId = dao.find(categoryId)
        when {
            sameId == category -> CategoryMutationResult.Applied(categoryId, replayed = true)
            sameId != null -> CategoryMutationResult.Conflict("该类别标识已被使用，请重新打开新增页面。")
            dao.findByName(type, category.normalizedName) != null -> CategoryMutationResult.Conflict("同类型中已有同名类别。")
            else -> { dao.insert(category); CategoryMutationResult.Applied(category.id) }
        }
    }

    open suspend fun rename(expected: CategoryEntity, newName: String): CategoryMutationResult = mutate {
        requireEditable(expected)
        val name = validatedName(newName)
        val target = expected.copy(name = name, normalizedName = CategoryNames.normalized(name))
        val dao = db.categoryDao()
        val current = dao.find(expected.id)
        when {
            current == target -> CategoryMutationResult.Applied(target.id, replayed = true)
            current != expected -> CategoryMutationResult.Conflict("类别已发生变化，请刷新后重试。")
            dao.findByName(expected.type, target.normalizedName)?.let { it.id != expected.id } == true ->
                CategoryMutationResult.Conflict("同类型中已有同名类别。")
            else -> {
                check(dao.renameIfUnchanged(expected.id, expected.type, expected.name, expected.normalizedName,
                    target.name, target.normalizedName) == 1) { "Category changed while renaming" }
                CategoryMutationResult.Applied(target.id)
            }
        }
    }

    open suspend fun previewMerge(sourceId: String, targetId: String): CategoryMergePreview = coordinator.withSnapshot {
        StoredValueValidation.requireUuid(sourceId)
        StoredValueValidation.requireUuid(targetId)
        db.withTransaction {
            db.requireDefaults()
            val source = requireNotNull(db.categoryDao().find(sourceId)) { "源类别已不存在，请刷新。" }
            val target = requireNotNull(db.categoryDao().find(targetId)) { "目标类别已不存在，请刷新。" }
            requireMerge(source, target)
            CategoryMergePreview(source, target, usage(source))
        }
    }

    open suspend fun merge(preview: CategoryMergePreview): CategoryMutationResult = mutate {
        requireMerge(preview.source, preview.target)
        require(preview.affectedCount >= 0) { "合并影响数量无效。" }
        val dao = db.categoryDao()
        val source = dao.find(preview.source.id)
            ?: return@mutate CategoryMutationResult.Conflict("源类别已不存在，请刷新类别。")
        val target = dao.find(preview.target.id)
        if (source != preview.source || target != preview.target) {
            // Absence proves no current source, not the identity of an earlier deletion/merge.
            return@mutate CategoryMutationResult.Conflict("类别已发生变化，请重新查看合并范围。")
        }
        if (usage(source) != preview.affectedCount) {
            return@mutate CategoryMutationResult.Conflict("关联记录数量已改变，请重新确认合并范围。")
        }
        val moved = when (source.type) {
            CategoryType.PROJECT -> dao.moveProjectReferences(source.id, target.id)
            CategoryType.SUBSCRIPTION -> dao.moveSubscriptionReferences(source.id, target.id)
        }
        check(moved.toLong() == preview.affectedCount) { "Category reference count changed" }
        check(usage(source) == 0L) { "Category references remain" }
        check(dao.deleteIfUnchanged(source.id, source.type, source.name, source.normalizedName) == 1) {
            "The source category changed"
        }
        CategoryMutationResult.Applied(target.id, preview.affectedCount)
    }

    private suspend fun usage(category: CategoryEntity): Long {
        val dao = db.categoryDao()
        val projects = dao.projectUsage(category.id)
        val subscriptions = dao.subscriptionUsage(category.id)
        return when (category.type) {
            CategoryType.PROJECT -> { check(subscriptions == 0L) { "Cross-type category reference" }; projects }
            CategoryType.SUBSCRIPTION -> { check(projects == 0L) { "Cross-type category reference" }; subscriptions }
        }
    }

    private fun validatedName(input: String): String = input.trim().also { name ->
        require(CategoryNames.normalized(name).isNotBlank()) { "请输入类别名称。" }
        require(name.none { it.isISOControl() }) { "类别名称不能包含控制字符。" }
    }

    private fun requireEditable(category: CategoryEntity) {
        require(!category.isFallback && category.id != LithicDatabase.PROJECT_FALLBACK_ID &&
            category.id != LithicDatabase.SUBSCRIPTION_FALLBACK_ID) { "默认“其他”类别需要保留，不能改名或作为合并来源。" }
    }

    private fun requireMerge(source: CategoryEntity, target: CategoryEntity) {
        requireEditable(source)
        require(source.id != target.id) { "请选择另一个合并目标。" }
        require(source.type == target.type) { "长期项目与订阅的类别不能跨类型合并。" }
    }

    private suspend fun mutate(operation: suspend () -> CategoryMutationResult): CategoryMutationResult = try {
        coordinator.withWrite {
            db.withTransaction { db.requireDefaults(); operation() }
        }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (invalid: IllegalArgumentException) {
        CategoryMutationResult.ValidationFailure(invalid.message ?: "类别操作无效。")
    } catch (_: SQLiteConstraintException) {
        CategoryMutationResult.Conflict("类别或关联记录存在冲突，请刷新后重试。")
    } catch (_: Exception) {
        CategoryMutationResult.StorageFailure("类别操作结果暂时无法确认，请保留本次内容后重试。")
    }
}

package com.cooper.lithic.data.repository

import androidx.room.withTransaction
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.domain.project.ProjectCostEngine
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ProjectCursor(val startedOn: LocalDate, val id: String)

data class ProjectDetail(
    val project: LongTermProjectEntity,
    val category: CategoryEntity,
    val events: List<ProjectMoneyEventEntity>,
    val cost: ProjectCostResult,
    val asOf: LocalDate,
)

data class ProjectPage(
    val items: List<ProjectDetail>,
    val nextCursor: ProjectCursor?,
    val totalCount: Long,
    val asOf: LocalDate,
)

/** A non-owning facade over the same database, coordinator and business clock. */
open class ProjectRepository(
    private val db: LithicDatabase,
    private val coordinator: LocalWriteCoordinator,
    private val clock: BusinessClock,
) {
    private val writer = ProjectWriteRepository(db, coordinator, clock)
    private val reader = LedgerReadRepository(db, coordinator)

    open suspend fun createProject(command: CreateProjectCommand): MutationResult = writer.createProject(command)
    open suspend fun appendMoneyEvent(command: AddProjectMoneyEventCommand): MutationResult = writer.addMoneyEvent(command)
    open suspend fun updateProject(command: UpdateProjectCommand): MutationResult = writer.updateProject(command)
    open suspend fun endProject(command: EndProjectCommand): MutationResult = writer.endProject(command)
    open suspend fun updateEnd(command: UpdateEndCommand): MutationResult = writer.updateEnd(command)
    open suspend fun correctMoneyEvent(command: CorrectMoneyEventCommand): MutationResult = writer.correctMoneyEvent(command)
    open suspend fun deleteProject(command: DeleteProjectCommand): MutationResult = writer.deleteProject(command)

    open suspend fun loadCurrentProjectWindow(pageCount: Int, asOf: LocalDate = clock.today()): ProjectPage = loadWindow(pageCount, asOf, ProjectFilter.CURRENT)
    open suspend fun loadProjectHistoryWindow(pageCount: Int, asOf: LocalDate = clock.today()): ProjectPage = loadWindow(pageCount, asOf, ProjectFilter.HISTORY)

    /** Null means a successful query found no project; read and calculation failures propagate. */
    open suspend fun loadProjectDetail(id: String, asOf: LocalDate = clock.today()): ProjectDetail? {
        StoredValueValidation.requireUuid(id)
        return coordinator.withSnapshot {
            db.withTransaction {
                val project = db.projectDao().findProject(id) ?: return@withTransaction null
                val category = checkNotNull(db.categoryDao().find(project.categoryId)) { "The project category is missing" }
                val events = db.projectDao().eventsForProjects(listOf(id))
                detail(project, category, events, asOf)
            }
        }
    }

    open suspend fun loadProjectPage(
        cursor: ProjectCursor? = null,
        pageSize: Int = 50,
        asOf: LocalDate = clock.today(),
    ): ProjectPage {
        require(pageSize in 1..50) { "A project page must contain between 1 and 50 records" }
        cursor?.let { StoredValueValidation.requireUuid(it.id) }
        return coordinator.withSnapshot {
            db.withTransaction {
                loadPageInTransaction(cursor, pageSize, asOf, db.projectDao().countProjects())
            }
        }
    }

    open suspend fun loadSummary(): LedgerSummary = reader.summary()

    /** Rebuild the loaded window from one snapshot even when a sort-key write is queued. */
    open suspend fun loadProjectWindow(pageCount: Int, asOf: LocalDate = clock.today()): ProjectPage {
        return loadWindow(pageCount, asOf, ProjectFilter.ALL)
    }

    private enum class ProjectFilter { ALL, CURRENT, HISTORY }

    private suspend fun loadWindow(pageCount: Int, asOf: LocalDate, filter: ProjectFilter): ProjectPage {
        require(pageCount > 0) { "A project window must contain at least one page" }
        return coordinator.withSnapshot {
            db.withTransaction {
                val count = when (filter) {
                    ProjectFilter.ALL -> db.projectDao().countProjects()
                    ProjectFilter.CURRENT -> db.projectDao().countCurrentProjects(asOf)
                    ProjectFilter.HISTORY -> db.projectDao().countHistoryProjects(asOf)
                }
                val items = mutableListOf<ProjectDetail>()
                var cursor: ProjectCursor? = null
                for (pageIndex in 0 until pageCount) {
                    val page = loadPageInTransaction(cursor, 50, asOf, count, filter)
                    items += page.items
                    cursor = page.nextCursor
                    if (cursor == null) break
                }
                ProjectPage(items, cursor, count, asOf)
            }
        }
    }

    open suspend fun loadProjectCategories(): List<CategoryEntity> = coordinator.withSnapshot {
        db.withTransaction { db.projectDao().projectCategories() }
    }

    // Includes the initial read signal and edits whose row counts do not change.
    open val changes: Flow<Unit> = db.invalidationTracker.createFlow(
        "long_term_projects", "project_money_events", "categories",
    ).map { Unit }

    /** The caller holds the single coordinator boundary and Room read transaction. */
    private suspend fun loadPageInTransaction(
        cursor: ProjectCursor?,
        pageSize: Int,
        asOf: LocalDate,
        count: Long,
        filter: ProjectFilter = ProjectFilter.ALL,
    ): ProjectPage {
        val dao = db.projectDao()
        // One sentinel determines continuation; only this page's <=50 visible rows get events.
        val queried = when (filter) {
            ProjectFilter.ALL -> dao.pageProjects(cursor?.startedOn, cursor?.id, pageSize + 1)
            ProjectFilter.CURRENT -> dao.pageCurrentProjects(asOf, cursor?.startedOn, cursor?.id, pageSize + 1)
            ProjectFilter.HISTORY -> dao.pageHistoryProjects(asOf, cursor?.startedOn, cursor?.id, pageSize + 1)
        }
        val visible = queried.take(pageSize)
        val events = if (visible.isEmpty()) emptyMap() else
            dao.eventsForProjects(visible.map { it.id }).groupBy { it.projectId }
        val categories = if (visible.isEmpty()) emptyMap() else
            dao.categoriesForProjects(visible.map { it.categoryId }.distinct()).associateBy { it.id }
        val items = visible.map { project ->
            val category = checkNotNull(categories[project.categoryId]) { "The project category is missing" }
            detail(project, category, events[project.id].orEmpty(), asOf)
        }
        val next = if (queried.size > pageSize) visible.last().let { ProjectCursor(it.startedOn, it.id) } else null
        return ProjectPage(items, next, count, asOf)
    }

    private fun detail(
        project: LongTermProjectEntity,
        category: CategoryEntity,
        events: List<ProjectMoneyEventEntity>,
        asOf: LocalDate,
    ): ProjectDetail {
        check(category.type == CategoryType.PROJECT) { "A project has an invalid category type" }
        val snapshot = ProjectSnapshot(
            LongTermProject(project.startedOn, project.endedOn),
            events.map { ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn) },
        )
        return ProjectDetail(project, category, events, ProjectCostEngine.evaluate(snapshot, asOf), asOf)
    }
}

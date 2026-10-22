package com.cooper.lithic.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.room.withTransaction
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.ReplacementEntity
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.project.ReplacementEdge
import com.cooper.lithic.domain.project.ReplacementRules
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LinkReplacementCommand(
    val commandId: String,
    val expectedPredecessor: LongTermProjectEntity,
    val expectedSuccessor: LongTermProjectEntity,
    val expectedLinks: List<ReplacementEntity>,
    val createdAt: Instant,
)

data class UnlinkReplacementCommand(
    val commandId: String,
    val expectedLink: ReplacementEntity,
    val expectedPredecessor: LongTermProjectEntity,
    val expectedSuccessor: LongTermProjectEntity,
    val expectedLinks: List<ReplacementEntity>,
)

data class TradeInCommand(
    val commandId: String,
    val expectedOldProject: LongTermProjectEntity,
    val expectedOldEvents: List<ProjectMoneyEventEntity>,
    val expectedLinks: List<ReplacementEntity>,
    val newProject: LongTermProjectEntity,
    val occurredOn: LocalDate,
    val cashCents: Long,
    val creditCents: Long,
    val linkReplacement: Boolean,
    val updatedAt: Instant,
)

sealed interface ReplacementMutationResult {
    data class Linked(val linkId: String, val replayed: Boolean) : ReplacementMutationResult
    data class Unlinked(val linkId: String, val replayed: Boolean) : ReplacementMutationResult
    data class TradeInRecorded(val oldProjectId: String, val newProjectId: String, val groupId: String, val replayed: Boolean) : ReplacementMutationResult
    data class ValidationFailure(val reason: String) : ReplacementMutationResult
    data class Conflict(val reason: String) : ReplacementMutationResult
    data class StorageFailure(val kind: StorageFailureKind, val message: String) : ReplacementMutationResult
}

data class ReplacementChain(
    val anchorId: String,
    val items: List<ProjectDetail>,
    val links: List<ReplacementEntity>,
    val allLinks: List<ReplacementEntity>,
    val asOf: LocalDate,
)

/** Non-owning access to the existing generation, coordinator and frozen cost engine. */
open class ReplacementRepository(
    private val db: LithicDatabase,
    private val coordinator: LocalWriteCoordinator,
    private val clock: BusinessClock,
) {
    open val changes: Flow<Unit> = db.invalidationTracker.createFlow(
        "replacements", "long_term_projects", "project_money_events", "categories",
    ).map { Unit }

    open suspend fun loadLinks(): List<ReplacementEntity> = coordinator.withSnapshot {
        db.withTransaction { db.replacementDao().listReplacements() }
    }

    open suspend fun loadChain(projectId: String, asOf: LocalDate = clock.today()): ReplacementChain? {
        StoredValueValidation.requireUuid(projectId)
        return coordinator.withSnapshot {
            db.withTransaction {
                ReplacementStoredFacts.requireValid(db)
                val projects = db.projectDao().listProjects()
                if (projects.none { it.id == projectId }) return@withTransaction null
                val links = db.replacementDao().listReplacements()
                checkGraph(projects, links)
                val predecessor = links.associateBy { it.successorId }
                val successor = links.associateBy { it.predecessorId }
                var first = projectId
                while (predecessor[first] != null) first = predecessor.getValue(first).predecessorId
                val ids = mutableListOf<String>()
                var current: String? = first
                while (current != null) { ids += current; current = successor[current]?.successorId }
                val chainIds = ids.toSet()
                val chainProjects = projects.filter { it.id in chainIds }
                val events = ids.chunked(50).flatMap { db.projectDao().eventsForProjects(it) }
                val byId = chainProjects.associateBy { it.id }
                val byProject = events.groupBy { it.projectId }
                val categories = chainProjects.map { it.categoryId }.distinct().chunked(50)
                    .flatMap { db.projectDao().categoriesForProjects(it) }.associateBy { it.id }
                val details = ids.map { id ->
                    val project = byId.getValue(id)
                    val category = checkNotNull(categories[project.categoryId]) { "A comparison category is missing" }
                    check(category.type == CategoryType.PROJECT) { "A comparison project has a subscription category" }
                    val facts = byProject[id].orEmpty()
                    checkProjectFacts(project, facts)
                    ProjectDetail(project, category, facts, ProjectCostEngine.evaluate(ProjectSnapshot(
                        LongTermProject(project.startedOn, project.endedOn),
                        facts.map { ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn) },
                    ), asOf), asOf, links.filter { it.predecessorId == id || it.successorId == id })
                }
                ReplacementChain(projectId, details, links.filter { it.predecessorId in chainIds && it.successorId in chainIds }, links, asOf)
            }
        }
    }

    open suspend fun link(command: LinkReplacementCommand): ReplacementMutationResult = coordinator.withWrite {
        operation {
            db.withTransaction {
                ReplacementStoredFacts.requireValid(db)
                validateIdentity(command.commandId, command.expectedPredecessor, command.expectedSuccessor)?.let { return@withTransaction invalid(it) }
                val previous = command.expectedPredecessor
                val next = command.expectedSuccessor
                val currentPrevious = db.projectDao().findProject(previous.id)
                val currentNext = db.projectDao().findProject(next.id)
                if (currentPrevious != previous || currentNext != next) return@withTransaction conflict("The loaded project changed or is missing")
                val projects = db.projectDao().listProjects()
                val links = db.replacementDao().listReplacements()
                checkGraph(projects, links)
                validateExpectedLinks(command.expectedLinks)?.let { return@withTransaction invalid(it) }
                val target = ReplacementEntity(TradeInIdentities.linkId(command.commandId), previous.id, next.id, command.createdAt)
                if (links.sortedLinks() == (command.expectedLinks + target).sortedLinks() && links.any { it == target }) {
                    return@withTransaction ReplacementMutationResult.Linked(target.id, true)
                }
                if (links.sortedLinks() != command.expectedLinks.sortedLinks()) return@withTransaction conflict("Replacement relationships changed after confirmation")
                ReplacementRules.canLink(projects.map { it.id }.toSet(), links.edges(), previous.id, next.id)?.let { return@withTransaction conflict(it) }
                db.replacementDao().insert(target)
                ReplacementMutationResult.Linked(target.id, false)
            }
        }
    }

    open suspend fun unlink(command: UnlinkReplacementCommand): ReplacementMutationResult = coordinator.withWrite {
        operation {
            db.withTransaction {
                ReplacementStoredFacts.requireValid(db)
                validateIdentity(command.commandId, command.expectedPredecessor, command.expectedSuccessor)?.let { return@withTransaction invalid(it) }
                val expected = command.expectedLink
                if (expected.predecessorId != command.expectedPredecessor.id || expected.successorId != command.expectedSuccessor.id ||
                    command.expectedLinks.count { it == expected } != 1) return@withTransaction invalid("The confirmed replacement identity is inconsistent")
                validateExpectedLinks(command.expectedLinks)?.let { return@withTransaction invalid(it) }
                if (db.projectDao().findProject(command.expectedPredecessor.id) != command.expectedPredecessor ||
                    db.projectDao().findProject(command.expectedSuccessor.id) != command.expectedSuccessor) return@withTransaction conflict("The loaded project changed or is missing")
                val projects = db.projectDao().listProjects()
                val links = db.replacementDao().listReplacements()
                checkGraph(projects, links)
                val target = command.expectedLinks.filterNot { it.id == expected.id }
                if (links.sortedLinks() == target.sortedLinks()) return@withTransaction ReplacementMutationResult.Unlinked(expected.id, true)
                if (links.sortedLinks() != command.expectedLinks.sortedLinks()) return@withTransaction conflict("Replacement relationships changed after confirmation")
                check(db.replacementDao().find(expected.id) == expected) { "The confirmed replacement no longer matches" }
                check(db.replacementDao().delete(expected.id) == 1) { "The replacement was not removed" }
                ReplacementMutationResult.Unlinked(expected.id, false)
            }
        }
    }

    open suspend fun recordTradeIn(command: TradeInCommand): ReplacementMutationResult = coordinator.withWrite {
        operation {
            db.withTransaction {
                validateIdentity(command.commandId, command.expectedOldProject, command.newProject)?.let { return@withTransaction invalid(it) }
                val old = command.expectedOldProject
                val new = command.newProject
                val targetOld = old.copy(endedOn = command.occurredOn, endReason = "以旧换新", updatedAt = command.updatedAt)
                val facts = tradeInEvents(command)
                val newLink = if (command.linkReplacement) ReplacementEntity(TradeInIdentities.linkId(command.commandId), old.id, new.id, command.updatedAt) else null
                val projects = db.projectDao().listProjects()
                val groupedEvents = db.replacementDao().transactionEvents()
                val affectedEvents = db.projectDao().eventsForProjects(listOf(old.id, new.id))
                val links = db.replacementDao().listReplacements()
                checkGraph(projects, links)
                TradeInFacts.validate(projects, groupedEvents)?.let { error(it) }
                val groupFacts = groupedEvents.filter { it.exchangeGroupId == command.commandId }
                val targetLinks = command.expectedLinks + listOfNotNull(newLink)
                val targetOldEvents = command.expectedOldEvents + facts.filter { it.projectId == old.id }
                val targetNewEvents = facts.filter { it.projectId == new.id }
                if (db.projectDao().findProject(old.id) == targetOld && db.projectDao().findProject(new.id) == new &&
                    groupFacts.sortedEvents() == facts.sortedEvents() &&
                    affectedEvents.filter { it.projectId == old.id }.sortedEvents() == targetOldEvents.sortedEvents() &&
                    affectedEvents.filter { it.projectId == new.id }.sortedEvents() == targetNewEvents.sortedEvents() &&
                    links.sortedLinks() == targetLinks.sortedLinks()) {
                    validateProjectReferences(old, "old")?.let { return@withTransaction invalid(it) }
                    validateProjectReferences(new, "new")?.let { return@withTransaction invalid(it) }
                    checkProjectFacts(old, command.expectedOldEvents)
                    validateCalculatedFacts(old, command.expectedOldEvents, targetOld, targetOldEvents, new, targetNewEvents)
                        ?.let { return@withTransaction invalid(it) }
                    return@withTransaction ReplacementMutationResult.TradeInRecorded(old.id, new.id, command.commandId, true)
                }
                if (groupFacts.isNotEmpty() || facts.any { candidate -> db.projectDao().findEvent(candidate.id) != null || db.projectDao().findEventByOperation(candidate.operationId) != null } ||
                    projects.any { it.id == new.id } || links.any { it.id == newLink?.id }) return@withTransaction conflict("The trade-in identity already belongs to different or incomplete facts")
                validateTradeInCommand(command)?.let { return@withTransaction invalid(it) }
                if (db.projectDao().findProject(old.id) != old || affectedEvents.filter { it.projectId == old.id }.sortedEvents() != command.expectedOldEvents.sortedEvents()) {
                    return@withTransaction conflict("The old project or its complete money history changed after confirmation")
                }
                if (links.sortedLinks() != command.expectedLinks.sortedLinks()) return@withTransaction conflict("Replacement relationships changed after confirmation")
                validateProjectReferences(old, "old")?.let { return@withTransaction invalid(it) }
                validateProjectReferences(new, "new")?.let { return@withTransaction invalid(it) }
                if (newLink != null) ReplacementRules.canLink((projects.map { it.id } + new.id).toSet(), links.edges(), old.id, new.id)?.let { return@withTransaction conflict(it) }
                val resultingProjects = projects.filterNot { it.id == old.id } + targetOld + new
                TradeInFacts.validate(resultingProjects, groupedEvents + facts)?.let { return@withTransaction invalid(it) }
                checkProjectFacts(old, command.expectedOldEvents)
                validateCalculatedFacts(old, command.expectedOldEvents, targetOld, targetOldEvents, new, targetNewEvents)
                    ?.let { return@withTransaction invalid(it) }
                check(db.projectDao().updateProjectIfUnchanged(old.id, old.name, old.categoryId, old.startedOn, old.endedOn, old.endReason,
                    old.coverAssetId, old.note, old.createdAt, old.updatedAt,
                    targetOld.name, targetOld.categoryId, targetOld.startedOn, targetOld.endedOn, targetOld.endReason, targetOld.note, targetOld.updatedAt) == 1) { "The old project compare-and-set failed" }
                db.projectDao().insertProject(new)
                facts.forEach { db.projectDao().insertEvent(it) }
                if (newLink != null) db.replacementDao().insert(newLink)
                val today = clock.today()
                if (MilestoneHistoryChanges.project(old.startedOn, old.endedOn, targetOld.startedOn, targetOld.endedOn, today)) {
                    MilestoneWatermarkStore.projectChangedWithinTransaction(db, old.id, today)
                }
                MilestoneWatermarkStore.projectCreatedWithinTransaction(db, new.id, today)
                ReplacementMutationResult.TradeInRecorded(old.id, new.id, command.commandId, false)
            }
        }
    }

    private fun validateTradeInCommand(command: TradeInCommand): String? {
        val old = command.expectedOldProject
        val new = command.newProject
        validateIdentity(command.commandId, old, new)?.let { return it }
        validateExpectedLinks(command.expectedLinks)?.let { return it }
        if (old.id == new.id || new.id != TradeInIdentities.newProjectId(command.commandId)) return "The new project must keep the transaction's distinct fixed identity"
        if (old.endedOn != null || old.endReason != null) return "Trade-in requires an old project without an existing end record"
        if (old.name.isBlank() || old.updatedAt.isBefore(old.createdAt)) return "The old project's stored name or revision is inconsistent"
        if (command.occurredOn.isBefore(old.startedOn) || command.occurredOn.isAfter(clock.today())) return "The actual trade-in date must be within ownership and business today"
        if (new.startedOn != command.occurredOn || new.endedOn != null || new.endReason != null) return "The new project must begin on the actual trade-in date and remain unended"
        if (new.name.isBlank()) return "The new project name cannot be blank"
        if (!command.updatedAt.isAfter(old.updatedAt) || command.updatedAt.isBefore(old.createdAt) ||
            new.createdAt != command.updatedAt || new.updatedAt != command.updatedAt) return "The transaction must preserve its fixed, later revision"
        if (command.expectedOldEvents.any { it.projectId != old.id || !validEventIdentity(it) } ||
            command.expectedOldEvents.map { it.id }.distinct().size != command.expectedOldEvents.size) return "The confirmation must retain the old project's distinct complete money facts"
        if (command.expectedOldEvents.any { it.role == ProjectMoneyEventRole.END_SETTLEMENT }) return "An old project with an end settlement cannot be traded in again"
        return ReplacementRules.validateTradeInAmounts(command.cashCents, command.creditCents)
    }

    private fun tradeInEvents(command: TradeInCommand): List<ProjectMoneyEventEntity> {
        fun event(role: String, projectId: String, direction: MoneyDirection, amount: Long, eventRole: ProjectMoneyEventRole, basis: MoneyBasis, description: String) =
            ProjectMoneyEventEntity(TradeInIdentities.eventId(command.commandId, role), projectId, direction, amount, command.occurredOn,
                description, eventRole, TradeInIdentities.operationId(command.commandId, role), basis, command.commandId)
        return listOf(
            event(TradeInIdentities.OLD_CREDIT, command.expectedOldProject.id, MoneyDirection.RECOVERY, command.creditCents, ProjectMoneyEventRole.END_SETTLEMENT, MoneyBasis.TRADE_IN_CREDIT, "以旧换新：非现金折抵收回"),
            event(TradeInIdentities.NEW_CASH, command.newProject.id, MoneyDirection.OUTLAY, command.cashCents, ProjectMoneyEventRole.INITIAL, MoneyBasis.CASH, "以旧换新：实际现金付款"),
            event(TradeInIdentities.NEW_CREDIT, command.newProject.id, MoneyDirection.OUTLAY, command.creditCents, ProjectMoneyEventRole.REGULAR, MoneyBasis.TRADE_IN_CREDIT, "以旧换新：非现金折抵投入"),
        )
    }

    private fun checkGraph(projects: List<LongTermProjectEntity>, links: List<ReplacementEntity>) {
        validateExpectedLinks(links)?.let { error(it) }
        ReplacementRules.validate(projects.map { it.id }.toSet(), links.edges())?.let { error(it) }
    }
    /** References are reread inside the same transaction, not inferred from the loaded UUID. */
    private suspend fun validateProjectReferences(project: LongTermProjectEntity, label: String): String? {
        if (db.categoryDao().find(project.categoryId)?.type != CategoryType.PROJECT)
            return "The $label project category is missing or belongs to subscriptions"
        if (project.coverAssetId != null && db.mediaDao().find(project.coverAssetId) == null)
            return "The $label project cover metadata is missing"
        return null
    }
    /** Reuse the frozen engine's checked accumulation across ALL stored dates before writing.
     * Checking only each individual amount would miss an old recovery sum overflowing.
     */
    private fun validateCalculatedFacts(
        old: LongTermProjectEntity, oldEvents: List<ProjectMoneyEventEntity>,
        targetOld: LongTermProjectEntity, targetOldEvents: List<ProjectMoneyEventEntity>,
        new: LongTermProjectEntity, newEvents: List<ProjectMoneyEventEntity>,
    ): String? = try {
        for ((project, events) in listOf(old to oldEvents, targetOld to targetOldEvents, new to newEvents)) {
            ProjectCostEngine.evaluate(ProjectSnapshot(LongTermProject(project.startedOn, project.endedOn),
                events.map { ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn) }), LocalDate.MAX)
        }
        null
    } catch (_: ArithmeticException) {
        "The project's cumulative money or duration overflows; no trade-in facts were changed"
    } catch (_: IllegalArgumentException) {
        "The project's stored dates or amounts cannot be safely calculated; no trade-in facts were changed"
    }
    private fun validateIdentity(commandId: String, vararg projects: LongTermProjectEntity): String? {
        if (!StoredValueValidation.isUuid(commandId) || projects.any { !StoredValueValidation.isUuid(it.id) || !StoredValueValidation.isUuid(it.categoryId) ||
                (it.coverAssetId != null && !StoredValueValidation.isUuid(it.coverAssetId)) }) return "Business identities must be canonical UUID strings"
        return null
    }
    private fun validateExpectedLinks(links: List<ReplacementEntity>): String? = when {
        links.map { it.id }.distinct().size != links.size -> "Replacement identities are duplicated"
        links.any { !StoredValueValidation.isUuid(it.id) || !StoredValueValidation.isUuid(it.predecessorId) || !StoredValueValidation.isUuid(it.successorId) } -> "Replacement identities must be canonical UUID strings"
        else -> null
    }
    private fun validEventIdentity(event: ProjectMoneyEventEntity): Boolean =
        listOf(event.id, event.projectId, event.operationId).all(StoredValueValidation::isUuid) && (event.exchangeGroupId == null || StoredValueValidation.isUuid(event.exchangeGroupId))
    private fun checkProjectFacts(project: LongTermProjectEntity, events: List<ProjectMoneyEventEntity>) {
        check(project.name.isNotBlank() && !project.updatedAt.isBefore(project.createdAt)) { "The stored project name or revision is inconsistent" }
        check(events.map { it.id }.distinct().size == events.size && events.map { it.operationId }.distinct().size == events.size) { "Money identities are duplicated" }
        check(events.count { it.role == ProjectMoneyEventRole.INITIAL } == 1) { "A project must retain its single initial outlay" }
        check(events.count { it.role == ProjectMoneyEventRole.END_SETTLEMENT } <= 1) { "Legacy end settlements are ambiguous" }
        events.forEach { event ->
            check(validEventIdentity(event) && event.projectId == project.id) { "Money fact identity is invalid" }
            Money(event.amountCents)
            if (event.role == ProjectMoneyEventRole.INITIAL) check(event.direction == MoneyDirection.OUTLAY) { "An initial event must be an outlay" }
            if (event.role == ProjectMoneyEventRole.END_SETTLEMENT) check(event.direction == MoneyDirection.RECOVERY && project.endedOn != null) { "An end settlement requires an ended project" }
        }
    }
    private fun List<ReplacementEntity>.edges() = map { ReplacementEdge(it.predecessorId, it.successorId) }
    private fun List<ReplacementEntity>.sortedLinks() = sortedBy { it.id }
    private fun List<ProjectMoneyEventEntity>.sortedEvents() = sortedBy { it.id }
    private fun invalid(reason: String) = ReplacementMutationResult.ValidationFailure(reason)
    private fun conflict(reason: String) = ReplacementMutationResult.Conflict(reason)
    private suspend fun operation(block: suspend () -> ReplacementMutationResult): ReplacementMutationResult = try {
        block()
    } catch (cancelled: CancellationException) { throw cancelled
    } catch (failure: SQLiteConstraintException) { conflict(failure.message ?: "A stored identity conflicts")
    } catch (failure: SQLiteFullException) { ReplacementMutationResult.StorageFailure(StorageFailureKind.DISK_FULL, failure.message ?: "Storage is full")
    } catch (failure: SQLiteDatabaseCorruptException) { ReplacementMutationResult.StorageFailure(StorageFailureKind.CORRUPT, failure.message ?: "Stored facts are corrupt")
    } catch (failure: SQLiteException) { ReplacementMutationResult.StorageFailure(StorageFailureKind.DATABASE, failure.message ?: "Database operation failed")
    } catch (failure: IllegalArgumentException) { ReplacementMutationResult.StorageFailure(StorageFailureKind.CORRUPT, failure.message ?: "Stored facts are invalid")
    } catch (failure: IllegalStateException) { ReplacementMutationResult.StorageFailure(StorageFailureKind.DATABASE, failure.message ?: "The transaction could not be completed") }
}

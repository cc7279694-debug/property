package com.cooper.lithic.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.room.withTransaction
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.domain.project.ProjectLifecycleRules
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.LocalDate

data class CreateProjectCommand(val commandId: String, val project: LongTermProjectEntity, val initialEvent: ProjectMoneyEventEntity)
data class AddProjectMoneyEventCommand(val commandId: String, val event: ProjectMoneyEventEntity)
data class UpdateProjectCommand(
    val commandId: String,
    val expected: LongTermProjectEntity,
    val name: String,
    val startedOn: LocalDate,
    val categoryId: String,
    val note: String?,
    val updatedAt: Instant,
)

enum class StorageFailureKind { DATABASE, DISK_FULL, CORRUPT }

sealed interface MutationResult {
    data class Success(val projectId: String, val eventId: String, val replayed: Boolean) : MutationResult
    data class ProjectUpdated(val projectId: String, val replayed: Boolean) : MutationResult
    data class LifecycleApplied(val projectId: String, val settlementEventId: String?, val replayed: Boolean) : MutationResult
    data class MoneyCorrected(val projectId: String, val eventId: String, val replayed: Boolean) : MutationResult
    data class ProjectDeleted(val projectId: String) : MutationResult
    data class ProjectAbsent(val projectId: String) : MutationResult
    data class ValidationFailure(val reason: String) : MutationResult
    data class Conflict(val reason: String) : MutationResult
    data class StorageFailure(val kind: StorageFailureKind, val message: String) : MutationResult
}

class ProjectWriteRepository(private val db: LithicDatabase, private val coordinator: LocalWriteCoordinator, private val clock: BusinessClock) {
    suspend fun endProject(command: EndProjectCommand): MutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val expected = command.expectedProject
                validateProjectIdentity(command.commandId, expected)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                val current = db.projectDao().findProject(expected.id)
                    ?: return@withTransaction MutationResult.ValidationFailure("The referenced project does not exist")
                val events = db.projectDao().endSettlementsForProject(expected.id)
                settlementConflict(events)?.let { return@withTransaction MutationResult.Conflict(it) }
                if (expected.endedOn != null) return@withTransaction MutationResult.ValidationFailure("Use end correction for an already ended project")
                validateRevision(expected, command.updatedAt)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                ProjectLifecycleRules.validateEnd(expected.startedOn, command.endedOn, command.endReason)?.let {
                    return@withTransaction MutationResult.ValidationFailure(it)
                }
                val settlement = command.settlement
                if (settlement != null) {
                    validateSettlement(settlement, expected.id)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                    if (settlement.operationId != command.commandId) {
                        return@withTransaction MutationResult.ValidationFailure("The settlement operation must match its command")
                    }
                }
                val target = expected.copy(endedOn = command.endedOn, endReason = command.endReason, updatedAt = command.updatedAt)
                val currentSettlement = events.singleOrNull { it.role == ProjectMoneyEventRole.END_SETTLEMENT }
                // No durable operation log exists: only these exact current target facts prove replay.
                if (current == target && currentSettlement == settlement) {
                    return@withTransaction MutationResult.LifecycleApplied(expected.id, settlement?.id, true)
                }
                if (current != expected) return@withTransaction MutationResult.Conflict("The project changed after this editor was loaded")
                if (currentSettlement != null || db.projectDao().findEventByOperation(command.commandId) != null) {
                    return@withTransaction MutationResult.Conflict("The end operation or its settlement is already associated with other facts")
                }
                validateProjectRelationships(target)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                applyProject(expected, target)
                if (settlement != null) db.projectDao().insertEvent(settlement)
                MutationResult.LifecycleApplied(target.id, settlement?.id, false)
            }
        }
    }

    suspend fun updateEnd(command: UpdateEndCommand): MutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val expected = command.expectedProject
                validateProjectIdentity(command.commandId, expected)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                val current = db.projectDao().findProject(expected.id)
                    ?: return@withTransaction MutationResult.ValidationFailure("The referenced project does not exist")
                val events = db.projectDao().endSettlementsForProject(expected.id)
                settlementConflict(events)?.let { return@withTransaction MutationResult.Conflict(it) }
                if (expected.endedOn == null) return@withTransaction MutationResult.ValidationFailure("An end correction requires an ended record")
                validateRevision(expected, command.updatedAt)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                ProjectLifecycleRules.validateEnd(expected.startedOn, command.endedOn, command.endReason)?.let {
                    return@withTransaction MutationResult.ValidationFailure(it)
                }
                val expectedSettlement = command.expectedSettlement
                val targetSettlement = if (expectedSettlement == null) {
                    if (command.settlementAmountCents != null || command.settlementOccurredOn != null || command.settlementDescription != null) {
                        return@withTransaction MutationResult.ValidationFailure("An end without settlement cannot create a payment through correction")
                    }
                    null
                } else {
                    validateSettlement(expectedSettlement, expected.id)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                    if (command.settlementAmountCents == null || command.settlementOccurredOn == null) {
                        return@withTransaction MutationResult.ValidationFailure("An associated settlement correction must keep its actual amount and date")
                    }
                    expectedSettlement.copy(
                        amountCents = command.settlementAmountCents,
                        occurredOn = command.settlementOccurredOn,
                        description = command.settlementDescription,
                    ).also { event -> validateAmountAndDate(event)?.let { return@withTransaction MutationResult.ValidationFailure(it) } }
                }
                val target = expected.copy(endedOn = command.endedOn, endReason = command.endReason, updatedAt = command.updatedAt)
                val currentSettlement = events.singleOrNull { it.role == ProjectMoneyEventRole.END_SETTLEMENT }
                if (current == target && currentSettlement == targetSettlement) {
                    return@withTransaction MutationResult.LifecycleApplied(expected.id, targetSettlement?.id, true)
                }
                if (current != expected || currentSettlement != expectedSettlement) {
                    return@withTransaction MutationResult.Conflict("The loaded project or associated settlement changed")
                }
                validateProjectRelationships(target)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                applyProject(expected, target)
                if (expectedSettlement != null && targetSettlement != null) applyEvent(expectedSettlement, targetSettlement)
                MutationResult.LifecycleApplied(target.id, targetSettlement?.id, false)
            }
        }
    }

    suspend fun correctMoneyEvent(command: CorrectMoneyEventCommand): MutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val expected = command.expectedProject
                val expectedEvent = command.expectedEvent
                validateProjectIdentity(command.commandId, expected)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                validateEventIdentity(expectedEvent)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                if (expectedEvent.projectId != expected.id) return@withTransaction MutationResult.ValidationFailure("The event must belong to the loaded project")
                val current = db.projectDao().findProject(expected.id)
                    ?: return@withTransaction MutationResult.ValidationFailure("The referenced project does not exist")
                val events = db.projectDao().endSettlementsForProject(expected.id)
                settlementConflict(events)?.let { return@withTransaction MutationResult.Conflict(it) }
                if (expectedEvent.role == ProjectMoneyEventRole.END_SETTLEMENT) {
                    return@withTransaction MutationResult.ValidationFailure("Correct an end settlement through its associated end record")
                }
                if (expectedEvent.role == ProjectMoneyEventRole.INITIAL && expectedEvent.direction != MoneyDirection.OUTLAY) {
                    return@withTransaction MutationResult.ValidationFailure("The initial event must remain an initial outlay")
                }
                validateRevision(expected, command.updatedAt)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                val targetEvent = expectedEvent.copy(amountCents = command.amountCents, occurredOn = command.occurredOn, description = command.description)
                validateAmountAndDate(targetEvent)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                val target = expected.copy(updatedAt = command.updatedAt)
                val currentEvent = db.projectDao().findEvent(expectedEvent.id)
                if (current == target && currentEvent == targetEvent) {
                    return@withTransaction MutationResult.MoneyCorrected(expected.id, expectedEvent.id, true)
                }
                if (current != expected || currentEvent != expectedEvent) {
                    return@withTransaction MutationResult.Conflict("The loaded project or money event changed")
                }
                validateProjectRelationships(target)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                applyProject(expected, target)
                applyEvent(expectedEvent, targetEvent)
                MutationResult.MoneyCorrected(target.id, targetEvent.id, false)
            }
        }
    }

    suspend fun deleteProject(command: DeleteProjectCommand): MutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val expected = command.expectedProject
                validateProjectIdentity(command.commandId, expected)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                if (command.expectedEvents.any { it.projectId != expected.id || validateEventIdentity(it) != null } ||
                    command.expectedEvents.map { it.id }.toSet().size != command.expectedEvents.size) {
                    return@withTransaction MutationResult.ValidationFailure("The confirmation must contain this project's complete distinct event identities")
                }
                val current = db.projectDao().findProject(expected.id)
                val events = db.projectDao().eventsForProjects(listOf(expected.id))
                if (current == null) {
                    return@withTransaction if (events.isEmpty()) MutationResult.ProjectAbsent(expected.id)
                    else MutationResult.StorageFailure(StorageFailureKind.CORRUPT, "A missing project still has orphaned money events")
                }
                settlementConflict(events)?.let { return@withTransaction MutationResult.Conflict(it) }
                if (current != expected || events.sortedBy { it.id } != command.expectedEvents.sortedBy { it.id }) {
                    return@withTransaction MutationResult.Conflict("The project or its money history changed after deletion was confirmed")
                }
                for (event in events) {
                    requireChanged(db.projectDao().deleteEventIfUnchanged(
                        event.id, event.projectId, event.direction, event.amountCents, event.occurredOn,
                        event.description, event.role, event.operationId,
                    ))
                }
                requireChanged(db.projectDao().deleteProjectIfUnchanged(
                    expected.id, expected.name, expected.categoryId, expected.startedOn, expected.endedOn,
                    expected.endReason, expected.coverAssetId, expected.note, expected.createdAt, expected.updatedAt,
                ))
                check(db.projectDao().findProject(expected.id) == null && db.projectDao().eventsForProjects(listOf(expected.id)).isEmpty()) {
                    "The deleted project still has stored facts"
                }
                // Shared and now-unreferenced media metadata/files belong to M10, never this delete.
                MutationResult.ProjectDeleted(expected.id)
            }
        }
    }

    suspend fun updateProject(command: UpdateProjectCommand): MutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val expected = command.expected
                val identities = listOf(command.commandId, expected.id, expected.categoryId, command.categoryId) + listOfNotNull(expected.coverAssetId)
                if (identities.any { !StoredValueValidation.isUuid(it) }) {
                    return@withTransaction MutationResult.ValidationFailure("Business identities must be canonical UUID strings")
                }
                if (!command.updatedAt.isAfter(expected.updatedAt)) {
                    return@withTransaction MutationResult.ValidationFailure("An edit revision must be later than the loaded revision")
                }
                val target = expected.copy(
                    name = command.name,
                    startedOn = command.startedOn,
                    categoryId = command.categoryId,
                    note = command.note,
                    updatedAt = command.updatedAt,
                )
                val current = db.projectDao().findProject(expected.id)
                    ?: return@withTransaction MutationResult.ValidationFailure("The referenced project does not exist")
                settlementConflict(db.projectDao().endSettlementsForProject(expected.id))?.let {
                    return@withTransaction MutationResult.Conflict(it)
                }
                // Schema v1 has no edit operation log: replay is an exact current-target comparison.
                if (current == target) return@withTransaction MutationResult.ProjectUpdated(current.id, true)
                if (current != expected) return@withTransaction MutationResult.Conflict("The project changed after this editor was loaded")
                if (target.name.isBlank()) return@withTransaction MutationResult.ValidationFailure("The project name cannot be blank")
                if ((target.endedOn == null) != (target.endReason == null)) {
                    return@withTransaction MutationResult.ValidationFailure("The end date and reason must be present together")
                }
                if (target.endReason != null && target.endReason.isBlank()) {
                    return@withTransaction MutationResult.ValidationFailure("An end reason cannot be blank")
                }
                if (target.endedOn != null && target.endedOn.isBefore(target.startedOn)) {
                    return@withTransaction MutationResult.ValidationFailure("The end date cannot precede ownership")
                }
                if (target.updatedAt.isBefore(target.createdAt)) {
                    return@withTransaction MutationResult.ValidationFailure("The update instant cannot precede creation")
                }
                validateProjectRelationships(target)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                applyProject(expected, target)
                MutationResult.ProjectUpdated(target.id, false)
            }
        }
    }

    suspend fun createProject(command: CreateProjectCommand): MutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val project = command.project
                val event = command.initialEvent
                val identities = listOf(command.commandId, project.id, project.categoryId, event.id, event.projectId, event.operationId) + listOfNotNull(project.coverAssetId)
                if (identities.any { !StoredValueValidation.isUuid(it) }) return@withTransaction MutationResult.ValidationFailure("Business identities must be canonical UUID strings")
                if (event.operationId != command.commandId) return@withTransaction MutationResult.ValidationFailure("The event operation must match its command")

                settlementConflict(db.projectDao().endSettlementsForProject(project.id))?.let {
                    return@withTransaction MutationResult.Conflict(it)
                }

                val existingEvent = db.projectDao().findEventByOperation(command.commandId)
                if (existingEvent != null) {
                    val existingProject = db.projectDao().findProject(existingEvent.projectId)
                    return@withTransaction if (existingEvent == event && existingProject == project) {
                        MutationResult.Success(existingEvent.projectId, existingEvent.id, true)
                    } else MutationResult.Conflict("The operation was already used for different contents")
                }

                validateNewProject(command)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                validateProjectRelationships(project)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                // The second ABORT insert can fail; Room rolls both inserts back together.
                db.projectDao().insertProject(project)
                db.projectDao().insertEvent(event)
                MutationResult.Success(project.id, event.id, false)
            }
        }
    }

    suspend fun addMoneyEvent(command: AddProjectMoneyEventCommand): MutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val event = command.event
                if (listOf(command.commandId, event.id, event.projectId, event.operationId).any { !StoredValueValidation.isUuid(it) }) {
                    return@withTransaction MutationResult.ValidationFailure("Business identities must be canonical UUID strings")
                }
                if (event.operationId != command.commandId) return@withTransaction MutationResult.ValidationFailure("The event operation must match its command")
                val project = db.projectDao().findProject(event.projectId)
                    ?: return@withTransaction MutationResult.ValidationFailure("The referenced project does not exist")
                val settlements = db.projectDao().endSettlementsForProject(event.projectId)
                settlementConflict(settlements)?.let { return@withTransaction MutationResult.Conflict(it) }
                val existing = db.projectDao().findEventByOperation(command.commandId)
                if (existing != null) {
                    return@withTransaction if (existing == event) MutationResult.Success(existing.projectId, existing.id, true)
                    else MutationResult.Conflict("The operation was already used for different contents")
                }
                validateAmountAndDate(event)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                if (event.role == ProjectMoneyEventRole.INITIAL) return@withTransaction MutationResult.ValidationFailure("An initial event must be created atomically with its project")
                validateProjectRelationships(project)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                if (event.role == ProjectMoneyEventRole.END_SETTLEMENT && project.endedOn == null) {
                    return@withTransaction MutationResult.ValidationFailure("An end settlement requires an ended project")
                }
                if (event.role == ProjectMoneyEventRole.END_SETTLEMENT) {
                    if (event.direction != MoneyDirection.RECOVERY) return@withTransaction MutationResult.ValidationFailure("An end settlement must be a recovery")
                    if (settlements.isNotEmpty()) return@withTransaction MutationResult.Conflict("The project already has an associated end settlement")
                }
                db.projectDao().insertEvent(event)
                MutationResult.Success(event.projectId, event.id, false)
            }
        }
    }

    private fun validateProjectIdentity(commandId: String, project: LongTermProjectEntity): String? {
        val identities = listOf(commandId, project.id, project.categoryId) + listOfNotNull(project.coverAssetId)
        return if (identities.any { !StoredValueValidation.isUuid(it) }) "Business identities must be canonical UUID strings" else null
    }

    private fun validateEventIdentity(event: ProjectMoneyEventEntity): String? =
        if (listOf(event.id, event.projectId, event.operationId).any { !StoredValueValidation.isUuid(it) })
            "Business identities must be canonical UUID strings" else null

    private fun validateRevision(expected: LongTermProjectEntity, updatedAt: Instant): String? = when {
        !updatedAt.isAfter(expected.updatedAt) -> "An edit revision must be later than the loaded revision"
        updatedAt.isBefore(expected.createdAt) -> "The update instant cannot precede creation"
        else -> null
    }

    private fun validateSettlement(event: ProjectMoneyEventEntity, projectId: String): String? {
        validateEventIdentity(event)?.let { return it }
        if (event.projectId != projectId || event.role != ProjectMoneyEventRole.END_SETTLEMENT || event.direction != MoneyDirection.RECOVERY) {
            return "The associated end settlement must be this project's recovery event"
        }
        return validateAmountAndDate(event)
    }

    private fun settlementConflict(events: List<ProjectMoneyEventEntity>): String? =
        if (events.count { it.role == ProjectMoneyEventRole.END_SETTLEMENT } > 1)
            "Multiple legacy end settlements have ambiguous association; no facts were changed" else null

    /** Throws so a failed affected-row check rolls back preceding mutations, never commits them. */
    private class WriteConflict(message: String) : RuntimeException(message)

    private fun requireChanged(rows: Int) {
        if (rows != 1) throw WriteConflict("The complete loaded row is no longer current")
    }

    private suspend fun applyProject(expected: LongTermProjectEntity, target: LongTermProjectEntity) {
        requireChanged(db.projectDao().updateProjectIfUnchanged(
            expected.id, expected.name, expected.categoryId, expected.startedOn, expected.endedOn,
            expected.endReason, expected.coverAssetId, expected.note, expected.createdAt, expected.updatedAt,
            target.name, target.categoryId, target.startedOn, target.endedOn, target.endReason, target.note, target.updatedAt,
        ))
    }

    private suspend fun applyEvent(expected: ProjectMoneyEventEntity, target: ProjectMoneyEventEntity) {
        requireChanged(db.projectDao().updateEventIfUnchanged(
            expected.id, expected.projectId, expected.direction, expected.amountCents, expected.occurredOn,
            expected.description, expected.role, expected.operationId,
            target.amountCents, target.occurredOn, target.description,
        ))
    }

    private fun validateNewProject(command: CreateProjectCommand): String? {
        val project = command.project
        val event = command.initialEvent
        if (project.name.isBlank()) return "The project name cannot be blank"
        if (event.projectId != project.id) return "The initial event must reference its new project"
        if (event.direction != MoneyDirection.OUTLAY || event.role != ProjectMoneyEventRole.INITIAL) return "The initial event must be an initial outlay"
        if ((project.endedOn == null) != (project.endReason == null)) return "The end date and reason must be present together"
        if (project.endReason != null && project.endReason.isBlank()) return "An end reason cannot be blank"
        if (project.endedOn != null && project.endedOn.isBefore(project.startedOn)) return "The end date cannot precede ownership"
        if (project.updatedAt.isBefore(project.createdAt)) return "The update instant cannot precede creation"
        return validateAmountAndDate(event)
    }

    private fun validateAmountAndDate(event: ProjectMoneyEventEntity): String? {
        try { Money(event.amountCents) } catch (_: IllegalArgumentException) { return "A single amount exceeds the allowed absolute CNY range" }
        if (event.occurredOn.isAfter(clock.today())) return "A payment cannot be recorded in the business future"
        return null
    }

    /** Must only run inside the Room transaction that applies the mutation. */
    private suspend fun validateProjectRelationships(project: LongTermProjectEntity): String? {
        val category = db.categoryDao().find(project.categoryId) ?: return "The referenced category does not exist"
        if (category.type != CategoryType.PROJECT) return "A project cannot use a subscription category"
        if (project.coverAssetId != null && db.mediaDao().find(project.coverAssetId) == null) return "The referenced cover metadata does not exist"
        return null
    }

    private suspend fun storageOperation(block: suspend () -> MutationResult): MutationResult = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (conflict: WriteConflict) {
        MutationResult.Conflict(conflict.message ?: "The loaded row changed")
    } catch (constraint: SQLiteConstraintException) {
        MutationResult.Conflict(constraint.message ?: "A stored identity or relationship conflicts")
    } catch (full: SQLiteFullException) {
        MutationResult.StorageFailure(StorageFailureKind.DISK_FULL, full.message ?: "Storage is full")
    } catch (corrupt: SQLiteDatabaseCorruptException) {
        MutationResult.StorageFailure(StorageFailureKind.CORRUPT, corrupt.message ?: "The database is corrupt")
    } catch (database: SQLiteException) {
        MutationResult.StorageFailure(StorageFailureKind.DATABASE, database.message ?: "The database operation failed")
    } catch (invalidStoredValue: IllegalArgumentException) {
        MutationResult.StorageFailure(StorageFailureKind.CORRUPT, invalidStoredValue.message ?: "Stored metadata is invalid")
    } catch (state: IllegalStateException) {
        MutationResult.StorageFailure(StorageFailureKind.DATABASE, state.message ?: "The database is unavailable")
    }
}

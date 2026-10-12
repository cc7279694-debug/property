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
    data class ValidationFailure(val reason: String) : MutationResult
    data class Conflict(val reason: String) : MutationResult
    data class StorageFailure(val kind: StorageFailureKind, val message: String) : MutationResult
}

class ProjectWriteRepository(private val db: LithicDatabase, private val coordinator: LocalWriteCoordinator, private val clock: BusinessClock) {
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
                val changed = db.projectDao().updateProjectMetadata(
                    target.id, expected.updatedAt, target.name, target.startedOn,
                    target.categoryId, target.note, target.updatedAt,
                )
                if (changed != 1) return@withTransaction MutationResult.Conflict("The loaded project revision is no longer current")
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
                val existing = db.projectDao().findEventByOperation(command.commandId)
                if (existing != null) {
                    return@withTransaction if (existing == event) MutationResult.Success(existing.projectId, existing.id, true)
                    else MutationResult.Conflict("The operation was already used for different contents")
                }
                validateAmountAndDate(event)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                if (event.role == ProjectMoneyEventRole.INITIAL) return@withTransaction MutationResult.ValidationFailure("An initial event must be created atomically with its project")
                val project = db.projectDao().findProject(event.projectId)
                    ?: return@withTransaction MutationResult.ValidationFailure("The referenced project does not exist")
                validateProjectRelationships(project)?.let { return@withTransaction MutationResult.ValidationFailure(it) }
                if (event.role == ProjectMoneyEventRole.END_SETTLEMENT && project.endedOn == null) {
                    return@withTransaction MutationResult.ValidationFailure("An end settlement requires an ended project")
                }
                db.projectDao().insertEvent(event)
                MutationResult.Success(event.projectId, event.id, false)
            }
        }
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

package com.cooper.lithic.data.media

import android.content.ContentResolver
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.room.withTransaction
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionFacts
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.data.repository.facts
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Same generation, owner and coordinator: no second media database or cost calculation. */
open class MediaRepository(
    private val database: LithicDatabase,
    private val coordinator: LocalWriteCoordinator,
    private val images: LocalImageStore,
    private val clock: BusinessClock,
    private val resolver: ContentResolver,
) {
    open suspend fun loadTarget(target: CoverTarget): MediaResult<CoverBaseline> = withContext(Dispatchers.IO) {
        storage {
            StoredValueValidation.requireUuid(target.id)
            requireOpen()
            // These existing readers already acquire the shared snapshot once, calculate with
            // frozen engines, and reject invalid stored relationships. Do not nest their mutex.
            when (target.type) {
                CoverTargetType.PROJECT -> {
                    val detail = ProjectRepository(database, coordinator, clock).loadProjectDetail(target.id)
                        ?: abort(MediaFailureKind.NOT_FOUND, "The project no longer exists")
                    CoverBaseline.Project(detail.project, detail.events)
                }
                CoverTargetType.SUBSCRIPTION -> {
                    val detail = SubscriptionRepository(database, coordinator, clock).loadSubscriptionDetail(target.id)
                        ?: abort(MediaFailureKind.NOT_FOUND, "The subscription no longer exists")
                    CoverBaseline.Subscription(detail.facts())
                }
            }
        }
    }

    open suspend fun prepareImport(uri: Uri, assetId: String): MediaResult<PreparedImage> = reconcilePreparationCancellation(assetId) {
        // Store registration/close fencing is local and short. Never retain the shared
        // Room/settings/snapshot mutex while awaiting source/native/Binder work.
        withContext(Dispatchers.IO) { storage {
            StoredValueValidation.requireUuid(assetId)
            requireOpen()
            value(images.prepareImport(resolver, uri, assetId))
        } }
    }

    open suspend fun restorePrepared(assetId: String): MediaResult<PreparedImage> = reconcilePreparationCancellation(assetId) {
        write {
            StoredValueValidation.requireUuid(assetId)
            value(images.restorePrepared(assetId))
        }
    }

    open suspend fun discardPrepared(assetId: String): MediaResult<Boolean> = write {
        StoredValueValidation.requireUuid(assetId)
        value(images.discardPreparedAsset(assetId))
    }

    /** Memory-only cancellation reconciliation: never delete recoverable private image bytes. */
    internal suspend fun releasePreparedLease(assetId: String) = withContext(NonCancellable + Dispatchers.IO) {
        coordinator.withWrite { images.releasePrepared(assetId) }
    }

    private suspend fun <T> reconcilePreparationCancellation(assetId: String, block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        releasePreparedLease(assetId)
        throw cancelled
    }

    open suspend fun commit(command: CoverCommand): MediaResult<CoverCommitOutcome> = write {
        val saved = database.withTransaction {
            validateCommand(command)
            val expected = command.expected
            val target = targetBaseline(command)
            val current = currentBaseline(expected.target)
                ?: abort(MediaFailureKind.CONFLICT, "The loaded record no longer exists")
            if (sameFacts(current, target)) {
                verifyRegisteredAsset(command.newAsset)
                return@withTransaction CoverCommitOutcome(current, true)
            }
            if (!sameFacts(current, expected)) abort(MediaFailureKind.CONFLICT, "The record changed while its cover was selected")

            command.newAsset?.let { asset ->
                val existing = database.mediaDao().find(asset.id)
                if (existing != null) {
                    if (existing != asset) abort(MediaFailureKind.CONFLICT, "The asset identity belongs to different immutable bytes")
                    value(images.verifyAsset(existing))
                } else {
                    val prepared = value(images.restorePrepared(asset.id))
                    if (prepared.asset != asset) abort(MediaFailureKind.CONFLICT, "The prepared asset changed after selection")
                    val committed = value(images.commitPreparedAsset(prepared))
                    if (committed != asset) abort(MediaFailureKind.CONFLICT, "The committed file does not match its fixed asset identity")
                    // A complete, verified file precedes both metadata and its business reference.
                    // Room rollback cannot undo this file; its durable ownership proof is retained.
                    database.mediaDao().insert(asset)
                }
            }
            applyCover(expected, target)
            val actual = currentBaseline(expected.target)
                ?: abort(MediaFailureKind.CONFLICT, "The record disappeared during cover update")
            if (!sameFacts(actual, target)) abort(MediaFailureKind.CONFLICT, "The complete cover target could not be verified")
            CoverCommitOutcome(actual, false)
        }
        command.newAsset?.let { images.releasePrepared(it.id) }
        saved
    }

    /** Read-only current-target reconciliation; it does not prove a permanent operation history. */
    open suspend fun verifyCommitted(command: CoverCommand): MediaResult<CoverBaseline> = snapshot {
        database.withTransaction {
            validateCommand(command)
            val current = currentBaseline(command.expected.target)
                ?: abort(MediaFailureKind.CONFLICT, "The saved record no longer exists")
            if (!sameFacts(current, targetBaseline(command))) abort(MediaFailureKind.CONFLICT, "The saved cover target has changed")
            verifyRegisteredAsset(command.newAsset)
            current
        }
    }

    /** Complete bounded bytes leave the snapshot; no unprotected file handle reaches Compose. */
    open suspend fun openAsset(id: String): MediaResult<StoredImage> = snapshot {
        StoredValueValidation.requireUuid(id)
        val asset = database.mediaDao().find(id)
            ?: abort(MediaFailureKind.NOT_FOUND, "The saved cover metadata is missing")
        value(images.openAsset(asset))
    }

    open suspend fun removeUnreferenced(): MediaResult<CleanupReport> = write {
        val proven = value(images.managedAssetsForCleanup())
        val removed = mutableListOf<String>()
        val failures = mutableListOf<CleanupFailure>()
        val active = mutableListOf<String>()
        // Unknown files/receipts are not candidates. A UUID filename alone is never ownership.
        for (asset in proven.distinctBy { it.id }) {
            if (images.isPreparedActive(asset.id)) {
                active += asset.id
                continue
            }
            when (val deletion = removeManagedAssetWithinWrite(database, images, asset) {
                images.removeAfterReferenceCheck(asset)
            }) {
                is MediaResult.Success -> if (deletion.value) removed += asset.id
                is MediaResult.Failure -> if (deletion.kind == MediaFailureKind.IN_USE) {
                    // A normal referenced cover is protected too, but is not an active editor.
                    if (images.isPreparedActive(asset.id)) active += asset.id
                } else failures += CleanupFailure(asset.id, deletion.kind, deletion.message)
            }
        }
        CleanupReport(removed, failures, active)
    }

    private suspend fun verifyRegisteredAsset(asset: LocalMediaAssetEntity?) {
        if (asset == null) return
        if (database.mediaDao().find(asset.id) != asset) abort(MediaFailureKind.CONFLICT, "The saved cover metadata no longer matches")
        value(images.verifyAsset(asset))
    }

    private fun validateCommand(command: CoverCommand) {
        val expected = command.expected
        val identities = listOf(command.operationId, expected.target.id) + listOfNotNull(expected.coverAssetId, command.newAsset?.id)
        if (identities.any { !StoredValueValidation.isUuid(it) }) abort(MediaFailureKind.PATH, "Cover identities must be canonical UUID strings")
        if (!command.writtenAt.isAfter(expected.updatedAt)) abort(MediaFailureKind.CONFLICT, "A cover revision must follow the loaded revision")
        when (expected) {
            is CoverBaseline.Project -> {
                if (expected.events.any { it.projectId != expected.project.id } || expected.events.map { it.id }.distinct().size != expected.events.size) {
                    abort(MediaFailureKind.CONFLICT, "The loaded project timeline is not complete and distinct")
                }
            }
            is CoverBaseline.Subscription -> {
                val facts = expected.facts
                if (facts.renewals.any { it.subscriptionId != facts.subscription.id } || facts.stateChanges.any { it.subscriptionId != facts.subscription.id } ||
                    facts.renewals.map { it.id }.distinct().size != facts.renewals.size || facts.stateChanges.map { it.id }.distinct().size != facts.stateChanges.size) {
                    abort(MediaFailureKind.CONFLICT, "The loaded subscription timelines are not complete and distinct")
                }
            }
        }
    }

    private fun targetBaseline(command: CoverCommand): CoverBaseline = when (val expected = command.expected) {
        is CoverBaseline.Project -> expected.copy(project = expected.project.copy(coverAssetId = command.newAsset?.id, updatedAt = command.writtenAt))
        is CoverBaseline.Subscription -> expected.copy(facts = expected.facts.copy(
            subscription = expected.facts.subscription.copy(coverAssetId = command.newAsset?.id, updatedAt = command.writtenAt),
        ))
    }

    /** Caller already owns the coordinator and the Room transaction. */
    private suspend fun currentBaseline(target: CoverTarget): CoverBaseline? = when (target.type) {
        CoverTargetType.PROJECT -> database.projectDao().findProject(target.id)?.let { project ->
            CoverBaseline.Project(project, database.projectDao().eventsForProjects(listOf(target.id)))
        }
        CoverTargetType.SUBSCRIPTION -> database.subscriptionDao().findSubscription(target.id)?.let { subscription ->
            CoverBaseline.Subscription(SubscriptionFacts(subscription,
                database.subscriptionDao().renewalsForSubscriptions(listOf(target.id)),
                database.subscriptionDao().statesForSubscriptions(listOf(target.id))))
        }
    }

    private fun sameFacts(first: CoverBaseline, second: CoverBaseline): Boolean = when {
        first is CoverBaseline.Project && second is CoverBaseline.Project -> first.project == second.project &&
            first.events.size == second.events.size && first.events.toSet() == second.events.toSet()
        first is CoverBaseline.Subscription && second is CoverBaseline.Subscription -> first.facts.subscription == second.facts.subscription &&
            first.facts.renewals.size == second.facts.renewals.size && first.facts.renewals.toSet() == second.facts.renewals.toSet() &&
            first.facts.stateChanges.size == second.facts.stateChanges.size && first.facts.stateChanges.toSet() == second.facts.stateChanges.toSet()
        else -> false
    }

    private suspend fun applyCover(expected: CoverBaseline, target: CoverBaseline) {
        val count = when (expected) {
            is CoverBaseline.Project -> {
                val old = expected.project
                val new = (target as CoverBaseline.Project).project
                database.projectDao().updateCoverIfUnchanged(old.id, old.name, old.categoryId, old.startedOn, old.endedOn,
                    old.endReason, old.coverAssetId, old.note, old.createdAt, old.updatedAt, new.coverAssetId, new.updatedAt)
            }
            is CoverBaseline.Subscription -> {
                val old = expected.facts.subscription
                val new = (target as CoverBaseline.Subscription).facts.subscription
                database.subscriptionDao().updateCoverIfUnchanged(old.id, old.name, old.categoryId, old.coverAssetId,
                    old.note, old.createdAt, old.updatedAt, new.coverAssetId, new.updatedAt)
            }
        }
        if (count != 1) abort(MediaFailureKind.CONFLICT, "The complete loaded record changed before its cover update")
    }

    private fun requireOpen() {
        if (!database.isOpen) abort(MediaFailureKind.DATABASE, "The current data generation is closed")
    }
    private suspend fun <T> write(block: suspend () -> T): MediaResult<T> = withContext(Dispatchers.IO) {
        storage { coordinator.withWrite { requireOpen(); block() } }
    }
    private suspend fun <T> snapshot(block: suspend () -> T): MediaResult<T> = withContext(Dispatchers.IO) {
        storage { coordinator.withSnapshot { requireOpen(); block() } }
    }
    private suspend fun <T> storage(block: suspend () -> T): MediaResult<T> = try {
        MediaResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: MediaAbort) {
        failure.result
    } catch (_: SQLiteConstraintException) {
        MediaResult.Failure(MediaFailureKind.CONFLICT, "The stored cover identity or relationship conflicts")
    } catch (_: SQLiteException) {
        MediaResult.Failure(MediaFailureKind.DATABASE, "The local database operation failed; keep the current input and retry safely")
    } catch (_: IOException) {
        MediaResult.Failure(MediaFailureKind.IO, "The private media operation failed")
    } catch (_: IllegalArgumentException) {
        MediaResult.Failure(MediaFailureKind.PATH, "A stored media identity or value is invalid")
    } catch (_: IllegalStateException) {
        MediaResult.Failure(MediaFailureKind.DATABASE, "The current local data could not be read or written")
    }
    private fun <T> value(result: MediaResult<T>): T = when (result) {
        is MediaResult.Success -> result.value
        is MediaResult.Failure -> throw MediaAbort(result)
    }
    private fun abort(kind: MediaFailureKind, message: String): Nothing = throw MediaAbort(MediaResult.Failure(kind, message))
    private class MediaAbort(val result: MediaResult.Failure) : RuntimeException()

    companion object {
        /** Production-owned public file removal takes this lock before any file mutex. */
        internal suspend fun removeManagedAsset(
            database: LithicDatabase,
            coordinator: LocalWriteCoordinator,
            images: LocalImageStore,
            asset: LocalMediaAssetEntity,
            rawRemove: suspend () -> MediaResult<Boolean>,
        ): MediaResult<Boolean> = withContext(Dispatchers.IO) {
            coordinator.withWrite { removeManagedAssetWithinWrite(database, images, asset, rawRemove) }
        }

        /** Shared by both public owned-store removal and GC, with no nested coordinator lock. */
        internal suspend fun removeManagedAssetWithinWrite(
            database: LithicDatabase,
            images: LocalImageStore,
            asset: LocalMediaAssetEntity,
            rawRemove: suspend () -> MediaResult<Boolean>,
        ): MediaResult<Boolean> = try {
            if (!database.isOpen) {
                MediaResult.Failure(MediaFailureKind.DATABASE, "The current data generation is closed")
            } else if (database.mediaDao().isReferenced(asset.id)) {
                MediaResult.Failure(MediaFailureKind.IN_USE, "The image is still referenced by a project or subscription")
            } else {
                // Ownership must be proven before unregistering even an unreferenced row.
                // Unknown files, replaced inodes and malformed receipts retain their metadata.
                when (val owned = images.validateOwnedRemoval(asset)) {
                    is MediaResult.Failure -> owned
                    is MediaResult.Success -> {
                        val metadata = database.withTransaction {
                            if (database.mediaDao().isReferenced(asset.id)) {
                                return@withTransaction MediaResult.Failure(MediaFailureKind.IN_USE, "The image gained a business reference")
                            }
                            val stored = database.mediaDao().find(asset.id)
                            if (stored != null && stored != asset) {
                                return@withTransaction MediaResult.Failure(MediaFailureKind.CONFLICT, "Stored media disagrees with its fixed asset identity")
                            }
                            if (stored != null) {
                                val changed = database.mediaDao().deleteIfUnreferenced(asset.id, asset.relativePath,
                                    asset.mime, asset.width, asset.height, asset.byteLength, asset.sha256)
                                if (changed != 1) {
                                    return@withTransaction MediaResult.Failure(MediaFailureKind.CONFLICT, "The asset could not be safely unregistered")
                                }
                                check(database.mediaDao().find(asset.id) == null) { "The image metadata was not removed" }
                            }
                            MediaResult.Success(stored != null)
                        }
                        when (metadata) {
                            is MediaResult.Failure -> metadata
                            is MediaResult.Success -> when (val physical = rawRemove()) {
                                is MediaResult.Failure -> physical
                                is MediaResult.Success -> MediaResult.Success(physical.value || metadata.value)
                            }
                        }
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SQLiteConstraintException) {
            MediaResult.Failure(MediaFailureKind.CONFLICT, "The stored image relationship conflicts")
        } catch (_: SQLiteException) {
            MediaResult.Failure(MediaFailureKind.DATABASE, "The local image metadata operation failed")
        } catch (_: IOException) {
            MediaResult.Failure(MediaFailureKind.IO, "The private image removal failed")
        } catch (_: IllegalArgumentException) {
            MediaResult.Failure(MediaFailureKind.PATH, "The image ownership metadata is invalid")
        } catch (_: IllegalStateException) {
            MediaResult.Failure(MediaFailureKind.DATABASE, "The current image metadata could not be safely read or written")
        }
    }
}

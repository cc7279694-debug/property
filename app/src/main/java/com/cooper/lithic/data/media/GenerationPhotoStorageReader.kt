package com.cooper.lithic.data.media

import android.database.Cursor
import androidx.room.withTransaction
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.settings.SettingsStorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Reads integer metadata aggregates only. No media path, file, bitmap, cache or cleanup is involved.
 * The current ledger includes ended projects and stopped subscriptions; their cover references count.
 */
internal class GenerationPhotoStorageReader(
    private val generation: ManagedGeneration,
    private val isCurrentOwner: () -> Boolean,
) : PhotoStorageReader {
    private val readerIdentity = Any()
    override val generationId: String get() = generation.id
    override val ownerKey: Any get() = readerIdentity

    override fun isCurrent(): Boolean = isCurrentOwner() &&
        !generation.coordinator.isRetired && generation.database.isOpen

    override suspend fun read(): PhotoStorageReadResult = withContext(Dispatchers.IO) {
        val caller = currentCoroutineContext()
        fun checkpoint() {
            caller.ensureActive()
            if (!isCurrent()) throw OwnerUnavailable()
        }
        try {
            checkpoint()
            val summary = generation.coordinator.withSnapshot {
                checkpoint()
                // Shutdown need not retire the coordinator. Check this exact settings Owner before
                // any Room access so a queued read cannot reopen a previously closed database.
                generation.settings.backupRecordZoneWithinSnapshot(generation.coordinator)
                checkpoint()
                generation.database.withTransaction {
                    checkpoint()
                    val database = generation.database.openHelper.readableDatabase
                    val metadata = database.query(METADATA).use { cursor ->
                        require(cursor.columnCount == 3 && cursor.moveToFirst()) { "Missing photo metadata aggregates" }
                        val result = Triple(cursor.integer(0), cursor.integer(1), cursor.integer(2))
                        require(!cursor.moveToNext()) { "Ambiguous photo metadata aggregates" }
                        result
                    }
                    checkpoint()
                    val references = database.query(REFERENCED_COUNT).use { cursor -> cursor.singleInteger() }
                    checkpoint()
                    val missing = database.query(MISSING_REFERENCE_COUNT).use { cursor -> cursor.singleInteger() }
                    checkpoint()
                    val historicalBytes = database.query(HISTORICAL_BYTES).use { cursor -> cursor.singleInteger() }
                    checkpoint()
                    photoStorageSummary(metadata.first, references, metadata.second,
                        missingReferences = missing, invalidLengths = metadata.third,
                        estimatedHistoricalBytes = historicalBytes)
                }
            }
            checkpoint()
            PhotoStorageReadResult.Ready(summary)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: OwnerUnavailable) {
            PhotoStorageReadResult.Unavailable(PhotoStorageReadFailure.OWNER_UNAVAILABLE)
        } catch (_: SettingsStorageException) {
            PhotoStorageReadResult.Unavailable(PhotoStorageReadFailure.OWNER_UNAVAILABLE)
        } catch (_: IllegalArgumentException) {
            PhotoStorageReadResult.Unavailable(PhotoStorageReadFailure.INCONSISTENT_METADATA)
        } catch (_: Exception) {
            // SQLite integer SUM overflow, unreadable storage and other query failures stay unknown.
            // In particular, never convert an overflow through Double or a failed read into zero.
            PhotoStorageReadResult.Unavailable(if (!isCurrent()) PhotoStorageReadFailure.OWNER_UNAVAILABLE
                else PhotoStorageReadFailure.READ_FAILED)
        }
    }

    private fun Cursor.integer(index: Int): Long {
        require(getType(index) == Cursor.FIELD_TYPE_INTEGER) { "Photo aggregate is not an exact integer" }
        return getLong(index)
    }

    private fun Cursor.singleInteger(): Long {
        require(columnCount == 1 && moveToFirst()) { "Missing photo reference count" }
        val result = integer(0)
        require(!moveToNext()) { "Ambiguous photo reference count" }
        return result
    }

    private class OwnerUnavailable : Exception()

    private companion object {
        // Fixed, read-only SQL in the data facade. UNION deduplicates across both record types,
        // including records in history, without allocating their complete business rows in memory.
        const val COVER_REFERENCES = """
            SELECT coverAssetId FROM long_term_projects WHERE coverAssetId IS NOT NULL
            UNION SELECT coverAssetId FROM subscriptions WHERE coverAssetId IS NOT NULL
        """
        const val METADATA = """
            SELECT COUNT(*), COALESCE(SUM(byteLength), 0),
                COUNT(CASE WHEN typeof(byteLength) <> 'integer' OR byteLength < 0 THEN 1 END)
            FROM local_media_assets
        """
        const val REFERENCED_COUNT = "SELECT COUNT(*) FROM ($COVER_REFERENCES)"
        const val MISSING_REFERENCE_COUNT = """
            SELECT COUNT(*) FROM ($COVER_REFERENCES) AS covers
            LEFT JOIN local_media_assets AS media ON covers.coverAssetId = media.id
            WHERE media.id IS NULL
        """
        // Counts registered lengths only, including neither physical allocation nor a deletion guarantee.
        const val HISTORICAL_BYTES = """
            SELECT COALESCE(SUM(media.byteLength), 0) FROM local_media_assets AS media
            WHERE NOT EXISTS (
                SELECT 1 FROM ($COVER_REFERENCES) AS covers WHERE covers.coverAssetId = media.id
            )
        """
    }
}

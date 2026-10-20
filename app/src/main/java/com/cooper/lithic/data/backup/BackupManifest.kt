package com.cooper.lithic.data.backup

import com.cooper.lithic.data.backup.BackupEntryFiles.hex
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class BackupFileEntry(val path: String, val byteLength: Long, val sha256: String) {
    init {
        BackupEntryFiles.requirePath(path)
        require(byteLength >= 0 && Regex("[0-9a-f]{64}").matches(sha256))
    }
    companion object {
        fun fromBytes(path: String, bytes: ByteArray) = BackupFileEntry(path, bytes.size.toLong(),
            java.security.MessageDigest.getInstance("SHA-256").digest(bytes).hex())
    }
}

/** Stable logical format v1; never serialize Room classes, database paths or permission state. */
@Serializable
internal data class BackupManifestV1(
    val formatVersion: Int = 1,
    val minReaderFormat: Int = 1,
    val producerAppVersion: String,
    val sourceDatabaseSchemaVersion: Int = 2,
    val createdAt: String,
    val recordZoneId: String,
    val recordCounts: Map<String, Long>,
    val files: List<BackupFileEntry>,
) {
    fun encode(): ByteArray = JSON.encodeToString(this).toByteArray(Charsets.UTF_8)

    companion object {
        private val JSON = Json { encodeDefaults = true }
        /** Only no-media synthetic/core artifacts can reach the archive writer in M11-A. */
        fun complete(core: PreparedBackupCore, producer: String, createdAt: Instant): BackupManifestV1 {
            require(producer.isNotBlank() && producer.length <= 128 && producer.none { it.code < 32 })
            ZoneId.of(core.recordZoneId)
            require(core.recordCounts.keys == setOf("categories", "long_term_projects", "project_money_events", "local_media_assets", "subscriptions", "subscription_renewals", "subscription_state_changes"))
            require(core.recordCounts.values.all { it >= 0 } && core.recordCounts.values.fold(0L, Math::addExact) < BackupLimits.MAX_RECORDS)
            require(core.requiredMediaPaths.isEmpty() && core.recordCounts.getValue("local_media_assets") == 0L) { "Media integration has not proved a complete payload" }
            require(core.entries.map { it.path }.toSet() == setOf("data.json", "settings.json") && core.entries.size == 2)
            core.entries.forEach { BackupEntryFiles.verify(core.directory, it) }
            return BackupManifestV1(producerAppVersion = producer, createdAt = createdAt.toString(),
                recordZoneId = core.recordZoneId, recordCounts = core.recordCounts.toSortedMap(), files = core.entries.sortedBy { it.path })
        }
    }
}

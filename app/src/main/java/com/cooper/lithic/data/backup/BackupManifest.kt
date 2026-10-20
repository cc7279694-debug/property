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
        /** A complete archive declares every registered media payload, including unreferenced assets. */
        fun complete(core: PreparedBackupCore, producer: String, createdAt: Instant): BackupManifestV1 {
            require(producer.isNotBlank() && producer.length <= 128 && producer.none { it.code < 32 })
            ZoneId.of(core.recordZoneId)
            require(core.recordCounts.keys == setOf("categories", "long_term_projects", "project_money_events", "local_media_assets", "subscriptions", "subscription_renewals", "subscription_state_changes"))
            require(core.recordCounts.values.all { it >= 0 } && core.recordCounts.values.fold(0L, Math::addExact) < BackupLimits.MAX_RECORDS)
            require(core.requiredMediaPaths.all { it.startsWith("media/") })
            core.requiredMediaPaths.forEach(BackupEntryFiles::requirePath)
            require(core.recordCounts.getValue("local_media_assets") == core.requiredMediaPaths.size.toLong()) { "Media identities are incomplete or duplicated" }
            val paths = core.entries.map { it.path }
            require(paths.size == paths.toSet().size && paths.toSet() == setOf("data.json", "settings.json") + core.requiredMediaPaths) { "Missing, duplicate or undeclared payload" }
            require(core.entries.size + 1 < BackupLimits.MAX_ENTRIES)
            core.entries.forEach { entry ->
                val limit = when (entry.path) {
                    "data.json" -> BackupLimits.MAX_DATA_BYTES
                    "settings.json" -> BackupLimits.MAX_SETTINGS_BYTES
                    else -> BackupLimits.MAX_MEDIA_BYTES
                }
                require(entry.byteLength < limit) { "Payload reached its resource limit" }
            }
            core.entries.forEach { BackupEntryFiles.verify(core.directory, it) }
            val result = BackupManifestV1(producerAppVersion = producer, createdAt = createdAt.toString(),
                recordZoneId = core.recordZoneId, recordCounts = core.recordCounts.toSortedMap(), files = core.entries.sortedBy { it.path })
            require(core.entries.fold(result.encode().size.toLong()) { total, entry -> Math.addExact(total, entry.byteLength) } < BackupLimits.MAX_TOTAL_BYTES)
            return result
        }
    }
}

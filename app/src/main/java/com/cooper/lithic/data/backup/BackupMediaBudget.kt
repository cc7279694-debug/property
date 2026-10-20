package com.cooper.lithic.data.backup

/** Pre-admit before opening any source: payload staging can never grow without a total bound.
 * The completed manifest's actual bytes are additionally admitted by the archive writer.
 */
internal fun requireMediaSnapshotBudget(core: List<BackupFileEntry>, media: List<BackupMediaAssetV1>) {
    require(core.size.toLong() + media.size.toLong() + 1 < BackupLimits.MAX_ENTRIES)
    var total = core.fold(0L) { sum, entry -> Math.addExact(sum, entry.byteLength) }
    for (entry in media) {
        require(entry.byteLength in 1 until BackupLimits.MAX_MEDIA_BYTES)
        total = Math.addExact(total, entry.byteLength)
    }
    require(total < BackupLimits.MAX_TOTAL_BYTES) { "Media snapshot exceeds its payload budget" }
}

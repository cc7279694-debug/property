package com.cooper.lithic.data.backup

import org.junit.Assert.*
import org.junit.Test

class BackupMediaBudgetTest {
    @Test fun entry_count_and_total_bytes_are_rejected_before_copying_any_media() {
        val asset = BackupMediaAssetV1("id","media/x.png","image/png",1,1,2_097_151,"a".repeat(64))
        for (media in listOf(List(9998){asset}, List(513){asset})) {
            try { requireMediaSnapshotBudget(listOf(BackupFileEntry("data.json",1024,"a".repeat(64))),media);fail("No media IO may start") }
            catch (_:IllegalArgumentException) { }
        }
    }
    @Test fun an_empty_or_admitted_media_set_remains_valid() {
        val core=listOf(BackupFileEntry("data.json",1024,"a".repeat(64)))
        requireMediaSnapshotBudget(core,emptyList())
        requireMediaSnapshotBudget(core,listOf(BackupMediaAssetV1("id","media/x.png","image/png",1,1,42,"a".repeat(64))))
    }
}

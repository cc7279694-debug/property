package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test

class M16RestoreProofCompatibilityTest {
    @Test fun legacy_proof_uses_exact_original_v1_serializer_bytes() {
        val legacy=BackupDataV1(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList())
        val snapshot=BackupSnapshot(legacy,BackupSettingsV1("UTC"))
        val output=ByteArrayOutputStream();RestoreProofData.write(snapshot,1,output)
        assertArrayEquals(BackupJson.encodeData(legacy),output.toByteArray())
        assertFalse(output.toString("UTF-8").contains("replacements"))
    }
    @Test fun current_proof_hash_includes_eighth_table_even_when_empty() {
        val snapshot=BackupSnapshot(BackupDataV1(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList()),BackupSettingsV1("UTC"))
        val output=ByteArrayOutputStream();RestoreProofData.write(snapshot,2,output)
        assertArrayEquals(BackupJson.encodeData(snapshot.data),output.toByteArray())
        assertTrue(output.toString("UTF-8").contains("replacements"))
    }
    @Test fun legacy_projection_rejects_new_facts_instead_of_omitting_them_from_hash() {
        val empty=BackupSnapshot(BackupDataV1(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList()),BackupSettingsV1("UTC"))
        val withLink=empty.copy(data=empty.data.copy(replacements=listOf(BackupReplacementV2("a","b","c","2026-01-01T00:00:00Z"))))
        try { RestoreProofData.write(withLink,1,ByteArrayOutputStream());fail("New fact disappeared behind old proof") }
        catch(_:IllegalArgumentException) {}
    }
    @Test fun unknown_proof_data_format_is_rejected() {
        val snapshot=BackupSnapshot(BackupDataV1(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList()),BackupSettingsV1("UTC"))
        try { RestoreProofData.write(snapshot,99,ByteArrayOutputStream());fail("Unknown proof format was accepted") }
        catch(_:IllegalArgumentException) {}
    }
}

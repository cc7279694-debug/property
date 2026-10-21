package com.cooper.lithic.data.backup

import java.io.OutputStream

/** The generation metadata protocol remains format 1; only its explicitly identified data digest changes. */
internal object RestoreProofData {
    fun write(snapshot:BackupSnapshot,dataFormatVersion:Int,output:OutputStream) {
        when(dataFormatVersion) {
            1 -> BackupJson.writeData(snapshot.data.legacyProjection(),output)
            2 -> BackupJson.writeData(snapshot.data,output)
            else -> throw IllegalArgumentException("Unknown restoration proof data format")
        }
    }
}

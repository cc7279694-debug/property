package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream

/** UTF-8 JSON with stable row order and explicit nulls; no Android objects or type discriminators. */
@OptIn(ExperimentalSerializationApi::class)
object BackupJson {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        prettyPrint = false
    }

    /** Caller owns output and its limit/close protocol; all output errors propagate. */
    fun writeData(data: BackupDataV1, output: OutputStream) {
        json.encodeToStream(BackupDataV1.serializer(), data.ordered(), output)
    }

    fun writeData(data:BackupDataV2,output:OutputStream) {
        json.encodeToStream(BackupDataV2.serializer(),data.ordered(),output)
    }

    fun encodeData(data:BackupDataV2):ByteArray = ByteArrayOutputStream().use { output ->
        writeData(data,output);output.toByteArray()
    }

    fun writeSettings(settings: BackupSettingsV1, output: OutputStream) {
        json.encodeToStream(BackupSettingsV1.serializer(), settings, output)
    }

    fun encodeData(data: BackupDataV1): ByteArray = ByteArrayOutputStream().use { output ->
        writeData(data, output)
        output.toByteArray()
    }

    fun encodeSettings(settings: BackupSettingsV1): ByteArray = ByteArrayOutputStream().use { output ->
        writeSettings(settings, output)
        output.toByteArray()
    }
}

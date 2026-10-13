package com.cooper.lithic.data.local

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import org.json.JSONObject

/** A real frozen-v1 file, built from the original export rather than the current Room owner. */
internal object V1DatabaseFixture {
    const val IDENTITY = "bdbb5a3f035020f10628f46ddfa4551d"
    const val LEGACY_IDENTITY = "830458787dc2277d20c2b49e42bbc8d2"
    const val EXPORT_SHA256 = "351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b"
    val TABLES = setOf("categories", "long_term_projects", "project_money_events", "local_media_assets")
    val CURRENT_PROJECT_ID = id(201)
    val ENDED_PROJECT_ID = id(202)
    val FUTURE_ENDED_PROJECT_ID = id(203)
    val CORRECTED_INITIAL_EVENT_ID = id(301)
    val END_SETTLEMENT_EVENT_ID = id(307)
    val MEDIA_PATHS = listOf("media/retained-a.png", "media/retained-b.png", "media/unreferenced.png")

    private val imageBytes: ByteArray get() = Base64.decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jT8QAAAAASUVORK5CYII=",
        Base64.DEFAULT,
    )

    fun create(file: File, keepCommittedWal: Boolean = false): SQLiteDatabase {
        check(!file.exists()) { "A v1 fixture cannot overwrite an existing file" }
        check(file.parentFile?.isDirectory == true)
        val exportedBytes = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.cooper.lithic.data.local.LithicDatabase/1.json").use { it.readBytes() }
        check(sha256(exportedBytes) == EXPORT_SHA256) { "The frozen v1 export changed" }
        val schema = JSONObject(exportedBytes.toString(Charsets.UTF_8)).getJSONObject("database")
        check(schema.getInt("version") == 1 && schema.getString("identityHash") == IDENTITY)
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            raw.setForeignKeyConstraintsEnabled(true)
            if (keepCommittedWal) {
                check(raw.enableWriteAheadLogging())
                raw.rawQuery("PRAGMA wal_autocheckpoint=0", null).use { check(it.moveToFirst()) }
            }
            raw.beginTransaction()
            try {
                val entities = schema.getJSONArray("entities")
                for (index in 0 until entities.length()) {
                    val entity = entities.getJSONObject(index)
                    val table = entity.getString("tableName")
                    check(table in TABLES)
                    raw.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.optJSONArray("indices")
                    if (indices != null) for (item in 0 until indices.length()) {
                        raw.execSQL(indices.getJSONObject(item).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
                val setup = schema.getJSONArray("setupQueries")
                for (index in 0 until setup.length()) raw.execSQL(setup.getString(index))
                seed(raw)
                raw.version = 1
                raw.setTransactionSuccessful()
            } finally {
                raw.endTransaction()
            }
            check(raw.version == 1 && snapshot(raw).identity == IDENTITY)
            raw.rawQuery("PRAGMA foreign_key_check", null).use { check(!it.moveToFirst()) }
            return raw
        } catch (failure: Throwable) {
            raw.close()
            throw failure
        }
    }

    fun writeMedia(directory: File) {
        val media = File(directory, "media")
        check(media.isDirectory || media.mkdir())
        MEDIA_PATHS.forEach { path -> File(directory, path).writeBytes(imageBytes) }
    }

    fun snapshot(file: File): Snapshot = SQLiteDatabase.openDatabase(
        file.path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
    ).use(::snapshot)

    fun snapshot(raw: SQLiteDatabase): Snapshot {
        val rows = TABLES.sorted().associateWith { table ->
            raw.rawQuery("SELECT * FROM `$table` ORDER BY id", null).use { cursor ->
                val values = mutableListOf<List<SqlValue>>()
                while (cursor.moveToNext()) values += cursor.columnNames.indices.map { column ->
                    when (val type = cursor.getType(column)) {
                        Cursor.FIELD_TYPE_NULL -> SqlValue(type, null)
                        Cursor.FIELD_TYPE_INTEGER -> SqlValue(type, cursor.getLong(column).toString())
                        Cursor.FIELD_TYPE_FLOAT -> SqlValue(type, cursor.getDouble(column).toString())
                        Cursor.FIELD_TYPE_STRING -> SqlValue(type, cursor.getString(column))
                        Cursor.FIELD_TYPE_BLOB -> SqlValue(type, cursor.getBlob(column).joinToString("") { "%02x".format(it) })
                        else -> error("Unknown SQLite storage class")
                    }
                }
                TableRows(cursor.columnNames.toList(), values)
            }
        }
        val schemaObjects = raw.rawQuery(
            "SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT GLOB 'sqlite_*' ORDER BY type,name",
            null,
        ).use { cursor ->
            val values = mutableListOf<SchemaObject>()
            while (cursor.moveToNext()) if (cursor.getString(2) in TABLES) {
                values += SchemaObject(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3))
            }
            values
        }
        val identity = raw.rawQuery("SELECT identity_hash FROM room_master_table WHERE id=42", null).use { cursor ->
            check(cursor.moveToFirst() && !cursor.isNull(0))
            cursor.getString(0).also { check(!cursor.moveToNext()) }
        }
        return Snapshot(raw.version, identity, LegacyFacts(rows, schemaObjects), businessTables(raw))
    }

    fun businessTables(raw: SQLiteDatabase): Set<String> = raw.rawQuery(
        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT GLOB 'sqlite_*' AND name NOT IN ('android_metadata','room_master_table') ORDER BY name",
        null,
    ).use { cursor ->
        buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
    }

    private fun seed(raw: SQLiteDatabase) {
        raw.execSQL("INSERT INTO categories VALUES(?,?,?,?,?)", arrayOf<Any>(LithicDatabase.PROJECT_FALLBACK_ID, "PROJECT", "其他", "其他", 1))
        raw.execSQL("INSERT INTO categories VALUES(?,?,?,?,?)", arrayOf<Any>(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, "SUBSCRIPTION", "其他", "其他", 1))
        raw.execSQL("INSERT INTO categories VALUES(?,?,?,?,?)", arrayOf<Any>(id(101), "PROJECT", "工具", "工具", 0))
        raw.execSQL("INSERT INTO categories VALUES(?,?,?,?,?)", arrayOf<Any>(id(102), "SUBSCRIPTION", "服务", "服务", 0))
        MEDIA_PATHS.forEachIndexed { index, path ->
            raw.execSQL("INSERT INTO local_media_assets VALUES(?,?,?,?,?,?,?)", arrayOf<Any>(id(401 + index), path, "image/png", 1, 1, imageBytes.size, sha256(imageBytes)))
        }
        val created = "2026-01-01T03:04:05.123456789Z"
        val updated = "2026-01-12T03:04:05.987654321Z"
        raw.execSQL("INSERT INTO long_term_projects VALUES(?,?,?,?,?,?,?,?,?,?)", arrayOf<Any?>(
            CURRENT_PROJECT_ID, "普通项目", LithicDatabase.PROJECT_FALLBACK_ID, day("2026-01-01"), null, null, id(401), "保留\n完整备注", created, updated,
        ))
        raw.execSQL("INSERT INTO long_term_projects VALUES(?,?,?,?,?,?,?,?,?,?)", arrayOf<Any>(
            ENDED_PROJECT_ID, "已结束项目", id(101), day("2026-01-02"), day("2026-01-09"), "卖出", id(402), "结束后真实结算", created, updated,
        ))
        raw.execSQL("INSERT INTO long_term_projects VALUES(?,?,?,?,?,?,?,?,?,?)", arrayOf<Any?>(
            FUTURE_ENDED_PROJECT_ID, "未来结束项目", id(101), day("2026-01-03"), day("2026-01-20"), "已记录边界", id(401), null, created, created,
        ))
        event(raw, 301, CURRENT_PROJECT_ID, "OUTLAY", 14683, "2026-01-01", "纠错后的初始事实", "INITIAL")
        event(raw, 302, CURRENT_PROJECT_ID, "OUTLAY", 100, "2026-01-02", "追加投入", "REGULAR")
        event(raw, 303, CURRENT_PROJECT_ID, "RECOVERY", 50, "2026-01-05", null, "REGULAR")
        event(raw, 304, ENDED_PROJECT_ID, "OUTLAY", 100000, "2026-01-01", "拥有前真实付款", "INITIAL")
        event(raw, 305, ENDED_PROJECT_ID, "OUTLAY", 2500, "2026-01-03", "修正后的投入", "REGULAR")
        event(raw, 306, ENDED_PROJECT_ID, "RECOVERY", 3000, "2026-01-04", "实际收回", "REGULAR")
        event(raw, 307, ENDED_PROJECT_ID, "RECOVERY", 20000, "2026-01-09", "唯一结束关联收回", "END_SETTLEMENT")
        event(raw, 308, ENDED_PROJECT_ID, "OUTLAY", 10000, "2026-01-10", "结束后的真实尾款", "REGULAR")
        event(raw, 309, ENDED_PROJECT_ID, "RECOVERY", 90000, "2026-01-11", "结束后的退款", "REGULAR")
        event(raw, 310, FUTURE_ENDED_PROJECT_ID, "OUTLAY", 0, "2026-01-03", "真实零元初始投入", "INITIAL")
    }

    private fun event(raw: SQLiteDatabase, number: Int, project: String, direction: String, cents: Long, date: String, note: String?, role: String) {
        raw.execSQL("INSERT INTO project_money_events VALUES(?,?,?,?,?,?,?,?)", arrayOf<Any?>(id(number), project, direction, cents, day(date), note, role, id(number + 500)))
    }

    fun id(number: Int): String = "80000000-0000-4000-8000-${number.toString().padStart(12, '0')}"
    private fun day(value: String): Long = LocalDate.parse(value).toEpochDay()
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    data class SqlValue(val storageClass: Int, val value: String?)
    data class TableRows(val columns: List<String>, val rows: List<List<SqlValue>>)
    data class SchemaObject(val type: String, val name: String, val owner: String, val sql: String?)
    data class LegacyFacts(val tables: Map<String, TableRows>, val schemaObjects: List<SchemaObject>)
    data class Snapshot(val version: Int, val identity: String, val legacy: LegacyFacts, val businessTables: Set<String>)
}

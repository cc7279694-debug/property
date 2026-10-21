package com.cooper.lithic.data.local

import android.database.sqlite.SQLiteDatabase
import java.util.Locale

/** Known Room identity plus actual SQLite structure, before any writable Room upgrade. */
internal object DatabaseSchemaValidation {
    private val v1Identities = setOf("bdbb5a3f035020f10628f46ddfa4551d", "830458787dc2277d20c2b49e42bbc8d2")
    // Verified against the actual v2 export and KSP RoomOpenDelegate on 2026-10-12.
    private val v2Identities = setOf("a8fd277b0ef08ce9cf59b704734d0c25", "c2791ca206586debaa4a6f2d03386e79")

    private val v3Identities = setOf("0c56480536dc34e026b703bd845dc307", "c8d9fdeec072099df560319f8b238ac9")

    fun requireKnownSchema(db: SQLiteDatabase) {
        val version = db.version
        val expected = when (version) {
            1 -> oldTables
            2 -> oldTables + subscriptionTables
            3 -> v3Tables
            else -> error("Existing database version has no registered migration")
        }
        requireTable(db, masterTable)
        db.rawQuery("SELECT id,identity_hash FROM room_master_table", null).use { cursor ->
            check(cursor.moveToFirst() && cursor.getInt(0) == 42 && !cursor.isNull(1)) { "Existing database has no valid Room identity" }
            val known = when (version) { 1 -> v1Identities; 2 -> v2Identities; else -> v3Identities }
            check(cursor.getString(1) in known && !cursor.moveToNext()) { "Existing database has an unknown Room identity" }
        }
        val expectedObjects = expected.flatMap { table ->
            listOf("table" to table.name) + table.indices.map { "index" to it.name }
        }.toSet()
        val actualObjects = mutableSetOf<Pair<String, String>>()
        db.rawQuery("SELECT type,name,sql FROM sqlite_master WHERE name NOT GLOB 'sqlite_*' AND NOT (type='table' AND name IN ('room_master_table','android_metadata'))", null).use { cursor ->
            while (cursor.moveToNext()) {
                val type = cursor.getString(0)
                val name = cursor.getString(1)
                val sql = cursor.getString(2).orEmpty().uppercase(Locale.ROOT)
                check(!sql.contains("CREATE VIRTUAL TABLE")) { "Virtual tables are not part of the known schema" }
                actualObjects += type to name
            }
        }
        check(actualObjects == expectedObjects) { "Existing database objects do not match the known schema" }
        expected.forEach { requireTable(db, it) }
        db.rawQuery("PRAGMA quick_check(1)", null).use { cursor ->
            check(cursor.moveToFirst() && cursor.getString(0) == "ok" && !cursor.moveToNext()) { "SQLite integrity validation failed" }
        }
        db.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
            check(!cursor.moveToFirst()) { "The database contains broken foreign-key relationships" }
        }
    }

    private fun requireTable(db: SQLiteDatabase, expected: Table) {
        val columns = mutableListOf<Column>()
        db.rawQuery("PRAGMA table_info(`${expected.name}`)", null).use { cursor ->
            while (cursor.moveToNext()) {
                columns += Column(cursor.getString(1), cursor.getString(2).uppercase(Locale.ROOT), cursor.getInt(3) != 0, cursor.getInt(5), if (cursor.isNull(4)) null else cursor.getString(4))
            }
        }
        check(columns == expected.columns) { "Existing table columns do not match the known schema: ${expected.name}" }
        // Newer SQLite exposes hidden/generated columns separately. API26 uses table_info.
        db.rawQuery("PRAGMA table_xinfo(`${expected.name}`)", null).use { cursor ->
            val hidden = cursor.getColumnIndex("hidden")
            if (hidden >= 0) while (cursor.moveToNext()) check(cursor.getInt(hidden) == 0) { "Hidden/generated columns are not part of the known schema" }
        }
        val foreignKeys = mutableSetOf<ForeignKey>()
        db.rawQuery("PRAGMA foreign_key_list(`${expected.name}`)", null).use { cursor ->
            while (cursor.moveToNext()) {
                check(cursor.getInt(1) == 0) { "Unexpected composite foreign key" }
                foreignKeys += ForeignKey(cursor.getString(3), cursor.getString(2), cursor.getString(4), cursor.getString(5), cursor.getString(6))
            }
        }
        check(foreignKeys == expected.foreignKeys) { "Existing foreign keys do not match the known schema: ${expected.name}" }
        val indices = mutableListOf<Index>()
        val primaryColumns = expected.columns.filter { it.primaryKey > 0 }.sortedBy { it.primaryKey }.map { it.name }
        // The Room identity table uses INTEGER PRIMARY KEY's rowid alias, not an auto-index.
        val rowidPrimaryKey = expected.name == "room_master_table"
        var primaryIndexCount = 0
        db.rawQuery("PRAGMA index_list(`${expected.name}`)", null).use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1)
                val origin = cursor.getString(cursor.getColumnIndexOrThrow("origin"))
                check(cursor.getInt(cursor.getColumnIndexOrThrow("partial")) == 0) { "Unexpected partial index" }
                val fields = requireIndexColumns(db, name)
                if (origin == "pk") {
                    check(!rowidPrimaryKey && name.startsWith("sqlite_autoindex_") && cursor.getInt(2) == 1 && fields == primaryColumns) {
                        "Existing primary-key index does not match the known schema: ${expected.name}"
                    }
                    primaryIndexCount++
                    continue
                }
                check(origin == "c") { "Unexpected automatic index" }
                indices += Index(name, cursor.getInt(2) != 0, fields)
            }
        }
        check(primaryIndexCount == if (rowidPrimaryKey) 0 else 1) { "The known primary-key index is missing or unexpected: ${expected.name}" }
        check(indices.toSet() == expected.indices.toSet()) { "Existing indices do not match the known schema: ${expected.name}" }
    }

    /** Named and automatic PK indices must keep both key shape and string comparison semantics. */
    private fun requireIndexColumns(db: SQLiteDatabase, name: String): List<String> {
        val fields = mutableListOf<String>()
        db.rawQuery("PRAGMA index_xinfo(`$name`)", null).use { cursor ->
            while (cursor.moveToNext()) if (cursor.getInt(cursor.getColumnIndexOrThrow("key")) == 1) {
                check(cursor.getInt(1) >= 0 && !cursor.isNull(2)) { "Expression indices are not part of the known schema" }
                check(cursor.getInt(3) == 0 && cursor.getString(4) == "BINARY") { "Unexpected index order or collation" }
                fields += cursor.getString(2)
            }
        }
        return fields
    }

    private data class Column(val name: String, val type: String, val notNull: Boolean = true, val primaryKey: Int = 0, val defaultValue: String? = null)
    private data class ForeignKey(val child: String, val parent: String, val parentColumn: String = "id", val onUpdate: String = "NO ACTION", val onDelete: String = "RESTRICT")
    private data class Index(val name: String, val unique: Boolean, val columns: List<String>)
    private data class Table(val name: String, val columns: List<Column>, val indices: List<Index> = emptyList(), val foreignKeys: Set<ForeignKey> = emptySet())
    private fun id() = Column("id", "TEXT", primaryKey = 1)
    private fun text(name: String, nullable: Boolean = false) = Column(name, "TEXT", !nullable)
    private fun integer(name: String, nullable: Boolean = false) = Column(name, "INTEGER", !nullable)

    private val masterTable = Table("room_master_table", listOf(Column("id", "INTEGER", false, 1), text("identity_hash", true)))
    private val oldTables = listOf(
        Table("categories", listOf(id(), text("type"), text("name"), text("normalizedName"), integer("isFallback")),
            listOf(Index("index_categories_type_normalizedName", true, listOf("type", "normalizedName")))),
        Table("long_term_projects", listOf(id(), text("name"), text("categoryId"), integer("startedOn"), integer("endedOn", true),
            text("endReason", true), text("coverAssetId", true), text("note", true), text("createdAt"), text("updatedAt")),
            listOf(Index("index_long_term_projects_categoryId", false, listOf("categoryId")), Index("index_long_term_projects_coverAssetId", false, listOf("coverAssetId")),
                Index("index_long_term_projects_startedOn_id", false, listOf("startedOn", "id"))),
            setOf(ForeignKey("categoryId", "categories"), ForeignKey("coverAssetId", "local_media_assets"))),
        Table("project_money_events", listOf(id(), text("projectId"), text("direction"), integer("amountCents"), integer("occurredOn"), text("description", true), text("role"), text("operationId")),
            listOf(Index("index_project_money_events_projectId_occurredOn_id", false, listOf("projectId", "occurredOn", "id")), Index("index_project_money_events_operationId", true, listOf("operationId"))),
            setOf(ForeignKey("projectId", "long_term_projects"))),
        Table("local_media_assets", listOf(id(), text("relativePath"), text("mime"), integer("width"), integer("height"), integer("byteLength"), text("sha256")),
            listOf(Index("index_local_media_assets_relativePath", true, listOf("relativePath")))),
    )
    private val subscriptionTables = listOf(
        Table("subscriptions", listOf(id(), text("name"), text("categoryId"), text("coverAssetId", true), text("note", true), text("createdAt"), text("updatedAt")),
            listOf(Index("index_subscriptions_categoryId", false, listOf("categoryId")), Index("index_subscriptions_coverAssetId", false, listOf("coverAssetId"))),
            setOf(ForeignKey("categoryId", "categories"), ForeignKey("coverAssetId", "local_media_assets"))),
        Table("subscription_renewals", listOf(id(), text("subscriptionId"), integer("paidOn"), integer("effectiveFrom"), integer("paidUntilExclusive"), integer("reminderDueOn"), integer("amountCents"), text("cycle"), integer("standardDays"), text("operationId")),
            listOf(Index("index_subscription_renewals_operationId", true, listOf("operationId")),
                Index("index_subscription_renewals_subscriptionId_paidOn_id", false, listOf("subscriptionId", "paidOn", "id")),
                Index("index_subscription_renewals_subscriptionId_effectiveFrom_id", false, listOf("subscriptionId", "effectiveFrom", "id"))),
            setOf(ForeignKey("subscriptionId", "subscriptions"))),
        Table("subscription_state_changes", listOf(id(), text("subscriptionId"), integer("effectiveOn"), text("state"), text("reason", true), text("note", true), text("operationId")),
            listOf(Index("index_subscription_state_changes_operationId", true, listOf("operationId")),
                Index("index_subscription_state_changes_subscriptionId_effectiveOn", true, listOf("subscriptionId", "effectiveOn")),
                Index("index_subscription_state_changes_subscriptionId_effectiveOn_id", false, listOf("subscriptionId", "effectiveOn", "id"))),
            setOf(ForeignKey("subscriptionId", "subscriptions"))),
    )
    private val v3Tables = oldTables.map { table ->
        if (table.name == "project_money_events") table.copy(columns = table.columns +
            listOf(Column("basis", "TEXT", defaultValue = "'CASH'"), text("exchangeGroupId", true))) else table
    } + subscriptionTables + Table("replacements", listOf(id(), text("predecessorId"), text("successorId"), text("createdAt")),
        listOf(Index("index_replacements_predecessorId", true, listOf("predecessorId")),
            Index("index_replacements_successorId", true, listOf("successorId"))),
        setOf(ForeignKey("predecessorId", "long_term_projects"), ForeignKey("successorId", "long_term_projects")))

}

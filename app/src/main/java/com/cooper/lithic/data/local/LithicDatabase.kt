package com.cooper.lithic.data.local

import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.io.DataInputStream
import java.io.File

@Database(
    entities = [CategoryEntity::class, LongTermProjectEntity::class, ProjectMoneyEventEntity::class,
        LocalMediaAssetEntity::class, SubscriptionEntity::class, SubscriptionRenewalEntity::class,
        SubscriptionStateChangeEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class LithicDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun projectDao(): ProjectDao
    abstract fun mediaDao(): MediaDao
    abstract fun subscriptionDao(): SubscriptionDao

    /** Only fresh generation initialization may add absent defaults. Existing rows must agree. */
    suspend fun initializeDefaults() = withTransaction {
        DEFAULT_CATEGORIES.forEach { expected ->
            val existing = categoryDao().find(expected.id)
            if (existing == null) categoryDao().insert(expected)
            else check(existing == expected) { "A persisted fallback category is inconsistent" }
        }
    }

    /** Validate an existing generation's fixed defaults without inserting or repairing rows. */
    suspend fun requireDefaults() = withTransaction {
        DEFAULT_CATEGORIES.forEach { expected ->
            check(categoryDao().find(expected.id) == expected) { "An existing generation has missing or changed fallback categories" }
        }
    }

    companion object {
        const val PROJECT_FALLBACK_ID = "00000000-0000-4000-8000-000000000001"
        const val SUBSCRIPTION_FALLBACK_ID = "00000000-0000-4000-8000-000000000002"
        private val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(PROJECT_FALLBACK_ID, CategoryType.PROJECT, "其他", "其他", true),
            CategoryEntity(SUBSCRIPTION_FALLBACK_ID, CategoryType.SUBSCRIPTION, "其他", "其他", true),
        )

        /** Synchronous; callers must use an IO dispatcher. */
        fun open(context: Context, file: File, requireExisting: Boolean = false): LithicDatabase {
            val canonical = file.canonicalFile
            check(canonical.parentFile?.isDirectory == true) { "The database directory is missing" }
            check(!requireExisting || canonical.isFile) { "Existing database is missing" }
            if (canonical.exists()) {
                check(canonical.isFile && canonical.length() >= 100) { "An existing database is empty or invalid" }
                val header = ByteArray(16)
                DataInputStream(canonical.inputStream()).use { it.readFully(header) }
                check(header.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))) {
                    "An existing database has an invalid SQLite header"
                }
                // Do not let Room's writable onCreate adopt an existing v0/foreign file.
                // Query SQLite itself so committed schema/identity in WAL is respected.
                SQLiteDatabase.openDatabase(canonical.path, null,
                    SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
                    DatabaseErrorHandler { throw SQLiteDatabaseCorruptException("Existing database is corrupt; original files are preserved") },
                ).use { existing ->
                    DatabaseSchemaValidation.requireKnownSchema(existing)
                }
            }
            val database = Room.databaseBuilder(context.applicationContext, LithicDatabase::class.java, canonical.path)
                .addMigrations(*DatabaseMigrations.ALL)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .openHelperFactory(PreservingOpenHelperFactory())
                .build()
            try {
                val sqlite = database.openHelper.writableDatabase
                sqlite.query("PRAGMA quick_check(1)").use { cursor ->
                    check(cursor.moveToFirst() && cursor.getString(0) == "ok" && !cursor.moveToNext()) { "SQLite integrity validation failed" }
                }
                sqlite.query("PRAGMA foreign_key_check").use { cursor ->
                    check(!cursor.moveToFirst()) { "The database contains broken foreign-key relationships" }
                }
                return database
            } catch (failure: Throwable) {
                try { database.close() } catch (closeFailure: Throwable) { failure.addSuppressed(closeFailure) }
                throw failure
            }
        }
    }
}

/** Android's default corruption callback may delete the DB; this callback preserves evidence. */
private class PreservingOpenHelperFactory : SupportSQLiteOpenHelper.Factory {
    private val delegate = FrameworkSQLiteOpenHelperFactory()

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val original = configuration.callback
        val preserving = object : SupportSQLiteOpenHelper.Callback(original.version) {
            override fun onConfigure(db: SupportSQLiteDatabase) = original.onConfigure(db)
            override fun onCreate(db: SupportSQLiteDatabase) = original.onCreate(db)
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = original.onUpgrade(db, oldVersion, newVersion)
            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = original.onDowngrade(db, oldVersion, newVersion)
            override fun onOpen(db: SupportSQLiteDatabase) = original.onOpen(db)
            override fun onCorruption(db: SupportSQLiteDatabase) {
                throw SQLiteDatabaseCorruptException("The database is corrupt; original files are preserved")
            }
        }
        return delegate.create(
            SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
                .name(configuration.name)
                .callback(preserving)
                .noBackupDirectory(configuration.useNoBackupDirectory)
                .allowDataLossOnRecovery(false)
                .build(),
        )
    }
}

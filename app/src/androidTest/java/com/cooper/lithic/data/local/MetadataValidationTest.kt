package com.cooper.lithic.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Metadata is admitted through the production entities/DAOs, without processing images. */
@RunWith(AndroidJUnit4::class)
class MetadataValidationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var db: LithicDatabase

    @Before fun setUp() = runBlocking {
        directory = File(context.noBackupFilesDir, "m04-metadata-test-${UUID.randomUUID()}")
        check(directory.mkdirs())
        db = withContext(Dispatchers.IO) { LithicDatabase.open(context, File(directory, "data.db")) }
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
        if (::directory.isInitialized) {
            check(directory.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            directory.deleteRecursively()
        }
    }

    @Test fun media_rejects_traversal_absolute_and_backslash_paths() = runBlocking {
        val unsafe = listOf("", "../cover.webp", "media/../cover.webp", "/storage/cover.webp", "C:/cover.webp", "media\\cover.webp", "media//cover.webp", "media/./cover.webp", "media/\u0000cover.webp", "data.db", "settings.preferences_pb", "ready.json", "attachments/cover.webp")
        unsafe.forEach { path ->
            expectInvalid { db.mediaDao().insert(validAsset().copy(relativePath = path)) }
            assertTrue(db.mediaDao().list().isEmpty())
        }
    }

    @Test fun media_rejects_invalid_uuid() = runBlocking {
        listOf("not-a-uuid", "1-1-1-1-1", "10000000-0000-4000-8000-000000000099-more").forEach { invalid ->
            expectInvalid { db.mediaDao().insert(validAsset().copy(id = invalid)) }
            assertTrue(db.mediaDao().list().isEmpty())
        }
    }

    @Test fun media_rejects_nonpositive_dimensions() = runBlocking {
        listOf(0 to 20, -1 to 20, 10 to 0, 10 to -1).forEach { (width, height) ->
            expectInvalid { db.mediaDao().insert(validAsset().copy(width = width, height = height)) }
            assertTrue(db.mediaDao().list().isEmpty())
        }
    }

    @Test fun media_rejects_negative_byte_length() = runBlocking {
        expectInvalid { db.mediaDao().insert(validAsset().copy(byteLength = -1L)) }
        assertTrue(db.mediaDao().list().isEmpty())
    }

    @Test fun media_rejects_invalid_sha256() = runBlocking {
        listOf("", "a".repeat(63), "a".repeat(65), "g".repeat(64), " " + "a".repeat(63)).forEach { digest ->
            expectInvalid { db.mediaDao().insert(validAsset().copy(sha256 = digest)) }
            assertTrue(db.mediaDao().list().isEmpty())
        }
    }

    @Test fun valid_metadata_roundtrips_without_claiming_image_processing() = runBlocking {
        val asset = validAsset().copy(byteLength = 0L)
        db.mediaDao().insert(asset)
        assertEquals(listOf(asset), db.mediaDao().list())
        assertEquals(asset, db.mediaDao().find("10000000-0000-4000-8000-000000000099"))
    }

    @Test fun category_rejects_invalid_uuid() = runBlocking {
        expectInvalid { db.categoryDao().insert(CategoryEntity("not-a-uuid", CategoryType.PROJECT, "工具", "工具", false)) }
        assertTrue(db.categoryDao().list().isEmpty())
    }

    @Test fun category_rejects_inconsistent_normalized_name() = runBlocking {
        expectInvalid { db.categoryDao().insert(CategoryEntity("10000000-0000-4000-8000-000000000098", CategoryType.PROJECT, "  ＴＯＯＬ  BOX  ", "TOOL BOX", false)) }
        assertTrue(db.categoryDao().list().isEmpty())
    }

    @Test fun category_rejects_blank_name() = runBlocking {
        expectInvalid { db.categoryDao().insert(CategoryEntity("10000000-0000-4000-8000-000000000098", CategoryType.PROJECT, "   ", "", false)) }
        assertTrue(db.categoryDao().list().isEmpty())
    }

    private suspend fun expectInvalid(block: suspend () -> Unit) {
        var rejected = false
        try { block() } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue("Unsafe metadata must be rejected at the real production entity/DAO boundary", rejected)
    }

    private fun validAsset() = LocalMediaAssetEntity("10000000-0000-4000-8000-000000000099", "media/cover.webp", "image/webp", 10, 20, 30L, "a".repeat(64))
}

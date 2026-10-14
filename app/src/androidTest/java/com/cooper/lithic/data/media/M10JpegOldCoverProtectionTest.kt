package com.cooper.lithic.data.media

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionFacts
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The supplied entropy-cut JPEG must fail before either complete persisted target changes. */
@RunWith(AndroidJUnit4::class)
class M10JpegOldCoverProtectionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var repository: MediaRepository
    private val sources = mutableListOf<SyntheticImageSource>()
    private val date = LocalDate.of(2026, 1, 31)
    private val time = Instant.parse("2026-01-31T03:04:05Z")
    private val clock = FixedBusinessClock(date)

    @Before fun setUp() = runBlocking {
        root = File(context.noBackupFilesDir, "m10-jpeg-protection-${uuid()}")
        generation = DataGenerationStore(context, root).open(ZoneId.of("UTC"))
        repository = MediaRepository(generation.database, generation.coordinator, generation.images, clock, context.contentResolver)
    }

    @After fun tearDown() = runBlocking {
        sources.forEach { it.delete() }
        if (::generation.isInitialized) generation.close()
        if (::root.isInitialized && root.exists()) {
            check(root.name.startsWith("m10-jpeg-protection-"))
            check(!Files.isSymbolicLink(root.toPath()))
            check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            check(root.walkTopDown().none { Files.isSymbolicLink(it.toPath()) })
            check(root.deleteRecursively()) { "Cannot remove this test's isolated generation" }
        }
    }

    // Accepting incomplete entropy as a prepared cover would violate the replacement boundary.
    @Test fun project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved() = runBlocking {
        assertBrokenSelectionPreservesOldCover(CoverTargetType.PROJECT)
    }

    // The same invalid input must not alter subscription payments or its persisted state history.
    @Test fun subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved() = runBlocking {
        assertBrokenSelectionPreservesOldCover(CoverTargetType.SUBSCRIPTION)
    }

    private suspend fun assertBrokenSelectionPreservesOldCover(type: CoverTargetType) {
        val project = project()
        val subscription = subscription()
        val original: CoverBaseline = if (type == CoverTargetType.PROJECT) project else subscription
        val healthySource = source(M10JpegFixtures.COMPLETE)
        assertEquals("image/jpeg", context.contentResolver.getType(healthySource.uri))
        val prepared = success(repository.prepareImport(healthySource.uri, uuid()))
        assertEquals("image/jpeg", prepared.asset.mime)
        val old = success(repository.commit(CoverCommand(uuid(), original, prepared.asset, time.plusSeconds(1)))).baseline
        val oldImage = success(repository.openAsset(prepared.asset.id))
        assertEquals(prepared.asset.sha256, sha(oldImage.bytes))
        val projectReader = ProjectRepository(generation.database, generation.coordinator, clock)
        val subscriptionReader = SubscriptionRepository(generation.database, generation.coordinator, clock)
        val beforeProject = requireNotNull(projectReader.loadProjectDetail(project.project.id))
        val beforeSubscription = requireNotNull(subscriptionReader.loadSubscriptionDetail(subscription.facts.subscription.id))
        val beforeMedia = generation.database.mediaDao().list()
        val beforeCategories = generation.database.categoryDao().list()
        val beforeFiles = finalCoverHashes()

        val brokenSource = source(M10JpegFixtures.BROKEN)
        assertEquals("image/jpeg", context.contentResolver.getType(brokenSource.uri))
        val replacementId = uuid()
        val result = repository.prepareImport(brokenSource.uri, replacementId)
        val afterProject = requireNotNull(projectReader.loadProjectDetail(project.project.id))
        val afterSubscription = requireNotNull(subscriptionReader.loadSubscriptionDetail(subscription.facts.subscription.id))
        val afterMedia = generation.database.mediaDao().list()
        val afterCategories = generation.database.categoryDao().list()
        val afterOldImage = success(repository.openAsset(prepared.asset.id))
        val afterFiles = finalCoverHashes()
        val active = generation.images.isPreparedActive(replacementId)
        val factsIntact = beforeProject == afterProject && beforeSubscription == afterSubscription && beforeCategories == afterCategories
        val observation = JSONObject()
            .put("targetType", type.name)
            .put("source", M10JpegFixtures.BROKEN)
            .put("declaredMime", "image/jpeg")
            .put("prepareWronglyAccepted", result is MediaResult.Success)
            .put("prepareResult", when (result) {
                is MediaResult.Success -> "SUCCESS"
                is MediaResult.Failure -> result.kind.name
            })
            .put("oldFactsIntact", factsIntact)
            .put("mediaTableIntact", beforeMedia == afterMedia)
            .put("oldCoverAssetId", old.coverAssetId)
            .put("oldCoverSha256Before", sha(oldImage.bytes))
            .put("oldCoverSha256After", sha(afterOldImage.bytes))
            .put("privateFinalFilesIntact", beforeFiles == afterFiles)
            .put("replacementAssetId", replacementId)
            .put("newFormalMetadataExists", generation.database.mediaDao().find(replacementId) != null)
            .put("replacementPreparedLeaseActive", active)
        if (result is MediaResult.Success) {
            observation.put("acceptedMetadata", JSONObject()
                .put("id", result.value.asset.id)
                .put("mime", result.value.asset.mime)
                .put("width", result.value.asset.width)
                .put("height", result.value.asset.height)
                .put("byteLength", result.value.asset.byteLength)
                .put("sha256", result.value.asset.sha256))
        } else if (result is MediaResult.Failure) {
            observation.put("failureMessage", result.message)
        }
        M10JpegObservation.record("old-cover-${type.name.lowercase()}", observation)

        assertEquals(beforeProject, afterProject)
        assertEquals(beforeSubscription, afterSubscription)
        assertEquals(beforeCategories, afterCategories)
        assertEquals(old, success(repository.loadTarget(old.target)))
        assertEquals(beforeMedia, afterMedia)
        assertEquals(oldImage.asset, afterOldImage.asset)
        assertArrayEquals(oldImage.bytes, afterOldImage.bytes)
        assertEquals(beforeFiles, afterFiles)
        assertNull(generation.database.mediaDao().find(replacementId))
        assertFalse(File(generation.directory, "media/$replacementId.jpg").exists())
        assertFalse(File(generation.directory, "media/$replacementId.png").exists())

        // This assertion intentionally remains RED if Android prepares the supplied damaged JPEG.
        assertTrue("Expected INVALID_IMAGE from the actual JPEG source, got $result", result is MediaResult.Failure)
        assertEquals(MediaFailureKind.INVALID_IMAGE, (result as MediaResult.Failure).kind)
        assertFalse("A failed preparation must not retain an editor lease", active)
        val cleanup = success(repository.removeUnreferenced())
        assertTrue(cleanup.failures.isEmpty())
        assertFalse(cleanup.skippedActiveAssetIds.contains(replacementId))
        assertFalse(File(generation.directory, "media/.m10-imports/$replacementId").exists())
        assertEquals(beforeMedia, generation.database.mediaDao().list())
        assertEquals(beforeFiles, finalCoverHashes())
        assertEquals(beforeProject, projectReader.loadProjectDetail(project.project.id))
        assertEquals(beforeSubscription, subscriptionReader.loadSubscriptionDetail(subscription.facts.subscription.id))
        assertArrayEquals(oldImage.bytes, success(repository.openAsset(prepared.asset.id)).bytes)
        observation.put("failedPreparationCleanupVerified", true)
        M10JpegObservation.record("old-cover-${type.name.lowercase()}-cleanup", observation)
    }

    private suspend fun project(): CoverBaseline.Project {
        val category = generation.database.categoryDao().list().single { it.type == CategoryType.PROJECT }
        val item = LongTermProjectEntity(uuid(), "Synthetic JPEG protection project", category.id, LocalDate.of(2026, 1, 1),
            note = "Keep the complete original project", createdAt = time, updatedAt = time)
        val event = ProjectMoneyEventEntity(uuid(), item.id, MoneyDirection.OUTLAY, 15_000,
            LocalDate.of(2026, 1, 1), "actual initial payment", ProjectMoneyEventRole.INITIAL, uuid())
        generation.database.projectDao().insertProject(item)
        generation.database.projectDao().insertEvent(event)
        return CoverBaseline.Project(item, listOf(event))
    }

    private suspend fun subscription(): CoverBaseline.Subscription {
        val category = generation.database.categoryDao().list().single { it.type == CategoryType.SUBSCRIPTION }
        val identity = SubscriptionEntity(uuid(), "Synthetic JPEG protection subscription", category.id,
            note = "Keep the complete original subscription", createdAt = time, updatedAt = time)
        val renewal = SubscriptionRenewalEntity(uuid(), identity.id, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), 15_000, BillingCycle.MONTHLY, 30, uuid())
        val state = SubscriptionStateChangeEntity(uuid(), identity.id, LocalDate.of(2026, 1, 15),
            SubscriptionOperationState.PAUSED, operationId = uuid())
        generation.database.subscriptionDao().insertSubscription(identity)
        generation.database.subscriptionDao().insertRenewal(renewal)
        generation.database.subscriptionDao().insertStateChange(state)
        return CoverBaseline.Subscription(SubscriptionFacts(identity, listOf(renewal), listOf(state)))
    }

    private fun source(name: String) = M10JpegFixtures.source(name).also { sources += it }
    private fun finalCoverHashes(): Map<String, String> = requireNotNull(File(generation.directory, "media").listFiles())
        .filter { it.isFile }.associate { it.name to sha(it.readBytes()) }
    private fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun uuid(): String = UUID.randomUUID().toString()
    private fun <T> success(result: MediaResult<T>): T {
        assertTrue("Expected successful setup/read, got $result", result is MediaResult.Success)
        return (result as MediaResult.Success<T>).value
    }
}

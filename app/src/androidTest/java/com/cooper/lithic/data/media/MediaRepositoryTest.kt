package com.cooper.lithic.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.repository.DeleteProjectCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionFacts
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real private generation/SQLite tests, with isolated synthetic facts only. */
@RunWith(AndroidJUnit4::class)
class MediaRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var images: LocalImageStore
    private lateinit var repository: MediaRepository
    private val sources = mutableListOf<SyntheticImageSource>()
    private val date = LocalDate.of(2026, 1, 31)
    private val time = Instant.parse("2026-01-31T03:04:05Z")

    @Before fun setUp() = runBlocking {
        root = File(context.noBackupFilesDir, "m10-repository-${UUID.randomUUID()}")
        generation = DataGenerationStore(context, root).open(ZoneId.of("UTC"))
        images = LocalImageStore(generation.directory)
        repository = MediaRepository(generation.database, generation.coordinator, images, FixedBusinessClock(date), context.contentResolver)
    }

    @After fun tearDown() = runBlocking {
        sources.forEach { it.delete() }
        if (::generation.isInitialized) generation.close()
        if (::root.isInitialized) {
            check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            root.deleteRecursively()
        }
    }

    // Dropping money events from the editor baseline would permit stale cover writes.
    @Test fun load_target_returns_complete_project_and_money_facts() = runBlocking {
        val expected = project()
        val loaded = repository.loadTarget(expected.target)
        assertTrue("Expected the real project baseline, got $loaded", loaded is MediaResult.Success)
        assertEquals(expected, (loaded as MediaResult.Success<CoverBaseline>).value)
        assertEquals(15_000L, (loaded.value as CoverBaseline.Project).events.single().amountCents)
    }

    // Cover removal must write only its reference and revision, including a missing old image.
    @Test fun remove_cover_keeps_original_money_and_identity() = runBlocking {
        val old = metadata()
        generation.database.mediaDao().insert(old)
        val expected = project(old.id)
        val command = CoverCommand(uuid(), expected, null, time.plusSeconds(1))
        val result = repository.commit(command)
        assertTrue("Expected successful cover removal, got $result", result is MediaResult.Success)
        val actual = generation.database.projectDao().findProject(expected.project.id)
        assertEquals(expected.project.copy(coverAssetId = null, updatedAt = time.plusSeconds(1)), actual)
        assertEquals(expected.events, generation.database.projectDao().eventsForProjects(listOf(expected.project.id)))
        assertNotNull(generation.database.mediaDao().find(old.id))
    }

    private suspend fun project(coverId: String? = null): CoverBaseline.Project {
        val category = generation.database.categoryDao().list().single { it.type == CategoryType.PROJECT }
        val item = LongTermProjectEntity(uuid(), "Synthetic M10 project", category.id, LocalDate.of(2026, 1, 1),
            coverAssetId = coverId, note = "Keep this original note", createdAt = time, updatedAt = time)
        val event = ProjectMoneyEventEntity(uuid(), item.id, MoneyDirection.OUTLAY, 15_000,
            LocalDate.of(2026, 1, 1), "actual initial payment", ProjectMoneyEventRole.INITIAL, uuid())
        generation.database.projectDao().insertProject(item)
        generation.database.projectDao().insertEvent(event)
        return CoverBaseline.Project(item, listOf(event))
    }

    private fun metadata(): LocalMediaAssetEntity {
        val id = uuid()
        return LocalMediaAssetEntity(id, "media/$id.png", "image/png", 1, 1, 1, "0".repeat(64))
    }
    private fun uuid() = UUID.randomUUID().toString()
    @Test fun subscription_baseline_contains_original_payments_and_states() = runBlocking {
        val expected = subscription()
        assertEquals(expected, success(repository.loadTarget(expected.target)))
    }

    @Test fun project_cover_write_preserves_cost_and_all_money_facts() = runBlocking {
        val original = project()
        val before = ProjectRepository(generation.database, generation.coordinator, FixedBusinessClock(date)).loadProjectDetail(original.project.id)!!
        val prepared = prepare()
        val command = command(original, prepared.asset)
        val result = success(repository.commit(command))
        assertFalse(result.replayed)
        assertEquals(original.project.copy(coverAssetId = prepared.asset.id, updatedAt = command.writtenAt), (result.baseline as CoverBaseline.Project).project)
        val after = ProjectRepository(generation.database, generation.coordinator, FixedBusinessClock(date)).loadProjectDetail(original.project.id)!!
        assertEquals(before.cost, after.cost)
        assertEquals(before.events, after.events)
        assertEquals(prepared.asset, success(repository.openAsset(prepared.asset.id)).asset)
        assertEquals(1, generation.database.mediaDao().list().size)
    }

    @Test fun subscription_cover_write_preserves_payment_and_state_history() = runBlocking {
        val original = subscription()
        val reader = SubscriptionRepository(generation.database, generation.coordinator, FixedBusinessClock(date))
        val before = reader.loadSubscriptionDetail(original.facts.subscription.id)!!
        val prepared = prepare()
        val input = command(original, prepared.asset)
        val result = success(repository.commit(input)).baseline as CoverBaseline.Subscription
        val after = reader.loadSubscriptionDetail(original.facts.subscription.id)!!
        assertEquals(original.facts.subscription.copy(coverAssetId = prepared.asset.id, updatedAt = input.writtenAt), result.facts.subscription)
        assertEquals(before.cost, after.cost)
        assertEquals(before.renewals, after.renewals)
        assertEquals(before.stateChanges, after.stateChanges)
    }

    @Test fun fixed_command_concurrency_creates_one_asset_and_replays_exact_target() = runBlocking {
        val original = project()
        val prepared = prepare()
        val input = command(original, prepared.asset)
        val results = (1..8).map { async(Dispatchers.IO) { success(repository.commit(input)) } }.awaitAll()
        assertEquals(1, results.count { !it.replayed })
        assertEquals(7, results.count { it.replayed })
        assertEquals(1, generation.database.mediaDao().list().size)
        assertEquals(1, File(generation.directory, "media").listFiles()!!.count { it.name.endsWith(".png") })
        assertTrue(results.all { it.baseline.coverAssetId == prepared.asset.id })
    }

    @Test fun lost_result_replay_after_generation_reopen_does_not_import_again() = runBlocking {
        val original = project()
        val prepared = prepare()
        val input = command(original, prepared.asset)
        success(repository.commit(input)) // The UI deliberately does not consume its return value.
        val savedBytes = success(repository.openAsset(prepared.asset.id)).bytes
        reopen()
        val replay = success(repository.commit(input))
        assertTrue(replay.replayed)
        assertArrayEquals(savedBytes, success(repository.openAsset(prepared.asset.id)).bytes)
        assertEquals(listOf(prepared.asset), generation.database.mediaDao().list())
    }

    @Test fun committed_verification_is_read_only_and_detects_later_target_changes() = runBlocking {
        val original = project()
        val first = command(original, prepare().asset)
        val saved = success(repository.commit(first)).baseline
        sql("CREATE TRIGGER m10_forbid_update BEFORE UPDATE ON long_term_projects BEGIN SELECT RAISE(ABORT,'must stay read only'); END")
        assertEquals(saved, success(repository.verifyCommitted(first)))
        sql("DROP TRIGGER m10_forbid_update")
        success(repository.commit(command(saved, prepare().asset)))
        assertFailure(MediaFailureKind.CONFLICT, repository.verifyCommitted(first))
    }

    @Test fun stale_project_metadata_cannot_replace_newer_facts() = runBlocking {
        val original = project()
        val prepared = prepare()
        assertEquals(1, generation.database.projectDao().updateProjectMetadata(original.project.id, time,
            "Later edited name", original.project.startedOn, original.project.categoryId, original.project.note, time.plusSeconds(1)))
        assertFailure(MediaFailureKind.CONFLICT, repository.commit(command(original, prepared.asset)))
        assertEquals("Later edited name", generation.database.projectDao().findProject(original.project.id)!!.name)
        assertNull(generation.database.projectDao().findProject(original.project.id)!!.coverAssetId)
    }

    @Test fun unchanged_project_row_with_new_money_event_is_stale_cover_baseline() = runBlocking {
        val original = project()
        val prepared = prepare()
        val later = original.events.single().copy(id = uuid(), operationId = uuid(), amountCents = 2500,
            role = ProjectMoneyEventRole.REGULAR, occurredOn = date)
        generation.database.projectDao().insertEvent(later)
        assertFailure(MediaFailureKind.CONFLICT, repository.commit(command(original, prepared.asset)))
        assertEquals(original.project, generation.database.projectDao().findProject(original.project.id))
        assertEquals(2, generation.database.projectDao().eventsForProjects(listOf(original.project.id)).size)
    }

    @Test fun deleted_project_cannot_acquire_a_prepared_cover() = runBlocking {
        val original = project()
        val prepared = prepare()
        val deleted = ProjectRepository(generation.database, generation.coordinator, FixedBusinessClock(date))
            .deleteProject(DeleteProjectCommand(uuid(), original.project, original.events))
        assertTrue(deleted is MutationResult.ProjectDeleted)
        assertFailure(MediaFailureKind.CONFLICT, repository.commit(command(original, prepared.asset)))
        assertTrue(generation.database.mediaDao().list().isEmpty())
    }

    @Test fun new_subscription_payment_blocks_a_stale_cover_command() = runBlocking {
        val original = subscription()
        val prepared = prepare()
        val first = original.facts.renewals.single()
        generation.database.subscriptionDao().insertRenewal(first.copy(id = uuid(), operationId = uuid(), paidOn = date,
            effectiveFrom = LocalDate.of(2026, 2, 1), paidUntilExclusive = LocalDate.of(2026, 3, 1), reminderDueOn = LocalDate.of(2026, 3, 1)))
        assertFailure(MediaFailureKind.CONFLICT, repository.commit(command(original, prepared.asset)))
        assertEquals(2L, generation.database.subscriptionDao().countRenewals(original.facts.subscription.id))
        assertNull(generation.database.subscriptionDao().findSubscription(original.facts.subscription.id)!!.coverAssetId)
    }

    @Test fun new_subscription_state_blocks_a_stale_cover_command() = runBlocking {
        val original = subscription()
        val prepared = prepare()
        generation.database.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(uuid(), original.facts.subscription.id,
            date, SubscriptionOperationState.STOPPED, operationId = uuid()))
        assertFailure(MediaFailureKind.CONFLICT, repository.commit(command(original, prepared.asset)))
        assertEquals(2, generation.database.subscriptionDao().statesForSubscriptions(listOf(original.facts.subscription.id)).size)
        assertNull(generation.database.subscriptionDao().findSubscription(original.facts.subscription.id)!!.coverAssetId)
    }

    @Test fun sqlite_asset_insert_failure_protects_old_cover_and_leaves_safe_orphan() = runBlocking {
        val old = coveredProject()
        val oldBytes = success(repository.openAsset(old.coverAssetId!!)).bytes
        val replacement = prepare()
        sql("CREATE TRIGGER m10_fail_asset BEFORE INSERT ON local_media_assets BEGIN SELECT RAISE(ABORT,'synthetic insert failure'); END")
        assertTrue(repository.commit(command(old, replacement.asset)) is MediaResult.Failure)
        assertEquals(old, success(repository.loadTarget(old.target)))
        assertArrayEquals(oldBytes, success(repository.openAsset(old.coverAssetId!!)).bytes)
        assertNull(generation.database.mediaDao().find(replacement.asset.id))
        assertTrue(File(generation.directory, replacement.asset.relativePath).isFile)
        sql("DROP TRIGGER m10_fail_asset")
        images.releasePrepared(replacement.asset.id)
        val cleanup = success(repository.removeUnreferenced())
        assertTrue(cleanup.removedAssetIds.contains(replacement.asset.id))
        assertFalse(File(generation.directory, replacement.asset.relativePath).exists())
    }

    @Test fun project_update_failure_rolls_back_new_metadata_and_protects_old_bytes() = runBlocking {
        val old = coveredProject()
        val oldBytes = success(repository.openAsset(old.coverAssetId!!)).bytes
        val replacement = prepare()
        sql("CREATE TRIGGER m10_fail_cover AFTER UPDATE ON long_term_projects BEGIN SELECT RAISE(ABORT,'synthetic cover update failure'); END")
        assertTrue(repository.commit(command(old, replacement.asset)) is MediaResult.Failure)
        assertEquals(old, success(repository.loadTarget(old.target)))
        assertEquals(1, generation.database.mediaDao().list().size)
        assertArrayEquals(oldBytes, success(repository.openAsset(old.coverAssetId!!)).bytes)
        sql("DROP TRIGGER m10_fail_cover")
    }

    @Test fun subscription_update_failure_rolls_back_new_metadata_and_keeps_timelines() = runBlocking {
        val old = coveredSubscription()
        val oldBytes = success(repository.openAsset(old.coverAssetId!!)).bytes
        val replacement = prepare()
        sql("CREATE TRIGGER m10_fail_subscription AFTER UPDATE ON subscriptions BEGIN SELECT RAISE(ABORT,'synthetic subscription cover failure'); END")
        assertTrue(repository.commit(command(old, replacement.asset)) is MediaResult.Failure)
        assertEquals(old, success(repository.loadTarget(old.target)))
        assertEquals(1, generation.database.mediaDao().list().size)
        assertArrayEquals(oldBytes, success(repository.openAsset(old.coverAssetId!!)).bytes)
        sql("DROP TRIGGER m10_fail_subscription")
    }

    @Test fun decode_failure_preserves_existing_cover_and_original_facts() = runBlocking {
        assertPreparationFailureKeepsOld(ImageStoreStage.BEFORE_DECODE)
    }

    @Test fun encode_failure_preserves_existing_cover_and_original_facts() = runBlocking {
        assertPreparationFailureKeepsOld(ImageStoreStage.BEFORE_ENCODE)
    }

    @Test fun prepared_file_write_failure_preserves_existing_cover_and_original_facts() = runBlocking {
        assertPreparationFailureKeepsOld(ImageStoreStage.BEFORE_PREPARED_WRITE)
    }

    @Test fun final_file_commit_failure_never_inserts_a_cover_reference() = runBlocking {
        val old = coveredProject()
        val oldBytes = success(repository.openAsset(old.coverAssetId!!)).bytes
        val faulty = LocalImageStore(generation.directory, faults = ImageStoreFaults { stage ->
            if (stage == ImageStoreStage.BEFORE_FILE_COMMIT) throw IOException("synthetic file commit failure")
        })
        val failedRepository = repo(faulty)
        val prepared = success(failedRepository.prepareImport(source().uri, uuid()))
        assertTrue(failedRepository.commit(command(old, prepared.asset)) is MediaResult.Failure)
        assertEquals(old, success(repository.loadTarget(old.target)))
        assertNull(generation.database.mediaDao().find(prepared.asset.id))
        assertArrayEquals(oldBytes, success(repository.openAsset(old.coverAssetId!!)).bytes)
    }

    @Test fun two_projects_share_a_cover_and_replacement_keeps_other_reference() = runBlocking {
        val first = coveredProject()
        val asset = generation.database.mediaDao().find(first.coverAssetId!!)!!
        val second = success(repository.commit(command(project(), asset))).baseline
        val oldBytes = success(repository.openAsset(asset.id)).bytes
        success(repository.commit(command(first, prepare().asset)))
        success(repository.removeUnreferenced())
        assertEquals(asset.id, success(repository.loadTarget(second.target)).coverAssetId)
        assertArrayEquals(oldBytes, success(repository.openAsset(asset.id)).bytes)
        assertNotNull(generation.database.mediaDao().find(asset.id))
    }

    @Test fun project_and_subscription_share_cover_until_last_reference_is_removed() = runBlocking {
        val first = coveredProject()
        val asset = generation.database.mediaDao().find(first.coverAssetId!!)!!
        val second = success(repository.commit(command(subscription(), asset))).baseline
        success(repository.commit(command(first, null)))
        success(repository.removeUnreferenced())
        assertNotNull(generation.database.mediaDao().find(asset.id))
        assertTrue(File(generation.directory, asset.relativePath).isFile)
        success(repository.commit(command(second, null)))
        val report = success(repository.removeUnreferenced())
        assertTrue(report.removedAssetIds.contains(asset.id))
        assertNull(generation.database.mediaDao().find(asset.id))
        assertFalse(File(generation.directory, asset.relativePath).exists())
        assertTrue(success(repository.removeUnreferenced()).failures.isEmpty())
    }

    @Test fun deleting_project_preserves_cover_still_used_by_subscription() = runBlocking {
        val first = coveredProject() as CoverBaseline.Project
        val asset = generation.database.mediaDao().find(first.coverAssetId!!)!!
        val second = success(repository.commit(command(subscription(), asset))).baseline
        val bytes = success(repository.openAsset(asset.id)).bytes
        val deleted = ProjectRepository(generation.database, generation.coordinator, FixedBusinessClock(date))
            .deleteProject(DeleteProjectCommand(uuid(), first.project, first.events))
        assertTrue(deleted is MutationResult.ProjectDeleted)
        success(repository.removeUnreferenced())
        assertEquals(asset.id, success(repository.loadTarget(second.target)).coverAssetId)
        assertArrayEquals(bytes, success(repository.openAsset(asset.id)).bytes)
    }

    @Test fun active_preparation_survives_cleanup_until_explicit_release() = runBlocking {
        val prepared = prepare()
        val report = success(repository.removeUnreferenced())
        assertTrue(report.skippedActiveAssetIds.contains(prepared.asset.id))
        assertEquals(prepared, success(repository.restorePrepared(prepared.asset.id)))
        images.releasePrepared(prepared.asset.id)
        assertTrue(success(repository.removeUnreferenced()).removedAssetIds.contains(prepared.asset.id))
        assertTrue(repository.restorePrepared(prepared.asset.id) is MediaResult.Failure)
    }

    @Test fun final_file_before_database_commit_is_reclaimable_after_reopen() = runBlocking {
        val prepared = prepare()
        success(images.commitPreparedAsset(prepared))
        images.releasePrepared(prepared.asset.id)
        assertTrue(generation.database.mediaDao().list().isEmpty())
        reopen()
        assertTrue(success(repository.removeUnreferenced()).removedAssetIds.contains(prepared.asset.id))
        assertFalse(File(generation.directory, prepared.asset.relativePath).exists())
    }

    @Test fun database_commit_before_old_cleanup_survives_generation_reopen() = runBlocking {
        val old = coveredProject()
        val next = prepare()
        val saved = success(repository.commit(command(old, next.asset))).baseline
        val bytes = success(repository.openAsset(next.asset.id)).bytes
        assertNotNull(generation.database.mediaDao().find(old.coverAssetId!!))
        reopen()
        assertEquals(saved, success(repository.loadTarget(saved.target)))
        assertArrayEquals(bytes, success(repository.openAsset(next.asset.id)).bytes)
        success(repository.removeUnreferenced())
        assertNull(generation.database.mediaDao().find(old.coverAssetId!!))
        assertArrayEquals(bytes, success(repository.openAsset(next.asset.id)).bytes)
    }

    @Test fun cleanup_cannot_delete_file_while_coordinator_snapshot_uses_it() = runBlocking {
        val old = coveredProject()
        val asset = generation.database.mediaDao().find(old.coverAssetId!!)!!
        val bytes = success(repository.openAsset(asset.id)).bytes
        success(repository.commit(command(old, null)))
        val acquired = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val snapshot = async(Dispatchers.IO) {
            generation.coordinator.withSnapshot {
                acquired.complete(Unit)
                release.await()
                assertArrayEquals(bytes, success(images.openAsset(asset)).bytes)
            }
        }
        acquired.await()
        val cleanup = async(Dispatchers.IO) { repository.removeUnreferenced() }
        try {
            delay(100)
            assertFalse(cleanup.isCompleted)
            assertTrue(File(generation.directory, asset.relativePath).isFile)
        } finally {
            release.complete(Unit)
        }
        snapshot.await()
        assertTrue(success(cleanup.await()).removedAssetIds.contains(asset.id))
    }

    @Test fun unknown_uuid_named_file_without_ownership_proof_is_retained() = runBlocking {
        val unknown = File(generation.directory, "media/${uuid()}.png")
        val bytes = png()
        unknown.writeBytes(bytes)
        success(repository.removeUnreferenced())
        assertArrayEquals(bytes, unknown.readBytes())
    }

    @Test fun physical_delete_failure_keeps_proof_and_retries_without_metadata_reference() = runBlocking {
        val old = coveredProject()
        val asset = generation.database.mediaDao().find(old.coverAssetId!!)!!
        success(repository.commit(command(old, null)))
        val faulty = LocalImageStore(generation.directory, faults = ImageStoreFaults { stage ->
            if (stage == ImageStoreStage.BEFORE_FILE_REMOVE) throw IOException("synthetic physical delete failure")
        })
        val report = success(repo(faulty).removeUnreferenced())
        assertTrue(report.failures.any { it.assetId == asset.id && it.kind == MediaFailureKind.IO })
        assertNull(generation.database.mediaDao().find(asset.id))
        assertTrue(File(generation.directory, asset.relativePath).exists())
        assertTrue(success(repository.removeUnreferenced()).removedAssetIds.contains(asset.id))
        assertFalse(File(generation.directory, asset.relativePath).exists())
    }

    @Test fun stale_old_command_cannot_overwrite_a_newer_cover() = runBlocking {
        val original = project()
        val first = command(original, prepare().asset)
        val firstSaved = success(repository.commit(first)).baseline
        val secondSaved = success(repository.commit(command(firstSaved, prepare().asset))).baseline
        assertFailure(MediaFailureKind.CONFLICT, repository.commit(first))
        assertEquals(secondSaved, success(repository.loadTarget(original.target)))
    }

    @Test fun missing_cover_is_explicit_failure_without_deleting_metadata_or_business_reference() = runBlocking {
        val original = coveredProject()
        val asset = generation.database.mediaDao().find(original.coverAssetId!!)!!
        assertTrue(File(generation.directory, asset.relativePath).delete())
        assertTrue(repository.openAsset(asset.id) is MediaResult.Failure)
        assertEquals(asset, generation.database.mediaDao().find(asset.id))
        assertEquals(original, success(repository.loadTarget(original.target)))
    }

    @Test fun corrupt_cover_is_explicit_failure_without_reencoding_or_clearing_facts() = runBlocking {
        val original = coveredProject()
        val asset = generation.database.mediaDao().find(original.coverAssetId!!)!!
        val bad = byteArrayOf(1, 2, 3, 4)
        File(generation.directory, asset.relativePath).writeBytes(bad)
        assertTrue(repository.openAsset(asset.id) is MediaResult.Failure)
        assertArrayEquals(bad, File(generation.directory, asset.relativePath).readBytes())
        assertEquals(asset, generation.database.mediaDao().find(asset.id))
        assertEquals(original, success(repository.loadTarget(original.target)))
    }

    @Test fun repeated_verified_reads_preserve_bytes_and_real_metadata() = runBlocking {
        val original = coveredProject()
        val asset = generation.database.mediaDao().find(original.coverAssetId!!)!!
        val first = success(repository.openAsset(asset.id))
        repeat(5) { assertArrayEquals(first.bytes, success(repository.openAsset(asset.id)).bytes) }
        assertEquals(first.bytes.size.toLong(), asset.byteLength)
        assertEquals(MessageDigest.getInstance("SHA-256").digest(first.bytes).joinToString("") { "%02x".format(it) }, asset.sha256)
        val bitmap = BitmapFactory.decodeByteArray(first.bytes, 0, first.bytes.size)
        assertEquals(asset.width, bitmap.width)
        assertEquals(asset.height, bitmap.height)
        bitmap.recycle()
        assertEquals("image/png", asset.mime)
        assertTrue(asset.relativePath.startsWith("media/"))
        assertFalse(asset.relativePath.contains("://"))
        assertEquals(asset, generation.database.mediaDao().find(asset.id))
    }

    // The generation's public store must not bypass the two-owner database reference guard.
    @Test fun production_owned_image_store_cannot_delete_project_or_subscription_cover() = runBlocking {
        val owned = generation.images
        val media = MediaRepository(generation.database, generation.coordinator, owned, FixedBusinessClock(date), context.contentResolver)
        val prepared = success(media.prepareImport(source().uri, uuid()))
        val projectSaved = success(media.commit(command(project(), prepared.asset))).baseline
        val subscriptionSaved = success(media.commit(command(subscription(), prepared.asset))).baseline
        val bytes = success(media.openAsset(prepared.asset.id)).bytes
        assertFailure(MediaFailureKind.IN_USE, owned.removeUnreferenced(prepared.asset))
        assertEquals(prepared.asset, generation.database.mediaDao().find(prepared.asset.id))
        assertEquals(projectSaved, success(media.loadTarget(projectSaved.target)))
        assertEquals(subscriptionSaved, success(media.loadTarget(subscriptionSaved.target)))
        assertArrayEquals(bytes, success(media.openAsset(prepared.asset.id)).bytes)
    }

    // Returning from the repository's IO boundary can cancel after inner preparation succeeded.
    @Test fun repository_prompt_cancellation_releases_unreceived_preparation_lease() = runBlocking {
        val assetId = uuid()
        val input = source()
        var reachedPreparedWrite = false
        lateinit var operation: Deferred<MediaResult<PreparedImage>>
        val cancelling = LocalImageStore(generation.directory, faults = ImageStoreFaults { stage ->
            if (stage == ImageStoreStage.BEFORE_PREPARED_WRITE) {
                reachedPreparedWrite = true
                operation.cancel(CancellationException("synthetic unreceived preparation"))
            }
        })
        val media = repo(cancelling)
        operation = async(Dispatchers.Main, start = CoroutineStart.LAZY) {
            media.prepareImport(input.uri, assetId)
        }
        operation.start()
        try {
            operation.await()
            fail("Repository cancellation must propagate instead of reporting image success")
        } catch (_: CancellationException) {
            // The caller stays active so it can safely reconcile the private preparation.
        }
        operation.join()
        assertTrue(reachedPreparedWrite)
        assertFalse("An unreceived preparation must not retain an active lease", cancelling.isPreparedActive(assetId))
        assertFalse(File(generation.directory, "media/$assetId.png").exists())
        assertTrue(generation.database.mediaDao().list().isEmpty())
        assertEquals(0L, generation.database.projectDao().countProjects())
        assertEquals(0L, generation.database.subscriptionDao().countSubscriptions())
        when (val recovered = media.restorePrepared(assetId)) {
            is MediaResult.Success -> {
                assertEquals(assetId, recovered.value.asset.id)
                assertEquals("image/png", recovered.value.asset.mime)
                success(media.discardPrepared(assetId))
            }
            is MediaResult.Failure -> assertEquals(MediaFailureKind.NOT_FOUND, recovered.kind)
        }
        Unit
    }

    // UUID naming and an unreferenced database row do not establish ownership of an unknown file.
    @Test fun production_owned_cleanup_preserves_unknown_file_and_its_metadata() = runBlocking {
        val id = uuid()
        val bytes = png()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val asset = LocalMediaAssetEntity(id, "media/$id.png", "image/png", 24, 12, bytes.size.toLong(), hash)
        File(generation.directory, asset.relativePath).writeBytes(bytes)
        generation.database.mediaDao().insert(asset)
        assertFailure(MediaFailureKind.PATH, generation.images.removeUnreferenced(asset))
        assertEquals(asset, generation.database.mediaDao().find(asset.id))
        assertArrayEquals(bytes, File(generation.directory, asset.relativePath).readBytes())
    }

    private fun repo(store: LocalImageStore) = MediaRepository(generation.database, generation.coordinator, store, FixedBusinessClock(date), context.contentResolver)
    private fun command(expected: CoverBaseline, asset: LocalMediaAssetEntity?) = CoverCommand(uuid(), expected, asset, expected.updatedAt.plusSeconds(1))
    private suspend fun prepare(): PreparedImage = success(repository.prepareImport(source().uri, uuid()))
    private suspend fun coveredProject(): CoverBaseline = success(repository.commit(command(project(), prepare().asset))).baseline
    private suspend fun coveredSubscription(): CoverBaseline = success(repository.commit(command(subscription(), prepare().asset))).baseline
    private fun source() = M10ImageSourceFixtures.source(png()).also { sources += it }
    private fun png(): ByteArray {
        val bitmap = Bitmap.createBitmap(24, 12, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0x804488cc.toInt())
        val output = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        bitmap.recycle()
        return output.toByteArray()
    }
    private suspend fun reopen() {
        generation.close()
        generation = DataGenerationStore(context, root).open(ZoneId.of("UTC"))
        images = LocalImageStore(generation.directory)
        repository = repo(images)
    }
    private suspend fun subscription(): CoverBaseline.Subscription {
        val category = generation.database.categoryDao().list().single { it.type == CategoryType.SUBSCRIPTION }
        val identity = SubscriptionEntity(uuid(), "Synthetic M10 subscription", category.id, note = "original subscription note", createdAt = time, updatedAt = time)
        val renewal = SubscriptionRenewalEntity(uuid(), identity.id, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), 15_000, BillingCycle.MONTHLY, 30, uuid())
        val state = SubscriptionStateChangeEntity(uuid(), identity.id, LocalDate.of(2026, 1, 15), SubscriptionOperationState.PAUSED, operationId = uuid())
        generation.database.subscriptionDao().insertSubscription(identity)
        generation.database.subscriptionDao().insertRenewal(renewal)
        generation.database.subscriptionDao().insertStateChange(state)
        return CoverBaseline.Subscription(SubscriptionFacts(identity, listOf(renewal), listOf(state)))
    }
    private suspend fun assertPreparationFailureKeepsOld(stage: ImageStoreStage) {
        val old = coveredProject()
        val oldBytes = success(repository.openAsset(old.coverAssetId!!)).bytes
        val faulty = LocalImageStore(generation.directory, faults = ImageStoreFaults { point ->
            if (point == stage) throw IOException("synthetic preparation failure")
        })
        assertTrue(repo(faulty).prepareImport(source().uri, uuid()) is MediaResult.Failure)
        assertEquals(old, success(repository.loadTarget(old.target)))
        assertArrayEquals(oldBytes, success(repository.openAsset(old.coverAssetId!!)).bytes)
        assertEquals(1, generation.database.mediaDao().list().size)
    }
    private fun sql(statement: String) = generation.database.openHelper.writableDatabase.execSQL(statement)
    private fun assertFailure(kind: MediaFailureKind, result: MediaResult<*>) {
        assertTrue("Expected $kind failure, got $result", result is MediaResult.Failure)
        assertEquals(kind, (result as MediaResult.Failure).kind)
    }
    private fun <T> success(result: MediaResult<T>): T {
        assertTrue("Expected success, got $result", result is MediaResult.Success)
        return (result as MediaResult.Success<T>).value
    }


}

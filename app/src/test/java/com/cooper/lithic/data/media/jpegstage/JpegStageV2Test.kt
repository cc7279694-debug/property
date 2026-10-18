package com.cooper.lithic.data.media.jpegstage

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.math.BigInteger
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JpegStageV2Test {
    @get:Rule val temporary = TemporaryFolder()
    private val generation = UUID.randomUUID().toString()
    private val worker = JpegStageV2.WorkerIdentity(130, 10123, 456L)
    private fun owner() = JpegStageV2.Owner(generation, UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString())
    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    // Real host file identities are read on every operation; no simulated persistence model.
    private fun fileIdentity(file: File): JpegStageV2.FileIdentity {
        val attributes = Files.readAttributes(file.toPath(), java.nio.file.attribute.BasicFileAttributes::class.java, java.nio.file.LinkOption.NOFOLLOW_LINKS)
        check(attributes.isRegularFile)
        val (device, inode) = if (System.getProperty("os.name").startsWith("Windows")) {
            // This JDK returns null for fileKey on Windows. Query the actual NTFS file ID;
            // no path/mtime/hash approximation is accepted as an inode replacement proof.
            val process = ProcessBuilder("fsutil.exe", "file", "queryfileid", file.absolutePath).redirectErrorStream(true).start()
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                error("Test-only fsutil identity query timed out")
            }
            val text = process.inputStream.use { String(it.readBytes(), Charsets.UTF_8) }
            check(process.exitValue() == 0 && text.length < 4096) { "Test file identity was not available" }
            val matches = Regex("0x([0-9a-fA-F]{16,32})").findAll(text).toList()
            check(matches.size == 1) { "Ambiguous test file identity" }
            1L to BigInteger(matches.single().groupValues[1], 16).longValueExact()
        } else {
            (Files.getAttribute(file.toPath(), "unix:dev", java.nio.file.LinkOption.NOFOLLOW_LINKS) as Number).toLong() to
                (Files.getAttribute(file.toPath(), "unix:ino", java.nio.file.LinkOption.NOFOLLOW_LINKS) as Number).toLong()
        }
        // Windows range locks reject even an EOF read of the empty guard. Its hash is
        // exactly SHA256(empty); no bytes are skipped for nonempty source/output files.
        val bytes = if (attributes.size() == 0L) byteArrayOf() else file.readBytes()
        return JpegStageV2.FileIdentity(device, inode, file.length(), digest(bytes))
    }
    private fun protocol(root: File) = JpegStageV2(root, generation, ::fileIdentity) { check(it.isDirectory) }
    private fun setup(): Triple<JpegStageV2, File, JpegStageV2.Owner> {
        val root = temporary.newFolder("stages-${UUID.randomUUID()}")
        val protocol = protocol(root)
        val owner = owner()
        protocol.create(owner, ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)))
        return Triple(protocol, root, owner)
    }
    private fun delivered(): Triple<JpegStageV2, File, JpegStageV2.Owner> = setup().also { (p, _, o) -> p.registerConsumer(o, worker); p.markDelivery(o) }
    private fun completion(p: JpegStageV2, o: JpegStageV2.Owner, outcome: JpegStageV2.Outcome = JpegStageV2.Outcome.SUCCESS): JpegStageV2.Completion {
        val output = if (outcome == JpegStageV2.Outcome.SUCCESS) p.outputFile(o).also { it.writeBytes(byteArrayOf(5, 6, 7)) } else null
        return JpegStageV2.Completion(o, worker, outcome, fileIdentity(p.sourceFile(o)), output?.let(::fileIdentity), 1, 1,
            sourceFdClosed = true, outputFdClosed = true, binderDead = true, matchingProcessDead = true, strictFinished = true, decoderDestroyed = true, connectionCleaned = true)
    }
    private fun blocked(action: () -> Unit) { assertThrows(JpegStageV2.Blocked::class.java) { action() } }

    @Test fun creates_v2_owner_and_real_source() { val (p, root, o) = setup(); assertEquals(JpegStageV2.State.SOURCE_STAGED, p.snapshot(o).state); assertArrayEquals(byteArrayOf(1, 2, 3, 4), p.sourceFile(o).readBytes()); assertTrue(File(root, o.assetId + "/owner.v2").readText().contains("version=2")) }
    @Test fun second_unfinished_stage_is_blocked() { val (p, _, _) = setup(); blocked { p.create(owner(), ByteArrayInputStream(byteArrayOf(1))) } }
    @Test fun canonical_owner_ids_required() { val root = temporary.newFolder(); val p = protocol(root); blocked { p.create(owner().copy(assetId = "../x"), ByteArrayInputStream(byteArrayOf(1))) } }
    @Test fun wrong_generation_rejected() { val (p, _, o) = setup(); blocked { p.snapshot(o.copy(generation = UUID.randomUUID().toString())) } }
    @Test fun pin_is_persisted_before_delivery() { val (p, root, o) = setup(); p.registerConsumer(o, worker); assertTrue(p.snapshot(o).pinned); assertTrue(File(root, o.assetId + "/pin.v2").isFile); assertFalse(File(root, o.assetId + "/delivered.v2").exists()) }
    @Test fun delivery_without_pin_rejected() { val (p, _, o) = setup(); blocked { p.markDelivery(o) } }
    @Test fun register_replay_is_idempotent() { val (p, _, o) = setup(); p.registerConsumer(o, worker); assertEquals(p.snapshot(o), p.registerConsumer(o, worker)) }
    @Test fun conflicting_worker_registration_rejected() { val (p, _, o) = setup(); p.registerConsumer(o, worker); blocked { p.registerConsumer(o, worker.copy(pid = 131)) } }
    @Test fun revoked_state_is_never_authorized() { val (p, _, o) = delivered(); assertTrue(p.isAuthorized(o)); p.revoke(o, "cancelled"); assertFalse(p.isAuthorized(o)); assertTrue(p.snapshot(o).pinned) }
    @Test fun persistent_revoke_survives_new_protocol_instance() { val (p, root, o) = delivered(); p.revoke(o, "cancelled"); assertFalse(protocol(root).isAuthorized(o)); blocked { protocol(root).admission() } }
    @Test fun success_needs_matching_process_death() { val (p, _, o) = delivered(); blocked { p.complete(completion(p, o).copy(matchingProcessDead = false)) }; assertTrue(p.snapshot(o).pinned) }
    @Test fun success_needs_binder_death() { val (p, _, o) = delivered(); blocked { p.complete(completion(p, o).copy(binderDead = false)) } }
    @Test fun success_needs_both_fd_closures() { val (p, _, o) = delivered(); blocked { p.complete(completion(p, o).copy(sourceFdClosed = false)) }; blocked { p.complete(completion(p, o).copy(outputFdClosed = false)) } }
    @Test fun duplicate_result_cannot_publish() { val (p, _, o) = delivered(); blocked { p.complete(completion(p, o).copy(resultCount = 2)) }; blocked { p.recordPrepared(o) } }
    @Test fun wrong_process_instance_cannot_complete() { val (p, _, o) = delivered(); blocked { p.complete(completion(p, o).copy(worker = worker.copy(startTicks = 457))) } }
    @Test fun success_requires_strict_finish_and_destroy() { val (p, _, o) = delivered(); blocked { p.complete(completion(p, o).copy(strictFinished = false)) }; blocked { p.complete(completion(p, o).copy(decoderDestroyed = false)) } }
    @Test fun complete_requires_released_real_file_lock() { val (p, _, o) = delivered(); val evidence = completion(p, o); FileChannel.open(p.guardFile(o).toPath(), StandardOpenOption.WRITE).use { channel -> channel.lock().use { blocked { p.complete(evidence) } } }; assertTrue(p.snapshot(o).pinned) }
    @Test fun success_receipt_unlocks_pin_then_prepares_real_output() { val (p, _, o) = delivered(); p.complete(completion(p, o)); assertFalse(p.snapshot(o).pinned); p.recordPrepared(o); assertArrayEquals(byteArrayOf(5, 6, 7), p.verifyPrepared(o).file.readBytes()); p.admission() }
    @Test fun terminal_receipt_survives_host_restart() { val (p, root, o) = delivered(); p.complete(completion(p, o)); p.recordPrepared(o); assertEquals(p.verifyPrepared(o).output, protocol(root).verifyPrepared(o).output) }
    @Test fun cancelled_late_success_cannot_publish() { val (p, _, o) = delivered(); val evidence = completion(p, o); p.revoke(o, "cancelled"); blocked { p.complete(evidence) }; blocked { p.recordPrepared(o) } }
    @Test fun verified_failure_can_end_resources_without_publication() { val (p, _, o) = delivered(); p.revoke(o, "invalid"); p.complete(completion(p, o, JpegStageV2.Outcome.IMAGE)); assertFalse(p.snapshot(o).pinned); blocked { p.recordPrepared(o) }; p.admission() }
    @Test fun unknown_outcome_cannot_end_resources() { val (p, _, o) = delivered(); blocked { p.complete(completion(p, o, JpegStageV2.Outcome.RESULT_UNKNOWN)) }; assertTrue(p.snapshot(o).pinned) }
    @Test fun source_byte_tamper_blocks_receipt() { val (p, _, o) = delivered(); val evidence = completion(p, o); p.sourceFile(o).writeBytes(byteArrayOf(9)); blocked { p.complete(evidence) } }
    @Test fun output_tamper_blocks_prepared_restore() { val (p, _, o) = delivered(); p.complete(completion(p, o)); p.recordPrepared(o); p.outputFile(o).writeBytes(byteArrayOf(8)); blocked { p.verifyPrepared(o) } }
    @Test fun unknown_child_is_preserved_and_blocks_cleanup() { val (p, root, o) = delivered(); p.revoke(o, "invalid"); p.complete(completion(p, o, JpegStageV2.Outcome.IMAGE)); val unknown = File(root, o.assetId + "/foreign"); unknown.writeText("preserve"); blocked { p.safeCleanup(o) }; assertEquals("preserve", unknown.readText()); assertTrue(File(root, o.assetId + "/source.bin").exists()) }
    @Test fun nonterminal_cleanup_is_blocked() { val (p, _, o) = delivered(); blocked { p.safeCleanup(o) }; assertTrue(p.sourceFile(o).exists()) }
    @Test fun known_terminal_cleanup_is_idempotent() { val (p, root, o) = delivered(); p.revoke(o, "invalid"); p.complete(completion(p, o, JpegStageV2.Outcome.IMAGE)); p.safeCleanup(o); p.safeCleanup(o); assertFalse(File(root, o.assetId + "/source.bin").exists()); p.admission() }
    @Test fun unknown_root_entry_blocks_new_session_without_deletion() { val root = temporary.newFolder(); val unknown = File(root, "v1-owned").also { it.mkdir() }; blocked { protocol(root).admission() }; assertTrue(unknown.isDirectory) }
    @Test fun duplicate_record_key_is_rejected() { val (p, root, o) = setup(); File(root, o.assetId + "/owner.v2").appendText("version=2\n"); blocked { p.snapshot(o) } }
    @Test fun truncated_record_is_rejected() { val (p, root, o) = setup(); File(root, o.assetId + "/owner.v2").writeText("version=2"); blocked { p.snapshot(o) } }
    @Test fun wrong_record_version_is_rejected() { val (p, root, o) = setup(); val owner = File(root, o.assetId + "/owner.v2"); owner.writeText(owner.readText().replace("version=2", "version=1")); blocked { p.snapshot(o) } }
    @Test fun pin_record_tamper_blocks_authorization() { val (p, root, o) = delivered(); File(root, o.assetId + "/pin.v2").writeText("unknown=state\n"); assertFalse(p.isAuthorized(o)); blocked { p.admission() } }
    @Test fun guard_inode_replacement_is_rejected() { val (p, root, o) = delivered(); val guard = File(root, o.assetId + "/consumer.guard"); assertTrue(guard.renameTo(File(root.parentFile, "old.guard-${UUID.randomUUID()}"))); guard.writeBytes(byteArrayOf()); blocked { p.snapshot(o) } }
    @Test fun source_empty_is_rejected_without_eligible_stage() { val root = temporary.newFolder(); val p = protocol(root); val o = owner(); blocked { p.create(o, ByteArrayInputStream(byteArrayOf())) }; blocked { p.snapshot(o) }; assertFalse(File(root, o.assetId).exists()); assertTrue(File(root, "abort-${o.attemptId}.v2").readText().contains("proof=NO_CONSUMER")); p.admission() }
    @Test fun source_release_uses_strict_receipt_and_preserves_output() { val (p, root, o) = delivered(); p.complete(completion(p, o)); p.recordPrepared(o); p.releaseSource(o); assertFalse(File(root, o.assetId + "/source.bin").exists()); assertTrue(p.verifyPrepared(o).file.exists()); assertEquals(p.verifyPrepared(o).output, protocol(root).verifyPrepared(o).output); p.releaseSource(o) }
    @Test fun source_release_intent_recovers_interrupted_marker_write() { val (p, root, o) = delivered(); p.complete(completion(p, o)); p.recordPrepared(o); p.releaseSource(o); assertTrue(File(root, o.assetId + "/source-released.v2").delete()); assertTrue(protocol(root).verifyPrepared(o).file.exists()); protocol(root).releaseSource(o) }
    @Test fun missing_source_without_release_intent_is_never_inferred_valid() { val (p, root, o) = delivered(); p.complete(completion(p, o)); p.recordPrepared(o); assertTrue(File(root, o.assetId + "/source.bin").delete()); blocked { protocol(root).verifyPrepared(o) }; blocked { protocol(root).admission() } }
    @Test fun late_epoch_cannot_read_another_session_stage() { val (p, _, o) = delivered(); assertFalse(p.isAuthorized(o.copy(nonce = UUID.randomUUID().toString()))); blocked { p.revoke(o.copy(sessionId = UUID.randomUUID().toString()), "stale") }; assertTrue(p.isAuthorized(o)) }
    @Test fun output_inode_replacement_even_with_equal_bytes_is_rejected() { val (p, root, o) = delivered(); val evidence = completion(p, o); val output = p.outputFile(o); assertTrue(output.renameTo(File(root.parentFile, "old-output-${UUID.randomUUID()}"))); output.writeBytes(byteArrayOf(5, 6, 7)); blocked { p.complete(evidence) } }
    @Test fun terminal_guard_taken_again_blocks_new_session() { val (p, _, o) = delivered(); p.complete(completion(p, o)); FileChannel.open(p.guardFile(o).toPath(), StandardOpenOption.WRITE).use { channel -> channel.lock().use { blocked { p.admission() } } } }
    @Test fun namespace_lock_blocks_concurrent_protocol_actor() { val (p, root, o) = setup(); FileChannel.open(File(root, ".namespace.guard").toPath(), StandardOpenOption.WRITE).use { channel -> channel.lock().use { blocked { protocol(root).snapshot(o) } } } }
    @Test fun terminal_cleanup_permits_new_attempt_without_reusing_identity() { val (p, root, o) = delivered(); p.complete(completion(p, o, JpegStageV2.Outcome.IMAGE)); p.safeCleanup(o); val next = owner(); p.create(next, ByteArrayInputStream(byteArrayOf(7))); assertEquals(JpegStageV2.State.SOURCE_STAGED, p.snapshot(next).state); assertTrue(File(root, o.assetId + "/terminal.v2").isFile) }
    @Test fun source_release_never_applies_to_unprepared_attempt() { val (p, _, o) = delivered(); blocked { p.releaseSource(o) }; assertTrue(p.sourceFile(o).exists()) }
    @Test fun cleanup_keeps_business_metadata_for_repository_reference_checks() { val (p, root, o) = delivered(); p.complete(completion(p, o, JpegStageV2.Outcome.IMAGE)); val metadata = File(root, o.assetId + "/asset.json").also { it.writeText("kept") }; p.safeCleanup(o); assertEquals("kept", metadata.readText()) }
    @Test fun malformed_receipt_never_unlocks_pin_or_admission() { val (p, root, o) = delivered(); File(root, o.assetId + "/terminal.v2").writeText("outcome=SUCCESS\n"); blocked { p.snapshot(o) }; blocked { p.admission() }; assertTrue(File(root, o.assetId + "/pin.v2").isFile) }
    @Test fun wrong_worker_uid_is_not_a_matching_death_proof() { val (p, _, o) = delivered(); blocked { p.complete(completion(p, o).copy(worker = worker.copy(uid = 10124))) }; assertTrue(p.snapshot(o).pinned) }
    @Test fun unknown_outcome_preserves_no_publication_even_with_all_death_flags() { val (p, root, o) = delivered(); blocked { p.complete(completion(p, o, JpegStageV2.Outcome.RESULT_UNKNOWN)) }; assertFalse(File(root, o.assetId + "/terminal.v2").exists()); assertTrue(p.snapshot(o).pinned) }
    @Test fun durability_failure_does_not_count_as_terminal_success() {
        val root = temporary.newFolder(); var refuseSync = false
        val p = JpegStageV2(root, generation, ::fileIdentity) { if (refuseSync) throw java.io.IOException("injected directory fsync failure") }
        val o = owner(); p.create(o, ByteArrayInputStream(byteArrayOf(1))); p.registerConsumer(o, worker); p.markDelivery(o)
        val evidence = completion(p, o); refuseSync = true
        blocked { p.complete(evidence) }; blocked { p.admission() }; blocked { p.verifyPrepared(o) }
        refuseSync = false; assertTrue(p.snapshot(o).pinned); p.complete(evidence); p.recordPrepared(o); assertTrue(p.verifyPrepared(o).file.exists())
    }
    @Test fun no_consumer_read_failure_leaves_only_bound_abort_proof() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner(); var sent = false
        val broken = object : java.io.InputStream() {
            override fun read(): Int = throw java.io.IOException("synthetic source error")
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int { if (sent) throw java.io.IOException("synthetic source error"); sent = true; bytes[offset] = 3; return 1 }
        }
        blocked { p.create(o, broken) }; assertFalse(File(root, o.assetId).exists()); assertTrue(File(root, "abort-${o.attemptId}.v2").readText().contains("sourceBytes=1")); p.admission()
    }
    @Test fun no_consumer_zero_progress_stream_is_not_an_infinite_copy() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner()
        blocked { p.create(o, object : java.io.InputStream() { override fun read() = 0; override fun read(bytes: ByteArray, offset: Int, length: Int) = 0 }) }
        p.admission(); assertFalse(File(root, o.assetId).exists())
    }
    @Test fun no_consumer_real_64mib_input_limit_aborts_without_writing_excess() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner(); var remaining = 64L * 1024 * 1024 + 1
        val oversized = object : java.io.InputStream() {
            override fun read(): Int = if (remaining-- > 0) 7 else -1
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int { if (remaining <= 0) return -1; val count = minOf(length.toLong(), remaining).toInt(); bytes.fill(7, offset, offset + count); remaining -= count; return count }
        }
        assertThrows(JpegStageV2.SourceLimitExceeded::class.java) { p.create(o, oversized) }; assertFalse(File(root, o.assetId).exists()); assertTrue(File(root, "abort-${o.attemptId}.v2").readText().contains("sourceBytes=67108864")); p.admission()
    }
    @Test fun owner_fsync_unknown_does_not_issue_no_consumer_proof() {
        val root = temporary.newFolder(); val o = owner()
        val p = JpegStageV2(root, generation, ::fileIdentity) { if (File(it, "owner.v2").exists()) throw java.io.IOException("synthetic owner sync failure") }
        blocked { p.create(o, ByteArrayInputStream(byteArrayOf())) }; blocked { p.admission() }; assertTrue(File(root, o.assetId + "/owner.v2").exists()); assertFalse(File(root, "abort-${o.attemptId}.v2").exists())
    }
    @Test fun abort_marker_fsync_unknown_keeps_all_creation_files() {
        val root = temporary.newFolder(); val o = owner(); var failMarkerSync = true
        val p = JpegStageV2(root, generation, ::fileIdentity) { if (failMarkerSync && it == root && File(root, "abort-${o.attemptId}.v2").exists()) throw java.io.IOException("synthetic marker sync failure") }
        blocked { p.create(o, ByteArrayInputStream(byteArrayOf())) }; blocked { p.admission() }; assertTrue(File(root, o.assetId + "/source.bin").exists()); assertTrue(File(root, o.assetId + "/owner.v2").exists())
        failMarkerSync = false
        // A visible proof does not authorize restart to finish an interrupted deletion.
        blocked { p.admission() }; assertTrue(File(root, o.assetId + "/source.bin").exists())
    }
    @Test fun unknown_creation_child_prevents_abort_cleanup() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner(); val foreign = File(root, o.assetId + "/foreign")
        val stream = object : java.io.InputStream() { override fun read(): Int { foreign.writeText("preserved"); throw java.io.IOException("synthetic read failure") } }
        blocked { p.create(o, stream) }; blocked { p.admission() }; assertEquals("preserved", foreign.readText()); assertTrue(File(root, o.assetId + "/source.bin").exists()); assertFalse(File(root, "abort-${o.attemptId}.v2").exists())
    }
    @Test fun source_inode_replacement_during_create_is_never_cleaned() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner(); val source = File(root, o.assetId + "/source.bin")
        val stream = object : java.io.InputStream() { override fun read(): Int { check(source.renameTo(File(root.parentFile, "replaced-source-${UUID.randomUUID()}"))); source.writeBytes(byteArrayOf(8)); throw java.io.IOException("synthetic read failure") } }
        blocked { p.create(o, stream) }; blocked { p.admission() }; assertArrayEquals(byteArrayOf(8), source.readBytes()); assertFalse(File(root, "abort-${o.attemptId}.v2").exists())
    }
    @Test fun guard_inode_replacement_during_create_is_never_cleaned() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner(); val guard = File(root, o.assetId + "/consumer.guard")
        val stream = object : java.io.InputStream() { override fun read(): Int { check(guard.renameTo(File(root.parentFile, "replaced-guard-${UUID.randomUUID()}"))); guard.writeBytes(byteArrayOf()); throw java.io.IOException("synthetic read failure") } }
        blocked { p.create(o, stream) }; blocked { p.admission() }; assertTrue(File(root, o.assetId + "/source.bin").exists()); assertFalse(File(root, "abort-${o.attemptId}.v2").exists())
    }
    @Test fun abort_proof_corruption_blocks_future_admission() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner(); blocked { p.create(o, ByteArrayInputStream(byteArrayOf())) }
        val marker = File(root, "abort-${o.attemptId}.v2"); marker.appendText("unknown=field\n"); blocked { p.admission() }; blocked { p.create(owner(), ByteArrayInputStream(byteArrayOf(1))) }; assertTrue(marker.exists())
    }
    @Test fun abort_proof_cannot_be_replayed_under_another_path() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner(); blocked { p.create(o, ByteArrayInputStream(byteArrayOf())) }
        val marker = File(root, "abort-${o.attemptId}.v2"); assertTrue(marker.renameTo(File(root, "abort-${UUID.randomUUID()}.v2"))); blocked { p.admission() }
    }
    @Test fun aborted_attempt_cannot_reacquire_authority() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner(); blocked { p.create(o, ByteArrayInputStream(byteArrayOf())) }
        blocked { p.create(o, ByteArrayInputStream(byteArrayOf(1))) }; assertFalse(p.isAuthorized(o)); blocked { p.recordPrepared(o) }; assertFalse(File(root, o.assetId).exists()); p.admission()
    }
    @Test fun stable_asset_may_retry_with_a_new_attempt_after_verified_abort() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner(); blocked { p.create(o, ByteArrayInputStream(byteArrayOf())) }
        val next = o.copy(attemptId = UUID.randomUUID().toString(), sessionId = UUID.randomUUID().toString(), nonce = UUID.randomUUID().toString())
        p.create(next, ByteArrayInputStream(byteArrayOf(1))); p.registerConsumer(next, worker); p.markDelivery(next); assertTrue(p.isAuthorized(next)); assertFalse(p.isAuthorized(o)); blocked { p.admission() }
    }
    @Test fun copied_abort_proof_cannot_authorize_another_owner() {
        val root = temporary.newFolder(); val p = protocol(root); val o = owner(); blocked { p.create(o, ByteArrayInputStream(byteArrayOf())) }
        val marker = File(root, "abort-${o.attemptId}.v2"); marker.writeText(marker.readText().replace(o.assetId, UUID.randomUUID().toString())); blocked { p.admission() }
    }
    @Test fun decoder_policy_is_bound_into_the_owner_and_terminal_hash() {
        val (p, root, o) = delivered(); assertTrue(File(root, o.assetId + "/owner.v2").readText().contains("decoderPolicy=libjpeg-turbo-3.2.0-e01-strict-v1")); p.complete(completion(p, o)); p.recordPrepared(o)
        val owner = File(root, o.assetId + "/owner.v2"); owner.writeText(owner.readText().replace("e01-strict-v1", "e01-strict-v2")); blocked { p.verifyPrepared(o) }; assertFalse(p.isAuthorized(o))
    }
    @Test fun normalization_policy_mismatch_cannot_restore_a_prepared_image() {
        val (p, root, o) = delivered(); p.complete(completion(p, o)); p.recordPrepared(o); val owner = File(root, o.assetId + "/owner.v2"); owner.writeText(owner.readText().replace("m10-jpeg-normalize-v1", "m10-jpeg-normalize-v2")); blocked { p.verifyPrepared(o) }; blocked { p.admission() }
    }
    @Test fun unknown_connection_cleanup_cannot_release_a_consumer_pin() { val (p, _, o) = delivered(); blocked { p.complete(completion(p, o).copy(connectionCleaned = false)) }; assertTrue(p.snapshot(o).pinned); blocked { p.recordPrepared(o) } }
    @Test fun repeated_revoke_retains_first_reason_and_confirms_its_durability() {
        val (p, root, o) = delivered(); p.revoke(o, "first_failure"); val marker = File(root, o.assetId + "/revoked.v2"); val first = marker.readBytes()
        p.revoke(o, "later_catch_failure"); assertArrayEquals(first, marker.readBytes()); assertFalse(protocol(root).isAuthorized(o)); assertTrue(protocol(root).snapshot(o).pinned)
    }
    @Test fun failure_result_with_unknown_connection_cleanup_keeps_pin() { val (p, _, o) = delivered(); p.revoke(o, "invalid"); blocked { p.complete(completion(p, o, JpegStageV2.Outcome.IMAGE).copy(connectionCleaned = false)) }; assertTrue(p.snapshot(o).pinned); blocked { p.admission() } }
    @Test fun repeated_revoke_fsync_unknown_does_not_replace_first_reason() {
        val root = temporary.newFolder(); var refuseSync = false
        val p = JpegStageV2(root, generation, ::fileIdentity) { if (refuseSync) throw java.io.IOException("synthetic revoke sync failure") }
        val o = owner(); p.create(o, ByteArrayInputStream(byteArrayOf(1))); p.registerConsumer(o, worker); p.markDelivery(o); p.revoke(o, "first_failure")
        val marker = File(root, o.assetId + "/revoked.v2"); val first = marker.readBytes(); refuseSync = true
        blocked { p.revoke(o, "different_catch") }; assertArrayEquals(first, marker.readBytes()); refuseSync = false
        p.revoke(o, "third_catch"); assertArrayEquals(first, marker.readBytes()); assertTrue(p.snapshot(o).pinned)
    }
}

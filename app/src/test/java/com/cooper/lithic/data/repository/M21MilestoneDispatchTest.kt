package com.cooper.lithic.data.repository

import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.domain.reminder.*
import com.cooper.lithic.platform.notification.MilestonePublisher
import java.io.IOException
import java.time.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class M21MilestoneDispatchTest {
    private val day = LocalDate.parse("2026-04-10")
    private class Publisher : MilestonePublisher {
        var allowed = true
        var success = true
        var callback: () -> Unit = {}
        val summaries = mutableListOf<MilestoneSummary>()
        override fun canPublish() = allowed
        override fun publish(summary: MilestoneSummary): Boolean {
            callback(); summaries += summary; return success
        }
    }
    private inner class Harness {
        val p = ProjectMilestoneSource("project", "合成项目", LocalDate.parse("2026-01-01"))
        var input = MilestoneDispatchInput(listOf(p), emptyList(), listOf(MilestoneWatermark(
            MilestoneOwnerType.PROJECT, p.ownerId, p.startedOn, 0)), emptyList(), ReminderPreferences(), ZoneId.of("Asia/Shanghai"))
        val coordinator = LocalWriteCoordinator()
        var current = true
        var instant = day.atTime(9, 0).atZone(input.recordZone).toInstant()
        val publisher = Publisher()
        var beforeRecord: suspend () -> Unit = {}
        val batches = mutableListOf<List<MilestoneCandidate>>()
        suspend fun execute() = MilestoneDispatch("generation", coordinator, { current }, { input }, { rows, at ->
            beforeRecord(); batches += rows
            input = input.copy(receipts = input.receipts + rows.map {
                MilestonePublishedReceipt(it.ownerType, it.ownerId, it.nodeCode, it.logicalKey, at)
            })
        }, publisher, { instant }).execute()
    }
    @Test fun publishesOneActualBatchAndRecheckDoesNotNotifyAgain() = runBlocking {
        val h = Harness(); assertEquals(ReminderDispatchResult.PUBLISHED, h.execute())
        assertEquals(1, h.batches.size); assertEquals(1, h.publisher.summaries.size)
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute()); assertEquals(1, h.publisher.summaries.size)
    }
    @Test fun newSameDayCandidateUpdatesStableSummaryAndKeepsPreviousSources() = runBlocking {
        val h = Harness(); h.execute()
        val p2 = h.p.copy(ownerId = "second")
        h.input = h.input.copy(projects = h.input.projects + p2, watermarks = h.input.watermarks +
            MilestoneWatermark(MilestoneOwnerType.PROJECT, p2.ownerId, p2.startedOn, 0))
        h.execute(); assertEquals(2, h.publisher.summaries.last().candidates.size)
        assertEquals(h.publisher.summaries.first().tag, h.publisher.summaries.last().tag)
        assertEquals(listOf(1, 1), h.batches.map { it.size })
    }
    @Test fun permissionFailureAndSwitchOffNeverInventReceipts() = runBlocking {
        val h = Harness(); h.publisher.allowed = false; h.execute(); assertTrue(h.batches.isEmpty())
        h.publisher.allowed = true; h.input = h.input.copy(preferences = ReminderPreferences(milestoneEnabled = false))
        h.execute(); assertTrue(h.publisher.summaries.isEmpty())
    }
    @Test fun platformFalseAndOldGenerationNeverRecordSuccess() = runBlocking {
        val h = Harness(); h.publisher.success = false; h.execute(); assertTrue(h.batches.isEmpty())
        h.current = false; assertEquals(ReminderDispatchResult.OLD_GENERATION, h.execute())
        assertEquals(1, h.publisher.summaries.size)
    }
    @Test fun beforeNineKeepsEligibilityWithoutPublishing() = runBlocking {
        val h = Harness(); h.instant = h.instant.minusSeconds(1)
        assertEquals(ReminderDispatchResult.WAITING_FOR_NINE, h.execute()); assertTrue(h.batches.isEmpty())
    }
    @Test fun receiptBatchFailureIsPropagatedAndStableRetryIsFiniteIdempotency() = runBlocking {
        val h = Harness(); h.beforeRecord = { throw IOException("synthetic batch rollback") }
        assertTrue(runCatching { h.execute() }.exceptionOrNull() is IOException); assertTrue(h.input.receipts.isEmpty())
        h.beforeRecord = {}; h.execute(); assertEquals(2, h.publisher.summaries.size)
        assertEquals(h.publisher.summaries.first().tag, h.publisher.summaries.last().tag)
        assertEquals(1, h.input.receipts.size)
    }
    @Test fun concurrentDispatchPublishesOnlyOnceUnderRealGenerationCoordinator() = runBlocking {
        val h = Harness(); awaitAll(async { h.execute() }, async { h.execute() })
        assertEquals(1, h.batches.size); assertEquals(1, h.publisher.summaries.size)
    }
    @Test fun cancellationAfterSuccessfulApiStillPersistsActualReceipt() = runBlocking {
        val h = Harness()
        val job = launch { h.publisher.callback = { cancel() }; h.execute() }; job.join()
        assertEquals(1, h.input.receipts.size)
    }
    @Test fun summaryUsesAllSourcesBeyondDisplayWindowAndClickIsBoundedReference() = runBlocking {
        val h = Harness(); h.input = h.input.copy(projects = (0..75).map { h.p.copy(ownerId = "p$it") },
            watermarks = (0..75).map { MilestoneWatermark(MilestoneOwnerType.PROJECT, "p$it", h.p.startedOn, 0) })
        h.execute(); val summary = h.publisher.summaries.single()
        assertEquals(76, summary.reference.count); assertEquals(64, summary.reference.digest.length)
        assertEquals(76, MilestoneSummaries.resolve(summary.reference, "generation", h.input).size)
        assertTrue(summary.text.length < 1000)
    }
    @Test fun clickRejectsDeletionHistoricalDateCorrectionAndOldGeneration() = runBlocking {
        val h = Harness(); h.execute(); val ref = h.publisher.summaries.single().reference
        assertTrue(runCatching { MilestoneSummaries.resolve(ref, "new-generation", h.input) }.isFailure)
        val corrected = h.input.copy(projects = listOf(h.p.copy(startedOn = h.p.startedOn.minusDays(1))))
        assertTrue(runCatching { MilestoneSummaries.resolve(ref, "generation", corrected) }.isFailure)
        assertTrue(runCatching { MilestoneSummaries.resolve(ref, "generation", h.input.copy(projects = emptyList())) }.isFailure)
    }
    @Test fun renamingKeepsAccurateIdentityAndUsesFreshName() = runBlocking {
        val h = Harness(); h.execute(); val ref = h.publisher.summaries.single().reference
        val sources = MilestoneSummaries.resolve(ref, "generation", h.input.copy(projects = listOf(h.p.copy(name = "新名称"))))
        assertTrue(sources.single().title.contains("新名称"))
    }
    @Test fun summaryTypedIdentitySeparatesSameUuidAndRejectsDuplicateNodes() {
        val one = MilestoneCandidate(MilestoneOwnerType.PROJECT, "same", "D100", 100, day, "MILESTONE:PROJECT:same:D100", "P", "P")
        val two = one.copy(ownerType = MilestoneOwnerType.SUBSCRIPTION, logicalKey = "MILESTONE:SUBSCRIPTION:same:D100")
        assertEquals(2, MilestoneSummaries.create("g", day, listOf(one, two)).reference.count)
        assertTrue(runCatching { MilestoneSummaries.create("g", day, listOf(one, one)) }.isFailure)
    }
    @Test fun crossingBusinessDateDuringPlanningDoesNotPublishAnExpiredCandidate() = runBlocking {
        val h = Harness()
        val old = day.plusDays(6).atTime(23, 59, 59).atZone(h.input.recordZone).toInstant()
        var reads = 0
        val result = MilestoneDispatch("generation", h.coordinator, { true }, { h.input }, { _, _ ->
            fail("No successful receipt may be written")
        }, h.publisher, { if (reads++ == 0) old else old.plusSeconds(2) }).execute()
        assertEquals(ReminderDispatchResult.SUPPRESSED, result)
        assertTrue(h.publisher.summaries.isEmpty())
    }
}

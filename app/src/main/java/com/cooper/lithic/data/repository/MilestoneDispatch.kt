package com.cooper.lithic.data.repository

import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.domain.reminder.*
import com.cooper.lithic.platform.notification.MilestonePublisher
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.security.MessageDigest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class MilestonePublishedReceipt(val ownerType: MilestoneOwnerType, val ownerId: String,
    val nodeCode: String, val logicalKey: String, val publishedAt: Instant)

data class MilestoneDispatchInput(val projects: List<ProjectMilestoneSource>,
    val subscriptions: List<RenewalReminderSource>, val watermarks: List<MilestoneWatermark>,
    val receipts: List<MilestonePublishedReceipt>, val preferences: ReminderPreferences, val recordZone: ZoneId)

/** A bounded reference. Sources are reconstructed from actual receipts and fresh facts on click. */
data class MilestoneNoticeRef(val generationId: String, val publicationOn: LocalDate,
    val digest: String, val count: Int)

data class MilestoneSummary(val reference: MilestoneNoticeRef, val candidates: List<MilestoneCandidate>) {
    val tag get() = "lithic-milestone-${reference.generationId}-${reference.publicationOn}"
    val notificationId get() = 21
    val title get() = "${candidates.size}个长期里程碑"
    val text get() = candidates.take(3).joinToString("\n") { "${it.title}：${it.text}" } +
        if (candidates.size > 3) "\n另有${candidates.size - 3}项，点击查看来源" else ""
}

object MilestoneSummaries {
    fun create(generationId: String, day: LocalDate, candidates: List<MilestoneCandidate>): MilestoneSummary {
        require(generationId.isNotBlank() && candidates.isNotEmpty())
        require(candidates.map { it.logicalKey }.distinct().size == candidates.size) { "Duplicate milestone source" }
        val sorted = candidates.sortedBy { it.logicalKey }
        sorted.forEach { row ->
            require(row.logicalKey == MilestonePlanner.logicalKey(row.ownerType, row.ownerId, row.nodeCode))
            require(!row.reachedOn.isAfter(day))
        }
        // Names/amounts are not a stored display cache. Historical date changes invalidate
        // the reference; harmless renames are shown using their current real name.
        val identity = sorted.joinToString("\n", prefix = "$generationId\n$day\n") {
            "${it.logicalKey}:${it.reachedOn}"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return MilestoneSummary(MilestoneNoticeRef(generationId, day, digest, sorted.size), sorted)
    }
    fun publishedSources(input: MilestoneDispatchInput, day: LocalDate): List<MilestoneCandidate> {
        require(input.receipts.map { it.logicalKey }.distinct().size == input.receipts.size) { "Duplicate published receipt" }
        val owners = input.projects.map { MilestoneOwnerType.PROJECT to it.ownerId }.toSet() +
            input.subscriptions.map { MilestoneOwnerType.SUBSCRIPTION to it.ownerId }
        input.receipts.forEach {
            require(it.logicalKey == MilestonePlanner.logicalKey(it.ownerType, it.ownerId, it.nodeCode))
            require(it.ownerType to it.ownerId in owners) { "Published source has been removed" }
        }
        val keys = input.receipts.filter { it.publishedAt.atZone(input.recordZone).toLocalDate() == day }
            .map { it.logicalKey }.toSet()
        return (input.projects.flatMap { MilestonePlanner.projectReached(it, day) } +
            input.subscriptions.flatMap { MilestonePlanner.subscriptionReached(it, day) })
            .filter { it.logicalKey in keys }
    }
    fun resolve(reference: MilestoneNoticeRef, generationId: String, input: MilestoneDispatchInput): List<MilestoneCandidate> {
        require(reference.generationId == generationId) { "Notification belongs to another dataset" }
        val sources = publishedSources(input, reference.publicationOn)
        val fresh = create(generationId, reference.publicationOn, sources)
        require(fresh.reference == reference) { "Milestone sources were deleted or corrected" }
        return fresh.candidates
    }
}

class MilestoneDispatch(private val generationId: String, private val coordinator: LocalWriteCoordinator,
    private val isCurrent: () -> Boolean, private val read: suspend () -> MilestoneDispatchInput,
    private val recordBatch: suspend (List<MilestoneCandidate>, Instant) -> Unit,
    private val publisher: MilestonePublisher, private val now: () -> Instant = Instant::now) {
    suspend fun execute(): ReminderDispatchResult = coordinator.withWrite {
        if (!isCurrent()) return@withWrite ReminderDispatchResult.OLD_GENERATION
        val input = read()
        currentCoroutineContext().ensureActive()
        if (!isCurrent()) return@withWrite ReminderDispatchResult.OLD_GENERATION
        if (!input.preferences.milestoneEnabled || !publisher.canPublish()) return@withWrite ReminderDispatchResult.SUPPRESSED
        val observed = now().atZone(input.recordZone)
        val atNine = if (observed.hour < 9) observed.toLocalDate().atTime(9, 0).atZone(input.recordZone) else observed
        val candidates = MilestonePlanner.evaluate(input.projects, input.subscriptions, atNine,
            input.watermarks, input.receipts.map { it.logicalKey }.toSet(), true)
        if (candidates.isEmpty()) return@withWrite ReminderDispatchResult.SUPPRESSED
        if (observed.hour < 9) return@withWrite ReminderDispatchResult.WAITING_FOR_NINE
        val prior = MilestoneSummaries.publishedSources(input, observed.toLocalDate())
        val summary = MilestoneSummaries.create(generationId, observed.toLocalDate(), prior + candidates)
        currentCoroutineContext().ensureActive()
        if (!isCurrent()) return@withWrite ReminderDispatchResult.OLD_GENERATION
        val atCall = now().atZone(input.recordZone)
        if (atCall.toLocalDate() != observed.toLocalDate() || atCall.hour < 9) {
            return@withWrite ReminderDispatchResult.SUPPRESSED
        }
        if (!publisher.publish(summary)) return@withWrite ReminderDispatchResult.SUPPRESSED
        // Like M20, successful API publication has a finite crash-to-receipt window.
        // Persist the entire NEW batch atomically even if the caller was just cancelled.
        withContext(NonCancellable) {
            val at = now()
            check(at.atZone(input.recordZone).toLocalDate() == observed.toLocalDate()) {
                "Publication crossed a business date; receipt outcome remains uncertain"
            }
            recordBatch(candidates, at)
        }
        ReminderDispatchResult.PUBLISHED
    }
}

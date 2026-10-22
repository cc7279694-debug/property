package com.cooper.lithic.domain.reminder

import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

enum class MilestoneOwnerType { PROJECT, SUBSCRIPTION }

data class MilestoneWatermark(
    val ownerType: MilestoneOwnerType,
    val ownerId: String,
    val cutoffOn: LocalDate,
    val suppressedMask: Int,
) {
    init {
        require(ownerId.isNotBlank() && ':' !in ownerId)
        require(suppressedMask in 0..63) { "Unknown milestone suppression bits" }
    }
}

data class ProjectMilestoneSource(
    val ownerId: String,
    val name: String,
    val startedOn: LocalDate,
    val endedOn: LocalDate? = null,
)

data class MilestoneCandidate(
    val ownerType: MilestoneOwnerType,
    val ownerId: String,
    val nodeCode: String,
    val threshold: Long,
    val reachedOn: LocalDate,
    val logicalKey: String,
    val title: String,
    val text: String,
)

/** Pure, non-publishing planning. Repositories must recheck facts under their own
 * generation lease immediately before using these candidates to publish. */
object MilestonePlanner {
    private data class Node(val code: String, val threshold: Long, val calendarYears: Boolean = false)

    private val projectNodes = listOf(
        Node("D100", 100), Node("D1000", 1000), Node("Y1", 1, true),
        Node("Y3", 3, true), Node("Y5", 5, true), Node("Y10", 10, true),
    )
    private val subscriptionNodes = listOf(
        Node("D100", 100), Node("D365", 365), Node("D1000", 1000),
        Node("D1095", 1095), Node("D1825", 1825), Node("D3650", 3650),
    )

    fun projectReached(source: ProjectMilestoneSource, asOf: LocalDate): List<MilestoneCandidate> {
        validateProject(source)
        val through = minOf(asOf, source.endedOn ?: asOf)
        return projectNodes.mapNotNull { node ->
            // A future target beyond LocalDate.MAX cannot have been reached. It is
            // omitted rather than wrapped, clamped, or replaced with a false date.
            val reachedOn = try {
                if (node.calendarYears) source.startedOn.plusYears(node.threshold)
                else source.startedOn.plusDays(node.threshold - 1)
            } catch (_: DateTimeException) {
                return@mapNotNull null
            }
            if (reachedOn.isAfter(through)) return@mapNotNull null
            val state = if (source.endedOn?.let { !it.isAfter(asOf) } == true) "项目已结束；" else ""
            val label = if (node.calendarYears) "${node.threshold}个日历周年" else "${node.threshold}个持有日"
            candidate(MilestoneOwnerType.PROJECT, source.ownerId, source.name, node, reachedOn,
                "${state}于${reachedOn}达到${label}。此提醒只记录真实时间节点。")
        }
    }

    fun subscriptionReached(source: RenewalReminderSource, asOf: LocalDate): List<MilestoneCandidate> {
        val snapshot = subscriptionSnapshot(source)
        // All intervals, state boundaries and confirmed payment totals are validated
        // by the frozen M03 engine, including facts outside the delivery window.
        val observation = SubscriptionCostEngine.evaluate(snapshot, asOf)
        if (observation.elapsedPaidDays < subscriptionNodes.first().threshold) return emptyList()
        val firstCoveredOn = source.renewals.minOf { it.renewal.effectiveFrom }
        val counts = mutableMapOf(asOf to observation.elapsedPaidDays)
        fun count(on: LocalDate): Long = counts.getOrPut(on) {
            SubscriptionCostEngine.evaluate(snapshot, on).elapsedPaidDays
        }
        val state = when (observation.displayStatus) {
            SubscriptionDisplayStatus.ACTIVE -> ""
            SubscriptionDisplayStatus.PAUSED -> "当前记录已暂停；"
            SubscriptionDisplayStatus.EXPIRED -> "当前付费覆盖已到期；"
            SubscriptionDisplayStatus.STOPPED -> "当前记录已停止；"
        }
        return subscriptionNodes.mapNotNull { node ->
            if (observation.elapsedPaidDays < node.threshold) return@mapNotNull null
            // Paid coverage intersected with ENABLED time is a monotone count for
            // fixed facts. Search at most 40 dates across the full LocalDate range;
            // never enumerate days or query a database from this pure planner.
            var low = firstCoveredOn.toEpochDay()
            var high = asOf.toEpochDay()
            while (low < high) {
                val middle = low + (high - low) / 2
                if (count(LocalDate.ofEpochDay(middle)) >= node.threshold) high = middle
                else low = middle + 1
            }
            val reachedOn = LocalDate.ofEpochDay(low)
            candidate(MilestoneOwnerType.SUBSCRIPTION, source.ownerId, source.name, node, reachedOn,
                "${state}于${reachedOn}达到累计付费有效天数${node.threshold}天；暂停、停止及无付费覆盖日期不计入。")
        }
    }

    fun projectWatermark(source: ProjectMilestoneSource, on: LocalDate, previous: MilestoneWatermark? = null): MilestoneWatermark {
        validatePrevious(MilestoneOwnerType.PROJECT, source.ownerId, previous)
        val cutoff = maxOf(on, previous?.cutoffOn ?: on)
        return mergeWatermark(MilestoneOwnerType.PROJECT, source.ownerId, cutoff,
            projectReached(source, cutoff), previous)
    }

    fun subscriptionWatermark(source: RenewalReminderSource, on: LocalDate, previous: MilestoneWatermark? = null): MilestoneWatermark {
        validatePrevious(MilestoneOwnerType.SUBSCRIPTION, source.ownerId, previous)
        val cutoff = maxOf(on, previous?.cutoffOn ?: on)
        return mergeWatermark(MilestoneOwnerType.SUBSCRIPTION, source.ownerId, cutoff,
            subscriptionReached(source, cutoff), previous)
    }

    fun evaluate(
        projects: List<ProjectMilestoneSource>,
        subscriptions: List<RenewalReminderSource>,
        recordZoneNow: ZonedDateTime,
        watermarks: List<MilestoneWatermark>,
        successfulKeys: Set<String>,
        milestoneEnabled: Boolean = true,
    ): List<MilestoneCandidate> {
        val sourceKeys = projects.map { MilestoneOwnerType.PROJECT to it.ownerId } +
            subscriptions.map { MilestoneOwnerType.SUBSCRIPTION to it.ownerId }
        require(sourceKeys.distinct().size == sourceKeys.size) { "Milestone owners must be unique within each type" }
        val marks = watermarks.associateBy { it.ownerType to it.ownerId }
        require(marks.size == watermarks.size && marks.keys == sourceKeys.toSet()) {
            "Milestone evaluation requires one matching watermark for every owner and no orphan watermarks"
        }
        val paymentIds = mutableSetOf<String>()
        subscriptions.forEach { source ->
            source.renewals.forEach { row ->
                require(paymentIds.add(row.id)) { "Milestone payments require globally unique stable row identities" }
            }
        }
        val today = recordZoneNow.toLocalDate()
        val reached = projects.flatMap { projectReached(it, today) } +
            subscriptions.flatMap { subscriptionReached(it, today) }
        if (!milestoneEnabled || recordZoneNow.hour < 9) return emptyList()
        return reached.filter { event ->
            val watermark = marks.getValue(event.ownerType to event.ownerId)
            event.reachedOn.isAfter(watermark.cutoffOn) &&
                (watermark.suppressedMask and nodeBit(event.ownerType, event.nodeCode)) == 0 &&
                event.logicalKey !in successfulKeys &&
                ChronoUnit.DAYS.between(event.reachedOn, today) in 0L..6L
        }.sortedWith(compareBy<MilestoneCandidate> { it.reachedOn }.thenBy { it.logicalKey })
    }

    fun logicalKey(ownerType: MilestoneOwnerType, ownerId: String, nodeCode: String): String {
        validateOwner(ownerId)
        validateNode(ownerType, nodeCode)
        return "MILESTONE:${ownerType.name}:$ownerId:$nodeCode"
    }

    fun validateNode(ownerType: MilestoneOwnerType, nodeCode: String) {
        require(nodes(ownerType).any { it.code == nodeCode }) { "Unknown $ownerType milestone node" }
    }

    fun nodeCodes(ownerType: MilestoneOwnerType): List<String> = nodes(ownerType).map { it.code }

    fun nodeBit(ownerType: MilestoneOwnerType, nodeCode: String): Int {
        validateNode(ownerType, nodeCode)
        return 1 shl nodes(ownerType).indexOfFirst { it.code == nodeCode }
    }

    private fun nodes(ownerType: MilestoneOwnerType): List<Node> = when (ownerType) {
        MilestoneOwnerType.PROJECT -> projectNodes
        MilestoneOwnerType.SUBSCRIPTION -> subscriptionNodes
    }

    private fun validateOwner(ownerId: String) {
        require(ownerId.isNotBlank() && ':' !in ownerId) { "A milestone requires its stable, unambiguous owner identity" }
    }

    private fun validateProject(source: ProjectMilestoneSource) {
        validateOwner(source.ownerId)
        require(source.name.isNotBlank()) { "A milestone requires the actual record name" }
        require(source.endedOn?.isBefore(source.startedOn) != true) { "A project cannot end before its start" }
    }

    private fun subscriptionSnapshot(source: RenewalReminderSource): SubscriptionSnapshot {
        validateOwner(source.ownerId)
        require(source.name.isNotBlank()) { "A milestone requires the actual record name" }
        require(source.renewals.all { it.id.isNotBlank() } &&
            source.renewals.map { it.id }.distinct().size == source.renewals.size) {
            "A milestone payment requires a unique stable row identity"
        }
        return SubscriptionSnapshot(Subscription(source.ownerId), source.renewals.map { it.renewal }, source.stateChanges)
    }

    private fun validatePrevious(ownerType: MilestoneOwnerType, ownerId: String, previous: MilestoneWatermark?) {
        require(previous == null || previous.ownerType == ownerType && previous.ownerId == ownerId) {
            "A milestone watermark cannot be merged across owner identities"
        }
    }

    private fun mergeWatermark(
        ownerType: MilestoneOwnerType,
        ownerId: String,
        cutoff: LocalDate,
        reached: List<MilestoneCandidate>,
        previous: MilestoneWatermark?,
    ): MilestoneWatermark {
        val mask = reached.fold(previous?.suppressedMask ?: 0) { mask, event ->
            mask or nodeBit(ownerType, event.nodeCode)
        }
        return MilestoneWatermark(ownerType, ownerId, cutoff, mask)
    }

    private fun candidate(
        ownerType: MilestoneOwnerType,
        ownerId: String,
        name: String,
        node: Node,
        reachedOn: LocalDate,
        text: String,
    ) = MilestoneCandidate(ownerType, ownerId, node.code, node.threshold, reachedOn,
        logicalKey(ownerType, ownerId, node.code), "$name · 长期里程碑", text)
}

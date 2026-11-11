package com.cooper.lithic.p2a2

import android.graphics.Bitmap
import android.graphics.Color
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryNames
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.media.CoverOwnerType
import com.cooper.lithic.data.media.CoverReadFailure
import com.cooper.lithic.data.media.CoverReadResult
import com.cooper.lithic.data.media.CoverReader
import com.cooper.lithic.data.media.CoverReference
import com.cooper.lithic.data.media.VerifiedCoverImage
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.data.repository.SearchItem
import com.cooper.lithic.data.repository.SearchNames
import com.cooper.lithic.data.repository.SearchStatus
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.report.CostTimelineEngine
import com.cooper.lithic.domain.report.LedgerSnapshot
import com.cooper.lithic.domain.report.OverviewCategory
import com.cooper.lithic.domain.report.OverviewEngine
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewSnapshot
import com.cooper.lithic.domain.report.OverviewSubscription
import com.cooper.lithic.domain.report.TimelineSampler
import com.cooper.lithic.domain.report.TotalCostEvent
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.feature.home.SearchUiState
import com.cooper.lithic.feature.project.ProjectDetailUiState
import com.cooper.lithic.feature.project.ProjectListUiState
import com.cooper.lithic.feature.review.TotalCostTrendUiState
import com.cooper.lithic.feature.subscription.RenewalHistoryUiState
import com.cooper.lithic.feature.subscription.SubscriptionDetailUiState
import com.cooper.lithic.feature.subscription.SubscriptionListUiState
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate

/** A fixed sample ledger for the isolated Activity. All facts and cover bytes are in memory. */
internal class P2A2Fixtures {
    val day: LocalDate = LocalDate.of(2026, 10, 26)
    private val created = Instant.parse("2026-06-28T00:00:00Z")
    private val landscapeId = id(4)
    private val portraitId = id(5)
    private val digital = category(1, CategoryType.PROJECT, "数码")
    private val daily = category(2, CategoryType.PROJECT, "日用")
    private val reading = category(3, CategoryType.SUBSCRIPTION, "阅读")

    private val reader = LongTermProjectEntity(
        id(6), "合成阅读器 Paper", digital.id, day.minusDays(120), coverAssetId = landscapeId,
        note = "演示用横向封面", createdAt = created, updatedAt = created,
    )
    private val bag = LongTermProjectEntity(
        id(7), "合成通勤包", daily.id, day.minusDays(59),
        note = "演示用无封面记录", createdAt = created, updatedAt = created,
    )
    private val readerEvents = listOf(
        ProjectMoneyEventEntity(id(8), reader.id, MoneyDirection.OUTLAY, 128_800, reader.startedOn,
            role = ProjectMoneyEventRole.INITIAL, operationId = id(9)),
        ProjectMoneyEventEntity(id(10), reader.id, MoneyDirection.RECOVERY, 8_800, day.minusDays(20),
            role = ProjectMoneyEventRole.REGULAR, operationId = id(11)),
    )
    private val bagEvents = listOf(
        ProjectMoneyEventEntity(id(12), bag.id, MoneyDirection.OUTLAY, 19_900, bag.startedOn,
            role = ProjectMoneyEventRole.INITIAL, operationId = id(13)),
    )
    private val readingService = SubscriptionEntity(
        id(14), "合成阅读服务", reading.id, coverAssetId = portraitId,
        note = "演示用竖向封面", createdAt = created, updatedAt = created,
    )
    private val paidFrom = day.withDayOfMonth(1)
    private val paidUntil = paidFrom.plusMonths(1)
    private val payment = SubscriptionRenewalEntity(
        id(15), readingService.id, paidFrom, paidFrom, paidUntil, paidUntil, 1_800,
        BillingCycle.MONTHLY, BillingCycle.MONTHLY.standardDays, id(16),
    )
    private val readerFacts = projectFacts(reader, readerEvents)
    private val bagFacts = projectFacts(bag, bagEvents)
    private val subscriptionFacts = SubscriptionSnapshot(Subscription(readingService.id), listOf(
        Renewal(Money(payment.amountCents), payment.cycle, payment.paidOn, payment.effectiveFrom,
            payment.paidUntilExclusive, payment.reminderDueOn, payment.standardDays),
    ))
    private val ledger = LedgerSnapshot(
        overview = OverviewSnapshot(
            listOf(
                OverviewCategory(digital.id, digital.name, OverviewKind.PROJECT),
                OverviewCategory(daily.id, daily.name, OverviewKind.PROJECT),
                OverviewCategory(reading.id, reading.name, OverviewKind.SUBSCRIPTION),
            ),
            listOf(
                OverviewProject(reader.id, digital.id, readerFacts),
                OverviewProject(bag.id, daily.id, bagFacts),
            ),
            listOf(OverviewSubscription(readingService.id, reading.id, subscriptionFacts)),
        ),
        events = projectTimeline(reader, readerEvents) + projectTimeline(bag, bagEvents) + TotalCostEvent(
            payment.id, payment.paidOn, OverviewKind.SUBSCRIPTION, readingService.id, readingService.name,
            "订阅付款", "月付", payment.amountCents,
        ),
    )

    val overview = OverviewEngine.evaluate(ledger.overview, day)
    val projects = ProjectListUiState(items = listOf(
        ProjectDetail(reader, digital, readerEvents, ProjectCostEngine.evaluate(readerFacts, day), day),
        ProjectDetail(bag, daily, bagEvents, ProjectCostEngine.evaluate(bagFacts, day), day),
    ), totalCount = overview.projectCount, loading = false)
    val subscriptions = SubscriptionListUiState(items = listOf(
        SubscriptionDetail(readingService, reading, listOf(payment), emptyList(),
            SubscriptionCostEngine.evaluate(subscriptionFacts, day), payment, day),
    ), totalCount = overview.subscriptionCount, loading = false)

    private val timeline by lazy { TimelineSampler.sample(ledger, day) }
    private val searchItems = projects.items.map {
        SearchItem(CategoryType.PROJECT, it.project.id, it.project.name, it.category.name,
            SearchStatus.PROJECT_CURRENT, isHistory = false)
    } + subscriptions.items.map {
        SearchItem(CategoryType.SUBSCRIPTION, it.subscription.id, it.subscription.name, it.category.name,
            SearchStatus.ACTIVE, isHistory = false)
    }

    val coverReader: CoverReader = object : CoverReader {
        override val generationId = "p2a2-synthetic"
        override val ownerKey = Any()
        private val landscape = CoverReference(CoverOwnerType.PROJECT, reader.id, landscapeId)
        private val portrait = CoverReference(CoverOwnerType.SUBSCRIPTION, readingService.id, portraitId)
        private val noCover = CoverReference(CoverOwnerType.PROJECT, bag.id, null)
        private val landscapeImage by lazy { syntheticPng(landscapeId, 96, 48) }
        private val portraitImage by lazy { syntheticPng(portraitId, 48, 96) }

        override fun isCurrent() = true

        override suspend fun read(reference: CoverReference): CoverReadResult = when (reference) {
            landscape -> CoverReadResult.Ready(landscapeImage)
            portrait -> CoverReadResult.Ready(portraitImage)
            noCover -> CoverReadResult.NoCover
            else -> CoverReadResult.Unavailable(CoverReadFailure.RECORD_MISMATCH)
        }
    }

    fun project(id: String): ProjectDetailUiState {
        val detail = projects.items.find { it.project.id == id }
        return ProjectDetailUiState(detail = detail, loading = false, missing = detail == null)
    }

    fun subscription(id: String): SubscriptionDetailUiState {
        val detail = subscriptions.items.find { it.subscription.id == id }
        val renewals = detail?.renewals.orEmpty()
        return SubscriptionDetailUiState(
            detail = detail,
            history = RenewalHistoryUiState(items = renewals, totalCount = renewals.size.toLong(), loading = false),
            loading = false,
            missing = detail == null,
        )
    }

    fun search(query: String): SearchUiState {
        val term = try {
            SearchNames.query(query)
        } catch (_: IllegalArgumentException) {
            return SearchUiState(query = query, error = "名称搜索最多 ${SearchNames.MAX_QUERY_LENGTH} 字，不能包含控制字符。")
        }
        if (term.isEmpty()) return SearchUiState(query = query)
        val matches = searchItems.filter { it.name.contains(term, ignoreCase = true) }
        return SearchUiState(query = query, items = matches, totalCount = matches.size.toLong(), asOf = day)
    }

    fun trend(date: LocalDate = day, showBreakdown: Boolean = false): TotalCostTrendUiState = TotalCostTrendUiState(
        snapshot = ledger,
        timeline = timeline,
        selectedDate = date,
        selectedPoint = CostTimelineEngine.point(ledger, date),
        events = timeline.events.filter { it.date == date },
        showBreakdown = showBreakdown,
    )

    private fun category(value: Int, type: CategoryType, name: String) =
        CategoryEntity(id(value), type, name, CategoryNames.normalized(name), isFallback = false)

    private fun id(value: Int): String = "b2a20000-0000-4000-8000-${value.toString().padStart(12, '0')}"

    private fun projectFacts(project: LongTermProjectEntity, events: List<ProjectMoneyEventEntity>) =
        ProjectSnapshot(LongTermProject(project.startedOn), events.map {
            ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn)
        })

    private fun projectTimeline(project: LongTermProjectEntity, events: List<ProjectMoneyEventEntity>) =
        events.map {
            val title = if (it.direction == MoneyDirection.OUTLAY) "项目投入" else "项目收回"
            TotalCostEvent(it.id, it.occurredOn, OverviewKind.PROJECT, project.id, project.name,
                title, "合成记录", it.amountCents)
        }

    /** Small graphic with asymmetric edges so Fit and Crop are distinguishable on the device. */
    private fun syntheticPng(assetId: String, width: Int, height: Int): VerifiedCoverImage {
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            when {
                x < width / 6 -> Color.rgb(75, 104, 124)
                y < height / 6 -> Color.rgb(193, 154, 97)
                x > width / 3 && x < width * 5 / 6 && y > height / 3 && y < height * 5 / 6 ->
                    Color.rgb(220, 222, 212)
                else -> Color.rgb(130, 147, 139)
            }
        }
        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val bytes = try {
            ByteArrayOutputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
        return VerifiedCoverImage(assetId, "image/png", width, height, bytes)
    }
}

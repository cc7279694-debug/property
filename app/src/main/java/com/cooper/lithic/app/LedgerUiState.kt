package com.cooper.lithic.app

import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.data.repository.ReportRepository
import com.cooper.lithic.data.repository.CategoryRepository
import com.cooper.lithic.data.repository.RankingRepository
import com.cooper.lithic.data.repository.SearchRepository
import com.cooper.lithic.data.repository.ReplacementRepository
import com.cooper.lithic.domain.report.OverviewResult
import java.time.ZoneId

/** Derived read state; database owners never enter saved navigation state. */
sealed interface LedgerUiState {
    data object Initializing : LedgerUiState
    data class Ready(
        val projectCount: Long,
        val recordZone: ZoneId,
        val repository: ProjectRepository,
        val businessClock: BusinessClock,
        val subscriptionRepository: SubscriptionRepository,
        val subscriptionCount: Long,
        val readError: String? = null,
        val generationId: String = "test-generation",
        val overview: OverviewResult? = null,
        val reportRepository: ReportRepository? = null,
        val categoryRepository: CategoryRepository? = null,
        val searchRepository: SearchRepository? = null,
        val rankingRepository: RankingRepository? = null,
        val replacementRepository: ReplacementRepository? = null,
        val itemTimelineRepository: com.cooper.lithic.data.repository.ItemTimelineRepository? = null,
        val totalCostTimelineRepository: com.cooper.lithic.data.repository.TotalCostTimelineRepository? = null,
        val settingsStore: com.cooper.lithic.data.settings.SettingsStore? = null,
    ) : LedgerUiState
    data object Failed : LedgerUiState
}

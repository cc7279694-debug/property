package com.cooper.lithic.app

import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.data.repository.SubscriptionRepository
import java.time.LocalDate
import java.time.ZoneId

/** A read-only window, separate from the counts of every recorded identity. */
data class HomePreview(
    val projects: List<ProjectDetail>,
    val subscriptions: List<SubscriptionDetail>,
    val currentProjectCount: Long,
    val managedSubscriptionCount: Long,
    val asOf: LocalDate,
)

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
        val mediaRepository: MediaRepository? = null,
        val homePreview: HomePreview? = null,
        val isRefreshing: Boolean = false,
    ) : LedgerUiState
    data object Failed : LedgerUiState
}

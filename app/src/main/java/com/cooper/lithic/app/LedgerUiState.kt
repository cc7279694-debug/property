package com.cooper.lithic.app

import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionRepository
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
        val mediaRepository: MediaRepository? = null,
    ) : LedgerUiState
    data object Failed : LedgerUiState
}

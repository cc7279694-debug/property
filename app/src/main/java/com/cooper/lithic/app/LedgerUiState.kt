package com.cooper.lithic.app

import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.repository.ProjectRepository
import java.time.ZoneId

/** Derived read state; database owners never enter saved navigation state. */
sealed interface LedgerUiState {
    data object Initializing : LedgerUiState
    data class Ready(
        val projectCount: Long,
        val recordZone: ZoneId,
        val repository: ProjectRepository,
        val businessClock: BusinessClock,
        val readError: String? = null,
    ) : LedgerUiState
    data object Failed : LedgerUiState
}

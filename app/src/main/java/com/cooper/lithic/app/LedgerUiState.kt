package com.cooper.lithic.app

import com.cooper.lithic.data.repository.LedgerSnapshot
import java.time.ZoneId

/** Read-only UI results; generation/database owners never enter saved navigation state. */
sealed interface LedgerUiState {
    data object Initializing : LedgerUiState
    data class Ready(val snapshot: LedgerSnapshot, val recordZone: ZoneId) : LedgerUiState
    data object Failed : LedgerUiState
}

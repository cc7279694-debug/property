package com.cooper.lithic.data.repository

import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.domain.report.LedgerSnapshot
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Read adapter over the already owned generation. No new owner, database or persistent cache. */
open class TotalCostTimelineRepository(
    private val reports: ReportRepository,
    private val clock: BusinessClock,
) {
    open suspend fun loadSnapshot(asOf: LocalDate = clock.today()): LedgerSnapshot = reports.loadLedgerSnapshot(asOf)
    open val changes: Flow<Unit> = reports.changes
}

package com.cooper.lithic.feature.home

import com.cooper.lithic.domain.report.OverviewResult
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Binds the presentation classifier to the frozen producer without duplicating its messages. */
class P2HomeReadSourceContractTest {
    @Test fun actual_ledger_failure_and_recovery_warning_sources_match_the_home_classifier() {
        val holder = source("app/LedgerUiStateHolder.kt")
        val failure = Regex("""private const val READ_ERROR\s*=\s*"([^"]+)"""")
            .find(holder)?.groupValues?.get(1) ?: error("Ledger failure source changed; review the home classifier")
        val separator = Regex("""private fun readFailureMessage\(\)\s*=\s*listOfNotNull\(recoveryWarning, READ_ERROR\)\.joinToString\("([^"]*)"\)""")
            .find(holder)?.groupValues?.get(1) ?: error("Ledger failure assembly changed; review the home classifier")
        assertEquals("Both existing failure paths use the same producer", 2,
            Regex("""readError\s*=\s*readFailureMessage\(\)""").findAll(holder).count())

        val warnings = Regex("""lastRecoveryWarning\s*=\s*"([^"]+)"""")
            .findAll(source("data/backup/DataGenerationStore.kt")).map { it.groupValues[1] }.toList()
        assertTrue("Read current recovery warning sources", warnings.isNotEmpty())
        val snapshot = OverviewResult(LocalDate.of(2026, 10, 26), BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, 0L, 0L, 0L, 0L, emptyList(), emptyList())
        assertEquals(HomeOverviewReadStatus.FAILED, homeOverviewReadStatus(null, true, failure))
        assertEquals(HomeOverviewReadStatus.RETAINED, homeOverviewReadStatus(snapshot, true, failure))
        for (warning in warnings) {
            assertEquals(warning, HomeOverviewReadStatus.UNCONFIRMED, homeOverviewReadStatus(snapshot, true, warning))
            assertEquals(warning, HomeOverviewReadStatus.WAITING, homeOverviewReadStatus(null, true, warning))
            val combined = listOf(warning, failure).joinToString(separator)
            assertEquals(warning, HomeOverviewReadStatus.RETAINED, homeOverviewReadStatus(snapshot, true, combined))
            assertEquals(warning, HomeOverviewReadStatus.FAILED, homeOverviewReadStatus(null, true, combined))
        }
    }

    private fun source(path: String): String {
        val relative = "src/main/java/com/cooper/lithic/$path"
        val file = listOf(File(relative), File("app", relative)).firstOrNull(File::isFile)
        return checkNotNull(file) { "Cannot locate production source $relative" }.readText()
    }
}

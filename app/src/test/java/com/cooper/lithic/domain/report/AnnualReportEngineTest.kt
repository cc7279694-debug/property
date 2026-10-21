package com.cooper.lithic.domain.report

import com.cooper.lithic.core.model.Money
import com.cooper.lithic.domain.report.FinancialFactKind.*
import com.cooper.lithic.domain.report.ReportEngineFixtures.pc
import com.cooper.lithic.domain.report.ReportEngineFixtures.sc
import com.cooper.lithic.domain.report.ReportEngineFixtures.today
import com.cooper.lithic.domain.report.ReportEngineFixtures.fact
import com.cooper.lithic.domain.report.ReportEngineFixtures.ledger
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AnnualReportEngineTest {
    @Test fun money_year_is_occurred_year() {
        val input = ledger(listOf(fact("buy", 800_000), fact("repair", 80_000, 2028)))
        assertEquals(800_000, AnnualReportEngine.evaluate(input, 2026, today).amounts.projectOutlayCents)
        assertEquals(80_000, AnnualReportEngine.evaluate(input, 2028, today).amounts.projectOutlayCents)
        assertFalse(AnnualReportEngine.evaluate(input, 2027, today).amounts.hasRecords)
    }

    @Test fun outlay_recovery_net() {
        val input = ledger(listOf(fact("buy", 1_300_000), fact("refund", 350_000, kind = PROJECT_RECOVERY)))
        val actual = AnnualReportEngine.evaluate(input, 2026, today).amounts
        assertEquals(1_300_000, actual.projectOutlayCents)
        assertEquals(350_000, actual.projectRecoveryCents)
        assertEquals(950_000, actual.projectNetCents)
        assertEquals(0, actual.subscriptionPaidCents)
    }

    @Test fun subscription_year_is_paid_year() {
        val input = ledger(listOf(fact("paid", 365_000, kind = SUBSCRIPTION_PAYMENT)))
        assertEquals(365_000, AnnualReportEngine.evaluate(input, 2026, today).amounts.subscriptionPaidCents)
        assertFalse(AnnualReportEngine.evaluate(input, 2027, today).amounts.hasRecords)
        assertEquals(listOf(2026), AnnualReportEngine.years(input, today))
    }

    @Test fun types_not_merged_into_cash() {
        val input = ledger(listOf(fact("old-buy", 400_000, record = "old"),
            fact("credit-recovery", 300_000, 2027, PROJECT_RECOVERY, "old", true, "group"),
            fact("new-cash", 500_000, 2027, PROJECT_OUTLAY, "new", false, "group"),
            fact("credit-outlay", 300_000, 2027, PROJECT_OUTLAY, "new", true, "group"),
            fact("paid", 36_500, 2027, SUBSCRIPTION_PAYMENT)))
        val actual = AnnualReportEngine.evaluate(input, 2027, today).amounts
        assertEquals(800_000, actual.projectOutlayCents)
        assertEquals(300_000, actual.projectRecoveryCents)
        assertEquals(500_000, actual.projectNetCents)
        assertEquals(36_500, actual.subscriptionPaidCents)
        assertTrue(actual.containsNonCash)
        assertEquals(3, actual.projectFactCount)
    }

    @Test fun annual_summary_no_judgement_zero_negative_and_empty_are_distinct() {
        val zero = AnnualReportEngine.evaluate(ledger(listOf(fact("zero", 0))), 2026, today)
        assertTrue(zero.amounts.hasRecords); assertEquals(0, zero.amounts.projectNetCents)
        val negative = AnnualReportEngine.evaluate(ledger(listOf(fact("buy", 100),
            fact("refund", 300, kind = PROJECT_RECOVERY))), 2026, today)
        assertEquals(-200, negative.amounts.projectNetCents)
        assertFalse(AnnualReportEngine.evaluate(ledger(emptyList()), 2026, today).amounts.hasRecords)
        assertEquals(setOf("year", "amounts", "categories", "records"), AnnualReport::class.java.declaredFields
            .filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) }.map { it.name }.toSet())
    }

    @Test fun post_end_settlement_and_correction_use_business_year_not_lifecycle_or_created_time() {
        val old = listOf(fact("buy", 800_000), fact("late", 80_000, 2028))
        val input = ledger(old, endByProject = mapOf("p" to LocalDate.of(2027, 1, 1)))
        assertEquals(80_000, AnnualReportEngine.evaluate(input, 2028, today).amounts.projectOutlayCents)
        assertEquals(ReportRecordStatus.ENDED, AnnualReportEngine.evaluate(input, 2028, today).records.single().status)
        val corrected = ledger(old.map { if (it.id == "late") it.copy(date = LocalDate.of(2027, 12, 15)) else it })
        assertFalse(AnnualReportEngine.evaluate(corrected, 2028, today).amounts.hasRecords)
        assertEquals(80_000, AnnualReportEngine.evaluate(corrected, 2027, today).amounts.projectOutlayCents)
    }

    @Test fun identical_legal_amount_dates_and_names_are_counted_as_distinct_facts() {
        val input = ledger(listOf(fact("one", 100), fact("two", 100)))
        val actual = AnnualReportEngine.evaluate(input, 2026, today)
        assertEquals(200, actual.amounts.projectOutlayCents); assertEquals(2, actual.amounts.projectFactCount)
    }

    @Test fun duplicate_missing_or_mismatched_typed_facts_are_rejected_not_inferred_from_display_text() {
        val input = ledger(listOf(fact("one", 100)))
        val invalid = listOf(input.copy(financialFacts = null), input.copy(recordIdentities = emptyList()),
            input.copy(financialFacts = input.financialFacts!! + input.financialFacts!!),
            input.copy(financialFacts = listOf(fact("one", 101))),
            input.copy(financialFacts = listOf(fact("one", 100, record = "absent"))))
        invalid.forEach { assertThrows(IllegalArgumentException::class.java) { AnnualReportEngine.evaluate(it, 2026, today) } }
    }

    @Test fun unknown_or_incomplete_trade_in_metadata_cannot_become_plain_cash() {
        val invalid = ledger(listOf(fact("credit", 100, nonCash = true, group = "group")))
        assertThrows(IllegalArgumentException::class.java) { AnnualReportEngine.evaluate(invalid, 2026, today) }
        val paid = ledger(listOf(fact("paid", 100, kind = SUBSCRIPTION_PAYMENT, nonCash = true, group = "bad")))
        assertThrows(IllegalArgumentException::class.java) { AnnualReportEngine.evaluate(paid, 2026, today) }
    }

    @Test fun as_of_cuts_off_future_facts_and_future_paid_years_without_mutating_input() {
        val facts = listOf(fact("now", 100), fact("later", 200, 2028))
        val input = ledger(facts)
        assertFalse(AnnualReportEngine.evaluate(input, 2028, LocalDate.of(2026, 12, 31)).amounts.hasRecords)
        assertEquals(listOf(2026), AnnualReportEngine.years(input, LocalDate.of(2026, 12, 31)))
        assertEquals(facts, input.financialFacts)
    }

    @Test fun actual_accumulator_checks_each_component_count_and_preserves_old_value_on_overflow() {
        val amount = ReportAmounts(projectOutlayCents = Long.MAX_VALUE, projectFactCount = 1)
        assertThrows(ArithmeticException::class.java) { addFinancialFact(amount, fact("next", 1)) }
        assertEquals(Long.MAX_VALUE, amount.projectOutlayCents)
        assertThrows(ArithmeticException::class.java) { addFinancialFact(ReportAmounts(projectRecoveryCents = Long.MAX_VALUE), fact("refund", 1, kind = PROJECT_RECOVERY)) }
        assertThrows(ArithmeticException::class.java) { addFinancialFact(ReportAmounts(subscriptionPaidCents = Long.MAX_VALUE), fact("paid", 1, kind = SUBSCRIPTION_PAYMENT)) }
        assertThrows(ArithmeticException::class.java) { addFinancialFact(ReportAmounts(projectFactCount = Long.MAX_VALUE), fact("zero", 0)) }
        assertThrows(ArithmeticException::class.java) { addFinancialFact(ReportAmounts(subscriptionPaymentCount = Long.MAX_VALUE), fact("paid", 1, kind = SUBSCRIPTION_PAYMENT)) }
    }

    @Test fun single_fact_money_limit_and_invalid_year_are_rejected() {
        assertThrows(IllegalArgumentException::class.java) { addFinancialFact(ReportAmounts(), fact("large", Money.MAX_CENTS + 1)) }
        assertThrows(IllegalArgumentException::class.java) { AnnualReportEngine.evaluate(ledger(emptyList()), 1_000_000_000, today) }
    }
}

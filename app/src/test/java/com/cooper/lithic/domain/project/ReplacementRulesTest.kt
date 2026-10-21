package com.cooper.lithic.domain.project

import org.junit.Assert.*
import org.junit.Test

class ReplacementRulesTest {
    @Test fun linear_chain_accepts_one_predecessor_and_successor() {
        assertNull(ReplacementRules.validate(setOf("a", "b", "c"), listOf(ReplacementEdge("a", "b"), ReplacementEdge("b", "c"))))
    }
    @Test fun self_reference_is_rejected() {
        assertNotNull(ReplacementRules.validate(setOf("a"), listOf(ReplacementEdge("a", "a"))))
    }
    @Test fun cycle_is_rejected() {
        assertNotNull(ReplacementRules.validate(setOf("a", "b", "c"), listOf(ReplacementEdge("a", "b"), ReplacementEdge("b", "c"), ReplacementEdge("c", "a"))))
    }
    @Test fun two_successors_are_rejected() {
        assertNotNull(ReplacementRules.validate(setOf("a", "b", "c"), listOf(ReplacementEdge("a", "b"), ReplacementEdge("a", "c"))))
    }
    @Test fun two_predecessors_are_rejected() {
        assertNotNull(ReplacementRules.validate(setOf("a", "b", "c"), listOf(ReplacementEdge("a", "c"), ReplacementEdge("b", "c"))))
    }
    @Test fun absent_project_is_rejected() {
        assertNotNull(ReplacementRules.validate(setOf("a"), listOf(ReplacementEdge("a", "b"))))
    }
    @Test fun duplicate_edge_is_rejected() {
        assertNotNull(ReplacementRules.validate(setOf("a", "b"), listOf(ReplacementEdge("a", "b"), ReplacementEdge("a", "b"))))
    }
    @Test fun disconnected_legal_chains_remain_independent() {
        assertNull(ReplacementRules.validate(setOf("a", "b", "c", "d"), listOf(ReplacementEdge("a", "b"), ReplacementEdge("c", "d"))))
    }
    @Test fun candidate_link_cannot_reconnect_cycle() {
        assertNotNull(ReplacementRules.canLink(setOf("a", "b", "c"), listOf(ReplacementEdge("a", "b"), ReplacementEdge("b", "c")), "c", "a"))
    }
    @Test fun zero_cash_is_a_real_allowed_amount() {
        assertNull(ReplacementRules.validateTradeInAmounts(0, 300_000))
    }
    @Test fun negative_cash_and_out_of_range_credit_are_rejected() {
        assertNotNull(ReplacementRules.validateTradeInAmounts(-1, 1))
        assertNotNull(ReplacementRules.validateTradeInAmounts(1, com.cooper.lithic.core.model.Money.MAX_CENTS + 1))
    }
    @Test fun individual_amount_limits_are_preserved_without_clamping_the_combined_cost() {
        assertNull(ReplacementRules.validateTradeInAmounts(com.cooper.lithic.core.model.Money.MAX_CENTS, com.cooper.lithic.core.model.Money.MAX_CENTS))
    }
}

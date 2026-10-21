package com.cooper.lithic.domain.project

import com.cooper.lithic.core.model.Money

/** A directed replacement is separate from lifecycle and payment facts. */
data class ReplacementEdge(val predecessorId: String, val successorId: String)

object ReplacementRules {
    fun validate(projectIds: Set<String>, edges: List<ReplacementEdge>): String? {
        if (edges.any { it.predecessorId !in projectIds || it.successorId !in projectIds }) return "A replacement references a missing project"
        if (edges.any { it.predecessorId == it.successorId }) return "A project cannot replace itself"
        if (edges.map { it.predecessorId }.distinct().size != edges.size) return "A project already has a successor"
        if (edges.map { it.successorId }.distinct().size != edges.size) return "A project already has a predecessor"
        val successor = edges.associate { it.predecessorId to it.successorId }
        val complete = mutableSetOf<String>()
        for (start in successor.keys) {
            val path = mutableSetOf<String>()
            var current: String? = start
            while (current != null && current !in complete) {
                if (!path.add(current)) return "Replacement chains cannot contain a cycle"
                current = successor[current]
            }
            complete += path
        }
        return null
    }

    fun canLink(projectIds: Set<String>, edges: List<ReplacementEdge>, predecessorId: String, successorId: String): String? =
        validate(projectIds, edges + ReplacementEdge(predecessorId, successorId))

    fun validateTradeInAmounts(cashCents: Long, creditCents: Long): String? = try {
        Money(cashCents)
        Money(creditCents)
        // The combined acquisition is two facts, not a new single payment.
        Math.addExact(cashCents, creditCents)
        null
    } catch (_: IllegalArgumentException) {
        "Each actual amount must stay within the supported CNY range"
    } catch (_: ArithmeticException) {
        "The combined acquisition amount overflows"
    }
}

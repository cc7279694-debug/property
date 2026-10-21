package com.cooper.lithic.data.repository

import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.domain.project.ReplacementEdge
import com.cooper.lithic.domain.project.ReplacementRules

/** Caller owns the generation coordinator and Room transaction. Never acquires either again. */
object ReplacementStoredFacts {
    suspend fun requireValid(db: LithicDatabase) {
        val links = db.replacementDao().listReplacements()
        val projects = db.replacementDao().referencedProjects()
        val events = db.replacementDao().transactionEvents()
        check(links.map { it.id }.distinct().size == links.size) { "Replacement identities are duplicated" }
        links.forEach { link -> listOf(link.id, link.predecessorId, link.successorId).forEach(StoredValueValidation::requireUuid) }
        ReplacementRules.validate(projects.map { it.id }.toSet(), links.map { ReplacementEdge(it.predecessorId, it.successorId) })?.let { error(it) }
        TradeInFacts.validate(projects, events)?.let { error(it) }
    }
}

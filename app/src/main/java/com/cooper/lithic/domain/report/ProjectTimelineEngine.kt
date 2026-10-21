package com.cooper.lithic.domain.report

import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.project.ProjectCostEngine
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ProjectTimelineEngine {
    /** One immutable snapshot and cutoff; the frozen engine owns every exact monetary result. */
    fun build(input: ProjectSnapshot, asOf: LocalDate, maxDisplayPoints: Int = 240): ProjectTimeline {
        requireItemDisplayPointLimit(maxDisplayPoints)
        val atCutoff = point(input, asOf, asOf)
        val eventDays = input.moneyEvents.asSequence().map { it.occurredOn }
            .filter { !it.isAfter(asOf) }.distinct().sorted().toList()
        val started = input.project.startedOn
        if (started.isAfter(asOf)) {
            return ProjectTimeline(asOf, emptyList(), emptyList(), eventDays, maxDisplayPoints, sampled = false)
        }
        val ended = input.project.endedOn
        val lastHeld = minOf(asOf, ended ?: asOf)
        val holdingPoints = itemChartDates(started, lastHeld, eventDays, maxDisplayPoints).map { day ->
            if (day == asOf) atCutoff else point(input, asOf, day)
        }
        val postEndAdjustments = if (ended != null && !ended.isAfter(asOf)) {
            eventDays.filter { it.isAfter(ended) }.map { day ->
                if (day == asOf) atCutoff else point(input, asOf, day)
            }
        } else emptyList()
        return ProjectTimeline(
            asOf = asOf,
            holdingPoints = holdingPoints,
            postEndAdjustments = postEndAdjustments,
            eventDays = eventDays,
            displayPointLimit = maxDisplayPoints,
            sampled = ChronoUnit.DAYS.between(started, lastHeld) >= maxDisplayPoints.toLong(),
        )
    }

    /** Selection calls the original engine, including for an unsampled date or a pre-start event. */
    fun point(input: ProjectSnapshot, asOf: LocalDate, date: LocalDate): ProjectTimelinePoint {
        require(!date.isAfter(asOf)) { "An item history cannot select a future date" }
        return ProjectTimelinePoint(date, ProjectCostEngine.evaluate(input, date))
    }
}

package com.cooper.lithic.data.repository

import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import java.time.Instant
import java.time.LocalDate

data class EndProjectCommand(
    val commandId: String,
    val expectedProject: LongTermProjectEntity,
    val endedOn: LocalDate,
    val endReason: String,
    val settlement: ProjectMoneyEventEntity?,
    val updatedAt: Instant,
)

data class UpdateEndCommand(
    val commandId: String,
    val expectedProject: LongTermProjectEntity,
    val expectedSettlement: ProjectMoneyEventEntity?,
    val endedOn: LocalDate,
    val endReason: String,
    val settlementAmountCents: Long?,
    val settlementOccurredOn: LocalDate?,
    val settlementDescription: String?,
    val updatedAt: Instant,
)

data class CorrectMoneyEventCommand(
    val commandId: String,
    val expectedProject: LongTermProjectEntity,
    val expectedEvent: ProjectMoneyEventEntity,
    val amountCents: Long,
    val occurredOn: LocalDate,
    val description: String?,
    val updatedAt: Instant,
)

data class DeleteProjectCommand(
    val commandId: String,
    val expectedProject: LongTermProjectEntity,
    val expectedEvents: List<ProjectMoneyEventEntity>,
)

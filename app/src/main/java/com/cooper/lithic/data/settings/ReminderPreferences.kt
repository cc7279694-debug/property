package com.cooper.lithic.data.settings

import java.time.LocalDate

data class ReminderPreferences(
    val renewalEnabled: Boolean = true,
    val milestoneEnabled: Boolean = true,
    val importCutoff: LocalDate? = null,
)

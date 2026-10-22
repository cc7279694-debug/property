package com.cooper.lithic.data.repository

import com.cooper.lithic.domain.reminder.RenewalReminderSource
import java.nio.ByteBuffer
import java.security.MessageDigest

/** Binds a queued task to exact confirmed facts; not a second persistent record. */
object ReminderTaskRevision {
    fun of(source: RenewalReminderSource): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun field(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array()); digest.update(bytes)
        }
        field(source.ownerId); field(source.name)
        source.renewals.sortedBy { it.id }.forEach { row ->
            field(row.id)
            with(row.renewal) {
                field(amount.cents.toString()); field(cycle.name); field(standardDays.toString())
                field(paidOn.toString()); field(effectiveFrom.toString()); field(paidUntilExclusive.toString()); field(reminderDueOn.toString())
            }
        }
        source.stateChanges.sortedWith(compareBy({ it.effectiveOn }, { it.state.name })).forEach {
            field(it.effectiveOn.toString()); field(it.state.name)
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}

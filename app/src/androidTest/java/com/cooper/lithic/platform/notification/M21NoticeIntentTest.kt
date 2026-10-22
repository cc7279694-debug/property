package com.cooper.lithic.platform.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.repository.MilestoneNoticeRef
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Intent codec only. Not a real system notification or delivery assertion. NOT_RUN. */
@RunWith(AndroidJUnit4::class)
class M21NoticeIntentTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun ref() = MilestoneNoticeRef(UUID.randomUUID().toString(), LocalDate.parse("2026-10-22"), "a".repeat(64), 76)
    @Test fun boundedReferenceRoundTripsWithoutSourceArraysOrAmounts() {
        val value = ref(); val intent = MilestoneNoticeIntents.create(context, value)
        assertEquals(value, MilestoneNoticeIntents.read(intent))
        assertEquals(setOf("generation", "day", "digest", "count"), intent.extras!!.keySet())
    }
    @Test fun mismatchedPathOrMalformedDigestDoesNotCreateAnOpenRequest() {
        val intent = MilestoneNoticeIntents.create(context, ref())
        intent.putExtra("generation", UUID.randomUUID().toString())
        assertNull(MilestoneNoticeIntents.read(intent))
        intent.putExtra("digest", "unknown"); assertNull(MilestoneNoticeIntents.read(intent))
    }
    @Test fun wrongActionAndUnboundedCountAreRejected() {
        val intent = MilestoneNoticeIntents.create(context, ref())
        intent.putExtra("count", Int.MAX_VALUE); assertNull(MilestoneNoticeIntents.read(intent))
        intent.action = "unknown"; assertNull(MilestoneNoticeIntents.read(intent))
    }
    @Test fun dateIsCanonicalAndCannotBeReplacedByAThresholdCode() {
        val intent = MilestoneNoticeIntents.create(context, ref())
        intent.putExtra("day", "D100"); assertNull(MilestoneNoticeIntents.read(intent))
    }
}

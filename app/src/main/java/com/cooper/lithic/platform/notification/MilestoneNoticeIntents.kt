package com.cooper.lithic.platform.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.cooper.lithic.app.MainActivity
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.repository.MilestoneNoticeRef
import java.time.LocalDate

/** No amount or unbounded source array in an externally reachable Activity intent. */
object MilestoneNoticeIntents {
    const val ACTION = "com.cooper.lithic.MILESTONE_SUMMARY"
    fun create(context: Context, ref: MilestoneNoticeRef): Intent {
        requireValid(ref)
        return Intent(context, MainActivity::class.java).setAction(ACTION)
            .setData(Uri.Builder().scheme("lithic").authority("milestone").appendPath(ref.generationId)
                .appendPath(ref.publicationOn.toString()).build())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("generation", ref.generationId).putExtra("day", ref.publicationOn.toString())
            .putExtra("digest", ref.digest).putExtra("count", ref.count)
    }
    fun read(intent: Intent?): MilestoneNoticeRef? {
        if (intent?.action != ACTION) return null
        return runCatching {
            val day = requireNotNull(intent.getStringExtra("day"))
            val ref = MilestoneNoticeRef(requireNotNull(intent.getStringExtra("generation")),
                LocalDate.parse(day), requireNotNull(intent.getStringExtra("digest")), intent.getIntExtra("count", 0))
            require(day == ref.publicationOn.toString()); requireValid(ref)
            val data = requireNotNull(intent.data)
            require(data.scheme == "lithic" && data.authority == "milestone" && data.query == null && data.fragment == null)
            require(data.pathSegments == listOf(ref.generationId, day))
            ref
        }.getOrNull()
    }
    private fun requireValid(ref: MilestoneNoticeRef) {
        StoredValueValidation.requireUuid(ref.generationId)
        require(ref.digest.matches(Regex("[0-9a-f]{64}")) && ref.count in 1..1_000_000)
    }
}

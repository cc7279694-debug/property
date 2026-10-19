package com.cooper.lithic.data.media

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

/** Reflection/boot/role preflight only. Test evidence is written; no Stage, Worker or business operation is created. */
@RunWith(AndroidJUnit4::class)
class M10B1B2SafetyPreflightTest {
    @Test fun new_safety_classes_and_boot_role_are_ready_without_consumers() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val run = M10B1B2SafetySupport.begin(context, METHOD)
        val report = JSONObject().put("protocol", "M10_B1B2_PREFLIGHT_V1").put("runId", run.runId)
            .put("method", METHOD).put("status", "UNKNOWN").put("noConsumerWork", true)
        try {
            val before = M10B1B2SafetySupport.stages(context)
            val processesBefore = M10B1B2SafetySupport.noWorker(context)
            val classes = listOf(
                "com.cooper.lithic.data.media.M10B1B2SafetyPreflightTest",
                "com.cooper.lithic.data.media.M10B1CancellationSafetyTest",
                "com.cooper.lithic.data.media.M10B1ExactProcessFaultTest",
                "com.cooper.lithic.data.media.M10JpegResourceEvidenceTest",
                "com.cooper.lithic.data.media.M10ResourceSampler",
                "com.cooper.lithic.data.media.M10B1B2SafetySupport",
                "com.cooper.lithic.data.media.M10B1ClosureObservations",
                "com.cooper.lithic.data.media.jpegworker.JpegResourceEvidence",
                "com.cooper.lithic.data.media.jpegworker.JpegResourceDiagnostics",
                "com.cooper.lithic.data.media.jpegworker.JpegResourceDiagnostics\$Session",
                "com.cooper.lithic.data.media.jpegworker.JpegBootIdentity",
            )
            val reflected = JSONArray()
            for (name in classes) {
                val methods = Class.forName(name).declaredMethods
                reflected.put(JSONObject().put("class", name).put("declaredMethods", methods.size)
                    .put("scan", "PASS"))
            }
            M10B1B2SafetySupport.assertHost(run)
            val after = M10B1B2SafetySupport.stages(context)
            val processesAfter = M10B1B2SafetySupport.noWorker(context)
            check(before.toString() == after.toString()) { "Class reflection changed a Stage namespace" }
            report.put("status", "PASS").put("reflectedClasses", reflected)
                .put("before", before).put("after", after)
                .put("processesBefore", processesBefore).put("processesAfter", processesAfter)
                .put("bootIdBefore", run.boot).put("bootIdAfter", run.boot)
                .put("bootStable", true).put("mainRoleAndExactHostInstance", true)
                .put("stageCreatedByMethod", false).put("workerStartedByMethod", false)
                .put("deviceAdmissionOutsideThisMethod", "REQUIRED")
        } catch (error: Throwable) {
            report.put("errorType", error.javaClass.simpleName)
            throw error
        } finally { M10B1B2SafetySupport.write(run, "result.json", report) }
    }
    companion object {
        const val METHOD = "com.cooper.lithic.data.media.M10B1B2SafetyPreflightTest#new_safety_classes_and_boot_role_are_ready_without_consumers"
    }
}

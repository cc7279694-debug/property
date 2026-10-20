package com.cooper.lithic.data.backup

import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.time.Instant
import java.time.LocalDate
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDtosTest {
    private val projectCategory = CategoryEntity(id(1), CategoryType.PROJECT, "数码", "数码", false)
    private val subscriptionCategory = CategoryEntity(id(2), CategoryType.SUBSCRIPTION, "服务", "服务", true)
    private val at = Instant.parse("2026-01-01T02:03:04.123456789Z")
    private val project = LongTermProjectEntity(
        id(3), "相机 📷", projectCategory.id, LocalDate.parse("2020-02-29"),
        LocalDate.parse("2026-01-01"), "已卖出", id(4), "引号\"与换行\n", at, at.plusSeconds(1),
    )
    private val media = LocalMediaAssetEntity(id(4), "media/${id(4)}.jpg", "image/jpeg", 1600, 900, 42, "a".repeat(64))
    private val subscription = SubscriptionEntity(id(5), "音乐", subscriptionCategory.id, null, null, at, at)

    @Test fun all_seven_tables_preserve_every_stored_field_and_canonical_date_string() {
        val renewal = SubscriptionRenewalEntity(
            id(6), subscription.id, LocalDate.parse("2026-01-20"), LocalDate.parse("2026-02-01"),
            LocalDate.parse("2026-03-01"), LocalDate.parse("2026-02-27"), 18_000,
            BillingCycle.MONTHLY, 30, id(7),
        )
        val event = ProjectMoneyEventEntity(id(8), project.id, MoneyDirection.RECOVERY, 500_000,
            LocalDate.parse("2026-01-02"), "真实收回", ProjectMoneyEventRole.END_SETTLEMENT, id(9))
        val state = SubscriptionStateChangeEntity(id(10), subscription.id, LocalDate.parse("2026-02-15"),
            SubscriptionOperationState.PAUSED, "暂时暂停", "保留状态历史", id(11))
        val data = map(events = listOf(event), renewals = listOf(renewal), states = listOf(state))
        val root = Json.parseToJsonElement(BackupJson.encodeData(data).decodeToString()).jsonObject
        assertEquals(setOf("categories", "projects", "projectMoneyEvents", "mediaAssets", "subscriptions", "subscriptionRenewals", "subscriptionStateChanges"), root.keys)
        val p = root.getValue("projects").jsonArray.single().jsonObject
        assertEquals(setOf("id", "name", "categoryId", "startedOn", "endedOn", "endReason", "coverAssetId", "note", "createdAt", "updatedAt"), p.keys)
        assertEquals("相机 📷", p.getValue("name").jsonPrimitive.content)
        assertEquals("2020-02-29", p.getValue("startedOn").jsonPrimitive.content)
        assertEquals(at.toString(), p.getValue("createdAt").jsonPrimitive.content)
        val r = root.getValue("subscriptionRenewals").jsonArray.single().jsonObject
        assertEquals(setOf("id", "subscriptionId", "paidOn", "effectiveFrom", "paidUntilExclusive", "reminderDueOn", "amountCents", "cycle", "standardDays", "operationId"), r.keys)
        assertEquals("2026-01-20", r.getValue("paidOn").jsonPrimitive.content)
        assertEquals("2026-02-01", r.getValue("effectiveFrom").jsonPrimitive.content)
        assertEquals("2026-03-01", r.getValue("paidUntilExclusive").jsonPrimitive.content)
        assertEquals("2026-02-27", r.getValue("reminderDueOn").jsonPrimitive.content)
        assertEquals("MONTHLY", r.getValue("cycle").jsonPrimitive.content)
        assertEquals("PAUSED", root.getValue("subscriptionStateChanges").jsonArray.single().jsonObject.getValue("state").jsonPrimitive.content)
        assertEquals("RECOVERY", root.getValue("projectMoneyEvents").jsonArray.single().jsonObject.getValue("direction").jsonPrimitive.content)
        assertEquals("END_SETTLEMENT", root.getValue("projectMoneyEvents").jsonArray.single().jsonObject.getValue("role").jsonPrimitive.content)
        assertEquals("media/${id(4)}.jpg", root.getValue("mediaAssets").jsonArray.single().jsonObject.getValue("relativePath").jsonPrimitive.content)
        assertEquals(mapOf("categories" to 2L, "long_term_projects" to 1L, "project_money_events" to 1L,
            "local_media_assets" to 1L, "subscriptions" to 1L, "subscription_renewals" to 1L,
            "subscription_state_changes" to 1L), data.recordCounts())
    }

    @Test fun integer_cents_are_serialized_exactly_without_floating_point_or_netting_recovery() {
        val input = listOf(
            ProjectMoneyEventEntity(id(12), project.id, MoneyDirection.OUTLAY, Long.MAX_VALUE,
                project.startedOn, null, ProjectMoneyEventRole.REGULAR, id(13)),
            ProjectMoneyEventEntity(id(14), project.id, MoneyDirection.RECOVERY, Long.MAX_VALUE,
                project.startedOn, null, ProjectMoneyEventRole.REGULAR, id(15)),
        )
        val json = BackupJson.encodeData(map(events = input)).decodeToString()
        val events = Json.parseToJsonElement(json).jsonObject.getValue("projectMoneyEvents").jsonArray
        assertEquals(listOf("9223372036854775807", "9223372036854775807"), events.map { it.jsonObject.getValue("amountCents").toString() })
        assertFalse(json.contains("dailyCost"))
        assertFalse(json.contains("netCost"))
    }

    @Test fun nulls_unicode_and_newlines_survive_utf8_without_android_or_class_discriminators() {
        val bytes = BackupJson.encodeData(map())
        val decoded = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
        val s = decoded.getValue("subscriptions").jsonArray.single().jsonObject
        assertEquals("null", s.getValue("coverAssetId").toString())
        assertEquals("null", s.getValue("note").toString())
        assertEquals(project.note, decoded.getValue("projects").jsonArray.single().jsonObject.getValue("note").jsonPrimitive.content)
        assertFalse(bytes.decodeToString().contains("com.cooper"))
        assertFalse(bytes.decodeToString().contains("/data/user"))
    }

    @Test fun all_tables_are_sorted_by_stable_id_independently_of_query_order() {
        val first = map()
        val reversed = first.copy(categories = first.categories.reversed())
        assertTrue(BackupJson.encodeData(first).contentEquals(BackupJson.encodeData(reversed)))
        assertEquals(listOf(id(1), id(2)), first.categories.map { it.id })
        val empty = map().copy(categories = emptyList(), projects = emptyList(), mediaAssets = emptyList(), subscriptions = emptyList())
        assertEquals(0L, empty.recordCounts().values.sum())
    }

    @Test fun settings_export_whitelist_contains_only_record_zone_not_permissions_caches_or_tasks() {
        val settings = BackupSettingsV1("Asia/Shanghai")
        assertEquals("{\"recordZoneId\":\"Asia/Shanghai\"}", BackupJson.encodeSettings(settings).decodeToString())
    }

    @Test fun media_dto_matches_frozen_media_path_rules_and_preserves_safe_existing_names() {
        val valid = map().mediaAssets.single()
        listOf("/data/user/0/a.jpg", "content://image/1", "media/../secret", "media\\${id(4)}.jpg",
            "media//a.jpg", "media/./a.jpg", "media/").forEach { path ->
            expectFailure { valid.copy(relativePath = path) }
        }
        listOf("media/a.jpg", "media/subdir/${id(4)}.jpg").forEach { path ->
            assertEquals(path, valid.copy(relativePath = path).relativePath)
        }
    }

    @Test fun streaming_encoder_matches_bytes_and_propagates_output_failure() {
        val data = map()
        val output = ByteArrayOutputStream()
        BackupJson.writeData(data, output)
        assertTrue(BackupJson.encodeData(data).contentEquals(output.toByteArray()))
        val settingsOutput = ByteArrayOutputStream()
        BackupJson.writeSettings(BackupSettingsV1("Asia/Shanghai"), settingsOutput)
        assertTrue(BackupJson.encodeSettings(BackupSettingsV1("Asia/Shanghai")).contentEquals(settingsOutput.toByteArray()))
        val unavailable = object : OutputStream() {
            override fun write(b: Int) { throw IOException("synthetic output full") }
        }
        try { BackupJson.writeData(data, unavailable); throw AssertionError("Output failure was swallowed") }
        catch (expected: IOException) { assertEquals("synthetic output full", expected.message) }
    }

    private fun map(
        events: List<ProjectMoneyEventEntity> = emptyList(),
        renewals: List<SubscriptionRenewalEntity> = emptyList(),
        states: List<SubscriptionStateChangeEntity> = emptyList(),
    ) = BackupDtoMapper.fromRows(listOf(subscriptionCategory, projectCategory), listOf(project), events,
        listOf(media), listOf(subscription), renewals, states)

    private fun expectFailure(block: () -> Unit) {
        try { block(); throw AssertionError("Expected invalid metadata rejection") }
        catch (_: IllegalArgumentException) { }
    }

    private fun id(value: Int) = "00000000-0000-4000-8000-${value.toString().padStart(12, '0')}"
}

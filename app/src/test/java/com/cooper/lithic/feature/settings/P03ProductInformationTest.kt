package com.cooper.lithic.feature.settings

import org.junit.Assert.*
import org.junit.Test

class P03ProductInformationTest {
    @Test fun project_explanation_preserves_true_cash_and_inclusive_frozen_days() {
        val copy = calculationExplanation.first().paragraphs.joinToString()
        listOf("截至观察日净投入", "真实投入与收回", "开始日计入第1天", "结束日也计入", "持有时间冻结", "后续付款或退款", "负净投入").forEach { assertTrue(it, copy.contains(it)) }
    }
    @Test fun subscription_copy_distinguishes_paid_normalized_time_and_effective_coverage() {
        val copy = calculationExplanation[1].paragraphs.joinToString()
        listOf("30、90、365", "不等于实际已经使用", "预付未来周期不等于当前已经生效", "确认真实付款", "历史平均日成本").forEach { assertTrue(it, copy.contains(it)) }
    }
    @Test fun home_explanation_does_not_promise_daily_cash_or_all_living_expenses() {
        val copy = calculationExplanation[2].paragraphs.joinToString()
        listOf("只统计已录入", "不等于今天实际扣款", "不代表全部生活消费", "展示时才舍入").forEach { assertTrue(it, copy.contains(it)) }
    }
    @Test fun safety_copy_has_manual_unencrypted_backup_and_no_delivery_guarantee() {
        val copy = productSafetyExplanation.flatMap { it.paragraphs }.joinToString()
        listOf("卸载", "未加密", "没有自动云同步", "不保证准点送达", "不会自动取消服务商扣费", "不代表已经完成严格JPEG").forEach { assertTrue(it, copy.contains(it)) }
        listOf("可丢弃数据", "唯一副本").forEach { assertTrue(it, copy.contains(it)) }
    }
    @Test fun version_label_uses_actual_supplied_version_and_long_code() {
        val info = InstalledAppInformation("砺石", "2.7.1", 8_000_000_042L)
        assertEquals("2.7.1（版本Code 8000000042）", info.versionLabel)
    }
    @Test fun unavailable_or_incomplete_version_does_not_invent_code17() {
        listOf(InstalledAppInformation(), InstalledAppInformation(versionName = "x"),
            InstalledAppInformation(versionCode = 99), InstalledAppInformation(versionName = " ", versionCode = 99),
            InstalledAppInformation(versionName = "x", versionCode = -1)).forEach {
            assertEquals("版本信息暂无法读取", it.versionLabel)
            assertFalse(it.versionLabel.contains("17"))
        }
    }
    @Test fun private_trial_is_only_identified_by_exact_own_profile_and_debug_flag() {
        assertTrue(InstalledAppInformation(packageName = "com.cooper.lithic.preview.debug", debuggable = true).buildLabel.contains("个人体验版"))
        assertFalse(InstalledAppInformation(packageName = "com.cooper.lithic.preview.debug", debuggable = true).buildLabel.contains("签名"))
        assertFalse(InstalledAppInformation(packageName = "other.preview.debug", debuggable = true).buildLabel.contains("个人体验版"))
        assertFalse(InstalledAppInformation(packageName = "com.cooper.lithic.preview.debug", debuggable = false).buildLabel.contains("个人体验版"))
    }
    @Test fun debug_release_and_unknown_builds_cannot_claim_formal_acceptance() {
        assertEquals("构建身份暂无法确认", InstalledAppInformation().buildLabel)
        assertTrue(InstalledAppInformation(debuggable = true).buildLabel.contains("非正式公开发行"))
        assertTrue(InstalledAppInformation(debuggable = false).buildLabel.contains("不代表已通过"))
    }
}

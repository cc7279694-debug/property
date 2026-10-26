package com.cooper.lithic.feature.review

import com.cooper.lithic.domain.report.CostTimelinePoint

/** Labels only. Every count, zero and sign comes from the existing exact snapshot result. */
internal fun reviewSelectionLabel(point: CostTimelinePoint): String = when {
    !point.hasRecords -> "该日尚无已录入记录生效。"
    !point.hasActiveContributions -> "该日没有有效成本贡献，历史记录仍保留。"
    point.isCancellationZero -> "该日正负贡献相抵为0。"
    point.totalRate.signum() == 0 -> "该日有有效记录，真实日均成本为0。"
    else -> "该日有效贡献：${point.activeProjectCount}个项目、${point.activeSubscriptionCount}个订阅。"
}

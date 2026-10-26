package com.cooper.lithic.feature.project

import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventRole

/** Menu placement only. Eligibility and writes remain in the existing guarded routes. */
internal enum class ProjectDetailAction(val label: String, val tag: String) {
    EDIT("编辑项目资料", "project-edit"),
    END("结束项目", "project-end"),
    CORRECT_END("修正结束记录", "project-update-end"),
    REPLACEMENT("替换链与纵向对比", "project-replacement"),
    TRADE_IN("记录以旧换新", "project-trade-in"),
    COST_HISTORY("查看成本历史", "project-cost-history-menu"),
    DELETE("永久删除项目", "project-delete"),
}

internal data class ProjectDetailMenuEntry(val action: ProjectDetailAction, val enabled: Boolean = true)

internal fun projectDetailMenuEntries(
    hasEnd: Boolean,
    hasExchangeGroup: Boolean,
    hasReplacementRoute: Boolean,
    hasTradeInRoute: Boolean,
    hasCostHistoryRoute: Boolean = false,
): List<ProjectDetailMenuEntry> = buildList {
    add(ProjectDetailMenuEntry(ProjectDetailAction.EDIT))
    add(ProjectDetailMenuEntry(if (hasEnd) ProjectDetailAction.CORRECT_END else ProjectDetailAction.END))
    if (hasReplacementRoute) add(ProjectDetailMenuEntry(ProjectDetailAction.REPLACEMENT))
    if (!hasEnd && hasTradeInRoute) add(ProjectDetailMenuEntry(ProjectDetailAction.TRADE_IN))
    if (hasCostHistoryRoute) add(ProjectDetailMenuEntry(ProjectDetailAction.COST_HISTORY))
    add(ProjectDetailMenuEntry(ProjectDetailAction.DELETE, enabled = !hasExchangeGroup))
}

internal fun projectDetailRateLabel(hasEnd: Boolean, contributesToCurrent: Boolean): String =
    if (hasEnd && !contributesToCurrent) "截至观察日的日均成本" else "当前日均成本"

/** Event labels remain factual; they do not infer cash flow from the displayed amount. */
internal fun projectMoneyEventTypeLabel(direction: MoneyDirection,
    role: ProjectMoneyEventRole): String {
    val name = if (direction == MoneyDirection.OUTLAY) "投入" else "收回"
    return when (role) {
        ProjectMoneyEventRole.INITIAL -> "初始$name"
        ProjectMoneyEventRole.END_SETTLEMENT -> "结束关联$name"
        ProjectMoneyEventRole.REGULAR -> name
    }
}

internal fun projectMoneyEventBasisLabel(basis: MoneyBasis): String =
    if (basis == MoneyBasis.TRADE_IN_CREDIT) "非现金折抵" else "现金"

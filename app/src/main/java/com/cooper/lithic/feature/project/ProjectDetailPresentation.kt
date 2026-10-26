package com.cooper.lithic.feature.project

/** Menu placement only. Eligibility and writes remain in the existing guarded routes. */
internal enum class ProjectDetailAction(val label: String, val tag: String) {
    EDIT("编辑项目资料", "project-edit"),
    END("结束项目", "project-end"),
    CORRECT_END("修正结束记录", "project-update-end"),
    REPLACEMENT("替换链与纵向对比", "project-replacement"),
    TRADE_IN("记录以旧换新", "project-trade-in"),
    DELETE("永久删除项目", "project-delete"),
}

internal data class ProjectDetailMenuEntry(val action: ProjectDetailAction, val enabled: Boolean = true)

internal fun projectDetailMenuEntries(
    hasEnd: Boolean,
    hasExchangeGroup: Boolean,
    hasReplacementRoute: Boolean,
    hasTradeInRoute: Boolean,
): List<ProjectDetailMenuEntry> = buildList {
    add(ProjectDetailMenuEntry(ProjectDetailAction.EDIT))
    add(ProjectDetailMenuEntry(if (hasEnd) ProjectDetailAction.CORRECT_END else ProjectDetailAction.END))
    if (hasReplacementRoute) add(ProjectDetailMenuEntry(ProjectDetailAction.REPLACEMENT))
    if (!hasEnd && hasTradeInRoute) add(ProjectDetailMenuEntry(ProjectDetailAction.TRADE_IN))
    add(ProjectDetailMenuEntry(ProjectDetailAction.DELETE, enabled = !hasExchangeGroup))
}

internal fun projectDetailRateLabel(hasEnd: Boolean, contributesToCurrent: Boolean): String =
    if (hasEnd && !contributesToCurrent) "截至观察日的日均成本" else "当前日均成本"

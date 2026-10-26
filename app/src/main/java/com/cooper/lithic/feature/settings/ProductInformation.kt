package com.cooper.lithic.feature.settings

/** Explanatory copy of the frozen contract, never an alternative cost calculator. */
internal data class InformationSection(val title: String, val paragraphs: List<String>)

internal val calculationExplanation = listOf(
    InformationSection("长期项目", listOf(
        "日均成本 = 截至观察日净投入 ÷ 持有自然日数。净投入来自记录中的真实投入与收回。",
        "开始日计入第1天，结束日也计入持有天数。结束后持有时间冻结，但真实的后续付款或退款仍可改变截至观察日的净投入。",
        "尚未开始或没有有效持有天数时，不把无法计算的日均成本显示为0。0元和负净投入如实保留。",
    )),
    InformationSection("固定周期订阅", listOf(
        "月付、季付、年付分别按30、90、365天折算。折算付费时长不等于实际已经使用的时长。",
        "预付未来周期不等于当前已经生效；真实付款日、生效日与付费覆盖结束日分别记录。只有您确认真实付款，才新增续费记录。",
        "历史平均日成本与当前日均成本回答不同问题；暂停、到期或停止时，不能用当前贡献为0代替历史平均。",
    )),
    InformationSection("首页当前日均成本", listOf(
        "只统计已录入、在观察日当前有效的长期项目与订阅贡献。已结束项目、暂停或无有效付费覆盖的订阅不计入当前贡献。",
        "它不等于今天实际扣款，也不代表全部生活消费。",
        "金额由原计算引擎精确计算和汇总，展示时才舍入；展示小计的舍入差异不代表另有付款。日期按记录时区理解。",
    )),
)

internal val productSafetyExplanation = listOf(
    InformationSection("本机数据与手动备份", listOf(
        "砺石以本地为主（Local-first），记录和已登记图片主要保存在本机。不需要登录，没有自动云同步或自动云备份。",
        "卸载应用可能导致本地记录丢失。需要保留数据时，请手动导出完整备份并自行妥善保管。",
        "备份文件未加密，包含记录、设置和已登记图片；保存位置由您选择，请避免交给不可信的人。",
    )),
    InformationSection("提醒与服务商扣费", listOf(
        "续费和里程碑提醒受系统权限、后台限制及厂商策略影响，不保证准点送达。请自行留意重要日期。",
        "停止App中的订阅记录不会自动取消服务商扣费；如需取消自动续费，请向服务商操作。",
    )),
    InformationSection("当前使用边界", listOf(
        "个人体验版和调试构建并非正式公开发行版本，仍有未完成的产品与设备验证。仅供个人试用，建议使用可丢弃数据；不要将其作为重要数据的唯一副本，未完成实际恢复验证的版本不能作为唯一保障。",
        "图片或其他内容无法完整校验时，完整备份会失败，不会跳过已登记图片后仍提示成功。备份保留原字节，不代表已经完成严格JPEG深度解码安全验证。",
    )),
)

internal data class InstalledAppInformation(
    val applicationName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val packageName: String? = null,
    val debuggable: Boolean? = null,
) {
    val versionLabel: String get() = if (!versionName.isNullOrBlank() && versionCode != null && versionCode >= 0)
        "$versionName（版本Code $versionCode）" else "版本信息暂无法读取"
    val buildLabel: String get() = when {
        debuggable == null -> "构建身份暂无法确认"
        debuggable && packageName == "com.cooper.lithic.preview.debug" -> "个人体验版 · 可调试构建，非正式Release"
        debuggable -> "调试构建 · 非正式公开发行版本"
        else -> "非调试构建 · 不代表已通过公开发行验收"
    }
}

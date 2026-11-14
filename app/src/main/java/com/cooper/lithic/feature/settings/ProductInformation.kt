package com.cooper.lithic.feature.settings

/** Explanatory copy of the frozen contract, never an alternative cost calculator. */
internal data class InformationSection(val title: String, val paragraphs: List<String>)

internal val calculationExplanation = listOf(
    InformationSection("长期项目", listOf(
        "日均成本 = 截至观察日净投入 ÷ 持有自然日数。净投入按真实投入与收回计算。",
        "开始日计入第1天，结束日也计入。结束后持有时间冻结，后续付款或退款仍改变净投入。",
        "未开始或无有效持有天数时，日均成本未知。保留真实 0 元和负净投入。",
    )),
    InformationSection("固定周期订阅", listOf(
        "月、季、年按30、90、365天折算，不等于实际已经使用的天数。",
        "预付未来周期不等于当前已经生效；真实付款日、生效日与付费覆盖结束日分别记录。只有您确认真实付款，才新增续费记录。",
        "历史平均日成本与当前贡献分开。暂停、到期或停止时，当前贡献为 0，历史平均仍按原记录。",
    )),
    InformationSection("首页当前日均成本", listOf(
        "只统计已录入、在观察日当前有效的长期项目与订阅贡献。已结束项目、暂停或无有效付费覆盖的订阅不计入当前贡献。",
        "它不等于今天实际扣款，也不代表全部生活消费。",
        "汇总后展示时才舍入。小计舍入差异不代表另有付款。日期使用记录时区。",
    )),
)

internal val productSafetyExplanation = listOf(
    InformationSection("本机数据与手动备份", listOf(
        "记录和已登记图片保存在本机。无需登录，没有自动云同步或云备份。",
        "卸载可能导致记录丢失。请先导出并妥善保管完整备份。",
        "备份含记录、设置和已登记图片，文件未加密。请选可信的保存位置，谨慎分享。",
    )),
    InformationSection("提醒与服务商扣费", listOf(
        "自动续费与里程碑提醒将在后续版本提供（V1.1），当前不会发送通知。未来的提醒也不保证准点送达，请自行留意重要日期。",
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

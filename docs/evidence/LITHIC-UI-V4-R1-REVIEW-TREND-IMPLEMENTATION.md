# 砺石 V4-R1 · 回看首页与成本趋势实施

状态 **PARTIAL**。本报告对应回看首页与成本趋势两个 Jetpack Compose 页面；Android 环境门禁与桌面结果分别登记。

## 基线与范围

| 项目 | 固定引用 |
| --- | --- |
| R0 设计基线 | `4a7e21b62103635c788363a874bbb24c736dab4e` |
| B2-R1 生产基线 | `2217e4ed8a10999914d83dda40bd21c1ed6109db` |
| B1 首页设计 | `b09563ea60dfe8fd5c201437b46c4cd60653cf52` |
| R1 分支 | `codex/lithic-ui-v4-r1-review-trend-implementation` |
| R1 源码提交 | `e3891e3ad26ccfd862bbb0118dc4339b1e0530f6` |
| R1 证据提交 | 引入本报告的提交，见分支末端及交付回执 |

从远端 R0 提交单独建工作树。R0 和 B2-R1 的 `app` 树相同。比较 R0 后只有回看及趋势展示层、对应测试和验收资料发生变化；首页、底栏、Nav3、全局主题、各 ViewModel、引擎、Room schema、备份、媒体读取、Manifest、构建和签名配置通过路径差异核对。main 和 Release 保留在原远端引用。[受保护范围核对](lithic-ui-v4-r1-review-trend-implementation/scope-check.txt)

核对了 [R0 设计规范](../design/lithic-ui-v4-r0/NONHOME_DESIGN_BASELINE.md)、[逐页审查](../design/lithic-ui-v4-r0/NONHOME_UI_AUDIT.md)、[参考映射](../design/lithic-ui-v4-r0/TAOSHENG_STYLE_MAPPING.md)、[分阶段计划](../design/lithic-ui-v4-r0/IMPLEMENTATION_ROADMAP.md)和[性能诊断](../design/lithic-ui-v4-r0/NAVIGATION_PERFORMANCE_AUDIT.md)。对照 TaoSheng `ddd3306a5273ffc5c8ef5c48135edf5ce712e9e1` 的 `TodayScreen`、`OverviewCard`、`AppShell`、`Theme`、`SettingsHome`、`Components` 原始源码，沿用 R0 已确定的版面间距、金额层级与连续分组映射。

## 生产差异与 R0 对照

| 文件 | 改动和保留行为 |
| --- | --- |
| [ReviewShell.kt](../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt) | 回看轻量摘要采用同一表面、84dp 单条总线、实际日期和 Ready 历史数量。全部历史、分析、历史档案与说明走现有回调和语义。保留原空账本去首页、失败重试、读失败快照、数量不一致提示。紧凑摘要保留历史选择且标明所选日。 |
| [TotalCostTrendScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt) | 成本、图例、日期工具、所选日事件、日期与事件记录、文字点清单分区。完整趋势继续使用 240dp Canvas，原共用尺度、线型和拖动选择。用精确点生成分项，用原事件 ID、种类和记录 ID 跳转，保留所选日／全部历史和 50 条逐步加载。说明入口移入页头，图表外的精确日期结果仍可由读屏获知。 |
| [TotalTrendPresentation.kt](../../app/src/main/java/com/cooper/lithic/feature/review/TotalTrendPresentation.kt) | 仅增加日期跨度及事实名词的呈现规则和稳定事件预览；已有快照与事件状态逻辑保留。 |

手机正文两侧 16dp，主列最大 560dp，分组 14dp 圆角，原主题表面和分隔角色、`MoneyMetric` 与读屏金额格式继续使用。视觉差异集中在原生信息说明、读失败和核对反馈、原金额组件的大字布局，以及真实状态需要的日期文案。画板中的示意事件由真实事件源替代。日期、分项和事件工具集中在完整趋势，回看不重复展示；旧摘要上的工具测试移到完整趋势，同一条目标入口验证回调。回看失败仅保留一个可重试动作；空态原装饰图标简化，空态标题、说明、跳转和相关语义标签继续覆盖。

单日按 `evaluatedDayCount == 1L && from == asOf` 判断，空账本完整观察天数为 0。一个真实事件、长时间金额不变和长区间采样均仍走多日。单日显示精确总额与两类贡献，仍可查询图表范围以外的过去日期；计算期间金额显示未知。已有值的 ¥0.00、相抵和无有效贡献由原精确点的状态区分。多日图横向选日与无障碍前后日仅选择日期，其金额仍由现有 ViewModel 精确计算。

### 视觉证据

- 回看三主题采用 [R0 冻结画板](../design/lithic-ui-v4-r0/review-themes.png) 核对。成本趋势对照 [R0 画板左页](../design/lithic-ui-v4-r0/cost-analysis.png)。
- 成本趋势补充 [三主题浏览器参考图](lithic-ui-v4-r1-review-trend-implementation/trend-themes-design-reference.png)。沿用 R0 浏览器样稿，验证 320／360／400 宽度、1×／2×、三主题共 18 个参考布局的横向范围和纵向滚动终点。[参考图回执](lithic-ui-v4-r1-review-trend-implementation/browser-reference.json)
- 单日期及确认空账本对照 [R0 少数据画板中、右页](../design/lithic-ui-v4-r0/long-term-states.png)。以上为设计参考。两页三主题、单日、空账本的原生截图，以及系统 200% 字体、真实键盘和 TalkBack 行为均 `NOT_RUN`。

## 测试、Lint 与构建

本轮在 Windows 11 使用 JDK 17.0.20.1、Gradle 8.14.3、Android SDK 36。命令为 `gradlew.bat --offline --max-workers=1 --console=plain :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:lintDebug`。首轮完成 989 项 JVM；只读独立代码复核后修正三处 R1 问题，最终批次重新编译生产和 AndroidTest，并执行完整 JVM。

| 核对面 | 实际验证 |
| --- | --- |
| 完整 JVM | **990 PASS／0 FAIL／0 跳过**，116 个类。其中任务指定的 9 类共 **97 PASS**，已经计入 990。 |
| 单日期，及多日期但只有一笔事件 | JVM 真实引擎合成快照通过。Android 单日布局、查询、文字点清单、采样后常数趋势和手势 `NOT_RUN`。 |
| 多日期持平、长跨度采样及负值尺度 | JVM `TimelineSamplerTest`、`CostTimelineEngineTest`、`ChartGeometryTest` 与新增单日判据回归通过。Android 可见图形、分项和拖动 `NOT_RUN`。 |
| 真零、相抵、无有效贡献及未知 | JVM 展示标签及精确点／读状态合同通过。Android 金额文本、读屏、布局 `NOT_RUN`。 |
| 等待读取、确认空账本、失败、旧快照、计数冲突 | JVM 展示和读状态测试通过；Android 状态树、重试和去首页 `NOT_RUN`。 |
| 同日事件预览、详情、日期工具与导航 | JVM 同日事件稳定顺序及事件展示回归通过。Android 分页、选日、来源回跳和返回连续性 `NOT_RUN`。 |
| Kotlin、AndroidTest、Lint | 生产和 AndroidTest Kotlin 任务实际执行并通过。Debug Lint **0 错误／20 警告**，忽略各工作树绝对路径后逐项与 B2-R1 一致。 |
| Android 仪器测试 | 实际执行 0。任务 11 类有 40 个方法；同时登记新增 5 个方法及 18 个相关方法，共 63 个 `NOT_RUN`。 |

[首轮桌面日志](lithic-ui-v4-r1-review-trend-implementation/desktop-initial.log) · [最终桌面日志](lithic-ui-v4-r1-review-trend-implementation/desktop-verification.log) · [JVM 汇总](lithic-ui-v4-r1-review-trend-implementation/jvm-result.json) · [定向 JVM 原始 XML](lithic-ui-v4-r1-review-trend-implementation/jvm/) · [Lint](lithic-ui-v4-r1-review-trend-implementation/lint-results-debug.txt) · [Android 方法及状态清单](lithic-ui-v4-r1-review-trend-implementation/android-method-inventory.json)

独立代码复核覆盖保留状态、图表与事件身份。已补正范围外日期的精确金额读屏、同日事件预览与展开次序，以及回看纵轴对小于一分的非零日均值的呈现。更新旧摘要结构断言后仍核对完整趋势的日期、分项、图例、事件及返回。新增适配用例覆盖两页各 18 种三主题、三宽度、两档字号的组合，采用 `LocalDensity` 模拟字号。

## APK 交付

最终门禁通过后运行一次 `gradlew.bat --offline --max-workers=1 --console=plain :app:assembleDebug`，退出 0。输出目录仅一份 APK，App 树为 `dbec57fe3362de7d193fcb32e34f156006788e0c`。

| 项目 | 核验结果 |
| --- | --- |
| 本机路径 | `E:\CODEX\lithic-v4-r1\app\build\outputs\apk\debug\app-debug.apk` |
| 包名及版本 | `com.cooper.lithic.debug`，`0.1.0-m22-debug`，版本号 `17` |
| 文件大小 | 20,623,590 字节 |
| APK SHA-256 | `3436682bfc7475b6a72038793dd5398e369850be4238256ad7a9261105b2ee51` |
| Debug 证书 SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，唯一签名者，与 B2-R1 一致 |
| 完整性 | APK v2 验签与 ZIP CRC 通过，解码后的 Manifest 与 B2-R1 逐行一致，原 B2-R1 Debug APK 的 SHA-256 复算一致 |

[构建日志](lithic-ui-v4-r1-review-trend-implementation/assemble-debug.log) · [身份回执](lithic-ui-v4-r1-review-trend-implementation/apk-identity.json) · [包规格](lithic-ui-v4-r1-review-trend-implementation/apk-badging.txt) · [签名回执](lithic-ui-v4-r1-review-trend-implementation/apk-signature.txt) · [Lint 对照](lithic-ui-v4-r1-review-trend-implementation/lint-comparison.json)

## Android 环境及流程边界

准入 **ENVIRONMENT_BLOCKED**。预检没有运行中的合格隔离 Android，也未确认任何可用健康砺石 AVD。候选主机首次预检空闲内存 4871 MiB，低于继承的 5120 MiB 启动门禁。ADB 5038、模拟器端口未监听；只读观察了 5037，无 ADB 调用、模拟器启动、安装、系统设置改动或个人设备操作。已有系统启动 ANR 及后来独立 AVD 未完成准入的历史记录继续有效。[本轮预检](lithic-ui-v4-r1-review-trend-implementation/environment-preflight.json) · [历史 Android 记录](LITHIC-UI-V4-ICON-NAVIGATION-01.md#2026-11-16--android-再准入)

Android 测试、原生截图、320／360／400dp、系统 100%／200%、IME、TalkBack、实际 Nav3 返回、曲线横拖与纵滚均须在隔离环境复验。切换帧时间与性能根因仍未测量，生命周期、读取、协程、数据库、采样与切换动画继承原实现。完成桌面交付后停止在 R1 独立审查节点。

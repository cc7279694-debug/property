# C5-R1 回看读取与趋势工具收口

日期：2026-10-28。分支：`codex/lithic-v1-product-completion`；基线：`82431c76934f3b4fb595a62ac6d4384b7b876785`。

状态：**C5-R1 IMPLEMENTED — READY_FOR_INDEPENDENT_REVIEW / ANDROID_VISUAL_AND_MOTION_PENDING**。

## 观察来源与生产差异

依据用户转交的约28秒手机录屏独立审查：首次进入回看时菜单先上移、图表后出现；重入时插入刷新说明；趋势工具仍拥挤。本轮未取得或直接观看该视频，不将描述当作本轮实测。未制作HTML、截图宿主或新体验APK。

| 源码原因 | 本轮处理 |
| --- | --- |
| 首次无趋势状态走独立短占位分支；完整图表出现后菜单整体移动 | `ReviewShell`从首次状态起复用同一生产趋势容器；摘要、图表预定区域、贡献及操作保留相同结构。没有可信事实时只展示空值和读取占位，不画假曲线。 |
| 同代刷新额外插入整行提示 | `TotalCostTrendScreen`改为图表标题旁的固定状态槽，显示读取中、刷新中、计算中或失败重试；保留当前state中的旧图、原日期及可见旧截止日。 |
| 默认状态重复展示贡献计数、指标解释及两个说明入口 | 默认保留选日、精确主金额、曲线、两类贡献；次级操作收进“更多工具”，轴范围、指标、自然日及采样解释归并“趋势说明”。 |

只新增无持久状态的`TotalTrendPresentation`呈现映射。事实仍由原VM提供：snapshot/timeline未成对取得不作为可信结果；选日尚未完成显示null占位，不借用旧点或填0；新state不复用此前事实。generation隔离仍依赖既有VM作用域和请求票据，本轮未重写或新增缓存。

错误、notice及重试在折叠工具之外；失败可从图表标题直接重试，正文继续显示原错误及旧截止日。外部读取/恢复提示会阻止确认空态；真实0、负值、历史无当前贡献仍按原事实呈现。普通选日计算中也有固定角标。

主要修改仅3个生产文件：[ReviewShell](../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt)、[TotalCostTrendScreen](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt)、[TotalTrendPresentation](../../app/src/main/java/com/cooper/lithic/feature/review/TotalTrendPresentation.kt)。

## 入口与断言连续性

- 前后一天保留在主工具行；指定日期、分项曲线、趋势说明经“更多工具”进入。完整模式的显示点、事件、每次50条继续查看、非现金说明及记录回调保留。
- 原精确金额、负号、0值、选日、分页及记录身份断言保留；7个既有Android文件仅适配工具展开、统一说明及截止日定位。原`total-trend-axis-toggle`归并至摘要`review-trend-details-toggle`／完整`total-trend-details-toggle`；轴、指标含义tag仍位于同一个说明弹窗。
- 当前金额tag仅用于真实所选点；未知状态使用`*-pending`，不把占位当结果。新增`total-trend-plot-frame`、`total-trend-refresh-status`及`total-trend-tools-toggle`供呈现断言。工具、重试、弹窗关闭继续至少48dp，保留曲线的完整金额及日期辅助语义。

## 实际验证

| 检查 | 本轮真实结果与证据 |
| --- | --- |
| 定向JVM | **53 PASS，0失败/错误/跳过**；7类，含12个新状态方法及41个相关回归。见[统计](c5r1-20261028-014329/jvm-summary.json)、[原始执行与退出码](c5r1-20261028-014329/targeted-jvm/command.json)、[stdout](c5r1-20261028-014329/targeted-jvm/stdout)、[JUnit XML](c5r1-20261028-014329/jvm-results/TEST-com.cooper.lithic.feature.review.C5R1TrendPresentationTest.xml)。没有故意制造RED或重跑历史大套件。 |
| Debug／AndroidTest编译／Lint | 合并命令实际exit0，406.515秒；**Lint 0错误、21警告**。见[命令及字节摘要](c5r1-20261028-014329/debug-quality/command.json)、[stdout](c5r1-20261028-014329/debug-quality/stdout)、[stderr](c5r1-20261028-014329/debug-quality/stderr)、[Lint XML](c5r1-20261028-014329/lint-results-debug.xml)、[质量与普通身份](c5r1-20261028-014329/quality-summary.json)。原两条Kotlin警告及SDK XML版本警告保留，没有升级工具或屏蔽检查。 |
| Android呈现 | 新增6个Compose方法覆盖首次等待/读取、同代刷新、失败、有旧图、选日计算及新state；包含受控360dp/LocalDensity2的工具与可达性断言。与7文件适配均**只编译，运行NOT_RUN**。没有Android RED/GREEN或视觉/触控/字体/IME/TalkBack/流畅度PASS结论。 |
| 边界及原体验包 | [生产边界](c5r1-20261028-014329/source-boundaries.json)、[原绘制比较](c5r1-20261028-014329/chart-source-check.json)、[Preview8字节](c5r1-20261028-014329/existing-preview8.json)。构建时生产源码已稳定，原输出长度/SHA与命令回执已复核。 |

## 保持与待确认

投影/坐标及Canvas绘制和语义主体与基线逐字相同；ChartGeometry、MoneyMetric、Theme、VM、Nav3三类转场及来源守卫、MainActivity、Repository/DAO、领域、Owner/generation、恢复Token/原子指针、Room5/backup4、媒体与提醒协议均未改。普通Debug/Release身份与签名配置未改；没有Private Trial构建、版本调整或新用户候选，原Preview8/code24的15,797,555字节与SHA保持。

手机/ADB/模拟器/实际Compose执行均0；不恢复C3调查，不运行M22-T06/T03/IME。首次与刷新时的真实手机排版、360/400dp、系统大字、TalkBack及切换流畅度仍**NOT_RUN / ANDROID_VISUAL_AND_MOTION_PENDING**，源码断言和编译不能替代用户体验。没有取得本轮Android截图。

M10 JPEG历史FAIL、M20平台FAIL/UNKNOWN、M22 BLOCKED、M13-T06、G1～G4、C3 CLOSED/运行UNKNOWN及MEDIA_DISPLAY_PENDING全部保留。本轮不是VISUAL_ACCEPTED、产品冻结或正式发行；提交推送后停止，不进入M23。

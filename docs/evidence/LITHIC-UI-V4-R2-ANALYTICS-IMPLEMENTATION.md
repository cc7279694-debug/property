# 砺石 V4-R2 · 三个分析页实施

**PARTIAL / READY_FOR_INDEPENDENT_CODE_REVIEW**（2026-11-23）。 R2 生产范围为年度回顾、类别分析与长期复盘。

## 基线与设计

Fetch 后单独从远端 R1 `53ae1d43d6e0e51023524d5da6423c0237f5f719` 建立 `codex/lithic-ui-v4-r2-analytics-implementation`。R0 冻结设计 `4a7e21b62103635c788363a874bbb24c736dab4e` 属于此基线的祖先。源码提交为 `977e74ec6971211f60c836cc5e1bb0fb15e7846b`，证据提交可从本报告的 Git 固定引用取得。

对照了 [R0 规范](../design/lithic-ui-v4-r0/NONHOME_DESIGN_BASELINE.md)、[逐页审查](../design/lithic-ui-v4-r0/NONHOME_UI_AUDIT.md)、[涛声视觉映射](../design/lithic-ui-v4-r0/TAOSHENG_STYLE_MAPPING.md)和[分阶段计划](../design/lithic-ui-v4-r0/IMPLEMENTATION_ROADMAP.md)。年度参照 [R0 年度画板](../design/lithic-ui-v4-r0/cost-analysis.png)，复盘参照 [R0 复盘画板](../design/lithic-ui-v4-r0/long-term-states.png)，三主题对照 [R0 色板实物](../design/lithic-ui-v4-r0/review-themes.png)。

## 生产差异

| 文件 | 页面变化 |
| --- | --- |
| [AnnualReviewScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/AnnualReviewScreen.kt) | 所选年度、净投入与订阅实付优先。投入和收回共用分组。年份箭头及文本查询保留。空间不足时年份控件和主金额纵排。无记录年份展示未知；已有事实的零值按实际金额显示。非现金折抵、分类构成、跨年比较和记录入口沿用报告事实。 |
| [CategoryAnalysisScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/category/CategoryAnalysisScreen.kt) | 正文只保留一个当前类别选择控件，多项选择放入最高 320dp 的可滚动原生弹窗。长名多行，独占 48dp 触控语义和所选状态，选择仍提交原类型与 ID。当前贡献、历史及各年金额分区，使用原当前分类口径及快照。 |
| [LongTermReviewScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/LongTermReviewScreen.kt) | 类型分段、排序和日期及数量分组。按 Repository 的全局有序窗口及可排名数显示名次，保留未排名原因、原始排序、项目／订阅详情和继续加载。类别与复盘共用内层裁切按压反馈及至少 48dp 外层触控。旧快照提示覆盖此前已确认的空结果。 |
| [ReportPresentation.kt](../../app/src/main/java/com/cooper/lithic/feature/review/ReportPresentation.kt) | 仅年度和类别引用的共享报告展示。错误和保留结果提示加可读状态。条形缩至 12dp，沿用左负右正与统一绝对比例。记录采用 56dp 最小高的分隔行，原记录 ID、类型、回调及查看详情语义保留。R1 回看与成本趋势没有调用这些共用报告组件。 |

搜索生产调用点、对照提交路径。首页、悬浮导航、R1 两页、Navigation 3、ViewModel、控制器、引擎、Repository、Room、备份、全局主题、图片和签名配置都在针对基线的保护核查范围内。[核查回执](lithic-ui-v4-r2-analytics-implementation/scope-check.txt)

## 三主题视觉对照

[年度回顾](lithic-ui-v4-r2-analytics-implementation/annual-three-themes-design.png) · [类别分析](lithic-ui-v4-r2-analytics-implementation/category-three-themes-design.png) · [长期复盘](lithic-ui-v4-r2-analytics-implementation/long-three-themes-design.png)

三张是 **浏览器设计对照**。三页 × 三主题 × 320／360／400 浏览器宽度 × 1×／2× 字号，共 54 个布局，核对横向滚动边界及浏览纵向终点。类别独立画板采用 R0 色板、收起的当前类选择与原合成账本。年度输入、类别选择弹窗、异常状态和实际返回路径须在 Android 核对。原生截图为 `NOT_RUN`。[浏览器回执](lithic-ui-v4-r2-analytics-implementation/browser-reference.json)

## 回归与构建

在 Windows 使用 JDK 17.0.20.1、Gradle 8.14.3、Android SDK 36。定向任务使用 `:app:testDebugUnitTest --tests` 并按原有 10 个测试类过滤。完整验证使用 `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:lintDebug`。两组均离线单 worker。

| 检查 | 结果与覆盖 |
| --- | --- |
| 定向 JVM | **92 PASS**，0 失败／0 跳过。包括年度与类别引擎、单类别回退、排名引擎与控制器、年度及类别导航、R1 快照和趋势呈现。 |
| 完整 JVM | **990 PASS**，116 类，0 失败／0 跳过。包含上述定向用例。 |
| 生产与 AndroidTest 编译 | 两项 Kotlin 任务通过。 |
| Lint | **0 错误、20 警告**。标准化工作树路径后，警告逐项与 R1 一致。 |
| 新增原生断言 | 年度有符号净投入及真实零付款，年份切换清除失效报错。类别多项滚动、长名称、选中及原 ID。复盘三主题／三宽度／模拟两档字号，真实首名、未排名原因、分页及类型跳转。 |
| 实际 AndroidTest | 指定回归清单 43 个方法，执行 0，全记 `NOT_RUN`。保留既有跨年、付款、非现金折抵、分页、导航旅程和按压用例。 |

独立只读代码复核后，修正了年份切换旧错误、空旧快照提示及名次测试。类别无历史资金事实时只显示既有缺数据说明，当前贡献继续使用引擎结果。编译与 Lint 使用修正后的源码重新核查。[定向日志](lithic-ui-v4-r2-analytics-implementation/jvm-targeted.log) · [定向 XML](lithic-ui-v4-r2-analytics-implementation/jvm-targeted/) · [最终桌面日志](lithic-ui-v4-r2-analytics-implementation/desktop-verification.log) · [测试汇总](lithic-ui-v4-r2-analytics-implementation/jvm-result.json) · [Lint 全量](lithic-ui-v4-r2-analytics-implementation/lint-results-debug.txt) · [Android 清单](lithic-ui-v4-r2-analytics-implementation/android-test-inventory.json)

## APK

桌面门禁通过后执行一次 `:app:assembleDebug`，退出 0。构建目录生成一个候选。交付文件名含 R2 和源码短号，副本 SHA-256 与原始产物一致。原 R1 APK 的 SHA-256 复算仍与既有证据一致。

| 项目 | 核验结果 |
| --- | --- |
| 命名交付路径 | `E:\CODEX\lithic-v4-r2-local\delivery\Lithic-V4-R2-Analytics-977e74e-code17-debug.apk` |
| 包名和版本 | `com.cooper.lithic.debug`，`0.1.0-m22-debug`，版本号 17 |
| APK SHA-256 | `9710372ca011a1ae4934723b6ec99acacd2742b166254bf21c185540abfb1213` |
| 文件大小 | 20,672,742 字节 |
| Debug 证书 SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，单签名者，v2 验签通过 |
| 归档检查 | ZIP CRC 通过，APK 内 Manifest 与 R1 字节相同，DEX 找到三页新增标记及 R1 单日事实标签 |

新旧 Debug 继续沿用同一包名、版本号和证书。要区分交付文件，请核对命名及完整哈希。DEX 初次对照发现年度旧标签已被 R1 使用，最终改查 R2 新的 `AnnualKeyAmounts`。原始 APK 无需重建。[构建日志](lithic-ui-v4-r2-analytics-implementation/assemble-debug.log) · [APK 身份](lithic-ui-v4-r2-analytics-implementation/apk-identity.json) · [签名回执](lithic-ui-v4-r2-analytics-implementation/apk-signature.txt) · [DEX 抽查](lithic-ui-v4-r2-analytics-implementation/dex-markers.json)

## 安卓现场及剩余核对

没有运行中的健康隔离模拟器，也未核实可用的隔离测试设备。历史 AVD 启动失败回执继续有效。仅做主机和现有监听的只读检查，未启动 AVD、调用 ADB 或操作小米14。AndroidTest 运行、原生截图、系统 200% 字体、IME、TalkBack、实际触控反馈及详情返回旅程均 `NOT_RUN`。[环境回执](lithic-ui-v4-r2-analytics-implementation/environment-preflight.json)

R2 源码和证据仅在独立分支交付。远端一致性及工作区状态以推送后 Git 核对为准。等待独立审查。

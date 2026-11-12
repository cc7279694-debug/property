# LITHIC HOME-INVESTMENT-01 · 首页总投入

首页布局基线 `154ff0fe3a6e0e60f6cee193daf6062b644d7bfd`，分支 `codex/lithic-home-investment` 从其后续证据提交 `9d46a4e7ce6cd898e0c613771ea785849e6d497f` 建立。开始时原分支的远端与本地一致，工作区干净。

固定源码提交 `ca6ec458b6705b1681812757ecb34993380931c3`，`app` 树 `988dadd64990e5a83565f86c7d9a17f923e14390`。

## 数据与显示

| 项目 | 实施 |
| --- | --- |
| 总投入 | 累加观察日及以前所有项目 `OUTLAY`（按 `occurredOn`，含以旧换新非现金折抵）与订阅付款（按 `paidOn`）。已结束、暂停、停止的记录仍计入。收回与未来事实排除。提前支付按实付日累计，覆盖生效当天无第二笔。 |
| 一致性 | `ReportRepository.loadOverview()` 复用原分页、身份与关联校验、`withSnapshot` 和 Room 事务。在每批原有成本计算成功后累加，最终分页完整性检查通过后将金额附到同一个 `OverviewResult`。未增加 SQL、DAO 调用或单独账本读取。 |
| 金额边界 | `Long?` 整数分与 `Math.addExact` 支持超过单笔 `Money` 上限的汇总。跨记录溢出只将总投入置为未知。原记录校验失败仍走既有读取失败路径。 |
| 首页 | 原卡片位置使用 `MoneyMetric`。有记录且总投入为零时显示 `¥0.00`，空账本保留引导与 `—`，缺少汇总或无快照显示 `—`。读取失败保留可信旧快照时，显示旧金额、旧日期及既有「上次取得的结果」提示。口径补充在原信息弹窗。 |
| 原有指标 | 日均成本、类别、计数与 `OverviewEngine.combine()` 保持原实现。项目日均仍基于原净投入，订阅日均仍基于原有效覆盖，年度报表仍保留各自金额口径。 |

## 文件范围

- `OverviewEngine.kt` 增加兼容字段 `OverviewResult.totalInvestmentCents`，默认 `null`。
- `ReportRepository.kt` 汇总通过既有校验的分页事实。
- `HomeShell.kt` 显示快照金额及口径。原卡片结构、双列内容、两个新增入口、分段切换和底栏保持。
- `OverviewInvestmentTest.kt`、`ReportRepositoryTest.kt`、`HomeOverviewUiTest.kt`、`P2HomeArchiveContractTest.kt` 覆盖汇总、Room 快照和界面呈现。

Room、备份恢复、Owner/generation、金额和日期模型、正式身份及签名配置未修改。

## 验证

- `:app:testDebugUnitTest` 定向 21 类，224 项通过，0 失败、0 错误、0 跳过。其中 3 项新增 JVM 回归覆盖时间截点、历史状态、收回、跨批相加、空与未知、单笔上限和溢出。其余覆盖首页、格式化、总览与既有报表计算。
- `:app:compileDebugKotlin` 与 `:app:compileDebugAndroidTestKotlin` 通过。Room 测试含两类各 103 条记录、分页期间协调写入、真实换新交易组及实付／生效日期对照。Compose 测试含空、零、未知、旧快照及 360dp 模拟 200% 字体的完整长金额。
- `:app:lintDebug` 通过，0 错误、20 警告。主源码、AndroidTest 与 JVM 测试源码分析均完成。最终 Gradle 退出码 0，定向 JVM 任务复查为 `UP-TO-DATE`。
- Android 运行 `NOT_RUN`。此前隔离模拟器未通过连接准入，本轮未重启。分页、并发写入与 200% 布局的端侧断言只有源码编译证据。未生成 APK，未访问个人账本。

`LITHIC_HOME_INVESTMENT_READY_FOR_INDEPENDENT_REVIEW`

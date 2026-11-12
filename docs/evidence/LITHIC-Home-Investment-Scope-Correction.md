# LITHIC 首页总投入 · 口径修正

以源码 `ca6ec458b6705b1681812757ecb34993380931c3` 为基线。开始时 `codex/lithic-home-investment` 位于 `4678b9bcc791aeb78797842a1602c24cc3400ee6`，相对源码基线只增加报告和状态登记。远端一致，工作区干净。

修正源码 `35149b58386e859103b22be5bdadc86d3a2322e3`，`app` 树 `ec111685f2030987b9783a94eb58e739c5855d21`。

## 修正

「总投入」仅累计所有长期项目截至观察日的 `OUTLAY`，包括已结束项目和以旧换新非现金折抵。项目 `RECOVERY`、订阅付款及观察日后的项目投入均不纳入。

`ReportRepository.loadOverview()` 只在已校验的项目分页中累计整数分。`Math.addExact` 溢出时仍将总投入留为未知。订阅分页继续用于原日均成本、数量和类别。所有首页指标仍来自同一个 `withSnapshot` 和 Room 事务。

`OverviewResult` 的字段说明与首页原信息弹窗同步修正。卡片布局、原日均与订阅业务、Room Schema、备份协议、正式包名和签名配置保持原样。

生产改动限于 `ReportRepository.kt`、`OverviewEngine.kt` 和 `HomeShell.kt`。回归改动限于 `OverviewInvestmentTest.kt`、`ReportRepositoryTest.kt` 和 `P2HomeArchiveContractTest.kt`。

## 验证

- 修正前运行 `OverviewInvestmentTest`，4 项中 1 项如期失败。订阅付款排除断言期望 12,500 分，旧代码返回 17,000 分。
- JVM 回归用同一组项目和同一观察日，比较两组订阅付款金额。Room 用例只修改一笔订阅付款，验证订阅日均和合计日均随之变化，而项目总投入保持 1,000 分。原分页、并发、换新及金额上限用例按新口径核对。
- 修正后 21 类共 225 项定向 JVM 通过，0 失败、0 错误、0 跳过。生产 Kotlin 与 AndroidTest Kotlin 源码编译通过。
- `:app:lintDebug` 通过。主源码、AndroidTest 及 JVM 测试源码均完成分析，报告为 0 错误、20 警告。最终 Gradle 退出码 0。
- Android 运行 `NOT_RUN`。Room 和 Compose 用例本轮只有编译证据。未生成 APK，未合并 `main`，未发布。

`LITHIC_HOME_INVESTMENT_SCOPE_CORRECTION_READY_FOR_INDEPENDENT_REVIEW`

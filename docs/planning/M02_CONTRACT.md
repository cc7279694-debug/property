# M02 执行契约

授权来源为用户本轮实际附件 `E:/CODEX/.codex/attachments/12799a35-02ac-46b5-b856-1a0a3eb28bf2/已粘贴的文本.txt`（M02 — 金额、日期与长期项目成本引擎）。已全文读取；9,692 字节，SHA-256：`f13202ff3505ddc644840b7512212861317a3dde8b3ea470cbc2a20427a58bc0`。附件是本轮当前用户请求，不是原归档的执行提示。

工作目录 `E:/CODEX/砺石新APP`，仓库 `cc7279694-debug/property`。起点为 M01 验收回写提交 `a25a06899bd157988c3a4466d7162d8616fb0249`，来源 `feat/m01-android-skeleton`；检查工作区干净、实际远端 M01 SHA 一致且 M02 本地/远端分支不存在后，创建 `feat/m02-project-cost-engine`。不从尚未包含 M01 的 main 开始，不覆盖其他未提交工作。

M00/M01 均已独立验收冻结为 **ACCEPTED / FROZEN — PASS WITH NOTES**。M00 评审 HEAD `59ba290cba69e1e77af7d0376a8b43aaabbf5f88`，M01 评审 HEAD `ca8499e3d3cb2e5bac3ab29a231f399636899c19`；既有 NOTES 与原验证不改写，见[当前验收登记](../ACCEPTANCE_MATRIX.md)。本契约记录授权与验收要求，唯一实时进度和已执行结果见[CURRENT_STATE](../CURRENT_STATE.md)，不以契约要求代替测试或独立验收结论。

## Goal / Scope

用纯 Kotlin 领域代码统一人民币金额、可注入日期与长期项目观察日计算，并通过固定日期的自动化测试验证。

- `Money.kt` / `MoneyParser.parseCny(text: String): MoneyParseResult`：Long 分、非负录入绝对值与独立投入/收回方向、最多2位小数、单笔上限999,999,999,999分、允许0元、明确拒绝非法输入/超范围、聚合溢出检查。资金不用 Double/Float；日均成本用 BigDecimal DECIMAL128，展示才 HALF_UP 两位，小额非零不能当零。
- `BusinessClock.kt`：LocalDate 与固定日期测试注入；引擎接受明确 asOf，不硬编码系统今天，不接 DataStore。
- `DomainModels.kt`：ProjectSnapshot、ProjectMoneyEvent、ProjectCostResult，开始/可选结束日期、方向、真实事件日期及金额；独立于 UI、Room Entity 与 Android Context。初始投入仅是事件，不重复累加。
- `ProjectCostEngine.evaluate(input: ProjectSnapshot, asOf: LocalDate): ProjectCostResult`：持有天数、投入/收回/净额、DECIMAL128日均（未做展示舍入）、观察日日末有效贡献及无有效分母原因。派生结果不保存每日快照，负净成本不钳为0。
- 必要的最小测试配置、真实 RED → GREEN、回归/Lint/构建/静态范围检查、当前文档和功能分支提交/推送；沿用 M01 锁定工具链。

## Out of Scope / Constraints

不进入 M03；不创建 Room/SQLite/DAO/迁移、业务 Repository、Server/API、项目新增编辑删除界面、订阅计算、首页统计/曲线/年度回顾、备份恢复、媒体、通知或临时展示页面。保留 M01 空壳。Local-only / 人民币 / 只记录事实；不引入预测、评分、估值或多币种。禁止 WHOCARE 修改、main 合并、公开发布/上传 APK、凭据或真实个人消费数据。

原 ZIP、43 原件、原130验收案例、M00/M01历史 checkpoint 及历史决策不改写。Q03～Q06 状态与期限逐项继承；原16组跨模块补测保持 NOT_RUN。本轮已确认 Q02/Q07，只确认有效解释，不等于已实施/测试通过，不擅自关闭其他问题。

## Dependencies / Confirmed Rules

已满足前置 M01 独立验收和本轮实施授权。实际读取[原 M02 计划](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m02.md)、[领域 C02/C03/C04/C05/C13](original-v1/lithic_v1_plan/docs/specs/DOMAIN_CONTRACT.md)、[架构 A03/A04](original-v1/lithic_v1_plan/docs/specs/ARCHITECTURE.md)及 M02-T01～T06；以当前有效解释处理歧义，归档不变。

**Q02 — USER_CONFIRMED：**asOf 在开始前无有效日均/无贡献；开始日至结束前按观察日计持有自然日；结束日起固定完整持有天数（含首末日），结束日日末已退出总成本。事件只按 occurredOn <= asOf 计入。后结算改变净投入/结算结果，不增长分母、不重新活跃。例：2026-01-01投入1000、01-10结束，01-02为2天/500，01-09为9天/1000÷9，01-10为10天/100且日末无贡献；01-20收回200，01-19仍100，01-20为80，分母始终10。详见[决策](../DECISIONS.md)与[Q02 补测](CLARIFICATIONS.md)。M17/M18集成仍待对应模块。

**Q07 — USER_CONFIRMED：**每模块只执行已具备条件且符合范围的验证，并满足其自身真正要求。M02只要求纯 Kotlin 测试、必要集成回归、Lint、构建和静态范围核查。没有数据/业务界面/备份时，不提前创建数据库、备份往返或无目标 Instrumentation Test；不适用能力写 NOT_RUN / N/A 与原因，未来测试和 NO-SOURCE 不能标PASS。解释适用后续模块，各自实施证据仍待执行。

## Acceptance Criteria / Verification

原 M02 六项命名测试全部落实：`same_day_is_one_day`、`dated_event_does_not_rewrite_earlier_days`、`end_day_removes_current_contribution`、`post_end_refund_keeps_duration`、`money_precision_and_zero`、`negative_cost_is_not_clamped`。落实 Q02-V01～V03 的本轮领域部分；补开始前、同日开始结束、非法结束日、未来于观察日的金额事件、0元、负净额、累加溢出、超精度、分期、独立退款与极小非零日均边界。所有计算测试使用固定日期。

先写真实行为失败测试，再实现最小代码，记录 RED → GREEN；编译/环境失败不充当行为红灯，不故意改坏已正确实现。至少实际运行：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug :app:assembleDebug
```

从 Gradle XML/HTML报告登记各原案例、Q02领域补测、测试总数和通过/失败/跳过，以及构建/Lint实际输出；检查与M01基线相比权限、依赖、界面和业务范围变化。原案例仍NOT_RUN快照，本轮实际结果仅登记[当前验收表](../ACCEPTANCE_MATRIX.md)。数据库/备份/Instrumentation、真机及其他未运行验证写 NOT_RUN / N/A 和原因，不用领域单测替代未来集成。

采用既有唯一 CURRENT_STATE 与历史 checkpoint，不建立平行状态机制。提交前检查差异/范围/敏感信息，仅提交并推送 `feat/m02-project-cost-engine`。最终报告接口、全部文件变化、测试/构建证据、数据/权限/依赖变化、遗留问题、Git SHA和M03前置条件；只输出 READY_FOR_REVIEW — M02 并停止等待独立验收，不能自行宣布 ACCEPTED / FROZEN 或进入 M03。

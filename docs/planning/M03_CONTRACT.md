# M03 执行契约

授权来源：2026-10-11 用户当前会话的“M03 — 订阅周期与状态计算引擎”。本轮从已验收 M02 回写提交 `0169c76bbf0946a0400b96d00124b3de9aea1cc0`、来源 `feat/m02-project-cost-engine` 开始；实际确认本地与远端基准一致、工作区干净、新分支不存在后，创建 `feat/m03-subscription-engine`。不从 main 开始，不覆盖其他工作。用户消息末尾停在提前续费例子的“可以包含”，按前文完整规则处理已付款未来覆盖，不补造未提供的后文。

## Goal / Scope / Dependencies

实现纯 Kotlin 单订阅快照、真实续费、状态历史及校验/计算，继承 M02 的 Money、BusinessClock 和 BigDecimal DECIMAL128 精度；相应自动测试、适用回归/Lint/构建、文档、当前功能分支提交和推送。M00/M01/M02 已独立验收冻结，保留所有历史 NOTES；M02 评审 HEAD `ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`，独立审查未在用户 Windows 本机重跑 Gradle。

已恢复当前 AGENTS、PROJECT、CURRENT_STATE、DECISIONS、ARCHITECTURE、ROADMAP、SPEC_ISSUES、CLARIFICATIONS、EXECUTION_PROTOCOL；实际阅读[原 M03](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m03.md)、[领域 C07～C12](original-v1/lithic_v1_plan/docs/specs/DOMAIN_CONTRACT.md)、[架构 A03/A04](original-v1/lithic_v1_plan/docs/specs/ARCHITECTURE.md)、原 M03-T01～T06。归档执行提示只作来源，不覆盖当前授权与项目记忆。

## Rules / Minimal API

- BillingCycle 仅 MONTHLY/QUARTERLY/YEARLY，标准 30/90/365；Renewal 同时保存 cycle、standardDays、Money 和四个日期。金额 > 0、标准天数匹配固定周期、覆盖为非空 `[effectiveFrom, paidUntilExclusive)`；reminderDueOn 默认付费边界，但可独立记录真实边界。付款晚于生效日不被擅自禁止。
- SubscriptionSnapshot 包含订阅身份、续费列表、状态历史，不引入数据库实体。无状态记录默认 ENABLED；同一天多个状态记录明确拒绝，修正应先替换旧记录。覆盖重叠拒绝，邻接和空档合法，列表顺序不改变结果。
- `SubscriptionIntervalRules.validateForWrite(input, clock)` 用业务今天拒绝未来付款；`validateStructure(input)` 仅校验结构。`evaluate(input, asOf)` 不调用今天，只纳入 paidOn <= asOf 的真实付款，不将后来支付的记录当非法历史输入。
- ACTIVE 才有有效当前价格，currentRate 为金额/该笔 standardDays；非贡献状态 currentRate=0、currentPrice=null。历史平均在没有已确认付款时为 null，并给出明确原因；已购标准时长和已付金额包含已付款尚未生效的下一期。
- 展示只用 ACTIVE/PAUSED/EXPIRED/STOPPED。按观察日状态历史和已知覆盖推导，不依赖后台刷新，不引入第五种状态。未来暂停/停止不提前生效，恢复复用已有付款。
- elapsedPaidDays 为已知覆盖与截至观察日（含当天）的 ENABLED 区间之交集并集天数。采用 epochDay 半开区间、排序和区间合并，不逐日枚举；暂停/停止不抹掉已付金额或标准时长。
- 默认停止 helper 返回当前所选真实付费记录的 paidUntilExclusive STOPPED 边界；提前停止须调用方显式提供状态日期。不生成付款、不操作服务商自动续费、不猜下一期套餐。

## Out of Scope / Constraints

不进入 M04，不创建 Room/SQLite、DAO/Repository、迁移、订阅 UI/三栏页面、设置持久化、通知/里程碑触发、媒体、备份、退款/自动折价、多套餐并行、模型训练、服务器或网络功能。M01 空壳和已冻结 M02 生产代码不改。无新依赖或权限，不升级工具链。不改 WHOCARE、不合并 main、不公开发布 APK、不上传私钥/个人数据。

原 ZIP、43 原件、130 NOT_RUN 案例、M00～M02 checkpoint 和证据不改写。Q03～Q06 状态/期限不变；本轮 Q03 订阅时间和 Q04 成本区间的领域断言另记当前验收，不能替代 M21/M20 通知及未来集成。16 组完整补测仍 NOT_RUN，Q02 历史总成本视图集成仍待 M17/M18；Q07 仅验证本模块适用能力。

## Acceptance Criteria / Verification / Stop

落实 M03-T01～T06：monthly_standard_rate、annual_purchase_not_elapsed_year、prepaid_next_rate_does_not_start_early、expired_has_zero_current_rate、pause_does_not_erase_paid_history、overlaps_rejected；补历史付款过滤、季度/改周期、闰年、暂停恢复、默认/提前停止、空档、同日冲突与重排、独立提醒边界、金额精度与日期极限。先执行可编译的真实行为 RED，再 GREEN；编译/环境错误不计 RED，测试源码前后保持一致。

实际执行过滤 M03 测试以及 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`，从 XML 登记真实数量/断言结果，核对最终 APK 的权限、运行依赖与业务范围。设备、Instrumentation、业务备份/数据库/迁移仍 NOT_RUN / N/A，不为通用模板创建后续能力。提交前核查归档字节、案例状态、文档链接、范围与敏感信息；证据记录在唯一[当前验收](../ACCEPTANCE_MATRIX.md)、[当前状态](../CURRENT_STATE.md)和历史 checkpoint。

只推送 feat/m03-subscription-engine，输出 READY_FOR_REVIEW — M03 后立即停止等待 ChatGPT 独立验收；不得自行宣布 ACCEPTED / FROZEN 或开始 M04。

## M03 独立验收补测授权（2026-10-11）

用户转交 ChatGPT 对 `80cf6473c21d5933963dd55b02dfbf9f97297849` 的结论：CHANGES REQUESTED，仅补累计金额/标准天数溢出测试，暂不冻结。六原案例及37＋31测试证据已核对，未发现确定性核心计算缺陷。当前工作区干净、本地/远端该HEAD一致，继续现有feat/m03-subscription-engine，不另建分支。

本轮仅提取生产 evaluate 实际调用的内部纯累计函数，保留两次Math.addExact；增加可重复的Long边界测试和真实付款路径回归，不放宽Money/周期/日期校验、不增加业务功能。使用合法Renewal作为增量，内部合成累计种子用于触发边界，明确不是海量合法完整历史的端到端验证。现有检查本已正确，本轮按边界补测/回归流程执行，不人为制造RED；旧37项订阅与31项M02测试保持原样。

实际执行新增测试及 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`，更新当前状态、当前验收和新的复验证据/checkpoint，保留原交付历史。只记录STOPPED后重新启用、多期预付默认停止边界为M09风险，不在本轮改变语义；Q03～Q06及原130/16补测边界不变。提交/推送现有M03功能分支，输出 READY_FOR_REVIEW — M03 (RECHECK) 后停止，等待独立复验，不进入M04或自授冻结。

# 单模块开发协作与执行规范

本文件依据当前用户协作规范和[阅读版总计划](../LITHIC_V1_产品与开发规划.md)第 7、9 节落地；不是未取得 ZIP 中原模板的替代原件。

## 执行流程

读取事实源与 Git → 明确本轮授权模块 → Task Contract → 风险匹配的测试与最小实现 → 实际验证 → 更新 CURRENT_STATE/适用决策 → 提交功能分支 → 模块完成报告 → 停止 → 独立验收。

实现者不得跳过验收开始下一模块。文档任务仅检查文档，不机械新增产品测试。业务计算、迁移、恢复、导入导出等重要逻辑优先可重复测试。

Task Contract 包含 Goal、Scope、Out of Scope、Constraints、Dependencies、Acceptance Criteria、Verification。修改前能解释为什么改该文件、可能破坏什么、如何验证。

额外问题分为 Blocking（不解决无法继续）、Related（当前授权内小范围）、Unrelated（登记而不处理）。阻塞性规格问题不得由执行者自行改变核心规则。

## 状态与证据

| 状态 | 使用要求 |
|---|---|
| NOT_STARTED / PLANNED | 没有实施证据 |
| IN_PROGRESS | 有获准的进行中工作 |
| BLOCKED | 明确条件未满足，列出影响与解除方式 |
| READY_FOR_REVIEW | 交付可检查，问题与验证已如实报告；不代表通过 |
| ACCEPTED / FROZEN | 有独立验收结论与来源，可进入下一授权门槛 |

执行状态、资料冻结状态、独立验收状态分别写明。例如文档可以提交审查，而完整资料冻结仍 BLOCKED。只有 CURRENT_STATE 维护当前状态；验收表维护案例结果，checkpoint 维护历史证据。

验证结果只用 PASS / FAIL / NOT_RUN / BLOCKED，必须有日期、实际方法与结果。没执行写 NOT_RUN 和原因；预期结果不能当成实际结果。

构建成功不证明金额正确、离线能力或恢复不丢数据。依赖版本在 M01 的真实环境验证后锁定；数据库 schema/backup 格式版本以实际实现和测试为准。

本地子代理审查可辅助发现问题，不能冒充 ChatGPT 独立验收，也不自动授予 ACCEPTED。

## Git 与资料安全

使用当前模块功能分支；提交只含本模块内容。提交前检查差异、格式、实际验证、无关修改、临时文件与敏感信息。禁止提交 .env、token、cookie、私钥、服务凭据、Android 签名密钥或真实个人数据库/备份。

Conventional Commits；获准时只推当前功能分支。没有另行授权不得合并 main、force push、重置远程、删除历史、部署或发布。

若来源文件变更，先对比原件和校验值，记录用户确认、受影响计算/数据/图表/迁移/备份与新增验收，不静默覆盖基线。

## 模块完成报告格式

1. 完成功能：实际实现与没有完成的内容分开。
2. 修改文件：新增、修改、删除。
3. 数据层变化：SQLite、Migration、Repository、File System、Backup、Config；没有则明确无。
4. 测试：只列实际执行结果；未执行写 NOT_RUN 与原因。
5. 当前问题：Known Bug、风险、技术债、尚未处理项。
6. Git：Branch、Commit、Push 的实际状态。
7. 下一模块：前置条件和建议，然后停止。

M00 评审输出为 READY_FOR_REVIEW — M00；若资料或验证仍阻塞，要紧邻说明，不能让该标签被理解为 ACCEPTED / FROZEN。

## 决策与检查点规范

DECISIONS 每条包含日期、状态、Decision、Context、Alternatives、Reason、Consequences 和来源。区分用户确认与规划工程默认；建议未确认不能登记为已接受。取代决定保留旧记录并标 Superseded。

有意义阶段停止点才创建 checkpoint。记录 Goal、Verified completed、Changes、Inherited decisions、Known limitations、Verification、Next step。阻塞停止点写 Blocked，不把未验证工作补写成完成历史。

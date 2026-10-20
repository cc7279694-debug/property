# 单模块开发协作与执行规范

**M11-B当前授权**：从已独立冻结M11-A 79771fc先行完整导出候选，包括只读媒体、归档/目标读回、SAF及设置页。此前M11-A禁止开放入口的阶段边界已被本轮授权取代。M10延期及旧现场保护不变，真实设备须先确认全新环境；不得把未运行项或兼容性缺口标PASS，M12未授权。

**2026-10-20当前授权**：用户暂停M10设备研究，保持PENDING / DEFERRED并保留全部现场；仅M11-A备份导出核心候选从M09冻结回写隔离先行。执行范围和禁止事项见当前AGENTS；下文M09及旧阶段描述为历史。正式完整M11仍须媒体集成和Android门禁，不自授冻结、不进入M12。

本文件依据当前用户协作规范和[总计划](planning/original-v1/lithic_v1_plan/docs/MASTER_PLAN.md)第7、9节落地；[原执行模板](planning/original-v1/lithic_v1_plan/docs/templates/EXECUTE_ONE_MODULE.md)已归档，本文件管理当前执行，原模板不是新的授权。

## 执行流程

读取事实源与 Git → 明确本轮授权模块 → Task Contract → 风险匹配的测试与最小实现 → 实际验证 → 更新 CURRENT_STATE/适用决策 → 提交功能分支 → 模块完成报告 → 停止 → 独立验收。

实现者不得跳过验收开始下一模块。文档任务仅检查文档，不机械新增产品测试。业务计算、迁移、恢复、导入导出等重要逻辑优先可重复测试。

M00～M09已独立冻结PASS WITH NOTES。[M09契约](planning/M09_CONTRACT.md)的A/B规则、状态/付款事实CAS、原子重开和安全删除保持不变。M09原549唯一方法及专用API36旅程属于历史证据，不代表M11-A实际运行。当前只按顶部授权执行M11-A核心，保持Room2/引擎/owner/时区/工具链；Q03～Q06及历史NOTES保留。旧模块下方执行要求属于当时历史；本轮READY_FOR_REVIEW后停止，不自授完整M11冻结，不进入M12或恢复M10研究。

Task Contract 包含 Goal、Scope、Out of Scope、Constraints、Dependencies、Acceptance Criteria、Verification。修改前能解释为什么改该文件、可能破坏什么、如何验证。

Q07 已在 M02 由用户正式确认并继承：每个模块只执行已具备实施条件且属于该交付范围的验证，各模块自身真正要求仍须满足。归档通用模板里的真实路径、备份往返和 APK 等完成条款按实际范围适用：M00 仅文档，M01 验证工程、最终 APK 与实际安装/断网冷启动，M02/M03 仅纯领域引擎，完整导出/恢复到 M11/M12 才实现。M02/M03 只要求纯 Kotlin 测试、必要集成回归、Lint、构建和静态范围核查；没有 Room、业务界面、真实数据或备份，不提前创建数据库、备份往返或没有实际目标的 Android Instrumentation Test。尚不适用的能力写 NOT_RUN / N/A 与原因，不能标未来测试 PASS，也不能把 NO-SOURCE 当作测试通过。此解释适用于后续模块，并非豁免其真正要求。原模板对照见[BASELINE_COMPARISON](planning/BASELINE_COMPARISON.md)，确认来源见[当前决策](DECISIONS.md)。

额外问题分为 Blocking（不解决无法继续）、Related（当前授权内小范围）、Unrelated（登记而不处理）。阻塞性规格问题不得由执行者自行改变核心规则。

## 状态与证据

| 状态 | 使用要求 |
|---|---|
| NOT_STARTED / PLANNED | 没有实施证据 |
| IN_PROGRESS | 有获准的进行中工作 |
| BLOCKED | 明确条件未满足，列出影响与解除方式 |
| READY_FOR_REVIEW | 交付可检查，问题与验证已如实报告；不代表通过 |
| ACCEPTED / FROZEN | 有独立验收结论与来源，可进入下一授权门槛 |

执行状态、资料接入状态、独立验收状态分别写明。文档可提交审查；资料齐备不代表ACCEPTED/FROZEN。只有CURRENT_STATE维护当前状态；验收表维护当前模块实际验证结果，归档案例保持原NOT_RUN，checkpoint维护历史证据。

验证结果只用 PASS / FAIL / NOT_RUN / BLOCKED，必须有日期、实际方法与结果。没执行写 NOT_RUN 和原因；预期结果不能当成实际结果。

M01 的 M01-T01～M01-T04 分别记录设备侧断网冷启动、合并 Manifest 与最终 APK 权限、Debug 身份隔离、版本锁及实际构建证据；lint 结果另列。没有可用设备时先交付其余结果和 APK，安装/断网冷启动写 NOT_RUN 或 BLOCKED 并给出最少手工步骤。模拟器必须标明类型，不能冒充真机。NO-SOURCE、未发现测试和环境配置失败不能算产品测试通过或有效的 TDD 红灯。

M02/M03历史验证要求继续保留：M02六案例和 Q02-V01～V03 领域结果不由M03订阅实现覆盖，后续 M17/M18 视图/总线集成仍待对应模块执行。M03 当时要求先写固定日期的失败测试，再实现最小领域代码，保留真实 RED → GREEN 输出；不得故意改坏已有正确行为制造红灯。逐项登记 M03-T01～T06 的实际结果，回归 M02 既有测试；至少实际运行 `:app:testDebugUnitTest`、`:app:lintDebug :app:assembleDebug`，从报告记录总数、通过、失败/跳过，环境错误或编译失败不能替代行为失败。当时保留 M01 界面及锁定工具链、不引入该模块禁止的业务能力；这不是禁止后续已明确授权模块演进页面。

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

历史 M05 当时要求实际运行 `:app:testDebugUnitTest`、`:app:connectedDebugAndroidTest`、`:app:lintDebug :app:assembleDebug`，原 74 项 JVM 与 86 项 Android 存储回归和新增测试分别登记。M05-T01～T04 须覆盖真实空/非空 Room、初始化及读取失败、三入口/返回、360dp/200%字体与合成表单操作；旋转、Activity重建、真正系统进程死亡和 force-stop 分开记录，不相互冒充。最终 APK、版本/权限、schema v1不变及 generation/记录时区稳定均须实际核对；合成表单回调不等于 M06 业务保存。原始 130 案例及16组完整跨模块补测保持 NOT_RUN，Q03～Q06 期限及 M09 两项风险不变，M12 完整恢复仍 NOT_RUN。

M06已独立验收冻结，其284个唯一方法及设备/APK历史结果保留。本轮M07必须实际运行JVM/Android完整回归及新增测试、Lint/构建，并验证最终APK、真实结束/纠错/尾款/取消与确认删除/历史返回/断网重启和状态恢复。编译错误不能代替行为RED；旧阶段断言若确需适配须逐项记录且保留事实验证。原130案例及16完整补测原件不改；完成标签见[M07契约](planning/M07_CONTRACT.md)，交付后不进入M08。

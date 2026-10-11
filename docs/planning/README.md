# 规划资料与来源登记

更新日期：2026-10-11。原始 Planning Baseline 1.0 已在 M00 接入，M00～M02 随后均独立验收冻结为 PASS WITH NOTES；当前用户已另行授权 M03，Q02/Q07 继承 M02 已确认解释。来源完整、规则确认与实际模块验证分别登记，M02独立未重跑Windows Gradle的NOTES保留。

## 实际取得的原始资料

用户工作单写的项目内ZIP路径在检查时不存在；实际读取用户附件路径 `E:/CODEX/LITHIC_V1_AEE6B4BCE591A7E5928C%85.zip`。用户原文件没有移动、改名或覆盖。

| 来源 | 存储/入口 | 校验与范围 |
|---|---|---|
| 原ZIP | [original-v1/source.zip](original-v1/source.zip) | 131,564字节，43个文件，解压合计240,094字节 |
| 解包原件 | [lithic_v1_plan/README.md](original-v1/lithic_v1_plan/README.md) | 原目录层级和相对引用完整保留，40份Markdown、3份JSON |
| 每个原文件的哈希/大小/CRC | [SOURCE_MANIFEST.json](SOURCE_MANIFEST.json) | ZIP校验、43原文件逐字节校验、安全路径检查依据 |
| 阅读版 | [LITHIC_V1_产品与开发规划.md](../../LITHIC_V1_产品与开发规划.md) | 629行、55,653字节，原样保留 |

ZIP SHA-256：`861cee1adb74cb575c67af8c14191fbb2792b66741c2e99760b9f339979812d7`

阅读版SHA-256：`3b1ea091c3bad117c208599c949c8799c6825b08ee94dd14b4305cbe38ee97ea`

实际验证：全部条目解压读取与CRC通过；UTF-8、3份JSON解析通过；无目录穿越、绝对/驱动器路径、Windows规范化重名、符号链接、加密条目或越限。逐个解包文件与ZIP条目原字节一致，Git用.gitattributes保留字节。

哈希与CRC证明本次复制/读取完整性，不证明来源的第三方签名或每条产品规则已通过测试。

## 原始计划和规格入口

| 内容 | 原件 |
|---|---|
| V1总计划 | [MASTER_PLAN.md](original-v1/lithic_v1_plan/docs/MASTER_PLAN.md) |
| 24个模块的原始索引/精确依赖 | [module-index.json](original-v1/lithic_v1_plan/docs/module-index.json) |
| M00原计划 | [2026-10-10-m00.md](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m00.md) |
| M01原计划，已冻结工程骨架 | [2026-10-10-m01.md](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m01.md) |
| M02原计划，已冻结项目成本引擎 | [2026-10-10-m02.md](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m02.md) |
| M03原计划，当前授权模块入口 | [2026-10-10-m03.md](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m03.md) |
| 全部M00～M23计划及边界 | [当前路线与逐模块链接](../ROADMAP.md) |
| 130条原验收矩阵 | [ACCEPTANCE_MATRIX.md](original-v1/lithic_v1_plan/docs/ACCEPTANCE_MATRIX.md) |
| 130条原验收JSON | [acceptance-cases.json](original-v1/lithic_v1_plan/docs/acceptance-cases.json) |
| 产品 | [PRODUCT_SPEC.md](original-v1/lithic_v1_plan/docs/specs/PRODUCT_SPEC.md) |
| 领域/计算/日期/状态 | [DOMAIN_CONTRACT.md](original-v1/lithic_v1_plan/docs/specs/DOMAIN_CONTRACT.md) |
| 架构/数据演进 | [ARCHITECTURE.md](original-v1/lithic_v1_plan/docs/specs/ARCHITECTURE.md) |
| 页面/交互 | [UI_SPEC.md](original-v1/lithic_v1_plan/docs/specs/UI_SPEC.md) |
| 备份/恢复 | [BACKUP_SPEC.md](original-v1/lithic_v1_plan/docs/specs/BACKUP_SPEC.md) |
| 通知 | [NOTIFICATION_SPEC.md](original-v1/lithic_v1_plan/docs/specs/NOTIFICATION_SPEC.md) |
| 单模块执行模板 | [EXECUTE_ONE_MODULE.md](original-v1/lithic_v1_plan/docs/templates/EXECUTE_ONE_MODULE.md) |
| 完成报告模板 | [MODULE_REPORT.md](original-v1/lithic_v1_plan/docs/templates/MODULE_REPORT.md) |
| 原平台技术来源 | [SOURCES.md](original-v1/lithic_v1_plan/docs/SOURCES.md) |

原包PLAN_VALIDATION.json只记录2026-10-10制作规划包时的文档检查。其artifact_checks=PASS不是本次验证证据，android_product_tests仍NOT_RUN。

## 原件与当前状态的责任边界

归档下的AGENTS、PROJECT、CURRENT_STATE、DECISIONS是原规划历史输入，保持原件，不覆盖仓库现有记忆。当前指令仍为根AGENTS，当前身份为根PROJECT，唯一实时状态为docs/CURRENT_STATE。

原JSON的24个模块均PLANNED、130案例均NOT_RUN；这只是不可改写的规划快照。M00历史文档检查、M01/M02实际验证及本轮 M03 实际结果分别记录在本仓库docs/ACCEPTANCE_MATRIX；既不修改原案例，也不把文档检查或规则确认当产品通过。

六份核心规格、原DECISIONS与SOURCES全文均与阅读版合并内容一致。新增可用内容为精确模块计划/依赖、130案例及模板。对照说明见[BASELINE_COMPARISON.md](BASELINE_COMPARISON.md)；Q02/Q07 已确认解释、其他建议与待运行测试见[CLARIFICATIONS.md](CLARIFICATIONS.md)。

## 其他来源与授权

- M00补齐工作单授权文档接入及提交/推送；随后用户转交独立验收结论，评审HEAD为59ba290，验收回写基线为77e61b。历史授权不自动授权下一模块。
- M01 实施授权来自当时用户明确指令及实际附件 `E:/CODEX/LITHIC_M01_Codex执行指令.md`（10,106字节，SHA-256 `f4b21192d707a0a23ec7dc4196f48219cc79b60b7dbf1ef924d38957e172c2cc`），见[M01执行契约](M01_CONTRACT.md)。其独立验收评审 HEAD 为 `ca8499e3d3cb2e5bac3ab29a231f399636899c19`，验收回写提交为 `a25a06899bd157988c3a4466d7162d8616fb0249`；均为历史记录，不自动授权下一模块。
- M02 授权及 Q02/Q07 正式确认来自当时实际读取的附件 `E:/CODEX/.codex/attachments/12799a35-02ac-46b5-b856-1a0a3eb28bf2/已粘贴的文本.txt`（9,692 字节，SHA-256 `f13202ff3505ddc644840b7512212861317a3dde8b3ea470cbc2a20427a58bc0`），见[M02执行契约](M02_CONTRACT.md)。起点为上述 M01 验收回写提交，功能分支 `feat/m02-project-cost-engine`；Q02/Q07 解释登记当前 DECISIONS/SPEC_ISSUES，原16组补测保持 NOT_RUN，实际结果另记当前验收登记。M01/M02 附件均未混入原 ZIP/43 原件，不改变原规划来源字节或 manifest。M02独立评审HEAD为 `ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`，验收回写提交为 `0169c76bbf0946a0400b96d00124b3de9aea1cc0`；独立审查未在用户Windows本机重跑Gradle。
- 当前 M03 授权来自本轮会话内联用户消息，没有新的授权附件路径/哈希；精确范围见[M03执行契约](M03_CONTRACT.md)。从上述 M02 验收回写提交与来源分支创建 `feat/m03-subscription-engine`，仅实施纯Kotlin订阅领域及自动测试，不自动进入M04。
- 引用对话ID `6ac3156b-2810-83ec-9b92-2f7b5c8241d0`只补背景；仓库原件与当前用户要求优先。

Q01 资料缺失已关闭；M00～M02 已独立冻结。M03 按本轮另行授权执行，交付后停止等待独立验收；不授权 M04。Q03～Q06 原状态及处理期限不变，16组跨模块NOT_RUN保留，Q02 的 M17/M18 集成验证留待对应模块。

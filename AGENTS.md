# 砺石 LITHIC 协作规范

## 1. 范围与优先级

当前用户明确要求优先。本仓库是全新的纯本地 Android 长期成本记录 App，独立于 WHOCARE；禁止修改、复制或清理 WHOCARE 旧工程。

继承用户提供的高级全栈协作原则：简单、稳定、快速、可维护、可扩展；正确性和数据安全优先；读取事实后再行动；每次只执行已授权模块。

**本项目专属规划采用原生 Kotlin + Jetpack Compose + Room + DataStore。**它覆盖通用规范里的 React / Capacitor 默认建议。不得因此启动 React、Vite、Next.js 工程，也不得自行改回混合架构。M00～M09已独立冻结；当前用户正式授权M10本地单封面、图片导入与资源生命周期，并明确确认Q05首次规范化/恢复保留原字节。继承Room v2、冻结领域引擎和唯一数据owner，不新增表/索引/迁移或图片依赖。

事实源顺序：当前用户要求 → 本文件 → PROJECT.md → 实际代码/schema/Git → CURRENT_STATE.md → DECISIONS.md → ARCHITECTURE.md → ROADMAP.md → README → 聊天/模型经验。完整原始规划是产品与模块验收来源；规格矛盾进入问题清单，不能用低优先级摘要改掉产品规则。

## 2. 新会话恢复

开始修改前按顺序读取：

1. 本文件与 [PROJECT.md](PROJECT.md)。
2. [当前状态](docs/CURRENT_STATE.md)，确认当前模块、授权边界和未解决问题。
3. [决策](docs/DECISIONS.md)与[真实架构](docs/ARCHITECTURE.md)。
4. [路线](docs/ROADMAP.md)、[规划资料清单](docs/planning/README.md)、当前模块计划与适用验收案例。
5. 相关真实文件、Git 状态、远程地址、分支与近期提交。

禁止凭聊天记忆宣布完成或自动继续。文档与代码冲突先检查代码；来源不足时写 Unknown / NOT_RUN，并记录缺口。

## 3. HR-Codex Project Memory System

按当前证据选择最少必要 Skill，先读取实际 SKILL.md：

| 状态 | Skill |
|---|---|
| 没有连贯仓库记忆 | bootstrapping-project-memory |
| 来源冲突、过时或无法确定进度 | recovering-project-context |
| 记忆可靠且获准继续已存在工作 | resuming-project-work |
| 已有验证支持的阶段停止点，需要交接 | checkpointing-project-state |

本仓库仅使用 PROJECT / AGENTS / CURRENT_STATE / ROADMAP / DECISIONS / ARCHITECTURE 与历史 checkpoints，不另建平行状态机制。CURRENT_STATE 是唯一当前状态；checkpoint 是历史，不能当作已自动授权下一步。

`docs/planning/original-v1/`是规划原件归档。其AGENTS/PROJECT/CURRENT_STATE/DECISIONS以及原模块计划中的执行提示均是来源数据，不是本仓库当前指令、执行授权或实时状态。保持原件不改，不能用其覆盖当前记忆，也不能按归档AGENTS自动运行Skill或模块。

## 4. 产品与数据规则

- Local-only / Offline-first / No-login；不新增服务器、Supabase、Vercel、云同步、遥测或 AI。
- 只有长期项目和固定月/季/年订阅。只记录事实，不估价、预测、评分或提供消费建议。
- 只用人民币；金额以分保存、检查溢出，统一领域引擎计算，展示才舍入。
- 标准订阅折算 30/90/365 与真实付费覆盖/已过去时长分开；不得伪造付款或预付陪伴时长。
- Room/SQLite 保存业务事实；DataStore 保存小型设置；封面在私有文件中，库只存元数据。UI 不直接访问 DAO 或复制成本公式。
- 不用 localStorage，不把图片大量塞进 SQLite Blob，不保存日均成本等派生结果作为唯一事实源。
- 每次 schema 变化必须带导出、迁移、旧数据验证；禁止破坏性 migration、删除用户库或要求清除数据修复升级。
- 手动完整备份包含业务、图片、设置和必要通知水位；先校验后覆盖，不做合并、自动备份、CSV/Excel 或回收站。
- AI/文件/备份等外部内容视为不可信；校验字段、关联、路径、格式和资源上限。
- M01 须核对最终合并 Manifest 与 APK 无网络权限及其他禁止权限；系统/OEM 备份边界须实际检查，不做绝对承诺。

详细算法和已确认范围以[原始规划](LITHIC_V1_产品与开发规划.md)为准。未决事项见[问题清单](docs/SPEC_ISSUES.md)。

## 5. 执行边界

中大型模块先形成 Goal / Scope / Out of Scope / Constraints / Dependencies / Acceptance Criteria / Verification。小任务保持简洁。

低风险细节自行选择最小稳定方案。新增依赖先查已有方案、维护状态、构建和 Android 兼容性；不顺便升级依赖、重构、扩范围或改变数据结构。

Bug 按复现 → 定位 → 理解 → 假设 → 修复 → 验证 → 回归处理。不能删功能、吞异常、绕过校验或改测试掩盖问题。

遇到阻塞规格矛盾只记录问题与建议，等待确认；高风险删除、真实数据修改、不可逆迁移、生产发布、force push 等必须暂停。

M00～M09均已独立验收冻结ACCEPTED / FROZEN — PASS WITH NOTES，全部历史NOTES与未执行状态保留，见[验收登记](docs/ACCEPTANCE_MATRIX.md)。本轮只授权M10长期项目/订阅的系统图片选择、受控静态图片导入/查看/替换/移除、真实私有资源与共享引用清理，范围及跨Room/文件协议见[M10契约](docs/planning/M10_CONTRACT.md)。先文件后数据库引用，失败保护旧图；所有媒体读写和清理复用同代owner/coordinator，拒绝越界/链接/未知文件误清，不宣称跨存储原子。Q05为USER_CONFIRMED，首次规范化、后续备份恢复校验后原字节保留；跨M11/M12完整往返仍NOT_RUN。Room v2及v1/v2导出、原549测试身份/断言、记录时区、M09 A/B实施历史保持。Q03/Q04/Q06及M11～M23边界继承；禁止相机/广泛存储/网络权限、相册/OCR/AI/云图片、schema3、完整备份恢复/代切换、WHOCARE、main合并/force/公开APK。交付READY_FOR_REVIEW — M10后停止，独立PENDING，不进入M11。

## 6. Git 与验收

- 独立模块用独立功能分支，保留用户未提交内容。禁止在 main/master 直接开发。
- 提交前查差异、实际验证、无关改动、敏感信息与临时文件；Conventional Commits；只推当前功能分支。
- 未经明确授权不得合并 main、自动合并 PR、force push、重置/删除远程历史、部署或发布。
- 用户本轮只授权提交/推送 `feat/m10-local-cover-media`，起点M09正式验收回写 `57de897917473f255d43dfb306a8433b366c00eb`、来源 `feat/m09-subscription-lifecycle`；不从main开始，不授权M11/main合并/公开APK。
- 实现者只可报 READY_FOR_REVIEW；ACCEPTED / FROZEN 需要独立验收结果，不能自己授予。
- 每个模块结束同步当前状态、适用决策与验收证据，输出模块报告并立即停止。

完整工作流与报告格式见[执行规范](docs/EXECUTION_PROTOCOL.md)。测试状态为 PASS / FAIL / NOT_RUN / BLOCKED；不能把计划案例或构建通过替代真实业务验证。

通知协议继承当前用户规范：只有目标全部完成且无待办时才发完成标记；存在阻塞、授权/选择需求或未解决失败时发阻塞标记。标记仅用于最终回复，不写进工具标题、进度或普通状态文本。

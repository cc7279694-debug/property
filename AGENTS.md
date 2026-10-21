<!-- M18 Android补验已完成：READY_FOR_REVIEW，17 Android PASS/10图；等待独立复验，不授权M19。 -->
# 砺石 LITHIC 协作规范

## 1. 范围与优先级

当前用户明确要求优先。本仓库是全新的纯本地 Android 长期成本记录 App，独立于 WHOCARE；禁止修改、复制或清理 WHOCARE 旧工程。

继承用户提供的高级全栈协作原则：简单、稳定、快速、可维护、可扩展；正确性和数据安全优先；读取事实后再行动；每次只执行已授权模块。

**本项目专属规划采用原生 Kotlin + Jetpack Compose + Room + DataStore。**它覆盖通用React/Capacitor默认建议。M00～M09已冻结，M11-A由用户转交ChatGPT独立结论ACCEPTED/FROZEN—PASS WITH NOTES（仅备份核心，来源79771fcf5978a938d240d517dd4dacb1d21021a6）。M11现由用户转交ChatGPT独立结论ACCEPTED / FROZEN — PASS WITH NOTES，评审635e33a57c66c286fa5c19f009a5565a1e70b8e2。M12已由用户转交独立验收冻结（评审e34187632468c9c3aaf7bb5d04d30a26320a18b0）。M14已由用户转交ChatGPT独立验收冻结PASS WITH NOTES；M15已由用户转交ChatGPT独立结论ACCEPTED / FROZEN — PASS WITH NOTES（评审`ec0b7286b4ca41f0eac84fd4f0b6885c27c5a473`），M16已由用户转交ChatGPT独立代码及证据结论ACCEPTED / FROZEN — PASS WITH NOTES（评审`d8ce0a8d602c2e9e544662e4b5bf202df5812aef`），冻结替换链/以旧换新及Room schema3/backup format2；[M16契约](docs/planning/M16_CONTRACT.md)保留冻结实施来源；[M17单项目图表](docs/planning/M17_CONTRACT.md)已由用户转交ChatGPT独立结论ACCEPTED / FROZEN — PASS WITH NOTES（评审`54360aa0e61779c7efa62b527f8901778a0845a2`），当前M18用户已明确授权；[M15契约](docs/planning/M15_CONTRACT.md)保留原交付来源，不构成再次开发或设备授权；M13-T01～T05独立PASS WITH NOTES，T06 NOT_RUN / DEFERRED、G1未整体通过，用户明确允许此前提下继续，不登记完整M13冻结，不合并M10 Worker/Stage V2/JNI或独立UI-C1页面。M10 PENDING / DEFERRED，历史FAIL/UNKNOWN/NOT_RUN/NOTES和设备现场保持。

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

M17保持ACCEPTED / FROZEN — PASS WITH NOTES及原证据。本轮用户M18补验授权从评审`6dfc4a6c79918d7e6f2e7c40d8b1c9fc2b5943c1`新建唯一隔离API36 `Lithic_M18_API36_20261021 / emulator-5692`，最终17 Android PASS、10图；仅两份androidTest定位/合成截图修正和测试APK构建，生产App字节不变，原174 JVM/Lint0/23不重跑，首次14/3保留。见[补验报告](docs/evidence/M18-Android-verification.md)。旧5672/5686/H2/C2/M10及其他设备操作0，新5692结束保持在线；不构成后续操作授权。现为READY_FOR_REVIEW，待ChatGPT独立复验，不自行冻结/进入M19。Room3/backup2/冻结引擎/M16保护/恢复及媒体协议不改。plain Application/隔离ComponentActivity、LocalDensity200%、注入时钟、受控文件往返不扩大为MainActivity、系统字号/TalkBack/真机/真午夜/新SAF。Q02完整跨模块与全部历史NOTES/UNKNOWN/NOT_RUN保持。下方M11/M12为继承安全约束和历史说明。

M11-B只在统一协调器下复制已证明归属的登记媒体；保持原字节，缺失/损坏/身份或完整性不明拒绝完整成功。空媒体合法。完整归档先在私有目录校验，再写本次系统保存位置，关闭并读回校验后才报告成功；取消和失败明确反馈。只用合成验证，不修改Room schema、冻结业务算法/M09语义、备份格式编号，恢复/代切换现按用户明确M12授权与[M12契约](docs/planning/M12_CONTRACT.md)实施；不做CSV/云/自动备份/密码，不进入下一模块。设备实验只允许经确认的全新隔离环境；H2/C2/M10保留现场绝对禁止操作。原规划/案例和全部历史NOTES不改；实现/编译通过不等于Android或正式M11冻结。已登记JPEG/PNG备份核验原字节长度/SHA、容器和实际尺寸，不以缺少M10深度解码为由拒绝所有JPEG；Android可读性不冒称严格熵验证。合法标准PNG编码不再人为收窄。未知或不匹配仍拒绝整份导出。

## 6. Git 与验收

- 独立模块用独立功能分支，保留用户未提交内容。禁止在 main/master 直接开发。
- 提交前查差异、实际验证、无关改动、敏感信息与临时文件；Conventional Commits；只推当前功能分支。
- 未经明确授权不得合并 main、自动合并 PR、force push、重置/删除远程历史、部署或发布。
- 本次补验来源`6dfc4a6c79918d7e6f2e7c40d8b1c9fc2b5943c1`；只提交/推送`codex/m18-total-cost-history`必要测试修正/证据/当前登记。M10 PENDING / DEFERRED及三JPEG历史FAIL、M13-T06 NOT_RUN / DEFERRED、G1未整体通过和全部历史NOTES保持；原保护设备不访问。独立复验前停止；稳定签名、真实录入、发布、M19与main合并未授权。
- 实现者只可报 READY_FOR_REVIEW；ACCEPTED / FROZEN 需要独立验收结果，不能自己授予。
- 每个模块结束同步当前状态、适用决策与验收证据，输出模块报告并立即停止。

完整工作流与报告格式见[执行规范](docs/EXECUTION_PROTOCOL.md)。测试状态为 PASS / FAIL / NOT_RUN / BLOCKED；不能把计划案例或构建通过替代真实业务验证。

通知协议继承当前用户规范：只有目标全部完成且无待办时才发完成标记；存在阻塞、授权/选择需求或未解决失败时发阻塞标记。标记仅用于最终回复，不写进工具标题、进度或普通状态文本。

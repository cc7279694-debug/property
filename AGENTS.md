<!-- 2026-10-22：M20已按用户转交的独立结论限定冻结；第二次平台收口BLOCKED。当前仅文档登记，末节为最新停止边界。 -->
# 砺石 LITHIC 协作规范

## 1. 范围与优先级

当前用户明确要求优先。本仓库是全新的纯本地 Android 长期成本记录 App，独立于 WHOCARE；禁止修改、复制或清理 WHOCARE 旧工程。

继承用户提供的高级全栈协作原则：简单、稳定、快速、可维护、可扩展；正确性和数据安全优先；读取事实后再行动；每次只执行已授权模块。

**本项目专属规划采用原生 Kotlin + Jetpack Compose + Room + DataStore。**它覆盖通用React/Capacitor默认建议。M00～M09已冻结，M11-A由用户转交ChatGPT独立结论ACCEPTED/FROZEN—PASS WITH NOTES（仅备份核心，来源79771fcf5978a938d240d517dd4dacb1d21021a6）。M11现由用户转交ChatGPT独立结论ACCEPTED / FROZEN — PASS WITH NOTES，评审635e33a57c66c286fa5c19f009a5565a1e70b8e2。M12已由用户转交独立验收冻结（评审e34187632468c9c3aaf7bb5d04d30a26320a18b0）。M14已由用户转交ChatGPT独立验收冻结PASS WITH NOTES；M15已由用户转交ChatGPT独立结论ACCEPTED / FROZEN — PASS WITH NOTES（评审`ec0b7286b4ca41f0eac84fd4f0b6885c27c5a473`），M16已由用户转交ChatGPT独立代码及证据结论ACCEPTED / FROZEN — PASS WITH NOTES（评审`d8ce0a8d602c2e9e544662e4b5bf202df5812aef`），冻结替换链/以旧换新及Room schema3/backup format2；[M16契约](docs/planning/M16_CONTRACT.md)保留冻结实施来源；[M17单项目图表](docs/planning/M17_CONTRACT.md)已由用户转交ChatGPT独立结论ACCEPTED / FROZEN — PASS WITH NOTES（评审`54360aa0e61779c7efa62b527f8901778a0845a2`），M18已由用户转交ChatGPT独立源码、Android原始执行及截图复验结论ACCEPTED / FROZEN — PASS WITH NOTES（评审`87211c8a3ed8982cdf7f46080afce10aeda4dda7`）；M19现由用户转交ChatGPT独立源码/原始证据/截图/设备身份验收结论ACCEPTED / FROZEN — PASS WITH NOTES（评审`21d6230543ac9c0c491d8da590d230396952f571`），仅冻结类别分析、真实资金年份、客观年度回顾及对应只读完整事实映射；[M15契约](docs/planning/M15_CONTRACT.md)保留原交付来源，不构成再次开发或设备授权；M13-T01～T05独立PASS WITH NOTES，T06 NOT_RUN / DEFERRED、G1未整体通过，用户明确允许此前提下继续，不登记完整M13冻结，不合并M10 Worker/Stage V2/JNI或独立UI-C1页面。M10 PENDING / DEFERRED，历史FAIL/UNKNOWN/NOT_RUN/NOTES和设备现场保持。

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

M17及此前冻结结论保持。M18已由用户正式转交ChatGPT独立复验结论 **ACCEPTED / FROZEN — PASS WITH NOTES**，评审`87211c8a3ed8982cdf7f46080afce10aeda4dda7`；按[正式登记](docs/ACCEPTANCE_MATRIX.md#m18正式冻结登记2026-10-21)限定认可T01～T05、此前174 JVM PASS、17 Android PASS、Lint0错误23警告及10张隔离截图。首次14 PASS/3 FAIL、启动外层回执NOT_CAPTURED、原报告/结果/checkpoint与所有历史NOTES/UNKNOWN/NOT_RUN保持。冻结只读总成本历史/汇总刷新/精确选日事件，不改Room3/backup2/冻结引擎/M16保护/恢复及媒体协议。API36/x86_64合成、plain Application/隔离ComponentActivity、LocalDensity200%、注入跨日、受控文件往返不扩大为MainActivity、系统字号/TalkBack、真机、真实午夜或新SAF/故障恢复。Q02仅已验证模块部分认可，16组完整跨模块门禁不关闭。M18当次仅登记文档，未重跑测试/构建/核验APK或操作设备；原历史记录不变。当前M19授权与执行见末节；下方M11/M12为继承安全约束和历史说明。

M11-B只在统一协调器下复制已证明归属的登记媒体；保持原字节，缺失/损坏/身份或完整性不明拒绝完整成功。空媒体合法。完整归档先在私有目录校验，再写本次系统保存位置，关闭并读回校验后才报告成功；取消和失败明确反馈。只用合成验证，不修改Room schema、冻结业务算法/M09语义、备份格式编号，恢复/代切换现按用户明确M12授权与[M12契约](docs/planning/M12_CONTRACT.md)实施；不做CSV/云/自动备份/密码，不进入下一模块。设备实验只允许经确认的全新隔离环境；H2/C2/M10保留现场绝对禁止操作。原规划/案例和全部历史NOTES不改；实现/编译通过不等于Android或正式M11冻结。已登记JPEG/PNG备份核验原字节长度/SHA、容器和实际尺寸，不以缺少M10深度解码为由拒绝所有JPEG；Android可读性不冒称严格熵验证。合法标准PNG编码不再人为收窄。未知或不匹配仍拒绝整份导出。

## 6. Git 与验收

- 独立模块用独立功能分支，保留用户未提交内容。禁止在 main/master 直接开发。
- 提交前查差异、实际验证、无关改动、敏感信息与临时文件；Conventional Commits；只推当前功能分支。
- 未经明确授权不得合并 main、自动合并 PR、force push、重置/删除远程历史、部署或发布。
- 本轮仅提交/推送`codex/m19-category-annual-review`的必要冻结登记文档，评审来源`21d6230543ac9c0c491d8da590d230396952f571`。独立结论M19 ACCEPTED / FROZEN — PASS WITH NOTES；M10延期/三JPEG历史FAIL、M13-T06延期/G1、G2未自动通过、Q02完整16组与全部历史NOTES保持。不改产品/测试、不构建、不操作或清理任何设备，不合并main、不发布、不启动M20。
- 实现者只可报 READY_FOR_REVIEW；ACCEPTED / FROZEN 需要独立验收结果，不能自己授予。
- 每个模块结束同步当前状态、适用决策与验收证据，输出模块报告并立即停止。

完整工作流与报告格式见[执行规范](docs/EXECUTION_PROTOCOL.md)。测试状态为 PASS / FAIL / NOT_RUN / BLOCKED；不能把计划案例或构建通过替代真实业务验证。

通知协议继承当前用户规范：只有目标全部完成且无待办时才发完成标记；存在阻塞、授权/选择需求或未解决失败时发阻塞标记。标记仅用于最终回复，不写进工具标题、进度或普通状态文本。

## M19 当前正式冻结登记

用户2026-10-22正式转交ChatGPT对`21d6230543ac9c0c491d8da590d230396952f571`的独立验收，结论 **ACCEPTED / FROZEN — PASS WITH NOTES**，来源与限定范围见[正式登记](docs/ACCEPTANCE_MATRIX.md#m19正式冻结登记2026-10-22)。M19-T01～T06、此前109 JVM/29 Android PASS、Lint0错误23警告及8张真实隔离截图获认可；原[M19契约](docs/planning/M19_CONTRACT.md)/[报告](docs/evidence/M19-verification.md)/结果/checkpoint保持历史原样。类别分析、真实资金年份、客观年度回顾和只读完整事实映射冻结，Room3/backup2/冻结算法保持。

本轮只登记文档和Git核查/提交/推送；未重新执行测试、构建、APK或设备核验，不把原5692结束在线当作本次观察。原导航/领域RED、初次Android测试编译失败、刷新提示Android RED及全部历史FAIL/UNKNOWN/NOT_RUN/NOTES保留。原API36/x86_64合成/隔离ComponentActivity、LocalDensity360dp/200%、注入时钟、受控文件恢复，不扩大为MainActivity、系统字体/TalkBack、真实午夜、真机/API26/OEM、Release或故障覆盖；长期性能、最坏取消时延和进程死亡选择恢复等仍NOT_RUN。M10 PENDING / DEFERRED和三JPEG历史FAIL、M13-T06 NOT_RUN / DEFERRED、G1未整体通过保持，G2不自动通过，Q02完整16组跨模块门禁不关闭。提交推送后停止，M20未授权；旧设备与全部历史现场禁止操作或清理。

## M20 当前明确授权覆盖

用户本轮正式授权[M20契约](docs/planning/M20_CONTRACT.md)的实现、迁移、备份兼容及定向验证，覆盖上方M19登记时“不启动M20”的阶段限制。只在codex/m20-renewal-notifications，从33ae4f4建立候选；明确允许schema3→4和backup2→3、WorkManager2.12.0与通知权限，不改冻结业务算法。仅核验原5692在线同实例后新M20合成根；禁止其他设备/个人账本/新建重启停止/清数据。M19等冻结及M10延期、M13-T06延期、G1/G2、Q04 PROPOSED及全部历史NOTES保持；READY_FOR_REVIEW — M20后停止，M21未授权。

### M20本轮候选交付边界

实际238定向JVM PASS（88新＋150回归）、Lint0错误25警告及Debug App/Test构建成功；Room4九表、format3与旧格式/proof兼容是本轮候选变更，未自行冻结。原5692锁定进程身份未取得，仅只读CIM、0 ADB/设备操作；新增22项Android/全部截图NOT_RUN，后续不可把编译或旧M19设备证据当成本轮迁移/通知/权限通过。M20 READY_FOR_REVIEW后等待ChatGPT独立验收，不自动M21。完整依据见[报告](docs/evidence/M20-verification.md)和[当前状态](docs/CURRENT_STATE.md)。

### M20低资源补验授权及停止点

用户另行明确批准一台全新隔离API36/x86_64、2048MB/2核/headless/no-snapshot模拟器及本次结束后正常关闭，覆盖前段候选时的“不新建”限制；仅执行`Lithic_M20_API36_20261022 / emulator-5704`，未操作原5692/5672/5686/H2/C2/M10或私人数据。原22项最终22 PASS（首轮16＋Context修正后Worker6），首轮21 PASS/1 FAIL保留；额外3方法最新2 PASS/1 FAIL，真实平台通知窗口谓词两次FAIL和第二次任务显式Owner/periodic收口UNKNOWN保留。所有测试输入、独立截图、真实系统发布子证据及限制见[补验报告](docs/evidence/M20-Android-verification.md)，不把系统发布或模拟器退出冒充整方法/应用收口PASS。

本次只改测试卫生、测试Context及取证代码/文档，未改产品代码。原238 JVM未重跑，最终Lint0错误25警告、Test构建成功；App原SHA不变。新5704已正常退出，原进程/端口结束，AVD和UNKNOWN现场保留。此后不得自行重启、恢复或清理；原候选报告与所有历史FAIL/UNKNOWN/NOT_RUN/NOTES保持。M20仍READY_FOR_REVIEW/独立PENDING，提交推送当前M20分支后停止；不自行冻结，不关闭Q04/G1/G2，不进入M21。

### M20最后一次定向收口停止点

用户另行授权仅原M20/5704冷启动一次，对固定第二run`eb140516-9807-45be-b01b-781a9b76f6c0`做精确收口，覆盖上一停止点的本次重启限制。实际启动身份及旧APK核对通过，但冷启动出现正常App进程与新默认generation，未能证明无新Worker，已停止；没有安装/运行收口测试或取消任务/通知、重开账本Owner。仅正常关闭本轮原实例，AVD和未知现场保留。收口BLOCKED，原22 PASS、平台FAIL及UNKNOWN不改，详见[报告](docs/evidence/M20-Platform-closure.md)。本次没有产品或测试源码变更；提交推送后停止，任何再次启动、取消、恢复或清理须另行明确授权，不冻结M20或进入M21。

## M20当前正式限定冻结登记

用户正式转交ChatGPT对`d3776e0b2852428911ad0f09296680046211b32d`的独立验收：**M20 — ACCEPTED / FROZEN — PASS WITH NOTES**。仅冻结续费Planner、权限/偏好、有限幂等回执、WorkManager集成及Room schema v4、backup format v3兼容实现；M20-T01～T06、此前238 JVM/原22 Android最终PASS、Lint0错误25警告获限定认可，见[正式登记](docs/ACCEPTANCE_MATRIX.md#m20正式限定冻结登记2026-10-22)。上方候选/补验/收口段落均为历史授权与观察，不再表示产品尚待冻结。

第二次平台收口依旧BLOCKED；25个唯一方法最新24 PASS/1 FAIL、首轮21 PASS/1 FAIL、真实平台JUnit两次FAIL及SystemUI截图全部保持。第二次周期任务、OS Job、指定通知及Owner收口UNKNOWN，冷启动App进程/默认generation出现及其触发来源UNKNOWN的边界不改。`Lithic_M20_API36_20261022 / emulator-5704`为**保留 / 隔离测试现场**，未经重新明确授权不得直接复用、删除或清理；不把模拟器退出或独立冻结当作应用收口证明。

M10 PENDING / DEFERRED/三JPEG历史FAIL、M13-T06 NOT_RUN / DEFERRED、G1/G2未整体通过、Q04 PROPOSED、G3未通过及全部历史NOTES/NOT_RUN保持；不保证OEM后台准点或exactly-once。当前只提交推送M20分支必要登记文档，不改源码、不构建、不测试、不核验或操作设备。完成后停止，不合并main，不进入M21。

## M21 当前明确授权覆盖（2026-10-22）

本节依据用户M21完整授权，为当前执行边界；此前M20“未授权M21/仅冻结回写”文字保留原时点。起点M20冻结`3e8f5fdef9853ada726b037cc8559e2117d24da9`，只推`codex/m21-milestone-reminders`。Q03 USER_CONFIRMED：开始日1、日节点N-1、周年日历年；订阅实际累计付费有效日只复用冻结M03。候选Room5仅新增typed水位表，backup4独立10表DTO及旧格式/proof严格升级；内部指针、媒体原字节、M16与冻结引擎不改。水位不冒充成功Receipt；同日稳定摘要、批量Room回执及有限崩溃重试，点击仍经dirty/pending/恢复守卫。

本轮只准产品代码、定向离线测试、Lint、Debug App/Test构建和Android测试源码准备；设备操作必须为0，真实迁移/通知/点击/恢复/系统重启均NOT_RUN。不查询、创建、启动、关闭或清理任何AVD、App、系统任务通知或私人数据，尤其M20/5704、5692、5672、5686、H2、C2、M10。M20限定冻结及额外平台FAIL/收口BLOCKED/UNKNOWN、M10延期三JPEG历史FAIL、M13-T06延期、Q04 PROPOSED、G1/G2/G3与全部NOTES保留。交付只可READY_FOR_REVIEW — M21，等待独立验收，不冻结、不进入M22。见[契约](docs/planning/M21_CONTRACT.md)与[验证](docs/evidence/M21-verification.md)。

### M21 Android补验当前授权（2026-10-23）

用户另行明确授权从`6c937ed7354b77d598bdeb55718a39708f572ee8`在当前M21分支新建、启动、结束一台低资源全新隔离API36/x86_64，仅执行既有31方法和必要Compose截图。上段“设备操作0”保持上轮历史；本轮独立根`E:/CODEX/.codex/research-env/lithic-m21-android-20261023`，新AVD`Lithic_M21_API36_20261023`、serial`emulator-5712`，全部ADB显式定向。先独立TestDriver，再逐类；plain Application隔离Runner，不启动MainActivity默认账本/生产周期队列，不操作任何历史设备。现有工具自动将2048MB请求提高为2560MB，实际值必须如实登记。禁止重跑395 JVM或扩充历史测试；失败原件保留，定向处理，不自行冻结/进入M22。结果见[M21 Android报告](docs/evidence/M21-Android-verification.md)。

本次补验停止点：31唯一方法首轮31 PASS、重跑0，4张真实隔离截图，App不变；新5712创建/启动/正常退出各1，进程/端口结束，AVD保留。13个已关闭业务测试根移除，15个未登记零字节缓存`.lck`保留，未声称全部缓存清空。真实系统发布/点击/重启及现有31项未含的日期子边界仍NOT_RUN。提交推送后停止等待独立复验，不能据本段再次启动、清理或复用任何设备，不自行冻结或进入M22。

# 决策记录

记录类型：User-confirmed（用户已确认）、Inherited planning default（继承规划工程解释）、Local operating rule（本轮执行规范）、Proposed（未接受建议）、Superseded（已取代）。建议不能自动变成已接受产品需求。

## 2026-10-11 — 继承纯本地长期成本产品边界

**状态：User-confirmed；来源为阅读版 U01～U16。**

### Decision
新建 LITHIC，不在 WHOCARE 上删除式重构。核心为长期项目和月/季/年固定周期订阅，人民币、事实、陪伴时间和日均成本；无账户/服务器/云同步。三入口现在/回看/设置。完整手动备份与覆盖恢复、单封面、名称搜索、双类别、历史/复盘/图表/年度和两种提醒沿用原范围。

### Context
原产品讨论已形成明确范围；当前用户只授权 M00。

### Alternatives
复用 WHOCARE 扩展资产/记账系统，或引入云架构。

### Reason
保持低维护、无网络核心、长时间事实可保存和简单体验。

### Consequences
不做记账/预算/投资、评分/建议、预测/估价、AI、打卡、发票保修、相册日记、CSV/Excel、回收站、订阅日历；新增这些必须重新确认产品方向。

## 2026-10-11 — 原生 Android 与统一本地领域架构

**状态：Inherited planning default；来源为阅读版 A01～A08、D11/D12。**

### Decision
Kotlin、Compose、Navigation 3、Room、DataStore、Coroutines/Flow、私有文件、SAF、本地通知/WorkManager；单 :app、按职责分包、轻量构造注入。统一领域引擎与 Repository，UI 不访问 DAO 或重算公式。

### Context
专属规划写明原生路线；用户通用规范的 React/Capacitor 是默认，允许专属明确要求覆盖。

### Alternatives
React/Capacitor、Next.js 或多工程模块/完整 DI 框架。

### Reason
沿用已确定路线，不引入无关迁移与复杂度。

### Consequences
M00 不建工程。工具链版本、SDK 与兼容性留 M01 实际核查后锁定，不在本轮编造版本；没有任何构建验证结论。

## 2026-10-11 — 计算与历史语义继承，歧义另列

**状态：User-confirmed + Inherited planning default，具体区分以阅读版 U/D 与 C01～C14 为准。**

### Decision
保留净投入/持有天数、真实事件、分期只记已付、负成本、结束后结算；保持月30/季90/年365标准折算。原规划工程解释包括首/末自然日、固定业务时区、日末总线、订阅真实覆盖、未来已付生效、状态派生和动态重算。

### Context
当前日均参考不是今日扣款；标准已购时长不等于实际已过去时间。

### Alternatives
按银行卡扣款日展示全部生活支出、预测未付金额、按真实31天改月付分母。

### Reason
遵循原范围，避免多个页面或历史口径漂移。

### Consequences
金额分/BigDecimal、可注入 BusinessClock、派生数据不持久化为唯一事实。公式观察日、里程碑首日和提醒边界有待澄清，见 [Q02～Q04](SPEC_ISSUES.md)，本记录不替其作新决定。

## 2026-10-11 — 完整手动备份与安全覆盖恢复

**状态：User-confirmed（手动/完整/覆盖/无加密）；Inherited planning default（B02～B08 技术方案）。**

### Decision
逻辑快照 ZIP 包含业务数据、媒体、设置与必要水位；先校验、后激活完整新数据集。schema/format 各自版本化。系统备份显式排除，实际 OEM 边界如实报告。

### Context
图片、SQLite 和 DataStore 不是同一事务资源，先清空再导入会危及长期数据。

### Alternatives
仅复制运行中的 .db、先清空、合并恢复、定时备份。

### Reason
承载真实数据前必须可完整恢复，失败保持旧或新完整数据集。

### Consequences
M11/M12 提前到核心版；临时快照和短暂回退不是用户自动备份。图片字节往返与持久回退元数据需后续澄清/设计验证（Q05/Q06），本轮不写存储代码。

## 2026-10-11 — 仓库记忆、独立模块与验收权

**状态：Local operating rule；来源为当前用户工作单/协作规范及阅读版第7/9节。**

### Decision
使用 HR-Codex Project Memory System 分工：稳定身份 PROJECT、协作 AGENTS、唯一当前状态 CURRENT_STATE、路线 ROADMAP、决策 DECISIONS、实际架构 ARCHITECTURE、历史 checkpoints。只执行获准模块，功能分支提交推送，独立验收后再授权下一模块。

### Context
仓库是长期记忆，聊天是临时上下文；M00 的目的正是建立可恢复状态。

### Alternatives
依赖单聊天、并行维护多套状态、实现者自行 ACCEPTED 后连续开发。

### Reason
控制范围与上下文漂移，让验证和授权可追溯。

### Consequences
本轮用 Bootstrap；验证停止点用 Checkpoint。M00 可以提交问题供评审，但缺资料时不能自封完整 FROZEN；本地子代理审查不等于 ChatGPT 独立验收。

## 2026-10-11 — 原始阅读版保留与缺失资料阻塞

**状态：历史Local operating rule；Q01已由下述补齐记录关闭，保留当时缺资料背景。产品规格建议未自行接受。**

### Decision
原始阅读版按原字节保留，登记 SHA-256；仅整理可读概要，不伪造完整包的24计划/130验收案例。

### Context
本地只有阅读版；引用对话有 ZIP 链接但没有可读取附件。当前缺原 M00 计划、详细验收矩阵和执行模板。

### Alternatives
根据摘要重建并声称与原包一致，或假定遗漏没有影响。

### Reason
Inspect before infer / Evidence before assertion。

### Consequences
原件接入是正式规划冻结与 M01 执行前的闸门；以 [SPEC_ISSUES.md](SPEC_ISSUES.md)登记，不自行更改已确认规则。

## 2026-10-11 — 补齐原规划包，保持当前记忆与原件职责

**状态：Local operating rule；来源为当前用户M00补齐工作单及已校验原ZIP。**

### Decision
完整保存ZIP和43解包原件在docs/planning/original-v1；保留原相对目录与字节，当前根AGENTS/PROJECT和docs/CURRENT_STATE不被旧原件覆盖。原130案例NOT_RUN、24模块PLANNED均不改写。

### Context
Q01此前因缺资料阻塞，现实际附件已取得。用户转述独立审阅认可现有文档/Git基本符合但未冻结；当前只授权补齐M00和现分支提交/推送。

### Alternatives
以旧状态覆盖当前状态、修改原案例结果、直接运行下一模块。

### Reason
区分来源、实时进度、实际文档验证和独立验收；保留可追溯证据。

### Consequences
Q01关闭；ROADMAP按原计划精确依赖校对。Q03订阅首日已有M03-T02依据，剩余长期日节点待确认；Q02/Q04/Q05保持建议，Q06留对应工程模块；Q07记录模板适用性。完整冻结仍等待独立验收，未进入M01。

### 原规划默认解释的继承对照

以下D01～D15均来自[原DECISIONS](planning/original-v1/lithic_v1_plan/docs/DECISIONS.md)第21～36行的“本轮补充的默认解释”，指原规划该轮，不能声称是用户已逐条单独确认，也不是当前补齐新加功能。

| 原ID | 继承的工程默认摘要 |
|---|---|
| D01 | 主指标是已录入项目的混合日均参考 |
| D02 | 项目自然日含首末日、日末排除结束、固定记录时区 |
| D03 | 金额/日期集中计算，舍入仅展示 |
| D04 | 计时结束与真实款项结算分离 |
| D05 | 折抵是非现金例外，非银行卡现金流水 |
| D06 | 标准时长叫折算，里程碑用实际有效日期 |
| D07 | 到期/生效/取消续费与当前价分离 |
| D08 | 不重叠周期，不自动补差/退款/免费管理 |
| D09 | 按当前类别回溯重算，无双时间审计 |
| D10 | 核心真实数据前具备安全恢复、逻辑快照 |
| D11 | 单app、24交付模块不是24工程模块 |
| D12 | 稳定身份签名、系统备份排除、无网络权限需验证 |
| D13 | M00～M13先形成安全核心，后续分析保留 |
| D14 | 通知尽力送达，不自动恢复系统权限 |
| D15 | 暂停独立分组，不伪造未知/未来付款 |

存在歧义处继续以[问题清单](SPEC_ISSUES.md)和[澄清建议](planning/CLARIFICATIONS.md)列明待确认边界，不用此继承对照替代验收批准。

## 2026-10-11 — M01 工具链、应用身份与离线空壳

**状态：User-authorized engineering implementation；来源为本轮用户 M01 指令及实际附件。**

### Decision
从已冻结 M00 的验收回写基线 77e61b 创建 M01 功能分支；建立单 :app、单 Activity、Compose/Nav3 空壳。正式身份 com.cooper.lithic，Debug 后缀 .debug 与明确 Debug 名称；只用本地 Debug 签名。锁定稳定兼容工具链，细节见[TOOLCHAIN_LOCK](TOOLCHAIN_LOCK.md)，后续库按模块另行核查，不提前集成。

### Context
用户已明确授权 M01，取代当前阶段的 M00 禁止启动工程限制；M00验收历史及其他安全规则继续有效。已有 JDK/SDK/Gradle 可复用，开发机联网下载依赖不等于 App 需要网络。指定中文工作目录触发 AGP Windows 路径预检查。

### Alternatives
更换全局 JDK、修改其他工程、迁移工作目录、引入混合架构或提前建立业务框架。

### Reason
在用户指定目录用项目级路径选项和真实构建验证，复用已检查环境，保持范围最小；固定身份、禁止非必要权限并核查最终 APK，避免仅凭源文件宣称通过。

### Consequences
保留临时工程图标，不声称品牌已定；无网络客户端、业务数据库或手动备份。系统备份排除配置与 OEM 行为验证分开；M01完整备份往返不适用且未实现。Q02～Q07期限不变，M02须独立验收后另行授权，不自动继续。

## 后续新增/变更格式

每条按以上日期/状态/Decision/Context/Alternatives/Reason/Consequences格式记录，增加来源与授权证据；列出受影响模块、schema、format、图表和验证案例。取代决定保留旧记录标 Superseded；不能以新想法擅自改整个 Roadmap。

# 决策记录

## 2026-10-20 — M10延期，M11-A从冻结M09隔离先行

**状态：User-confirmed；来源：用户本轮“暂停M10，启动M11-A备份导出核心”指令。**

用户将M10设为PENDING / DEFERRED，保留三个JPEG历史FAIL、全部UNKNOWN/NOT_RUN/NOTES、候选分支和设备现场；不清理、重跑或合并。M11-A从M09冻结回写`57de897917473f255d43dfb306a8433b366c00eb`独立实现业务/设置一致快照、format v1和文件完整性核心，不带入M10 Worker/Stage V2。

这是对原M11依赖顺序的有限例外，不改变原规划或降低完整备份标准。正式媒体未集成时完整导出必须拒绝完成；不开放正式成功入口，不将核心文件当作完整备份。完整M11仍待媒体/SAF/Android验证及独立验收，M12未授权；Room v2、冻结算法、业务和UI不变。

Q05沿用用户M10已确认的首次规范化、后续导出/恢复校验后原字节保留；不因代码从M09隔离而退回PROPOSED。确认来源见[当前问题清单](SPEC_ISSUES.md)的锁定M10引用，完整跨模块往返仍NOT_RUN，不由M11-A核心测试关闭。

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

## 2026-10-11 — Q02 历史观察日与结束后结算

**状态：USER_CONFIRMED；来源为本轮用户 M02 授权附件第3节，实施和验证结果另行登记。**

### Decision
长期项目以观察日 `asOf` 判断分母和日末贡献：`asOf < startedOn` 为尚未开始，无有效日均成本且不贡献历史总成本；`startedOn <= asOf < endedOn` 使用开始日至观察日的自然日数（含开始日）；`asOf >= endedOn` 固定完整持有自然日数（含结束日）。未结束项目截至观察日计时。结束日当天仍计入持有天数，但该日日末已退出总成本。仅纳入 `occurredOn <= asOf` 的金额事件；结束后真实尾款、分期、退款可以改变结算结果，不能延长持有天数或重新活跃。

### Context
原 DOMAIN C03/C04/C13 存在已结束快照回看结束前日期的公式歧义。M01 已独立验收冻结；用户在本轮明确授权 M02，并正式确认此解释。来源附件实际路径 `E:/CODEX/.codex/attachments/12799a35-02ac-46b5-b856-1a0a3eb28bf2/已粘贴的文本.txt`，9,692 字节，SHA-256 `f13202ff3505ddc644840b7512212861317a3dde8b3ea470cbc2a20427a58bc0`；详见[M02 契约](planning/M02_CONTRACT.md)。

### Alternatives
仅根据快照已有结束日期，在结束前的历史观察日也使用最终天数；或结束后继续增加分母、把结算重新算作活跃持有。

### Reason
用户已正式明确历史分支，保持真实事件日期和结束日日末语义，避免未来结束或后续结算改写更早事实。

### Consequences
2026-01-01 开始、投入 1000 元、01-10 结束：01-02 为 2 天/500 元每天，01-09 为 9 天/1000÷9，01-10 为 10 天/100 元每天且日末贡献为零；01-20 收回 200 元时，01-19 仍为 100 元每天，01-20 更新为 80 元每天，持有固定 10 天。M02 落实 M02-T01～T04 与 Q02-V01～V03 的领域断言，M17/M18 仍须完成各自视图/总线集成验证。确认规则不等于这些测试已经 PASS；原归档规格和 130 案例不改写。取代 Q02 的未确认建议，Q03～Q06 原状态和期限不变。

## 2026-10-11 — Q07 验收模板按模块实际范围适用

**状态：USER_CONFIRMED；来源为同一本轮用户 M02 授权附件第3节。**

### Decision
每个 Module 只执行具备实施条件、且与交付范围相符的验证，同时满足自身真正要求。M02 要求纯 Kotlin 测试、必要集成回归、Lint、构建与静态范围核查；没有 Room、业务界面、真实数据、备份能力，因此不要求提前实现数据库/备份往返，也不创建没有实际测试目标的 Android Instrumentation Test。不适用能力明确写 NOT_RUN / N/A 和原因，不能把未来测试或 NO-SOURCE 标为 PASS。

### Context
原 M02/M03 纯领域模块共用包含真机和完整备份往返的通用完成模板，完整导出/恢复直到 M11/M12 才存在。用户现已正式确认模板适用性，解释同时约束后续模块。M02 起点为 M01 验收回写基线 `a25a06899bd157988c3a4466d7162d8616fb0249`，仅在 `feat/m02-project-cost-engine` 执行并交付。

### Alternatives
为满足通用模板越界提前创建数据库、业务界面和备份；或把不存在、未运行的测试视为通过。

### Reason
匹配真实范围与验证风险，保留不可变原规划，同时不制造实现或测试证据。

### Consequences
当前执行规范和各模块验收按有效解释执行；Q07 的规格澄清已确认，并非所有模块验证已完成。M00/M01 验收与交付历史保持不变；原 130 案例和未来测试不改。M02 交付只报 READY_FOR_REVIEW 并停止，M03 尚未授权，不合并 main 或发布。

## 2026-10-11 — M03 历史付款、状态边界与实际日期计算

**状态：用户已授权 M03；产品规则继承原 C07～C12 和本轮内联请求，同日冲突处理和算法是工程选择，独立验收尚待进行。**

### Decision
写入校验独立使用 BusinessClock；evaluate 只按 asOf 过滤已知付款，不把后来真实付款判为非法。每条 Renewal 同时保存固定周期及标准天数，真实半开覆盖与提醒日期独立。状态按生效日期排序推导；同一服务同日多条状态边界明确拒绝，纠错须先替换原记录。覆盖重叠拒绝，邻接允许。非活跃 currentRate 为0、有效 currentPrice 为 null；无付款 historicalAverage 为 null 并附原因。

### Context
M02 已验收冻结，用户从验收回写基线 `0169c76bbf0946a0400b96d00124b3de9aea1cc0` 授权 M03。原 M03/C09 要求区分标准购买时长与实际有效日期；C10 要求同日只有一个可修正日末状态，不能将最新状态套用全部历史。授权、接口及测试边界见[M03 契约](planning/M03_CONTRACT.md)。

### Alternatives
让历史 evaluate 使用业务今天校验；同日冲突静默选输入最后一条；逐日遍历整个订阅年份；提前将预付下一期价格作为当前价格。

### Reason
分开事实写入合法性和历史观察，避免输入排序造成随机状态或未来付款改写过去价格。明确拒绝冲突比缺少稳定记录ID/纠错事务时静默覆盖更可靠；不引入产品新功能。

### Consequences
elapsed 使用 Long epochDay 半开覆盖与 ENABLED 状态段求交并集，含观察日，排除暂停/停止日期；排序后双指针计算，不逐日枚举，LocalDate.MAX 截止也不调用 plusDays。已付款金额和标准天数不因暂停/停止减少。默认停止 helper 要求调用方选择真实付费期，以其排他结束边界生效；明确提前停止用显式状态日期。Money/BusinessClock 与 DECIMAL128 沿用 M02，无权限/依赖/持久化变化。Q03 长期日节点和 M21触发、Q04提醒资格、Q05/Q06仍待原模块；订阅合计溢出采用显式检查，但本轮未进行九百多万条金额记录的专门溢出实测，不把源码审查当此项 PASS。

## 2026-10-11 — M04 四表 v1、协调写入与私有初始 generation

**状态：用户授权的工程实现；Q06 仅本轮设计与初始数据集范围，独立验收待进行。**

### Decision
使用精确锁定的 Room 2.8.5/KSP2 与 Preferences DataStore。仅四张业务表；外键/唯一索引由 SQLite 强制，UUID/金额/类别类型/媒体 metadata 由实体与 Repository 校验。一个共享 Mutex 协调写入/快照/关闭；项目和初始事件由 Room 事务原子保存，精确相同操作可重放，异内容同键返回冲突。

### Context
起点为 M03 验收回写 `ffd066ffe0efb6c00f35526ae792b50829d3230e`，功能分支 `feat/m04-room-storage-foundation`。本轮首次引入真实持久化，必须用 Android SQLite/DataStore 实测。Q06 设计先于 generation 实现记录于[M04 契约](planning/M04_CONTRACT.md)，用户附件来源哈希在该文件中保留。

### Alternatives
将数据库/设置/图片散放不同目录；损坏时初始化空库；无受控生命周期反复创建 DataStore；仅凭 Mutex 声称跨文件原子性；本轮提前实施完整覆盖恢复。

### Reason
使同一代的数据库与 WAL/SHM、media、设置、ready 有明确私有身份，失败后仍保留原数据和可审查证据。复用已有 Money/BusinessClock，不引入第二套金额规则、复杂事务框架或未来业务表。

### Consequences
数据放 noBackupFilesDir/generations/UUID；真实文件名 settings.preferences_pb 匹配 Preferences 格式。ready 只记录 formatVersion/generationId；active 指针记录 formatVersion/activeId/previousId/stage，M04 仅 STABLE、previousId=null。初始化全部完成后才原子激活；重开按持久身份验证，未知阶段/异常数据明确失败，普通打开不清理旧代。DataStore 单一 canonical owner，关闭取消并等待 scope 完成才释放所有权；SQLite 腐败处理不得删除原库。上述内部元数据版本不是备份 format v1。M08 才实施 schema v1→v2；M11/M12 才创建备份格式与正式恢复/回退/杀进程验证，Q06 不能 CLOSED。

原 C03 首次启动记录时区要求继承；Application 后台装配负责启动时捕获设备时区，后续读取持久 zone。用户界面仍为 M01 占位页，正式存储错误界面留待 M05；初始化状态/错误由容器明确保存并传播，不能默默返回空账本。

## 2026-10-12 — M05 只读三入口、存储门控与数值展示

**状态：本轮明确授权的最小工程选择；继承已冻结V1与M00～M04，M05独立验收尚待进行。**

### Decision
沿既有Nav3宿主演进，仅Home/Review/Settings和只读ProjectList。rememberNavBackStack保存路线身份；合法栈为[Home]或[Home,二级/其他主Tab]，切Tab替换，重复选择不入栈。保留旧Skeleton的序列化类名，已解码非法栈归Home。

Application继续是唯一持久存储所有者；ViewModel只观察storageState并通过既有Repository/SettingsStore在IO读取。快照和zone均成功才Ready；初始化/读取失败是保护性错误，不是空账本。取消继续传播，UI清理不关闭AppContainer/generation，退出由MainActivity.finish落实。

### Context
用户附件[M05契约](planning/M05_CONTRACT.md)锁定页面/组件、真实数据/错误及设备验证；起点为M04验收回写 `aecbe86fce6e5c4375f2c35d2df8539df0a557bb`，本轮 `feat/m05-ui-foundation`。M04独立NOTES要求M05失败不伪称正常空库；原UI U01～U03/U10与DOMAIN C02提供视觉和显示边界。

### Alternatives
在Compose重开Room/DataStore；失败用空列表或零值兜底；每次选Tab累积路线；复制另一套导航；M05提前汇总成本或展示虚构订阅/图表/保存；为三入口增加DI或完整设计框架。

### Reason
让页面只反映已读取的本地事实，保持M04生命周期与一致快照，最小有界栈便于返回和恢复。通用组件不计算成本、不改变数值，不把合成表单回调当业务保存。原C02规定日均非零绝对值小于一分整体显示微值提示，不能只检查舍入到0。

### Consequences
普通金额BigDecimal HALF_UP两位/toPlainString；null为—，0为¥0.00，日均±0.005等微值显示<¥0.01/−<¥0.01及明确语义。38/24/16/14sp、六级间距、低饱和蓝灰/中性浅深和系统字体；无新Logo或主题设置。Name/Amount/Date受控、错误和IME仅组件；合成表单在androidTest。

列表状态只陈述“未记录结束日期/已记录结束日期”和实际日期，不仅凭endedOn非空宣称已经结束：M04允许未来结束日期，冻结领域引擎在结束日前仍可贡献，M05不新增业务时钟/状态公式去覆盖规则。对应实际边界补测另登记验收；不修改Repository校验或结束流程。

必要依赖精确Compose测试1.9.5匹配既有BOM；显式Lifecycle2.10.0是M04已解析版本。Espresso3.7.0只用于androidTest，以显式编译依赖调用同步真实KEYCODE_BACK；此前Compose测试运行已解析该版本，未升级生产依赖或更改生产返回逻辑。Debug运行依赖只增加ui-test-manifest及其空测试Activity，无旧坐标升级，release不加该宿主；最终APK另检查生产无JUnit/合成表单。既有schema/成本引擎/owner/备份排除不改，Q03～Q06与M09期限保留。M06须独立验收及新授权，不能因组件具备回调就直接开始。

## 2026-10-12 — M06复用写边界、统一窗口与任务提交恢复

**状态：Local operating rule；来源：本轮用户M06明确授权及实际RED/GREEN。**

### Decision

以非owner ProjectRepository复用M04创建/金额事务，资料编辑完整行CAS；首页只COUNT，SQL50游标＋可见事件/类别批读，整个已加载窗口一次协调边界/Room事务。SavedStateHandle只存当前任务单表单raw和固定命令，未知结果只同身份重放；CAS冲突必须明确点击核对后换基线，普通前台恢复只读。

### Context

M05独立要求真实写后刷新/分页；M06要避免初始投入双计、并发陈旧覆盖与已写成功后重复创建。真实竞态旧循环在101源项目返回102行；读初始资料中断及迟到旧详情也需要明确身份约束。

### Alternatives

复用全量快照启动、每行查询、页间释放锁、自动替换冲突输入、创建新数据库或操作日志；均不能满足本轮边界或扩大scope。

### Reason

同一Room事实与被冻结引擎保持唯一真值；SQL批读限制正常入口负担，同事务解决已复现混合快照。提交前固定raw/UUID/时间与阶段，已保存和读失败分开，避免金额重复。DatePicker UTC仅为日历编码，业务今天固定recordZone/BusinessClock。

### Consequences

schema v1/owner/领域金额算法/依赖不改，编辑没有持久操作日志，重放只核对当前完整目标；单项目必要事件仍完整读取。任务保存恢复不等于后台业务草稿或force-stop恢复。真实SQLite满页回滚有时将SQLiteFull遮蔽为DATABASE；本轮不猜字符串分类，保留同命令/旧事实安全，透明登记测试预期修正。284方法、真实设备/APK证据见[M06报告](evidence/M06-verification.md)，不授予独立冻结或启动M07。


## 2026-10-12 — M07 复用事实、完整快照与明确缺失核对

**状态：Local operating rule / Inherited planning default；来源为本轮用户M07完整授权及[M07契约](planning/M07_CONTRACT.md)。不改变冻结产品规则。**

### Decision

沿用现有Room v1、协调器和单活动SavedState表单。结束与关联收回、结束/原款纠错、普通金额纠错和删除均用完整事实基线及同事务；保留事件身份，不创建第二份账本或持久操作日志。全部正式写入口维护最多一条结束关联款，歧义旧数据明确冲突。项目删除保留媒体给M10，历史以数据库观察日筛选。

### Context

M06已独立冻结，M07要求不重复回收、修改后历史重算、失败无半条记录和明确不可逆删除。现有schema没有永久command日志，不能用一个UUID虚构永久可查询的操作成功。已支付尾款仍可改变净成本而不改变持有天数。

### Alternatives

新增操作日志/schema、用反向金额抵消历史错误、重新新增结束款、按列表最后日期延长计时、删除媒体目录、用全量UI筛选历史；均扩大范围或违背事实边界。

### Reason

完整项目/事件CAS与固定目标用于当前事实核对，同Room事务保证多步副作用回滚。删除比较项目和全部事件，避免陈旧确认漏掉刚补录的资金。Absent只说明当前不存在；显式核对缺失和读取异常区分，不能把异常当删除成功。结束编辑采用全屏滚动表单、删除采用AlertDialog，沿用统一返回守卫并保证大字体输入可达。

### Consequences

本轮无迁移/索引/依赖升级，不增加单独事件删除、多段重新启用、回收站、媒体清理或备份。原INITIAL身份、冻结引擎和固定记录时区继承。SavedState只覆盖任务实际保存后的恢复，长期备份仍归M11/M12；M07交付停在READY_FOR_REVIEW，独立验收PENDING。实际结果见当前验收登记；内部代码审查不授予冻结。

## 后续新增/变更格式

每条按以上日期/状态/Decision/Context/Alternatives/Reason/Consequences格式记录，增加来源与授权证据；列出受影响模块、schema、format、图表和验证案例。取代决定保留旧记录标 Superseded；不能以新想法擅自改整个 Roadmap。

## 2026-10-13 — M08 显式安全升级与独立真实付款事实

本条保留M08工程实施时语境；M08现已依据用户转交的独立验收正式冻结，下文PENDING或未授权M09是当时历史，当前M09交付状态见CURRENT_STATE。

**状态：Local operating rule / Inherited planning default；来源为本轮用户 M08 完整授权及 [M08 契约](planning/M08_CONTRACT.md)，继承原 DOMAIN C07～C11 与 ARCH A02～A08，不改变冻结产品规则。**

### Decision

在同一个 AppContainer / ManagedGeneration / LocalWriteCoordinator 中，通过已注册的显式 Room Migration(1,2)仅新增订阅身份、逐笔真实续费和状态时间线三张表；保留原四表结构/业务行及不可变 v1 导出。正式 writable Room 前先只读核对已知 v1/v2 身份及真实结构、主键/索引 BINARY 比较规则、完整性和关联，拒绝不合法库，不清库或用新空代代替升级。订阅身份不存第二份当前套餐或派生成本；首次付款与身份同事务，之后只保存用户确认的独立真实付款。非 owner SubscriptionRepository 与项目入口共享同代存储和固定 recordZone，完整事实唯一调用已冻结 M03 引擎计算。

### Context

M00～M07 已独立验收冻结，现有 v1 已包含需要保持的项目、资金、类别和媒体元数据。M08 同时引入真实订阅持久化和跨版本升级，必须区分“新空库可运行”与“旧事实可安全升级”；预付日期也不能被最近付款价或标准购买天数替代。实际接口和迁移闸门见 [M08 契约](planning/M08_CONTRACT.md)，当前实现见[迁移](../app/src/main/java/com/cooper/lithic/data/local/DatabaseMigrations.kt)、[只读结构预检](../app/src/main/java/com/cooper/lithic/data/local/DatabaseSchemaValidation.kt)及[订阅 Repository](../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionRepository.kt)。

### Alternatives

损坏或未知旧库直接创建空数据集；仅凭版本/Room hash 先进入迁移；使用 destructive fallback；把最后一次付款价覆盖在订阅主表；按到期自动新增付款；另建存储 owner、价格公式或持久操作日志。

### Reason

严格预检在任何 writable 升级前保留旧事实和可审查失败，显式 DDL 将 schema 演进范围限制到授权的三表。独立付款和四日期保留真实金额、付款年及生效/到期边界，同一写协调器与 Room 事务保证首次付款原子性、精确同命令重放和全部覆盖校验。完整必要事实复用 M03 可保持观察日价格、折算时长与实际经过时间一致，避免页面或分页产生第二种口径。

### Consequences

当前 Room schema 为 v2，旧 v1 导出及四表不改；内部 generation metadata format1 保持，无完整 backup format，数据库、DataStore 和文件系统不因此成为跨存储原子事务。SQL 50+1 稳定分页与批读窗口限制展示负担，历史平均仍使用完整事实；SavedState 固定原命令/UUID，COMMITTED 只重读，两类 ViewModel 的 pending/dirty 全局导航守卫共同保护提交。M08 不提供 M09 暂停/恢复/停止、自动付款、退款或期中补差；Q03～Q06 和 M09 两项流程风险继续保留，媒体 M10、完整备份/恢复 M11/M12、汇总 M13 不提前实施。工程决定不授予独立冻结；M08 独立验收 PENDING，实际测试/最终 APK/交付状态以唯一 [CURRENT_STATE](CURRENT_STATE.md)和[验收登记](ACCEPTANCE_MATRIX.md)为准。真机/API26/OEM/Release正式签名/完整 TalkBack/真实 LMK 未验证。


## 2026-10-13 — M09用户正式确认STOPPED重新开通与固定最后付费停止边界

**状态：USER_CONFIRMED。来源：本轮完整用户执行指令，SHA-256 `d83fb2d77190b0c49df8b5adf9a6e13c8cae8ca203513f9f5e98b0729350eaab`，实施契约见 [M09_CONTRACT](planning/M09_CONTRACT.md)。产品规则已确认，现有 Room v2、Repository、表单与导航中的工程实现已接通，最终四任务549/549方法PASS、最终APK与专用API36实际旅程已验证；两项流程风险按模块证据关闭。当前READY_FOR_REVIEW、独立PENDING，不自授冻结。**

### Decision

STOPPED不是永久销毁：在原Subscription ID上明确重新开通，保留付款和日末状态历史。当前已有有效覆盖可ENABLED而不付款；仅未来已付覆盖可明确ENABLED但生效前仍EXPIRED；无当前/未来覆盖必须用户确认真实续费，与ENABLED原子提交。同日状态修正原行ID/operationId与完整旧事实，不再追加重复边界。

停止默认采用max(业务今天,全部已确认真实付款的最晚paidUntilExclusive)，包含预付和空档；只提供到期后停止与明确提前停止。确认时固定为持久STOPPED事实，付款新增/纠错不自动移动停止日期；与计划冲突要求明确修正/取消。提前停止不退款、不减少金额/标准已购天数，不替服务商取消续费。

### Context

M03独立审查登记了STOPPED再启用入口和多期预付默认停止两项流程风险；M08保留未定案边界。当前用户已在开发前给出正式A/B解释，M09实现及验证可以据此推进。

### Alternatives

永久停止后另建服务身份；只取当前一期结束；付款时自动延后停止；通过零额付款或自动续费启用；均不符合本轮确认。

### Reason

稳定身份保留多年历史，固定计划日期避免用户既有停止意图被后来付款静默改变。M03仍是唯一状态/成本引擎，Room v2已有事实足够，完整基线CAS与现有协调事务保护同日修正、付款纠错和重开原子性。

当前工程沿用已有协调器和一次 Room 事务实施六种生命周期命令，校验订阅及全部付款/状态事实，事务内写入后核对完整目标；状态/付款纠错保留原事实身份。对应可追溯实现见[命令](../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionLifecycleCommands.kt)、[Repository](../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionRepository.kt)和[单表单 ViewModel](../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt)。以下为本轮最小工程规则，不新增产品功能：

- **固定信封跨日恢复。**先比较完整已存目标是否精确吻合，再对尚未提交的新写入验证今天/默认边界；不能用恢复日重新移动已保存停止日期。一次提交的 UUID、时间、原事实和目标在原活动 Bundle 内固定，不分配新付款来处理结果丢失。
- **当前缺失语义。**schema v2没有永久业务操作日志，删除付款及取消计划的 `CurrentAbsence` 只确认当前目标不存在，不提供永久 commandId 查询或历史操作身份的证明；结果未知继续保留原输入和待核对信封。
- **有界完整预检。**保持页外/筛选外坏事实检查，以稳定 UUID keyset 每批50个身份读取必要关联并释放上一批，避免一次装载全部订阅历史。预检仍 O(total facts)，单项完整时间线仍必须加载，不能将该保护或分页正确性测试宣称为长期性能基准。
- **COMMITTED 与详情版本保护。**已成功保存只重读，不重复写；后来事实变化与读失败明确区分，用户显式核对后安全返回，不覆盖新事实。成功读取与显式冲突读取都捕获 detailVersion，迟到结果不能重定向已打开的另一详情或解除原待确认守卫。导航来源以新读取的 M03 状态决定，不能沿用旧缓存。

### Consequences

不新增schema/操作审计系统/次数指标。已付剩余期可能因明确停止被截断贡献，金额-only纠错仍合法；扩张计划停止后的覆盖须先处理停止计划。已删除事实只可核对当前缺失，不提供永久command重放证明。PAUSED无覆盖仍继承M03的PAUSED主状态，辅助显示覆盖已到期。Q03～Q06及M10～M23范围不变，两项M09流程风险仅按本轮已完成真实验证关闭；独立冻结仍需ChatGPT验收。完整回归、最终APK/设备及交付证据见[M09报告](evidence/M09-verification.md)，历史失败保留，Git自身SHA和推送以实际回执为准。SavedState只覆盖系统实际保存的任务状态，不是永久草稿，也不承诺force-stop、断电或真实LMK恢复；完整备份恢复仍归M11/M12。

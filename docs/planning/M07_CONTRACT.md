# M07 项目生命周期 Implementation Plan

> **For agentic workers:** Execute only this authorized module, using the existing repository memory and task-scoped delegation. Checkbox tasks track actual evidence; no independent acceptance is granted here.

**Goal:** 原生结束/唯一关联收回、身份稳定历史纠错、真实删除与基本历史档案，所有事实由冻结ProjectCostEngine重算。
**Architecture:** 原单owner/Room v1/LocalWriteCoordinator；Compose → 现有ProjectViewModel单表单SavedStateHandle → ProjectRepository/ProjectWriteRepository。无新持久状态系统。
**Tech Stack:** 全部M06冻结版本，0计划新增依赖；Debug code5/0.1.0-m07-debug。
**Spec:** 用户本轮附件SHA `66a4810c80deaa3df766294442a23a88ceca06ed10b377501b5f68f3e17309f8`，[原M07](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m07.md)、DOMAIN C02～C05/C13、UI U01/U03～U05/U08、PRODUCT长期/历史/删除、ARCH A02～A06及M06独立NOTES。

## Global Constraints / Authorization

M00～M06已冻结；实际起点fb0eafdc43efa6e13ebd76e3491fa4331a145abb与origin一致、工作区干净；指定新分支feat/m07-project-lifecycle此前本地/远端不存在。按用户指定E:/CODEX/砺石新APP在干净checkout工作，不另建工作树、不从main开始。

本轮既有详细产品/事务/测试规范是直接执行授权；本契约只固定低风险工程接口，不重复请求产品批准。原284方法保留；正确旧行为直接回归，不人为破坏。原43规划/130 NOT_RUN/16完整补测/历史证据不改。禁止M08、替换、订阅、汇总、图表、回收站/多段复用、媒体清理/备份恢复/迁移/schema/依赖升级、WHOCARE、main合并/force/正式发布。

## Review Focus / 工程解释

1. 关联结束款的身份冲突/多条旧END_SETTLEMENT：全部正式写入口校验；不猜一条、不清历史。兼容原addMoneyEvent对已结束项目新增首条END_SETTLEMENT。
2. 结束与回收、修改结束与关联金额、删除事件与项目：同协调器同Room事务；真实SQLite触发器失败注入，验证无半成品。
3. schema v1无操作日志：更新按完整项目/事件基线与固定目标CAS，稳定command/event UUID，未知结果仅重试原信封；缺失删除目标只报已核对不存在，不伪称可证明是谁执行过删除。
4. 历史纠错只改金额/真实日期/说明，不改id/projectId/role/direction/operationId；INITIAL不删除不换角色，END_SETTLEMENT只由修正结束管理。本轮不开放单独事件删除，减少未授权的复杂审计，真实退款继续新增REGULAR RECOVERY。
5. 未来结束日期继承M04合法数据及冻结引擎边界，不提前归档；真实金额不晚于固定BusinessClock.today。结束后晚款/退款不增加持有天数。

## Task 1 — 生命周期与数据事务

Owner文件：新增domain/project/ProjectLifecycleRules.kt、data/repository/ProjectLifecycleCommands.kt；修改ProjectWriteRepository.kt/ProjectRepository.kt/ProjectDao.kt；新增Android ProjectLifecycleRepositoryTest.kt及必要JVM规则测试。其它实体/schema/owner/设置/引擎不改。

固定命令：
- EndProjectCommand(commandId:String, expectedProject:LongTermProjectEntity, endedOn:LocalDate, endReason:String, settlement:ProjectMoneyEventEntity?, updatedAt:Instant)
- UpdateEndCommand(commandId:String, expectedProject:LongTermProjectEntity, expectedSettlement:ProjectMoneyEventEntity?, endedOn:LocalDate, endReason:String, settlementAmountCents:Long?, settlementOccurredOn:LocalDate?, settlementDescription:String?, updatedAt:Instant)
- CorrectMoneyEventCommand(commandId:String, expectedProject:LongTermProjectEntity, expectedEvent:ProjectMoneyEventEntity, amountCents:Long, occurredOn:LocalDate, description:String?, updatedAt:Instant)
- DeleteProjectCommand(commandId:String, expectedProject:LongTermProjectEntity, expectedEvents:List<ProjectMoneyEventEntity>)

MutationResult新增LifecycleApplied(projectId:String, settlementEventId:String?, replayed:Boolean)、MoneyCorrected(projectId:String,eventId:String,replayed:Boolean)、ProjectDeleted(projectId:String)、ProjectAbsent(projectId:String)。Absent只证明此ID当前无项目/关联事件，不是持久删除日志的成功重放证明；UI准确表达并安全返回。

ProjectRepository及writer新增open/suspend endProject/updateEnd/correctMoneyEvent/deleteProject对应命令→MutationResult。DTO放单一Commands文件，不重复实现写边界。资料/金额纠错提升project.updatedAt到固定严格递增时间；保留所有不可变字段。结束不接受已结束基线（修正用updateEnd）；可选settlement为唯一RECOVERY/END_SETTLEMENT，id与operationId稳定且匹配commandId。updateEnd只更新已有关联事件；原无关联事件时三个settlement修改字段均null，不偷偷创建或删除款项。关联多条则Conflict。

delete先完整比较项目及其全部事件快照（按id比较，不依赖传入顺序），拒绝新增/纠错/资料变化导致的陈旧确认；再删除该ID事件和项目，逐项核对影响行数。失败整体回滚。缺失但仍有孤立事件明确异常，不猜成功。不删除任何media元数据或真实文件，保留共享/未引用资源给M10；无悬空项目FK。不开事件删除入口。

读取保留M06 loadProjectPage/loadProjectWindow全项目API语义以兼容284回归；新增loadCurrentProjectWindow(pageCount:Int,asOf:LocalDate)、loadProjectHistoryWindow(pageCount:Int,asOf:LocalDate)返回ProjectPage。数据库过滤current：endedOn IS NULL OR endedOn>asOf；history：endedOn IS NOT NULL AND endedOn<=asOf。未开始记录仍在未结束列表，标签准确。两者按startedOn,id稳定游标、每页51查询/50展示、批读事实及整个窗口一致事务。首页COUNT仍指全部已录入项目，不伪装为成本合计。ProjectLifecycleRules仅校验日期/原因与日末结束状态，不复制成本/天数公式。

- [x] 可编译命令/API stub与真实Rule/Room测试先RED；原已正确领域行为直接回归。
- [x] 实现原子结束/修正/纠错/删除、全入口唯一关联、过滤/分页及完整CAS；真实触发器失败、并发/重放/未知结果、共享媒体保护、旧多关联冲突。
- [x] 精确GREEN与原存储/项目回归；冻结schema和引擎hash核对。

## Task 2 — 单表单提交、恢复与Nav3

Owner：ProjectViewModel.kt、ProjectUiModels.kt、LithicRoutes.kt、LithicNavigation.kt。保持现有构造和公开方法；新状态字段追加且有默认值，旧组件构造保持兼容。所有新提交复用原form Bundle/phase/save/retry/guard；新增kind end/updateEnd/correctMoney/delete及完整原项目/事件基线/固定submitted。不能另建ViewModel/持久owner。

UI模型新增EndProjectUiState(projectId,projectName,correcting,endedOn,endReason,recordRecovery,recoveryAmount,recoveryOn,recoveryDescription,hasSettlement,today,phase,dirty,loading,errors,message,conflict,needsInitialRead)，MoneyCorrectionUiState(projectId,eventId,projectName,direction,role,amount,occurredOn,description,today,phase,dirty,loading,errors,message,conflict,needsInitialRead)，DeleteProjectUiState(projectId,projectName,phase,loading,message,conflict,needsInitialRead)。全部默认值/派生busy/canEdit，相关类型沿现有数据类。ProjectUiState追加history:ProjectListUiState、endEditor?、moneyCorrection?、deleteConfirmation?、deletedProjectId?。

公开新方法：loadHistory/loadMoreHistory/startEnd(projectId)/startUpdateEnd(projectId)/startCorrectMoney(projectId,eventId)/startDelete(projectId)，changeEndedOn/changeEndReason/changeRecordRecovery(Boolean)/changeRecoveryAmount/changeRecoveryOn/changeRecoveryDescription；纠错金额继续changeAmount/changeOccurredOn/changeDescription；save/retryRead/refresh/discardForm/leaveDecision复用；consumeDeleted用于导航消耗。删除save只在用户明确确认后调用，取消仅discard，未知删除不能换ID再猜。读失败与不存在分开；既有初始化/COMMITTED读失败语义保持。

Nav追加ProjectHistory及End/UpdateEnd/MoneyCorrection/Delete路线；仅UUID/enum身份，ProjectOrigin(CURRENT,HISTORY)作为可序列化来源，旧Detail/Edit/Money构造的origin默认CURRENT。当前链Home/List/Detail/Editor最多4；历史Home/Review/History/Detail/Editor最多5。历史返回正确父链/主Tab选回看，无无限增长。成功结束/修正仍回同ID详情及来源；删除确认/核对Absent回来源列表，不保留旧详情。未知/失败禁止自动导航。外部已删目标不能显示陈旧详情。首次读中断恢复及迟到响应身份校验覆盖新表单。

- [x] 先状态/恢复/路由测试RED，不以回调模拟Room业务。
- [x] 实现单信封/完整基线/固定UUID、已保存读取失败、陈旧冲突显式重读、删除未知核对与来源导航。
- [x] 验证所有原284方法；仅必要旧阶段断言适配需详细登记，不能删弱事实断言。

## Task 3 — 确认、纠错及历史UI

Owner：新增EndProjectDialog.kt、MoneyEventCorrectionScreen.kt、DeleteProjectDialog.kt、ProjectHistoryScreen.kt；修改ProjectDetailScreen/ProjectListScreen/ReviewShell必要接线。复用已有Theme/AmountText/日期/raw输入/48dp控件，不计算成本不直接DAO。

EndProjectDialog以滚动全屏表单承载结束记录，便于日期/可选实际回收在360dp200%/IME下可达；由Nav3统一守卫。新结束recordRecovery默认false，显式启用时0合法，实际日期默认结束日且可修改/验证不在业务未来；修正无关联事件不显示新增收回入口，有关联只能更新原项。历史纠错说明区别于真实退款，方向/角色只读。删除为真正AlertDialog二次确认，固定文案“将删除项目及金额记录，历史统计也会重算；无法在App内撤回。”，取消不写，busy/unknown不能误触重复，明确核对原提交。

History数据库已结束筛选/有界50、真实空/加载/错误保留状态，点入真实详情；详情已结束标结果截至观察日/含结束后结算，保留普通金额追加，逐项事件纠错与关联结束纠错分流，不开放事件删除或多段恢复。只在回看增加历史档案真实入口，其它未来功能仍尚未开放。

- [x] 受控Screen/确认/大字体状态测试先RED。
- [x] 正式生产Nav/VM/真实Room的ProjectLifecycleFlowTest五命名案例及额外纠错/退款/来源返回闭环。
- [x] 专用API36实际创建→结束回收→历史→修正→尾款→取消/确认删除→重启/断网/大字体/状态恢复；只操作本轮创建的合成UUID，保留M06原2测试项目。

## Task 4 — 最终验证与交付

- [x] root独占Gradle/ADB/Git；原284+新增源码/XML/JSON身份精确对应，完整testDebugUnitTest/connectedDebugAndroidTest/lintDebug/assembleDebug。
- [x] 核对最终APK code5/签名/SHA/合并权限/备份排除，schema v1/依赖/recordZone/generation不变；仅模拟器不冒充手机，Release/API26/OEM/TalkBack/真正LMK等NOT_RUN。
- [x] 当前文档/验收/checkpoint/12项报告及完整变更/敏感信息/旧原件核查；原130/16不改PASS，Q03～Q06/M09/未来边界保留。
- [x] 仅提交推送feat/m07-project-lifecycle，READY_FOR_REVIEW — M07后停止，ChatGPT独立验收PENDING，不授予冻结、不进入M08。

## 实际收尾证据（2026-10-12）

上述勾选表示已执行适用范围，不授予独立冻结。最终86 JVM＋273 Android＝359唯一方法，原284保留/新75，0失败错误跳过；Lint0/20，完整Debug构建成功。规则/真实Room/Nav3/状态/触控RED、诊断和最终GREEN见[M07报告](../evidence/M07-verification.md)及[JSON](../evidence/M07-verification.json)。专用API36最终APK632a402c…实际完成断网创建/结束收回/历史/连续修正/原金额纠错/尾款/退款/取消确认删除/重启，原M06两项目四事件逐字段不变，schema1/integrity/FK/generation/settings核对及临时guest设置恢复通过。200%字体IME和保存任务SIGKILL恢复限定路径有真实证据；Release/真机/API26/OEM/TalkBack/真正LMK/完整备份仍NOT_RUN。本轮Git提交/推送自身SHA以最终日志与交付回执为准；只M07功能分支，独立验收PENDING，M08未授权。

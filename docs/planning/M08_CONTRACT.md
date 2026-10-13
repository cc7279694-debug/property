# M08 固定周期订阅、真实续费与 Room v2 Implementation Plan

> **For agentic workers:** Execute only this authorized module. Root owns all Gradle/ADB/Git and evidence. Disjoint storage, state/navigation and screen tasks may prepare in parallel; production migration starts only after its contract and real failing tests. Internal review is not independent acceptance.

**Goal:** 在同一本地数据集内无损升级v1→v2，正式记录用户确认的首次付款和逐笔续费，复用冻结M03计算及原导航保护。
**Architecture:** 单AppContainer/ManagedGeneration/LocalWriteCoordinator；Compose → 轻量SubscriptionViewModel → SubscriptionRepository → Room v2 / M03。项目当前4级/历史5级保留，新订阅链最多4级。
**Tech Stack:** 冻结Gradle8.14.3/AGP8.13.2/Kotlin2.3.10/JDK17/Compose BOM2025.11.01/Nav3 1.0.1/Room2.8.5/KSP2.3.6/DataStore1.2.1，SDK26/36/36。计划不新增依赖；原schema v1不改，Debug code6/0.1.0-m08-debug。
**Spec:** 本轮完整附件SHA-256 `31b0dea9ce575192cc22b7da229c5f73e2020a9ea785db287cb672f568c029f6`；[原M08](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m08.md)、DOMAIN C02/C03/C07～C11/C13、UI U01/U03/U06/U07、ARCH A02～A08、PRODUCT固定周期订阅及M07全部独立NOTES。

## Global Constraints / Authorization

起点fcb850f549285d310040318a5f7adfd51abb4c33 / feat/m07-project-lifecycle，实际本地/远端一致且工作区干净，目标分支此前不存在，已创建feat/m08-subscription-renewals。指定目录E:/CODEX/砺石新APP，不从main开始，不另建平行状态系统。用户已直接授权详细架构、测试、实现和Git交付，本契约不再次请求相同授权。

只允许M08三表/显式迁移/合法库预检、真实首次付款与追加续费、列表详情表单/刷新/分页/提交守卫、真实测试与合成数据升级。禁止M09状态用户操作、STOPPED规则变更、按量/自动付款/退款/期中补差、备份恢复、媒体管理、M13总成本汇总、云端、生产样本、WHOCARE、main合并/force/公开APK。原43原件/130案例/16完整补测和全部旧证据不改；原359方法身份保留，只有真实阶段不再成立的断言允许明确记录并保留等效事实证明。

## Migration Design Gate

现有四表Category/Project/MoneyEvent/Media与v1 JSON SHA `351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`保持。先写真正v1 SQLite fixture及迁移/预检失败案例，在旧生产v1运行见行为RED；再实现显式Migration(1,2)和schema2。新库成功不替代旧库升级。

预检继续只读打开既存DB、检查SQLite文件头、通过SQLite读取含已提交WAL的user_version/Room身份，验证已知v1/v2身份和真实表列/PK/索引/FK、quick_check及foreign_key_check；拒绝v0/未知/外库/腐败/伪造已知hash但结构不符的库。不得先迁移已知不合法旧库，不省略迁移失败保护。Room正式注册迁移并验证v2结构，保持保留文件的corruption handler/禁用data-loss recovery，不用destructive fallback。

迁移仅CREATE三个新表及索引，不改旧表/行/身份；DDL失败全事务回滚，原v1仍可读取。DataGenerationStore.openExisting沿原指针打开同代升级，无新空generation；settings/recordZone/ready/pointer/媒体文件均不变。M12完整切换与回退不在本轮。

## Task 1 — Storage / Migration / Repository

Owner：新增data/local/SubscriptionEntities.kt、SubscriptionDao.kt、必要DatabaseSchemaValidation.kt；修改LithicDatabase.kt/DatabaseMigrations.kt/DatabaseConverters.kt；新增data/repository/SubscriptionRepository.kt、SubscriptionCommands.kt（可含Result/DTO）、必要Mapper；新增Migration/SubscriptionRepository Android测试和v1 fixture。app/build.gradle.kts的测试schema assets由root处理。原M03引擎和四表实体/DAO不改。

三表：

- subscriptions：id/name/categoryId/coverAssetId/note/createdAt/updatedAt；category/cover RESTRICT FK，categoryId及coverAssetId索引。
- subscription_renewals：id/subscriptionId/paidOn/effectiveFrom/paidUntilExclusive/reminderDueOn/amountCents/cycle/standardDays/operationId；parent RESTRICT FK、operationId唯一、(subscriptionId,paidOn,id)和(subscriptionId,effectiveFrom,id)索引。
- subscription_state_changes：id/subscriptionId/effectiveOn/state/reason/note/operationId；parent RESTRICT FK、operationId唯一、(subscriptionId,effectiveOn)唯一及必要日期查询索引；M08只建表及类型转换/读取，用户不写状态事件，空历史由M03默认ENABLED。

真实日期以LocalDate/epochDay，Instant以既有转换；BillingCycle和OperationState稳定name转换，未知值失败。标准天数独立持久化但须等于该次cycle.standardDays。UUID/跨类别SUBSCRIPTION/金额>0及范围/固定业务today/全部覆盖不重叠均在同一写协调器与Room事务内验证，不能用FK冒充业务规则。

固定接口（公开suspend，读取可open供实际失败注入）：

```kotlin
CreateSubscriptionCommand(commandId: String, subscription: SubscriptionEntity, initialRenewal: SubscriptionRenewalEntity)
ConfirmRenewalCommand(commandId: String, renewal: SubscriptionRenewalEntity)
SubscriptionMutationResult.Success(subscriptionId: String, renewalId: String, replayed: Boolean)
// 另有ValidationFailure(reason), Conflict(reason), StorageFailure(StorageFailureKind,message)，不改旧MutationResult。
SubscriptionDetail(subscription, category, renewals, stateChanges, cost: SubscriptionCostResult, currentRenewal?, asOf)
SubscriptionCursor(lastPaidOn: LocalDate, id: String)
SubscriptionPage(items: List<SubscriptionDetail>, nextCursor: SubscriptionCursor?, totalCount: Long, asOf: LocalDate)
RenewalCursor(paidOn: LocalDate, id: String)
RenewalHistoryPage(items, nextCursor, totalCount, asOf)
SubscriptionRepository(db: LithicDatabase, coordinator: LocalWriteCoordinator, clock: BusinessClock)
createWithRenewal(command): SubscriptionMutationResult
confirmRenewal(command): SubscriptionMutationResult
loadSubscriptionDetail(id, asOf = clock.today()): SubscriptionDetail?
loadSubscriptionPage(cursor = null, pageSize = 50, asOf = clock.today()): SubscriptionPage
loadSubscriptionWindow(pageCount, asOf = clock.today()): SubscriptionPage
loadRenewalHistory(subscriptionId, cursor = null, pageSize = 50, asOf = clock.today()): RenewalHistoryPage
loadRenewalHistoryWindow(subscriptionId, pageCount, asOf = clock.today()): RenewalHistoryPage
loadSubscriptionCategories(): List<CategoryEntity>
loadSubscriptionCount(): Long
changes: Flow<Unit>
```

Result成功身份必须对应submitted。create身份+首付款同事务，renewal.operationId==commandId；重放核对完整已存事实，异内容同operation/实体ID冲突，不增加第二笔。不向身份表覆盖当前价/周期/状态/总额；confirm只追加真实已付期，无自动写。重叠包含已付未来覆盖，两个并发命令只允许一致合法结果；真实失败保留旧事实。

列表按真实MAX(paidOn) DESC / subscription.id ASC，不按createdAt/effectiveFrom伪造最近续费；历史按paidOn DESC / renewal.id ASC。SQL50展示/51哨兵，类别/续费/状态批读，已加载窗口一致快照；不能INNER JOIN隐藏异常无付款身份。完整成本依全部必要事实映射冻结M03，历史分页不作为不完整分母。currentRenewal仅ACTIVE且paidOn<=asOf且日期覆盖，无覆盖为null/currentRate=0，不伪装套餐。算式与周期由M03唯一提供。

- [x] 真实v1 fixture/迁移预检安全测试及可编译订阅API/test stub→行为RED，原正确行为直接回归。
- [x] 实现Migration1→2、三表/转换/预检/事务/幂等/分页及引擎映射；v1原件四表与所有旧行原样。
- [x] T01/T02/T04/T05/T06、跨类型/FK/0负精度未来/并发重叠/预付三观察日/文件保留/同代设置验证GREEN并内部审阅。

## Task 2 — State / Navigation / Assembly

Owner：新增feature/subscription/SubscriptionViewModel.kt、SubscriptionUiModels.kt；修改LithicRoutes/LithicNavigation/LedgerUiState/LedgerUiStateHolder/HomeShell；新增SubscriptionViewModel/Routes Android测试。原ProjectViewModel与AppContainer不重构不另建owner。

UiModels按ignored state-nav-design原型，固定SubscriptionEditorKind CREATE/RENEWAL，feature-localSubmissionPhase或兼容复用原phase/LeaveDecision均可；对screens固定SubscriptionEditorUiState、SubscriptionListUiState、SubscriptionDetailUiState、RenewalHistoryUiState、SubscriptionUiState。所有字段有默认值；editor保留serviceName/categoryId/note/amount/cycle/paidOn/effectiveFrom/paidUntilExclusive/reminderDueOn/today/categories/phase/dirty/loading/errors/message/conflict/needsInitialRead。至少busy/canEdit派生。

固定VM构造SubscriptionViewModel(repository,savedState,clock,instant=Instant::now)；loadList/loadMore/openDetail/loadMoreHistory/startCreate/startRenewal，changeName/Category/Note/Amount/Cycle/PaidOn/EffectiveFrom/PaidUntil/ReminderDue，save/retryRead/refresh/discardForm/leaveDecision/consumeCommitted。Bundle唯一key lithic.subscription.activeForm；raw及稳定command/subscription/renewal IDs/固定Instant和submitted在启动写入前保存。IN_FLIGHT锁定，恢复为UNCONFIRMED，仅原信封重放；未知身份结果不得导航。成功COMMITTED持久化，只重读，读失败明确已保存且不允许再写；成功精确ID读取后才发committedSubscriptionId并消耗表单。

初读中断/迟到响应核对kind/id/command/event，只补未输入字段；冲突不得悄悄换身份/覆盖/回到可付款新命令。显式核对结果或已证实未写后允许安全返回，不能死锁；读取异常保留原信封。无自动付款或保存业务草稿。

NavKeys仅UUID：SubscriptionList/CreateSubscription/SubscriptionDetail(id)/ConfirmSubscriptionRenewal(id)。Home/List/Detail/Editor最多4，Home/Create或Home/List/Create父链恢复；旧项目history5/current4全部继承。requestLeave汇总两个VM：任何pending阻止，其次任何dirty提示弃改；系统/工具栏/Tab含重选/业务入口统一走同一guard，成功精确身份读取是窄内部导航旁路。隐藏恢复的pending表单必须有可达核对/返回入口，不无限阻止。取消不丢输入，确认弃改不绕过另一VMpending。

LedgerReady增加真实SubscriptionRepository/count，源于同一个generation/coordinator/BusinessClock；首页正式订阅入口/真实数量，读取异常不呈假0/空库。M13汇总保留未开放，无第四Tab。若旧测试Ready直接构造需要兼容默认或明确适配，只保持原测试主体事实，不给正式路径假repo/假count。

- [x] 可编译状态/路由测试先RED；只读研究非测试PASS。
- [x] 固定单信封/COMMITTED重读、跨功能守卫、有界路线/迟到身份/读取失败与刷新实现。
- [x] 原359回归及必要阶段断言精确登记，状态和实际Room导航验证GREEN并内部审阅。

## Task 3 — Controlled UI / Real Flow

Owner：新feature/subscription四Screen及内部共享表单/展示工具，新SubscriptionScreensTest/SubscriptionRenewalFlowTest。与Task2按其UI设计文件回调接口一致，不改VM/Nav/数据库。复用Theme/AmountText/已有受控日期与48dp控件；ProjectCategoryPicker硬过滤PROJECT不能直接用于订阅，选择小型订阅专用picker，避免改冻结项目组件。

新建与续费为滚动单页显式真实付款确认；名称/类别/备注只在创建。金额绝对值>0、月季年、四个独立日期，合理自然日默认仅为参考需核对，不用标准30日自动当实际月份；错误定位字段且raw保持。日期解释“付费到期边界当天不包含在前一期”。打开/修改/弃改/到期均不保存。

详情主历史平均与折算付费时长，当前套餐独立区块，真实历史分页四日期/独立金额周期；未来已付期标“尚未生效”，无有效套餐不显示假当前价格，无历史分母明确无值。真实经过时间只辅助说明，不把预付365写成已使用365。没有M09状态操作入口。

Screen接口沿state-nav-design UI段命名：List onBack/onCreate/onOpenSubscription/onLoadMore/onRetry；Editor onNameChange/onCategoryChange/onNoteChange/onAmountChange/onCycleChange/onPaidOnChange/onEffectiveFromChange/onPaidUntilChange/onReminderDueChange/onConfirmPayment/onRetryRead；Renewal少身份编辑；Detail onBack/onRenewal/onLoadMoreHistory/onRetry。新form tags和文案固定在测试brief，和VM同步。Home入口仅Task2修改。

- [x] 受控屏状态先有效行为RED；真实Room Flow首次runner错误单列诊断，7方法首次有效执行GREEN，不伪造Flow行为RED。T03正式UI实际触摸，不调用model.save冒充。
- [x] 正式四Screen及M08六命名案例可测用户闭环，200%字体IME/系统返回/弃改/未来已付/读错无假空数据。
- [x] root完整GREEN、专用API36最终APK触摸与内部审阅。

## Task 4 — Root Verification / Delivery

root独占Gradle/ADB/Git。已保留真实M07 code5 APK SHA632a402c…于ignored build/m08-evidence；先记录合成v1旧项目/金额/结束事实和generation/settings，再同身份签名覆盖code6，不卸载/清除旧数据。测试、fixtures用受控私有目录，不使用真实个人数据。迁移前后逐行比较，DB/WAL适用检查，旧项目继续可用、新订阅续费重启共存，临时guest设置恢复。

- [x] 原359与新增JVM/Android完整实跑，源码/XML/JSON逐方法同集；保留真正RED及诊断，编译/runner错误不算行为RED。
- [x] :app:testDebugUnitTest / :app:connectedDebugAndroidTest / :app:lintDebug :app:assembleDebug；仅必要测试schema assets配置，不升级依赖。核对最终code6APK、签名兼容/权限/九域排除/测试类打包和依赖差异。
- [x] 同代真实跨APK升级、六原案例和20项安全覆盖、预付150/180的1/19=5与150、1/20=5与330/60天、2/1=6；历史150→180→年付和146.83→148.10均保留。
- [x] CurrentState/Acceptance/Architecture/真正Decision/SpecIssues适用阶段/来源/契约/checkpoint/12项报告及全文件/links/secrets/source-hash审查，保留原43/130/16及旧证据。
交付Git流程：仅Conventional Commit/推送feat/m08-subscription-renewals；最终自身SHA/远端一致和干净工作区以提交后的Git日志与交付回执核对，不记录递归自SHA。READY_FOR_REVIEW — M08后立即停止，独立PENDING，不进入M09。

NOT_RUN：真机/API26/OEM/Release正式签名/完整TalkBack/真实LMK、M11/M12完整备份、M09及后续未授权流程。Q03～Q06及M09两风险/媒体清理M10/备份恢复M12/汇总M13原边界保留。内部指针format1不等于备份格式。

本轮实际收尾证据：[M08报告](../evidence/M08-verification.md) / [逐方法JSON](../evidence/M08-verification.json) / [checkpoint](../checkpoints/2026-10-13-M08.md)。原359方法身份保留；两个旧测试做真实v1/正式入口适配，另两个只加强完整状态等待和IME触摸同步，原断言及生产代码不改。编译/runner/准备/匹配/同步诊断均如实保留，最终455方法源码/XML同集PASS。

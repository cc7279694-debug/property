# M06 长期项目录入与投入闭环 — 实施契约

2026-10-12，编码前记录。用户本轮完整执行指令是直接实施授权；以下落实工程细节，继承冻结V1，不另行改变产品、技术路线或验收闸门。

**Goal：**连接M02统一成本、M04真实Room事务、M05原生界面，完成长期项目创建/资料编辑/详情/真实投入与收回，保存后相关页面一致刷新。

**Architecture：**既有AppContainer继续唯一持久存储owner。ProjectRepository是同一数据库/协调器/业务时钟上的轻量Facade，写入复用ProjectWriteRepository，读取按需、SQL分页及批量事件后调用ProjectCostEngine。Room变化驱动COUNT和当前页面重查；UI状态是可重建派生视图，不是第二份账本。

**Tech Stack：**原Kotlin/Compose/Nav3/Room/Coroutines/DataStore，冻结版本不升级，优先不新增依赖。

**Spec：**本轮用户附件SHA-256 `ac70a90d15afcdc42c8b43395037f77004beab32708d0bb8daf148cbaded3ee0`；[原M06](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m06.md)、[PRODUCT](original-v1/lithic_v1_plan/docs/specs/PRODUCT_SPEC.md)、[UI U01～U05/U10](original-v1/lithic_v1_plan/docs/specs/UI_SPEC.md)、[DOMAIN C01～C05/C13](original-v1/lithic_v1_plan/docs/specs/DOMAIN_CONTRACT.md)、[ARCH A02～A06](original-v1/lithic_v1_plan/docs/specs/ARCHITECTURE.md)及M05独立NOTES。

## Global Constraints / Dependencies / Out of Scope

- 已冻结M00～M05；起点 `4576b03565e58011e2fa78a086039fee33e3352f`，来源feat/m05-ui-foundation，本轮feat/m06-project-entry。实际本地/远端相同，工作区干净，目标本地/远端分支此前不存在。按用户指定工作目录在当前干净checkout创建新功能分支，不另建工作树或从main开始。
- schema v1四表/关系/索引/导出不变；不新增操作日志、迁移或数据库owner，不删除用户数据或generation，不改成本引擎/Money算法。
- 金额复用MoneyParser，以分Long；0合法、负绝对值/三位小数/超限拒绝，不使用Double。初始付款仅一条INITIAL事件，真实预付款可早于拥有日；未来付款依固定recordZone的BusinessClock拒绝。
- 只新增、看详情、编辑名称/开始日期/PROJECT类别/备注、追加普通投入/收回。编辑保留UUID、createdAt、结束字段、封面身份和所有旧事件；开始晚于既有结束日拒绝。
- 禁止M07结束/卖出向导/删除/资金历史编辑，M08订阅，M10媒体，M11/M12备份恢复，M13首页合计，M14类别CRUD/搜索/额外排序，替换、图表、年度、服务器/账号/网络/估价/建议。
- 原43规划/130案例/历史JSON和checkpoints不可变；Q03～Q06、M09两风险及M04残留代/M08版本预检/M12完整恢复边界保持。
- Debug code4/0.1.0-m06-debug；只提交推送feat/m06-project-entry，不合并main/force push/公开APK/修改WHOCARE。最终READY_FOR_REVIEW，独立验收PENDING，不进入M07。

## Review Focus / 关键失败类

1. 写入已提交但结果未到UI：原命令/事件/项目ID和内容持久于当前任务的SavedStateHandle，不生成第二笔；真实重放/并发/中断测试。
2. 编辑结果不明或陈旧基线：事务比较完整基线与稳定目标，未知时先核对/重放，不能无提示覆盖或换内容后当旧操作未发生。
3. 保存成功后读取失败：明确“已保存，暂时无法读取”，只重读/查看原身份，不重启创建流程；失败不能伪造空库/零。
4. 记录时区、预付款、未开始、未来结束和负净额：测试固定BusinessClock及真实Room，成本仅由冻结引擎给出。
5. 排序键被编辑/大数据/未保存返回：数据库50分页、批量事件、变化后窗口重建；顶部/系统/手势/Tab统一弃改守卫、大字体/IME和恢复测试。

## Task 1 — 数据Facade、资料CAS与有界读取

文件：既有data/local/ProjectDao.kt、data/repository/ProjectWriteRepository.kt、LedgerReadRepository.kt；新增data/repository/ProjectRepository.kt（必要时分出ProjectRepositoryModels.kt），新增对应Android真实数据库测试。Entity/schema、DataGenerationStore、SettingsStore、AppContainer生命周期不改。

锁定接口（字段可按单一职责分文件，不复制写实现）：

```kotlin
data class UpdateProjectCommand(
    val commandId: String, val expected: LongTermProjectEntity,
    val name: String, val startedOn: LocalDate, val categoryId: String,
    val note: String?, val updatedAt: Instant,
)
data class ProjectCursor(val startedOn: LocalDate, val id: String)
data class ProjectDetail(
    val project: LongTermProjectEntity, val category: CategoryEntity,
    val events: List<ProjectMoneyEventEntity>,
    val cost: ProjectCostResult, val asOf: LocalDate,
)
data class ProjectPage(
    val items: List<ProjectDetail>, val nextCursor: ProjectCursor?,
    val totalCount: Long, val asOf: LocalDate,
)
data class LedgerSummary(val projectCount: Long)

// 使用同一个db/coordinator与注入BusinessClock；不拥有关闭权。
ProjectRepository(db, coordinator, clock)
suspend fun createProject(command: CreateProjectCommand): MutationResult
suspend fun appendMoneyEvent(command: AddProjectMoneyEventCommand): MutationResult
suspend fun updateProject(command: UpdateProjectCommand): MutationResult
suspend fun loadProjectDetail(id: String, asOf: LocalDate = clock.today()): ProjectDetail?
suspend fun loadProjectPage(cursor: ProjectCursor? = null, pageSize: Int = 50,
    asOf: LocalDate = clock.today()): ProjectPage
suspend fun loadProjectWindow(pageCount: Int,
    asOf: LocalDate = clock.today()): ProjectPage
suspend fun loadSummary(): LedgerSummary
suspend fun loadProjectCategories(): List<CategoryEntity>
val changes: Flow<Unit>
```

MutationResult继续区分既有Success/ValidationFailure/Conflict/StorageFailure，并新增明确的`ProjectUpdated(projectId: String, replayed: Boolean)`成功分支，仅用于资料编辑；原Success.eventId继续保持真实资金事件身份，不用空字符串/虚构eventId代表编辑。读失败传播至显式UI错误，null仅代表经过成功查询确实不存在，不吞SQLite异常。

创建/追加继续原operationId唯一约束与事务，Facade不再次外包不可重入Mutex。资料更新在同一withWrite+Room事务：目标只由expected.copy允许字段形成；current==目标则成功重放，current!=expected则陈旧冲突；否则校验关系/日期/updatedAt严格递增后更新。expected包含全部保护字段，实际命令与时间在单次提交内固定；更新不动资金事件。

schema v1没有持久编辑操作日志：编辑按当前完整基线/目标可安全重试，不能宣称永久操作历史查重。M04创建重放仍对原项目/事件做完整内容检查；若创建已确认之后资料已被编辑，再重放旧创建会Conflict，保留该安全行为，不放宽为任意同ID内容通过；逻辑提交未确认时先处理原命令，不开启新的内容/ID。

分页按startedOn升序/id稳定平局；数据库最多读取50可展示行，允许额外1行判断下一页，游标仅日期+ID。单次事务批量读取本页ID的事件/类别、统一引擎求值，不逐行N+1。详情单项目取全部必要事件以求准确成本，UI LazyColumn虚拟化事件显示；不把展示字符串用于排序/计算。

首页只COUNT，正常启动不调用全量LedgerSnapshot；原snapshot()保留给既有数据测试/未来指定使用。Room真实变化Flow监视项目/资金/类别，相关当前页面统一重查；写入/排序变化后从第一页重建当前已加载窗口（每次数据库查询≤50），避免旧游标拼接重复/缺页。没有新搜索或用户排序。

内部审查补充：整个已加载窗口通过`loadProjectWindow`持有一次协调边界及Room事务，每页仍最多51行查询/50行显示、事件及类别批读。不得在页间释放锁后把不同提交的快照拼接；新增真实并发测试已复现旧循环101项目返回102行，修复与复验另记证据。普通前台恢复只重读，不自动丢弃冲突输入；只有用户明确点击读取当前资料重新编辑才确认冲突并换基线。

- [x] 新增真实Room测试与可编译接口，先运行取得新增读取/CAS/分页/刷新行为RED；已有正确创建/追加直接回归。
- [x] 实现最小Facade/DAO/CAS；校验0/1000、预付/未来、负净成本、旧事件/结束/封面保护、陈旧修改、重放/并发、51/100/1000与真实Flow。
- [x] 精确过滤新数据测试GREEN，保留完整XML/源码哈希及所有失败，不把编译/设备错误算RED。

## Task 2 — UI状态、提交身份与导航

文件：app/LedgerUiState/Holder/MainActivity/LithicNavigation/LithicRoutes，新增feature/project/ProjectViewModel.kt、ProjectUiModels.kt及对应恢复/状态Android测试。UI模型与Screen纯受控接口先锁定，再并行编辑。

LedgerUiState保留Initializing/Ready/Failed。Ready改为已成功读取的COUNT+recordZone+同一Repository接口（必要派生字段）；不用全量snapshot作为正常启动入口。Holder与ProjectViewModel仅派生UI状态，不新建/关闭存储owner；同一Repository/Room变更驱动数量、列表和详情。业务今天由ZonedBusinessClock(Clock.system(recordZone))或注入测试Clock得到，不在页面选设备默认时区/LocalDate.now。

ProjectViewModel与SavedStateHandle只保留一个活动表单：原始字符串、模式、稳定UUID、完整编辑基线、submitted envelope、提交阶段。有效首次提交在launch前同步固定所有身份/内容/Instant；busy阻止再提交/改内容。未知结果保持原envelope、明确待核对并锁定内容，只核对/重放同命令；确知未提交后才可修改为另一逻辑操作。验证失败保留原输入；SQLite错误/空间不足明确提示，不假定所有异常都意味着未写入。

保存结果与读取结果分开：Success后才跳转原项目详情；若后续重查失败，明确已保存且只能重读，不提供新创建。原命令已提交的恢复也跳到同一ID，不重新生成业务记录。旋转保留同VM；任务保存状态恢复保留输入/身份。force-stop是新冷启动，不冒称自动持久业务草稿或覆盖任意系统未保存状态的前台断电。

NavKey仅路线身份/规范UUID：Home/Review/Settings/ProjectList沿用；增加CreateProject、ProjectDetail(id)、ProjectEdit(id)、ProjectMoneyEntry(id,初始方向)。创建从Home或List进入时分别保留真实父链；成功进入[Home,List,Detail]；编辑/金额为[Home,List,Detail,Editor]，最多4项。无效序列归Home，缺失项目显示可返回错误，不崩溃。复选不无限压栈。

所有顶部/原生按键/手势/主Tab（含重选）通过同一requestLeave；dirty弹弃改确认、取消留输入；busy/结果未确认不能无声弃掉待确认操作。确认后的导航只丢UI未提交输入，不删除数据库事实。成功后原编辑器出栈。

原200测试方法全部保留并回归。M05阶段特有“详情与编辑尚未开放”断言改为真实详情可进入；Ready全量snapshot测试fixture按COUNT/真实Repository读取新接口调整，事实/错误/日期/导航断言不删。必要更改限旧NavigationShellTest/StorageUiStateTest，记录具体差异；禁止为过旧断言留下假生产分支，旧M05报告/JSON/checkpoint字节不改。

- [x] 先锁ProjectUiModels与公开VM方法，写真实Room/保存失败/稳定ID/恢复/guard测试和最小stub，root运行行为RED。
- [x] 接COUNT/Room变化、SavedState与事务结果；验证真实刷新、已提交后失败/结果丢失/同ID重试、旋转和陈旧编辑。
- [x] 接UUID有界Nav3及统一离开守卫，补恢复/原生返回/Tab保护；全部200方法继续有结果。

## Task 3 — 正式表单、列表与详情

新增feature/project/ProjectListScreen.kt、ProjectDetailScreen.kt、ProjectEditorScreen.kt、MoneyEventEditor.kt及小型日期/类别控件；HomeShell接真正新增入口；新增纯组件Android测试和真实ProjectEntryFlowTest。Theme/金额/卡片复用，不换品牌、无无效图片/结束/删除按钮。

Screen均消费ProjectUiModels，输出onFieldChange/onSave/onBack/onRetry/onLoadMore/onOpen/onEdit/onAddMoney回调；不访问DAO/创建Room或重新计算成本。

创建必填名称/拥有日/初始金额，默认PROJECT其他。付款日默认跟随拥有日，用户显式改付款日后不再强制跟随；实际0元与预付允许。编辑模式只有名称/拥有日/类别/备注，不编辑初始事件。金额表单是投入/收回、绝对值金额、真实日期、说明，没有细分类或未付计划。

日期字段保留原始字符串校验，选择器使用明确epochDay↔UTC日期内部值，不选设备时区；业务“今天”和未来付款校验仍用recordZone。选择器不能悄悄更改不支持的历史值，可保留日期文本输入。系统浅深、360dp/200%、48dp、IME避让/滚动、错误关联语义和忙碌态；不把合成回调当业务闭环。

详情用ProjectDetail.cost显示持有天数/投入/收回/净额/日均；所有数值来自ProjectCostEngine。未开始明确无有效日均，未来结束不提前结束；结束事实仅只读，不在本轮开结束操作；结束后结算不重新活跃。同日事件occurredOn/id排序，全部事实不丢，备注/类别清楚。

- [x] 写受控组件/DatePicker/大字体/IME/错误/忙碌/分页按钮测试，先最小stub取得行为RED。
- [x] 完成纯Screen后接真实VM/Repository；零元创建、1000一次投入、双击、失败保留、历史金额刷新五原案例都在真实Room闭环执行。
- [x] 设备演示从现在创建→列表→详情→投入/收回→编辑→返回更新，force-stop重开及断网，大字体/日期/失败状态留下XML与实际截图。

## Task 4 — 回归、核对与交付

- [x] 完整testDebugUnitTest、connectedDebugAndroidTest、lintDebug、assembleDebug（max-workers=2/ANDROID_SERIAL仅专用emulator-5596）。原200与新增方法/结果逐项核对；没有设备/NO-SOURCE不冒PASS，偶发失败查根因后复验。
- [x] 最终APK code4身份/版本/大小/SHA/Debug签名、合并Manifest无新增网络及原备份排除；schema v1 hash、generation身份和recordZone同代重启保持；依赖不升级，Release/真机/API26/OEM/TalkBack完整/真实LMK等未测明确NOT_RUN。
- [x] 更新唯一CURRENT_STATE/当前ACCEPTANCE、ARCHITECTURE/实际DECISIONS/适用SPEC_ISSUES/工具链、M06历史checkpoint和证据；原130/16完整补测仍NOT_RUN。内部代理审查不替代ChatGPT独立验收。
- [x] 查完整diff/链接/哈希/敏感信息/个人数据；只提交推送feat/m06-project-entry；交付11项报告、准确SHA/APK路径与未测边界。READY_FOR_REVIEW — M06后停止，不自行冻结或进入M07。


## 实际停止点

2026-10-12：全部本轮任务已执行；284个唯一方法与设备/APK结果见[M06报告](../evidence/M06-verification.md)。复验包含统一窗口竞态、冲突读取入口、迟到详情结果及首次编辑读取恢复。未适用未来能力没有因勾选本轮任务变PASS；原130/16完整补测不改。READY_FOR_REVIEW，ChatGPT独立验收PENDING，停止不进入M07。

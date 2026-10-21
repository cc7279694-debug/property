# 当前架构事实

## M15：只读全局长期复盘（2026-10-21）

`RankingRepository`是同一ManagedGeneration数据库/协调器/BusinessClock上的非owner只读入口。一次withSnapshot＋Room事务固定业务观察日，按50批读取该类型所有身份及完整资金/付款/状态事实，交给`RankingEngine`复用M02/M03，再全局稳定排序，最后取50×已加载页数。计算量随全部相关事实增长，排序O(n log n)，50只约束显示窗口；无每日快照、持久榜单或缓存表。不可计算项稳定排末且不授予有效排名，0和负值不丢弃，订阅只按已购标准天数。

`ReviewController`按请求序号与榜单隔离结果，刷新重建整个窗口；读取失败显式保留旧快照/日期并提示。`ReviewViewModel`按generation作用域创建，未打开榜单不启动全量排名；写入失效、跨日/前台触发刷新，代更换清旧ViewModel与导航，不让旧代结果展示。Nav3新增LongTermReview与REVIEW详情返回来源，所有编辑仍走原全局未保存/pending/恢复守卫。历史档案直接复用已结束项目和STOPPED订阅列表，不复制管理系统。

Room schema2、backup format1、冻结领域引擎、写入/恢复协议及依赖保持原样。M15当前候选待独立验收，M10延期和M13发行门槛不因此改变。


## M14：两套类别与名称搜索（2026-10-21）

`CategoryRepository`共用generation coordinator及Room事务。新建使用稳定类别UUID、同类型标准名唯一；改名核对原类别，合并预览核对源/目标/影响数后搬移关联并删除源。未知提交保留同参数重试；源缺失仅返回冲突，不冒称历史成功。fallback身份不变，金额、付款、状态不改。

`SearchDao`以转义LIKE绑定值仅搜索项目/订阅名称，type/ID排序、50+1 sentinel。`SearchRepository`一次Room读事务批量取本页事实，复用M02/M03导出状态；UI请求与写后刷新重建已加载窗口。类别/查询controller直接用于生产ViewModel，序号隔离迟到请求；旧generation被退休后不能写新集。当前项目保留startedOn/ID分页，历史按endedOn-startedOn+1降序与ID次排序，值等同冻结引擎实际持有天数。

首页增加顶部搜索轻入口，设置增加类别管理；沿用原三Tab、详情当前/历史栈及全局pending/discard守卫。新增ViewModel归既有GenerationUiScope，恢复后换epoch；首页、列表和详情继续依赖原Room invalidation刷新。类别字段与引用已在M11/M12 format1，不新增schema或备份字段。设备执行及大字体截图本轮NOT_RUN。



## M13：首页只读汇总（2026-10-20）

`ReportRepository`依附现有generation/owner和`LocalWriteCoordinator`；在锁内取得一个BusinessClock日期，使用同一个Room事务，以50个身份为一批读取完整金额/付款/状态事实。`OverviewEngine`复用冻结M02/M03，原精度BigDecimal相加；两类和类别同时产生，不从列表已加载窗口拼总额。仅新增ProjectDao只读孤儿计数，不改schema/业务写入。

`LedgerUiStateHolder`持有同一次overview，监听七表变更；请求序号及repository/generation身份阻止同代旧请求与旧代迟到结果覆盖。读取失败保留带日期的上次结果并明确反馈，不显示假零。初始化失败仍走原存储保护页。

现有Nav3/三栏/表单/恢复守卫保持。STARTED范围的日期观察器按记录时区下一午夜触发，最长60秒复核系统时钟变化；停止时取消，返回前台重读，不创建后台任务。跨日同时刷新项目/订阅和首页；M12恢复新代自动创建新读模型，不保存派生金额。

M13测试只通过显式`m13Isolated`测试构建选择plain Application runner，真实组件在新合成私有owner中运行，不启动默认账本。正式默认runner与M12故障runner入口保留，M13不执行其故障场景。Debug仅为内部候选，稳定签名/Release另设门槛。

## M12安全恢复候选增量（2026-10-20）

基于已独立冻结M11 `635e33a`、Room v2和backup format v1；不包含延期M10的Worker/Stage V2/JNI。以下为真实实现，执行结果另见当前验收登记；下方旧阶段的未实施描述保留为当时架构历史。

`RestoreCoordinator`只读复制SAF输入到独占私有缓存。`BackupValidator`在解压前核对ZIP中央/本地记录和完整压缩流、条目路径/重复/越界/版本/数量/字节预算、严格JSON字段及业务关联/聚合溢出。复用M11 DTO/manifest及冻结领域引擎，解析器另设UTF8名称4096B、单段255B、中央元数据16MiB准入预算；这不是总进程内存硬上限或格式版本升级。未通过的归档不修改当前代。

预览能力只在进程内绑定归档、完整旧事实/设置快照和提取文件身份；用户明确确认后消耗一次令牌。`RestoredGenerationWriter`只填充空的新代，在一Room事务写七表，设置和全部登记媒体原字节写完/校验/关闭/fsync/重新打开后才生成ready。`DataGenerationStore`持同一生命周期锁，在旧协调器内比对预览快照、退休旧任务并关闭旧Room/DataStore，再AtomicFile写SWITCHING(activeNew,previousOld)。新集首次打开全校验通过后写STABLE；失败只在持久身份明确时回到完整旧集并持久ROLLED_BACK/明确警告。未知指针拒绝，不生成空账本。

后续STABLE启动重新验证当前事实和媒体，不拿原恢复hash否定合法后续写入，也不静默回滚到陈旧旧集。唯一已知前代关闭后，先持久清空previous指针，才按完整树、UID/inode/单链接、SHA及ready身份清理；未知子项整代保留，不扫描其他代。中途删除失败只能留下已不可达的保守残留。ROLLED_BACK不触发旧目标清理；异常/进程终止的真实设备门禁仍分别登记。

`AppContainer`共用恢复/关闭互斥，旧写队列在代退休后拒绝；`LedgerUiStateHolder`按generation/repository拒绝旧回调，Nav3和子ViewModelStore在新代或同ID回退重开时换epoch，原表单SavedState不能写回新代。设置页系统选择、七表数量预览、取消/覆盖确认/结果反馈均通过现有Compose组件实现；恢复URI、令牌和确认命令不跨进程重放。

SQLite、DataStore和文件系统不是一个事务；未知/中断私有准备文件保留。format1当前只含M11已实现的记录时区设置；M20通知水位及系统权限不伪造恢复。


## M11-B候选增量（2026-10-20）

基于冻结M11-A 79771fc。`RoomBackupSnapshotSource.captureInto`在同一协调器持有期内将七表/设置交给媒体快照；源文件逐项核对只读FD、UID/dev/inode/nlink及前后身份、原字节长度/SHA，再对内部副本做格式和尺寸检查。复制前核对媒体条目数和累计字节预算，未知/变化拒绝，不对源文件重新编码或写入。

`BackupExportService` → 完整manifest/ZIP → 本地ZipFile中央目录和条目核验 → 本次SAF URI写入/关闭 → 全量读回匹配。成功和取消结果在原调用方检查点与安全返回边界处理，验证成功后不会因返回时取消丢失结果而误删已完成文件。内部未知残留不递归扫除；目标删除只限当前手动选择的失败尝试。设置页只开放导出，恢复未开放；进程重启不自动重跑。

本轮已修正备份与导入的边界：登记JPEG按原SHA/长度、SOF0/1/2容器和实际尺寸/受限可读性检查，PNG按合法颜色/色深、Adam7、CRC/zlib与实际尺寸检查。两者都保持源字节，不以Bitmap成功证明JPEG熵流严格完整。不能明确支持的格式、缺失/变化/不匹配仍拒绝整份导出。未知残留不自动清理；M10缺损JPEG导入问题仍PENDING / DEFERRED。

真实API36发现应用挂载命名空间中`/data/user/0`表现为系统链接，导致内部缓存的严格祖先检查失败。仅将可信Context数据/cache根经过UID/真实目录和精确父子归属检查后canonical化，再执行原有严格祖先门禁；一般媒体路径和payload门禁未放宽。生产修复后16项定向Android通过，新的测试包再完成1项实际MainActivity→SAF导出，独立读取实际归档并核对七表、设置和5张图片原字节。本轮仍为READY_FOR_REVIEW，未加入M10 Worker/Stage V2/JNI，Room/schema/领域规则不变。历史报告中的NOT_RUN保留其当时状态。

更新日期：2026-10-20。本候选从已冻结M09回写`57de897917473f255d43dfb306a8433b366c00eb`演进；M10暂停且未合并，UI-C1冻结分支保持独立。实际状态见[CURRENT_STATE](CURRENT_STATE.md)。下文M09及更早阶段的执行数字保留为历史，不是本轮重新运行的Android结果。

## M11-A：一致业务快照与导出核心（以下保留A冻结时范围）

`ManagedGeneration.backupSnapshotSource()`提供只读入口。`RoomBackupSnapshotSource`取得原`LocalWriteCoordinator.withSnapshot`，在同一持有期读取SettingsStore的记录时区并完成Room事务七表读取；先核对总记录上限、分400个订阅ID读取关联历史，再逐表核对完整行数。DTO只保留持久事实，以字符串ID、人民币分、ISO日期/时间与显式枚举表示；不存派生成本。Settings仅含M09实际存在的`record_zone_id`，不制造尚未实现的偏好或通知水位。

内部`BackupExporter.prepareCore()`将脱离数据库的快照流式写为私有`data.json`和`settings.json`，关闭后重新读取校验长度/SHA；format v1 manifest与内部ZIP写入器核对实际字节并传播目标写入/关闭异常。内部无媒体合成归档只用于验证核心；`exportComplete()`始终返回`MEDIA_INTEGRATION_REQUIRED`且不打开目标，没有正式UI/SAF成功入口。

成功候选有显式`close()`，只删除经私有随机凭据、精确子项、内容哈希确认的本次文件；未知/变化残留拒绝清理。Windows的Basic fileKey实际为null，不能作为inode证明；此处是单owner内部临时文件责任，不是M10媒体GC协议。准备中断保留候选，逐文件清理不具多文件原子性，尚无跨进程恢复/自动扫描。

本阶段尚未复制封面字节，未证明媒体/Room/DataStore跨资源完整快照；后续必须在同一协调边界复制已验证不可变媒体，或经独立审查的等价租约方式，再接SAF和用户界面。Room schema、DAO、领域引擎、UI及generation切换协议均未改变；完整M11及M12仍未完成。真实Room/DataStore/SAF和设备验证仍NOT_RUN，不能用JVM核心结果替代。

## M09：订阅状态、同身份重开与付款历史修正

M09在既有单个 `:app`、原生Compose/Navigation3、Room schema v2七张业务表上演进。没有新表、索引、迁移、第二数据库owner或独立成本/状态引擎。Project与Subscription仍共用AppContainer拥有的ManagedGeneration、LocalWriteCoordinator及SettingsStore持久记录时区；SubscriptionRepository不拥有关闭/替换数据集的权力。M08显式Migration(1,2)和已知数据库结构预检继承，schema v1/v2导出保持基线。完整备份/恢复仍未实现。

```text
Compose / Navigation3（现在｜回看｜设置）
  ├─ ProjectViewModel ────── ProjectRepository ────── ProjectCostEngine
  └─ SubscriptionViewModel ─ SubscriptionRepository ─ SubscriptionCostEngine(M03)
               │                       │
  单任务SavedState Bundle       LocalWriteCoordinator + Room transaction
  UUID/来源NavKeys                        │
                                  同一generation / Room v2
                                  subscriptions
                                  subscription_renewals
                                  subscription_state_changes
```

### 持久写边界与精确事实

[SubscriptionLifecycleCommands](../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionLifecycleCommands.kt)定义完整SubscriptionFacts基线、五种用户状态动作、六个事务命令及结果。基线包括订阅元信息、全部续费和全部状态；UUID/关联、重复实体/操作/日期及领域结构先校验。列表顺序没有业务含义，完整字段/数量/集合相等才算CAS一致，不以“最后一条”或单个commandId代替旧事实核对。

`setState`、`updateState`、`cancelScheduledStop`、`updateRenewal`、`deleteRenewal`、`reenableWithRenewal`均在原协调写边界的一次Room事务内执行，事务内重读及校验后才写，末尾再核对完整目标已存事实。付款修正保留原Renewal ID/Subscription ID/operationId；状态同日修正保留原State ID/operationId，不追加第二条日末边界。单笔错误付款删除拒绝最后一笔，计划取消仅允许尚未生效的STOPPED。异常写入使同事务副作用回滚；取消传播，冲突、校验、磁盘满和数据库失败明确返回。

精确已存目标识别先于重新计算业务今天和默认停止日期。因此保存信封跨日恢复不会悄悄迁移原日期。完整已存事实才能返回replayed；schema v2没有永久操作日志，付款删除及计划取消返回CurrentAbsence只证明当前目标缺失，不证明历史操作是谁完成。SavedState不是永久审计库。

### 状态、真实付款与固定停止

派生状态、当前价格/贡献、累计真实金额、标准已购时长、历史平均和实际有效付费天数全部复用冻结 `SubscriptionCostEngine.evaluate(snapshot, asOf)`，没有持久派生金额或另一套状态真值。ENABLED有真实当前覆盖才ACTIVE；无覆盖EXPIRED/currentPrice空/贡献0。PAUSED仍在当前管理、无覆盖时仍主状态PAUSED并说明覆盖到期。实际已生效STOPPED移至回看历史，未来停止在生效日前仍依当日历史状态及真实覆盖计算。

用户确认A：停止不销毁服务身份。当前覆盖内重开ENABLED不增加付款；只有未来已付覆盖时重开仍EXPIRED直到真实生效；无当前/未来覆盖时原订阅ID＋明确真实付款＋ENABLED在同事务提交。UI金额与真实付款日不自动填入。失败不能留下只启用或半笔付款。

用户确认B：到期后停止确认时固定为全部已确认付款最晚paidUntilExclusive与业务今天较晚者，预付空档也不变。显式提前停止只允许今天/未来更早日。后续新付费或扩张纠错如跨未来固定停止，则要求明确修正/取消原计划；不自动延长，不退款。已有未来停止不能再用新的普通停止身份遮蔽。允许修正原已截断付费区间的金额，不能把原存付款误当新增覆盖拒绝。

### 管理/历史查询与坏数据

[SubscriptionDao](../app/src/main/java/com/cooper/lithic/data/local/SubscriptionDao.kt)在LIMIT前按asOf最近已生效状态边界筛选current/stopped，未来STOPPED保留当前，PAUSED保留当前。当前管理UI再按冻结引擎ACTIVE/PAUSED/EXPIRED分组；Review打开stopped历史。稳定排序沿用付款日期降序＋UUID升序；真实SQL每页50展示＋1哨兵，关联事实按可见ID批读，已加载窗口一次协调快照/Room事务重建，避免混合页。

读取及生命周期写入先检查完整持久事实，隐藏在筛选或当前页外的错误身份、类别、状态和区间不能被伪装成空历史/正常EXPIRED。预检先检查孤儿引用，再以稳定UUID keyset分50个身份加载关联类别/付款/状态并释放上一批。**该过程仍O(total facts)，并且单服务完整时间线仍需内存加载；不是长期性能基准或固定O(50)总耗时。**asOf之前观察必须忽略paidOn晚于观察日的后来真实付款，不能因为查看历史就把已存真实记录判坏。

### 单表单、保存确认与Navigation3

[SubscriptionViewModel](../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt)继续只拥有一个活动表单；[SubscriptionFactsCodec](../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionFactsCodec.kt)将原始持久事实放进已有SavedState Bundle，不保存第二份派生账本。状态/付款纠错/取消/删除/原子重开共享raw、固定command/fact UUID、原基线、固定submitted信封及SubmissionPhase。写前先固定Instant和整个信封；IN_FLIGHT锁定重复与输入，恢复为UNCONFIRMED只核对/重放原信封。COMMITTED只读，不再次写付款；明确读取失败显示已保存暂不可读，输入/信封保留。

已存目标后来被其他操作改变与读取异常分开处理：保留COMMITTED原信封，提示明确核对当前事实；显式核对后只能安全返回查看，不覆盖新事实或用旧结果导航。COMMITTED读和显式冲突读均捕获detailVersion，迟到结果不能重定向后来打开的详情，不能解除原pending保护。

[SubscriptionStateDialog](../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionStateDialog.kt)使用滚动和IME避让的确认页，显示状态生效日、可选原因/说明及“只改变本App记录，不替服务商取消自动续费”。[SubscriptionDetailScreen](../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt)提供适用暂停/恢复/停止/重开、未来计划修正取消、逐付款纠错删除入口。[SubscriptionHistoryScreen](../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionHistoryScreen.kt)从回看打开原ID；普通续费与原子重开付款沿用现有[RenewalEditorScreen](../app/src/main/java/com/cooper/lithic/feature/subscription/RenewalEditorScreen.kt)，四个真实日期独立。

NavKeys仅序列化规范UUID/目标事实UUID、动作及来源enum；current链最多四级，history为Home/Review/History/Detail/Editor最多五级。成功来源来自新读取的M03状态，立即停止转历史、明确重开转当前；未来停止仍当前。项目/订阅双ViewModel全局离开守卫先pending后dirty，统一工具栏、Tab重选/切换、系统/手势返回、根返回和内部成功导航；恢复隐藏表单有返回核对入口。非法恢复栈归Home，不暗中丢弃待确认付款。

### 当前验证边界

上述为已读源码的实际装配与行为边界，不能以实现存在替代模块测试通过。M09最终四任务549/549 PASS、源码/XML及构建输入一致；所有早期RED和全量失败证据保留。最终APK与专用API36旅程、小屏IME/旋转/手势/已保存任务恢复和设置收尾已留证，不由内部辅助审阅授予独立冻结。两项M09流程风险按用户确认及实际专项验证关闭；Q03～Q06 原状态与期限保持。M10 媒体、M11/M12 完整备份恢复及 Q06 切换回退、M13 汇总、M20 通知/Worker 未实施。真机、API26、OEM、Release 正式签名、完整 TalkBack、真实 LMK 未验证；SavedState 不承诺 force-stop、断电或系统未保存状态后的恢复。

## M08：固定周期订阅与真实续费（冻结继承事实）

本节保留 M08 交付时的结构和验证语境；其中“不提供 M09 状态入口”“四个页面”及“最多四级”描述 M08 当时范围，当前 M09 演进见上节。M08 已依据用户转交的独立验收结论冻结，其历史证据与 NOTES 保留。

[SubscriptionRepository](../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionRepository.kt)是非 owner 的轻量读写入口，与 ProjectRepository 使用同一个 ManagedGeneration 的 Room、LocalWriteCoordinator 和持久 recordZone 派生的 BusinessClock。[LedgerUiStateHolder](../app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt)装配两个 Repository，先成功读取时区和两类真实 COUNT 才发布 Ready；后续合并两者的 Room 变化流刷新数量，读取失败保留上次结果并明确错误，不回落为假 0 或空账本。AppContainer 仍是唯一存储所有者，两个业务 ViewModel 和页面均不创建、关闭或替换 generation。

```text
Compose / Navigation 3
  ├── ProjectViewModel      → ProjectRepository      → ProjectCostEngine
  └── SubscriptionViewModel → SubscriptionRepository → SubscriptionCostEngine
                                  ↓
                 同一个 LocalWriteCoordinator + Room v2
                                  ↓
                   原四张表 + 三张订阅业务表
```

[Room v2](../app/src/main/java/com/cooper/lithic/data/local/LithicDatabase.kt)新增[三张实体表](../app/src/main/java/com/cooper/lithic/data/local/SubscriptionEntities.kt)：`subscriptions`只保存身份、类别、可选封面引用、备注与写入时间；`subscription_renewals`逐笔保存真实付款、该次金额/周期/standardDays/operationId，以及 paidOn、effectiveFrom、paidUntilExclusive、reminderDueOn 四个独立日期；`subscription_state_changes`保存状态时间线结构。类别、封面和父订阅使用 RESTRICT 外键；续费/状态 operationId 分别唯一，同订阅同生效日的状态唯一。付款和有效期查询索引及稳定 id 次排序见[DAO](../app/src/main/java/com/cooper/lithic/data/local/SubscriptionDao.kt)。M08 仅读取状态事实，不提供暂停、恢复、停止写入口；空状态历史仍由冻结 M03 默认为 ENABLED。身份表不保存第二份当前价格、周期、状态、累计金额或派生成本真值。

[显式 Migration(1,2)](../app/src/main/java/com/cooper/lithic/data/local/DatabaseMigrations.kt)已通过 `addMigrations(*DatabaseMigrations.ALL)` 注册，只创建三张新表及索引；原四张表的结构与业务行不改。[v1 导出](../app/schemas/com.cooper.lithic.data.local.LithicDatabase/1.json)保持原字节，SHA-256 为 `351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`；[v2 导出](../app/schemas/com.cooper.lithic.data.local.LithicDatabase/2.json)包含全部七张业务表。已有库先检查 SQLite 文件头，再用保留文件的只读 SQLite 打开，通过 SQL 读取包含已提交 WAL 的 user_version 和 Room 身份。正式 writable Room 之前，[结构预检](../app/src/main/java/com/cooper/lithic/data/local/DatabaseSchemaValidation.kt)只接受已知 v1/v2 的正式或 legacy 身份，并检查实际对象集合、列及主键、外键、命名索引和自动主键索引；索引键列、升序及 BINARY collation 必须一致，拒绝 NOCASE、表达式/部分索引与隐藏/生成列。对象筛选使用 `name NOT GLOB 'sqlite_*'` 的字面下划线前缀，不用 LIKE 通配符误放过额外用户表。预检和正式打开均检查 quick_check/foreign_key_check；未知、伪造身份而结构不符或损坏库明确失败，不先迁移或初始化它们。

保留名称对象筛选已限定仅排除两张 table，同名 trigger 仍参与拒绝；两个名称 × v1/v2 四项先取得有效行为 RED，最终22项迁移测试全部通过。额外表前缀、PK BINARY 和异常触发器均在正式可写打开之前被拒绝，旧版本、身份、schema 及全部业务事实保留。实际证据见[M08报告](evidence/M08-verification.md)。

迁移由 Room 升级事务执行，失败保留原库；正式开库继续使用保留原文件的 corruption handler 并禁用 data-loss recovery，没有 destructive fallback。[DataGenerationStore.openExisting](../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt)按既有 active 指针在同一代升级，不用新空 generation 替代旧数据；pointer、ready、settings、recordZone 与媒体文件的职责和格式不变。SQLite、DataStore 和文件系统仍不是一个跨存储原子事务，M12 的完整恢复切换与回退不由这次 schema 升级代替。

[创建与续费命令](../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionCommands.kt)在同一协调写入边界和 Room 事务内校验规范 UUID、operationId 与 commandId 对应、SUBSCRIPTION 类别、正数有界金额、固定月/季/年周期及标准天数、付款不晚于业务今天，以及包含已付未来期的全部覆盖不重叠。创建身份和首次付款一起提交；续费只追加用户确认的真实已付款，不覆盖旧套餐、不自动到期付款。精确重放比较完整已存事实，异内容同操作/实体身份返回冲突；取消继续传播，校验、关联冲突、磁盘满与数据库/损坏失败明确区分。SQL 外键不被当作类别类型或日期业务校验。

列表按真实 `MAX(paidOn) DESC / subscription.id ASC`，付款历史按 `paidOn DESC / renewal.id ASC`；每页 SQL 50 条展示加 1 条哨兵。类别、全部必要续费和状态按可见订阅 ID 批读，已加载窗口在一次协调边界及 Room 事务内重建，避免付款改排序后混合页。历史分页只限制显示行，成本计算仍将完整必要事实映射到冻结 [SubscriptionCostEngine](../app/src/main/java/com/cooper/lithic/domain/subscription/SubscriptionCostEngine.kt)，不拿当前页当完整分母。无付款身份、缺失关联和读取异常明确报错，不用 INNER JOIN 隐藏异常身份或伪装成正常空列表。`currentRenewal`仅在引擎判为 ACTIVE、paidOn <= asOf 且真实覆盖包含 asOf 时提供；已付未来期计入观察日已知金额/折算时长，直到生效才切换当前价格，也不提前增加实际经过天数。

[SubscriptionViewModel](../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt)只管理派生读状态和单活动任务表单。SavedStateHandle 的 `lithic.subscription.activeForm`保存 raw、稳定 command/subscription/renewal UUID、阶段和固定 submitted Bundle，写入前固定 Instant 并同步保存完整命令。IN_FLIGHT 禁止重复和修改，恢复为 UNCONFIRMED，只核对/重放原信封；返回身份不匹配不能导航。COMMITTED 已持久保存，只能重读；核对同订阅和完整原付款成功后才发成功导航，读失败显示“已保存，暂时无法读取”，不能再次写付款。初读只补未输入字段并校验请求身份，迟到详情按 id/版本丢弃；显式冲突核对保留原 raw/身份/信封，精确已存事实可确认成功，明确不能写才允许安全返回，读取异常继续保留未知结果。Room 变化和 ON_RESUME 刷新已请求的窗口/详情，前台恢复不确认冲突或制造付款。SavedState 不是新的账本、持久业务草稿或 force-stop/真实 LMK 恢复承诺。

[四个正式受控页面](../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionEditorScreen.kt)为订阅列表、详情、创建和真实续费；分别见[列表](../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionListScreen.kt)、[详情](../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt)、[续费表单](../app/src/main/java/com/cooper/lithic/feature/subscription/RenewalEditorScreen.kt)。创建和续费均是滚动表单，四日期独立输入，日期默认值只供核对，明确说明半开付费边界；页面不写 DAO 或复制公式。详情分开历史平均、折算付费时长、当前有效套餐和实际经过时间，未来期标“尚未生效”，没有有效套餐不展示假当前价。复用既有 AmountText、日期控件、主题、安全区、IME 避让和至少 48dp 操作目标，没有 M09 状态操作入口。

[NavKeys](../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt)新增 SubscriptionList、CreateSubscription、SubscriptionDetail(UUID)、ConfirmSubscriptionRenewal(UUID)，只序列化路线/UUID。订阅 Home/List/Detail/Editor 最多四级，创建保留 Home 或 Home/List 来源父链；旧项目 current 四级和 history 五级保留。[全局离开规则](../app/src/main/java/com/cooper/lithic/app/SubscriptionNavigationPolicy.kt)汇总两个 ViewModel：任一 pending 优先阻止，其次任一 dirty 提示弃改。工具栏、业务入口、Tab 含重选、系统/手势返回，以及仅剩 Home 的系统返回均走同一守卫；确认弃改前再次检查另一业务的 pending。隐藏恢复的待确认表单有返回核对入口，保留原表单；成功精确事实读取与返回待核对表单是窄内部导航路径，不能绕过另一业务的待确认付款/项目保护。

## M07：继承的生命周期、纠错与历史档案（冻结历史）

M07 实施时沿用唯一 AppContainer、Room v1、LocalWriteCoordinator 和 BusinessClock；M08 仅在同一代将 Room 升至 v2，以下项目行为继续继承。[生命周期命令](../app/src/main/java/com/cooper/lithic/data/repository/ProjectLifecycleCommands.kt)由原ProjectWriteRepository执行：结束和可选END_SETTLEMENT、结束与原关联款纠错、普通/INITIAL历史金额纠错、项目和全部金额删除均在同一Room事务内。项目及目标事件使用完整字段CAS，nullable字段用SQL `IS`，影响行数不为1即抛出冲突使此前副作用回滚。纠错保留项目、事件、方向、角色和operationId；所有正式写入口拒绝旧多关联歧义，旧addMoneyEvent仍可向已记录结束的项目增加首条合法关联收回。

删除先比较完整项目及全部事件快照，按事件→项目逐行删除并检查；Absent仅证明当前项目/事件不存在，不是持久删除日志。媒体元数据和文件均留给M10，不清理共享或未引用资源。M07 当时 schema、索引、领域计算与持久 generation 指针没有变化；M08 不改原四表、项目索引、引擎或指针。

ProjectRepository保留旧全项目读取API，新增current/history窗口，分别用结束边界与观察日在SQL筛选；50展示/51哨兵、稳定拥有日和UUID排序、事件/类别批读、整个已加载窗口单事务。Home仍是所有已录入项目COUNT；基本历史不实现分析、排名或成本总线。详情唯一调用被冻结ProjectCostEngine，结束后结算不延长持有天数；只有真实事件满足endedOn<occurredOn<=asOf才说明已包含结束后结算，未发生的未来事件不成为当前事实。

原ProjectViewModel的单活动Bundle增加四种表单及完整项目/事件基线；提交前固定command/event UUID、revision与submitted。恢复IN_FLIGHT为UNCONFIRMED，核对/重试原信封；COMMITTED读错只重读。冲突仅明确点击读取后重新确认，删除陈旧快照不自动变成可删除的新快照。SavedState仍只是已保存任务状态，未成为持久操作日志或永久草稿。显式冲突重读成功确认目标不存在后，普通表单保留原raw/身份/信封、禁止保存并允许安全返回；删除通过原submitted由writer核对项目与全部事件均缺失，读取异常仍为未知。活动表单在本地构造完整下一状态后单次发布，避免中间空状态。

Nav3仅序列化UUID/来源enum：当前链最多4级，历史Home/Review/History/Detail/Editor最多5级；原路线来源默认CURRENT。结束及纠错回同UUID详情，删除/核对Absent返回对应来源列表并清除旧详情。结束和金额纠错是滚动全屏表单，删除是明确不可撤回的AlertDialog二次确认。当前实现的真实验证与独立验收状态见[CURRENT_STATE](CURRENT_STATE.md)及[验收登记](ACCEPTANCE_MATRIX.md)，本段不自行授予冻结。

## M06：继承的项目读写闭环（冻结历史）

[ProjectRepository](../app/src/main/java/com/cooper/lithic/data/repository/ProjectRepository.kt)复用同一Room、LocalWriteCoordinator和BusinessClock；创建/追加调用原ProjectWriteRepository，不外套第二次非重入锁。资料更新在写协调边界和Room事务中比较完整expected与固定target：目标已存在则重放、陈旧基线则Conflict；只更新名称、开始日、类别、备注和updatedAt，保留旧事件及保护字段。M06 当时的 schema v1 没有持久编辑操作日志，当前 v2 也未新增该类日志。

[ProjectDao](../app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt)按startedOn升序/id平局游标查询，每页最多51行，其中50行可显示；事件和类别按本页ID批读。`loadProjectWindow`在一次协调边界及读取事务中重建已加载窗口，避免页间排序修改混合不同提交。首页只COUNT；正常业务路径不调用全量snapshot。单项目详情取得必要事实后，唯一调用ProjectCostEngine计算，UI不复制公式。

[ProjectViewModel](../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt)观察Room项目/事件/类别变化，刷新已请求的列表和详情；ON_RESUME只重读，不确认冲突或改变提交。详情发布核对项目ID和请求版本。初次资料读取恢复只补缺失字段，保留已有原始输入；已submitted的基线绝不换掉。

SavedStateHandle只存当前任务单活动表单的原始字符串、UUID、完整编辑基线和固定submitted Bundle（primitive/ISO字符串），提交前同步存入。IN_FLIGHT阻止重复和修改；恢复/异常为UNCONFIRMED，只重放原身份内容。已确认成功与读取失败分开，已保存只允许重读；明确CAS冲突由用户点击核对后重载当前资料。该状态不是新的账本或后台持久草稿，也不承诺force-stop后保留未保存草稿。

正式表单、列表、详情只消费派生状态与回调；真实日期保留文本，DatePicker内部使用UTC日期转换，业务今天仍来自持久recordZone。编辑初次读失败有明确重读入口且不允许无基线保存。顶部、主Tab重选/切换、系统和手势返回统一处理dirty/待确认守卫；M06当时未增加结束、删除、历史金额编辑或媒体入口，M07 在其授权内扩展前三者，M08 继承项目行为并汇总双业务守卫。

真实SQLite页数耗尽有两种已观察到的错误：直接SQLiteFull可归DISK_FULL；嵌套事务收尾有时遮蔽为普通DATABASE的“cannot rollback”错误。生产保留明确未知结果及原命令，不按字符串猜分类、吞异常或清库；当前验证必须证明既有行不变、无部分记录、同命令恢复重试仅一次。实际证据与未测边界另记验收，不由此授予冻结。

## 当前实际存在的工程结构

单 Gradle `:app`、单业务 `MainActivity` 与 `LithicApplication`，Kotlin + Compose + Navigation 3。当前沿原[Activity](../app/src/main/java/com/cooper/lithic/app/MainActivity.kt)和[Nav3 宿主](../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt)接入长期项目闭环、生命周期/历史，以及 M08 订阅创建/真实续费和 M09 状态/重开/付款纠错历史。Debug 仍另含空 ComponentActivity 测试宿主；Release 仅按配置排除，尚未实际构建。

```text
LithicApplication
  → AppContainer（唯一存储所有者）/ StorageInitializationState
    → MainActivity / LedgerUiStateHolder（仅UI生命周期）
      → 两类真实 COUNT + SettingsStore记录时区 / 同代两个 Repository
        → LithicTheme / LithicApp：Initializing、Ready、Failed
          → Ready才进入Navigation3 / 三主入口 + 两个轻量业务 ViewModel
```

三个主入口为现在/回看/设置；首页只查真实项目和订阅数量，提供两类正式管理/新增入口；业务页面按需读取事实并展示各自冻结领域引擎结果。总成本汇总、分析、媒体处理、完整备份/恢复和提醒仍未开放。支持系统浅深、safeDrawing、IME避让和滚动；图标仍为临时工程图标。

字体使用设备系统字体。AndroidX Emoji2的自动初始化默认会查询可下载字体提供者，因此按[官方说明](https://developer.android.com/develop/ui/views/text-and-emoji/emoji2)及实际1.4.0源码，在Manifest合并中仅移除EmojiCompatInitializer；不手动初始化下载配置。保留其他Startup初始化器，既有最终APK检查脚本包含Emoji初始化入口移除验证，M08 最终 APK 检查为历史证据；M09 最终产物须另行核验并登记当前执行结果。

正式 namespace/applicationId 为 `com.cooper.lithic`；Debug 为 `com.cooper.lithic.debug`，名称含 Debug，仅本地调试签名。没有生产签名配置。[Manifest](../app/src/main/AndroidManifest.xml)与历次最终 APK 的网络/存储/通知/精确闹钟权限检查见[当前验收登记](ACCEPTANCE_MATRIX.md)，M08 模块独立验收与历史最终 APK 执行证据保留，独立审查未核验 APK 字节；M09 最终产物不能用旧结果替代。既有检查的唯一 uses-permission 来自 AndroidX Core 的签名级 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，用于保护内部接收器；不提供网络或用户数据访问权限。ProfileInstallReceiver 上的 DUMP 是调用方须持有的权限，不是 App 申请 DUMP。

系统备份配置包括 `allowBackup=false`、[旧规则](../app/src/main/res/xml/backup_rules.xml)和[新规则](../app/src/main/res/xml/data_extraction_rules.xml)；旧规则以及 cloud-backup/device-transfer 均排除九类域。M01～M08的合并 Manifest 和最终 APK 编译资源历史证据保留，M09最终APK仍须另行核查；不据此宣称所有 OEM 的迁移行为均已验证。

M04存储所有者、generation与固定记录时区保持；M06/M07项目 Repository 和 M08 订阅 Repository 复用同一写协调器，领域引擎不改。当前 schema 为 v2，无完整备份格式版本；没有图片处理、通知、WorkManager 或完整 Backup/Restore。实际验证以[验收登记](ACCEPTANCE_MATRIX.md)为准。

## M02：纯 Kotlin 领域计算

已实现[金额模型与解析](../app/src/main/java/com/cooper/lithic/core/model/Money.kt)、[领域模型](../app/src/main/java/com/cooper/lithic/core/model/DomainModels.kt)、[业务日期注入](../app/src/main/java/com/cooper/lithic/core/time/BusinessClock.kt)和[长期项目成本引擎](../app/src/main/java/com/cooper/lithic/domain/project/ProjectCostEngine.kt)。它们只有Kotlin/JDK依赖；M06由Repository映射Room事实至ProjectSnapshot后调用，领域层没有反向依赖UI或数据库。

```text
ProjectSnapshot（开始/可选结束日 + 全部真实金额事件）
  + asOf（明确观察日）
  → ProjectCostEngine.evaluate
  → ProjectCostResult（Long 天数/分、BigDecimal 元每天、日末贡献、不可用原因）
```

单笔 Money 为 0～999,999,999,999 分的非负绝对值；投入/收回由独立方向表达。解析拒绝负号、科学计数、超过两位小数和越界值，不做浮点转换或偷偷舍入。总投入、总收回分别用 `Math.addExact` 检查 Long 溢出，净额允许负数；非法构造抛 IllegalArgumentException，聚合溢出抛 ArithmeticException，解析返回类型化 Failure。

引擎只纳入 `occurredOn <= asOf` 的事件；开始前保留已发生的预付款总额，但天数为0、日均为null、原因PROJECT_NOT_STARTED且无贡献。开始日至观察日含首日；结束日起分母固定为含首末日的完整天数，结束日日末不再贡献。后结算只改变净额。日均以元/天及 DECIMAL128（34位有效数字）计算，不做展示两位舍入，0.005/-0.005保留原符号。M05通用AmountText只展示调用方数值，不在页面计算/持久化成本或接入M13汇总。

BusinessClock可注入固定LocalDate或明确时区的JDK Clock；记录时区由M04 SettingsStore持久保存。M06读写共享该业务时钟，前台恢复重读今天。初始投入只是一笔事件；稳定操作ID区分重放与独立付款，不按相同日期/金额删事实。派生结果不保存每日快照。

M02 测试使用 `testImplementation` 的 JUnit4.13.2；实际运行结果与构建证据见[当前登记](ACCEPTANCE_MATRIX.md)及[M02证据](evidence/M02-verification.md)，不以源码推断通过。M02 当时没有订阅引擎；本轮 M03 继承其已冻结输入，没有修改 M01 Activity、导航、资源、Manifest和系统备份排除配置。

## M03：纯 Kotlin 订阅领域计算

已实现[订阅模型](../app/src/main/java/com/cooper/lithic/domain/subscription/SubscriptionModels.kt)、[区间/写入校验](../app/src/main/java/com/cooper/lithic/domain/subscription/SubscriptionIntervalRules.kt)和[订阅成本引擎](../app/src/main/java/com/cooper/lithic/domain/subscription/SubscriptionCostEngine.kt)。复用 Money 与 BusinessClock，无 Android Context、Room、UI、数据库或新依赖。

```text
SubscriptionSnapshot（身份 + Renewal 列表 + 状态变化历史）
  → validateForWrite(snapshot, BusinessClock)：结构 + 付款不在业务未来
  + asOf → evaluate：仅结构校验 + paidOn <= asOf 的付款
  → SubscriptionCostResult（状态、当前贡献/价格、已付金额、标准时长、历史平均、实际有效日期数）
```

Renewal 是领域事实，保存正数 Money、cycle、standardDays 与四个独立日期；月/季/年标准固定 30/90/365。付费覆盖是非空半开区间，重叠/重复覆盖拒绝、邻接/空档允许；写入时付款不能晚于 BusinessClock 今天。历史 evaluate 不读取今天，不拒绝观察日之后的真实付款，而是不将其纳入该观察日结果。没有按支付日或提醒边界延长服务覆盖。

同日状态重复明确拒绝，调用方修正时替换原记录；不按输入顺序猜最后一项。无状态历史默认为 ENABLED。观察日状态从生效日期推导，PAUSED/STOPPED 优先于无覆盖判断；ENABLED 加有效已知覆盖为 ACTIVE，否则 EXPIRED。当前贡献 currentRate 非活跃为0，currentPrice 为 null；无已确认付款时 historicalAverage 为 null，并附 NO_CONFIRMED_PAYMENTS。已付款未来覆盖仍计入历史金额/标准时长，未生效不贡献当前成本。

实际有效天数将已知付费区间裁到观察日含当天，与 ENABLED 区间求交并合并；使用 Long epochDay、排序和双指针，不逐日枚举，也不把预付标准时长当已陪伴时间。暂停/停止生效日排除、恢复日计入。聚合使用 Math.addExact；日均 BigDecimal DECIMAL128 元/天，不做展示舍入。引擎不修改输入或跨调用保存状态；M08 Repository 已提供一致快照、稳定记录ID及事务校验，未改冻结算法。

独立验收补测后，evaluate 的付款循环实际调用 internal `accumulateConfirmedPayment(SubscriptionPaidTotals, Renewal)`，保留原金额/标准天数两次Math.addExact，返回新不可变累计值；溢出抛ArithmeticException，不返回部分更新。内部累计种子作为边界测试接口，不是新的用户输入、数据库字段或公共业务功能；没有放宽Money/固定周期校验。当前边界/回归证据见[M03补测](evidence/M03-recheck-verification.md)。

M03 默认停止 helper 只从调用方选定的真实付费期生成 paidUntilExclusive 的 STOPPED 边界；提前停止须明确提供日期，外部服务商自动续费不由本地引擎控制。M03 当时没有存储或订阅界面，M08 已接入两者；M09 当前由 Repository 按用户确认规则选择 max(业务今天, 所有已确认付款的最晚边界) 并保存固定 STOPPED 事实，不更改冻结 helper 或领域算法。M09 状态用户流程已接通，通知资格和里程碑触发仍未实施。实际测试与局限见[当前验收](ACCEPTANCE_MATRIX.md)和[M03 契约](planning/M03_CONTRACT.md)。

## Planned：继承的实施契约

来自[阅读版 A01～A08](../LITHIC_V1_产品与开发规划.md)：

```text
Compose → ViewModel → UseCase / Repository → Room / 领域引擎
                                         → 私有媒体文件
                                         → DataStore
```

一个 Gradle :app 模块，按功能和职责分包；轻量 AppContainer/构造注入。金额、项目、订阅、历史、年度等引擎接受可测试时钟与观察日，页面不复制公式。

完整恢复按阅读版 B05 规划为独立完整 generation、校验完成后原子切换小型指针、保留短暂失败回退。数据库、图片与设置须共同验证，不能用 Room 事务声称覆盖全部文件。

schema 当前 v2(M08)，v1(M04) 导出保留；后续计划 → v3(M16) → v4(M20)。backup format 计划 v1(M11) → v2(M16) → v3(M20)，当前尚无完整备份格式；schema 与内部 generation metadata format1 均不等于备份格式。

后续结构在对应获准模块实施后更新。M00～M08 已独立冻结；M09 按[当前契约](planning/M09_CONTRACT.md)已接通并完成本轮实际验证，READY_FOR_REVIEW、独立PENDING，交付后停止等待独立验收；M10 及后续未授权。Q03～Q06 原状态/期限及 M09 两项流程风险见[问题清单](SPEC_ISSUES.md)。STOPPED 后重新启用和多期预付默认停止边界采用本轮用户明确确认的 A/B 规则，两项流程风险只依据本轮用户确认、实现和真实测试关闭。媒体处理/清理仍归 M10，完整导出与恢复仍归 M11/M12，总成本汇总仍归 M13。真机、API26、OEM、Release正式签名、完整 TalkBack 和真实 LMK 未验证；当前实际验证及独立验收状态以唯一 CURRENT_STATE 为准。

## M04：继承的存储 owner 与私有初始数据集

[AppContainer](../app/src/main/java/com/cooper/lithic/app/AppContainer.kt)由 Application 持有，onCreate 捕获启动设备时区后发起单一 IO 初始化任务；openStorage 等待同一个任务。Initializing/Ready/Failed 明确保存状态，失败原因向读取者传播，不自动重试或返回空账本；取消继续传播，关闭等待 owner Job 并释放本代资源。M05在下节接入正式保护性错误页，未改这些M04接口或生命周期。领域引擎仍独立于Android/Room，未复制Money或成本公式。

```text
AppContainer → DataGenerationStore → ManagedGeneration
  ├── LithicDatabase / Room v2 → 原 4 张业务表 + M08 的 3 张订阅表与 DAO
  ├── SettingsStore → Preferences DataStore
  └── LocalWriteCoordinator → 共用 Mutex

ProjectWriteRepository → 同一个 coordinator + Room.withTransaction
LedgerReadRepository  → 同一个 coordinator + Room.withTransaction 的四表快照
ProjectRepository / SubscriptionRepository → 同一个 coordinator + 各自 Room 事务
```

M04 导出为保留原字节的 [Room v1 JSON](../app/schemas/com.cooper.lithic.data.local.LithicDatabase/1.json)，当前 [Room v2 JSON](../app/schemas/com.cooper.lithic.data.local.LithicDatabase/2.json)继承原四表：categories、long_term_projects、project_money_events、local_media_assets；Room/platform 自用表不计业务表。项目类别/可选封面、金额事件项目引用有 RESTRICT 外键；operationId、类别(type, normalizedName)、媒体相对路径有唯一索引，并配置日期/关联查询索引。两个“其他”使用不同稳定 UUID 和 PROJECT/SUBSCRIPTION 类型。M08 三张订阅表见上节，仍没有替换、通知表或派生成本字段。

LocalDate 转 Long epochDay，Instant 转 ISO-8601 字符串保留纳秒。实体构造校验类别规范化与媒体 UUID/相对 media 路径/尺寸/长度/SHA；写入 Repository 在事务内校验项目/事件 UUID、关系、金额、日期及类别类型。SQL 本身负责外键/索引，业务校验不能被说成 SQLite CHECK 约束。新增项目和初始金额事件 ABORT 插入在同一事务；完全相同 commandId 重试返回原记录，同 ID 异内容明确 Conflict。业务验证失败与 SQLite/磁盘满/腐败存储失败分别返回，取消不被吞掉。零元事件合法，收回可由已有引擎导出负净成本。

所有本代存储位于 noBackupFilesDir：

```text
active-generation                 # AtomicFile；内部元数据版本 1
generations/<UUID>/
  data.db                         # WAL / SHM / journal 与主库同目录
  media/                          # 目录与 metadata，尚无图片处理
  settings.preferences_pb         # 真实 Preferences DataStore 文件
  ready.json                      # formatVersion + generationId
```

SettingsStore 为 canonical 设置文件登记唯一所有者；记录 record_zone_id，仅首次初始化采用设备时区，后续重开读取持久值。缺失/非法时区及损坏设置明确失败，没有默认替换 corruption handler。关闭取消并等待 DataStore scope 完成后释放文件所有权，generation 关闭依次关闭设置和数据库。

首次初始化数据库、两个默认类别、设置、media 目录和 ready 全部完成后，才原子写 activeId/previousId/stage 指针；M04 只写 STABLE、previousId=null。重开先验证持久身份、受控路径、ready、真实数据库和设置；已有目录但指针缺失、损坏或未知阶段都失败，保留原数据。AtomicFile 的 .bak 提供已提交指针恢复边界，不按目录时间猜最新代；只有所指代通过验证才恢复 .bak。任何已有库均先用不会删除原文件的只读 SQLite 打开，按 SQL 读取当前版本与 Room 身份，拒绝 v0/异库/不支持版本，再由 Room 验证身份。只读预检支持已提交 WAL，不能只读主库头推断版本。正式开库同样使用保留原库的 corruption 回调与禁用 data-loss recovery；数据库还须通过 quick_check/foreign_key_check，不清空重建。

共享 Mutex 协调写入/读取/关闭；各自业务一致快照由实际 Room 读取事务保证。它没有让 SQLite、DataStore 和文件系统形成一个跨存储原子事务。M12 仍须实现 previousId 与 PENDING_VALIDATION 等状态机、真实导入、验证后切换/回退、杀进程故障注入、图片/设置共同校验及旧代清理。Q06 保持 ENGINEERING_REQUIRED，范围见[实现前设计](planning/M04_CONTRACT.md)。M04 当时迁移注册为空；M08 当前已注册显式 v1→v2 并增加严格只读已知库预检，没有伪造迁移或 destructive fallback。

## M05：只读页面、状态门控与基础视觉

[LedgerUiStateHolder](../app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt)保留Initializing/Ready/Failed门控。M06已取代M05一次性全量快照；M08 当前先读取持久记录时区及两类真实 COUNT，再观察同一 Room 的变化；后续读取失败保留上次数量并明确错误。holder不拥有数据库关闭权；实际存储仍由AppContainer唯一管理。

[LithicApp](../app/src/main/java/com/cooper/lithic/app/LithicApp.kt)在Ready前不组成正常导航；保护页无空账本/零值/原始路径/堆栈/无效重试，退出回调必须由宿主提供，MainActivity实际finish。加载和错误页均支持安全区及纵向滚动。

[NavKeys](../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt)仅序列化路线和规范UUID；M06 项目当前链最多四级 Home/List/Detail/Editor，M07 增加最多五级历史链；M08 当时订阅链最多四级，M09 增加原身份订阅历史及对应编辑来源链，当前订阅最多四级、历史最多五级。创建保留实际来源父链，成功进入原UUID详情。rememberNavBackStack保留路线，旧Skeleton仍可解码；已解码非法栈归Home，缺失记录有返回入口。未保存和未确认操作由当前双业务统一离开守卫保护。

[LithicTheme](../app/src/main/java/com/cooper/lithic/core/design/LithicTheme.kt)使用系统字体与系统浅深模式、低饱和蓝灰交互色、中性背景，主38/title24/body16/辅助14sp及六级间距。[AmountText](../app/src/main/java/com/cooper/lithic/core/design/AmountText.kt)只做BigDecimal展示：null为无值、真实0为¥0.00，普通金额HALF_UP两位/toPlainString；日均非零绝对值<0.01为<¥0.01或−<¥0.01，含±0.005，不经Double。[EntryCard](../app/src/main/java/com/cooper/lithic/core/design/EntryCard.kt)接收调用方状态/时间和可选封面/真实点击；[FormFields](../app/src/main/java/com/cooper/lithic/core/design/FormFields.kt)只提供受控输入、错误语义、键盘IME和日期选择回调，不写库。

测试专用合成表单仅在androidTest源码，生产菜单不包含测试宿主、测试合成表单或测试合成记录。Compose测试依赖精确1.9.5匹配既有BOM；Lifecycle显式2.10.0是M04已解析版本；Espresso3.7.0只用于androidTest编译调用真按键并同步，不改变生产返回逻辑。Debug测试宿主Activity与运行依赖的实际变化、真实回归/设备证据另见当前验收和工具链；源码存在本身不作为验证PASS。

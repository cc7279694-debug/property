# 当前架构事实

## M06：当前项目读写闭环

[ProjectRepository](../app/src/main/java/com/cooper/lithic/data/repository/ProjectRepository.kt)复用同一Room、LocalWriteCoordinator和BusinessClock；创建/追加调用原ProjectWriteRepository，不外套第二次非重入锁。资料更新在写协调边界和Room事务中比较完整expected与固定target：目标已存在则重放、陈旧基线则Conflict；只更新名称、开始日、类别、备注和updatedAt，保留旧事件及保护字段。schema v1没有持久编辑操作日志。

[ProjectDao](../app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt)按startedOn升序/id平局游标查询，每页最多51行，其中50行可显示；事件和类别按本页ID批读。`loadProjectWindow`在一次协调边界及读取事务中重建已加载窗口，避免页间排序修改混合不同提交。首页只COUNT；正常业务路径不调用全量snapshot。单项目详情取得必要事实后，唯一调用ProjectCostEngine计算，UI不复制公式。

[ProjectViewModel](../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt)观察Room项目/事件/类别变化，刷新已请求的列表和详情；ON_RESUME只重读，不确认冲突或改变提交。详情发布核对项目ID和请求版本。初次资料读取恢复只补缺失字段，保留已有原始输入；已submitted的基线绝不换掉。

SavedStateHandle只存当前任务单活动表单的原始字符串、UUID、完整编辑基线和固定submitted Bundle（primitive/ISO字符串），提交前同步存入。IN_FLIGHT阻止重复和修改；恢复/异常为UNCONFIRMED，只重放原身份内容。已确认成功与读取失败分开，已保存只允许重读；明确CAS冲突由用户点击核对后重载当前资料。该状态不是新的账本或后台持久草稿，也不承诺force-stop后保留未保存草稿。

正式表单、列表、详情只消费派生状态与回调；真实日期保留文本，DatePicker内部使用UTC日期转换，业务今天仍来自持久recordZone。编辑初次读失败有明确重读入口且不允许无基线保存。顶部、主Tab重选/切换、系统和手势返回统一处理dirty/待确认守卫；不增加结束、删除、历史金额编辑或媒体入口。

真实SQLite页数耗尽有两种已观察到的错误：直接SQLiteFull可归DISK_FULL；嵌套事务收尾有时遮蔽为普通DATABASE的“cannot rollback”错误。生产保留明确未知结果及原命令，不按字符串猜分类、吞异常或清库；当前验证必须证明既有行不变、无部分记录、同命令恢复重试仅一次。实际证据与未测边界另记验收，不由此授予冻结。

## Verified：当前实际存在

单 Gradle `:app`、单业务 `MainActivity` 与 `LithicApplication`，Kotlin + Compose + Navigation 3。M06沿原[Activity](../app/src/main/java/com/cooper/lithic/app/MainActivity.kt)和[Nav3 宿主](../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt)接入长期项目创建、分页、详情、资料编辑和真实金额追加。Debug仍另含空ComponentActivity测试宿主；Release仅按配置排除，尚未实际构建。

```text
LithicApplication
  → AppContainer（唯一存储所有者）/ StorageInitializationState
    → MainActivity / LedgerUiStateHolder（仅UI生命周期）
      → COUNT + SettingsStore记录时区 / 同一ProjectRepository
        → LithicTheme / LithicApp：Initializing、Ready、Failed
          → Ready才进入Navigation3 / 三主入口 + 项目录入闭环
```

三个主入口为现在/回看/设置；首页只查真实项目数量，长期项目页面按需读取事实并展示冻结领域引擎结果。汇总、订阅、分析、媒体、备份和提醒仍未开放。支持系统浅深、safeDrawing、IME避让和滚动；图标仍为临时工程图标。

字体使用设备系统字体。AndroidX Emoji2的自动初始化默认会查询可下载字体提供者，因此按[官方说明](https://developer.android.com/develop/ui/views/text-and-emoji/emoji2)及实际1.4.0源码，在Manifest合并中仅移除EmojiCompatInitializer；不手动初始化下载配置。保留其他Startup初始化器，最终APK检查脚本验证Emoji初始化入口已移除。

正式 namespace/applicationId 为 `com.cooper.lithic`；Debug 为 `com.cooper.lithic.debug`，名称含 Debug，仅本地调试签名。没有生产签名配置。[Manifest](../app/src/main/AndroidManifest.xml)与最终 APK 的网络/存储/通知/精确闹钟权限检查见[当前验收登记](ACCEPTANCE_MATRIX.md)。唯一 uses-permission 来自 AndroidX Core 的签名级 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，用于保护内部接收器；不提供网络或用户数据访问权限。ProfileInstallReceiver 上的 DUMP 是调用方须持有的权限，不是 App 申请 DUMP。

系统备份配置包括 `allowBackup=false`、[旧规则](../app/src/main/res/xml/backup_rules.xml)和[新规则](../app/src/main/res/xml/data_extraction_rules.xml)；旧规则以及 cloud-backup/device-transfer 均排除九类域。已检查合并 Manifest 和最终 APK 编译资源；不据此宣称所有 OEM 的迁移行为均已验证。

M04存储所有者、generation和领域引擎保持；M06通过轻量Repository和UI ViewModel调用原子写入及资料CAS。没有图片处理、通知、WorkManager或完整Backup/Restore。schema仍v1，无完整备份格式版本；实际验证以[验收登记](ACCEPTANCE_MATRIX.md)为准。

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

实际有效天数将已知付费区间裁到观察日含当天，与 ENABLED 区间求交并合并；使用 Long epochDay、排序和双指针，不逐日枚举，也不把预付标准时长当已陪伴时间。暂停/停止生效日排除、恢复日计入。聚合使用 Math.addExact；日均 BigDecimal DECIMAL128 元/天，不做展示舍入。引擎不修改输入或跨调用保存状态；未来 Repository 必须提供一致快照、稳定记录ID及事务校验。

独立验收补测后，evaluate 的付款循环实际调用 internal `accumulateConfirmedPayment(SubscriptionPaidTotals, Renewal)`，保留原金额/标准天数两次Math.addExact，返回新不可变累计值；溢出抛ArithmeticException，不返回部分更新。内部累计种子作为边界测试接口，不是新的用户输入、数据库字段或公共业务功能；没有放宽Money/固定周期校验。当前边界/回归证据见[M03补测](evidence/M03-recheck-verification.md)。

默认停止 helper 只从调用方选定的真实付费期生成 paidUntilExclusive 的 STOPPED 边界；提前停止须明确提供日期，外部服务商自动续费不由本地引擎控制。未实现通知资格/里程碑触发、存储或订阅界面。实际测试与局限见[当前验收](ACCEPTANCE_MATRIX.md)和[M03 契约](planning/M03_CONTRACT.md)。

## Planned：继承的实施契约

来自[阅读版 A01～A08](../LITHIC_V1_产品与开发规划.md)：

```text
Compose → ViewModel → UseCase / Repository → Room / 领域引擎
                                         → 私有媒体文件
                                         → DataStore
```

一个 Gradle :app 模块，按功能和职责分包；轻量 AppContainer/构造注入。金额、项目、订阅、历史、年度等引擎接受可测试时钟与观察日，页面不复制公式。

完整恢复按阅读版 B05 规划为独立完整 generation、校验完成后原子切换小型指针、保留短暂失败回退。数据库、图片与设置须共同验证，不能用 Room 事务声称覆盖全部文件。

schema 当前 v1(M04)，计划 → v2(M08) → v3(M16) → v4(M20)；backup format 计划 v1(M11) → v2(M16) → v3(M20)，当前尚无完整备份格式。

后续结构在对应获准模块实施后更新。M00～M05已独立冻结，当前只授权M06；M07及后续未授权。当前验证及独立验收状态以唯一CURRENT_STATE为准。

## M04：真实存储与私有初始数据集

[AppContainer](../app/src/main/java/com/cooper/lithic/app/AppContainer.kt)由 Application 持有，onCreate 捕获启动设备时区后发起单一 IO 初始化任务；openStorage 等待同一个任务。Initializing/Ready/Failed 明确保存状态，失败原因向读取者传播，不自动重试或返回空账本；取消继续传播，关闭等待 owner Job 并释放本代资源。M05在下节接入正式保护性错误页，未改这些M04接口或生命周期。领域引擎仍独立于Android/Room，未复制Money或成本公式。

```text
AppContainer → DataGenerationStore → ManagedGeneration
  ├── LithicDatabase / Room v1 → 4 张业务表与 DAO
  ├── SettingsStore → Preferences DataStore
  └── LocalWriteCoordinator → 共用 Mutex

ProjectWriteRepository → 同一个 coordinator + Room.withTransaction
LedgerReadRepository  → 同一个 coordinator + Room.withTransaction 的四表快照
```

实际 schema 导出为 [Room v1 JSON](../app/schemas/com.cooper.lithic.data.local.LithicDatabase/1.json)。四表为 categories、long_term_projects、project_money_events、local_media_assets；Room/platform 自用表不计业务表。项目类别/可选封面、金额事件项目引用有 RESTRICT 外键；operationId、类别(type, normalizedName)、媒体相对路径有唯一索引，并配置日期/关联查询索引。两个“其他”使用不同稳定 UUID 和 PROJECT/SUBSCRIPTION 类型。没有订阅、替换、通知表或派生成本字段。

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

共享 Mutex 协调写入/读取/关闭；四表一致性由实际 Room 读取事务保证。它没有让 SQLite、DataStore 和文件系统形成一个跨存储原子事务。M12 仍须实现 previousId 与 PENDING_VALIDATION 等状态机、真实导入、验证后切换/回退、杀进程故障注入、图片/设置共同校验及旧代清理。Q06 保持 ENGINEERING_REQUIRED，范围见[实现前设计](planning/M04_CONTRACT.md)。schema v1→v2 的真实迁移留待 M08；当前迁移注册为空，没有伪造迁移或 destructive fallback。

## M05：只读页面、状态门控与基础视觉

[LedgerUiStateHolder](../app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt)保留Initializing/Ready/Failed门控。M06已取代M05一次性全量快照：先读取持久记录时区和COUNT，再观察同一Room变化；后续读取失败保留上次数量并明确错误。holder不拥有数据库关闭权；实际存储仍由AppContainer唯一管理。

[LithicApp](../app/src/main/java/com/cooper/lithic/app/LithicApp.kt)在Ready前不组成正常导航；保护页无空账本/零值/原始路径/堆栈/无效重试，退出回调必须由宿主提供，MainActivity实际finish。加载和错误页均支持安全区及纵向滚动。

[NavKeys](../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt)仅序列化路线和规范UUID；M06最多四级Home/List/Detail/Editor，创建保留实际来源父链，成功进入原UUID详情。rememberNavBackStack保留路线，旧Skeleton仍可解码；已解码非法栈归Home，缺失记录有返回入口。未保存和未确认操作由统一离开守卫保护。

[LithicTheme](../app/src/main/java/com/cooper/lithic/core/design/LithicTheme.kt)使用系统字体与系统浅深模式、低饱和蓝灰交互色、中性背景，主38/title24/body16/辅助14sp及六级间距。[AmountText](../app/src/main/java/com/cooper/lithic/core/design/AmountText.kt)只做BigDecimal展示：null为无值、真实0为¥0.00，普通金额HALF_UP两位/toPlainString；日均非零绝对值<0.01为<¥0.01或−<¥0.01，含±0.005，不经Double。[EntryCard](../app/src/main/java/com/cooper/lithic/core/design/EntryCard.kt)接收调用方状态/时间和可选封面/真实点击；[FormFields](../app/src/main/java/com/cooper/lithic/core/design/FormFields.kt)只提供受控输入、错误语义、键盘IME和日期选择回调，不写库。

测试专用合成表单仅在androidTest源码，生产菜单不包含测试宿主、测试合成表单或测试合成记录。Compose测试依赖精确1.9.5匹配既有BOM；Lifecycle显式2.10.0是M04已解析版本；Espresso3.7.0只用于androidTest编译调用真按键并同步，不改变生产返回逻辑。Debug测试宿主Activity与运行依赖的实际变化、真实回归/设备证据另见当前验收和工具链；源码存在本身不作为验证PASS。

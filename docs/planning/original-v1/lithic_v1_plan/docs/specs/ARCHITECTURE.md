# Android 架构与数据契约

## A01｜技术基线
原生Android、Kotlin、Jetpack Compose、Navigation 3、Room、DataStore、Coroutines/Flow。Android官方建议采用明确数据层、Repository和单向状态流，且可在简单应用中手动注入依赖。[S1]

本项目用一个Gradle `:app`模块，按功能和职责分包；不因有24个交付模块就建24个工程模块。用轻量AppContainer和构造函数注入，不提前引入Hilt/Koin、插件系统或通用框架。

minSdk建议26。M01核查构建机后，把Kotlin/AGP/Gradle/JDK/Compose BOM/Navigation 3/Room/KSP/DataStore/WorkManager/compileSdk/targetSdk的相互兼容稳定版本锁到`gradle/libs.versions.toml`及`docs/TOOLCHAIN_LOCK.md`。不使用`+`、alpha/beta/RC或每模块自动升级。现在没有实际构建验证，不能伪造一组已经兼容的版本号。[S2]

Navigation采用Navigation 3，不与旧版Navigation Compose混用。图片仅本地读取，图表用轻量Compose Canvas/Semantics实现有限折线/阶梯图，不引入WebView/ECharts/D3。无需网络图片加载依赖。

## A02｜目录边界
```
app/src/main/java/com/cooper/lithic/
  app/                   # Application、AppContainer、单Activity、导航
  core/model/            # ID、金额、日期、周期和结果类型
  core/time/             # BusinessClock
  core/design/           # 字号、间距、颜色语义、通用控件
  domain/project/        # 项目成本与结束规则
  domain/subscription/   # 周期、覆盖、状态和成本
  domain/report/         # 总览、历史、年度派生结果
  domain/reminder/       # 可测试的提醒规则，不依赖系统API
  data/local/            # Room、Entity、DAO、迁移
  data/repository/       # 用例事务与唯一写入口
  data/media/            # 私有图片
  data/backup/           # DTO、验证、恢复协调
  data/settings/         # DataStore
  platform/notification/ # WorkManager、Channel、通知发布
  feature/home/
  feature/project/
  feature/subscription/
  feature/review/
  feature/category/
  feature/settings/
```

读取：Compose → ViewModel → Repository/ReportRepository → Room/领域引擎 → UiState。
写入：用户动作 → UseCase/Repository校验与事务 → Room → Flow刷新所有相关页面。
Compose不直接调用DAO；ViewModel不拼SQL、不复写日均公式。AppContainer是装配处，不是全局可变业务状态仓库。

## A03｜领域接口（规划契约，不是实现）
```
BusinessClock.today(): LocalDate
MoneyParser.parseCny(text: String): MoneyParseResult
ProjectCostEngine.evaluate(input: ProjectSnapshot, asOf: LocalDate): ProjectCostResult
SubscriptionCostEngine.evaluate(input: SubscriptionSnapshot, asOf: LocalDate): SubscriptionCostResult
CostTimelineEngine.evaluate(snapshot: LedgerSnapshot, dates: List<LocalDate>): List<CostPoint>
AnnualReportEngine.evaluate(snapshot: LedgerSnapshot, year: Int): AnnualReport
```
ProjectSnapshot = project + moneyEvents；SubscriptionSnapshot = subscription + renewals + stateChanges。
LedgerSnapshot = 同一次一致读取中的全部项目、金额事件、订阅、续费、状态、类别、替换关系。
ProjectCostResult包含heldDays、grossOutlayCents、recoveryCents、netCents、dailyRate、contributesToCurrentTotal。
SubscriptionCostResult包含displayStatus、currentRate、confirmedPaidCents、standardPurchasedDays、historicalAverage、elapsedPaidDays。
CostPoint包含date、projectRate、subscriptionRate、totalRate、relatedEventIds。
无有效分母时用Nullable结果和原因，不用NaN/Infinity/魔法0。

Repository写接口采用`suspend`，输入Command和operationId，输出MutationResult；重复operationId返回已执行结果。错误必须可区分校验错误、关联冲突、空间不足和数据库异常。

## A04｜数据对象
| 对象 | 关键字段 | 真正职责 |
|---|---|---|
| Category | id,type,name,normalizedName,isFallback | 独立类别体系；同类型标准化名称唯一 |
| LongTermProject | id,name,categoryId,startedOn,endedOn,endReason,coverAssetId,note,createdAt,updatedAt | 身份和生命周期，不存每日成本 |
| ProjectMoneyEvent | id,projectId,direction,amountCents,occurredOn,description,role,operationId | 实际投入/收回；role仅内部初始/普通/结束结算 |
| LocalMediaAsset | id,relativePath,mime,width,height,byteLength,sha256 | 一张封面对应的私有资源；不是相册 |
| Subscription | id,name,categoryId,coverAssetId,note,createdAt,updatedAt | 稳定身份，不单独覆盖旧套餐价 |
| SubscriptionRenewal | id,subscriptionId,paidOn,effectiveFrom,paidUntilExclusive,reminderDueOn,amountCents,cycle,standardDays,operationId | 一次真实付费及覆盖区间 |
| SubscriptionStateChange | id,subscriptionId,effectiveOn,state,reason,note,operationId | 暂停/启用/停止的时间线 |
| ProjectReplacementLink | id,previousProjectId,nextProjectId,createdAt | 可选一对一前后关系，无环 |
| NotificationReceipt | logicalKey,ownerId,kind,boundary,publishedAt | 去重，不存WorkManager内部任务作为用户事实 |

所有业务ID用UUID字符串；操作ID单独唯一；外键保证引用存在，跨类别类型和订阅区间不重叠由Repository事务内验证，不把普通FK说成能自动验证业务类型。
当前状态和当前套餐从记录派生，不同时维护一份容易失配的真值。长期项目的endedOn和endReason组合需一致校验。
所有写入经过同一个写协调器，关键多记录操作必须Room事务化。图片与设置跨文件一致性由备份规格定义，不假装被Room事务一起覆盖。

## A05｜数据库演进计划
| 模块 | 目标schema | 变化 | 同步责任 |
|---|---|---|---|
| M04 | v1 | Category、Project、MoneyEvent、LocalMediaAsset | 导出schema；DAO/事务测试 |
| M08 | v2 | 加Subscription、Renewal、StateChange | v1→v2迁移；旧项目不变 |
| M11–M12 | v2不变 | 备份格式v1 | 支持全部已有业务与图片设置 |
| M16 | v3 | 替换关系；金额事件basis与exchangeGroupId | 格式v2；v1备份默认CASH、无替换关系 |
| M20 | v4 | 通知去重记录 | 格式v3；旧备份不补发过去提醒 |
| M21–M23 | 无业务扩展 | 设置/去重规则补齐 | 有格式变化必须带迁移而非省略 |

这是计划中的版本，尚未执行。真正交付以CURRENT_STATE和schema导出为准。
不能使用`fallbackToDestructiveMigration()`抹掉迁移问题。Room官方说明破坏性回退会删除数据，因此迁移测试必须覆盖所有已发行版本到当前版本。[S3]

## A06｜索引与查询
列表分页50条，稳定次排序使用id。金额按(projectId,occurredOn,id)；续费按(subscriptionId,paidOn,id)及有效期；状态按(subscriptionId,effectiveOn,id)；类别(type,normalizedName)唯一；operationId唯一。
名称搜索用已转义LIKE和参数绑定，处理%/_/引号；V1不引入FTS。
趋势按事件和日期派生，不持久化十年的每日快照。需要缓存时仅内存或可丢弃版本键缓存，不成为唯一历史来源。
年级/全量图在长历史下使用显示抽样，保留起止、极值和事件邻点；点击读原始日数据，不能改变计算口径。

## A07｜Local-only实际边界
移除INTERNET权限，检查最终合并Manifest，不只检查自己写的那份。依赖中不得包含遥测、广告、云崩溃上报或远程字体。
应用数据放noBackupFilesDir管理范围；显式禁用Android云备份，并为旧版fullBackupContent与新版dataExtractionRules配置排除。仅设allowBackup=false并不足以覆盖所有厂商设备迁移行为，必须检查实际设备，不能声称绝对阻止系统/OEM备份。[S4]
图片、备份用系统选择器，优先本地模式；用户主动选第三方云文件提供程序属于显式导出/选择，不是App自动同步。界面提示纯本地保存应选择设备文件夹。[S5]
通知权限按功能需要申请；拒绝不会阻断记录。[S6]

## A08｜应用身份、签名和升级
Debug使用`.debug`隔离，只放合成/可丢弃数据。第一个长期录入真实数据的内部版使用稳定生产applicationId与独立长期签名密钥。
密钥由开发环境本地保管，不进Git、不放进用户数据备份、不上传对话；用户另行保管。生产升级要求正确应用身份和签名兼容，具体受Android签名规则约束。[S7]
每个可安装交付记录versionName、递增versionCode、Git SHA、APK SHA-256、schema与备份格式版本。升级测试必须覆盖“旧版有记录→覆盖安装→继续读取/修改/备份”。

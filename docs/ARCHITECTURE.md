# 当前架构事实

## Verified：当前实际存在

已建立 M01 原生 Android 工程：单 Gradle `:app` 模块、单 `MainActivity`、最小 `LithicApplication`，Kotlin + Compose + Navigation 3。实际入口位于 [app 包](../app/src/main/java/com/cooper/lithic/app/MainActivity.kt)，[Nav3 宿主](../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt)只有一个可保存的 Skeleton 导航键。

```text
LithicApplication
  → MainActivity / Compose MaterialTheme
    → Navigation3 NavDisplay / rememberNavBackStack
      → 三句工程占位文案
```

占位页只显示“砺石 LITHIC”“M01 工程验证版”“业务功能尚未接入”；支持系统浅/深色和 safeDrawing inset。最小主题不是 M05 完整设计系统；没有三栏页面、业务按钮、演示项目或虚构金额。图标为临时工程图标，不是已批准品牌 Logo。

字体使用设备系统字体。AndroidX Emoji2的自动初始化默认会查询可下载字体提供者，因此按[官方说明](https://developer.android.com/develop/ui/views/text-and-emoji/emoji2)及实际1.4.0源码，在Manifest合并中仅移除EmojiCompatInitializer；不手动初始化下载配置。保留其他Startup初始化器，最终APK检查脚本验证Emoji初始化入口已移除。

正式 namespace/applicationId 为 `com.cooper.lithic`；Debug 为 `com.cooper.lithic.debug`，名称含 Debug，仅本地调试签名。没有生产签名配置。[Manifest](../app/src/main/AndroidManifest.xml)与最终 APK 的网络/存储/通知/精确闹钟权限检查见[当前验收登记](ACCEPTANCE_MATRIX.md)。唯一 uses-permission 来自 AndroidX Core 的签名级 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，用于保护内部接收器；不提供网络或用户数据访问权限。ProfileInstallReceiver 上的 DUMP 是调用方须持有的权限，不是 App 申请 DUMP。

系统备份配置包括 `allowBackup=false`、[旧规则](../app/src/main/res/xml/backup_rules.xml)和[新规则](../app/src/main/res/xml/data_extraction_rules.xml)；旧规则以及 cloud-backup/device-transfer 均排除九类域。已检查合并 Manifest 和最终 APK 编译资源；不据此宣称所有 OEM 的迁移行为均已验证。

没有 Room/业务 SQLite、schema、DAO、Repository、UseCase、ViewModel、DataStore、媒体存储、通知、WorkManager 或手动 Backup/Restore。没有建立实际 schemaVersion/backupFormatVersion。构建和设备证据以[工具链锁](TOOLCHAIN_LOCK.md)及验收登记为准；这不代表未来业务可离线、可备份或可升级。

## M02：纯 Kotlin 领域计算

已实现[金额模型与解析](../app/src/main/java/com/cooper/lithic/core/model/Money.kt)、[领域模型](../app/src/main/java/com/cooper/lithic/core/model/DomainModels.kt)、[业务日期注入](../app/src/main/java/com/cooper/lithic/core/time/BusinessClock.kt)和[长期项目成本引擎](../app/src/main/java/com/cooper/lithic/domain/project/ProjectCostEngine.kt)。它们只有 Kotlin/JDK 依赖，没有 Android Context、UI、Room Entity 或持久化调用，尚未接入空壳页面。

```text
ProjectSnapshot（开始/可选结束日 + 全部真实金额事件）
  + asOf（明确观察日）
  → ProjectCostEngine.evaluate
  → ProjectCostResult（Long 天数/分、BigDecimal 元每天、日末贡献、不可用原因）
```

单笔 Money 为 0～999,999,999,999 分的非负绝对值；投入/收回由独立方向表达。解析拒绝负号、科学计数、超过两位小数和越界值，不做浮点转换或偷偷舍入。总投入、总收回分别用 `Math.addExact` 检查 Long 溢出，净额允许负数；非法构造抛 IllegalArgumentException，聚合溢出抛 ArithmeticException，解析返回类型化 Failure。

引擎只纳入 `occurredOn <= asOf` 的事件；开始前保留已发生的预付款总额，但天数为0、日均为null、原因PROJECT_NOT_STARTED且无贡献。开始日至观察日含首日；结束日起分母固定为含首末日的完整天数，结束日日末不再贡献。后结算只改变净额。日均以元/天及 DECIMAL128（34位有效数字）计算，不做展示两位舍入，0.005/-0.005保留原符号；展示层尚未实现。

BusinessClock 可注入固定 LocalDate 或携带明确时区的 JDK Clock；引擎自身只用 asOf，不读取设备今天或猜测业务时区。记录时区的持久化留待对应模块。输入事件代表每笔真实事实，初始投入也只是一笔事件；重复写入幂等与稳定ID留待M04/M06，不能在这里按相同金额/日期误删两笔独立付款。派生结果不缓存每日快照。

本轮测试使用 `testImplementation` 的 JUnit4.13.2；实际运行结果与构建证据见[当前登记](ACCEPTANCE_MATRIX.md)及[M02证据](evidence/M02-verification.md)，不以源码推断通过。M01 Activity、导航、资源、Manifest和系统备份排除配置未改，未创建业务数据层或订阅引擎。

## Planned：继承的实施契约

来自[阅读版 A01～A08](../LITHIC_V1_产品与开发规划.md)：

```text
Compose → ViewModel → UseCase / Repository → Room / 领域引擎
                                         → 私有媒体文件
                                         → DataStore
```

一个 Gradle :app 模块，按功能和职责分包；轻量 AppContainer/构造注入。金额、项目、订阅、历史、年度等引擎接受可测试时钟与观察日，页面不复制公式。

完整恢复按阅读版 B05 规划为独立完整 generation、校验完成后原子切换小型指针、保留短暂失败回退。数据库、图片与设置须共同验证，不能用 Room 事务声称覆盖全部文件。

schema 计划 v1(M04) → v2(M08) → v3(M16) → v4(M20)；backup format 计划 v1(M11) → v2(M16) → v3(M20)。这仅是规划，不是当前版本。

后续领域与数据结构在对应获准模块实施和验证后更新。M01 空壳已冻结，本轮 M02 仅实现上述纯领域计算；M03及后续模块未授权。

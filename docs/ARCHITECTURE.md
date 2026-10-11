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

后续领域与数据结构在对应获准模块实施和验证后更新。本轮 M01 只实现上述启动空壳；后续模块未授权。

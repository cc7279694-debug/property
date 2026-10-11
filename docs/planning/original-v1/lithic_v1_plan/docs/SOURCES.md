# 外部技术依据

核查日期：2026-10-10。只使用Android官方资料；下列资料支持平台行为，不替用户决定产品语义。技术版本需要M01在真实构建环境中验证后锁定，不能把网页版本表等同本项目构建通过。

| 编号 | 官方资料 | 本计划采用的事实 |
|---|---|---|
| S1 | Android architecture recommendations：`https://developer.android.com/topic/architecture/recommendations` | UI/数据分层、Repository、单向流、Compose、Navigation 3、简单应用可手动依赖注入 |
| S2 | Navigation 3 release notes：`https://developer.android.com/jetpack/androidx/releases/navigation3`；稳定渠道：`https://developer.android.com/jetpack/androidx/versions/stable-channel` | 工具链在开工时依据正式发布记录校验，不猜版本兼容性 |
| S3 | Room migrations：`https://developer.android.com/training/data-storage/room/migrating-db-versions` | schema导出、迁移测试、破坏性回退的数据风险 |
| S4 | Auto Backup：`https://developer.android.com/identity/data/autobackup` | Android自动备份、目录排除、allowBackup与厂商设备迁移边界 |
| S5 | Storage Access Framework：`https://developer.android.com/training/data-storage/shared/documents-files` | 用户选择文件和提供程序；不把导出等同私有本地存储 |
| S6 | Notification permission：`https://developer.android.com/develop/ui/compose/notifications/notification-permission` | 运行时通知授权与拒绝处理 |
| S7 | Sign your app：`https://developer.android.com/studio/publish/app-signing`；Prepare release：`https://developer.android.com/studio/publish/preparing` | 稳定应用标识、签名与覆盖升级 |
| S8 | Zip Path Traversal：`https://developer.android.com/privacy-and-security/risks/zip-path-traversal` | ZIP解压路径校验 |
| S9 | AtomicFile：`https://developer.android.com/reference/androidx/core/util/AtomicFile` | 单文件原子写与另行提供并发保护 |
| S10 | Work requests：`https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work` | 持久任务、执行时机受系统优化和约束影响 |
| S11 | Photo picker：`https://developer.android.com/training/data-storage/shared/photo-picker` | 按需选择图片，避免广泛访问照片库 |

阅读规划不需要打开所有资料。实现相关模块时，只需核对对应条目和最新官方变更。

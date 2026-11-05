# 砺石 V1-R2 正式发行准备

2026-11-05。仓库 `cc7279694-debug/property`。开始时 `codex/v1-release-alignment` 的本地 HEAD 与远端同为 `9726b4e762e18eb5f589f2e0e6cda316716c5ce4`，工作区干净，App 树为 `35293bb9d73726f60106849f396b07c29e203e21`。从该提交建立 `codex/v1-r2-release-preparation`。核心分支远端为 `e01b9368eabd967733d08ed307f3797ed61bc453`，最终审计从 `61e28101:docs/evidence/LITHIC-V1-Final-Release-Readiness.md` 只读复核。R2 仅交付本报告。

**V1_RELEASE_NO_GO**。正式身份为 `com.cooper.lithic`，验收目标是独立全新安装、空账本和正式包自身的备份恢复。首发版本名建议 `1.0.0`。正式包实际发行历史最高 VersionCode **UNKNOWN**，本轮不冻结数字。用户最新决定覆盖旧报告中的迁移计划。 **`PREVIEW_TO_RELEASE_DATA_MIGRATION = NOT_APPLICABLE_BY_USER_DECISION`**。Preview19 维持原安装，正式安装验收无需制作 Preview 迁移备份。 [此前源码与静态证据](LITHIC-V1-Release-Alignment.md)

## 包身份与首发版本

| 项目 | 正式默认 Release | 现存 Preview19 |
| --- | --- | --- |
| 应用 ID | `com.cooper.lithic` | `com.cooper.lithic.preview.debug` |
| 当前生成配置 | code 17 / `0.1.0-m22`，仅当前配置值 | code 35 / `0.1.0-preview.19-debug` |
| 首发版本 | 建议 `1.0.0`，code 待确认该包历史后再定 | 已交付版本 |
| 签名 | 仓库无显式 Release 签名接线，正式证书和签署 NOT_RUN | 原 Debug 证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，继承实物审计 |
| 产物与安装 | R2 无 APK，实物身份与并存安装 NOT_RUN | 本轮归档复算 20,506,559 字节，SHA-256 `34ef14ac074fe5afafdf9a554518671e576ea8c0d4144537e39195f0947cf2f6` |

五份既有生成清单经本轮解析，正式包与体验包的 package、Startup authority、专用接收器权限均不同，无 `sharedUserId`，静态满足独立包身份。正式 Launcher 为 MainActivity，Application 为 LithicApplication。Preview18→19 小米 14 同包覆盖、业务记录保留及使用正常按用户反馈登记为 `USER_REPORTED_MANUAL_PASS_WITH_NOTES`。正式包安装另待验证。 [Gradle 配置](../../app/build.gradle.kts) · [Preview19 原实物审计](v1-core-noop-20261105/artifact-2/identity.json)

正式包版本号需要区分源码可配置值和已发行实物。审查 `git log --all -- app/build.gradle.kts` 可达的 48 份不同配置快照，普通默认正式配置的最高已见值为 **17**，M22 提交 `5a446ee2` 引入。但 `e01b9368:app/build.gradle.kts` 的旧 `lithicPrivateTrial=true` 会在 defaultConfig 设 code35，当时 Release 没有被该开关禁用，且包名后缀只在 Debug 生效。因此旧配置曾允许正式 ID 搭配 code35，**证据仅到配置**。Alignment 已关闭 Private Trial/M22 的 Release 变体。Preview19 的实物 code35 属于体验包自己的序列。

Git 检查覆盖 237 个本地可达提交，含远端 81 个唯一分支 tip。远端 tags 和 GitHub Releases 查询均为空。使用 AAPT2 36.0.0 `dump badging` 只读核对历史保护清单、交付/验收目录和已知构建输出的 39 个去重 APK，全部可读，0 个包名精确等于 `com.cooper.lithic`，未找到 AAB。外部分发历史和既有正式安装状态为 **UNKNOWN**。code36 仅是超过已知旧配置上界的技术候选，须先确认该包已发行实物最高值不超过 35。若核实更高发行记录，取已知最高值加一。code18 缺少依据，当前保持版本配置原值。 [历史保护清单](v1-core-noop-20261105/protected-apks-after-archive.json) · [M13-T06 签名就绪审查](M13-T06-readiness.md) · [Alignment 变体记录](LITHIC-V1-Release-Alignment.md)

## 签名准备

当前跟踪的 Gradle 仅见显式试验或验证模式对既有 Debug keystore 文件的存在性要求。没有发现 Release `signingConfig`、正式外部属性或环境注入接线、CI 签名定义，或默认 Release 指向 Debug 签名的路径。本轮环境没有经验证的正式签名注入。密钥文件和 secret 未读取，用户安全存储及外部 CI 状态 **UNKNOWN**。未显式配置 Release R8/minify 和资源压缩。实际 APK 签名、优化、可重复构建验证均为 **NOT_RUN**。 [当前构建定义](../../app/build.gradle.kts) · [内部交付与正式签名门禁](../INTERNAL_RELEASE.md)

后续签名前由用户确认发行渠道、正式 ID、首发 code、是否已有正式签名证书及密钥保管责任。签名流水线只在获授权后配置受限的外部密钥和凭据，缺失时停止，保存公开证书指纹。由用户确认加密离线备份可恢复、访问名单和轮换条件。正式候选需记录源码、工具链、依赖版本、构建参数与产物 SHA，再核对证书、合并清单、版本及正式包同签名更新行为。已交付的 Debug 证书仅作为体验包身份凭证。

## 照片能力复核

| 已重新读取的 Alignment 生成输出 | PRODUCTION / EXPERIMENT | 包名后缀 |
| --- | --- | --- |
| 默认 Release | true / false | 无 |
| 普通 Debug | false / false | `.debug` |
| Private Trial Debug | false / true | `.preview.debug` |
| PHOTO-LITE Debug | false / true | `.photolite.debug` |
| B1 QA Debug | false / true | `.b1qa.debug` |

三种试验或体验身份均不允许 Release。正式合并 Manifest 的 SHA-256 为 `f434d8318b552447c489306a7d5921772c10080140bd5dda1e98671bc1a6ef08`，与 Alignment 记录一致。五份生成清单均设 `allowBackup=false`，保留各自包内组件和非导出媒体服务，未见互联网或广泛媒体读取权限。原通知权限声明仍在，旧 Worker 在访问 Owner 之前早退，自动提醒保持延期至 V1.1。 [原变体生成记录及清单指纹](LITHIC-V1-Release-Alignment.md) · [旧 Worker 静态证据](v1-core-noop-20261105/artifact-2/core-worker-code.stdout.log)

构建开关和 `PhotoLiteLimits.supportsApi()` 共同控制 writer 创建。对 API 26–35 和 37+，正式照片写入关闭，详情隐藏操作并显示说明，选择和缩略图解码均不可达。已有封面走独立读取路径。对 API 36，生产 writer 仍绑定同一 Owner，导航仅在 Picker 检查通过时给出操作。添加、更换和取消封面继续经过路由、Owner/generation、文件身份、PNG 尺寸与字节预算、备份恢复互斥检查。取消封面只取消引用，历史照片仍保留。严格 JPEG 导入和永久清理保持关闭。 [能力开关](../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteCapability.kt) · [Writer 接线](../../app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt) · [导航](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt) · [详情](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt) · [缩略图](../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteThumbnailPipeline.kt)

**设备验收缺口。** 当 Picker 探测返回 false，源码会隐藏操作并显示说明。实际依赖 Activity 1.12.4 的 `isPhotoPickerAvailable(context)` 在 SDK ≥33 依据 API 判定为 true，无法据此证明 API 36 设备的处理组件一定能启动。特殊设备上处理器缺失或禁用时，现有 Picker 启动异常路径会反馈失败。要求中的「不可用时预先隐藏操作并显示中性说明」在该场景仍未证实，标记 **NOT_RUN/UNKNOWN**。独立审查时先用受控 API 36 环境验证真实启动和失效行为。若复现探测为真但无法启动，最小修复应评审可靠的处理器准入及失效后的状态处理，继续遵守已批准的 API 范围。 [Picker 面板](../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteCoverPanel.kt) · [依赖锁定版本](../../gradle/libs.versions.toml)

## 全新安装与空账本

MainActivity 启动主进程 Owner，AppContainer 在当前 Application 的 `noBackupFilesDir` 内打开数据代。只有活动指针及其备份/暂存均不存在，且无既有数据代条目时，才创建新代、初始化 Room 5 和设置，并发布活动指针。新库只建立物品与订阅各自的默认分类，业务记录为空。首页依据查询得到的项目与订阅计数呈现空态。若已有数据代且指针异常，则保留现场并拒绝把它当作新账本。跟踪源码未发现共享 UID、直接读取 Preview 私有目录、共享数据库或自动跨包复制路径。Manifest 关闭自动备份且相应规则排除各列出域。 **首次安装、重开和并存仍为 NOT_RUN**。 [MainActivity](../../app/src/main/java/com/cooper/lithic/app/MainActivity.kt) · [AppContainer](../../app/src/main/java/com/cooper/lithic/app/AppContainer.kt) · [数据代初始化](../../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt) · [Room 默认记录](../../app/src/main/java/com/cooper/lithic/data/local/LithicDatabase.kt) · [主 Manifest](../../app/src/main/AndroidManifest.xml)

在正式 APK 和签名另获确认后，保留原 Preview19 安装和数据。在已授权设备独立安装正式包，核对 `com.cooper.lithic`，首次打开应见空首页和零物品、零订阅。新建一条测试物品和一条测试订阅，退出重开验证读回。随后单独打开 Preview19，确认仍可使用且原状态保持。正式包已有业务记录、包身份不符或首次显示体验版记录时立即停止并保留现场。测试数据清理须由用户另行确认。

## 正式包自身 backup4 的独立门禁

备份 format / minReader / schema 为 **4 / 4 / 5**。同一 Owner 协调器内快照十表，覆盖分类、物品、投入/收回事件、登记媒体、订阅、续费、订阅状态变化、替换关系、通知回执与里程碑水位。设置包括记录时区、续费提醒偏好、里程碑偏好和导入截止日。主题、URI 授权、WorkManager 队列在备份之外。归档复制全部已登记媒体的存储字节并核验路径、UID、文件身份、链接数、长度、SHA、格式和尺寸，已取消封面但仍登记的历史照片也参与。 [备份清单](../../app/src/main/java/com/cooper/lithic/data/backup/BackupManifest.kt) · [快照](../../app/src/main/java/com/cooper/lithic/data/backup/RoomBackupSnapshotSource.kt) · [媒体核验](../../app/src/main/java/com/cooper/lithic/data/backup/BackupMediaSnapshot.kt) · [设置字段](../../app/src/main/java/com/cooper/lithic/data/backup/BackupDtos.kt)

备份检查仅验证暂存副本。恢复阶段对受限私有副本再验证，展示源与目标计数，消费一次性预览 token，复核 Owner 与目标快照后准备并验证新代，再原子切换。失败时若原代仍可确认读取才反馈保全，无法确认时反馈 UNKNOWN，源备份由用户保管。恢复保护可能推进提醒截止日和里程碑水位，核对时按该合同处理。 [检查](../../app/src/main/java/com/cooper/lithic/data/backup/BackupInspectionService.kt) · [恢复协调](../../app/src/main/java/com/cooper/lithic/data/backup/RestoreCoordinator.kt) · [活动代切换](../../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt)

最小设备验收使用正式包的可丢弃合成账本。创建物品、订阅和金额事实，API 36 条件下添加及更换封面，再取消显示以覆盖历史登记媒体。导出正式包自身 backup4，检查归档、查看预览并在明确确认后恢复到一次性合成目标。重开核对物品、订阅、金额、类别、照片引用和媒体原字节，按上述设置及恢复保护口径比对。至少覆盖一次校验失败和获授权的中断场景，失败或 UNKNOWN 时停止，验证不误报成功且不删除源备份。该独立门禁仍为 **NOT_RUN**。

## 验证结果与下一次验收

- **R2 实际 PASS（只读静态）**。本地与远端基线比对，Preview19 本地归档重算尺寸与 SHA，重新解析五份同基线 BuildConfig 和合并 Manifest，核对源码、应用 ID、启动组件、权限及 Git 差异。正式包实物验签、并存安装和启动行为未执行。
- **R2 Gradle NOT_RUN（资源）**。执行前测得 4,053,016 KiB（约 3.87 GiB），低于 4 GiB 门槛，预检退出 3，Gradle 未启动。JVM、Kotlin 编译与 Lint 本轮均无新执行结果。
- **继承 PASS**。Alignment 同一 App 树的 71 项定向 JVM、Debug/Release/AndroidTest Kotlin 编译、Release Lint 0 错误和 20 条警告。沿用其中照片能力、备份检查及恢复反馈的证据。本轮未增加或执行 Preview 迁移测试。 [原验证记录](LITHIC-V1-Release-Alignment.md)
- **仍待独立验收**。正式发行记录最高 code 和外部签名状态 UNKNOWN，正式签发实物 NOT_FOUND，签名与升级 NOT_RUN。真实空账本、并存、API 36 Picker 失效、照片资源峰值、正式包自身 SAF/异常恢复，以及 M22 离线、长历史和真实 IME/大字仍待验证。M20 历史可见性两次 FAIL、任务/Owner 收口 UNKNOWN 与 M22 历史 FAIL 保留。 [M20 平台](M20-Android-verification.md) · [M20 收口](M20-Platform-closure.md) · [M22](M22-verification.md)

最少下一阶段验收为正式身份和签名方案独立审查，API 36 的独立安装/空账本及照片操作和 Picker 失效核验，同一正式包的合成 backup4 往返与失败路径，以及适用的 A 类门禁。R2 未构建或签名 APK、未操作设备、未接触真实用户数据。保持 `V1_RELEASE_NO_GO`、`STRICT_JPEG_IMPORT_BLOCKED`、`ANDROID_ISOLATION_RUNTIME_PENDING` 及自动提醒延期至 V1.1。

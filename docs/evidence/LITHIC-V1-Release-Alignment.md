# 砺石 V1.0 正式版功能与身份对齐

2026-11-05。仓库 `cc7279694-debug/property`。开始时工作区干净，当前审计分支及远端均为 `61e28101644762887f63fe782836d759a0471ba8`，核心分支及远端均为 `e01b9368eabd967733d08ed307f3797ed61bc453`。本次从后者建立 `codex/v1-release-alignment`。上一轮审计可从 `61e28101:docs/evidence/LITHIC-V1-Final-Release-Readiness.md` 读取。

正式照片接线已在源码层对齐，建议保留 API 36 与系统照片选择器的现有准入范围。**V1_RELEASE_NO_GO**。正式签名与版本身份、正式变体在设备上的照片行为、跨包 SAF 迁移及发行审计 A 类门禁仍待验证。

## 正式照片开关与变体核对

基线 Release 继承默认 `PHOTO_LITE_EXPERIMENT=false`，而 LedgerUiStateHolder 原先只看这一标志，因此 Release 没有封面写入器。现在正式 Release 使用独立 `PHOTO_LITE_PRODUCTION`，普通 Debug 两个标志均关闭。Private Trial、PHOTO-LITE 和 B1 QA 的 Debug 继续使用实验标志。Private Trial 和 M22 也只允许 Debug，避免同次构建属性生成借用体验版本号的正式身份。 [Gradle](../../app/build.gradle.kts) · [LedgerUiStateHolder](../../app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt)

[PhotoLiteCapability](../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteCapability.kt) 复用 `PhotoLiteLimits` 的 API 36 判断。仅支持的构建与 API 创建同代 writer，进入选择及编码前仍需通过 AndroidX Picker 检查。缺少准入时，详情页只显示 Android 16 与系统选择器说明，不提供添加、更换或取消封面按钮。正常操作仍经过照片控制器的 Owner、路由、备份与恢复保护。已登记封面读取独立于写入能力，备份包含旧照片的合同保持。

实际执行 `generate<Variant>BuildConfig`、`process<Variant>MainManifest`，按独立输出核对五份结果。标志顺序为 **PRODUCTION / EXPERIMENT**。

| 配置 | 实际生成的包名、code / name | 标志 | Manifest 差异与 SHA-256 |
| --- | --- | --- | --- |
| 普通 Debug | `com.cooper.lithic.debug`，17 / `0.1.0-m22-debug` | false / false | 默认标签，依赖合入 Debug 测试宿主 ComponentActivity。 `712c4402aa3b4cab1896df72bcd34d21152d57afd7243b44ddd76f5dd024c282` |
| Private Trial Debug | `com.cooper.lithic.preview.debug`，35 / `0.1.0-preview.19-debug` | false / true | 体验版标签，无测试宿主 Activity。 `2085cc14134e1d55a1fbeb30afae52371ffda1cf6900248209244492283ad338` |
| PHOTO-LITE Debug | `com.cooper.lithic.photolite.debug`，1 / `0.1.0-photo-lite.p1-debug` | false / true | 试验标签，非导出的 ComponentActivity。 `63a46e6f574418b15ac7fb382f767dafc30f545ad775fc0a37be1855c8f69ef6` |
| B1 QA Debug | `com.cooper.lithic.b1qa.debug`，1 / `0.1.0-b1qa.1-debug` | false / true | B1-QA 标签，无测试宿主 Activity。 `8e407b19fc91cb440d42b94fd89dc946821b7d396fccf5d21d61830107545d92` |
| 默认 Release | `com.cooper.lithic`，17 / `0.1.0-m22` | true / false | 默认标签，唯一 Activity 为 MainActivity。 `f434d8318b552447c489306a7d5921772c10080140bd5dda1e98671bc1a6ef08` |

合并结果均使用 LithicApplication、MainActivity、主图标资源和 `allowBackup=false`，含非导出的媒体隔离服务。各变体的接收器权限及 Startup provider authority 随包名区分。未见 `INTERNET` 或广泛媒体读取权限。既有 `POST_NOTIFICATIONS`、`ACCESS_NETWORK_STATE`、`RECEIVE_BOOT_COMPLETED`、`WAKE_LOCK` 声明仍在。通知功能本身固定关闭，旧 Worker 在获取 Owner 之前返回，不发出或取消旧任务。权限声明的正式取舍仍属于候选审查。

## Android 照片支持边界

工程 `minSdk=26`、`compileSdk=36`、`targetSdk=36`，AndroidX Activity 由 [版本目录](../../gradle/libs.versions.toml) 固定在 1.12.4。本地该版本运行库的字节码显示，框架 Picker 路径在 API 33+ 或 API 30–32 的 R 扩展 ≥2 可用，其后依次检查 OEM 系统 fallback 与通用 `ACTION_OPEN_DOCUMENT`。应用使用的 `isPhotoPickerAvailable(context)` 只认可框架或系统 fallback。API 36 走框架 `ACTION_PICK_IMAGES`，平台探测不能保证每个设备都能启动选择器，现有启动失败处理仍适用。用户可授权单张图片的临时读取，应用不持久化 grant，系统可展示合格的云媒体来源。 [AndroidX Picker 合同](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.PickVisualMedia) · [Photo Picker 授权与来源](https://developer.android.com/training/data-storage/shared/photo-picker)

| API | V1 源码准入与数据路径 | 运行证据 |
| --- | --- | --- |
| 26–28 | 照片写入关闭，不发起选择或获取 URI。OEM Picker/文档选择即使存在，`loadThumbnail` 仍从 API 29 才提供。已登记封面、备份和恢复走各自的受校验链。 | 本轮设备 NOT_RUN。 |
| 29–35 | 照片写入关闭，不发起选择或解码。API 29 起可调用系统缩略图，API 29 的 AOSP 采样取两轴较小值，极端长宽比时可能先解码超出期望的 raster。API 30–35 即使支持所需 Picker 或缩略图，提供者、资源与取消闭环的证据仍不足以扩大准入。 | 正式支持 NOT_RUN。 [API 合同](https://developer.android.com/reference/android/content/ContentResolver#loadThumbnail(android.net.Uri,%20android.util.Size,%20android.os.CancellationSignal)) · [Android 10 采样](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-10.0.0_r47/core/java/android/content/ContentResolver.java#L3373-L3379) |
| 36 | 构建标志启用且通过系统 Picker 检查时，使用单图 content URI 和下述既有缩略图、取消与 PNG 写入合同。 | P1 照片使用、Preview19 小米 14 的一般体验和覆盖升级均为各自的用户反馈。P3 的 56 项 JVM PASS 和 61 项 Android 方法编译属于既有证据。正式变体 Picker、提供者异常、资源峰值、恢复与逐图一致性 NOT_RUN/UNKNOWN。 [P2 用户反馈记录](LITHIC-PHOTO-LITE-P2-Integration.md) · [P3 验证](LITHIC-PHOTO-LITE-P3-Integration.md) · [Android 16 采样](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-16.0.0_r1/core/java/android/content/ContentResolver.java#L3922-L3928) |
| 37+ | 保守维持当前照片准入上限，不发起选择或解码。新系统通常可运行旧目标版本应用，仍需逐项验证后才扩大照片范围。 | 正式兼容 NOT_RUN，设备与提供者行为 UNKNOWN。 [Android 17 兼容说明](https://developer.android.com/about/versions/17/migration) |

缩略图流水线只接受有 authority 的 content URI 与具体 image MIME。在提供者端请求 512×512 的缩略图，主进程通过系统解码，返回后拒绝硬件或超出 1600×1600、12 MiB 的位图。再绘制为最长边 640 的 sRGB ARGB_8888（像素分配最多 640×640×4 字节），认证 PNG 严格小于 2 MiB，复制的是像素。单一照片工作槽与封面预览共享 16 MiB 像素记账预算。上述是请求值和应用侧预算，提供者原始输入、返回前的解码分配、系统总 RSS 或不返回的原生调用没有已证实的硬上限。 [缩略图流水线](../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteThumbnailPipeline.kt) · [像素预算](../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviewPolicy.kt)

取消向提供者传 CancellationSignal，工作槽及像素租约等底层实际返回再释放。过期票据、路由变化或 Owner 换代不能提交。资源状态未知保持阻断。确认后由 [同代封面写入器](../../app/src/main/java/com/cooper/lithic/data/media/photolite/GenerationPhotoLiteCoverWriter.kt) 校验归属、格式、长度、尺寸与 SHA，再进行文件及事务写入。移除只取消封面引用，登记媒体仍在。API 26–35 与 37+ 不进入此选择、解码、迟到回调和写入链。各 API 上的登记媒体读取及 backup4 校验不依赖此 gate。现有 C6 严格隔离通道、原图 JPEG 和永久删除仍按各自门禁处理。API 36 范围是待独立确认的发行候选边界。

## 正式身份、版本与签名

建议保留 `com.cooper.lithic`，首次正式版本使用 `1.0.0`。确定该正式包所有既有安装和发行过的 code 后，为首版选取高于该包已发最大值的 code，每次正式更新递增。Preview19 的 code35 属于另一包，不参与正式同包升级的比较。本轮生成配置仍为表中 17 / `0.1.0-m22`。同包升级验证还需证书连续，跨包并行安装与 SAF 恢复需要单独验证。 [Android 版本规则](https://developer.android.com/studio/publish/versioning) · [Android 签名指南](https://developer.android.com/studio/publish/app-signing)

检查跟踪的 Gradle、源码集及仓库自动化定义，未发现显式 Release signingConfig、安全外部注入或正式发布流水线。未找到明确的 Release R8/minify 或资源压缩配置，产物级优化与重现性 NOT_RUN。本地安全存储及外部 CI 的密钥状态 UNKNOWN。Debug 走既有本地 Debug 签名。正式构建建议经授权后只在需要签名的流水线引入专用受限密钥文件与 CI secret 或本地安全属性，失败即停止，构建日志不含值。由用户确认发行渠道、证书指纹与保管责任，持有经加密且可恢复的离线备份，记录访问授权和轮换条件。候选验收记录工具链、输入提交、解析依赖版本、正式版本、证书公开指纹及 APK SHA。本轮未读取、生成或输出签名秘密。

## Backup4 跨包迁移设计

[BackupManifest](../../app/src/main/java/com/cooper/lithic/data/backup/BackupManifest.kt) 与 [BackupValidator](../../app/src/main/java/com/cooper/lithic/data/backup/BackupValidator.kt) 接受当前 format/minReader/schema **4/4/5**，检查 ZIP 结构、路径、长度、SHA、记录数、关联及业务约束。导出在 [BackupExportService](../../app/src/main/java/com/cooper/lithic/data/backup/BackupExportService.kt) 从当前包读取 `versionName`，manifest 只保存 `producerAppVersion`，准入只校验其字符串形态。包名、versionCode 和签名证书均不是备份匹配字段。合成归档可验证版本标签无关准入，真实跨包 SAF 和代切换仍须设备验收。

[RoomBackupSnapshotSource](../../app/src/main/java/com/cooper/lithic/data/backup/RoomBackupSnapshotSource.kt) 在协调器内快照十表，包括分类、物品、投入/收回事件、订阅、续费、订阅状态变化、替换关联、登记媒体、通知回执及里程碑水位。[BackupDtos](../../app/src/main/java/com/cooper/lithic/data/backup/BackupDtos.kt) 仅备份记录时区、续费与里程碑偏好及导入截止日。主题存放在独立 `noBackupFilesDir`，需在正式包重选，设备 URI 授权、WorkManager 队列和外部原始照片不在备份中。[BackupMediaSnapshot](../../app/src/main/java/com/cooper/lithic/data/backup/BackupMediaSnapshot.kt) 复制所有已登记媒体的存储字节，包括历史无引用照片。检查受控相对路径、普通文件及链接数、UID、大小、SHA、格式和尺寸。PHOTO-LITE 保存和备份的是已规范化 PNG。旧登记媒体是否缺失或损坏须由具体归档验证。

[B1 检查](../../app/src/main/java/com/cooper/lithic/data/backup/BackupInspectionService.kt) 只读暂存副本并校验，不切换账本。VERIFIED 表示归档通过当前结构及媒体检查，ZIP 未加密，SHA 校验不验证制作方身份。[RestoreCoordinator](../../app/src/main/java/com/cooper/lithic/data/backup/RestoreCoordinator.kt) 再校验 SAF 输入的私有限额副本，展示源与目标记录数，消费一次性预览 token，在原 Owner 资格下准备和验证候选代。由 [DataGenerationStore](../../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt) 原子切换。失败后能确认原代仍可读取才报告未被替换，无法确认时提示 UNKNOWN。恢复保护可能推进通知截止日和里程碑水位，后续比对须区分这些有意变化。原备份文件不属于恢复清理目标。

1. 先以隔离包及合成账本完成全链试迁移，再单独审批正式数据迁移。用户从 Preview19 导出新 B0 到 App 之外的受控位置，用只读检查验证，另存一份已验证的副本，保留 Preview19 安装与账本。
2. 单独安装经核验签名与包名的正式包，首次启动确认它有独立空账本。通过 SAF 打开 B0，先检查，再查看恢复预览。仅在确认包身份、目标账本和预览后执行一次覆盖。目标已有记录或预览计数与预期不符时停止。
3. 重开正式版，核对物品、投入/收回、订阅、付款与历史、类别、关联、当前封面和历史照片统计。重选主题。再导出新 B-H，留作正式包恢复点。
4. 另获授权后，仅对合成 B0/B-H 离线比较业务字段、关联、媒体 ID、存储字节、长度、SHA 和尺寸。检查时区及备份覆盖的设置，按恢复保护规则核对截止日和水位，不以整个 ZIP 字节相等作为通过条件。
5. 校验失败、身份不符、恢复 UNKNOWN 或前后不一致时停止。保留两个包、账本、B0 和现场，等独立审查。Preview18→19 的同包用户成功反馈保持 USER_REPORTED_MANUAL_PASS_WITH_NOTES，此迁移是跨包 SAF 路径。

## 验证结果与验收缺口

- JVM 定向运行 `:app:testDebugUnitTest`，过滤 `*PhotoLite*Test`、`*BackupInspectionServiceTest`、`*BackupRestoreControllerTest`。共 71 PASS、0 失败、0 错误、0 跳过。七类覆盖备份检查 31、backup4 照片 7、照片变更策略 7、资源限制 5、PNG 9、能力门禁 2、恢复反馈 10。新增合成归档使用 Preview19 和 `1.0.0` 生产者标签，验证两份可准入及现有封面与历史照片的记录、长度、SHA、尺寸与原字节。
- 新增两项 AndroidTest 源码，分别检查不可用说明与操作隐藏，以及模拟授权失效和坏缩略图后安全拒绝并可重新选择。真实 Picker、URI 和文件系统验证仍待设备执行。
- `:app:compileDebugKotlin`、`:app:compileDebugAndroidTestKotlin`、`:app:compileReleaseKotlin` 与 `:app:lintRelease` 均通过。Release Lint XML 有 0 错误、20 警告。规则、文件和行号与既有 Preview19 Debug Lint 记录一致。通过 `javap -c` 检查实际 Kotlin class，`PhotoLiteCapability.getEnabledInBuild()` 在默认 Release 返回 true，在普通 Debug 返回 false。
- Gradle 8.14.3 离线单 worker，配置与验证输出按 `app/build/v1-release-alignment-{base,trial,experiment,b1qa}` 隔离。主验证起始可用内存 4,818,416 KiB，退出 0，耗时 12m24s。执行前 dry run 核对任务图，结束后四个目录中 APK 数为 0。Private Trial、PHOTO-LITE 和 B1 QA 任务列表无 Release 配置及组装任务。

既有证据在 [171 项 JVM 原始汇总](v1-core-noop-20261105/junit-summary.json)、[编译与 Lint 日志](v1-core-noop-20261105/verification-rerun.log)、[Lint XML](v1-core-noop-20261105/lint-results-debug.xml)、[Preview19 实物审计](v1-core-noop-20261105/artifact-2/identity.json) 和 [36 份历史 APK 归档核对](v1-core-noop-20261105/protected-apks-after-archive.json)。这些是原次的 PASS。本轮已重算 Preview19 本地归档大小 20,506,559 字节及 SHA-256 `34ef14ac074fe5afafdf9a554518671e576ea8c0d4144537e39195f0947cf2f6`，证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` 沿用原静态证据。

下一阶段在用户授权的独立设备上集中验证 API 36 正式变体的选择、取消、授权失效、坏图、超预算、迟到结果、换代与三种封面操作。对不支持的 API 观察说明和已登记封面读取。在合成 B0/B-H 完成真实 SAF、媒体字节及异常恢复核验后，再审正式签名与 G1–G4 的适用门禁。Android instrumentation、正式签名、实物包验证、真实 SAF、跨包恢复和资源峰值仍为 NOT_RUN 或 UNKNOWN。M20 的两次历史通知可见性 FAIL 和旧任务收口 UNKNOWN、M22 的历史 FAIL 与后续 NOT_RUN 保留。

本轮未构建或签署 Release APK，未生成新 Preview，未操作设备或真实用户数据。**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**，自动提醒延期至 V1.1。

# 砺石 V1.0 正式发行收官

2026-11-06。**V1_RELEASE_BLOCKED / V1_RELEASE_NO_GO**。本轮完成桌面审查。正式版本号待确认，正式签名未接线、未获签署授权。没有生成正式 APK。

## 基线与现有产物

本地与远端 R2 HEAD 均为 `94b09cdb6a42cd9ced141f84627526ecd3f4c0b2`，工作区干净。从该提交建立 `codex/v1-final-release-closeout`，远端当时无同名分支。Release Alignment 为 `9726b4e762e18eb5f589f2e0e6cda316716c5ce4`，核心基线为 `e01b9368eabd967733d08ed307f3797ed61bc453`，审计分支 `61e28101` 只读。收官只增加本文，App 树保持 `35293bb9d73726f60106849f396b07c29e203e21`，报告提交的实际 SHA 由 Git 记录。 [R2 准备](LITHIC-V1-R2-Release-Preparation.md) · [Release Alignment](LITHIC-V1-Release-Alignment.md)

Preview19 归档实测 20,506,559 字节，SHA-256 `34ef14ac074fe5afafdf9a554518671e576ea8c0d4144537e39195f0947cf2f6`。AAPT2 确认 `com.cooper.lithic.preview.debug`、`0.1.0-preview.19-debug`、code35。JDK17 调用现有 apksigner 验证退出 0，v2 签名通过，Debug 证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。小米 14 的 Preview18→19 同包更新及记录保留为 `USER_REPORTED_MANUAL_PASS_WITH_NOTES`。 [Preview19 实物审计](v1-core-noop-20261105/artifact-2/identity.json) · [核心版交付证据](LITHIC-V1-CORE-FASTTRACK.md)

## A–B · 身份、版本和签名

| 项目 | 结论 |
| --- | --- |
| 正式身份 | `com.cooper.lithic`。当前默认 Release 配置仍是 code17 / `0.1.0-m22`。建议首发 `1.0.0`、code36，状态 `PROPOSED_PENDING_USER_CONFIRMATION`。 |
| 配置历史 | Git 复核 239 个可达提交、48 份不同 Gradle 配置、82 个远端分支头。普通默认 Release 最大已见 code17，由 `5a446ee2` 引入。`e01b9368`、`782f80ea` 的旧试用参数可能为 Release 的正式 ID 配置 code35。Alignment 禁止该组合。 |
| 发行实物 | 远端 tag、GitHub Releases 和可见 Actions 产物均为 0。历史保护清单、交付、验收和已知构建输出合计 39 条去重 APK 路径，AAPT2 均退出 0。精确匹配正式 ID 的 APK 为 0，未找到 AAB。外部分发 **UNKNOWN**。如确认曾发行更高 code，使用该正式包已发行最高值加一。 |
| 签名与优化 | 跟踪源码没有正式 Release signer、外部凭据接线或 CI 签名定义。用户安全存储和外部 CI 为 **UNKNOWN**。R8/minify、资源压缩均未显式开启，正式实物的优化状态、签名及构建可重复性 **NOT_RUN**。 |

正式包的 fail-closed 方案在签署获批后才接线。独立 Release 构建入口先核对源提交、正式包名和批准的版本，要求用户控制的 Git 外密钥路径、alias、受限凭据注入以及预先确认的公开证书 SHA-256。凭据缺失、正式证书不匹配或等于 Debug 证书时，在打包前失败。用锁定 JDK/SDK/Gradle、依赖与明确的优化配置构建一次，记录输入和任务结果。归档前重新用 apksigner 校验签名及证书，再核对 AAPT2 包名/版本、权限与合并清单、实际 DEX、R8 状态、字节数与 SHA，最后与构建产物逐字节比对。正式归档目标为 `deliveries/lithic-v1.0.0/`，签署输入和密钥由所有者通过安全渠道管理。 [当前构建定义](../../app/build.gradle.kts) · [原签名门禁](M13-T06-readiness.md) · [历史 APK 清单](v1-core-noop-20261105/protected-apks-after-archive.json)

## C–D · 照片、空账本和并存

- 五种既有生成变体为默认 Release `PRODUCTION=true / EXPERIMENT=false`、普通 Debug `false / false`、Private Trial/PHOTO-LITE/B1 QA Debug `false / true`。试用与实验身份限 Debug。已复查同树 Release 合并 Manifest SHA-256 `f434d8318b552447c489306a7d5921772c10080140bd5dda1e98671bc1a6ef08`。用 JDK17 `javap -c -p` 检查现有 Release Kotlin 类，构建开关折叠为 true，API 检查仍调用 `supportsApi`。已有 Preview19 DEX 只证明其自身的照片、B1/B3、主题和启动接线。正式 APK 的 DEX、R8 产物及证书审计 **NOT_RUN**。 [生成清单与开关](LITHIC-V1-Release-Alignment.md) · [体验包 DEX 审计](v1-core-noop-20261105/artifact-2/identity.json)
- Release writer 仅 API 36 创建，Picker 探测通过后详情才接入添加、更换、取消封面。其他 API 不进入选图、输入缩略图/解码或写入路径，已有认证封面的读取独立。缩略图处理单飞，成品静态 PNG 最长边 640 px、小于 2 MiB。写入由同一 Owner 实例与 generation、协调器租约、文件身份/长度/SHA 和原引用 CAS 守护。备份检查、导出、恢复及未决编辑时禁用操作。取消封面只清引用，登记记录及文件继续保留。 [能力开关](../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteCapability.kt) · [Writer](../../app/src/main/java/com/cooper/lithic/data/media/photolite/GenerationPhotoLiteCoverWriter.kt) · [导航](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt) · [处理预算](../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteThumbnailPipeline.kt)
- Picker 探测返回 false 时隐藏操作并显示说明。现有 AndroidX Activity 1.12.4 在 API ≥33 按平台版本报告能力，API 36 的实际处理组件缺失或禁用时可能误报。添加/更换启动异常会反馈失败，取消入口仍受同一探测控制。该场景的预先隐藏、OEM 启动和实际资源峰值是 **UNKNOWN / NOT_RUN**。受控环境若复现，应先修准入和失败后状态，再通过对应正式门禁。 [Picker 面板](../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteCoverPanel.kt) · [依赖版本](../../gradle/libs.versions.toml)
- 合并清单保留 `LithicApplication`、`MainActivity`、各自包名下的 authority 与专用权限，`allowBackup=false`。无共享 UID、网络或广泛媒体读取权限。正常 Activity 才启动业务 Owner，账本位于当前应用的 `noBackupFilesDir`。活动指针及备份/暂存均不存在且没有既有代时才创建初始 Room5，业务记录为空，内置分类正常存在。既有代或指针异常时保留现场并报错。正式首次安装、实际 UID/数据隔离、重开及 Preview19 并存为 **NOT_RUN**。 [主清单](../../app/src/main/AndroidManifest.xml) · [MainActivity](../../app/src/main/java/com/cooper/lithic/app/MainActivity.kt) · [AppContainer](../../app/src/main/java/com/cooper/lithic/app/AppContainer.kt) · [数据代保护](../../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt)

**`PREVIEW_TO_RELEASE_DATA_MIGRATION = NOT_APPLICABLE_BY_USER_DECISION`**。正式包从独立空账本开始，Preview19 保留原安装和记录。

## E · 正式包自身完整备份

backup4 的 format/minReader/schema 是 **4/4/5**。协调器保护下快照十表：分类、物品、金额事件、登记媒体、订阅、付款、状态变更、替换、通知回执与里程碑水位。备份包含记录时区、两项提醒偏好及导入截止日。主题、外部 URI grants 和 WorkManager 队列不在 backup4。复制所有已登记图片的原存储字节，包含当前无引用的历史照片。校验 UID、普通文件与链接数、身份、长度、SHA、格式和像素尺寸。 [快照](../../app/src/main/java/com/cooper/lithic/data/backup/RoomBackupSnapshotSource.kt) · [设置字段](../../app/src/main/java/com/cooper/lithic/data/backup/BackupDtos.kt) · [媒体核验](../../app/src/main/java/com/cooper/lithic/data/backup/BackupMediaSnapshot.kt)

B1 检查只操作受控暂存副本。恢复对私有副本再校验并展示数量，确认时一次性消费预览 token，复核 Owner 和目标快照，在验证新代后原子切换。仅在原代仍可确认时反馈已保全，否则反馈 UNKNOWN。源备份保留。恢复的提醒截止日和里程碑水位按原保护合同核对。桌面测试区分 UI 重复确认与真实 token/Owner 切换。后者及真实 SAF、中断、重开验收在当前正式包仍为 **NOT_RUN**。 [B1/R1 分类](LITHIC-V1-B1-R1-Classification.md) · [恢复协调器](../../app/src/main/java/com/cooper/lithic/data/backup/RestoreCoordinator.kt)

## F · 核心门禁

| 门禁 | 桌面、历史与真实运行边界 |
| --- | --- |
| 物品/成本、订阅/付款/历史（G1/G2） | **PASS**，C7 `1c97ff61` 的 56 项桌面回归；Preview19 为用户人工反馈。正式包完整旅程 **NOT_RUN**。 [C7](LITHIC-C7-Product-Integration.md) |
| 照片、B1/B3、三主题 | **PASS**，P3 `dd31ce4b` 桌面，B1-R1 `08e11f20` 桌面分类，B3 `0e9b3610` 的 62 JVM、8 SQLite，C9-R1 `3aa4f30f` 的 95 JVM。独立 B1-QA A～H 为用户报告。正式 API36 Picker、照片操作和主题持久化 **NOT_RUN**。 [P3](LITHIC-PHOTO-LITE-P3-Integration.md) · [B3](LITHIC-V1-B3-Photo-Space-Management.md) · [B2](LITHIC-V1-B2-SAF-Acceptance-Preparation.md) · [C9-R1](LITHIC-C9-R1-Final-Visual-Refinement.md) |
| Room5/backup4 | **PASS**，格式、快照、媒体及恢复反馈的桌面证据。当前正式包真实 SAF 往返、一次性恢复确认与失败/异常中断 **NOT_RUN**。 [R2](LITHIC-V1-R2-Release-Preparation.md) |
| 启动/Owner、旧提醒 no-op（G3） | **PASS**，核心 `e01b9368` 171 JVM 与 Preview19 DEX，后台 Application/配置不主动打开账本，旧 Worker 四种 mode 在获取 Owner 前提前返回，无提醒入队和主动 WorkManager 取消。真实唤醒、任务图、终结及耗电 **UNKNOWN / NOT_RUN**。自动提醒和 RC4 **DEFERRED** 至 V1.1。M20 `d2be4cb2` 22 项历史用例 PASS、两项系统通知可见性 **FAIL**，`d3776e0b` 平台及 Owner 收口 **UNKNOWN**。 [核心版](LITHIC-V1-CORE-FASTTRACK.md) · [M20 平台测试](M20-Android-verification.md) · [M20 收口](M20-Platform-closure.md) |
| 媒体安全 | C6-1 `88e7edb2` **PASS**，44 JVM，14 Android **NOT_RUN**。C6-2D `76ae1daf` 环境阻断，8 Android **NOT_RUN**。M10 三项 JPEG 历史 **FAIL**。严格 JPEG 原图路径保留阻断，永久删除/GC **DEFERRED**。开放的 API36 PHOTO-LITE 仍须单独验收。 [C6-1](LITHIC-C6-1-ReadOnly-Media.md) · [C6-2D](LITHIC-C6-2D-Runtime-Verification.md) · [JPEG 证据](LITHIC-C6-2A-JPEG-Admission.md) |
| M22 离线、长历史、字体/IME、兼容 | `5a446ee2` 为历史局部 **PASS**，T06 三次 **FAIL/INCOMPLETE**，后来真机离线准入 `3c3e293c` 后仍 **NOT_RUN**。T03 正式 20 年样本、真实 IME/200% 字体和当前正式包兼容 **NOT_RUN**。 [M22](M22-verification.md) |
| 权限、包身份和发行（G4） | **PASS**，同树生成清单与现有 Preview19 签名实物的各自范围。正式证书、版本历史、签署、安装及后续同签升级仍 **UNKNOWN / NOT_RUN**。Preview→正式迁移 **NOT_APPLICABLE**。G1～G4 不因局部证据关闭。 [Alignment](LITHIC-V1-Release-Alignment.md) · [RC0 发行差距](LITHIC-V1-RC0-Product-Integration.md) |

## 本轮验证与构建停止点

- **本轮实跑 PASS（JDK17/JUnit4，80 项，0 失败、0 跳过，退出 0）**。复用 R2 同一 App 树的 155 项 Gradle worker classpath 与已编译类，直接运行有界 JVM（384 MiB 堆、2 个可用处理器、120 秒上限）。启动前可用内存 3,826,828 KiB，未达到 Gradle 的 4 GiB 门槛。本轮不启动 Gradle。按类结果为 Owner 启动 8、提醒无入队/取消 3、照片能力 2、恢复控制器 10、恢复反馈 11、完整归档 9、备份检查 31、照片 backup4 集成的非迁移用例 6。覆盖合成完整归档、媒体缺失/损坏、SHA 拒绝及源字节保留、历史媒体纳入、重复确认、失败和 UNKNOWN。无 Preview 迁移测试。
- **首轮退出 124（90 秒上限）**。完整归档 9 项已输出 PASS，后续整组 `BackupValidatorTest` 未输出完成结果，原因 UNKNOWN，未计入通过数。上述定向实跑未纳入整组 Validator 测试，仍由备份检查及媒体集成断言覆盖本轮 SHA、损坏、缺失和格式合同。首次和定向原始 stdout/stderr、命令入口及回执分别保存在本机 `%TEMP%/lithic-v1-closeout-20261106-105046/` 与 `%TEMP%/lithic-v1-closeout-focused-20261106-105313/`。两轮 stdout SHA-256 分别为 `8ee6d7edbf41f4612f996c4b84b9f94b3592ee3c2e3f450db9e9876258039c75` 和 `541bbe537b01129d045c945045d17a1aa6e3d693e1822211f9ed29b96790e619`。
- **继承／缓存复用**。Alignment 同 App 树的 71 JVM、Debug/Release/AndroidTest Kotlin 编译和 Release Lint（0 错误、20 警告）。R2 另有 57 JVM 实跑，同树编译、生成及 Lint 任务为 UP-TO-DATE。本轮仅重新读取既有生成清单和 Release JVM 字节码，未重编、未运行 Lint 或 AndroidTest。 [Alignment](LITHIC-V1-Release-Alignment.md) · [R2](LITHIC-V1-R2-Release-Preparation.md)
- **本轮实物静态 PASS**。已复算历史保护清单中 36/36 份 APK 的长度和 SHA，全部匹配。无新增候选。正式 APK、签名和实际 DEX/R8、真机启动、Picker、SAF、异常恢复及正式发布证据 **NOT_RUN**。

正式版本号与签署仍是出包门禁。用户一次确认正式 ID 是否曾分发 code>35、分发渠道与正式签名保管方式，提供预期公开证书 SHA-256，并在安全外部注入就绪后授权一次正式签署。随后由 Codex 执行正式构建、实物审计，由用户完成下面的真机核对，由独立审查方决定发行。未运行正式 APK、签名、设备、ADB、模拟器、真实 SAF 或真实用户数据操作。

## 用户正式候选验收清单

1. 保留 Preview19。独立安装经审计的 `com.cooper.lithic`，验证首次空账本。创建一条测试物品和订阅，重开读回，另开 Preview19 检查原状态。身份或数据异常立即停止。
2. 在 API36 且系统 Picker 可启动时，用可丢弃图片依次添加、预览保存、更换、取消封面，核对历史登记媒体仍保留。在不支持的受控环境检查入口隐藏、提示准确和已有封面可读。
3. 正式包用合成账本导出一份唯一文件，检查后核对预览，仅在明确确认合成目标时执行一次恢复。重开核对物品、订阅、金额、照片引用及媒体。失败或 UNKNOWN 时保留原备份和现场。
4. 核对首页、成本/历史、退出重开及三主题持久化。留存正式包适用的离线、长历史、系统大字/IME、兼容和受控恢复异常证据。若出现无响应、记录差异或错误成功提示，停止依赖操作。

**V1_RELEASE_BLOCKED / V1_RELEASE_NO_GO**。`STRICT_JPEG_IMPORT_BLOCKED`、`ANDROID_ISOLATION_RUNTIME_PENDING` 保留。自动提醒延期至 V1.1，照片永久删除/GC 延期。

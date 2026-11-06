# 砺石 V1.0 私人 APK 发行准备

2026-11-06。**V1_PRIVATE_APK_READY_FOR_INDEPENDENT_REVIEW**。已归档唯一正式签名 APK，渠道为个人或少数人直接分发。真机验收与独立审查待执行，保持 **V1_RELEASE_NO_GO**。

- 交付文件为 [Lithic-1.0.0-release.apk](../../deliveries/lithic-v1.0.0/Lithic-1.0.0-release.apk)，15,747,081 字节。
- 实物身份为 **com.cooper.lithic / 1.0.0 / code36**，minSdk 26，targetSdk 36。
- APK SHA-256 为 ff909ca1eb21e04756d91d067082ca12df1ac17c263d793843fbbf4c67e40c2b。
- 正式证书 SHA-256 为 3a10918a24ab4c66fd2f5e2725b0b8a65164d4da43f71a40d6e6d74afc4b1f19。

## 基线与版本

从干净且与远端一致的 `604f5e1407bcf519c41ac59e15f7417028a34de3` 建立 `codex/v1-private-apk-release`。原 App 树为 `35293bb9d73726f60106849f396b07c29e203e21`。本轮改动限 App 构建脚本及本文。正式版本由 Release VariantOutput 设置为 **`com.cooper.lithic` / `1.0.0` / code36**，普通 Debug 与实验身份继续使用原配置。用户确认正式包从未发行。旧配置 code35 的可见历史参见收官审查。 [构建定义](../../app/build.gradle.kts) · [收官报告](LITHIC-V1-Final-Release-Closeout.md)

Preview19 归档复核 20,506,559 字节、SHA-256 `34ef14ac074fe5afafdf9a554518671e576ea8c0d4144537e39195f0947cf2f6`。AAPT2 与 apksigner 退出 0，实际包名 `com.cooper.lithic.preview.debug`、`0.1.0-preview.19-debug`、code35、单一 v2 Debug 签名，证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。本次再次对照历史保护清单，36 份 APK 的实际大小和 SHA 全部一致。小米 14 Preview18→19 同包更新反馈为 `USER_REPORTED_MANUAL_PASS_WITH_NOTES`。 [历史保护清单](v1-core-noop-20261105/protected-apks-after-archive.json) · [Preview19 证据](LITHIC-V1-CORE-FASTTRACK.md)

**`PREVIEW_TO_RELEASE_DATA_MIGRATION = NOT_APPLICABLE_BY_USER_DECISION`**。正式版独立安装，由本包 `noBackupFilesDir` 管理数据代。无指针及既有数据代时创建 Room5 空账本和默认分类。发现异常活动指针或既有代时保留现场并报错。普通用户入口仍为 `MainActivity`，旧 Worker 冷启动不会主动打开账本。

## 签名、备份与签署授权

所有者创建的既有 PKCS12 原件保存在本机受控目录，固定 alias 为 lithic-release。所有者在独立 PowerShell 输入密码后，keytool 导出 DER 公开证书。复核主体 CN=Lithic Release、RSA 3072、证书 SHA-256 与上文一致，有效期为 2026-11-06 16:19:15 至 2053-11-06 16:19:15（Asia/Shanghai）。证书与 Preview19 的 Debug 证书不同。

2026-11-06 17:08:29（Asia/Shanghai），所有者在证书显示后输入 SIGN，一次确认两处独立加密备份均已验证可恢复，并授权本次正式签署。备份恢复结果按所有者确认记录。Codex 审计范围为公开证书、无密回执和 APK。密码仅通过本机隐藏提示输入，由签署进程临时提供给 Gradle。可能含凭据的 AGP 中间文件保留在所有者控制的 Git 外构建目录。

既有签名门禁在配置时检查证书有效期、公开指纹、别名、私钥与证书对应性及输出目录。执行入口为离线 :app:assembleRelease，单 worker，关闭常驻 daemon、配置缓存、构建缓存及扫描。

## 照片、权限与备份

- 新生成的 Release BuildConfig 确认 code36、`1.0.0`、`PHOTO_LITE_PRODUCTION=true`、`PHOTO_LITE_EXPERIMENT=false`。普通 Debug 为 `com.cooper.lithic.debug`、code17、`0.1.0-m22-debug`，两个照片开关均为 false。实验身份仅 Debug 的配置沿用 Alignment。JDK17 javap 确认新编译的 Release 照片能力返回 true、Debug 返回 false，两个变体仍调用 API 准入判断。 [变体配置证据](LITHIC-V1-Release-Alignment.md)
- Release 合并 Manifest SHA-256 `34ee913dab73ac4730f2b714681590b1fe13d77c815a39776c41bfbbbab50fe0`。与 Alignment XML 逐项比较，只有版本字段不同。`allowBackup=false`，使用既有数据提取及备份排除规则。无 sharedUserId、INTERNET 或广泛媒体权限。Startup authority 为 `com.cooper.lithic.androidx-startup`。导出组件为 Launcher、受 BIND_JOB_SERVICE 保护的 WorkManager 服务和两项受 DUMP 保护的库接收器。Manifest 保留旧通知、唤醒锁、网络状态和开机声明，提醒运行仍关闭。

Release 照片写入仍为 API36 + Picker 准入，仅追加、更换、取消封面引用。其他 API 不进入 Picker、输入缩略图或写入，既有认证封面可独立读取。单飞处理、静态 PNG ≤640 px、<2 MiB、文件身份与 SHA、同一 Owner/generation、原引用 CAS 和备份/恢复互斥继续受原实现保护。取消显示后已登记图片仍可纳入完整备份。Picker 探测为 false 时隐藏入口，AndroidX 的 API ≥33 检查无法证明 OEM 处理器一定能启动，处理器异常及资源峰值为 **UNKNOWN / NOT_RUN**。 [照片与启动源码证据](LITHIC-V1-Final-Release-Closeout.md)

正式包自身 backup4 使用 format/minReader/schema **4/4/5**，同代协调器保护十表快照和登记媒体，包括无引用历史照片的原存储字节。检查身份、长度、SHA、格式及尺寸。设置保存记录时区、提醒偏好和导入截止日。主题、外部 URI grants、WorkManager 队列不在备份范围。B1 检查受控副本。恢复先验证并预览，确认时一次性消费 token、复核目标，验证新代后原子切换。失败或 UNKNOWN 如实呈现，保留源备份。真实 SAF、恢复 token 与 Owner 切换及中断恢复 **NOT_RUN**。 [备份与恢复证据](LITHIC-V1-R2-Release-Preparation.md)

## 实际验证

- 首轮离线单 worker，JDK17、Gradle 8.14.3、AGP 8.13.2，启动前可用内存 5,000,944 KiB。Release Kotlin、Release 单元测试 Kotlin、Debug AndroidTest Kotlin 及 Manifest/BuildConfig 均实际执行完成。生产 Lint 分析已产生 partial 输出。单元测试 Lint 阶段长时间未完成，整轮约 55 分钟后停止本轮 Gradle daemon，退出 1。首轮 JVM 执行尚未启动。完整 `lintRelease` 本轮 **BLOCKED**，继承 Alignment 的 0 错误、20 警告作为历史参考。原始回执和日志在 `%TEMP%/lithic-v1-private-apk-20261106-113012/`，日志 SHA-256 `84be666962a944c37ad57c4e23174b6b04eee4bf463683bd2fd0316b9d6668dc`。 [继承 Lint 证据](LITHIC-V1-Release-Alignment.md)
- 最终脚本复核启动前可用内存 4,488,348 KiB，Gradle 成功配置，Release Kotlin、Release 单元测试 Kotlin、Debug AndroidTest Kotlin、BuildConfig 与 Manifest 均为 **UP-TO-DATE**。本轮实际执行 Release 定向 JVM **80 项 PASS**，0 失败、0 错误、0 跳过。分别为 Owner 启动 8、旧提醒 3、照片准入 2、备份检查 31、完整归档 9、照片与 backup4 集成 6、恢复控制器 10、恢复反馈 11。用例覆盖缺失/损坏媒体、SHA、源备份保护、一次性确认与 UNKNOWN。此处的确认测试是桌面控制器行为，真实恢复 token 与代切换继续待验。
- 检查真实 AGP `packageRelease`、`bundleRelease`、`packageReleaseBundle`、`signReleaseBundle` 的门禁依赖。无签名门禁和 AAB 拒绝门禁各返回预期错误，补跑命令退出 1 仅因这两项主动拒绝。JVM XML 已单独核对。没有运行 `assembleRelease` 或实际包装任务。独立验证目录 APK/AAB 均为 0。回执在 `%TEMP%/lithic-v1-private-apk-final-20261106-122645/`，日志 SHA-256 `6c8b9ab560aa15fd2e4aa41de5b0f5935fc502b5f35e3693bcc6517135e5793d`，最终构建脚本输入 SHA-256 `5fd0496845465de23a43966fcd0bcf37275c8dc3d9de04dc6e9376d0168b2b1c`。
- **完整 Release Lint 复验通过**。同一正式构建基线 `efded90b69518e7d3f2b97a201512513f0f4be4f` / App 树 `39295c800ceefdeee19aefce39b26755d3b8d63a`，离线、单 worker，使用 E 盘现存同版 JDK17 与 SDK36，未改源码、Lint 规则或检查范围。启动前可用内存 6,928,372 KiB。仅请求 `:app:lintRelease`，任务图核验没有 APK/AAB 打包、签署、测试执行或设备任务。生产分析 `UP-TO-DATE`，`lintAnalyzeReleaseUnitTest`、`lintReportRelease`、`lintRelease` 实跑，Gradle 退出 0（5 分 59 秒）。新生成 XML 及文本均为 **0 错误、20 警告**。XML SHA-256 `6c430927fc0f93b327738bfad09877fb091e51744024ca88fd4f1d6a517fde34`，位于 `app/build/v1-private-apk-preflight/reports/lint-results-release.xml`。原始日志与回执在 `%TEMP%/lithic-v1-release-lint-20261106-153433/`，日志 SHA-256 `6edfa27fae5a471357bc9cedb61549abb7463d0c72cc65556dfdd1ef6ceb16e6`。此前 80 项 JVM 结果与编译证据继承，本次未重跑；AndroidTest 实际运行仍 **NOT_RUN**。
- **卡点与保留证据**。AGP Lint 31.13.2 的单元测试模型未提供生产源码根，UAST 会退回扫描模块目录，同时仍纳入声明的测试源码。模块 `app/build` 当时有约 8 万个历史构建文件；同一 Java 进程的快照先显示 `Files.walkFileTree` / Windows 可读性检查，随后进入 UAST 解析和规则处理。定性为本机目录扫描耗时，未发现导致卡住的生产源码缺陷。复验前一次诊断拦截把 Lint 所需的 `bundleReleaseClassesToCompileJar` 误判为发包任务，退出 1，未运行任何分析；修正本地诊断拦截后才进行上述复验，未放行实际打包或降低 Lint。前次日志留于 `%TEMP%/lithic-v1-release-lint-20261106-153234/`，SHA-256 `aa7f694b7dc35b8132ee9225298bd99832eae3b8f79805492b9fb4feff95fbd3`。本机原 `local.properties` 未改，旧 SDK 路径提示和 SDK XML 版本提示未阻断复验。

M20 两项历史系统通知可见性 **FAIL**，旧任务/Owner 收口 **UNKNOWN**。旧 Worker no-op 保留，自动提醒及 RC4 延期至 V1.1。M22 历史离线旅程 FAIL/INCOMPLETE，当前正式包的离线、长历史、系统大字/IME 及兼容验证 **NOT_RUN**。严格 JPEG 三项历史 **FAIL**，`STRICT_JPEG_IMPORT_BLOCKED`、`ANDROID_ISOLATION_RUNTIME_PENDING` 保留，永久照片删除/GC 延期。没有操作设备、ADB、模拟器或真实用户数据。 [完整门禁](LITHIC-V1-Final-Release-Closeout.md)

## 正式签名构建与实物审计

- 正式构建来源为 a5b58158d98507d80eb619dec1beeeb1e3de2fee，App 树为 39295c800ceefdeee19aefce39b26755d3b8d63a。沿用同树完整 Release Lint（0 错误、20 警告）及 80 项定向 JVM PASS。核对既有 XML，保留签名打包自带的 Release 检查。AndroidTest 实际运行仍 NOT_RUN。
- 首次构建在 :app:kspReleaseKotlin 退出 1。C 盘外部构建目录与 E 盘源码不在同一文件系统根，KSP 无法计算 BuildConfig.java 相对路径，未到 APK 生成阶段。将唯一重试的构建输出改为 E 盘 NTFS 上的独立受控目录，沿用同一源码、正式证书与授权。所有者在安全终端重新输入原密码。重试于 17:14:04 至 17:26:46（Asia/Shanghai）完成，Gradle 退出 0。
- AAPT2、apkanalyzer、apksigner 严格模式与 zipalign 均退出 0。实际为单一 v2 签名者，正式公开证书 SHA 与授权记录匹配。二进制 Manifest 的包名、版本、入口、组件可见性及权限与正式合并基线一致。无 INTERNET、广泛媒体权限、调试、测试或共享 UID 声明。由实际资源表解析备份 XML，核对自动备份禁用及云备份和设备传输排除规则。
- APK 有三个 DEX，均可读取。核对正式照片开关开启、实验开关关闭、API 恰为 36 且需要真实 Picker 的入口、封面写入实现、主界面 Owner 启动、旧提醒 Worker 直接成功返回，以及完整备份检查和恢复实现。PNG、Room5、backup4 和恢复门禁沿用原 App 树。APK 仅含既有 AndroidX Graphics Path 与 DataStore native 库，无严格 JPEG 库。APK ZIP 校验及 16 KiB 页对齐检查通过。
- 首轮与第二轮静态脚本分别修正本机 AAPT2 的 minSdkVersion 输出名称及正式包压缩后的 XML 文件路径解析，保留两轮原始输出。第三轮从同一个 APK 解析 Manifest 及资源表，完成签名、权限、代码及历史包核对。另核对 Kotlin 编译后的实际方法体。36 份旧 APK 的大小和 SHA-256 均匹配。通过门禁后独占归档，原产物与交付文件逐字节一致。公开签名、构建及审计回执留在本机签署会话的公开证据区。

## 小米 14 首次安装与验收

1. 保留 Preview19。将本报告归档的 APK 复制到小米 14，从文件管理器点开安装。若系统要求，允许该文件管理器安装应用。首次打开正式版，确认版本 1.0.0 和空账本。创建一条测试物品和订阅，重开读回，单独打开 Preview19 核对原状态。
2. 在 API36 使用可丢弃照片添加、预览保存、更换、取消封面，核对历史登记媒体仍保留。检查 Picker 取消、不可用和坏图反馈。
3. 只用正式包合成账本导出并检查 backup4，明确确认后覆盖恢复。重开核对物品、订阅、金额、照片及媒体。失败或 UNKNOWN 时保留源文件和现场。
4. 检查首页、物品/订阅详情、成本和历史，三主题切换及重启持久化，提醒保持关闭。遇到无响应或数据异常时停止依赖操作。

桌面构建及实物静态审计已完成，等待 ChatGPT 独立审查。用户真机验收和独立审查通过前，保持 **V1_RELEASE_NO_GO**。

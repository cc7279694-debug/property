# 砺石 V1.0 私人 APK 发行准备

2026-11-06。**V1_PRIVATE_APK_BLOCKED / V1_RELEASE_NO_GO**。渠道为个人或少数人直接分发 APK。正式包首发历史按用户确认登记。完整 Release Lint 已通过（0 错误、20 警告）；剩余门禁为所有者创建密钥、核验并登记公开证书、验证两处独立加密备份及在本机明确授权签署。正式 APK 和 AAB 均未构建。

## 基线与版本

从干净且与远端一致的 `604f5e1407bcf519c41ac59e15f7417028a34de3` 建立 `codex/v1-private-apk-release`。原 App 树为 `35293bb9d73726f60106849f396b07c29e203e21`。本轮改动限 App 构建脚本及本文。正式版本由 Release VariantOutput 设置为 **`com.cooper.lithic` / `1.0.0` / code36**，普通 Debug 与实验身份继续使用原配置。用户确认正式包从未发行。旧配置 code35 的可见历史参见收官审查。 [构建定义](../../app/build.gradle.kts) · [收官报告](LITHIC-V1-Final-Release-Closeout.md)

Preview19 归档复核 20,506,559 字节、SHA-256 `34ef14ac074fe5afafdf9a554518671e576ea8c0d4144537e39195f0947cf2f6`。AAPT2 与 apksigner 退出 0，实际包名 `com.cooper.lithic.preview.debug`、`0.1.0-preview.19-debug`、code35、单一 v2 Debug 签名，证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。本次再次对照历史保护清单，36 份 APK 的实际大小和 SHA 全部一致。小米 14 Preview18→19 同包更新反馈为 `USER_REPORTED_MANUAL_PASS_WITH_NOTES`。 [历史保护清单](v1-core-noop-20261105/protected-apks-after-archive.json) · [Preview19 证据](LITHIC-V1-CORE-FASTTRACK.md)

**`PREVIEW_TO_RELEASE_DATA_MIGRATION = NOT_APPLICABLE_BY_USER_DECISION`**。正式版独立安装，由本包 `noBackupFilesDir` 管理数据代。无指针及既有数据代时创建 Room5 空账本和默认分类。发现异常活动指针或既有代时保留现场并报错。普通用户入口仍为 `MainActivity`，旧 Worker 冷启动不会主动打开账本。

## 签名、备份与签署授权

所有者已批准创建一个永久正式密钥，并选择本机原件加两处独立加密云备份。已准备 `%LOCALAPPDATA%\LithicSigning`，它不在仓库、临时目录或同步目录下；核对时目标 `lithic-release.p12` 不存在。目录 ACL 只列所有者、本机管理员与 SYSTEM。未打开任何 keystore、读取秘密、生成密钥或执行签名。签名输入环境变量未设置；公开证书、两处可恢复备份及一次正式签署授权均待所有者确认。**`SIGNING_BACKUP_PENDING`**。

Gradle 预留显式本机签署入口。正常测试、编译、Lint 无需签名输入。正式打包前必须核对 Git 外的密钥和独立受控输出目录、alias、有效公开证书及预期 SHA-256，再验证私钥与证书对应。拒绝已知 Preview Debug 证书、Android Debug 证书和 Android 注入签名覆盖。APK 打包任务直接依赖签名门禁，AAB 包装任务拒绝执行。当前未启用 R8/minify 或资源压缩，避免在首发引入未经实物验证的反射、序列化和资源差异。实际正式 DEX、签名和 R8 状态的产物审计为 **NOT_RUN**。 [构建定义](../../app/build.gradle.kts)

请在自己打开的 PowerShell 中运行以下命令。`keytool` 会交互提示输入、确认密钥库密码；密码留在本机，不加到命令里。脚本发现同名文件时会停止，避免重新生成或覆盖。证书不包含个人资料，固定别名 `lithic-release`，有效期 9,862 天（约 27 年）。

```powershell
$lithicKeytool = 'E:\CODEX\MirraAndroid\jdk17\jdk-17.0.20.1+1\bin\keytool.exe'
$lithicKeystore = Join-Path $env:LOCALAPPDATA 'LithicSigning\lithic-release.p12'
if (Test-Path -LiteralPath $lithicKeystore) { throw '签名文件已存在，停止。' }
& $lithicKeytool -genkeypair -keystore $lithicKeystore -storetype PKCS12 `
  -alias lithic-release -keyalg RSA -keysize 3072 -validity 9862 -dname 'CN=Lithic Release'
if ($LASTEXITCODE -ne 0) { throw '密钥创建未成功，请保留现场。' }
& $lithicKeytool -list -v -keystore $lithicKeystore -storetype PKCS12 -alias lithic-release
if ($LASTEXITCODE -ne 0) { throw '公开证书核对未完成。' }
```

1. 记下 `keytool` 显示的 **证书 SHA-256** 和有效期。证书指纹不是 `.p12` 文件哈希，且必须不同于上文的 Preview19 Debug 证书。
2. 在 7-Zip 文件管理器中仅选择 `.p12`，添加到压缩包。选 **7z**、**AES-256**、勾选**加密文件名**。设置不同于密钥库密码的强密码，用密码管理器或妥善保存的纸质记录分别保管两种密码。只将加密的 `.7z` 上传到两个独立的云服务或受控存储；原始 `.p12` 留在本机。上传及账号操作由所有者亲自完成。
3. 分别从两个位置取回加密包，使用 7-Zip 的“测试”，再各自解压到本机新建、受控且不参与同步的目录。用 `Get-FileHash -Algorithm SHA256 -LiteralPath <文件>` 在本机比较原件与各自恢复的 `.p12`；两份都须与原件一致。用上述 `keytool -list -v` 指向每份恢复文件核对同一公开证书。验证完由所有者处理临时解压副本，原始密钥和加密备份保留。只需回报两处是否都通过恢复核验，不提供云凭据、压缩密码或密钥文件。
4. 完整 Release Lint 通过后，回报公开证书 SHA-256、有效期、两份备份已验证可恢复，并在本机安全终端明确授权一次正式签署。以后正式更新沿用同一正式证书。

获准签署时，仅在用户控制的独立本地进程向 Gradle 注入 `LITHIC_PRIVATE_APK_SIGNING`、`LITHIC_RELEASE_KEYSTORE`、`LITHIC_RELEASE_BUILD_DIR`、`LITHIC_RELEASE_KEY_ALIAS`、`LITHIC_RELEASE_CERT_SHA256` 、`LITHIC_RELEASE_STORE_PASSWORD` 和 `LITHIC_RELEASE_KEY_PASSWORD`。签署仅用 `:app:assembleRelease`，离线、单 worker，关闭 daemon 常驻、配置缓存、构建缓存和构建扫描。AGP 可能写出含凭据的受限中间文件，留在所有者的 Git 外工作区。最终只归档经验签和 Manifest、DEX、权限、版本、R8、大小及 SHA 审计通过的 `deliveries/lithic-v1.0.0/Lithic-1.0.0-release.apk`，并逐字节核对。

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

## 正式候选通过实物审计后的用户验收

1. 保留 Preview19。独立安装正式包，确认包名和空账本。创建一条测试物品和订阅，重开读回，单独打开 Preview19 核对原状态。
2. 在 API36 使用可丢弃照片添加、预览保存、更换、取消封面，核对历史登记媒体仍保留。检查 Picker 取消、不可用和坏图反馈。
3. 只用正式包合成账本导出并检查 backup4，明确确认后覆盖恢复。重开核对物品、订阅、金额、照片及媒体。失败或 UNKNOWN 时保留源文件和现场。
4. 检查首页、物品/订阅详情、成本和历史，三主题切换及重启持久化，提醒保持关闭。遇到无响应或数据异常时停止依赖操作。

正式签署和实际 APK 静态审计完成后才可申请独立候选审查。用户真机验收和独立审查通过前，保持 **V1_RELEASE_NO_GO**。

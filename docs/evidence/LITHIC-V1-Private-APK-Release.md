# 砺石 V1.0 私人 APK 发行准备

2026-11-06。**V1_PRIVATE_APK_BLOCKED / V1_RELEASE_NO_GO**。渠道为个人或少数人直接分发 APK。正式包首发历史按用户确认登记。正式签名材料、公开证书及签署授权待确认，本轮完整 Release Lint 未完成。正式 APK 和 AAB 均未构建。

## 基线与版本

从干净且与远端一致的 `604f5e1407bcf519c41ac59e15f7417028a34de3` 建立 `codex/v1-private-apk-release`。原 App 树为 `35293bb9d73726f60106849f396b07c29e203e21`。本轮改动限 App 构建脚本及本文。正式版本由 Release VariantOutput 设置为 **`com.cooper.lithic` / `1.0.0` / code36**，普通 Debug 与实验身份继续使用原配置。用户确认正式包从未发行。旧配置 code35 的可见历史参见收官审查。 [构建定义](../../app/build.gradle.kts) · [收官报告](LITHIC-V1-Final-Release-Closeout.md)

Preview19 归档复核 20,506,559 字节、SHA-256 `34ef14ac074fe5afafdf9a554518671e576ea8c0d4144537e39195f0947cf2f6`。AAPT2 与 apksigner 退出 0，实际包名 `com.cooper.lithic.preview.debug`、`0.1.0-preview.19-debug`、code35、单一 v2 Debug 签名，证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。历史保护清单 36 份全部与实际大小和 SHA 一致。小米 14 Preview18→19 同包更新反馈为 `USER_REPORTED_MANUAL_PASS_WITH_NOTES`。 [历史保护清单](v1-core-noop-20261105/protected-apks-after-archive.json) · [Preview19 证据](LITHIC-V1-CORE-FASTTRACK.md)

**`PREVIEW_TO_RELEASE_DATA_MIGRATION = NOT_APPLICABLE_BY_USER_DECISION`**。正式版独立安装，由本包 `noBackupFilesDir` 管理数据代。无指针及既有数据代时创建 Room5 空账本和默认分类。发现异常活动指针或既有代时保留现场并报错。普通用户入口仍为 `MainActivity`，旧 Worker 冷启动不会主动打开账本。

## 签名安全和唯一人工确认

任务书未指定现存密钥的安全路径。已检查正式签名接线及环境配置是否存在，无可核验正式 keystore 引用或已批准的公开指纹。外部安全存储是否已有正式密钥仍 **UNKNOWN**。未打开任何 keystore、读取秘密、生成密钥或执行签名。

Gradle 预留显式本机签署入口。正常测试、编译、Lint 无需签名输入。正式打包前必须核对 Git 外的密钥和独立受控输出目录、alias、有效公开证书及预期 SHA-256，再验证私钥与证书对应。拒绝已知 Preview Debug 证书、Android Debug 证书和 Android 注入签名覆盖。APK 打包任务直接依赖签名门禁，AAB 包装任务拒绝执行。当前未启用 R8/minify 或资源压缩，避免在首发引入未经实物验证的反射、序列化和资源差异。实际正式 DEX、签名和 R8 状态的产物审计为 **NOT_RUN**。 [构建定义](../../app/build.gradle.kts)

请所有者一次确认正式密钥安排。若已有密钥，提供仓库外安全路径和公开证书 SHA-256，并确认其为今后正式更新使用的证书。若尚无密钥，明确授权创建长期使用的正式 keystore，并确认能在两个受控位置备份、所有正式更新沿用同一密钥。创建或解锁由所有者在安全终端交互完成，凭据留在受限会话。备份后再明确授权一次签署。

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
- **后续桌面门禁**。完整 Release Lint 必须完成，正式证书经所有者确认后才执行授权签名与实物审计。AndroidTest 仅为源码编译，实际运行 **NOT_RUN**。

M20 两项历史系统通知可见性 **FAIL**，旧任务/Owner 收口 **UNKNOWN**。旧 Worker no-op 保留，自动提醒及 RC4 延期至 V1.1。M22 历史离线旅程 FAIL/INCOMPLETE，当前正式包的离线、长历史、系统大字/IME 及兼容验证 **NOT_RUN**。严格 JPEG 三项历史 **FAIL**，`STRICT_JPEG_IMPORT_BLOCKED`、`ANDROID_ISOLATION_RUNTIME_PENDING` 保留，永久照片删除/GC 延期。没有操作设备、ADB、模拟器或真实用户数据。 [完整门禁](LITHIC-V1-Final-Release-Closeout.md)

## 正式候选通过实物审计后的用户验收

1. 保留 Preview19。独立安装正式包，确认包名和空账本。创建一条测试物品和订阅，重开读回，单独打开 Preview19 核对原状态。
2. 在 API36 使用可丢弃照片添加、预览保存、更换、取消封面，核对历史登记媒体仍保留。检查 Picker 取消、不可用和坏图反馈。
3. 只用正式包合成账本导出并检查 backup4，明确确认后覆盖恢复。重开核对物品、订阅、金额、照片及媒体。失败或 UNKNOWN 时保留源文件和现场。
4. 检查首页、物品/订阅详情、成本和历史，三主题切换及重启持久化，提醒保持关闭。遇到无响应或数据异常时停止依赖操作。

正式签署和实际 APK 静态审计完成后才可申请独立候选审查。用户真机验收和独立审查通过前，保持 **V1_RELEASE_NO_GO**。

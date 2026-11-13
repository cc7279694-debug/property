# LITHIC 首页 Android 验收复验 05

**LITHIC_HOME_ANDROID_ACCEPTANCE_REPAIR_05 — ENVIRONMENT_BLOCKED**

固定起点 `c13bd2aee1cfc07a4e8da561b75d374787033f34`，新分支 `codex/lithic-home-android-acceptance-repair-05`。04 基线的两份 App APK 原字节复用。原 Test APK 固定要求旧 AVD，测试源码唯一变化是把名称切换为 `Lithic_Home_Acceptance_Repair05_API36`，继续双重核对实际 AVD 属性和测试参数。生产、既有 UI 修复、测试断言、Room、备份和发行配置均未变更。[上轮报告](LITHIC-Home-Android-Acceptance-Repair-04.md)

## 新建隔离环境与身份

- **PASS**。SDK `avdmanager` 在本次专用目录新建 `Lithic_Home_Acceptance_Repair05_API36`，退出码 0。复用从旧配置实际读取的 `system-images;android-36;default;x86_64` revision 2。配置复读为 720×1648／320 dpi、1536 MiB／2 核。启动前新目录只有 `config.ini`，无 userdata、缓存、SD 卡或快照。冷启动显式指定新目录和 `userdata-qemu.img`，禁用快照存储和首启快照。已安装 SDK 镜像的 `data/empty_data_disk` 标记使模拟器在首次启动生成数据文件。[创建命令](lithic-home-android-acceptance-repair-05/avd-create.txt) · [配置与初始文件](lithic-home-android-acceptance-repair-05/avd-creation.json)
- **PASS**。22 项 Python 单连接离线测试、`compileDebugAndroidTestKotlin` 和 `assembleDebugAndroidTest`。使用原 Debug 证书复验三个 APK 的 v2 签名、唯一签名者、App 包名 `com.cooper.lithic.debug`／code17、`LithicApplication` 和 `MainActivity`、Test 包名 `com.cooper.lithic.debug.test`、唯一 `AndroidJUnitRunner` 与目标 App。证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。[构建](lithic-home-android-acceptance-repair-05/desktop-build-results.txt) · [APK 字节身份](lithic-home-android-acceptance-repair-05/apk-audit.json) · [APK 审计命令](lithic-home-android-acceptance-repair-05/apk-audit-commands.txt)

## 唯一一次启动与预检

**PASS**，单连接准入。启动前主机可用 RAM 4.336 GiB。SDK ADB 37.0.1 在独立 5038 启动，使用独立用户目录、空额外密钥目录，禁用 mDNS 自动连接。唯一 `emulator-17640` 约 33.2 秒为 `offline`、96.4 秒为 `device`，149.5 秒内通过启动完成、Shell、AVD 名称、API 36、x86_64、QEMU 属性、端口归属和唯一传输校验。无手动连接。实际硬件 1536 MiB／2 核，guest `MemTotal` 1,503,188 kB，安装前采样 `MemAvailable` 548,932 kB，`/data` 可用 5,611,764 KiB。[启动参数](lithic-home-android-acceptance-repair-05/launch.txt) · [准入日志](lithic-home-android-acceptance-repair-05/admission-commands.jsonl) · [实际硬件](lithic-home-android-acceptance-repair-05/effective-avd.json)

**PASS**，本次新生成的数据镜像。`hardware-qemu.ini` 指向新 AVD 的 `userdata-qemu.img`，基础镜像与其 QCOW2 层均在本次启动后创建，缓存和加密卷也指向本次新目录。基础镜像创建于 04:38:43.847 UTC，退出后 SHA-256 为 `48d6753085207cbbdf6b1c2bc9416af3fe92ad42f6000ed5cfa49103ed0f9997`。[启动后文件信息](lithic-home-android-acceptance-repair-05/boot-userdata.json) · [退出后两层哈希](lithic-home-android-acceptance-repair-05/userdata-after-shutdown.json)

**BLOCKED**，设备健康检查。SystemUI PID 1045 和 system_server PID 696 前后稳定，Activity 服务可用。限定事件日志在新 AVD 开机后记录一项 `am_anr`，对象是 Android `com.android.documentsui`，原因为 `failed to complete startup`。沿用既有门禁，当前运行观察到 ANR 即在安装前停机。持续性和原因未确认。[系统进程复核](lithic-home-android-acceptance-repair-05/health-admission-pids.txt) · [ANR 原始证据](lithic-home-android-acceptance-repair-05/health-admission-anr.txt) · [控制器结果](lithic-home-android-acceptance-repair-05/controller-result.json)

## Android 未执行范围

User 0 两包记录、安装路径、精确私有目录存在性检查均为 **NOT_RUN**。独立数据镜像的创建及实际路径已验证，guest 包状态无本轮证据。安装、更新、卸载、Instrumentation、截图、字体写入及合成账本写入均为 0。原始字体未变更，无需恢复。夹具和媒体清理 **NOT_RUN**，本轮未创建夹具。04 代码在 Android 上的修复效果仍待验。

| Android 执行组 | 计划 | 实际 | 状态 |
| --- | ---: | ---: | --- |
| 基线 200% 诊断，单独计数 | 1 | 0 | NOT_RUN |
| `HomeAndroidAcceptanceTest` | 3 | 0 | NOT_RUN |
| `HomeOverviewUiTest` | 6 | 0 | NOT_RUN |
| `P2HomeArchiveContractTest` | 6 | 0 | NOT_RUN |
| `ReportRepositoryTest` | 19 | 0 | NOT_RUN |
| `CoverPreviewsTest` | 7 | 0 | NOT_RUN |
| `ActivityNavigationTest` | 2 | 0 | NOT_RUN |
| 修复版合计 | 43 | 0 | NOT_RUN |

空账本、三主题、两张 200% 卡片截图和修复前布局测量均 **NOT_RUN**。03 的原始截图和失败记录保留。

## 安全退出

**PASS**。核验本次控制台身份后正常关闭 AVD，wrapper PID 33396 退出码 0，QEMU 及 17640／17641 无残留。独立 SDK 5038 PID 30860 在核验归属后退出，原 5037 监听、PID 16276 与创建时间前后一致。旧 AVD 未启动、清理或覆盖，旧配置 SHA 和顶层文件大小及修改时间复核一致。未启动第二次、未切换端口、未访问手机或用户数据。未脱敏日志、APK 与新 AVD 镜像仅保存在本机。[退出回执](lithic-home-android-acceptance-repair-05/emulator-cleanup.json) · [ADB 生命周期](lithic-home-android-acceptance-repair-05/receipt.json) · [旧 AVD 复核](lithic-home-android-acceptance-repair-05/old-avd-after.json)

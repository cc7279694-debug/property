# LITHIC_HOME_ANDROID_ACCEPTANCE_02

起点 `codex/lithic-home-android-adb-isolation-03` 本地与远端均为 `d4db53e5edd424a03a8aea3ce0885d638420c5f8`，工作区干净。本轮在 `codex/lithic-home-android-acceptance-02` 执行。生产源码、Room Schema、备份协议、金额算法、正式身份及签名配置按固定基线核对。[已批准的参考图](lithic-home-card-ui/approved-reference.png)

## 指定 AVD 与主机条件

- 指定 AVD 是验收 01 的 `Lithic_Home_Acceptance_01_API36`。其登记目录和配置核对为 API 36 AOSP x86_64、720×1648、320 dpi，配置 `hw.ramSize=1536`，拟以 `-cores 2` 启动。上轮没有安装 APK、运行 Instrumentation 或生成截图。上轮生成的镜像和锁原样保留，锁记录的进程已退出。历史 `Lithic_P2A1_API36` 不在本轮范围。
- 上轮同一 AVD、同一 `-memory 1536` 选项的原始 Emulator 日志记录 `Increasing RAM size to 2560MB`，其生成的 `hardware-qemu.ini` 同样为 2560。主机初始可用约 2.4–2.6 GiB。启动前要按已知有效的 2.5 GiB guest 分配复核资源，另留约 2 GiB 给 Windows 与模拟器开销。两组内存数分别记录，避免把启动请求当作实际配置。
- 用户随后明确要求实际内存严格为 1536 MiB，并指示暂停启动。本轮 AVD 启动次数为 0，未执行 `admit_owned_emulator()`，未改变 AVD 配置或旧锁。[AVD 与主机回执](lithic-home-android-acceptance-02/avd-preflight.txt)

## 独立 ADB 与测试 APK

| 检查 | 状态 | 实际结果 |
| --- | --- | --- |
| 独立 SDK ADB | PASS | `adb -P 5038 --one-device <本轮无匹配标记> start-server` 启动 SDK **37.0.1-15733141**，PID **34300**。CIM 与服务自报程序路径均为 `E:/CODEX/MirraAndroid/sdk/platform-tools/adb.exe`。使用独立 ADB 目录、空的额外密钥目录，关闭 mDNS 自动连接及其他设备自动扫描。`adb -P 5038 devices -l` 和主机查询确认列表为空。 |
| 独立服务收口 | PASS | 核对 5038 所有权后 `adb -P 5038 kill-server` 退出 0，监听与 PID 34300 消失。5037 的 PID **13756**、监听、创建时间与身份前后一致，仅执行只读身份查询。 |
| 首轮桌面构建 | PASS | `:app:assembleDebug :app:assembleDebugAndroidTest` 退出 0，耗时 1 分 6 秒。 |
| 首轮 APK 身份审计 | FAIL | 构建进程继承 ADB 的 `ANDROID_USER_HOME`，在独立目录生成了另一个 Debug keystore。两包签名均有效且彼此相同，证书摘要 `c28e93871286ade0154a83487c791bdc60d0b9b66830ecd045594952f038bd9d` 与原 Debug 证书不同，审计拒绝，未安装。 |
| 桌面修正及复验 | PASS | 仅在构建子进程去掉 ADB 目录和地址覆盖，复用现有 Debug 签名。两构建任务退出 0，耗时 34 秒。App、Test 的 Manifest、目标包、原 Debug 签名和 SHA-256 核对通过。未修改 Gradle 或签名配置。 |

最终两包证书 SHA-256 均为 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。Runner 为 `androidx.test.runner.AndroidJUnitRunner`，目标为 `com.cooper.lithic.debug`。

| 包 | 字节 | APK SHA-256 |
| --- | ---: | --- |
| `com.cooper.lithic.debug` | 20,541,670 | `6706499e31ef9dbff1ef060dc6c604cfb7b0fb5ecbaa8082dd6179773c0853d9` |
| `com.cooper.lithic.debug.test` | 4,034,867 | `7c5a7bf5b4ae232666379c8317901bd4abb913978fe62fe52282f28dbbffb254` |

[ADB 实际命令与生命周期](lithic-home-android-acceptance-02/adb-lifecycle.json) · [首轮证书失败回执](lithic-home-android-acceptance-02/first-apk-signatures.json) · [桌面构建结果](lithic-home-android-acceptance-02/desktop-build-results.txt) · [最终 APK 审计](lithic-home-android-acceptance-02/apk-audit.json)

## 运行、截图与交互

| 验收范围 | 状态 |
| --- | --- |
| 1536 MiB 实际配置与设备准入 | BLOCKED。旧日志与生成的硬件配置显示 2560，用户要求严格 1536 并暂停启动。 |
| 实际 guest 身份、SystemUI、存储、可用内存及安装 | NOT_RUN |
| 空账本、合成金额、项目和订阅首页、三主题、封面及 Crop | NOT_RUN |
| `home_routes_open_and_back()` 的分段、详情、两个新增入口、成本构成、底栏和返回 | NOT_RUN |
| 360dp、1.0／2.0 系统字号、Activity 实际字号、字体恢复 | NOT_RUN。未改设备设置，无需恢复。 |
| `HomeOverviewUiTest`、`P2HomeArchiveContractTest`、`ReportRepositoryTest`、`CoverPreviewsTest`、`ActivityNavigationTest` | NOT_RUN |
| 夹具写入、合成数据清理、截图比对 | NOT_RUN |

App/Test 安装 **0**，Instrumentation 执行 **0**，原生截图 **0**。因此没有实际参考图差异或产品缺陷结论。后续仍须先证明指定配置能实际使用 1536 MiB，再按独立 5038 路线完成一次设备验收。释放主机内存只解决资源容量，当前没有证据证明它会改变 Emulator 的自动调整。

本机完整构建、APK 审计及服务日志保存在 `E:/CODEX/lithic-home-android-acceptance-02-local/`。该目录中的本次运行控制器草稿没有执行，不属于已验证产物。仅提交报告及必要证据，未操作个人设备、真实账本、`main` 或正式发行。

**LITHIC_HOME_ANDROID_ACCEPTANCE_02 — ENVIRONMENT_BLOCKED**。等待 ChatGPT 独立审查。

## 2026-11-13 低内存模式单次复验

用户明确授权 `-lowram` 单次启动，保留实际 1536 MiB 及设备健康的安装门槛。主机复测可用 5.643 GiB，实际启动前 4.903 GiB，C/E 盘可用约 17.75／29.03 GiB。复用已通过原 Debug 证书核验的两个 APK，重新校验其 SHA-256，未重新构建。

- **硬件配置 PASS，guest 内存和健康 NOT_RUN。** 本机 Emulator 帮助列出 `-lowram`。AOSP 源码显示该选项可撤销最低 RAM 限制。本次以原 `-ports 17640,17641`、`-lowram -memory 1536 -cores 2` 启动一次。新生成的 `hardware-qemu.ini` 显示 `hw.ramSize = 1536`、`hw.cpu.ncore = 2`。ADB 未准入，未取得 guest `/proc/meminfo` 或 SystemUI 证据。[AOSP Emulator 源码](https://android.googlesource.com/platform/external/qemu/+/emu-master-dev/android/android-emu/android/main-common.c#1295)
- **ADB 准入 BLOCKED。** 仅使用 SDK 37.0.1 的独立 5038，PID 22412。核对 17641 属于本次 QEMU 后，发出一次定向回环连接，6 秒超时后继续只读观察。约第 22 秒同时出现 `127.0.0.1:17641` 和 `emulator-17640`，均为 `offline`。现有歧义守卫立即停止，未走满 180 秒，未选中 serial，未发出设备 Shell。现有证据不足以确认两个 transport 的对应关系或离线根因。[准入日志](lithic-home-android-acceptance-02/lowram/admission-commands.jsonl)
- **收口 PASS。** 仅关闭本次拥有的模拟器 wrapper 和已核对的 QEMU 子进程，17640／17641 无监听。核对并关闭本次 5038 服务，PID 22412 及监听均消失。原 5037 在本次尝试前后均为 PID 16276，服务身份一致。该 PID 与昨日记录的 PID 属于不同观察时段。[收口及实际配置回执](lithic-home-android-acceptance-02/lowram/runtime-receipt.json)

本次 App/Test 安装 **0**、Instrumentation **0**、截图 **0**。字体、合成数据、首页视觉与交互仍为 `NOT_RUN`。未重试启动、换端口、修改生产或验收源码。若后续另获授权，需先明确如何在保持目标唯一性校验的前提下处理自动注册与手动回环注册的并存。

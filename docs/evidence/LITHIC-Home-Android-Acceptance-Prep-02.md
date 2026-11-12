# LITHIC_HOME_ANDROID_ACCEPTANCE_PREP_02

基线 `0df0336e64b79d4f786df6eecdd4205fb3a51035` 的本地与远端一致，开始时工作区干净。本轮分支为 `codex/lithic-home-android-acceptance-prep-02`。生产基线 `35149b58386e859103b22be5bdadc86d3a2322e3` 的 `app/src/main/`、Room Schema、金额算法、备份协议和 Gradle 正式发行配置均通过差异核对。[前次验收](LITHIC-Home-Android-Acceptance-01.md)

测试及脚本固定提交 `07621265304253f40b3c46970326af45661e6240`，生产树 `b023080e96f118d263bae7f5176426bfa0e8d97f`。

## 主机诊断

| 检查 | 本轮只读观察 |
| --- | --- |
| SDK ADB 客户端 | `E:\CODEX\MirraAndroid\sdk\platform-tools\adb.exe`，37.0.1，build 15733141，ADB 1.0.41。`where adb.exe` 未找到 PATH 上的客户端，退出 1。 |
| 已有 ADB 服务 | `127.0.0.1:5037`，PID 13756。`host:version` 返回 `0029`，即协议 41。`host:server-status` 报告 36.0.1、build 13811061 和 `E:\向日葵\Oppo Connect\daemon\bin\adb.exe`，文件存在。Windows CIM 未返回进程映像路径，路径证据来自正在响应的服务。 |
| 设备与端口 | `host:devices-l` 返回空列表。未观察到模拟器进程或 17640／17641 监听。IPv4 与 IPv6 的 5458–5557、5558–5657 排除段仍覆盖标准 5554–5585。 |
| Emulator 与资源 | 37.1.11.0（15917651）。本机 `-help-ports` 明确支持指定控制台和 ADB 端口。采样时可用 RAM 4.97 GiB，C/E 盘可用约 16.79／29.20 GiB。资源须在未来启动前复测。 |

运行 `python -B scripts/home_android_host_diagnostics.py --sdk E:/CODEX/MirraAndroid/sdk` 生成命令、起止时间、耗时、退出码、原始标准输出与错误。三条 ADB 主机查询走既有回环服务，每条 3 秒，无进程退出码。服务返回值中的认证路径和可能的真实设备序列均未保存。[主机诊断原始回执](lithic-home-android-acceptance-prep-02/host-diagnostics.json)

前次 Windows 默认端口绑定失败已有 `WinError 10013` 与端口排除记录。随后使用专用端口时，本次 QEMU 的回环监听已确认，但 `connect` 等待 6 秒后脚本重抛 `TimeoutExpired` 并提前退出。已有服务目前能回应主机查询，服务与 SDK 客户端来自两份不同构建。版本差异、端口预留和客户端提前退出均可证实，17641 注册超时的底层原因仍为 `UNKNOWN`。

## 准入脚本与独立夹具

- 原始本机 `E:/CODEX/lithic-home-android-acceptance-01-local/run.py` 仍为 17,811 字节，SHA-256 `6519952105b1460761038862b9f666d7aa7a70dd7e00fd2f0bbe4df6ac12c99c`。历史原件和上轮 Git 证据原样保留。后续启动器调用新受版本控制的准入模块，替代原脚本中的 `admit()`。[准入实现](../../scripts/home_android_admission.py)
- 窗口从单次启动前的 `monotonic()` 起算，总长 180 秒。每条命令另设上限并受剩余时间约束。使用既有 5037 smart socket，已核对归属的回环端点最多发一次 `host:connect`。超时记录后继续只读观察。进程已退出、身份或端口归属冲突时立即阻断。不存在 ADB CLI 子进程，归属查询的 PowerShell 子进程有界退出。成功需要精确目标 `device`、`sys.boot_completed=1`、Shell 探针及实际 AVD／QEMU／API／ABI 匹配。报告保留 `admission-claim.json`、`admission-commands.jsonl`、`admission-result.json`。
- `HomeAndroidAcceptanceTest` 三个方法各自生成独立 UUID，在 `@Before` 核对空账本，在各方法内种入自己的数据，在 `@After` 对本方法的夹具执行受限清理。规则在启动 `MainActivity` 前核对 opt-in、AVD、Debug App、Test/Runner、API 与 ABI。夹具保存本次实体、媒体 inode、大小与 SHA。当前 Owner、各表行、实体内容、媒体目录和文件身份均符合本方法收据时，才按精确 ID 在 coordinator/Room 边界内删除自己的行和已知 PNG。发现未知或上轮异常残留即停止，保留现场。清理证据及截图的 method UUID 仍可追溯。

## 下次运行手册（需另获执行授权）

1. 运行上方只读主机诊断。确认 SDK、已有 ADB 服务、空传输列表、可用资源以及目标端口可绑定。获准后从本轮提交构建 Debug 和 AndroidTest，核对实物包名、签名、Runner 和 SHA，使用单独证据目录。上轮 Test APK 缺少本轮夹具修正。隔离 AVD 必须经确认可丢弃，不能用个人设备、含未知数据的旧 AVD 或之前失败的 P2A1 环境。既有服务无响应时停止，保留证据。
2. 独立 AVD 使用 API 36 AOSP x86_64，指定 `Lithic_Home_Acceptance_01_API36`、720×1648、320 dpi，以及经本机核对可用的一对 `-ports`。只启动一次。调用新版准入模块时传入本次 `Popen` 和启动前的单调时钟值。只有 `ADMITTED` 才继续检查 SystemUI 稳定性、有界 ANR 记录、空间及设备身份。Guest 日志的开机完成不作为 ADB 准入依据。
3. 向唯一已准入的 serial 定向确认两个目标包在 User 0 尚不存在。各装一次 `com.cooper.lithic.debug` 与 `.debug.test`，核对 `androidx.test.runner.AndroidJUnitRunner` 的 targetPackage。跳过任何个人或历史包。遇未知状态时停止依赖步骤并保留原始日志。
4. 读取原始系统 `font_scale` 并保存，区分 `null` 和显式值。只对已准入的隔离 serial 执行 `adb -s <serial> shell settings put system font_scale 1.0`，用 `settings get system font_scale` 读回。在实际 Activity 报告 360dp 与 1.0 后，分别运行 gallery、routes。gallery 先查空账本，再种入数据，采集项目和订阅的三主题。routes 独立种入数据，验证列表、详情、两处新增、成本构成、底栏和返回。
5. 正常字号组结束后，定向 `settings put system font_scale 2.0` 并读回。单独运行 `large_font_uses_android_configuration`。核对 Settings 和实际 `resources.configuration.fontScale=2.0`，记录金额、名称、状态、封面卡片及底部添加的真实显示。无论两组成功或失败，均在 `finally` 还原原值并读回。原始值为 `null` 时，定向 `settings delete system font_scale`。目标丢失时记录恢复 `UNKNOWN`，不得切换到其他设备。
6. 每个原生方法单独执行 `am instrument -w -r -e class com.cooper.lithic.ui.HomeAndroidAcceptanceTest#<method>`，加 `-e lithicHomeAcceptance fresh-api36-emulator -e lithicHomeAcceptanceAvd Lithic_Home_Acceptance_01_API36 -e homeAcceptanceRunId <attempt-UUID>`，最后指定 `com.cooper.lithic.debug.test/androidx.test.runner.AndroidJUnitRunner`。执行顺序 `projects_subscriptions_and_theme_gallery`、`home_routes_open_and_back`、`large_font_uses_android_configuration`，各自等待上限建议 150、120、90 秒。分组仅因系统字体要求，每个方法都能独立创建自己的数据。按需分别运行 `HomeOverviewUiTest`、`P2HomeArchiveContractTest`、`ReportRepositoryTest`、`CoverPreviewsTest`、`ActivityNavigationTest`，保留原断言和实际 JUnit 状态。
7. 用 Instrumentation 的 `homeAcceptanceAttemptId`、`homeAcceptanceRunId`、`homeAcceptanceMethod`、`homeAcceptancePng` 和配置字段定位证据。仅 `exec-out run-as com.cooper.lithic.debug cat` 提取对应 `files/home-android-acceptance/<attempt-UUID>/<method-UUID>/<name>.png`，记录 PNG 尺寸、SHA、系统字体值及 Activity 字体值。只接受真正 `MainActivity` 整屏图。完成后退出本次拥有的模拟器，复核进程、端口、原 ADB 服务、日志和未运行项。不得在同一轮换端口重启。

启动器接线见模块开头的调用示例。将本仓库 `scripts/` 加入导入路径，在本次隔离 AVD 的 `Popen` 前记录 `launched_at = time.monotonic()`，随后调用 `admit_owned_emulator(proc, launched_at, avd, console_port, transport_port, evidence_dir)`。非 `ADMITTED` 时直接进入本次进程收口，跳过安装和 guest 测试。原上轮 `run.py` 只作为历史证据，未接入修正，不用于下一轮启动。

## 验证与待验范围

- 离线 Python 测试 13 项通过。覆盖单次连接超时后成功准入、后续只读超时、截止 180 秒、服务无响应、未知或多重目标、错误身份、监听归属与实际 smart socket 协议封包。测试使用替身，真实 Windows ADB 设备协议待验。[单测输出](lithic-home-android-acceptance-prep-02/admission-unit-tests.txt)
- 离线 Gradle `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` 退出 0，2 分 23 秒。生产 Kotlin 为 `UP-TO-DATE`，AndroidTest Kotlin 本轮执行通过。`git diff --check` 和生产冻结边界检查通过。[编译结果](lithic-home-android-acceptance-prep-02/compile-result.txt)

本轮仅做主机诊断、离线脚本测试及 Kotlin 源码编译。Android、AVD 准入、Instrumentation、系统字体写入及截图全部 `NOT_RUN`。未创建或启动 AVD，未执行 `adb connect`／`disconnect` 或重启服务，未安装 APK，也没有访问个人账本。实际 ADB 端侧连通、独立夹具清理、200% 字体显示与首页视觉均须等待下一次设备验收。

**READY_FOR_NEXT_ANDROID_ATTEMPT**。等待 ChatGPT 独立审查。

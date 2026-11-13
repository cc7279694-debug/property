# LITHIC_DOCUMENTSUI_ANR_DIAGNOSTIC_06

**结论 D：已确认 Android 进程初始化超时，根因 UNKNOWN。** Repair05 的首页验收由现有健康门禁在安装前阻断。现有证据不足以归因于电脑资源、API 36 镜像缺陷或某项系统依赖，也不足以认定门禁过严。首页大字体修复、43 项 Android 回归和两张 200% 截图仍为 **NOT_RUN**。基线 `80dfc2cda196d9cabd22334d4e5c5bd9917625fa`。[Repair05 报告](LITHIC-Home-Android-Acceptance-Repair-05.md)

## A. 原始事件和时间线

```text
11-13 04:41:22.080   696  1998 I am_anr  : [0,1769,com.android.documentsui,818527813,Process ProcessRecord{143968d 1769:com.android.documentsui/u0a78} failed to complete startup]
```

- **CONFIRMED**：04:38:33.955 UTC 启动全新 AVD。主机控制器约在 04:41:02.928 UTC 收到 `sys.boot_completed=1`，随后 Shell、QEMU、API、ABI 与唯一传输复核成功。以保存的主机及 guest 时间戳对照，DocumentsUI 事件约晚 19 秒。日志中的 696 是写出事件的 `system_server` PID，1769 是事件涉及的 DocumentsUI PID。AVD 随后正常退出。[启动参数](lithic-home-android-acceptance-repair-05/launch.txt) · [准入时序](lithic-home-android-acceptance-repair-05/admission-commands.jsonl) · [原始 ANR](lithic-home-android-acceptance-repair-05/health-admission-anr.txt)
- **CONFIRMED，超时类型**：本机实际使用的默认镜像中，`/system/framework/services.jar` 的 DEX 显示，`ActivityManagerService.handleBindApplicationTimeoutHard` 生成该日志文案，随后调用 `TimeoutRecord.forAppStart` 和 `AnrHelper.appNotResponding`。AOSP 参考源码显示，ActivityManager 发出应用绑定后安排超时，收到匹配的启动完成回报后撤销。应用在回报前可能初始化 Provider、调用 `Application.onCreate()` 或等待系统调用。事件证明 Android 未及时收到初始化完成信号，触发启动的组件、具体卡点及后续恢复均未知。SDK 构建与参考源码标签的逐字节对应未核验。[ActivityManagerService](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r2/services/core/java/com/android/server/am/ActivityManagerService.java#5022) · [ActivityThread](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r2/core/java/android/app/ActivityThread.java#7806)
- **UNKNOWN，具体原因和持续性**：对本地启动、控制器、准入和健康日志按目标包、进程启动／死亡／重启、ActivityManager／PackageManager、Provider、权限、磁盘错误、OOM 和 lmkd 关键词检查。目标包只在该条 ANR 事件中出现。保留的 guest 事件查询为 `logcat -b events -d -t 150 -s am_anr`，没有完整的 ±60 秒 guest 日志。本次目录无已保存的 guest main/system/crash logcat、ANR 线程栈、DropBox 或 bugreport。启动日志提到的主机 Crashpad 目录在该时间窗无文件修改记录。没有证据判定重复启动、崩溃、Provider 初始化失败、权限或 I/O 错误，也无法排除先发生的依赖服务异常。[主机启动日志](lithic-home-android-acceptance-repair-05/emulator.stdout.redacted.txt) · [控制器结果](lithic-home-android-acceptance-repair-05/controller-result.json)

## B. 主机、内存与磁盘

| 已保存样本 | 观察 |
| --- | --- |
| 启动前主机 | 可用内存 4.336 GiB，C 盘约 17.40 GiB、E 盘约 28.84 GiB 可用 |
| 实际 QEMU | 1536 MiB／2 核，WHPX，SwiftShader |
| guest 两次采样 | `MemTotal=1503188 kB`，`MemAvailable=621464`、`548932 kB` |
| `/data` | 5,611,764 KiB 可用，使用率 6% |
| 健康采样和退出 | SystemUI 1045、system_server 696 两次一致，Activity 服务可用，模拟器正常退出，wrapper 退出码 0 |

**UNKNOWN**：已保存记录中没有明确 OOM、lmkd 杀进程或 system_server 内存异常。可用内存是两次采样，未保存内存回收、交换区、CPU 等待、I/O 等待及主机资源趋势。仅凭 1536 MiB 的配置和这些读数无法证明资源是原因，也无法排除初始化期间的短时压力。03 和 04 使用相同镜像路径及 1536 MiB／2 核，健康时可用内存更低，分别为 467,156 和 480,732 kB，限定查询均未见 ANR，03 还实际运行了 43 项测试。这削弱了固定内存值或镜像路径本身必然触发问题的推断。各次 userdata 和首启状态不同，历史系统镜像字节也未独立对照。[Repair05 启动前资源](lithic-home-android-acceptance-repair-05/prelaunch.json) · [早期内存](lithic-home-android-acceptance-repair-05/effective-guest-memory.txt) · [健康采样](lithic-home-android-acceptance-repair-05/guest-health.txt) · [03 对照](LITHIC-Home-Android-Acceptance-03.md) · [04 对照](LITHIC-Home-Android-Acceptance-Repair-04.md)

## C. SDK 镜像与系统服务依赖

从本机已安装 `system.img` 以只读方式解析 GPT、动态分区和 ext4，从 `system` 逻辑分区将两个 APK 导出到独立本机临时目录。对副本运行 SDK Build Tools 36.0.0 的 `aapt2 dump badging` 和 `aapt2 dump xmltree <apk> --file AndroidManifest.xml`，均退出 0。APK ZIP CRC 校验通过，两者均包含 DEX，均无 `lib/` 打包内容。

| 已安装镜像 | 镜像内组件 | Manifest 与 APK SHA-256 |
| --- | --- | --- |
| `system-images;android-36;default;x86_64` rev2，API 36 ext17，`BE2A.250530.026.D1`／13818094，Repair05 实际使用 | `/system/priv-app/DocumentsUI/DocumentsUI.apk`，6,667,707 字节 | `com.android.documentsui`，versionName 16、code36，min30／target36／compile36。`e06d2934feeeaf65c5b1609d687df8bb4ca78990263729c9fd9aa5c05139e6ff` |
| `system-images;android-36;google_apis;x86_64` rev7，API 36 ext17，`BE2A.250530.026.F3`／13894323 | `/system/priv-app/DocumentsUIGoogle/DocumentsUIGoogle.apk`，7,619,800 字节 | `com.google.android.documentsui`，versionName 16、code360527520，min30／target36／compile36。`5402f120ceb0f3e44054b1f45232719adae54c249c9238ea281aa2c14e3a8784` |

**CONFIRMED，静态接线**：默认镜像含 `DocumentsApplication`、导出的 `picker.PickActivity` 和文件界面入口。`PickActivity` 声明 `OPEN_DOCUMENT`、`CREATE_DOCUMENT`、`GET_CONTENT`、`OPEN_DOCUMENT_TREE`。Provider 包括 `LastAccessedProvider`、`PickCountRecordProvider`、需 `MANAGE_DOCUMENTS` 的 `ArchivesProvider`、以及声明 `ProcessLifecycleInitializer` 的 AndroidX `InitializationProvider`。另有 `PackageReceiver`、`PreBootReceiver`、Manifest 默认禁用的 `BootReceiver` 和独立进程的 `FileOperationService`。Google APIs 包对应组件声明可见。两份 Manifest 均未声明 `uses-library`、`uses-native-library`、`uses-static-library`、`uses-sdk-library` 或 `uses-feature`，未见显式库或 SDK 版本缺项。

**UNKNOWN，运行依赖与镜像故障**：默认镜像内有 DocumentsUI 特权权限配置。对实际 APK 的 DEX 核对发现，`DocumentsApplication.onCreate()` 涉及 ActivityManager、OverlayManager 和用户状态，并调用 `ProvidersCache.updateAsync()`。`PreBootReceiver` 有 PackageManager 和 OverlayManager 调用。没有进程启动来源、线程栈、Binder 等待方或同一启动窗口的服务日志，无法确认哪个初始化路径已执行或停在哪一步。首启较慢、主机或 guest 调度压力、镜像缺陷、服务依赖异常仍未区分。静态检查没有发现足以建议换镜像的缺陷。

## D. 健康门禁与产品影响

本机 Repair05 `run.py:290–318,623` 首先核对 SystemUI 和 system_server 的 PID 前后一致、`service check activity` 返回 `Service activity: found`，然后匹配查询结果中的 `\bam_anr\b`。匹配任一包即阻断，没有次数、包名或恢复观察过滤。本次 PID 与服务检查通过，**阻断原因是匹配到真实的 DocumentsUI 事件**。这是明确的保守验收策略。一次事件不足以判定系统持续不健康，当前证据也不足以支持放宽策略。[进程回执](lithic-home-android-acceptance-repair-05/health-admission-pids.txt) · [Activity 回执](lithic-home-android-acceptance-repair-05/health-admission-activity.txt) · [ANR 回执](lithic-home-android-acceptance-repair-05/health-admission-anr.txt)

| 范围 | 实际接线和风险 |
| --- | --- |
| 首页基础视觉 | `LithicNavigation.kt:466` 接入 `HomeShell`，`HomeShell.kt:445` 的封面走本地 CoverReader。`HomeAndroidAcceptanceTest.kt:85,127,174` 和 `HomeAndroidAcceptanceFixture.kt:98–100,133–134` 使用合成记录和本地 PNG，覆盖三主题、导航、200% 字体，无文件选择器调用。未见直接依赖 DocumentsUI。系统异常可能影响测试可靠性，修复版仍 NOT_RUN。 |
| 照片新增／替换 | `PhotoLiteCoverPanel.kt:44,59` 使用 `PickVisualMedia(ImageOnly)`。本地 AndroidX Activity 1.12.4 在 API 36 选择系统 `ACTION_PICK_IMAGES`，通用的 `ACTION_OPEN_DOCUMENT` 是缺少系统及系统回退选择器时的另一分支。默认镜像的 MediaProvider APEX 内另有 `com.android.photopicker`，Manifest 声明处理 `ACTION_PICK_IMAGES`，APK SHA-256 `ad5ce4fa6ec09ecc5de3a8099d0432d27ff09d38e7a67502b52623c761d7980f`。这条路线没有直接依赖 DocumentsUI 的证据。实际 Intent 解析、打开选择器及返回 URI 后的读取均待验。普通验收 Debug 的 Photo-Lite 标志关闭。[AndroidX Photo Picker 合同](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.PickVisualMedia) |
| 备份文件选择 | `BackupDocumentPicker.kt:14` 使用 SAF `CreateDocument`，`BackupRestoreDocumentPicker.kt:13`、`BackupInspectionDocumentPicker.kt:21` 使用 `OpenDocument`。对应实际镜像的 DocumentsUI `PickActivity`。文件选择因此有直接风险，是否可正常打开、读写仍 NOT_RUN。[Android SAF 合同](https://developer.android.com/guide/topics/providers/document-provider) |
| 整体稳定性 | 启动完成、两次 PID 样本及单项 ANR 证明的是这段时间的具体观察。进程是否自行恢复、是否复发、其他服务是否受影响均 UNKNOWN。 |

首页、本地封面、原生照片选择器及 SAF 可以分开验收。沿用系统准入门禁，分别登记各链路的实际结果。首页通过也不覆盖文件选择器。

## E. 下一次最小补证方案

**建议 D，若值得再投入一次，先准备可取证的诊断启动。** 保留 Repair05 的 AVD 和数据镜像，旧 AVD 保持原状。若只检验该环境后续冷启动是否复发，另行获准后复用现有 Repair05 的独立 AVD 即可，并复核其路径和身份。若后续需要控制变量重现真正的第一次启动，再单独授权新建独立数据目录。一次不复发只能说明该次观察，不能验证首次启动假设。现阶段没有足够证据需要换镜像、增加 guest RAM 或改变产品。

1. 启动前写好单次执行和取证顺序，预设 180 秒身份准入和准入后 60 秒有界观察。只使用专属 5038、已确认的唯一 AVD，维持原 RAM／核数／镜像，保留启动前后的主机 QEMU CPU、内存和磁盘样本。避免在同次试验同时改多个变量。
2. 身份通过后立即保存从该次开机保留的限量 `events`、`main`、`system`、`crash` 日志，记录时间基准，覆盖事件前后各 60 秒或注明实际覆盖。关联 `am_proc_start`、进程绑定及死亡、`am_anr`、ActivityManager、Provider、权限、存储和 lmkd。并行少量采样 DocumentsUI／SystemUI／system_server PID、guest `/proc/meminfo`、`/proc/vmstat`、可用的 CPU／内存／I/O 压力数据。
3. 若出现新的 ANR，保留事件并阻断安装。只在已确认的测试 guest 上尝试保存同一 PID、包名和时间对应的 ANR 线程栈，受限时记录缺口并收口。唯一性、启动完成、关键进程、日志取证或设备健康无法确认时停止依赖步骤。若观察窗无 ANR 且原门禁和包／目录预检通过，按下一次授权范围决定是否同次进入首页验收。实际 SAF 和照片选择器验证另记，避免为同一诊断重复启动。[Android ANR 诊断](https://developer.android.com/topic/performance/issues/anr) · [AOSP lmkd 与内存压力](https://source.android.com/docs/core/perf/lmkd)

## 已读证据索引

- 本机 `E:/CODEX/lithic-home-android-acceptance-repair-05-local/` 内的 `launch.txt`、`prelaunch.json`、`admission-claim.json`、`admission-commands.jsonl`、`admission-result.json`、`emulator.stdout.txt`、`emulator.stderr.txt`（0 字节）、`runtime.stdout.txt`、`controller-result.json`、`run.py`、`effective-avd.json`、`effective-guest-memory.txt`、`guest-health.txt`、`health-boot.txt`、`health-initial-pids.txt`、`health-admission-pids.txt`、`health-admission-activity.txt`、`health-admission-anr.txt`、`emulator-cleanup.json`、`receipt.json`、`avd-creation.json`、`boot-userdata.json`、`userdata-after-shutdown.json`。复查 AVD `config.ini`、`hardware-qemu.ini` 和 userdata 路径、文件元信息。日志原件保留，公开证据沿用 05；五份关键文本与本机原件 SHA-256 一致。`health-admission-anr.txt` SHA-256 `7e2e6637ec75aa6a9d7b444ede9591c8a61868f4ade289ddc1a8e3696ef9086c`。
- SDK 两个 API 36 镜像的 `source.properties`、`build.prop`、`system.img`、实际 APK Manifest，及默认镜像的 `/system/framework/services.jar`、DocumentsUI 权限配置和 APK DEX。`services.jar` SHA-256 `d82d6412c5843131f6992f53021dbcfbd368695bf5fd440edfb2c8a2046a0b5a`，其 `classes.dex` SHA-256 `08af436e41cfa3fe350e53b2ad1f8f9d8368431f3dc48ee08e6f1bf55441a0a9`。只读提取脚本 `E:/CODEX/lithic-documentsui-image06.py`，本机副本、Manifest 摘录与 SDK `dexdump -d -n` 输出在 `E:/CODEX/lithic-documentsui-image06-local/`，其中 `services-startup-timeout-evidence.txt` 保留对应方法和三处调用。
- 仓库的 MainActivity、首页导航及合成验收、封面读取、Photo-Lite、SAF 三个入口和 Gradle 功能开关。仅提交诊断文档。本轮设备、模拟器、ADB、产品源码与 APK 均无操作。

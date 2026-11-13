# LITHIC_DOCUMENTSUI_DIAGNOSTIC_RUN_07

执行起点 `0f4aec43f5431f28d761452c1dbdaec88ee772ee`，生产 UI 仍基于 `c13bd2aee1cfc07a4e8da561b75d374787033f34`。这是 `Lithic_Home_Acceptance_Repair05_API36` 在仓库证据中的第二次启动，也是本轮唯一一次。复用 Repair05 数据目录、userdata 基础镜像与 QCOW2 层。启动前两层 SHA-256 与 05 关机回执一致，配置 SHA-256 为 `e7d7b4e42fe1e2069f439dcb2f15a5b95e03d4525bcd8ef7a3129d2dc234fe75`。仍用 API 36 AOSP x86_64 默认镜像 rev2，1536 MiB／2 核，720×1648／320 dpi。启动命令显式指向原数据路径并禁用快照加载与快照存储。启动前主机可用 5.116 GiB，C/E 盘分别可用约 17.42／26.22 GiB。[启动前身份与数据指纹](lithic-documentsui-diagnostic-run-07/prelaunch.json) · [启动命令](lithic-documentsui-diagnostic-run-07/launch.txt) · [上轮结论](LITHIC-DocumentsUI-ANR-Diagnostic-06.md)

### 采集准备

启动前准备日志文件和采样进程。独立 SDK 5038 验证为 37.0.1，初始设备列表为空。准入模块保留 180 秒窗口和现有身份检查，增加一次回调，仅在指定传输为唯一 `device`、监听属于本次 QEMU 且进程仍存活后触发。采集侧复核 guest 属性再启动只读流。离线 23 项通过，新增测试覆盖回调在 boot 完成前且只触发一次、拒绝外部监听。App/Test 实物与 Repair05 一致，APK v2、原 Debug 证书、包名、MainActivity、唯一 Runner 和目标通过核验。本轮计划只安装修复版与 Test，不重复安装诊断基线。[离线结果](lithic-documentsui-diagnostic-run-07/offline-tests.txt) · [APK 身份](lithic-documentsui-diagnostic-run-07/apk-audit.json)

## 冷启动与健康门禁

**结论：LITHIC_DOCUMENTSUI_DIAGNOSTIC_RUN_07 — ENVIRONMENT_BLOCKED。** 本轮捕获范围内未观察到新 ANR。安装前的综合门禁由采集器换行误判触发，首页 Android 验收未开始。

| UTC | 现场证据 |
| --- | --- |
| 08:05:21.345 | 启动唯一一次 Repair05 冷启动，wrapper PID 32784，QEMU PID 25396 |
| 08:05:57.526 | 5038 首次确认唯一自动注册的 `emulator-17640 device`，监听属于本次 QEMU |
| 08:05:57.591–58.008 | 早期属性复核完成，启动 all、events、main+system、crash 四路采集，首个缓冲区快照 439,858 字节 |
| 08:06:35.695–36.002 | 收到 `sys.boot_completed=1`，Shell 与 API 36、x86_64、AVD 名称验证通过，距启动约 74.6 秒 |
| 08:07:11.151–08:08:19.601 | 两次健康检查相隔 68.45 秒，SystemUI PID 984、system_server PID 717 一致，Activity 服务可用，两次定向 ANR 查询退出码 0、无事件 |
| 08:08:19.617 | 四路采集全部关闭，控制器记录有效 guest 样本 0，安装前门禁停止 |

180 秒设备准入为 **PASS**。额外 60 秒实际观察及结束时的系统检查已执行。综合门禁为 **BLOCKED**，控制器退出码 2，错误原文 `Insufficient diagnostic coverage in observation window`。29 份 guest 命令的实际退出码均为 0，均有 epoch、uptime、内存和 vmstat。Windows ADB 输出 `uptime\r\n`，本轮我编写的采样器只匹配 `uptime\n`，把 29 份样本全部误判为不完整。保留原控制器及日志结果，关机后只在本地修正换行匹配并离线回放。原 CRLF 29/29、LF 1/1、非零退出和缺失标记 2/2 正确拒绝。没有重新启动、重算本轮门禁或继续安装。[准入](lithic-documentsui-diagnostic-run-07/admission-result.json) · [命令和时序](lithic-documentsui-diagnostic-run-07/admission-commands.jsonl) · [采集时间线](lithic-documentsui-diagnostic-run-07/timeline.jsonl) · [系统服务](lithic-documentsui-diagnostic-run-07/system-services.txt) · [控制器退出](lithic-documentsui-diagnostic-run-07/controller-result.json) · [原始换行核对](lithic-documentsui-diagnostic-run-07/guest-sample-offline-check.json) · [离线修正回执](lithic-documentsui-diagnostic-run-07/collector-offline-repair.json)

## DocumentsUI 与资源证据

- **CONFIRMED** · 在 all、events、main+system 和首个缓冲区快照内未找到新 `am_anr`、ActivityManager ANR 或 Input dispatching 超时。all 实际覆盖 guest UTC 08:05:31.328–08:08:18.845，共 3,584,676 字节。crash 缓冲区 0 字节。DocumentsUI 只出现权限配置读取、User 0 权限状态警告和无旧式存储权限三条日志。29 份原始采样均未得到该进程 PID，本轮没有可报告的新 ANR 时间或 ANR PID。[all 摘录](lithic-documentsui-diagnostic-run-07/logcat-all.txt) · [events 摘录](lithic-documentsui-diagnostic-run-07/logcat-events.txt) · [PID 采样](lithic-documentsui-diagnostic-run-07/documentsui-process.txt)
- **CONFIRMED** · 36 份主机样本的可用内存为 2.716–5.116 GiB。QEMU 工作集约 218–2,349 MiB，CPU 累计约 0.67–190.98 秒。C/E 可用空间约 17.41–17.42／26.20–26.22 GiB。实际 QEMU 配置为 1536 MiB／2 核，guest `MemTotal=1503188 kB`，设备 720×1648／320 dpi。guest 可用内存 447,768–1,126,520 KiB，swap 使用 0–129,236 KiB。三个 PSI 文件在本轮采样中均返回 `unavailable`。[主机样本](lithic-documentsui-diagnostic-run-07/host-resource-samples.jsonl) · [guest 样本](lithic-documentsui-diagnostic-run-07/guest-resource-samples.txt) · [实际硬件](lithic-documentsui-diagnostic-run-07/effective-avd.json) · [设备屏幕](lithic-documentsui-diagnostic-run-07/display-identity.txt)
- **CONFIRMED** · guest 日志 08:06:44.081 UTC 记录 lmkd 因低水位终止 QuickSearchBox PID 1552，记录预计回收 105,824 kB RSS，随后有对应的进程死亡事件。29 份采样的内核 `oom_kill` 均为 0。它证明本轮发生过低内存回收，无法据此归因先前的 DocumentsUI 超时。[lmkd 与进程事件](lithic-documentsui-diagnostic-run-07/logcat-all.txt)
- **UNKNOWN** · 上轮 DocumentsUI 初始化超时的触发组件和根因仍未知。本轮沿用原 userdata，但样本与日志都无法确认上轮 DocumentsUI 进程初始化路径实际再次发生。没有足够证据评定 Binder、Provider、PackageManager、OverlayManager、StorageManager、I/O 或锁竞争。当前证据不足以形成单一 **LIKELY** 根因。
- **NOT_RUN** · 无新 ANR 触发定向 `lastanr`、DropBox、`/data/anr`、线程栈、进程详情或 bugreport。空白取证文件只表示本轮未触发采集。

Windows UTC、monotonic、guest epoch 和 `/proc/uptime` 在 guest 样本内并列保留。四路采集各限 24 MiB／30 分钟，首次缓冲区快照限 12 MiB。仓库发布摘录、命令、最小健康回执和完整原始文件指纹。未筛选的日志、原采集器字节及 APK 保存在 `E:/CODEX/lithic-documentsui-diagnostic-run-07-local/`。[原始字节索引](lithic-documentsui-diagnostic-run-07/raw-sha256.json)

## 首页 Android 验收

| 测试类 | 计划 | 实际 |
| --- | ---: | --- |
| HomeAndroidAcceptanceTest | 3 | 0 执行，3 NOT_RUN |
| HomeOverviewUiTest | 6 | 0 执行，6 NOT_RUN |
| P2HomeArchiveContractTest | 6 | 0 执行，6 NOT_RUN |
| ReportRepositoryTest | 19 | 0 执行，19 NOT_RUN |
| CoverPreviewsTest | 7 | 0 执行，7 NOT_RUN |
| ActivityNavigationTest | 2 | 0 执行，2 NOT_RUN |
| 合计 | 43 | 0 PASS／0 FAIL／43 NOT_RUN |

包状态检查、安装、MainActivity、三主题、双列卡片、Crop、导航及统计运行验收均 **NOT_RUN**。项目、订阅两张真实 200% 字体截图均 **NOT_RUN**。字体未修改，合成账本未创建，无相应恢复或清理操作。SAF 备份导入导出、文件浏览及 Photo Picker 实际操作 **NOT_RUN**。

## 安全退出与交付范围

确认 AVD 名称后使用 5038 的指定模拟器控制台正常关机。wrapper 退出码 0，QEMU、17640/17641 和采集进程无残留。核对独立服务身份后 5038 停止，原 5037 的监听地址、PID 16276 和进程创建时间前后一致。配置 SHA-256 前后一致。基础 userdata 保持 6,356,992 字节及原 SHA-256。QCOW2 层保留原卷、文件 ID 和创建时间，系统启动后由 573,898,752 字节变为 586,153,984 字节，退出后 SHA-256 为 `dcb70e71317a20092e4dfb90d6b25f861ea7cfe486cd078eafa442bb119df136`。没有擦除、重建、第二次启动或访问个人设备。[服务回执](lithic-documentsui-diagnostic-run-07/receipt.json) · [模拟器退出](lithic-documentsui-diagnostic-run-07/emulator-cleanup.json) · [退出后 userdata](lithic-documentsui-diagnostic-run-07/userdata-after-shutdown.json)

仓库代码差异仅为主机准入早期采集挂钩及其离线测试。生产源码、AndroidTest、Gradle、Manifest、Room、备份、金额口径、正式身份和签名配置均保持基线。未构建或发布 APK。独立审查后如授权新一轮设备运行，应使用已通过离线回放的采样器并继续沿用相同的安装门禁。

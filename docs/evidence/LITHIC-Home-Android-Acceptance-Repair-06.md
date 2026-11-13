# LITHIC HOME ANDROID ACCEPTANCE REPAIR 06

基线 `476a2c12316533156f3dcd68ce0595092e426e0d`，生产 UI `c13bd2aee1cfc07a4e8da561b75d374787033f34`。本轮以同一台 Repair05 隔离模拟器完成一次启动。App、AndroidTest、Gradle、正式身份、签名、Room、备份协议及金额算法与基线零差异。复用已经构建的 Debug App/Test，重新验签，没有重新构建 APK。[Run07 报告](LITHIC-DocumentsUI-Diagnostic-Run-07.md) · [APK 核验](lithic-home-android-acceptance-repair-06/apk-audit.json)

## 采集器修正与离线验证

采集器纳入 `scripts/home_android_diagnostic_collector.py`。实时采样与离线回放共用 `validate_guest_sample()`。用 `splitlines()` 读取 CRLF 和 LF，验证命令状态、Android epoch、uptime、MemTotal、MemAvailable、swap、vmstat 关键计数以及尾部 PID 行。原始 stdout 在判定前保存，失败写明原因。采集范围和原 ANR 门禁保持，额外 60 秒观察内单独核对主机及 guest 的有效样本数。

离线运行 `python -B -m unittest discover -s scripts -p 'test_home_android*.py' -v`，23 项准入及 7 项采集测试全部通过。缺少 uptime、内存、vmstat、输出截断和非零退出均正确拒绝。Run07 的原始采集器、修正副本和原日志指纹已对照，原 `ENVIRONMENT_BLOCKED` 保留。本轮控制器加载的仓库模块以实际路径、SHA-256 与离线回放回执绑定，加载一致性在启动前确认。[离线测试](lithic-home-android-acceptance-repair-06/offline-tests.txt) · [原样本回放](lithic-home-android-acceptance-repair-06/offline-replay.json) · [实际加载身份](lithic-home-android-acceptance-repair-06/collector-loaded.json)

## 单次冷启动、健康门禁和安装

- Repair05 AVD 第三次已记录启动，也是本轮唯一一次。保留原 API 36 default x86_64 rev2 镜像。启动前的基础 userdata 为 6,356,992 字节、SHA-256 `48d6753085207cbbdf6b1c2bc9416af3fe92ad42f6000ed5cfa49103ed0f9997`。QCOW2 层为 586,153,984 字节、SHA-256 `dcb70e71317a20092e4dfb90d6b25f861ea7cfe486cd078eafa442bb119df136`。文件身份与 Run07 关机回执一致。主机启动前可用 5.552 GiB，C/E 盘约 17.41／26.16 GiB 可用。实际 1536 MiB／2 核，guest MemTotal 1,503,188 kB，720×1648／320 dpi。[启动前回执](lithic-home-android-acceptance-repair-06/prelaunch.json) · [实际硬件](lithic-home-android-acceptance-repair-06/effective-avd.json)
- 08:39:52.352 UTC 启动，08:40:21.985 首次确认唯一自动注册的 `emulator-17640 device`，四路日志在 08:40:22 开始，首个环缓冲区 442,164 字节。08:41:01.263 完成启动、Shell 与身份核对，距启动约 68.9 秒。SDK ADB 37.0.1 独占 5038，初始设备列表为空。[准入记录](lithic-home-android-acceptance-repair-06/admission-commands.jsonl) · [独立服务](lithic-home-android-acceptance-repair-06/controller-adb-identity.json)
- 08:41:37.316–08:42:46.111 完成额外 60 秒观察及结束检查。窗口计入有效 guest 样本 14 份、主机样本 14 份，四路日志持续运行。SystemUI PID 974、system_server PID 696 保持一致，Activity 服务可用。初始健康探针 MemAvailable 455,084 kB，满足原有 450,000 kB 门槛。观察附近的独立遥测最低 447,508 kB，随后回到约 500,000 kB 以上。lmkd 在 08:41:10.334 记录低水位回收 QuickSearchBox PID 1518。启动窗口内没有捕获 DocumentsUI 启动或 ANR。[时序](lithic-home-android-acceptance-repair-06/timeline.jsonl) · [主机样本](lithic-home-android-acceptance-repair-06/host-resource-samples.jsonl) · [guest 样本](lithic-home-android-acceptance-repair-06/guest-resource-samples.txt) · [logcat 摘录](lithic-home-android-acceptance-repair-06/logcat-all.txt)
- App/Test 的 User 0 包记录、APK 路径及四个目标私有目录均确认不存在。随后用显式 `-R --user 0` 分别安装两份审核过的 APK。设备安装字节的 SHA-256 与本地相同，证书延续原 Debug 证书。唯一 Runner 为 `androidx.test.runner.AndroidJUnitRunner`，目标为 `com.cooper.lithic.debug`。[目录查询](lithic-home-android-acceptance-repair-06/preinstall-private-path-presence.txt) · [App 安装](lithic-home-android-acceptance-repair-06/install-repaired-app.txt) · [Test 安装](lithic-home-android-acceptance-repair-06/install-test.txt) · [Runner](lithic-home-android-acceptance-repair-06/runner.txt)

## 原生截图与 200% 字体

| 主题／场景 | 项目 | 订阅 |
| --- | --- | --- |
| 纯白 | [项目](lithic-home-android-acceptance-repair-06/projects-pure-white.png) | [订阅](lithic-home-android-acceptance-repair-06/subscriptions-pure-white.png) |
| 岩白雾蓝 | [项目](lithic-home-android-acceptance-repair-06/projects-stone-mist.png) | [订阅](lithic-home-android-acceptance-repair-06/subscriptions-stone-mist.png) |
| 深色黑灰 | [项目](lithic-home-android-acceptance-repair-06/projects-charcoal.png) | [订阅](lithic-home-android-acceptance-repair-06/subscriptions-charcoal.png) |
| 纯白／系统 200% | [完整双列](lithic-home-android-acceptance-repair-06/projects-font-200-cards.png) | [完整双列](lithic-home-android-acceptance-repair-06/subscriptions-font-200-cards.png) |

另有[空账本](lithic-home-android-acceptance-repair-06/empty.png)和[200% 总览](lithic-home-android-acceptance-repair-06/projects-font-200-summary.png)。10 张 PNG 均来自真实 MainActivity，保留 720×1648、360dp、实际字体、运行 ID 和 SHA-256。主题由固定测试的切换、断言与截图文件名对照。原 `font_scale=1.0` 已保存，写入 2.0 后由系统设置和 Activity 配置共同核实，finally 恢复后读回 1.0。两类卡片各两张完整入镜，名称可换行，类别与状态分行，金额及单位完整显示。八处类别／状态测量的 `hasVisualOverflow` 均为 false。[PNG 指纹及运行 ID](lithic-home-android-acceptance-repair-06/screenshots.json) · [字体实测](lithic-home-android-acceptance-repair-06/instrument-system-font-200.txt) · [测量值](lithic-home-android-acceptance-repair-06/repaired-measurements.json) · [恢复读回](lithic-home-android-acceptance-repair-06/font-restore-read.txt)

与[批准参考图](lithic-home-card-ui/approved-reference.png)对照，品牌栏、总览、分段切换、双列和底部添加／导航结构均可对照。实际截图保留品牌副标题，系统三键导航和底部按钮之间的留白与样图不同。测试账本使用合成相机与背包、两种订阅封面占位和真实测试日金额。订阅状态为实际的「订阅中」，总投入用现有格式显示为 `¥62480.00`。样图的照片、日期、分组符、示例金额和自动续费标签存在可见差异。

## Android 回归结果

| 测试类 | 实际执行 | PASS | FAIL |
| --- | ---: | ---: | ---: |
| HomeAndroidAcceptanceTest | 3 | 3 | 0 |
| HomeOverviewUiTest | 6 | 5 | 1 |
| P2HomeArchiveContractTest | 6 | 6 | 0 |
| ReportRepositoryTest | 19 | 19 | 0 |
| CoverPreviewsTest | 7 | 7 | 0 |
| ActivityNavigationTest | 2 | 2 | 0 |
| 合计 | 43 | 42 | 1 |

唯一失败为 `HomeOverviewUiTest.signed_category_values_and_type_subtotals_use_the_supplied_unrounded_results`。成本构成面板内，`home-rounding-note` 在 `performScrollTo()` 后仍未显示，触发 `HomeOverviewUiTest.kt:82` 的可见性断言。同方法在此前已完成正负金额和小计断言。生产源码仍渲染带该标记的说明，未定位真实窗口中的不可见原因。保留原断言，未重跑。上轮其余四项失败在本轮通过。SAF 导入、导出和文件浏览、Photo Picker 以及真实个人数据全部 NOT_RUN。[逐方法结果](lithic-home-android-acceptance-repair-06/test-methods.json) · [失败原始输出](lithic-home-android-acceptance-repair-06/instrument-home-overview.txt) · [运行摘要](lithic-home-android-acceptance-repair-06/test-summary.jsonl)

## 安全退出与验收状态

已确认三个 MainActivity 方法均发出 `fixture-cleaned` 回执。夹具按本轮身份移除合成记录及测试封面并确认账本归空。已导出的截图证据在隔离 App 的本轮专用路径及本机保留。保留本轮安装的两个 Debug 包和原 AVD，未擦除 userdata。运行期间 SystemUI、system_server PID 保持稳定，未捕获新 ANR，定向 ANR 线程栈和 bugreport 均 NOT_RUN。29/29 份本轮 guest 原始样本有效，采集线程和子进程全部退出。[清理与配置回执](lithic-home-android-acceptance-repair-06/test-summary.jsonl) · [系统服务](lithic-home-android-acceptance-repair-06/system-services.txt) · [采集退出](lithic-home-android-acceptance-repair-06/timeline.jsonl)

模拟器正常关机，wrapper 退出码 0，无 QEMU 或端口残留。本轮 5038 正常退出，原 5037 PID 16276 及进程创建时间保持一致。userdata 基础镜像 SHA-256 保持，QCOW2 保留原文件身份，正常写入后为 624,033,792 字节，SHA-256 `4c2a38d22b4957595bd10ed4df4c069cfe143a7b33b54552f115b2197d65bfd6`。原始系统日志与本机控制器保存在 `E:/CODEX/lithic-home-android-acceptance-repair-06-local/`，Git 收录日志摘录、原始文件指纹、JUnit 输出与 PNG。历史 Run07 证据与原 App/Test 源码保持。[退出回执](lithic-home-android-acceptance-repair-06/emulator-cleanup.json) · [ADB 回执](lithic-home-android-acceptance-repair-06/receipt.json) · [userdata 回执](lithic-home-android-acceptance-repair-06/userdata-after-shutdown.json) · [原始文件指纹](lithic-home-android-acceptance-repair-06/raw-sha256.json)

**LITHIC_HOME_ANDROID_ACCEPTANCE_REPAIR_06 — READY_FOR_REVIEW_WITH_FAILURES**。控制器和本轮工作流退出码均为 3，保留上述失败，交由独立审查。

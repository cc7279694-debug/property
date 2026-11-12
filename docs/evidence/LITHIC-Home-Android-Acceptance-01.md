# LITHIC_HOME_ANDROID_ACCEPTANCE_01

## 基线与隔离

开始时 `codex/lithic-home-investment` 本地和远端均为 `ec0c1fc0c33a86ba98887422f33596b74f17c671`，工作区干净。相对生产源码 `35149b58386e859103b22be5bdadc86d3a2322e3` 只增加前序报告和状态登记。本轮在独立分支 `codex/lithic-home-android-acceptance-01` 验证。[批准的视觉参考](lithic-home-card-ui/approved-reference.png)

使用正常 `assembleDebug` 和 `assembleDebugAndroidTest`。测试新增 `HomeAndroidAcceptanceTest.kt` 和 `HomeAndroidAcceptanceFixture.kt`，只在指定隔离 API 36 AVD 和明确参数通过后启动 `MainActivity`，在空的 Debug 账本中写入合成事实及 PNG。截图由 Android `UiAutomation` 捕获生产界面的整屏。组件测试的运行结果与整屏证据各自记录。

生产源码、Room Schema、备份协议、金额算法、正式身份和构建配置与基线一致。没有接触个人设备、正式 APK 或个人账本。

## 构建与身份

- `:app:assembleDebug` 和 `:app:assembleDebugAndroidTest` 均离线退出 0，分别耗时 2 分 34 秒和 5 分 27 秒。测试源码提交 `de899a13c6453b8618ef2320f0c9e1bfeac0aa0c`，生产 `app/src/main` 树 `b023080e96f118d263bae7f5176426bfa0e8d97f`。
- App 实物是既有 Debug 身份 `com.cooper.lithic.debug`，原 Manifest 含 `LithicApplication` 和 Launcher `MainActivity`。Test 实物是 `com.cooper.lithic.debug.test`，唯一 Runner 为 `androidx.test.runner.AndroidJUnitRunner`，指向该 Debug App。两包的 Debug 签名、SHA-256 与大小已核对。[实物审计](lithic-home-android-acceptance-01/apk-audit.json)

## 模拟器准入 · ENVIRONMENT_BLOCKED

- 默认注册目录有 10 个 AVD，均未运行，未发现有当前健康证据且可丢弃的现有 AVD。历史 `Lithic_P2A1_API36` 未启动。本机已有 AOSP API 36 x86_64 镜像、Emulator 37.1.11.0、ADB 37.0.1、可用 WHPX。已有 ADB 服务 `127.0.0.1:5037` 启动前传输列表为空。
- 启动前检查发现 Windows IPv4/IPv6 TCP 预留覆盖默认模拟器端口范围。绑定 `127.0.0.1:5558` 得到 `WinError 10013`，这次检查发生在创建模拟器进程前。随后使用 Emulator 自带的显式端口选项，以 `-ports 17640,17641` 启动新建 `Lithic_Home_Acceptance_01_API36` 一次。配置 720×1648、320 dpi、1536 MiB、2 核，无快照载入或保存。启动时主机可用 RAM 3.858 GiB，C/E 盘分别约 16.77/31.79 GiB。[运行摘要](lithic-home-android-acceptance-01/runtime-summary.txt)
- 观察至约第 41 秒，既有 ADB 仍未注册模拟器。确认 17641 由本次 QEMU 在本机回环监听后，只发出一次 `adb -H 127.0.0.1 -P 5037 connect 127.0.0.1:17641`。该命令 6 秒未返回，准入脚本提前退出，未走满 180 秒。`device`、`sys.boot_completed=1`、可用设备 Shell 和 guest 身份均未确认。超时根因仍未知。[观察](lithic-home-android-acceptance-01/boot-observations.txt) · [连接回执](lithic-home-android-acceptance-01/local-transport-connect.txt)
- 只结束了本次启动且身份已核对的模拟器进程。ADB 稍后出现本轮端点的 `offline` 残留，向该精确端点发出一次主机 `disconnect`，随后只读确认传输列表为空。17640/17641 已无监听，原 5037 服务仍为同一 PID。没有二次启动或安装。[进程回执](lithic-home-android-acceptance-01/cleanup.txt) · [离线传输收口](lithic-home-android-acceptance-01/owned-offline-disconnect.txt)

实际启动命令（现有 SDK 的 `emulator.exe`）

```text
emulator -avd Lithic_Home_Acceptance_01_API36 -no-window -no-audio -no-snapshot-load -no-snapshot-save -memory 1536 -cores 2 -gpu swiftshader_indirect -ports 17640,17641 -no-boot-anim
```

## 验收结果

| 检查 | 状态 | 证据 |
| --- | --- | --- |
| Debug 和 AndroidTest 构建、身份及源码范围 | PASS | 两份构建退出 0，已核对 App/Test 身份与生产树 |
| 新 AVD 的可用 Android 连接与设备健康 | BLOCKED | 单次启动后连接命令超时，未取得设备准入 |
| 项目／订阅首页、参考图对照、三主题 | NOT_RUN | 整屏截图 0 张 |
| 360dp、常规及系统 200% 字体 | NOT_RUN | 只有拟用 AVD 配置，未核实 Activity 的实际宽度和字体 |
| 横图、竖图、无封面、封面失败及 Crop | NOT_RUN | 合成夹具已编译，未写入 guest |
| 分段、列表、详情、两处新增、菜单、底栏、返回与面板 | NOT_RUN | 原生交互脚本已编译 |
| 空、零、正常、未知、失败、旧快照、历史、收回、换新及订阅实付独立性 | NOT_RUN | 未执行 Room 或 Compose 断言 |

计划执行的 `HomeOverviewUiTest`（6）、`P2HomeArchiveContractTest`（6）、`ReportRepositoryTest`（19）、`CoverPreviewsTest`（7）、`ActivityNavigationTest`（2）及新增真实 `MainActivity` 验收方法（3）全部 `NOT_RUN`。JUnit 实际执行 0，测试失败 0。因没有运行证据，无法评价视觉相似度或判定产品缺陷。

未裁剪的构建日志、APK Manifest／签名输出、宿主和 emulator 原始日志保留在本机 `E:/CODEX/lithic-home-android-acceptance-01-local/`。GitHub 保留运行回执及去除本地路径的准入脚本完整输出。[准入脚本输出](lithic-home-android-acceptance-01/runtime-output.txt)

**ENVIRONMENT_BLOCKED**。等待 ChatGPT 独立验收。

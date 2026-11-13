# LITHIC 首页 Android 验收补修 04

**LITHIC_HOME_ANDROID_ACCEPTANCE_REPAIR_04 — ENVIRONMENT_BLOCKED**

基线 `d7b67c811193c9d17f558a03a0756fa11cb542bf`，分支 `codex/lithic-home-android-acceptance-repair-04`。上轮的 43 项原始结果与 8 张截图保留在原目录。[Acceptance 03](LITHIC-Home-Android-Acceptance-03.md)

## 修改范围

- `HomeShell.kt`：在字体缩放大于 135% 时，双列卡片内部的类别、状态各占一行。保留原标签样式、字号、间距、封面和金额组件。标签使用 Compose 的 `AnnotatedString` 版本，文字布局动作可返回实际段落结果。
- `HomeOverviewUiTest.kt`：三处已失败的日均金额预期改为精确匹配完整 `/天`。同一方法中随后读取的分类与分项金额也匹配完整语义。两种新增菜单选项直接断言可见并点击，保留四项导航回调的顺序和内容断言。
- `HomeAndroidAcceptanceTest.kt`：保留原 `hasVisualOverflow`、文字边界和按钮防遮挡断言。新增类别、全部三导航可见性以及两张卡片同时位于视口内的截图前置检查。新增基线测量模式，仍执行原断言。

## 方法

同一次隔离 AVD 启动先加载只增加 `home-card-labels` 测量标记的基线 Debug App，再用同签名、同身份的修复 App 更新模拟器内的可丢弃测试包。两个阶段共用重新编译并审计的 Test APK。基线额外执行仅用于采集修复前证据，与修复版 43 项分别计数。真实用户设备、正式 APK 和个人账本不在本次运行范围。

Compose 1.9.5 本地依赖字节码显示，`Text(String)` 的 `GetTextLayoutResult` 可用最大宽度重建 `MultiParagraph`，同时带回原缓存的布局尺寸。`TextLayoutResult.didOverflowWidth` 比较这两个值。`Text(AnnotatedString)` 的语义动作则复用实际测量结果。基线探针记录宽高约束、测量尺寸、实际行宽、字号、行高及宽高溢出标志，供运行证据区分视觉裁切与语义报告差异。

## 桌面验证与 APK 身份

- **PASS**：22 项离线准入测试、47 项 JVM 回归、生产及 AndroidTest Kotlin 编译、两项 Debug APK 构建及 Git 差异检查。[离线输出](lithic-home-android-acceptance-repair-04/offline-tests.txt) · [JVM 结果](lithic-home-android-acceptance-repair-04/jvm-results.json) · [构建输出](lithic-home-android-acceptance-repair-04/desktop-build-results.txt)
- **PASS**：三个内部 APK 的实物 Manifest、包名、原 Debug 证书和签名。两个 App 同为 `com.cooper.lithic.debug`、code17、`0.1.0-m22-debug`，入口为正常 `LithicApplication`／`MainActivity`。Test 为 `com.cooper.lithic.debug.test`，Runner 为 `androidx.test.runner.AndroidJUnitRunner`，唯一目标为该 Debug App。只保留本地测试实物与核验记录。[APK 指纹](lithic-home-android-acceptance-repair-04/apk-audit.json)
- 启动前主机可用 3.697 GiB，C/E 可用 17.74／29.11 GiB。本轮只运行 1536 MiB guest，Gradle 已结束。独立 ADB 37.0.1 仅用 5038，原 5037 仅用 Windows 监听与进程信息前后核对。

## 单次设备准入

**PASS**。约 103.3 秒内完成唯一自动注册、`sys.boot_completed=1`、Shell 及指定 AVD／API 36／x86_64 核验。新生成的硬件文件为 1536 MiB／2 核，Android `MemTotal` 为 1,503,188 kB，显示参数为 720×1648／320 dpi。[准入日志](lithic-home-android-acceptance-repair-04/admission-commands.jsonl) · [硬件回执](lithic-home-android-acceptance-repair-04/effective-avd.json) · [guest 内存](lithic-home-android-acceptance-repair-04/effective-guest-memory.txt)

**PASS**，安装前健康检查。SystemUI PID 961 和 system_server PID 707 前后一致，Activity 服务可用，限定事件查询未见 ANR。MemAvailable 为 480,732 kB，`/data` 可用 5,577,344 KiB。[健康记录](lithic-home-android-acceptance-repair-04/guest-health.txt) · [ANR 查询](lithic-home-android-acceptance-repair-04/health-admission-anr.txt)

**BLOCKED**，首条 `pm list packages -u --user 0 com.cooper.lithic.debug` 同时返回精确 App 与 Test 包记录。控制器在安装前停止。`-u` 可包含未安装包，本轮未确认实际安装状态、数据归属或保留机制。AVD 配置虽标为 `<temp>`，上轮退出和夹具清理通过，本轮实际查询仍证明不能假定包记录会消失。退出日志也出现 `Saving snapshot 'default_boot'`，该行不足以确定包记录来自快照。预检继承了 03 的空包条件。预启动没有验证 AVD 的存储是否会复用包记录，是本次准备的缺口。[包查询原件](lithic-home-android-acceptance-repair-04/preinstall-com.cooper.lithic.debug.txt) · [控制器回执](lithic-home-android-acceptance-repair-04/controller-result.json) · [启动和退出日志](lithic-home-android-acceptance-repair-04/emulator.stdout.redacted.txt)

| 修复版 Android 执行组 | 计划项 | 实际执行 | 状态 |
| --- | ---: | ---: | --- |
| `HomeAndroidAcceptanceTest` | 3 | 0 | NOT_RUN |
| `HomeOverviewUiTest` | 6 | 0 | NOT_RUN |
| `P2HomeArchiveContractTest` | 6 | 0 | NOT_RUN |
| `ReportRepositoryTest` | 19 | 0 | NOT_RUN |
| `CoverPreviewsTest` | 7 | 0 | NOT_RUN |
| `ActivityNavigationTest` | 2 | 0 | NOT_RUN |
| 合计 | 43 | 0 | NOT_RUN |

基线测量与两张 200% 卡片截图均为 **NOT_RUN**。360dp 的静态卡片宽度为 `(360−24−10)/2 = 163dp`，两层 5dp 左右内边距留下 143dp，标签 6dp 左右内边距再减少 12dp。实际父子测量约束、行宽以及 `didOverflowWidth`／`didOverflowHeight` 本轮没有观测结果。Compose 语义重建是有源码支持的解释，尚无法确认上轮是实际文字裁切、测量报告误差或两者同时存在。保留 03 的原始 FAIL，未做修复前后视觉判定。

## 安全退出与独立审查

本轮安装、卸载、字体写入、合成账本写入、Instrumentation 和截图均为 0。原字体未改，无需恢复。一次启动额度已用完，未重启、切换端口或清理 AVD 数据。已通过核对归属的控制台正常关闭模拟器，wrapper PID 29172 退出码 0。QEMU、17640／17641 无残留。核验 SDK 进程后关闭 5038，退出码 0，PID 32084 无残留。5037 监听 PID 16276 和进程创建时间前后一致，未向 5037 发协议请求。[模拟器退出](lithic-home-android-acceptance-repair-04/emulator-cleanup.json) · [ADB 收口](lithic-home-android-acceptance-repair-04/receipt.json)

生产差异仅为首页卡片标签。Room、备份协议、金额逻辑、发布身份、其他生产页面和历史证据均与基线一致。完整复验需要另获一次启动授权，并预先明确使用可验证为空的测试数据目录，或先验明该 AVD 中保留的包记录及测试账本归属。本轮只提交分支，等待独立审查。

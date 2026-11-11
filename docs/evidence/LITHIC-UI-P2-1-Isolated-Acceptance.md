# LITHIC P2-1 隔离补验

**结果** `P2-1 SUPPLEMENT — ENVIRONMENT_BLOCKED`，整体仍为 `PARTIAL`。记录日期为 2026-11-11（Asia/Shanghai）。按用户转达的独立结论，生产代码审查为 PASS。本次复核固定提交 `f394c3c46a3acdb90b959d118c0f76bb7d4094f8`，开始时本地与远端 `codex/lithic-ui-p2-home` 一致，工作区干净。

## 隔离宿主及环境

- [M13IsolatedRunner](../../app/src/androidTest/java/com/cooper/lithic/app/M13IsolatedRunner.kt#L8) 的 `newApplication` 使用 `android.app.Application`，避免由生产 Application 初始化业务 Owner。仅启用 `m13Isolated` 不切换包身份。
- 现有 [Gradle 隔离配置](../../app/build.gradle.kts#L116) 另有显式 `c2VisualVerification` 模式，使用独立 App/Test 包 `com.cooper.lithic.c2visual.debug` / `com.cooper.lithic.c2visual.debug.test` 和同一 Runner。[测试目标 Manifest](../../app/src/c2Visual/AndroidManifest.xml) 使用普通 Application 和无 Launcher 的 `ComponentActivity`，关闭备份。此次仅核对源码与配置。
- 主机进程检查 `Get-Process` 未发现 emulator 或 QEMU。监听检查 `Get-NetTCPConnection -State Listen` 确认已有本机 ADB 服务监听 `5037`。仅向现有本机服务发送主机级 `host:devices`，退出码 `0`，脱敏回执为 `host:devices OKAY; total=0; emulator=0`。未启动 ADB 服务，未发送设备命令。
- 注册目录内发现的砺石 AVD 配置 `Lithic_M01_API_36.ini` 指向未运行的 API 36 环境。现有证据无法确认其数据可丢弃、当前健康及获准使用，因此未启动。历史 M22 SystemUI ANR 环境与 C3 小米 14 现场均排除。见 [M22 旧环境](M22-Android-supplement.md)、[M22 隔离环境记录](M22-Clean-Android-verification.md)、[C3 采集记录](UI-C2-C3-Native-Capture-Execution.md)。

## 指定 Android 测试

| 测试类 | 源码方法数 | 实际执行 | 本轮结果 |
| --- | ---: | ---: | --- |
| [C9R1HomeHierarchyTest](../../app/src/androidTest/java/com/cooper/lithic/ui/C9R1HomeHierarchyTest.kt#L27) | 1 | 0 | NOT_RUN |
| [C9ThemeMatrixTest](../../app/src/androidTest/java/com/cooper/lithic/ui/C9ThemeMatrixTest.kt#L30) | 6 | 0 | NOT_RUN |
| [C9RecordThemeTest](../../app/src/androidTest/java/com/cooper/lithic/ui/C9RecordThemeTest.kt#L50) | 3 | 0 | NOT_RUN |
| 合计 | 10 | 0 | ENVIRONMENT_BLOCKED |

未启动 Instrumentation，未产生本轮 Android JUnit XML 或测试日志。原 [P2-1 桌面回执](LITHIC-UI-P2-1-Home-Foundation.md) 的 25 项 JVM PASS、AndroidTest 编译及 Lint（0 错误、20 警告）为既有证据，本轮未复跑。

## 静态复核

| 核对项 | 源码证据与验证边界 |
| --- | --- |
| HomeShell 测试标签 | 对比正式基线 `d42a78125b83bee806add5e3fdd8d2328055b694` 与固定提交，22 个直接 `testTag` 的值及出现次数完全一致。5 处变量标签调用及项目、订阅预览、小计和类别标签的传参保留。见 [HomeShell](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt)。 |
| 三主题切换 | 测试源码覆盖五个页面根节点、外观选择面板切换后的选中状态、首页构成及新增面板状态，以及未确认表单输入。主题状态在 Android 上的实际表现待验，现有断言也未验证 DataStore 持久化。 |
| 大字体与长名称 | 首页层级测试在 360dp 下局部模拟 `fontScale=2f`。长名称、负金额和完整金额断言在项目详情测试。真实系统 200% 字体、首页长名称排版及窄屏无遮挡待验。 |
| 唯一 FAB 与零写入 | 生产首页仅有一个 56dp、标记为 `home-open-add` 的 FAB，点按先打开新增选择面板。测试源码保留唯一节点与合成回调零调用断言。实际节点数量及断言执行结果均为 NOT_RUN。 |

本轮仅新增补验报告。未访问账本、操作手机或启动 AVD，未构建 APK，生产和测试源码无改动。停在 P2-1，等待 ChatGPT 独立验收。

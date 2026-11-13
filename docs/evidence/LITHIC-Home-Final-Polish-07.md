# LITHIC HOME FINAL POLISH 07

基线 `eab81e72b0f554259bdff8bdce81281f3f4a8b3d`。只调整 `HomeShell.kt` 日均成本展示和 `HomeOverviewUiTest.kt` 的舍入说明验证。沿用上轮可复核的原生 Android 测试、合成账本和已验证的采集器。[Repair06 报告](LITHIC-Home-Android-Acceptance-Repair-06.md)

## 源码与桌面验证

移除 `HomeDailyRate` 内三根固定装饰柱及其专用布局。日均成本、日期、总投入、数量和成本说明入口保留。成本构成面板布局未改。无数据库、迁移、备份、模型、金额算法、正式包名、版本或签名变化。

保留 `HomeOverviewUiTest` 的六项测试、金额断言和舍入说明可见性断言。失败方法额外保留语义滚动前后、手势后坐标、展开操作与滚动范围，对说明执行真实上滑、完整文本和可见窗口检查。诊断截图只在隔离验收传入本轮标识时保存。

定向 JVM 31 PASS、离线采集与设备准入 30 PASS。Debug App 和 AndroidTest APK 构建通过，最终测试变更单独重编通过。原 Debug 证书 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，Debug App code17，Test Runner `androidx.test.runner.AndroidJUnitRunner` 及目标包均通过核对。[JVM 结果](lithic-home-final-polish-07/jvm-test-summary.json) · [离线结果](lithic-home-final-polish-07/offline-tests.txt) · [源码与 APK](lithic-home-final-polish-07/source-apk-binding.json)

## 成本构成舍入说明

Repair06 的堆栈确认节点存在，`performScrollTo()` 返回后，可见性断言失败。同一方法的金额断言通过。读取了本机 Material3 1.4.0 和 Compose UI Test 1.9.5 字节码。默认面板内容超过屏幕一半时可先停在半展开位置。语义滚动只向最近可滚动父节点发送 `ScrollBy`，实际显示检查还会考虑 Android 窗口可见范围。半展开状态可能解释说明仍在屏幕外，本轮未取得面板坐标或手势前后截图，真实根因仍为 **UNKNOWN**。按任务约束保留生产面板布局。[上轮原始失败](lithic-home-android-acceptance-repair-06/instrument-home-overview.txt)

本轮测试在失败时准备记录完整语义树、窗口与节点坐标、可滚动父节点和可用展开操作，并保留诊断截图。该测试尚未运行，当前不能认定测试修正或生产面板已通过。

## 实际 Android 验收

**LITHIC_HOME_FINAL_POLISH_07 — ENVIRONMENT_BLOCKED**

原 Repair05 AVD 和两层 userdata 通过哈希、文件身份和配置检查。本轮仅启动一次，API 36 x86_64、实际 1536 MiB／2 核、720×1648／320 dpi。独立 SDK ADB 37.0.1 使用 5038，唯一自动注册目标 `emulator-17640` 在约 80.7 秒完成设备准入。采集器记录 14/14 份有效 guest 样本，四路日志在 boot 完成前开始。本轮观察范围内没有记录到新 ANR。[设备准入](lithic-home-final-polish-07/admission-result.json) · [设备命令](lithic-home-final-polish-07/admission-commands.jsonl) · [采集时序](lithic-home-final-polish-07/timeline.jsonl)

原健康检查读取 `MemAvailable=446216 kB`，比 450,000 kB 门槛少 3,784 kB。同一探针的内置存储余量 5,576,088 KiB，满足要求。该探针触发安装前停止，控制器退出码 2，外层工作流退出码 1。额外 60 秒观察、设备端已装 Debug 包核对、APK 更新、运行时面板诊断均为 NOT_RUN。[资源探针](lithic-home-final-polish-07/guest-health.txt) · [控制器结果](lithic-home-final-polish-07/controller-result.json)

| Android 测试类 | 本轮执行 | 本轮状态 |
| --- | ---: | --- |
| HomeAndroidAcceptanceTest | 0 | 3 NOT_RUN |
| HomeOverviewUiTest | 0 | 6 NOT_RUN |
| P2HomeArchiveContractTest | 0 | 6 NOT_RUN |
| ReportRepositoryTest | 0 | 19 NOT_RUN |
| CoverPreviewsTest | 0 | 7 NOT_RUN |
| ActivityNavigationTest | 0 | 2 NOT_RUN |
| 合计 | 0 | 0 PASS／0 FAIL／43 NOT_RUN |

本轮新纯白项目、纯白订阅、舍入说明截图及删除装饰柱后的视觉对照均为 **NOT_RUN**。Repair06 的 [200% 项目](lithic-home-android-acceptance-repair-06/projects-font-200-cards.png) 与 [200% 订阅](lithic-home-android-acceptance-repair-06/subscriptions-font-200-cards.png) 只证明该基线的 360dp 系统大字、双列完整、8 处标签无溢出以及底部操作与导航。本轮未重新验证这些行为。

## 安全退出

控制器未更改字体、未创建合成账本或测试媒体，原字体无需恢复。本轮未卸载、覆盖安装或清理既有 Debug 包。模拟器正常关闭，wrapper 退出码 0，QEMU 与四个日志采集进程均退出，独立 5038 已关闭，原 5037 PID 16276 与创建时间保持。基础镜像哈希和两层文件身份保持，启动产生的 QCOW2 为 632,946,688 字节，SHA-256 `13565eea479b3d134e47caca8b8650411081509b66281fefe50e56e35720f8f5`。本轮没有操作手机、真实账本或正式 APK。[模拟器关机](lithic-home-final-polish-07/emulator-cleanup.json) · [端口与进程回执](lithic-home-final-polish-07/receipt.json) · [关机后 userdata](lithic-home-final-polish-07/userdata-after-shutdown.json)

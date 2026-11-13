# LITHIC HOME FINAL POLISH 09 — 单次复验

**LITHIC_HOME_FINAL_POLISH_09 — READY_FOR_INDEPENDENT_REVIEW**

复验源码 `f9d5029e916fff9fb605ef7c0f25a3c99308579c`，`app` 树 `560dedb63f049856be581f6610b27d08097ff105`。沿用上次已编译的 AndroidTest 两段返回修正，重新打包 Debug AndroidTest，Gradle 退出 0。复用原生产 Debug APK，两包证书一致，已安装包 SHA-256 与本地一致。生产 Kotlin 相对于 `2232c377bd5ddd174c85edcd747d45bac031e50d` 无差异。本轮仅提交报告、原生截图和运行证据。[上次失败记录](LITHIC-Home-Final-Polish-09.md) · [构建日志](lithic-home-final-polish-09-retry/build.txt) · [源码与 APK](lithic-home-final-polish-09-retry/source-binding.json)

## 设备与系统健康

Windows 启动前 8 个逻辑处理器，60 秒 CPU 均值 18.03%，可用内存 5.209 GiB，C/E 可用 17.38/25.59 GiB。原 Repair05 AVD 单次启动，使用同一 userdata 和系统镜像。SDK ADB 37.0.1／5038 自动注册唯一 `emulator-17640`，API 36 x86_64、720×1648／360dp。新硬件配置 1536 MiB，guest `MemTotal=1502724 kB`，在线 CPU `0-3`。永久配置仍为 2 核，配置 SHA 不变。[资源准入](lithic-home-final-polish-09-retry/host-admission.json) · [设备准入](lithic-home-final-polish-09-retry/admission-result.json) · [硬件配置](lithic-home-final-polish-09-retry/effective-avd.json) · [guest CPU](lithic-home-final-polish-09-retry/effective-guest-cpu.txt)

安装前 guest 可用内存 455,000 kB，通过 450,000 kB 门槛。额外 60 秒健康观察内取得 14 份 guest、13 份主机样本。SystemUI 1052、system_server 760、Phone 1189 从准入到最后一组测试均保持稳定，Activity 服务正常。全程 133 份 guest、140 份主机样本，guest `MemAvailable` 为 284,608–1,101,004 kB，`oom_kill` 均为 0。采集窗口无新 ANR，线程栈采集 NOT_RUN。此前两核的 SystemUI／Phone ANR 在上次及本次四核运行中均未复现，CPU 是否为历史根因仍 UNKNOWN。[系统服务](lithic-home-final-polish-09-retry/system-services.txt) · [guest 资源](lithic-home-final-polish-09-retry/guest-resource-samples.txt) · [主机样本](lithic-home-final-polish-09-retry/host-resource-samples.jsonl) · [ANR 事件](lithic-home-final-polish-09-retry/anr-events.txt)

## 舍入说明与 43 项回归

真实 MainActivity 的前置专项 **1 PASS**。半展开时，说明根坐标 `[40,1677–680,1757]` 位于 1648 px 窗口下方，窗口内裁切为零，内部滚动范围为 `0/0`。语义滚动后坐标相同。真实拖动展开再向上滑动后，完整文案位于 `[40,1424–680,1504]`，完整文案和可见性断言均通过。第一次 Back 回到半展开，第二次 Back 关闭并返回首页。成本构成布局保持原样，首页总览无固定装饰柱。[专项原始输出](lithic-home-final-polish-09-retry/instrument-routes-focused.txt) · [坐标和语义树](lithic-home-final-polish-09-retry/rounding-viewport.txt) · [半展开截图](lithic-home-final-polish-09-retry/cost-composition-before-gesture.png) · [说明完整截图](lithic-home-final-polish-09-retry/cost-composition-rounding-note.png)

| 测试类 | PASS | FAIL |
| --- | ---: | ---: |
| `HomeAndroidAcceptanceTest` | 3 | 0 |
| `HomeOverviewUiTest` | 6 | 0 |
| `P2HomeArchiveContractTest` | 6 | 0 |
| `ReportRepositoryTest` | 19 | 0 |
| `CoverPreviewsTest` | 7 | 0 |
| `ActivityNavigationTest` | 2 | 0 |
| 合计 | **43** | **0** |

43 个不同方法均为原始 JUnit 状态码 0，无跳过或假设失败。前置专项重复调用 `home_routes_open_and_back`，独立记录，不计入 43 项。Android 控制器和工作流均退出 0。[逐方法记录](lithic-home-final-polish-09-retry/test-methods.json) · [运行回执](lithic-home-final-polish-09-retry/controller-result.json) · [核对汇总](lithic-home-final-polish-09-retry/evidence-verification.txt)

## 原生截图与 200% 字体

| 主题／状态 | 项目 | 订阅 |
| --- | --- | --- |
| 纯白 | [截图](lithic-home-final-polish-09-retry/projects-pure-white.png) | [截图](lithic-home-final-polish-09-retry/subscriptions-pure-white.png) |
| 岩白雾蓝 | [截图](lithic-home-final-polish-09-retry/projects-stone-mist.png) | [截图](lithic-home-final-polish-09-retry/subscriptions-stone-mist.png) |
| 深色黑灰 | [截图](lithic-home-final-polish-09-retry/projects-charcoal.png) | [截图](lithic-home-final-polish-09-retry/subscriptions-charcoal.png) |
| 纯白／系统字体 200% | [双列卡片](lithic-home-final-polish-09-retry/projects-font-200-cards.png) | [双列卡片](lithic-home-final-polish-09-retry/subscriptions-font-200-cards.png) |

系统设置及 Activity 均实测 fontScale 2.0、宽 360dp。滚动后两张项目卡和两张订阅卡均完整显示，八处类别和状态标签 `VisualOverflow=false`，底部添加和导航没有盖住卡片。另存总览截图核对大字的总投入、日均金额、单位和日期。全套 14 张截图均由本次原生 MainActivity 采集，封面及账本为合成数据。与批准样图相比，订阅截图展示了无封面和封面不可用的实际状态，金额和数量来自本轮测试账本。[大字总览](lithic-home-final-polish-09-retry/projects-font-200-summary.png) · [8 项布局读数](lithic-home-final-polish-09-retry/repaired-measurements.json) · [截图索引与 SHA](lithic-home-final-polish-09-retry/screenshots.json)

## 收口与未运行范围

四次 MainActivity 方法调用均收到夹具清理回执，夹具复查空业务表和媒体目录。系统字体由 2.0 恢复为原值 1.0 并读回。模拟器正常关闭，原 AVD 与 userdata 文件身份保留，基础层 SHA 一致，隔离 QCOW2 正常写入后为 704,970,752 字节。独立 5038、模拟器及日志采集均退出，5037 PID 16276 与创建时间保持一致。仅操作隔离模拟器内既有 Debug 测试身份，未连接个人设备，未构建或发布正式 APK。小米 14、TalkBack 实际朗读、真实 SAF 和 Photo Picker 不在本轮执行范围，保留 NOT_RUN。完整日志留存于 `E:/CODEX/lithic-home-final-polish-09-retry-local/`，仓库保存运行输出、筛选后的系统诊断及原始流指纹。[字体恢复](lithic-home-final-polish-09-retry/font-restore-read.txt) · [模拟器退出](lithic-home-final-polish-09-retry/emulator-cleanup.json) · [主机复查](lithic-home-final-polish-09-retry/host-postflight.json) · [userdata](lithic-home-final-polish-09-retry/userdata-after-shutdown.json) · [原始指纹](lithic-home-final-polish-09-retry/raw-sha256.json)

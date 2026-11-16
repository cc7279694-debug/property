# 砺石 V4 B2 R1 · 首页分段按压修复

任务 `LITHIC_UI_V4_B2_SEGMENT_PRESS_REPAIR_01`。用户在小米14录屏观察到「项目／订阅」快速切换时的灰色外环、双边轮廓及渐隐。本轮以该反馈和 B2 实物为对照，修复范围仅为这个分段控件。

## 根因调查

1. B2 的 `HomeBrowseTab` 外层高度至少 48dp。`javap` 确认编译产物调用未显式传入 indication 的 `selectable` 重载。选中胶囊是另一层，上下各缩进 7dp。
2. 从 B2 APK 的版本标记确认 Foundation、Material Ripple 为 1.9.5，Material3 为 1.4.0。该版本字节码显示，默认重载使用外层节点的 `LocalIndication`，MaterialTheme 提供默认有界 Ripple，Android 绘制接收 indication 节点的宽高。外层按最小 48dp 高度测量时，选中胶囊只有 34dp 高，上下差额仍处于旧 Ripple 的可绘制范围。这里确证了绘制路径与范围差异，具体到小米14录屏逐帧的归因仍待设备复验。[B2 生产实现](https://github.com/cc7279694-debug/property/blob/1b8f9e639be786e1a2196e50e6b33d974032c5ab/app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L425-L437) · [编译产物及依赖字节码摘录](lithic-ui-v4-b2-segment-press-repair-01/bytecode-trace.txt)
3. 对照已修复的 V4-A 方案。其触控节点发出按压、释放与取消，图标圆层使用同一个交互源和带形状裁切的居中 Ripple。[原悬浮导航实现](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L845)

## 代码差异

- [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) 只改 `HomeBrowseTab` 及导入。外层沿用原触控尺寸、圆角、`Role.Tab`、选中语义、测试标签和回调，独立记忆一个交互源并显式关闭外层 indication。内层沿用 7dp 缩进与选中颜色，未选中时表面透明。贴在内层的 Ripple 居中绘制并受 `CircleShape` 裁切，外层边缘的按压仍能触发内层反馈。释放与取消由同一 `selectable` 发出。
- [HomeSegmentPressTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/home/HomeSegmentPressTest.kt) 增加两项定向回归。沿用原测试及固定合成预览。独立代码复核未发现阻断。
- 底栏、分段轨和三个主题 Token 从 B2 继承。生产差异核对覆盖首页其它区域、Repository、数据与成本、Nav3、详情和备份路径，除 `HomeBrowseTab` 外没有生产差异。

## 定向 Android 验证

| 测试 | 断言 |
| --- | --- |
| `held_feedback_stays_inside_the_capsule_and_clears_in_each_theme_at_both_font_scales` | 三主题 × 模拟 100%／200%，两项分别触发选中和未选中状态。手指从外层距顶 3dp 处按下，检查内层像素改变、外部稳定。取消不切换，已选项释放后保持选中，逐像素比对恢复后的画面。 |
| `switching_rapidly_and_reselecting_keeps_tab_semantics_and_matching_preview_and_list` | 模拟两档字号下双向切换、重复点按两项、24ms 间隔的连续点按、最终选中状态与恢复后的像素。对应预览与「全部」回调，以及 48dp、`Role.Tab`、标签和选中语义均有断言。 |

像素判据给予胶囊边缘 2dp 抗锯齿容差与 RGB 3/255 差值阈值。快速切换覆盖最终状态和按压淡出后的残留，逐帧闪烁留给实机录屏复验。

主机预检未找到已准入的隔离 Android。没有运行中的模拟器，5038 与历史测试端口未监听，5037 原服务保持。历史 V4 隔离 AVD 未达到启动门禁，既有 ANR 环境未重复启动。本轮没有 ADB、安装或小米14操作。两项新增用例、旧代码原生像素复现、新代码真机像素、真机快速切换及系统 200% 字体均为 `NOT_RUN`。B2 其它功能的设备验收仍待单独执行。

## 基线与提交

Fetch 后确认远端 B2 和干净的本地工作区均位于 `1b8f9e639be786e1a2196e50e6b33d974032c5ab`。从该提交建立 `codex/lithic-ui-v4-b2-segment-press-repair-01`。源码提交 `86e7bf3897a27e6b028083520b0fb0a630005917`，App 树 `ae0ffdbbb1aa6d85e9ba7b06fb2d0a4be84b3d86`。`main` 和 `codex/lithic-release-01` 不在交付范围。

## 桌面结果

桌面采用 JDK 17.0.20.1、Android SDK 36、Gradle 8.14.3，离线单 worker。桌面检查启动时可用内存 4.627 GiB。

- 定向 JVM 来自 14 个类，共 **78 PASS**，0 失败、0 错误、0 跳过。覆盖首页状态和搜索、导航策略及三主题色板。
- `:app:compileDebugKotlin` 和 `:app:compileDebugAndroidTestKotlin` 均实际执行并通过，其中新增两项 Android 用例只完成编译。
- `:app:lintDebug` 完成生产、AndroidTest 与 JVM 分析，**0 错误／20 警告**。验证批次退出 0，共 49 个任务实际执行。
- 最终编译物 `HomeBrowseTab` 使用显式的 indication 重载，并在 7dp 内缩绘制层安装单独 `indication`。[编译对照](lithic-ui-v4-b2-segment-press-repair-01/bytecode-trace.txt)

[桌面日志](lithic-ui-v4-b2-segment-press-repair-01/desktop-verification.log) · [JVM 汇总](lithic-ui-v4-b2-segment-press-repair-01/jvm-result.json) · [原始 JVM XML](lithic-ui-v4-b2-segment-press-repair-01/jvm/) · [Lint 报告](lithic-ui-v4-b2-segment-press-repair-01/lint-results-debug.txt)

## 唯一 Debug APK

在桌面门禁通过后执行一次 `:app:assembleDebug`。开始时可用内存 4.788 GiB。构建退出 0，19 项实际执行、21 项复用，输出目录中有一份 APK。[构建日志](lithic-ui-v4-b2-segment-press-repair-01/assemble-debug.log)

| 项目 | 核对结果 |
| --- | --- |
| 本机路径 | `E:\CODEX\lithic-v4-b2-segment-repair\app\build\outputs\apk\debug\app-debug.apk` |
| 身份 | `com.cooper.lithic.debug`，`0.1.0-m22-debug`，版本号 17，与 B2 一致 |
| 字节数 | 20,590,822 |
| SHA-256 | `b9a09f25141b27b3a5732475f0bc6bc9dbe3b2ff92d60c0180036eda38dd37d2` |
| 签名 | 单一 B2 原 Debug 证书 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，APK v2 验签通过 |
| 保留 | Manifest 与 B2 解码结果逐行一致，ZIP CRC 通过。原 B2 APK 与正式 1.0.1 APK SHA-256 复算一致 |

[身份回执](lithic-ui-v4-b2-segment-press-repair-01/apk-identity.json) · [包规格](lithic-ui-v4-b2-segment-press-repair-01/apk-badging.txt) · [签名回执](lithic-ui-v4-b2-segment-press-repair-01/apk-signature.txt)

**`READY_FOR_DEVICE_RECHECK`。** 构建及证据位于独立修复分支，交付后用 `git ls-remote` 核对本地、远端与受保护分支。等待用户小米14复验和 ChatGPT 独立验收。

# LITHIC UI V4 · 悬浮导航按压反馈修复 02

## 现象与前后证据

用户报告小米14录屏中的三个底部图标会出现灰色矩形，部分按压反馈在手指离开后短暂可见。本轮收到的是现象描述。基线证据为固定提交 `c3c0b977d7db3142a7231af028d702e369a0907a` 的导航代码及之前就绪的 Debug APK。已有浏览器画板只用于核对外观，无法显示 Android Ripple 的绘制。[基线源码](https://github.com/cc7279694-debug/property/blob/c3c0b977d7db3142a7231af028d702e369a0907a/app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L842-L899) · [基线 APK 和历史 Android 状态](LITHIC-UI-V4-ICON-NAVIGATION-01.md)

| 层 | 基线 | 修复 |
| --- | --- | --- |
| 外层触控区 | 48dp 高的矩形 `Box` 使用 `selectable` 的默认 `LocalIndication`。三项仍为等宽触控目标。 | 同一 `selectable` 接收每个页签记忆化的 `MutableInteractionSource`，明确设为 `indication = null`。原触控区与点击回调保留。 |
| 内层图标与反馈 | 40dp 的 `CircleShape` 只用来画选中底色，外层 Ripple 的绘制边界不受这层形状约束。 | 40dp 内层以 `CircleShape` 裁切同源的 Material Ripple。`bounded = false` 让它始终从内部中心绘制，包括手指按在外层热区边缘的场景，外部仍由圆裁切。 |
| 释放与取消 | 外层默认 Ripple 的视觉生命周期在矩形区域内运行。 | 原点击节点负责发出 `PressInteraction.Release` 或 `Cancel`。内层显示同一交互源的反馈并按 Material 生命周期清除。淡出期间仍受圆形裁切。 |

Compose 默认 `selectable` 会使用 `LocalIndication`，Android 有界 Ripple 按接收其指示的绘制边界绘制。旧版的两层布局解释了矩形反馈的来源。用户录屏中的具体帧和本轮 Android 对照仍待实际运行。[AndroidX Selectable 源码](https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/selection/Selectable.kt#L42-L95) · [AndroidX Android Ripple 绘制](https://github.com/androidx/androidx/blob/androidx-main/compose/material/material-ripple/src/androidMain/kotlin/androidx/compose/material/ripple/RippleHostView.android.kt#L188-L259) · [Material 3 Ripple API](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#ripple(kotlin.Boolean,androidx.compose.ui.unit.Dp,androidx.compose.ui.graphics.Color))

## 范围与回归设计

基线分支为 `codex/lithic-ui-v4-icon-navigation-01`。Fetch 后核对该分支和正式 Release、本地及远端，基线工作区干净。工作分支为 `codex/lithic-ui-v4-icon-nav-press-repair-02`。

- [LithicNavigation.kt](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt) 的生产差异限于 `C2PrimaryNavigationBar` 的反馈接线和对应导入。原 210×58dp 胶囊、40dp 选中圆、24dp 图标、边框、阴影、三主题色角色及两处 200ms 选中动画均从基线继承。`Role.Tab`、名称、选中语义、原触控面积、路由、返回和离开守卫沿用原实现。
- [V4PrimaryNavigationPressTest.kt](../../app/src/androidTest/java/com/cooper/lithic/ui/V4PrimaryNavigationPressTest.kt) 验证三主题下实际按住时圆内像素改变、圆外像素稳定，以及释放和取消后恢复。取样忽略圆缘 2dp 抗锯齿区。测试从圆外但仍位于原触控区的位置按住，防止把缩小热区当作修复。取消回调为零，释放时单次回调。
- 同一测试类使用触摸注入检查首页→回看→设置→首页、快速切换与重复点击的单次回调、选中状态和最小 48dp 触控目标。已有页面导航和返回守卫测试均保留。无障碍语义断言与 TalkBack 的真实语音播报分别记录。

## Android 运行状态

本机没有已确认健康且只存可丢弃测试数据的砺石设备。只读检查未发现运行中的模拟器或独立 5038 监听。本阶段抽样可用内存为 4.199 GiB，低于既有 5 GiB 模拟器门槛。上一轮新隔离 AVD 在 180 秒内未能确认系统启动，Repair05 有系统 ANR 记录。此次保持此前场地，不启动模拟器，也不操作小米14或 ADB。[上一轮隔离准入证据](LITHIC-UI-V4-ICON-NAVIGATION-01.md#2026-11-16--android-再准入)

| 端侧项目 | 本轮实际状态 |
| --- | --- |
| 三主题按住、释放、手势取消及圆外像素 | `NOT_RUN`。新测试已编译，本轮实际运行 0 项。 |
| 实机三个入口切换、连续点击、重复点击和返回守卫 | `NOT_RUN`。原测试保留。 |
| Android 触控面积、TalkBack 选中播报、原生截图或录屏 | `NOT_RUN`。本轮无原生画面。 |

端侧总体 `ENVIRONMENT_BLOCKED`。源码与编译物的前后结构来自实际文件，不能作为原生渲染的验证结果。[前后编译字节码及实际编译的测试方法](lithic-ui-v4-icon-nav-press-repair-02/compiled-navigation.txt)

## 桌面执行与 APK

生产修复和新增测试绑定到提交 `98153ef3c44badb44863e0d8394c4fe7284306d9`，App 树 `d49b2bac3b4f398247fc5e5fc7447b81b5477757`。离线 Gradle 在 5.522 GiB 可用内存下启动，以 JDK 17、SDK 36 和单 worker 执行，退出 0，68 个任务实际执行。最终内存复查为 5.404 GiB，设备健康和数据隔离资格仍缺少可确认的目标。

| 检查 | 实际结果 |
| --- | --- |
| JVM 定向回归 | 7 类，47 PASS，0 失败，0 跳过。覆盖 `C2NavigationTest`、`C4NavigationMotionTest`、`C7JourneyNavigationTest`、`M14NavigationPolicyTest`、`P02BrowseNavigationTest`、`P03SettingsNavigationTest`、`C9ThemePaletteTest`。 |
| Kotlin、AndroidTest | `compileDebugKotlin` 与 `compileDebugAndroidTestKotlin` 通过，新增两项 Android 回归实际编译。 |
| Lint、Debug APK | `lintDebug` 0 错误、20 警告，`assembleDebug` 通过。 |
| 真机前后画面 | `NOT_RUN`。本轮保留用户现象描述、源码及已编译前后证据。 |

[构建回执](lithic-ui-v4-icon-nav-press-repair-02/desktop-result.json) · [构建日志](lithic-ui-v4-icon-nav-press-repair-02/desktop-build.log) · [JVM 原始 XML](lithic-ui-v4-icon-nav-press-repair-02/jvm/) · [Lint 原始报告](lithic-ui-v4-icon-nav-press-repair-02/lint-results-debug.txt)

唯一的新 Debug APK 位于 `E:\CODEX\lithic-press-nav\app\build\outputs\apk\debug\app-debug.apk`。

| APK 项目 | 实际值 |
| --- | --- |
| 包名、版本、版本号 | `com.cooper.lithic.debug`、`0.1.0-m22-debug`、17 |
| 大小 | 20,590,822 字节 |
| 新 SHA-256 | `642c60b76e1f97eefb8deb1413866855519203074f59d498e5a8bb370388775c` |
| Debug 证书 SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |
| 验证 | 一个签名，APK v2 验签及 ZIP CRC 通过，Manifest 解码结果与上一版相同。 |
| 历史 APK | 旧 Debug SHA-256 为 `a40e488be31c2e92cd2f10da105c445872118f812fa731383a5ea9c8290c9afb`。正式 1.0.1 为 `498b0c84dd780944e5f76c43b81343d95603cc985a1ed5d2c64e8723e0204342`。两份均复算一致。 |

[APK 身份与哈希](lithic-ui-v4-icon-nav-press-repair-02/apk-identity.json) · [APK 规格](lithic-ui-v4-icon-nav-press-repair-02/apk-badging.txt) · [原样验签](lithic-ui-v4-icon-nav-press-repair-02/apk-signature.txt) · [APK Manifest](lithic-ui-v4-icon-nav-press-repair-02/apk-manifest.txt)

状态 **READY_FOR_INDEPENDENT_REVIEW**。端侧仍 **ENVIRONMENT_BLOCKED**。小米14的修复效果尚未实证。

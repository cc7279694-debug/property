# LITHIC UI V4 · 悬浮图标导航 01

## Git 基线与范围

2026-11-16（Asia/Shanghai）从远端确认的 V4 设计提交 `af612160efca7b08dc3f071ab7fa3bc7cbada32e` 建立 `codex/lithic-ui-v4-icon-navigation-01`。正式 Release 分支 `87d423e7f21722eff851cac445b9ab798ab6996d` 是该设计提交的直接父提交，两者的 App 源码一致。远端 main 起点为 `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`。发布分支当时是干净的，原状态为等待独立发布验收。[V4 设计基线](../design/lithic-ui-v4-taosheng-style-port-01/LITHIC_UI_V4_TAOSHENG_STYLE_PORT_01.md) · [正式 Release 记录](LITHIC-RELEASE-01.md)

实现提交 `ec800702ad0368c9839a624f197d04abd4cba2b8`。随后 `54155598bcabd04f0fb9c885e9352cade2503906` 仅补一个 AndroidTest 导入，最终 App 树为 `5be4adf157641b5cb16e401112046b83f7207c50`。正式配置、Manifest、全局主题、首页代码、数据及媒体代码和 `LithicRoutes.kt` 均与设计基线逐文件相同。App 树差异限于导航视觉组件和三份 AndroidTest。

| 位置 | 变更 |
| --- | --- |
| [LithicNavigation.kt](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt) | `C2PrimaryNavigationBar` 使用居中悬浮胶囊、线性图标、选中动画和可访问语义。 |
| [NavigationShellTest.kt](../../app/src/androidTest/java/com/cooper/lithic/ui/NavigationShellTest.kt) | 原三个页签文字断言替换为真实无障碍名称、`Role.Tab`、无视觉文字的断言，保留入口数量及原行为验证。 |
| [C9ThemeMatrixTest.kt](../../app/src/androidTest/java/com/cooper/lithic/ui/C9ThemeMatrixTest.kt) | 在图标之外分别采样胶囊和选中圆，覆盖三个主题及语义选中状态。 |
| [P2HomeArchiveContractTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/home/P2HomeArchiveContractTest.kt) | 原 18 组主题、宽度、字体测试新增胶囊居中、210×58dp、按顺序相邻的等宽触控区，保留「添加」与内容、底栏不重叠的断言。 |

## 导航规格与行为

外壳宽 210dp，可随可用宽度收窄。48dp 高的触控区配 5dp 上下内边距得到 58dp 外壳。在 320／360／400dp 的可用画幅，三个触控区各约 66.7×48dp。胶囊外侧四边由现有 `Scaffold.bottomBar` 测量，上方间距 4dp，下缘距系统导航区 14dp，左右留边 16dp。仍用现有 `NavigationBarDefaults.windowInsets` 和内容 `consumeWindowInsets` 分配空间。首页「添加」由原 `HomeShell` 独立占位。

复用现有三枚 24dp、1.65 viewport 单位线宽的 VectorDrawable，顺序为房屋、趋势、齿轮。图标的可访问名称「首页／回看／设置」位于可点击的 `Tab` 节点，节点提供选中语义，图标本身为装饰。保留 `tab-now`、`tab-review`、`tab-settings`。选中项 40dp 圆形填底，图标用主题主文字，未选中项用次级文字。选中圆与图标颜色约 200ms，使用 Compose 动画时长比例。原点击合同不触发触觉反馈，本轮沿用。参考涛声的悬浮布局和独立选中面。[涛声 v0.4.0 底栏源码](https://github.com/0xCoach/TaoSheng/blob/v0.4.0/app/src/main/java/app/resound/ui/AppShell.kt#L121-L167)

选中来源仍为 `currentPrimary`。三个 `NavKey` 继续传给 `requestLeave(canonicalBackStack(destination))`，包括重复点击。Nav3、页面状态、返回来源、系统返回、表单未保存和等待操作的决定仍由原路由与守卫执行，恢复中继续由同一资格判断拦截。导航仅在首页／回看／设置挂载。

| 主题 | 胶囊／边框 | 选中圆 | 选中／未选中图标计算对比度 |
| --- | --- | --- | --- |
| 纯白 | `#F7F8FA`／`#E4E7EB` | `#EFF4FA` | 15.46:1／6.26:1 |
| 岩白雾蓝 | `#FFFFFF`／`#D3DEE9` | `#E8EFF6` | 13.05:1／6.96:1 |
| 深色黑灰 | `#1E1E1E`／`#343434` | `#202C39` | 12.78:1／7.86:1 |

边框为 1dp。浅色使用 2dp 阴影，深色用表面和边框分层。颜色取现有主题角色。表中对比度基于色值计算。

## 浏览器画板与 Android 准入

[旧版与新版三主题对比](lithic-ui-v4-icon-navigation-01/visual/new-old-three-themes.png) · [320／360／400 与字体、系统栏占位](lithic-ui-v4-icon-navigation-01/visual/width-font-insets.png) · [浏览器样稿](lithic-ui-v4-icon-navigation-01/visual/browser.html) · [检查结果](lithic-ui-v4-icon-navigation-01/visual/browser-results.json)

Chromium 157 实际验证 36 组布局，组合为三个主题、三种逻辑宽度、CSS 100%／200% 和两类系统栏占位。复核了图标实际资源路径数据、选中圆、位置、触控目标、布局边界与添加操作留位。浏览器可访问名称、Tab 选中属性及键盘、点击验证通过。App 内可访问语义已从源码和 AndroidTest 断言核对，原生朗读仍需 Android 执行。

Android 准入为 `ENVIRONMENT_BLOCKED`。主机只读核对未找到可立即使用且已确认健康的砺石隔离设备。5038 和目标 17640／17641 未监听，5037 监听的 PID 初查为 15168，最终复查为 10636，变化原因未核实，本轮未调用 ADB。两次构建前的物理内存分别为 4.066 和 4.516 GiB，最终复查 3.835 GiB，均低于现有模拟器 5 GiB 门槛。原 API36 Repair05 的历史记录已有 SystemUI、Phone 及 Traceur 系统启动 ANR，本轮启动次数为 0。[历史 Android 证据](LITHIC-UI-V3-Project-Detail-Final-Acceptance-03.md#单次-android-启动与阻断)

| Android 项目 | 本轮结果 |
| --- | --- |
| 路由切换、重复点击、页面与滚动状态、详情返回、Back 及未保存／待处理保护 | `NOT_RUN`，现有导航与守卫回归保留。 |
| 三主题原生截图、选中动画、0 倍动画、320／360／400dp、真实 200% 系统字体 | `NOT_RUN`。 |
| Android 手势、三键、IME、添加按钮与系统安全区真实验证 | `NOT_RUN`。 |
| TalkBack 焦点顺序、朗读和实际选中状态 | `NOT_RUN`。 |

## 桌面验证实录

离线 Gradle 使用 JDK 17、Android SDK 36、Build Tools 36.0.0，单 worker。首轮主机可用 4.066 GiB。生产 Kotlin 编译完成，7 类 JVM 共 **47 PASS，0 FAIL，0 SKIP**。首轮整体退出码 1，原因是新增 AndroidTest 断言缺少 `androidx.compose.ui.test.assert` 导入。补齐导入后保留原断言以及首轮产出的 JVM XML。同一次重跑在 4.516 GiB 可用内存下完成生产 Kotlin 检查、AndroidTest 编译、Debug Lint 和唯一一次 Debug APK 打包，退出码 0。最终 Lint **0 错误、20 警告**。

| JVM 类 | 通过数 |
| --- | ---: |
| C2NavigationTest | 5 |
| C4NavigationMotionTest | 4 |
| C7JourneyNavigationTest | 10 |
| M14NavigationPolicyTest | 5 |
| P02BrowseNavigationTest | 12 |
| P03SettingsNavigationTest | 4 |
| C9ThemePaletteTest | 7 |

GitHub 日志副本仅整理了行尾空白。两次完整原始输出保存在本机 `E:\CODEX\lithic-v4-icon-nav-local`。[首轮失败日志](lithic-ui-v4-icon-navigation-01/desktop/first-build.log) · [首轮命令与结果](lithic-ui-v4-icon-navigation-01/desktop/first-build-receipt.json) · [JVM XML](lithic-ui-v4-icon-navigation-01/desktop/jvm/) · [最终构建日志](lithic-ui-v4-icon-navigation-01/desktop/final-build.log) · [最终回执](lithic-ui-v4-icon-navigation-01/desktop/final-build-receipt.json) · [Lint 原始报告](lithic-ui-v4-icon-navigation-01/desktop/lint-results-debug.txt)

## 唯一 Debug APK

本机路径 `E:\CODEX\lithic-v4-icon-nav\app\build\outputs\apk\debug\app-debug.apk`。

| 项目 | APK 实检 |
| --- | --- |
| 包名／版本 | `com.cooper.lithic.debug`／`0.1.0-m22-debug`／code 17 |
| APK 大小 | 20,590,822 字节 |
| APK SHA-256 | `a40e488be31c2e92cd2f10da105c445872118f812fa731383a5ea9c8290c9afb` |
| Debug 证书 SHA-256 | `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` |
| 签名与字节 | 单一原 Debug 证书，APK v2 验签通过，ZIP CRC 通过。 |
| Manifest 与代码 | 自有 MainActivity，`application-debuggable`，target 36。DEX 中存在 `LithicNavigationKt` 和新胶囊、动画、三个原页签标记。 |
| 构建绑定 | `54155598bcabd04f0fb9c885e9352cade2503906`，App 树 `5be4adf157641b5cb16e401112046b83f7207c50`。 |
| 正式 1.0.1 | 构建前后复算为 `498b0c84dd780944e5f76c43b81343d95603cc985a1ed5d2c64e8723e0204342`。 |

[APK 身份与 SHA](lithic-ui-v4-icon-navigation-01/desktop/apk-identity.json) · [签名原文](lithic-ui-v4-icon-navigation-01/desktop/apk-signature.txt) · [APK 规格](lithic-ui-v4-icon-navigation-01/desktop/apk-badging.txt) · [APK 实际 Manifest](lithic-ui-v4-icon-navigation-01/desktop/apk-manifest.txt)

本轮代码复查确认改动限于底栏视觉、语义与定向断言。构建和静态材料已到 **READY_FOR_INDEPENDENT_REVIEW**。Android 运行状态保留 **ENVIRONMENT_BLOCKED**，原生截图及端侧检查均为 **NOT_RUN**。后续只在可确认的健康隔离环境执行原路由与守卫用例、三主题截图、真实 200% 字体、两种系统导航、IME 与 TalkBack。

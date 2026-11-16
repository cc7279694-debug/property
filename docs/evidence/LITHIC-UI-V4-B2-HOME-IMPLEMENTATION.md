# 砺石 V4 B2 首页生产实施

## Git 基线

Fetch 后核对了 `main`（`145326ddd4ce28abbf7c2706fe25f9d38c599c3a`）、Release 1.0.1（`87d423e7f21722eff851cac445b9ab798ab6996d`）、V4-A 导航（`c3c0b977d7db3142a7231af028d702e369a0907a`）及其按压修复（`b7ca3ea742fcf960fe36eae94a06d4a418a5682a`）。前三个提交都是按压修复基线的祖先，`98153ef3` 圆形按压修复也在该基线中。B1 的 `b09563ea60dfe8fd5c201437b46c4cd60653cf52` 只在同一生产基线上增加设计文件。本轮在独立工作区从 `b7ca3ea7` 创建 `codex/lithic-ui-v4-b2-home-implementation`，B1 文件通过固定提交引用。

已阅读 B1 冻结规范、视觉评审、交互样稿和验证脚本，七张 PNG 的 SHA-256 均与浏览器原始回执一致。[B1 固定设计与验证材料](https://github.com/cc7279694-debug/property/tree/b09563ea60dfe8fd5c201437b46c4cd60653cf52/docs/design/lithic-ui-v4-b1-home-design-freeze)

源码提交 `419a9ed6bccfdaaaf81d075b127d112bfbf2349c`，App 树 `cd7e9a0f7d372b03f290ccc0208bdb8ad2884dd4`。

## 实际修改

| B1 冻结区域 | 生产实施 |
| --- | --- |
| 品牌 | 黑石、砺石和 LITHIC 字标同组居中对齐。搜索与顶部新增保留各自 48dp 目标、能力判断和回调，新增圆底使用首页次级表面。 |
| 账本总览 | 去掉外卡、阴影和竖向分隔，金额直接排在背景上。现有 `MoneyMetric` 以 24sp／28sp 区别总投入／日均，两档比 B1 浏览器 26sp／30sp 各小 2sp，沿用既有金额排版角色。标准宽度优先并排，金额放不下或字体放大时纵排，保有首选字号和完整金额入口。使用原 `OverviewResult` 字段。 |
| 日期、数量和口径 | 观察日移入口径弹窗。正常为「统计日期」，保留结果为「上次结果日期」，有日期时同样出现在信息按钮的无障碍状态中。无快照保持 `—` 且无日期。原历史数量、成本构成、完整计算说明和舍入说明保留。 |
| 我的拥有 | 16dp 横向边距、12dp 双列间距、约 20dp 区段节奏。标题与当前类型的「全部」对齐，项目与订阅用有独立选中面的轻分段轨，仍是互斥 Tab。按既有顺序每类最多两张，单条保留一列，名称自然换行。 |
| 记录卡 | 14dp 圆角及 1dp 细描边，明主题 1dp 阴影，深色靠表面明度。4:3 Crop 封面占卡片宽度。类别、状态与项目持有天数去掉胶囊，项目显示「日均」，订阅显示「历史日均」，读屏分别保留完整指标名与元／天。未知值、负值和零值使用原格式化。 |
| 底部操作 | 正文有独立滚动区，添加按钮单独占位，V4-A 悬浮导航继续由原壳层安放。顶部新增和底部添加继续使用同一菜单的两个回调，关闭也走原路径。 |
| 三主题 | 直接读现有 `background`、`surface`、`surfaceContainer`、`onBackground`、`onSurfaceVariant`、`outlineVariant`、`primary`／`onPrimary`。深色选中轨仅在首页以既有色叠出较亮一档。 |

生产文件为 [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) 和 [CoverPreviews.kt](../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviews.kt)。后者只为原失败按钮提供兼容的可选展示插槽，实现首页可读的「封面暂不可用」文字，保留独立失败语义和状态弹窗。其它页面默认仍使用原失败图标。未关联封面的 Reader 早退、解码与内存预算、Owner、generation、票据和取消流程由原代码负责。

源差异核对涵盖 Room 与迁移、Backup format 4、Money 与 BusinessClock、Repository、Owner 与 generation、项目事件和订阅付款身份、照片读取协议、导航与返回保护、全局主题及两类详情页。上述路径与原生产基线一致。源码独立复核未发现阻断，随后补充了日期状态语义回归断言。

## 桌面和 Android 验证范围

测试改动共八类，保留原 51 个测试方法。更换过时视觉断言时，仍核对原来的数字、入口或业务状态。

| 测试文件 | 核对项 |
| --- | --- |
| `P2HomeArchiveContractTest`、`C2C1HomeVisualContractTest`、`C9ThemeMatrixTest` | 320／360／400dp × 三主题 × 模拟 100%／200%，双列和页面间距，字号纵排，长名称和完整金额，扁平总览的背景色，添加与导航空间隔离。 |
| `P2HomeReadPresentationTest`、`P01HomeShellSemanticsTest`、`HomeOverviewUiTest` | 真实零与未知，正常／空／无当前／失败／旧快照，数量、金额、日期和独立重试，原构成面板正负金额及舍入说明。 |
| `HomeAndroidAcceptanceTest`、`NavigationShellTest` | 真实 Activity 的两类卡片、封面、搜索、构成、两个新增入口和原返回路径。原生截图及系统字体断言仍由 `HomeAndroidAcceptanceTest` 采集。 |

B1 参考截图为[纯白](https://github.com/cc7279694-debug/property/blob/b09563ea60dfe8fd5c201437b46c4cd60653cf52/docs/design/lithic-ui-v4-b1-home-design-freeze/home-white.png)、[岩白雾蓝](https://github.com/cc7279694-debug/property/blob/b09563ea60dfe8fd5c201437b46c4cd60653cf52/docs/design/lithic-ui-v4-b1-home-design-freeze/home-stone-mist.png)及[深色黑灰](https://github.com/cc7279694-debug/property/blob/b09563ea60dfe8fd5c201437b46c4cd60653cf52/docs/design/lithic-ui-v4-b1-home-design-freeze/home-charcoal.png)。这些是浏览器设计画板。B2 没有取得三主题原生截图。

本机只读预检未发现运行中的模拟器或独立 5038 服务。可用内存 4.320 GiB 低于原隔离模拟器的 5 GiB 准入门槛。V4 的新隔离 AVD 曾在 180 秒内达不到启动门禁，Repair05 有系统 ANR 记录。本轮没有启动这些历史环境、操作小米14或执行 ADB。Android 运行环境为 `ENVIRONMENT_BLOCKED`，端侧实际执行 0 项。三主题真实布局、320／360／400dp、系统 200% 字号、图片裁切与异常态、两类预览重试、Nav3 系统返回及 TalkBack 均为 `NOT_RUN`。[历史隔离检查](LITHIC-UI-V4-ICON-NAVIGATION-01.md#2026-11-16--android-再准入)

桌面使用 JDK 17.0.20.1、Android SDK Platform 36、Gradle 8.14.3，以离线单 worker 执行。正式验证开始前可用内存 4.284 GiB。以下任务同一次 Gradle 运行退出 0，共 49 项任务实际执行。

| 任务 | 实际结果 |
| --- | --- |
| `:app:testDebugUnitTest` | 116 类、985 PASS，0 失败、0 错误、0 跳过。包含总投入、日均成本、状态、主题、导航策略和封面预算规则。 |
| `:app:compileDebugKotlin` | PASS，生产 Kotlin 实际编译。 |
| `:app:compileDebugAndroidTestKotlin` | PASS，AndroidTest 源码实际编译。 |
| `:app:lintDebug` | PASS，0 错误／20 警告，分析包含生产、JVM 与 AndroidTest。 |
| `git diff --check` 与保护范围 | PASS，生产差异限于两个列明文件。 |

[桌面构建原始日志](lithic-ui-v4-b2-home-implementation/desktop-verification.log) · [JVM 汇总](lithic-ui-v4-b2-home-implementation/jvm-result.json) · [原始 JVM XML](lithic-ui-v4-b2-home-implementation/jvm/) · [Lint 原始报告](lithic-ui-v4-b2-home-implementation/lint-results-debug.txt)

## 唯一 Debug 交付

桌面门禁通过后，在 5.229 GiB 可用内存下执行 `:app:assembleDebug` 一次。Gradle 退出 0，19 项任务实际执行，21 项复用。该工作区只生成一份 APK。仍无已通过 Android 准入的独立设备，端侧验证由 B3 承接。[唯一构建日志](lithic-ui-v4-b2-home-implementation/assemble-debug.log)

| 审核项 | 结果 |
| --- | --- |
| 本机位置 | `E:\CODEX\lithic-v4-b2-home\app\build\outputs\apk\debug\app-debug.apk` |
| 包名和版本 | `com.cooper.lithic.debug`，`0.1.0-m22-debug`，版本号 `17` |
| 字节数和 SHA-256 | 20,590,822 字节，`eb754ffaff7872f4d70247b1366d378880fe26299dc1a93eb58b8632d6dff88b` |
| 签名 | 单一原 Debug 证书，SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，APK v2 验签通过。 |
| APK 审计 | Debug 身份、APK ZIP CRC 均通过，解码 Manifest 与 V4-A 按压修复版逐行一致。实物 DEX 包含新版首页及金额、日期节点标记。 |
| 既有实物 | 复算原 V4-A Debug 与正式 1.0.1 的 SHA-256，均与原回执一致。 |

[APK 身份回执](lithic-ui-v4-b2-home-implementation/apk-identity.json) · [包规格](lithic-ui-v4-b2-home-implementation/apk-badging.txt) · [签名原始结果](lithic-ui-v4-b2-home-implementation/apk-signature.txt) · [Manifest](lithic-ui-v4-b2-home-implementation/apk-manifest.txt)

**状态 `READY_FOR_DEVICE_ACCEPTANCE`，限桌面和构建门禁。** 独立交付分支为 `codex/lithic-ui-v4-b2-home-implementation`。APK 对应上列源码提交，报告和原始回执随证据提交推送。通过 `git ls-remote` 核对远端交付提交及受保护分支，本地工作区保持干净。等待 ChatGPT 独立审查，Android 实际结果待 B3。

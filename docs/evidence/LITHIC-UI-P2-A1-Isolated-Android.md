# LITHIC P2-A1 隔离 Android 验证

**P2_A1_DEVICE_EVIDENCE_READY_FOR_REVIEW** · 2026-11-11。取得隔离 Android 运行证据，回归和最终视觉验收仍有待复核。

## 基线与安全准入

- 开始时 `codex/lithic-ui-p2-home` 本地、远端均为 `3bfed238845b65ca421942fc03845cbe93d9d9fa`，工作区干净。P2 桌面结论按用户转交记录为 `P2_DESKTOP_ACCEPTED — PASS WITH NOTES`。候选源码提交为 `8224ce5149f304a022a6f8d7895e8bbb3595e3c3`，根树 `a1461dbb8a5c659635118539627516383c37bfee`，生产 `app/src/main` 树仍为 `6f1553b0ae5b2137cfb327263e30fb41769e4845`。
- 复用 M13 测试启动机制，增加显式 `-Pp2A1Verification=true`、独立 Debug App/Test 及输出目录。App 的 Manifest 采用普通 `android.app.Application`，只注册私有 `ComponentActivity`，禁止备份。没有 Launcher、生产入口、Startup Provider、Service 或 Receiver。三主题截图测试属于专用 androidTest 源集，使用内存合成事实及色块 PNG，截图存入该测试包下新建的 UUID 目录。已有 Nav3 测试使用独立合成存储根。[隔离构建](../../app/build.gradle.kts) · [Manifest](../../app/src/p2A1/AndroidManifest.xml) · [截图测试](../../app/src/p2A1/androidTest/java/com/cooper/lithic/ui/P2A1HomeGalleryTest.kt)
- 主机原有 ADB 服务的只读枚举开始时为 0 台设备。排除含已有数据的旧 AVD 和历史 C3 身份，C3 超时的原记录保留。使用现成本地 AOSP 镜像新建一次性 `Lithic_P2A1_API36`。单次冷启动约 138 秒，API 36 / x86_64，SystemUI PID 1027 在入场前后及结束观察一致，有界事件查询无 ANR 记录。入场时可用内存 1,613,364 KiB，数据分区可用 5,613,264 KiB。两个精确目标包起初均不存在，各安装一次，唯一 Runner 绑定与运行中的 App/Test UID 区隔检查通过。
- 所有设备命令只发给新建模拟器 `emulator-5588`。没有连接或操作小米14，也没有启动历史模拟器、安装或覆盖用户包、访问个人账本。模拟器内的合成证据保留。正式 APK 本机核验仍为 15,747,081 字节，SHA-256 `ff909ca1eb21e04756d91d067082ca12df1ac17c263d793843fbbf4c67e40c2b`。生产源码、Room schema、签名与正式发行配置差异为零。

## 构建及产物

离线 Gradle 8.14.3、JDK 17、SDK 36，运行 `:app:assembleDebug :app:assembleDebugAndroidTest`（独立开关、单 worker）。生产和测试 Kotlin 编译、两包打包均完成，退出 0，73 个任务实际执行，6 分 48 秒。本轮 JVM、Lint `NOT_RUN`，历史桌面证据沿用原范围。

两包在 `E:/CODEX/lithic-ui-p2-a1-evidence/candidate/`，归档后逐字节核对一致。签名均通过 APK v2 验证，单一原 Debug 证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。App 唯一权限为本包作用域的签名权限。Test 唯一请求权限为测试框架的 `REORDER_TASKS`，唯一 Instrumentation 为 `com.cooper.lithic.app.M13IsolatedRunner`，指向该 App。DEX 核对截图类和 Runner 仅在 Test，Runner 使用平台 Application。

| 归档文件 | 包名与版本 | 字节 | SHA-256 |
| --- | --- | ---: | --- |
| `P2-A1-App.apk` | `com.cooper.lithic.p2a1.debug` · `0.1.0-p2a1-debug` / code1 | 20,505,139 | `0e7bac22701987c743b31839747be7b3b515fe2bee8037f5f1005369ecdd7f8d` |
| `P2-A1-Test.apk` | `com.cooper.lithic.p2a1.debug.test` · Manifest 无显式版本 | 3,994,499 | `ff61304963f2a35401eaca34335f4ba0ad652228ff88509b40dc9c594db22537` |

本机证据根为 `E:/CODEX/lithic-ui-p2-a1-evidence/`。保留构建及退出码、设备准入与安装日志、每条原始 Instrumentation 输出、`static-audit-3/identity.json` 和 `source-binding.json`。第一次静态审计器误要求本轮未安排的 Lint 回执，第二次误解析 AAPT2 的 SDK 属性编码。两次失败及第三次成功证据均留存，APK 字节始终一致。

## 原生截图与 R2.1 对照

三张常规图为 Android 隔离 Activity 的生产 Compose 根节点，实测 **360dp / 320dpi / fontScale 1.0 / 720×1648px**。用同一组合成账本观察主题、信息层级和封面。窗口顶部和底部有测试宿主的系统栏留白，深色图底边为浅色，正式 MainActivity 的系统栏实际效果未验。[岩白雾蓝](lithic-ui-p2-a1/home-stoneMist.png) · [纯白](lithic-ui-p2-a1/home-pureWhite.png) · [深色黑灰](lithic-ui-p2-a1/home-charcoal.png)

| 对照 R2.1 | 实际观察 |
| --- | --- |
| 首页层级和色彩 | 保持成本、项目、订阅、含历史数量、类别构成的顺序。雾蓝页首、纯白内缩分隔及黑灰表面明度可区分。Android 实际档案行比画板高，状态与持有/付费天数换成两行，构成入口更靠近底栏。三个主题中主金额和单位均同行。 |
| 4:3 封面 | 80×60dp 框内横图上下留边、竖图左右留边。两种方向都通过既有 Fit/Crop 像素断言。合成竖图用于验证订阅行可接收封面，画板示例订阅使用类型图标。无封面、封面读取失败分别有组件测试。 |
| 窄屏和大字 | 320/360/400dp、宽窗口、长名称及金额在受控 Compose 测试中检查。另在本次全新模拟器将系统字体设为 200%，Activity 回报 `fontScale=2.0`，取证后恢复并读回 1.0。实际 360dp 截图中，成本和项目金额单位仍可读，日期末字换行，订阅区「全部」在该滚动位置被圆形新增按钮遮住一部分。该位置的视觉冲突需独立复核。[系统 200% 原图](lithic-ui-p2-a1/home-stoneMist-system200.png) |
| 读取状态 | A–H 使用合成组件或受控 Ready 状态验证，包含历史真实零、确认空账本、旧快照和无快照。H 保留原 P2 批准约束，省去无事实支撑的截止日期及「非今日扣款」。历史冷启动失败和保护协议保留原证据范围。 |

[固定 R2.1 三张画板](https://github.com/cc7279694-debug/property/tree/1b688bc1756dc3f42fcd3a3c0e57732d56023f34/docs/design/lithic-ui-r2-1-personal-archive/mockups)

## Instrumentation 与停止点

17 次有界调用均取得 JUnit 结尾。62 次方法执行为 **46 PASS / 16 FAIL**，共 61 个不同方法，岩白雾蓝截图在 200% 系统字号下另运行一次。所有失败保留原断言、退出状态和堆栈，失败方法没有重跑。[逐次原始输出](lithic-ui-p2-a1/instrumentation.txt)

| 范围 | PASS | FAIL |
| --- | ---: | ---: |
| 专用启动检查和三主题截图（含一次系统 200%） | 5 | 0 |
| `P2HomeArchiveContractTest`、`P01HomeShellSemanticsTest` | 8 | 0 |
| `CoverPreviewsTest` | 7 | 0 |
| `P2HomeReadPresentationTest`、`StorageUiStateTest` 受控 H | 5 | 0 |
| `C9R1HomeHierarchyTest`、`C9ThemeMatrixTest`、`C9RecordThemeTest` | 7 | 3 |
| `NavigationShellTest` | 10 | 5 |
| `ProjectNavigationGuardTest`、`SubscriptionNavigationGuardTest` | 3 | 8 |
| 设置恢复进行中互斥的既有方法 | 1 | 0 |

- 三个 C9 失败断言把 `¥` 或 `−¥` 当成完整节点文本匹配。实际输出包含 `¥9.92/天`、`−¥9999998711.99`，本机 Compose Test 断言字节码核对默认 `substring=false`。这三项仍保留 FAIL。
- Nav3 的系统返回用例已通过。五项失败涉及空账本/数量旧文案、次级工具栏返回测试的底栏选中前置断言、项目结束日期展示、设置备份入口选择器。表单守卫八项失败集中在未找到新增菜单或记账入口，部分关键断言未到达，写入和离开保护不能据此结案。
- `NOT_RUN` 包括 M09 订阅守卫剩余 3 项、其他未列出的 P2-2/P2-3 测试、生产 MainActivity 全程、人工体验、TalkBack 朗读与焦点、真实键盘与 Insets、实际较宽/横屏、真实备份恢复和小米14。新 AOSP 镜像未发现 TalkBack 包。首页入口分发、局部大字和隔离 Nav3 与生产用户旅程分别记录。

提交本轮运行证据，等待 ChatGPT 独立验收。

# LITHIC P2-1 首页视觉基础与结构

**状态** `P2-1 READY_FOR_INDEPENDENT_REVIEW`（桌面验证）。**日期** 2026-11-11（Asia/Shanghai）。

## 基线与文件

- 独立分支 `codex/lithic-ui-p2-home` 从正式交付源码 `d42a78125b83bee806add5e3fdd8d2328055b694` 创建。创建前核对正式分支和 R2.1 设计分支的本地及远端引用。二者原 App 树相同，均为 `39295c800ceefdeee19aefce39b26755d3b8d63a`。
- [已接受的 R2.1 设计](https://github.com/cc7279694-debug/property/tree/1b688bc1756dc3f42fcd3a3c0e57732d56023f34/docs/design/lithic-ui-r2-1-personal-archive) · [正式交付记录](LITHIC-V1-Private-APK-Release.md)。已审本地计划 `P2_IMPLEMENTATION_PLAN.md` 的 SHA-256 为 `eedee12816a57573931b9416ca590d7f34dfbdea22645ddc590969a98840ec0a`。
- 唯一生产改动为 [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt)，本报告为新增文档。原设计工作树中的计划和已签署交付工作树各自保留。

## 实现与影响范围

- 使用已有黑石资源及「砺石」标题，搜索维持原 48dp 入口。品牌、当前日均、日期和口径入口组成连续页首。金额由现有 `MoneyMetric` 渲染，信息按钮和金额分别占行，为长金额留出整行。
- 岩白雾蓝和黑灰沿用首页既有明度层，纯白用内缩细线及首页主金额局部加重区分层次。公用主题、Typography、Shapes、`LithicSpacing` 与三主题存储文件均无差异。
- 项目和订阅预览之后呈现「已录入 · 含历史」文字摘要。保留原快照计数、未知和保留旧值的语义，按宽度和字体条件顺排或堆叠。成本与类别构成、计算说明、56dp 唯一新增及底栏沿原调用路径。
- 原 22 个 `HomeShell` 测试标记全部保留，分项、预览和类别组件实现与基线逐字一致。生产差异核验覆盖 App、Gradle、Manifest、数据、测试、导航和签名配置。Android 资源树和正式交付分支均无改动。

## 验证回执

在独立工作树用现有 JDK 17.0.20.1、SDK 36 和缓存的 Gradle 8.14.3。命令共用 `--offline --no-daemon --console=plain --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1536m -Pkotlin.compiler.execution.strategy=in-process`。编译内存通过单次进程参数控制，Gradle 项目配置未改。正式签署入口保持关闭。

- 正式基线 `testDebugUnitTest`，退出 0，**25 PASS / 0 FAIL / 0 skipped**，耗时 5m 26s。
- 实施后以 `testDebugUnitTest --rerun` 执行相同四类，**25 PASS / 0 FAIL / 0 skipped**。生产 Kotlin、AndroidTest Kotlin 编译和完整 Debug Lint 同轮完成，Gradle 退出 0，耗时 13m 51s。Lint XML **0 错误、20 警告**，`HomeShell.kt` 无定位项。该工作树的 Gradle 输出 APK 数量为 0。
- 两轮 JUnit 实际分布为 `C9ThemePaletteTest` 5、`C9R1VisualTokensTest` 3、`AppearanceControllerTest` 14、`DataStoreAppearanceStorageTest` 3。原 XML 分别在 `baseline-junit/` 和 `p21-junit/`。
- 启动前一次按历史资源值复测时仅 3,183,112 KiB，当次未启动 Gradle。实际基线和复跑分别为 3,252,980 与 3,420,640 KiB，采用上述较低堆和同进程编译完成，原测试和 Lint 范围均保留。

完整命令、退出码、编译告警和 Gradle 原始输出保留在本机 `E:/CODEX/lithic-ui-p2-1-evidence/`。实际验证的 `HomeShell.kt` Git blob 为 `2540b74e5e0a70b588518c1f1002d0ab45bc19a6`，工作树原字节 SHA-256 为 `35298523a1506f0a0c1d8bcd277f7e84a167f4ca842bf9b531b3caaeeceb061d`。

| 原始文件 | 字节数 | SHA-256 |
| --- | ---: | --- |
| `baseline-jvm.log` | 7113 | `a91b6f08d1ee359143e1a7224fab3802bd3ed56c0ecc5429c5391c3eff1233c8` |
| `p21-verify.log` | 18862 | `915ebb202a3c84206b57d98dd0ac12c0d353261b7c416193cbaf5011dbf4fa7c` |
| `lint-results-debug.xml` | 22662 | `feb6a44d197a338ed32b62e4325f766f0fe199bbbcaad7e677c12d352c7df396` |

独立代码复核检查了本包的生产差异、主题局部作用域、原数据和操作路径，未发现阻止交付的问题。该复核仅为代码证据，ChatGPT 独立验收待进行。

## 未验证事项与停止点

- `C9R1HomeHierarchyTest`、`C9ThemeMatrixTest`、`C9RecordThemeTest` 共 10 项 Android 方法实际运行 **NOT_RUN**。无遮挡真机画面、320/360/400dp 的真实布局、系统 200% 字体、TalkBack、Insets、预测返回和冷启动性能均待端侧证据。
- 旧 `C2C1HomeVisualContractTest` 的黑灰取色期望取页面背景，已交付源码使用较亮 `brandSurface`。这是基线静态差异，本轮保留旧断言和历史结果。若后续调整，只按 P2-4 审查已接受的视觉期望，金额、状态及尺寸断言继续保留。
- 档案行及照片 Fit 属于 P2-2，G/H 读状态呈现属于 P2-3。冷启动 `LedgerUiState.Failed` 和 `StorageGate` 沿用原路径。

本工作包停在独立验收点。后续工作包按已授权顺序等待独立结论。

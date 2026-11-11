# LITHIC P2-3 首页读取状态与重试

**状态** `P2-3 READY_FOR_INDEPENDENT_REVIEW`（桌面）。**日期** 2026-11-11（Asia/Shanghai）。

## 基线和范围

本地及远端 `codex/lithic-ui-p2-home` 实施前同为 `bdcf7e1c0f7661127b1575e12596e157a661e3e9`，工作区干净。用户转达 [P2-2](LITHIC-UI-P2-2-Home-Archive.md) 的独立结论为 ACCEPTED — PASS WITH NOTES。其 Android NOT_RUN 保留。验证所用的 Git App 树为 `30d48e41c57b1a36ee876f6249b8b50e37c45599`。实施依据为已审本地 `P2_IMPLEMENTATION_PLAN.md`，SHA-256 `eedee12816a57573931b9416ca590d7f34dfbdea22645ddc590969a98840ec0a`。

| 生产文件 | 改动 |
| --- | --- |
| [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) | 成本标题区区分等待、未确认、旧快照和无快照失败；两组预览分别显示自己的重试按钮。原快照继续提供金额、日期和两类计数。 |
| [HomeOverviewPresentation.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeOverviewPresentation.kt) | 首页专用读取状态映射。保留原 `homeOverviewStatus` 及 `c2HomeIsEmpty` 与 `c2PreviewStatus` 的判定。 |
| [LithicNavigation.kt](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt) | 仅首页注入点传递 `Ready.readError`，分接两组重试。原全局 `ledger-retry-read` 和错误详情位置及回调保留。 |

`Ready.readError` 同时承载恢复警告和刷新失败。首页映射只识别现有发布端附在末尾的明确刷新失败句，以及受控无快照夹具的简短失败句。警告本身不构成失败或过期证据，原全局错误和恢复反馈照常显示。该识别依赖发布端的现有文案，纯 JVM 测试覆盖当前失败句、恢复警告及两者并存。原 `hasSnapshotNotice` 继续覆盖无快照、警告、恢复中和恢复结果未知，并守住真正空账本的条件。

## A–H 合成场景

| 状态 | 呈现与证据 |
| --- | --- |
| A 正常有记录 | 继续从同一 `OverviewResult` 取金额、日期、登记总数，两组预览分别遵守原日期与发布规则。沿用 `HomeOverviewUiTest`。 |
| B 无封面 | 档案行使用原图标补位路径，已有 P2-2 的无引用零读取断言。 |
| C 封面不可用 | 原独立失败说明、详情与封面读取隔离沿用 P2-2 证据。 |
| D 多条 | 既有各两条、原顺序及两种成本口径沿用，实际登记总数来自账本快照。 |
| E 只有历史 | 当前日均为真实 `¥0.00/天`，历史登记数量、两组「全部」及构成保留。 |
| F 确认空账本 | 金额 `—`、已确认 0/0。显示新增引导，保留唯一 FAB，隐藏两组预览及构成。 |
| G 失败有旧快照 | 保留真实金额、截止日与两类数量，标「上次取得的结果」，原全局重试可达。预览仅在自身读失败时标记保留。 |
| H 受控 Ready 失败无快照 | 显示「暂时无法取得成本数据」，金额及两类数量均为 `—`，省略截止日和「非今日扣款」，隐藏首条引导。仅合成 `Ready` 可达，走现有全局账本重试。 |

`LedgerUiState.Failed`、`StorageGate`、真实冷启动、Owner 初始化、领域计算、数据存储与图片层均与基线一致。正常生产 `Ready` 仍在读到 `overview` 后发布，真实初读失败继续走保护页。

## 测试变更

- [P01HomeOverviewPresentationTest.kt](../../app/src/test/java/com/cooper/lithic/feature/home/P01HomeOverviewPresentationTest.kt) 新增五项映射断言，涵盖无快照等待、H、恢复告警、旧快照及空态守卫。
- [P2HomeReadPresentationTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/home/P2HomeReadPresentationTest.kt) 新增四项合成 Compose 测试，覆盖 G、恢复警告保护、历史零与确认空账本、分组预览重试。
- [StorageUiStateTest.kt](../../app/src/androidTest/java/com/cooper/lithic/ui/StorageUiStateTest.kt) 新增一个受控 `Ready.copy(overview = null, readError = …)` 场景，通过真实首页注入验证 H 的未知值与唯一 `ledger-retry-read`，沿用隔离测试存储夹具。原保护页断言不变。
- [C2HomeClosureTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/home/C2HomeClosureTest.kt) 和 [C2C1HomeVisualContractTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/home/C2C1HomeVisualContractTest.kt) 的旧快照用例补入明确失败夹具，原断言未弱化。P2-1 已登记的旧黑灰取色期望差异保留。

## 独立复核与重试接线

独立复核发现，原通用 VM `retryRead()` 也处理编辑冲突。项目冲突删除路径可调用已保存的提交 envelope，不能用于首页的纯预览操作。最终接线改用进入首页时已有的两个 `loadList()`，每个按钮只调用对应 VM 的列表刷新，保留编辑页原重试合同。账本按钮继续走唯一 `onRetryLedger`，本轮未移动或增加账本重试控件。

对最终源码重新做接线核对，确认 Navigation 除 Home 注入点外逐字一致（统一换行），两个 VM 原实现逐字一致。核对生产实际刷新失败句及五条恢复警告，五条单独出现均不会触发 G/H，任一警告与真正刷新失败并存时均识别为失败。原始核对见 `source-wiring.txt`。


## 桌面验证结果

使用已有 JDK 17.0.20.1、SDK 36、Gradle 8.14.3 和离线缓存。命令以单 worker、进程内 Kotlin 编译运行。固定起点用 1 GiB 堆，最终验证用 1.5 GiB 堆。原命令及启动时内存值在各日志首部。

| JVM 测试类 | 基线 PASS | 最终 PASS |
| --- | ---: | ---: |
| `C2HomePresentationTest` | 5 | 5 |
| `P01HomeOverviewPresentationTest` | 6 | 11 |
| `R1HomeEmptyStateTest` | 3 | 3 |
| 合计 | 14 | 19 |

两次实际 JUnit 运行均为 0 失败、0 错误、0 跳过。最终 XML 保留所有基线方法，新增五项通过。实现前新增用例因映射 API 尚不存在，在 `compileDebugUnitTestKotlin` 退出 1，未执行断言。独立复核提出接线问题后，主动中止初次实施验证，保留原始输出和中止记录。随后用最终接线完整复验。

最终运行 `testDebugUnitTest --rerun`（上述三类）、`compileDebugKotlin`、`compileDebugAndroidTestKotlin` 与 `lintDebug`，Gradle 退出 0，耗时 9m 36s。生产、AndroidTest 和单元测试的 Lint 分析均执行，报告字节与 P2-2 一致，**0 错误、20 条既有警告**。

原始命令、退出码、两轮 JUnit XML、Lint XML 与接线回执保存在本机 `E:/CODEX/lithic-ui-p2-3-evidence/`。JUnit XML 分别在 `baseline-junit/` 与 `p23-junit/`。

| 原始证据 | 字节 | SHA-256 |
| --- | ---: | --- |
| `baseline-jvm.log` | 2850 | `b259789e408888a3a38e36b4c7d14aa2fe6dc13302f96fb816727d5a21f0e63f` |
| `red-jvm.log` | 6484 | `8870d78120294a479867096e3a40369e3a3e253cf898d01edb1d07619c62efde` |
| `p23-verify.log` | 6604 | `36d5b42472ea9837859e76a3240a6b4d9ef07bd4c4607a1fd5d726325f7e2798` |
| `preliminary-cancellation.txt` | 206 | `2ebb6fe784e9343820ab8df3d6d1fcb843a07d8b1c69b7ad447403e3d96d53cb` |
| `p23-final.log` | 19422 | `40a55d203046f546c5d5e81a02b828949d600276500b9e7263c2d58d2180a339` |
| `lint-results-debug.xml` | 22662 | `feb6a44d197a338ed32b62e4325f766f0fe199bbbcaad7e677c12d352c7df396` |
| `source-wiring.txt` | 417 | `676d35a9891cd889b7e1e5c17c4f6b64ac28a5fc7d9e49c16dcfa976771a3e06` |
| `android-environment.txt` | 98 | `ac2ad3437344c3e050e32e59cc05cdc4f17d6e32407cbc24580d6e77ebbafd54` |

## 端侧缺口与停止点

主机级检查返回 `host:devices OKAY; total=0; emulator=0`，退出 0。未发现正在运行的 emulator/QEMU。历史或未知 AVD 不启动，本轮无手机、设备测试、账本操作或 APK 产物。

| Android 回归范围 | 方法数 | 实际执行 |
| --- | ---: | --- |
| `C2C1HomeVisualContractTest` | 8 | NOT_RUN |
| `C2HomeClosureTest` | 7 | NOT_RUN |
| `HomeOverviewUiTest` | 6 | NOT_RUN |
| `P2HomeReadPresentationTest` | 4 | NOT_RUN |
| `M13HomeIntegrationTest` 读取失败保留、迟到旧代丢弃 | 2 | NOT_RUN |
| `StorageUiStateTest` 合成 H、首次初始化失败、数据库读取失败、设置读取失败 | 4 | NOT_RUN |
| 合计 | 31 | 0 执行 |

实际 Android 合成场景、冷启动保护、三种重试点击和窄屏全局警告高度仍待设备证据。P2-1 和 P2-2 的原有 NOT_RUN 保留。停在 P2-3，等待 ChatGPT 独立验收。

# LITHIC P2-2 首页档案行与封面

**状态** `P2-2 READY_FOR_INDEPENDENT_REVIEW`（桌面）。**日期** 2026-11-11（Asia/Shanghai）。

## 基线

固定生产基线为 `f394c3c46a3acdb90b959d118c0f76bb7d4094f8`。本地与远端分支 `codex/lithic-ui-p2-home` 执行前同为 `61c40fed207761580f84ae6c22a3832b3d3fb013`，工作区干净。两者间唯一差异是 [P2-1 补验记录](LITHIC-UI-P2-1-Isolated-Acceptance.md)。用户转达 P2-1 独立验收为 ACCEPTED — PASS WITH NOTES，端侧缺口仍为 NOT_RUN。

实施依据为已审计划 `P2_IMPLEMENTATION_PLAN.md`，本地文件 SHA-256 为 `eedee12816a57573931b9416ca590d7f34dfbdea22645ddc590969a98840ec0a`。实际查看了 [R2.1 封面、记录与大字画板](https://github.com/cc7279694-debug/property/blob/1b688bc1756dc3f42fcd3a3c0e57732d56023f34/docs/design/lithic-ui-r2-1-personal-archive/mockups/board-02-cover-and-records.png)。本轮验证的 Git App 树为 `4c676bd8e25218fc20b394855611a8e2c30904a7`。

## 源码与测试差异

| 文件 | 改动 |
| --- | --- |
| [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) | 两组使用同一档案行，保持原快照、发布资格、顺序、每类最多两条及详情回调。 |
| [HomeArchiveRow.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeArchiveRow.kt) | 新增首页专用档案行。展示名称、真实类别、状态及项目持有天数或订阅折算付费天数。正常宽度使用 80×60dp 封面框，极窄约束可缩至 64×48dp，保持 4:3。无引用直接显示类型图标，不进入读取组件；关联失败使用原封面状态操作，和打开详情各自可点。 |
| [CoverPreviews.kt](../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviews.kt) | 新增默认 `Crop` 的参数，只有首页明确传入 `Fit`。参数只传入 `Image`。核对四个列表和两个详情的调用仍走默认值，调用文件逐字未变。 |
| [P2HomeArchiveRowTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/home/P2HomeArchiveRowTest.kt) | 三项合成测试覆盖顺序与上限、不同成本、无封面零读取、错误与详情互不误触、局部 2.0 字体及长名称和长金额。 |
| [CoverPreviewsTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/media/CoverPreviewsTest.kt) | 新增横竖合成 PNG，通过生产档案行与默认封面组件比较四边和裁切；原五项断言保留。 |

项目使用原日均值，显示「项目日均成本」，项目条目内保留原「当前日均成本」朗读标签及原测试。订阅用原历史平均值，显示与朗读均为「历史平均日成本」。金额和完整指标标签按实际字形测量，窄屏或大字时整组下移，极长金额继续走 `MoneyMetric` 的横向查看及完整金额入口。保留 22 个首页固定标签、5 处变量标签和唯一 FAB。

独立代码复核确认，Reader、Owner、generation、票据、采样、序列读取、像素预算、取消与失败隔离均沿原路径。数据层、图片存储、导入、清理、导航与发行配置无差异。

## 横图、竖图与 Crop 证据

使用经原始 AAR 字节核对的 Compose UI 1.9.5 实现运行 JDK 17 几何探针。以下尺寸以 80×60 框为单位，四项断言均通过，退出 0。

| 合成图 | 首页 Fit | 默认 Crop |
| --- | --- | --- |
| 横图 48×24 | 80×40，完整居中，上下各留 10 | 120×60，超出框的左右部分裁切 |
| 竖图 24×48 | 30×60，完整居中，左右各留 25 | 80×160，超出框的上下部分裁切 |

Android 新增用例会直接捕获生产档案行，核对横竖四边颜色和留边，并与默认裁切对照。这两项实际运行仍为 NOT_RUN。桌面结果证明等比缩放几何，真实像素、布局、设备性能与无障碍仍待端侧验收。

## 验证

JDK 17.0.20.1、SDK 36、Gradle 8.14.3，离线普通 Debug，单 worker、1.5 GiB JVM 堆。首次基线命令因环境缺少 `JAVA_HOME` 以 9009 结束，保留原日志，指定已有本地 JDK 和 SDK 后重跑。


| 定向 JVM | 固定起点 PASS | P2-2 PASS |
| --- | ---: | ---: |
| `CoverPreviewPolicyTest` | 6 | 6 |
| `CoverPreviewPipelineTest` | 2 | 2 |
| `C5MoneyMetricPresentationTest` | 8 | 8 |
| `R1AmountLayoutTest` | 8 | 8 |
| 合计 | 24 | 24 |

两轮 JUnit XML 的方法集合一致，各为 0 失败、0 错误、0 跳过。基线复跑 Gradle 退出 0。最终命令执行 `testDebugUnitTest --rerun`（上述四类）、`compileDebugKotlin`、`compileDebugAndroidTestKotlin` 和 `lintDebug`，退出 0，耗时 11m 29s。三种 Debug Lint 分析均执行，报告内容与 P2-1 相同，**0 错误、20 警告**，本包改动的 Kotlin 文件无定位项。

原始命令、退出码、编译输出、JUnit XML（`baseline-junit/`、`p22-junit/`）、Lint XML、环境回执及几何脚本保存在本机 `E:/CODEX/lithic-ui-p2-2-evidence/`。

| 原始证据 | 字节 | SHA-256 |
| --- | ---: | --- |
| `baseline-jvm.log`（缺少 JAVA_HOME） | 723 | `8841057c2e83c9f6e1a1250bbcd25f08aa121bd23df5887d89d20a3a780af481` |
| `baseline-jvm-r2.log` | 2910 | `f6f8a0417807cbbba103a7487cb309b1bfa1ed8574ccd8cec408242004942bf7` |
| `p22-verify.log` | 19114 | `ae658358749fdedbb0fbf5825efe0854b3b781ac37c7927010dfd4986fc68c18` |
| `lint-results-debug.xml` | 22662 | `feb6a44d197a338ed32b62e4325f766f0fe199bbbcaad7e677c12d352c7df396` |
| `geometry/content-scale-probe.log` | 975 | `607d7a1950475b8f139fc283ada2936bdf225ca9211621f553d4c558742054ff` |
| `android-environment.txt` | 98 | `ac2ad3437344c3e050e32e59cc05cdc4f17d6e32407cbc24580d6e77ebbafd54` |

## 未验证事项与停止点

现有本机服务的主机级枚举退出 0，返回 `host:devices OKAY; total=0; emulator=0`。未发现正在运行的 emulator/QEMU，现有历史或未确认 AVD 不符合准入。未启动模拟器、操作手机或访问账本，本工作树构建输出中的 APK 数为 0。

| Android 测试类 | 方法数 | 实际执行 |
| --- | ---: | --- |
| `CoverPreviewsTest` | 7 | NOT_RUN |
| `C6CoverPageWiringTest` | 2 | NOT_RUN |
| `C4HomePreviewSemanticsTest` | 2 | NOT_RUN |
| `P2HomeArchiveRowTest` | 3 | NOT_RUN |

四类实际执行 0/14。横图、竖图和其他页面实际像素，320/360/400dp 与系统 200% 字体的真实布局，TalkBack、点击焦点，以及四张封面同时进入时的端侧资源表现仍待验证。P2-1 的 Android NOT_RUN 继续保留。停在 P2-2，等待 ChatGPT 独立验收。

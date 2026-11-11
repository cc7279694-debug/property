# LITHIC P2-4 首页无障碍、状态与导航集中回归

**状态** `P2_READY_FOR_FINAL_INDEPENDENT_REVIEW`（桌面）。**日期** 2026-11-11（Asia/Shanghai）。

实施前工作区干净，本地和远端 `codex/lithic-ui-p2-home` 同为 `9048190f6dacf039f151cb85fd56b4855e6b7a4b`。用户转达 [P2-3](LITHIC-UI-P2-3-Home-Read-Presentation.md) 源码及已报告桌面验证获 ACCEPTED — PASS WITH NOTES。验证所用 App 树为 `26dfe7fd260f077ef11e5501ff5f4918236b71fa`。适用的已审 `P2_IMPLEMENTATION_PLAN.md` SHA-256 为 `eedee12816a57573931b9416ca590d7f34dfbdea22645ddc590969a98840ec0a`。

## 本轮实现与断言迁移

- [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) 以组件实际可用宽度决定 16/24dp 页边距。宽窗口以 560dp 为单列阅读上限，居中排列，并让 FAB 与该列右缘对齐。两个可见「全部」分别获得「查看全部长期项目」和「查看全部订阅服务」语义。
- [HomeArchiveRow.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeArchiveRow.kt) 让项目行的可见及朗读标签统一为「项目日均成本」，区别于账本「当前日均成本」及订阅「历史平均日成本」。金额、符号、单位和原导航回调不变。
- [P2HomeReadSourceContractTest.kt](../../app/src/test/java/com/cooper/lithic/feature/home/P2HomeReadSourceContractTest.kt) 直接读取真实生产源码中的错误常量、组合分隔符、两个失败赋值点与恢复警告。把原值和组合送入实际首页状态映射，校验 G、H 与恢复警告的区别。源代码变更导致提取失败也会要求重新审查。测试未另抄生产错误文案，未改 Holder 或冷启动架构。
- [C9ThemePaletteTest.kt](../../app/src/test/java/com/cooper/lithic/core/design/C9ThemePaletteTest.kt) 用生产色值新增每主题八个首页文本与操作色对的对比度断言。现有主题、颜色、金额和安全测试保留。
- [P2HomeArchiveContractTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/home/P2HomeArchiveContractTest.kt) 新增六项合成组件测试。覆盖 A–H、恢复结果未知和空快照、错日与单组失败、320/360/400dp × 1.0/2.0 局部字体 × 三主题、宽窗口与 FAB、首页入口及底栏事件分发。H 的实际合成 Ready 注入仍由 P2-3 的 `StorageUiStateTest` 覆盖。独立的既有 Nav3 守卫用例继续覆盖真正返回、未保存输入、未确认写入及恢复保护。

| 旧断言 | 精确迁移理由 |
| --- | --- |
| [C2C1HomeVisualContractTest](../../app/src/androidTest/java/com/cooper/lithic/feature/home/C2C1HomeVisualContractTest.kt) 黑灰摘要取色 | R2.1 接受较亮的黑灰页首。期望从底色改为 `brandSurface`，实际色值从 `#121212` 对照到 `#1E1E1E`。RGB 取样、容差、摘要紧凑、日期、完整金额、滚动、FAB 和真实 0 断言保留。主题的固定色值另有 JVM 断言。 |
| [C4HomePreviewSemanticsTest](../../app/src/androidTest/java/com/cooper/lithic/feature/home/C4HomePreviewSemanticsTest.kt)、[P2HomeArchiveRowTest](../../app/src/androidTest/java/com/cooper/lithic/feature/home/P2HomeArchiveRowTest.kt) 的三个项目朗读期望 | 仅把单件项目的标签从「当前日均成本」改成「项目日均成本」，保留负号、精度、单位、长名称、完整值入口与详情回调断言。账本主金额及订阅标签不变。 |
| `C2HomeClosureTest` | 原 360dp 横排、局部 2.0 字体堆叠和 FAB 断言仍适用，本包无迁移。 |
| `P01HomeShellSemanticsTest` | 早期兼容计数和整句订阅文案与已接受的快照规则有冲突，原文件及 NOT_RUN 保留。 |

## R2.1 视觉对照与边界

实际查看 R2.1 固定提交 `1b688bc1756dc3f42fcd3a3c0e57732d56023f34` 的三张画板，逐项比对生产源代码。[三主题、封面和状态画板](https://github.com/cc7279694-debug/property/tree/1b688bc1756dc3f42fcd3a3c0e57732d56023f34/docs/design/lithic-ui-r2-1-personal-archive/mockups)

| 画板 | 生产对应与已知差异 |
| --- | --- |
| 三主题 | 品牌与搜索、账本日均、项目、订阅、含历史数量、成本构成、FAB 与底栏保持画板顺序。雾蓝页首、纯白内缩分隔和黑灰明度层次有对应色值。原生按钮、分隔线及字体度量仍需截图比较。 |
| 封面与大字 | 80×60dp 的 4:3 Fit、无封面类型图标、两类档案指标和大字金额整组重排有生产对应。栏目跟随可用宽度，正常字号下的并排或下移仍取实际文字宽度。竖图边缘、长中文换行、极长金额和实际触控需端侧复核。 |
| 边界状态 | G 保留同一旧快照的金额、日期及计数。H 按 P2-3 批准调整省略日期和「非今日扣款」，画板当时仍写「非今日扣款」。全局警告与原重试保留在导航内容上方，位置与画板有意不同。 |

金额组件保留一个带金额与单位的语义节点，绘制文本与档案金额说明不再各自发出第二次数值。测试断言单一完整语义值及没有单独单位节点。TalkBack 实际朗读与焦点顺序未运行。源码中搜索和口径为 48dp，FAB 为 56dp，档案、类别、选项和底栏交互保留 48dp 下限。合成尺寸用例只在获准 Android 宿主中运行后才能形成实际布局证据。

## P2-1 至 P2-4 累计源码审计

对比正式交付基线 `d42a78125b83bee806add5e3fdd8d2328055b694`，生产累计差异只有五个 Kotlin 文件。

| 文件 | 累计职责 |
| --- | --- |
| `feature/home/HomeShell.kt` | P2-1 品牌与视觉层级、数量摘要。P2-2 档案。P2-3 读取状态与重试。P2-4 宽度及操作语义。原 22 个固定标签保留。 |
| `feature/home/HomeArchiveRow.kt` | 首页档案行及明确成本语义，复用真实预览。 |
| `feature/home/HomeOverviewPresentation.kt` | P2-3 读取呈现映射。原空态与预览规则未变。 |
| `feature/media/CoverPreviews.kt` | 可选显示缩放参数仅传给 `Image`。首页 Fit，其他四列表和两详情沿默认 Crop。 |
| `app/LithicNavigation.kt` | 只改 P2-3 的首页注入，项目和订阅分别走原列表刷新。原账本重试、Nav3、恢复与表单守卫字节一致。 |

Gradle、Manifest、签名接线、包与版本配置、Room schema、数据库与领域源码、backup4/恢复、Owner/代、照片 Reader/导入/清理、安全权限、图标和三主题源码均与正式基线一致。非首页的搜索、回看趋势、类别分析、设置及表单源码和共用金额组件也保持原字节。非首页运行验证仍按既有证据边界。

现有正式 APK 只读复核 **15,747,081 字节**，SHA-256 `ff909ca1eb21e04756d91d067082ca12df1ac17c263d793843fbbf4c67e40c2b`，与原签名交付记录相符。包名 `com.cooper.lithic`、`1.0.0` / code36 及证书沿原记录。本轮无打包、签署或发布。[原正式交付记录](LITHIC-V1-Private-APK-Release.md)


## 执行结果与原始证据

本轮使用已有 JDK 17.0.20.1、SDK 36、Gradle 8.14.3，离线、单 worker、1.5 GiB Gradle JVM 堆、进程内 Kotlin 编译。基线 13 类共 82 项通过，最终 16 类共 96 项通过。两轮均为 0 失败、0 错误、0 跳过。基线全部方法保留，最终增加 12 项既有守卫方法及 2 项本轮新增方法。

| 最终回归范围 | JVM 方法数 |
| --- | ---: |
| `C2NavigationTest` / `C7JourneyNavigationTest` / `P02BrowseNavigationTest` / `P03SettingsNavigationTest` | 5 / 10 / 12 / 4 |
| `M17NavigationPolicyTest` / `M21NavigationPolicyTest` | 4 / 8 |
| `C2HomePresentationTest` / `P01HomeOverviewPresentationTest` / `R1HomeEmptyStateTest` / `P2HomeReadSourceContractTest` | 5 / 11 / 3 / 1 |
| `CoverPreviewPolicyTest` / `CoverPreviewPipelineTest` | 6 / 2 |
| `C5MoneyMetricPresentationTest` / `R1AmountLayoutTest` / `C9ThemePaletteTest` / `C9R1VisualTokensTest` | 8 / 8 / 6 / 3 |

生产及 AndroidTest Kotlin 编译通过。Debug Lint 的生产、AndroidTest 及单元测试分析执行完成。最终 Gradle 退出 0，耗时 10m 5s。Lint 报告与 P2-3 字节相同，**0 错误、20 条既有警告**。新断言中的 24 个实际色对均达到 4.5:1，岩白雾蓝、纯白、深色黑灰最低分别为 **4.87:1、6.69:1、7.86:1**。这组数字来自生产色值，见 JVM XML。

| 工作包 | 当时定向 JVM 基线 / 实施后 | 当时 Debug Lint | Android 实际执行 |
| --- | ---: | --- | --- |
| [P2-1](LITHIC-UI-P2-1-Home-Foundation.md) | 25 / 25 | 0 错误，20 警告 | [0/10 NOT_RUN](LITHIC-UI-P2-1-Isolated-Acceptance.md) |
| [P2-2](LITHIC-UI-P2-2-Home-Archive.md) | 24 / 24 | 0 错误，20 警告 | 0/14 NOT_RUN |
| [P2-3](LITHIC-UI-P2-3-Home-Read-Presentation.md) | 14 / 19 | 0 错误，20 警告 | 0/31 NOT_RUN |
| P2-4 | 82 / 96 | 0 错误，20 警告 | 0/35 NOT_RUN |

各包 JVM 结果对应当时的 App 树，存在跨包重合。前序失败、补验中止及 P2-2 的 4 项桌面 Fit/Crop 几何结果按原回执解释。独立代码复核未发现本轮桌面交付阻断，P2 最终验收仍待独立审查。

本轮原始命令、退出码、JUnit XML（`baseline-junit/`、`p24-junit/`）、Lint XML 和环境证据保存在 `E:/CODEX/lithic-ui-p2-4-evidence/`。

| 原始证据 | 字节 | SHA-256 |
| --- | ---: | --- |
| `baseline-jvm.log` | 3431 | `19182d108db9fe0c0d4d7568db3ea834e751d49794ac6b1878fc75d5588723ea` |
| `p24-verify.log` | 20150 | `f1a61b1cfd7f415a5fa2e67a0e19ab3419c7b06d7d3f94d1e0fbb8b04c60e62e` |
| `lint-results-debug.xml` | 22662 | `feb6a44d197a338ed32b62e4325f766f0fe199bbbcaad7e677c12d352c7df396` |
| `cumulative-source-audit.txt` | 1127 | `dd2d7ab5cd76f177f942bd3169a1ac56e55bac5215913fe897e08cdfff4b53c6` |
| `android-environment.txt` | 98 | `ac2ad3437344c3e050e32e59cc05cdc4f17d6e32407cbc24580d6e77ebbafd54` |

## Android 未执行清单与停止点

主机级只读枚举退出 0，返回 `host:devices OKAY; total=0; emulator=0`。未发现运行中的 emulator/QEMU。没有启动未知或历史 AVD，没有操作手机和账本，构建输出中的 APK 数为 0。

| 端侧范围 | 方法数 | 状态 |
| --- | ---: | --- |
| P2-4 `P2HomeArchiveContractTest` | 6 | NOT_RUN |
| P2-4 `NavigationShellTest` / `ProjectNavigationGuardTest` / `SubscriptionNavigationGuardTest` / `M09SubscriptionNavigationGuardTest` | 15 / 4 / 7 / 3 | NOT_RUN |
| P2-1 三主题、记录优先、唯一 FAB 和零写入 | 10 | NOT_RUN |
| P2-2 首页及列表详情封面 Fit/Crop、无引用、错误与身份 | 14 | NOT_RUN |
| P2-3 读取状态、重试、旧代丢弃、冷启动保护 | 31 | NOT_RUN |

本轮更新的旧画面和朗读断言也保持 NOT_RUN。仍需确认真实 Android 三主题画面、320/360/400dp 布局与宽窗口、封面像素、极长金额访问、系统 200% 字体、TalkBack 朗读与焦点、IME/Insets、旋转、实际触控、底栏避让，以及 Nav3 返回与安全守卫。已编译的局部 `LocalDensity` 和回调测试提供待运行用例。

停在 `P2_READY_FOR_FINAL_INDEPENDENT_REVIEW`，等待 ChatGPT 最终独立验收。

## 最终独立复核补修 · BLOCKING-01

ChatGPT 对固定提交 `5b5a110f423c3768fc9bd71b41bfb6c11b15c373` 的结论为 DESKTOP IMPLEMENTATION PASS WITH NOTES，P2 FINAL FREEZE PENDING。本节补修上文记录的两项早期测试冲突，其余 P2-4 原证据按原提交解释。

App 内唯一修改为 [P01HomeShellSemanticsTest.kt](../../app/src/androidTest/java/com/cooper/lithic/feature/home/P01HomeShellSemanticsTest.kt)。两条测试以新名称延续各自场景。主金额取 `overview` 提供的真实日均，数量从同一快照读取，金额语义来自原金额组件。生产 `app/src/main` 树仍为 `6f1553b0ae5b2137cfb327263e30fb41769e4845`，构建、发布、签名、Manifest 和 schema 相关文件保持原字节。[首页金额与读取状态](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L183) · [首页数量语义](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L236) · [金额可访问描述](../../app/src/main/java/com/cooper/lithic/core/design/AmountText.kt#L30)

| 原方法与断言 | 新方法与断言 |
| --- | --- |
| `unavailable_cost_preserves_the_verified_record_count_without_inventing_zero`。金额包含「—」，显示等待文案，要求显示兼容参数拼成的「已录入 2 条长期项目」，仅按整字串检查 `¥0.00` 缺失。 | `missing_overview_keeps_amount_and_both_counts_unknown`。仍以兼容参数项目 2、订阅 0 作为反例。金额及两类计数按各自测试标签精确为「—」，无障碍描述分别为「当前日均成本，暂无数据」「长期项目，数量尚未取得」「订阅服务，数量尚未取得」。保留等待文案，断言旧兼容句和包含 `¥0.00` 的值均不存在，也无确认空账本引导。 |
| `subscription_only_summary_does_not_lead_with_zero_projects`。手工拼装单订阅快照，要求订阅计数标签含整句「已录入 1 条订阅服务」，禁止旧的「已录入 0 条长期项目」，金额按 `¥0.00` 校验。 | `subscription_history_summary_keeps_one_record_and_verified_zero`。以一条已在付费周期末停止的合成订阅、零项目，通过原 `OverviewEngine` 生成真实结构的快照。兼容参数项目 2、订阅 7 与快照刻意不同。订阅及项目计数精确为 `1` 和 `0`，对应描述分别为「订阅服务，已录入数量1，包含历史」「长期项目，已录入数量0，包含历史」。可见「已录入 · 含历史」，口径弹窗明确提到已停止的订阅。保留旧项目整句缺失断言，金额精确为 `¥0.00/天`，朗读描述为「当前日均成本，0.00元/天」，保留无当前有效记录提示。 |

两项都保留显示或语义断言，并增加列表及新增回调计数为零的检查。已知快照用例点开本地口径弹窗后仍检查零回调。夹具只用合成事实。

### 实际验证

- 定向 JVM 共 **88 PASS / 0 FAIL / 0 跳过**。分别为 `C2HomePresentationTest` 5、`P01HomeOverviewPresentationTest` 11、`R1HomeEmptyStateTest` 3、`P2HomeReadSourceContractTest` 1、`C5MoneyMetricPresentationTest` 8、`OverviewEngineTest` 23、`SubscriptionCostEngineTest` 37。
- 离线普通 Debug 验证退出 0，耗时 3m 59s。生产 Kotlin 编译任务复用同源码的有效结果（UP-TO-DATE），AndroidTest Kotlin 编译实际执行并通过。Debug Lint 0 错误、20 条既有警告，Lint XML 与原 P2-4 字节相同。
- 原命令、退出码、JUnit XML、Lint XML 和源码边界核对保存在 `E:/CODEX/lithic-ui-p2-4-blocking-01-evidence/`。

| 原始证据 | 字节 | SHA-256 |
| --- | ---: | --- |
| `blocking-01-verify.log` | 14820 | `ab63b83072beb9c47b745df5acfcc4f75d59258437f4240c88c608461edddc0e` |
| `lint-results-debug.xml` | 22662 | `feb6a44d197a338ed32b62e4325f766f0fe199bbbcaad7e677c12d352c7df396` |
| `source-audit.txt` | 635 | `047092220ea0852dae7de5127860967e1308ab6a1e833e3afda89a41c991769a` |

### 单独登记的端侧测试

| `P01HomeShellSemanticsTest` 方法 | 实际状态 |
| --- | --- |
| `missing_overview_keeps_amount_and_both_counts_unknown` | NOT_RUN |
| `subscription_history_summary_keeps_one_record_and_verified_zero` | NOT_RUN |

这两项实际执行 **0/2**，独立于原 P2-4 的 35 项。未启动模拟器、操作手机、运行 Instrumentation 或生成 APK。停在 BLOCKING-01 补修回执，等待 ChatGPT 最终复核。P2 FINAL FREEZE PENDING。

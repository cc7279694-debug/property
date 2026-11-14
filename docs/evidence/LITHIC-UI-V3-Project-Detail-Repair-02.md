# LITHIC UI-V3 · 项目详情 Android 修复 02

证据起点 `c0cb77e7a7e39204a113d3142b730f0e5f7a55af`，生产基线 `0f6fed74c03a82ee5a90f5e209e6ea6fb3989a25`，工作分支 `codex/lithic-ui-v3-project-detail-repair-02`。[上一轮报告](LITHIC-UI-V3-Project-Detail-Implement-01.md) · [11 项原失败日志](lithic-ui-v3-project-detail-implement-01/android-io-recheck/)

## 修改范围

| 文件（均在 `app/src/androidTest/`） | 改动 |
| --- | --- |
| `V3ProjectDetailScreenTest.kt` | 类别、项目、事件、操作及折抵组使用由夹具命名空间和角色生成的确定性规范 UUID。只换 generation 时项目 ID 保持，切换项目时使用不同 ID。先滚动到记录区，再验证三条记录时没有「全部」。保留切换、纠错及零写入断言。 |
| `ProjectEntryFlowTest.kt`、`ProjectLifecycleFlowTest.kt` | 通过 `home-open-add` 打开现行新增入口，要求 `home-create-project` 实际可见再进入表单。按真实界面展开日期，校验固定保存与后退操作，按原路由来源回到首页／记录列表／回顾。保留连点防重复、金额、日期、历史事实与守卫。 |
| `ProjectScreensTest.kt` | 精确到持有期节点与成本区域。分别验证未开始、未知金额 `—` 与负净投入。 |
| `V3ProjectDetailAndroidAcceptanceTest.kt` | 原溢出断言保留。记录实际标题、段落尺寸、布局约束、宽高溢出、字形行宽和垂直边界、节点及父布局、字号／行高、系统 fontScale。增加末字可见、无省略和节点裁切断言。每个主题先保存原生图，汇总独立断言后由 JUnit 如实给出结果。 |

九项测试修正依据原始失败和现有页面合同。生产源码、Money、Room、Owner、图片协议、首页和订阅均未改。继续使用原签名 Debug App，测试 APK 由本分支构建。

## 验证结果

- 固定测试源码 `98af7d1d4cea61bf7f9d36ffcf25df7eaa6e5a9c`，`app` 树 `64ec9fd44e81ea9208ad3e7840a4849872607eb5`。生产 Kotlin 实际编译，51 项定向 JVM **51 PASS／0 FAIL**，AndroidTest 编译、打包及 Debug Lint 均退出 0（0 错误／20 警告）。首轮实际 JVM 和 Lint 记录保留，最终针对后续测试调整再次编译打包，并在同一 Gradle 验证入口核对任务状态。[Gradle 参数](lithic-ui-v3-project-detail-repair-02/desktop/desktop-arguments.json) · [首轮日志](lithic-ui-v3-project-detail-repair-02/desktop/desktop-build.log) · [固定树构建日志](lithic-ui-v3-project-detail-repair-02/desktop/desktop-final.log) · [定向测试 XML](lithic-ui-v3-project-detail-repair-02/desktop/jvm/) · [Lint](lithic-ui-v3-project-detail-repair-02/desktop/lint-results-debug.txt)
- 离线检查核对 Android runner 的前 11 个失败调用与完整 43 个调用身份，验证前者失败时不进入后者。原采集器 29／29 份回放记录可解析。[顺序与范围校验](lithic-ui-v3-project-detail-repair-02/desktop/runner-offline-check.txt) · [采集器回放](lithic-ui-v3-project-detail-repair-02/desktop/collector-preflight.json)
- 复用原 `com.cooper.lithic.debug` 21,224,212 字节，SHA-256 `8f638588cb738f8eec949bfb758b72285f29056fa2747a6601ad077530fcc9c5`。本轮 `com.cooper.lithic.debug.test` 4,650,628 字节，SHA-256 `43272a287b7b2bb18966695a94b60ee83d507ef47b8d2fbf94b50bd0552925e4`，仍由唯一 AndroidJUnitRunner 指向既有 Debug App。两份证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。实际 DEX 与编译类均保留 `hasVisualOverflow` 断言、单一聚合失败及所需诊断字段。首个审计输出受 Windows 控制台 GBK 编码阻断，使用 UTF-8 重跑审计通过。[APK 与 DEX 结果](lithic-ui-v3-project-detail-repair-02/desktop/apk-audit.json) · [源码与实物绑定](lithic-ui-v3-project-detail-repair-02/desktop/apk-build-inputs.json)


## 隔离环境与安装边界

主机启动瞬时可用内存 5.815 GiB。复用原 Repair05 AVD、原 userdata 与 API 36 x86_64 镜像，冷启动一次。沿用上一轮诊断支持的只读启动文件预热（25.219 秒）。本次观察可用于准入，ANR 长期根因及预热的稳定效果仍待证实。

生成的配置为 1536 MiB、临时四核，设备侧读回四核、360dp，独立 SDK ADB 5038 自动注册唯一设备 `emulator-17640`，系统报告启动完成。guest 内存从 438,480 kB 自然恢复到 469,800 kB，再执行额外 60 秒观察。14 份 guest 样本和 13 份主机样本通过门禁，观察期间未捕获 ANR，准入后才执行测试包操作。[启动前资源](lithic-ui-v3-project-detail-repair-02/android/prelaunch.json) · [设备准入](lithic-ui-v3-project-detail-repair-02/android/admission-result.json) · [健康时间线](lithic-ui-v3-project-detail-repair-02/android/timeline.jsonl)

端侧回读证实既有 Debug App 已是预期字节，直接复用。只替换已核对归属与签名的 Debug Test。正式包和历史 Preview 未进入安装范围。没有连接或操作真机。[端侧包和 SHA 回读](lithic-ui-v3-project-detail-repair-02/android/apk-install-disposition.json)

## 标题溢出诊断

上一轮在系统字体 1.0、2.0 各得到一次 `hasVisualOverflow=true`，没有宽高分项或约束记录。只有系统 200% 的截图能复核六字标题。本轮在实际 MainActivity 中记录了同一标题「合成横向相机」的完整布局，三主题正常字号数据一致。以下都是端侧 px，系统字号与 Compose Density 读回一致。[布局原始值](lithic-ui-v3-project-detail-repair-02/android/title-layout.jsonl)

| 字体与主题 | 实际节点与测量尺寸 | 最大宽／高与重建段落尺寸 | 行数、行宽、行底 | 字号／行高及溢出 |
| --- | --- | --- | --- | --- |
| 系统 100%，三主题 | 288×70 | 656／无上限，656×70 | 1 行，宽 288、底 70 | 24sp／32sp，48／64px。宽 true、高 false |
| 系统 200%，纯白 | 432×105 | 656／无上限，656×105 | 1 行，宽 432、底 105 | 24sp／32sp，72／79.54px。宽 true、高 false |

两种字号的文字高度、行底及节点高度一致。六个字形分别完整落在实测节点宽高内，最后可见索引为 6。省略、超出最大行和节点裁切均为 false。三主题 100% 的标题边界是 (32,612)–(320,682)，父布局 (32,160)–(688,742)。纯白 200% 的标题边界是 (32,540)–(464,645)，父布局 (32,160)–(688,823)。两者均位于滚动视口内。

**判定 C，Compose 语义接口重建的宽度与实际绘制范围不同。** 项目名称的普通文本节点通过 `GetTextLayoutResult` 返回测量结果。Compose 1.9.5 的 `ParagraphLayoutCache` 为该语义调用按最大宽度 656 重建 `MultiParagraph`，同时返回原节点按内容测量的宽度。其 `getDidOverflowWidth` 比较节点宽度和整个重建段落容器宽度，288 或 432 小于 656 就会返回 true。原生截图、逐字边界和完整可见索引共同证实本合成标题在已测配置里没有裁切。此结论限于该现场及样本。保留原布尔断言，两次画廊实际运行仍为 **FAIL**，生产标题布局未改。[依赖实现证据](lithic-ui-v3-project-detail-repair-02/desktop/compose-title-layout-bytecode.txt)

## 原生截图

均为隔离 AVD 的真实 MainActivity、360dp（原图 720×1648），仅包含合成账本。正常字号的三主题及纯白真实系统 200% 如下。画廊同时验证了当前成本、三张财务卡、底部操作和无裁切断言。截图记录与测试的最终失败分别保留。

- 100%：[纯白](lithic-ui-v3-project-detail-repair-02/android/project-detail-pure-white.png) · [岩白雾蓝](lithic-ui-v3-project-detail-repair-02/android/project-detail-stone-mist.png) · [深色黑灰](lithic-ui-v3-project-detail-repair-02/android/project-detail-charcoal.png) · [竖图 Fit](lithic-ui-v3-project-detail-repair-02/android/project-detail-portrait.png)
- 系统 200%：[标题和成本](lithic-ui-v3-project-detail-repair-02/android/project-detail-font-200.png) · [滚动后三张财务卡和固定操作](lithic-ui-v3-project-detail-repair-02/android/project-detail-font-200-money.png) · [系统字体读回](lithic-ui-v3-project-detail-repair-02/android/focused-font-200-read.txt)
- [截图原始指纹](lithic-ui-v3-project-detail-repair-02/android/screenshots.jsonl)

## 原 11 项失败定向复验

下表名称对应 [原始 11 项失败](lithic-ui-v3-project-detail-implement-01/android-io-recheck/test-summary.jsonl)。终态和方法名见[本轮原始结果](lithic-ui-v3-project-detail-repair-02/android/test-summary.jsonl)。

| # | 原失败用例 | 本轮 Android | 观察 |
| --- | --- | --- | --- |
| 1 | MainActivity 画廊，系统 100% | FAIL | 三主题原图已保存，仍命中原标题布尔断言。 |
| 2 | MainActivity 画廊，系统 200% | FAIL | 纯白标题与财务原图已保存，仍命中原标题布尔断言。 |
| 3 | 折抵事实、备注与受保护的纠错路径 | PASS | 实际进入单笔明细，保留关联折抵限制及原纠错入口。 |
| 4 | 三条金额记录 | PASS | 三条均可达，无「全部」。 |
| 5 | 大字体财务与底部操作 | PASS | 组件大字断言通过，三张财务卡与独立底部操作可达。 |
| 6 | 四条记录、收起及身份换代 | PASS | 同列表展开、收起，切换项目或 generation 后折叠状态重置。 |
| 7 | 未开始项目与负净投入 | PASS | 持有期和成本原因节点均准确，未知日均未冒充零，负净投入保留。 |
| 8 | 回填金额并从详情、清单、首页读取 | PASS | 完整进入真实 Room 路径，日期、回填计算和负值断言通过。 |
| 9 | 项目及金额连点提交 | PASS | 保留连点路径，仅得一条项目及一条所追加事件。 |
| 10 | 编辑名称和开始日期 | PASS | 实际查看更正结果，原金额历史保持。 |
| 11 | 历史更正与真实收回 | PASS | 两条路径均触达，原事件身份及历史归属守卫保留。 |

本轮先跑上一轮全部 11 个失败执行，**9 PASS／2 FAIL**。剩余两个 FAIL 触发后续门禁，完整 **43 项套件本轮 NOT_RUN**。上一轮 32 PASS／11 FAIL 保持历史原值，不能与本轮的通过项合并。原生画廊分别实测了横图、竖图的 4:3 封面，正常字号下只读放大与系统返回。两次画廊所用纯合成账本均有清理回执。其他原有独立回归，包括未在这 11 个执行里覆盖的无图／失效、长标题、真实零值、已结束与旧快照、单独的导航守卫等，本轮未重跑。[全量门禁](lithic-ui-v3-project-detail-repair-02/android/full-regression-gate.json) · [系统 100% 原始断言](lithic-ui-v3-project-detail-repair-02/android/instrument-focused-project-detail-gallery.txt) · [系统 200% 原始断言](lithic-ui-v3-project-detail-repair-02/android/instrument-focused-project-detail-system-font-200.txt)

## 清理与交付状态

- 原系统字体为 1.0，200% 取证后恢复到 1.0 并实际读回。两次画廊均收到夹具清理 PASS，覆盖合成账本与媒体。三个临时账本目录探针均为空，截图拉取后移除了 guest 临时截图目录。测试写入限于已验证的合成数据，没有清应用数据或重建 AVD。[字体恢复](lithic-ui-v3-project-detail-repair-02/android/focused-font-restore-read.txt) · [夹具目录探针](lithic-ui-v3-project-detail-repair-02/android/fixture-scan-focused-project-detail-lifecycle-flow.txt)
- AVD 正常退出，退出码 0，无本轮子进程或监听残留。独立 SDK ADB 37.0.1 的 5038 已退出，原 5037 前后均为 PID 15576 且创建时间相同。原 userdata 文件身份保留，运行后的覆盖层 SHA 变化有回执，无 wipe。测试控制器及外层如实退出 3。[模拟器清理](lithic-ui-v3-project-detail-repair-02/android/emulator-cleanup.json) · [userdata 核对](lithic-ui-v3-project-detail-repair-02/android/userdata-after-shutdown.json) · [ADB 收口](lithic-ui-v3-project-detail-repair-02/android/adb-closeout.json) · [控制器终态](lithic-ui-v3-project-detail-repair-02/android/controller-result.json)
- 生产 Kotlin、应用身份、签名配置、Room、备份、金额和媒体协议与固定源码基线无差异。原 App APK 未重新打包，无新产品身份或正式签署。下一项待独立审查的是标题的运行验收判据，需保留字形、可见末尾、省略和实际节点裁切的检查，并补证长标题与完整 43 项回归。本轮的两次失败继续保留。

**READY_FOR_REVIEW_WITH_FAILURES** · 等待 ChatGPT 独立审查。

# UI-C1 阶段1 — 视觉精修复审

本轮依据用户转交的 ChatGPT 对 `53fd944fe70a912fd4c1e1fa76d4ec31d1a103d5` 中19张原始截图和阶段1证据的 **CHANGES_REQUESTED** 继续精修。授权仍仅为阶段1 Task1～Task5；不是 Task6～Task8、M10 JPEG 修复或 M11 的授权。

状态：**VISUAL_REVIEW_REQUIRED — UI-C1（视觉精修复审）**。当前仍等待用户看图，不自行授予ACCEPTED/FROZEN。原阶段1交付、所有失败与正式矩阵保持。

## 1. 本轮六项调整

| 审查要求 | 本轮 UI 调整 | 保留的事实及操作 |
|---|---|---|
| 首页双数字指标 | 用并列的长期项目、订阅服务数字卡替代纯文字记录概览。数字仍来自传入的真实全身份计数；完整朗读名称保留“已录入 N 条”语义。 | 显式说明“数量包含历史与已停止记录”。不合计卡片金额，不编造总日均成本，不实施 M13。 |
| 列表更早显示封面卡 | 将返回、长期项目标题、新增压缩为一行；返回和新增各为48dp图标按钮，提供完整中文无障碍名称。下方只保留真实“当前列表 N 条 · 未结束”和必要错误信息。 | 原条目集合、稳定顺序、日期、状态、真实封面、原身份回调、继续加载及失败重试均保留。未增加搜索、筛选或新的当前/历史判断。 |
| 详情封面与核心指标比例 | 保存封面移至核心卡之前，用2.2比例的较矮裁切预览；核心卡紧凑组合名称、状态、日均和持有天数。大字体或窄宽时转为纵向排布。 | `CoverDisplay` 仅接受可选的展示比例，默认仍为16:9；原图查看保持原比例。未改保存图片、引用、导入校验或清理协议。日均、天数、观察日、投入/收回/净投入、基本信息、备注、结束结算说明及全部事件操作均保留。 |
| 首页全空只突出一次新增 | 成功确认全空时只显示一个空态主卡与一次“新增记录”操作；不再重复渲染两类空预览卡。 | 原长期项目、订阅浏览路由以简单文字按钮保留。读取中或失败不会冒充已确认空账本；仅历史或已停止记录不被当作全空。 |
| 新增按钮不覆盖内容 | 非空首页新增按钮放在标题栏的独立48dp布局位置，滚动内容占用下方剩余空间，移除覆盖卡片的悬浮定位。 | 原 `home-add-record` 标签、两项新增菜单和回调保留；全空首页仍只显示首次新增按钮。新增和浏览仍经过原导航装配与守卫，不产生业务预写。 |
| 大字体返回与菜单 | 详情返回和更多改为48dp图标控件，返回目的地全文保留在 `contentDescription`。更多菜单使用明确中文文字，并按系统字体比例增加最小行高。 | 不强行缩小字体、不省略关键金额，不更改原 Back 来源链、菜单动作或删除确认。结束操作与危险删除继续分义。 |

对应设计澄清见[设计规格](../design/UI_C1_SPEC.md)，本轮范围与验证计划见[阶段1精修 R1 计划](../planning/UI_C1_PLAN.md#阶段1精修-r1用户已授权)。53fd944的[原阶段1报告](UI-C1-stage1-verification.md)及原截图、测试和 checkpoint 保留当时结论，不覆盖旧文件来伪造新证据。

## 2. 改动范围与数据来源保护

本轮生产改动限定于四个展示文件：`HomeShell.kt`、`ProjectListScreen.kt`、`ProjectDetailScreen.kt`、`CoverDisplay.kt`，以及相应 UI 测试、文档和独立视觉证据。颜色与字体系统沿用已批准规格，不重新选择视觉方向。

Room v2、实体、DAO、Migration、领域引擎、Money、BusinessClock、业务 Repository、同代 owner/coordinator、图片导入和资源清理协议不是本轮改动范围。应用身份、签名、版本、权限、运行依赖及工具链继承。`LithicNavigation`、序列化路由键、来源链及三 ViewModel 离开守卫不因新按钮位置重写；UI 继续调用既有回调。

本轮27张原图分组见[图册](ui-c1/refinement/GALLERY.md)与[逐图SHA登记](ui-c1/refinement/screenshot-manifest.json)：

- **MainActivity：**最终安装 APK 读取设备已有 QA 账本。保留原七表、原封面及默认 generation，不注入合成展示物品或替换旧封面。此类截图证明真实安装应用的运行布局。
- **隔离展示宿主：**使用独立测试目录与 Room generation，通过已有 Repository、真实领域引擎和媒体接口生成测试事实；物品名称为合成名称，封面为测试时自制几何图。不得将这些行或资源写入活动账本，也不得打包进生产 APK。
- **隔离空态宿主：**独立成功读取的空 Room，运行真实业务 Compose/导航展示空首页；不通过清库、切换活动指针或伪造计数制造 MainActivity 空账本。

不同宿主的截图必须分别标注，测试宿主图不能代替 MainActivity 系统栏、安装字节或真实活动账本验证。原始截图不进行修图或叠加假数据；DB/WAL/SHM、原私有图片、签名私钥及私人数据不提交。

## 3. 测试设计与假阳性说明

新增布局测试直接观察真实 Compose 结果：双计数实际数字与范围说明、全空唯一新增、标题栏新增与滚动区域不交叠、列表首卡位置、360dp/200%标题和48dp操作、详情封面与核心指标位置，以及完整菜单和历史事实可达性。测试 fixture 使用隔离事实与冻结引擎，不复制 UI 成本公式。

原方法身份和业务断言保留。计数由一句文字改成数字与标签后，旧“已录入 N 条”断言通过完整中文无障碍名称检查同一真实数量；返回目的地由可见长文字改为图标后，旧返回来源断言检查完整无障碍名称。此类选择器适配不取消原金额、身份、事务或导航保护要求，也不删失败测试。

200%菜单验证曾出现 `TextLayoutResult.hasVisualOverflow=true`。当时诊断同时记录了单行 `252 × 61` 像素文本、行右界252、无省略，菜单行还具有更大的可见空间。仅依赖该布尔值不能确定字符真的被裁切，因此保留失败与诊断，继续检查实际文本布局；不能把失败直接改成期望值或用截图替代断言。

当前菜单测试保留完整原文检查、无截断行、无省略、全部字符存在，并逐行、逐字符验证边界，还检查文字框位于菜单动作行内。比较只允许 **1个物理像素** 的布局舍入容差，独立于系统字体比例。`hasVisualOverflow` 的原值继续记录用于诊断；详细尺寸/原像素见[diagnostics](ui-c1/refinement/diagnostics)，方法结果见[测试登记](ui-c1/refinement/test-executions.json)。最终真实OS200%诊断为文字框504px、实际行右界504px、段落预留宽648px，未省略；旧布尔值比较段落宽与文字框宽，不能独立证明字形裁切。最初“小数舍入导致”的推测已被这一实测解释修正。局部LocalDensity测试的Popup实际仍可为OS字体1.0，因此另以真实font_scale=2.0/density=480执行，不能用局部参数冒充系统200%。这是修正可能产生假阳性的测量方法，不是缩小系统字体、移除原菜单项目或修改业务语义。

主代理应逐阶段登记命令、输入、退出码、鲜取 XML 与实际结果，明确区分编译/环境失败、行为 RED、诊断及最终回归。不同输入、不同宿主或不同阶段的通过记录不能拼成一次最终全量通过。

### 已完成阶段的实际历史

下表已逐项读取 `build/ui-c1-refinement/*-summary.json` 及对应原始 XML/日志。秒数为各 summary 的 `durationSeconds`，包含该阶段执行墙钟；不是 XML 内单个用例耗时。表中八阶段的登记输入均为 `inputsStable=PASS`。本表仅列前8个历史阶段；最终主跑与未改源码单项复跑见第6节。

| 阶段 | 实际执行与墙钟 | 结果/退出码 | 失败性质及后续处理 |
|---|---|---|---|
| `red-refinement` | 0个方法；58.058秒 | 命令失败，退出1 | 参数传递错误使 `.testInstrumentationRunnerArguments.class=...` 被解释成不存在的任务。未执行产品断言，不作为行为 RED。 |
| `red-refinement-quoted` | 0个方法；523.616秒 | 设备准备失败，退出1 | 测试已成功编译；设备属性查询无响应，随后报告 `emulator-5596` 不存在/不可用，实际启动0项。不是8项新测试失败。 |
| `red-refinement-after-restart` | 8个 Android 方法；137.016秒 | 0 PASS /8 FAIL，退出1 | 7项旧布局不满足本轮新布局契约；1项菜单布尔溢出测量随后证实需要更精确核对。不能把全部8项写成已证实的产品缺陷。 |
| `green-refinement-samples` | 50个 Android 方法；338.980秒 | 46 PASS /4 FAIL，退出1 | 首页/列表等新契约已通过；剩余为1项菜单旧测量断言和3项窗口无焦点失败。后者有外部 Wellbeing ANR 窗口证据，见下节。 |
| `menu-and-focus-diagnostic` | 4个 Android 方法；75.734秒 | 3 PASS /1 FAIL，退出1 | 原3项无焦点失败重跑通过；菜单仍在旧 `hasVisualOverflow` 断言失败。97份登记 main 输入相对上一阶段哈希完全相同，没有用生产改动换取焦点恢复。 |
| `green-refinement-readable-menu` | 5个 Android 方法；76.206秒 | 4 PASS /1 FAIL，退出1 | 更精确的菜单文本边界断言已越过，返回/封面首屏及两类隔离截图宿主通过。唯一失败发生在后续资金事实滚动：直接 `performScrollTo()` 查找尚未组合的 LazyColumn 屏外节点失败。 |
| `green-refinement-lazy-scroll` | 1个 Android 方法；41.035秒 | 1 PASS /0 FAIL，退出0 | 驱动改为先对原 `project-detail-content` 执行 `performScrollToNode(hasTestTag(...))`，再检查相同投入/收回/净投入/备注节点；原事实断言和最终数据库相等断言保留。 |
| `green-refinement-real-os-200-menu` | 1个 Android 方法；33.220秒 | 1 PASS /0 FAIL，退出0 | 同一菜单及资金事实可达性方法在真实系统200%设置下单独重跑通过；完整设备设置/截图登记由主代理补齐，不将这一项当作全站大字体验通过。 |

上述已执行阶段均无跳过项。各阶段有重复方法，不能把表中执行次数相加当作最终唯一覆盖数量，也不能用较晚的单项 PASS 改写先前失败 XML。

对8项 RED 的原始 XML 逐项核对后，7项布局契约失败分别是：全空仍出现重复预览区、双数字指标尚未出现、旧首页没有与标题栏分开的滚动区域、列表仍是文字返回而非本轮纯图标完整描述、首卡低于160dp目标、详情长返回尚未提供图标式完整目的地描述，以及封面仍位于成本卡之后。这些断言证明旧布局尚未满足新目标；其中标签或 `contentDescription` 目标缺失不单独证明旧文字原本无法被 TalkBack 读取。第8项菜单失败仅来自旧测量布尔值，不能据此宣称已确认字符裁切。

菜单测量修正后的另一项失败也不掩盖：`green-refinement-readable-menu` 的 XML 明确为找不到 `project-detail-outlay` 的 `performScrollTo()` 目标，而不是金额丢失。LazyColumn 的屏外项尚未组合，驱动须从容器按节点滚动；后续1项通过是在保留相同事实及存储相等检查的前提下取得。

## 4. 窗口焦点与外部 ANR 诊断

本轮一组运行出现 `RootViewWithoutFocusException`，测试窗口及菜单窗口长时间没有取得焦点。实际 UI 层级显示系统对话框 **“Digital Wellbeing isn't responding”**；窗口诊断包含 `Application Not Responding: com.google.android.apps.wellbeing`。这为当时焦点被系统层外部应用窗口占用提供了直接依据，不能将相关焦点失败直接解释为本轮 LITHIC 布局断言失败。

现有证据没有说明 Digital Wellbeing 内部 ANR 的根因，也不能据此宣称 LITHIC 在所有环境均无 ANR 或性能已经达标。相关失败、系统窗口与原截图应保留；环境恢复后的重新运行必须作为独立阶段登记。不得通过删除失败 XML、增加等待上限、清空账本、重建/wipe AVD 或修改业务代码来掩盖该诊断。

`green-refinement-samples` 中3项无焦点方法为：

- `ProjectScreensTest#create_form_exposes_real_payment_date_zero_amount_and_controlled_category`
- `ProjectLifecycleScreensTest#end_form_only_records_recovery_after_explicit_opt_in_and_accepts_zero`
- `ProjectLifecycleScreensTest#detail_routes_regular_and_initial_corrections_separately_from_end_settlement`

三项在 `menu-and-focus-diagnostic` 的原 XML 均实际 PASS；该阶段仍有另1项菜单测量 FAIL，因此整个阶段退出码仍为1。焦点诊断与恢复不能替代最终候选的回归，完整日志和系统对话框原像素截图必须一并保留。

## 5. 仍保留的边界

- M10 的三项 JPEG 拒绝失败保持原方法、原拒绝断言和原生产链路；M10 仍 PENDING，不用封面显示成功替代输入完整性。Q05 USER_CONFIRMED 不等于 M11/M12 完整往返已验证。
- 原130条归档案例、16组跨模块边界、M00～M09历史冻结与 NOTES、Q03～Q06期限不改写。本轮不回写正式 ACCEPTANCE_MATRIX。
- Task6～Task8 未授权，完整应用改版和全量测试不能由本轮定向验证推定通过。原其他页面、表单统一及完整全站视觉验收仍按原门禁。
- 真机、API26、OEM、Release、正式签名升级、完整 TalkBack/焦点遍历、真实低内存自动回收、断电及完整备份恢复没有本轮新证据时继续 NOT_RUN。
- 只在 UI 功能分支提交推送，不改 M10 分支、WHOCARE 或 main，不合并、不公开发布 APK。
- 只读同伴检查属于本地辅助核对，不替代 ChatGPT 独立审查、用户实际看图确认或正式接受结论。

## 6. 最终实际结果

| 执行 | 实际结果 | 证据含义 |
|---|---|---|
| final-refinement | 903.053秒，86 JVM＋161 Android＝247；243 PASS /4 FAIL /0跳过，退出1 | JVM86全PASS；Android157 PASS/4 FAIL。原三JPEG拒绝FAIL＋旧合成表单保存点击超时；原始失败全部保留。 |
| recheck-unchanged-synthetic-form | 46.435秒，原方法1 PASS，退出0 | 未改变源码/断言/10秒超时。是单独复跑，不把原247重写为全通过；偶发点击时序风险仍记录。 |
| 独立Lint/assemble | 6.652秒，退出0，日志BUILD SUCCESSFUL in 6s | UP-TO-DATE任务如实保留；Lint0 errors/23 warnings，与原警告基线一致，不升级依赖。 |

最终181登记输入（含main/debug/test/androidTest/schema、Gradle包装器和配置）在上述两次测试中逐字节一致，且与当前文件相同；各阶段ZIP/鲜取XML/SHA均已核对。见[输入绑定](ui-c1/refinement/input-and-test-binding.json)、[真实命令/结果](ui-c1/refinement/test-executions.json)、[独立构建命令](ui-c1/refinement/final-artifact-command.json)、[构建日志](ui-c1/refinement/final-artifact.log)和[Lint XML](ui-c1/refinement/lint-results-debug.xml)。登记输入并非工具链缓存/签名私钥的完整可重建环境快照。

静态当前701方法，原692身份全部保留，本轮新增9；原阶段1新增35＋本轮9＝44个UI方法在final-refinement同一次主运行全PASS。主运行与未改源码单项复跑合并的**247唯一身份较晚结果**为244 PASS/3 FAIL，仅表示两条命令的覆盖历史，不能写成一次247绿灯；源方法中454未选，Task8全量701回归NOT_RUN且未授权。旧表单源码本轮没有改，原超时发生在保存点击后的回调等待；失败时合成宿主按钮接近屏幕下缘，时序/命中区域根因没有完全锁定，不宣称已修复或证明M05/M06所有IME路径。

三项JPEG的生产链路/测试字节与起点无差异，完整原方法名见上述输入绑定的primaryFailCases。当前M10仍PENDING，未修JPEG，不把封面展示验证当输入完整性通过。TypeScript/ESLint不适用于原生工程。

## 7. APK、实际运行与数据保护

- 本地APK：`E:/CODEX/.codex/worktrees/lithic-ui-c1/砺石新APP/build/ui-c1-refinement/lithic-ui-c1-refinement-debug.apk`，**13,273,760 bytes**；SHA-256 `05680d06032900531977d216e7719b2b3467649c817035568f2d32dc9c2d8ebd`，设备安装字节相同。
- 测试APK SHA-256 `aab6fb6d80ee49a958054b7969743d2a26c3eb513a31a5b79ec44a5365886f26`，仅用于仪器宿主绑定；APK本体不提交/不公开发布。包名`com.cooper.lithic.debug`、code8、`0.1.0-m10-debug`、签名证书保持；最终权限只含原同包动态receiver权限，无网络/相机/广泛存储权限。备份排除资源逐字节一致。扫描目标dex未含四新增测试类及三合成物品名称，没有匹配的fixture资产名；原Debug空测试Activity声明不因本轮被当作Release可交付。
- 实际工具版本见[工具输出](ui-c1/refinement/tool-versions.json)、[APK清单/签名/版本核对](ui-c1/refinement/apk/apk-verification.json)：JDK17.0.20.1、Gradle8.14.3、AGP8.13.2、Kotlin2.3.10、Compose BOM2025.11.01/UI1.9.5、M3 1.4.0、Nav3 1.0.1、Room2.8.5；依赖/配置未改。使用已有API36的`emulator-5596`，没有真机/Release新增结论。
- 原七表全部字段精确相同：2类别/4项目/7资金事件/2媒体/4订阅/6续费/5状态；Room v2身份不变，integrity=ok，foreignKeyCheck=[]。两私有封面原字节、active指针/ready/设置、AVD描述/config哈希相同；12项实际登记guest设置完全恢复。见[数据与设备核对](ui-c1/refinement/device-preservation.json)。原DB/WAL/SHM和私有原图仅留本地ignored目录，没有推送。
- 原AVD起初响应异常，先快照，再正常关闭并重新启动**同一现有AVD**，未wipe/清数据/新建镜像或改时钟；重启后旧事实和设置再次核对。Digital Wellbeing ANR弹窗关闭后，原三焦点失败以未改生产输入复跑3 PASS。根因/限制见第4节；[启动检测](ui-c1/refinement/boot-check.json)不表示应用性能基准。

## 8. 图像来源与复审范围

27张实际原始PNG：**19张MainActivity QA（正常6＋压力/滚动13），6张合成物品隔离宿主，2张独立空Room宿主**。逐图SHA/尺寸/主题/系统字体与密度/前台Activity/安装APK指纹在[截图登记](ui-c1/refinement/screenshot-manifest.json)。MainActivity使用系统主题；宿主显式传LithicTheme，目标APK＋测试APK双SHA绑定，来源不能混同。所有27图已实际查看，不使用修图、截图拼贴或外部系统遮罩图作为合格样板。

合成数据观察日2026-10-15：轻薄笔记本7999元/653天、通勤耳机999元/684天、旅行相机4999元/745天；另有音乐年卡198元/365标准天和云盘18元/30标准天。数值来自生产Repository和冻结领域引擎；三张封面为测试Canvas原创几何图，经原导入规范化后保存，截图前后校验规范化存储字节，不能声称原输入未被首次规范化。详见[fixture来源](ui-c1/refinement/fixture-source.json)。fixture不写默认活动库，应用Application默认owner仍可能常规打开活动库，禁止宣称整个进程从未打开。

正常列表首屏已出现原QA封面；详情正常首屏封面与日均/持有天数均清晰可见。360dp/200%下大字体保留，需要滚动；首页08/17负金额卡与新增分离，详情14显示日均与天数，19显示全部三个累计金额，11/13菜单完整。15/18只是滚动中间视口，不能据此声称所有标签同屏。辅助语义/48dp由自动测试及实际UI层级核对；完整TalkBack仍NOT_RUN。

采集发现02/03转场/读取中及06切换密度时的混合元数据，原字节与原因保留在[排除清单](ui-c1/refinement/disqualified-manual.json)，以21/22/20替代，不加入27张复审图。首轮6张被外部ANR遮挡的宿主图只在diagnostics保留。手工UI层级读取曾返回null，保留错误后重取唯一临时XML，不复用旧XML；后续采集留出原导航转场时间。前台检查要求MainActivity，菜单额外核对附属窗口包名/owner/父Activity；未修改App导航动画或业务时序。

## 9. 文件、Git与停止点

本轮文件与提交前核对见[文件清单](ui-c1/refinement/files.md)及[检查登记](ui-c1/refinement/precommit-checks.json)。只提交四个生产Compose展示文件、相关测试/当前文档/新证据；无删除、无依赖/schema/业务引擎/Repository/媒体协议/导航守卫改动。原53fd944的19张图、旧报告/checkpoint、M10失败、正式矩阵/原130案例不修改。

基线`53fd944fe70a912fd4c1e1fa76d4ec31d1a103d5`，仅`feat/ui-c1-visual-redesign`；最终提交SHA与推送一致性由最终Git回执给出，避免文档递归引用自身提交。原M10工作树保持`41596820145dc4a3fa6d041728a59540d828108a`，main保持`145326ddd4ce28abbf7c2706fe25f9d38c599c3a`。不发PR、不合并、不公开APK、不改WHOCARE。

**停止：VISUAL_REVIEW_REQUIRED — UI-C1（视觉精修复审）**。待用户和ChatGPT看实际图；不自行接受/冻结，不进入Task6～Task8或M11。真机/API26/OEM/Release/完整TalkBack/真实LMK、备份恢复与原后续边界继续NOT_RUN；视觉是否达到参考标准由用户确认。

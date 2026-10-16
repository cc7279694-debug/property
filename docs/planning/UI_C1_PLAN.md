# UI-C1 Implementation Plan — 分阶段原生双主题改版

> **For agentic workers:** 书面规格与本计划未获用户确认前不得实施。确认后按已授权阶段使用 superpowers:subagent-driven-development 或 superpowers:executing-plans；每完成阶段门禁立即停止，不能把此计划当作后续阶段的自动授权。

**Goal:** 在现有原生功能上实现用户两图所选的简约双主题，先交付可审查的三页真实样板，再由用户看图决定扩展。

**Architecture:** 维持Compose → 当前ViewModel/只读展示状态 → 既有Repository/领域引擎。主题/视觉组件复用；列表读取、同代图片校验和三VM守卫继承。仅首页UI持有器组合已有有界接口，不新建业务层或数据库owner。

**Tech Stack:** Kotlin2.3.10、Compose BOM2025.11.01（UI1.9.5/Material3 1.4.0）、Navigation3 1.0.1、Gradle8.14.3/AGP8.13.2/JDK17、min26/target36；Room2.8.5/schema2与现有依赖全部锁定。

**Spec:** [UI_C1_SPEC.md](../design/UI_C1_SPEC.md)，来源为[完整v2](../design/ui-c1/source-v2/CODEX_PROMPT.txt)。日期2026-10-15。阶段0已获用户及ChatGPT书面确认；本轮仅执行阶段1 Task1～5。实际逐项结果见[阶段1报告](../evidence/UI-C1-stage1-verification.md)，不以阶段0NOT_RUN或下方原计划勾选框替代执行登记。

## Global Constraints

- 基线 `41596820145dc4a3fa6d041728a59540d828108a`，仅分支`feat/ui-c1-visual-redesign`、独立worktree；不改M10分支/main/WHOCARE，不自动整合JPEG修复。
- 不改schema/实体/迁移/DAO、持久结构、领域引擎、业务Repository、媒体协议、应用身份/签名/versionCode/权限/依赖；图片加载只能UI层受控展示。
- 默认系统双主题，无主题持久设置；金额保留精度、null/0/负值/微小非零；两种订阅日均与两种时长严格区分。
- 不实现M13汇总/搜索/全局筛选/统计/备份/提醒；不把原图示例或品牌素材放进生产账本/资源。
- 未保存/IN_FLIGHT/UNCONFIRMED/COMMITTED及稳定操作ID必须继承；保留所有旧业务断言与三项JPEG拒绝失败。
- 原正式ACCEPTANCE_MATRIX、原130案例、16组跨模块边界和历史证据不改；UI验收在新UI证据中登记。
- 只用已有专用AVD，禁止重建/wipe/卸载清数据/改宿主时钟/指针造空态；测试后恢复设置并保护原账本/媒体。

## Review Focus

1. 全部身份count与当前管理子集不一致：历史或停止记录仍能理解，不能误标活跃数量（Task2）。
2. 首页旧读取回传晚于换代/重试/写入：不得覆盖新状态或显示旧代图片（Task2/3）。
3. 360dp、200%字体与负长金额：完整数值、状态、提交及返回仍可达（Task4/7）。
4. 弹层、Tab、详情更多菜单绕过隐藏pending：三VM优先阻止离开，不能丢输入或重复付款（Task5/7）。
5. 同资源图片坏文件、共享引用、缓存重入与大量滚动：仍走openAsset校验，取消/换代与8MiB缓存/并发2可证明（Task3/8）。

## 阶段与停止点

| 阶段 | 允许工作 | 交付与门禁 |
|---|---|---|
| 0 已书面批准 | 读来源/代码，写本规格/计划，原件归档和文档验证 | 原`DESIGN_READY_FOR_CONFIRMATION`历史保留；本次用户及ChatGPT已确认 |
| 1 当前交付 | Task1～5：共用主题/导航所需装配、现在/项目列表/项目详情 | 实际三页×深浅6图+首页空态2图+360dp/200%关键图；`VISUAL_REVIEW_REQUIRED — UI-C1`，待用户实际看图 |
| 2 看图确认后 | Task6～8：订阅/回看/设置/弹层和所有旧表单统一，完整回归 | 最终APK全8组×双主题16图及压力图，`READY_FOR_REVIEW — UI-C1` |
| 3 独立验收 | 用户与ChatGPT评审，按反馈范围修正 | 不自授ACCEPTED/FROZEN；M10 JPEG仍独立阻塞，不进入M11 |

## 允许文件与职责

下列路径以`app/src/main/java/com/cooper/lithic/`为根，在阶段0核对；“拟新增”是编制时分类。阶段1已获授权的真实文件见执行报告，阶段2文件不得据此提前创建。

| 阶段 | 文件 | 修改理由/保持接口 |
|---|---|---|
| 1 | `core/design/LithicTheme.kt`、`EntryCard.kt`、`AmountText.kt` | 色板/字号/形状、图文卡片和金额单位布局；保留AmountFormatter行为及原公开参数兼容 |
| 1 | 拟新增`core/design/UiPrimitives.kt` | 少量PageHeader/StatusBadge/EmptyState等，仅相同行为复用，不做万能页面配置 |
| 1 | `app/LedgerUiState.kt`、`LedgerUiStateHolder.kt`、`feature/home/HomeShell.kt` | 新增只读预览状态/发布保护和真实概要，不触及存储owner或持久SavedState结构 |
| 1 | `feature/media/CoverDisplay.kt`；拟新增`CoverPreviewLoader.kt`同目录 | 单一展示加载器、并发/采样/缓存；保留openAsset和原查看/管理回调 |
| 1 | `feature/project/ProjectListScreen.kt`、`ProjectDetailScreen.kt` | 连接真实封面、卡片、分组和全部旧操作；历史页只作共享组件必要兼容 |
| 1 | `app/LithicNavigation.kt`；`LithicRoutes.kt`主要只读继承 | 原NavKey/来源链与requestLeave不变；新UI回调仍交由全局守卫，不改命令恢复逻辑 |
| 1/2 | 拟新增`feature/home/AddRecordSheet.kt`、少量`res/drawable/ic_ui_*.xml` | FAB两选项、取消零写、原创/已核验本地图标；样板所需最小弹层，阶段2补全一致性 |
| 2 | `feature/subscription/SubscriptionListScreen.kt`、`SubscriptionDetailScreen.kt`、`SubscriptionHistoryScreen.kt` | 主次指标/分组/历史/旧操作可达，状态公式不复制 |
| 2 | `feature/review/ReviewShell.kt`、`feature/settings/SettingsShell.kt` | 真实入口/标准分组/运行版本/风险提示；不新建未实现功能 |
| 2 | `core/design/FormFields.kt`、已有project/subscription表单/对话框、`feature/project/CoverPicker.kt` | 统一视觉，字段/raw/校验/事务/身份/确认回调不变；逐文件列清单后最小改 |
| 各阶段 | `app/src/androidTest/...`现有UI测试与少量新测试；新UI文档/截图 | 保留旧方法/业务断言，记录语义等价的选择器/文案适配；不动JPEG fixture/断言 |

`data/`、`domain/`、`core/model`、`core/time`、schema/config/工具链为保护区；必要只读调用不等于修改授权。不顺手拆分大文件或重构所有ViewModel。

## Task1 — 实施前冻结真实基线与主题规则（阶段1，已授权）

**Files:** `LithicTheme.kt`、候选UI组件及新增`androidTest/.../ui/UIC1ThemeTest.kt`（拟新增）。

**Consumes:** 既有`LithicTheme(darkTheme, content)`、`LithicSpacing`、AmountFormatter、当前依赖与152输入登记。

**Produces:** 相同主题入口，明确的palette/typography/shapes、可复用小组件；系统切换只影响展示。

- [ ] 再核对UI/M10远端、worktree、用户确认记录；如M10前进只报告，不自动合并。记录已有AVD身份、原数据/媒体、设置和APK，不安装或清理其他设备。
- [ ] 在未改产品代码前真实运行JPEG5项及受影响UI路径基线，保留完整命令/XML/退出码/输入；精确确认3个拒绝FAIL，不把历史2PASS当新执行。出现不同失败先调查。
- [ ] 写主题/字号/状态/金额可读性目标测试并实际观察RED，再实现palette/字体/形状最小改动；无界面代码镜像测试，主要用可见结果和对比度/语义断言。
- [ ] 保留`<¥0.01`、`−<¥0.01`、负号/空值的原格式和中文朗读，验证12候选之外所有实际交互/状态配对；触控≥48dp。
- [ ] 定向测试和Debug/Lint验证，保存输入及运行结果；提交单一主题/基础组件改动，不声称整个UI完成。

## Task2 — 首页真实概要与只读预览（阶段1）

**Files:** `LedgerUiState.kt`、`LedgerUiStateHolder.kt`、`HomeShell.kt`；拟新增`androidTest/.../app/UIC1HomePreviewTest.kt`。

**Consumes:** `ProjectRepository.loadSummary()` / `loadCurrentProjectWindow(1, asOf)`、`SubscriptionRepository.loadSubscriptionCount()` / `loadManagementPage(false, null, 3, asOf)`、原`changes`、同一个`BusinessClock.today()`。

**Produces:** 生命周期内home预览（项目/订阅各最多3条）、准确数量、独立loading/error/stale状态；HomeShell继续只发导航回调。

- [ ] 先写并实际运行数据范围测试：全身份计数含历史/停止；当前子集可能为空或未来开始；每类展示≤3；项目接口有界50+哨兵而非新SQL3。订阅既有全库分批事实预检不改，不承诺整个读取O(3)。
- [ ] 写请求交错测试：旧请求/旧repository在新请求或换代后结束、重试与changes相撞；预期不能发布旧结果。沿用一个asOf，增加只在UI持有器内的请求序号和repository身份核对，不宣称跨对象原子快照。
- [ ] 复用既有接口完成只读组合，不调用snapshot()加载全账本；UI不持久化预览/派生日均、不新建owner。保留失败时上次数据显示及明确重试。
- [ ] 以成功全空/单类型有数据/仅历史或已停止/加载/失败/上次数据分别验证首页；只有成功读空显示首次新增。非空FAB和空态主按钮互斥，不生成假总额、伪最近活动、搜索/筛选。
- [ ] 测试GREEN或失败如实登记；保存窗口/调用范围证据，提交首页展示改动。

## Task3 — 真实封面和轻量图文卡片（阶段1）

**Files:** `EntryCard.kt`、`CoverDisplay.kt`、拟新增`CoverPreviewLoader.kt`与`androidTest/.../feature/media/UIC1CoverPreviewTest.kt`。

**Consumes:** `MediaRepository.openAsset(assetId): MediaResult<StoredImage>`、现有assetId/元数据；原查看和管理回调。

**Produces:** 共用UI预览加载器（repository归属+assetId+目标尺寸键、取消、并发2、LRU8MiB）、可复用列表封面slot和详情预览；媒体协议零修改。

- [ ] 先测试未设图、有效图、有引用但缺失/校验失败、取消/重入、换repository、关闭旧代及共享引用；失败必须可见，不能使用旧成功缓存掩盖失败。
- [ ] 测试并发≤2、缓存计费≤8MiB、超限单图不入缓存、列表/详情采样、重复重组不重复加载；重入先openAsset验证，不能外包第二层非重入锁。资源预算是加载器范围，不是整个进程硬上限。
- [ ] 在IO读取及解码；按尺寸采样，Lazy key用真实身份；保存的图像文件字节/资源引用零写。别手工recycle仍被显示的Bitmap。
- [ ] 卡片/详情接入已校验真实字节，空图显示本地图标、失败保留错误/管理入口；原图查看Fit，卡片Crop不写文件。
- [ ] 定向测试、滚动与取消证据通过后提交；若优化需改Repository/协议则停步列问题，不混入JPEG修复。

## Task4 — 项目列表和详情三页样板（阶段1）

**Files:** `ProjectListScreen.kt`、`ProjectDetailScreen.kt`、共享组件；继承`ProjectHistoryScreen.kt`及`ProjectScreensTest/ProjectLifecycleScreensTest`。

**Consumes:** `ProjectListUiState`、`ProjectDetailUiState`、`ProjectDetail.cost`及原点击/分页/编辑/资金/结束/删除/封面回调。

**Produces:** 保持相同业务行为的图文列表、分组详情、可见的更多操作和统一空/错状态。

- [ ] 为未来开始、未来结束、零/负/微小日均、结束后结算、长中文和大负金额写可观察等价测试；保留历史来源、原排序/分页及所有旧断言。
- [ ] 详情主指标与次级事实分组，200%字体转纵排；更多操作有标签；结束/删除分色分义，删除保留原确认和影响说明，所有资金记录可读。
- [ ] 小屏/大字体真实运行验证金额、状态、错误、返回、继续加载可达；不固定卡高/缩字体或截关键金额。
- [ ] 定向回归并记录合法UI选择器变化的“旧断言→新等价行为”；不删除测试/延长超时换绿。

## Task5 — 导航/FAB守卫及第一道视觉门禁（阶段1）

**Files:** `LithicNavigation.kt`、`AddRecordSheet.kt`、本地小图标；既有`ui/NavigationShellTest.kt`、`feature/subscription/M09SubscriptionNavigationGuardTest.kt`、`M09SubscriptionNavigationFlowTest.kt`、`feature/media/M10CoverNavigationGuardTest.kt`及拟新增`ui/UIC1NavigationTest.kt`（测试路径根为`app/src/androidTest/java/com/cooper/lithic/`）。

**Consumes:** 原NavKey/来源链、`requestLeave`、三个ViewModel的leaveDecision与原恢复入口。

**Produces:** 三Tab图标+文字、一个新增入口/两行弹层；用户确认前的三页样板证据。

- [ ] 新增关闭弹层零写、两选项进入原创建、dirty确认、任一VM pending阻止、COMMITTED只读、隐藏待核对任务可达的测试；返回/旋转/系统主题切换保持固定ID与raw。
- [ ] 接入原全局requestLeave，不把菜单取消当业务discard，不新增第二套路由，不改Serialized NavKey/来源；仅必要UI装配。
- [ ] 原业务与导航定向回归；阶段内重新运行JPEG5项并逐项报告真实结果。它们不进入跳过/通过清单。
- [ ] 在已有专用AVD上通过隔离测试存储或明确QA对象使用生产接口建合成资料，拍现在/项目列表/项目详情各Light+Dark共6张正常原图；另拍首页全空两张及360dp/200%首页和详情关键图。用测试隔离Application/容器展示真实业务Compose，清楚标“测试宿主”；最终APK手工图与测试宿主图分别登记，不拿后者冒充最终APK安装图。禁止篡改正式generation指针。
- [ ] 截图登记APK SHA/工作树完整输入、设备/API/dp/font/theme/fixture/页面状态；保留未缩放原图，拼图只作总览。无设备则NOT_RUN，视觉门禁不通过。
- [ ] 恢复guest主题/字体/网络/IME/旋转；核对原账本/媒体。独立命名样板APK，不覆盖原M10交付物；不改code8/applicationId/签名。版本从实际产物核对。
- [ ] 更新唯一CURRENT_STATE、新UI证据/checkpoint并推UI分支，返回`VISUAL_REVIEW_REQUIRED — UI-C1`；**待用户实际看图确认，不做Task6～8**。

## Task6 — 其余列表、详情与可用主页面（阶段2，现已授权）

**Files:** 订阅三Screen、ReviewShell、SettingsShell，继承原SubscriptionScreens/M09SubscriptionScreens等测试。

**Consumes:** `SubscriptionDetail.cost/currentRenewal`及原所有状态/续费/纠错回调、现有历史入口、recordZone、Context PackageManager版本。

**Produces:** 与样板统一的订阅主次指标、完整旧操作和真实回看/设置页面。

- [x] 先测试历史平均/折算时长、当前标准日价/周期独立、未来预付、暂停/到期/停止、固定计划及同身份重开文案/行为；不用图中“已使用/已付天数”。
- [x] 只改展示与分组，所有状态/付款/取消/修正原回调保留，服务商取消说明可见；无新通用编辑功能。
- [x] 回看仅两个真实入口，无来源count不显示；设置只读行无假箭头，实际版本，开发版/备份未完成风险保留；未实现功能隐藏。
- [x] 深浅/空错/历史路径/阶段1样板定向回归，记录真实结果并提交。

Task6执行证据见[独立记录](../evidence/UI-C1-task6-verification.md)：定向结果按各真实输入登记，不能代替Task8全量；提交后进入用户已授权Task7。

## Task7 — 全部旧表单/对话框一致化（阶段2）

**Files:** FormFields及项目/订阅/封面已有Editor/Dialog逐项列实际清单，不改ViewModel业务命令/持久信封。

**Consumes:** 原字段/raw/错误、初值、稳定ID、阶段与确认回调；原Nav3安全返回。

**Produces:** 与主页面相同的输入/错误/按钮/弹层视觉，原业务验证强度和恢复行为不变。

- [ ] 用真实IME、raw精度错误、键盘Back、日期/金额校验、重复点击、结果未知/只读重入验证行为；样式变更先RED再最小实现。
- [ ] 三VM全局守卫全部回归，覆盖隐藏pending、旋转、保存后返回与未保存确认；不能SideEffect重置新导航，不能为了简洁删确认。
- [ ] 长说明/状态/金额在360dp200%可滚动，布局/焦点/标签与48dp实际核对；逐条记录原驱动的等价调整，保持业务断言。

## Task8 — 最终证据、全量回归和独立交付（阶段2）

- [ ] 冻结同一最终完整源码/资源/构建输入；每次命令保存真实退出码/XML/日志/指纹。执行：

```powershell
.\gradlew.bat :app:testDebugUnitTest --max-workers=2 --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=2 --console=plain
.\gradlew.bat :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain
```

- [ ] 统计实际唯一方法数（起点657不预设最终数），检查原方法身份和断言；三项JPEG仍按原class#method/拒绝断言报告FAIL，总connected如实FAIL。诊断PASS不是拒绝PASS；若失败集合改变先调查。不同阶段/UP-TO-DATE/旧652不能合成一次全量成功。
- [ ] 核对最终APK应用身份、code/version、签名兼容、Manifest/禁止权限、测试资源未进生产、schema/依赖/保护区差异；用独立文件名保留UI APK，登记绝对路径/大小/SHA，不上传APK。
- [ ] 用同一最终输入/最终APK重新取得现在、项目列表、项目详情、订阅列表、订阅详情、回看、设置、新增弹层各深浅16张正常原图；另留空态/错误/360dp200%/真实IME证据。阶段1旧图不能代表新最终APK；不同宿主证据类型分开。
- [ ] 对照两图逐页记录留白/标题/封面/卡片/按钮/导航及业务差异，不把代码里有Card/Icon当视觉通过；保留没有覆盖的平台/TalkBack/Release/LMK等NOT_RUN。
- [ ] 原账本/私有文件/设置前后核对；不清库/重建AVD/偷偷处理JPEG。更新UI证据和CURRENT_STATE，正式矩阵/历史原件不改。
- [ ] 差异/凭据/链接检查，Conventional Commit，只推UI分支。返回`READY_FOR_REVIEW — UI-C1`，分别报告UI视觉待验收、全量真实结果、M10仍PENDING；立即停止。

## 独立UI验收清单（保留阶段0编制时全部NOT_RUN）

下表是批准时的基线，不是本次运行状态。阶段1逐项适用范围、实际结果及剩余NOT_RUN见[执行登记第7节](../evidence/UI-C1-stage1-verification.md#7-阶段1验收登记不覆盖正式矩阵)。Task1～5已实施并停在VISUAL_REVIEW_REQUIRED；未获用户实际样板确认。Task6～8不勾选、不执行。

| 编号 | 观察结果/证据要求 | 阶段1范围 | 最终范围 | 当前 |
|---|---|---|---|---|
| T01 | 同结构深浅；切系统主题零业务写入 | 三页/空态 | 全站 | NOT_RUN |
| T02 | 首页真实卡片/空态，无假汇总 | 完整 | 回归 | NOT_RUN |
| T03 | 弹层关闭零写，两原创建路径 | 样板入口最小闭环 | 一致化全验 | NOT_RUN |
| T04 | 原排序/分页/当前历史，无假搜索筛选 | 项目；订阅未测部分保留 | 两类型 | NOT_RUN |
| T05 | 真实/无/坏/缺图、并发/采样/缓存与零业务写入 | 样板 | 所有封面 | NOT_RUN |
| T06 | 项目金额/时间/结束结算不变 | 完整 | 回归 | NOT_RUN |
| T07 | 订阅主次指标/时长/状态不混淆 | 尚未改的部分不计通过 | 完整 | NOT_RUN |
| T08 | 原动作可达，结束/删除/服务商说明准确 | 项目；订阅未测保留 | 全部 | NOT_RUN |
| T09 | 三VM、Back/旋转/固定身份/恢复不回归 | 导航改即三VM验证 | 全站 | NOT_RUN |
| T10 | 初始化/空/错/上次数据明确 | 首页/项目 | 全站 | NOT_RUN |
| T11 | 360dp200%/长名长负额/真实IME可达 | 首页/详情关键场景 | 全站 | NOT_RUN |
| T12 | 实际配对/标签/触控/焦点/状态可读 | 样板，辅助技术未测保留 | 全站适用范围 | NOT_RUN |
| T13 | 断网冷启/浏览/旧操作，无新权限/持久结构 | 样板及静态差异 | 最终APK/全回归 | NOT_RUN |
| T14 | 用户先看样板再扩展；视觉/代码/独立验收分开 | 8原图+压力图，待确认 | 最终16原图+压力图 | NOT_RUN |

## 已知问题与本轮停止

M10原652是旧147输入的历史PASS；新增5项实际2PASS/3FAIL，UI起点源码657、输入152。三项为`M10JpegIntegrityTest.jpeg_entropy_truncated_with_eoi_is_rejected`及`M10JpegOldCoverProtectionTest`项目/订阅两拒绝方法，完整名称见[补充报告](../evidence/M10-JPEG-supplement-verification.md)。本次最终UI输入174，实际86 JVM＋102 Android＝185 PASS/3 FAIL，新增35全PASS；旧结果只作历史，不能替代新输入运行或Task8全量回归。

已知全库事实预检性能仍归M22；API26/真机/OEM/Release/完整TalkBack/LMK/断电及M11/M12往返保留NOT_RUN。缓存和首页请求发布保护已实施并有定向测试，实际边界见阶段1报告，不扩张成整个进程硬预算或原子快照。Q05 USER_CONFIRMED，不整体关闭。

阶段0在`edc8be7`完成；本次用户已确认本规格/计划并只授权Task1～5。当前实施结果、输入、测试和样板见阶段1报告；完成后停在VISUAL_REVIEW_REQUIRED，等待实际看图，不能据此开始Task6～8或M11。原阶段0证据/checkpoint保留当时NOT_RUN。

## 阶段1精修 R1（用户已授权）

**Goal：**落实用户转交的53fd944截图审查六项CHANGES_REQUESTED，使三页首屏接近已选参考的数字/图文层级，并消除新增按钮覆盖。

**Locked：**既定色板/系统字体缩放、产品与页面职责、真实计数/冻结成本口径、Room/DAO/领域/Repository/媒体协议、Nav3守卫、身份/依赖/签名、全部旧测试身份及JPEG三FAIL。

**Adjustable：**HomeShell的真实双指标、单空态与标题栏新增；ProjectListScreen紧凑头部；ProjectDetailScreen封面/指标比例和返回/菜单；CoverDisplay只增加可选裁切比例；相关UI测试和隔离展示证据。无Task6～8、M11或M10修复。

- [x] 原工作树/远端/设备/七表/媒体/设置核对，保留53fd944全部旧证据。
- [x] 新布局测试先真实RED，再最小改UI。首页双数字/空态唯一/新增与内容几何不交叠；列表首卡位置/200%头部；详情封面/指标同首屏、48dp短返回与完整可读菜单。原测试只做等价选择器/布局驱动适配。
- [x] 同一候选定向执行新旧UI/全部35旧UI新增方法/三VM及项目流程/媒体入口/原5JPEG，JVM回归、Lint和Debug构建；真实结果逐方法/输入/退出码归档，不能称Task8全量。
- [x] 最终安装APK拍MainActivity三页深浅六图、360dp200%与新增不遮挡。隔离真实Room/原Repository/引擎+测试时原创封面另拍合成物品六图；独立空Room深浅两图。每图原像素/来源/主题/大小/实际APK指纹登记。
- [x] 原七表、两封面、默认指针与设置精确核对，恢复guest所有改动；检查差异/链接/敏感信息，提交推送UI分支。
- [x] 停在VISUAL_REVIEW_REQUIRED — UI-C1（视觉精修复审），等用户看图，不自授接受。

本轮计划不另起业务模块或平行当前状态；实际结果记新精修报告与checkpoint，不改阶段1旧交付或正式矩阵。

本轮实际：181输入，86 JVM全PASS；161 Android157 PASS/4 FAIL，原表单同输入单项复跑PASS但保留偶发时序风险，JPEG三FAIL原样。27张原图已分源，旧692身份＋新增9＝701，全量Task8未跑。新[精修报告](../evidence/UI-C1-refinement-verification.md)和[图册](../evidence/ui-c1/refinement/GALLERY.md)为当前证据；上方174输入等属于53fd944初次交付历史，不覆盖旧结论。停止VISUAL_REVIEW_REQUIRED，未批准视觉。


## 阶段2正式执行（当前用户已授权）

**来源：**[用户完整指令](../design/ui-c1/source-stage2/USER_INSTRUCTIONS.md)，SHA-256 `3f21544a2338b66dd111a4e1fe90625098287a6ae6a52155349263748dc0d001`。用户已实际确认f000639精修27图，ChatGPT视觉复审PASS WITH NOTES，当前允许Task6～8。上方所有阶段0/1等待、未授权及旧测试数量为历史，当前不能继续用作阻碍。

**Goal / Scope：**同一视觉品质扩展全部已实现页面和表单；有限Compose组件/UI装配/测试/文档。Task6订阅三页、回看、设置和项目历史；Task7枚举并统一全部原表单/对话框，专项诊断旧保存超时；Task8同一最终输入无过滤全量、最终APK16主图及空/错/压力/IME/历史/危险确认图、T01～T14逐项证据。

**Locked / Out of Scope：**阶段1已确认三页布局，所有业务/金额/日期/状态/领域/Repository/Room v2/DAO/迁移/owner/媒体协议；三VM持久信封/固定身份/Nav3守卫，身份/签名/版本/权限/依赖，原701方法和业务断言，历史规划/证据/正式矩阵。无JPEG修复、M11、主分支、WHOCARE或新功能。

**依赖与验证顺序：**Task6复用现有设计/媒体预览和只读原模型；先新布局/文案测试RED，再最小展示变更； root独占设备/构建。定向GREEN、深浅截图、本地代码复核及Task6提交后才Task7生产修改。Task7先实际清单和旧超时诊断，保留历史，不简单加时/盲跑。Task8先冻结所有输入，再JVM/Android无class过滤/Lint+assemble三条命令逐条日志/退出码/XML/方法集合；原JPEG三失败导致整体FAIL，如实报告，不强改GREEN。全量期间不改输入；若改必须重新完整跑最终输入。

- [x] 核对唯一UI工作树、本地/远端、原账本/两封面/generation/ready/settings/记录时区/设备设置，保存源码身份701。
- [x] Task6新布局/指标与可用页面RED→展示实现→定向测试/深浅原图/代码复核→独立提交。
- [x] Task7实际清单、旧超时诊断、表单展示与定向验证完成；范围明确提交。完整三VM/IME/旋转与全量核查继续由Task8最终输入验证，不把定向结果当全量。
- [x] Task8同一最终完整输入无过滤全量、方法/业务断言核对，记录真实FAIL及未测。
- [x] 最终APK实际安装与16主图+状态/空态/压力/真实IME/历史/确认图，每图双APK/来源/前台/设置指纹；逐图看原像素并直接对照两参考原图。
- [x] 七表完整事实/原媒体字节/时区/同代与设置前后核对、恢复guest改动；T01～T14逐项范围和缺失证据。
- [x] 新报告/checkpoint/唯一当前状态、差异/链接/凭据检查，仅UI分支推送，READY_FOR_REVIEW — UI-C1后停止；不自授ACCEPTED/FROZEN。


### Task7 execution contract (2026-10-15)

Task6 committed `e9bb44f` before Task7 implementation. Goal: extend approved visual system across the 8 actual form implementations and existing dialogs. Scope: presentation-only shared headers/controlled inputs, existing form layout, dialog touch targets and UI tests. Protect every raw value/date, field error, callback, enabled predicate, branch, operation identity, three VM guards and all storage/domain/media protocols. No new business flows, dependencies or permissions. [Actual inventory](../evidence/ui-c1/stage2/task7/form-inventory.md) identifies all reused modes. Verify new visual contracts against old source first, then existing forms/date/guard/cover tests; diagnose the historical save timeout without changing its 10-second wait or removing assertions. Final same-input whole-suite evidence and fresh final APK/gallery belong to Task8.

Task7真实结果与新测试期望纠正见[执行报告](../evidence/ui-c1/stage2/task7/verification.md)。旧保存间歇根因继续UNKNOWN；M10三JPEG保留FAIL。


### Task8最终实际交付（2026-10-16）

Task6～8实施与验证已收口，阶段结果READY_FOR_REVIEW，独立验收PENDING。187项同一最终输入，718方法715PASS/3原JPEGFAIL，整体FAIL；原701保留新增17。首轮22项资源字节失败及修正后的全量重新运行分别保留。最终68张同APK原图、真实IME/360dp200%、七表/两封面/代际/13项设置精确核对，见[阶段2报告](../evidence/UI-C1-stage2-verification.md)与[最终图册](../evidence/ui-c1/stage2/FINAL_GALLERY.md)。最终提交/仅UI分支推送以最后Git回执确认。原阶段0表格保持NOT_RUN历史，不写正式矩阵、不自授冻结、不修M10/进入M11。

# 砺石 LITHIC UI 重设计 · 第一阶段

**LITHIC-UI-R1-P1 · READY_FOR_DESIGN_REVIEW**

推荐保留砺石的黑石图标、三套主题和原生 Android 交互。首页集中呈现一个成本主数字，随后用两组易扫读的记录行承接查看与操作。岩白雾蓝保留暖白页面，成本摘要使用明确的雾蓝表面；纯白用中性浅灰分区；黑灰用表面明度形成层次。

## 1. 审查基线与证据

| 核对对象 | 结论 |
| --- | --- |
| 正式签名交付源码 | `codex/v1-private-apk-release`，`d42a78125b83bee806add5e3fdd8d2328055b694`。Git origin 为 `https://github.com/cc7279694-debug/property.git`。正式候选 `com.cooper.lithic / 1.0.0 / code36` 由 Release VariantOutput 设置。签名交付报告保留正式发行 NO-GO 和端侧待验。 |
| 现状文档差异 | `docs/CURRENT_STATE.md` 顶部仍是 2026-11-05 的 Preview19 状态。2026-11-06 的正式交付报告与实际 Release VariantOutput 对上 code36。三主题、页面行为与入口按上列提交的源码盘点。 |
| 工作区和远端 | 原工作区为干净的 `feat/m10-local-cover-media`（`41596820`），正式发行工作树亦干净。审查基线与远端同名分支一致，核对当时 84 个远端分支头后未发现更新的相关交付。设计在独立分支 `codex/lithic-ui-r1-design-review` 上完成，创建时工作树干净。 |
| 当前运行画面 | **未找到 code36 对应的当前 Android 运行截图，视觉表现待核实**。源码证据可确认布局与分支，不能确认真机观感或可访问性。 |

依据 [AGENTS.md](../../../AGENTS.md)、[PROJECT.md](../../../PROJECT.md)、[CURRENT_STATE.md](../../CURRENT_STATE.md)、[DECISIONS.md](../../DECISIONS.md)、[ARCHITECTURE.md](../../ARCHITECTURE.md)、[当前 Release 构建配置](../../../app/build.gradle.kts#L370)、[V1.0 私人 APK 发行记录](../../evidence/LITHIC-V1-Private-APK-Release.md)。

| 来源 | 提交与适用范围 |
| --- | --- |
| UI-C1 浅深原图、旧 gallery | `feat/ui-c1-visual-redesign` · `0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`。原图 `docs/design/ui-c1/source-v1/ui-reference-{light,dark}.png` 和 `docs/evidence/ui-c1/refinement/GALLERY.md`。森林绿阶段的历史参考。 |
| UI-C2 首页浅深对照图 | `fef1f8efcf9afe170d36f686ee02a7c3d88c2f5f` · `docs/design/ui-c2/PROTOTYPE_ONLY-HOME-{light,dark}-comparison.png`。历史 HTML 原型。 |
| M22 局部 Compose 截图 | `5a446ee2d36cf1ed86e7d544787e7913acbc7301` · `docs/evidence/m22/screenshots/ui-index.json`。2026-10-23、API36、隔离 ComponentActivity 与合成状态。ui-01 项目列表（主题未登记）、ui-02/03 浅/深色负金额、ui-05 系统 2 倍字体表单、ui-07 恢复预览。捕获范围外曾有 SystemUI ANR 弹层，证据限于局部 Compose 图。 |
| 本轮七个视图 | HTML/CSS 概念样稿，使用合成内容。当前 App 视觉仍待 Android 核实。 |

### 本地设计参考

| 已读取资料 | 实际采用 |
| --- | --- |
| UI UX Pro Max · `%USERPROFILE%/.agents/skills/ui-ux-pro-max/SKILL.md`，`data/ux-guidelines.csv`，`data/stacks/jetpack-compose.csv`。入口未声明版本。 | 主数字优先，标题与操作分层，稳定对齐。以 Kotlin / Jetpack Compose 为落地目标。 |
| Material 3 · `%USERPROFILE%/.agents/skills/material-3/SKILL.md`，`references/color-system.md`，`layout-and-responsive.md`，`navigation-patterns.md`。入口未声明版本。 | 表面与文字色配对，底栏选中态有形状及语义，保留至少 48dp 操作区域、系统安全区和键盘适配。 |
| Mobile Android Design · `%USERPROFILE%/.agents/skills/mobile-android-design/SKILL.md`，`references/android-navigation.md`，`details.md`。入口未声明版本。 | 用于列表到详情的原生返回、较窄窗口重排与标题/内容可滚动性。 |
| Impeccable 4.1.0 · `E:/CODEX/.codex/skills/impeccable/SKILL.md`，`reference/android.md`，`reference/audit.native.md`。 | 仅参考文本，检查同类卡片堆叠、主操作位置、长文本与状态辨识。未运行其检测器、脚本或 Hook。 |

## 2. 当前页面与组件

源码定位以下均相对于 `app/src/main/java/com/cooper/lithic/`。

| 页面或组件 | 核心信息、操作与已有状态 | 源码 |
| --- | --- | --- |
| 现在 | 当前日均、截止日期与非今日扣款；项目和订阅预览、含历史的已录入数量、分项及类别、搜索、圆形新增。区分空账本、只有历史无当前贡献、读取失败、旧结果及真实 0。 | `feature/home/HomeShell.kt:53–134,139–158,179–213` |
| 项目当前及历史列表 | 名称、类别、状态、持有时间、截至相应日期的日均、已认证封面或图标。支持分页、失败重试及保留旧结果。 | `feature/project/ProjectListScreen.kt`、`ProjectHistoryScreen.kt`、`core/design/EntryCard.kt:47–87` |
| 项目详情 | 身份、封面、类别、状态、持有日期与天数、日均、净投入与投入/收回明细；主动作「记一笔」，管理菜单保留编辑、纠错、结束、替换/以旧换新、危险删除与封面操作。 | `feature/project/ProjectDetailScreen.kt:155–225,255–359`、`feature/media/CoverPreviews.kt` |
| 订阅列表、历史及详情 | 当前与历史分开，暂停、到期、停止有区别。历史平均日成本、折算天数、当前套餐及真实付款是各自独立事实。保留续费、纠错与状态操作。 | `feature/subscription/SubscriptionListScreen.kt`、`SubscriptionHistoryScreen.kt`、`SubscriptionDetailScreen.kt:82–160` |
| 回看与分析 | 总成本趋势摘要、类别、年度、长期榜单、项目/订阅历史。支持真正空账本、读取异常、重试、保留结果、无排名。年度真实现金事实与订阅付款分别呈现。 | `feature/review/ReviewShell.kt:43–89,135–194`、`CategoryAnalysisScreen.kt:34–70`、`AnnualReviewScreen.kt:61–114`、`LongTermReviewScreen.kt:52–109,125–153` |
| 趋势与图表 | 精确选日、前后日、项目/订阅贡献、显示点、全部事件与原记录跳转。等待读取和选日计算时区分未知和旧结果。图表已有读数与前后日期的无障碍操作。 | `feature/review/TotalCostTrendScreen.kt:514–546`、`core/design/CostChartPage.kt:94–168`、`CostChart.kt:95–110` |
| 关键表单 | 项目创建/编辑、投入/收回、订阅、续费、纠错均复用真实输入和验证。保留 0 元/空值区别、原日期规则、失败、未确认写入核对与返回保护。 | `feature/project/ProjectEditorScreen.kt:87–206`、`MoneyEventEditor.kt:39–87`、`feature/subscription/SubscriptionEditorScreen.kt:68–197`、`RenewalEditorScreen.kt:41–61` |
| 设置、备份与恢复 | 三主题、类别、记录时区、延期提醒、导出、只读文件检查、覆盖恢复、照片空间和产品说明。恢复预览保留十组当前→备份计数、完整覆盖后果、取消及先备份选项。 | `feature/settings/SettingsShell.kt:55–155`、`RestorePreviewScreen.kt:29–83`、`BackupRestoreController.kt:12–37,95–116`、`BackupInspectionController.kt:15–28` |
| 共用主题、组件及导航 | Material3 颜色、字体、形状与按钮；同一金额/单位组件；适配标题、表单底栏、状态标签、装饰图标与礼貌播报。主导航为现在、回看、设置；详情保留进入来源及返回、未保存/未确认、恢复保护。 | `core/design/LithicVisuals.kt:16–57`、`LithicTheme.kt:41–86`、`MoneyMetric.kt:142–197`、`C2Page.kt:22–37,58–88`、`UiFeedbackText.kt:30`、`app/LithicNavigation.kt:268–275,430–448,543–615,675–689,716–752,808–824` |

## 3. 优先问题

| 优先级与性质 | 位置、证据和影响 | 建议 |
| --- | --- | --- |
| P1 · 已确认结构 | 首页两组记录、独立数量卡、折叠中的分项均出现同类标题。数量含历史，预览只取当前记录，容易把计数理解为下面可见记录数。见 `HomeShell.kt:97–134,179–213`。 | 将两项数量合并为摘要下方一组，共用「已录入 · 含历史」。数量未知使用未知状态。分项与类别仍有入口，失败与保留旧结果按原分支呈现。 |
| P1 · 已确认色彩关系 | 纯白主题主摘要和背景同色，源码通过颜色相等把摘要横向内边距改为 0，三主题的首屏对齐策略因此不同。见 `LithicVisuals.kt:49–57`、`HomeShell.kt:53–76`。 | 三套主题同构、同一摘要内边距。黑灰用高一级表面，纯白用中性浅灰，岩白雾蓝用明确雾蓝，主色只服务于重点和操作。 |
| P1 · 已确认组件分歧 | 首页预览与列表使用两套独立排版和缩略图规格，金额说明也由各自实现。见 `HomeShell.kt:284–325`、`EntryCard.kt:47–87`。 | 统一「身份→名称与状态→时间→对应成本」的扫读顺序。首页可用更小缩略图，列表保持稳定行分隔。现有图片可用、无引用、读取失败三个状态独立。 |
| P1 · 可访问性待运行 | 两组首页可见操作均为「全部」，源码未提供按记录类型命名的专用语义。现有金额组件会测量并提供完整值入口，极端内容仍需真机确认焦点及完整播报。见 `HomeShell.kt:255–263`、`MoneyMetric.kt:142–197`。 | 视觉保留「全部」，语义分别命名为「查看全部长期项目」「查看全部订阅」。窄屏或大字将金额与辅助信息重排到下一行。数字与单位同组，超出仍可打开完整值/复制。 |
| P2 · 排版机会 | 标题大/中同为 18/26sp，辅助正文中/小同为 14/20sp；多个 surfaceContainer 共用同一个 group 色。见 `LithicTheme.kt:41–45,53–75`。 | 用字重、间距、表面数量区分页面题、区块题、正文、辅助和主要金额；维持中文读感与系统字号。普通列表少用容器，摘要和危险提示独立分组。 |
| P2 · 已确认文案重复 | 订阅表单常驻实付约束与按需规则重复，当前周期也已有选中控件表达。项目表单单独实现顶栏/底栏，共用表单另有相同能力。见 `SubscriptionEditorScreen.kt:101–132`、`ProjectEditorScreen.kt:87–206`、`C2Page.kt:77–88`。 | 字段旁保留必要实付约束、就地错误和状态后果；周期只由选中态表达。共享顶栏、字段间距及底栏规格。保留四种日期的准确标签、错误展开与定位、固定保存/核对操作。 |
| P2 · 适配风险 | 总趋势标题同排始终预留 128dp 状态位，失败重试、未确认快照和读取/计算也在该位显示。固定宽度在窄屏和放大字体下需要验证。见 `TotalCostTrendScreen.kt:514–539`。 | 正常宽度保持日期、主值和曲线位置稳定，窄屏/大字将状态动作顺排。保留截止日、异常重试、精确查询和全量事件，详细口径沿原入口。 |
| P1 · 高风险操作呈现 | 恢复预览嵌在长设置页；现有预览具备明确覆盖警告与两列/纵排分支。危险动作的触达、确认焦点及系统返回需实测。见 `SettingsShell.kt:111–130`、`RestorePreviewScreen.kt:29–83`。 | 预览形成独立的视觉区，先对照数据，再显示明确的覆盖后果及确认操作。继续区分「检查通过」「恢复完成」「结果未知」。已保存偏好、危险确认和同代保护沿原合同。 |

## 4. 信息与操作保留对照

| 当前信息或动作 | 推荐位置 |
| --- | --- |
| 当前日均、非今日扣款、截止日期、计算口径 | 首页唯一主摘要并设信息按钮。日期及「非今日扣款」紧邻主值；完整定义仍可按需查看。 |
| 未取得、确实 0、负值、空账本、历史无当前贡献、读失败保留旧结果 | 未取得显示「—」并明示状态，真实 0 保持金额，负号与单位完整。真实空账本仅显示已核实的 0 个计数和短引导；只有历史时保留含历史的计数与入口。读失败就地提示，旧值明确标注旧截止日并保留重试。 |
| 项目/订阅已录入数量、当前有效数、分项及类别 | 两项已录入数量共用一次含历史说明，预览范围按原数据。当前有效数与类别、分项共用原成本与类别构成入口。 |
| 搜索、圆形新增、全部、三主导航 | 顶部搜索、右下唯一新增、组尾「全部」及底部图标加文字。新增仍打开两项选择，系统返回按原守卫。 |
| 项目身份及封面、类别、状态、日期、金额 | 列表行与详情顶部一致地呈现身份；详情主值为日均，时间和真实投入/收回/净投入在相邻分组，事实时间线保留。封面入口沿 API36/Picker 准入；缺失、读取失败各自反馈。 |
| 项目新增、记一笔、纠错、编辑、结束、以旧换新、删除 | 列表保留新增长期项目，详情固定主操作仍为记一笔。其余动作留在对应记录入口或原管理菜单。删除有危险分组与二次确认。 |
| 订阅的当前套餐、历史平均、折算付费天数、付款、状态 | 订阅列表使用有标签的历史平均日成本；详情继续区分当前套餐与真实历史付款。续费为原主操作，停止、暂停、恢复及纠错继续遵循原资格和后果提示。 |
| 趋势日期、分项曲线、分析、历史档案 | 回看先给所选日期和主值，再给真实图表；工具区保留精确选日、前后日、分项、文字清单及分页。原导航可返回搜索、类别或历史来源。 |
| 表单、三主题、设置、备份与覆盖恢复 | 表单沿原字段、主动作、错误和未确认写入核对；设置使用分组行，原主题保存机制不变；高风险操作保持原确认及真实结果。 |

### 尺寸与视觉方案

主视图按 **360dp 等效概念宽度、1×** 同构排布。建议沿现有 Material3 系统字体，主要金额约 36–38sp、页面题 24sp、正文 16sp、辅助 14sp，优先调整字重和行距。参考 16dp 页面边距、12–16dp 行内间距、24dp 分组间距。底栏、返回、搜索、更多、信息与列表入口保持至少 48dp 触控区。岩白雾蓝可复用 `#F7F6F2` 暖底、`#E8EEF2` 雾蓝摘要、`#29343E` 主字；行动色 `#536F86` 在 `#E8EEF2` 上静态比值约 4.5:1，后续需按实装组合逐项检查。纯白保持中性背景和中性浅灰摘要，黑灰保持低彩度与明度分层。

窄屏和大字下，先把金额、日期及操作从同排移到次行，标题自然换行。压力样稿用 **320dp / 约 180% 的浏览器概念**展示长名称自然换行及收回金额完整显示。真实系统 200%、长负金额与单位、完整值入口、页面滚动和 TalkBack 焦点需 Android 验证。较宽窗口可以给正文限宽并保持可读行长，继续沿现有导航和来源链。

## 5. 七个视图与复核

| 画板 | 包含视图 |
| --- | --- |
| [三主题首页](mockups/01-home-themes.png) | 岩白雾蓝、纯白、黑灰，同内容、同结构，1236 × 963 px。 |
| [项目与边界状态](mockups/02-project-and-states.png) | 岩白雾蓝项目列表、项目详情、空账本结构、长名称/大金额/大字概念压力，1534 × 963 px。 |
| [画板源文件](mockups/index.html) | 在本地浏览器打开可查看全部七个视图。参数 `?board=home` 与 `?board=states` 可分别打开两张画板。 |

合成账本取自 [C2VisualFixtures.kt](../../../app/src/androidTest/java/com/cooper/lithic/ui/C2VisualFixtures.kt)，压力样本取自 [C9RecordThemeTest.kt](../../../app/src/androidTest/java/com/cooper/lithic/ui/C9RecordThemeTest.kt)。样稿直接引用现有黑石图标。详情的「添加照片」示意 API36 且 Picker 可用的状态，保留产品原有资格检查。

使用本机 Chrome 157 离线渲染并查看两张画板。七个视图的内容区域未测得横向或纵向溢出，画板中的按钮最小边为 48 CSS px，四处图标均读取到原 1254 px 资源。压力画面显示完整 `¥9,999,999,999.99`。这些测量仅适用于画板所示的浏览器窗口和字号。

## 6. 后续实施范围与核验

用户确认后，优先调整 `core/design/LithicTheme.kt`、`LithicVisuals.kt`、`C2Page.kt`、`EntryCard.kt`、`feature/home/HomeShell.kt`，再复用到项目/订阅列表、两类详情、回看、表单和设置。优先保留 `MoneyMetric.kt` 的值格式、完整查看/复制和单组语义；改动涉及组件迁移时保留原测试标记或对应断言。保持 Room5、backup4、Owner、恢复原子切换、金额规则、三主题保存、权限、包身份及历史发行门禁。

- Android 核对 320/360/400dp 与较宽窗口、1× 和系统 200%、长中文、金额极端值和单位、三主题对比、至少 48dp 命中区、TalkBack 名称/顺序、系统栏/IME、横竖屏及返回。
- 核对空账本、历史无当前贡献、真实 0、负值、加载、读取失败/旧结果、没有封面和封面不可用。使用独立合成数据验证。
- 核对搜索/历史/分类返回、长表单末字段、失败和未确认写入、照片操作取消/失败、备份取消/检查以及恢复预览和覆盖确认。依赖数据、安全和设备的实际结果按各自证据登记。

本阶段执行源码与现有证据核对、信息映射和样稿制作。**Android 运行、TalkBack、IME、系统大字、端侧性能、业务测试与构建均为 NOT_RUN**。本轮差异限 `docs/design/lithic-ui-r1/` 的报告、两张画板和一份 HTML 源文件，App 源树仍为 `39295c800ceefdeee19aefce39b26755d3b8d63a`。等待用户与 ChatGPT 集中审查。

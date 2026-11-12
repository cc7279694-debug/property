# LITHIC UI-V3-01 · 全应用视觉审查

**审查基线** · `codex/lithic-ui-p2-home` · `f38cb97aec3559f79c70c2069c322e4ab67f45c1`。P2-A1-R1 的生产改动位于 `5e6920056903abbd73444ec9dddbfd57a16e5e50`。页面归属见[页面清单](APP_INVENTORY.md)，操作与安全边界见[用户旅程](USER_JOURNEYS.md)。

## 证据与结论

| 证据 | 可以说明 |
| --- | --- |
| 正式产品分支的 Compose 实现及导航 | 当前信息顺序、组件尺寸、语义、交互回调和状态分支。源码推断的拥挤、焦点问题和宽屏风险标为**待端侧核验**。 |
| P2-A1-R1 既有隔离 Android 证据 | API 36 / x86_64，360dp 设备级 fontScale 2.0，新增栏和订阅「全部」有独立空间。报告中的 320/360/400dp 和局部模拟字号属于受控 Compose 验证。 [历史报告](../../evidence/LITHIC-UI-P2-A1-R1.md) · [订阅区原生截图](../../evidence/lithic-ui-p2-a1-r1/home-stoneMist-system200-subscription.png) |
| P2-A2 既有隔离 Android 截图 | 三主题复用了生产首页组件，能观察同一份合成记录的阅读顺序和局部遮挡。体验标识、几何封面和合成名称来自宿主。此次只读参考其证据。 [岩白雾蓝](https://github.com/cc7279694-debug/property/blob/cba603f9595a674491d0bab8850a8b577d28d809/docs/evidence/lithic-ui-p2-a2/home-stone-mist.png) · [纯白](https://github.com/cc7279694-debug/property/blob/cba603f9595a674491d0bab8850a8b577d28d809/docs/evidence/lithic-ui-p2-a2/home-pure-white.png) · [深色黑灰](https://github.com/cc7279694-debug/property/blob/cba603f9595a674491d0bab8850a8b577d28d809/docs/evidence/lithic-ui-p2-a2/home-charcoal.png) · [历史报告](https://github.com/cc7279694-debug/property/blob/cba603f9595a674491d0bab8850a8b577d28d809/docs/evidence/LITHIC-UI-P2-A2.md) |
| R2.1 视觉基线 | 首页曾明确将账本日均置于品牌下方，封面回归档案条目，并为三套主题建立表面关系。设计稿用于比较阅读顺序及留白意图。 [R2.1 报告](https://github.com/cc7279694-debug/property/blob/1b688bc1756dc3f42fcd3a3c0e57732d56023f34/docs/design/lithic-ui-r2-1-personal-archive/REVIEW.md) |

“模板感”在现有首页主要来自**连续几层都用数字、标签和对齐的重复网格组织**。账本金额、条目金额和信息入口共同占据第一眼，身份不同的物品排起来仍像同一组指标。全应用源码同时存在另一种问题，同一记录在首页、列表、搜索和详情切换时，封面比例、状态容器、金额次序都会变化。V3 应让档案页先建立记录身份，让录入与设置页把可执行的下一步放在最容易识别的位置。记录真实日期、金额、状态和风险的能力是本轮评审基准。

## 1. 信息层级与视觉布局

| 发现 | 证据与影响 | V3 审查重点 |
| --- | --- | --- |
| **首页的成本块与条目指标一起争夺注意力** | 岩白雾蓝、深色把品牌、搜索、信息、当前日均及截止日期置于连续高对比页首。金额独立占一行并保留单位。下方条目每行又显示项目或订阅指标标签。已有原生截图中，读完金额和重复标签才更容易留意具体物品。 [页首源码](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L174-L233) · [档案行源码](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeArchiveRow.kt#L118-L142) | 保留账本当前日均、日期、非今日扣款及计算说明的邻近关系。让第一条物品身份及封面尽早进入视野。仅在阅读范围明确时考虑共用指标口径，保留项目日均与订阅历史平均日成本的区别及逐条可读语义。 |
| **详情的事实分组已有基础，量值层级仍需要取舍** | 项目有名称、封面、状态、日均和持有时长，再下接财务合计及金额事实。订阅有历史平均日成本主块、折算付费时长、当前套餐金额和标准日均价，再下接状态事实与付款。订阅同屏出现多种正确但分母或时间区间不同的量值，用户需要先理解分组才能对比。 [项目详情](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt#L255-L356) · [订阅套餐](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt#L79-L142) · [订阅历史指标](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt#L241-L257) | 项目和订阅复用档案骨架。项目强调身份、持有和真实资金事实，订阅强调身份、当前有效期、实际付款和历史成本口径。核对负值、真实 0、暂无数据和已付款尚未生效的表现。 |
| **回看和设置需要不同阅读节奏** | 回看同时有趋势、报告和历史入口。非空状态才有真实趋势，空状态仍有多个独立装饰表面。类别分析先区分记录类型，再展开选择、年度金额及关联记录。设置同页包含主题、记录管理、备份、检查、恢复、照片空间和说明，恢复预览再给出数量与完整覆盖说明。 [回看布局](../../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt#L43-L130) · [类别分析](../../../app/src/main/java/com/cooper/lithic/feature/category/CategoryAnalysisScreen.kt#L30-L105) · [设置](../../../app/src/main/java/com/cooper/lithic/feature/settings/SettingsShell.kt#L58-L155) · [恢复预览](../../../app/src/main/java/com/cooper/lithic/feature/settings/RestorePreviewScreen.kt#L29-L62) | 回看按时间与选中事实组织。设置按任务分组，以短动作词和结果反馈为主。覆盖、保留旧数据、未确认结果及取消说明必须可见。 |

## 2. 色彩、字体与间距

- **三主题差异是真实存在的。** 岩白雾蓝采用 `#F7F6F2` 背景和 `#E8EEF2` 页首，主操作使用 `#536F86`。纯白用白底、黑灰前景和内缩细线区分首页层次。深色采用 `#121212` 背景和 `#1E1E1E` 页首，主要按钮以浅灰底配深色内容。截图与源码相符。当前风险在于岩白雾蓝整段页首色面比其他内容醒目，且容器中、高、最高三级在实现中共享同一 `group`。叠用不同的 Material 容器名也可能画成同一色块。V3 先定义页面、分组、交互表面的职责，再逐主题分配色值。 [主题色角色](../../../app/src/main/java/com/cooper/lithic/core/design/LithicTheme.kt#L29-L50) · [色板和专用表面](../../../app/src/main/java/com/cooper/lithic/core/design/LithicVisuals.kt#L16-L59)
- **读数系统比页面层级系统更一致。** 当前已有统一的 38sp 展示样式，以及 16/24sp 正文和 14/20sp 辅助字。多个 heading/title 角色分别合并成 24sp/18sp。同一标题与状态样式跨多处复用时，结构主要依赖色块、边线或字重补充。通用 4/8/12/16/24/32dp 与详情内独立的 20dp 内距、首页 12dp 档案行垂直内距并存。V3 用少量有明确阅读职责的字级和间距，保留现有系统字体缩放与金额格式化。 [字级和间距](../../../app/src/main/java/com/cooper/lithic/core/design/LithicTheme.kt#L19-L75) · [订阅指标内距](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt#L241-L251) · [档案行](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeArchiveRow.kt#L73-L100)
- **静态色值抽样**采用 sRGB 相对亮度比对纯色前景和背景。按岩白雾蓝／纯白／深色黑灰，正文在页首约 10.84／17.93／15.02:1，辅助字在页首约 5.82／6.69／7.86:1，主操作前景与底色约 5.27／15.72／14.91:1。分隔线相对页面底色约 1.21／1.25／1.50:1，适合做辅助分隔，不能单独表达选中、触控边界或状态。这是源码取色，图片上文字、禁用态、图表线条、焦点和所有组合仍需单独测量。 [色值来源](../../../app/src/main/java/com/cooper/lithic/core/design/LithicVisuals.kt#L16-L37)

## 3. 组件规范

| 组件 | 当前证据 | 设计方向 |
| --- | --- | --- |
| 记录身份和封面 | 首页 `HomeArchiveRow` 有图时使用 4:3 Fit、常见宽度为 80dp。无图时是 48dp 的无底色图标槽。列表 `EntryCard` 使用 64dp 方形区域、状态胶囊并把指标标签放在金额上方。搜索使用 `C2RecordIdentity` 和另一种胶囊。实际读图失败有独立入口。 [首页行](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeArchiveRow.kt#L73-L113) · [列表行](../../../app/src/main/java/com/cooper/lithic/core/design/EntryCard.kt#L47-L89) · [搜索结果](../../../app/src/main/java/com/cooper/lithic/feature/home/SearchScreen.kt#L81-L103) · [封面](../../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviews.kt#L125-L143) | 共享名称、封面、状态、日期的相对位置，允许紧凑检索行与展开详情采用不同尺寸。明确区分无图、读取中和不可用。外观调整不改封面真实来源、读取票据、资源预算和错误隔离。 |
| 状态和图标 | 设置行、回看入口都有 32dp 有底色图标。列表、订阅详情和搜索都有胶囊，首页档案改用文字状态。跨面重复底板会提高次要动作的视觉权重。 [设置行](../../../app/src/main/java/com/cooper/lithic/core/design/C2Page.kt#L41-L72) · [回看入口](../../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt#L199-L223) | 次要导航优先共用无底板线性图标或文字。需要突出异常时以文字加语义色，状态与可点击筛选的样式要可辨认。 |
| 金额、表单和弹窗 | `MoneyMetric` 保留金额与单位、完整值入口及单节点语义。项目编辑与订阅首次付款各有不同的帮助、日期、操作区路径，已有字段内校验和独立底部动作。恢复覆盖、放弃草稿、待核对写入有各自后果及回调。 [金额](../../../app/src/main/java/com/cooper/lithic/core/design/MoneyMetric.kt#L97-L200) · [字段](../../../app/src/main/java/com/cooper/lithic/core/design/FormFields.kt#L59-L109) · [项目编辑](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectEditorScreen.kt#L103-L205) · [订阅付款](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionEditorScreen.kt#L87-L196) · [退出保护](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L717-L736) | 统一字段、说明和主动作的相对位置。保留“空白不代表 0”、实际付款条件、同一操作核对、恢复完整覆盖等与当前动作相关的说明。纯概念解释可按需展开。 |
| 图表 | 全账本趋势有日期、分项、说明、列表、点与事件入口。多条序列用实线、不同虚线和阶梯区分。单记录曲线保留日期输入、读值、零值和负值；触摸 Canvas 外有语义与替代路径。 [总趋势](../../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt#L179-L210) · [序列语义](../../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt#L400-L452) · [单记录图](../../../app/src/main/java/com/cooper/lithic/core/design/CostChart.kt#L92-L127) | 让选中日期及对应金额主导阅读。曲线样式、图例和明细须能互相核对。颜色不可是区分曲线的唯一线索，旧数据和未知选中点要保留独立状态。 |

## 4. 无障碍与适配

- **已观察问题 · 常规字号首页的 FAB。** A2 合成记录的初始位置中，三主题截图均可看到圆形新增按钮进入订阅右侧指标区域。深色截图最容易辨认，订阅的「历史平均日成本」下缘被挡住一部分。源码给滚动内容留了底部 88dp，允许滚动找回内容，浮层仍然覆盖当前视口。V3 需要在正常字号也验证关键金额、标签和入口的可见性及实际命中区域。 [首页视口与 FAB](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L57-L145) · [历史遮挡记录](https://github.com/cc7279694-debug/property/blob/cba603f9595a674491d0bab8850a8b577d28d809/docs/evidence/LITHIC-UI-P2-A2.md#L39)
- **已有保护 · 真实 200% 新增栏与内容独立占位。** 360dp 历史 Android 截图可见订阅「全部」在新增栏上方，三导航可见。档案行和常用页头另有字级与宽度重排，金额溢出有完整值入口。V3 样本还要测 150% 阈值附近、长中文和混合名称、带符号超长金额、横屏和多窗口。 [历史大字复验](../../evidence/LITHIC-UI-P2-A1-R1.md) · [档案行测量](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeArchiveRow.kt#L58-L109) · [通用页头](../../../app/src/main/java/com/cooper/lithic/core/design/C2Page.kt#L22-L37)
- **语义已有可继承路径。** 首页搜索、计算说明和「全部」具备标签或最小触控尺寸，金额提供合并描述，表单反馈有礼貌播报区。导航采用原生三项 `NavigationBar`，非顶层路由使用来源返回；入口实现系统安全区域，表单与搜索各有 IME 处理。V3 应实测 TalkBack 的标题、身份、金额、状态顺序和弹窗焦点返回，核对同一行中「打开记录」与「查看封面状态」可分别命中。局部 `48.dp`、`imePadding` 和语义声明不能证明全部场景可用。 [首页入口](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L182-L210) · [金额语义](../../../app/src/main/java/com/cooper/lithic/core/design/MoneyMetric.kt#L158-L197) · [反馈播报](../../../app/src/main/java/com/cooper/lithic/core/design/UiFeedbackText.kt#L17-L33) · [原生导航](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L811-L820) · [Insets](../../../app/src/main/java/com/cooper/lithic/app/LithicApp.kt#L68-L75) · [表单 IME](../../../app/src/main/java/com/cooper/lithic/core/design/C2Page.kt#L77-L89)

## 5. 设计审查标准

| 检查 | 可接受证据 |
| --- | --- |
| 视觉层级和品牌一致性 | 同一组合成数据跑三主题。对照首页、列表、搜索、项目详情和订阅详情，确认用户能认出同一对象并理解每种成本的口径。对照表单、设置、备份恢复时主要动作可直接找到，危险后果可读。 |
| 可辨识、可操作 | 三主题实测文本和关键交互对比度，检查焦点/选中/禁用/警告。真实 TalkBack 检查导航、金额、两个「全部」、封面错误、字段错误、图表替代和弹窗。最小操作目标 48dp，逐页确认相邻区域可分别命中。 |
| 响应和平台 | 至少检查 320/360/400dp、正常／大字体、宽屏或横屏、系统栏、手势及三键返回、IME 下的长表单与确认框。新增入口、确认主按钮与内容不能互相遮挡。原生返回、选中状态及守卫须用实操证据复核。 |
| 事实与安全 | 样本包含空账本、仅历史、真 0、负值、未知、旧快照、过期、封面不可用。A–K 用户旅程保持输入、同笔记录身份、确认、取消、重试与未确认写入保护。备份检查与覆盖恢复须可区分。 |
| 状态及性能 | 对比原生截图、可读语义、实际操作和测试输出。长列表与图表检查滚动、重排及状态恢复；源码里已有 LazyColumn、封面采样与异步路径，启动和滚动性能需实测。 |

**本轮证据状态** · 静态审查及历史截图复读已执行。独立视角采用 Impeccable 的 Native Audit 和 Android 检查项，结合 Material 3 触控及组件角色、Mobile Android Design 的布局与系统适配、UI UX Pro Max 的移动端信息层级检查。新 Android 构建、安装、自动化、真机、TalkBack、系统大字、正式流程和性能测量均为 **NOT_RUN**。P2-A1-R1 的既有结论继续按其原范围使用，P2-A2 的历史 Lint FAIL 及小米14 NOT_RUN 保留。审查证据不改变任何发行或设备门禁。

## 使用的本机设计资料

| 资料 | 实际读取和使用 |
| --- | --- |
| UI UX Pro Max | 读取 `SKILL.md` 的查询与移动端审查部分。运行本地 `search.py` 的 `--stack jetpack-compose` 与 `--domain ux` 查询，检查 Lazy 列表、语义、可访问名称和焦点遮挡。Web 结果仅作问题提示，Android 准入由原生检查决定。 |
| Material 3 | 读取 `SKILL.md` 与 `references/navigation-patterns.md`、`layout-and-responsive.md`、`typography-and-shape.md`、`color-system.md`。用于三目的地导航、Inset、48dp、sp 字级与语义色角色。沿用既有 Nav3 与三套固定主题。 |
| Mobile Android Design | 读取 `SKILL.md` 及 `references/details.md`、`android-navigation.md`、`material3-theming.md`。用于窄屏重排、长列表、点击/语义分离和大小窗口适配。 |
| Impeccable 4.1.0 | 读取 `SKILL.md` 与 `reference/audit.native.md`、`android.md`、`init.md`。运行 `context.mjs` 只读识别现有实现。它寻找的 `PRODUCT.md` / `DESIGN.md` 在仓库不存在，本轮使用 `PROJECT.md`、实际 Compose、R2.1 与用户任务书作为产品事实。原生审查采用源码和已有截图。 |

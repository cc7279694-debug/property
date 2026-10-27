# LITHIC C5 — 全产品UI与交互统一规范（独立审查候选）

状态：**C5 GLOBAL DESIGN — READY_FOR_INDEPENDENT_REVIEW**。日期：2026-10-27。

本文件是本轮唯一交付。它规定下一阶段的统一实现，**本轮没有实施这些规则**，也未取得新的Android画面或性能证据。未经ChatGPT独立审查及后续实施授权，不进入全量Compose迁移。

## 1. 契约、来源与证据等级

- 仓库：`cc7279694-debug/property`；分支：`codex/lithic-v1-product-completion`；审计基线：`6a00c1e8d128e499b072407f72ade9bb5ecbdb78`，与本轮开始时本地/远端HEAD一致，工作区干净。
- 用户正式转交：**Preview 7真实体验 CHANGES REQUIRED**。保留其构建、签名及139项JVM历史记录；这些工程证据不能撤销真实体验拒绝，也不算本轮重新执行。
- Goal：统一全App金额、单位、信息层级、状态反馈和导航可读性；先形成可一次迁移的完整规范，再交付一次完整候选。
- Scope：只读审计生产UI、呈现状态、导航与相关库行为；仅新增本文件。无产品/测试源码修改，无构建、测试、APK、HTML或设备操作。
- Locked：原生Kotlin/Compose/Nav3；现在/回看/设置；Room5、backup4、冻结金额/日期/订阅/替换/提醒规则、Repository、Owner、generation、恢复Token/原子切换、业务写入和离开守卫；原包名/签名/已交付版本与全部历史证据。
- Adjustable（待审查设计）：公共金额组合、已有页面的信息顺序/密度、普通说明的入口、统一背景及导航呈现、同代已有结果的可见连续性。不得增加新业务指标、写入入口、数据库字段或第二套缓存。
- 当前已存在LithicTheme、C2Page和EntryCard共享基础；问题是金额组合、密度规则与导航呈现仍分叉，并非没有任何共享样式。本次收敛已有体系，不另建复杂组件库或替换架构。

本规范不是ACCEPTED/FROZEN，也不是Android视觉或无障碍通过。M10/M20/M22/M13及G1～G4历史FAIL/UNKNOWN/NOT_RUN/NOTES不变。

| 来源 | 本轮实际检查 | 能证明什么 |
|---|---|---|
| 用户转交的26秒录屏文字审查 | 已读完整任务；**未取得录屏/手机截图原件** | 记录用户拒绝及其描述的约2秒、17.4秒重叠；不声称亲自逐帧观看 |
| UI-C1冻结`0dce7dd0725a54be4f0280870ef6ca47c48ea7ed` | 已从Git原件实际打开[浅色图](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/docs/design/ui-c1/source-v1/ui-reference-light.png)、[深色图](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/docs/design/ui-c1/source-v1/ui-reference-dark.png)，并读原`UI_C1_SPEC.md` | 设计参考：暖白/墨绿、主卡、数量卡、紧凑行、指标区、分组；不是本轮Android实拍 |
| 上述原PNG | 浅色SHA `5251235024baec1480eea61c09cada60430cd9f09dade2334f746afc587d628a`；深色SHA `46aba6544fd4637bb823e8b7a3626f55571e5c2de15f91febd6241fc676161b1` | 原设计身份；示例照片/品牌/金额不进入生产事实，旧“即将开放”不覆盖现有功能 |
| 当前生产源码 | 下述逐页面、逐金额调用点；行号均指审计基线 | 静态结构、真实数据来源、现存回调及保护，不能证明实际布局/帧时序 |
| Navigation3 1.0.1 | 版本目录与既有实际缓存classes字节码只读核对；classes SHA `a7bb4faba8aa704590dbfedbd009725c5199845edeb05fbe261b83d16176d033` | API默认转场、分支与Scene元数据传播；不是手机运行轨迹 |

本轮不借用HTML、DEX存在或测试数量证明视觉通过。手机当前fontScale、density、动画倍率、实际触控与帧时间均**UNKNOWN/NOT_RUN**；历史手机设置不自动成为本次实时事实。

## 2. 全产品页面清单与责任

以下源码路径统一相对`app/src/main/java/com/cooper/lithic/`。同一文件中的模式也列入审计，避免只迁移一种创建/纠错状态。`§4`为逐金额清单，`§7`为状态合同。

| 页面/模式及生产位置 | 当前源码问题或已成立部分 | 默认主焦点、次级信息与说明去向 | 必须保留的操作/状态 |
|---|---|---|---|
| 现在：`feature/home/HomeShell.kt:53–162` | 主卡/数量卡/FAB已有；金额在多个局部自行拼单位 | 总日均→两类已录入数量→各最多2条真实预览；类别构成按需；口径统一信息入口 | 总数含历史，不冒充active；原排序不命名“最近活动”；真实空/历史无当前贡献/读取失败/旧快照；唯一FAB及两项选择、关闭零写入 |
| 项目列表：`feature/project/ProjectListScreen.kt:30–113` | 范围说明及开始/结束/持有时间堆行 | 名称/状态→持有天数与一个日均；完整日期到详情，列表口径到信息入口 | 创建、准确详情、默认持有时长排序、50条分页/继续/重试；未取得不填0 |
| 项目历史：`feature/project/ProjectHistoryScreen.kt:29–107` | 行内起止/持有/结果说明重复 | 已结束身份→冻结持有天数→截至观察日的结果；一条紧凑日期 | 只有已结束；尾款/退款影响金额但不延长持有期；旧结果/分页/详情返回 |
| 订阅列表：`feature/subscription/SubscriptionListScreen.kt:36–138` | 状态分组、列表口径入口已有，行内事实偏多 | 名称/真实状态→最近实付或覆盖事实→**历史平均日成本** | 暂停/到期仍在当前管理；原最近真实付款排序、状态分组、分页；不能把historicalAverage换为currentRate |
| 停止订阅：`feature/subscription/SubscriptionHistoryScreen.kt:29–90` | 常态长解释及纵向金额单位 | 已停止身份→折算付费时长→历史均价；范围说明按需 | 暂停、到期、未来停止不提前归档；加载/错误/旧结果、分页和详情 |
| 名称搜索：`feature/home/SearchScreen.kt:39–109` | 范围与业务日期常驻；结果本来没有金额 | 输入→名称/类型/状态结果；范围/日期进信息入口 | 当前/历史范围、转义LIKE及原分页不变；旧查询结果明确归属；**不为统一行样式新增成本查询或假金额** |
| 项目详情/金额历史：`feature/project/ProjectDetailScreen.kt:64–216,222–374` | 身份、指标、摘要和时间线已有；单位及普通解释重复 | 身份/状态→日均与持有天数→投入/收回/净投入→金额时间线；计算说明按需 | “记一笔”主操作；投入/收回面板、逐事件纠错、资料、结束/修正、替换/交易、曲线、永久删除二次确认，资格不变 |
| 订阅详情/付款历史：`feature/subscription/SubscriptionDetailScreen.kt:31–289` | 历史均价大区先于当前套餐，折算长说明常驻 | 身份/状态→当前套餐价格/周期和生效/到期、未来预付→历史均价/折算时长→付款历史 | 记录续费主操作；付款纠错/删除、价格历史、暂停/恢复/停止/同身份重开、状态纠正、未来停止修改/取消；不新增不存在的一般资料编辑路由 |
| 新建/编辑项目：`feature/project/ProjectEditorScreen.kt:47–207` | 渐进表单、固定底部已存在，部分规则长文 | 名称/初始投入/拥有日期→类别、付款日期、备注；规则变字段帮助 | 0合法、空白不变0；不同付款日期与隐藏错误必须显示/定位；operationId、未确认/冲突/重读、交易日期锁定不变 |
| 记投入/收回：`feature/project/MoneyEventEditor.kt:24–77` | 方向选择与文字重复 | 一个方向控件→金额/真实日期→补充说明，底部保存 | canEdit、原身份、IN_FLIGHT/UNCONFIRMED/COMMITTED及原核对入口 |
| 金额纠错：`feature/project/MoneyEventCorrectionScreen.kt:15–47` | 普通纠错解释与字段同权 | 原事实短摘要→可纠正字段→保存；详解按需 | 交易关联金额/日期只读；真实新退款仍走新增收回，不能改作历史纠错 |
| 结束/修正结束：`feature/project/EndProjectDialog.kt:25–80`，共用`LifecycleFormActions:86–122` | 实际是完整表单，不应按文件名强改成短弹层 | 结束日期→可选实际收回→确认 | “结束不是删除”、实际到账日、现金/折抵与交易日期锁定就地；未确认/冲突/已提交只重读 |
| 永久删除：`feature/project/DeleteProjectDialog.kt:24–78` | 高风险确认，长说明并非全属噪声 | 对象身份→删除影响→原二次确认 | 永久删除/历史重算、交易禁删、冲突/未知及取消保护；不得改滑动删除 |
| 以旧换新：`feature/project/TradeInEditorScreen.kt:33–109` | 既有录入→核对两阶段 | 旧项/新项→现金与折抵输入→整组核对→确认 | 三笔事实、五项核对值、非现金性质、不得重复计初始价、整组纠错/撤销限制及未知提交，均不隐藏 |
| 替换链/纵向对比/建链/解除：`feature/project/ReplacementComparisonScreen.kt:36–147` | 每项多按钮及关系解释占正文 | 链身份→持有/日均→准确详情；关系管理次级，说明按需 | 方向、目标、一前一后、解除不撤销金额/结束、不自动接两端；候选分页/预览确认/UNCONFIRMED |
| 新订阅/首次付款：`feature/subscription/SubscriptionEditorScreen.kt:49–142` | 四日期各有长说明 | 名称→金额/周期→实际付款日/生效/到期/提醒日；短标签，详解按字段展开 | 真实付款>0、首次付款明确确认；预付不提前生效、排他到期、提醒不延长服务；免费不自动造付款 |
| 续费/付款纠错/同身份重开：`feature/subscription/RenewalEditorScreen.kt:25–54`，共用`PaymentFields`与`SubscriptionPaymentActions:146–186` | 同一页不同模式，不能漏迁 | 金额/周期/四日期→原模式的唯一确认 | 原付款身份、固定停止日期冲突、同提交核对、canEdit与重读保护 |
| 暂停/恢复/停止/状态纠正/取消未来停止/删除错误付款：`feature/subscription/SubscriptionStateDialog.kt:35–109` | 多种高风险/日期操作共页 | 指定对象与动作→真实日期及影响→明确确认 | 两种停止模式、服务商仍可能扣费、删付款非退款、取消仅指定计划、原阶段/冲突/未知；内容长用滚动完整页，不强塞短Sheet |
| 项目成本/订阅价格历史：`feature/project/ProjectCostChart.kt:15–47`、`feature/subscription/SubscriptionPriceChart.kt:15–49`→`core/design/CostChartPage.kt:41–214`、`core/design/CostChart.kt:45–132` | 多处采样、自然日、事件说明默认铺开 | 真实图→所选完整日期与精确日均→紧凑日期操作；显示点/事件/指标说明按需 | 项目历史日均与订阅当时套餐标准日均不混为历史均价；结束后结算分离、不延长曲线，非现金/未来计划、50条扩展及准确详情 |
| 回看首页：`feature/review/ReviewShell.kt:52–245` | 摘要已紧凑，但路由临时null会走“尚未读取”分支 | 当前/选日读数→172dp真趋势→两类贡献→分析/两类历史入口 | 真空不造图；历史有记录不作空库；读失败/旧快照可见；类别/年度/长期/完整趋势及守卫返回首页 |
| **完整总成本趋势**：`feature/review/TotalCostTrendScreen.kt:68–285,340–433` | `summaryOnly=false`仍三金额纵排、多段技术说明和多个全宽按钮 | 所选完整日期/总日均→主图→项目/订阅贡献→一行日期工具；详细指标、显示点、事件按需 | 日期必须与金额同一请求/快照；当前/历史0含义可查；任意精确选日、真实峰谷/负值、全部事件/50条继续、所属记录跳转 |
| 长期复盘三榜：`feature/review/LongTermReviewScreen.kt:36–146` | 行内多个大指标及解释等权 | 陪伴榜以heldDays、低成本榜以日均、订阅时间榜以折算付费时长为主；其余次级 | 类型分开；未舍入排序/稳定ID不变；当前/已结束、未开始无排名、0/负/无分母、准确详情及分页 |
| 类别分析：`feature/category/CategoryAnalysisScreen.kt:20–98` | 当前贡献与历史资金、年份规则同权 | 所选类型/类别的当前日均贡献→历史金额/年度分布；说明按需 | 当前分类口径、非现金短标记、两类体系独立、缺年份不填0、原类别/年份/记录入口 |
| 年度回顾：`feature/review/AnnualReviewScreen.kt:18–101` | 年份多个全宽按钮，重复口径与多金额块 | 年度工具条→分类型事实摘要/真实年度图→记录；详解按需 | 年份是主上下文；项目投入/收回/净投入与订阅实付分区，**不制造跨类型年度总支出**；非现金、空年vs真0、原50条年度/记录分页 |
| 报表公共展示：`feature/review/ReportPresentation.kt:28–118` | 金额单位与条形图文本手写，记录卡无金额 | 统一金额/状态/图表标签，普通口径入说明 | ReportReadStatus错误/旧结果/notice；共同正负轴、非现金；RecordCard用类型+ID，不能为排版新增金额 |
| 设置/导出/校验/恢复中：`feature/settings/SettingsShell.kt:33–137` | 已有分组与按需说明，保持并统一尺寸 | 简短分组行；备份和恢复实际阶段、结果就地 | 导出未加密、恢复完整覆盖；Success/取消/残留/中断/未知按真实类型；取消导出/校验、繁忙禁第二操作 |
| 覆盖恢复预览：`feature/settings/RestorePreviewScreen.kt:26–57` | 安全核对页，不能按普通空页缩减 | 当前→备份内容对照→覆盖影响→原确认/取消 | 全部计数、校验时快照/失效重选、图片/设置/时区替换、未含记录不保留、系统权限不恢复；busy/Token合同不变 |
| 提醒设置：`feature/settings/NotificationSettingsScreen.kt:47–163` | 偏好/权限真实，但默认规则文字多 | 两开关→系统权限→必要警示；水位/补发/09:00细则入提醒说明 | 拒绝权限/无法发布/不保证准点、错误/旧状态可见；里程碑关闭不影响续费，不借UI重启调度 |
| 类别管理/改名/合并预览：`feature/category/CategoryManagementScreen.kt:37–153` | 多层说明可按需，影响预览必须保留 | 类型→类别及引用数→一个新增；改名/合并作明确次操作 | 默认“其他”、重名、陈旧确认、源删除/引用影响/金额不变、跨类型拒绝与未知结果；不跳过确认 |
| 计算说明/关于：`feature/settings/ProductInformationScreen.kt:27–67` | 这里本来是阅读任务，不能全App去说明时删正文 | 页面标题→短章节→完整规则/真实版本 | 原规则全文可读；运行时versionName/code、体验Debug身份、本地卸载/手动未加密备份/提醒边界 |
| 里程碑来源/无效来源：`feature/reminder/MilestoneSourcesScreen.kt:49–149` | 专门来源页，不能变普通空结果 | 类型/名称/真实节点；无效时明确提示与返回 | 加载/错误、点击前原引用重验、同UUID不同类型、准确详情与50条；不扩大为真实送达通过 |
| 本地存储门禁：`app/LithicApp.kt:32–72` | Initializing/Failed与就绪导航已经分开 | 一条读取/故障主信息及适当动作 | 失败不自动建空账本、不要卸载/清数据、退出可达；Ready后才按generation进入导航 |

### 页面职责的不可变差异

“统一”不意味着所有页面变成同一种卡片。浏览行用紧凑列表；详情用身份/指标/记录；表单用分组输入与固定主操作；图表用读数/图形/选日；专门说明页用阅读排版；危险确认保留完整影响。

原设计中的“最近记录”不自动适用于当前列表排序。原图“月付金额”“已付天数”不能替换现有当前套餐、historicalAverage、standardPurchasedDays或elapsedPaidDays。页面没有取得的事实不补算、不猜测。

## 3. 全局MoneyMetric合同

### 3.1 证实的问题与唯一呈现内核

`core/design/AmountText.kt:98–103`固定以Column排数字和单位；`:92–95`只量数字字符串。生产共有**24个调用点，16个沿用默认纵向单位、8个关闭单位**；后者包含合理的纯金额以及页面自拼单位，不能一概认定为错误。首页又有两套测宽/单位策略，EntryCard取数字/标签/单位三者最大宽度而非整个金额组合，故只改单页不能收口。

下一阶段采用单一`MoneyMetric`呈现内核（可用薄`AmountText`适配保留调用兼容，但不得保留第二套布局/缩放/单位算法）。消费已有`BigDecimal?`和明确的CNY或日均语义；只格式化/排版，不访问Repository，不推断现金性质、业务日期或价格。

| 合同 | 规范 |
|---|---|
| 日均 | `¥0.67/天`，金额与小单位共基线、同一不可任意断开的视觉组；不用另一行“元/天” |
| 人民币金额 | `¥2.00`，不再追加独立“元”行；**CNY单位不是CASH支付方式**，非现金折抵仍单独明确标记 |
| 标签 | 当前日均/已结束结果/历史平均/当前标准价/当前套餐等名称由原事实决定，不能因为样式统一而统一成“成本” |
| 数值 | 原AmountFormatter、HALF_UP、BigDecimal和未展示舍入结果不变；`null`为“—/暂无数据”，真0正常显示，负号保留，非零亚分钱仍`<¥0.01/天`或`−<¥0.01/天` |
| 语义 | 一个合并读数，包括指标名、完整格式化金额及自然语言“元/天”或“元”；原关键testTag/完整金额语义保留或做明确兼容映射，不能重复朗读两套读数 |
| 宽度 | 量完整金额、单位、间距及真实字体；先使父容器重排，再在对应层级候选字号内适配，仍不够则让**整个金额组合**横向查看，不截断、不省略、不用K/W |
| 完整查看 | 溢出时有可发现的“查看完整金额”48dp操作，保留选择/复制、完整单位和复制失败反馈；“完整”是原格式化口径完整展示，不擅自增加新精度口径 |
| 大字 | 保留系统fontScale；允许一整块指标换到下一行，禁止把小数尾数或单位拆成无关块；不能把普通金额普遍降到18sp来掩盖压力 |
| 输入 | MoneyMetric是只读；MoneyInput仅统一既有AmountField的标签/前缀/间距/错误。原受控String、Parser、空/0资格、原值和提交身份不改 |

### 3.2 四种稳定密度（设计候选，真实尺寸待验证）

| 层级 | 使用责任 | 金额/单位字号与行高 | 压力下的优先顺序 |
|---|---|---|---|
| Hero | 首页或回看唯一主金额 | 38/46sp；单位14/20sp | 整组独占宽度→32→28sp→整组横向/全额入口；普通合法短金额维持38sp |
| Detail | 详情或完整图表主读数 | 28/36sp；单位14/20sp | 次信息下移→24sp→横向/全额入口 |
| Card | 同组次级投入/收回/套餐等 | 24/32sp；日均单位14/20sp | 两/三列改纵排→20sp→横向/全额入口 |
| Row | 列表、时间线、贡献/点清单 | 18/26sp；日均单位14/20sp | 身份/日期与指标上下重排→18sp保留→横向/全额入口 |

数字与单位视觉间距2～4dp，基线对齐；标签与读数4～8dp。候选尺寸均以sp自然参与系统缩放；实际适配用测量而非`字符串.length`，不覆盖LocalDensity来抵消用户字号。200%时以可滚动完整读数优先，不能保证所有指标仍横排。

## 4. 金额及单位全局迁移账本

### 4.1 全部24个AmountText调用点

下表点号是后续迁移验收ID；一个公共调用可能服务多种页面/多个数值，不能只在其某个调用分支改好。

| ID | 文件:调用行 | 原指标/当前布局 | 迁移目标 |
|---|---|---|---|
| M01 | `core/design/EntryCard.kt:87` | 列表日均，默认分行单位 | Row；4个列表均生效 |
| M02 | `core/design/CostChartPage.kt:119` | 单项图所选日准确读数，默认分行 | Detail；日期必须匹配 |
| M03 | `feature/home/HomeShell.kt:190` | 总日均，关闭单位另拼 | Hero；去掉页面自测/自拼 |
| M04 | `feature/home/HomeShell.kt:262` | 两类型小计，关闭单位另拼 | Card；active计数/金额不换口径 |
| M05 | `feature/home/HomeShell.kt:343` | 两类预览，关闭单位另拼 | Row；订阅“历史均价”标签保留 |
| M06 | `feature/home/HomeShell.kt:376` | 类别当前贡献，默认分行 | Row |
| M07 | `feature/project/ProjectDetailScreen.kt:271` | 日均，关闭单位、单位藏在标题 | Detail；区分当前/已结束 |
| M08 | `feature/project/ProjectDetailScreen.kt:330` | ProjectTotal三项人民币数，默认“元”行 | Card/CNY；投入/收回/净投入及折抵标签保留 |
| M09 | `feature/project/ProjectDetailScreen.kt:350` | 金额事件，CNY且关闭单位 | Row/CNY；不丢方向/非现金性质 |
| M10 | `feature/project/ReplacementComparisonScreen.kt:87` | 每项日均，默认分行 | Row或Detail；当前/结束状态保留 |
| M11 | `feature/project/TradeInEditorScreen.kt:109` | TradeAmount核对，默认“元”行 | Detail/CNY；5项核对值独立 |
| M12 | `feature/review/LongTermReviewScreen.kt:134` | 项目日均，默认分行 | Row；不同榜单主次不同，不改排序 |
| M13 | `feature/review/LongTermReviewScreen.kt:143` | 订阅历史平均，默认分行 | Row次指标；时间榜主指标仍折算时长 |
| M14 | `feature/review/ReportPresentation.kt:57` | ReportMoney人民币值，默认“元”行 | Card/CNY；不把含折抵项目金额叫现金 |
| M15 | `feature/review/TotalCostTrendScreen.kt:144` | 摘要总日均，关闭单位 | Hero/Detail；完整所选日期 |
| M16 | `feature/review/TotalCostTrendScreen.kt:301` | 摘要项目贡献，关闭单位 | Row，完整日均组 |
| M17 | `feature/review/TotalCostTrendScreen.kt:308` | 摘要订阅贡献，关闭单位 | Row，完整日均组 |
| M18 | `feature/review/TotalCostTrendScreen.kt:325` | 完整页DailyValue总额/两类贡献，默认分行 | 总额Detail，两贡献Row；覆盖full模式 |
| M19 | `feature/category/CategoryAnalysisScreen.kt:64` | 类别当前日均，默认分行 | Detail；与历史金额分区 |
| M20 | `feature/subscription/SubscriptionDetailScreen.kt:103` | 当前套餐价，默认“元”行 | Detail/CNY；周期及真实日期分开 |
| M21 | `feature/subscription/SubscriptionDetailScreen.kt:106` | 当前标准日均，默认分行 | Card/Row；不等于历史平均 |
| M22 | `feature/subscription/SubscriptionDetailScreen.kt:135` | 累计确认付款，默认“元”行 | Card/CNY |
| M23 | `feature/subscription/SubscriptionDetailScreen.kt:240` | 历史平均，默认大金额/分行 | Detail或Card；明确“历史平均” |
| M24 | `feature/subscription/SubscriptionDetailScreen.kt:275` | 付款历史，默认“元”行 | Row/CNY；付款/覆盖/未来标记保留 |

### 4.2 公共组件的间接展开清单

| 上游位置 | 实际影响面/不能遗漏 |
|---|---|
| `feature/project/ProjectListScreen.kt:93`、`feature/project/ProjectHistoryScreen.kt:87`、`feature/subscription/SubscriptionListScreen.kt:107`、`feature/subscription/SubscriptionHistoryScreen.kt:73` | EntryCard四列表；订阅传historicalAverage，不能改为currentRate |
| `feature/project/ProjectDetailScreen.kt:308–310,318,321` | ProjectTotal三项数据及横/纵两布局；含折抵时明确投入/收回非纯现金 |
| `feature/review/TotalCostTrendScreen.kt:159,160,161` | DailyValue完整页总额/项目/订阅三项，不可只覆盖摘要 |
| `feature/project/TradeInEditorScreen.kt:87,88,89,91,92` | 旧项折抵收回、新项现金、新项折抵、取得总成本、合计净投入增加；既有checked运算/空值处理不改 |
| `feature/review/ReportPresentation.kt:45,46,47,50` | 项目投入/收回/净投入、订阅付款四项，语义分开 |
| `feature/category/CategoryAnalysisScreen.kt:66`、`feature/review/AnnualReviewScreen.kt:62,73` | ReportMoneyValues三处 |
| `feature/category/CategoryAnalysisScreen.kt:81`、`feature/review/AnnualReviewScreen.kt:91` | ReportAnnualBars两处，正负/真实0共享轴 |
| `feature/project/ProjectCostChart.kt:42`、`feature/subscription/SubscriptionPriceChart.kt:38` | CostChartPage两种事实适配；项目历史日均vs当时套餐标准日均 |

### 4.3 手写金额、单位、测宽及语义清单

| 位置 | 当前责任 | 迁移或保留方式 |
|---|---|---|
| `feature/home/HomeShell.kt:172–192` | 主数字/单位两套测量、字号与拼装 | 全部交给Hero组合测量，不留页面例外 |
| `feature/home/HomeShell.kt:263` | 类型小计单位 | 由M04统一 |
| `feature/home/HomeShell.kt:308–310,340,344` | 预览自测数字+/天、手写语义 | Row统一测宽/读数，避免重复朗读 |
| `feature/project/ProjectDetailScreen.kt:266,269,312` | 字符长度选布局、单位藏标题 | 改真实宽度约束；交易非现金分组的强制重排不取消 |
| `feature/subscription/SubscriptionDetailScreen.kt:113` | 无有效套餐时“当前贡献0元/天” | 保留原已验证0贡献与无套餐区别；不要把无套餐称免费或把currentPrice缺失填0 |
| `feature/subscription/SubscriptionStateDialog.kt:89` | 删除付款确认`${state.renewalAmount}元` | 只读确认组适配；原输入/提交字符串不改，不为显示额外查库；无法明确转换则不填0 |
| `feature/review/TotalCostTrendScreen.kt:139` | 摘要标签带单位 | 单位归MoneyMetric，标签保留指标含义 |
| `feature/review/TotalCostTrendScreen.kt:248,249` | 显示点列表总额/两贡献字符串 | 完整日期+MoneyMetric.Row，保留选点入口 |
| `feature/review/TotalCostTrendScreen.kt:276,277` | 事件formatCny及非现金标记 | Row/CNY；标记不可只进说明 |
| `feature/review/TotalCostTrendScreen.kt:376,382,383` | 轴范围与Canvas/选日语义 | 可见范围按需，完整范围/三额及自然日语义始终可访问 |
| `core/design/CostChart.kt:77,78,84` | 零线/范围/选日金额文本和语义 | 共享读数字符串，不改轴/投影；不能删零线/负值的辅助说明 |
| `core/design/CostChartPage.kt:158,172,202` | 显示点、结束后调整、事件金额 | 日期+Row；结束后结算与冻结天数保留，事件性质保留 |
| `feature/review/ReportPresentation.kt:82,85` | 年度条标签+金额及正负语义 | 标签+Row；共同尺度、正负方向及完整语义不变 |
| `core/design/EntryCard.kt:55–57` | 对数字、标签、旧单位分别取max | 测完整组合，再决定身份/金额横排或纵排 |
| `core/design/AmountText.kt:99,102,114–123` | 主金额、单位、全额弹窗/选择/复制 | 单内核统一主组及全额组；保留复制失败反馈 |

`AmountFormatter`实际定义在`core/design/AmountText.kt:38–64`，不是待另造的格式化器。VM中的`feature/project/ProjectViewModel.kt:717`、`feature/subscription/SubscriptionViewModel.kt:274,455`金额字符串是已读事实/输入装载，**不因浏览组件迁移修改**。业务引擎、DTO/备份序列化和MoneyParser不属于UI排版迁移。

### 4.4 全部7个AmountField输入调用

| 位置 | 呈现迁移边界 |
|---|---|
| `feature/project/ProjectEditorScreen.kt:110` | initialAmount受控String；0合法，空白不能转0 |
| `feature/project/MoneyEventEditor.kt:68` | 原方向、金额、日期、canEdit、operationId |
| `feature/project/MoneyEventCorrectionScreen.kt:41` | 原纠错身份、交易只读锁及errors |
| `feature/project/EndProjectDialog.kt:70` | 实际收回、非现金折抵与结束关联 |
| `feature/project/TradeInEditorScreen.kt:72,74` | 现金补款/折抵独立；原Parser与整组校验 |
| `feature/subscription/SubscriptionEditorScreen.kt:93` | 共用PaymentFields，覆盖新增/续费/纠错/重开；真实付款>0 |

`core/design/FormFields.kt:42`是定义，不计调用；RenewalEditor复用PaymentFields，不另造输入解析。搜索与ReportRecordCard原无金额字段，迁移身份/状态即可。

## 5. 统一视觉、组件与响应式规则

### 5.1 继承UI-C1，不建立第三套主题

| 角色 | 浅色 | 深色 |
|---|---|---|
| 页面背景/基础表面 | `#F6F5F1` / `#FFFFFF` | `#111815` / `#1B2520` |
| 主字/次字 | `#17221D` / `#596A61` | `#F1F3ED` / `#B4BFB7` |
| 主色/主色上文字 | `#365F4A` / `#FFFFFF` | `#9EBCAA` / `#17221D` |
| 轻强调/分隔 | `#E6EFE7` / `#DFE4DE` | `#2D4135` / `#32433A` |

保留LithicTheme的系统明暗跟随与语义色；首页浅色深绿主摘要、深色分层绿表面、两张柔和数量卡。禁止动态色替换色板、额外持久主题开关、玻璃/渐变海报或品牌示例照片。原Launcher与已选品牌资产不改。

| 层级/组件 | 统一关系及候选数值 |
|---|---|
| 字体 | 页面24/32sp；区块18/26sp；正文/记录名16/24sp；辅助14/20sp；底部导航12/16sp。金额用§3角色，不再每页任取headline/display。12sp不承担金额、状态或风险正文 |
| 间距 | 页面左右16dp；区块24dp；组内12/16dp；标签/读数/短辅助4/8dp；32dp仅真正分离的区域，不能靠统一大留白填满空页 |
| 表面 | 主摘要20dp圆角；一般内容16dp；身份图12dp；胶囊只作状态。轻分隔与列表不重复套多层相同大卡；危险色按后果使用 |
| 标题/返回 | 正常字48dp返回图标/语义＋标题占剩余宽度；长名/大字标题独立行自然增长，返回/更多各保留48dp；来源完整描述不截成错误去向。统一C2PageHeader，避免每页两套临时排法 |
| 列表身份 | 64dp上限身份区与12dp间隔，名称/状态、必要时间/一个主要指标；大字可把整指标移到身份下，不挤断金额。按已有类别/类型矢量，不按名称猜品牌 |
| 详情指标 | 日均与天数主次明确；投入/收回/净投入成次级组；订阅当前套餐与历史平均分区。天数同组`3天`，不得把折算时长称为已使用 |
| 图表 | 回看摘要172dp；完整图候选220～240dp随可用区，不改变采样/投影。主图紧邻准确选日读数；图例/日期工具短行，解释按需；大字允许自然滚动 |
| 菜单/设置 | 56dp为普通最小行高，随内容增长；图标24dp、图标底块32dp；值优先行尾，窄/大字整值下移。只读行不假造箭头/点击回调 |
| 表单 | 可滚动正文、16dp分组、标签与输入4～8dp、错误紧邻字段；底部单一主操作由布局真实测量占位，遵守safeDrawing/IME、避免重复叠加同一安全区，最后字段可滚到按钮上方 |
| 触控/底部 | 所有操作至少48×48dp；首页唯一56dp圆FAB、右16dp、在导航上方，内容末端有实际避让；深页隐藏主导航，继续原来源返回 |

### 5.2 信息层级与响应式压力

1. 日常浏览默认只回答身份、指标、状态和下一步。日期/指标含义保留短标签，完整定义进入明确的“指标说明”或字段信息；不能把必需业务状态归为无关说明。
2. 360dp、400dp均用真实可用约束；正常1.0/1.1与大字1.5/2.0是待验证矩阵，不是已通过尺寸。先压缩非必要文案、重排整组、允许滚动，禁止全局缩字体、锁死行高或删字段。
3. 一屏只有一个突出主操作；短选择沿原Sheet/菜单，复杂日期/风险表单继续完整页。关面板不丢输入、不换operationId、不绕requestLeave。
4. 长中文、负长金额、亚分钱、单点图、无事实、有历史但当前0、无封面与坏封面分别检查。媒体显示仍**MEDIA_DISPLAY_PENDING**；C2RecordIdentity/可选coverContent不等于安全媒体闭环。
5. 专门的计算说明/关于是阅读页面，保留正文。年度按所选年组织、两业务分区各突出其真实指标，不为追求“一主数字”创建跨类型总支出。
6. 既有“展开显示点/事件/说明”应保留焦点和上下文；不把危险操作藏在长按/滑动中。完整金额、完整日期、负号及TalkBack文字不可因视觉收敛消失。

## 6. 导航叠层与刷新：事实、风险链、修正设计

### 6.1 可证实与不能推定的内容

| 检查点 | 静态证据/判断 | 确定性 |
|---|---|---|
| 主入口普通切换 | `app/C4NavigationMotion.kt:5–7`仅不同Home/Review/Settings两端生效；`app/LithicNavigation.kt:414–424,428,437,482`赋metadata并选择None | CONFIRMED；未发现metadata映射的确定性错误，不能说None一定未生效 |
| 完整趋势返回回看 | TotalCostTrend无顶层metadata，其普通forward/pop路径走180ms同时fadeIn/fadeOut；主要页面根是透明Box/Column/LazyColumn，见Home53、Review77、TotalTrend103、Settings50及C2FormScaffold | CONFIRMED的叠字结构条件；对应录屏帧仍待运行核验 |
| 各Scene背景 | 外层Scaffold/StorageGate有背景，不等于各过渡Scene均有独立不透明底 | CONFIRMED；透明页面与双向alpha过渡可让两层文字同时可读，仅缩短时长不能建立正确可读性 |
| predictive返回 | 应用没有传`predictivePopTransitionSpec`；1.0.1独立默认为目标fadeIn(spring，damping1/stiffness1600)＋旧页scaleOut至0.7；选择分支先predictive再普通pop/forward | CONFIRMED；不能把普通pop的180/None写成覆盖全部返回 |
| Scene元数据/保留 | 实际库Scene.metadata取最后NavEntry，SinglePaneScene是单entry；库在过渡期间可同时持有进/出内容。既有[字节码证据](../evidence/ui-c2-c4-20261027-070740/navigation3-scene-metadata.txt) | CONFIRMED库结构；是否异常滞留超过转场结束为UNKNOWN |
| 栈替换 | `app/LithicNavigation.kt:239–242`的clear/addAll是无挂起的同步两写；`app/LithicRoutes.kt:337–358`空/非法栈归Home | CONFIRMED；没有证据证明中间空栈实际显示或它是重叠根因 |
| 趋势入口临时null | `app/LithicNavigation.kt:203–211`按当前routes在NavDisplay外决定trendModel/state；出场Scene可能尚在而闭包输入已变；Review85–95对null显示“成本趋势尚未读取” | 源码支持的闪烁风险链；并非已确认的逐帧归因 |
| open/refresh/pause | `feature/review/TotalCostTrendViewModel.kt:52–94`：open触发refresh；refresh只copy loading/error/notice，**保留旧snapshot/timeline/selectedPoint**；pause取消请求但不清结果 | CONFIRMED；不能把“刷新清空结果”当根因去重写VM |
| 请求/换代保护 | VM的readTicket/pointTicket/active/捕获snapshot校验仍在；`app/MainActivity.kt:111–127`清Store/换handle/epoch；`app/LithicApp.kt:41–43`以generation为key | CONFIRMED，必须保留 |

因此能够提出导航呈现修正，但不能宣称已确定全部卡顿原因。录屏两处究竟走普通pop、预测返回或快速连续导航，手机动画倍率、系统资源、帧时序、是否真实长于过渡期限，仍UNKNOWN。没有主线程/数据库阻塞证据，不开展引擎重写或性能试错。

### 6.2 一次实施的导航设计

- 首个C5完整候选中，**普通forward、普通pop、predictive三条呈现路径统一不产生同位置双层可读内容**；首选三条均无页面alpha/scale转场，先取消C4深页双向淡化，不先创造另一套动效。
- 每个Nav Scene内容根统一满尺寸、不透明主题背景；保持原NavDisplay、entry装配及Saveable/ViewModel装饰责任。不能只给外层Scaffold上色，也不能只加背景却保留双向半透明叠读。
- 不重写backstack、不替换Nav3。requestLeave、navigationAvailable、dirty/pending/discard、恢复中禁导航、canonical/browsingAware/milestoneAware及来源ID均保持；预测手势取消/提交仍由原Nav3/onBack合同处理，**无动画不等于禁用手势或绕过确认**。
- 若实现时收敛clear/addAll为单次UI快照批次，只改变提交可见性，目标栈内容/校验/守卫不变；这是需验证的收敛项，不预设为已证实的根因修复，更不借机重做路由系统。
- 快速连续点击应得到最后一个获守卫允许的路由，不能留下另一Scene盖住点击区域。旧Scene离场不触发业务提交/清库/取消未知Owner。菜单/确认弹层的有意遮罩与页面叠字要分开判断。
- 保持底部选中态与实际目的地一致；返回保留来源与可用浏览上下文。首页不为切换预跑长期趋势；也不关闭刷新来换取表面稳定。

### 6.3 趋势与读取连续性

- 在同一generation/同一Repository内，惰性首次创建、保留**已有**ViewModel/StateFlow引用；离开时仍pause取消无用任务，不在出场Scene还可见时突然把其数据输入设null。不建立第二个缓存Store、持久缓存或默认首页计算。
- 重入仍按既有open/refresh和写后/跨日/返回前台规则更新。若有可确认的同代旧图，显示该旧图、真实截止日及固定的小刷新状态；失败持续提示“尚未刷新”和原重试，不能冒称最新。
- 首次确无结果显示读取中/真实错误；不以null或生命周期变化显示“空账本”。只有原事实/读取合同确认空库才用空态。不能仅删掉“尚未读取”四字来掩盖错误输入。
- 新选日尚未算完时，日期与旧金额不能被拼为一个新读数；保留原selectedPoint.date匹配、票据和snapshot身份栅栏。换代/Repository身份变化必须丢旧结果，不能为了少闪烁显示另一代数据。
- 围绕现有控制保留快照，不修改Owner/协调器、数据加载一致性或成本计算。若需要触碰这些冻结合同，停止该风险子项上报。

## 7. 用户操作与安全反馈策略

| 状态 | 默认呈现/允许行为 | 禁止的简化 |
|---|---|---|
| 首次加载、无可信快照 | 稳定读数占位/局部“读取中”，原允许操作；不显示0金额或0数量 | 用空态、假曲线或假0填充 |
| 同代刷新、有旧快照 | 保留正确身份/日期的旧内容＋紧凑刷新标识，保留原操作资格 | 每次清成“尚未读取”或假装最新 |
| 读取失败、有旧快照 | 旧截止/旧结果明确、持续错误＋原重试 | 折进Info、仅Toast或覆盖成空列表 |
| 读取失败、无旧快照 | 错误与安全重试/退出；根存储错误不建空库 | 改成“暂无记录”、建议清数据 |
| 确认空/筛选无匹配/无可排名项/对象缺失 | 分别说清当前范围，给原下一步，不互相替代 | 历史存在但当前0等同空库；丢失对象继续用旧详情 |
| 真0/负值/未开始无分母 | 原值与状态准确；不可计算用—，不参与不具资格的排名 | 截成0、隐藏负值、按显示字符串重新排序 |
| 正在提交IN_FLIGHT | 固定操作区禁重复，保留输入，标明正在保存 | 仅动画/Toast，重建operationId再提交 |
| UNCONFIRMED/冲突/已提交待重读 | 持续可见对象/结果不确定及原核对、同提交重试/重读资格 | 通用“失败重试”、重新造付款、把读取成功当写入成功 |
| 导出成功/取消/失败/残留/中断 | 由真实BackupExportResult及类型化状态呈现；残留警告持续 | 根据IDLE、字符串前缀或路径推断成功 |
| 恢复校验/预览/确认/恢复中/失败/未知 | 原阶段、完整对照、原确认；currentPreserved决定旧数据保留或未知；恢复中禁第二操作 | 先覆盖再预览、误称合并、虚假成功、取消已切换后仍称数据未改 |
| 系统权限拒绝/提醒不保证送达 | 业务仍可用；相关设置就地显示权限和受限原因 | 为了好看隐藏拒绝、主动重排/发布任务或保证准点 |
| 已确认低风险成功 | 原真实结果和可见数据变化；必要时短提示 | 用普通read提示推断write成功 |

### 7.1 什么可以按需，什么必须就地

- 可按需：公式长解、自然日/30/90/365完整定义、图表采样/重建口径、不是全部生活消费/不是当天扣款的重复段落、列表默认排序及范围细则。首页保留简短“非今日扣款”，相关完整说明始终可达。
- 必须就地：历史均价/当前套餐/折算时长等**指标身份**；暂停/到期/停止/未来生效等影响读数的**真实状态**；非现金折抵；旧结果/读取失败/写入未知；表单错误和隐藏字段错误；停止不取消服务商扣费；永久删除及重算影响；交易组保护；类别合并影响；备份未加密/残留不可用于恢复；恢复完整覆盖及结果未知。
- 继承UiMessageTone/提交phase/真实结果，不能按中文前缀猜等级。UiFeedbackText的“提示/需要核对/遇到问题”可统一为紧凑反馈区；严重错误和未知必须持久、文字可读、有原恢复操作，不一律变Toast。
- 覆盖恢复预览可分“内容对照/覆盖影响”，但全部当前→备份计数及重要覆盖事实、两个操作始终可达；不折叠关键后果、不绕Token、不重写协调器。
- 计算说明与关于仍是完整阅读入口；产品风险准确，但正常每页不复述风险报告。已知JPEG、提醒平台和未完成真机验证继续留开发/最终风险记录，不虚称关闭。

## 8. 下一阶段生产迁移范围与一次性实施顺序

以下全部是**待审查/待新授权**，本轮均未实施。主实现留在当前产品分支，不整merge/cherry-pick旧UI-C1，也不生成长期脱节的UI支线。

| 顺序/可回溯提交单元 | 生产文件与责任 | 完成检查 |
|---|---|---|
| 1. 导航与同代连续性 | `app/LithicNavigation.kt`、`app/C4NavigationMotion.kt`（可收敛为统一策略）；按需在C2Page收敛统一背景包装（当前没有满屏不透明保证）。`app/LithicRoutes.kt`/`app/MainActivity.kt`/`feature/review/TotalCostTrendViewModel.kt`先作保护对照，非默认重写目标 | 三种转场路径、透明背景、出场输入null均有针对性处理；来源/guard/generation/request票据不回归，先不增加动画 |
| 2. 全局金额内核 | `core/design/AmountText.kt`、`core/design/AmountLayout.kt`、必要的单一`MoneyMetric.kt`；`core/design/FormFields.kt`只改输入呈现，格式器/Parser不变 | §4的24直接/全部间接/手写/7输入点打勾；不存在两套缩放和单位拼接 |
| 3. 日常浏览和档案 | `core/design/EntryCard.kt`、`core/design/C2Page.kt`、`core/design/LithicTheme.kt`仅归一现有角色；Home/Search、项目/订阅List/History/Detail | 身份/状态/主指标一致、空与失败分离；全部菜单/付款/事件资格与来源保留 |
| 4. 表单、状态与交易 | ProjectEditor、MoneyEventEditor/Correction、End/Delete、TradeIn/ReplacementComparison、SubscriptionEditor/Renewal/StateDialog | 固定操作、字段错误与未确认持续；不改输入值/提交信封/三笔交易/删除及覆盖保护 |
| 5. 全部分析与完整图表 | ReviewShell、TotalCostTrendScreen（summary/full都做）、LongTermReview、CategoryAnalysis、AnnualReview、ReportPresentation、CostChartPage/CostChart及两种图表适配 | 精确读数和真实图形优先；不是只改摘要；采样/算法/轴/事件分页不变，年度不新聚合 |
| 6. 设置与全局反馈 | SettingsShell、RestorePreview、NotificationSettings、CategoryManagement、ProductInformation、MilestoneSources、UiFeedbackText | 普通说明按需、关键风险不隐藏，真实版本/类型化结果/通知引用保护不变 |
| 7. 一次整合与唯一候选 | 检查正常LithicApplication/MainActivity的生产路由、全部上述页面和§9矩阵；必要构建身份检查 | 中途不发Preview8/9/10；最后才准备一个完整候选，同preview包/原证书、code高于23。版本/设备操作另按后续明确授权，不在本轮修改 |

实施中可形成小范围独立提交，但**不以单页修好为交付、不要求用户反复安装**。不扩大Repository、数据层、媒体协议或工具链；确定性数据损失/越权删除/迁移恢复风险先停该风险子项并保留证据。真实封面单列专项前置条件，不将品牌样图或占位当实现完成。

### 定向验证落点（下阶段计划，均未运行）

- 金额：复用AmountFormatterTest、R1AmountLayoutTest及相关完整金额语义断言；补“完整金额组合”宽度、负/零/null/亚分钱、横向/全额查看测试。输入测试确认空白、0、付款>0及String/operationId不变。
- 导航：基于C2NavigationTest、P02BrowseNavigationTest、P03SettingsNavigationTest、C4NavigationMotionTest，覆盖主入口/深页普通进退及预测提交/取消；旧C4的180ms只是已被替代的呈现预期，不能连带删业务保护断言。
- 连续性：TotalCostTrendViewModel原票据/取消/换代/错误保留回归；补同代重入、退出Scene可见期间输入稳定、新选日不配旧金额及新generation不接旧结果。原本正确的行为直接回归，不人为制造RED。
- 页面：现有C4ReviewDensityTest等必要Compose断言更新为共同规范；覆盖完整趋势、单项图、两类详情、表单/交易/恢复预览，不能只测Home/Review。新增行为先记录实际失败再修正，不能改业务预期掩盖错误。
- 桌面：只跑受影响JVM、AndroidTest源码编译、Debug及必要候选构建/Lint，Manifest/权限/生产入口/签名/源绑定审计；不重跑M22完整历史套件。
- Android：必须另获设备授权或由用户自愿提供真实正常App画面；不恢复C3 A～F调查，不默认使用ANR旧AVD或接触旧账本。没有运行，保持NOT_RUN；编译、HTML、孤立宿主不能替代MainActivity视觉。

## 9. 下一阶段验收矩阵与停止点

本表都是未来门禁，当前为**DESIGN_ONLY / NOT_RUN**。A表示设计/源码核对，R表示真实Android画面/交互；A通过不能代替R。

| ID | 范围/样本 | 要求与证据 |
|---|---|---|
| C5-A01 | 金额账本M01～M24及所有上游 | 每点有迁移标记，无漏掉full模式/历史/对比；调用来源与指标含义不变 |
| C5-A02 | CNY/日均的正常、负、真0、null、正负亚分钱 | 统一组合/格式/语义；不重复元、负号/小数不丢，无K/W/省略；原formatter断言保留 |
| C5-A03 | 极长负金额与360/400dp、字体1.0/1.1/1.5/2.0 | 量整组、重排与可发现完整查看；普通金额不全降18sp；标明真实系统字体或仅LocalDensity |
| C5-A04 | Home全部、局部旧结果、确认空、历史无当前贡献 | 深绿主卡/真实数量/FAB、真预览/准确排序、无假记录；空不冒充0贡献，未知不冒充0数量 |
| C5-A05 | 两类列表/历史/搜索与>50记录 | 状态/一个主指标（搜索不新增指标）、稳定分页/排序、查询对应结果/错误，准确详情与原返回 |
| C5-A06 | 项目当前/结束/未来开始，含0/负/结算/折抵 | 当前与截至今天结果标签、冻结天数、真实事件/全部管理入口，删除二次确认/交易禁改仍有效 |
| C5-A07 | 订阅中/暂停/到期/停止/预付未来期 | 当前价/历史均价/折算时长/实际已过天数不混淆；所有现有付款与状态操作可达 |
| C5-A08 | 所有项目及订阅表单模式、长中文、IME | 字段/标签/错误/底部保存可达，隐藏错误展开，0/空/String/operationId/资格保持 |
| C5-A09 | 替换/交易预览、纠错/结束/永久删除 | 三笔事实/五值/非现金/整组保护与确认没有因精简丢失；不新造简化写入 |
| C5-A10 | 回看摘要和**完整总趋势** | 真图/完整选日/主金额优先，两贡献/所有精确日期入口/完整事件>50可达；普通说明不铺正文 |
| C5-A11 | 项目/订阅单项图 | 正负、零、峰谷、结束后结算段/未来计划/非现金准确，显示点/事件与原完整事实不丢 |
| C5-A12 | 类别、年度、三种长期榜 | 当前/历史口径分离；不合成新年度总支出；未舍入稳定排序/折算标签、不可计算不伪造0 |
| C5-A13 | 设置、类别/提醒、计算说明、关于、来源页 | 正常入口简洁且全可达；说明专页完整；版本真实、拒绝权限/无效来源可辨认 |
| C5-A14 | 导出/取消/写入失败/残留/中断/恢复预览/未知 | 真结果驱动，重要风险持续；覆盖对照与明确确认、原Token/旧数据保护和busy约束不变 |
| C5-A15 | 根存储未就绪/失败、旧generation/迟到请求 | 不建假空账本，不显示其他代结果；错误/退出可达，原保护回归 |
| C5-R01 | 正常MainActivity三主入口往返及快速连续切换 | 同一时刻无非预期双层可读页面，无旧Scene拦截触控；选中态/最终获准路由一致 |
| C5-R02 | 完整趋势→回看→其他页；普通返回及预测取消/提交 | 无双淡化重影、无临时“尚未读取”假状态；取消不丢输入、提交仍过guard；记录所用返回路径 |
| C5-R03 | 同代刷新/写后/跨日/返回前台/换代 | 旧图带真实截止和刷新/失败标记；新日期不贴旧金额，新代不保旧图；不得关闭必要刷新换稳定 |
| C5-R04 | 400dp浅深、确认空/合成有数据；360dp和大字关键页 | 每种页面族实际对照UI-C1及本规范；记录真实宽度/fontScale/density、主题和宿主；不用HTML替代 |
| C5-R05 | 固定保存区、长金额全额入口、重要弹层及辅助语义 | 实际滚动/点击/IME/焦点/返回可达；TalkBack实际操作未做则单列NOT_RUN，语义断言不冒充全覆盖 |
| C5-G01 | 最后唯一候选APK | 当前生产MainActivity使用已审查组件；同包同证书更高版本，权限/Room5/backup4/旧数据边界核对；不是正式Release/G1～G4自动通过 |

实际画面必须区分：原设计参考、生产Compose源码、隔离宿主合成渲染、正常MainActivity、用户实拍。合成事实不进入用户账本；手机序列、私人屏幕及无关数据不得提交。真实使用反馈拒绝时，不以JVM计数或APK身份PASS代替视觉整改。

## 10. 本轮审计结论与未证实项

- **CONFIRMED**：24个金额调用/7个金额输入及公共上游已盘点；默认分行和局部测量分叉成立。已覆盖上述30组页面/模式、公共图表/报表和存储门禁，范围不只Home/Review。
- **CONFIRMED**：深页双向180ms、透明Scene根、独立predictive规则、导航层趋势引用条件与VM原有保留机制；必须以三条呈现路径和已有同代结果连续性一起设计。
- **UNKNOWN**：具体录屏帧的实际路由分支、是否异常Scene滞留、资源/线程/帧时间瓶颈、手机当前字号/密度/动画倍率。本轮没有运行证据，不宣布根因全部定位或问题已修复。
- **NOT_RUN**：本轮JVM、Android/Instrumentation、编译/Lint、安装/升级、真实视觉/动效/IME/TalkBack；原C4的139 PASS及构建记录仅保留历史身份。
- **保护**：本轮受版本控制的变更只新增本文件；原设计PNG仅提取到忽略的build目录供查看，不是新样板。无需清理旧现场；不改生产/测试/版本/签名/数据库/备份/Owner/业务算法，未用ADB/设备、未清理任何现场。C3调查CLOSED，已装现场状态UNKNOWN；MEDIA_DISPLAY_PENDING及M10三JPEG FAIL、M20平台FAIL/UNKNOWN、M22 BLOCKED、M13与全部发行门禁历史状态保持。

下一决定仅为ChatGPT对**这份全产品设计规范**的独立核对。完成文档提交推送后停止；未获独立审查与后续授权，不实施Compose、不生成Preview8、不合并main、不进入M23。

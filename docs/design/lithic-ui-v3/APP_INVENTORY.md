# LITHIC UI-V3-01 · 全应用页面盘点

以生产工作树 `codex/lithic-ui-p2-home` 的 `f38cb97aec3559f79c70c2069c322e4ab67f45c1` 为准。P2-A1-R1 的应用源码基线为 `5e6920056903abbd73444ec9dddbfd57a16e5e50`，两提交间 `app/` 无差异。检查 MainActivity、LithicApp、Nav3 注册及实际 Compose 函数。下表覆盖 38 个注册项，按共用界面归并。另列出页内流程、弹窗、系统选择器与存储门。最后一列是源码观察或需样机验证的版式风险。没有在本轮运行 Android。[MainActivity][activity] · [LithicApp][app] · [Nav3 注册][nav] · [返回栈][routes]

## 主导航、当前与搜索

| 界面 | 用户任务和核心信息 | 操作、入口与返回 | 共用组件和关键状态 | 现状与设计问题 |
|---|---|---|---|---|
| 现在 | 扫读当前总日均成本、截至日期、非今日扣款，查看项目和订阅各至多两条、含历史已录入数量和成本构成 | 顶栏搜索、唯一新增菜单、两组「全部」、点档案条目、数量和成本说明、构成入口。底栏切换回看和设置 | `HomeArchiveRow`、`MoneyMetric`、`RecordCover`。正常、空账本、只有历史、真实零、旧快照、未取得、各预览独立失败 | 成本独占大标题和色面，条目又重复各自金额标签。照片与名称的识别权重偏低。总额和单件成本的时间口径须清楚。[首页][home] · [档案行][archive] |
| 搜索名称 | 同时找当前与历史项目和订阅。结果含类型、类别、状态和本批业务日期 | 现在 → 搜索。输入、IME 搜索、重读、继续加载、打开结果。详情返回原搜索栈，搜索返回现在 | `C2PageHeader`、`NameField`、可点击 `Surface`。空查询、加载、零结果、读取失败、旧查询结果、分页 | 类型、类别、状态各占层级，长名称下建议先核对信息优先级和焦点恢复。[搜索][search] · [返回栈][routes] |
| 顶层底栏 | 在现在／回看／设置间定位 | 选择顶层目的地。深层页面用页面返回及 Android Back。切换经 `requestLeave` 守卫 | `C2PrimaryNavigationBar`、`NavigationBarItem`、`C2Icon`。选中语义与系统 Insets | 保留三标签与选中态。图标、容器和文字应统一到同一品牌尺度，避免成为第四组内容。[底栏][tabs] · [离开守卫][leave] |

## 长期项目

| 界面 | 用户任务和核心信息 | 操作、入口与返回 | 共用组件和关键状态 | 现状与设计问题 |
|---|---|---|---|---|
| 长期项目列表 | 找未结束项目，查看名称、封面、类别、持有状态和时长、日均成本 | 现在 → 项目「全部」。新增、打开记录、继续加载、重读。返回现在 | `C2PageHeader`、`EntryCard`、`RecordCover`。加载、确认空、旧结果、未知、分页 | 列表的 64dp 方封面和状态胶囊与首页的 80×60dp 档案行形成两套密度。金额无法并排时每行变高。[项目列表][plist] · [EntryCard][entry] |
| 项目历史档案 | 查已结束项目、截至观察日的结果和后续真实结算 | 回看 → 历史档案。打开详情、加载更多、重读，返回回看 | `C2PageHeader`、`C5InformationButton`、`EntryCard`。空、旧快照、未知、分页 | 列表仍用当前记录式视觉。结束事实与后续资金事实的展示关系需要更明确。[项目历史][phistory] |
| 项目详情 | 识别物品、持有情况与日均成本，核对投入、收回、净投入、逐笔事实、取得与结束信息 | 从首页、列表、搜索、回看/趋势/类别/年度/长期复盘或替换对比进入，按来源返回。记一笔、编辑资料、结束/更正、纠错、成本历史、替换、以旧换新、删除、照片操作 | `C2PageHeader`、`RecordCover`、`MoneyMetric`、`C5InformationButton`、`ModalBottomSheet`。缺失、旧快照、未开始/无日均、封面不可用、交易组保护 | 照片和身份、摘要、事件、资料分布在多个区域。维护独立的风险动作，但让名称、持有和成本能连贯读完。[项目详情][pdetail] · [详情接线][detailnav] |
| 新增长期项目 | 记录名称、类别、取得日与初始实际投入，可补付款日和备注 | 首页新增菜单或项目列表新增。保存进入真实详情，返回相应父级。退出受未保存和写入守卫 | `ProjectEditorScreen`、`C2FormScaffold`、`NameField`、`AmountField`、日期/类别选择。空金额与真实 0、日期校验、提交中和待核对 | 折叠能减轻首屏压力。核心输入与隐藏字段错误之间需要明确焦点和展开顺序。[项目编辑器][pedit] |
| 编辑长期项目 | 更正已有项目资料和允许编辑的初始事实 | 项目详情更多 → 编辑。保存返回记录详情，返回时守卫草稿 | 同一 `ProjectEditorScreen` 的编辑模式。加载、冲突重读、已提交只读核对、以旧换新日期限制 | 与新建共用骨架，但编辑的原始事实、锁定字段和改动影响不能被新建式标签掩盖。[项目编辑器][pedit] |
| 记录投入或收回 | 录入新的实际发生款项，核对方向、金额、日期、说明 | 项目详情「记一笔」→ 投入或收回，两入口进入同一表单并预选方向。保存/返回项目详情 | `MoneyEventEditor`、`C2FormScaffold`、`AmountField`、`ProjectDateInput`。真实零、输入无效、保存中、未确认与只读复核 | 两个方向同页切换，设计需优先强调方向和金额，避免把收回误当负值输入或旧记录纠错。[金额记录][pmoney] |
| 结束项目／修正结束记录 | 保存或更正结束日和原因，可关联实际收回日期、金额、说明 | 项目详情更多或相关结束记录进入。`ProjectEnd`、`ProjectUpdateEnd` 两路由共用全页表单。完成回详情，已结束可从历史进入 | 名为 `EndProjectDialog`，实际使用 `C2FormScaffold`。收回可选、交易组锁定、冲突和提交核对 | 结束保留档案，删除永久移除。并排呈现会让不同后果不够突出，需在操作附近明确解释。[结束与更正][pend] · [项目详情][pdetail] |
| 纠正金额记录 | 对照原项目、方向、记录类型，修正既有事实 | 详情金额事件 → 纠错。保存回详情。真实的新退款从「记一笔」新增收回 | `MoneyEventCorrectionScreen`、`C2FormScaffold`、`C5InformationButton`。交易组只能改说明、无效/冲突/待核对 | 原事实和新输入相邻但仍为分开的段落，建议在窄屏评估一眼辨识新旧的能力。[金额纠错][pcorr] |
| 删除项目 | 核对受影响记录数量、封面引用、删除后果 | 详情危险操作 → 独立路由承载 `AlertDialog`。取消回详情，确认成功后回原浏览入口 | `DeleteProjectDialog`、`UiFeedbackText`。交易关联阻断、记录冲突、忙、未确认、完成后重读 | 对话框信息多但必要。先呈现对象和不可逆后果，再展示受影响范围和恢复动作。[项目删除][pdelete] · [原浏览路径][routes] |
| 项目成本历史 | 查某日项目成本和构成该时间线的金额事实 | 详情「查看成本历史」→ 日期定位/前后选点、历史事实、重读，返回详情 | `ProjectCostChart`、`CostChartPage`。加载、缺失、旧值、无有效分母、真实零或负值 | 共用图表页面能保留一致操作，项目图仍需突出净投入与持有天数的业务关系。[项目图][pchart] · [图表页][chart] |
| 替换对比与关系编辑 | 看前任/后任链和各自时长成本，选择目标与方向、预览建立或解除关系 | 项目详情 → 替换对比。切换项目、建立或解除关系并确认。子详情返回对比，回退回来源详情 | `ReplacementComparisonScreen`、`C2Section`、`MoneyMetric`、页内关系 `AlertDialog`。没有关系、分页、冲突、提交待核对 | 链方向、关系对象和金额事实相邻。须让关系编辑的影响清楚，它只调整关系。[替换对比][replace] |
| 以旧换新 | 确认旧物、交易日、新物、真实补款和非现金折抵，可选择同时建替换关系 | 项目详情 → 表单 → 整组预览 → 一次提交。保留返回修改和确认 | `TradeInEditorScreen`、`C2FormScaffold`、`MoneyMetric`。输入校验、可确认、未确认、交易组保护 | 确认屏必须保留三笔事实、非现金口径和不能整组撤销的提示，适合分成对象、事实、后果三个可扫描区段。[以旧换新][trade] |

## 订阅

| 界面 | 用户任务和核心信息 | 操作、入口与返回 | 共用组件和关键状态 | 现状与设计问题 |
|---|---|---|---|---|
| 订阅服务列表 | 找未停止服务，比较状态、折算付费时长、历史平均日成本 | 现在 → 订阅「全部」。新增、打开、继续加载、重读、列表口径说明，返回现在 | `C2PageHeader`、`EntryCard`、`RecordCover`。暂停与到期仍列在此，空、未知、旧快照 | 「已录入」提示与当前范围并列，设计需把列表范围和全账本计数说清。历史平均日成本不能缩成当前套餐价。[订阅列表][slist] |
| 已停止订阅 | 查阅停止记录和真实付款历史 | 回看 → 已停止订阅。打开详情、加载更多、重读，返回回看 | `EntryCard`、`C2PageHeader`。空、旧快照、未知、分页 | 与当前列表共用档案外观，停止与历史付款仍要快速区分。[停止历史][shistory] |
| 订阅详情 | 查看服务身份、状态、历史平均日成本、当前套餐与标准日均价、真实付款、状态事实及两种时长 | 从首页、列表、搜索或分析入口进入，按原来源返回。主操作记真实续费，管理面板进价格历史和状态操作，付款行可修正或删除错误付款 | `C2PageHeader`、`RecordCover`、`MoneyMetric`、`C2Section`、管理 `ModalBottomSheet`、付款 `DropdownMenu`。暂停到期、预付未来、无有效套餐、无有效分母、缺失、旧付款快照 | 多个日均指标及三组日期在纵向连续出现。需要把历史成本和当前覆盖组织成可快速比较的两组信息。[订阅详情][sdetail] · [订阅接线][subnav] |
| 新增订阅 | 建立服务身份，确认第一笔真实付款，输入名称、类别、备注、金额、周期和四个日期 | 首页新增菜单或订阅列表进入。保存进入详情，返回相应父级 | `SubscriptionEditorScreen`、`SubscriptionPaymentFields`、`C2FormScaffold`。正金额、免费期不生付款、有效覆盖、待核对 | 四日期和周期是必核事实，适合以付款、覆盖、提醒语义分组，降低密度。[订阅创建][sedit] |
| 记录续费／修正付款／付款并启用 | 保存新付款或更正既有付款。区分预填参考、真实生效、到期边界、提醒日和固定停止日 | 详情主操作、付款行菜单、管理入口中的启用条件分流。返回订阅详情 | `RenewalEditorScreen`、`SubscriptionPaymentActions` 和共用字段。真实款项、历史身份、固定停止日、确认状态、只读重查 | 模式标题已区分新事实与纠错，共用字段使说明密度相近。建议突出当前付款目标和覆盖差别。[续费编辑][renew] · [订阅接线][subnav] |
| 「编辑订阅」现有范围 | 纠正既有付款及状态事实。现有生产 Nav3 没有单独修改名称、类别、备注的通用资料编辑路由 | 使用订阅详情的付款或状态事实入口。本轮盘点覆盖已存在的编辑能力 | `CorrectSubscriptionRenewal`、`CorrectSubscriptionState`；类别归属变更另有全局类别管理规则 | 设计交付应准确命名「修正付款」「修正状态」，避免用「编辑订阅」承诺源码中不存在的表单。[订阅路由][routes] · [订阅接线][subnav] |
| 暂停、恢复、重新开通、停止、修正状态、取消尚未生效停止计划、删除错误付款 | 核对旧状态、真实覆盖、固定停止日或错误付款的影响。服务商扣费需在服务商侧处理 | 订阅管理、状态事实或付款菜单进入；提交/取消返回详情。恢复无有效覆盖时按业务条件进入付款确认 | `SubscriptionStateDialog` 实为路由内 `C2FormScaffold`。`SubscriptionOperationContent` 按状态显示付款或状态表单。确定失败、忙、最后一笔付款不得删、未确认、已提交后重读 | 页面不是一组等价开关。暂停、停止、取消计划和删除错误付款需各自显示对象、结果与确认。[订阅操作][sstate] · [操作路由][opnav] |
| 订阅价格历史 | 查付款、实际生效及付费到期边界，回看选定日期的价格事实 | 订阅详情管理 → 查看价格历史，定位日期、查事件和重读，返回详情 | `SubscriptionPriceChart`、`CostChartPage`。未生效、无有效套餐、读取失败、无记录、截取窗口 | 单项订阅图可共用操作和零值语义，标题和图例需避免与项目累计成本混用。[订阅图][schart] · [图表页][chart] |

## 回看与分析

| 界面 | 用户任务和核心信息 | 操作、入口与返回 | 共用组件和关键状态 | 现状与设计问题 |
|---|---|---|---|---|
| 回看 | 浏览趋势摘要、分析入口、已结束项目和已停止订阅 | 底栏回看。进完整趋势、类别、年度、长期复盘、历史档案；空态回首页记录 | `ReviewShell`、`TotalCostTrendScreen(summaryOnly)`、`C5InformationButton`。可信空、跨来源未核对、读取提示、旧结果 | 分组卡和说明可能与趋势主任务争抢首屏。空态、摘要和各分析入口需分清主次。[回看][review] |
| 完整成本趋势 | 查选定日总日均、项目与订阅贡献、历史曲线及事件 | 回看 → 成本趋势。日期定位、前后选择、分项曲线、图表说明、显示点及事件、跳详情。返回回看 | `MoneyMetric`、`TotalCostChart`、`TrendInformationDialog`。首次占位、旧快照刷新、错误、请求日期不可得、真实零/负值、历史事实可继续查 | 图、日期、说明和事件入口密集。需要统一图表工具区与主阅读顺序，保留选中日和完整事件入口。[完整趋势][trend] |
| 类别分析 | 按当前分类看项目或订阅当前贡献、历史真实金额、年度及相关记录 | 回看或首页类别入口进入。选类型和类别、重读、继续查看年度/记录，打开原记录后保留类别上下文 | `C2PageHeader`、`MoneyMetric`、年度条形图与记录行。单类别直接展示、多类别、无类别/事实/年份、旧结果 | 类型、类别、汇总和历史叠在同一滚动层级，适合稳定筛选区和清晰的结果读序。[类别分析][category] · [类别返回][nav] |
| 年度回顾 | 按实际资金日看当年项目投入/收回/净投入和订阅付款，查看类别和跨年变化 | 回看 → 年度回顾。精确选年、上一年/下一年、展开年份/相关记录并跳详情，返回回看 | `C2PageHeader`、共用报告金额/条形组件。无记录年份、真实零/负值、旧快照和结果未知 | 输入年度、年度切换和条形图里的选年重叠。设计需厘清主选择器，区分两类金额，缺失年份保留空语义。[年度回顾][annual] |
| 长期复盘 | 按项目陪伴时长、日均成本或订阅折算付费时长比较当前与历史档案 | 回看 → 长期复盘。切换类型和排序、分页、重读、打开记录，按来源返回 | `C2PageHeader`、`FilterChip`、`MoneyMetric`、排名条目。不可排名、无分母、真实零/负值、旧结果 | 两层选择器、名次和说明占据视线，需以可识别记录和排序口径为主，保留未排名事实。[长期复盘][long] |

## 设置、备份与辅助页面

| 界面 | 用户任务和核心信息 | 操作、入口与返回 | 共用组件和关键状态 | 现状与设计问题 |
|---|---|---|---|---|
| 设置与外观 | 查看本地记录时区、管理类别、切换主题、了解本地保存与备份、阅读计算和关于 | 底栏设置。主题为页内底部面板；类别、计算、关于走子路由。备份、恢复和照片空间在设置内 | `C2Section`、`C2SettingRow`、`AppearanceThemePicker`。主题加载或保存反馈、备份操作互斥 | 重复的 32dp 图标底色与箭头让普通说明和高风险动作视觉相近，可用分组及明确动作动词区分。[设置][settings] · [外观][appearance] · [设置行][page] |
| 完整导出与文件检查 | 导出未加密完整备份，或仅校验外部备份文件 | 设置 → 系统 SAF 保存/选取位置，回到设置结果区。可以取消导出或校验，后续操作按状态启用 | `C2SettingRow`、`BackupInspectionFeedback`、三个 SAF launcher。选择、进行中、成功/已验证、无效/不支持、取消、失败、未知 | 相关动作共享入口区域但后果不同。完整检查的 VERIFIED 只表示文件校验，恢复还需单独核对与确认。[设置备份][settings] · [检查结果][confidence] |
| 完整恢复与核对备份 | 核对备份创建时间、时区、当前与备份的对象数量，选择保留现状或覆盖 | 设置 → SAF 选文件 → 校验 → 在设置内 `RestorePreviewScreen` → 明确确认完整覆盖/取消。覆盖时导航和其他备份动作受守卫 | `RestorePreviewScreen`、`UiFeedbackText`。不支持、校验失败、快照改变、取消、恢复中、成功/失败/结果未知 | 比较表及覆盖警告都在设置滚动区。设计需让比较对象和不可合并后果在确认前连续可见。[恢复预览][restore] · [设置备份][settings] · [离开守卫][leave] |
| 照片存储情况 | 查登记总数、被记录引用的去重照片数、历史未引用数、历史登记体积和总登记体积 | 设置页内查看和重读 | `PhotoStorageSection`。加载、就绪、不可用，无永久清理动作 | 估计登记体积需要和磁盘实占区分，历史记录可能继续参加完整备份。[照片存储][confidence] |
| 类别管理 | 分开管理项目与订阅的类别和含历史引用数 | 设置 → 类别管理。新建、改名、选择合并目标、核对影响数量后确认；返回设置 | `C2PageHeader`、`FilterChip`、类别 `AlertDialog`。默认「其他」保护、冲突、忙、未知结果 | 列表行里重复改名和合并按钮。可归并次要操作，但保留历史重分类和删除源类别的后果。[类别管理][catmanage] |
| 计算说明／关于 | 查业务口径和安装版本、本地存储等产品信息 | 设置进入对应子页面，计算说明也可从首页说明进入，均回原父级 | `InformationPage`、`C2PageHeader`、可滚动章节 | 密集但属于按需信息，适合可跳读的章节层级及宽度限制。[说明与关于][about] |
| 提醒延期／历史里程碑路由 | 当前提示自动提醒 V1.1，源码保留两个历史通知目的地 | 设置内可见延期文字。`NotificationSettings`、旧 `MilestoneSources`、`InvalidMilestoneNotice` 仍注册。提醒能力关闭时旧通知不解析为实际里程碑详情 | `NotificationDeferredScreen`；`MilestoneSourcesScreen` 和历史通知设置实现属于门控/未接入路径 | 规划只覆盖当前延期说明。需用导航接线区分有源码与用户可用，避免把延期能力画进 V3。[提醒接线][settingsnav] · [延期页][deferred] |

## 模态、系统交接与异常

| 表面 | 用途、核心信息及主要操作 | 调用和退出 | 关键状态与设计问题 |
|---|---|---|---|
| 首页新增／数量与成本口径 | 从一个新增入口选项目或订阅；按需看含历史数量及总日均语义 | 首页底部面板/对话框。未选直接返回首页 | 200% 字号路径独立预留底部操作区。正常字号与最后一条内容的间距仍待端侧核对。[首页][home] |
| 项目更多／记一笔，订阅管理／付款操作 | 分离浏览、正向写入和风险操作 | 详情页内 `DropdownMenu` 或 `ModalBottomSheet`。关闭回详情，选操作进入原路由 | 停止不是取消服务商扣费，结束不是删除。新设计保持能辨认且能安全退出。[项目详情][pdetail] · [订阅详情][sdetail] |
| 日期、类别、口径、完整金额 | 核对真实日期与范围，选择合适类别，查看解释或长金额原值 | 表单 `ProjectDateInput` 的 Material3 日期选择、分类对话框、页面 `C5InformationButton`、`MoneyMetric` 完整金额对话框 | 保留日期错误和真实边界、焦点返回、金额单位及完整原值；解释不必每个表面占固定色块。[表单选择器][inputs] · [订阅表单][sedit] · [信息按钮][info] · [金额][metric] |
| 系统照片选择与确认 | 项目可选照片、预览、更换或取消封面显示 | 项目详情 → `PhotoLiteCoverPanel` → 系统 Photo Picker／本地确认。订阅仅显示现有引用。确认受 API36、系统 Picker 与当前账本资格约束 | 准备、读取、预览、保存、失败、结果未知、取消显示但保留历史媒体。风险操作须说明作用范围，不能暗示文件永久删除。[封面面板][photo] · [详情接线][detailnav] |
| 未保存／提交未确认 | 拦截会丢草稿或重复写入的返回和跳转 | Nav3 `requestLeave` 以及表单返回/跨目的地。未保存选择继续或明确放弃；提交未确认继续核对、按原表单及身份恢复 | 保留原始输入、原提交身份、只读重查和替换确认边界。未确认不能套用可放弃草稿的提示。[离开守卫][leave] · [守卫弹窗][dialogs] |
| 账本启动门与已就绪读失败 | 初次本地读取、阻断不可打开账本；已就绪时保留旧快照和受控重读 | `LithicApp` 在初始化和失败时用 `StorageGate`。已就绪页面通过通知和独立列表重读处理刷新失败 | 真实冷启动失败提供保护提示与退出。首页无快照错误 H 是受控 Ready 呈现，不能将它设计成启动失败时重建或重试账本。[启动门][app] · [首页][home] |

## 复用边界与证据

- 生产入口沿用 MainActivity、LithicTheme、LithicApp 和现有 Owner。三主题使用固定主题选择，V3 计划从视觉角色着手。共用页头大字号时将标题移到下一行，表单骨架留出 IME 和固定操作区。需要核验任何共用改动的全部调用者。[入口][activity] · [主题][theme] · [共用页面][page]
- 首页的 `HomeArchiveRow` 使用 4:3 的等比 Fit 封面，目标 80×60dp，窄宽下可收至 64×48dp。列表共用的 `EntryCard` 使用 64dp 框，`RecordCover` 默认 Crop。封面没有引用时以类型占位，无效引用有不可用反馈。读取预算、Owner、generation 与请求票据由现有封面组件保护。[档案行][archive] · [列表条目][entry] · [封面组件][cover]
- 图表按总成本、项目成本、订阅价格、年度与类别等分别表达。共用画法不应合并统计口径。金额保留完整元／元每天、未得值、真实零与负值以及复制/完整查看路径。[金额][metric] · [趋势][trend] · [共用图表页][chart] · [年度回顾][annual]
- 阅读范围为上述页面及组件、路由与入口、设置与合成状态接线。历史 P2-A1-R1 的端侧证据用于首页；P2-A2 只作归档参考。详情、表单、恢复、TalkBack、极窄屏和系统字体下的具体视觉表现本轮未运行。视觉审查与 Skill 使用见 [UI_AUDIT.md](UI_AUDIT.md)，核心操作详见 [USER_JOURNEYS.md](USER_JOURNEYS.md)。

[activity]: ../../../app/src/main/java/com/cooper/lithic/app/MainActivity.kt#L45
[app]: ../../../app/src/main/java/com/cooper/lithic/app/LithicApp.kt#L34
[nav]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L451
[routes]: ../../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt#L161
[leave]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L273
[tabs]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L811
[dialogs]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L717
[detailnav]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L555
[subnav]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L675
[opnav]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L773
[settingsnav]: ../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L506
[home]: ../../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L42
[archive]: ../../../app/src/main/java/com/cooper/lithic/feature/home/HomeArchiveRow.kt
[search]: ../../../app/src/main/java/com/cooper/lithic/feature/home/SearchScreen.kt#L38
[plist]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectListScreen.kt#L37
[phistory]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectHistoryScreen.kt#L35
[pdetail]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt#L67
[pedit]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectEditorScreen.kt#L48
[pmoney]: ../../../app/src/main/java/com/cooper/lithic/feature/project/MoneyEventEditor.kt#L28
[pend]: ../../../app/src/main/java/com/cooper/lithic/feature/project/EndProjectDialog.kt#L26
[pcorr]: ../../../app/src/main/java/com/cooper/lithic/feature/project/MoneyEventCorrectionScreen.kt#L16
[pdelete]: ../../../app/src/main/java/com/cooper/lithic/feature/project/DeleteProjectDialog.kt#L24
[pchart]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectCostChart.kt#L15
[replace]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ReplacementComparisonScreen.kt#L38
[trade]: ../../../app/src/main/java/com/cooper/lithic/feature/project/TradeInEditorScreen.kt#L36
[slist]: ../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionListScreen.kt#L40
[shistory]: ../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionHistoryScreen.kt#L35
[sdetail]: ../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt#L35
[sedit]: ../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionEditorScreen.kt#L52
[renew]: ../../../app/src/main/java/com/cooper/lithic/feature/subscription/RenewalEditorScreen.kt#L26
[sstate]: ../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionStateDialog.kt#L39
[schart]: ../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionPriceChart.kt#L15
[review]: ../../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt#L43
[trend]: ../../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt#L85
[annual]: ../../../app/src/main/java/com/cooper/lithic/feature/review/AnnualReviewScreen.kt#L21
[long]: ../../../app/src/main/java/com/cooper/lithic/feature/review/LongTermReviewScreen.kt#L36
[category]: ../../../app/src/main/java/com/cooper/lithic/feature/category/CategoryAnalysisScreen.kt#L19
[catmanage]: ../../../app/src/main/java/com/cooper/lithic/feature/category/CategoryManagementScreen.kt#L39
[settings]: ../../../app/src/main/java/com/cooper/lithic/feature/settings/SettingsShell.kt#L34
[appearance]: ../../../app/src/main/java/com/cooper/lithic/feature/appearance/AppearanceThemePicker.kt#L32
[restore]: ../../../app/src/main/java/com/cooper/lithic/feature/settings/RestorePreviewScreen.kt#L29
[confidence]: ../../../app/src/main/java/com/cooper/lithic/feature/settings/DataConfidenceSections.kt
[about]: ../../../app/src/main/java/com/cooper/lithic/feature/settings/ProductInformationScreen.kt#L27
[deferred]: ../../../app/src/main/java/com/cooper/lithic/feature/settings/NotificationDeferredScreen.kt#L17
[photo]: ../../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteCoverPanel.kt
[cover]: ../../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviews.kt#L52
[inputs]: ../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectInputs.kt#L105
[entry]: ../../../app/src/main/java/com/cooper/lithic/core/design/EntryCard.kt
[chart]: ../../../app/src/main/java/com/cooper/lithic/core/design/CostChartPage.kt#L43
[page]: ../../../app/src/main/java/com/cooper/lithic/core/design/C2Page.kt
[info]: ../../../app/src/main/java/com/cooper/lithic/core/design/C5Information.kt
[metric]: ../../../app/src/main/java/com/cooper/lithic/core/design/MoneyMetric.kt
[theme]: ../../../app/src/main/java/com/cooper/lithic/core/design/LithicTheme.kt

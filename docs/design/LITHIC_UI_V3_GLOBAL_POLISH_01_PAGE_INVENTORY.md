# 砺石 UI-V3 全应用页面清单

盘点基线为交接分支 `c5797d4dd11374c6368d64e4b283d64e0a1b57d6`。页面按 [Nav3 注册和调用](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L468) 的 38 个 `entry<T>` 核算。共用页面合并描述，保留每个注册键。其余行是宿主、嵌入区、局部弹层和系统交接。源码路径以 `app/src/main/java/com/cooper/lithic/` 为前缀。

## 颜色和执行标记

- **页**使用 `background/onBackground` 或宿主 Scaffold，正文和辅助信息用 `onSurface/onSurfaceVariant`。**面**使用 `surface`、分组容器、边线和分隔线。**主**使用 `primary/onPrimary` 或首页品牌操作色。**态**包括错误配对和 `LocalLithicVisuals` 的成功、警告配对。**图**使用 `LocalLithicVisuals` 的实数据图表角色。**层**使用 Material 3 对话框、选择器、菜单或面板的容器及遮罩。
- 基线纯白页面和卡片均为 `#FFFFFF`，分组为 `#F5F5F5`。雾蓝页面为 `#F7F6F2`，卡片 `#FFFFFF`，分组 `#E8EEF2`。深色页面、卡片、分组分别为 `#121212`、`#1E1E1E`、`#292929`。共享颜色接线见 [主题映射](../../app/src/main/java/com/cooper/lithic/core/design/LithicTheme.kt) 和 [颜色角色](../../app/src/main/java/com/cooper/lithic/core/design/LithicVisuals.kt)。以下角色兼含直接及通过共用组件取得的颜色。
- 「主题」指随共享 token 更新。「精简候选」指有确定的可核查冗余，实际删改以同阶段 Copy Audit 和源码差异为准。「保留」表示语义或资格约束，无局部文案变动建议。列出的测试简称可在 `app/src/androidTest/java` 找到，用于覆盖映射。它们以及主题切换、真实 200% 和 TalkBack 本轮均记为 `NOT_RUN`，直至另有端侧证据。

## 导航注册清单

| 页面／注册键 | Compose 源码与来回路径 | 颜色 | 可见文字问题及本轮决定 | 验证 |
| --- | --- | --- | --- | --- |
| 现在 `Home` | `feature/home/HomeShell.kt`。一级 Tab，进入完整列表、记录、搜索和成本分析。 | 页、面、主、态与品牌总览 | 主题和说明入口内部精简候选。保留总投入、日均、日期、数量含历史、上次快照和未知区分。 | `HomeAndroidAcceptanceTest`、`HomeOverviewUiTest` |
| 回看 `Review` | `feature/review/ReviewShell.kt`。一级 Tab，分发历史、趋势、类别、年度和长期复盘。 | 页、面、态 | 路由入口描述与标题重复可缩。保留数据不足及历史入口。 | `P02ReviewShellSemanticsTest` |
| 类别分析 `CategoryAnalysis` | `feature/category/CategoryAnalysisScreen.kt`。首页构成或回看进入；项目／订阅或类别汇总，记录返回原分析。 | 页、面、图、态 | 精简反复出现的结果范围说明。类别、口径、日期、无法读取与真实零保留。 | `SingleCategoryPresentationSemanticsTest` |
| 年度回顾 `AnnualReview` | `feature/review/AnnualReviewScreen.kt`。回看进入；真实资金年份、金额和记录入口。 | 页、面、态 | 减少静态介绍。真实付款年份与实际归属保留。 | 年份切换、无数据、记录返回 |
| 总成本趋势 `TotalCostTrend` | `feature/review/TotalCostTrendScreen.kt`。回看进入，图表点选、日期和来源记录，返回选定区间。 | 页、面、图、态 | 主题与展示文字精简候选。读点、数据缺口、图表说明仍可达。 | `M18TotalCostTrendSemanticsTest`、`M18TotalCostTrendJourneyTest` |
| 长期复盘 `LongTermReview` | `feature/review/LongTermReviewScreen.kt`。回看进入，条件和客观排行，进入记录后返回原榜单。 | 页、面、态 | 精简静态引导；排行榜条件、单位、不可比较的项保留。 | `HistoryReviewTest` |
| 设置 `Settings` | `feature/settings/SettingsShell.kt`。一级 Tab，外观、类别和时区、备份、照片统计、产品说明。 | 页、面、主、态 | 主题与冗余小标题、重复帮助精简候选。保留本地保存、未加密、延期能力及覆盖风险。 | `C4SettingsDensitySemanticsTest`、`P03SettingsInformationTest` |
| 计算说明 `CalculationExplanation` | `feature/settings/ProductInformationScreen.kt`。首页或设置进入，系统返回原来源。 | 页、面 | 精简重复句。保留总投入只统计项目 OUTLAY、非现金折抵、收回及订阅边界。 | 同屏核对四类金额口径和返回 |
| 关于 `AboutLithic` | `feature/settings/ProductInformationScreen.kt` 的 `AboutLithicEntry`。设置进入。 | 页、面 | 精简泛介绍。保留已安装身份、版本读取失败和本地数据事实。 | 可用／未知版本及返回 |
| 提醒兼容 `NotificationSettings`、`InvalidMilestoneNotice`、`MilestoneSources` | `feature/settings/NotificationDeferredScreen.kt`、`feature/reminder/MilestoneSourcesScreen.kt`。当前延期路由显示延期页，历史通知兼容；禁用门后才有有效来源／无效通知页面。设置上的延期说明是嵌入文本。 | 页、面、态 | 保留延期文案和禁用资格，主题继承。活动 `NotificationSettingsScreen.kt` 为源树历史能力，不计入当前产品可达面。 | `CoreLegacyReminderNavigationTest`，关闭能力断言 |
| 类别管理 `CategoryManagement` | `feature/category/CategoryManagementScreen.kt`。设置进入，项目／订阅分别管理，弹层创建、改名和合并。 | 页、面、主、态、层 | 常规引导可缩。保留合并影响、目标及确认后不可拆分等业务事实。 | `CategoryManagementScreenTest` |
| 名称搜索 `NameSearch` | `feature/home/SearchScreen.kt`。首页进入，当前与历史结果各回到原搜索。 | 页、面、态 | 初始提示、无匹配和末尾提示可去重。查找范围与结果状态仍可辨。 | `SearchScreenTest`、`P02SearchExperienceTest` |
| 项目列表 `ProjectList` | `feature/project/ProjectListScreen.kt`。首页「全部」，可新增或打开详情。 | 页、面、主、态 | 精简重复范围、空列表帮助。持有状态、排序及失败不等同空保留。 | `ProjectScreensTest`、`ProjectRoutesTest` |
| 项目详情 `ProjectDetail` | `feature/project/ProjectDetailScreen.kt`。现在、当前列表、历史、搜索或分析进入，返回来源。 | 页、面、主、态、层；成本浅色面和操作色曾局部定义 | 保留冻结结构。检查重复帮助与身份描述。截止日期、非现金、已结束、写入未知与照片失败保留。 | `V3ProjectDetailScreenTest`、`V3ProjectDetailAndroidAcceptanceTest` |
| 项目成本历史 `ProjectCostHistory` | `feature/project/ProjectCostChart.kt` → `core/design/CostChartPage.kt`。详情操作进入。 | 页、面、图、态 | 标题和静态说明可紧缩。时间范围、累计事件、读点金额保留。 | 图表日期、可访问数据点、返回 |
| 订阅价格历史 `SubscriptionPriceHistory` | `feature/subscription/SubscriptionPriceChart.kt` → `core/design/CostChartPage.kt`。详情操作进入。 | 页、面、图、态 | 主题和共享图表科目文字复核；不能把价格和实付混为一项。 | 期限、标准价和付款事实、返回 |
| 替换链 `ReplacementComparison` | `feature/project/ReplacementComparisonScreen.kt`。项目更多进入，显示历代详情、关联表单与纠错。 | 页、面、主、态、层 | 次级重复解释可缩。不同关联类型、身份、候选资格及修改后果保留。 | 链与关联的只读／编辑及取消 |
| 以旧换新 `TradeInProject` | `feature/project/TradeInEditorScreen.kt`。项目更多／替换链操作进入，父路径保留。 | 页、面、主、态、层 | 精简标签。金额组成、非现金折抵、旧件结束和保留保护。 | 取值、提交保护、返回 |
| 新增／编辑项目 `CreateProject`、`ProjectEdit` | `feature/project/ProjectEditorScreen.kt`、`ProjectInputs.kt`。从新增菜单／列表或详情更多进入。 | 页、面、主、态、层 | 行标题、字段提示重复精简候选。初投、付款日、开始日、未保存、未知写入分开。 | `C5ProjectFormsTest`、`ProjectEntryFlowTest`、`ProjectNavigationGuardTest` |
| 记录投入／收回 `ProjectMoneyEntry` | `feature/project/MoneyEventEditor.kt`。详情「记一笔」选择方向，保存或取消回详情。 | 页、面、主、态、层 | 精简方向重复。金额、日期、真实收回、提交资格和明确反馈保留。 | `ProjectEntryFlowTest`、`ProjectNavigationGuardTest` |
| 已结束项目 `ProjectHistory` | `feature/project/ProjectHistoryScreen.kt`。回看进入，详情返回同一历史。 | 页、面、态 | 精简说明与空态。日期、状态和历史读取失败保留。 | `HistoryReviewTest` |
| 结束／更正结束 `ProjectEnd`、`ProjectUpdateEnd` | `feature/project/EndProjectDialog.kt`。虽名 Dialog，实际经 Nav3 显示 C2 表单页；更多及专用纠错进入。 | 页、面、主、态、层 | 文字可合并。结束与永久删、真实到账、折抵日期／金额保护保留。 | `ProjectLifecycleScreensTest`、`ProjectLifecycleFlowTest` |
| 更正金额 `ProjectMoneyCorrection` | `feature/project/MoneyEventCorrectionScreen.kt`。单笔事实入口，返回对应详情。 | 页、面、主、态、层 | 原事实与待修改值文字检查。事件 ID、方向、初始／常规纠错、结束结算和折抵边界保留。 | `ProjectLifecycleScreensTest` |
| 永久删除项目 `ProjectDelete` | `feature/project/DeleteProjectDialog.kt`。已注册路由内的真 AlertDialog，详情更多进入。 | 层、态、主 | 确认实际后果、不可撤回、取消和已有拒绝理由全部保留。 | 删除资格／取消／未知提交 |
| 订阅列表 `SubscriptionList` | `feature/subscription/SubscriptionListScreen.kt`。首页「全部」，记录或新增进入相应页面。 | 页、面、主、态、层 | 精简重复的筛选说明文字。状态、开始与到期边界、历史日均口径保留。 | `C2B1SubscriptionListTest` |
| 新增订阅 `CreateSubscription` | `feature/subscription/SubscriptionEditorScreen.kt`。首页新增菜单／列表进入。 | 页、面、主、态、层 | 字段辅助文字候选。有真实付款日期、日期边界、周期和免费选项原意义；无独立通用编辑路由。 | `SubscriptionScreensTest`、`SubscriptionNavigationGuardTest` |
| 已停止订阅 `SubscriptionHistory` | `feature/subscription/SubscriptionHistoryScreen.kt`。回看进入，历史记录详情后返回。 | 页、面、态 | 检查重复历史提示，保留停止日和读失败状态。 | `HistoryReviewTest`、`SubscriptionRoutesTest` |
| 订阅详情 `SubscriptionDetail` | `feature/subscription/SubscriptionDetailScreen.kt`。现在、当前列表、历史、搜索和复盘等来源。 | 页、面、主、态、层 | 精简重复说明和次级标题。历史平均日成本、当前标准日均价、累计实付分开，下一期已付待生效有事实才显示。 | `SubscriptionScreensTest`、`C2B2SubscriptionLayoutTest` |
| 续费／更正付款 `ConfirmSubscriptionRenewal`、`CorrectSubscriptionRenewal` | `feature/subscription/RenewalEditorScreen.kt`。主按钮或付款菜单进入，经原守卫返回。 | 页、面、主、态、层 | 可收紧已有文案。真实实付、生效、覆盖边界、原付款身份和重复写入保护保留。 | `SubscriptionRenewalFlowTest`、`SubscriptionNavigationGuardTest` |
| 暂停／恢复／停止／重新启用、纠正状态、取消待生效停止、删除付款 `ChangeSubscriptionState`、`CorrectSubscriptionState`、`CancelSubscriptionStop`、`DeleteSubscriptionRenewal` | `feature/subscription/SubscriptionStateDialog.kt`，由 `SubscriptionOperationContent` 复用 C2 表单页。管理面板和付款操作菜单进入。 | 页、面、主、态、层 | 保留不替服务商停扣款、尊重已付剩余期、固定截止、纠错影响、最后一笔不可删、删除非退款及未知写入。仅精简重复提示。 | `M09SubscriptionScreensTest`、`M09SubscriptionLifecycleFlowTest` |

## 局部状态、弹层和系统交接

| 所属页面／控件 | 实现位置 | 颜色 | 文案与决定 | 验证 |
| --- | --- | --- | --- | --- |
| 冷启动、初始化失败、快照刷新失败 | `app/LithicApp.kt` 的 `StorageGate`、`app/LithicNavigation.kt` 的读错误、`feature/home/HomeShell.kt` 的受控状态 | 页、态 | 保留真空、零、未知、旧快照和重试。冷启动守卫保持。 | `NavigationShellTest`、首页读取态测试 |
| 系统栏及三 Tab | `app/MainActivity.kt`、`app/LithicNavigation.kt` 的 Scaffold 与 `C2PrimaryNavigationBar` | 页、面、主 | 三个短标签保留，主题与图标对比检查。 | `ActivityNavigationTest`、Insets 和 TalkBack |
| 首页新增、成本构成、计算说明 | `feature/home/HomeShell.kt` 两个 `ModalBottomSheet`、信息 `AlertDialog` | 层、页、面、主、态 | 两个新增入口、项目和订阅选择、金额小计、分类和舍入说明保留。说明重复才缩。 | `HomeOverviewUiTest`、舍入说明、展开／返回 |
| 项目更多、记一笔面板、单笔金额事实 | `feature/project/ProjectDetailScreen.kt` 的 `DropdownMenu`、`ModalBottomSheet`、`AlertDialog` | 层、面、态 | 单笔日期、方向、金额、备注、非现金和有资格的纠错不可丢。更多可精简重复通用后果，具体危险处保留。 | `V3ProjectDetailScreenTest`、`ProjectLifecycleScreensTest` |
| 订阅管理、付款操作、列表口径 | `feature/subscription/SubscriptionDetailScreen.kt` 的面板、菜单，`SubscriptionListScreen.kt` 的口径对话框 | 层、面、态 | 保留各操作及真实状态，重复解释优先放口径入口。 | `SubscriptionScreensTest`、操作与 Back |
| 设置主题、备份信息 | `feature/appearance/AppearanceThemePicker.kt` 的 `ModalBottomSheet`，`feature/settings/SettingsShell.kt` 的信息对话框 | 层、面、主 | 三个主题名和选中语义。设置反复出现的提醒延期、文件安全、覆盖边界可合并，危险处保留。 | `C9ThemeMatrixTest`、`P03SettingsInformationTest` |
| 备份导出、检查、恢复预览与确认 | `feature/settings/SettingsShell.kt`、`DataConfidenceSections.kt`、`RestorePreviewScreen.kt`；三个 `*DocumentPicker.kt` 交给系统 SAF | 页、面、主、态；系统选择器用其系统主题 | 设置中的段落和嵌入预览。未加密、检查不写入、覆盖不合并、对照快照和保留安全副本的提醒保留。 | `BackupSettingsUiTest`、`BackupRestoreSettingsUiTest`、`P03BackupFeedbackSemanticsTest`；真实 SAF 待验 |
| 照片存储统计 | `feature/settings/DataConfidenceSections.kt` | 页、面、态 | 登记容量与实际磁盘空间有别，历史照片保留处理方式不改变。可缩重复叙述。 | 当前／历史／预计字节、错误与旧值 |
| 日期、项目类别、订阅类别 | `feature/project/ProjectInputs.kt` 的 `DatePickerDialog` 和类别 `AlertDialog`，`feature/subscription/SubscriptionEditorScreen.kt` 的类别 `AlertDialog` | 层、主 | 选择与取消短词、日期角色和筛选标签清晰。共享表单主题继承。 | `ProjectDateInputTest`、类别选择与大字 |
| 替换关联，类别建／改／合并 | `feature/project/ReplacementComparisonScreen.kt`、`feature/category/CategoryManagementScreen.kt` 的表单弹窗 | 层、主、态 | 保留选项、预览、确认、错误与不确定写入等条件；避免标签与标题反复。 | 目标资格、取消、回退、已知／未知状态 |
| 未保存输入、请先核对保存 | `app/LithicNavigation.kt` 两个 `AlertDialog` | 层、态、主 | 语义和按钮保留，防误离开及防重复写入优先。 | 项目及订阅两个 `*NavigationGuardTest` |
| 照片不可用和全图 | `feature/media/CoverPreviews.kt` 解释 `AlertDialog` 和只读放大 `Dialog` | 层、态、照片全图背景 | 无图、读中、不可用有区别。保持认证读取、放大关闭、回退和替换读入隔离。 | `CoverPreviewsTest` |
| 照片选择及确认 | `feature/media/PhotoLiteCoverPanel.kt`、`PhotoLiteCapability.kt`；详情回调及预览 `AlertDialog` | 层、主、态；选择器系统主题 | 普通 Debug 关闭照片写入，既有 Release 与 opt-in 条件见 build.gradle.kts。此次保留预览、移除确认、失败语义与开关。 | 静态开关核对，真实 Photo Picker `NOT_RUN` |
| 通用口径、趋势口径、完整金额 | `core/design/C5Information.kt`、`feature/review/TotalCostTrendScreen.kt`、`core/design/MoneyMetric.kt` | 层、面、图 | 简短标题；完整金额含单位、实际观测窗口和正确的定义保留。 | 长金额、200%、可访问描述和关闭 |

第一阶段结果为源码盘点。验收以生产编译、AndroidTest 编译、主题 token／文案定向测试和后续真实界面证据分别记录。主题在弹层、导航和主要页面的真实呈现，以及大字、触控、系统 SAF 和 TalkBack 均仍取决于设备验证。

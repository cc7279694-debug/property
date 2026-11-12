# LITHIC V3 用户操作旅程

源码基线 `codex/lithic-ui-p2-home@f38cb97aec3559f79c70c2069c322e4ab67f45c1`。以下是生产 Compose、Nav3 和写入边界的源码追踪。端侧证据沿用 [P2-A1-R1 限定复验](../../evidence/LITHIC-UI-P2-A1-R1.md) 的原范围。本轮没有运行 Android 或访问账本。

## 阅读导航

「现在 / 回看 / 设置」使用底部导航，进入详情和表单后使用页面返回及系统返回。详情保留来源，例如首页、当前列表、搜索、历史、趋势、类别或年度回顾。以下各流程中的“返回来源”指保留当前来源链，搜索详情可回到搜索。现有路由资格及恢复期间的导航锁继续约束所有入口。[LithicRoutes 返回路径](../../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt#L161)、[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L268)。

## A · 录入长期项目

1. **现在 → 新增记录 → 长期项目**，或从现在的长期项目「全部」进入列表，再点「新增长期项目」。两处打开同一个 `ProjectEditorScreen`。提交前返回由进入路径决定，草稿按 J 处理。
2. 核对名称、开始拥有日期、初始实际投入及初始实际付款日期，选择项目类别，需要时补充备注。开始拥有日与实际付款日分开，初始实付可为 0 元，空白仍需要填。首次保存原子提交项目与初始投入事实。
3. 校验失败时保留输入。成功进入新项目详情，从详情返回原入口对应的首页或项目列表。未确认按 K 处理。后续可从详情「编辑项目资料」修改名称、开始拥有日期、类别及备注，纠正原初始金额另走事件入口。以旧换新关联日期有额外保护。

[HomeShell 新增菜单](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L151)、[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L631)、[LithicRoutes 返回路径](../../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt#L161)、[ProjectEditorScreen 字段](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectEditorScreen.kt#L103)、[ProjectViewModel](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt#L480)、[ProjectWriteRepository 创建事务](../../../app/src/main/java/com/cooper/lithic/data/repository/ProjectWriteRepository.kt#L276)。

## B · 查看项目详情并记录投入

1. 从首页档案行、长期项目列表或其他已有来源进入项目详情，核对名称、状态和金额历史。点击「记一笔」→「记录投入」。底部菜单只选择方向。
2. 在「记录投入或收回」中核对记录对象及「投入」，填写实际金额、真实付款日和可选说明。点「保存金额记录」提交一条事实，返回原项目详情。日期为记录时区中的真实发生日，不能填写未来实付日期。
3. 成功后在详情核对新金额事件与成本。已结束项目仍可补录实际发生的尾款，持有天数保持冻结。若录入有误，从金额事件进入「纠正这条记录」。结束关联款项或换新事实有对应保护。未确认按 K 处理。

[ProjectDetailScreen 金额记录和操作菜单](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt#L187)、[MoneyEventEditor](../../../app/src/main/java/com/cooper/lithic/feature/project/MoneyEventEditor.kt#L39)、[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L555)、[ProjectViewModel](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt#L649)。

## C · 记录项目收回

从同一详情的「记一笔」选择「记录收回」。使用 B 的金额编辑器，但先确认方向为「收回」，填已到账的金额和日期。输入值使用非负人民币金额，由方向表达其在净投入中的作用。点「保存金额记录」后返回详情，核对事件方向与发生日。取消和未确认分别走 J、K。换新非现金折抵有单独事实类型和交易路径，现金收回入口保留实际到账语义。

[ProjectDetailScreen 投入和收回入口](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt#L234)、[MoneyEventEditor 方向和金额](../../../app/src/main/java/com/cooper/lithic/feature/project/MoneyEventEditor.kt#L58)、[ProjectViewModel](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt#L480)、[ProjectWriteRepository 结束关联事实校验](../../../app/src/main/java/com/cooper/lithic/data/repository/ProjectWriteRepository.kt#L50)。

## D · 结束持有及更正结束日期

1. 项目详情「更多项目操作」→「结束项目」。核对结束日期与真实原因，可勾选「同时记录实际收回」，填已到账的金额、日期及可选说明。「保存结束记录」确认结束事实及可选关联收回。结束按事实影响当前列表和持有计时，历史仍可查。
2. 从原详情或「回看 → 历史档案 → 已结束项目」进入对应项目，在菜单选择「修正结束记录」。核对原日期及原因后提交。普通关联收回存在时修正同一条记录。没有关联收回时补录实际退款走 C。若原项目参与以旧换新，受保护日期和折抵事实不可单独改写。
3. 结束及纠正后回到原项目详情。保留与「删除项目」的视觉和确认区别。后者永久移除项目及其金额记录，不能被包装成结束流程的快捷操作。

[ProjectDetailScreen 危险操作菜单](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt#L100)、[EndProjectDialog](../../../app/src/main/java/com/cooper/lithic/feature/project/EndProjectDialog.kt#L38)、[ProjectWriteRepository 结束及原事实比对](../../../app/src/main/java/com/cooper/lithic/data/repository/ProjectWriteRepository.kt#L50)、[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L643)。

## E · 新增订阅并记录付款

1. **现在 → 新增记录 → 订阅服务**，或从订阅服务「全部」进入列表，再点「新增订阅」。填服务名称、类别、备注及首笔实际付款。核对金额大于 0、月/季/年周期、真实付款日、实际覆盖起点、到期边界及提醒到期日。免费期不产生付款，预填也不代表付款已确认。
2. 确认后订阅身份与首笔付款在同一事务中保存，回读校验通过后进入订阅详情。提交前返回到启动入口。提交失败或未确认走 K。
3. 详情区分历史平均日成本、当前标准日均价、实际有效覆盖与折算付费时长。已付未来期到实际生效日才切换当前套餐。提醒日期是独立事实，自动提醒当前标记延期至 V1.1。

[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L660)、[SubscriptionEditorScreen 实付与日期](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionEditorScreen.kt#L70)、[SubscriptionViewModel](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt#L170)、[SubscriptionRepository 创建事务](../../../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionRepository.kt#L241)。

## F · 续费、暂停、恢复和停止订阅

| 意图 | 页面和确认 | 结果与风险 |
| --- | --- | --- |
| 续费 | 订阅详情 →「记录真实续费」→ 核对实付、周期和四日期 → 确认。 | 只提交本笔真实付款。前次金额和周期是参考，未来付费期在实际生效时才影响当前套餐。 |
| 暂停 / 恢复 | 详情 →「订阅管理」→「暂停」或「恢复」→ 核对状态生效日后确认。 | 只改变砺石记录及统计，保留付款历史。恢复或重新开通缺少可用的已付款覆盖时，表单转为核对并记录真实付款。 |
| 停止 / 重新开通 | 「订阅管理」→「停止记录」→ 选「到期后停止」或「提前停止」→ 确认。已停止时可从历史详情选择「重新开通」。 | 停止确认后的生效日期固定，付款修正不会自动移动它。停止事实生效后归入历史。暂停、到期未停止仍在订阅列表。停止或暂停不会替服务商取消自动续费或扣费。 |
| 更正原事实 | 详情的状态行「修正这条状态事实」，管理面板「修正停止计划」或「取消尚未生效的停止计划」，付款历史「付款操作」。 | 定向修改原状态或付款，保留对应身份。删除错误付款有独立确认且不可撤回，只影响该笔及相关统计。最后一笔付款不能单独删除，删除付款也不代表退款。 |

各表单先读取原资料。成功且回读通过后进入原订阅详情。退出与不确定结果走 J、K。生产路由中的订阅维护是这些事实入口，V3 页面设计不引入一个额外的通用订阅编辑能力。

[SubscriptionDetailScreen 管理与续费](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt#L187)、[RenewalEditorScreen](../../../app/src/main/java/com/cooper/lithic/feature/subscription/RenewalEditorScreen.kt#L46)、[SubscriptionStateDialog](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionStateDialog.kt#L44)、[SubscriptionViewModel](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt#L401)、[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L675)。

## G · 回看历史与成本变化

1. 在「回看」查总成本摘要，进入总成本趋势，或从历史档案选择「已结束项目」或「已停止订阅」再点档案。项目详情的「查看成本历史」与订阅详情的「查看价格历史」是对应单条时间序列入口，返回对应详情。
2. 总趋势可选观察日，查看当天计算值、项目和订阅分项、原事件并回到记录。也可进入类别分析、年度回顾、长期复盘后点相关记录。项目日均成本、订阅历史平均日成本、当前贡献及年度实际付款保留各自口径。所有这些浏览动作只读取。
3. 区分结果为零、该日无记录、当前无有效贡献、数据未读到及上次读取结果。历史仅包含已结束项目及已停止订阅。趋势保留日期、数值与文字/事件列表的关联。年度项目投入、收回、净投入及订阅实付不拼成一个年度支出值。

[ReviewShell 历史和分析入口](../../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt#L165)、[TotalCostTrendScreen 日期与事件](../../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt#L185)、[AnnualReviewScreen 年度口径](../../../app/src/main/java/com/cooper/lithic/feature/review/AnnualReviewScreen.kt#L60)、[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L461)、[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L600)。

## H · 搜索项目和订阅

从「现在」的搜索入口输入名称，跨当前与历史匹配项目及订阅。结果保留类型、状态及其对应的业务日期。点条目进入真实详情，返回原搜索。输入为空、已确认无匹配、尚未取得结果及查询失败分别呈现。更换查询时旧请求失效，重试仍核对当前查询。若从搜索详情启动编辑，也继续经过同一返回与未保存/未确认保护。

[SearchScreen 输入和结果](../../../app/src/main/java/com/cooper/lithic/feature/home/SearchScreen.kt#L38)、[SearchController 请求身份](../../../app/src/main/java/com/cooper/lithic/feature/home/SearchController.kt#L34)、[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L541)、[LithicRoutes 搜索来源](../../../app/src/main/java/com/cooper/lithic/app/LithicRoutes.kt#L303)。

## I · 备份、检查与覆盖恢复

1. **设置 → 导出完整备份 → Android 文件保存位置**。用户选址后开始导出校验，验证完整写入后才给出成功。取消、失败、流程中断和可能的残留文件各有反馈。导出文件未加密，包含业务记录、已登记图片及业务设置。本机外观偏好独立于备份。
2. **设置 → 检查备份文件 → Android 文件选择器**。检查完成可看到完整性结论。此操作不会恢复或改变当前账本。检查的 `VERIFIED` 与随后恢复流程的准备、覆盖和成功结果分别呈现。
3. **设置 → 选择完整备份 → 校验 → 核对备份**。同一设置页显示创建时间、记录时区、当前与备份内容数量对照。预览可取消，保留当前账本。如需保存当前内容，先取消再导出。继续需主动点「确认覆盖恢复」。当前项目、订阅、图片与备份内业务设置全部以备份为准，不合并。
4. 预览 token 只使用一次。确认后重新核对选定备份与当前数据资格，切换期间锁住导航和其他备份操作。确认成功显示重新读取后的记录。明确失败且当前记录已保留时可重新选择。结果未知时重新打开应用核对本地记录，勿清除应用数据。系统文件选择取消没有覆盖效果。

[SettingsShell 三条入口](../../../app/src/main/java/com/cooper/lithic/feature/settings/SettingsShell.kt#L78)、[BackupDocumentPicker](../../../app/src/main/java/com/cooper/lithic/feature/settings/BackupDocumentPicker.kt#L14)、[BackupExportController](../../../app/src/main/java/com/cooper/lithic/feature/settings/BackupExportController.kt#L75)、[BackupInspectionController](../../../app/src/main/java/com/cooper/lithic/feature/settings/BackupInspectionController.kt#L87)、[RestorePreviewScreen](../../../app/src/main/java/com/cooper/lithic/feature/settings/RestorePreviewScreen.kt#L29)、[BackupRestoreController](../../../app/src/main/java/com/cooper/lithic/feature/settings/BackupRestoreController.kt#L69)、[RestoreCoordinator 覆盖边界](../../../app/src/main/java/com/cooper/lithic/data/backup/RestoreCoordinator.kt#L80)。

## J · 取消未保存表单

正常返回和系统返回共用离开策略。无修改且无阻塞时返回原入口。已有未保存输入时，显示「放弃未保存修改？」。「继续编辑」保留输入，关闭确认框也停在当前表单。「放弃修改」确认后才丢弃草稿并导航。确认时会再核对是否进入待核对状态或恢复锁，`BLOCK_PENDING` 优先于放弃草稿，且取消本身无写入。当前详情和表单通常不展示底部导航，跨入口保护仍在中央导航决策中。

[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L268)、[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L714)、[SubscriptionNavigationPolicy 决策优先级](../../../app/src/main/java/com/cooper/lithic/app/SubscriptionNavigationPolicy.kt#L6)、[ProjectViewModel](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt#L447)、[SubscriptionViewModel](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt#L236)。

## K · 处理可能已写入但结果未确认

1. 用户确认前输入可编辑。提交时冻结原输入及提交身份。正在提交时阻止重复保存和离开。进程重建或收到无法对照身份的结果等情况，可以进入 `UNCONFIRMED`。相同的全局确认框提示先核对原表单。有可恢复的原表单路由时提供返回入口。
2. 当原表单允许「核对并重试」，必须使用同一冻结的提交和原记录身份。Repository 以事实核对、同代写入协调与原版本比对处理重复请求。有冲突时先读取当前事实，不覆盖新的状态。明确校验失败可按原提示修改。数据空间不足、损坏或结果未知分别展示，不推断写入失败。
3. 订阅已确认付款但详情回读失败时，使用「重新读取已保存结果」等对应只读入口，不能再记录一笔付款。项目的已确认结果后续详情读取失败同样是重读。导航留在安全路径，直至可按原策略核对或返回。只读证据确认当前不存在时，按现有反馈表达范围，不把缺失说成某笔删除的历史证明。
4. 恢复和照片操作的不确定结果仍使用各自原有保护。恢复未知需重新打开应用核对。照片结果未知不提供原金额表单的重试入口。应用从 `StorageGate` 冷启动读失败时保留原错误出口，不借首页重试绕开账本启动保护。

[ProjectViewModel](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt#L306)、[ProjectViewModel](../../../app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt#L533)、[SubscriptionViewModel](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt#L170)、[SubscriptionViewModel](../../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt#L284)、[SubscriptionRepository 重复付款保护](../../../app/src/main/java/com/cooper/lithic/data/repository/SubscriptionRepository.kt#L278)、[LocalWriteCoordinator](../../../app/src/main/java/com/cooper/lithic/data/repository/LocalWriteCoordinator.kt#L11)、[LithicNavigation](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L730)、[LithicApp 冷启动状态](../../../app/src/main/java/com/cooper/lithic/app/LithicApp.kt#L54)。

## V3 回归证据

以后使用合成记录按 A–K 逐条演练，记录入口、返回来源、明确确认前后记录数、金额和状态。J 要确认两种离开选择分别保留或丢弃草稿且无写入。K 要验证同一提交最多产生一组事实、冲突不会覆盖、已确认后的重读无重复写入。I 要分别记录仅检查、预览取消、明确覆盖及结果未知的行为。页面文案、按键位置和 Android 手势的真实表现留待后续端侧核验。

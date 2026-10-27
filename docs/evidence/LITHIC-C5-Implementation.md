# C5 全产品UI实施与唯一体验候选

**C5 IMPLEMENTED — READY_FOR_INDEPENDENT_PRODUCT_REVIEW**

**ANDROID_VISUAL_AND_MOTION_PENDING / MEDIA_DISPLAY_PENDING**

基线/设计独立冻结：`5838e863ededec3ecd569c5332b04ab621a918c4`；分支`codex/lithic-v1-product-completion`。本轮依据[已冻结C5规范](../design/LITHIC_C5_GLOBAL_UX_SPEC.md)实施；32组是源码桌面清单，不是32页Android运行PASS。没有HTML、设备操作或原生截图。设计冻结与生产产品独立验收分开。

## 六个实施单元

| 单元 | 提交 | 范围 |
|---|---|---|
| U1 | `533f5ca5f093262fdb5b314a106a5def6375879d` | 三类Nav3转场、Scene不透明背景、趋势引用连续性；构造器消歧在U3收口。 |
| U2 | `4057e2115af7ef64cc54c851bde6d79e0b171a9c` | 唯一MoneyMetric内核、角色/宽度策略及全额查看；父组测宽在U3接入。 |
| U3 | `5994f99509f1586cb3c12aba80a667ddc3bec64c` | 浏览/身份/详情、长金额父重排、语义与导航编译兼容收口。 |
| U4 | `cb90674e3479c2a5fed3b2040e7e43eb8999b0be` | 全量表单、交易五值及原提交/危险操作保护。 |
| U5 | `0081a9b19713092d60f3ca60628cee1b04647b20` | 完整/摘要趋势、单项图及全部真实分析。 |
| U6 | 本报告所在提交 | 设置/恢复/提醒/反馈、Preview8版本与最终集成证据。 |

普通前进、返回、预测返回均显式使用`EnterTransition.None/ExitTransition.None`；保留原NavEntry副本身份/metadata/装饰器，每个Scene补不透明背景。原backstack、来源、requestLeave、dirty/pending/discard和恢复守卫未改。趋势按需首次创建，离开保留已有StateFlow引用；原暂停/请求票据/换代清理不改。真实预测手势、快速切换、帧率与流畅度仍**NOT_RUN**。

## 32组生产页面

30组实施或由共享组件生效；永久删除与存储门禁两组保留已经正确的完整安全结构。无记录/加载/失败/旧结果、真实0/负值/不可计算继续分开；未知类别实际渲染通用类型矢量，不取名称首字、不推断品牌。

| 组 | 生产落点 | 桌面状态及变化 |
|---|---|---|
| 01 现在 | [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) | IMPLEMENTED (DESKTOP)：深绿主卡、真实数量卡和唯一FAB保持；成本/预览/类别均用内核 |
| 02 项目列表 | [ProjectListScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectListScreen.kt) | IMPLEMENTED (DESKTOP)：紧凑身份、状态、持有时间及准确日均；列表说明按需 |
| 03 项目历史 | [ProjectHistoryScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectHistoryScreen.kt) | IMPLEMENTED (DESKTOP)：冻结持有时间和截至观察日结果；原历史范围/分页 |
| 04 订阅列表 | [SubscriptionListScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionListScreen.kt) | IMPLEMENTED (DESKTOP)：最近真实付款/覆盖事实与历史均价；原分组/排序 |
| 05 停止订阅 | [SubscriptionHistoryScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionHistoryScreen.kt) | IMPLEMENTED (DESKTOP)：停止边界与折算时长；暂停/过期不误归档 |
| 06 名称搜索 | [SearchScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/home/SearchScreen.kt) | IMPLEMENTED (DESKTOP)：只呈现名称/类型/状态；查询归属与旧结果提示保持 |
| 07 项目详情/金额历史 | [ProjectDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt) | IMPLEMENTED (DESKTOP)：日均/持有→三项金额→事件；更多与纠错资格保留 |
| 08 订阅详情/付款历史 | [SubscriptionDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt) | IMPLEMENTED (DESKTOP)：当前套餐/日期/预付→历史均价/折算→付款操作菜单 |
| 09 新建/编辑项目 | [ProjectEditorScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectEditorScreen.kt) | IMPLEMENTED (DESKTOP)：渐进输入、隐藏错误定位、固定保存与原operationId |
| 10 记投入/收回 | [MoneyEventEditor.kt](../../app/src/main/java/com/cooper/lithic/feature/project/MoneyEventEditor.kt) | IMPLEMENTED (DESKTOP)：方向/金额/日期与固定保存；未确认保护 |
| 11 金额纠错 | [MoneyEventCorrectionScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/MoneyEventCorrectionScreen.kt) | IMPLEMENTED (DESKTOP)：原事实与可纠正字段；交易金额/日期锁定 |
| 12 结束/修正结束 | [EndProjectDialog.kt](../../app/src/main/java/com/cooper/lithic/feature/project/EndProjectDialog.kt) | IMPLEMENTED (DESKTOP)：结束≠删除、实际到账与锁定；原重读/未知 |
| 13 永久删除 | [DeleteProjectDialog.kt](../../app/src/main/java/com/cooper/lithic/feature/project/DeleteProjectDialog.kt) | UNCHANGED_WITH_REASON：原高风险后果、冲突/未知及二次确认已正确，保留 |
| 14 以旧换新 | [TradeInEditorScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/TradeInEditorScreen.kt) | IMPLEMENTED (DESKTOP)：五项DETAIL核对、三笔事实和非现金限制 |
| 15 替换链/纵向对比/建链/解除 | [ReplacementComparisonScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ReplacementComparisonScreen.kt) | IMPLEMENTED (DESKTOP)：关系身份/持有/日均，原预览/分页/确认 |
| 16 新订阅/首次付款 | [SubscriptionEditorScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionEditorScreen.kt) | IMPLEMENTED (DESKTOP)：金额/周期/四日期；首次真实付款确认保持 |
| 17 续费/付款纠错/同身份重开 | [RenewalEditorScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/RenewalEditorScreen.kt) | IMPLEMENTED (DESKTOP)：共享表单覆盖续费/纠错/同身份重开及原阶段 |
| 18 暂停/恢复/停止/状态纠正/取消未来停止/删除错误付款 | [SubscriptionStateDialog.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionStateDialog.kt) | IMPLEMENTED (DESKTOP)：原动作、两种停止/服务商扣费/删付款非退款保持 |
| 19 项目成本/订阅价格历史 | [ProjectCostChart.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectCostChart.kt) | IMPLEMENTED (DESKTOP)：共享图页迁移；真实曲线、日期、点/事件与分页 |
| 20 回看首页 | [ReviewShell.kt](../../app/src/main/java/com/cooper/lithic/feature/review/ReviewShell.kt) | IMPLEMENTED (DESKTOP)：实际趋势摘要与轻分组；无数据不造图 |
| 21 完整总成本趋势 | [TotalCostTrendScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt) | IMPLEMENTED (DESKTOP)：统一图标返回；选日总额→真实曲线→贡献→日期操作 |
| 22 长期复盘三榜 | [LongTermReviewScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/LongTermReviewScreen.kt) | IMPLEMENTED (DESKTOP)：三榜分类型；Row日均/历史均价与真实折算时长 |
| 23 类别分析 | [CategoryAnalysisScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/category/CategoryAnalysisScreen.kt) | IMPLEMENTED (DESKTOP)：当前贡献与历史金额分区，真实0/负值保持 |
| 24 年度回顾 | [AnnualReviewScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/AnnualReviewScreen.kt) | IMPLEMENTED (DESKTOP)：真实资金年份、分类型与共享正负轴保持 |
| 25 报表公共展示 | [ReportPresentation.kt](../../app/src/main/java/com/cooper/lithic/feature/review/ReportPresentation.kt) | IMPLEMENTED (DESKTOP)：人民币金额/年度柱统一；不推断纯现金 |
| 26 设置/导出/校验/恢复中 | [SettingsShell.kt](../../app/src/main/java/com/cooper/lithic/feature/settings/SettingsShell.kt) | IMPLEMENTED (DESKTOP)：分组菜单和真实导出/恢复状态；风险与未知持续可见 |
| 27 覆盖恢复预览 | [RestorePreviewScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/settings/RestorePreviewScreen.kt) | IMPLEMENTED (DESKTOP)：十项原计数键对照、完整覆盖与!busy确认 |
| 28 提醒设置 | [NotificationSettingsScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/settings/NotificationSettingsScreen.kt) | IMPLEMENTED (DESKTOP)：权限/渠道真实状态就地，普通调度说明按需 |
| 29 类别管理/改名/合并预览 | [CategoryManagementScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/category/CategoryManagementScreen.kt) | IMPLEMENTED (DESKTOP)：原影响范围、源删除及事务未知/冲突反馈保持 |
| 30 计算说明/关于 | [ProductInformationScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/settings/ProductInformationScreen.kt) | IMPLEMENTED (DESKTOP)：真实运行版本/包身份，完整计算说明仍可阅读 |
| 31 里程碑来源/无效来源 | [MilestoneSourcesScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/reminder/MilestoneSourcesScreen.kt) | IMPLEMENTED (DESKTOP)：有效/无效来源、点击时再核验和安全返回保持 |
| 32 本地存储门禁 | [LithicApp.kt](../../app/src/main/java/com/cooper/lithic/app/LithicApp.kt) | UNCHANGED_WITH_REASON：原真实读取/失败/退出及不建空账本已正确，保留 |

## M01～M24与七处输入

金额只走`MoneyMetric`；`AmountText`为兼容薄适配。原`AmountFormatter`对象逐字一致，BigDecimal/舍入与亚分钱规则未变。一个不可拆行的数值＋单位Text组合、一个完整读数语义；默认日均`¥0.67/天`、CNY`¥2.00`。父组先按真实字宽纵排，再在HERO38/32/28、DETAIL28/24、CARD24/20、ROW18候选内适配，仍不足时整组横向查看与48dp全额/复制入口；完整弹窗使用自身主题前景色。未修改系统fontScale。

| ID | 生产来源 | 已迁移角色/范围 |
|---|---|---|
| M01 | [EntryCard.kt](../../app/src/main/java/com/cooper/lithic/core/design/EntryCard.kt) | EntryCard：ROW，四列表共用 |
| M02 | [CostChartPage.kt](../../app/src/main/java/com/cooper/lithic/core/design/CostChartPage.kt) | CostChartPage：DETAIL，准确选日 |
| M03 | [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) | 首页主值：HERO |
| M04 | [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) | 首页类型小计：CARD（按需） |
| M05 | [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) | 首页预览：ROW，保留指标身份 |
| M06 | [HomeShell.kt](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) | 类别贡献：ROW |
| M07 | [ProjectDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt) | 项目主值：DETAIL |
| M08 | [ProjectDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt) | 项目三合计：CARD/CNY |
| M09 | [ProjectDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt) | 项目事件：ROW/CNY |
| M10 | [ReplacementComparisonScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/ReplacementComparisonScreen.kt) | 替换对照：ROW |
| M11 | [TradeInEditorScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/project/TradeInEditorScreen.kt) | 交易五值：DETAIL/CNY |
| M12 | [LongTermReviewScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/LongTermReviewScreen.kt) | 项目榜单：ROW |
| M13 | [LongTermReviewScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/LongTermReviewScreen.kt) | 订阅历史均价：ROW |
| M14 | [ReportPresentation.kt](../../app/src/main/java/com/cooper/lithic/feature/review/ReportPresentation.kt) | ReportMoney：CARD/CNY |
| M15 | [TotalCostTrendScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt) | 总趋势摘要：HERO/DETAIL |
| M16 | [TotalCostTrendScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt) | 项目贡献：ROW |
| M17 | [TotalCostTrendScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt) | 订阅贡献：ROW |
| M18 | [TotalCostTrendScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/review/TotalCostTrendScreen.kt) | 完整趋势总额：DETAIL；两贡献ROW |
| M19 | [CategoryAnalysisScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/category/CategoryAnalysisScreen.kt) | 类别贡献：DETAIL |
| M20 | [SubscriptionDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt) | 当前套餐：DETAIL/CNY |
| M21 | [SubscriptionDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt) | 当前标准价：ROW |
| M22 | [SubscriptionDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt) | 累计确认付款：CARD/CNY |
| M23 | [SubscriptionDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt) | 历史均价：CARD |
| M24 | [SubscriptionDetailScreen.kt](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt) | 付款历史：ROW/CNY |

七处`AmountField`：项目初始金额、投入/收回、纠错、结束收回、交易现金、交易折抵、共享订阅付款字段。原String/回调逐项与基线一致，`FormFields`/MoneyParser未改；共享付款覆盖首次/续费/纠错/同身份重开。隐藏错误定位、operationId、canEdit、IN_FLIGHT/UNCONFIRMED/COMMITTED及原重读资格保留。详见[源码边界与输入对照](c5-20261027-093354/source-boundaries.json)。

兼容映射：`home-total-rate`保持并含完整单位；旧独立`home-total-unit`并入该读数及其TextLayoutResult。`amount-full-value`含完整金额和单位，旧`amount-full-unit`不再单独渲染。其余关键数值tag保留；付款纠错/删除原tag与ID保留，先经`subscription-renewal-actions-<id>`菜单到达。相关呈现断言同步保留精确值与负号，不删除业务断言。

## 信息密度与数据安全

完整趋势重排为所选日期/准确总额→真实曲线→两类贡献→紧凑日期操作；点、事件、非现金标记、50条扩展及准确日期仍在，技术口径按需。订阅先看当前套餐/真实日期/未来预付，再看历史均价与折算时长。普通说明进就地信息入口；完整计算说明/关于仍可阅读。错误、未知、未加密备份、覆盖恢复、残留文件和危险后果留在原操作位置。

Room5、backup4、domain/data、Repository/DAO、Controller/ViewModel、Owner/generation、Token/原子切换、原Manifest/依赖/Theme/媒体与提醒协议无修改。恢复仍先完整校验、预览、明确确认，十项原摘要计数与互斥/!busy保护不变。年度按真实资金年份分类型，没有跨项目/订阅的非法年度总支出。

## 实际桌面结果与失败历史

| 检查 | 实际结果/原始证据 |
|---|---|
| 27类定向JVM | **188 PASS，0 FAIL/ERROR/SKIP**；[摘要](c5-20261027-093354/jvm-summary.json)、[逐类XML目录](c5-20261027-093354/jvm-results) |
| 首轮产品编译 | exit1：NavEntry主/副本构造器类型歧义；[原命令与输出](c5-20261027-093354/debug-quality-initial/command.json) |
| 第二轮产品/JVM/Debug | 产品与188 JVM、Debug完成；后续AndroidTest两处无效import导致整体exit1；[原命令](c5-20261027-093354/debug-quality-final/command.json) |
| 最小修正后的AndroidTest编译/Lint/默认身份 | exit0；[续跑命令](c5-20261027-093354/debug-quality-completion/command.json)。只删除无效import，未改断言，未再跑188 JVM。 |
| 普通Debug Lint/证书/默认身份 | **0错误、21警告**；[Lint XML](c5-20261027-093354/debug-lint.xml)、[默认身份与签名摘要](c5-20261027-093354/debug-quality-summary.json) |
| Private Trial构建/Lint | exit0，**0错误、13警告**；[命令](c5-20261027-093354/private-trial-build-lint/command.json)、[Lint XML](c5-20261027-093354/preview8-lint.xml) |
| APK Manifest/签名/DEX/ZIP/旧产物 | [实际SDK审计](c5-20261027-093354/artifact-audit.json)；无测试宿主、JUnit/Runner/合成夹具进入用户APK |
| Android/Compose实际运行 | **NOT_RUN，0次设备操作**；宽度360/400dp、大字/IME、触控、TalkBack、三类导航与性能均未获真实运行证据。 |

stdout/stderr原字节、退出码、长度、SHA随各command.json同目录保存。首两次编译失败保留，不冒充业务RED或被消除的历史FAIL；没有重跑M22/C3或不相关历史大套件。普通Debug/Release默认包名与code17保持；仅显式Private Trial递增版本。

## 唯一Preview8候选

- 包名：`com.cooper.lithic.preview.debug`；版本`0.1.0-preview.8-debug` / code`24`；显示名“砺石·体验版”。
- 本机绝对路径（APK不入Git）：`E:\CODEX\.codex\worktrees\lithic-m13\砺石新APP\deliveries\lithic-preview-0.1.0-preview.8\Lithic-0.1.0-preview.8-debug.apk`。
- 大小：**15,797,555字节**；SHA-256：`09ffc3c0a39d055e6fb89b4a738fe1a4ba579687c86c880f864fb428cd056bbd`。
- 原Debug证书SHA-256：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；不是正式Release签名。
- 构建输入Git app树：`8877e8195ab673a9e8c19d06e54657026273fae7`；最终提交的`HEAD:app`必须相同。正常`LithicApplication/MainActivity`接入当前32组生产功能；Manifest/DEX为静态接线证据，不能代替实际视觉。
- 无INTERNET、sharedUserId、Instrumentation、testOnly或未经授权新增权限；独立Provider authority。交付副本与构建字节一致；Preview5/6/7和两份C3旧APK校验不变。

## 待独立审查与风险

Android视觉/字体/触控/IME/导航动态与真实升级均**PENDING/NOT_RUN**；本轮手机实际fontScale/density为UNKNOWN。真实物品封面仍MEDIA_DISPLAY_PENDING。保留M10三个JPEG历史FAIL、M20额外平台FAIL/第二次收口UNKNOWN、M22原BLOCKED及T06/T03/IME未完成、C3调查CLOSED且运行根因/现场UNKNOWN、M13稳定签名升级与G1～G4原状态。没有VISUAL_ACCEPTED、正式V1.0或M23准入结论。

用户以后可自愿按现在/项目订阅/表单交易/回看完整趋势/设置恢复逐页体验，以及普通前进、来源返回、预测返回、快速切换。已有体验记录先保护并手动备份；同包覆盖升级尚未真机验证，不以卸载清数据作为默认处理。候选仅供个人试用，不作重要记录唯一副本。本轮不安装、不清理、不处理C3或任何既有手机现场，不合并main；推送后停止等待ChatGPT独立审查。

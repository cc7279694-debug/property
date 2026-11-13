# 详情配对设计审查

**DESIGN_REVIEW_ONLY — NOT_PRODUCTION_UI**

## 独立审查前的检查

| 决策 | 核对结果与取舍 |
| --- | --- |
| 项目首屏 | 4:3 Fit 画框最大 240×180dp，占 824dp 示意视口约 22%。照片、名称、持有状态及日均金额可连续扫读。无图缩成类型位，读取中保留稳定图框，关联不可读提供状态文字。正常纯白首屏含累计金额组，模拟 200% 的金额记录通过滚动可达。 |
| 订阅首屏 | 服务身份下方突出历史平均日成本，累计实付和折算天数集中在一处，当前套餐另列当前标准日均价与边界。选择保留完整指标名称，将计算口径收进说明入口。已付款未来期仍在历史按真实付款日期排列，独立标记尚未生效。 |
| 事实和操作 | 项目投入、收回、净投入使用三个读数，折抵场景纵排保留限定。金额正负和真零由符号与实际数值表达。订阅管理说明保留服务商操作边界；结束、删除、暂停、停止和草稿退出分别画出后果。主操作和正文占不同布局区域。 |
| 对照旧稿 | 保留旧档案候选的名称与封面关系、区间语义和状态保护。根据新首页强化深蓝黑操作色、限制照片占幅、收敛局部表面。项目和订阅的操作按钮使用同一视觉语言，先读什么由各自的成本模型决定。 |
| 状态和主题 | 同一记录复用三套主题。封面、旧快照与错误采用状态文字。已停止无续费主按钮，空历史仅为现有 UI 的合成防御分支。完全读取失败时保留返回和重读，无虚构金额、记录或日期。 |

[首屏配对](boards/renders/pair.png) · [历史与纠错](boards/renders/history.png) · [关键确认](boards/renders/guards.png)

## 基线与源码

审阅 V3-01 的[策略](../lithic-ui-v3/V3_DESIGN_STRATEGY.md)、[计划](../lithic-ui-v3/V3_REDESIGN_PLAN.md)、[盘点](../lithic-ui-v3/APP_INVENTORY.md)、[视觉审查](../lithic-ui-v3/UI_AUDIT.md)、[旅程](../lithic-ui-v3/USER_JOURNEYS.md)，以及 V3-02 的[设计系统](../lithic-ui-v3-02/DESIGN_SYSTEM.md)、[画板索引](../lithic-ui-v3-02/HIGH_FIDELITY_SCREENS.md)和[无障碍方案](../lithic-ui-v3-02/ACCESSIBILITY_AND_RESPONSIVE.md)。旧画板对照涵盖[项目概念](../lithic-ui-v3-02/boards/renders/compare-project.png)、[订阅概念](../lithic-ui-v3-02/boards/renders/compare-subscription.png)、[项目主题](../lithic-ui-v3-02/boards/renders/themes-project.png)、[订阅主题](../lithic-ui-v3-02/boards/renders/themes-subscription.png)、[尺寸](../lithic-ui-v3-02/boards/renders/responsive.png)、[身份状态](../lithic-ui-v3-02/boards/renders/identity-states.png)及[写入保护](../lithic-ui-v3-02/boards/renders/write-guards.png)。

项目详情核对 `ProjectDetailScreen`、`ProjectDetailPresentation`、`ProjectCostChart`、`ProjectHistoryScreen`、`ProjectUiModels`、`ProjectViewModel`、`ProjectRepository` 与金额事件查询。订阅详情核对 `SubscriptionDetailScreen`、`SubscriptionPriceChart`、`SubscriptionHistoryScreen`、`SubscriptionUiModels`、`SubscriptionStateDialog`、`SubscriptionViewModel`、`SubscriptionRepository` 与分页。核对 `LithicNavigation`、`LithicRoutes`、金额及封面组件，按原资格呈现操作。视觉候选以任务和事实层级组织，Android 样式以 48dp 目标、系统返回和明确模态后果为准。[项目源码索引](PROJECT_DETAIL_DESIGN.md) · [订阅源码索引](SUBSCRIPTION_DETAIL_DESIGN.md) · [路由及保护](BUSINESS_FACTS_AND_GUARDS.md)

## 验证与审查门禁

- Chromium 导出 10 组静态画板。核对两种详情在 360／400／720 逻辑宽度 × 100%／模拟 200% 的 12 组组合。浏览器测量未见横向页面溢出或底部操作覆盖正文，示意按钮实际盒至少 48 CSS px。极端主金额按数字组换行，符号、分位和单位完整可读。长金额行及完整金额查看与复制沿用规格中的兜底。
- 检查实际 PNG 首屏、历史、三主题、大字体、读取状态和确认文案。画板中的按钮是静态控件标本。源文件可以切换画板和滚动正文。
- Android Compose、真实系统 200% 字体、TalkBack、焦点、实际命中区、Insets／IME、系统及预测返回和账本读写均为 **NOT_RUN**。现有首页 43 项 Android 证据仅用于首页。本轮提交静态设计与当前状态，Android 生产实现须在独立审查及用户确认之后另行开始。

[布局检查](boards/layout-checks.json) · [无障碍与原生复验计划](ACCESSIBILITY_RESPONSIVE.md) · [已冻结首页验收证据](../../evidence/LITHIC-Home-Final-Polish-09-Retry.md)

**LITHIC_UI_V3_DETAIL_DESIGN_01 — READY_FOR_DESIGN_REVIEW**

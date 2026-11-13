# 项目与订阅详情共用组件

`DESIGN_REVIEW_ONLY — NOT_PRODUCTION_UI`

以已冻结首页的白底、深蓝黑强调、黑石品牌和原生图标为参照。详情在相同设计语言下改用连续阅读区。项目照片与物品身份成组，订阅把首屏空间留给套餐和付款时间。保留现有三主题、中文系统字体和次级路由。[纯白项目首页](../../evidence/lithic-home-final-polish-09-retry/projects-pure-white.png) · [纯白订阅首页](../../evidence/lithic-home-final-polish-09-retry/subscriptions-pure-white.png)

| 共用部分 | 候选规则 | 生产复用点 |
| --- | --- | --- |
| 次级顶栏 | 返回与更多独立 48dp 目标，中心留给详情标题。长名称放在内容区并自然换行。系统返回与顶部返回沿用同一来源链。 | `C2PageHeader`、`C2Icon`、现有导航回调和来源语义。 |
| 身份与封面 | 项目使用最大 240×180dp 的 4:3 展示框，窄窗口内宽度随可用空间缩小，`Fit` 完整呈现原图。订阅用 56dp 小型身份位，有已登记封面时用原读链，无封面用通用订阅标识。读取中和已关联不可用使用现有状态语义。 | `RecordCover`、`C2RecordIdentity`、`C2StatusChip`。保持 owner、引用和读取预算。 |
| 主金额 | 34sp 候选，观察日期紧跟本项金额，标题用指标完整名称。保留人民币、正负号、真实零值、未知值和现有完整金额查看／复制。 | `MoneyMetric` 与现有格式化；`C5InformationButton` 容纳计算口径。 |
| 事实与历史 | 页面边距从 20dp 起，组距 16–24dp；标题 18sp，事实与记录正文 14–16sp。用细分隔线组织，金额沿列对齐。事实行、行操作、状态标签的角色各自清楚。 | `C2Section`、原分页列表、各自的事实模型。 |
| 主动作与确认 | 主操作在独立测量的底部区域，内容滚动至操作区上方。文字按钮允许换行并随文字增高。管理保留原菜单／面板，破坏性动作经现有确认页。 | `LithicButton`、`C2FormScaffold`、已有 BottomSheet、返回与提交守卫。 |

来源：[页面与表单容器](../../../app/src/main/java/com/cooper/lithic/core/design/C2Page.kt) · [原生图标](../../../app/src/main/java/com/cooper/lithic/core/design/C2Icon.kt) · [身份](../../../app/src/main/java/com/cooper/lithic/core/design/C2RecordIdentity.kt) · [金额](../../../app/src/main/java/com/cooper/lithic/core/design/MoneyMetric.kt) · [封面](../../../app/src/main/java/com/cooper/lithic/feature/media/CoverPreviews.kt)

## 主题与顺序

- 纯白由白底、黑灰文字及细分隔线建立层级，深蓝黑集中在主按钮和交互提示。
- 岩白雾蓝用暖白画布、克制的雾蓝动作强调，连续内容保持清楚。
- 深色黑灰用较亮的局部表面、柔和分隔与高对比文字，照片保持本色。状态、危险和未知仍有文字或原生图标可辨。[现有角色](../../../app/src/main/java/com/cooper/lithic/core/design/LithicVisuals.kt)

720dp 采用居中阅读栏，宽度候选上限 560dp，保留原来的单目的地返回链。窄宽或大字号时，事实与历史金额从左右排布重排为垂直分组，状态行按可用空间换行。阅读焦点为顶栏、身份、主金额、事实、记录、底部操作。金额单位和数值以一个可读单元组织，媒体只有独立状态或操作需要焦点，装饰分隔线退出语义树。静态画板用于检查顺序和文字可读性，实际 200% 字体、TalkBack、焦点、触控与 Insets 待 Android 实测。

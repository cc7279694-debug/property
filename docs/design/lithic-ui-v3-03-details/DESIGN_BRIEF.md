# 砺石 UI-V3 · 项目与订阅详情

**DESIGN_REVIEW_ONLY — NOT_PRODUCTION_UI**

以 `434127f3199f2e9883a97401d97d1f965ef1c62f` 为固定起点。已冻结首页确立纯白、黑石标识、深蓝黑操作色和双列卡片。详情延续字体、颜色、图标与按钮，正文以连续分区呈现档案和历史。三套主题同一读序、同一组事实。独立设计分支 `codex/lithic-ui-v3-detail-design-01`。[冻结项目首页](../../evidence/lithic-home-final-polish-09-retry/projects-pure-white.png) · [冻结订阅首页](../../evidence/lithic-home-final-polish-09-retry/subscriptions-pure-white.png) · [首页 200% 项目](../../evidence/lithic-home-final-polish-09-retry/projects-font-200-cards.png) · [首页 200% 订阅](../../evidence/lithic-home-final-polish-09-retry/subscriptions-font-200-cards.png)

## 推荐方案

项目页先让用户认出物品，最多 240×180dp 的照片紧邻名称、类别、状态和持有时间，紧接一个日均读数。订阅页以 56dp 的已有封面或通用标识建立身份，历史平均日成本领读，当前套餐保持独立分区，实际付款日期与生效覆盖分别呈现。两页都用单列阅读、细分隔与原生返回。底部仅留常用主操作，已有低频操作按资格进入更多、现有表单或记录行。

与 V3-02 两个候选相比，项目保留档案识别优先，同时限制照片高度以让日均成本进入正常和模拟大字首屏。订阅省去大图，把空间给历史均值、当前套餐和下一期的真实时间关系。两页复用视觉规范，指标名称和事实模型各自保留。页内选用少量底色处理状态、封面占位和模态，无装饰图表。[项目详情规格](PROJECT_DETAIL_DESIGN.md) · [订阅详情规格](SUBSCRIPTION_DETAIL_DESIGN.md) · [共用组件](SHARED_COMPONENTS.md) · [事实和保护](BUSINESS_FACTS_AND_GUARDS.md)

## 画板

| 画板 | 场景 |
| --- | --- |
| [首页风格下的详情配对](boards/renders/pair.png) | 纯白项目和订阅首屏 |
| [项目三主题](boards/renders/project-themes.png) · [订阅三主题](boards/renders/subscription-themes.png) | 同内容的纯白、岩白雾蓝、深色黑灰 |
| [历史与纠错](boards/renders/history.png) | 实际发生顺序、非现金折抵、已付尚未生效、付款及覆盖日期、付款操作。分页规则见订阅规格 |
| [项目状态](boards/renders/project-states.png) · [订阅状态](boards/renders/subscription-states.png) | 封面、读错、旧快照、空历史、结束和停止、长名和极端值 |
| [360dp 大字](boards/renders/font-200.png) · [400dp](boards/renders/width-400.png) · [720dp](boards/renders/wide-720.png) | 标准与 200% 字体、向下滚动及宽窗 |
| [确认与退出保护](boards/renders/guards.png) | 结束、永久删除、放弃未保存、暂停、停止 |
| [静态可滚动画板](boards/index.html) · [评审及检查结果](DESIGN_REVIEW.md) | 从本地浏览器切换画板。GitHub 直接打开 PNG。 |

示例沿用首页截图的 2026-11-13 观察日和代表性读数，明细使用新建的合成设计样本。相机封面为本轮生成的合成素材，保持原始 4:3。订阅有图示例复用 V3-02 中 Hexafluoride 的《Kindle 4th Generation》照片，授权为 CC BY-SA 4.0，通用订阅图标不代表服务商。原黑石图标用于画板页眉，App 次级页把顶栏空间留给返回、页标题和更多。[相机素材](boards/assets/synthetic-camera.png) · [原品牌资源](../../../app/src/main/res/drawable-nodpi/ic_launcher_scene_p3.png) · [既有阅读器素材来源和授权](../lithic-ui-v3-02/HIGH_FIDELITY_SCREENS.md#图片来源)

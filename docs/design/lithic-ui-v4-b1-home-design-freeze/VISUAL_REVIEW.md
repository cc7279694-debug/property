# 砺石 V4 首页视觉评审

`READY_FOR_INDEPENDENT_DESIGN_REVIEW` · 设计冻结候选，等待独立审查和用户确认。

[首页规范](HOME_DESIGN_FREEZE.md) · [可交互样稿](prototype.html) · [浏览器原始结果](browser-verification.json)

## 与现在的首页对照

生产对照取自 `b7ca3ea742fcf960fe36eae94a06d4a418a5682a`。更早的首页验收提交 `434127f3` 之后，`d0c69897` 已在首页和搜索统一主题并减少文案。本稿以包含该改动的生产版本为准。相较 V4 设计基线 `af612160efca7b08dc3f071ab7fa3bc7cbada32e`，当前总览、封面读取和主题源码一致，并包含 `98153ef3` 修正的悬浮底栏圆形按压反馈。[生产首页](../../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt) · [底栏](../../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt#L846) · [用户体验的前序交付](../../evidence/LITHIC-UI-V4-ICON-NAV-PRESS-REPAIR-02.md)

| 位置 | 当前生产首页 | B1 候选及取舍 |
| --- | --- | --- |
| 品牌栏 | 黑石标记、字标和两个动作 | 维持原标记和入口，将标记、文字、搜索及新增压到同一中心线。搜索和新增都保有独立的 48dp 目标。 |
| 总览 | 一个带描边的总览面板，面板内含两个指标、数量和构成 | 两个数字直接排在页面上，以字阶区分总投入和日均成本。底部一道分隔线引向记录区。保持含历史的两类数量和「成本构成」，准确日期及完整口径从信息入口查看。 |
| 项目／订阅 | 双列预览，项目和订阅各自切换，有独立「全部」入口 | 标题和对应「全部」先对齐，分段轨变轻，选中项有独立表面。保留双列、原排序和最多两条。状态及类别直接排字，照片、物品名和金额优先。 |
| 添加和导航 | 顶部加号、底部添加、三枚悬浮图标 | 两处新增继续打开同一个菜单。添加、导航与系统区各有空间；记录超出首屏时，可以滚到最后一张完整可见。保留用户在小米14确认的已交付底栏形态，当前画板只呈现首页。 |

首屏去掉总览外卡、独立观察日期行和金额旁重复的日单位。金额的人民币／天语义仍在读屏描述和完整口径中；上次快照的日期在说明里标为「上次结果日期」。正常态无需额外说明，失败、旧快照、封面失效及重试仍有独立表达。纯白用浅灰分隔，石雾蓝用蓝灰页面衬白卡，深色用黑底与深灰卡建立层级。遮挡与留白以完整手机画板和 200% 滚动位置对照评估。

## 从 TaoSheng v0.4.0 迁移的规则

已检查 v0.4.0 对应提交 `ddd3306a5273ffc5c8ef5c48135edf5ce712e9e1` 的七份原始资料。以下是用于 B1 的对应关系。

| 参考证据 | 首页中落实的规则 |
| --- | --- |
| TodayScreen | 16dp 页面边距、12dp 双列间隔、约 20dp 区域节奏。状态块按条件出现，正常态把面积交给记录。 [源文件](https://github.com/0xCoach/TaoSheng/blob/ddd3306a5273ffc5c8ef5c48135edf5ce712e9e1/app/src/main/java/app/resound/ui/today/TodayScreen.kt#L239-L296) |
| OverviewCard | 概览统领少数数字，标签、主值与补充量各有字阶。当前候选采用同一阅读节奏，将指标落在页面背景上。 [源文件](https://github.com/0xCoach/TaoSheng/blob/ddd3306a5273ffc5c8ef5c48135edf5ce712e9e1/app/src/main/java/app/resound/ui/today/OverviewCard.kt#L99-L154) |
| Theme 与窗口 XML | 借鉴有限字阶与低对比表面边界。窗口主题用于核对系统边缘的职责。颜色仍来自砺石现有三套主题，亮暗层级在同一构图里对照。 [字体与色彩角色](https://github.com/0xCoach/TaoSheng/blob/ddd3306a5273ffc5c8ef5c48135edf5ce712e9e1/app/src/main/java/app/resound/ui/theme/Theme.kt#L53-L119) · [窗口主题](https://github.com/0xCoach/TaoSheng/blob/ddd3306a5273ffc5c8ef5c48135edf5ce712e9e1/app/src/main/res/values/themes.xml) |
| AppShell | 用独立底板和选中形状拉开层级。保留砺石既已确认的图标底栏，分段轨采用相同的简洁选中表达。 [原导航](https://github.com/0xCoach/TaoSheng/blob/ddd3306a5273ffc5c8ef5c48135edf5ce712e9e1/app/src/main/java/app/resound/ui/AppShell.kt#L65-L167) |
| README 与 179 交付说明 | 一条记录对应一张可打开的卡片。正文优先给身份和当前用户任务，元数据贴着记录，详情由已有入口承担。 [项目说明](https://github.com/0xCoach/TaoSheng/blob/ddd3306a5273ffc5c8ef5c48135edf5ce712e9e1/README.md) · [179 交付说明](https://github.com/0xCoach/TaoSheng/blob/ddd3306a5273ffc5c8ef5c48135edf5ce712e9e1/docs/%E4%BB%BB%E5%8A%A1%E5%8D%A1/179-%E4%BA%A4%E4%BB%98%E8%AF%B4%E6%98%8E.md) |

## 画板、样本和边界

| 证据 | 内容 |
| --- | --- |
| [纯白](home-white.png) · [岩白雾蓝](home-stone-mist.png) · [深色黑灰](home-charcoal.png) | 同一账本、相同图片与金额，360dp、100% 字体。 |
| [记录卡](home-record-cards.png) | 项目、订阅各有一张有图和一张无图示例。订阅主视图只有一条真实模拟记录，仍占一列。 |
| [关键状态](home-states.png) | 七种状态，项目和订阅预览各有自己的失败提示及重试。 |
| [三种宽度](home-responsive.png) · [模拟 200%](home-font-scale-200.png) | 320 / 360 / 400dp。大字画板左侧看首屏总览，中、右分别滚到项目和订阅，展示长名称的自然换行和底部操作。 |

合成观察日为 2026-11-13。长期项目投入为 ¥1,400.00 + ¥199.20 = ¥1,599.20。相机收回 ¥200.00，影响该项目日均而不扣减总投入。截点有效项目贡献为 ¥1,200.00 ÷ 121 天与 ¥199.20 ÷ 60 天，当前订阅贡献为 ¥59.40 ÷ 30 天，页面总日均合并后显示 ¥15.22。订阅卡自身另用历史实付 ¥297.60 ÷ 120 天，显示 ¥2.48。只有历史的样本保留总投入和 2 项目、1 订阅，当前日均显示真实的 ¥0.00。无快照错误的金额和数量均用 `—`。

相机使用仓库已有的合成封面，黑石来自砺石既有品牌资源。阅读器照片为 Hexafluoride 的《Kindle 4th Generation》，授权 CC BY-SA 4.0。画板使用 4:3 视口裁切，并注明来源和许可。记录研究中把同一授权照片用在「订阅有封面」示例，表示用户已关联的封面，并非服务图标。 [合成相机说明](../lithic-ui-v3-03-details/DESIGN_BRIEF.md) · [照片原作](https://commons.wikimedia.org/wiki/File:Kindle_4th_Generation.jpg) · [许可](https://creativecommons.org/licenses/by-sa/4.0/)

## 验证结果和待核对项

- **浏览器设计验证 PASS。** Chrome 157.0.8103.52、Playwright 1.65.1，共 36 个布局组合、7 个状态场景、5 组入口交互，浏览器错误 0。核对两类互斥选中、对应「全部」、卡片详情、两个新增入口、搜索、成本构成及口径说明。预览对话框使用合成数据，没有账本写入。
- **布局检查 PASS。** 浏览器逐一检查 4:3 封面、金额与长名称、可点击区域、双列、横向溢出、底栏圆形选中和正文滚动区几何。18 个正常字号组合的样本卡片首屏可见。18 个大字组合都可滚动至最后一张卡完整可见。七张输出 PNG 的尺寸和 SHA-256 记录在 [原始结果](browser-verification.json)。
- **Android `NOT_RUN`。** 原生裁切、不同系统 Insets、手势及三键导航、IME、返回、Android 系统字体放大、实际读屏和 Android 颜色合成仍需 B2/B3 核对。当前页使用 CSS 模拟 200% 字号。网页菜单和分段仅用于评审交互，设置与回看也只展示入口。
- **B2 实施核对。** 日期从主画面移入口径说明后，确认原 `home-cutoff-date` 的可访问语义及旧快照归属；检查极长金额仍可经现有 `MoneyMetric` 完整查看，并与原测试中的首页入口、封面读取预算、分段排序和返回守卫对照。较宽窗口继续沿用生产的居中限宽。

本地从仓库打开 `prototype.html` 即可切换样稿和画板。交互页默认采用纯白 360dp，参数示例 `?board=lab&theme=stone-mist&width=320&font=2&tab=subscriptions`。复验脚本接收本机的 `playwright-core` 模块路径和 Chrome 路径作为两个参数，设置 `LITHIC_EXPORT_BOARDS=1` 可同时重新导出 PNG。[验证脚本](verify-preview.cjs)

本次独立分支基于远端导航修复提交。开始时复核远端 `main` 为 `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`，Release 为 `87d423e7f21722eff851cac445b9ab798ab6996d`，V4 设计为 `af612160efca7b08dc3f071ab7fa3bc7cbada32e`。交付差异仅在当前设计目录，范围覆盖设计、画板和验证材料。

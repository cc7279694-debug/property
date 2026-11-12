# 高保真画板索引

PNG 适合直接在 GitHub 打开与放大。也可从本地打开 [画板源文件](boards/index.html) 浏览全部场景，画板内的按钮为视觉样式标本，外层导航可以切换画板。所有页面使用合成事实，浏览器渲染不代表 Android 实测。

## 概念与三主题

| 画板 | 覆盖 |
| --- | --- |
| [首页 A/B](boards/renders/compare-home.png) | 身份优先与总账优先。有封面、无封面、项目、订阅、当前成本和独立新增区 |
| [项目详情 A/B](boards/renders/compare-project.png) | 同一阅读器、完整封面、持有事实、成本、金额事件及记一笔 |
| [订阅详情 A/B](boards/renders/compare-subscription.png) | 同一服务，当前覆盖、月费、标准日均、历史日均与实际付款 |
| [三主题首页](boards/renders/themes-home.png) · [三主题项目](boards/renders/themes-project.png) · [三主题订阅](boards/renders/themes-subscription.png) | 岩白雾蓝、纯白、深色黑灰。对应三联画保持文案、数据、操作和布局一致 |
| [320/360/400 与模拟大字](boards/renders/responsive.png) · [720 宽窗口](boards/renders/responsive-wide.png) | 字体可重排的阅读列、操作区测量、宽窗居中列 |

## 推荐方向的全应用展开

| 画板 | 主要画面 |
| --- | --- |
| [项目档案](boards/renders/project-archive.png) | 项目列表、新增长期项目、编辑资料 |
| [项目操作](boards/renders/project-actions.png) | 记录投入、记录收回、结束持有、删除项目确认 |
| [订阅与付款](boards/renders/subscription-payments.png) | 订阅列表、首次付费订阅、记录续费、修正实际付款 |
| [订阅状态](boards/renders/subscription-lifecycle.png) | 暂停、恢复、停止。实际覆盖和固定停止规则按各动作保留 |
| [搜索与回看](boards/renders/search-and-review.png) | 搜索结果、回看首页、总成本趋势、类别分析 |
| [分析与设置](boards/renders/review-and-settings.png) | 年度回顾、长期复盘、设置、类别管理 |
| [备份与恢复](boards/renders/backup-and-restore.png) | 完整备份导出、检查文件、恢复预览、覆盖确认区。后两幅是同一设置流程的两个视口 |
| [封面与大字滚动](boards/renders/identity-states.png) | 首条项目无封面、封面暂不可用、2× 字号滚至订阅区 |
| [空、历史与真实零](boards/renders/ledger-states.png) | 区分无记录、无当前贡献、实际零投入 |
| [读取状态](boards/renders/read-states.png) | 正在读取、失败保留旧快照、失败且无快照 |
| [写入保护](boards/renders/write-guards.png) | 未保存输入、待核对写入、已确认写入后重读 |

以旧换新、修正结束、单项图表、照片操作和其他低频分支继续遵守 [页面盘点](../lithic-ui-v3/APP_INVENTORY.md) 与 [A–K 旅程](../lithic-ui-v3/USER_JOURNEYS.md)。本轮高保真以主任务和高风险变体为代表，不能用未画出的操作推断其被移除。

## 合成账本

观察日为 2026-11-12，示例按 Asia/Shanghai 记录日期。金额单位为人民币。

| 记录 | 事实 | 观察日读数 |
| --- | --- | --- |
| 电子书阅读器 Paper · 数码 | 7 月 16 日开始及实际投入 ¥1,200.00，10 月 20 日实际收回 ¥100.00 | 持有 120 天，净投入 ¥1,100.00，项目日均 ¥9.17/天 |
| 帆布通勤包 · 日用 | 8 月 15 日开始及实际投入 ¥360.00，无封面 | 持有 90 天，项目日均 ¥4.00/天 |
| 阅读会员 · 阅读 | 11 月 1 日实际月付 ¥30.00 并生效，覆盖 11 月 1–30 日，到期边界 12 月 1 日 | 折算付费 30 天，历史平均 ¥1.00/天 |
| 音乐会员 · 影音 | 同月实际月付 ¥15.00 并生效，覆盖 11 月 1–30 日 | 折算付费 30 天，历史平均 ¥0.50/天 |

当前项目贡献约 ¥13.17/天，订阅贡献 ¥1.50/天，未舍入汇总后展示 ¥14.67/天。截至 11 月 11 日的旧结果按同一账本少一天计算为 ¥14.79/天。备份预览使用独立的 10 月 31 日备份样本。表单中的草稿及明确标注的状态示例不回写基准账本。只有历史与真实零值使用各自的状态夹具。

## 图片来源

阅读器封面复用 R2.1 中的同一 JPEG，来自 Hexafluoride 的 Kindle 4th Generation 作品，按 CC BY-SA 4.0 提供。R2.1 文件等比缩小，本轮画板用 Fit 完整放入，未改写图像。包含该照片的画板保留同样的素材署名。[本轮素材](boards/assets/reader.jpg) · [R2.1 素材与出处记录](https://github.com/cc7279694-debug/property/blob/1b688bc1756dc3f42fcd3a3c0e57732d56023f34/docs/design/lithic-ui-r2-1-personal-archive/REVIEW.md) · [原照片](https://commons.wikimedia.org/wiki/File:Kindle_4th_Generation.jpg) · [授权](https://creativecommons.org/licenses/by-sa/4.0/)

品牌图标引用仓库现有黑石资源。无图条目使用类型图标。没有接触私人账本或照片。[原品牌资源](../../../app/src/main/res/drawable-nodpi/ic_launcher_scene_p3.png)

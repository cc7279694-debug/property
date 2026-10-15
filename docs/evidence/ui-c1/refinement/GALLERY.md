# UI-C1 阶段1视觉精修复审图册

本图册收录 **27 张未经编辑的 Android 原始截图**：19 张来自已安装 Debug APK 的真实 MainActivity，6 张来自隔离合成数据测试宿主，2 张来自隔离空账本测试宿主。来源分组不可互换；这些截图提供复审证据，**不代表用户已批准视觉效果**。

逐图来源、SHA-256、设备设置及 APK 绑定见 [截图清单](screenshot-manifest.json)。构建、测试、已知失败和未执行边界见 [本轮验证报告](../../UI-C1-refinement-verification.md)。

## A. MainActivity：真实现有 QA 账本，正常字体六图

本组使用设备原有 QA 记录，保持原名称、排序、状态、金额和封面。首页的 4 条长期项目、4 条订阅是实际已录入身份数量，包含历史与已停止记录；没有展示或编造总日均成本。蓝色详情封面为既有 QA 图片，未替换为合成展示封面。

| 页面 | 浅色 | 深色 |
| --- | --- | --- |
| 现在 | ![MainActivity 现在，浅色，正常字体](screenshots/01-main-now-light.png) | ![MainActivity 现在，深色，正常字体](screenshots/20-main-now-dark-normal.png) |
| 长期项目列表 | ![MainActivity 长期项目列表，浅色，稳定页面](screenshots/21-main-project-list-light-stable.png) | ![MainActivity 长期项目列表，深色](screenshots/05-main-project-list-dark.png) |
| 长期项目详情 | ![MainActivity 长期项目详情，浅色，稳定页面](screenshots/22-main-project-detail-light-stable.png) | ![MainActivity 长期项目详情，深色](screenshots/04-main-project-detail-dark.png) |

## B. 隔离测试宿主：合成物品与真实领域引擎六图

**本组六图不是 MainActivity，也不是正式用户记录。** 测试创建独立 `noBackup` 子目录及独立 Room generation，经现有 ProjectRepository / SubscriptionRepository 写入 3 个项目、2 项订阅，再通过真实 LithicNavigation 和冻结领域引擎展示。观察日固定为 `2026-10-15`，记录时区为 `Asia/Shanghai`。

| 合成项目 | 开始拥有 | 初始投入 | 领域引擎持有天数 | 展示日均成本 |
| --- | --- | --- | --- | --- |
| 旅行相机 | 2024-10-01 | ¥4,999.00 | 745 天 | ¥6.71／天 |
| 通勤耳机 | 2024-12-01 | ¥999.00 | 684 天 | ¥1.46／天 |
| 轻薄笔记本 | 2025-01-01 | ¥7,999.00 | 653 天 | ¥12.25／天 |

两项合成订阅为音乐年卡（¥198，365 标准天，覆盖 `[2026-01-01, 2027-01-01)`）及云盘空间（¥18，30 标准天，覆盖 `[2026-10-01, 2026-11-01)`）。付款日均为覆盖开始日。测试未手工指定日均结果，精确计算值和规范化后封面 SHA-256 见 [合成数据来源](fixture-source.json)。

封面由 `androidTest` 在运行时使用 Canvas 绘制原创几何物品插画，经现有 ContentResolver → MediaRepository 导入及规范化。它们不是参考图照片或私人图片，不作为正式资源、正式记录打包。测试核对截图前后已提交封面字节及合成事实一致；这不表示输入 PNG 跳过了首次规范化。

应用默认 owner 在常规启动时可以打开活动库；**fixture 不使用活动账本，不向其写入合成记录，不切换其指针**。测试结束清理自身独立数据根及自己创建的输入 URI。原有账本、封面及设备设置的实际前后核对见 [数据与设备保留证据](device-preservation.json)。

| 页面 | 浅色：隔离合成宿主 | 深色：隔离合成宿主 |
| --- | --- | --- |
| 现在 | ![隔离合成测试宿主，现在，浅色](screenshots/synthetic-now-light.png) | ![隔离合成测试宿主，现在，深色](screenshots/synthetic-now-dark.png) |
| 长期项目列表 | ![隔离合成测试宿主，长期项目列表，浅色](screenshots/synthetic-project-list-light.png) | ![隔离合成测试宿主，长期项目列表，深色](screenshots/synthetic-project-list-dark.png) |
| 长期项目详情 | ![隔离合成测试宿主，轻薄笔记本详情，浅色](screenshots/synthetic-project-detail-light.png) | ![隔离合成测试宿主，轻薄笔记本详情，深色](screenshots/synthetic-project-detail-dark.png) |

## C. 隔离空 Room：首页双主题空态

本组通过另一独立空 Room generation 显示真实零记录状态，**没有清空或覆盖现有 QA 账本**。截图来自测试宿主；仅突出一次首次新增入口，不以多个重复空态填满首屏。

| 浅色：隔离空账本宿主 | 深色：隔离空账本宿主 |
| --- | --- |
| ![隔离空账本测试宿主，首页浅色](screenshots/empty-home-light.png) | ![隔离空账本测试宿主，首页深色](screenshots/empty-home-dark.png) |

## D. MainActivity：360dp／系统 200% 字体压力截图

以下截图来自真实 MainActivity；设备为 1080×1920，密度覆盖为 480 dpi，对应 360dp 宽，系统字体为 200%。未通过缩小系统字体达成布局。新增入口占据页头独立空间，不悬浮覆盖卡片。首屏、滚动内容及菜单分别取证；**不承诺所有事实在一屏显示**。

| 检查内容 | 深色 | 浅色 |
| --- | --- | --- |
| 首页顶部：真实双数量、包含历史说明及新增入口 | ![MainActivity，360dp 200% 字体，深色首页顶部](screenshots/07-main-now-360dp-200-dark.png) | ![MainActivity，360dp 200% 字体，浅色首页顶部](screenshots/16-main-now-360dp-200-light.png) |
| 首页滚动：完整负日均金额与独立页头新增入口 | ![MainActivity，360dp 200% 字体，深色首页滚动及负金额](screenshots/08-main-now-360dp-200-dark-scrolled.png) | ![MainActivity，360dp 200% 字体，浅色首页滚动及负金额](screenshots/17-main-now-360dp-200-light-scrolled.png) |
| 详情顶部：短返回、更多入口、真实封面及项目身份；日均金额可在后续滚动位置查看 | ![MainActivity，360dp 200% 字体，深色详情顶部](screenshots/10-main-detail-360dp-200-dark.png) | ![MainActivity，360dp 200% 字体，浅色详情顶部](screenshots/12-main-detail-360dp-200-light.png) |
| 更多菜单：操作名称与布局 | ![MainActivity，360dp 200% 字体，深色详情更多菜单](screenshots/11-main-detail-more-360dp-200-dark.png) | ![MainActivity，360dp 200% 字体，浅色详情更多菜单](screenshots/13-main-detail-more-360dp-200-light.png) |

| 补充检查内容 | 实际截图 |
| --- | --- |
| 紧凑长期项目列表，保持真实排序及卡片内容 | ![MainActivity，360dp 200% 字体，深色长期项目列表](screenshots/09-main-list-360dp-200-dark.png) |
| 详情核心金额、持有天数及观察日所在滚动位置 | ![MainActivity，360dp 200% 字体，浅色详情核心事实](screenshots/14-main-detail-360dp-200-light-core.png) |
| 累计投入、累计收回、净投入三组完整金额取证 | ![MainActivity，360dp 200% 字体，浅色详情三组金额](screenshots/19-main-detail-360dp-200-light-all-amounts.png) |

这组截图证明指定视口内的重要内容和操作未被新增按钮覆盖。滚动列表在上下视口边界自然裁切其他行，不能据单张截图推断所有记录或操作同时可见；完整 TalkBack、真机及其他未执行边界按验证报告保留。

## E. 两张中间滚动视口：仅作补充

以下两张也属于上述 27 张原图。它们没有转场或悬浮新增遮挡，但**边缘标签或金额被当前滚动视口裁切，不作为全部标签完整同屏的证据**；完整核心事实及三金额请结合 14、19 查看。

- [15：详情金额与投入／收回操作的中间滚动视口](screenshots/15-main-detail-360dp-200-light-amounts.png)。顶部日均金额及底部累计投入标签部分在视口外。
- [18：财务事实的中间滚动视口](screenshots/18-main-detail-360dp-200-light-financial-facts.png)。三组金额可见，顶部累计投入标签部分在视口外，后续基本信息也未全部同屏。

## 证据绑定与排除项

MainActivity 图绑定截图时设备已安装的目标 Debug APK 字节。隔离宿主图使用同一目标 APK，并另行绑定独立 AndroidTest APK；后者包含测试宿主、Canvas 绘图及合成数据代码，不能只用目标 APK 的 SHA 代替测试宿主绑定。两个 APK 的 SHA、测试输入快照、逐图 SHA 和采集配置以 [截图清单](screenshot-manifest.json) 及 [验证报告](../../UI-C1-refinement-verification.md) 为准，APK 不作为公开仓库资源提交。

以下原图只作为诊断历史保存，**不计入本图册 27 张最终原图**：

- `diagnostics/disqualified-manual/` 中的 02、03 为导航转场／尚未稳定的画面，分别由 21、22 替代；06 采集与密度修改重叠，由 20 替代。原 SHA 和排除原因见 [排除登记](disqualified-manual.json)。
- `diagnostics/disqualified-anr-host/` 中的早期六张合成宿主图被 Digital Wellbeing 系统无响应弹窗遮挡，仅作诊断，已重新采集为 B 组最终六图。

本轮停在 **VISUAL_REVIEW_REQUIRED — UI-C1（视觉精修复审）**。Task6～Task8、M11 及视觉正式批准均未由本图册授予。

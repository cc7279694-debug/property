# C9 三主题与全局呈现实施计划

**Goal:** 同一套生产Compose支持深色黑灰、纯白、岩白雾蓝，主题可保存且不改变账本、草稿、来源与提交合同。

**Architecture:** 沿用LithicTheme、MoneyMetric和C2/C5共享组件；新增仅本机的外观偏好及状态控制器，在主进程校验后单实例创建，文件放noBackupFilesDir。外观通过CompositionLocal与现有Activity的重组传播，不增加导航或业务Owner。

**Tech Stack:** 现有Kotlin、Compose Material3、Coroutines、DataStore Preferences，无新依赖。

**Spec:** 用户转交的 `E:/CODEX/LITHIC_C9_Codex_完整执行任务书.md`（2026-10-29）；已完整读取。锁定基线`d1c752938792f0a88d19713f06d32c183696d34f`，本地/远端一致、工作区初始干净。

## Locked / Adjustable

锁定：Room5、backup4、领域/Repository、Owner/generation、恢复协议、C7导航来源与UNKNOWN保护；C6读取/预算/取消/换代与隔离实现；Logo、Preview10/code26与原证书。禁止设备、旧AVD、图片写入、网络或新安装包。

可调整：主题Token、系统栏图标、共享组件和页面布局、主题选择与独立本机偏好。固定三项，无跟随系统或动态配色；无旧选择默认岩白雾蓝。当前SettingsStore仅含时区与提醒项，源码无旧外观键，本轮**不虚构迁移**。未知值/读取失败不当成首次安装，显式选择落盘成功后才更新确认状态，失败保留原值。

## 三主题起点

| 角色 | 黑灰 charcoal | 纯白 pure_white | 岩白雾蓝 stone_mist |
|---|---|---|---|
| 背景/表面/分组 | #121212 / #1E1E1E / #292929 | #FFFFFF / #FFFFFF / #F5F5F5 | #F7F6F2 / #FFFFFF / #E8EEF2 |
| 主/次文字 | #F3F3F3 / #B2B2B2 | #171717 / #686868 | #29343E / #5E6B75 |
| 主操作底/字 | #E9E9E9 / #161616 | #232323 / #FFFFFF | #536F86 / #FFFFFF |
| 装饰线 | #343434 | #E6E6E6 | #DDE2E5 |

补齐Material表面/输入/选中/弹层及错误、警告、成功、图表角色；装饰线不作为唯一控件边界。正文目标4.5:1，主图线/控件目标3:1，按实际背景组合和透明度核对，禁用态另列。金额保留原Formatter、负号、精度和单次朗读。

## 实际文件职责

| 文件/区域 | 实施内容 |
|---|---|
| 新feature/appearance | LithicAppearance稳定标识、AppearanceUiState/Controller、独立DataStore及进程准入Provider；不改LithicApplication或业务设置存储。 |
| core/design/LithicTheme、颜色Token | 唯一三套Material主题及图表/反馈语义色，页面留白小屏16/其他24dp；保留系统字体缩放。 |
| MainActivity | 仅在正常入口取得主进程外观Provider；状态收集和主题/系统栏重组，不重建Activity、generation UI scope或业务VM。 |
| 新主题选择组件、SettingsShell | 外观/记录/提醒/备份/关于自然分组，三项同布局小预览、选中语义和读写反馈；不新增路由。 |
| C2Page、EntryCard、C2RecordIdentity、FormFields、UiFeedbackText、MoneyMetric外观 | 轻分隔、对齐、状态文字、表单/弹窗/禁用反馈一致；保留48dp、IME布局及全部业务回调。 |
| HomeShell、两类列表/历史/详情、SearchScreen | 去厚品牌摘要，真实总额/小计/数量与范围保留；身份/名称/状态/时间/金额自然行、大字重排；一次FAB底部避让。 |
| ReviewShell、TotalCostTrendScreen、两类单项图、CostChart/Page | 日期金额→真实图→贡献/工具；图例与线型/点形辅助；C5-R1状态和全历史入口不变。 |
| Annual/LongTermReview、ReportPresentation、CategoryAnalysis | 相同语义色、轻分组，保留真实事实与分类规则。 |
| 其他表单/确认/设置深页 | 经共享主题与组件适配并核查硬编码颜色，不改写入资格、输入String、operationId或风险确认。 |

## 同一阶段内部步骤

- [x] A 核对真实源码/旧偏好与边界，写本计划；无可访问的新录屏/概念图，不声称看过。
- [x] B 先写外观解析、失败与快速切换测试和可编译占位，实际RED后实现；主进程准入/单实例/独立存储。
- [x] C 公共Token/列表/输入/反馈及首页、两类详情、回看、设置、历史搜索与表单同步适配。
- [x] D 保留草稿/来源/未知写入和C7错误反馈；新增主题/布局Compose断言，真实运行NOT_RUN。
- [x] E 定向JVM、AndroidTest源码编译、Debug构建、Lint、对比度与冻结路径/Preview10实物保护；集中报告后提交推送并停止。

## 验证与证据边界

新逻辑先RED再GREEN，旧业务仅相关回归，不重跑M22大套件。核心视觉矩阵为三主题×首页/项目详情/订阅详情/回看/设置15位，另测长金额/长中文/360dp/2倍字体表单。仅用已有可用能力；当前无设备授权，原生截图、系统栏/IME/真实字体及动态运行均NOT_RUN。桌面示意如有必须标DESIGN_REFERENCE，不冒充生产画面；不重开C3或搭建截图系统。

集中报告 `docs/evidence/LITHIC-C9-Visual-Theme-V2.md` 记录实际结果、未验证项和历史门禁；不生成Preview11，不授予视觉/图片/发行通过。

## 实施校准

- 实际颜色以 `LithicVisuals.kt` 为准。纯白次字改为#5C5C5C、岩白雾蓝次字改为#4E5D67；边界/警告/错误/成功/图表均有命名角色。校准是为12%按压/焦点合成和明确禁用色保留对比，不改三主题方向。黑灰主题普通操作保持中性，不用岩绿替代。
- 新外观文件使用现有Preferences DataStore 1.2.1的公开OkioStorage重载及公开PreferencesSerializer，仍为protobuf `theme_id`，不加依赖/自写解析器、不改业务SettingsStore。Windows JVM模拟SDK0的旧rename分支失败及后续调查完整保留；这不是已证实的API26+产品缺陷。损坏文件不自动重置，持续提示读取/保存失败。
- 按当前源码未找到旧持久主题键；旧系统深浅色只有临时主题判断，未伪造迁移数据。明确历史预览的Boolean主题重载仅供兼容测试，不形成用户第四项。
- 系统栏使用显式SystemBarStyle和图标亮暗；Theme/LocalProvider不key业务子树、不recreate Activity。此为源码接线，真实冷启动闪变、系统栏、IME和快速切换体验仍需授权后运行。

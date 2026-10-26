# UI-C2 A段集中样板

> 当前视觉结论：原A段经独立审查为 **CHANGES REQUIRED**。本次仅提交A-R1精修候选，见末节；仍为PROTOTYPE_ONLY / APP_VISUAL_PENDING，未获得用户视觉认可。以下原A段记录保留，原版图/报告可按锁定提交读取。

**VISUAL_SAMPLE_READY — UI-C2**

**PROTOTYPE_ONLY / APP_VISUAL_PENDING**

来源`9de00d3889180cb1d3c078ade543c75100a4196d`，当前产品分支`codex/lithic-v1-product-completion`。四页生产Compose已落入产品源码；本轮停止供一次集中视觉反馈，未推广其他页面、未生成新用户安装包。

## 看样板

- [离线交互HTML](../design/ui-c2/UI_C2_A_Prototype.html)：四页×浅深8框，以及无记录、长负金额、大字3框；可演示新增/记一笔面板、更多、备注/日期及分项展开。全部为合成设计数据。
- [浅深总览](../design/ui-c2/PROTOTYPE_ONLY-overview.png) · [代表状态](../design/ui-c2/PROTOTYPE_ONLY-states.png)。使用本机已有浏览器离线渲染，已实际打开查看。**不是Compose渲染、MainActivity或APK截图**，大字是浏览器排版模拟，不是系统200%字体/IME/TalkBack通过。
- [原始字节、尺寸和SHA](ui-c2-20261026-031540/prototype-final-observation.json)。无现成Compose Desktop/Layoutlib/Paparazzi/Roborazzi配置，未安装渲染框架、重开旧AVD或新建Web产品。

## 四页变化及真实接线

| 页面 | 实施 | 保留 |
|---|---|---|
| 现在 | 总日均主摘要/两小计；搜索+新增；两类各最多2条真实预览与全部；空库唯一新增；类别按需 | 原当前列表窗口/排序/VM/generation，含历史总数单独标注；0/负值/无当前贡献/读取失败/旧结果分开 |
| 项目详情 | 紧凑身份/日均和持有主指标/次级金额/完整记录线；唯一记一笔+更多 | 编辑、投入/收回、结束/修正、纠错、替换/以旧换新、成本历史及危险删除原保护全部仍在 |
| 新增项目 | 主要字段优先、付款/备注渐进、隐藏错误自动展开定位、底部保存及IME安全区 | 原VM默认/输入/operationId、0与空金额区分、未确认提交/重新读取；没有创建前媒体写入 |
| 回看 | 现有趋势为主、准确读数/日期/分项与精确选日；分析和历史轻入口 | 原图表/负值/峰谷/事件分页及完整趋势路由；仅进入回看按需读，离开暂停，不在现在预算20年 |

主要差异：`LithicTheme/C2Icon`、`HomeShell`、`ProjectDetailScreen`、`ProjectEditorScreen`、`ReviewShell/TotalCostTrendScreen`、`LithicNavigation/LithicRoutes`及小型纯呈现helper。三主入口改真实矢量图标+文字，详情/表单以原来源返回；HOME/REVIEW_HOME仅为UI来源，原requestLeave、dirty/pending/discard和恢复门禁不变。首页成本说明亦返回现在。只选择性复用旧UI主题/12矢量，未merge/cherry-pick旧分支或重画Launcher。

首页复用原列表VM，仍读取既有50条窗口及已加载页，仅显示前2条，非最近活动/全量数量。回看复用既有全事实计算，不谎称固定50条开销。原GenerationUiScope继续隔离旧代模型，新增迟到呈现断言仅编译。

## 真实验证

| 检查 | 实际结果 | 原始证据 |
|---|---|---|
| 定向JVM | 75唯一方法PASS：26新增、49相关回归；不重跑历史大套件 | [逐方法](ui-c2-20261026-031540/jvm-approved-summary.json)、[XML](ui-c2-20261026-031540/jvm-approved/) |
| Debug / AndroidTest源码编译 | exit0；18新增Android/Compose方法编译，运行全部NOT_RUN | [最后命令及退出](ui-c2-20261026-031540/quality-approved-input/command.json)、[stdout](ui-c2-20261026-031540/quality-approved-input/stdout)、[18方法范围](ui-c2-20261026-031540/android-compile-only.json) |
| Lint | 0错误、17警告 | [实际XML](ui-c2-20261026-031540/lint-approved.xml) |
| 包/签名/冻结边界 | 原Debug证书、默认身份/版本配置不变；Room5/backup4/data/domain/写入/Owner/媒体协议不改；旧preview18字节未变 | [最终静态核验](ui-c2-20261026-031540/artifact-approved.json)、[测试输入绑定](ui-c2-20261026-031540/input-binding.json) |

新增呈现断言先于实现；没有故意破坏既有正确行为或冒称业务RED→GREEN。首轮AndroidTest编译因错误导入`assertDoesNotExist`失败（节点断言仍保留），[首次stderr](ui-c2-20261026-031540/quality-initial/stderr)永久保留。移除错误导入后通过；新增图标参数顺序Lint警告亦修正，不改业务断言。历史测试/失败不改写。

## 限制与停止

- **封面接入PENDING**：当前产品基线没有页面可用的MediaRepository/封面管理入口，仅有媒体元数据与备份能力。无图用图标；有coverAssetId但没有受校验读取时明确“封面暂不可用”。仅提供纯UI插槽，没有裸路径/主线程解码/新缓存，也没有把旧M10媒体实现合并。不能宣称已有图片显示或封面管理已完成。
- Android视觉、点击/返回/面板/IME/系统字体/辅助技术/换代实际运行均NOT_RUN。18方法覆盖面板关闭零动作、原回调、错误展开、未知提交、旧代迟到隔离等；编译不充当设备证据。旧旅程部分定位仍基于旧布局，后续需在授权范围内调整定位，不改业务断言；未重开M22-T06/T03/IME循环。
- 全局主题传递到其他页面，但未进行B段布局重做。当前APK仅普通Debug检查产物，未构建新preview/Release/M22验证APK；没有安装/升级或新证书。实际App视觉与未来最终MainActivity/APK逐页一致性仍PENDING。
- M10三JPEG历史FAIL、M20平台FAIL/UNKNOWN、M22 BLOCKED、M13-T06与G1～G4及全部历史NOTES/NOT_RUN保持。没有ADB、AVD、手机、个人账本或受保护现场操作；不合并main、不冻结、不进入M23。

下一步只等待一次集中视觉反馈。确认前不自动推广订阅、列表、搜索、设置或备份恢复深页。


## A-R1精修记录（2026-10-26）

**VISUAL_REFINEMENT_READY — UI-C2 A-R1**；**PROTOTYPE_ONLY / APP_VISUAL_PENDING**。来源`49ca1fcd01758f3aab0d1bdf84bf4943b246cc9b`，原A视觉CHANGES REQUIRED不改判。仍只在产品完善分支实施四页精修，不进入B段或M23。

### 前后对照与实现边界

[修改前浅深图](https://github.com/cc7279694-debug/property/blob/49ca1fcd01758f3aab0d1bdf84bf4943b246cc9b/docs/design/ui-c2/PROTOTYPE_ONLY-overview.png) · [修改前代表状态](https://github.com/cc7279694-debug/property/blob/49ca1fcd01758f3aab0d1bdf84bf4943b246cc9b/docs/design/ui-c2/PROTOTYPE_ONLY-states.png) · [原A报告](https://github.com/cc7279694-debug/property/blob/49ca1fcd01758f3aab0d1bdf84bf4943b246cc9b/docs/evidence/UI-C2-Sample-Review.md)。原字节和所有历史结果留在原提交，不复制大包。

[修改后浅深8页](../design/ui-c2/PROTOTYPE_ONLY-overview.png) · [修改后4个代表状态](../design/ui-c2/PROTOTYPE_ONLY-states.png) · [离线交互HTML](../design/ui-c2/UI_C2_A_Prototype.html)。两张图均已实际打开查看；来源是本机已有Chrome的独立无界面浏览器，非Android/Compose截图。

| 整改 | 原问题 → HTML处理 | 生产Compose对应实现 | 仍未实际运行 |
|---|---|---|---|
| R1-01 金额 | 零碎断行→真实字形宽度适配，数字整体单行；宽度仍不足则单独横向视口/明确提示，全量ARIA单位；网格用minmax(0,1fr)防小计撑宽 | AmountText使用当前density/fontScale的TextMeasurer，首选字号降至min(原字号,18sp)，比例行高；maxLines1/softWrapfalse；必要时至少48dp高的横向查看，完整内容描述。AmountFormatter原对象不变 | Android字形/滑动/读屏；360dp及LocalDensity2断言仅编译 |
| R1-02 表单 | 返回/标题竞争、浮动标签拥挤→返回48独立行、全宽标题/外置8px标签；正文真实滚动、底部保存占独立空间 | 标题在完整宽度正文；仅S03启用labelOutside可选参数，外置标签8dp；原滚动正文与按实际高度测量的保存区/IME padding，末尾空间；隐藏错误仍展开定位 | 真Android大字/触控/实际IME；没有关闭字体缩放 |
| R1-03 首页 | 重复空列表/大摘要→确认空库只保留紧凑“—”和唯一新增；已有数据压缩小计/日期，真实预览前移 | c2HomeIsEmpty仅在已验证空快照且无读取提示时成立；不把历史0贡献、真实0/负值或错误当空库；沿用原VM窗口、总数和顺序 | 真机首屏与动态刷新布局 |
| R1-04 详情 | 长说明抢占首屏→身份/主指标/累计摘要/真实时间线/资料历史分层；类型与金额、日期及现金口径不同层级 | 所有事件原顺序完整保留；记一笔唯一主动作；更多保留原管理、危险删除原确认；成本历史增加更多快捷入口，不埋在长时间线下 | Android菜单/时间线及二次确认点击；原保护协议源码不改 |
| R1-05 回看 | 计数和重复摘要压低曲线→精简计数、唯一主总额后紧邻曲线，分项/截止信息移到图后 | 原实际趋势VM/TotalCostChart，保留准确选日/分项/完整入口；原图表实现尾部逐字节（规范化换行）一致，不改采样/事件分页/查询 | 实际APK绘图、选日触控及性能 |

[浏览器原始几何/完整金额](ui-c2-r1-20261026-050536/browser-signoff/metrics.json)、[基本页检查](ui-c2-r1-20261026-050536/browser-signoff/overview-qa.json)、[代表状态检查](ui-c2-r1-20261026-050536/browser-signoff/states-qa.json)：普通360×800框内，浅/深回看绘图区顶部约192.3px、高约208.4px；浏览器×2展开备注并滚至末尾后，最后字段仍在保存区上方，间隔约56.5px。长负数常规字号完整一行；更长金额在×2下保留缩放、通过自身横向视口查看，不撑宽页面。这些数值**仅属于浏览器原型**，不是Android fontScale2/IME证据。原型曲线/金额均合成，不冒称引擎或真实账本结果。

阶段内发现并修复了原型网格的真实min-content溢出；初始浏览器读数保留在本机工作记录（非Android结果），最终归档检查按实际可见金额及真实viewport滚动容器核验。未放宽金额完整性、返回48dp或字段不被保存区遮挡的要求。两张图的尺寸/SHA和实际取证脚本见[视觉证据](ui-c2-r1-20261026-050536/visual-evidence.json)。

### 实际技术验证

- 最终 **60唯一JVM PASS**（13新增、47相关回归，不累计重复执行）：[逐方法](ui-c2-r1-20261026-050536/jvm-signoff-summary.json)。原A的75 JVM及18个编译未运行方法原样保留。
- Debug构建、AndroidTest源码编译、Lint最终exit0；**Lint0错误17警告**：[完整命令/退出](ui-c2-r1-20261026-050536/quality-signoff/command.json)、[stdout](ui-c2-r1-20261026-050536/quality-signoff/stdout)、[Lint XML](ui-c2-r1-20261026-050536/lint-signoff.xml)。
- **12新增Android/Compose方法仅编译，全部NOT_RUN**：[范围](ui-c2-r1-20261026-050536/android-compile-only.json)。未运行ADB、真机、AVD、T06/T03/IME或历史大套件。
- [冻结边界](ui-c2-r1-20261026-050536/frozen-boundaries.json)、[源码输入绑定](ui-c2-r1-20261026-050536/input-binding.json)：Room5/backup4、data/domain/VM/导航/写入/Owner/恢复/调度与原图表不变；原AmountFormatter保持完整一致。
- [构建身份/原证书](ui-c2-r1-20261026-050536/artifact-signoff.json)：仅普通Debug检查产物；默认Debug/Release/验证包配置未改，旧preview code18原APK字节保持，不构建/交付新版体验包，不安装或迁移。

### 剩余边界与停止

没有真实Android截图，APP_VISUAL_PENDING。媒体受校验读取入口仍PENDING，不合并M10、不读裸路径，不声称真实封面已显示。全部旧FAIL/UNKNOWN/NOT_RUN/NOTES及M10/M20/M22/M13/G1～G4状态不变。A-R1仅为待视觉确认候选；不自行ACCEPTED/FROZEN，不推广B段、不合并main、不进入M23。

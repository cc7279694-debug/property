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


## A-R2原设计视觉候选（2026-10-26）

**VISUAL_CANDIDATE_READY — UI-C2 A-R2** · **PROTOTYPE_ONLY / APP_VISUAL_PENDING**。

从`4d6671fb4a012950bc4b00307572277671059a8f`继续，工作区起始干净、本地与远端一致。用户与ChatGPT判定A-R1排版部分有效、视觉保真度仍CHANGES REQUIRED；上述A/A-R1报告、60/75 JVM与NOT_RUN不是本轮重新验收结果。本轮只精修A段，不自动开展B段。

### 直接看对照

- [原设计—A-R1—A-R2首页/详情并排](../design/ui-c2/PROTOTYPE_ONLY-A-R2-comparison.png)。原图来自旧UI冻结`0dce7dd`的[浅色原件](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/docs/design/ui-c1/source-v1/ui-reference-light.png)、[深色原件](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/docs/design/ui-c1/source-v1/ui-reference-dark.png)，均实际打开；对照仅等比裁切显示，不改原图字节，示例照片/商标只在设计引用列中。A-R1列由锁定`4d6671f`原HTML渲染，不覆盖旧图。
- [本轮浅色四页](../design/ui-c2/PROTOTYPE_ONLY-A-R2-light.png) · [深色四页](../design/ui-c2/PROTOTYPE_ONLY-A-R2-dark.png) · [无记录/长负金额/浏览器×2/合成封面](../design/ui-c2/PROTOTYPE_ONLY-A-R2-states.png) · [离线交互HTML](../design/ui-c2/UI_C2_A_Prototype.html)。四张均实际打开查看，8基本+5代表框。来源为离线Chrome，**不是生产Compose渲染、MainActivity或APK截图**；浏览器×2不是Android系统200%字体、真实IME或TalkBack证据。
- [原字节SHA、尺寸与来源](ui-c2-r2-20261026-061804/visual-evidence.json) · [真实浏览器几何/检查](ui-c2-r2-20261026-061804/browser-final/metrics.json) · [实际取证脚本](ui-c2-r2-20261026-061804/browser-final/capture.cjs)。原A/R1截图仍保留原路径与Git历史。

### 原设计、旧样板与本轮处理

| 原设计元素 | A-R1差距 | A-R2生产Compose与HTML | 当前真实数据口径 |
|---|---|---|---|
| 暖白/深墨背景与深绿核心卡 | 摘要与背景层次偏弱 | 浅色深墨绿`#26362C`、深色`#2D4135`主卡；浅色大金额/轻辅助字；小计移至卡外 | OverviewResult.totalRate，非今日扣款；未知为—，0/负数、历史无贡献与读取失败分开 |
| 两类小摘要 | 同一大卡内堆文字 | 紧凑两列，窄宽/大字纵排；“当前有效”直接取active字段 | activeProjectCount/activeSubscriptionCount；列表含历史总数另列，不用预览数或总数冒充active |
| 有身份的图文行 | 通用图标与连续段落 | 明确类别映射的自绘电脑/书籍/音频/运动图标，其他类别用真实名称首Unicode字符；状态胶囊/时间/金额分层；按实际宽度将金额放侧边或下方 | 原VM窗口/顺序与每类前2条；不称最近活动。订阅预览historicalAverage/折算付费时长，卡外小计是当前贡献，不是月费 |
| 详情封面、指标与分组操作 | 身份/指标层次弱 | 生产64dp身份区、主成本+持有组、精确投入/收回/净投入、完整时间线与管理菜单；有引用不能显示时明确提示 | ProjectCostEngine与原事件/现金口径不改；无封面与有引用但暂不可用分开。真实媒体展示PENDING；有封面图仅自绘合成阅读器示意 |
| 轻输入页与底部操作 | 大字已修，普通间隔仍大 | 保留独立48dp返回/全宽24sp标题；16dp正文节奏、56dp日期/类别轻行、外置标签；滚动正文+实际保存区/IME边界 | 原VM输入、日期、operationId、错误展开与未确认提交保护不变，不新增媒体写入 |
| 简洁图标分组回看 | 图表提前后仍需分组层次 | 保留唯一主金额/选日/真实趋势，图表轻表面、分析/历史分组与细分隔 | 原TotalCostChart及后续实现不变；不照搬旧图“即将开放”，现在图表/分析/提醒仍可达 |
| 完整金额 | A-R1已修单行/自适应/横滑 | 保留TextMeasurer/Formatter；超出最小视觉字号再提供“查看完整金额”弹窗、可选择/复制的完整格式化金额及单位；复制失败持续提示 | 不改精度/算法，不缩成K/W或省略尾数；普通6.48首选38sp的Compose断言仅编译 |

日期/类别摘要的展开收起不重建VM；所有写入/删除/结束/替换及原requestLeave导航守卫不改。关闭金额查看是纯呈现操作，没有业务写入；当前首页只读事实仍受原generation/asOf和迟到发布保护。HTML只演示设计交互，不能证明真实业务执行。

### UI-C2-V01～V08检查

| 门禁 | 本轮证据与状态 |
|---|---|
| V01 原浅色设计还原 | 已提供实际并排原型，视觉确认PENDING，不自行判PASS |
| V02 原深色设计还原 | 同上；原色板与层次继承，最终App像素未测 |
| V03 首页密度/物品预览 | 浏览器普通360框可见主金额和合成物品行，生产只读接原VM预览，未实机运行；active/含历史总数分别接线；Android NOT_RUN |
| V04 详情封面/指标 | 无图备用、指标与完整事件实现并编译；合成封面布局示意；MEDIA_DISPLAY_PENDING，不计真实图片显示PASS |
| V05 表单/大字 | 浏览器真实滚动几何检查：返回48、标题独立、展开最后字段滚至保存区上方；Android系统字体/IME NOT_RUN |
| V06 回看图表/分组 | 浏览器普通首屏有有效绘图区；精确选日/分项/全部历史仍接原回调；Android绘图/触控 NOT_RUN |
| V07 金额完整 | 格式器/布局相关JVM PASS；浏览器单行/横滑/完整值弹窗检查PASS；Android滑动/复制/读屏 NOT_RUN |
| V08 真实数据/原动作保护 | 静态无假数据进入main、无新写协议/导航绕过；原相关回归PASS；Android菜单/旧代/确认保护只编译，运行NOT_RUN |

### 媒体可行性：MEDIA_DISPLAY_PENDING

当前`data/backup/DataGenerationStore.kt`的ManagedGeneration只有directory/database/settings/coordinator；没有旧UI-C1的images/mediaResolver。旧`feature/media/CoverPreviewLoader`依赖旧`MediaRepository.openAsset → LocalImageStore`及AndroidImageCodec验证链，无法只搬组件工作。当前BackupMediaSnapshot是完整备份中的受控原字节复制，不是页面图片loader，也不能把其中的容器/字节校验称为严格JPEG深度解码。

下一阶段建议仅在另外授权后定义同代只读媒体resolver：在现有协调器/媒体元数据身份下验证归属、无链接路径、长度/SHA/格式/尺寸与前后身份，后台有界缩略解码；请求/缓存必须绑定generation、asset身份和尺寸，换代取消旧结果，失败区分不存在与不可读。不迁入M10写入、Worker、JNI、GC或未知Stage。需独立覆盖坏图、换代、资源上限与释放；本轮不实施。M10三JPEG历史FAIL仍为FAIL，本次不能证明图片安全风险已解决。示例品牌照片没有进入main资源或生产账本。

### 实际定向验证与停止

- **64唯一JVM PASS（4新增身份呈现、60相关回归）**：[逐方法](ui-c2-r2-20261026-061804/jvm-summary.json)。未重跑历史大套件，不累计重复执行；没有捏造视觉或业务RED→GREEN。
- **9新增Android/Compose方法仅编译，运行全部NOT_RUN**：[方法范围](ui-c2-r2-20261026-061804/android-compile-only.json)。已有A/R1未运行边界保持。
- Debug构建、AndroidTest编译与Lint最终exit0；**0错误17警告**：[命令/退出](ui-c2-r2-20261026-061804/quality-signoff/command.json)、[stdout](ui-c2-r2-20261026-061804/quality-signoff/stdout)、[Lint XML](ui-c2-r2-20261026-061804/lint.xml)。必要迭代均保留各自记录，不扩算覆盖数。
- [边界核对](ui-c2-r2-20261026-061804/boundaries.json)、[实际构建/原证书](ui-c2-r2-20261026-061804/artifact-audit.json)、[最终源码绑定](ui-c2-r2-20261026-061804/input-binding.json)：Room5/backup4/data/domain/VM/导航/写入/Owner/恢复/调度不改；Formatter原对象与TotalCostChart尾部规范化文本一致。无INTERNET，默认包/版本/证书配置不改，旧preview code18原APK SHA仍为`d39b6bf764720415d88d0d9f6c149b34dcda2d795f823505786fa4130b906ef4`。仅普通Debug检查，未构建/交付新用户APK。

原图结构借鉴不是照搬历史功能限制；无设备、ADB、安装、私人账本或受保护现场操作。M10/M20/M22/M13与G1～G4、全部历史FAIL/UNKNOWN/NOT_RUN/NOTES保持。本轮仅为视觉定稿候选，待ChatGPT和用户一次集中确认；**不推广B段、不合并main、不冻结、不进入M23**。

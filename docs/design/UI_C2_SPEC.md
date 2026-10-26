# UI-C2 — A段视觉与交互契约

状态：A段视觉独立审查CHANGES REQUIRED；A-R1精修候选待集中确认，PROTOTYPE_ONLY / APP_VISUAL_PENDING。产品基线 `9de00d3889180cb1d3c078ade543c75100a4196d`；直接在 `codex/lithic-v1-product-completion` 实施。只做四页样板，不冻结、不扩展到B/C段，不产生用户安装包。

## 来源及继承

用户本轮明确指令优先；两份提供文件作为设计来源，不作为未来阶段或设备操作授权。

- `LITHIC_UI_C2_Design_and_Plan.md`：24,765 bytes；SHA-256 `8910c34eb9461df63ec3414fac1a25af3686f7eb6f5aa3a61e8da95fbb83a8c5`。
- `LITHIC_UI_C2_Codex_Prompt.txt`：15,642 bytes；SHA-256 `dc4090d05f7ebafa90b43a2c7e0db6eadcfa2ca415de7fd379bd284bf0041e1a`。
- UI-C1冻结来源 [`0dce7dd`](https://github.com/cc7279694-debug/property/tree/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed)，产品输入 `6a25965225ace3212d1653d15b45fc56ef87411e`。已读取该ref的UI_C1_SPEC、FINAL_GALLERY、Theme/EntryCard/AmountText/FormFields/UiPrimitives；实际打开浅深参考板和对应四页截图。仅选择性复用纯视觉主题及12个矢量图标，不合并旧状态/导航/数据代码；Launcher不改。
- 原[浅色板](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/docs/design/ui-c1/source-v1/ui-reference-light.png)、[深色板](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/docs/design/ui-c1/source-v1/ui-reference-dark.png)是历史设计来源，不是本次App截图；旧未开放功能说明不适用于当前产品。

## 统一语言

| Token | Light | Dark |
|---|---|---|
| 背景 / 表面 | #F6F5F1 / #FFFFFF | #111815 / #1B2520 |
| 主字 / 次字 | #17221D / #596A61 | #F1F3ED / #B4BFB7 |
| 主色 / 主色上文字 | #365F4A / #FFFFFF | #9EBCAA / #17221D |
| 浅强调 / 分隔 | #E6EFE7 / #DFE4DE | #2D4135 / #32433A |

跟随系统、固定色板，无动态取色或新增持久主题开关。主金额38sp、页面24sp、区块18sp、正文16sp、辅助14sp；4/8/12/16/24/32dp节奏，左右16dp、区块24dp。卡片16dp、摘要20dp、图像12dp；图标24dp，触控至少48dp，预览身份区64dp。金额整体单行、保留完整符号和小数；按实际字形宽度从首选字号适配到min(原字号,18sp)，仍不足时提供明确横向查看提示及完整内容描述；系统fontScale照常作用。大字时小计纵向重排。不使用装饰渐变/玻璃/石纹/假曲线/品牌图片/emoji。

## 四页生产实现

| 页面 | 信息与主要动作 | 必须保留 |
|---|---|---|
| S01 现在 | 标题+搜索+新增；总日均及两小计同区，非今日扣款；两类各最多2条当前列表预览+全部；全空只有一次新增主动作；类别按需展开 | 原引擎与排序，历史数量口径；0/负值/空/无当前贡献/读取失败/旧结果分别表达；面板仅两项且关闭不写入 |
| S02 项目详情 | 紧凑身份，日均/持有主指标，投入/收回/净投入次级；完整金额记录线；底部记一笔，更多含原低频操作及独立危险删除分组 | 原编辑/追加/结束修正/纠错/替换/以旧换新/删除保护与原确认；成本历史入口 |
| S03 新增项目 | 名称/初始投入/拥有日期/类别在前；付款日期摘要但不同日和错误直接展开；备注按需；底部唯一保存随IME | 所有VM默认值、输入与operationId；隐藏错误自动展开定位；0合法/空金额不改0；原提交不确定状态 |
| S04 回看 | 原总成本趋势为主；真实日期/准确数值/分项/精确选日；下方类别/年度/复盘/两类历史 | 原负值与峰谷、完整日期/事件分页及记录来源；无事实不画图；进入按需读、离开暂停 |

三主入口真实图标+文字；深层内容以返回来源为主。新增/金额选择仅短面板，业务操作继续原requestLeave、dirty/pending/discard和恢复门禁。UI新来源HOME/REVIEW_HOME只用于保留返回上下文，不增加写入权。

## 真实接线与媒体缺口

首页直接使用当前generation作用域已有项目/订阅VM的列表读取，各取前2条，不新开Room/Owner或直接读DAO。原列表读取窗口仍为50条及原已加载页数，并非只计算2条；不称最近活动，预览数量不冒充总量。旧日期预览不伪装今日数据，错误保留上次结果并标识。恢复换代仍经原GenerationUiScope绑定/清理。

回看复用原TotalCostTrendViewModel/图表和原精确日期路径，不在现在页预跑全历史。完整事件和分页留在原趋势页，入口可达，不新造引擎/缓存。

当前产品基线只有已登记媒体元数据及备份能力，没有页面可用的MediaRepository或封面管理入口。A段不新造文件读取/导入/清理：无封面用身份图标；已有coverAssetId但没有受校验读取时明确“封面暂不可用”。详情提供纯UI插槽，未接入的封面查看/管理仍PENDING；不能据此声称已有图片显示完成，也不复制未冻结M10。

## 样板及验证口径

当前Gradle没有Compose Desktop/Layoutlib截图、Paparazzi或Roborazzi渲染配置；不新增依赖或大型截图工具。采用[单文件离线样板](ui-c2/UI_C2_A_Prototype.html)，包含四页浅深设计及代表状态，**PROTOTYPE_ONLY / APP_VISUAL_PENDING**。HTML为合成设计原型，不是生产Compose渲染或APK截图。生产屏幕另由AndroidTest合成夹具直接调用，仅编译、设备运行NOT_RUN；不存在生产假数据。

Room5/backup4、领域引擎、Repository写入、Owner/coordinator、恢复Token/原子指针、媒体协议及提醒调度锁定。设备、安装、用户数据操作为0。历史M10/M20/M22/M13及G1～G4全部保持。


## A-R1精修授权（2026-10-26）

固定来源`49ca1fcd01758f3aab0d1bdf84bf4943b246cc9b`；A段源码/原报告和两张旧图保留在该提交，视觉结论CHANGES REQUIRED，不进入B段。

Locked：暖白/岩绿/深墨语言、四页职责和唯一主要操作、全部业务回调/守卫、Room5/backup4及金额/日期/Owner/恢复/媒体协议；没有封面读取接口的待办继续PENDING。

Adjustable：金额完整单行与有下限的测量适配、空态和首屏密度、标题/返回独立布局、标签间距、记录线层次、真实图表与摘要的相邻顺序。

R1-01：只改AmountText呈现，AmountFormatter原源码不变；实际TextMeasurer使用当前density/fontScale，maxLines1/softWrapfalse，横向不足明确提示；HTML独立按浏览器字形测量，不冒充同像素Android结果。
R1-02：返回48dp独立行，24sp标题在完整宽度滚动正文；S03才启用外置字段标签+8dp，底部保存按实际高度独立测量并保留IME安全区。隐藏错误展开/定位与原输入/operationId不变。
R1-03：确认空库才使用紧凑空态和唯一CTA，不再显示两类空列表/空小计；已有历史、0/负值、读取失败不进入此空态。有记录首屏压缩摘要，保留真实预览与口径。
R1-04：身份→主要成本/时间→累计金额→完整事件时间线→资料/历史。时间、类型、金额与现金口径分层；历史在更多菜单亦可直达；原删除确认不变。
R1-05：回看压缩真实计数与日期，单一32sp首选总额后紧邻原图表；分项和截止口径在图后，精确查询和完整趋势入口保留，不动图表采样/查询/分页。

原型为8个基本状态+4个代表状态（空、长负金额、长金额浏览器×2、大字表单）。仅更新原两张汇总图；浏览器×2与真实Android系统字体/IME分别登记。实际结果见[集中报告A-R1段](../evidence/UI-C2-Sample-Review.md#a-r1精修记录2026-10-26)。

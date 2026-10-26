# UI-C2 — A段视觉与交互契约

当前首页规范以文末 **B-HOME** 为准：已录入总数量小卡＋独立底部区域的圆形新增。A/A-R1/A-R2保留为历史设计与对照；B1～B4其余成果不回退。生产Android视觉仍APP_VISUAL_PENDING。
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


## A-R2原设计保真候选（取代上一轮继续推广假设）

来源4d6671f；A-R1排版部分有效、视觉仍CHANGES REQUIRED。LOCKED：UI-C1原浅深设计语言、四页责任/主要动作、业务/导航/数据安全与R1金额/大字保护。ADJUSTABLE：主卡表面强度、两类小计与物品行比例、图标身份、指标/分组/表单密度；不新建产品风格。

浅色主摘要复用深墨绿#26362C，深色#2D4135；主字#F1F3ED、辅助#C3D9CA。保留既有其他色板/圆角/系统字缩放，首页和回看24sp标题。普通主金额38sp首选，真实测量后才适配；极长值另有完整格式化金额查看/选择复制。小计分离，当前数量仅用active字段，含历史总数明确另列。类别图标仅基于明确类别名；自定义类别显示真实名称首字符，不按名称猜品牌/物品照片。

无封面小身份、主成本/持有组、累计金额及完整时间线；真实媒体MEDIA_DISPLAY_PENDING，仅另提供明确合成素材的有封面原型。旧UI-C1的未开放功能说明不覆盖已实现的趋势/备份/提醒。UI-C2-V01～V08及原设计—A-R1—A-R2对照见[集中记录](../evidence/UI-C2-Sample-Review.md#a-r2原设计视觉候选2026-10-26)。状态VISUAL_CANDIDATE_READY / PROTOTYPE_ONLY / APP_VISUAL_PENDING，等待集中视觉确认。


## B段生产UI整合（2026-10-26）

A-R2获独立限定认可：**PASS WITH NOTES — PROTOTYPE VISUAL DIRECTION ONLY**；不等于Android生产视觉通过。沿用原暖白/岩绿/深墨色板、身份图标、精确金额、48dp返回和主次操作，不改A段四页结构。

- 列表/历史使用64dp身份、状态胶囊及原引擎真实指标；自定义类别使用真实首字。搜索DTO没有金额/日期/封面，保持真实名称/类型/类别/状态，不补假指标或额外查询。当前/历史、空/加载/失败/旧结果分别表达；原排序与分页不变。
- 订阅档案先身份，再历史平均与折算时长，当前套餐/当前标准日均另组；折算不是已经使用时长。唯一主操作记录续费；暂停/停止/纠错/重新启用等保留原资格并可从更多进入。付款及状态表单使用同一固定操作区、滚动正文、原未知提交保护。
- 项目金额/纠错/结束/删除/以旧换新使用共用页面层级，投入与收回、结束与永久删除不混淆。生命周期按钮沿用原状态与回调，关联交易事实不简化。
- 复盘/类别/年度/图表/通知来源保持真实读数与入口；设置为分组行，记录时区是只读事实。导出/恢复的Success、取消、残留、中断/未知仍依据原类型化结果，预览核对与明确覆盖确认保留，繁忙时不发第二操作。
- 共用状态标签合并语义，使原tag仍可读到真实状态；返回按钮保留原标签和内容描述。正文与底部操作区分开测量，IME padding不意味着真实键盘已验证。

B段少量代表样板：[订阅与设置浅深HTML](ui-c2/UI_C2_B_Prototype.html)及[四帧图](ui-c2/PROTOTYPE_ONLY-B-overview.png)，均 **PROTOTYPE_ONLY / APP_VISUAL_PENDING**。不是Compose渲染、APK截图或系统字体/IME证据。详细检查见[集中报告](../evidence/UI-C2-B-Integration.md)。

媒体仍 **MEDIA_DISPLAY_PENDING**：没有现成同代页面resolver；不得UI拼路径、直查DAO、搬旧M10写入/JNI或把BitmapFactory成功当严格JPEG安全证明。后续需另行批准同代只读身份/归属/完整性核验、后台有界缩略解码、换代缓存隔离及分级失败状态。无封面与无法读取明确区分，本轮不实现媒体。


## B-HOME 首页数量卡与悬浮新增（2026-10-26）

本轮Goal：在B1～B4桌面实施获限定PASS WITH NOTES后，仅收口用户指定的首页原设计。基线`b5a7e3112e2cd63463103697da90f07d065fa335`；不进入C段。参考实际从旧UI冻结`0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`打开的浅/深原图，不merge旧代码。

LOCKED：B1～B4、Room5/backup4、金额/日期/订阅引擎、数据/Owner/导航守卫/恢复/媒体协议、默认构建身份、原证书与preview18。Scope仅HomeShell生产呈现、相关测试路径、HOME离线样板与本轮证据；不做真实图片、主题持久化、设备、安装或新版用户包。

- 数量卡：同一OverviewResult的projectCount/subscriptionCount，含历史；不使用active字段冒充总数，也不回退不明来源兼容参数。未知显示—，旧快照有持续警示及数量可访问描述。72dp最小高度、等宽两列、36dp轻图标底块、24sp数量/14sp短标签；大字>1.4或窄容器纵排，随内容增长，不加金额第三行。
- 口径：主卡原信息入口显示本地数量/成本说明，明确含已结束/停止记录；查看/关闭不导航，明确“完整计算说明”才沿用原导航回调。首页重复含历史/总数长句不默认铺出。
- 成本：原两类未舍入结果、active计数、类别贡献与原金额testTag留在“查看成本与类别构成”下；0、负值、小于分位、无数据/未知不混淆。没有复制计算公式或删除查询能力。
- 新增：顶栏仅搜索。非确定空库时56dp圆形FAB，原home-open-add；内容区右/下16dp、2dp轻层次，底部导航仍在父NavDisplay内容之外。非空账本将滚动视口底部提前72dp，保留独立透明FAB操作带；正文再留16dp，记录/操作不在FAB下绘制，最后内容可完整滚出。真正空库仅“记下第一条记录”；面板仍原两项，关闭零写入，选择沿用原回调/守卫。
- 记录：首页48dp身份（其他列表64dp保持），名称优先，状态与时间流式同组，右侧短标签“日均”/“历史均价”并保留完整金额/口径语义；订阅“折算付费”不伪称已使用。长中文/金额与大字可自然扩展，不锁死行高。

比较与验证按[UI-C2 B整合报告的B-HOME增量](../evidence/UI-C2-B-Integration.md#b-home首页原设计收口2026-10-26)登记；旧A-R2与B原型/结果保留。新HOME HTML/浅深三列及空态/浏览器大字均PROTOTYPE_ONLY / APP_VISUAL_PENDING。未运行Android，真实字体、IME、触控与生产MainActivity视觉仍NOT_RUN；媒体MEDIA_DISPLAY_PENDING。


## C1 真机反馈后的空态与层级修订（2026-10-26）

Preview3静态交付记录保留，视觉由用户转交独立结论VISUAL DELIVERY REJECTED。最新C1覆盖旧空态分支：Home始终保留深绿主卡，确认空Overview为—及两张真实0数量卡；未知仍—/错误旧结果有提示，空库仅原单CTA。金额按实测文本宽紧邻元/天，原Formatter/完整金额入口保留。Review确认空仅短引导及守卫回首页，无菜单墙/虚构图表；加载、错误、恢复未知、残留事实不判空，内嵌趋势不重复声明empty。正常菜单56dp最小、标题16sp，说明按需；主标题24/32、主金额38/46、辅助14/20与系统缩放保持，底部导航独立12/16sp，不通过压缩触控或关闭缩放解决问题。

本轮生产Compose已编译；设计参考、生产源与真实截图是不同证据。没有HTML和真实Android截图，A～E及360/400/系统字号/IME仍待真实运行。APP_VISUAL_PENDING／MEDIA_DISPLAY_PENDING；不能登记VISUAL_ACCEPTED。见[C1报告](../evidence/UI-C2-C1-Real-Visual-Remediation.md)。


## C2 Preview 5拒绝后的空态收口（2026-10-26）

本节覆盖C1与B-HOME的空库新增结构，历史记录不改。Preview5视觉仍VISUAL REJECTED；当前仅UI-C2 C2 IMPLEMENTED / APP_VISUAL_PENDING。

- Home无论空/有记录均保留56dp圆形＋、原两项选择面板、72dp底部独立操作带；删除第二全宽空库新增按钮。绿主卡纵向12dp、组间4dp、标签14sp，主金额38sp及完整金额/单位适配保持。确认空Overview为—与两个真实0；未知/旧快照不冒充0，只有一句引导。
- Review确认空库采用透明自然高度内容组：112×88dp现有Review/Project/Subscription矢量组合、单句引导及唯一“去首页记录”，原requestLeave路径；不是单色铺满大卡。非空、历史贡献0、加载、错误、旧结果判断不变，真实趋势与入口保留。
- 不改变Theme、全局字体缩放、金额公式和媒体边界；真实手机fontScale/density UNKNOWN。旧HTML不再作为本阶段验收证据。
- A～E原生夹具仅调用生产HomeShell/ReviewShell及共享底栏，测试源集不可变合成事实经原引擎计算，无Room/Owner；400/360dp和2倍字为LocalDensity，隔离ComponentActivity不冒充MainActivity或真实系统字体/IME。仅编译，实际截图NOT_CAPTURED，视觉对照仍PENDING，不能申请VISUAL_ACCEPTED。

范围及真实证据见[C2报告](../evidence/UI-C2-C2-Visual-Fidelity.md)。没有Preview6或设备操作；封面MEDIA_DISPLAY_PENDING，业务/Owner/恢复/导航守卫及所有历史风险不变。

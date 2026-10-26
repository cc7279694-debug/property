# UI-C2 B 全应用整合

**UI-C2 B IMPLEMENTATION COMPLETE — READY FOR INTEGRATION REVIEW**

来源`e24703b556660683bb25f5b14e059c1838e7b3f9`，当前产品完善分支。A-R2独立认可仅 **PASS WITH NOTES — PROTOTYPE VISUAL DIRECTION ONLY**；本轮未取得Android生产视觉PASS。

## 生产页面与保护

| 部分 / 独立代码提交 | 已实施 | 验证与边界 |
|---|---|---|
| B1 `100a369` | EntryCard、项目/订阅当前与历史列表、名称搜索；64dp身份、状态、真实指标、紧凑标题/分页/失败与旧结果 | 原查询、排序、分页与返回来源不变；搜索DTO没有金额/日期/封面，不添假数值或额外查库 |
| B2 `dce6bf5` | 订阅身份→历史平均/折算付费时长→当前套餐→付款事实；固定续费主操作、更多管理；新增/续费/状态渐进表单 | 当前套餐与历史平均分开；30/90/365原口径、未来预付/暂停/到期/停止与原资格不改；停止不是取消服务商扣款 |
| B3 `6f2446a` | 金额/纠错、结束/删除、以旧换新、替换对照；共用滚动正文/独立操作区 | 原LifecycleFormActions、IN_FLIGHT/UNCONFIRMED、幂等重读、删除确认和三笔关联交易不变 |
| B4 `e011212` | 复盘、年度、类别分析/管理、图表外层、通知设置/来源、产品说明、设置、恢复预览 | 原图表核心/精确值/负值/日期/事件不改；设置改分组行；记录时区只读；导出恢复只有真实Success成功，busy/未知/预览确认及十表核对完整保留 |

共用`C2Page.kt`统一48dp返回、24sp标题、列表行、状态标签和独立底部操作区；字体较大时标题动作重排。状态tag合并后代文本语义，原返回描述保留。`SubscriptionPriceChart`、`ProjectCostChart`通过共享`CostChartPage`外观适配，计算文件未改。A-R2主题、首页/项目详情/新增项目/回看主要结构与AmountText原测量/完整金额能力保持。

独立定位器修正提交`30a4fa1`。已有测试仅同步More/固定按钮的定位，不删业务断言或修改预算。收尾源码审查又发现两份旧Journey的订阅按钮仍在正文找More/footer，以及一个`end-back`仍按正文滚动，已仅调整定位并独立编译；**没有执行T06、M13旅程或以此宣称历史失败已修复**。T06失败保留teardown不改。

## 实际验证

- 阶段定向JVM：B1 **86 PASS**、B2 **21 PASS**、B3 **54 PASS**、B4 **77 PASS**，均有独立XML；重复方法不累计覆盖。见[分段摘要B1](ui-c2-b-20261026-072403/b1-summary.json)、[B2](ui-c2-b-20261026-072403/b2-summary.json)、[B3](ui-c2-b-20261026-072403/b3-summary.json)、[B4](ui-c2-b-20261026-072403/b4-summary.json)。
- 最终受影响集成回归 **169个唯一JVM PASS / 0失败 / 0跳过**：[逐方法](ui-c2-b-20261026-072403/integration-summary.json)、[实际命令/退出0](ui-c2-b-20261026-072403/integration-final/command.json)、[stdout](ui-c2-b-20261026-072403/integration-final/stdout)。涵盖呈现/导航、P01～P03、金额展示、导出恢复Controller、复盘，不重跑无关历史套件。
- 新增 **21个Compose方法仅编译，Android执行0，全部NOT_RUN**：[方法清单](ui-c2-b-20261026-072403/android-compile-only.json)。最终定位器编译另见[命令/退出](ui-c2-b-20261026-072403/locator-approved-compile/command.json)。未把编译或源码审查当运行PASS。
- Debug构建/AndroidTest编译/Lint退出0；**Lint 0错误17警告**：[XML](ui-c2-b-20261026-072403/lint.xml)、[统计](ui-c2-b-20261026-072403/lint-summary.json)。最后仅修改测试定位器，生产源码与该构建相同。
- B2两次必要编译失败原样保留：[首次](ui-c2-b-20261026-072403/b2-check/command.json)、[同一旧参数失败](ui-c2-b-20261026-072403/b2-final-compile/command.json)。原因是两个UI日期调用误用不存在的`compactRow`参数；改为已有`labelOutside`，原日期/校验/回调不变，[修复后退出0](ui-c2-b-20261026-072403/b2-approved/command.json)。这是编译错误，不是Android业务RED。

## 视觉样板

[浅深四帧图](../design/ui-c2/PROTOTYPE_ONLY-B-overview.png) · [离线HTML](../design/ui-c2/UI_C2_B_Prototype.html) · [浏览器尺寸/原字节SHA](ui-c2-b-20261026-072403/browser-b-approved/metrics.json)。仅用订阅/设置两页×浅深代表，不重复铺大量图片。已实际打开最终图核对；年付标签、精确金额与未知警示的样板排版已修正。

**PROTOTYPE_ONLY / APP_VISUAL_PENDING**：HTML全部合成设计数据，不是Compose渲染、APK截图或MainActivity；不证明Android触控、真实系统字体、IME、TalkBack、业务写入/恢复运行。原型按钮只展示说明，无真实付款/文件/通知动作。

## B5媒体与未运行范围

**MEDIA_DISPLAY_PENDING**。[只读源码核对与建议](ui-c2-b-20261026-072403/B5-media-readonly-review.md)：当前generation没有页面媒体resolver；旧UI-C1可参考纯显示与生命周期分支，旧存储/解码API不能直接搬。备份归属/长度/SHA/尺寸和Android可读性不等于严格JPEG熵流安全；M10三JPEG历史FAIL保持。

后续需专项授权：既有Owner提供同代只读身份、归属及前后文件核验，有界后台缩略解码、generation/asset/尺寸缓存与迟到发布隔离；分别表达无引用、元数据/文件缺失、校验/解码失败与未知归属。LRU和协程取消不冒称进程硬上限或native已结束。本轮不加resolver、不拼私有路径、不直查DAO、不迁入M10 Worker/JNI/Stage/GC；真实封面与资源释放均待验证。

## 身份与停止

[边界核对](ui-c2-b-20261026-072403/boundaries.json)、[构建/证书核对](ui-c2-b-20261026-072403/artifact-audit.json)、[最终源码绑定](ui-c2-b-20261026-072403/final-source-binding.json)：Room5/backup4、data/domain/schema、VM/Controller、导航、Owner/恢复/提醒协议与默认构建配置无diff，未新增依赖或INTERNET。普通研发Debug仍`com.cooper.lithic.debug` / code17 / `0.1.0-m22-debug`，LithicApplication/MainActivity及原Debug证书保持；这不是用户交付包。未构建新的Private Trial/Release。已有preview code18 APK原字节SHA仍`d39b6bf764720415d88d0d9f6c149b34dcda2d795f823505786fa4130b906ef4`。

无ADB/模拟器/真机/安装或私人数据操作。Android视觉/字体/IME、媒体、实际返回与恢复体验均NOT_RUN；M10、M20平台FAIL/UNKNOWN、M22 BLOCKED、M13-T06及G1～G4全部历史状态不变。

下一C段仅建议：另获明确设备授权后，在隔离合成账本中集中核对生产MainActivity真实接入、浅深主题、关键表单/菜单/来源返回、未知提交及恢复预览。需要交付时才构建同preview ID/原证书、code>18的唯一新版体验包；本轮未执行C、不合并main或进入M23。停止等待独立整合审查。


## B-HOME首页原设计收口（2026-10-26）

来源`b5a7e3112e2cd63463103697da90f07d065fa335`。用户转交B1～B4桌面实施独立结论PASS WITH NOTES，但Android整体仍APP_VISUAL_PENDING；本轮只补首页，不执行C段。

### 真实数据与原设计映射

| 原UI-C1元素 | 本轮生产HomeShell | 保护与状态 |
|---|---|---|
| 两张图标/大数字/短标签小卡 | 同一Overview的projectCount/subscriptionCount，包含历史；72dp最小高度、24sp数字、36dp图标底块；大字纵排 | 不拿active冒充总数，不回退独立计数参数；未知—，旧快照持续警示和语义标记“上次” |
| 右下圆形＋ | 内容Box内56dp圆形FAB，右/下16dp、2dp轻层次；顶栏保留搜索 | home-open-add与原两项新增面板/原callbacks不变；确定空库仅首条CTA；72dp独立FAB操作带＋正文底部16dp，滚动视口截止于FAB顶部，最后内容可完整滚出 |
| 紧凑记录行 | 首页48dp身份（其他列表64保持）、名称/状态/时间、金额＋日均/历史均价与单位两行 | 没有假照片；长字/金额/大字自然扩展；完整口径及原金额仍可访问，折算时长不称已使用 |
| 轻量说明 | 主卡信息先开数量/成本口径，本地关闭零导航，完整说明再走原回调 | 口径明确含历史；不改Nav3来源/requestLeave；两类小计/active和类别精确金额移至原展开入口，未删除计算或金额断言 |

数量卡取的是已录入总数；真实成本继续来自原未舍入Overview/Engine结果。原home-project-rate、home-subscription-rate、类别及rounding tags在展开后可达；0、负值、小于分位和不可用断言保持。两个旧Journey只各加一次展开动作，不改原金额、预算或失败保留teardown；没有运行T06或其他历史旅程。

### 视觉来源与对照

已从冻结`0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`提取并实际打开浅深原图；原图不在当前工作树中，未因此假称本地原路径存在。对比图左为同ref裁切原设计，中为原A-R2冻结HTML，右为当前HOME合成HTML。旧A-R2 HTML及截图原字节保留。

- [浅色三列](../design/ui-c2/PROTOTYPE_ONLY-HOME-light-comparison.png)
- [深色三列](../design/ui-c2/PROTOTYPE_ONLY-HOME-dark-comparison.png)
- [空态/浏览器×2代表图](../design/ui-c2/PROTOTYPE_ONLY-HOME-states.png)
- [当前首页独立HTML样板](../design/ui-c2/UI_C2_HOME_Prototype.html)

以上均 **PROTOTYPE_ONLY / APP_VISUAL_PENDING**，不是生产Compose渲染或Android截图；原设计品牌照片只作参考，不在生产数据/资源中。浏览器检查了72px数量卡、56px圆FAB、导航上16px、顶栏无＋、空态单CTA、大字纵排和无横向溢出，以及末尾内容可滚高于FAB；首轮标准悬浮样板曾遮挡部分订阅金额/大字操作；最终生产与样板同步改为独立72dp操作带。浏览器几何确认普通/×2的viewport.bottom≤FAB.top，不只验证末尾滚出；旧采样记录保留。真实Android触控/字体/IME/生产MainActivity仍NOT_RUN。

### 定向验证与停止

- 首页/Overview/导航/金额呈现 **56个唯一JVM PASS**，未重跑B段169项全集；最终逐方法与XML在本轮原始目录保存。
- 新增 **7个生产Compose合成夹具方法，仅编译，运行全部NOT_RUN**。覆盖总数/active、未知/旧结果、唯一FAB/关闭零写入、说明回调及360dp/LocalDensity×2布局；几何断言增加滚动视口截止在FAB顶部。原金额精度/0/负数/不可用断言保留，呈现路径改为先展开。
- 最终Debug构建、AndroidTest源码编译与Lint **exit0；0错误17警告**。Lint Analyze实际执行，Report因诊断相同为UP-TO-DATE，原报告时间保留，未冒称新写报告。[最终命令/退出](ui-c2-home-20261026-085124/home-no-overlap-signoff/command.json)／[stdout](ui-c2-home-20261026-085124/home-no-overlap-signoff/stdout)／[56逐方法](ui-c2-home-20261026-085124/final-jvm-summary.json)／[7新增Android仅编译](ui-c2-home-20261026-085124/android-compile-only.json)／[Lint XML](ui-c2-home-20261026-085124/lint.xml)。
- [首次编译失败](ui-c2-home-20261026-085124/home-quality/command.json)及[原始错误](ui-c2-home-20261026-085124/home-quality/stderr)保留：弹窗括号遗漏，修正后各独立检查通过；不作为Android业务失败或伪造RED→GREEN。中间质量检查与最终操作带版本均保留，重复次数不累计覆盖。
- [最终浏览器几何/尺寸/SHA](ui-c2-home-20261026-085124/browser-band-signoff/metrics.json)／[原图及R2来源](ui-c2-home-20261026-085124/browser-band-signoff/source-provenance.json)／[命令](ui-c2-home-20261026-085124/browser-band-signoff/command.json)。最终三图已实际打开；较早普通悬浮采样位于browser-prototype，仅作遮挡诊断历史，不能用其旧SHA冒称最终图片。
- [最终普通Debug身份/原证书](ui-c2-home-20261026-085124/artifact-signoff.json)／[冻结边界](ui-c2-home-20261026-085124/boundaries.json)／[最终源码绑定](ui-c2-home-20261026-085124/final-source-binding.json)。只构建研发Debug核验，不交付该包，未构建新的Private Trial或Release。

仅HomeShell生产文件改变，B1～B4其余生产页面、data/domain/VM/导航、Room5/backup4、媒体/Owner/恢复/调度及默认构建配置不改。真实封面继续MEDIA_DISPLAY_PENDING；原证书与preview18旧字节保持。无设备、ADB、新用户APK、数据读取或清理。历史M10/M20/M22/M13/G1～G4及全部失败/未知不变。

停止于 **UI-C2 HOME CLOSURE — READY FOR VISUAL REVIEW**；等待集中视觉审查，不自动C段、main合并或M23。

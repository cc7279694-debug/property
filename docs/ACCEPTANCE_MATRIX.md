# 验收登记

当前交付：**M18 — READY_FOR_REVIEW（离线候选）**，分支`codex/m18-total-cost-history`，来源M17冻结`7e74df960e994e3a36f4417f7d83b07ead63763f`。174 JVM PASS，Lint0错误23警告、App/Test构建及测试源码编译成功；17 Android和截图NOT_RUN，不宣告M18全部通过或冻结。最近正式限定冻结为M17及此前模块；历史记录与延期门禁保持。

## M18 当前执行验收（2026-10-21）

本轮实际实现与离线验证，**不是独立验收/冻结**。[契约](planning/M18_CONTRACT.md)、[集中报告](evidence/M18-verification.md)、[逐类统计](evidence/m18/results.json)、[原始证据](evidence/m18/README.md)。

| 原案例 | 本轮实际结果及未运行范围 |
|---|---|
| M18-T01 every_point_matches_reference | PASS（JVM）：固定seed合成账本100观察日、独立逐项冻结引擎参照、未展示舍入总额/两类贡献精确一致。 |
| M18-T02 closed_or_expired_removed | PASS（JVM）：Q02结束日退出及后结算不重进总额；暂停/到期/停止重开/未来期/同日边界。真实Room子项NOT_RUN。 |
| M18-T03 today_equals_home | PASS（JVM同快照口径）；NOT_RUN（真实Room >50条/零负值/预付/M16交易）。不登记完整案例PASS。 |
| M18-T04 sampling_keeps_spikes | PASS（JVM）：20年7306日完整参考、三线桶极值/事件邻点、峰谷/抵消、非显示日精确查询。Android绘图及截图NOT_RUN。 |
| M18-T05 multi_event_day_not_false_causality | PASS（JVM同日73事件索引）；NOT_RUN（Android50条窗口/文字/详情返回/只读集成），不登记完整交互PASS。 |

**174 JVM PASS＝30新增（领域24＋导航6）＋144相关回归，0失败/错误/跳过**；源码方法与14份XML精确匹配。最终同组4任务BUILD SUCCESSFUL in 5m39s，294份构建输入前后相同。Lint0错误23警告（较M17增加14条锁版本提示，未改变依赖，原生产文件提示保留）。原导航2/3 FAIL、领域21/22 FAIL、空账本2/2 FAIL与最终GREEN分别保留；首次2处Android测试类型导入编译失败不当行为RED。

**17新增Android方法全部NOT_RUN（Repository7＋VM4＋Compose5＋旅程1），新截图0。** 只读CIM原5672两个进程不存在，未发ADB或操作任何设备；不以编译、旧M17结果或本地辅助审阅冒充新Android PASS。完整快照排队写、跨日/失败/迟到/换代、纠错删除、format2往返/旧代拒读、真实Nav3及360dp/200%均仍待设备执行。截图/系统字号/真实午夜/SAF等不扩大。

Q02仅M18领域子断言有新执行证据，Android/16组完整跨模块未关闭；Q03～Q06与未来期限保持。Room3/backup2/冻结引擎/M16保护/M12恢复不改；M10 PENDING / DEFERRED、三JPEG历史FAIL、M13-T06 NOT_RUN / DEFERRED、G1未整体通过与全部历史NOTES保留。停止待独立验收，不进入M19。

## M17正式冻结登记（2026-10-21）

**M17 — ACCEPTED / FROZEN — PASS WITH NOTES。** 依据用户本轮正式转交的ChatGPT独立源码及证据验收结论；评审提交`54360aa0e61779c7efa62b527f8901778a0845a2`，评审/回写分支`codex/m17-item-cost-charts`。修改前本地/远端HEAD均为该提交且工作区干净。下方原执行登记、报告、结果和checkpoint保留候选交付时的状态，不覆盖原始证据。

| 获独立认可案例 | 限定冻结范围 |
|---|---|
| M17-T01 past_event_boundary | PASS：真实事件边界、历史观察日分母、结束后结算和完整事实读取；Q02仅M17部分。 |
| M17-T02 subscription_is_step_not_smooth | PASS：生效日阶梯，预付不提前改价，暂停/到期/停止零区间及未来计划边界。 |
| M17-T03 events_are_accessible | PASS：同日完整事件、超过50条、折抵与日期/状态说明，360dp/200%隔离交互可达；非完整TalkBack。 |
| M17-T04 negative_values_visible | PASS：0、负值、极小非零、全负与单点按真实值展示，不截0。 |
| M17-T05 no_forecast | PASS：截至业务观察日/真实结束日；后结算独立，不延长持有天数，未来计划不画预测线。 |

独立认可此前实际结果：**144 JVM PASS（33新增＋111相关回归）＋28 Android PASS（19本模块＋9相关回归）＝172唯一方法，0失败/错误/跳过；Lint0错误9警告；Debug App/Test构建成功；9张隔离截图。** [原报告](evidence/M17-verification.md)、[方法统计](evidence/m17/results.json)、[原始证据](evidence/m17/README.md)、[截图来源](evidence/m17/screenshots/README.md)、[原checkpoint](checkpoints/2026-10-21-M17.md)。本轮仅核对登记文档、Git、链接与敏感信息，未重跑测试、构建、APK核验或设备验证；不把独立验收或文档检查登记为新一次产品PASS。

**NOTES与未执行边界保留：**仅原5672/API36/x86_64合成环境、plain Application隔离runner、隔离ComponentActivity中的真实LithicApp/Nav3，不是MainActivity/私人账本。360dp/200%为LocalDensity画布，注入BusinessClock/前台生命周期不是实际午夜，受控文件往返不是新SAF验收。API26/真机/OEM、Release/稳定签名、系统大字/完整IME/TalkBack、真实进程终止/LMK/断电、长期大数据性能仍NOT_RUN，未穷举调度竞态。240为均匀基础点，另保留真实边界与邻点，不宣称总点数或计算耗时硬上限。

Room3/backup2、冻结领域引擎、M16交易保护及恢复/媒体协议保持；Q02的M18未完成部分和原16组完整跨模块门禁仍NOT_RUN，不整体关闭。原RED/FAIL/UNKNOWN/NOT_RUN/NOTES及全部历史证据不改。M10 PENDING / DEFERRED及三JPEG历史FAIL、M13-T06 NOT_RUN / DEFERRED、G1未整体通过、Q03～Q06既定边界均保持。登记后停止，不操作设备、不合并main、不开始M18。

## M17 当前执行验收（2026-10-21）

用户完整附件明确授权；Codex实际执行后READY_FOR_REVIEW，**非独立验收/冻结**。[契约](planning/M17_CONTRACT.md)、[集中报告](evidence/M17-verification.md)、[逐类/方法统计](evidence/m17/results.json)、[原始索引](evidence/m17/README.md)。

| 原案例 | 本轮实际状态 |
|---|---|
| M17-T01 past_event_boundary | PASS：维修前/退款后、Q02历史分母/结束后结算、完整105事件及M16三事件只计一次。 |
| M17-T02 subscription_is_step_not_smooth | PASS：按生效日阶梯；预付不提前改价，暂停/恢复/到期/停止/同ID重开与未来计划。 |
| M17-T03 events_are_accessible | PASS：同日完整事件/说明、超过50条、折抵标识、日期/状态文字替代、360dp/200%交互可达。非完整TalkBack。 |
| M17-T04 negative_values_visible | PASS：零/负/极小非零/全负/单点，真实日期间距/零线；源金额不截0。 |
| M17-T05 no_forecast | PASS：持有期到真实结束日；后结算独立、不增天数；未来预付/停止不画历史预测线。 |

实际**144 JVM＝33新增＋111相关回归，28 Android＝19本模块＋9相关回归，合计172个唯一方法，0失败/错误/跳过**。源码方法与最终XML/Instrumentation身份精确对应，不将重跑叠加；早期口头164 JVM加总错误已按原始11类XML纠正，没有丢失或补造20项。最终Lint0错误9警告、App/Test构建成功，[9图与边界](evidence/m17/screenshots/README.md)。

真实RED/GREEN：时间线/几何29项初次28失败/1通过；设备折抵标签1失败后修复；跨日新图发布时短暂丢失选日1失败后原子发布与捕获快照围栏修复。编译接缝和默认整段文本匹配的测试错误另记，均保留原输出，不改变原业务断言。完整读取、写后/跨日/前台刷新、迟到请求、确认删除、读错留旧图、只浏览不写、format2往返重算及旧代拒读通过。

仅原5672/API36/x86_64全合成环境；plain Application、隔离ComponentActivity中的真实LithicApp/Nav3，非MainActivity/个人账本。LocalDensity360dp/200%不代表系统字号/真机；可变BusinessClock/生命周期不代表真实午夜，受控文件目标不代表新SAF验收。API26/OEM/真机、Release/稳定签名、完整IME/TalkBack、真实进程终止/LMK/断电和长期性能仍NOT_RUN。240为基础点，另保留全部边界/邻点，非总点数或耗时硬上限。M10、M13-T06/G1、Q03～Q06和全部旧NOTES保持；Q02仅M17子断言PASS，原16组完整跨模块记录及M18不回填。Room3/backup2/领域算法/M16组保护/恢复/媒体协议不改。

## M16正式冻结登记（2026-10-21）

**M16 — ACCEPTED / FROZEN — PASS WITH NOTES。** 来源为用户本轮正式转交的ChatGPT独立代码及证据验收结论，评审提交`d8ce0a8d602c2e9e544662e4b5bf202df5812aef`，评审/回写分支`codex/m16-replacement-trade-in`。修改前本地/远端HEAD均为该提交，工作区干净。下方候选执行登记、原报告/结果/checkpoint保持历史状态；不回填旧PASS或改写原始证据。

| 获独立认可案例 | 限定冻结范围 |
|---|---|
| M16-T01 one_predecessor_successor_no_cycles | PASS：一前任/一后任；A→B→C合法，自指/环/多前后任/缺项及陈旧事实拒绝。 |
| M16-T02 link_does_not_end_old | PASS：纯关联/解除不改变金额或生命周期。 |
| M16-T03 trade_in_balance | PASS：旧回收3000、新现金5000＋折抵3000，新成本8000、净新增5000；保留唯一INITIAL和三事件身份，零现金/同日边界。 |
| M16-T04 trade_in_all_or_nothing | PASS：六处SQL故障回滚、完整事实CAS、精确目标重放/冲突及用户确认的交易组保护。 |
| M16-T05 delete_middle_does_not_guess | PASS：普通中间项目删除只断相关边、不重连；交易项目删除继续受保护。 |
| M16-T06 v1_backup_upgrade | PASS：真实v1先原格式校验再显式升级；v2八表/关系/折抵/设置/媒体往返，坏备份保护旧集，旧schema2＋五字段proof＋SWITCHING生产打开兼容。 |

**数据范围：Room schema v3、backup format v2。** schema3为八表、basis/group及replacements，显式1→2→3，旧schema1/2原字节不改。format2严格保留新增字段、替换关系和完整折抵组；真实format1验证后补CASH/null/空关系。M12 pointer/ready/proof协议仍1，旧五字段按原v1 serializer/hash核验，新proof明确dataFormatVersion2；不扩大为M10严格JPEG或未来备份格式通过。

独立认可此前实际结果：**252 JVM PASS（46新增＋206回归）＋50 Android PASS（34本模块＋16回归）＝302个唯一方法，0失败/错误/跳过；Lint0错误9警告；7张隔离截图。** [原集中报告](evidence/M16-verification.md)、[原方法/身份统计](evidence/m16/results.json)、[原始证据入口](evidence/m16/README.md)、[截图来源](evidence/m16/screenshots/README.md)。本轮只核对既有记录与Git，并执行文档差异/链接/敏感信息检查；不重跑产品测试、构建、APK或设备核验，不把独立审查记作新一次运行PASS。

**NOTES与未执行边界全部保留：**仅API36/x86_64合成环境、plain Application隔离runner、隔离ComponentActivity/真实LithicApp/Nav3；360dp和02/04两张200%为LocalDensity画布，非MainActivity或系统字号。受控文件往返不是新的SAF验证。API26、真机/OEM、正式发行/Release/稳定签名/真实个人记录、系统200%/完整IME/TalkBack、真实午夜、长期大规模性能、新SAF及schema3/format2真实进程终止、LMK、断电仍NOT_RUN。旧SWITCHING用例不替代M12真实故障门禁，Q06不整体CLOSED。原RED、FAIL、UNKNOWN、NOT_RUN和9条Lint提示不删除、不改写。

**交易保护不解除：**已参与交易项目不能单独删除、改取得/既有结束日期或改单笔成组金额/日期；名称/类别/备注、普通后续金额、首次正常结束与关系仍依原规则。整组纠错/撤销仍未实现、未另行授权；冻结不把该限制自动CLOSED。

M10继续PENDING / DEFERRED，三历史JPEG FAIL与保留现场不变；M13-T06继续NOT_RUN / DEFERRED，G1未整体通过；Q03～Q06及未来验收期限保持。冻结登记后停止，不操作设备、不合并main、不启动M17。

## M16候选执行登记（2026-10-21）

从M15冻结回写`661d2b5addc4eca377756f08edb84668ba700736`，在`codex/m16-replacement-trade-in`实施。以下是Codex实际运行证据，**不是独立验收或冻结**。[集中报告](evidence/M16-verification.md)、[精确方法/身份统计](evidence/m16/results.json)、[证据入口](evidence/m16/README.md)。

| 案例 | 本轮实际结果与边界 |
|---|---|
| M16-T01 one_predecessor_successor_no_cycles | PASS：纯规则及真实Room验证A→B→C，自指/环/多前后任/缺项/陈旧事实拒绝。 |
| M16-T02 link_does_not_end_old | PASS：建链/断链不改变金额和生命周期。 |
| M16-T03 trade_in_balance | PASS：旧回收3000、新现金5000＋折抵3000，新成本8000、净新增5000；真实三笔及零现金/同日。 |
| M16-T04 trade_in_all_or_nothing | PASS：六处SQL故障回滚、身份/完整事实CAS、精确重放/冲突、用户确认的成组保护。 |
| M16-T05 delete_middle_does_not_guess | PASS：普通中间项目删除只断相关边、不重连，失败回滚及陈旧关系拒绝。交易项目删除按用户确认禁止。 |
| M16-T06 v1_backup_upgrade | PASS：真实v1原件校验升级、v2八表/关系/组/设置/媒体往返、损坏保护旧集；旧schema2＋五字段proof＋SWITCHING生产打开兼容。 |

最终**252 JVM PASS＝46新增＋206相关回归；50 Android PASS＝迁移5＋交易19＋备份5＋VM3＋页面旅程2＋代作用域3＋首页6＋恢复守卫7**，总302个唯一方法，0失败/错误/跳过。重复运行不重复计数。最终构建6m51s、Lint0错误9警告、App/Test及最终实际运行字节/同签名一致；schema1/2原blob不改。7张截图来自隔离ComponentActivity中的真实LithicApp/Nav3，360dp及02/04两张200%是LocalDensity画布，不是MainActivity/系统字号。

断组导出、同刻提交时间与pending比较返回上下文有真实RED和最终GREEN，初期缺实现/编译/证书环境失败也如实保留；不是通过改原断言获取PASS。用户确认的整组纠错/撤销排除边界、所有历史NOTES保留。仅原API36/x86_64 5672，同证书App/Test各更新3次、仅新合成根，原实例继续在线且no_backup为空，默认账本/其它设备未访问。

**NOT_RUN / NOTES：**API26、真机/OEM、Release/稳定签名、真实个人记录、系统200%/完整IME/TalkBack、真实午夜、大规模长期性能、新SAF及本轮schema3/format2真实终止/断电/LMK。旧SWITCHING用例是兼容测试，不等于重跑M12三个真实终止点；Q06不整体关闭。M10三历史JPEG FAIL/延期、M13-T06延期/G1未整体通过、Q03～Q06及未来集成期限不变。等待ChatGPT，不进入M17。

## M15正式冻结登记（2026-10-21）

**M15 — ACCEPTED / FROZEN — PASS WITH NOTES。** 来源是用户本轮正式转交的ChatGPT独立验收结论；评审提交`ec0b7286b4ca41f0eac84fd4f0b6885c27c5a473`，评审/回写分支`codex/m15-history-review`，修改前本地/远端HEAD均为该提交且工作区干净。下方原M15执行登记、原报告/结果/checkpoint与实现时的候选记录保留历史语境，其当前验收效力由本节正式结论更新，不回填旧PASS或改写原件。

| 获独立认可案例 | 限定冻结范围 |
|---|---|
| M15-T01 review_includes_still_active | 8年仍持有项目参与全局陪伴榜，当前与历史共同回看。 |
| M15-T02 history_excludes_paused | 档案仅已结束项目/STOPPED订阅，暂停、到期未停止、未来停止不提前归档。 |
| M15-T03 types_rank_separately | 两类型分开；项目陪伴/日均榜、订阅折算时长榜，不跨类型评分。 |
| M15-T04 current_final_labels | 当前/已结束截至今天逐行标注；后续真实结算改变成本、不延长持有天数；写后/跨日/返回前台刷新。 |
| M15-T05 zero_negative_are_honest | 零/负值及未展示舍入值真实排序，跨50条与稳定ID无遗漏；不可计算项不伪造0。 |
| M15-T06 subscription_time_label | 明确折算付费时长；3650元年付当天标准365天、历史10元/天、实际1天，暂停currentRate0不冒充历史平均。 |

独立认可此前真实证据：**133 JVM PASS＝29新增＋104受影响回归；32 Android PASS＝20本模块＋12相关回归；7张隔离截图**，截图不另计测试方法。原Lint0错误9警告、Debug App/Test构建记录保留。[原结果及APK身份](evidence/m15/results.json)、[JUnit逐方法/XML](evidence/m15/jvm/results.json)、[Android本模块](evidence/m15/android-core/summary.json)、[相关回归](evidence/m15/android-regression/summary.json)、[截图索引](evidence/m15/screenshots/index.json)、[原集中报告](evidence/M15-verification.md)。本轮只核对既有文档、证据与Git，不重新运行任何产品测试、构建、APK或设备核查。

**NOTES / 验证边界保留：**仅原API36/x86_64的5672隔离合成环境；真实LithicApp/Nav3由隔离ComponentActivity承载，非MainActivity/正式发行。360dp和后2图LocalDensity 200%不等于系统字号验证；跨日为可变BusinessClock＋前台生命周期，不是真实午夜；两次format1往返为受控文件目标，不是新SAF实测。全局排名读全部相关事实后再提供50条窗口，计算/内存随规模增长，不声称固定50条开销。API26/真机/OEM/Release/稳定签名、默认账本与真实个人数据、系统200%/完整IME/TalkBack、真实午夜、长期规模性能、新SAF、LMK/断电及M12故障扩展仍NOT_RUN。全部RED、准备失败、UNKNOWN、历史NOT_RUN、9条Lint警告及其他NOTES原样保留，不将本次独立验收登记为新测试PASS；此前设备结束状态只作历史，本轮未访问、操作、清理或停止任何设备。

M10 **PENDING / DEFERRED**及三历史JPEG FAIL、M13-T06 **NOT_RUN / DEFERRED**、G1未整体通过、UI-C1原冻结、Q03～Q06与全部历史NOTES保持；本次不自动关闭任何NOTE，不扩大发行/真实数据准入。原130条归档案例不改，M16未授权，不合并main。

## M15执行登记（2026-10-21）

仅本轮实现者实际执行结果，**READY_FOR_REVIEW**，独立验收PENDING。原130案例及历史报告不改。[集中报告](evidence/M15-verification.md)、[唯一方法/产物身份](evidence/m15/results.json)、[原始证据与7图](evidence/m15/README.md)。

| 原案例 | 本轮结果与边界 |
|---|---|
| M15-T01 review_includes_still_active | PASS：8年仍ACTIVE项目参与全局陪伴榜，真实Room与页面均验证。 |
| M15-T02 history_excludes_paused | PASS：原历史过滤仅已结束/STOPPED，PAUSED/EXPIRED/未来停止排除，实际档案与详情。 |
| M15-T03 types_rank_separately | PASS：项目两种排名/订阅一个折算时长榜，类型分开，不评分。 |
| M15-T04 current_final_labels | PASS：逐行当前/已结束截至今天；结束后退款写后改变成本，不增持有天数，跨日及返回前台刷新。 |
| M15-T05 zero_negative_are_honest | PASS：零/负值保留、BigDecimal未展示舍入排序、105条跨50与同值ID无遗漏，不可计算不伪造0。 |
| M15-T06 subscription_time_label | PASS：年付3650今天生效，标准365、历史平均10、实际1天；暂停currentRate0不替代历史平均。 |

**133 JVM PASS＝29新增＋104受影响回归；32 Android PASS＝Room12＋备份换代1＋Compose5＋真实Nav3旅程2＋相关首页/代作用域/守卫12**，0失败/错误/跳过。最终构建5m10s、Lint0错误9警告、Debug App/Test，RED及环境/编译准备失败保留。两格式v1受控文件往返（Repository与真实UI各一次）验证七表、设置、媒体原字节、旧代拒读及新代重算；不扩称新的SAF验证。

仅API36/x86_64原5672，App/Test UID10216/10217、同既有Debug证书保留数据更新各1，结束仍原实例在线、no_backup为空、设备设置不变。7图均隔离ComponentActivity中的真实LithicApp/Nav3，360dp；后2为LocalDensity 200%。不是MainActivity/系统大字/正式发行验证。API26/真机/OEM/Release、真实午夜、系统IME/TalkBack、长年性能、LMK/断电等NOT_RUN。M10三历史JPEG FAIL/延期、M13-T06/G1及全部历史NOTE保持。停止等待ChatGPT，不进入M16。

## M14正式冻结登记（2026-10-21）

**M14 — ACCEPTED / FROZEN — PASS WITH NOTES。** 来源是用户本轮正式转交的ChatGPT独立复验，评审提交`bf621689180b4d5d86e42f8136e4aafb1971c5fa`，评审/回写分支`codex/m14-category-search`。下方初交付与补验执行登记、原报告和checkpoint保留当时状态，不回填历史PASS。

| 获独立认可的案例 | 冻结范围 |
|---|---|
| M14-T01 | 同类型类别合并的事务原子性、当前/历史引用、金额/付款/状态及总额保护、失败完整回滚；真实竞争仍NOT_RUN。 |
| M14-T02 | 跨类型拒绝、默认类别保护、标准名冲突、陈旧确认、重复操作与旧代拒写。 |
| M14-T03 | 当前/历史名称搜索、真实状态和精确详情路由；复用冻结引擎。 |
| M14-T04 | 参数化LIKE与特殊字符字面匹配，不扩为备注/金额搜索。 |
| M14-T05 | 项目/订阅默认排序、稳定ID次排序、分页无重复遗漏。 |

独立认可此前实际证据：**136项JVM PASS**；**最终29项Android PASS＝原28＋截图旅程1**（类别11、搜索/排序12、完整format1往返1、原Compose4、旅程1），0失败/未运行；**8张截图**不另计测试方法。原Lint0错误9警告。原[136项统计](evidence/m14/results.json)、[最终29项及APK身份](evidence/m14-android/results.json)、[原始结果/图库](evidence/m14-android/README.md)、[补验报告](evidence/M14-Android-verification.md)保留；本轮只读取这些既有资料及Git，不执行新的测试、构建、APK或设备操作。

**NOTES / 未扩大范围：**仅API36/x86_64隔离合成验证；截图来自隔离ComponentActivity中的真实LithicApp/Nav3，非MainActivity/正式发行，360dp及搜索200%仅LocalDensity画布，类别/合并页200%、系统字号设置、真实IME/完整TalkBack仍NOT_RUN。备份往返使用受控文件目标，不是新SAF实测；真实并发压力、API26、真机/OEM、Release/稳定签名和真实账本升级仍NOT_RUN。保留三处原测试接缝修正、全部历史RED/UNKNOWN/NOT_RUN和9条Lint提示，不把文档登记或ChatGPT复验写成重新运行测试的PASS。补验报告及索引的构建日志原位于未跟踪本地build目录；本次仅将两份实际原日志原字节归档至logs目录并修正链接，原本地日志仍保留，没有新运行、重造或用其他批次日志替代。

M10 **PENDING / DEFERRED**及三历史JPEG FAIL、M13-T06 **NOT_RUN / DEFERRED**、G1未整体通过、UI-C1原冻结、Q03～Q06及全部历史NOTES保持。M14冻结不授予M15、发行、真实数据录入或任何模拟器操作权限。本轮只提交文档并停止，不合并main。

## M11正式独立冻结登记（2026-10-20）

- 来源：用户本轮正式转交ChatGPT独立验收，M11 **ACCEPTED / FROZEN — PASS WITH NOTES**。
- 评审HEAD：`635e33a57c66c286fa5c19f009a5565a1e70b8e2`，分支`codex/m11b-final-export-validation`，本地/远端已核对。
- 限定范围：冻结M09/schema2、format1完整手动导出、登记JPEG/PNG原字节备份及真实Android SAF用户流程。
- NOTES：149 JVM及17项Android（16＋1分两Test身份）的既有证据、原RED/FAIL/UNKNOWN/NOT_RUN不改；API26/真机/OEM/Release、提供方磁盘满/权限撤回/断电、完整格式语义穷尽及M12往返未因此PASS。未知中断临时文件保守保留，SAF阻塞不保证即时取消，JPEG字节/容器不冒充严格熵解码。
- 本节只是独立结论登记，未因文档回写重跑测试。原M11-A限定冻结保留；M10 PENDING / DEFERRED和UI-C1冻结不变，M12本轮获开发授权但未经独立验收不得FROZEN。


本文件记录 M00～M09 实际执行与独立验收历史，以及M11-A/M11/M12的执行与正式冻结登记；[原130条Markdown矩阵](planning/original-v1/lithic_v1_plan/docs/ACCEPTANCE_MATRIX.md)与[原JSON](planning/original-v1/lithic_v1_plan/docs/acceptance-cases.json)完整归档且全 NOT_RUN，不在原件改结果。

日期：2026-10-20。执行项 PASS 表示当时实际执行通过，NOT_RUN 表示未执行，历史 PENDING 表示当时待独立确认。M00～M09正式结论均为 **ACCEPTED / FROZEN — PASS WITH NOTES**，详见各模块独立验收登记；M10当前 **PENDING / DEFERRED**，UI-C1独立冻结结论保留于其分支。M11-A仅备份核心已由用户转交独立冻结结论；M11已按其独立结论正式限定冻结；文末候选READY_FOR_REVIEW保留为历史执行记录；M12已按[本次独立结论](#m12正式冻结登记2026-10-20)正式冻结（PASS WITH NOTES）。原130案例、16组完整跨模块补测与全部历史NOTES保持原状态；各次新增执行分节记录，不回填历史。

## 独立验收记录

- 记录日期：2026-10-11。
- 结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立验收结论；本次Codex仅回写结果，不自行授予验收，也不重新运行或代替独立审阅。
- 仓库：cc7279694-debug/property。
- 评审分支：feat/m00-planning-freeze。
- 评审HEAD：59ba290cba69e1e77af7d0376a8b43aaabbf5f88；回写前已实际确认本地及远端功能分支均匹配。
- 独立核对范围：原始ZIP、43个归档原件、24份模块计划、130条案例、规格文件、146个本地链接和Git变更范围。
- Notes：Q01已关闭；Q02～Q07按[SPEC_ISSUES](SPEC_ISSUES.md)保留状态和模块期限，不阻塞M00冻结与M01启动准备。具体建议未因此自动获得批准，工程未实施；M01仍需另行授权。

| 问题 | 保留状态 | 保留处理期限 |
|---|---|---|
| Q02 | PROPOSED | M02前明确，关联M17/M18 |
| Q03 | PARTIALLY_RESOLVED_BY_SOURCE | 长期100/1000日节点M21前明确，不整体阻塞M03；首日口径有原案例依据，实现测试仍NOT_RUN |
| Q04 | PROPOSED | M20前明确，关联M03/M21 |
| Q05 | PROPOSED | M10/M12前明确，关联M11 |
| Q06 | ENGINEERING_REQUIRED | M04设计、M12实施与故障验证；尚未设计/实现/验证 |
| Q07 | OPEN | M02等受影响模块执行前明确通用模板适用性 |

本次授权只回写当前状态与本登记、检查并提交/推送功能分支；未修改产品/补测结果或问题清单。规划及历史checkpoint中的“未冻结/PENDING”保留为当时交付记录，当前正式结论见本节。

## M00 规划补齐交付的文档验收

以下保留评审基准59ba290时的实际检查记录；其中“未冻结/PENDING”是补齐交付时状态，不是当前独立验收结论。

| ID | 条件 | 本轮结果 | 证据 / 说明 |
|---|---|---|---|
| M00-D01 | 文档入口清晰、当前记忆职责唯一 | PASS | 57份Markdown、146个本地文件链接、0失效；归档旧同名文件只作来源，人工职责核对通过 |
| M00-D02 | 产品、原生路线和明确不做清单保持 | PASS | 六规格及原DECISIONS/SOURCES规范化全文均匹配阅读版；核对当前PROJECT/AGENTS，D01～D15明确是原规划补充默认 |
| M00-D03 | 完整读取M00计划、130矩阵及执行规范 | PASS | 实际读取ZIP43文件，CRC/严格UTF-8/3 JSON解析与大小/哈希/清单一致；原计划、矩阵、模板已接入并审查 |
| M00-D04 | 24模块顺序、原依赖、边界与迁移配对 | PASS | 24唯一编号按M00～M23；计划/index/ROADMAP直接依赖一致、无环；130原案例在计划/矩阵/JSON逐条一致 |
| M00-D05 | 问题来源、建议与补测明确，不改变规则 | PASS | Q01关闭、Q03部分来源澄清；16组补测仍NOT_RUN，Q07登记；人工核对引用/输入/闸门，无新产品功能 |
| M00-D06 | 状态可恢复、停止边界与独立验收明确 | PASS | 唯一实时CURRENT_STATE；本轮未冻结，M01～M23 PLANNED且NOT_STARTED；历史checkpoint未改 |
| M00-D07 | ZIP/43原文件/阅读版保持来源字节 | PASS | 附件/归档ZIP SHA-256一致，43原件逐字节/CRC/manifest匹配；45原件文件的工作blob与暂存blob完全相同 |
| M00-D08 | 只有M00资料和文档，无Android/业务/旧仓库改动 | PASS | 本轮60项暂存变更：12修改、48新增、0删除；全部为Markdown/规划JSON/原ZIP及.gitattributes，无业务代码 |
| M00-D09 | 链接、结构、敏感信息与差异检查 | PASS | 146链接0失效、57文档代码围栏无失衡、敏感模式0命中；人工差异审查及新写文档git diff检查通过，归档硬换行原样保留 |
| M00-D10 | 现有功能分支提交及推送、main未合并 | PASS | 主体daf9615792cf85bf2521a98879b8654fbd79925e已推送，实际功能远端SHA匹配、工作区干净、main仍145326d；随后只登记交付证据 |
| M00-D11 | 等待独立验收，不自动ACCEPTED/FROZEN/M01 | PASS | 本轮仅READY_FOR_REVIEW；独立验收仍PENDING，M01须用户另行授权 |

## 原 M00 三项文档案例的本轮执行记录

记录独立于归档NOT_RUN字段；不把本次结果写回原130案例。

| 原ID | 本次实际检查 | 本轮结果 |
|---|---|---|
| M00-T01 | PROJECT/AGENTS明确禁止旧账户/股票/量化/云同步、不复制WHOCARE；60项本仓库文档变更范围已核对 | PASS |
| M00-T02 | 当前M01～M23 PLANNED且NOT_STARTED，无APK/工程/伪造产品PASS；原模块索引仍24项PLANNED | PASS |
| M00-T03 | 当前DECISIONS逐项继承原D01～D15，15行与原默认解释一致，明确不冒称用户逐条确认 | PASS |

## M00 历史验证范围与尚未执行

本轮已检查来源完整性、路径安全、逐字节一致、相对链接、24模块/依赖、130原案例一致性、六规格阅读版一致、当前文档边界及主体Git提交/推送。11项文档检查和3项当前M00文档案例均PASS；详细实际证据见[本轮checkpoint](checkpoints/2026-10-11-M00-planning-package.md)。

原130案例（包括归档M00记录）全部NOT_RUN；其中适用M00三项文档检查仅在上表记录本轮结果。M01～M23产品验证及16组补测未执行。

TypeScript、ESLint、Unit Test、Integration Test、Build、Android Build、APK Manual Verification：NOT_RUN。原因：M00仅文档，没有产品工程或业务代码；不为满足原通用完成模板提前开发未来备份或运行M01。

当前独立验收：ACCEPTED / FROZEN — PASS WITH NOTES，依据见上方独立验收记录。原130案例与16组补测仍全部NOT_RUN；M01～M23仍PLANNED且未实施，本次状态回写后停止。

## M01 独立验收记录（2026-10-11）

- 结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立验收结论；Codex本次仅回写，不自行授予验收或代替独立审阅。
- 仓库：cc7279694-debug/property。
- 评审分支：feat/m01-android-skeleton。
- 评审HEAD：`ca8499e3d3cb2e5bac3ab29a231f399636899c19`；回写前实际确认本地与远端功能分支均匹配，工作区干净。
- 独立审查确认：单 `:app`、单Activity、Compose/Navigation 3空壳符合M01范围；20项已提交构建输入SHA-256独立比对一致；正式与Debug applicationId正确；Manifest、系统备份排除配置及权限核查脚本符合阶段要求；构建、Lint、模拟器安装和离线冷启动的既有执行证据已审阅；Git范围26新增、13修改、0删除，未进入M02或M05。

保留NOTES：

1. 模拟器证据已审阅，独立审查未重新安装APK；不新增设备验证PASS。
2. 真实手机、API26运行、OEM备份行为仍NOT_RUN；其他未运行产品验证及16组补测继续NOT_RUN。
3. Lint的12条版本更新提示暂不处理，未抑制或升级依赖。
4. Q02～Q07继续按既有状态和模块期限处理，不因M01冻结而关闭或标记已实施。
5. M02执行前必须明确Q02历史观察日公式和Q07通用验收模板适用性，且须用户另行授权；本次不开始M02。

本次授权只更新唯一CURRENT_STATE和本登记，检查差异后提交/推送当前M01功能分支。原始规划、130案例、问题清单和历史checkpoint不改；历史checkpoint的READY_FOR_REVIEW/PENDING是当时交付状态，本节记录随后获得的独立结论。本次回写的最终提交SHA及远端校验以Git日志/最终报告为准。

## M01 当前执行验收（2026-10-11）

授权：此前用户M01执行指令；执行基线 `77e61b653ebf1ce4838b4acce4b5b366e17891f4`；功能分支 `feat/m01-android-skeleton`。M00独立验收历史不变。本节保留Codex既有实际验证结果，交付时为READY_FOR_REVIEW；随后用户转交的M01独立结论为 **ACCEPTED / FROZEN — PASS WITH NOTES**，依据见上节，本次未重跑测试。

| 原案例 | 实际结果 | 证据 / 边界 |
|---|---|---|
| M01-T01 empty_app_cold_starts_offline | PASS（模拟器） | 专用API36/Android16模拟器安装Success；设备airplane=1、wifi=0、mobile_data=0、Active default network=none；force-stop后无进程，am start -W为COLD/Status ok；截图/UI三个文案核对；返回与重新冷启动无应用崩溃；网络状态恢复。未验证用户真机或API26 |
| M01-T02 merged_manifest_has_no_internet | PASS | aapt2检查最终APK及合并Manifest：无INTERNET、两种精确闹钟、三种广泛存储、通知/相机/照片权限；唯一签名级内部接收器权限来自AndroidX Core 1.16.0；allowBackup=false，旧规则/cloud-backup/device-transfer编译资源各排除九域；下载字体自动初始化入口已从最终包移除；不等于所有OEM行为验证 |
| M01-T03 debug_id_is_isolated | PASS | 最终APK为com.cooper.lithic.debug，名称砺石 LITHIC Debug，versionCode=1、versionName=0.1.0-m01-debug；正式namespace/applicationId配置com.cooper.lithic；APK为本地Android Debug证书，未构建正式发行包 |
| M01-T04 versions_are_locked | PASS | 精确稳定目录与官方Wrapper/JAR/分发校验值核对；Gradle8.14.3/JDK17实际输出；根assembleDebug + lintDebug退出0；debugRuntimeClasspath实际解析一致。版本来源见工具链锁，不声称重验现有缓存原ZIP |

具体输出、日志位置、APK/截图哈希及命令见[M01证据](evidence/M01-verification.md)与[M01 checkpoint](checkpoints/2026-10-11-M01.md)。模拟器已安装的base.apk与最终本地APK SHA-256一致。

另行验证：lintDebug PASS（0 errors / 12版本更新提示，未抑制）；最终APK apksigner verify退出0，v2签名通过；原ZIP/43原件字节、130 NOT_RUN案例、Q02～Q07原状态及补测未改变；文档链接/差异/敏感信息检查在交付checkpoint记录。

NOT_RUN：业务Unit/Integration Test、Android Instrumentation Test、真实手机、API26运行、OEM备份/迁移行为、业务schema/migration/完整备份往返。原因：M01为原生空壳，无业务数据与对应实现；完整备份往返不适用且未实现，不标Backup PASS。TypeScript/ESLint不适用于本原生工程。

原130案例保持NOT_RUN（含归档M01），既有执行结果只登记本节；M02～M23及16组补测保持NOT_RUN。Q02～Q06状态/期限不变，Q07只明确M01的模板适用边界，不关闭其他模块。M01已按用户转交的独立结论冻结；M02未授权，执行前须明确Q02/Q07，不自动进入。

## M02 独立验收记录（2026-10-11）

- 结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立代码验收结论；Codex本次仅回写，不自行授予验收，也不以本地辅助审查代替独立验收。
- 仓库：`cc7279694-debug/property`。
- 评审分支：`feat/m02-project-cost-engine`。
- 评审HEAD：`ff1a898eaf4da5068c36af3e1c0bdcbe61c0b1f7`；回写前实际确认本地与远端M02功能分支均匹配，工作区干净。
- 独立检查范围：金额解析、日期计算、项目成本引擎、31项JUnit测试源码、RED/GREEN证据、核心文件哈希及Git范围；未发现阻塞性代码问题。

保留NOTES及后续边界：

1. **本次独立审查未在用户Windows本机重新运行Gradle测试。**本次Codex状态回写同样不重跑Gradle、构建或设备验证；下节保留此前实际执行结果，不把代码/证据审查记为新测试PASS。
2. 原130条规划案例和16组完整跨模块补测仍NOT_RUN，原件不可变；Q02的M17/M18历史视图/总成本集成仍NOT_RUN，不因领域冻结而提前通过。
3. Q03～Q06状态和处理期限保留：Q03长期100/1000日节点M21前，Q04提醒边界M20前，Q05图片往返M10/M12前，Q06持久回退M04设计/M12实施与故障验证，均未在本次实施或关闭。
4. 未运行的Instrumentation、真机/API26/OEM、数据库/迁移/业务备份往返和其他未来能力保持原NOT_RUN / N/A边界；12条既有Lint版本提示不处理。
5. M03尚未授权；本次只更新唯一CURRENT_STATE与本登记、提交并推送当前M02分支，不修改业务代码/WHOCARE，不合并main或进入M03。

M02历史checkpoint与证据的READY_FOR_REVIEW/PENDING是当时交付状态，不改写原记录；当前正式结论见本节。验收回写提交自身SHA、推送与最终远端校验以Git日志/最终报告为准。

## M02 当前执行验收（2026-10-11）

此前用户已授权M02并正式确认Q02/Q07。实施起始基线 `a25a06899bd157988c3a4466d7162d8616fb0249`，分支 `feat/m02-project-cost-engine`；只实现纯Kotlin领域规则，保留M01空壳。本节保留既有实际验证结果，交付时为READY_FOR_REVIEW；随后用户转交的独立结论为 **ACCEPTED / FROZEN — PASS WITH NOTES**，依据见上节，本次未重跑测试。历史阶段限制与本次状态回写授权区分，实施契约见[M02契约](planning/M02_CONTRACT.md)。

| 原案例 / 当前补测 | 实际结果 | 实际断言与边界 |
|---|---|---|
| M02-T01 `same_day_is_one_day` | PASS | 01-01投入1000元/持有1天/1000元每天，初始投入只累计一次 |
| M02-T02 `dated_event_does_not_rewrite_earlier_days` | PASS | 01-11追加200，01-10仍1000/10=100；01-11为1200/11的DECIMAL128结果 |
| M02-T03 `end_day_removes_current_contribution` | PASS | 01-10结束/完整10天/日均100，结束日日末贡献false |
| M02-T04 `post_end_refund_keeps_duration` | PASS | 结束后01-20收回200，净800/10=80；分母固定/不重新活跃 |
| M02-T05 `money_precision_and_zero` | PASS | 0.10+0.20经过解析和实际引擎得30分/0.30元，0有效；超精度/单笔超限拒绝 |
| M02-T06 `negative_cost_is_not_clamped` | PASS | 投入5000收回6000，净-1000元/-100元每天，不钳0 |
| Q02-V01（本轮领域部分） | PASS | 已结束快照回看开始前无有效日均/贡献，01-02为2天/500，01-09为9天/1000÷9 |
| Q02-V02（本轮领域部分） | PASS | 01-10/19为100、01-20/31为80，10天固定，结束日起无贡献 |
| Q02-V03（本轮领域部分） | PASS | 同日开始结束为1天/日末退出，补录结算仅按发生日改变后续结果，不延长/重启 |

实际测试：31通过/0失败/0错误/0跳过，7金额解析+2时钟+22项目引擎。RED为同一标准命令31项/1通过/30 AssertionError失败；初次classloading环境错误不算RED。覆盖非法结束日、预付款、分期、独立退款、负净额、同日/乱序事件、单笔范围及累计Long溢出、0.005/-0.005、闰日与Long日期跨度；固定日期测试，不模拟算法自身。测试源码RED/GREEN哈希相同。

构建和Lint：`:app:lintDebug :app:assembleDebug`退出0，0 errors / 12既有版本提示，不升级/抑制。最终APK实际核对身份/权限/编译备份资源/Wrapper及领域类，不增加运行依赖；测试JUnit4.13.2/Hamcrest1.3没有进入运行classpath/APK。27个最终输入SHA-256、逐项测试结果、日志/XML/产物哈希见[M02证据](evidence/M02-verification.md)与[JSON](evidence/M02-verification.json)。

原ZIP、43原件与阅读版字节/CRC/哈希核对PASS；130原案例及16组完整跨模块补测仍NOT_RUN。Q02/Q07为USER_CONFIRMED，Q02本轮领域已实现，但M17/M18集成尚未执行；Q03～Q06状态及期限逐行与基线一致，不提前关闭。M00/M01历史checkpoint及既有证据不改写。

NOT_RUN / N/A：Instrumentation、设备安装/断网冷启动、业务Room/SQLite/迁移/Repository/完整备份往返，本轮纯领域范围没有对应能力；真机/API26/OEM保留M01未测NOTES；展示舍入/页面和M03～M23尚未实施。TypeScript/ESLint不适用原生Kotlin。NO-SOURCE不计测试PASS。

既有Git/文档检查与交付证据见[M02 checkpoint](checkpoints/2026-10-11-M02.md)。M02已按本次用户转交的独立结论冻结；本次仅回写两份状态文档后停止，不进入M03或合并main。

## M03 当前执行验收（2026-10-11）

授权：用户本轮内联M03请求；基线 `0169c76bbf0946a0400b96d00124b3de9aea1cc0`，来源feat/m02-project-cost-engine，当前feat/m03-subscription-engine。M00～M02已冻结，历史限制是当时授权记录，本轮M03已授权、M04未授权。当前 **READY_FOR_REVIEW**，ChatGPT独立验收 **PENDING**，不自行冻结。

| 原案例 / 当前领域补测 | 实际结果 | 证据与边界 |
|---|---|---|
| M03-T01 monthly_standard_rate | PASS | 150/30=5，真实31天覆盖独立；实际31天、标准30 |
| M03-T02 annual_purchase_not_elapsed_year | PASS | 3650/365=10，首日实际1、第100日100/年末365，到期不增；未实现里程碑通知 |
| M03-T03 prepaid_next_rate_does_not_start_early | PASS | 1月20实付下一期180，当前仍5，2月1生效变6；历史330/标准60=5.5 |
| M03-T04 expired_has_zero_current_rate | PASS | 排他到期日EXPIRED/贡献0/当前价格null，实际31天封顶；不依赖后台 |
| M03-T05 pause_does_not_erase_paid_history | PASS | 1月10暂停、20恢复；付款150/标准30保留，暂停实际9、恢复日10/月末21 |
| M03-T06 overlaps_rejected | PASS | 重叠拒绝、邻接允许，业务今天验证未来付款拒绝；历史观察不误拒后来付款 |
| Q03-V01（仅订阅日期领域） | PASS | 首日1/4月10日100/365及到期不增；M21触发尚NOT_RUN |
| Q03-V02（仅订阅日期领域） | PASS | 暂停10日期年末355；已真实续费邻接到2027-01-10累计365；跨模块仍NOT_RUN |
| Q03-V04（仅订阅闰日日期） | PASS | 2024-02-29首日1，2025-02-27第365日；长期节点/导入通知水位仍NOT_RUN |
| Q04-V01（仅成本区间） | PASS | [09-15,10-15)的150/月；10-15至10-20提醒日贡献0/实际30不增；提醒资格仍PROPOSED、M20前 |

实际RED37项/3通过/34 AssertionError失败/0错误/0跳过；同一测试源码GREEN37通过。完整 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 退出0，68测试全通过（金额7/时钟2/项目22/订阅37）；Lint0 errors/12既有提示不处理，构建成功。没有编译/环境错误被冒充RED，也不把NO-SOURCE计PASS。覆盖历史付款、预付未来、改周期、暂停恢复/停止、同日冲突/重排、覆盖空档、独立提醒边界、精度、闰日与极限Long日期。

最终APK检查通过：身份/权限/编译备份资源/Wrapper，17个关键领域类描述符存在且测试框架/类未打包，v2签名验证通过。31项最终输入哈希（27继承＋4新增）、逐项测试/XML/日志/产物哈希与实际范围见[M03证据](evidence/M03-verification.md)、[JSON](evidence/M03-verification.json)及[checkpoint](checkpoints/2026-10-11-M03.md)。182条唯一运行依赖坐标线与M02一致，无权限/依赖变化。未安装或公开上传APK。

原ZIP/43原件与阅读版字节/CRC/hash通过，130原案例和16完整跨模块补测保留NOT_RUN；上表仅当前领域结果，不改原件。Q03～Q06状态/期限逐行保持，未关闭提醒/长期节点/图片/回退问题，Q02 M17/M18仍NOT_RUN。

NOT_RUN / N/A：本轮设备安装/断网冷启动/Instrumentation、Room/Repository/迁移/完整业务备份、专门M03订阅合计Long溢出执行；没有对应Android交互或业务存储，源码显式检查不等于该行为PASS。真实手机/API26/OEM继承M01未测NOTES；通知/里程碑/订阅UI与未来集成仍未实施。TypeScript/ESLint不适用。M03等待独立验收，本轮交付后停止，不进入M04或合并main。

## M03 独立源码审查与补测复验（2026-10-11）

用户转交ChatGPT独立源码审查：**CHANGES REQUESTED — 仅补齐累计溢出测试，暂不冻结**。评审分支feat/m03-subscription-engine，评审HEAD `80cf6473c21d5933963dd55b02dfbf9f97297849`。已确认M03-T01～T06覆盖及原37＋31交付证据一致，核心逻辑暂未发现确定性计算缺陷；缺口为累计金额/标准天数溢出未实际测试。额外STOPPED再启用、多期预付默认停止风险归M09，未授权本轮改语义。

本轮从相同本地/远端评审HEAD与干净工作区继续，仅提取生产evaluate实际调用的内部累计函数并补6测试，原37与31测试源码不改。已有Math.addExact行为保留，采用边界补测/回归，未人为制造RED。完成执行状态 **READY_FOR_REVIEW — M03 (RECHECK)**，独立复验 **PENDING**；不自行授予ACCEPTED / FROZEN。

| 当前新增补测 | 实际结果 | 证据 / 边界 |
|---|---|---|
| M03-R01 金额到精确Long.MAX | PASS | 合成累计种子加合法最大单笔，得到精确MAX；另一字段正常、种子不变 |
| M03-R02 金额超过Long.MAX | PASS | 数学结果MAX＋1，重复执行均ArithmeticException，不回绕/修改种子 |
| M03-R03 标准天数到精确Long.MAX | PASS | 合成种子加合法年付365，得到精确MAX；金额仅1分 |
| M03-R04 标准天数超过Long.MAX | PASS | 数学结果MAX＋1，重复执行均ArithmeticException，不暴露部分金额更新 |
| M03-R05 5倍数近界后再续费 | PASS | MAX−367＋365=MAX−2，下一笔月付30明确溢出，前结果不变 |
| M03-R06 两笔付款/生产连接 | PASS | 内部累计与实际evaluate均420元/120标准天；编译字节码核对生产调用同函数 |

这是内部生产同路径运算边界，增量均合法Renewal；合成累计种子不代表可形成的完整合法历史，不声称海量续费端到端溢出验证。现有List的标准天数至多Int.MAX×365，不可能达Long极限，且合法周期合计为5倍数。

实际完整命令 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 退出0，74通过/0失败/0错误/0跳过（金额7/时钟2/项目22/原订阅37/新增累计6），六原案例继续回归通过。Lint0 errors/12既有提示，构建成功。最终APK身份/权限/备份资源/Wrapper与v2签名核对通过，18关键领域类存在，测试类/框架未打包；32最终输入哈希及逐项XML、日志、APK/编译调用链见[补测证据](evidence/M03-recheck-verification.md)和[JSON](evidence/M03-recheck-verification.json)。

原130案例/16完整跨模块补测NOT_RUN不变，原M03证据和上节初次交付记录保留为历史，当前补测结果在本节登记。Q03～Q06按既定期限，M09两风险[另行记录](SPEC_ISSUES.md#m09-订阅流程风险本次独立审查登记当前不实施)。设备、业务数据库/迁移/备份与通知仍NOT_RUN / N/A。本轮停止等待独立复验，不进入M04或合并main。

## M03 独立复验与正式验收记录（2026-10-11）

- 正式结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立复验正式结论；Codex本次仅回写，不自行授予验收或以本地辅助审查代替独立复验。
- 仓库：`cc7279694-debug/property`。
- 评审分支：`feat/m03-subscription-engine`。
- 评审HEAD：`8b13d20dad89f9c3e76a3d19161cf2232b36b94d`；回写前实际确认本地/远端均匹配，工作区干净。
- 独立确认：六项新增累计边界测试与生产代码使用同一累计函数；累计金额与标准天数均由Math.addExact检查溢出；失败不产生部分更新的累计结果；74个JUnit方法与提交的74条PASS结果完全对应；评审补测Git范围3新增/7修改/0删除，未越界进入M04。

保留NOTES与后续边界：

1. **本次独立审查未在用户Windows本机重新运行Gradle。**本次Codex状态回写同样不运行Gradle、构建或设备验证；既有74项结果与RED/GREEN/回归证据保持原样，不把源码/证据审查登记为新测试执行PASS。
2. 原ZIP/43归档原件、原130条规划案例及16组完整跨模块补测保留，原案例/完整补测继续NOT_RUN；历史交付、M03 checkpoint和两组M03证据不改写。内部合成累计种子的边界验证不宣称海量合法完整历史端到端测试。
3. Q03～Q06状态/期限不变：长期100/1000日节点M21前，提醒资格M20前，图片往返M10/M12前，持久回退M04设计/M12实施及故障验证。Q02的M17/M18集成仍NOT_RUN；不因领域引擎冻结而提前关闭问题或集成验证。
4. 保留[M09两项流程风险](SPEC_ISSUES.md#m09-订阅流程风险本次独立审查登记当前不实施)：STOPPED后重新启用的入口/历史/纠错边界、多期已预付默认停止边界，仍待M09明确与验证，NOT_RUN，当前未改产品语义或实施流程。
5. 设备安装/断网冷启动/Instrumentation、真机/API26/OEM、业务Room/SQLite/Repository/迁移/完整备份、M09流程及M20/M21通知等未运行验证继续NOT_RUN / N/A；12条既有Lint版本提示不处理。
6. 本次只更新唯一CURRENT_STATE和本登记、检查差异后提交/推送现有M03功能分支。M04仍须用户另行授权；不修改业务代码、WHOCARE或原规划，不合并main或进入M04。

本节正式结论取代先前CHANGES REQUESTED/复验PENDING的当前效力；旧记录完整保留为历史。回写提交自身SHA、推送与最终远端核对以Git日志/最终报告为准。

## M04 当前执行验收（2026-10-12）

用户此前正式授权M04实施；基线 `ffd066ffe0efb6c00f35526ae792b50829d3230e`，来源feat/m03-subscription-engine，分支feat/m04-room-storage-foundation。实现前已记录[契约/Q06设计](planning/M04_CONTRACT.md)。本节保留既有执行证据，交付时为READY_FOR_REVIEW/独立验收PENDING；随后正式结论为 **ACCEPTED / FROZEN — PASS WITH NOTES**，依据见下节，本次未重跑测试。原130案例原件仍NOT_RUN。

| 原案例 | 本轮实际结果 | 真实Android证据 |
|---|---|---|
| M04-T01 fk_rejects_orphan_event | PASS | 实际SQLite金额事件项目外键拒绝孤立行，表仍为空；类别/封面外键另有回归 |
| M04-T02 operation_id_is_unique | PASS | 不同事件主键复用operationId触发SQLite唯一约束，原行不变 |
| M04-T03 transaction_rolls_back | PASS | 项目已INSERT后第二步事件主键故意冲突，Room事务完整回滚新项目，旧项目/事件保持 |
| M04-T04 zone_is_persisted | PASS | 真正Preferences DataStore写入/关闭/重开读同zone；不同模拟设备时区不覆盖，损坏/缺字段/非法值拒绝 |
| M04-T05 generation_is_self_contained | PASS | DB/WAL/SHM、media、settings、ready同一私有UUID代；完整准备后AtomicFile激活、重开同身份、原代保留，异常不空库兜底 |
| M04-T06 defaults_are_typed | PASS | 两个“其他”类型/ID不同，同类normalizedName唯一；项目引用订阅类别在Repository事务内拒绝 |

完整命令 `:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain` 退出0：JVM **74 PASS**，Instrumentation **86 PASS**，均0失败/错误/跳过。Android按源码方法和XML逐项核对：App装配6、Room34、媒体/类别metadata9、Settings13、generation24；仅专用API36 emulator-5596，真实Android数据库/文件/DataStore。原74领域测试与21项既有源码/资源测试输入保持；本轮未修改M01 Activity/导航/资源/Manifest或M02/M03计算规则。

真实行为RED依次61项/55失败（53 AssertionError、2 ComparisonFailure）、补充20/19、生命周期3/3、启动/旧库9/8；已有正确行为直接回归。两次编译夹具问题分别为异类arrayOf推断与RoomDatabase非Closeable，不计有效RED。初始RED之后增加边界/清理夹具，测试文件整体哈希并非四轮相同；App装配6项RED/GREEN断言保持，最终全部86实际通过。完整逐项结果/输入/日志/XML哈希见[证据JSON](evidence/M04-verification.json)。

另测PASS：同键精确重试/异内容冲突、并发串行、金额0/真实收回负净成本、Money越界/业务未来日期、UUID/关系、LocalDate与纳秒Instant往返、关闭重开、四表读取事务对独立SQLite写入保持一致、媒体路径/尺寸/长度/hash格式、指针/ready/设置/已有库异常拒绝且保留。实际发现并修复存在性lease竞态、IO返回取消资源泄漏、应用首次启动装配、已有v0/异库原地修改；真实页面腐败回归保留原数据库，live WAL预检通过。没有SQL CHECK或跨文件事务声明。

Lint **0 errors / 13版本更新提示**，未抑制/顺便升级；Room schema v1实际KSP导出4表/30字段/7索引/3外键，JSON已纳入交付。精确新增依赖、兼容构建和实际传递变化见[工具链](TOOLCHAIN_LOCK.md)；无网络权限或业务页面。

最终APK 11,809,696字节、SHA-256 `6a9f67a6fb84da44bdca1198a85afe1c1ca5aecc80042fa328bf7f50d3a00a10`。APK/合并Manifest/备份排除资源/Wrapper、生产存储类及无测试框架、v2签名检查PASS。专用模拟器安装、两次断网COLD/Status ok启动、进程重启同代/设置hash不变、已安装APKhash一致、占位UI截图/XML核对PASS；网络恢复，未操作手机或其他AVD，未公开发布。

NOT_RUN：真机/API26/OEM，M08真实v1→v2迁移、M10图片、M11备份格式、M12导入/切换/回退/杀进程/共同校验/清理。Q06保持ENGINEERING_REQUIRED，仅M04设计/初始实测完成；Q06-V01～V03全流程、Q03～Q05后续期限、M09两项风险、16组完整跨模块补测均保留NOT_RUN。正式用户存储错误界面留M05；TypeScript/ESLint不适用，NO-SOURCE不计PASS。M00～M03独立审阅NOTES不改写。

文件、scope/敏感信息/来源/链接及Git交付见[M04报告](evidence/M04-verification.md)和[checkpoint](checkpoints/2026-10-12-M04.md)。只推M04分支；M05需M01/M04独立验收及用户另行授权，本轮不继续。

## M04 独立代码验收与正式冻结记录（2026-10-12）

- 正式结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立代码验收结论；本次Codex仅回写，不自行授予冻结或以内部辅助审查替代独立验收。
- 仓库：`cc7279694-debug/property`。
- 评审分支：`feat/m04-room-storage-foundation`。
- 评审HEAD：`1b7058ebb9dfbe904252a08efe6815d9952971ad`；回写前实际确认本地及远端当前功能分支均匹配，工作区干净。

独立审查确认（由用户转交）：

1. Room schema v1四表、外键、唯一索引及导出结构符合M04。
2. M04-T01～T06实现与Android测试证据符合要求。
3. 86个Android测试源码方法与86条PASS结果对应。
4. 74项JVM回归证据保留。
5. 16份关键源码、测试及配置文件SHA-256独立比对一致。
6. 未发现阻塞性代码问题。
7. **独立审查未在用户Windows本机重跑Gradle或安装APK。**证据/源码核对不登记为新的实际测试或安装PASS；本次状态回写同样不重跑。

保留NOTES与后续边界：

1. Q06完整切换、恢复及故障回退仍归M12；保持ENGINEERING_REQUIRED，M12完整流程与Q06-V01～V03仍NOT_RUN，不因M04冻结整体关闭。
2. 首次初始化中断后的残留generation必须安全处理，不能静默清除真实数据；本次未新增清理、修复或恢复实施/故障验证，不把保留异常数据解释为已完成完整恢复。
3. M08 v1→v2显式迁移须同步调整既有数据库版本预检，保持旧记录完整；迁移与集成仍未实施/NOT_RUN。
4. M05必须明确呈现存储初始化失败，不得显示虚假的正常空账本；正式错误界面/流程仍NOT_RUN，本次不进入M05。
5. 真机、API26、OEM行为仍NOT_RUN；原86 Android/74 JVM及APK交付证据保留，不冒充独立重新执行。
6. Q03～Q05原期限、M09 STOPPED重新启用和多期预付默认停止两项风险及后续验收边界保持。原130案例及16组完整跨模块补测仍NOT_RUN，历史交付报告/JSON/checkpoint与归档不改写。

本次仅更新唯一CURRENT_STATE与本登记，核查差异/敏感信息/Git后提交并推送当前M04功能分支；无产品源码、配置或数据层变更，不合并main、不修改WHOCARE、不开始M05。正式结论取代先前交付PENDING的当前效力，旧交付历史完整保留；回写提交自身SHA及最后远端核对以Git日志与最终报告为准。

## M05 当前执行验收（2026-10-12）

授权：用户M05附件；SHA-256 `419d5a963729d1c93f94aa935f2a189f455da0a1aa331de4e92119bc10968ece`。起点 `aecbe86fce6e5c4375f2c35d2df8539df0a557bb`、来源feat/m04-room-storage-foundation，本轮feat/m05-ui-foundation，开工已实际核对本地/远端一致且干净。编码前[M05契约](planning/M05_CONTRACT.md)固定最小结构；当前 **READY_FOR_REVIEW**，ChatGPT独立验收 **PENDING**。M00～M04冻结历史保留，不以本地辅助审查替代独立结论。

| 原案例 | 当前实际结果 | 执行证据及边界 |
|---|---|---|
| M05-T01 only_three_primary_destinations | PASS | 仅现在/回看/设置，标签/选中与内容一致；复选有界不增长；二级顶部/真实KEYCODE_BACK及实际边缘手势回Home；根真按键退出Activity |
| M05-T02 empty_screen_has_no_fake_metrics | PASS | 真Room默认两类别/无项目；合成项目与事件进入真实Repository后显示非空；Initializing/Failed/实际关闭DB与设置读取异常不成空库/零值/正常底栏；无假趋势/订阅读取 |
| M05-T03 large_text_has_no_blocked_action | PASS | 实际360px/density160/font_scale2.0；三Tab/滚动/二级返回/长金额无溢出/48dp语义；IME显示的合成表单错误、保存和返回回调实际可达，3项补测全PASS；不代表M06业务保存 |
| M05-T04 back_stack_restores | PASS（已执行场景） | 四NavKey保存/恢复，旧Skeleton及非法已解码栈归Home；ActivityScenario.recreate、横竖旋转；HOME后UID SIGKILL使旧PID消失、新PID恢复Settings；force-stop另记COLD回Home。真实内存压力自动LMK NOT_RUN，不笼统覆盖任意损坏保存数据 |

完整 `:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain` 退出0，**82 JVM＋118 Android＝200唯一方法及200条PASS，0失败/错误/跳过**。保留并实际执行原74 JVM与86 Android，新增8 AmountFormatter及32 UI（Components8/Navigation15/Storage7/Activity2）。首个完整GREEN3m27s实际运行全部；仅私有AVD改软件导航后再完整3m54s，118 Android实跑、JVM等up-to-date且源码/原XML一致。补测3个既有方法51s/3PASS、0新增方法；仅专用API36 emulator-5596。

真实RED金额8/7失败、UI31/30行为失败；未来结束日期1项断言失败后仅改事实标签。初期编译/runner配置错误不算RED；SystemUI ANR覆盖与返回注入不同步单列，Espresso同步真实按键修正测试驱动后孤立3/3和完整118/118通过。UI测试初始/最终整个文件hash不同，必要新增边界/实际Theme/截图及驱动同步已说明，未删失败断言或改生产Nav避过失败。

实际设备：安装最终APKSuccess，断网COLD/Status ok；360dp/200%三入口与只读列表、浅深、旋转、真正边缘返回PASS。HOME核对Launcher前台，am kill请求旧PID仍存活不算死亡；UID SIGKILL实际死亡模拟4568消失、新PID5103恢复Settings，另force-stop COLD回Home。同代pointer/ready/settings hash及安装APK hash一致。原hw.mainKeys=yes缺软件边缘handler，旧失败不冒PASS；仅本项目AVD设no保留userdata重启后已实际复验，配置保留。临时屏幕/字体/IME/明暗/旋转/网络均恢复，没有操作手机/其他AVD。

最终APK code3/0.1.0-m05-debug、11,837,748字节、SHA-256 `c70b89fd82b9480bbad8d1e0f06805317f6d6c002637583f5299b0fb769b790e`，包名com.cooper.lithic.debug。APK/合并Manifest/备份九域排除/Wrapper、18生产类、无测试框架/合成表单、v2签名PASS；无新增INTERNET，Debug新增非launcher/exported空ComponentActivity测试宿主，Release只配置排除/未构建。主依赖121→122仅ui-test-manifest1.9.5、0旧版本提升/移除，必要Lifecycle/Compose/Espresso精确依赖理由见[工具链](TOOLCHAIN_LOCK.md)。Lint0 errors/20 warnings＝17版本提示＋3旧占位UnusedResources，不抑制/顺便升级。

Room四表v1与原schema hash不变；50冻结输入、旧74/86测试源码不改，本轮70构建输入登记；唯一M04存储owner与真实Repository读取不改。ZIP CRC/43字节hash/130NOT_RUN/16完整补测NOT_RUN及Q03～Q06/M09风险体保留。完整文件清单、逐项200测试、来源/hash/依赖/设备/截图与Git范围见[M05报告](evidence/M05-verification.md)、[JSON](evidence/M05-verification.json)与[checkpoint](checkpoints/2026-10-12-M05.md)。

NOT_RUN：真机、API26、OEM、TalkBack完整审查、真实内存压力自动回收、Release正式签名升级、M06 CRUD/写后刷新、M08迁移/版本预检集成、M10图片、M11/M12备份恢复、M13汇总及其他未来业务。Q03～Q06期限及M09两风险不变，Q06仍ENGINEERING_REQUIRED；M04残留代安全与M08预检NOTES继承。TypeScript/ESLint不适用，NO-SOURCE不计PASS。

只提交并推送feat/m05-ui-foundation；提交自身SHA/推送后远端一致与干净工作区以Git日志/最终报告为准。不合并main、不force push、不改WHOCARE或公开APK。M05交付后立即停止等待独立验收，不授予ACCEPTED / FROZEN，不进入M06。

## M05 独立代码验收与正式冻结记录（2026-10-12）

- 正式结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立代码验收正式结论；本次Codex只回写，不自行授予冻结或以本地辅助检查替代独立审查。
- 仓库：`cc7279694-debug/property`。
- 评审分支：`feat/m05-ui-foundation`。
- 评审HEAD：`6909858cd0c03ec8e3277908e05d407d7f4cafca`；回写前已实际核对本地/远端一致、工作区干净。

独立审查确认（由用户转交）：

1. M05-T01～T04实现及现有测试证据符合本模块要求。
2. 82项JVM与118项Android方法均与提交的PASS记录逐项对应，共200个唯一测试。
3. 另外3项大字体补测有独立执行记录，不重复计入200个唯一测试总数。
4. 已审阅正常首页、设置、长期项目列表、横屏、进程恢复及键盘显示等实际截图。
5. 初始化、真实数据读取与存储失败状态明确区分，不将错误伪装成空账本。
6. Git范围为29新增、14修改、0删除，未提前进入M06。
7. **独立审查未在用户Windows本机重新运行Gradle或重新安装APK。**源码/证据审阅不是新的执行PASS，本次Codex状态回写同样不重跑Gradle、构建或设备验证。

保留全部NOTES与后续边界：

1. **M06真实数据刷新：**必须实现业务写入后的真实刷新；当前只读快照不代表保存及写后刷新已实施，后续流程/验收仍NOT_RUN。
2. **M06正式列表性能：**应落实分页或等效的大数据性能保护；不把M05只读列表当作长期大数据验收，后续实现与验证仍NOT_RUN。
3. **Debug与Release隔离：**Debug测试宿主不得未经验证进入正式生产交付；Release实际构建尚未执行，仍NOT_RUN。原配置排除及Debug额外空ComponentActivity的交付事实保留，不冒充Release产物验证。
4. **设备与可访问性边界：**真机、API26、OEM、完整TalkBack及真实低内存自动回收仍NOT_RUN；API36实测与应用UID SIGKILL进程死亡模拟不替代这些场景。
5. **规格与数据安全：**Q03～Q06及既定期限、M09两项流程风险、M04/M08/M12后续安全边界保持。Q06仍ENGINEERING_REQUIRED，完整切换/恢复/故障回退归M12；M04首次初始化中断残留generation不能静默清除真实数据；M08 v1→v2须同步调整数据库版本预检。未执行的迁移、恢复、故障验证及其他未来测试仍NOT_RUN，不擅自关闭问题。原130条归档案例、16组完整跨模块补测及历史交付/JSON/checkpoint原样保留。
6. **Lint：**现有20条警告不要求本轮强行升级依赖；不升级、抑制或修改既有执行结果。

本节正式结论取代先前交付PENDING的当前效力；原交付报告、证据JSON、checkpoint及上节当前执行验收保留为当时历史，不改其NOT_RUN/PENDING字段。本次仅修改唯一CURRENT_STATE与本登记，检查差异/敏感信息后提交并推送当前M05功能分支；不修改业务代码、WHOCARE或原规划，不合并main或进入M06。M06须用户明确新授权；回写提交自身SHA、推送和最终远端核对以Git日志/最终报告为准。


## M06 当前执行验收（2026-10-12）

用户M06授权附件SHA-256 `ac70a90d15afcdc42c8b43395037f77004beab32708d0bb8daf148cbaded3ee0`；基线 `4576b03565e58011e2fa78a086039fee33e3352f` / feat/m05-ui-foundation，当前feat/m06-project-entry。开始实际核对本地/远端一致、干净、新分支不存在；[契约](planning/M06_CONTRACT.md)编码前固定。当前 **READY_FOR_REVIEW**，ChatGPT独立验收 **PENDING**；没有自授冻结。

| 原案例 | 当前实际结果 | 实际证据及界限 |
|---|---|---|
| M06-T01 create_zero_and_normal_project | PASS | ProjectEntryFlowTest及ProjectRepositoryTest用真实Room创建0/普通项目、唯一INITIAL、关闭重开；正式MainActivity零元/1000录入和force-stop再开，持久2项目/4事件，未保存探针未入库 |
| M06-T02 initial_cost_counted_once | PASS | INITIAL只有一条，1000仅计1000，10天=100/天；真实重放/重开保持，不存在主表购买价；正式APK详情截图 |
| M06-T03 double_tap_no_duplicate | PASS | 正式UI快速双击、busy阻止第二次及改内容，真实并发创建/追加/编辑，同命令重放/写成功结果中断/SavedState身份恢复；异内容Conflict，原operationId唯一约束不变 |
| M06-T04 failed_save_preserves_form | PASS（已执行路径） | 校验/真实SQLite满页/关闭库失败保留raw和原命令；正式Nav弃改取消/Tab重选/顶部/真系统返回，未知提交不能弃掉；Activity状态恢复、真实STOPPED保存任务后SIGKILL/Recents同任务恢复；首轮未等待状态保存的失败如实保留，不覆盖任意前台断电 |
| M06-T05 backdated_money_recalculates | PASS | 1/1开始1000、1/10观察100；历史1/5追加200→120，1/4观察不提前计入；真实Room/UI详情/列表/首页COUNT刷新及重开；收回超投入得到负净额，改开始日重新计算 |

完整 `:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain` 退出0，9m 53s，**82 JVM＋202 Android＝284唯一方法PASS，0失败/错误/跳过**。原200身份全部保留且实跑，新84 Android：Repository34、ViewModel17、Routes3、Screens15、DateInput5、EntryFlow6、NavigationGuard4。源码@Test、XML、JSON精确同集；75最终source/schema输入与7份完整GREEN XML哈希核对。两份原UI测试仅适配COUNT/API及本轮实际详情，不删事实/错误/返回断言；旧M05证据字节不改。

真实行为RED64/55失败/9既有PASS；统一窗口竞态和冲突按钮RED2/2，迟到详情/初始编辑恢复与磁盘分类诊断RED3/3，最终初读错误按钮RED4/1后全部GREEN。编译import、JUnit非void runner及其他测试驱动错误单列；各失败XML/日志未丢弃。真实满页原设置曾作用于池中读连接，改为主写连接后复现；存量文件Room回滚可掩盖为DATABASE/cannot rollback，新测试仅修正过度严格分类预期，完整旧数据/无部分插入/同命令恢复一次断言保留。另VM真实DISK_FULL路径通过。生产没有按消息改分类或隐藏异常。

SQL50游标、同日ID稳定、批量事件/类别、COUNT无全库查询，真实51/100/1000合成行通过；已加载窗口一次事务，原竞态101→102真实复现后修复。所有金额仅MoneyParser/冻结M02引擎；固定记录时区、预付、未来付款拒绝、负净额、已有结束/封面字段保护及陈旧编辑通过。

最终APK code4 / `0.1.0-m06-debug`，**12,076,349 bytes**，SHA-256 `81b4c698c1d1355b018094bcc391be9b1ada8e4c4e447b7eb43ace8d7c7a2895`；com.cooper.lithic.debug。最终APK/合并Manifest/九域备份排除/Wrapper、26生产类/v2签名检查通过；无网络权限/测试框架/测试类，Debug额外非launcher空宿主保留，Release未构建。运行坐标122→122精确相同，0新增/删除/升级；Lint0 errors/20 warnings原样保留。

初次业务/大字体/草稿恢复13张证据来自整理末尾空行前APK d75549af…；仅移除ReviewShell末尾CRLF后完整284复跑，最终APK指纹为81b4c698…，另实际重装/断网COLD/2项目4事件/零元与负净额/1.234校验及弃改复验，并补5张最终截图，合计18张。两次产物证据在JSON明确区分。

仅专用API36模拟器：首次APK（d75549af…）安装，断网COLD启动，正式零元/正额→历史投入→超额收回→改名/开始日→列表/详情/首页COUNT刷新→force-stop再读；360dp/200%真实IME/日期/错误/弃改、旋转、深色及草稿恢复已查看。首轮HOME后过早SIGKILL未恢复草稿不计PASS；正确复验Task659/Record83054542已STOPPED且haveState=true/Bundle4444、PID12436消失、新PID12799从Recents恢复同Task及raw名称/1.234。系统Pacific/Honolulu日期10月11日，固定GMT业务日仍10月12日；pointer/ready/settings SHA一致。2项目/4事件/2INITIAL/4operationId、integrity_check=ok/FK无错，首次安装hash与首次APK相同；最终APK（81b4c698…）的安装hash另行核对相同，实际复验范围见上段及JSON.finalApk。临时设备设置已全部恢复，私有诊断库只在忽略目录，不实现/公开App备份。

NOT_RUN：真机/API26/OEM、完整TalkBack、真实低内存LMK、Release/正式签名升级、M07及后续业务/迁移/媒体/备份恢复/汇总。原43原件/130案例/16完整补测不变，Q03～Q06期限/M09两风险/M04残留代/M08预检/M12恢复边界保留。M05真实刷新和分页交接本轮已解决，不因此宣布其他能力完成。完整[报告](evidence/M06-verification.md)、[逐方法JSON](evidence/M06-verification.json)、[checkpoint](checkpoints/2026-10-12-M06.md)及新增XML/合成截图可审阅。

只提交推送feat/m06-project-entry；最终自身SHA及远端一致以Git日志与推送后本地回执为准。不合并main/force/PR/发布/WHOCARE，不提交APK、真实库或私钥。交付后立即停止，等待ChatGPT独立代码验收，M07未授权。


## M06 独立代码验收与正式冻结记录（2026-10-12）

- 正式结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话正式转交的ChatGPT独立代码验收结论；本次Codex仅回写，不自行授予冻结，不以本地辅助审查替代独立审查。
- 仓库：`cc7279694-debug/property`。
- 评审分支：`feat/m06-project-entry`。
- 评审HEAD：`2d3358724c737ca47093f3d0b3dc8ba3caf0f156`；本次回写起点相同，开始已实际核对本地/远端一致、工作区干净。

独立审查确认（由用户转交）：

1. M06-T01～T05的生产实现与自动化验收证据符合要求。
2. 82项JVM及202项Android测试源码方法均有对应PASS记录，共284个唯一方法。
3. 七份最终GREEN XML与测试结果登记一致，0失败、0错误、0跳过。
4. 创建/追加事件的事务与幂等、资料编辑的并发冲突保护，以及同操作安全重放已审阅。
5. 成本展示复用ProjectCostEngine，补录真实历史金额后能够刷新详情和列表。
6. 分页使用真实SQL限制、稳定日期和UUID排序、批量事件读取；已加载窗口具备一致快照保护。
7. Room schema v1与M05相同；M06未新增数据库版本或迁移。
8. 未发现确定性的阻塞性代码问题。
9. **ChatGPT独立审查未在用户Windows本机重新执行Gradle或重新安装APK。**源码/证据审阅不是新的执行PASS；本次Codex仅状态回写，同样不重跑测试、构建或设备验证。

保留全部NOTES与后续边界：

1. **SQLite空间不足的错误分类：**可能表现为普通DATABASE回滚异常；后续持续改进分类，但不得牺牲保留输入、事务原子性和安全重试。本次不修改实现或宣称分类已改善，原真实失败/复验和安全重放证据保留。
2. **分页验证范围：**1000条分页测试是正确性和查询范围验证，不是长期性能基准；不因冻结宣称长期性能已验证。
3. **SavedState恢复范围：**草稿恢复不保证系统未保存状态、force-stop或断电后的恢复；保留此前原任务状态已保存后SIGKILL/Recents模拟的真实边界。
4. **长期数据与备份：**完整备份/恢复仍归M11/M12，尚未完成；Debug版本暂不用于不可丢失的长期个人记录。冻结不等于完整备份/恢复或长期数据保障已验证。
5. **未执行的平台验证：**Release正式构建、真机、API26、OEM、完整TalkBack及真实内存压力回收仍**NOT_RUN**；不以已有Debug/API36/进程死亡模拟替代这些验证。
6. **后续功能与规格：**M07结束/删除/历史纠错、M09的STOPPED重新启用及多期预付默认停止两项风险、Q03～Q06及后续集成验收保留原边界，不自动实施或关闭。Q03长期100/1000日节点M21前、Q04提醒资格M20前、Q05图片往返M10/M12前、Q06 ENGINEERING_REQUIRED及M12完整切换/恢复/故障回退均不变；M04中断残留generation不得静默清除真实数据，M08 v1→v2须同步已有版本预检。原130条不可变案例、16组完整跨模块补测和历史交付证据原样保留，未运行状态仍NOT_RUN。

本节正式结论取代M06先前交付READY_FOR_REVIEW/PENDING的当前效力；上节执行验收、原报告/JSON/XML/截图/checkpoint保留为当时历史，不改其PASS来源或NOT_RUN/PENDING字段。本次仅更新唯一CURRENT_STATE和本登记，实际检查Git差异、链接、敏感信息及本地状态后提交/推送feat/m06-project-entry；不修改业务代码、WHOCARE或原规划，不合并main、不进入M07。本次文档检查不增加产品测试执行PASS。回写提交自身SHA、推送与最终远端核对以Git日志/最终报告为准。


## M07 当前执行验收（2026-10-12）

用户本轮授权附件 SHA-256 66a4810c80deaa3df766294442a23a88ceca06ed10b377501b5f68f3e17309f8；基线 fb0eafdc43efa6e13ebd76e3491fa4331a145abb，来源 feat/m06-project-entry，当前 feat/m07-project-lifecycle。[M07 契约](planning/M07_CONTRACT.md)编码前落地；M00～M06 正式冻结及全部 NOTES 继续有效。本轮状态 **READY_FOR_REVIEW — M07**，ChatGPT 独立验收 **PENDING**，实现者不自授 ACCEPTED / FROZEN。最终自动化、最终 APK 手工闭环、数据完整性与临时设备设置恢复已通过；Git自身提交/推送SHA以最终Git日志与交付回执核对。

| 原命名案例 | 当前实际结果 | 当前执行证据与边界 |
|---|---|---|
| M07-T01 end_and_recovery_atomic | PASS | 生产 Nav3/单 ViewModel/真实 Room；结束与可选唯一 END_SETTLEMENT 同事务，真实 SQLite 触发器使第二步失败时项目/事件整体回滚，原信封核对后只提交一次；0 元实际收回合法 |
| M07-T02 edit_end_recovery_not_duplicated | PASS | 连续两次修改关联收回只更新同一事件，id/projectId/direction/role/operationId 保留；成本重新计算，数据库关闭重开及结果中断原信封重放保留身份 |
| M07-T03 late_installment_does_not_reactivate | PASS | 实际结束后的 REGULAR 尾款增加净投入，持有天数封顶、不重新回当前；历史来源进入详情及返回、主 Tab 选回看；未来结束不提前归档 |
| M07-T04 correct_end_date_updates_result | PASS | 合法结束日修正使冻结引擎重算日均结果；早于开始日期拒绝且保留原输入和旧事实；无关联收回时修正不能偷偷新增收回 |
| M07-T05 delete_requires_confirmation | PASS | 真实 AlertDialog 二次确认，取消不写；完整项目/全部事件 CAS，任一删除失败整体回滚，确认后项目/事件移除并回来源列表；无悬空项目 FK，共享/未引用媒体保留给 M10，未知结果只核对原删除信封，Absent 不冒称永久删除日志 |

以上是当前执行验收，不改写原 ZIP/43 原件、不可变 130 案例或 16 组完整跨模块补测的 NOT_RUN；原规划及历史验收/报告/JSON/XML/checkpoint 保留原字节和当时状态。

最终完整四任务退出 0，7m 38s，**86 JVM＋273 Android＝359 个唯一方法 PASS，0 失败/错误/跳过**，等于保留原 284＋新增 75；JVM 明确实跑。7 份 JVM XML＋1 份 Android XML，测试源码方法与 XML/结果登记精确同集；五个生产 Flow 命名案例与数据事务、规则、UI、状态/恢复、路由边界均有实际执行记录。未运行的 NO-SOURCE 不计 PASS。Lint **0 errors / 20 warnings**，既有提示保留，不升级或抑制。完整文件/逐方法/输入与产物指纹见 [M07 报告](evidence/M07-verification.md)、[JSON](evidence/M07-verification.json)及 [checkpoint](checkpoints/2026-10-12-M07.md)，本地执行证据不替代 ChatGPT 独立验收。

真实 RED 与诊断边界：

- 新生命周期规则、Room 写入口与 UI/状态最初使用可编译 stub，真实失败原因是缺少行为；编译、JUnit runner 或测试驱动错误单列，不冒充行为 RED。
- 陈旧冲突后成功读到目标缺失曾无法返回；3 项真实 Room RED 为 2 FAIL / 1 PASS。删除明确读 null 后仍须以原 submitted 交由 writer 同时核对项目及关联事件；其它表单只标记已核对缺失、保留原 UUID/raw/信封、禁止再保存且允许返回，不伪造 COMMITTED。读异常继续未知，不自动换命令或 rebase。
- Unconfined StateFlow collector 在真实 Room 表单更新期间实际观察到活动 end 表单为空，expected UUID / actual null，行为 RED 后改为本地构造完整状态再单次发布。完整循环继续覆盖 end/金额纠错/delete，不新增持久状态 owner。
- 详情无结束后事实或仅有观察日之后事件时曾错误声明已包含结算；两项否定边界实际失败后改为仅依据真实且观察日前的结束后事件标注。冻结持有天数说明、事件方向/角色只读、真实退款与原记录纠错分离的事实断言保留。
- 连续修正曾在组合运行中反复超时，单例 PASS 未被认作已修复；最终诊断显示输入仍 IDLE/dirty、没有进入 save、原持久金额不变。ComposeTimeoutException 实际继承 Throwable，最初 Exception/AssertionError catch 未抓到；随后保留完整终态。最小驱动修正为关闭真实 IME、等待 Compose idle、重新滚至显示/启用的保存按钮再真实触摸；保留原 10/15 秒与全部持久身份/金额/日期断言，不直接调用 ViewModel 或语义回调代替触摸。缺失目标修复和完整状态发布修复不被冒称该点击超时的根因。

最终 Debug APK com.cooper.lithic.debug：code 5 / 0.1.0-m07-debug，**12,213,370 bytes**，SHA-256 **632a402c62bc827552b14f3613b3eb64828a2bd584f7c410a576c4e4220e765a**。Room 四表 schema v1、冻结 Money/ProjectCostEngine/BusinessClock、依赖版本、持久记录时区、同一 generation/settings/owner 与备份排除边界继承；未新增 Migration、备份格式、云端或生产样本。APK 与实际安装指纹一致，合并 Manifest、无网络及禁止权限、九域备份排除、v2 签名与生产类核对通过；无测试框架或合成测试类打包。Debug 非 launcher/exported 空 ComponentActivity 测试宿主保留，Release 未构建。

仅专用 API36 emulator-5596，最终 APK 全程断网：创建→结束/关联收回→历史→连续修改原结束金额→初始金额纠错→真实尾款 100/退款 800→净投入 -50、持有 8 天、日均 -6.25→取消/确认删除→重启均 PASS；force-stop 冷重开保持负净额事实。取消前后 3 项目/8 事件每个字段完全相同，确认只删除本轮 M07 目标及 4 事件；重启历史 0、首页数量 2。最终 SQL 2 项目/4 事件与 M06 原记录每个字段一致，integrity_check=ok、foreign_key_check 无错误、schema=1，generation/ready/settings SHA 全部不变。200%/360dp 真实 IME 的 1.234 原输入与精度错误保留，保存按钮实际可达；横竖保持原输入。Task1729/Record201059537 已 STOPPED/haveState=true/Bundle5944 后 UID SIGKILL：旧 PID22880 消失，经最近任务真实卡片恢复同任务/Record、新 PID23948，结束原因、真实日期和原金额保持。20 张最终 APK 截图已归档，临时网络/屏幕/字体/IME/明暗/旋转及时区设置全部恢复；未操作真机或其它 AVD，M06 原数据未改。最终Git回执另由提交/推送后的真实日志核对；系统未保存状态、force-stop/断电或真正内存压力 LMK 不在草稿恢复保证内。

继承全部 NOTES / NOT_RUN：

- M01 独立未重新安装；M02/M03 独立未重跑用户本机 Gradle；M04/M05 独立未重跑 Gradle/安装等；M06 独立审查未在用户 Windows 本机重新执行 Gradle或重新安装 APK。旧结论的来源与执行边界不因本轮新增验证改写。
- SQLite 满页错误可能被 Android/Room 回滚异常表现为 DATABASE；后续继续改进分类，本轮不宣称所有磁盘满均准确分类，不牺牲原始输入、事务和同命令安全恢复。
- 1000 条分页验证是正确性和查询范围检查，不是长期性能基准；单项目完整成本仍需必要事件。SavedState 不是持久业务草稿，不保证未保存系统状态、force-stop或断电恢复。
- 完整备份/恢复归 M11/M12，尚未完成；Debug 暂不用于不可丢失的长期个人记录。真机、API26、OEM 备份/迁移、完整 TalkBack、真实低内存 LMK、Release/正式签名升级仍 NOT_RUN；Debug 空 ComponentActivity 测试宿主不能未经 Release 验证正式交付。
- Q03 长期节点 M21 前、Q04 提醒资格 M20 前、Q05 图片往返 M10/M12 前、Q06 ENGINEERING_REQUIRED/M12 完整恢复与回退均保留；Q02 M17/M18 视图集成仍 NOT_RUN。M09 STOPPED 重新启用/多期预付默认停止两风险保持。M04 初始化中断残留 generation 不得静默删除；M08 v1→v2 须同步数据库版本预检；M12 完整切换、恢复、中断/故障回退及清理待实施。
- M05 写后刷新/正式分页交接已在 M06 解决，其历史 NOTES 保留。M08 订阅、替换、多段复用、M10 媒体清理、M11/M12 备份恢复、M13 汇总、图表/排行、M14 搜索/类别 CRUD、单独资金事件删除与其它未来业务保持原边界/NOT_RUN。TypeScript/ESLint 不适用。

仅提交/推送 feat/m07-project-lifecycle，最终提交自身 SHA、远端一致及工作区核对以 root 的 Git 日志/交付回执为准；交付自SHA从Git日志/最终回执恢复，避免递归自引用。不合并 main、不 force、不自动 PR/生产发布、不改 WHOCARE、不提交 APK、数据库、签名私钥或真实个人数据。M07 交付后立即停止，ChatGPT 独立验收 PENDING；不进入 M08。


## M07 独立代码验收与正式冻结记录（2026-10-12）

- 正式结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立代码验收结论；本次Codex仅回写，不自行授予冻结，也不以本地辅助审查代替独立验收。
- 仓库：`cc7279694-debug/property`。
- 评审分支：`feat/m07-project-lifecycle`。
- 评审HEAD：`4089428e7acdc0f2b8e2fd660f437b5bc7d29f61`；回写前实际确认本地/远端一致、工作区干净。

独立审查确认（依据用户转交的11项结论，不登记为本轮测试执行）：

1. M07-T01～T05的生产实现和测试证据符合原始模块要求。
2. 86项JVM与273项Android测试源码方法均与最终359条PASS结果对应。
3. 八份最终GREEN XML没有失败、错误或跳过。
4. 最终成功日志为BUILD SUCCESSFUL in 7m 38s；早期失败及驱动诊断证据如实保留。
5. 结束及关联收回使用同一Room事务，失败完整回滚。
6. 修改结束款只更新原关联事件，不重复生成收回。
7. 历史资金纠错保留事件身份；结束后的真实投入/退款不增加持有天数或重新激活项目。
8. 删除执行完整基线核对、事件与项目事务删除，并保护其他项目及共享媒体数据。
9. 历史档案由真实数据库过滤，未来结束日期不提前归档。
10. Room schema v1与依赖基线保持不变，未提前进入M08。
11. **独立审查未在用户Windows本机重新执行Gradle或重新安装APK。**本次Codex状态回写同样未重跑测试、构建或设备验证，不把源码/证据审阅登记为新的测试PASS。

保留全部NOTES与后续边界：

1. **ProjectAbsent：**只确认当前不存在，不证明历史删除操作身份；没有永久删除操作日志的证明能力。
2. **旧关联款歧义：**多条旧END_SETTLEMENT存在时明确冲突，未提供自动修复；本次不改实现或清理历史数据。
3. **媒体生命周期：**媒体数据与文件生命周期清理归M10，尚未完成；共享资源保护不等于清理功能已实现。
4. **未运行的平台验证：**真机、API26、OEM、Release、完整TalkBack、真实LMK仍 **NOT_RUN**；不以现有Debug/API36/SIGKILL证据替代这些验证。
5. **既定规格与未来边界：**Q03～Q06、M09两项风险、M08迁移预检及M12完整备份恢复边界继续保留。Q03长期100/1000日节点M21前，Q04提醒资格M20前，Q05图片往返M10/M12前，Q06 ENGINEERING_REQUIRED及M12完整导入/切换/回退/中断/共同校验/清理仍未实施或未完成；Q02 M17/M18集成仍NOT_RUN。M09 STOPPED重新启用及多期预付默认停止两风险未关闭；M04残留generation不静默清理，M08 v1→v2须同步既有数据库版本预检。本次不实施或关闭这些事项。
6. **Lint及历史证据：**原Lint20条提示及历史执行证据完整保留，不因验收回写重跑或改写。原130条归档案例、16组完整跨模块补测与旧报告/JSON/XML/截图/checkpoint保持原字节和NOT_RUN/当时状态；此前359条PASS保持历史实际执行来源，本轮文档检查不增加产品测试执行PASS。M00～M06全部历史NOTES继续有效。

本节正式结论取代M07此前交付READY_FOR_REVIEW/PENDING的当前效力；上节执行验收及原报告/checkpoint保留当时状态。回写只修改唯一CURRENT_STATE与本登记，核对差异、链接、敏感信息、历史原件及Git状态后提交/推送feat/m07-project-lifecycle。回写自身SHA、推送及远端一致以Git日志/最终回执核对，避免递归自引用。不修改业务代码、WHOCARE或原规划，不合并main、不进入M08；完成后立即停止。


## M08 当前执行验收（2026-10-13）

用户完整 M08 授权附件 SHA-256 `31b0dea9ce575192cc22b7da229c5f73e2020a9ea785db287cb672f568c029f6`；起点 `fcb850f549285d310040318a5f7adfd51abb4c33`，来源 feat/m07-project-lifecycle，交付分支仅 feat/m08-subscription-renewals。开始实际核对来源本地/远端一致、工作区干净、目标此前不存在；[M08 契约](planning/M08_CONTRACT.md)在实现前固定迁移闸门及最小范围。当前 **READY_FOR_REVIEW — M08**，ChatGPT 独立验收 **PENDING**，不授予 ACCEPTED / FROZEN；M09 未授权，本地辅助审阅不替代独立验收。

| 原命名案例 | 当前结果 | 实际证据与边界 |
|---|---|---|
| M08-T01 v1_to_v2_preserves_projects | PASS | 真实 v1 SQLite fixture 四表完整事实、纠错/结束/收回/媒体引用逐字段相同；显式 Room 1→2/关闭重开/同代设置保留；新 v2 空库另测，升级后旧项目仍可写 |
| M08-T02 create_and_first_payment_atomic | PASS | 真实 Room 首次身份＋正额付款同事务；付款故障身份一起回滚；相同完整命令重放/并发仅一次，异内容冲突，成功结果中断后同身份恢复 |
| M08-T03 renewal_needs_confirmation | PASS | 正式 UI/Nav3＋Room 打开/编辑/弃改/到期不付款，显式确认才追加；非法金额及真实 SQL ABORT 后 raw/周期/四日期保留；最终 APK 取消后2订阅/2付款、确认后2订阅/3付款 |
| M08-T04 renewal_history_is_preserved | PASS | ¥150/月→¥180/月→年付各自金额/cycle/standardDays/四日期完整重开；预付不提前切价；最终 APK150→180和三观察日另行实际验证 |
| M08-T05 accurate_dates_retained | PASS | 四日期彼此不同，reminderDueOn 可不同于排他覆盖结束；真实写入、关闭重开完整；无覆盖当前价格null、贡献0 |
| M08-T06 currency_change_is_allowed | PASS | 同 MONTHLY ¥146.83→¥148.10 合法，各自标准30天和独立付款保留，未误判为按量服务 |

当前登记独立于不可变原130案例；原ZIP/43原件、24模块计划、130 NOT_RUN 和16组完整跨模块补测不改。完整[12项报告](evidence/M08-verification.md)、[逐方法/校验JSON](evidence/M08-verification.json)、[文件清单](evidence/M08-files.md)及[M08 checkpoint](checkpoints/2026-10-13-M08.md)记录具体日志/XML/输入哈希和截图，不以源码存在代替实际执行。

### 本轮20项额外安全要求

| # | 当前结果 | 实际测试/核对 |
|---:|---|---|
| 1 | PASS | Migration合法v1/v2/legacy、非法版本/v0/身份/索引/列/表/FK；sqliteXforeign、PK NOCASE、保留表同名trigger拒绝且保留完整事实 |
| 2 | PASS | 实际 Room DDL 故障整体回滚原v1结构/身份/旧行，无清空/另一空账本；既有存储失败状态回归保留 |
| 3 | PASS | 新v2空库、迁移后Room验证与schema2导出一致；APK/schema旧四实体比对一致，原v1 JSON字节不改 |
| 4 | PASS | Repository拒绝PROJECT类别，受控Screen只选SUBSCRIPTION类别 |
| 5 | PASS | DAO真外键拒绝孤立续费；confirm_requires_existing_subscription，无残留付款 |
| 6 | PASS | 同operationId不同完整事实明确冲突；精确重放先于覆盖检查，不增加第二笔 |
| 7 | PASS | 0/负额/超限拒绝；MoneyParser/表单三位小数校验且保留raw；最终APK1.234错误可见 |
| 8 | PASS | 固定BusinessClock.today拒绝未来付款；历史asOf忽略后来真实付款，不误判其非法 |
| 9 | PASS | 真实预付且未来生效的首期/续期合法，进入已购历史但不虚构未来已经过时长 |
| 10 | PASS | 相邻/真实空档覆盖允许，全部已付覆盖含未来期重叠拒绝 |
| 11 | PASS | 两个并发重叠续费不能都提交，失败完整保留旧事实 |
| 12 | PASS | Repository/正式Flow/最终APK：1/19当前5/历史150/标准30，1/20当前5/历史330/标准60，2/1当前6 |
| 13 | PASS | EXPIRED/null当前价格/贡献0，无自动付款；最终恢复10/13后历史330/标准60/实际59 |
| 14 | PASS | 30/90/365独立持久化并校验cycle一致；自然覆盖/elapsed分开，冻结M03全回归 |
| 15 | PASS | 列表MAX(paidOn) DESC/UUID、历史paidOn DESC/UUID，真实SQL分页/窗口刷新，不按生效日造假 |
| 16 | PASS | v2重开后旧项目仍可写、原359回归；最终旧四表3项目/6事件逐字段等于升级前，与2订阅/3付款共存 |
| 17 | PASS | 同DataGenerationStore升级/跨APK快照，activeId/ready/settings SHA/recordZone GMT不变，无另一空代 |
| 18 | PASS | 刷新失败保留已有行和明确错误；Screen加载/空态/错误、缺付款身份/未知cycle异常不呈假空库 |
| 19 | PASS | 360dp/200%/真实IME/系统返回自动化＋最终APK操作，横竖保留raw；完整TalkBack仍NOT_RUN |
| 20 | PASS | 真实成功后结果丢失的同命令恢复/精确重放，COMMITTED只重读/误身份继续pending/隐藏pending可达；最终已保存任务SIGKILL恢复raw，命令身份由正式Room/Nav3仪器测试证明 |

### 完整自动化与诊断历史

最终四任务 `:app:testDebugUnitTest --rerun :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain` 退出0，**BUILD SUCCESSFUL in13m30s；86 JVM＋369 Android＝455个唯一方法 PASS，0失败/错误/跳过**。7份JVM XML＋1份Android XML，源码/XML方法 **EXACT MATCH**，108源码/schema输入哈希与实际测试版本一致；旧359方法身份保留、新增96（Migration22、Repository30、ViewModel12、Routes6、NavigationGuard7、Screens12、真实Flow7）。JVM实跑，NO-SOURCE不计PASS，限定复验不增加唯一数量；Lint **0 errors/20 warnings**。

四个旧测试文件调整明确记录：两个阶段适配分别以真实v1 fixture继续证明四表、以真实订阅空态替代“尚未开放”；另外两个只加强原删除恢复状态等待和真实IME/类别触摸同步。原方法身份、业务断言/超时及冻结旧业务代码保留；新Screen的waitForIdle也仅驱动同步，不声称生产缺陷修复。

真实行为RED与全部编译/runner/匹配/准备/同步诊断日志/XML/输入保留：旧v1上迁移13项9FAIL/4PASS，可编译空壳60项40FAIL/20PASS；额外表前缀/PK NOCASE四项、根首页pending/dirty返回及缺失目标VM/Screen四项、v1/v2×两保留表同名trigger四项均有效FAIL。非行为诊断不冒充RED。

完整回归历史保留：首版451 PASS/12m39s；trigger补测后369 Android曾368PASS/1FAIL/11m31s，新Screen限定12PASS/2m19s；下一完整轮曾367PASS/2FAIL/9m9s，两个旧同步方法限定2PASS/1m25s；之后455 PASS/10m19s另归档green-full-before-visual-selection。只补周期可见选择和“提醒日期当前只保存不通知”两句文案后，再取得最终455 PASS/13m30s。旧失败/中间产物不改写为最终GREEN，详细阶段/输入见报告和JSON。

### 最终APK、升级及设备

最终Debug `com.cooper.lithic.debug` code6/`0.1.0-m08-debug`，**12,386,935 bytes**，SHA-256 **6e04f28cc0998fa772cfeeedf99eb457b71852ee7549eeda39525c6155afbcf6**，实际安装指纹同值。v2签名兼容M07证书、合并Manifest/无网络及禁止权限、九域备份排除、63指定生产类/无测试框架或合成测试打包均已核对。运行依赖122→122，无新增/删除/升级；Debug空宿主边界保留，Release未构建。

仅专用API36 emulator-5596：旧code5→中间code6首次升级发生于中间目标Application启动，最终APK再同身份覆盖并读旧库；未卸载/清数据/换代，不称最终APK首次触发迁移。旧四表3项目/6事件每字段相同，旧纠错结束项目350净投入/10天/35每天。此前候选创建的M08_SUBSCRIPTION首笔150保留，完整最终APK闭环使用M08_FINAL_SUBSCRIPTION150→180；最终2订阅/3续费/0状态变更，不删参考样本。

最终APK全程断网：实际guest日期1/19首付款后当前5/历史150/标准30；1/20取消不写，确认180后当前仍5/历史330/标准60/平均5.5/实际20且尚未生效；2/1当前6；恢复10/13 COLD重开EXPIRED/null价格0贡献/历史330/标准60/实际59，无自动新增付款。四日期及身份完整保留；200%/360dp真实IME、1.234错误/raw、返回收键盘和横竖屏实际操作完成。

Task3038已STOPPED/HaveState=true/Bundle6360bytes后UID SIGKILL，旧PID14365消失，Recents恢复同任务/新PID15775/raw1.234；不称真实LMK、force-stop/断电草稿保证，精确命令身份由仪器测试证明。最终SQL integrity_check=ok/FK=[]、旧新事实及同代pointer/ready/settings SHA/GMT保持。临时guest网络/屏幕/字体/IME/旋转/明暗/时区及auto_time/auto_time_zone全部恢复，guestUTC与宿主差少于10秒，未改宿主时钟或其它设备。

### 保留NOTES与停止点

- Q01 CLOSED、Q02/Q07 USER_CONFIRMED；Q02 M17/M18集成仍NOT_RUN。Q03长期100/1000日节点M21前、Q04提醒资格M20前、Q05图片往返M10/M12前、Q06 ENGINEERING_REQUIRED及M12完整导入/切换/回退/中断/共同校验/清理/Q06-V01～V03仍NOT_RUN，未因本轮关闭。
- M09 STOPPED重新启用/历史纠错及多期预付默认停止两风险未定案/实施/验证，M09未授权；M08没有暂停恢复停止入口、退款/期中补差/自动付款。M04残留generation不得静默清除；M08版本预检已落实，完整恢复/故障回退仍归M12。
- M07 ProjectAbsent只证明当前不存在、不证明历史操作身份；多条旧END_SETTLEMENT明确冲突、无自动修复；M10媒体生命周期未实施。SQLite空间不足可能表现普通DATABASE，不能牺牲raw、原子性和安全重放来猜分类。
- 分页验证正确性/查询范围，不是长期性能基准；完整成本仍需必要历史。SavedState仅覆盖已保存系统任务，不保证未保存状态、force-stop、断电或真正LMK。M11/M12完整备份恢复未完成，Debug暂不用于不可丢失的长期个人记录。
- 真机/API26/OEM、Release/正式签名升级、完整TalkBack、真实低内存LMK仍NOT_RUN；Debug测试宿主须后续Release核查。全部历史独立未重跑本机Gradle/安装NOTES保留，不把本轮执行改称此前独立重跑。TypeScript/ESLint不适用，NO-SOURCE不计PASS。
- 原ZIP/43原件/130案例、16完整补测及所有旧报告/JSON/XML/截图/checkpoint字节和当时状态保留；M10～M23/未授权业务按原边界，不实现媒体、备份、M13汇总、云端或WHOCARE改动。

只Conventional Commit/推送feat/m08-subscription-renewals；最终自身提交SHA、远端/工作区/保护引用核对以实际Git日志及交付回执为准，不提前登记推送PASS，不合并main/force/自动PR/公开APK，不提交数据库、私钥或真实个人数据。交付后立即停止等待ChatGPT独立验收，**M08独立PENDING，不进入M09**。

## M08 独立代码验收与正式冻结记录（2026-10-13）

- 正式结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的 ChatGPT 独立代码验收正式结论；本次 Codex 仅回写，不自行授予冻结，也不以本地辅助审阅代替独立验收。
- 仓库：`cc7279694-debug/property`。
- 评审分支及本次回写分支：`feat/m08-subscription-renewals`。
- 评审 HEAD：`a716bc5ad9e3ab49cbe858f2c966fecf93e09fbb`；回写前已实际确认本地及远端 M08 分支均一致，工作区干净。

独立审查确认（完整登记用户转交的11项结论，不登记为本轮测试执行）：

1. M08-T01～T06 均有符合原始要求的代码和真实测试证据。
2. 86 项 JVM 与369 项 Android 测试源码方法和最终 XML 逐项对应，共455项，0失败、0错误、0跳过。
3. M07 原359项方法身份保留，本轮实现新增96项。
4. 最终成功日志为 `BUILD SUCCESSFUL in 13m 30s`；中间失败及诊断证据保留。
5. v1→v2 显式 Migration、新三表和原 v1 schema 保护正确。
6. 实际迁移测试覆盖旧四表逐行比较、WAL、同 generation 及迁移失败回滚。
7. 既有数据库版本预检已支持已知合法 v1/v2，并明确拒绝未知或结构不匹配数据库。
8. 创建身份与首次真实付款同事务，续费具备稳定操作身份和重复提交保护。
9. 续费价格、周期及四日期独立持久化，预付不提前改变当前套餐价格。
10. 未发现确定性阻塞问题，main 和历史冻结代码范围得到保护。
11. **独立审查没有在用户 Windows 本机重新运行 Gradle、重新安装 APK 或独立核验 APK 字节；超大 JSON 未整体重新解析。**验收以源码/XML/日志/迁移及设备证据为依据。本次 Codex 状态回写同样未运行产品测试、构建、安装、设备验证或重新核验 APK；既有455项 PASS 为原交付执行记录，不新增一次产品测试 PASS。

保留全部 NOTES 与后续边界：

1. **M09 未实现：**暂停、恢复和停止用户流程尚未实施；STOPPED 后重新启用/历史纠错，以及多期预付默认停止两项既有产品风险保留，未定案、未关闭、未实施。M09 必须另获明确授权，本轮不开始。
2. **Q03～Q06：**维持原状态及处理期限。Q03 长期100/1000日节点归 M21 前，Q04 提醒日期筛选/资格仍归 M20 前，Q05 图片往返归 M10/M12 前，Q06 保持 ENGINEERING_REQUIRED；不因 M08 冻结自动关闭。Q02 的 M17/M18 集成仍 NOT_RUN。
3. **备份及故障回退：**M11/M12 完整备份恢复以及 Q06 完整切换、恢复、中断、验证后回退、共同校验和清理仍 NOT_RUN；Q06-V01～V03 完整流程未完成。M04 首次初始化中断残留 generation 不得静默清除真实数据。
4. **后续 schema 升级：**必须继续维护数据库版本预检及已知 Room 身份，显式迁移和旧数据验证不可省略；不放行未知或结构不匹配数据库。
5. **跨 APK 升级证据边界：**M08 中间 Debug 产物首次触发跨 APK 的 v1→v2 升级，最终产物经过同身份覆盖安装和数据复查；不得写成最终 APK 首次触发迁移。既有 generation、设置、记录时区及旧四表事实保护证据保留。
6. **平台 NOT_RUN：**真机、API26、OEM、Release、完整 TalkBack 和真实 LMK 仍 NOT_RUN，不由模拟器或已保存任务 SIGKILL/Recents 替代。正式签名升级未运行；SavedState 不保证未保存状态、force-stop 或断电后的草稿恢复。
7. **重要数据保障：**当前 Debug 版本不能用于保存无法丢失的重要个人记录，完整备份恢复尚未具备；本次验收不宣告 G1 通过。
8. **历史 NOTES、归档与未来边界：**原有全部历史 NOTES、原130条 NOT_RUN 归档案例及16组完整跨模块验收边界保持不变；旧报告/JSON/XML/截图及历史 checkpoint 保留原字节和当时状态。继续保留 M07 ProjectAbsent 仅证明当前不存在、多条旧 END_SETTLEMENT 冲突且无自动修复、M10 媒体生命周期、SQLite 满页可能表现 DATABASE、分页非长期性能基准及原20条 Lint 提示。不因本轮回写重跑、改写或把后续未执行项标 PASS；TypeScript/ESLint 不适用，NO-SOURCE 不计 PASS。

本节正式结论取代 M08 此前 READY_FOR_REVIEW / PENDING 的当前效力；上节执行验收及原报告/checkpoint保留当时状态。本次仅更新唯一 CURRENT_STATE 与本登记，检查 Git 差异、文件链接、敏感信息、工作区及历史资料保护后，提交并推送 `feat/m08-subscription-renewals`。回写提交自身 SHA、推送及远端一致以实际 Git 日志/最终回执核对，避免递归自引用。不修改业务代码、WHOCARE、原规划或旧证据，不合并 main、不 force、不进入 M09；完成后立即停止。


## M09 当前执行登记（2026-10-13，READY_FOR_REVIEW）

用户正式授权完整附件SHA-256 `d83fb2d77190b0c49df8b5adf9a6e13c8cae8ca203513f9f5e98b0729350eaab`。起点M08冻结回写 `0063ca504ca48ef9c7231d79dc155e9bf4bc8c82`、来源feat/m08-subscription-renewals，本轮仅feat/m09-subscription-lifecycle。开工本地/远端一致、干净、目标不存在；[M09契约](planning/M09_CONTRACT.md)和A/B正式规则先于实现登记。

| 案例 | 当前实际执行 | 证据与边界 |
|---|---|---|
| M09-T01 expiry_needs_no_worker | PASS | 正式Room/Flow固定Clock在排他边界EXPIRED/0/null，无自动付款/状态；实际guest跨日至3/1及断网冷启 |
| M09-T02 cancel_renewal_keeps_paid_access | PASS | 默认双期最晚边界、空档/全部到期/提前停止、固定计划冲突及跨日重放；实际3/1边界STOPPED、修正/取消不改原付款 |
| M09-T03 pause_is_not_history | PASS | asOf SQL分页筛选/正式当前三分组与停止历史、原ID重开、历史观察/有效天数；未来停止不提前归档 |
| M09-T04 resume_without_new_payment | PASS | 覆盖内恢复零付款，未来已付仍EXPIRED，无覆盖空金额/付款日＋显式付款/ENABLED原子提交与故障回滚；实际240原ID启用 |
| M09-T05 future_prepaid_has_own_label | PASS | 真实付款paidOn排序/尚未生效标记，状态不覆盖金额/标准时长；实际预付1/25仍5/天、2/1切6/天 |
| M09-T06 stop_reason_does_not_cancel_vendor | PASS | 正式服务商说明/原因保存、取消零写/主动确认；最终APK无INTERNET/禁止权限或第三方调用 |

以上为本轮执行证据，原130案例仍NOT_RUN，原43原件/24计划/16组跨模块边界及旧报告/checkpoint字节保持。不是独立验收；**M09独立PENDING，未自授ACCEPTED/FROZEN，M10未授权**。

最终四任务退出0、**Gradle日志13m 24s，命令墙钟805.631秒，86 JVM＋463 Android＝549唯一方法PASS，0失败/错误/跳过；旧455身份完整、新94（47Repository＋47UI/VM/Nav/Flow），源码/XML EXACT MATCH，128构建输入哈希一致**。Lint0errors/20warnings、assemble完成，122运行依赖无变化。早期RED、runner void错误、两次全量失败及限定复验全部保留，四旧测试仅加强实际驱动/接口/目标同步，原方法/业务断言/超时不降级。

完整22项风险逐方法映射、真实SQLite故障/并发/幂等/陈旧CAS、迟到读保护、所有执行阶段、最终APK及专用API36证据见[M09报告](evidence/M09-verification.md)、[JSON](evidence/M09-verification.json)、[文件](evidence/M09-files.md)和[checkpoint](checkpoints/2026-10-13-M09.md)。code7 APK 12,521,904 bytes、SHA256 becd15b3bbf5de9425419504bde58837d0d6fa6e1cacc32d995f8dc11ec437f2，真实旅程同字节，签名/权限/九域排除/生产打包已检查。最终手工安装为code7→7覆盖；第一次code6→7发生于此前仪器部署，不改写升级时间点。

实际设备：仅API36 emulator-5596，合成服务状态全旅程/付款纠错删除、断网跨日、固定recordZone、360dp200%/IME/旋转/手势返回、系统已保存同任务SIGKILL/Recents；旧七表逐字段相同、generation/ready/settings不变，临时设置及实际UTC恢复。原DB/WAL/SHM/preferences/APK/密钥不入Git，公开证据只含合成M09事实/计数/哈希/截图。

保留NOTES：CurrentAbsence仅当前缺失，不是永久操作日志；全量坏数据预检50身份批次但O(total facts)，单服务历史完整加载，不是长期性能基准；SavedState只覆盖已保存任务，不保证force-stop/断电/真实LMK；SQLite空间不足可能表现DATABASE。真机/API26/OEM/Release正式签名/完整TalkBack/真实LMK、M11/M12完整备份恢复及Q06故障回退仍NOT_RUN，Debug不用不可丢失个人记录。Q03～Q06状态期限、Q02未来集成、M04残留generation、M08后续schema预检、M07原删除/媒体边界及全部历史独立NOTES保持。M09两项用户已确认流程风险只按本轮实测关闭，不关闭提醒资格或未来模块。

只提交/推送feat/m09-subscription-lifecycle，自身提交SHA与实际推送依据Git日志/回执，避免递归自引用。不合并main、不force、不自动PR/公开APK、不进入M10；交付后停止。

## M09 独立代码验收与正式冻结记录（2026-10-13）

- 正式结论：**ACCEPTED / FROZEN — PASS WITH NOTES**。
- 来源：用户在当前会话明确转交的ChatGPT独立代码验收正式结论；本次Codex仅回写，不自行授予冻结，也不以本地辅助审阅代替独立验收。
- 仓库：`cc7279694-debug/property`。
- 评审分支及本次回写分支：`feat/m09-subscription-lifecycle`。
- 评审HEAD：`bfc25378927ca8d04eaecbb9d2b1455d72633da9`；回写前已实际确认本地及远端M09分支一致、工作区干净。

独立审查确认（完整登记用户转交的12项结论，不登记为本次产品测试执行）：

1. M09-T01～T06的正式实现和实际测试证据符合要求。
2. 86项JVM、463项Android，共549项测试通过；原455项保留，M09实现新增94项。
3. 最终成功记录为 `BUILD SUCCESSFUL in 13m24s`；此前失败和诊断历史保留。
4. 暂停、恢复、到期、停止、计划修正与取消均使用持久状态事实和M03统一计算引擎。
5. 多期预付默认停止边界正确固定，后续付款不会静默延长原计划。
6. STOPPED后可同身份重新开通；无覆盖时真实付款与ENABLED原子提交，并有失败回滚测试。
7. 同日状态修正保留身份；续费纠错、删除具备事务、幂等和陈旧基线保护。
8. 当前管理、暂停分组、已停止历史及真实刷新得到验证。
9. API36模拟器提供20条实际操作路径及旧数据保留证据。
10. Room schema v2、既有M03成本引擎、存储owner和运行依赖未越界改变。
11. 未发现确定性阻塞问题。
12. **ChatGPT独立审查未在用户Windows本机重新运行Gradle或独立核验最终APK字节。**本次Codex状态回写同样未运行产品测试、构建、安装、设备验证或重新核验APK；既有549项PASS为原交付执行记录，不新增一次产品测试PASS。

保留全部NOTES与后续边界：

1. **CurrentAbsence：**只证明当前缺失，不提供永久删除操作日志；不将缺失结果升级为历史删除操作身份的证明。
2. **长期性能：**全量存储事实预检在长期大数据下的性能仍待M22验证；当前分页/查询范围证据不等于长期性能基准。
3. **草稿恢复：**SavedState不保证force-stop、断电或真实LMK后的未持久化草稿恢复；已保存同任务SIGKILL/Recents证据不替代这些验证。
4. **空间不足：**SQLite空间不足可能被底层包装为DATABASE错误；继续保证原子性、保留输入和安全重试，不为猜测错误分类牺牲数据安全。
5. **平台NOT_RUN：**真机、API26、OEM、Release、完整TalkBack和真实LMK仍NOT_RUN；正式签名升级及Debug测试宿主的Release核查继续保持未执行边界。
6. **备份与故障回退：**M11/M12完整备份恢复及Q06故障回退仍NOT_RUN；Q06保持ENGINEERING_REQUIRED，完整导入/切换/验证后回退/中断/共同校验/清理与Q06-V01～V03仍待M12。当前Debug版本不能用于保存无法丢失的重要个人记录。
7. **规格与未来集成：**Q03～Q06其他规格问题和未来集成验收继续按原期限处理。Q03长期100/1000日节点在M21前，Q04提醒资格在M20前，Q05图片往返在M10/M12前；Q02的M17/M18视图集成仍NOT_RUN。M04初始化中断残留generation不得静默清除真实数据；后续schema升级继续维护M08已建立的版本预检及已知Room身份。M07旧END_SETTLEMENT歧义、M10媒体生命周期及全部历史NOTES保留。
8. **不可变原件与历史：**原130条NOT_RUN归档案例、16组跨模块案例、43原件/24模块计划及历史报告/JSON/XML/截图/checkpoint不改写；早期失败、诊断、Lint20条提示与各模块原执行状态保留。不将后续未执行项标PASS，TypeScript/ESLint不适用，NO-SOURCE不计PASS。

M09两项已确认产品规则继续保留原来源与实施验证：用户完整M09执行指令SHA-256 `d83fb2d77190b0c49df8b5adf9a6e13c8cae8ca203513f9f5e98b0729350eaab`、[DECISIONS](DECISIONS.md)、[M09契约](planning/M09_CONTRACT.md)和[问题清单](SPEC_ISSUES.md)登记A/B规则；[M09报告](evidence/M09-verification.md)、[原结构化证据](evidence/M09-verification.json)与[历史checkpoint](checkpoints/2026-10-13-M09.md)保留实际实施与验证。A规则允许STOPPED后同身份重开，覆盖内不创建付款，未来已付未生效仍EXPIRED，无覆盖时显式真实付款与ENABLED同事务；B规则默认停止固定全部已确认覆盖最晚边界与业务今天中的较晚日，后续付款不静默延长，需明确修正或取消计划。维持USER_CONFIRMED / IMPLEMENTED / VERIFIED及已关闭的两项流程风险，本次不重新定案、不修改实现，不自动关闭Q03～Q06或未来模块事项。

本节正式结论取代M09此前READY_FOR_REVIEW/PENDING的当前效力；上节执行登记及原报告/checkpoint保留当时状态。本次仅更新唯一CURRENT_STATE与本登记，检查Git差异、链接、敏感信息、工作区及历史资料保护后，提交并推送 `feat/m09-subscription-lifecycle`。回写提交自身SHA、推送及远端一致以实际Git日志/最终回执核对，避免递归自引用。不修改业务代码、WHOCARE、原规划或旧证据，不合并main、不force、不进入M10；完成后立即停止。

## M11-A 核心候选当前执行（2026-10-20）

用户本轮只授权从冻结M09回写`57de897917473f255d43dfb306a8433b366c00eb`隔离先行M11-A；当前`codex/m11a-backup-export-core`，**READY_FOR_REVIEW，独立验收PENDING**。M10为PENDING / DEFERRED，三JPEG历史FAIL与全部UNKNOWN/NOT_RUN/NOTES及设备现场保留；本轮无设备操作。完整M11未冻结，M12未授权。

| 对应原案例 | 本轮实际核心子集 | 未取得的完整验收 |
|---|---|---|
| M11-T01 | PASS：七表每个v2字段映射、设置白名单、整数分/ISO/枚举与稳定JSON；原86＋新增27 JVM共113 PASS | 真实Room/DataStore五项仅编译，运行NOT_RUN；媒体元数据不等于媒体字节，原完整T01仍NOT_RUN |
| M11-T02 | PASS：生产使用的同一coordinator读写互斥、写前/读中一致性、失败及取消释放 | 实际Room/DataStore/媒体并发与Android运行NOT_RUN，不以JVM替代原完整T02 |
| M11-T03 | PASS：真实内部文件长度/SHA、manifest v1字段、无媒体合成ZIP逐条核对，变化即拒绝 | 封面复制、Q05字节完整往返、SAF与原完整T03仍NOT_RUN |
| M11-T04 | PASS：内部输出写入/关闭失败、资源边界、内容变化不返回成功；完整入口不打开目标，固定拒绝媒体未集成 | 系统URI/空间不足/取消/不完整目标删除和原完整T04仍NOT_RUN |
| M11-T05 | 本轮未新增密码/自动备份/CSV/云端及用户成功入口 | 正式用户路径未开放，原完整T05仍NOT_RUN |

最终同源码四任务重执行 **BUILD SUCCESSFUL in 5m 4s**：JVM113/113，0失败/错误/跳过（11份XML，源码方法完全对应，旧86身份/源码未改）；Lint0错误/7警告；Debug assemble与Android测试Kotlin编译PASS。新增5个Android测试方法仍NOT_RUN。运行依赖122→122完全相同，无工具链升级。APK静态身份、权限/备份排除、签名检查实际PASS，不等于安装或设备通过。

原始RED、112项首轮失败、Windows null fileKey诊断、环境转义与缓存Lint失败均保留，不改成PASS。完整数字、命令、输入/日志SHA和APK身份见[集中报告](evidence/M11A-verification.md)及[必要证据](evidence/m11a/results.json)。归档130原案例、16组完整跨模块补测、历史XML/checkpoint不改写；此登记仅M11-A核心子集，不自授正式M11 ACCEPTED/FROZEN。

## M11-A 独立冻结登记与M11-B当前执行（2026-10-20）

用户本轮明确转交ChatGPT结论：M11-A **ACCEPTED / FROZEN — PASS WITH NOTES，仅限备份核心代码**；指定锁定来源`79771fcf5978a938d240d517dd4dacb1d21021a6`。不另臆造独立评审HEAD或扩大范围。原M11-A报告、checkpoint、113项结果及所有未运行/临时文件NOTES不改写；该结论不等于已重跑Android、媒体或SAF。

M11-B来源为上述冻结提交，分支`codex/m11b-complete-backup-export`。当前 **BLOCKED / 候选待独立审查**；执行证据见[M11-B集中报告](evidence/M11B-verification.md)及[小型结果索引](evidence/m11b/results.json)。正式M11未冻结，M12未授权。

| 原案例 | 本轮实际子断言 | 实际状态 / 未满足范围 |
|---|---|---|
| M11-T01 全实体/图片/设置 | 七表与设置DTO沿用；空媒体合法、登记媒体完整集合/条目检查；新增合成Android6项源码 | 宿主归档子集PASS；真实Room/非空媒体/完整编码/SAF NOT_RUN，完整案例未通过 |
| M11-T02 一致快照 | 同一协调器内七表、设置与媒体复制，原协调器宿主回归；新增真实Room阻写测试源码 | 宿主协调器回归PASS；新Android实际并发NOT_RUN，不以源码代替通过 |
| M11-T03 长度与SHA | 本地ZIP中央目录/结束边界、每条payload/manifest、目标写后关闭及全量读回 | 实际宿主PASS；真实媒体/SAF提供方NOT_RUN |
| M11-T04 失败不成功 | 写入/flush/close/读回故障、取消、重复回调、残留反馈；Android缺图/坏图/目标故障源码 | 宿主PASS；真实SAF取消、空间不足、写入/关闭失败NOT_RUN |
| M11-T05 纯手动明文 | 设置只有导出入口/明文说明，恢复未开放；无自动、密码、云同步或CSV | 源码/最终APK权限审计PASS；真实设置/系统保存器操作NOT_RUN |

最终同一构建输入：**143/143 JVM PASS**（113原＋30新，16份XML，0失败/错误/跳过，源码与结果身份完全一致）；Lint **0错误/7警告**；Debug及Android测试APK构建PASS，四任务exit0、2m57s。477项Android方法（原M09 463＋M11-A 5＋本轮9）仅编译，运行全NOT_RUN，本轮没有安装/启动APK或调用ADB。

真实RED保留：初始缺API编译失败；138项中1项取消反馈失败，修复后通过；其后两次Lint分别发现O_CLOEXEC Java字段API27、fcntlInt API30不适用API26，原失败不删。最终依据已安装官方NDK UAPI常量以API21 Os.open原子传入close-on-exec标志，不提高minSdk、不新增native依赖，不把Lint通过当作API26运行证明。

BLOCKING：完整JPEG/PNG编码覆盖尚无可信证明，未认证输入拒绝整份备份，不静默漏图；全新隔离AVD创建唯一一次exit1，真实导出门禁未完成。未知内部半文件保守保留；提供方阻塞取消、断电持久性、真机/OEM/Release及M12往返仍NOT_RUN。M10 PENDING / DEFERRED、三JPEG历史FAIL、全部UNKNOWN/NOTES与UI-C1冻结原样保留。

## M11-B 最小整改与真实导出执行登记（2026-10-20）

来源`3d018e9aedb89052983e6c54af40d03c47ecc9fb`；分支`codex/m11b-final-export-validation`。状态 **READY_FOR_REVIEW**，仅实际执行结果；正式M11由ChatGPT独立验收决定，不自行冻结，不进入M12。旧候选BLOCKED、失败AVD claim及全部历史测试保留，不回填原130条案例。

| 原ID | 本轮已执行范围 | 结果与边界 |
|---|---|---|
| M11-T01 | 七表/设置、空媒体、JPEG及RGBA/索引/灰阶16-bit/Adam7共5登记媒体；包括未被封面引用的资产 | PASS：真实Room/归档逐项一致；项目12345分、订阅付款18000分及状态不变。M12恢复不在本轮 |
| M11-T02 | 写协调器阻止并发业务/媒体变更，脱离快照后允许后续写入 | PASS：RoomBackupSnapshotSourceTest及CompleteBackupExportTest实际Android执行；不宣称跨进程或未知外部写原子 |
| M11-T03 | 私有归档、实际SAF目标关闭/读回、宿主独立ZIP/CRC/每项长度SHA及媒体原字节 | PASS：3691字节，8 ZIP条目（manifest＋7 payload），5媒体无遗漏 |
| M11-T04 | 缺图/损坏、真实系统保存器取消、可控目标写入/关闭/错误读回 | PASS：失败不显示成功；目标故障断言确认实际进入写入/读回出口。真实SAF磁盘满/提供方权限撤回/断电NOT_RUN |
| M11-T05 | 设置页只有手动明文完整导出，无恢复/密码/计划/CSV/云同步 | PASS：设置UI断言及实际MainActivity/SAF；不是M12通过 |

149个唯一JVM方法PASS（18 XML，原143保留＋6新增）；Lint0错误/7警告；Debug及Android测试APK构建PASS。最终生产App SHA `500cc5349012a20acad630ffa0c05d992f1dd466db20ad7004588f6f7c4dd92a`下16项定向AndroidPASS（Test SHA3f07c553…）；仅测试驱动修正后新Test SHA `2e6e87838ac726be42d4d4b92e5863c92b400ccb454ea15d60f920fa9f21c9c2`执行独立SAF续接方法1 PASS。不是17项同一Test包全量重跑。原SAF方法在保存前控件定位失败，旧FAIL/claim保留，由新参数绑定旧SHA/generation并严格校验合成账本的单次续接获得新PASS；不将旧FAIL改判。

原初次3/6 Android失败、两次平台诊断FAIL、JVM RED及一个辅助元数据命令exit1如实保留。生产缺陷经同进程路径证据定位并最小修复；系统保存器驱动为测试兼容问题。完整原始结果、产物身份、来源哈希和真实成功截图见[简短报告](evidence/M11B-final-verification.md)和[必要证据](evidence/m11b-final/results.json)。M10 PENDING / DEFERRED、三个JPEG历史FAIL、UI-C1冻结、Q03～Q06/所有NOTES保持；API26/真机/OEM/Release/完整备份恢复及提供方断电持久性NOT_RUN。


## M12当前执行验收（2026-10-20）

仅候选交审，未独立冻结。来源635e33a，分支codex/m12-safe-backup-restore；[集中报告](evidence/M12-verification.md)与[原始结果](evidence/m12/verification.json)。96 JVM、74 Android唯一方法PASS＝68新增＋102定向回归；73 Android使用88e9c1a…Test、最后真实SAF1项使用d426c58e…Test。旧驱动两FAIL及所有RED/初始化错误保留，不把多次执行累加为新方法数量，不重跑无关模块或改原130案例/16组补测原件。

| 原案例 | 当前真实证据 | 当前边界 |
|---|---|---|
| M12-T01 | BackupRoundTripTest、RestoreGenerationWriterTest、真实SAF：七表/设置/类别/备注/付款/状态/所有登记图片原字节往返 | PASS（候选范围；不包含M10未冻结导入） |
| M12-T02 | BackupValidatorTest20、Writer/Coordinator缺图损坏/格式/身份拒绝；旧集与指针不变 | PASS |
| M12-T03 | 候选IO/目录冲突/迟插失败回滚、切前失败、旧集保留 | 受控故障PASS；真实ENOSPC/永久阻塞读NOT_RUN |
| M12-T04 | 原异常/回退证据保留；本轮新增三个真实自身终止＋三个独立重启完整核验 | **三个指定点PASS，独立待审**；见下节，不扩展为全部Q06故障或断电 |
| M12-T05 | 新Repository依观察日重算日数、150→180预付边界及过期/STOPPED，事实不变 | PASS |
| M12-T06 | 空媒体与非空完整替换、旧独有类别不混入；未知字段/媒体/外键失败不部分激活 | PASS |

设置页真实SAF成功前，先保留两次测试驱动FAIL与两claim；经实际DocumentsUI树确认误点preview_icon后，只修测试选择器，严格旧集/claim/唯一哨兵准入下追加第三claim取得成功。旧失败不改判，当前新代STABLE/旧完整代暂保留。App SHA3bb0d801…、最终Test d426c58e…，签名/权限/四ABI/输入及结束身份见报告。API26/真机/OEM/Release/TalkBack/LMK/断电、真实磁盘耗尽、Q06-V01～V03完整故障仍NOT_RUN；Q05仍USER_CONFIRMED，当前只实测登记媒体的M11→M12子集。不自行CLOSED任何旧NOTE。M10延期与原三JPEG FAIL、UI-C1冻结不变；不进入下一模块。

## M12-T04 / Q06指定进程终止门禁追加（2026-10-20）

本轮用户明确授权三个独立合成目录各一次自身终止，来源b5691419ad8f113b837a4cb16e4c1e47227e2752，分支codex/m12-process-death-validation。[唯一集中报告](evidence/M12-process-death-verification.md)，[实际结果与输入](evidence/m12-process-death/results.json)。

| 指定点 | 真正终止 → 独立核验PID | 当前选定数据集 | 本次执行 |
|---|---|---|---|
| 切换前 | 9900 → 10092 | 完整旧A；候选B未生效 | PASS |
| SWITCHING持久化后 | 10360 → 10534 | 完整新B | PASS |
| STABLE持久化后 | 10799 → 10975 | 完整新B，不回旧记录 | PASS |

七表/设置逐字段、JPEG/PNG原字节、完整性/FK及再次打开均实际核验。三次中断调用是预期Process crashed，不能登记为JUnit PASS；三个重启调用各1项JUnit PASS，两种唯一方法由case参数区分。此前96 JVM/74 Android原证据保留，本轮未重跑。Test专用构建/Lint成功1m1s，0错误9警告；生产App原SHA3bb0d801…/默认账本不变，新Test e23be35a…同签名更新一次。没有App安装或AVD创建/启动/停止，没有访问其他设备或私人数据。

第一例许可shell回读仅LF→CRLF，监督器中断；同一许可原始485B的exec-out/设备SHA严格一致，没有重复终止，随后首次独立重启PASS。外层ADB退出码未取得仍为null/NOT_CAPTURED，不补记0；Android中断、kill-intent、旧PID消失及新进程完整核验均保留。前期Runner清单被AGP覆写已在安装前拦截、修为显式Test构建开关；名字/安装前缀解析差异均留原证据，无重复安装/故障方法。

仅上述真实子门禁待独立验收，**不自行将M12或Q06整体ACCEPTED/FROZEN/CLOSED**。坏新代后再终止、清理中终止、真实断电/LMK/ENOSPC及未测平台保持NOT_RUN；全部原报告/130案例/历史FAIL/UNKNOWN/NOTES保留。M10 PENDING/DEFERRED、UI-C1冻结不变，M13未授权。更多故障不自动执行。

## M12正式冻结登记（2026-10-20）

**正式结论：ACCEPTED / FROZEN — PASS WITH NOTES。** 来源为用户本轮正式转交的ChatGPT独立验收结论；评审提交及本次文档回写起点为`e34187632468c9c3aaf7bb5d04d30a26320a18b0`，分支`codex/m12-process-death-validation`，仓库`cc7279694-debug/property`。原M12候选提交为`b5691419ad8f113b837a4cb16e4c1e47227e2752`。下方登记的是此前实际执行证据获得独立认可，**本次没有重新运行产品测试、构建、安装或设备操作**。上文候选交付时的PENDING和历史NOT_RUN保留原语境，当前正式状态以本节为准；原报告、结果、checkpoint和130条规划原件均不改写。

### 冻结范围与既有证据

- M12安全完整恢复：严格format v1 / ZIP与七表关联校验、预览及明确覆盖确认、独立私有generation完整重建、设置和JPEG/PNG原字节、验证后AtomicFile切换、持久首次打开回退、旧写任务退休及已知前代保守清理；不修改Room v2、冻结业务算法或备份格式。
- **96项JVM PASS**（33项M12新增＋63项定向回归），[逐类结果](evidence/m12/jvm-results.json)。**74项Android唯一方法PASS**（35新增＋39回归），[逐类结果](evidence/m12/android-results.json)：73项使用Test `88e9c1a…`，最终真实SAF续接1项使用`d426c58e…`，不能写成同一Test身份的一次全量回归。
- **真实SAF恢复PASS**：[原候选报告](evidence/M12-verification.md)、[原证明](evidence/m12/saf-restore-proof.json)及[截图](evidence/m12/saf-restore-success.png)。从M11的3691B合成备份预览取消，再确认覆盖；七表/设置和五张登记JPEG/合法PNG原字节一致，旧独有类别不混入。旧两次驱动FAIL及claims、全部RED/初始化错误保持历史原样，不改成PASS。
- **M12-T04三个真实进程终止场景PASS**：[三点报告](evidence/M12-process-death-verification.md)、[原始结果](evidence/m12-process-death/results.json)。每场景一次自身终止，分别由新进程独立验证所选代七表、设置、媒体原字节、完整性/外键及再次打开；不将预期中断调用计为JUnit PASS，也不把同一核验方法的三种case重复计为三个唯一方法。

| 真实终止点 | 终止PID → 独立核验PID | 重启结果 | 独立验收登记 |
|---|---|---|---|
| 切换前 | 9900 → 10092 | 完整旧A，新候选未生效 | PASS |
| SWITCHING持久化后 | 10360 → 10534 | 完整新B，无混合数据集 | PASS |
| STABLE持久化后 | 10799 → 10975 | 保持完整新B，不回到陈旧旧集 | PASS |

### 必须保留的NOTES与未运行边界

1. **第一例ADB外层退出码仍NOT_CAPTURED**：原`exitCode=null`、`exitCodeStatus=NOT_CAPTURED_AFTER_SUPERVISOR_ASSERTION`不改。shell回读485B许可多出CR的观察原样保留；随后原始字节与设备SHA严格一致、没有重发终止，并取得首次独立重启核验。不能补写外层exit0，也不能把Android中断代码当作Windows客户端退出码。[原记录](evidence/m12-process-death/BEFORE_SWITCH/execute-command.json)、[精确字节核对](evidence/m12-process-death/BEFORE_SWITCH/permit-readback-analysis.json)。
2. **Q06不整体CLOSED**：本次认可三个指定真实终止子门禁；坏新代后再次终止、旧代清理中途终止、真实断电、真实LMK、真实磁盘耗尽/永久阻塞提供方等未运行项继续NOT_RUN。既有受控回退、未知残留保守保护的证据不替代这些故障实测。
3. **平台边界继续NOT_RUN**：API26、真机/OEM、Release/正式签名、完整TalkBack、真实权限撤回与提供方断电持久性等既有未执行事项保持，不扩大API36合成场景结论。
4. **恢复与媒体边界不变**：未知准备残留保守保留，不猜测递归清理；多份副本需要额外磁盘空间，payload预算不是整机资源硬上限；SAF阻塞取消不保证即时；登记媒体原字节恢复不等于M10严格JPEG导入通过，Q05及其他跨模块边界保持。
5. **M10 PENDING / DEFERRED**、三项历史JPEG FAIL、全部UNKNOWN/NOT_RUN/安全NOTES、原始证据及H2/C2/M10等受保护现场不变；UI-C1与M00～M09、M11原冻结结论不变。不自动合并、发布、重跑或进入M13。

本次仅对当前状态、验收登记和决策记录做文档回写，检查范围/链接/敏感信息与Git差异后提交推送当前功能分支。冻结依据是用户转交的ChatGPT正式结论，本地文档核对或辅助审阅不是新的一轮独立验收或产品测试。


## M13 首页与核心内部候选执行登记（2026-10-20）

来源`2e298ceb87a30d436a64180ccaf7d819e55ae005`，分支`codex/m13-home-core`。用户正式授权M13及原5686合成实例中的新隔离账本/测试宿主；本节是Codex真实执行，不是ChatGPT独立冻结结论。原130条案例、历史checkpoint和所有旧FAIL/UNKNOWN/NOT_RUN不改。

| 案例 | 当前执行结果 | 验证边界 |
|---|---|---|
| M13-T01 | PASS | 冻结M02/M03统一成本、真实Room读及UI写后刷新 |
| M13-T02 | PASS | 活跃范围、0/负、未来开始/结束、暂停/到期/停止、预付未生效 |
| M13-T03 | PASS | 两类/类别未舍入精确和、类别类型隔离、103＋103跨批一致读取、舍入说明 |
| M13-T04 | PASS | 空库、有记录0、相抵0、不活跃、读取错误均区分 |
| M13-T05 | PASS | 真实前台观察循环/Activity返回、写后刷新、备份恢复新代及旧读迟到；日期为注入业务时钟，未修改设备日期 |
| M13-T06 | NOT_RUN | 合成核心UI闭环子项PASS；稳定签名内部版安装、正式升级与首次真实录入未执行，Debug不能替代原完整案例 |

最终同App/Test：113 JVM＋36 Android＝149唯一方法PASS，0失败/错误/跳过；新增27 JVM＋35 Android，另1项既有跨代回归。不是全历史回归。Lint 0错误/9警告；Debug与Test构建通过、最终安装SHA/签名/UID一致。App code8/0.1.0-m13-debug SHA `6b4925409db634a126dfc56cddd6fb345d0692856c6691e5eb6017491ec668da`。截图宿主裁切修正只改androidTest，App字节不变；新最终Test重新跑36项，旧结果/截图保留本地。

[集中报告与原始证据](evidence/M13-verification.md)、[计数](evidence/m13/results.json)、[内部版准入](INTERNAL_RELEASE.md)。唯一许可设备原5686，默认账本/设置保护不变；无其他设备/故障操作。备份选择回调在隔离宿主接合成文件，本轮不重复声称系统SAF新通过。Release、稳定签名、真实重要数据、真机/API26/OEM、完整TalkBack/LMK/断电仍NOT_RUN；M10 PENDING / DEFERRED/三历史JPEG FAIL、UI-C1独立冻结、Q03～Q06与历史NOTES不变。M13等待独立验收，不自行ACCEPTED/FROZEN或进入M14。


## M13-T01～T05独立结论与T06签名前置阻塞（2026-10-20）

用户本轮转交对`775951b9373c67460b713548fba1f3f263e84b45`的独立结论：**T01～T05 PASS WITH NOTES；正式M13仍PENDING，T06 NOT_RUN**。此前执行报告/113 JVM/36 Android/截图和历史状态原样保留，不将本次核对登记为重跑PASS。

本轮T06仅只读检查Git、正式applicationId、Gradle签名接线、已知位置的文件元数据和既有Debug APK SHA。正式包名已声明为`com.cooper.lithic`，但未发现项目发布签名配置/登记，用户长期密钥位置和备份保管尚未确认；不证明整机不存在其他密钥。未读取密钥/密码，未构建、签名、生成密钥、ADB/AVD/安装/页面或账本操作。

按用户第2、6条，签名门禁BLOCKED，安装/同签名升级/真实正式页面/SAF往返及首次真实录入均NOT_RUN。[短报告与保管方案](evidence/M13-T06-readiness.md)。M10 PENDING / DEFERRED、三历史JPEG FAIL、UI-C1、Q03～Q06及全部NOTES/UNKNOWN/NOT_RUN不改。停止等待用户完成本地签名保管，不自行冻结M13或进入M14。

## M14初次交付执行登记（2026-10-21）

本节保留初交付时的实际状态和NOT_RUN语境；后续明确授权的Android补验另见下一节，不回填或改判原报告。

来源`2c5e23a7af713f6a96b345bce2a90e613458f270`；分支`codex/m14-category-search`。**READY_FOR_REVIEW**，不自授ACCEPTED/FROZEN。用户允许M13签名门槛延期后开发M14，不把完整M13/G1改成通过。原130条及历史验收不改。

| 原案例 | 当前实际证据与未执行边界 |
|---|---|
| M14-T01 同类型原子合并 | 实现/测试源码已齐；宿主SQLite引用搬移、失败回滚、无孤儿PASS。**真实Room金额/首页总额不变及并发场景NOT_RUN**。 |
| M14-T02 跨类型拒绝 | Repository在事务中校验type、保护默认类别，11项类别Android源码含拒绝/重名/回滚。**实际Android NOT_RUN**。 |
| M14-T03 当前历史搜索 | Repository复用M02/M03、页面显示明确状态；查询迟到/清空/失效重建的实际生产controller JVM PASS。**真实Room/Android状态搜索NOT_RUN**。 |
| M14-T04 特殊字符 | 4项SearchNames JVM与5个实际DAO宿主SQLite字面/注入样本PASS；参数绑定及ESCAPE。**Android执行NOT_RUN**。 |
| M14-T05 默认排序及分页 | 当前startedOn、订阅paidOn排序保留；历史冻结heldDays降序，实际SQL105条同键分页与“早买早结束”RED→GREEN。**真实Room分页NOT_RUN**。 |

最终136个唯一JVM方法PASS（113回归＋23本模块），0失败/错误/跳过。Lint0错误9警告，App/Test Debug构建PASS；28项新增Android源码全部只编译，不计产品PASS。类别/七表/设置/原字节媒体完整format1往返及恢复后的Search/Report等值测试已写，**NOT_RUN**。APK/证书与权限核查PASS只代表产物检查，不代表安装、启动或业务路径通过。

授权原`Lithic_M11B_Final_API36_20261020 / emulator-5686`主机进程0、监听0；不启动/重建。ADB、安装及AVD操作0次，截图/大字体/TalkBack仍NOT_RUN。[短报告](evidence/M14-verification.md)、[唯一测试统计/XML](evidence/m14/results.json)、[原始日志](evidence/m14/logs/)。原RED、初次KSP/测试导入构建失败保留，不伪称产品PASS；本地辅助代码审阅不代替ChatGPT独立验收。

M10延期及三历史JPEG FAIL、M13-T06延期、Q03～Q06和全部历史NOTE/UNKNOWN保持；不合并main，不进入M15。

## M14 Android补验当前登记（2026-10-21）

实现提交`730fd2a`，分支`codex/m14-category-search`。用户另行授权全新隔离`Lithic_M14_API36_20261021 / emulator-5672`（API36/x86_64）完成定向验收。**READY_FOR_REVIEW；未经ChatGPT独立验收，不授予ACCEPTED / FROZEN。**

| 原案例 | 本次实际Android证据 |
|---|---|
| M14-T01 同类型原子合并 | PASS：两类型类别引用搬移、当前/历史记录、金额/付款/状态不变、总额不变、源删除失败回滚；真实竞争NOT_RUN。 |
| M14-T02 跨类型拒绝 | PASS：拒绝跨类型/标准名冲突/陈旧预览，fallback受保护且可接收同类型合并；重复合并及旧代拒写保持。 |
| M14-T03 当前历史搜索 | PASS：真实Room状态搜索、名称限定、窗口重建；隔离LithicApp/Nav3旅程确认当前/历史状态和精确历史详情路由。 |
| M14-T04 特殊字符 | PASS：真实Room下`%`、`_`、引号、`!`及注入文本按字面匹配；原JVM/宿主SQL证据保持。 |
| M14-T05 默认排序及分页 | PASS：157条混合搜索、105条列表及同冻结时长历史分页无重复遗漏；当前startedOn、历史heldDays、订阅真实paidOn排序。 |

**原28＋新旅程1＝29个唯一Android方法PASS**，逐方法完成码0、外层exit0；不是全历史回归，8张截图不另计方法。原28身份保留：类别Repository11、搜索/排序12、format1往返1、Compose4。往返实际核对七表/设置/媒体原字节、恢复后的Search/Report等值及旧代拒写，使用受控文件目标，不是新的真实SAF验证。原136 JVM保持此前执行证据；当前Lint0错误9警告。

三处原测试仅修正空/默认类别保留契约、完整句子断言与合并语义节点选择，并加强集合/汇总/完整状态断言；早期真实失败与原始日志保留。补验无产品/schema/引擎/format变更。最终App SHA `59d45ad38b467e5c4bb895c71feab3315bb872d670177ab7457033179c4b35b9`未变，最终Test SHA `7ef8915685801fbbc3cdeb76ab80ff2e4b6d31264aa761cc95071c91ce1e6ff7`承载最终29项；安装回读及源码/逐方法身份见[补验报告](evidence/M14-Android-verification.md)、[结果](evidence/m14-android/results.json)和[证据索引](evidence/m14-android/README.md)。

专用环境根`E:/CODEX/.codex/research-env/lithic-m14-android-20261021`，AVD创建1/启动1、App安装1、Test首次安装1＋同签更新2；没有旧设备ADB、清数据或停止。结束时同boot在线，合成owner已清理、no_backup为空，files保留profileInstalled及m14-evidence截图，cached Test进程5993（UID10217）未强杀。8张图均为隔离ComponentActivity中的真实LithicApp/Nav3；360dp/搜索200%仅LocalDensity测试画布，系统font1/density420未改。类别/合并页200%、真实IME、MainActivity/发行入口、真实竞争、新的真实SAF、API26/物理设备/OEM/Release仍NOT_RUN。

原M14报告/results/checkpoint不改。M13-T06 NOT_RUN / DEFERRED、G1未整体通过，M10 PENDING / DEFERRED及三历史JPEG FAIL、UI-C1独立冻结、Q03～Q06和所有历史NOTE/UNKNOWN保持；不合并main，不进入M15。本地辅助审阅不冒充ChatGPT独立验收。

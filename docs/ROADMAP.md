# V1 模块路线与执行闸门

**2026-10-23 M22候选停止点**：READY_FOR_REVIEW，Android整体门禁BLOCKED。137 JVM PASS；37 Android唯一方法最新36 PASS/1 FAIL，正式规模长历史1项NOT_RUN；SystemUI ANR原帧及窗口证据阻断后续验证。仅原5712启动1/正常结束1、AVD和旧15锁/旧图保留，新14锁保留。[报告](evidence/M22-verification.md)。未冻结M22、不进入M23；下方M21“未授权M22”为原冻结时点记录。

**2026-10-23 M21正式限定冻结**：用户正式转交ChatGPT对`f2e7ea73924a5671ef0da78b3b1ecc8f38634893`的独立复验，M21 **ACCEPTED / FROZEN — PASS WITH NOTES**。T01～T06、原395 JVM、首轮31 Android/无重跑、独立查看的4张隔离Compose截图及Lint0错误25警告获限定认可；仅真实里程碑规则、水位、同日摘要、受保护点击接口与Room5/backup4已验证实现。见[正式登记及完整NOTES](ACCEPTANCE_MATRIX.md#m21正式限定冻结登记2026-10-23)。本轮仅文档/失效链接回写，不运行测试、构建或设备；补验构建/Lint原件未归档，不以原候选Lint冒充。31项外Android、真实系统发布/送达/MainActivity点击/重启、15个锁文件与5712保留现场、M20/M10/M13-T06及Q03/Q04/G1/G2/G3原边界不变。M22未授权，模块冻结不等于发行门禁通过。下方交付记录保留历史语境。

**2026-10-23 M21 Android补验**：原31个唯一方法首轮31 PASS，4张真实隔离Compose截图，原395 JVM不重跑；新API36/5712已正常退出并保留AVD，历史设备未接触。[补验报告](evidence/M21-Android-verification.md)。只为本次Room/Worker/Intent/恢复/TestDriver/Compose的限定证据，真实系统送达/点击/后台重启及未覆盖子边界仍NOT_RUN；M21保持READY_FOR_REVIEW，G3不通过，不进入M22。下方2026-10-22离线NOT_RUN为历史记录。

**2026-10-22 M21当前授权**：从M20冻结`3e8f5fd`实施真实时间里程碑、抑制水位和同日摘要候选，Room5/backup4，Q03 USER_CONFIRMED。当前READY_FOR_REVIEW，395 JVM PASS、Lint0错误25警告、App/Test构建成功，结果见[M21报告](evidence/M21-verification.md)；没有设备授权，31新增Android方法全部NOT_RUN，独立验收PENDING，不进入M22。M20限定冻结及额外平台失败/收口UNKNOWN、M10/M13-T06延期、Q04/G1/G2/G3原边界不扩大。下方原阶段说明保留历史。

**2026-10-22 M20当前覆盖**：依据用户转交的ChatGPT独立结论，评审`d3776e0b2852428911ad0f09296680046211b32d`，M20 **ACCEPTED / FROZEN — PASS WITH NOTES**。T01～T06、此前238 JVM、原22 Android最终PASS及Lint0错误25警告获限定认可；仅冻结续费Planner、权限/偏好、有限幂等回执、WorkManager集成及Room4/backup3兼容实现，不保证OEM后台准点。25唯一方法最新24 PASS/1 FAIL、首轮21 PASS/1 FAIL、真实平台JUnit两次FAIL及SystemUI截图不改；第二次平台收口仍BLOCKED，周期任务/OS Job/指定通知/Owner收口UNKNOWN，新App进程/默认generation事实及来源UNKNOWN保留。M20 AVD为保留/隔离测试现场，未经重新明确授权不复用或清理。本轮仅文档登记，不重跑测试/构建或操作设备。见[正式登记](ACCEPTANCE_MATRIX.md#m20正式限定冻结登记2026-10-22)。M10/M13-T06延期、G1/G2未整体通过、Q04 PROPOSED、G3未通过及全部历史NOTES/NOT_RUN保持，M21未授权。下方旧登记仅表示原时点。

**2026-10-22当前覆盖**：M19已由用户正式转交ChatGPT独立源码、JVM/Android原始证据、截图及设备身份验收结论，评审`21d6230543ac9c0c491d8da590d230396952f571`，**ACCEPTED / FROZEN — PASS WITH NOTES**。T01～T06、此前109 JVM PASS、29 Android PASS、Lint0错误23警告及8张真实隔离截图获限定认可，见[正式登记](ACCEPTANCE_MATRIX.md#m19正式冻结登记2026-10-22)。仅冻结类别分析、真实资金年份归属、客观年度回顾及对应只读映射，Room3/backup2/冻结算法不改。本轮仅文档登记，不重跑测试/构建或操作设备。M10延期、M13-T06延期/G1未整体通过、G2未自动通过、Q02完整16组及全部历史RED/FAIL/UNKNOWN/NOT_RUN/NOTES保持。登记后停止，M20未授权。下方旧日期授权为历史。

**2026-10-20授权历史（现状以页首为准）**：M11、M12已由用户转交独立验收限定冻结PASS WITH NOTES；当前授权M13首页汇总与核心内部Debug候选，依赖M07/M09/M12。M10仍PENDING / DEFERRED且现场不动，不合并Worker/Stage V2/JNI或独立UI-C1分支。M13-T06稳定签名/首次真实记录门槛单独保留，M14未授权。

来源：[原总计划](planning/original-v1/lithic_v1_plan/docs/MASTER_PLAN.md)、[原module-index](planning/original-v1/lithic_v1_plan/docs/module-index.json)及24份原模块计划。保留M00～M23顺序，不用早期Module 0～8粗路线。

本表保留原M00～M23顺序与精确前置依赖。M00～M09、M11/M12及M14已限定独立冻结PASS WITH NOTES；M10延期，M13-T06延期且整体发行门槛未通过，M15已依据用户转交的独立结论限定冻结PASS WITH NOTES；M16～M21均已获限定独立冻结PASS WITH NOTES；M22另获明确授权并候选交付，Android门禁未完成。M09范围仍见[M09契约](planning/M09_CONTRACT.md)；其原执行报告的READY_FOR_REVIEW为历史，冻结回写57de897为M11-A起点，M11-B从79771fc继续。实际进度与验证只在[CURRENT_STATE](CURRENT_STATE.md)及验收登记维护；全部历史NOTES保留。

## 执行顺序与依赖解释

原计划按编号逐模块授权与停止；M11-A/B从M09先行的历史有限例外保留。M09两流程风险的实施/验收结论按原记录保留，Q03～Q06与后续完整媒体、备份恢复、汇总和通知边界不变。M12冻结历史保持；用户此前明确授权M14在M13-T06延期下先行，现M14限定冻结，不据此扩大M13/G1通过范围，M15现限定冻结，M16、M17已限定独立冻结，仍不恢复M10设备实验；M18现已限定独立冻结；M19及M20现限定独立冻结，G1/G2未整体通过、Q04 PROPOSED、G3未通过；M21冻结保持；M22当前候选状态见顶部，M23未授权。

24计划的“必须先验收”与原JSON逐条一致，无前向依赖或环。后续还需读原计划、引用规格、适用验收、当前澄清和实际代码，不能只凭路线表开始工程。

## 模块交付边界

| 模块 | 交付范围与原计划 | 原包前置验收依赖 | 验收概要与停止边界 |
|---|---|---|---|
| M00 | 文档、来源、记忆、问题与验收基线 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m00.md) | 无 | 原交付仅文档；已独立验收冻结，保留验收历史 |
| M01 | Android空壳、版本锁、debug身份与权限 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m01.md) | M00 | 断网冷启动、合并Manifest无网络权限；不写业务 |
| M02 | 金额/日期/项目纯引擎 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m02.md) | M01 | 首日、同日结束、负成本、历史/结束后结算；不写CRUD UI |
| M03 | 订阅周期/状态/预付/平均引擎 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m03.md) | M02 | 150/30=5；生效首日实际1天；不伪造续费/经过时间 |
| M04 | Room v1、数据集、稳定ID、事务 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m04.md) | M02、M03 | 外键/幂等/事务；不做完整业务页面 |
| M05 | 三导航与基础视觉/空状态 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m05.md) | M01、M04 | 仅现在/回看/设置；不添加伪造业务 |
| M06 | 项目CRUD与逐笔金额 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m06.md) | M04、M05 | 0元保存重启、失败保留输入；不扩分析 |
| M07 | 结束/回收/删除/历史修正 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m07.md) | M06 | 结束与回收同事务、不重复回收；无回收站 |
| M08 | 订阅/真实续费与schema v2 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m08.md) | M03、M04、M05、M07 | v1→v2保旧记录，不自动付款 |
| M09 | 暂停/恢复/到期/停止 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m09.md) | M08 | 无后台仍正确EXPIRED、贡献0；不替服务商取消 |
| M10 | 单封面与本地资源生命周期 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m10.md) | M06、M09 | PENDING / DEFERRED；历史JPEG FAIL及研究现场保留 |
| M11 | 完整逻辑导出format v1 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m11.md) | M10（完整阶段）；M11-A获授权从M09先行 | M11正式限定冻结；完整手动导出/format1/登记JPEG和PNG原字节，无CSV/自动备份 |
| M12 | 校验/覆盖恢复/故障回退 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m12.md) | M11 | 已限定冻结PASS WITH NOTES，原往返和三个指定终止点通过；完整Q06仍不整体关闭 |
| M13 | 首页与安全核心内部版 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m13.md) | M07、M09、M12 | 候选READY_FOR_REVIEW：统一引擎、两类/类别/刷新及合成备份恢复已验证；T06稳定签名NOT_RUN，未冻结/非完整V1 |
| M14 | 类别/搜索/分页/排序 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m14.md) | M13 | ACCEPTED / FROZEN — PASS WITH NOTES；T01～T05及原136 JVM/最终29 Android/8图获限定认可；M13-T06延期例外不等于G1通过；当前用户另行明确授权M15 |
| M15 | 历史与跨当前历史长期榜 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m15.md) | M14 | ACCEPTED / FROZEN — PASS WITH NOTES；T01～T06及此前133 JVM/32 Android/7图获限定认可；M16已另行限定冻结 |
| M16 | 替换/折抵、schema v3/format v2 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m16.md) | M15 | ACCEPTED / FROZEN — PASS WITH NOTES；T01～T06及原252 JVM/50 Android/7图获限定认可，schema3/format2；原冻结范围保留 |
| M17 | 项目曲线/订阅阶梯图 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m17.md) | M16 | ACCEPTED / FROZEN — PASS WITH NOTES；T01～T05及原144 JVM/28 Android/9图、Lint0/9获限定认可；不扩大Q02的M18门禁，见[正式登记](ACCEPTANCE_MATRIX.md#m17正式冻结登记2026-10-21) |
| M18 | 总成本历史与事件 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m18.md) | M17 | ACCEPTED / FROZEN — PASS WITH NOTES；T01～T05及此前174 JVM/17 Android/10图、Lint0/23获限定认可；首次14/3与NOT_CAPTURED保留，Q02完整16组不关闭；见[正式登记](ACCEPTANCE_MATRIX.md#m18正式冻结登记2026-10-21) |
| M19 ACCEPTED / FROZEN — PASS WITH NOTES | 类别/年度/客观回顾 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m19.md) | M18 | 年度按真实日期、类型分开，不生成建议 |
| M20 ACCEPTED / FROZEN — PASS WITH NOTES | 续费通知、schema v4/format v3 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m20.md) | M19 | T01～T06限定PASS；238 JVM/原22 Android最终PASS、Lint0/25；额外平台FAIL及第二次收口BLOCKED/UNKNOWN保留，G3未通过 |
| M21 ACCEPTED / FROZEN — PASS WITH NOTES | 实际时间里程碑/低打扰 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m21.md) | M20 | T01～T06限定通过，原395 JVM/首轮31 Android/4隔离截图；平台及其他NOT_RUN保留，G3不整体通过；M22现另行候选交付 |
| M22 READY_FOR_REVIEW / Android BLOCKED | 小屏/字体/无障碍/长历史 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m22.md) | M21 | 已实施最小修正；正式规模与完整UI验证未完成，不加业务功能、不自行冻结 |
| M23 | 可靠性/签名升级/正式V1 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m23.md) | M22 | 全发行schema/旧备份/断网/故障/签名；独立验收后冻结 |

## 阶段门槛

| 门槛 | 原规划节点 | 所需结果 |
|---|---|---|
| G0 | M05 | 领域、存储和骨架可验证，主要用合成数据 |
| G1 | M13 | 两条业务闭环、完整备份恢复、首页及稳定应用身份/签名；允许长期真实数据 |
| G2 | M19 | 类别、搜索、历史、替换、曲线和年度回顾 |
| G3 | M21 | 两种通知的授权、去重、过期、重建验证；不承诺省电下准点 |
| G4 | M23 | 升级、旧格式、故障、小屏、长历史、签名交付与独立验收 |

以上仍是计划门槛，尚未宣告任何门槛整体通过；M05正式冻结不等于后续G1及全部门槛均已通过，未执行的验证仍为 NOT_RUN。M06/M09 Debug 演示不等于 G1 可长期使用。

## 版本配对计划

| 模块 | schema 计划 | backup format 计划 | 迁移责任 |
|---|---|---|---|
| M04 | v1 | 尚无备份 | schema 导出与 DAO/事务测试 |
| M08 | v2 | 尚无备份 | v1→v2，项目/事件/媒体不变 |
| M11/M12 | v2 | v1 | 全业务、图片、设置完整往返 |
| M16 | v3 | v2 | 替换关系、basis/exchangeGroupId；旧 format 默认 CASH |
| M20 | v4 | v3 | 通知去重水位；旧备份不补过去提醒 |
| M21限定冻结 | Room5 | backup4/minReader4/schema5 | 已验证实现限定认可；旧1/2/3严格解析、原proof认证后合并水位；内部指针版本1不变 |
| M22～M23 | 以实际批准变化为准 | 有变化就显式迁移 | 不通过漏字段或破坏性回退规避迁移 |

进度只在 [CURRENT_STATE.md](CURRENT_STATE.md)维护。所有规格问题在进入受影响模块前处理；变更先登记 [DECISIONS.md](DECISIONS.md)。

## V1产品完善（2026-10-25用户决策）

独立分支`codex/lithic-v1-product-completion`的P01为 **IMPLEMENTED — PENDING FINAL PRODUCT REVIEW**，见[产品记录](planning/LITHIC-V1-PRODUCT-COMPLETION.md)与[P01验证](evidence/LITHIC-P01-Core-Experience.md)。P02浏览/回看现为IMPLEMENTED — PENDING FINAL PRODUCT REVIEW（89唯一JVM PASS、17 Compose仅编译/NOT_RUN），见[P02报告](evidence/LITHIC-P02-Browse-Review.md)；P03设置、P04集中真实体验仍为建议/未实施，本轮无设备操作。原M22仍BLOCKED / CHANGES REQUIRED、剩余验收DEFERRED，不用产品完善替代原门禁或自动进入M23，历史冻结和延期结论不改。

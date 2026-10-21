# V1 模块路线与执行闸门

**2026-10-21当前覆盖**：M15已依据用户转交的ChatGPT独立验收登记 **ACCEPTED / FROZEN — PASS WITH NOTES**，评审`ec0b7286b4ca41f0eac84fd4f0b6885c27c5a473`，分支`codex/m15-history-review`。T01～T06、此前133 JVM/32 Android/7图获限定认可，边界见[正式登记](ACCEPTANCE_MATRIX.md#m15正式冻结登记2026-10-21)。本轮仅文档回写，未重跑测试/构建或操作设备。M14等原冻结保持，M13-T06 NOT_RUN / DEFERRED、G1未整体通过、M10 PENDING / DEFERRED与所有历史NOTES保持；M16未授权，不合并main。下方旧日期授权与原依赖表属于历史。

**2026-10-20授权历史（现状以页首为准）**：M11、M12已由用户转交独立验收限定冻结PASS WITH NOTES；当前授权M13首页汇总与核心内部Debug候选，依赖M07/M09/M12。M10仍PENDING / DEFERRED且现场不动，不合并Worker/Stage V2/JNI或独立UI-C1分支。M13-T06稳定签名/首次真实记录门槛单独保留，M14未授权。

来源：[原总计划](planning/original-v1/lithic_v1_plan/docs/MASTER_PLAN.md)、[原module-index](planning/original-v1/lithic_v1_plan/docs/module-index.json)及24份原模块计划。保留M00～M23顺序，不用早期Module 0～8粗路线。

本表保留原M00～M23顺序与精确前置依赖。M00～M09、M11/M12及M14已限定独立冻结PASS WITH NOTES；M10延期，M13-T06延期且整体发行门槛未通过，M15已依据用户转交的独立结论限定冻结PASS WITH NOTES；M16未授权。M09范围仍见[M09契约](planning/M09_CONTRACT.md)；其原执行报告的READY_FOR_REVIEW为历史，冻结回写57de897为M11-A起点，M11-B从79771fc继续。实际进度与验证只在[CURRENT_STATE](CURRENT_STATE.md)及验收登记维护；全部历史NOTES保留。

## 执行顺序与依赖解释

原计划按编号逐模块授权与停止；M11-A/B从M09先行的历史有限例外保留。M09两流程风险的实施/验收结论按原记录保留，Q03～Q06与后续完整媒体、备份恢复、汇总和通知边界不变。M12冻结历史保持；用户此前明确授权M14在M13-T06延期下先行，现M14限定冻结，不据此扩大M13/G1通过范围，M15现限定冻结，不自动进入M16或恢复M10设备实验。

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
| M15 | 历史与跨当前历史长期榜 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m15.md) | M14 | ACCEPTED / FROZEN — PASS WITH NOTES；T01～T06及此前133 JVM/32 Android/7图获限定认可；M16未授权 |
| M16 | 替换/折抵、schema v3/format v2 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m16.md) | M15 | 无环/自指/一对多；3笔折抵同事务，迁移旧格式 |
| M17 | 项目曲线/订阅阶梯图 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m17.md) | M16 | 后事件不改发生前值、负值/断档；不预测 |
| M18 | 总成本历史与事件 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m18.md) | M17 | 观察日有效贡献合计，不连接停订空档 |
| M19 | 类别/年度/客观回顾 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m19.md) | M18 | 年度按真实日期、类型分开，不生成建议 |
| M20 | 续费通知、schema v4/format v3 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m20.md) | M19 | 3天候选、复核、去重、恢复水位；无精确闹钟 |
| M21 | 实际时间里程碑/低打扰 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m21.md) | M20 | 预付不冒充365天，不补旧节点，不游戏化 |
| M22 | 小屏/字体/无障碍/长历史 [原计划](planning/original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m22.md) | M21 | 360dp/200%字体/48dp触控；不加功能 |
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
| M21～M23 | 以实际批准变化为准 | 有变化就显式迁移 | 不通过漏字段或破坏性回退规避迁移 |

进度只在 [CURRENT_STATE.md](CURRENT_STATE.md)维护。所有规格问题在进入受影响模块前处理；变更先登记 [DECISIONS.md](DECISIONS.md)。

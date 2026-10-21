# M18 总成本历史执行契约

## Goal / Scope / Sources

用户本轮完整指令授权在 `codex/m18-total-cost-history` 从M17冻结登记 `7e74df960e994e3a36f4417f7d83b07ead63763f` 实施。起点本地与远端一致、工作区干净，目标分支此前不存在。原件保持不变：[M18原计划](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m18.md)、[领域C01～C04/C06/C08～C11/C13](original-v1/lithic_v1_plan/docs/specs/DOMAIN_CONTRACT.md)、[架构A03/A06](original-v1/lithic_v1_plan/docs/specs/ARCHITECTURE.md)、[UI U01/U09](original-v1/lithic_v1_plan/docs/specs/UI_SPEC.md)、[已确认Q02](M02_CONTRACT.md)。

在“回看”增加总成本趋势，默认总线、可展开项目与订阅贡献，共用日期轴/金额尺度、负值与文字图例。精确选日显示三项未展示舍入金额及“该日相关事件”，能进入准确详情并返回；保留dirty/pending/恢复守卫。历史是按现存已录入/纠正事实重建的参考指标，不是当天扣款、完整生活支出或不可变快照。

## Computation / Snapshot / Events

- `LedgerSnapshot` 仅封装已有 `OverviewSnapshot` 完整事实与业务事件索引；`CostTimelineEngine.evaluate(snapshot, dates)`逐日期复用冻结引擎/首页汇总口径，不复制金额公式。项目仅`contributesToCurrentTotal`贡献；订阅仅该日ACTIVE的`currentRate`贡献。Long分、BigDecimal及既有溢出检查保持，展示时才舍入。
- 同一generation、业务截止日、协调器和Room事务捕获全部当前/历史项目、金额、订阅、付款、状态、类别及替换校验。稳定分批读完整事实，不拿首50条当全账本，不逐日期查库；事务释放后后台计算，不读封面原图。复用ReportRepository事实校验，损坏引用或交易组拒绝读取。
- 项目结束日日末退出总额；结束后结算保留事件而不重进总额。未来预付款不提前改价，覆盖排他边界、暂停/停止/重开按冻结状态历史计算。M16只计既存三行事实，不额外制造取得总价。
- 事件按真实业务日；付款/覆盖生效/结束/状态分别说明，多个边界不伪造成多次付款。替换边不产生金额；以旧换新保留所属记录及非现金标识。同日全部事件可按50条窗口继续查看；不声称某一笔唯一导致跳变。

## Sampling / Refresh

- `TimelineSampler`基础绘图预算240，后台流式遍历完整已记录日期范围，以分桶保留总线、项目、订阅各自最小/最大值；合并所有真实边界及±1日邻点、起止和截止日。三线使用同一保留日期集合，抵消时不丢分项峰。关键日期超预算全部保留，不截断历史。精确选日回原快照，不用插值。
- 不持久化每日真值或缓存。日级求值工作量随日期数D与冻结引擎处理事实量F增长，约O(D×C_frozen(F))，C_frozen包含单日冻结引擎的排序/结构校验与求值；另有事件索引及保留点排序；临时仅保留分桶极值/关键点，而非完整日级对象列表。20年样本只验证该范围，不能声称无限历史恒定耗时。后台协作取消必须检查，UI线程不做长跨度枚举。
- generation作用域ViewModel以请求序号隔离旧结果，快照、截止日、选日、精确三项金额和事件匹配发布；保留选日/拆分意图。写后、纠错/删除、分类、跨日/前台与恢复后重建；失败明确尚未刷新，空账本/无贡献/真0/正负抵消分别表达。浏览不写事实。

## Out of Scope / Constraints

Room schema3、backup format2、M02/M03算法、M16交易保护、M12恢复和媒体协议不变；不增加依赖/表/迁移/备份字段/后台统计任务。无年度统计、预测、评分、AI/云/通知、M10研究或M19。M10 PENDING / DEFERRED、三JPEG历史FAIL、M13-T06 NOT_RUN / DEFERRED、G1未整体通过及所有历史NOTES保持。仅登记本轮Q02/M18子断言，不整体关闭Q02或16组完整跨模块门禁。

## Acceptance / Verification / Stop

原M18-T01固定种子独立逐项参照；T02结束/暂停/到期/未来预付/停止重开和Q02；T03同快照同日精确等于首页；T04完整20年日级参照保留三线峰谷/抵消/邻点且非显示日精确；T05多事件完整、无伪归因、跨50条和准确详情返回。必要验证包括并发排队快照、刷新/失败/迟到/换代、纠错删除重算、只读、format2往返及M17图表回归。

新增可离线执行行为先真实RED再GREEN；既有正确行为直接回归。执行定向JVM、Lint、Debug App/Test构建，Android仅在原 `Lithic_M14_API36_20261021 / emulator-5672` 同实例仍在线且身份核验通过后进行。原实例退出时不发设备操作、不启动/重建，仅登记NOT_RUN，继续离线部分。新截图必须来自本轮真实运行；不得用M17截图代替。LocalDensity/隔离宿主/注入时钟/受控文件均不冒充MainActivity、系统大字、真实午夜或新SAF。

交付一份短报告、必要原始结果与checkpoint，更新唯一当前状态和验收登记；只推本分支，`READY_FOR_REVIEW — M18`后停止等待独立验收，不自行冻结、不合并main、不进入M19。

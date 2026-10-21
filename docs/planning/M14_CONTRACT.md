# M14 — 类别、名称搜索与列表效率

日期：2026-10-21；用户正式授权一次完成本模块。来源 `2c5e23a7af713f6a96b345bce2a90e613458f270`，来源分支 `codex/m13-t06-release-readiness`，实施分支 `codex/m14-category-search`。复用干净的既有隔离 worktree，不修改原 M10 工作区。

## Goal / Scope

两套类别各自新建、改名、同类型合并；首页汇总随映射刷新。顶部轻入口搜索当前及历史项目/订阅名称，显示真实状态。保留当前项目 startedOn 升序；已结束项目按冻结持有天数降序，订阅最近真实 paidOn 降序及 ID 次排序；每页最多50条，刷新已加载窗口使用同一次一致快照。

复用已有 CategoryNames 标准化、唯一索引、LocalWriteCoordinator、Room 事务、M02/M03、Nav3 守卫及 M11/M12。合并先预览影响数，确认后核对源/目标及影响基线，改引用后删源；失败完整回滚。默认“其他”不可改名或作为合并源，可接收同类型合并。名称搜索只参数绑定与转义 LIKE，不使用 FTS。

## Constraints / Dependencies

- M13-T01～T05独立 PASS WITH NOTES；用户明确允许在T06 NOT_RUN / DEFERRED、G1整体未通过的前提继续M14，不回填完整M13通过。
- Room schema v2、backup format v1、冻结领域算法、旧导航/恢复协议不改；无新增依赖。原M14计划及130条原案例保持原字节。
- M10 PENDING / DEFERRED；不合并Worker/Stage V2/JNI或独立UI-C1代码。不签名、不录入个人账本、不操作H2/C2/M10等保留现场、不合并main、不进入M15。
- 当前M14设备许可仅复用5686原合成实例。2026-10-21主机只读检查：模拟器进程0、目标监听0，原实例缺失；不得重启/新建，Android实际执行与截图NOT_RUN。设备身份事实不由旧报告推定。

## Implementation / Verification

- [x] 类别Repository/DAO：标准名冲突、fallback、同类型原子合并、跨类型拒绝、陈旧预览、事务失败回滚、冻结金额/付款/状态不变。
- [x] 名称SearchRepository/DAO：当前历史两类型、M03状态、%/_/引号/转义符字面匹配、绑定参数、稳定分页、查询及观察日绑定cursor。
- [x] Compose类别管理/搜索、generation隔离ViewModel、原返回守卫、首页/列表/编辑类别刷新；窄屏大字体使用可滚动布局。
- [ ] M14-T01～T05定向测试；类别变更后format1完整备份恢复一致性。执行JVM、Lint、Debug和Android测试源码编译。未运行的Room/Android/截图明确NOT_RUN，不用编译代替。
- [ ] 差异、schema/归档/敏感信息检查，状态、验收登记、短报告与checkpoint同步，仅提交推送M14分支，READY_FOR_REVIEW后停止。

纯规则先保留真实RED→GREEN；已存在排序通过行为直接回归，不故意制造失败。设备不可用不新建测试系统。

执行结果见[短报告](../evidence/M14-verification.md)。后两项中的实际Android及运行截图仍NOT_RUN；代码交审不等于这些案例通过。

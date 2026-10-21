# M14 — 类别、名称搜索与列表效率

日期：2026-10-21；用户正式授权一次完成本模块。来源 `2c5e23a7af713f6a96b345bce2a90e613458f270`，来源分支 `codex/m13-t06-release-readiness`，实施分支 `codex/m14-category-search`。复用干净的既有隔离 worktree，不修改原 M10 工作区。

## Goal / Scope

两套类别各自新建、改名、同类型合并；首页汇总随映射刷新。顶部轻入口搜索当前及历史项目/订阅名称，显示真实状态。保留当前项目 startedOn 升序；已结束项目按冻结持有天数降序，订阅最近真实 paidOn 降序及 ID 次排序；每页最多50条，刷新已加载窗口使用同一次一致快照。

复用已有 CategoryNames 标准化、唯一索引、LocalWriteCoordinator、Room 事务、M02/M03、Nav3 守卫及 M11/M12。合并先预览影响数，确认后核对源/目标及影响基线，改引用后删源；失败完整回滚。默认“其他”不可改名或作为合并源，可接收同类型合并。名称搜索只参数绑定与转义 LIKE，不使用 FTS。

## Constraints / Dependencies

- M13-T01～T05独立 PASS WITH NOTES；用户明确允许在T06 NOT_RUN / DEFERRED、G1整体未通过的前提继续M14，不回填完整M13通过。
- Room schema v2、backup format v1、冻结领域算法、旧导航/恢复协议不改；无新增依赖。原M14计划及130条原案例保持原字节。
- M10 PENDING / DEFERRED；不合并Worker/Stage V2/JNI或独立UI-C1代码。不签名、不录入个人账本、不操作H2/C2/M10等保留现场、不合并main、不进入M15。
- 初交付仅许可原5686实例；其缺失时设备操作0、Android/截图NOT_RUN的历史证据保留。用户后续另行明确授权唯一全新隔离`Lithic_M14_API36_20261021 / emulator-5672`（API36/x86_64），专用根`E:/CODEX/.codex/research-env/lithic-m14-android-20261021`；创建/启动各1次、App安装1次、Test初装1次及同签更新2次已执行，只用于合成数据。不访问旧设备，不清数据/停止，不自行重复创建或启动。

## Implementation / Verification

- [x] 类别Repository/DAO：标准名冲突、fallback、同类型原子合并、跨类型拒绝、陈旧预览、事务失败回滚、冻结金额/付款/状态不变。
- [x] 名称SearchRepository/DAO：当前历史两类型、M03状态、%/_/引号/转义符字面匹配、绑定参数、稳定分页、查询及观察日绑定cursor。
- [x] Compose类别管理/搜索、generation隔离ViewModel、原返回守卫、首页/列表/编辑类别刷新；窄屏大字体使用可滚动布局。
- [x] M14-T01～T05定向测试；原28项Android与新截图旅程1项全部PASS，涵盖真实Room及类别变更后受控文件目标format1完整往返。原JVM/构建证据保留，当前Lint0错误9警告。8张隔离ComponentActivity截图；360dp/搜索200%仅LocalDensity画布，不扩称系统字号/全面大字验证。真实竞争、新的真实SAF/IME及其他平台明确NOT_RUN。
- [x] 差异、schema/归档/敏感信息及169个本地链接已检查，状态、验收登记、短报告与checkpoint同步。交付仅允许提交推送M14分支，实际SHA与远端一致性由最终Git回执确认，READY_FOR_REVIEW后停止。
- [ ] ChatGPT独立复验；未经确认不自行冻结或进入M15。

纯规则先保留真实RED→GREEN；已存在排序通过行为直接回归，不故意制造失败。原三处测试契约/语义选择修正及早期失败日志保留，不改产品代码；设备许可用尽不自行重建或重启。

初交付[短报告](../evidence/M14-verification.md)、结果及checkpoint保持原NOT_RUN语境。当前补验见[Android报告](../evidence/M14-Android-verification.md)、[结果](../evidence/m14-android/results.json)与[新checkpoint](../checkpoints/2026-10-21-M14-Android.md)。仍只交付READY_FOR_REVIEW，等待独立验收，不自授冻结或进入M15。

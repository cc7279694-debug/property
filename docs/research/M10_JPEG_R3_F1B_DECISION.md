# M10-R3-F1B — 决策与停止边界

2026-10-17。**BLOCKED — M10-R3-F1B Android Normal**。

## Decision

本轮保持停止，只交付安全预检及只读证据；不安装/启动APK，不试图通过解绑、杀研究进程、清数据或删除未知stage改善状态，不运行任何normal或其他案例。只推F1B研究分支，等待ChatGPT独立审查。

## Context / evidence

F1-A已由用户转交独立PASS WITH NOTES，范围只到研究代码/JVM/构建，N01～N03继续有效。F1B安装前门禁发现目标WorkerService ServiceRecord，初始监督器以任何目标服务记录为由拒绝，可能过严。实际停止时无Worker进程；补充只读记录为app=null、无目标Bindings段，且stage namespace不存在。

[当时停止理由](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked.json)不是已证实活跃Worker/待处理绑定；[逐项分类](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/bounded-result.json)将安全闲置状态保持UNKNOWN。**这是监督器门禁及证据分类的停止点，不是新APK实现FAIL。**不隐瞒门禁可能过严，也不把更多只读信息当作授权放行。

## Alternatives

| 选项 | 本轮处理 |
|---|---|
| 看不到Worker PID就直接更新安装 | 不采用；进程采样不能替代全部消费者/待完成任务确认 |
| 以stage缺失/瞬时lease成功放行 | 不采用；未调用lease探测，且不能据此证明终止 |
| 识别app=null为空闲后当场放宽门禁、继续normal | 不采用；初次安全门禁已停止，不能自行把残留分类未知变成已证安全 |
| 卸载、清数据、杀旧研究进程或删除残留 | 不采用；本轮未获得未知状态下的这些收口操作许可 |
| 提交事实、独立审查最小无消费者判据 | **采用**；保留现状及范围 |

## 需要独立审查的问题

1. 目标ServiceRecord是否只是F0拒绝路径后的无任务系统登记，哪些字段能充分区分它和真实待完成绑定/消费者？当前仅观察到app=null、无Bindings段和无Worker进程，不能直接作结论。
2. 初始门禁是否把无害残留记录过度阻塞？应先审阅目标段和采集边界，明确只读判据，不为让normal通过而抹除记录。
3. 若确实需要处置旧研究状态，应另行明确研究包内的最小准备范围和保护前提，禁止拓展到正式App/清数据/旧stage跳过。
4. F1A N01继续保留；本轮stage缺失不是REVOKED语义测试，也不是已解决。

后续是否继续Outsider及一次normal由独立审查与明确授权决定。本轮没有生成normal运行身份或启动次数，不存在“失败后重复normal”。不自行进入下一验证点。

## Evidence / source integrity

- preflight/finish实际执行源码快照、时间、SHA与全部54条只读命令均保留；最终verify.py移除未执行的安装/启动预留入口，只保留只读取证，未重新执行原阶段或覆盖旧快照。
- 截取目标ServiceRecord排除了无关全局Last ANR前缀；[范围/前后SHA](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked-service-detail-scope.json)说明派生过程，更宽派生文本仅本机忽略目录，原ADB返回登记不变。不能把无关ANR绑定或执行字段归给Worker。
- 本地锁定F1A APK/证书/Manifest实查通过；当前装机签名连续性NOT_RUN。研究已装仍为旧R3代码，未更新。
- 16项宿主合成snapshot检查器PASS与阶段保护比较PASS都有适用边界；Outsider、normal及生命周期全部NOT_RUN。F1A60项结果只继承历史，不登记为本轮重新执行。

## Consequences

本轮没有完成两项Android验收，R3完整生命周期继续BLOCKED，原29仍0PASS/1UNKNOWN/28NOT_RUN。历史两次attach和R1超时UNKNOWN不关闭。UI-C1 ACCEPTED/FROZEN — PASS WITH NOTES；M10 PENDING/三项JPEG FAIL；M11 NOT_AUTHORIZED。

只新增研究报告/只读工具/合成检查测试/证据/checkpoint与当前状态，不改产品、AIDL/WorkerService、Manifest、Gradle、原130案例、验收矩阵或R0～F1A历史。不合并、不发布，只提交推送`research/m10-jpeg-strict-r3-f1b`后停止。

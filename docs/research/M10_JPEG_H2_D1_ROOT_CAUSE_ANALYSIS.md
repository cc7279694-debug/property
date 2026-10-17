# M10-R3-F1B-H2-D1 — 离线故障分析

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1B-H2-D1**，只表示设计资料可独立审查。H2 normal仍FAIL，完整生命周期仍BLOCKED；本轮没有设备观察。

## 1. 契约、来源及验证范围

Goal：解释H2失败的已证事实、定位失败收口缺陷，并提出最小修复及安全恢复前提。Scope仅离线源码、既有证据、纯Python合成状态模型、三报告、CURRENT_STATE与checkpoint。禁止ADB、App/AVD操作、研究APK/正式代码修改、真实recover/cleanup、JNI及M11。

来源锁定`7612c0461df59fabf85d01d32e9b051eaab6bc1f`，本地/远端H2一致且原工作区干净；独立worktree `E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-h2-d1/砺石新APP`，分支`research/m10-jpeg-strict-r3-h2-d1`。已有AGENTS、PROJECT、当前状态、相关决策/架构/路线、执行规范及H2资料已读取；当前用户授权优先于旧阶段文字。

已实际读取ProbeActivity、ProbeRunner、ProbeSupport、ProbeApplication、StageStore、WorkerService、SessionGate、StageAdmission、StageIdentity、AuthorizationFence及AIDL。离线脚本核对30份锁定输入、派生39条范围日志观察，见[输入SHA](m10-jpeg-h2-d1/evidence/input_manifest.json)、[时间线](m10-jpeg-h2-d1/evidence/timeline.json)、[结果](m10-jpeg-h2-d1/evidence/analysis_results.json)。源码路径以下统一相对`m10-jpeg-r3/host/probe/src/main/java/com/cooper/lithic/m10r3/probe/`，行号以锁定提交为准。没有重新采样设备、读取私人数据或修改历史证据。

## 2. CONFIRMED：源码与既有观察

| 编号 | 结论及依据 | 边界 |
|---|---|---|
| C01 | `ProbeRunner:85,215`：bind只向main Handler post，随后executor立即开始5秒connected.await。 | 当前5秒混合排队、同步bindService、系统启动及连接回调，不是纯系统绑定耗时。没有三个边界时间。 |
| C02 | Activity:9,18→Runner:19,21–30在提交executor前同步初始化目录、持久claim；Application:13–18创建marker并写事件。 | 主线程确有文件操作，不能由存在推定它们导致本次ANR。 |
| C03 | Support:21–33是每进程类锁下的同步事件写入，每条getFD.sync；时间戳先生成，后写入/sync。 | 日志时间是事件采样点，不是fsync完成点；主线程可等待相同monitor或其I/O。 |
| C04 | Runner:43–46失败出口只记日志/FAIL，未撤销gate、未持久revoke、未收口连接。 | **H2-N04确定缺陷**。FAIL文件不是授权终结证明。 |
| C05 | Runner:211只检查disconnected，失败没有设置该状态；迟到连接仍可安装endpoint/linkToDeath。 | 代码缺少failure/epoch围栏。实际回调进入时刻没有日志；晚死亡监听关联属下节推理。 |
| C06 | Runner:218先bound=false再unbind；抛异常可丢失清理责任。98–112的FD开/用/关无整体finally。 | 静态异常路径缺口，不是H2已发生连接泄漏或FD泄漏证明；H2未走到FD段。 |
| C07 | H2唯一Intent进入Host6423，CASE_FAIL_STOP及run JSON均为bind timeout；系统记录Input-dispatch ANR。 | am的timeout/UNKNOWN、FAIL及ANR均保留，不被晚Displayed覆盖。 |
| C08 | Worker6610在Host失败后才启动；锁定Host在:85失败、未继续到:91 reserve，且既有事件链无握手/FD。SERVICE_CREATED后约30秒UNRESERVED_DEADLINE自退出。 | 未reserve不是仅由deadline名称推断；Worker的30秒任务本身不检查request、也不因reserve而取消。Binder/OS死亡不是认证退休、FD关闭或lease释放。 |
| C09 | 系统采集6423栈的日志随后为Aborted stack trace dump / deadline exceeded；归档无完整可归因栈。 | 不能声称已查到ANR栈中的fsync、bind或锁等待，也不断言设备上从未产生其他栈文件。 |

原始依据：[Host trace](m10-jpeg-r3-f1b/evidence/h2-20261017-01/normal-host-trace-6423.jsonl)、[Worker trace](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-ending-worker-trace-6610.jsonl)、[系统日志](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-ending-window-system.log)、[主日志](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-ending-window-main.log)、[事件日志](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-ending-window-events.log)、[原FAIL](m10-jpeg-r3-f1b/evidence/h2-20261017-01/normal-failure-result.json)。

## 3. 线程及依赖

| 线程 | 实际职责/依赖 |
|---|---|
| Host main（PID/TID6423） | Application角色/trace、Activity初始化、Runner构造与claim；执行bind post及连接回调/解绑。Activity.runner是static，不能误写成每Activity独立Runner。 |
| Host executor（H2 TID6442） | CASE_BEGIN、旧stage准入、新stage/pin、post bind后await、同步reserve/start、等待结果/死亡和文件收口。H2只到bind await失败。 |
| Host Binder/死亡回调（H2观察TID6433） | 同步hello/grant/result授权及死亡通知。H2此TID有WORKER_ENDPOINT_DIED，没有握手/结果接受。 |
| Worker main（PID/TID6610） | 角色、Service创建/绑定。H2是迟到启动，不持有第二owner。 |
| Worker Binder / compute / control | reserve取得抽象socket lease并同步回调Host hello；start同步grant后投compute；compute同步result；control执行期限/认证退休。H2仅观察control TID6641执行未预留期限退出。 |

AIDL均为同步调用，没有oneway。若Host持授权锁同步调用Worker，Worker又回调Host获取同一锁，会形成潜在闭环；现有正常调用位于锁外，**未发现可确认的当前ABBA锁循环**。StageStore嵌套synchronized是同线程重入，不构成死锁证据。现有顺序包含authorizationLock→SessionGate，以及授权锁内磁盘/Support事件锁；慢I/O可延长等待，但具体等待未被栈证明。

## 4. H2时序与取证限度

| epoch（179222前缀） | 事件 |
|---|---|
| 9881.382 / 9881.659 | 系统START / Host6423启动 |
| 9884.472 / 9889.482 | 主线程APPLICATION_ROLE / executor CASE_BEGIN |
| 9894.214 / 9897.657 | STAGE_CREATED / PIN_DURABLE DELIVERY_UNKNOWN |
| 9903.069 | executor FAIL bind timeout；pin采样至失败单调时钟差5411.846851ms |
| 9905.772 | Displayed，系统报告+24s415ms；performCreate事件另报4591ms |
| 9911.877 / 9925.583 / 9936.776 | 收集栈 / dump中止 / ANR Input dispatch FocusEvent waited5001ms |
| 9961.449 / 9965.398 | Worker6610进程启动 / SERVICE_CREATED |
| 9995.403～9995.424 | 未预留期限、自退出、OS/Binder死亡及ServiceDisconnected |

Worker进程启动距Host FAIL的wall时间差58380ms；Service创建至期限的单调差30005.216232ms。**这些不是单次fsync或bind调用耗时**。事件时间生成在日志fsync之前，pin后的5.412秒还可能包含该条日志写入和线程调度。缺少BIND_POSTED、BIND_CALL_BEGIN、BIND_RETURNED与CONNECTED_ENTER，无法把延迟分配到某段。

## 5. SUPPORTED_HYPOTHESIS / UNKNOWN

| 级别 | 判断 | 所缺证据/可证伪方向 |
|---|---|---|
| SUPPORTED_HYPOTHESIS | 主线程同步文件操作、同步trace及跨线程日志锁可能加重排队和输入延迟。 | 现象相容但无栈/调用耗时；未来逐边界记录并验证移出主线程的效果，不能先认定fsync根因。 |
| SUPPORTED_HYPOTHESIS | 队列、bindService同步往返或系统Service启动任一/组合耗尽混合期限。 | 没有入口/返回点，未来分期计时才可区分。 |
| SUPPORTED_HYPOTHESIS | 迟到onServiceConnected越过了失败状态并注册死亡监听。 | 唯一linkToDeath在Runner:211，后有匹配死亡事件；没有该回调独立入口时间。 |
| UNKNOWN | 真实ANR阻塞点、磁盘/monitor具体等待、系统晚58秒启动Worker的原因。 | 缺主线程栈、队列/绑定边界及系统调度证据；本轮不补采。 |
| UNKNOWN | 首轮结束unknown process under research identity的原名称。 | 原ps行未归档，后续crash_dump日志和Host-only采样不能回填。 |

## 6. 最小改善设计及验收门槛

不简单放大5秒。以单调时钟记录三个独立期限：`UI_BIND_POSTED→BIND_CALL_BEGIN`排队；`BIND_CALL_BEGIN→CONNECTED_ACCEPTED且BIND_RETURNED=true`绑定（false/exception立即失败）；`RESERVE_ISSUED→HELLO_ACCEPTED且RESERVE_RETURNED`握手。每个观察需attempt/epoch/序号/TID；回调可能早于bind返回，必须合并两项事实，不能强加错误顺序。阶段期限取与不可重置总deadline的较早者，数值由未来测量/独立审查确定，本轮不改工具链/期限。

Runner/StageStore初始化和持久claim移到有界后台入口，claim持久成功仍须早于Session创建/授权。主线程只提交请求/显示；trace改为有界序号队列，溢出/持久失败显式UNKNOWN。授权/revoke/current/pin的持久写不能降级为可丢弃trace。

watchdog不能依赖正在同步Binder等待的executor或UI。超时先执行无I/O的失败围栏，之后持久化/解绑另行收敛；Future.cancel或停止等待不证明远端停止。详见[失败围栏](M10_JPEG_H2_D1_FAILURE_FENCE_DESIGN.md)。真实队列/同步Binder阻塞、日志I/O、ANR改善、线程/FD/lease均待未来独立授权Android测试；纯模型不能证明实际最坏响应时间。

## 7. 本轮实际验证及结论

纯Python模型RED以“旧相关谓词失败后仍允许grant”得到1项断言FAIL；是静态反例模型，不是重跑Java/H2。最终GREEN **22个唯一测试方法PASS**，含120种失败后抽象顺序及20个恢复前提逐项False/None拒绝。日志、输入哈希和方法名单见[模型说明](m10-jpeg-h2-d1/README.md)及[最终GREEN](m10-jpeg-h2-d1/evidence/green-final/result.json)。两次GREEN方法身份相同，不累加44项；最终输入以green-final为准。这不计入Android或原R3测试数。RED原CP936日志、无损解码副本及初次显示编码异常均保留，不将编码错误算行为RED。

D1有可审查的最小设计，未发现必须重做Binder/生产媒体架构才可提出方案的问题；真实ANR根因仍UNKNOWN，当前现场**不具备恢复执行资格**。H2 FAIL/UNKNOWN/NOT_RUN、N01～N03、M10 PENDING/3 JPEG FAIL、UI-C1冻结及M11 NOT_AUTHORIZED保持。提交后停止，等待ChatGPT独立审查。

交付前另核对119个本地链接、30份原输入、最终22方法与日志/源码SHA，保护范围diff为空、未发现敏感信息或禁止产物，见[核查回执](m10-jpeg-h2-d1/evidence/delivery-verification.json)。Codex协作代理分别只读复核时序、围栏和恢复约束，不操作设备、不代替ChatGPT独立验收。只提交推送D1；最终SHA与远端一致性取Git回执，不合并main。数据库、Repository、设备文件、APK/构建和权限均无变更。

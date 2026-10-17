# H2-D2B — 线程、期限与兼容性设计审查

2026-10-17。**BLOCKED — M10-R3-F1B-H2-D2B**。以下是已编译的研究候选，不是获准安装版本，也不证明真实ANR已修复。[实施范围](M10_JPEG_H2_D2B_SCOPE.md)先于代码提交。

## 当前候选的线程分工

| 路径 | 当前实现 | 不能推定的事情 |
|---|---|---|
| Host main | Activity复制有界字符串并submit；Application建立队列/诊断writer，API28+读取framework进程名提示；不构造Runner、不写owner/claim、不执行日志文件/fsync | 不保证framework调用、线程创建或系统bindService有硬实时上限 |
| 唯一Host command线程 | bootstrap角色/owner/Runner → durable claim → Invocation → 报告；容量16，拒绝策略不是CallerRuns | 初始化、claim和POSTED之前的stage I/O仍可能阻塞，未纳入30秒预算 |
| Binder回调 | 既有完整持久授权检查 → 短InvocationFence/Timeline/Gate；grant/result再次检查时序失败 | 原慢授权校验、源哈希及磁盘延迟没有变成有界操作 |
| 独立watchdog | 每Session独立daemon，20ms检查单调期限，仅触发短内存失败；不排在UI、Host任务或诊断writer后 | Android调度、GC、进程冻结/OOM时最坏响应仍UNKNOWN；不能终止同步Binder或证明Worker退出 |
| Host trace writer | 独立线程，容量256、单条UTF-8最多16KiB；logcat、文件append及fsync在writer执行 | 约4MiB为payload准入预算，另有String/对象/编码临时副本，非整个App内存上限 |
| Worker | WorkerService/AIDL源码完全不改；已知Worker的同步诊断sink保留 | Application/Support共享启动逻辑有变化，**不能宣称Worker启动语义完全不变** |

入队前只做内存校验/活跃键去重。相同(command,runId)正在执行时返回DUPLICATE，避免intent_replay等待自身队列；任务结束释放内存键后，历史重放仍必须经过真实磁盘claim。初始化失败保持UNKNOWN，claim异常/未确认均不开始Invocation。没有第二条Session执行队列。

## 六个边界及不可重置期限

`UI_BIND_POSTED → BIND_CALL_BEGIN → BIND_RETURNED / CONNECTED_ACCEPTED → RESERVE_ISSUED → HELLO_ACCEPTED / RESERVE_RETURNED`。

CONNECTED允许早于BIND_RETURNED；必须取得true返回和连接两个事实。HELLO允许早于同步reserve返回；必须取得接受的hello及true返回两个事实。实际bind的try/finally内记录返回，避免把解绑耗时混成同步bind耗时；false、throw、迟到返回分别保留。reserve抛异常先记UNKNOWN，不伪装成明确返回false；最终返回值在finally记录与失败围栏之后重新检查。

研究初值：UI排队、同步bind、返回后等待连接、握手分别5秒；POSTED到完整握手最多15秒；POSTED到completed最多30秒。每次取阶段deadline与绝对total的较早者，exact边界拒绝。不是简单放大原混合5秒，不按回调、轮询或重试重置时钟；这些数值未由Android设备测量验证。

每边界保存单调纳秒、threadId，Runner诊断补attempt/epoch/接受结果。snapshot实际计算queueNanos、bindSyncNanos、connectedFromCallNanos、connectionAfterReturnNanos、helloNanos及reserveSyncNanos；缺失为-1，连接早于返回时“返回后等待”为0，不制造负时长。completed只结束时序观察，**不是资源终结证明**。

watchdog在Timeline锁外才调用InvocationFence；慢authorization → InvocationFence → Timeline/Gate，没有Timeline持锁反向等待InvocationFence。触发后工作线程若仍卡在Binder/fsync，只有内存围栏已关闭，持久撤销/解绑仍pending或UNKNOWN；不以Future取消或等待结束推定消费者消失。宿主阻塞线程测试通过不证明Android timer一定及时运行。

## 诊断完整性

Host生产者不调用logcat、文件或fsync；writer与Host命令线程、watchdog分离。队满、超大事件、写失败、drain超时、初始化前丢失都显式计数并保持sticky UNKNOWN。首次未知observer只执行短内存围栏；原异常保留，observer本身失败也可见。

必需成功候选事件先入队，再在后台有界drain，之后才能检查receipt/terminal/report资格；UNKNOWN禁止成功。StageStore授权、revoke、receipt、terminal仍是原同步持久操作，绝不当作异步日志。报告分别列Trace observer和command queue observer，避免混同。并发序号标识准入身份，不能冒充全进程wall-clock严格排序。

drain只证明该观察点已接受事件的状态，不与未来事件/文件提交形成跨存储原子事务。COMMAND_OUTCOME在Invocation返回后产生，属于后续调度诊断，不是之前生命周期proof；后续日志失败不能追认之前永远完整。未完成写入、永久sink阻塞和崩溃都不能当writer已停止。

## 阻塞性API26/27反例

静态可达顺序：Application因API<28没有framework名称 → identity任务已排入后台但尚未调度 → 系统在main调用未改动的WorkerService.onBind → 它调用role(UNKNOWN) → main不能等待后台，故返回REJECT并抛SecurityException。这个顺序无需假设错误身份；它是**安全拒绝但功能兼容失败**，不是Android实测FAIL。

API28+有framework名称提示，可避免上述UNKNOWN名称分支；但提示与后台/proc交叉核验是不同证据，不能声称所有Worker入口均已完成后台初始化。Host侧仍严格在角色/owner/claim确认后才bind。真实API26、API36及ANR结果全部NOT_RUN。

在“不新增main等待、身份完全异步、Worker仍同步强身份onBind”的组合下，不能仅靠更长timeout或默认Worker名称解决。当前候选不具备进入设备验证资格，停止扩展架构。

### 提交独立审查的最小选项（均未实施）

1. **优先建议审查A：API26/27保留原有必要的256字节/proc身份探测同步例外**，仅角色探测保留；owner/Runner/claim/trace重I/O仍后台。无WorkerService/Manifest/minSdk修改，范围最小；字节有界不等于时间有界，残余main探测延迟仍须明确测量/UNKNOWN。需独立接受这个例外后才实现，不能悄悄改测试把它称完全无main I/O。
2. 若要求API26也完全异步身份，**B：单独设计Worker启动/绑定就绪协作**，在完成角色核验前不授予reserve/grant；这触及WorkerService现同步onBind门槛，超出本轮。必须另行设计审查，不能当前就改Service或AIDL。

不建议非SDK反射、把未知进程当Worker、main阻塞等待、提高minSdk或修改Manifest来消除反例。当前没有实施A/B，也未改变磁盘格式或正式产品架构。

## 其他未解决事项

新Lint指出scheduleAtFixedRate在Android缓存/恢复后可能集中追赶；本轮保留原警告，不能宣称watchdog CPU/最坏响应已证明。后续审查可采用fixed-delay且继续按绝对时钟检查，不能以轮询次数代替期限。

旧stage_model和未完成握手的早退路径不强行调用completed，也不放宽N05；其他29场景未运行。kill_host等原控制入口现在受同一串行队列约束，阻塞任务期间不具备原即时控制用途，不能泛称所有旧场景行为兼容；本轮禁止执行它们，若后续需要，应另审控制路径而不增加Session授权绕行。

实际ANR根因、历史attach/R1超时、真实磁盘/线程调度均未解决。独立审查先决定API26兼容路线，再决定是否修订候选；当前不授权安装、恢复H2或重试normal。

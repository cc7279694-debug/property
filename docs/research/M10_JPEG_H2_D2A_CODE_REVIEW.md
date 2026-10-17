# H2-D2A — 研究源码与架构差异

2026-10-17。实现状态 **IMPLEMENTED / PENDING_ANDROID_VERIFICATION**；Codex内部复核不替代ChatGPT独立审查。[执行](M10_JPEG_H2_D2A_EXECUTION.md) · [测试](M10_JPEG_H2_D2A_TEST_RESULTS.md)。

## 原位置与最小修改面

锁定D1源码位置沿用其报告：`ProbeRunner:43–46`失败只写FAIL；`:211`迟到连接未看失败；`:218`解绑前擦除bound；`:98–112`整体FD finally缺失；`:208`拒绝结果仍可countDown。`StageStore:117–118`仅pin等条件，未明确拒绝REVOKED；`recordOutput:171–172`同类分支不一致。行号是**修改前**位置。

仅研究Host增加四个小型纯Java责任/策略helper，原SessionGate增加原子grant，StageAdmission增加不清理的终结验证模式。它们由真实Runner/Store直接使用，不是仅供测试的独立示例。没有Worker/Binder wire协议、stage磁盘格式或生产媒体协议变化。

## 锁与线性化

`authorizationLock（磁盘校验）→ InvocationFence → SessionGate`。失败入口直接先取短InvocationFence/Gate，释放后才进入持久撤销；没有短锁→慢授权锁的嵌套。短决策只读/改内存；`asInterface`、Binder UID/PID采样、bind/unbind、trace、fsync均在外。现有慢授权校验结束后，在同短边界复核Ticket和Gate。

每次begin占用准入，创建中途失败也不释放。attach前仅用isAdmitted；发布完成后才可decide接受回调。失败固定firstFailure并撤销自己的Gate；旧catch/endpoint只触碰自己的Ticket/Gate。当前已终结Ticket若最终报告失败，会撤销其终结准入；旧Ticket迟到失败不关闭新Ticket。

grant与failure由同短锁/Gate线性化。failure之前已获准的Binder调用/FD交付可能仍在飞行，不能物理收回，必须保留pin并隔离。Gate.result保留匹配death先到的合法成功；失败/撤销/旧epoch后不再接受。重复result不会推进latch。

## 持久授权与旧stage

- 同完整owner的revoked.json写入后回读；已有撤销也重新核验。写入、同步、回读异常均不开放内存准入，不把FAIL或state=REVOKED等同持久撤销。
- 只有DELIVERY_UNKNOWN/FD_SENT且pin=true、当前身份正确、无terminal/revoked/pending才可能授权；未知状态/损坏JSON由适配器拒绝。所有旧inode、receipt、unknown保护继续存在。
- recordOutput使用同策略，单独严格验证源/命名空间，再保存新输出inode并回读；已有输出owner只能匹配，不能改成新inode。不能在新输出尚无owner时盲调用完整validateStage而自锁。
- 普通run先在全部独占lease持有期间核对所有stage终结、全部pending缺失，保留current；失败不清理、不创建新有效消费者。原closeAll仍保留为研究底层历史函数，但普通run和recover均不能以它自动处理H2；recover入口明确拒绝。

## 连接与FD

共享绑定adapter在false/throw出口先短失败围栏，再让责任helper做匹配解绑，避免解绑阻塞在围栏之前。queued在实际执行前检查epoch；已准入调用与物理bind之间的微小窗口视为在飞行，返回后收口。true/false/throw分别保留；异常解绑不先擦责任，UNBIND_UNKNOWN及原异常可见，未知不自动重试。

迟到连接拒绝endpoint安装；重复/不一致连接关闭本Invocation。绑定死亡、service disconnected、Binder death、retire请求、解绑完成分别记录；不把任何一种替代OS死亡。未改变WorkerService，因此也未新增OS死亡证据。

HostFdScope被真实source/output和duplicate FD路径使用；操作异常在自动关闭之前先围栏，全部Host副本尝试关闭，失败保留主异常和suppressed。编入的Os.fstat/EBADF观察仅限Host副本，尚无Android实测。原额外rebind连接也使用匹配责任helper，异常先围栏，再在finally请求收口。

## 慢提交、清理与已知风险

CURRENT/RECEIPT各只有一次候选，慢I/O前取得、返回后重新核对epoch/失败/资格；receipt还要求原Gate成功、唯一结果、FD关闭、匹配death及连接收口。失败时不得仅凭磁盘receipt宣称成功。receipt落盘期间发生失败，后验拒绝并保守阻塞，不继续清理。

**不承诺失败能瞬间停止已开始的rename/fsync或cleanup**。cleanup仍使用既有独占lease、完整owner/inode及终结proof；open检查之后已进入的清理可能与迟到失败重叠，随后markTerminal失败则总体失败，必须保留未知而不能宣称资源均未变化。这与D1的逻辑资格/物理写入区分一致；没有引入跨磁盘/内存事务。

以下均未解决：主线程初始化/claim重I/O、同步trace/fsync、5秒混合期限、同步Binder或磁盘长等待、ANR真实根因及最坏响应时间。D2A未引入独立watchdog，也不保证观察不到的永久卡住会及时触发失败；一旦执行到失败围栏才有本次短内存保护。持久撤销若一直未返回，memory关闭但持久结果仍pending/UNKNOWN，不能宣称已完成收口。

D2B需要另行授权；没有简单增大超时、迁移主线程、异步trace或阶段时钟改造。新研究APK从未安装，无法据JVM结果宣称实际bind/FD/lease/退休通过。旧非normal场景本轮均未验证，不能因修了附加责任路径升级原29项。

## 对照登记与后续门槛

| 项目 | 本轮证据 | 状态 |
|---|---|---|
| H2-N04内存失败/epoch/late回调 | 真实helper方法、组合竞态JUnit、Runner集成及APK输入 | IMPLEMENTED / PENDING_ANDROID_VERIFICATION |
| N01 REVOKED/pending及持久撤销 | 真实Store共享策略、回读失败/未知/重复测试 | IMPLEMENTED / PENDING_ANDROID_VERIFICATION，历史不CLOSED |
| 连接责任、Host FD | 真实helper异常/幂等及绑定围栏组合测试 | IMPLEMENTED / PENDING_ANDROID_VERIFICATION |
| N02失败残留 | 普通run不清理、未知一律保守阻塞 | 保留，不提供恢复执行资格 |
| N03 | 原Outsider代码及H1单次API36边界不改 | 历史保留；非本轮新设备PASS |
| ANR、真实H2现场、完整生命周期 | 无设备操作 | 原FAIL/UNKNOWN/BLOCKED不变 |

下一步仅ChatGPT独立代码审查。后续如另获授权，应逐项验证排队/调用中失败、迟到连接、bind false/throw、撤销/fsync异常、Host FD中途异常、result/death竞态、未知解绑和准入关闭；必须先满足D1恢复门禁才能考虑H2现场。不能直接重跑normal或自动进入D2B。

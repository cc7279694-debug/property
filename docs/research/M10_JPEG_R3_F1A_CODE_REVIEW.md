# M10-R3-F1A — 研究代码审阅

2026-10-17。**实现方本地审阅**，含协作代理辅助检查，不替代 ChatGPT 独立验收。审阅输入为 F0 `f513725b862d3422b371883536557c4233bed2e6` 与本轮最终构建输入；正式接受状态仍等待独立审查。

[执行](M10_JPEG_R3_F1A_EXECUTION.md) · [测试](M10_JPEG_R3_F1A_TEST_RESULTS.md)。

## 修改前的确切缺口（行号以锁定 F0 为准）

1. `host/outsider/src/main/java/com/cooper/lithic/m10r3/outsider/OutsiderActivity.java:14` 调用 bindService；连接只在 try 局部可见，15行只在 true 返回时 unbind。SecurityException 分支记录拒绝却不负责解绑，且没有 onDestroy 收口。
2. `host/probe/src/main/java/com/cooper/lithic/m10r3/probe/ProbeRunner.java:71` 只探测旧 guard，72行立即创建 Session 并替换 current。临时获得 lease 并释放不证明旧 attempt 已撤销、资源关闭或安全清理。
3. `StageStore.java:68` 开始 cleanup；owner/receipt 在 lease 前检查，旧清理凭证不绑定完整身份；93行源缺失仍被 ownedSourceAndOutput 接受。`ProbeRunner.java:186` result 回调没有持久撤销或 current 围栏。恢复只看 current，遗漏孤立 stage。

原始泄漏堆栈：[F0系统日志](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/outsider-scoped-system.log)。系统拒绝非导出 Worker 和连接注册泄漏可以同时成立；不能把“originally bound here”解读为成功跨 UID 绑定。F0 trace 表明连接曾被登记后未清理；具体 Android dispatcher 在所有异常下的行为仍需后续系统验证，不用 JVM 模拟代替。

## A：连接责任

Activity 字段持有同一 ServiceConnection 与实际使用的 BindingAttempt。尝试前进入 REQUIRED，绑定 true/false、SecurityException、其他 RuntimeException 都经 finally 清理。onDestroy 重试此前清理失败，已经 RELEASED/NOT_REGISTERED 的清理不重复调用。

解绑 IllegalArgumentException 在未成功返回的路径明确记为 NOT_REGISTERED，保留异常文本；成功返回 true 后发生该异常视作意外清理失败。其他解绑错误保留首次错误，即使后来成功也不覆盖成 PASS。原 SecurityException 仍记 DENIED；意外绑定成功仍 FAIL。日志未删除，没有换 Application Context 或更改权限。真实 ServiceConnectionLeaked 消失、生命周期调度和跨 UID 拒绝 **NOT_RUN**。

## B：旧状态不变量

| 不变量 | 实际实现与证据边界 |
|---|---|
| 先关闭旧 attempt，后建立新会话 | ProbeRunner 76行起持 authorizationLock，closeAllPriorAttempts 完成后才 new Session/publishCurrent；原命令固定身份 claim 不变 |
| current 不是完整清单 | StageStore 枚举整个 synthetic-stages，只认规范 UUID 目录与精确 current；孤立/无 current 的 stage 也必须检查 |
| 没有 guard 检查/清理空窗 | StageAdmission 同时取得全部旧 stage lease，在同一持有期间校验全部身份，然后撤销/清理/验证；所有已取得 lease 在退出时释放。canAcquireGuard 只剩诊断/等待用途 |
| 先完整预检再删除 | owner/state/receipt/pending、源哈希/大小/inode、输出归属、代/nonce/guard/stage inode 在 lease 内核对；未知项阻止 admission。不依赖持久 PID、mtime 或名称猜测 |
| 持久撤销不能反转 | revoked 绑定完整 owner，保留到旧 stage；同一天/重复指令不抹去撤销。身份或格式不符一律拒绝 |
| 终结不能由资源缺失推断 | 必须先持久撤销、删除已确认归属资源、fsync，再写 CLEANED_SYNTHETIC/pin=false，最后持久完整 cleanup；终结还绑定保留 receipt/output-owner SHA 或 ABSENT，验证资源确实不存在 |
| 部分清理失败保守保留 | 不写完整终结证明、不发布新 current；部分已删除的已知文件不会重造，剩余资源保留。下一次无完整证明仍 BLOCKED，不做 journal/自动修复/换代 |
| 幂等仅限已证实终结 | 再校验完整 terminal/revoke/pin/身份/保留 metadata/缺失资源；只有全部成立才跳过重复删除 |
| 迟到回调不能复活旧授权 | hello/grant/result 实际使用 AuthorizationFence；current、完整 owner/pin/revoke 与 gate 变更在同一 Host monitor 内判断，错误记录并拒绝。旧回调不能改变新 Session gate |
| 未知文件不误删 | cleanup 可保留未知子项以继承原 stage_model 断言；新会话 admission 因未知项拒绝。未知根项/子目录/链接不认领、不自动删除 |

所有文件/状态仍是合成研究 v2，不接正式 MediaFileProtocol/Room，不生成正式封面引用。新严格检查不会自动认领旧版简化 revoked/cleanup；发现这类历史残留时必须保留并阻塞，不能清 App 数据绕过。

## 时序与边界

- WorkerService 保持原实现：reserve 先取得同 attempt lease，再同步 hello；start 取得同步 grant 后才消费 FD，lease 保持到进程退休/死亡。Host 清理期间无法与已持 lease 的合作 consumer 并行删除。
- 旧 attempt 清理后迟到 reserve 即使取得旧 lease，持久撤销/current 围栏也会拒绝 hello/grant。若迟到 reserve 恰在两次 admission 之间占用旧 lease，新 publish 会拒绝；可能留下未发布合成 stage，下一次必须枚举处理，而不是越过它。
- close/publish 与三个回调共用静态 Host monitor。同步 Binder 调用、等待 latch/退休均在该 monitor 外，避免 reserve→hello 死锁。回调中的可控 delay 也在锁外。
- `recordOutput` 在首次写 output-owner 前使用更窄的 current+pin+未撤销检查，避免“尚无 output-owner 导致无法首次记录”的自锁；随后 FD open 和 grant 使用完整检查。
- 原 SessionGate 允许同会话 result/death 乱序收敛的17项语义保留；新增持久围栏只阻止已撤销/替换会话。
- 发布 current 本身若 rename 后 fsync 失败，不能声称旧指针必定仍在；此时调用失败且不会 bind 新消费者，后续严格扫描保留/验证未知持久化结果。**旧收口失败**发生在创建/发布之前，current 不被替换。

以上为源码推理和 JVM 模型，不证明真实 Android socket、FD、文件持久化、Binder 竞态或 SIGKILL 恢复已通过。lease 可取得只证明受控合作消费者未占有该 lease，不能当作死亡证明或任意 native 对抗安全。

## 保持范围

两份研究 Manifest、AIDL、WorkerService、SessionGate/旧测试、原构建脚本、正式代码/schema/配置/媒体协议均未改；只为研究 outsider 添加既有 JUnit 测试配置，没有新增运行依赖。F1A 不重新执行 M10 三项 JPEG 测试，不改其 FAIL，不修 R1 120秒 UNKNOWN。

本地审阅未发现需要重做 Binder/Service/stage 模型的确定性阻塞。源码审阅只能支持提交 F1-A 独立检查，不能授予 F1-B、生产 GO 或生命周期 PASS。完整系统行为继续 NOT_RUN。

本轮额外只读协作审阅完成：检查了normal源码时序、两项修复集成及terminal/迟到回调边缘，未发现阻塞F1-A提交的确定性问题；审阅者未执行设备、测试或构建，不能写作新的实验或ChatGPT独立验收。

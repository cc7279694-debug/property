# M10-R3 — FD、资源归属与恢复模型

日期：2026-10-16。**源码模型已交付，Android验证NOT_RUN / R3 BLOCKED**。不得据此宣称正式M10存储、GC、CAS、generation关闭或备份恢复通过。

配套：[执行](M10_JPEG_R3_EXECUTION.md) · [生命周期](M10_JPEG_R3_LIFECYCLE_RESULTS.md) · [决策](M10_JPEG_R3_DECISION.md)。

## 1. 生产边界与研究模型

现生产`MediaRepository.prepareImport`仍占coordinator、Store再持file mutex，active只是内存集合；生产Application仍无条件启动AppContainer。R3没有改这些文件。研究Host只有合成owner marker、stage和gate，无Room/真实资金/封面提交，也不修改Nav3守卫。

模型位置：[StageStore](m10-jpeg-r3/host/probe/src/main/java/com/cooper/lithic/m10r3/probe/StageStore.java)、[WorkerService](m10-jpeg-r3/host/probe/src/main/java/com/cooper/lithic/m10r3/probe/WorkerService.java)、[ProbeRunner](m10-jpeg-r3/host/probe/src/main/java/com/cooper/lithic/m10r3/probe/ProbeRunner.java)。协议使用[AIDL强类型SessionEnvelope](m10-jpeg-r3/host/probe/src/main/aidl/com/cooper/lithic/m10r3/probe/SessionEnvelope.aidl)，字段显式覆盖protocolVersion/session/attempt/generation/source大小与SHA、状态/结果/资源关闭；本地Bundle仅作实现适配，不跨协议承载任意路径或图片。

仅研究包私有`synthetic-stages`：owner v2、源/输出、pin状态及合成receipt。receipt明确`SYNTHETIC_TASK_ONLY`、`formalReferences=0`，不能成为正式media/Room引用。v1拒认谓词是研究模型，不直接调用生产MediaFileProtocol/GC；模型测试即使将来通过，也不能证明生产GC已改造。

## 2. 设备前复核与方案调整

初始FileLock草案在设备执行前被否决：POSIX进程锁可能因同inode另一FD关闭而释放，不能靠Java `isValid()`断言重复FD交付后的内核锁仍存在。没有运行该危险清理实验，不把静态发现包装为设备RED。

改为现有Android `LocalServerSocket`在Linux抽象命名空间中的独占lease，名称固定绑定随机attempt UUID，不从任意自报路径生成。worker在同步hello前取得、强引用保持到自退/死亡；data FD关闭不释放lease。Host恢复必须取得**同一owner绑定名称**并全程持有，完成持久撤销、全归属预检和清理之后才关闭。不能“探测可得→关闭→再删”。[Android官方LocalServerSocket](https://developer.android.com/reference/android/net/LocalServerSocket)。

这是R3对R2恢复凭据的研究细化，不是生产批准或权限沙箱。名字占用/取得失败只导致保留/UNKNOWN，不推断谁仍活；可得也只证明受控合作consumer未持该lease，**不能等同原进程死亡**。正常准备仍另需匹配完整结果、data FD关闭与原session Binder死亡；[IBinder死亡语义](https://developer.android.com/reference/android/os/IBinder)。B1同UID任意native代码执行、主动违约关闭lease、整VM失调度及跨UID恶意占名DoS均未证明安全。

## 3. 预定FD与grant顺序（尚未实测）

1. Host排他创建合成stage/source，写owner、同步并固定stage/source/guard inode和源SHA；pin先持久为`DELIVERY_UNKNOWN`。
2. worker reserve校验UID/角色/协议，监听当前client Binder死亡，取得lease，再进行worker→Host同步hello。Host在真实transaction中读取calling UID/PID，检查当前session及持久撤销标记；PID仅诊断。
3. Host只有保留有效会话才交只读source FD及独立output FD。worker开始前再次同步`grant`核对当下撤销，检查regular file/dev/inode/长度及自己的FD flags；不打开任意字符串路径。
4. Host故意将offset移到123后关闭自己副本；worker用显式`pread`读8192B并核对SHA，验证只读写入被EBADF拒绝。正常关闭后再以fstat/EBADF确认data FDs失效；lease继续保持。
5. 完整结果不直接准备；直到原endpoint death与结果均匹配、未撤销才允许合成PREPARED。任意重复/旧nonce/错attempt/gen、迟到success不能产生第二次有效结果。

整个顺序目前**NOT_RUN**。min26静态Lint通过；`/proc/self/cmdline`和`/proc/self/fdinfo`失败均拒绝，API26真实可用性仍待设备证明。没有用API30的fcntlInt或隐藏反射伪造API26兼容。

## 4. 预定恢复与清理不变量（尚未实测）

- **交付/ACK缝隙**：pin落盘先于FD。FD已交但ACK未持久时，恢复不能由“无ACK”推断无consumer，必须取得资源lease证据并撤销旧授权。
- **活跃保护**：超时、取消、parent关FD、解绑或没有回调不解除pin；已持lease时GC拒绝。控制和计算线程分离，认证retire只让worker `killProcess(myPid())`，Host不按持久PID杀进程。
- **恢复收敛**：重启Host不绑定新worker、不重放旧尝试，先核对owner/pin、同名称lease及全部文件身份；旧worker靠client Binder死亡或自身deadline自退。无法取得则保留/UNKNOWN，不启动第二消费者。没有自动清账本/重建代/按文件龄删除兜底。
- **同Host迟到授权**：hello/grant同时核对持久`revoked.json`和内存gate。`revoked_reserve`/`revoked_start`刻意不先撤销内存gate，拟独立证明持久fence有效；实际未运行。
- **精确归属**：owner/pin身份字段一致；stage/source/output的dev/inode、regular/no-follow及源SHA全部在任何删除前检查。任一不明则零删除，保留未知子项，损坏receipt不认领。已知清理后持久pin=false。
- **重放**：每个研究command/runId先排他持久claim；恢复旧Intent不能自动重跑旧任务或重复自杀。这是测试宿主恢复保护，不是永久业务日志。
- **原子receipt**：写前、完整temp未sync、temp已sync/rename前、rename后/目录sync前及目录sync后分别规划切断。receipt中`workerRetired=false`不等于PREPARED。SIGKILL即使将来通过也不证明断电持久化。

这些不变量的实现代码需要独立审查及真实设备验证。目前没有证明所有竞态已消除，也没有证明UNKNOWN能最终收敛；不能据模型存在判B1生产可行。

## 5. 实际FD/恢复结果

两次Host启动均未完成attach，没有取得源FD、pin、stage、receipt、worker PID或死亡监听事件；[最终研究目录观察](m10-jpeg-r3/evidence/device/20261016T150602.612810Z/research-files-presence.json)没有研究files目录。所有12个故障点、消费者FD观察、恢复和不同UID绑定均NOT_RUN；不是“0次泄漏”“全部保旧”或“清理PASS”。

正式App仅做前后APK及文件元数据保护检查：[比较](m10-jpeg-r3/evidence/device/20261016T150602.612810Z/protection-comparison.json)。未读私人账本/图片字节，未调用正式MediaRepository；不能宣称已逐行比对七表或证明真实CAS/共享引用/旧两处SIGKILL继续成立。原历史证据仍保持原效力；新的跨进程生产接缝尚未实现。

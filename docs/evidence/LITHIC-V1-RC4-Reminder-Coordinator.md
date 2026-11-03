# LITHIC V1-RC4 — 提醒共享协调器：实现与安全阻断

2026-11-03。基线 `d8fa6049210f56daa946156db39cba15eb7b7897`；用户转交 RC3 **ARCHITECTURE REVIEW — CONDITIONAL GO**。本轮实施了进程内协调和设置反馈，但安全审查发现两个未关闭门禁，故结果为 **BLOCKED — NOT READY FOR PREVIEW19**。本分支源码仅供独立审查，不应合并、安装或用于真实数据，不能称为最终收口。

## 已实施的范围

- [`ReminderEnqueueCoordinator`](../../app/src/main/java/com/cooper/lithic/platform/notification/ReminderEnqueueCoordinator.kt) 将周期与即时请求分为两个有限槽，每槽最多一个在途、一个合并待处理；观察采用非取消底层 Future 的回调。状态分为 PENDING、ENQUEUED、FAILED、UNKNOWN、CANCELLED；超时、结果 Future 取消、未知同步副作用及回调注册异常不释放槽，不自动重发。只有能证明实际平台入队方法尚未调用的准备异常才记 FAILED；实际入队调用抛错一律保守 UNKNOWN。此前已接受的 Worker 有界重试不改。
- [`ReminderRuntime`](../../app/src/main/java/com/cooper/lithic/platform/notification/ReminderRuntime.kt) 及 [`ReminderWorkManagerPort`](../../app/src/main/java/com/cooper/lithic/platform/notification/ReminderWorkManagerPort.kt) 将正常生产启动、Room/DataStore 变化和设置请求接入共享协调器。渠道初始化在独立协程内处理，失败不阻断调度路径。Room 订阅要求 `emitInitialState=true`，DataStore 首值产生当前设置刷新；入队等待与流收集分离。旧 WorkManager 唯一名称及周期 KEEP / 即时 REPLACE 不变；旧 `start/request` 仅保留供既有隔离 Android 平台测试调用，生产源码没有调用。
- [`ReminderOwnerBarrier`](../../app/src/main/java/com/cooper/lithic/app/ReminderOwnerBarrier.kt) 以实例 lease 而非 generation ID 保护短暂入队准入；`AppContainer.close()` 入口撤销新准入，恢复切换前撤销旧令牌，新代或回退创建新令牌。已在撤销前准入的同步提交和系统 Operation 可能继续，取消观察不证明回滚。**恢复等待风险见下，不能称原恢复活性已验证。**
- 设置保存仍先持久化偏好；页面显示入队确认中、入队成功、明确失败、UNKNOWN 或本地取消，入队成功不表示送达。引入页面票据和真实 lease 校验；同一 VM 回退重绑、旧保存迟到、保存取消、页面重入及刷新保留 UNKNOWN 均有纯 JVM 用例。偏好保存结果 UNKNOWN 时不自动重复保存或入队。

## 交付门禁阻断

1. **进程重建后 UNKNOWN 不继承。** 当前槽与票据仅在内存中；若 WorkManager 事务可能已经生效但结果未观察、随后进程退出，新协调器没有旧 UNKNOWN 的证据，初次刷新可能再次发出 REPLACE。[合成 JVM 用例](../../app/src/test/java/com/cooper/lithic/platform/notification/ReminderEnqueueCoordinatorTest.kt) `new_coordinator_does_not_inherit_old_unknown_process_death_remains_blocked` 确认新实例会再次提交；该用例通过是为了揭示缺口，**不是安全通过或真实 Android 进程死亡证据**。持久未决标记/权威终态核查是可能方向，但须另审标记原子性、丢失/裁剪、恢复及隐私边界，当前没有实现或批准。
2. **恢复与关闭可能等待卡住的同步入队。** 实例门禁为保证撤销与实际 WorkManager 同步提交的线性化，在短锁内执行 enqueue；`revokeForSwitch()` 在 `restoreLifecycle` 内等待该锁，`finishClose()` 亦等待它。若平台同步提交永不返回，恢复可能在切换前持续挂起、close 可能无法返回。现有 JVM 测试最终主动释放锁，并不证明此情形可收口。简单移到锁外会产生退役后才首次入队的竞态，且旧 REPLACE 可能影响新任务；不能通过削弱屏障或把 timeout 当系统取消来回避。**这影响原恢复活性，必须先解决并经独立审查；当前源码不得进入体验候选。**

另外：真实 Room/DataStore 注册时序、WorkManager 状态、进程死亡、通知显示、Owner 真实关闭、OEM 限流及锁调度均未在 Android 执行。现有 flow 测试只证明合成流的订阅合同；渠道失败单元测试只验证局部 helper，不能代替真实启动调度链。M20 平台可见性两次历史 FAIL 和 Owner/周期任务 UNKNOWN 保留。

## 桌面验证与失败保留

- [最终验证回执](v1-rc4-20261103/verification-3/command.json)：最终源码输入哈希与结束时一致，[原始字节及 Git 归一化绑定](v1-rc4-20261103/source-binding.json)已核对；离线、单 worker、隔离输出目录，启动前 RAM 5.026 GiB。**149 项 JVM PASS，0 失败/错误/跳过**，其中 56 项为本轮新增方法；[逐类结果](v1-rc4-20261103/verification-3/jvm.json)。范围涵盖进程内合并、操作结果、timeout/取消、旧 lease、设置票据/UNKNOWN、既有 M20/M21 及规则回归。进程重建缺口的用例按上述解释，不能用总 PASS 关闭阻断。
- 最终生产 Kotlin、AndroidTest Kotlin 源码编译、Lint 均 exit 0；[Lint](v1-rc4-20261103/verification-3/lint.xml) **0 错误、20 警告**。新增 2 项 Owner AndroidTest 仅编译、运行 **NOT_RUN**；没有真实通知、系统任务或用户数据测试。
- [首次验证](v1-rc4-20261103/verification/command.json) exit1：4 个新增设置测试因 runBlocking 返回内部 UI 类型导致编译错误；[原 stderr](v1-rc4-20261103/verification/stderr.log)保留，仅显式声明新测试返回 Unit，原有测试断言未修改。[第二次](v1-rc4-20261103/verification-2/command.json) 136 项 PASS、编译/Lint exit0，对应中间源码；随后复核识别设置页退役/保存和 UNKNOWN 反馈竞态，补修并对最终源码重新验证。一个本轮尚未冻结的新测试改为要求 refresh 保留 UNKNOWN；原有六项设置测试断言保持。
- 未启动 ADB、手机、模拟器、Instrumentation、通知发布或真实账本；没有构建 Preview19 或正式 Release、没有更改版本/签名、没有覆盖旧 APK。Room5、backup4、金额/订阅算法、照片、主题和图标未改；**新增 owner 屏障对恢复活性存在上述阻断，不能宣称恢复合同已通过。**

## 下一步

先独立审查并决定如何在不改变恢复原子协议和原唯一任务策略的条件下同时解决：可证明的跨进程未决入队收口；同步 enqueue 永不返回时不阻断恢复/close，且旧 Owner 退役后不能首次提交。需补受控故障注入及真实 Android 证据，不能用当前 149 JVM PASS 代替。只有上述问题和本任务全部门禁关闭后，才可另行评估 Preview19；本轮不生成候选。

**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**。M10/M20/M22 及 G1～G4 历史 FAIL、UNKNOWN、NOT_RUN 不改判。

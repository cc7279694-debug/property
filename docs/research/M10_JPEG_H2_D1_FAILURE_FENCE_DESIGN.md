# M10-R3-F1B-H2-D1 — 失败围栏最小修复设计

2026-10-17。**DESIGN_ONLY / NOT_IMPLEMENTED**。本轮研究APK、Java及协议实现不改。入口：[离线分析](M10_JPEG_H2_D1_ROOT_CAUSE_ANALYSIS.md)；具体现场恢复见[受控提案](M10_JPEG_H2_D1_SAFE_RECOVERY_PROPOSAL.md)。

## 1. H2-N04与确定缺陷

`ProbeRunner.java:43–46`只写FAIL，不撤销SessionGate、持久owner授权或连接。`:211`仅查disconnected，失败后仍可接受迟到连接并注册death listener。`:218`解绑前先置bound=false，异常可能抹去责任。`:98–112`Host FD没有整体finally。后三者分别是迟到连接围栏、清理责任和异常资源路径缺口；H2未进入FD段，不声称本次已泄漏FD。

`StageStore.java:117–118`不读取run FAIL报告。若current/owner/source有效、pin=true、无cleanup/revoked.json，源码谓词仍可能为真；H2归档的结构满足这些前提，但本轮没有调用设备谓词。**不能把“仍可能授权”写成H2实际接受过旧回调**：H2在Runner:85失败，:91 reserve未执行，Worker6610没有取得IHost callback/nonce/lease。

静态反例适用于已reserve后start/等结果异常：旧gate未撤销、持久条件未变，已有合法UID/PID/nonce的Worker可能继续grant/result。D1的RED模型只复现这个相关状态，不执行原Java。已有durable revoked.json及原AuthorizationFence确实能拒绝其后的匹配回调，不否认F1-A已实现的保护。

## 2. 失败与成功各自的终结条件

建议每个Invocation保存**自己的Session句柄和epoch**。catch不能根据可替换activeSession任意撤销另一任务。Session创建中途失败、尚无完整owner时，关闭全局准入并登记未归属残留，不能猜删或借用旧Session。

```text
ACTIVE → FAILURE_CLOSING → FENCED_DURABLE 或 FENCE_UNKNOWN
                         → CONNECTION_CLOSING / CONSUMER_UNCERTAIN
                         → RECOVERY_REQUIRED
独立获准恢复并验明全部前提 → FAILED_TERMINAL
```

这些是Host控制状态，不是擅自新增StageStore requestStatus枚举。本轮仅画图。失败后pin/current/已知资源保守保留；不能在catch中顺便cleanup。FENCE_UNKNOWN是持久化/清理未知的控制结果，绝非有效套餐、JPEG拒绝PASS或资源已释放。

健康会话保留原SessionGate的结果/死亡乱序契约：**同一有效endpoint的SUCCESS与death可任意先后，只有两者及FD关闭事实齐备才可prepare**。不能为修N04笼统拒绝所有dead后的匹配result。失败/撤销/错误epoch则始终拒绝，旧endpoint死亡只影响旧对象。

## 3. 授权与关闭的线性化边界

1. 第一次失败先关闭全局新Session准入，调用本Session的短内存Gate失败转换，固定第一失败原因、epoch和关闭状态。这个转换不做磁盘、trace fsync、Binder或等待UI。
2. hello、result的现有Gate同步方法必须把失败/epoch检查与状态变更放在同一短临界区。grant也需一个同Gate的原子决策方法，不能把多次isRevoked/isDead读取误当作共同线性化点。失败与接受竞态由该Gate决定先后。
3. 已在失败线性化**之前**发出的grant/同步Binder调用可能仍在飞行；不声称能收回已交付FD或物理阻止Worker继续。它们进入quarantine，保留pin/源，直到独立证明消费者终结。失败点之后不再接受新的授权决策、结果或prepare。
4. 现authorizationLock内有validateStage/读文件/源哈希以及部分fsync。不能让watchdog排队等这些操作，再宣称立即关闭。优先复用SessionGate短同步revoke（不持慢授权锁），结束后释放Gate，再进入后台持久化；回调完成慢持久检查后仍须进入同Gate重查closing/epoch。禁止持Gate再取authorizationLock，保留慢锁→短Gate的既有顺序，避免反向嵌套。
5. 当前代码未实现该安排；Python模型把单次转换当原子，仅证明抽象决策。Java内存可见性、最坏锁等待、线程调度及Binder卡住情况下的watchdog反应均NOT_RUN，不能由模型推定有界毫秒数。

全局准入转换同样要绑定Invocation epoch；迟到的旧catch只能关闭旧Session，不能通过可变activeSession撤销新Session。只有当前epoch可更新当前准入状态；若发现非终结旧Session与新授权并存，作为协议冲突关闭准入，不猜测修复。实际全局协调必须有单一串行入口和短原子状态转换，不能仅靠一次open布尔读取。

### 慢文件提交与内存失败的检查/使用边界

`canPrepare()`后锁外直接落SUCCESS receipt，或“检查准入open→慢I/O→发布current”，都存在TOCTOU。未来实现必须绑定attempt/epoch的单次候选资格，并在同一短协调边界确认其逻辑提交资格；不能只追加一次普通读取。失败前尚未开始发布的候选必须取消，不覆盖current。只有全部旧attempt已安全终结才允许创建新Session候选，原StageAdmission不放宽。

若一个失败前已开始的rename/fsync在失败后才返回，**无法同时承诺watchdog不等磁盘和物理文件绝不变化**。这类在飞行落盘一律作为候选/持久结果未知处理，不是授权：不得bind/grant、认作成功或清理；保持准入关闭，回读/核对落盘身份，未知残留交受控恢复。不得用“token已检查过”给锁外写入授予永久有效资格，也不得凭receipt/current文件存在认定提交。重启时任何未核验终结stage仍阻塞普通run。

这里的提交资格是控制状态，不要求D1新增磁盘格式。若未来实现需要候选文件或proof格式变更才能准确区分，须另行评审，不能把候选伪装为既有已确认receipt或放宽旧断言。22项模型没有模拟Java rename/fsync与失败的真实竞态；新Session发布和receipt提交线性化是未来必测门槛，不能由本轮MODEL_ONLY PASS自动通过。

## 4. 持久围栏及重启

后台串行执行同完整owner的既有`store.revoke(id)`，核对文件内容及同步完成；记录失败与收口状态时保留原异常，不让新的报告写入异常覆盖第一失败。`revoked.json`才是持久撤销，单写state=REVOKED或FAIL报告不能替代。

N01独立保持OPEN：仅REVOKED/pin=true而无revoked文件，当前isCurrentAuthorized未明确拒绝。未来最小方案须将有效状态限定为明确的可授权集合，REVOKED/终结/未知/pending均拒绝；再做其独立回归。本轮模型的REVOKED拒绝测试只是**拟议修复的依赖验证**，不关闭原N01。

若revoke写入、rename、目录sync或回读任一未知：memory fence维持，pin/current/源保留，禁止新Session。进程若在内存关闭后、持久revoke前死亡，未来启动必须完整扫描所有stage：**任何非终结/未知stage都进入RECOVERY_REQUIRED，普通run不得自动closeAll再继续**。这防止内存标记丢失后重新开放。无需新增生产数据库/状态体系。

原StageAdmission的“所有旧lease持续持有至验证、收口/发布完成”不得放宽；新控制入口必须先证明旧authority和连接已收口，不能靠StageAdmission的文件清理替代旧Host终结。

## 5. 绑定责任、迟到回调与FD

| 状态/出口 | 最小要求 |
|---|---|
| POSTED尚未运行 | bind runnable开始前重查本Session/epoch；已失败则不调用bindService，记录CLOSED_NOT_STARTED。移除Runnable只是辅助，不能替代入口检查。 |
| BIND_CALL_BEGIN | 先登记本次连接尝试及清理责任；bindService返回true/false/抛异常分别记录，不能只用一个bound布尔值。Binder调用在授权锁/Gate之外。 |
| 调用期间失败 | 不假定能取消已进入的同步调用；返回后按同连接/attempt履行责任，不能把迟到true解释为新授权。 |
| late onServiceConnected | 先查closing/epoch。失败对象不进入正常reserve/grant，不替换当前有效endpoint；必要death监听只用于隔离观察，不能恢复授权。 |
| 解绑 | 在与原注册匹配的Context/ServiceConnection履行责任；只有明确成功或符合未注册语义才UNBOUND。抛异常保持UNBIND_UNKNOWN及首次异常，不先擦掉责任。重复已确认解绑幂等；未知不能用吞异常变PASS。 |
| result回调 | 被拒绝的回调不能推进正常成功latch；当前Runner:208仅identity相同就countDown需最小调整。可另发“拒绝通知”唤醒停止路径，但不得当阶段完成。 |
| death | 绑定epoch及具体endpoint；重复死亡幂等，不是新endpoint死亡证据。解绑、retire请求、Binder death和OS death分别记录。 |
| Host FD | 每个已打开source/output由本Invocation整体try/finally关闭，包含output创建/元数据写/start抛异常；关闭Host副本不证明Worker副本关闭。 |

已认证Worker的协议retire和未预留Worker的正常退出有不同前提；未知endpoint不可伪造nonce进行退休。取消等待/Future.cancel、解绑、发出retire都不证明远端已停。真实退役、任何包限定停止或恢复均须后续设备授权，本设计不执行。

## 6. 未来最小修改面（本轮均未实施）

| 文件/责任 | 拟议变化 |
|---|---|
| ProbeActivity / ProbeApplication | 入口与角色保持；初始化/claim重I/O离开main，不丢幂等；识别仅恢复模式，不自动run。 |
| ProbeRunner.Session / dispatch | 明确Invocation句柄、失败总出口、阶段deadline、绑定责任及late回调围栏、Host FD finally。 |
| SessionGate / AuthorizationFence使用处 | 短内存失败原子边界、grant原子检查、epoch；保留原健康result/death语义。 |
| ProbeSupport | 有界异步trace、序号/掉日志/写失败可见；不能把授权持久化混入可丢弃trace。 |
| StageStore调用入口/授权谓词 | N01显式拒绝；普通启动非终结stage阻塞；恢复前置门禁。沿用原owner/revoke/terminal格式与严格inode检查。 |

优先不修改WorkerService、AIDL、Manifest、Gradle、依赖、权限、签名或生产MediaFileProtocol；若未来实现证明必须修改，应另行提交范围而不是自行扩展。D1不提供可运行恢复入口。

## 7. 测试及独立验收门槛

本轮22个纯宿主方法验证失败不再授权、排队/飞行绑定、解绑异常、重复失败、持久未知、旧epoch/endpoint、健康结果/死亡乱序、20项恢复前提、部分删除/terminal sync未知及幂等；其中1方法枚举120个抽象顺序，不另算120项测试。见[模型](m10-jpeg-h2-d1/fence_model.py)、[测试](m10-jpeg-h2-d1/test_fence_model.py)、[最终日志](m10-jpeg-h2-d1/evidence/green-final/unittest.log)。

未来必须先独立审查代码和纯JVM回归，保留既有断言；再另获设备授权验证main排队、bind false/异常/超时/迟到回调、同步Binder卡住、watchdog、trace阻塞/溢出、撤销写失败、FD中途异常、death/解绑重复及收口。在这些真实条件通过前，H2-N04/N01均未实施/未关闭，不能重试normal或操作H2残留。

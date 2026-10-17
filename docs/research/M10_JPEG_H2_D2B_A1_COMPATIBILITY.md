# H2-D2B-A1 — API26/27身份兼容与证据边界

2026-10-17。仅研究Host/Support层；用户明确批准D2B方案A。实现/测试状态见[执行报告](M10_JPEG_H2_D2B_A1_EXECUTION.md)，本文件不授予设备运行许可。

## 缺陷与最小修复

原D2B的Application把`/proc/self/cmdline`读取排入后台；API26/27没有API28框架名称提示。未改动的WorkerService在main.onBind调用`ProbeSupport.role(UNKNOWN)`时立即REJECT，形成安全拒绝但合法启动兼容失败。另一个同源入口问题是已知名称原先直接`knownRole`，可能把框架提示或尚未完成的后台名称当作完整授权证据。

本轮只调整ProbeSupport、ProbeApplication，新增被二者实际使用的纯Java `ProcessIdentityGate`。WorkerService、AIDL、Manifest、权限、Gradle、依赖、StageStore和正式App不改。

| 入口/事实 | API26/27 | API28+ | 资格边界 |
|---|---|---|---|
| main角色判定，完整身份尚PENDING | 必要时同步读取最多256字节/proc首个NUL终止参数 | 使用已登记framework提示，不新增同步/proc探测 | 仅HOST/WORKER角色观测；UNKNOWN/异常拒绝，不创建owner、不发布READY |
| 后台初始化 | 独立读取/proc并与已有观测交叉核验，再完成原owner/Runner流程 | 独立读取/proc与framework提示/已有观测交叉核验，再完成原流程 | 名称正确本身还不是READY；流程失败或任意冲突sticky FAILED |
| Binder/非main角色入口，包括reserve | 最多沿用原1秒等待完整READY | 同样要求完整READY，不能仅凭framework名称通过 | 未完成、超时、异常、冲突或任意未知caller均REJECT；保留Worker原UID/lease/hello及Host原围栏 |
| 完整后台已完成但同步探测仍在飞 | READY延后到全部在飞探测确认一致 | 同一进程身份gate约束 | 不能先授权，再让迟到探测覆盖冲突 |

Application只把framework名称及后台实际名称提供给gate；`processName`仍可用于诊断，但不是完整授权真值。同步probe不修改全局processName或mainOwner，单独的同步观测不足以READY。若后台核验及owner/Runner已完整完成，最后一项一致的同步观测结束后，统一gate才可发布READY；并非仅凭本次读取授权。Host持久claim前同时检查mainOwner和gate.verifiedRole，不借用名称提示。未确认claim仍不得开始Invocation。

## 读取、时钟和并发

`ProbeSupport.processName26()`持有try-with-resources文件作用域，实际调用gate的受控解析器；读取、关闭异常都会返回到身份失败路径。解析器累计最多消费256字节，可处理短读取，但零进展、EOF前无NUL、空首参数、非ASCII/控制字符或非精确研究进程名均拒绝。只解析首参数，NUL之后的数据不是身份依据。不得默认UNKNOWN为Worker。

同步读取和时钟获取在短身份锁外执行。发布时重新检查sticky失败及framework、后台、其他同步观测的一致性；在飞计数防止后台提前READY。短锁内不执行Binder、磁盘写、Trace或fsync。该锁只保护进程身份，不替代原Invocation/Session/StageStore锁、epoch或持久撤销。

同步探测用单调纳秒时钟记录开始/结束，snapshot保留probe计数、在飞数、最后耗时、身份状态和首次失败，既有后台报告序列化这些内存信息。100ms为本研究采用的**返回后拒绝阈值**，达到阈值即拒绝；它不是实测手机阈值或硬实时保证。缺少时钟、时钟异常、负值、倒退、差值溢出或读取/关闭错误均fail-closed。永久阻塞的syscall无法由事后检查强制终止，不能因256字节或watchdog存在而宣称最坏时延已界定。

非main等待使用单调总期限，不按唤醒/回调重置；中断恢复线程中断标记并拒绝。等待超时只表示本次未授权，不表示Worker死亡、FD关闭、后台流程取消或stage可清理。

## 保留的风险和独立验收门槛

- 实际API26/27启动、主线程/proc耗时、系统读取行为及ANR均NOT_RUN。宿主注入的API值/时钟/流/调度只能验证研究Java决策，不能升级为设备PASS。
- API28+框架提示仍不是完整核验；真实Binder早到/回调顺序、FD关闭、Service死亡和文件同步仍PENDING_ANDROID_VERIFICATION。
- 原D2A-N01的H2 ANR根因UNKNOWN；N02 watchdog的Android最坏响应UNKNOWN，fixed-rate追赶警告保留；N03内存/磁盘不能原子化，未知继续关闭准入；N04无新增设备证据；N05不实现失败自动terminal，不改变D1恢复门槛。
- WorkerService源码保持不等于共享Application/Support启动行为完全相同；A1必须由独立代码审查确认后，另行决定设备验证授权。本轮不得自行安装、恢复H2或重跑normal。

原H2 FAIL/UNKNOWN、DELIVERY_UNKNOWN/pin=true及源文件现场不动；R3完整生命周期BLOCKED，M10 PENDING/三JPEG FAIL，UI-C1冻结，M11 NOT_AUTHORIZED保持。

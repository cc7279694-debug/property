# M10 最终安全门禁与冻结建议

2026-10-19。**建议：暂不能 ACCEPTED/FROZEN；BLOCKED — M10-FINAL-SAFETY-CLOSURE。**

来源 `9f378199c8bebb2a96d3d061efe16b3e966906ba`，独立分支 `codex/m10-final-safety-closure`。本报告的“当前实现”指该未合并候选；正式 `feat/m10-local-cover-media` 仍为 `415968…`，不能把候选验证写成正式分支已经修复。

**本轮契约：**核对旧29场景与当前实现、重用已有真实证据、执行最小宿主安全实证，给出有限的冻结门槛。既有API26/API36各九项自然测试按用户转交的ChatGPT独立结论继承 **PASS WITH NOTES**，本轮重跑 **0**。没有调用ADB、访问AVD或保留现场、安装/启动App、构建APK、下载工具、重跑native压测；未修改生产/测试App、Room、备份、金额算法、UI或历史断言。故障注入受本轮指令第4条“另行明确授权＋全新合成环境”约束，当前没有该新环境及具体操作许可，不能挪用保留的API36。

## 1. 本轮实际实证与证据层次

- **真实Java组件回归：143个既有唯一JUnit方法PASS**，7个生产组件、12个测试类重新编译，0失败/跳过；不是Android Binder/FD实证。[原始结果/编译/运行](m10-final-safety-closure/evidence/host-143-plus-open-blocker/RESULTS.json)
- **另1个组件刻画实验：REPRODUCES_OPEN_BLOCKER。**真实 `JpegBindingDuty.callRpc()` 在两次清理机会都被在途票据阻止后才返回；其返回仅释放票据，`cleaned=false`，需新的清理责任主体。通过断言证明问题仍存在，不计作修复PASS，也没有模拟Android解绑。[刻画源码](m10-final-safety-closure/LateRpcOpenBlockerTest.java)
- **Stage定向宿主：15个既有唯一方法PASS，0失败/跳过。**重新编译当前Kotlin实现，覆盖持久撤销、迟到成功、真实文件锁、非终结拒绝清理、错凭据/未知子项、源读取中断与GC竞争；不增加正常流程方法。NTFS文件身份是真实的，目录同步回调是宿主测试端口，不能替代Android文件系统/进程死亡验证。[原始编译与15项结果](m10-final-safety-closure/evidence/stage-15/RESULTS.json)
- 7份历史来源与4份本地大图实物重新核对SHA；原报告、原3JPEG FAIL、旧UID UNKNOWN及所有历史记录不改。[重用证据与样本身份](m10-final-safety-closure/evidence/reused-evidence.json)

## 2. 原R3的29项如何收敛

原件：[R3计划及原始结果](M10_JPEG_R3_LIFECYCLE_RESULTS.md)。旧Java研究Host/socket租约原型的 **0 PASS / 1 UNKNOWN / 28 NOT_RUN** 永久保留。以下是风险等价映射，不是把当前产品结果回填旧29项；不重跑过时研究程序。

| 原场景序号与名称（合计29项） | 当前候选门禁、已有证据及真实缺口 |
|---|---|
| 1 `normal` | 两API自然测试已限定验收，**不再测试**。 |
| 2/8/12/13/15/29 `identities, rebind, revoked_reserve, revoked_start, intent_replay, outsider` | **I 身份/重复授权**：SessionFence、同步calling UID/PID、持久revoke、重复reserve/deliver拒绝；正常身份有自然证据。错nonce/重复FD与当前生产Worker的不同UID拒绝仍未设备验证。旧Intent命令入口不在产品；旧H1 Outsider只证明研究包，不能替代当前组件。 |
| 3/5/11 `cancel, late_success, normal_finish` | **A 取消/迟到结果**：当前协程取消、短围栏、持久revoke、abandoned hook；有组件/Stage测试，无当前Android取消实证。旧Activity finish应映射调用方退出，不新增产品命令。 |
| 4 `ignore_cancel` | **B 固定超时/迟到RPC**：watchdog与票据不把等待超时当事务结束；晚于最终清理窗口的返回未自动收口，见B1。 |
| 6/14 `worker_kill, death_race` | **C 异常死亡**：自然自退不等于crash，也不证明所有result/death乱序；当前真实异常死亡未验证。 |
| 7 `unbind` | 正常认证ACK→解绑→死亡已验；提前/异常解绑归A/B，未设备验证。 |
| 9 `fd_duplicate` | Worker检查inode/模式/别名；普通传递已有证据。实际只读写入失败、重复/别名FD、共享offset反例仍NOT_RUN，合入I。 |
| 10 `stage_model` | **G GC/并发**：当前FileLock、owner/inode/未知子项保护；持久review终结Stage有自然证据，活跃lease及异常竞争仅宿主。 |
| 16 `before_pin` | **D0**：source已准备但未pin；当前Worker可能已reserve，不能由“无pin”推断无消费者。生产者复制中断属同源风险，但不冒充同一时点。 |
| 17/18 `after_pin, before_fd` | **D1**：pin已持久、FD交付尚未确认；两者可合并风险门禁。 |
| 19/20 `after_fd_before_ack, before_result` | **D2**：可能已交付FD/计算中Host中断，重启不得新授权或猜删。 |
| 21/26 `after_result, before_retire` | **D3**：有结果但无完整退休/terminal，禁止prepare/释放pin。 |
| 22/23/24 `before_receipt, receipt_pending, receipt_renamed` | **D4**：当前terminal写入前/部分写入/force或目录sync未知。当前 `StageRecords.put()` 是CREATE_NEW直接写，无原型的receipt rename，不能捏造旧hook或旧窗口PASS。 |
| 25 `receipt_synced` | **D5**：完整terminal之后至prepared/清理；已有终结可幂等核验，真实Host重启仍NOT_RUN。 |
| 27 `after_retire` | **D4→D5间隙**：死亡/guard已证但terminal可能尚未写入，直到其完整持久化；不能借后来的terminal证明较早的窗口。 |
| 28 包级 `force-stop before_result` | 与仅Host中断不同；仍NOT_RUN。不能借正常退出授权执行包强停，也不需为了凑29单独重跑旧程序。 |

对应当前源码：[Client](../../app/src/main/java/com/cooper/lithic/data/media/jpegworker/JpegWorkerClient.kt)、[Worker](../../app/src/main/java/com/cooper/lithic/data/media/jpegworker/JpegWorkerService.kt)、[Stage V2](../../app/src/main/java/com/cooper/lithic/data/media/jpegstage/JpegStageV2.kt)、[StageRecords](../../app/src/main/java/com/cooper/lithic/data/media/jpegstage/StageRecords.kt)。历史当前候选[JVM证据](m10-jpeg-stage-v2-review-fix/RESULTS.json)与[API36自然证据](M10_API36_UID_RESUME.md)分开使用；旧D2A/F1等研究测试不累计到当前产品。

## 3. 最后的两项真实BLOCKING

### B1 — 异常消费者最终收口/恢复没有闭环

1. **迟到RPC连接责任**：Client的 `boundedRpc()` 超时保留真实票据是正确的；但两次 `cleanupConnection()` 都因在途RPC返回false后，`finishRpc()` 只有清票据，没有唤醒新的解绑/死亡监督者。Worker仍bound时不强杀以避免BIND_AUTO_CREATE自动重启。因此可能永久保留绑定、Worker、pin和全局JPEG准入关闭。组件实验复现了责任缺口；完整Android触发/时延仍 **NOT_RUN/UNKNOWN**，不是已证实的金额损坏。
2. **无结构化结果的孤儿Stage**：`JpegStageV2.complete()` 要求唯一结果和FD关闭事实；`admissionLocked()` 遇无terminal即阻塞。取消计算中、Worker突然死或Host中断可能没有上述结果；当前 `LocalImageStore.abandoned()` 只为满足唯一结果/完整终结的少数结果收口。进程不见或瞬时取到guard不能补造terminal，重启也没有通用孤儿恢复器。保留是正确的数据保护，但不能称恢复已完成或保证下一次导入可用。

**停止的部分：**没有为“跑通”而放宽terminal、删Stage、补假结果、延长超时或添加无人验证的后台reaper。当前未发现确定的金额破坏/已引用旧封面误删路径；该结论只属代码审查，不替代故障实证。

**关闭B1所需的有限工作：**在单独可审候选中明确“原会话仍唯一负责”的迟到RPC后续清理；对无结果故障给出可验证的非成功终结/保守恢复契约（不能伪装严格解码成功）。随后只验证上表A/B/C/D/G/I异常族及受影响回归，不增加正常流程测试。必须观察：撤销先行、无迟到提交/第二消费者、真正FD/连接/进程终结、持久pin与安全清理/恢复；无法证明则保留并停止。不能预先假定所有未知都可自动恢复。

### B2 — 当前Android全链40MP资源门禁尚无实测

固定libjpeg-turbo **3.2.0**、既有E01/集中候选的GCC/Clang、268次probe及sanitizer/失败释放证据直接复用，**没有重跑**。原[CI结果](m10-jpeg-accelerated/RESULTS.json)中的三类合成图与R1原件SHA匹配；以下是历史 **Linux GCC**，不是当前Android：

| 40MP输入 | 计费峰值B | 进程RSS KiB | wall / CPU ms |
|---|---:|---:|---:|
| baseline 420 | 333,126 | 30,296 | 398.270 / 398.216 |
| progressive 420 | 120,535,371 | 119,264 | 701.964 / 701.836 |
| progressive 444 | 240,265,637 | 236,212 | 1,115.860 / 1,115.783 |

这些运行计费live/liveBlocks/badFrees均0；旧R1 Android standalone的约232MiB RSS、6.141秒检查空档和120秒UNKNOWN仍保留，均不能替代当前Worker严格解码后再进行Android规范化的峰值/耗时/释放。

当前Worker收到native16字段，但没有发布计费峰值/CPU/RSS指标；随后系统Bitmap、EXIF、旋转/缩放不受E01账本覆盖。分配释放后的allocator驻留、ART/系统codec、Host/UI/PNG重叠未测。**512MiB只是受覆盖分配请求预算，不是native堆/RSS/手机硬上限。**40MP、64MiB、合法progressive及1600px/2MiB输出规格不变，不引入更小的隐含拒绝规则。

**关闭B2所需的有限实验：**只用已核对原件，串行执行baseline420、progressive420、完整progressive444三个真实Stage→Worker→规范化→退休资源用例，记录认证PID/创建时间、FD、wall/CPU、Host/Worker峰值与失败释放。采样峰值必须标为下界，内存预算还须包含未计费部分/并发余量，不能凭三张图定义所有手机安全cap。完整444是R1 `single444/valid_40mp_progressive_444.jpg`，SHA `055523…67e6`；旧fixtures同名444实为缺损 `ec0a70…de700`，已特别排除。没有复制或重新生成图片。

精确native指标已经在JNI long[16]内，当前Worker不导出；如需新增诊断适配，只能单独受审测试/候选，不把诊断缺失当作已证明的生产计算缺陷随意改产品。当前锁定Test只有小图入口，不能改叫“大图已测试”。

## 4. 分类、授权边界及停止

| 分类 | 本轮最终结论 |
|---|---|
| **BLOCKING** | **B1异常最终收口/恢复；B2当前Android全链40MP资源与异常释放。**二者未关闭，推荐不冻结。 |
| **PASS WITH NOTES** | 用户转交的API26/API36各九项自然验收；已验证的预算backend/宿主native与sanitizer；本轮真实宿主回归仅按各自证据层次成立。不是故障Android PASS或正式分支冻结。 |
| **NOT_RUN** | 本轮Android取消/超时/迟到RPC/Worker异常死亡/Host中断/恢复与GC竞争、40MP Worker资源、主动FD/身份反例；真机/arm64与32位运行、OEM、Release、完整备份恢复保持原边界，不能把未来模块全部列为本轮新阻塞。 |
| **UNKNOWN保持** | R1超时/取消空档、历史attach/ANR、旧日志/安装UID未知、无限阻塞provider或在途Binder的最坏结束时间；复跑不能消除历史未知。 |

**下一次设备执行唯一准入条件：**先完成B1最小收口契约及离线回归，再由用户另行明确批准“全新专用合成设备＋准确的进程终止/故障方法、次数、包名/UID/Stage范围＋必要测试包/候选安装及3类大图资源采集”。复用已安装SDK/镜像，无新下载；任何关键UNKNOWN停当前依赖链并保留。原API26/API3618项自然用例不重跑；原保留API36、H2、C2及其他旧现场永不作为故障目标。不在本轮擅自创建新AVD或把笼统任务目标当成第4条所要求的另外一份故障授权。

本轮没有App候选代码修复或APK；没有理由用宿主GREEN改变已验收字节。仅提交本报告、最小可复现实验/证据、唯一CURRENT_STATE与checkpoint。M10 **PENDING**、原3JPEG历史FAIL、UI-C1 **FROZEN**、M11 **NOT_AUTHORIZED**、全部历史NOTE保持。停止等待ChatGPT独立审查；本报告给出冻结建议，不自行授予ACCEPTED/FROZEN。

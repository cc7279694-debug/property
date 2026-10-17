# M10-R3 — Worker生命周期实际结果

日期：2026-10-16。**BLOCKED**。只有JVM门禁及构建/安装核对有通过证据；29个计划Android场景中，正常场景两次尝试均UNKNOWN，其余28个NOT_RUN。没有任何Android worker场景PASS。

配套：[执行](M10_JPEG_R3_EXECUTION.md) · [FD恢复](M10_JPEG_R3_FD_RECOVERY.md) · [决策](M10_JPEG_R3_DECISION.md)。

## 1. 实际启动时间线

时刻为原Android日志值，不能擅自当作宿主时区；每次原ADB记录还带UTC及wall时间。

| 观察 | 第一次 | 第二次 |
|---|---|---|
| runId | `fd073806-889e-4eaf-baca-9d373ecba065` | `0cecd3e8-bd56-4068-8cdf-0926c28e272e` |
| 系统START | 15:01:29.224 | 15:06:08.658 |
| Host Start proc | 15:01:40.522 / PID27893 | 15:06:08.779 / PID28245 |
| UID | u0a218＝10218 | u0a218＝10218 |
| failed to attach | 15:01:52.134 | 15:06:18.791 |
| 系统Killing/start timeout | 15:01:52.399 | 15:06:18.804 |
| `am start -W`/场景 | 可以返回Status ok，但没有完整场景报告 | 同样没有完整场景报告 |
| 结论 | UNKNOWN，不能算宿主可用/正常完成 | UNKNOWN；重复后停止扩展 |

原件：[首次run-summary](m10-jpeg-r3/evidence/device/20261016T150047.952611Z/run-summary.json)、[第二次run-summary](m10-jpeg-r3/evidence/device/20261016T150602.612810Z/run-summary.json)、[仅研究包系统日志](m10-jpeg-r3/evidence/device/20261016T150602.612810Z/research-package-only-system-diagnostic.log)、[首次仅研究UID日志](m10-jpeg-r3/evidence/device/20261016T150047.952611Z/research-uid-only-diagnostic.log)。日志只显示该研究UID早期启动，不包含私有账本内容。

最终[进程只读观察](m10-jpeg-r3/evidence/device/20261016T150602.612810Z/research-processes-end.json)未见Host/worker；[研究文件观察](m10-jpeg-r3/evidence/device/20261016T150602.612810Z/research-files-presence.json)为`NO_RESEARCH_FILES_DIRECTORY`。`LITHIC_R3`标签无Application/CASE_BEGIN/worker日志。观察指向Host在attach阶段未完成启动；**底层原因UNKNOWN**，不能断言是某个PoC函数、native内存、OOM、LMK或设备设置造成。

这两次是系统启动超时结束，不是已执行计划中的`kill_host`、包级force-stop、worker主动退休或真正native SIGSEGV。没有自行杀任意PID，也没有为解除阻塞重启/重建/清理AVD。

## 2. 分层结果矩阵

| R3范围 | 实际状态 | 证据边界 |
|---|---|---|
| 独立项目、min26配置、最终APK身份/无权限/无native/JUnit | PASS（构建/静态/安装核对） | 不是API26运行或worker成功 |
| 主包UID10218与helper UID10219不同，正式UID10216 | PASS（安装身份） | 未观察worker PID/UID，不能称Host/worker同UID已实测 |
| Main/worker不同PID、worker不建立第二owner、未知进程拒绝 | NOT_RUN（Android） | 源码有门禁、JVM含role拒绝；Host未完成启动 |
| 非导出Service普通不同UID绑定拒绝 | NOT_RUN（运行） | Manifest exported=false，helper已安装；未执行其绑定场景 |
| 真正同步Binder calling UID/PID、nonce/attempt隔离 | NOT_RUN（Android） | typed AIDL/回调源码存在；17项纯Java不等于Binder实测 |
| FD只读/共享offset/关闭、pin/lease清理 | NOT_RUN | 无源FD授权、无stage创建证据 |
| 取消、TIMEOUT/UNKNOWN、Host响应及worker退休 | NOT_RUN | 尚无worker实例，不能用系统结束Host替代 |
| worker异常结束、回调与death竞态、防二次消费者/自动重连 | NOT_RUN | 合成SIGKILL方案未执行，真实native/OOM/LMK也NOT_RUN |
| Host正常结束、Host独立SIGKILL、包级force-stop及恢复 | NOT_RUN | 原始系统start timeout不满足这些故障点 |
| stage v2、receipt落盘、错代/未知残留与恢复收敛 | NOT_RUN（Android） | 研究模型源码待审，正式M10 GC/owner不变 |
| API26、真机/OEM | NOT_RUN | 未新增镜像/AVD/安装工具，生产硬门槛保留 |

## 3. 计划场景清单与未运行原因

这些是脚本实际列出的待执行场景，不登记成测试方法PASS：

| 类别 | 场景 | 当前结果 |
|---|---|---|
| 正常 | `normal` | UNKNOWN，两次启动无报告 |
| 会话/FD | `identities`, `fd_duplicate`, `rebind`, `revoked_reserve`, `revoked_start`, `intent_replay` | NOT_RUN，前序阻塞即停 |
| 中断/响应 | `cancel`, `ignore_cancel`, `late_success`, `worker_kill`, `unbind`, `normal_finish`, `death_race` | NOT_RUN，同上 |
| 归属模型 | `stage_model` | NOT_RUN，同上 |
| 12个Host自SIGKILL窗口 | `before_pin`, `after_pin`, `before_fd`, `after_fd_before_ack`, `before_result`, `after_result`, `before_receipt`, `receipt_pending`, `receipt_renamed`, `receipt_synced`, `before_retire`, `after_retire` | NOT_RUN，同上 |
| 包级强停 | `before_result`＋研究包force-stop | NOT_RUN；不与仅Host死亡混称 |
| 普通不同UID | outsider bound-service拒绝 | NOT_RUN |

15普通场景＋12窗口＋1包级强停＋1不同UID绑定＝29个唯一计划场景。两次normal尝试不是两个通过案例；原始`attemptedScenarios`和`notRunAfterStop`如实保留。

## 4. JVM测试能证明的部分

最终[XML](m10-jpeg-r3/evidence/build-runs/20261016T145619.048625Z/reports/test-results/testDebugUnitTest/TEST-com.cooper.lithic.m10r3.probe.SessionGateTest.xml)17/17 PASS，无失败/错误/跳过。包含完整身份不匹配拒绝、重复握手/结果、不可逆撤销、8线程至多一次、有效结果和death的两种顺序、缺FD关闭不准备。

这证明纯Java gate的相应输入/并发行为。它依赖调用方提供真实calling身份和匹配死亡证据，不能自行证明FD已关、进程已死、文件可删或生产AppContainer安全。没有把它登记成R3-1～R3-5完整PASS，也没有重跑原M10测试。

## 5. 保留UNKNOWN

本轮Host启动失败根因UNKNOWN；R1的6.141秒检查空档及120秒无结构化native结果继续UNKNOWN，互不替代。研究JVM成功、研究APK已安装、系统结束Host和元数据保护PASS，都不能关闭这些缺口。

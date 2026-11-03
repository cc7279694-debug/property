# LITHIC V1-RC1 — 提醒与后台任务可靠性

## 范围与证据

以 `7cc659df8cb69ea72f6dc89218727c7873188fd4` 为起点，用户反馈 Preview17 体验正常，登记为 **USER_REPORTED_MANUAL_PASS_WITH_NOTES**；该反馈没有逐项运行记录，不扩展为提醒、通知送达、Owner 收口或 Android 自动测试通过。

M20 已有 **ACCEPTED / FROZEN — PASS WITH NOTES**，限既定业务逻辑和原候选范围。原 22 项 Android 方法的最终结果为 22 PASS：由首轮未受影响的 16 项与修正测试 Context 后的 6 项组成，不能声称相同 Test APK 一次执行通过。M21 的既有限定通过另见 [当前状态](../CURRENT_STATE.md)。

| 历史事项 | 原始结论及本轮边界 |
| --- | --- |
| 通知窗口可见性 | 额外真实平台方法两次 JUnit FAIL，均为通知窗口可见性谓词 10 秒超时；个别队列、通知发布、Receipt 子断言及 SystemUI 截图不能改判整项 PASS。[原报告](M20-Android-verification.md) |
| 第二次平台任务和 Owner 收口 | UNKNOWN / BLOCKED：原运行结果有周期任务 ENQUEUED，`ownerClosed=false`；不能用候选成功或进程退出证明周期任务停止或 Owner 优雅关闭。[原结果](m20-android/platform-final-observation/retained-result.json) |
| 冷启动后出现 App 和默认 generation | 进程及新默认代出现是已观察事实。系统组件是否为具体触发源、是否已有新 Worker 执行均 UNKNOWN；源码存在可能路径，不等于已证实 Worker 已运行。[收口报告](M20-Platform-closure.md) |
| 之前已修复的同步异常 | V1 Stability Closure 已处理同步 request 抛异常时同代 changes 监听被拆掉的问题；不可计作本轮新修复。[历史报告](LITHIC-V1-Stability-Closure.md) |
| Android 平台覆盖 | 真机/OEM、API26、重启后队列、真实午夜/LMK/断电、完整 TalkBack 及实际系统大字号，均仍 NOT_RUN；发布调用、系统可见和用户收到是不同事实。成功发布到 Receipt 落盘之间的崩溃重试窗口仍为历史 NOTE。 |

## 本轮源码审查与结果

- **确定的同步故障耦合**：原 `ReminderWorker` 的 RECONCILE 先请求里程碑，若请求同步抛异常，续费协调直接跳过。现经 `ReminderReconciliation.kt` 先协调续费，再请求里程碑；普通异常不跳过另一侧，两侧失败保留主异常及附加异常，仍进入 Worker 原有有界 retry/failure；取消立即传播。
- **确定的异步结果未观察**：原 `requestMilestones()` 丢弃 WorkManager `Operation`，异步入队失败时 Worker 仍可能报告成功。现等待该操作结果，失败经上述边界传播；**入队完成不等于 Worker 执行、通知显示或资源关闭**。若操作一直不返回、进程中断或取消后系统实际状态，Android 尚未证明。
- 调度仍使用唯一周期 KEEP / reconcile REPLACE，候选绑定 generation 和事实 revision，发布在当前 Owner 的协调器租约下核对权限及 Receipt；暂停、计划停止及恢复按冻结业务日期判断。已停止订阅可能仍安排候选，执行时重新读取事实抑制；现有证据不足以认定错误发布或安全终止，不据此改变调度规则。
- `ReminderRuntime` 在启动时可安排周期任务及重协调；正常 Application 冷启动可能创建默认 Owner，但历史触发者、是否有实际新 Worker 和任务最终状态仍 UNKNOWN，不声称已消除这些不确定性。原崩溃发生于通知发布与 Receipt 持久化之间的重复窗口仍保留。
- 仅更改 `ReminderWorker.kt`、`ReminderScheduling.kt`，新增 `ReminderReconciliation.kt` 和相应测试。未改变 Room5、backup4、Owner/generation、恢复、金额/日期/订阅规则、照片、主题、图标、版本或签名；没有清理未知任务、后台数据库或用户数据。

## 定向验证

- [最终定向验证命令与回执](v1-rc1-20261103/final-recheck/command.json)：离线、单 worker、独立构建输出（[执行时的 init 脚本副本](v1-rc1-20261103/isolated-output.init.gradle)）；启动前可用内存 4.768 GiB，Gradle exit 0。8 个相关类 **118 项 JVM PASS，0 失败/错误/跳过**，其中 `ReminderReconciliationTest` 9 项为新测试；覆盖正常、同步与挂起后的失败、取消、双失败，以及既有权限、Receipt、旧 Owner、事实 revision、停止/恢复与失败边界。[逐类结果](v1-rc1-20261103/final-recheck/jvm.json)。这些是 JVM 合成测试，不是 WorkManager 平台集成或故障注入。
- 同一命令生产 Kotlin、AndroidTest Kotlin 源码编译及 Lint exit 0；[Lint 原始 XML](v1-rc1-20261103/final-recheck/lint.xml) 为 0 错误、20 警告。**所有 Android instrumentation、设备、系统通知和后台运行均 NOT_RUN**，不以编译替代运行。
- 首轮仅同步修复版本曾 115 JVM PASS、编译/Lint 通过；随后识别异步入队问题，结果不用于证明最终版本。含异步修复的首轮测试 118 项中 **2 FAIL**：新增测试在协程实际挂起后对异常强制对象同一性，观察到相同类型及消息但对象不同；[原 XML](v1-rc1-20261103/final/failed-junit/TEST-com.cooper.lithic.platform.notification.ReminderReconciliationTest.xml)、[原命令与退出码](v1-rc1-20261103/final/command.json)保留。协程栈恢复可复制异常；新测试改为同时严格验证到达 helper 的异常对象被原样传播、类型/消息正确及原始注入异常的对象身份存在于 cause 链，真实挂起与取消断言保留；原有测试断言未修改。失败后回显日志的主机包装器另出现 GBK `UnicodeEncodeError`，不影响保存的原始 Gradle 日志与 exit1，[说明](v1-rc1-20261103/final/wrapper-error.json)保留。
- Preview17 与其余 32 个既有 APK 在最终运行前后 SHA/大小均一致，未构建任何新 APK。未调用 ADB、手机、模拟器或真实账本。

## 后续最小手机验收

仅在设备身份、独立可丢弃账本和用户另行授权满足后，由用户完成或配合执行：

1. 记录真实 Android 版本、厂商及通知原设置；在独立账本建立符合实际提醒时间窗口的合成订阅。核对允许与拒绝通知权限下的实际表现。
2. 核对提醒开关、暂停、停止与重新启用后的状态，观察发布/送达、取消及有无重复；明确区分任务注册、Pending、Active、系统显示和用户实际收到。
3. 按批准范围正常退出重开，必要时在另获批准下核对重启后的状态；分别记录业务 Owner 是否得到明确关闭证据、任务是否仍活动。
4. 若无合法时间窗口、系统受限或证据不足，登记 NOT_RUN / UNKNOWN；不改系统时钟、不强刷通知、不自动重试，也不清理未知任务。出现异常停止并保存脱敏证据，还原用户原设置。

无真实 Android 运行或足够证据时，不宣称系统通知准点、跨 OEM 后台送达或任务彻底收口。

## 发行条件

本轮桌面源码修复和定向验证可提交独立审查；通过审查并获新授权后，可准备下一次集中体验候选。**正式 V1 不具备 GO 条件**：M20 可见性原 FAIL、周期任务与 Owner 收口 UNKNOWN、真实备份恢复及升级、M22 长历史/离线/大字、C6 隔离和其他发行门禁仍须按各自范围验证或由用户明确决定是否延期。

正式发行仍 **V1_RELEASE_NO_GO**；严格 JPEG 门禁 **STRICT_JPEG_IMPORT_BLOCKED**，隔离通道 **ANDROID_ISOLATION_RUNTIME_PENDING**。历史 M10/M20/M22 与 G1～G4 未通过和未运行记录保持，未引入照片删除/GC。报告不替代独立审查。

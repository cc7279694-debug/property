# LITHIC V1-RC3 — 提醒入口阻断设计与发行门禁

2026-11-03。基线 `b828d2ddd1d4fccec47885fa35c8fb6d2236aa0d`；用户转交 Preview18 **ACCEPTED / FROZEN — PASS WITH NOTES（仅桌面候选）**，详见 [RC2](LITHIC-V1-RC2-Integration.md)。正式 V1 仍 **NO-GO**。本轮对照真实源码、现有测试及历史报告完成只读审查，按任务书的架构安全例外停止实施，未改生产/测试源码。

## 提醒入口：BLOCKING — 需先独立审查共享协调设计

1. [`ReminderScheduling.start/request`](../../app/src/main/java/com/cooper/lithic/platform/notification/ReminderScheduling.kt) 分别丢弃周期 KEEP 与一次性 REPLACE 的 WorkManager `Operation`。本机 AndroidX 2.12.0 入队异步执行，返回并不证明成功；启动、账本变更、设置保存及设置页恢复前台均可能请求，异步失败不能由当前同步异常处理观察到。具体设备是否发生故障 **UNKNOWN**。
2. [`observeReminderRequests`](../../app/src/main/java/com/cooper/lithic/platform/notification/ReminderRequestObservation.kt) 先请求后订阅；直接在此等待 Operation 会推迟对 [`ReminderRuntime`](../../app/src/main/java/com/cooper/lithic/platform/notification/ReminderRuntime.kt) 中 Room `emitInitialState=false` 变更流的订阅，等待期间事件存在遗漏风险。设置 DataStore 变更、保存回调和 ON_RESUME 还会触发多个 REPLACE；单独给每个入口重试可能覆盖较新任务或形成任务风暴。
3. `AppContainer.close()` 关闭实际 owner，但没有将已发布的 `storageState` 明确更新为非 Ready；ReminderRuntime 有自己的 SupervisorJob。因此仅看 generation ID、Ready 状态或 `===` 不能证明 owner 尚未关闭。取消等待、超时或 Future `cancel(false)` 只说明观察被取消，不证明 WorkManager 入队事务或 Worker 已撤销；对 UNKNOWN 自动 REPLACE 不安全。
4. [`NotificationSettingsController`](../../app/src/main/java/com/cooper/lithic/feature/settings/NotificationSettingsViewModel.kt) 的保存回调为同步 `() -> Unit`，只能报告同步异常；已保存偏好并不证明异步调度成功。若只改为后台回调，旧保存、刷新和 owner 关闭后的迟到失败可能覆盖新页面反馈。现有页面没有明确的“调度成功”声明；没有 notice 也不能当成成功。
5. `ensureChannel()` 与 `ReminderScheduling.start()` 同处一个 try；前者同步异常会跳过周期注册和该启动请求。分开两个错误边界可以独立缩小此耦合，但不能解决所有异步入口、去重和 owner 问题；**本轮未将局部调整冒充最终修复**。

为满足本轮全部竞态和安全合同，所有上述请求需要共享同一有界、可取消的协调边界，并贯通真实 owner 生命周期和设置反馈，属于调度架构变更；目前没有足够证据证明小补丁能同时覆盖所有入口。按用户明确要求，停止强行实施，等待独立审查。

## 建议提交独立审查的最小设计（尚未实施）

- 一个进程内共享、单飞且有界的入队协调器，统一启动、变更及设置来源；仅保留有界的最新待处理事实标记，不为每次变更创建无限观察者。需要先订阅并证明监听已建立，再处理初始刷新；严禁为避免空窗而跳过原有真实变更。
- 每次携带**实际 owner 实例生命周期与单调请求票据**。生命周期必须包含 `close()` 的真实退役信号，而不是仅比较 generation ID 或 Ready；旧 owner/过期结果不得更新新页面或驱动自动补发。如何不改变恢复/原子切换协议，需要明确评审和测试。
- 明确区分同步失败、异步明确失败、已确认入队、取消与 UNKNOWN。监听只在后台且有界；UNKNOWN 或超时不自动重发或取消系统任务，不能把 Future 取消当成底层事务终止。仅在明确失败、owner 有效、请求仍为最新且去重安全时，才考虑一次有限恢复；前一结果未证实时保持风险状态，不用 REPLACE 假设已经结束。
- 设置先按原合同持久化，独立展示“入队确认中 / 明确失败 / 未知”；不得把偏好保存或静默无 notice 说成调度成功。设置刷新、再次保存、页面退出和 owner 退役要使旧反馈票据失效，不自动回滚已保存偏好。
- 将渠道创建和周期/即时注册的同步错误隔离，验证一侧失败不会阻断另一侧；周期 KEEP、即时 REPLACE、日期/权限/Receipt 与默认开关保持。所有真实系统任务状态仍需设备证明。
- 实施前先由 ChatGPT 独立审查上述状态机、实际 owner 退役信号、UNKNOWN 后如何恢复，以及与设置页面和重复来源的接线；不得单凭协程单测推断系统任务已停止。

## 验证事实

本轮 **SOURCE_AUDIT_ONLY / REMINDER_ENTRY_BLOCKED**：实际读取代码、RC1/RC2/RC0及既有发行证据；没有新增或修改生产/测试源码。审查期间可用 RAM 曾为 2.966 GiB，低于项目此前约定的 4 GiB 构建门槛；用户释放内存后复测为 4.451 GiB，已超过门槛。架构方案仍待独立审查且没有可供验证的新修复，因此本轮 Gradle、JVM、生产/AndroidTest 编译及 Lint 均 **NOT_RUN**；内存现已不是本次停止的原因。RC1 的 118 项及 RC2 的 80 项 JVM 和既有编译/Lint 仅为各自已记录历史，不作本轮重跑。未操作 ADB、手机、模拟器、历史 APK 或用户数据；未生成新 Preview/Release。

后续获准实施后需一次性覆盖：同步/异步失败、持续挂起、取消及 UNKNOWN、不丢 Room/设置变更、并发入口/真实任务去重、旧 owner 关闭/同 generation 复用、过期结果、设置持久化与提示、渠道与调度独立失败；桌面 fake Operation 仅证明实现合同，真实 WorkManager/OEM 行为仍需独立 Android 验证。M20 两次平台窗口可见性 FAIL 及第二次任务/Owner 收口 UNKNOWN 不变。

## 发行差距与最小决策清单

| 分类 | 建议与现有证据 |
| --- | --- |
| A — 正式发行前必须验证或修复 | 若提醒维持当前产品范围，先解决上述入队结果观察与多入口竞争，并在获授权的独立账本上验证真实权限、取消/停止/恢复、重复送达和 Owner/任务收口；原 M20 历史 FAIL/UNKNOWN 保留。现有核心数据和照片功能须完成当前候选真实 SAF 备份、确认覆盖恢复、重新打开、异常中断和媒体完整性；Preview 同包升级须先保护备份并验证数据不丢失。M22 T06 原三次 FAIL 后完整离线旅程、正式 T03 规模、真实 IME/大字和支持设备性能仍待验；Release 正式签名、密钥保管、升级链及 G1～G4 按原门禁审查，Debug 同证书不替代。 |
| B — 可建议限制或延期，须用户及独立审查批准 | 严格 JPEG 导入和 C6 隔离 8 项设备测试可针对**尚未开放的对应链路**建议延期，但严格 JPEG 历史三 FAIL 与隔离 NOT_RUN 不变。永久删除/GC、历史照片逐张管理及更广 API/OEM 可提出延期；已开放的 PHOTO-LITE PNG 照片与备份恢复不能因此免于当前支持范围的数据安全验证。提醒的降级、关闭或默认设置变化也须另行批准，不由本报告决定。 |
| C — 有足够既有证据，无须无关重复 | 在源码及边界不变的限定范围内，继承 RC1 118 JVM、RC2 80 JVM/编译/Lint及 Preview18 静态身份/签名/DEX；原 M20 22 项由不同轮次 16+6 组成、仅约束当时对象。用户对 B2 合成旅程及 Preview17 的反馈只作为 USER_REPORTED，不提升为自动测试或当前完整产品 PASS。已有历史证明不需机械重跑，但不能用其替代新增状态机回归或真实设备门禁。 |

**下一决策**：独立审查是否批准共享协调器及真实 owner 退役信号的实施范围；之后再按明确授权运行定向桌面和独立 Android 验证。任何延期/范围变化须用户决定。本报告不批准正式发布或进入 M23。

**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING**。

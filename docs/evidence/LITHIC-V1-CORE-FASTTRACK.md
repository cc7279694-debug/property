# LITHIC V1 核心版 — 提醒延期与体验候选

2026-11-05。状态：**SOURCE_PREPARED / BUILD_DEFERRED / CORE_PREVIEW19_BLOCKED**。依据用户提交的加速任务，从 Preview18 基线 `b828d2ddd1d4fccec47885fa35c8fb6d2236aa0d` 建立 `codex/v1-core-fasttrack-no-reminders`；基线 App 树 `fa84443f0569427390767fbc5df0d7f43b3a899a` 与 Preview18 构建记录一致。RC4 `354b99d` 不是基线祖先，未合并、挑拣或复制 RC4 协调器/Owner Barrier。

## 已准备的源码

- `ReminderCapability.AUTOMATIC_NOTIFICATIONS` 固定关闭，独立于历史 DataStore 偏好；正常启动不再调用 `ReminderRuntime`。Runtime、周期/即时/里程碑调度、两个通知发布方法及渠道创建均加入停用保护。
- 保留 `ReminderWorker` 类：`doWork()` 在读取 mode、调用 `openStorage()`、写回执或发布之前检查门禁并成功退出。旧四种 mode 的隔离 Android 测试源码已准备，尚未编译或运行。
- 进程启动时在后台一次性提交三个旧唯一任务名及两个提醒专属标签的取消；不调用 `cancelAllWork()`，不等待系统事务或自动重试。每项初始记为 UNKNOWN，仅明确取得 Operation 终态才记录结果；Operation 成功不等于系统任务已经实际终结。历史数据库是否存在关联的其他依赖链及设备真实取消状态均 **UNKNOWN**。
- 设置页显示“自动提醒将在后续版本提供”；旧提醒路由显示中性说明与返回，不提供开关或权限引导。订阅、提醒日期事实、历史偏好、Receipt 和其他业务导航保留。

## 安全阻断与证据边界

旧任务在冷进程中被系统唤起时，Android 会先创建正常 `LithicApplication`，其 `onCreate()` 仍按 Preview18 原合同启动 `AppContainer`；WorkManager 配置也持有该 Owner。因此目前仅有 **Worker 自身在 `openStorage()` 前退出** 的源码依据，不能证明旧任务引发的整个进程在任何 Owner/数据库活动前退出。该事实与任务书要求的更强先后顺序存在差距；不能修改历史断言、暗中改变 Owner 启动合同或宣称门禁通过。需要独立审查确认安全的启动时序方案，并按该方案验证核心启动、恢复及旧任务行为。

已有 `M20NotificationSettingsUiTest` 的旧入口断言、M20/M21 的旧提醒发送断言与本轮延期决策不再相同；原测试与历史证据均未删除或改写，当前实际运行 **NOT_RUN**，不能称旧自动通知测试通过。

## 验证与体验包

本次先完成源码准备。用户要求等内存充足后再继续构建；实际复测可用内存在约 3.6～4.3 GiB 波动，**未启动 Gradle、JVM 测试、Kotlin/AndroidTest 编译、Lint 或 APK 构建**。新增 JVM 及 AndroidTest 仅为源码，均 **NOT_RUN**；`git diff --check` 和只读源码核对不等于编译或运行通过。

本轮只读源码结构核对：门禁固定关闭；Worker 内门禁在 `openStorage()` 前；正常入口不创建 `ReminderRuntime`；旧提醒路由转至说明页；取消计划恰为三个已知名称及两个专属标签，未使用 `cancelAllWork()`。源码检索未发现其他业务使用 WorkManager 入队，但历史设备任务依赖关系不能由此推定。冻结的数据层、schema、构建身份及资源目录没有源码差异。这些检查不是 JVM/Android 安全证明。

只读核对的 Preview18 为 20,506,559 字节、SHA-256 `afa9b49da1952342a56403eaa5150261d641f3ef2f6cba68d43501e1555d8e4a`，原证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；既有清单 35 份及 Preview18 均与原记录一致。**Preview19 未生成或归档，版本配置仍为 Preview18/code34。**

## 发行差距与未来个人核对

| 分类 | 状态 |
| --- | --- |
| 正式发行前必须验证 | 当前同源候选的真实写入和数据保留、SAF 备份/覆盖恢复与异常中断、Preview18→19 升级、正式签名/G1～G4，以及适用的 M22 离线、长历史和输入兼容。 |
| 延期且保留历史结论 | 自动通知、RC4 协调器、严格 JPEG 原图导入、未开放媒体链路的 C6 运行验证和历史照片永久清理；已开放 PHOTO-LITE/备份不因此免除数据安全门禁。 |
| 用户报告 | Preview17 体验及独立 B1-QA 备份恢复为 USER_REPORTED_MANUAL_PASS_WITH_NOTES，不是 Android 自动测试 PASS。 |

未来候选通过静态和独立审查后，用户可自行核对：①原记录和物品操作；②订阅及付款事实；③照片与三主题；④使用可丢弃数据检查备份与恢复；⑤升级后数据和异常提示。手机安装、覆盖恢复及数据操作均需当时明确授权。

未操作 ADB、手机、模拟器或真实账本。**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING** 和历史 FAIL/UNKNOWN/NOT_RUN 保留。

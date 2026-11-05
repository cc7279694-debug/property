# LITHIC V1 核心版 — 提醒延期与体验候选

## V1-CORE-R1 冷启动修复（2026-11-05）

**状态：CORE_PREVIEW19_BLOCKED。** 从 `a5e40f27fea3649b2e0e92bbee8bb9ec5d390151` 继续，未合并 RC4。`LithicApplication` 的后台冷启动不创建 Owner；WorkManager Configuration 只取得惰性 provider；禁用 Worker 在求值 provider 前退出。`MainActivity` 调用 `startOwnerForUi`，使用同一个原 `AppContainer` 和它原有的幂等 `start`。Owner/generation、Room5、backup4 与恢复实现未修改。系统冷进程不发起旧任务清理；UI 路径才在后台尝试本进程一次，普通同步异常记录 UNKNOWN，不阻断账本启动。系统重建新进程后仍可能再次尝试，无法宣称跨进程结果已收口。

桌面定向验证最终 **167 项 JVM PASS，0 失败/错误/跳过**，包含生产 `CoreOwnerBootstrap` 的模拟冷启动、惰性 provider、重复/并发 UI 入口、取消入口失败及真实 `AppContainer` 合成失败与重复启动；生产 Kotlin、AndroidTest Kotlin 编译及 Lint exit 0，Lint **0 错误、20 警告**。首次源码阶段 166 项 PASS，随后审查发现同进程同步异常后的重试及主线程准备问题，保留[首轮日志](v1-core-r1-20261105/verification.log)、[首轮测试](v1-core-r1-20261105/first-junit/)和[首轮 Lint](v1-core-r1-20261105/first-lint.xml)，补修后[最终日志](v1-core-r1-20261105/verification-final.log)、[最终测试](v1-core-r1-20261105/final-junit/)和[最终 Lint](v1-core-r1-20261105/final-lint.xml)另存。最终[源码输入 SHA](v1-core-r1-20261105/source-inputs.json)经复核未变。JVM 注入模拟与实际 Android Application/Worker/Activity 启动不同；后者、真实恢复、系统任务取消与通知均 **NOT_RUN / UNKNOWN**。原 `AppContainerTest.actual_application_onCreate_starts_its_private_default_storage` 和直接依赖旧 Application 启动的历史测试未改，其原启动时序与本轮新授权不一致，实际运行 NOT_RUN。

**未通过的安全门禁：**已保留的三个唯一名称和两个专属 tag 取消使用 WorkManager 2.12.0；[本地库原始字节码与 AAR 类核验](v1-core-r1-20261105/workmanager-cancel-audit/receipt.json)证明其按名称/tag取消会级联取消未完成的依赖后继。产品源码历史未发现非提醒依赖链，但实际设备历史 WorkManager 数据拓扑 UNKNOWN；若非提醒任务依赖旧提醒任务，现实现会取消该后继。因此不能满足本轮“不误伤其他 WorkManager 任务”的无条件门禁，也不能把五个目标的 JVM fake 测试当系统隔离证明。准备了隔离 AndroidTest 源码，但既定测试身份尚未配置、实际未运行。此项为平台行为与现有取消合同的冲突，不代表已观察到用户任务损失。

因此未将 Private Trial 改为 code35，未构建或归档 Preview19，也未执行 APK Manifest/DEX/证书新包审计。等待独立审查安全取消策略后再决定后续；不得在未解决该门禁时以编译或测试数量宣布候选可用。测试后[36份历史 APK 的大小及 SHA](v1-core-r1-20261105/protected-apks.json)均与原记录一致；本轮未操作 ADB、手机、模拟器或用户数据。V1_RELEASE_NO_GO、STRICT_JPEG_IMPORT_BLOCKED、ANDROID_ISOLATION_RUNTIME_PENDING 保持。

---

2026-11-05。状态：**TARGETED_CHECKS_PASS / CORE_PREVIEW19_BLOCKED**。依据用户提交的加速任务，从 Preview18 基线 `b828d2ddd1d4fccec47885fa35c8fb6d2236aa0d` 建立 `codex/v1-core-fasttrack-no-reminders`；基线 App 树 `fa84443f0569427390767fbc5df0d7f43b3a899a` 与 Preview18 构建记录一致。RC4 `354b99d` 不是基线祖先，未合并、挑拣或复制 RC4 协调器/Owner Barrier。

## 已准备的源码

- `ReminderCapability.AUTOMATIC_NOTIFICATIONS` 固定关闭，独立于历史 DataStore 偏好；正常启动不再调用 `ReminderRuntime`。Runtime、周期/即时/里程碑调度、两个通知发布方法及渠道创建均加入停用保护。
- 保留 `ReminderWorker` 类：`doWork()` 在读取 mode、调用 `openStorage()`、写回执或发布之前检查门禁并成功退出。旧四种 mode 的隔离 Android 测试源码已编译，实际运行 NOT_RUN。
- 进程启动时在后台一次性提交三个旧唯一任务名及两个提醒专属标签的取消；不调用 `cancelAllWork()`，不等待系统事务或自动重试。每项初始记为 UNKNOWN，仅明确取得 Operation 终态才记录结果；Operation 成功不等于系统任务已经实际终结。历史数据库是否存在关联的其他依赖链及设备真实取消状态均 **UNKNOWN**。
- 设置页显示“自动提醒将在后续版本提供”；旧提醒路由显示中性说明与返回，不提供开关或权限引导。订阅、提醒日期事实、历史偏好、Receipt 和其他业务导航保留。

## 安全阻断与证据边界

旧任务在冷进程中被系统唤起时，Android 会先创建正常 `LithicApplication`，其 `onCreate()` 仍按 Preview18 原合同启动 `AppContainer`；WorkManager 配置也持有该 Owner。因此目前仅有 **Worker 自身在 `openStorage()` 前退出** 的源码依据，不能证明旧任务引发的整个进程在任何 Owner/数据库活动前退出。该事实与任务书要求的更强先后顺序存在差距；不能修改历史断言、暗中改变 Owner 启动合同或宣称门禁通过。需要独立审查确认安全的启动时序方案，并按该方案验证核心启动、恢复及旧任务行为。

已有 `M20NotificationSettingsUiTest` 的旧入口断言、M20/M21 的旧提醒发送断言与本轮延期决策不再相同；原测试与历史证据均未删除或改写，当前实际运行 **NOT_RUN**，不能称旧自动通知测试通过。

## 验证与体验包

此前用户要求先完成源码、等待内存，故该时点未启动 Gradle、JVM、编译或 Lint；之后用户告知内存充足，本轮重测 5.88 GiB 后，在独立 `app/build/core-fasttrack-verification` 目录离线、单 worker 执行验证。首次运行 12 个测试类 **135 项 JVM PASS**，补充执行设置说明和备份导出两个现有测试类 **23 项 PASS**，合计 **158 项，0 失败/错误/跳过**。生产 Kotlin 与 AndroidTest Kotlin 编译通过，Lint **0 错误、20 警告**。日志和逐类结果见[验证证据](v1-core-fasttrack-20261105/)，验证源码提交 `f78200033b5e85084a3298733911306c0f2fb1d9` / App 树 `483f767b10222051551c5d66a29812bfbb7be7cb`；Android instrumentation、真实任务取消和通知仍 **NOT_RUN / UNKNOWN**。没有执行 APK 打包。

本轮只读源码结构核对：门禁固定关闭；Worker 内门禁在 `openStorage()` 前；正常入口不创建 `ReminderRuntime`；旧提醒路由转至说明页；取消计划恰为三个已知名称及两个专属标签，未使用 `cancelAllWork()`。源码检索未发现其他业务使用 WorkManager 入队，但历史设备任务依赖关系不能由此推定。冻结的数据层、schema、构建身份及资源目录没有源码差异。这些检查不是 JVM/Android 安全证明。

只读核对的 Preview18 为 20,506,559 字节、SHA-256 `afa9b49da1952342a56403eaa5150261d641f3ef2f6cba68d43501e1555d8e4a`，原证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；测试后[既有清单 35 份及 Preview18](v1-core-fasttrack-20261105/protected-apks.json)均与原记录一致。**Preview19 未生成或归档，版本配置仍为 Preview18/code34。**

## 发行差距与未来个人核对

| 分类 | 状态 |
| --- | --- |
| 正式发行前必须验证 | 当前同源候选的真实写入和数据保留、SAF 备份/覆盖恢复与异常中断、Preview18→19 升级、正式签名/G1～G4，以及适用的 M22 离线、长历史和输入兼容。 |
| 延期且保留历史结论 | 自动通知、RC4 协调器、严格 JPEG 原图导入、未开放媒体链路的 C6 运行验证和历史照片永久清理；已开放 PHOTO-LITE/备份不因此免除数据安全门禁。 |
| 用户报告 | Preview17 体验及独立 B1-QA 备份恢复为 USER_REPORTED_MANUAL_PASS_WITH_NOTES，不是 Android 自动测试 PASS。 |

未来候选通过静态和独立审查后，用户可自行核对：①原记录和物品操作；②订阅及付款事实；③照片与三主题；④使用可丢弃数据检查备份与恢复；⑤升级后数据和异常提示。手机安装、覆盖恢复及数据操作均需当时明确授权。

未操作 ADB、手机、模拟器或真实账本。**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING** 和历史 FAIL/UNKNOWN/NOT_RUN 保留。

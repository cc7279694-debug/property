# M22 真机默认进程安全收口

**最新续接结果：ALREADY_STOPPED，force-stop 0次。**手机重新连接后已取得独立证据，见末节；下列首轮USB失败记录原样保留，不回写为PASS。M22产品状态仍BLOCKED / CHANGES REQUIRED。

## 首轮USB未准入（历史记录）

**READY_FOR_REVIEW — M22 PHYSICAL PROCESS CLOSURE**（仅交付本次准入结果）；**实际 BLOCKED — AUTHORIZED_USB_TARGET_UNAVAILABLE**。未能确认此前精确USB目标处于唯一在线`device`状态，第一道门禁即停止。**force-stop 0次，不能登记ALREADY_STOPPED或PROCESS_STOPPED。**M22仍BLOCKED / CHANGES REQUIRED。

## 授权与真实执行

来源`fb888d569b65d58babfdcfaefb52f9b771bb4303`，分支`codex/m22-experience-accessibility`；开始时本地/远端HEAD一致、工作区干净。用户仅授权核验后最多一次系统强制停止独立M22 App `com.cooper.lithic.m22verify.debug`（预期UID10420、App SHA b749…b2d3），不授权Test更新或任何测试。原M10 UID10376/code8和Test UID10427/旧SHA c379…037f为保护对象；上述均是锁定输入，**本轮未取得新的手机安装身份核验**。

2026-10-24 **11:50:19.938859～11:50:20.434654（Asia/Shanghai）**执行唯一一次主机设备发现。实际ADB argv显式含此前锁定USB序列，公开回执仅用其SHA-256替代：`e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92`。

- 命令`devices -l`退出码0，stdout **143 bytes**，SHA-256 **`4cd9b283c6b0411a7e04918dacea2cc057808b698300237193fad89cbeee37cb`**；stderr 0 bytes。
- 选择器未能确认旧精确USB序列唯一且处于`device`状态，记录`Authorized USB unavailable`，本地监督器退出1。命令exit0不等于目标身份准入通过。
- 全设备清单只在内存处理，未归档原文，避免提交私人硬件序列或其他设备身份；仅保存命令、时间、原始长度/SHA和失败原因。选择失败前没有保存目标行细节，**无法区分未连接、offline、未授权或其他身份/状态差异，原因UNKNOWN**，不猜测。
- 用户随后在本聊天明确回复“没有，当前没有任何重要操作”。这只补充用户操作事实，不替代USB身份、进程或Job核验；未因此重新查询或执行停止。

[唯一原始命令完整性回执](m22-process-closure-20261024-035019/commands/before-discovery/command.json)；[本轮结果及操作计数](m22-process-closure-20261024-035019/result.json)。没有向手机发送任何shell命令，也未查询或操作其他设备。

## 分项结果

| 检查 | 本轮结果 |
| --- | --- |
| 唯一手机型号/API/Boot/实时安装身份 | **NOT_RUN**；USB准入BLOCKED，不能复用旧观察冒充当前核验 |
| 当前PID/UID/startTicks、前台及重要操作 | **UNKNOWN / NOT_RUN**；旧PID32313仅为历史，不认为当前仍有效 |
| 默认generation、Room/WAL/SHM、WorkManager路径和文件元数据 | **NOT_RUN**；未读取或修改，未打开数据库 |
| API36命名空间Job的注册/Pending/Active | **UNKNOWN / NOT_RUN**；无本轮Job查询，不把旧空提取数组当作零任务 |
| 系统强制停止 | **NOT_RUN，0次**；没有创建停止操作claim或发送`am force-stop`，未重试 |
| PROCESS_STOPPED / ALREADY_STOPPED | **均未证明**；不能由设备未准入推断App已退出 |
| FD_CLOSED_BY_PROCESS_EXIT | **未证明**；本轮没有进程或FD取证 |
| AppContainer正常关闭 | **NOT_PROVEN**；未执行关闭，也不把force-stop设计成优雅关闭 |
| WorkManager任务业务收口 | **UNKNOWN**；未取消、重排、删除或恢复任务 |
| 默认账本完整性 | **NOT_VERIFIED**；未读取业务内容 |
| 三包前后SHA/UID/版本及手机设置 | **NOT_RUN**；本轮安装、启动、停止、设置写入及清理均0，不宣称重新核验一致 |

## 保护与停止

本轮仅1次主机ADB发现，手机shell、force-stop、安装、Instrumentation、App启动、数据库内容读取、任务取消、文件删除、系统设置写入、模拟器或其他设备操作均 **0**。没有运行JVM/Android测试、构建APK或修改产品/测试源码。原M10、独立M22 App/Test及全部历史现场未被本轮修改；未重新检查的现有内容不改判为完整或安全关闭。

[上次进程与10个数据库FD观察](M22-Physical-Final-Supplement.md)保留原时点；既有T06三次FAIL、137 JVM/36 Android PASS、三项未完成补验、M10/M13-T06/M20/Q03/Q04/G1/G2/G3及其他所有历史NOTES/UNKNOWN/NOT_RUN保持。设备重新取得明确USB身份前不能执行本次停止；提交推送后停止，不因后续连接恢复自动续跑，不更新Test、不继续T06、不冻结M22、不合并main或进入M23。

## 2026-10-24 手机重新连接后的限定续接

**READY_FOR_REVIEW — M22 PHYSICAL PROCESS CLOSURE**。用户明确告知手机已重新连接后，从`ed35deceb82cd1cc04abb50b258e210f8d3e4b61`接续同一任务。旧首轮、原始失败和强停0次记录均保留；没有重置或消耗强停额度。起点工作区干净，首次远端查询发生TLS握手错误，最终Git结果另核对。

### 实际结果

| 检查 | 本次续接观察 |
| --- | --- |
| 手机 | 同一锁定USB、`23127PN0CC` / API36 / arm64-v8a；Boot仍为`a9edfd47-8cb5-40a5-92eb-c41925ab00ea`。仅目标序列命令，无其他设备操作 |
| App/Test进程 | **ALREADY_STOPPED**；完整数字UID进程表两次均无UID10420/10427及目标包进程，独立`pidof`两包均无匹配；目标不在前台、无目标ServiceRecord。未发送停止命令 |
| 退出原因 | **UNKNOWN**；M22包`User 0 stopped=true`在本次任何修改前已存在，不能归因本轮，也不宣称自然或优雅退出 |
| 原PID及FD | 同Boot下原UID进程已不在；旧PID32313/FD目录有最初ABSENT、中间shell观察PRESENT及最终ABSENT。保留中间差异，复用/可见性/采样时差原因UNKNOWN，不把同一数字PID当同一进程。最终公开固定标记的完整长度/SHA精确核对；登记原M22 FD **FD_CLOSED_BY_PROCESS_EXIT**，只限原进程所持副本，不扩大为全部消费者或锁证明 |
| JobScheduler | 两轮过滤后注册目标块0，最后完整安全section明确Registered `None.`、Pending `None`、Active为空；全局`Registered 261 jobs`不是目标数。**当前目标注册/Pending/Active均0的观察**。旧历史未来任务不改判为正在执行；不把当前OS记录消失当WorkSpec已取消 |
| AppContainer正常关闭 | **NOT_PROVEN**；没有调用`close()`，没有借安装或Instrumentation结束进程 |
| WorkManager业务收口 / 默认账本 | 分别 **UNKNOWN / NOT_VERIFIED**；未打开业务或WorkManager数据库，不验证内容，不取消或重排任务 |
| 文件与设置 | 默认generation、数据库/WAL/SHM、WorkManager等被检查路径的UID/inode/大小/时间元数据在本次前后完全一致，文件保留。font1.1、density480/1200×2670、airplane0/Wi-Fi1/mobile_data0、主题值0及Boot前后相同；无设置写入或恢复操作 |

三包前后包名/UID/版本/User0安装字段、唯一APK路径及设备SHA均一致：原M10 UID10376/code8，M22 App UID10420/code17 SHA **`b749b771e4f6f9cebcb7355c95834b7ae2eb4ba4f7528536a92bf9e09eefb2d3`**，当前Test UID10427仍为 **`c3791305aed28899f8df98dc7c89f27c986c022b84a0e528c68c59ebf9f2037f`**。原M10 SHA **`939b3b20d3c6cd02c800f320ccddc300736b75084b609c404f38b096270f7094`**未变；未启动或读取其私有目录/账本。手机硬件序列仅存本机忽略的控制记录，公开只保存此前SHA占位符。

### 证据与限制

- [续接总结果及36次只读命令计数](m22-process-closure-resume-20261024-035729/result.json)：1次主机发现＋35次定向只读手机查询；force-stop、安装、App启动、Instrumentation、任务取消、设置写入、清理、数据库内容读取均0。没有创建强停claim，也没有强停后重试。
- [独立完整UID进程观察](m22-process-closure-resume-20261024-035729/commands/final-verified-target-processes/command.json)、[两包pidof](m22-process-closure-resume-20261024-035729/commands/independent-exact-pidof/stdout)、[最初原PID/FD观察](m22-process-closure-resume-20261024-035729/commands/old-pid-and-fd-existence/stdout)、[中间PRESENT原文](m22-process-closure-resume-20261024-035729/commands/end-independent-absence/stdout)、[最终标记长度/SHA补充核验](m22-process-closure-resume-20261024-035729/old-pid-format-supplement.json)。最终标记是与原回执完整SHA匹配的公开字面量补充解释，**不是伪造原始stdout文件**；原解析失败保留。
- [最终API36完整目标任务section](m22-process-closure-resume-20261024-035729/commands/final-verified-jobs/command.json)、[前后默认文件元数据](m22-process-closure-resume-20261024-035729/commands/end-store-metadata/stdout)、[最终三包及设置汇总](m22-process-closure-resume-20261024-035729/result.json)。完整进程/系统任务输出仅内存处理，保存完整长度/SHA及必要目标内容，不归档其他App身份或日志。
- 保留三次本地监督器退出1：无匹配UID的`ps`仅表头/exit1（随后独立完整进程表及pidof核验）；中间数字PID路径PRESENT触发停止后处理断言；最终固定标记CRLF使LF首尾断言失败（原exit0、62字节/SHA全部保留，并离线精确核对）。这些不是Android业务测试结果。未修改旧回执、未将差异隐藏为连续不存在，也未因解析问题发送停止命令。

本任务至此停止，仅完成“已停止”及状态取证。**未更新新12e…ae26 Test，未运行T06/T03/IME**；未验证任何业务完整性或优雅关闭，全部原FAIL/UNKNOWN/NOT_RUN/NOTES及其他受保护现场保持。进程收口观察不授权下一步测试，不冻结M22、不合并main、不进入M23。

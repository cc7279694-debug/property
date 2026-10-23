# M22 真机Test更新与最终定向补验

**READY_FOR_REVIEW — M22 PHYSICAL FINAL SUPPLEMENT**（仅交付审查）。**实际准入BLOCKED：运行中的新M22进程仍持有默认generation及WorkManager数据库FD。Test更新0次，三个方法全部NOT_RUN。M22继续BLOCKED / CHANGES REQUIRED。**

来源`adb9fa791a27cba88fc474e243f337cedc22c081`，分支`codex/m22-experience-accessibility`；2026-10-23。起点本地HEAD匹配、工作区干净；提交前核对远端。本轮仅此前小米`23127PN0CC` / API36 / arm64-v8a，Boot ID `a9edfd47-8cb5-40a5-92eb-c41925ab00ea`。完整USB serial不提交，公开证据仅SHA-256 `e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92`。只读Windows USB PnP精确匹配原序列后缀、状态OK，并结合唯一非模拟器ADB目标和手机/包身份确认；未对任何模拟器或其他设备发送设备命令。[USB发现](m22-physical-final-20261023-151835/usb-discovery-followup.json)、[Windows USB实际证明](m22-physical-final-20261023-151835/windows-usb-proof.json)、[手机资源/原设置](m22-physical-final-20261023-151835/commands/before-device-settings/stdout)。

## 三包实际身份与更新门禁

| 实际安装包 | 前后身份及设备APK SHA-256 |
| --- | --- |
| 原M10 `com.cooper.lithic.debug` | UID10376、code8/`0.1.0-m10-debug`；SHA **`939b3b20d3c6cd02c800f320ccddc300736b75084b609c404f38b096270f7094`**，前后相同。仅PackageManager安装元数据和获准的APK校验，不启动、更新或读取私人账本 |
| M22 App `com.cooper.lithic.m22verify.debug` | UID10420、code17/`0.1.0-m22-debug`；SHA **`b749b771e4f6f9cebcb7355c95834b7ae2eb4ba4f7528536a92bf9e09eefb2d3`**，前后相同 |
| 当前Test `com.cooper.lithic.m22verify.debug.test` | UID10427；仍为历史SHA **`c3791305aed28899f8df98dc7c89f27c986c022b84a0e528c68c59ebf9f2037f`**，前后相同，未更新 |

三包PackageManager身份字段（忽略非身份的Package头诊断token）前后一致。[前后SHA/UID及设置总结果](m22-physical-final-20261023-151835/result.json)、[原M10结束SHA](m22-physical-final-20261023-151835/commands/end-oldM10-sha/stdout)、[App结束SHA](m22-physical-final-20261023-151835/commands/end-app-sha/stdout)、[Test结束SHA](m22-physical-final-20261023-151835/commands/end-test-sha/stdout)。未复制手机APK、业务数据库、图片或个人文件。

桌面新候选实物SHA **`12e17f01be03677501a753b1992badffdfaece0f4db1742ba27b498afc1dae26`** /3,403,324 bytes已核对；无重建。[本轮锁定输入](m22-physical-final-20261023-151835/locked-input.json)。因第一门禁阻断，第二门禁的新Test验签/Manifest/Runner复核和`install -r`均**NOT_RUN**；此前[桌面构建与静态核验](M22-T06-Failure-Preservation.md)保持历史，不冒充本轮重新执行或手机更新结果。没有创建安装claim或发送更新命令。

## 真实阻塞：默认资源仍被持有

明确UID选择的`ps`观察到 **UID10420 / PID32313 / PPID1716**，进程名为新M22 App。以同UID只读`/proc`进一步取得 **startTicks84068317**、同一Boot及状态；FD链接元数据显示该进程当时持有：

- 默认目录`generations/919ab3ee-2a7e-4988-8d65-5641a7ddcf2b/`的`data.db`、`data.db-wal`、`data.db-shm`。
- `no_backup/androidx.work.workdb`及其wal/shm。

共观察到10个匹配上述路径的FD链接。这是目标进程/具体文件资源仍被持有的直接元数据证据，**不是只凭PID存在或目录存在推测**；不等同于证明某个Worker正在执行，也不判断账本内容。未打开SQLite、读取数据库/设置/ready/active-generation内容或代指针值；目录UUID不冒充已验证的当前指针目标。[目标UID进程原文](m22-physical-final-20261023-151835/commands/target-uid-processes/stdout)、[进程身份及FD链接原文](m22-physical-final-20261023-151835/commands/blocked-target-process-metadata/stdout)、[固定路径/文件元数据](m22-physical-final-20261023-151835/commands/blocked-default-store-metadata/stdout)、[阻塞分析](m22-physical-final-20261023-151835/blocking-analysis.json)。

JobScheduler输出中另观察到至少一条目标`SystemJobService`注册头，命名空间ID为`androidx.work.systemjobscheduler:u0a420/0`，以及未来约19小时26分的时间约束。**不能把注册等同于正在执行**；完整JOB块提取未覆盖本机命名空间头格式，原提取块数组为空不表示零Job。目标Timer的“0 running bg jobs”也不单独替代完整运行上下文证明；pending/active完整状态、WorkSpec身份和任务关闭仍**UNKNOWN**。只保存实际目标行与整体输出长度/SHA，无其他App日志或Job内容。[目标Job观察](m22-physical-final-20261023-151835/blocked-target-jobs.json)。

按用户第一门禁停止Test更新和全部测试，只做必要只读结束核查。未借安装结束App，未强停、取消任务、解绑、删除文件或“重开再关闭”默认Owner。当前不能证明默认Owner已经安全关闭；这不是新的金额算法或T06业务断言失败。后续须独立审查该默认Owner/队列的精确安全处置，不得直接继续安装或Instrumentation。

## 三方法结果与保护

| 方法 | 本轮真实结果 |
| --- | --- |
| T06完整离线旅程 | **NOT_RUN**；第一门禁阻断，未发Instrumentation。原三次FAIL和新失败保留实现`PENDING_ANDROID_VERIFICATION`保持；失败账本保留/成功清理的Android实证亦NOT_RUN |
| T03正式20年规模 | **NOT_RUN**；T06 PASS及关闭前置未满足。1000/1000对象、20000/10000事实、固定日期/seed和120秒预算不改；Room计数、性能、响应及内存结果NOT_CAPTURED |
| 真实系统200%字体/IME | **NOT_RUN**；未启动测试Activity，实际Activity screenWidthDp未取得，不由全局宽度换算判断通过或不通过；未调整density/字体或运行替代方法 |

没有请求用户切断网络或调整字体；网络/字体写入0。前后实测均font1.1、density480/1200×2670（无override）、airplane0/Wi-Fi1/mobile_data0、主题设置0及同一Boot，故无本轮设置恢复操作。没有截图或运行日志，不能凭未运行捏造JUnit结果。[结束设置](m22-physical-final-20261023-151835/commands/end-device-settings/stdout)。Owner未关闭/未作关闭证明，现存FD和未知任务现场保留；不把只读检查结束当作资源释放。数据内容未读取，完整业务事实和私人字节完整性未验证。

## 取证限制与操作次数

两次监督器格式失败保留：最初错误要求`devices -l`含`usb:`字段，后续Windows USB PnP独立证明目标；首次全局ps四字段校验失败，后续明确UID选择取得目标进程，不忽略目标身份检查。两次失败未在异常前写入完整原始输出长度/SHA，均保持 **NOT_CAPTURED**，不以新观察回填。`get-devpath`未取得USB证明的旧记录亦保留。Job块提取不完整保持UNKNOWN，不为通过门禁临场放宽。[首次发现失败](m22-physical-final-20261023-151835/first-discovery-failure.json)、[首次ps失败](m22-physical-final-20261023-151835/first-process-parser-failure.json)、[原USB路径观察](m22-physical-final-20261023-151835/usb-transport-proof.json)。

本轮已知26次只读ADB命令（24份完整回执，另2次上述输出元数据未捕获）、1次只读Windows USB核对；全部ADB均显式指定原目标序列，设备列表仅主机发现、原文只在内存处理。App/Test安装、App启动、Instrumentation、设置写入、任务取消、数据库内容读取、清理、模拟器或其他设备操作均 **0**。没有重跑137 JVM/36 Android、构建APK、下载工具或修改产品/测试源码。历史两次USER_RESTRICTED、安装成功记录、全部FAIL/UNKNOWN/NOT_RUN和M10/M13-T06/M20/Q03/Q04/G1/G2/G3状态保持。提交推送后停止，不冻结M22、不合并main、不进入M23。

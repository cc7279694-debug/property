# M22 真机默认进程安全收口

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

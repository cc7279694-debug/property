# M10-R3-F0 — 实际诊断时间线

## 当前补充时间线 — 2026-10-17

**READY_FOR_REVIEW / D INTERMITTENT_RECOVERED**；以下UTC取自设备epoch或宿主启动登记，北京时间加8小时，未写时钟。

| UTC / 北京时间 | 事件 | 实证 |
|---|---|---|
| 01:51:36.914 / 09:51:36.914 | 按用户新批准启动原AVD | 既有ini与数据，launcher PID1044；不是boot完成声明 |
| 01:51:53～01:52:20 | 冷启动get-state多次offline | 10次退出1，保留；不发App启动 |
| 01:52:24以后 | ADB出现device | boot属性当时仍空，不提前授PASS |
| 01:55:14.730 / 09:55:14.730 | boot_completed实读1 | 后续身份/APK/保护预检通过，无旧Host/worker |
| 01:56:04.552 / 09:56:04.552 | 唯一Host START | bare组件，无extras |
| 01:56:04.624 / .632 | Zygote fork / AM Start proc | PID3928，UID10218 |
| 01:56:04.830 | am_proc_bound | Start proc至bound约198ms |
| 01:56:04.958（日志.959） | APPLICATION_ROLE与私有trace | HOST、syntheticOwner=true，不是正式AppContainer门禁 |
| 01:56:05.501 / .535 | onCreate / onResume事件 | 目标PID确实到达Activity生命周期 |
| 01:56:06.078 | Displayed | TotalTime1572ms，am命令wall1750ms，退出0 |
| 01:58:07.211 | 唯一Outsider START | 无实验extras，UID10219 |
| 01:58:07.287 / .297 / .388 | fork / Start proc / bound | PID4062；已到达进程绑定 |
| 01:58:07.860 / .922 | 系统拒绝 / 应用记录bindingDenied | 私有Worker跨UID绑定被拒；不授R3案例PASS |
| 01:58:08.870 | ActivityThread ServiceConnectionLeaked | 独立辅助代码NOTE，堆栈指向OutsiderActivity.java:14；未修复 |
| 01:58:37.414 | 结束进程采样 | Host3928/Outsider4062仍在，未见worker；未终止进程 |

原件：[Host核心日志](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/host-core-system.log)、[Host trace](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/host-research-traces.log)、[Outsider日志与完整关联堆栈](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/outsider-scoped-system.log)、[对照结果](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/outsider-result-read.log)、[完整命令](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/adb-commands.jsonl)。Host原始启动登记的49行是初筛计数，发布版本40行及9行外移说明见[筛选登记](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/host-core-filter.json)，不是删改原R3日志。

### 当前分层结果

| 项目 | F0实际结果/边界 |
|---|---|
| 原AVD/API36/三包实际SHA与UID/私有Manifest | PASS（当前设备核对） |
| 一次裸Host绑定/Application/Activity显示 | PASS（仅F0启动） |
| 一次Outsider绑定/私有Service拒绝 | 拒绝事实PASS；辅助Activity有ServiceConnectionLeaked NOTE |
| 资源与系统观察 | MEASURED；load均值偏高但短时近空闲，PSI拒绝，ART全状态UNKNOWN |
| 正式APK、私有元数据、设置保护 | PASS（前后比较），私人字节一致性NOT_RUN |
| Host/worker双进程会话/FD/pin/终止/恢复 | NOT_RUN；无worker实例，不扩大为架构通过 |
| API26/native fatal/OOM/LMK/原29/产品测试/构建 | NOT_RUN |

分类D只表示这次能够启动；AVD从离线被正常启动、Intent从历史带extras变为裸启动，两个条件都变了，不能单独归因。历史两次attach UNKNOWN、R3 0PASS/1UNKNOWN/28NOT_RUN保持。

---

## 首次交付历史时间线（ef25b4d）

日期：2026-10-17。**BLOCKED**。只有Windows侧ADB可用性证据；没有新的Android进程、attach或应用初始化时间线。

配套：[诊断](M10_JPEG_R3_F0_DIAGNOSIS.md) · [评估](M10_JPEG_R3_F0_ROOT_CAUSE_ASSESSMENT.md)。

## 本轮实际事件

原命令时间为UTC，表内北京时间按Asia/Shanghai换算；未修改宿主或设备时钟。

| 北京时间 | 事件 | 实际结果 |
|---|---|---|
| 09:35:58.890 | 固定serial get-state完成 | 退出1、78ms；目标not found；无Host启动请求 |
| 随后只读核查，09:36:47.766前完成 | devices -l | 退出0，空设备列表；无序列号可替代原目标 |
| 同一核查窗口 | Windows三个指定名称进程查询 | 退出0，仅adb PID5568，未观察到emulator/qemu指定名称 |
| 同一核查窗口 | adb version | 退出0，既有工具，无安装/升级 |
| 09:36:47.766 | 可用性结果登记 | ADB_TARGET_UNAVAILABLE；停止，不进入Host/Outsider |

逐条更精确命令完成时间、argv、耗时、返回码以[原始JSONL](m10-jpeg-r3-f0/evidence/20261017T013551.734384Z/adb-commands.jsonl)为准；[汇总](m10-jpeg-r3-f0/evidence/20261017T013551.734384Z/availability.json)。没有把shell返回值0当成Android应用启动成功。

## F0实验结果矩阵

| 检查 | 状态 | 边界 |
|---|---|---|
| Windows ADB可执行/版本读取 | PASS（命令） | 不代表Android设备在线 |
| 授权serial在线 | BLOCKED | not found；设备列表为空 |
| AVD/API36/boot与资源健康 | NOT_RUN | 无授权目标，不能查询当前值 |
| 研究/正式APK SHA、UID、实际Manifest | NOT_RUN | 只保留预期与历史值，不重新授PASS |
| 无参数Host启动、Zygote fork/attach | NOT_RUN | 0次启动请求，无新PID |
| APPLICATION_ROLE、研究trace、崩溃/超时 | NOT_RUN | 没有目标日志，不以日志缺失定位Java执行边界 |
| Outsider启动与绑定拒绝 | NOT_RUN | 0次启动；当前私有Service/不同UID门槛未验证 |
| 正式App/设置前后快照、私有字节一致性 | NOT_RUN | 未执行设备写操作，仍不能冒充测得一致 |
| 原29生命周期/故障注入 | NOT_RUN（F0） | 不改变R3原矩阵，也不计入新增测试 |

没有logcat -c、完整日志导出、设备清理、进程kill/force-stop、超时阈值修改、安装或打包。没有通过拉长等待时间制造成功。脚本入口被设备门槛挡住，不存在本轮人为Host/worker死亡实验。

## 继承历史，不与本轮混用

R3 2026-10-16原Android时刻：PID27893在15:01:40.522 Start proc、15:01:52.134 failed attach、15:01:52.399 start-timeout kill；PID28245在15:06:08.779 Start proc、15:06:18.791 failed attach、15:06:18.804 kill。其原时区未被重新确定，不能将这些值直接当本轮北京时间。

原件：[R3生命周期](M10_JPEG_R3_LIFECYCLE_RESULTS.md)、[原系统日志](m10-jpeg-r3/evidence/device/20261016T150602.612810Z/research-package-only-system-diagnostic.log)。两次UNKNOWN及0/1/28矩阵保持原效力；本轮设备缺失不解释其历史根因。

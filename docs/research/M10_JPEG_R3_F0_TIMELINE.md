# M10-R3-F0 — 实际诊断时间线

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

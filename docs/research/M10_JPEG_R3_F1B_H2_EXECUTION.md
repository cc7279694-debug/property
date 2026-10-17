# M10-R3-F1B-H2 执行报告

2026-10-17。**BLOCKED — M10-R3-F1B-H2**，停止等待ChatGPT独立审查。唯一normal已发送，不得再次运行、清理或恢复。

## 契约与来源

用户转交H1独立PASS WITH NOTES，仅限研究APK更新与单次API36 Outsider。H2只授权一次normal及必要只读保护。来源`0dc49621f44f31eed0cdfc4d0dbb56d8756b667a`本地/远端一致，H1工作区干净；经Codex管理工具创建独立worktree，再建立`research/m10-jpeg-strict-r3-f1b-h2`。目录`E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1b-h2/砺石新APP`。

实际读取AGENTS、PROJECT、CURRENT_STATE、相关决策/架构与路线、H1四报告/证据、F1-A源码/审阅/构建输入、U2门禁及R3原设计和停止限制。当前授权覆盖旧AGENTS中的阶段性分支文字，不改变冻结架构。两位Codex协作代理只读审查源码/证据，未操作设备，不代替ChatGPT独立验收。

## 执行前与唯一启动

原AVD在线，原emulator PID16988，ini/config SHA与历史锁定值一致；启动/重启/安装均0。设备再次确认为`Lithic_M01_API_36 / emulator-5596 / API36`、boot=1。

独占新建证据目录，原G1/U1 `verify.py preflight`真实执行一次。7份脚本SHA相同，两轮均INACTIVE_RECORD_CANDIDATE（记录9879f0e）、无研究进程、stage ABSENT，得到PREFLIGHT_ELIGIBLE。H1曾存在的Outsider缓存进程本轮未观察到，没有主动清理。27份F1-A构建输入再次匹配；无新构建。正式/Probe/Outsider实际SHA与锁定值相同，UID分别10216/10218/10219。Manifest和证书由当前已装字节与H1已审计字节的相同SHA关联，不伪称本轮重跑证书工具。

H2监督器只支持单一normal。发送前另做一次即时只读身份/进程/Service/stage检查，不是第二次完整preflight。持久独占并fsync[发送标记](m10-jpeg-r3-f1b/evidence/h2-20261017-01/normal-launch-issued.json)后，只发送一次：

```text
adb -s emulator-5596 shell am start -W -n com.cooper.lithic.m10r3.probe/.ProbeActivity --es command run --es case normal --es runId h2_e22caecf8afc4a72957afc061c68d5a3
```

参数依据实际ProbeActivity/ProbeRunner。未使用`-S`、fault、run_device.py run或其他场景。完整返回见[原始日志](m10-jpeg-r3-f1b/evidence/h2-20261017-01/adb-commands.jsonl)。退出0、外部wall17.187秒，但stdout为**Status: timeout / LaunchState: UNKNOWN (-1) / WaitTime: 13596**。不是subprocess超时，也不是启动PASS。立即停止新实验，未重发。

## 实际失败及只读结束

Intent确实进入Host6423/UID10218，唯一CASE_BEGIN为本run/normal。[真实run报告](m10-jpeg-r3-f1b/evidence/h2-20261017-01/normal-failure-result.json)为FAIL、`java.lang.AssertionError: bind timeout`，Host trace同样CASE_FAIL_STOP。系统随后Displayed耗时24.415秒，并明确记录Input-dispatch ANR，根因UNKNOWN。

迟到Worker6610/UID10218在Host失败后启动，未获得reserve/hello/FD。SERVICE_CREATED后30.005秒因UNRESERVED_DEADLINE自行退出，匹配Binder、ActivityManager、Zygote及am_proc_died。不是正常AUTHENTICATED_RETIRE，不证明consumer lease释放。没有ADB kill、force-stop或故障注入。

首轮只读结束保留process BLOCKED（unknown process under research identity）及非终结stage。完整ps按隐私策略丢弃，仅留原字节/SHA，不能补造当时未知名称。后来的Host-only采样不能反向解决它。补充读取另出现两处收集UNKNOWN：exec-out shell引号不兼容；trace在旧快照后因迟到死亡回调增长。原错误保留。报告与Host trace从已保存命令响应UTF-8无损重建，对照同次原始字节数/SHA完全匹配，见[恢复索引](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-recovered-raw-index.json)，不是重跑normal。

只继续有界只读结束日志与最终保护。最终Worker未见、服务NO_TARGET_RECORD；current仍指向本attempt，DELIVERY_UNKNOWN/pin=true、source8192保留，无receipt/revoked/cleanup终结证明。全部保守保留，不清理/恢复。正式APK/UID、188条元数据聚合及设置相同，私人字节NOT_RUN。

## 修改及验证

新增H2限定监督/只读取证/离线核查脚本、原始证据、四报告和checkpoint，更新唯一CURRENT_STATE。研究Java、WorkerService、StageStore、SessionGate、Manifest、Gradle、APK、正式产品、JPEG断言、旧证据和验收登记不改。

153条ADB＝主日志152条（与完整性记录逐项匹配）＋初始get-state1条；1 normal、0安装、0 Outsider、0重试。所有ADB退出0、subprocess超时0，仍保留am timeout、normal FAIL、stage BLOCKED及收集UNKNOWN。原G2结束JSON固定0/0仅该只读阶段，不是H2总数。[离线核查](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-offline-audit.json)验证命令身份/数量、7脚本和27构建输入，不能计作Android场景PASS。

未重跑Gradle、JVM、Lint、构建或JPEG，其历史结果原样保留。只提交推送H2分支，最终SHA/远端一致性以Git回执为准。提交后停止，不自行批准下一项诊断或恢复。

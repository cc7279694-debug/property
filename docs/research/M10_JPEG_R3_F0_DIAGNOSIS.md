# M10-R3-F0 — Host Attach专项诊断

2026-10-17（Asia/Shanghai）。**BLOCKED — M10-R3-F0 Attach Diagnosis**。本轮在设备可用性预检停止，Host与Outsider均未启动；历史attach根因仍UNKNOWN。

入口：[时间线](M10_JPEG_R3_F0_TIMELINE.md) · [根因评估](M10_JPEG_R3_F0_ROOT_CAUSE_ASSESSMENT.md) · [checkpoint](../checkpoints/2026-10-17-M10-JPEG-R3-F0.md)。

## 契约与来源

- Goal：以最小非破坏性诊断区分研究Host启动失败原因；不是修复或重跑R3。
- Scope：新研究分支上的诊断脚本、经过范围过滤的证据、三份报告、当前状态和新checkpoint。仅既有`Lithic_M01_API_36 / emulator-5596`允许被诊断。
- Out of scope：正式App/研究APK源码、Manifest、Gradle、JNI、数据库、工具升级、安装/打包、原29场景、清数据、重启/重建AVD、系统设置写入、任意PID终止、M11。
- Acceptance：设备身份与安装SHA/UID实际核对后，至多一次无参数Host启动；条件满足才至多一次Outsider对照；缺设备则明确BLOCKED，不能以历史结果替代当前实测。
- Source：本地与远端R3 `17b2cb40d8f3b95136f3d43e5c91cd7aee7b8e5d`，工作区干净；新独立`research/m10-jpeg-strict-r3-f0`、worktree `E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f0/砺石新APP`。不是从main开始。
- 本轮用户附件`cfb8c989-2805-451f-bf97-0f456f5ebf8a/已粘贴的文本.txt`实际读取，SHA-256 `ff0f90631f8cfd78747f3be8f01452f3228990f1c6bc56eb3c3601e37ce953a4`。用户转交R3独立结论：研究工程/有限测试PASS WITH NOTES；生命周期BLOCKED；B1生产NO-GO。未自授新验收。

已读取AGENTS、PROJECT、CURRENT_STATE、R3四份报告/checkpoint、ProbeApplication/Activity/Support、Outsider、研究Manifest及最终APK历史dump、run_device.py、两份run-summary及两次系统超时日志。相关来源保持原字节；[本轮执行输入SHA](m10-jpeg-r3-f0/evidence/20261017T013551.734384Z/source-inputs.json)。

## 实际执行与停止位置

1. 新[诊断脚本](m10-jpeg-r3-f0/diagnose.py)通过Python AST语法解析；预检入口实际执行。
2. 固定序列号`adb -s emulator-5596 get-state`退出1：`error: device 'emulator-5596' not found`。约78ms返回，不是等待超时。
3. 只读`adb devices -l`退出0，仅有标题、没有设备行。
4. 只读Windows进程名称/ID查询，匹配emulator、qemu-system-x86_64、adb；结果仅`adb` PID5568，无前两种名称。未采集命令行、其他应用或私有文件。
5. 既有ADB版本读取退出0，未安装或升级工具。停止设备实验，无自动重试/启动AVD/清理进程。

原始命令、退出码、耗时与输出：[命令记录](m10-jpeg-r3-f0/evidence/20261017T013551.734384Z/adb-commands.jsonl)；[预检阻塞](m10-jpeg-r3-f0/evidence/20261017T013551.734384Z/preflight-blocked.json)；[可用性结果](m10-jpeg-r3-f0/evidence/20261017T013551.734384Z/availability.json)。两次只读设备清单观察均为空，不是两次Host实验。

本轮没有取得新的Android日志：目标不可访问，因此没有采集不受限logcat，也没有清日志。脚本为原授权诊断准备的Host、Outsider和结束快照入口均**NOT_RUN**，没有编译/安装研究APK；其代码存在不表示路径已验证，不授权下一会话直接执行。

## APK、身份与环境核查

以下只列**预期**，不标当前PASS。UID10218/10219/10216为R3历史实测，不是F0重新核实。

| 包 | 预期SHA-256 | F0实际SHA/UID/Manifest |
|---|---|---|
| probe | `230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0` | NOT_RUN：目标缺失 |
| outsider | `214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358` | NOT_RUN：目标缺失 |
| 正式砺石debug | `4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46` | NOT_RUN：目标缺失 |

API36/AVD身份、boot_completed、Android CPU/内存/空间/pressure、Zygote/ART/ActivityManager当前状态、研究PID、设备设置及正式数据前后元数据快照均**NOT_RUN**。不能把Windows没有匹配名称进程等同于这些检查通过。没有读取、复制或上传私人账本/封面；没有正式App或设备写操作，但这不构成本轮私有数据字节一致性或保护快照PASS。

## 源码复核与安全NOTE

无command启动的静态路径：`ProbeActivity.java:9–18`仍先执行Application及界面创建，`command==null`后返回，不构造/dispatch ProbeRunner。`ProbeApplication.java:8–18`有进程名、合成owner及APPLICATION_ROLE；`ProbeSupport.java:21–33`有日志与trace fsync。缺少日志不能证明某行代码尚未运行；本轮没有新的Host执行证据。

Outsider在`OutsiderActivity.java:14`使用BIND_AUTO_CREATE，不能作为纯启动对照。历史Manifest的worker exported=false不能替代本轮实际安装与不同UID核对；这些前置条件未实核，故Outsider **NOT_RUN**。

**保留独立静态NOTE，未修复/未实跑：**`ProbeRunner.runCase():70–72`仅对prior调用`canAcquireGuard`，随后创建新Session并写current；没有强制完成旧attempt恢复、持久撤销和清理。`StageStore.canAcquireGuard():64–66`的短暂lease取得不等于`cleanup():68–85`。后续若获授权恢复生命周期工作，应优先处理该NOTE，不能因Host能启动就跑完29场景。

## 当前状态与交付边界

F0诊断 **BLOCKED / UNRESOLVED**；R3历史29场景0 PASS/1 UNKNOWN/28 NOT_RUN、17 JVM PASS、Lint0错误4警告不改写。无本轮JUnit/Lint/Gradle/Android构建或产品测试PASS。原两次attach UNKNOWN、R1 120秒UNKNOWN保留；B1生产NO-GO；M10 PENDING/原3 JPEG FAIL；UI-C1冻结PASS WITH NOTES；M11 NOT_AUTHORIZED。

只提交推送F0研究分支，最终SHA/远端一致性由Git回执提供；受保护分支、本地工作区及旧报告不改。不修复、不合并、不发布APK。环境操作和重试均等待新授权及独立审查。

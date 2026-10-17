# M10-R3-F0 — Host Attach专项诊断

## 当前补充交付 — 原AVD获准上线后

2026-10-17。**READY_FOR_REVIEW — M10-R3-F0 Attach Diagnosis**。分类 **D INTERMITTENT_RECOVERED / EVIDENCED**：同一锁定Host APK本次无参数启动已到达系统绑定、Application trace及Activity显示。**历史两次attach根因仍UNKNOWN**，不关闭R3 UNKNOWN或授予生命周期/生产通过。

本轮附件`ac886943-5757-4b5e-b56b-e5ce7ed81aa1`与首轮附件字节相同，SHA仍为`ff0f90631f8cfd78747f3be8f01452f3228990f1c6bc56eb3c3601e37ce953a4`。用户随后明确回复 **“允许仅启动原模拟器”**。从本分支干净且远端一致的首轮交付`ef25b4d403be026511b036890ccfd3961eb651c5`继续；未重建分支/覆盖工作。锁定R3来源17b2cb4不变。

### 授权操作与实际结果

- 仅启动已有ini映射的`E:/Android/avd/Lithic_M01_API_36_fresh.avd`，端口5596，既有镜像/工具；未wipe、重建、清数据、改设置或安装APK。使用既有无窗口/无音频、不加载/保存snapshot参数；没有在已运行AVD上执行重启。启动登记含配置哈希与实际参数：[授权启动](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/authorized-avd-start.json)。冷启动曾offline，后实际boot_completed=1；全部失败返回保留。
- 实核serial/AVD/API36/x86_64、三包APK SHA、三个不同UID与设备读出的研究Manifest。probe10218/outsider10219/正式10216，三哈希均与锁值一致；worker exported=false、`:jpeg_worker`。研究APK只读复制到本机忽略目录供既有aapt2读取，不安装/打包/上传。见[安装身份](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/installed-packages.json)、[实际Manifest](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/installed-probe-manifest.txt)。
- **Host仅1次**裸`am start -W -n`，无command/case/fault/runId。PID3928/UID10218，Zygote fork、am_proc_bound、APPLICATION_ROLE/独立trace、onCreate/Resume和Displayed相互印证；TotalTime1572ms、命令wall1750ms。不是仅凭Status ok判断成功。
- **Outsider仅1次**，PID4062/UID10219；系统绑定完成后明确Permission Denial，拒绝访问UID10218的非导出Worker，研究结果bindingDenied=true。没有调用Worker API；采样与保留日志没有worker。此为F0启动/拒绝观察，**不计入R3原29项**。
- 新发现辅助Activity销毁时报`ServiceConnectionLeaked`，堆栈指向OutsiderActivity.java:14。拒绝结果仍有系统实证，但辅助对照不是“无错误全PASS”；**仅登记NOTE及待批最小修复，不修改APK源码**，详见[根因评估](M10_JPEG_R3_F0_ROOT_CAUSE_ASSESSMENT.md)。

### 资源、日志与数据保护

实际4个在线CPU；启动后loadavg14.31→后续9.01→结束2.09，短时/proc/stat样本忙碌约0.928%、iowait/steal0%。MemAvailable前后1,177,560/1,212,780kB，/data可用8,097,308/8,098,016KiB；Zygote running，debug_app=null、wait_for_debugger=0。**这是当前短时观察，不证明历史无压力或CPU/内存上界**。PSI文件读权限被拒，组合cat前后都退出1，保留部分stdout及stderr，不改为PASS；artd/secondary属性为空，不能推断已验证ART全部健康。见[资源](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/health-before.json)、[补充CPU/运行状态](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/prelaunch-runtime-state.json)、[结束资源](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/health-after.json)。

日志按epoch窗口、实际研究PID和核心系统事件取证，包含Zygote/ART/ActivityManager及目标进程堆栈，不仅按包名筛选。未清日志；不受限logcat只在内存筛选，未落盘提交。Host初筛49行再限制目标PID/核心系统，发布40行；9行非核心actor只移至本机忽略目录，初筛另丢3条混包行；Outsider保留60行，丢2条混包行。保留4000行尾窗口及筛选局限，**不凭缺行宣称无异常**。诊断脚本仅修正技术类名筛选、核心actor范围、去掉旧system PID常量及本地审计目录冲突；研究APK及旧R3源码不改。Host执行时脚本快照与当前版本均已登记SHA，见[22份来源输入](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/source-inputs.json)、[Host筛选登记](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/host-core-filter.json)。

正式APK/UID、188条私有元数据聚合和主题/字体/旋转/wm前后一致，且开始元数据与R3旧聚合一致。**保护比较PASS仅限这些实际项目，私有字节一致性NOT_RUN**；不读/复制私人账本或图片。见[保护比较](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/protection-comparison.json)。结束仍观察到Host/Outsider进程，未观察到worker；未kill/force-stop或关闭AVD。原AVD保持在线，等待后续授权。

实际103条命令为91次退出0、12次退出1（10次冷启动offline、2次PSI拒绝），**不是103个测试案例**。无本轮JUnit/Lint/Gradle/产品APK构建，无29项生命周期/FD/故障实验；M10三FAIL、R1 120秒UNKNOWN、旧stage静态NOTE及所有平台NOT_RUN保留。

完整[补充结果](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/diagnostic-summary.json)、[命令原件](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/adb-commands.jsonl)、[新checkpoint](../checkpoints/2026-10-17-M10-JPEG-R3-F0-resume.md)。提交推送同一F0分支后停止；这是F0研究交付，不自授ACCEPTED，不进入R3生命周期、M11或任何生产修复。

---

## 首次交付历史原文（ef25b4d；非当前停止原因）

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

# M10-R3-F1B-H1 执行报告

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1B-H1**，等待ChatGPT独立验收，不自授ACCEPTED/FROZEN。

## 契约与基线

用户转交U2独立 **PASS WITH NOTES**。本轮仅授权原API36环境、当前时刻完整安全预检、两份锁定研究APK保留数据更新及最多一次Outsider拒绝/连接清理验证；禁止normal、取消、杀进程、故障恢复、JNI或正式App集成。

来源`a3dc3ac9635d5572317d38b94519d70c12052955`，本地/远端U2一致、原工作区干净；独立worktree `E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1b-h1/砺石新APP`，分支`research/m10-jpeg-strict-r3-f1b-h1`。读取AGENTS、PROJECT、CURRENT_STATE、相关决策/架构、U2三报告及证据、F1-A三报告/构建输入、G1/U1规则、实际研究源码/Manifest与F0原泄漏完整堆栈。旧来源、脚本、APK源码、规划及验收原件不改。

## 环境及新的预检

原`Lithic_M01_API_36 / emulator-5596`已在线，既有emulator PID16988/QEMU20516；ini/config SHA与F0锁定值一致，启动0次，不重启、不snapshot、不wipe。API36/boot1在本轮实际重新确认。

独占新建`docs/research/m10-jpeg-r3-f1b/evidence/h1-20261017-01`，使用原G1/U1 `verify.py preflight` **一次**，新结果为PREFLIGHT_ELIGIBLE；没有复用U2资格。原规则实际读取两份独立完整快照，均NO_TARGET_RECORD、研究进程[]、stage ABSENT，无未知UID/危险stage。正式APK/UID及保护基线实核。7份原执行脚本与锁定SHA一致，外层回执器只观察同次subprocess结果，不改判定。

来源：[本轮契约](m10-jpeg-r3-f1b/evidence/h1-20261017-01/h1-contract.json)、[预检结果](m10-jpeg-r3-f1b/evidence/h1-20261017-01/preflight-gate.json)、[执行回执](m10-jpeg-r3-f1b/evidence/h1-20261017-01/h1-preflight-execution.json)。

## APK及逐包更新

| APK | 原始F1-A产物字节 | 实际SHA-256 | 最终UID |
|---|---:|---|---:|
| Probe | 50205 | `a3eea12dfe05736a69c473fa812d0b52a43c8100bbed2cffd0ba9c8f5f0ea684` | 10218 |
| Outsider | 10473 | `a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879` | 10219 |

使用F1-A原始产物路径，未重建或修改APK。27份最终构建输入与本轮源码逐字节/SHA匹配。现有官方工具实际执行6次host审计：两份Manifest打印、两份新APK证书和两份旧APK证书验证，全部退出0。包名正确、min26/target36、testOnly=false、无权限/无native库；Probe Worker仍`exported=false`、`:jpeg_worker`。新旧研究APK证书SHA-256均为`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。本轮已装旧SHA与已核验证书的旧本地APK相同，故签名连续性有实际字节依据，不只采用旧报告结论。

安全预检及全部身份检查通过后，先Probe、后Outsider分别执行 **`adb -s emulator-5596 install -r <锁定APK>`**，各一次，实际均返回Success。未加卸载、降级、清数据或重试参数。每包之前另做即时只读状态核对；每包更新后立即核对已装SHA/原UID、完整进程/服务/stage以及研究文件元数据，成功后才继续下一包。已装APK与host已打印Manifest的APK SHA完全相同，证明其Manifest/证书同字节；实际系统拒绝又确认Worker非导出，结束ServiceRecord确认worker进程名。

Probe研究files元数据逐包更新前后相同。Outsider已知合成`files/result.json`在其更新前后字节、inode、UID相同；旧结果另保留，不清除。Probe阶段结果中的`outsiderSyntheticResultUnchanged=false`表示该项未在Probe阶段检查，不能解释为发现变化；实际Outsider更新阶段该项为true。测试随后按锁定设计覆盖这个已知结果文件，不声称测试后其字节仍旧。

来源：[host审计及原始工具回执](m10-jpeg-r3-f1b/evidence/h1-20261017-01/h1-host-apk-audit.json)、[Probe更新](m10-jpeg-r3-f1b/evidence/h1-20261017-01/install-probe-result.json)、[Outsider更新](m10-jpeg-r3-f1b/evidence/h1-20261017-01/install-outsider-result.json)。APK仅在本机使用，不提交Git、签名私钥或缓存。

## 唯一Outsider与结束

两包目标身份正确、再次只读检查无活动研究进程/危险stage后，生成外部唯一编号`989aa7a9-ba21-422e-bc5a-77e858ac7d87`，以对应Intent action只启动Outsider一次。研究代码不读取runId，不把该编号伪称App持久nonce；通过实际系统START/activity token、PID5352/UID10219和时间窗关联本轮结果。

系统明确拒绝跨UID访问Probe UID10218的非导出Worker；App真实DENIED/RELEASED，onCreate和onDestroy日志及新持久onDestroy结果一致，系统销毁事件对应同PID/activity。销毁后覆盖10.869993秒的四buffer完整有界窗口未见ServiceConnectionLeaked或越权Worker事件。详细时间、字节/SHA、保留范围见[Android结果](M10_JPEG_R3_F1B_H1_ANDROID_RESULTS.md)及[资源生命周期](M10_JPEG_R3_F1B_H1_RESOURCE_LIFECYCLE.md)。没有重复启动补证。

一次必要G2结束保护核查中，正式APK/UID、188条私有元数据聚合及设备设置前后一致，两个研究包最终身份正确；stage ABSENT，Outsider已知进程5352仍在，系统记录其冻结，Worker进程未见。服务为INACTIVE_RECORD_CANDIDATE，不清理该记录。**Activity销毁不等于进程死亡**。

## 数量及交付

本轮205条ADB命令＝原主日志204条＋初始devices1条；204条含2次install和1次Outsider start，其余读取，全部退出0、0超时。原主日志与两份完整性回执一一匹配。6次host APK审计另计，不是Android测试数量。G2结束JSON内0安装/0启动是其只读阶段的固定字段，**不能作为H1总数**；原JSON不改，实际总数见[计数澄清](m10-jpeg-r3-f1b/evidence/h1-20261017-01/h1-actual-command-counts.json)。

新增H1外层受限监督/取证脚本、四份报告、新证据、新checkpoint及唯一CURRENT_STATE；不改正式产品、研究Java/Manifest/Gradle、G1/U1判定或旧报告。没有新的构建、Lint、JVM测试或正式JPEG测试，不把F1-A/U1历史PASS当本轮重跑。

只提交推送`research/m10-jpeg-strict-r3-f1b-h1`，不合并main；最终SHA/远端一致性以Git交付回执为准。N01～N03和原FAIL/UNKNOWN/NOT_RUN保留；本轮N03相关新证据等待独立审查，不自行CLOSED。提交后停止，不运行normal或下一阶段。

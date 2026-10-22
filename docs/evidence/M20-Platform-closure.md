# M20 — 第二次平台实验定向收口（2026-10-22）

**本次收口：BLOCKED；M20保持READY_FOR_REVIEW / 独立PENDING。** 起点`d2be4cb29a03de100e784527f8e99e86e2cf45d6`，分支`codex/m20-renewal-notifications`。冷启动后的只读检查发现新的正常App进程和默认generation，未满足“不产生新Worker/新账本”的准入条件，因此未编译、安装或执行收口测试，未取消任务、清除通知或重开Owner。原22项最终22 PASS、额外平台原FAIL和全部历史记录不变。

## 实际环境及停止原因

只启动原`Lithic_M20_API36_20261022 / emulator-5704`一次，复用原研究根`E:/CODEX/.codex/research-env/lithic-m20-android-20261022`、镜像和数据；2048MB/2核/headless/no-snapshot。没有新建AVD、下载或修改SDK、操作其他设备。实际launcher PID11528（14:30:42.1246360+08:00）、QEMU PID6900（14:30:42.3156020+08:00），父子关系、真实工具路径、5704/5705端口及全部可写磁盘路径通过；[主机证明](m20-platform-closure/observe/host-proof.json)。API36/x86_64、boot=1、AVD名称通过，新Boot ID为`7168f6f3-1f80-41bc-9c02-f6843bb81511`，没有要求与旧Boot相同；[设备证明](m20-platform-closure/observe/device-proof.json)。

首次静态检查因config原字节SHA变化停止在启动之前（0次启动）；只读比较确认仅格式变化及数据分区`2G→6442450944`，后者与此前已归档SHA完全相同的runtime hardware中的`6g`一致。注册、镜像全文件SHA、RAM/CPU和路径不变；没有改写config或重建数据。[差异](m20-platform-closure/config-readonly-diagnosis.json)、[归属依据](m20-platform-closure/start-authorized/config-provenance.json)、[唯一启动claim](m20-platform-closure/start-authorized/attempt-claim.json)。另一次只读安装身份收集的host JSON组装TypeError保留，复用其原始输出形成独立证明，未据此重启或安装；[错误记录](m20-platform-closure/preflight/collection-error.json)。

**确定事实**：监督器没有发送App Activity或Instrumentation启动命令，但[实际进程](m20-platform-closure/preflight-read/target-processes.stdout.txt)出现`com.cooper.lithic.debug` PID3115 / UID10216 / startTicks10334；[实际合成私有根](m20-platform-closure/preflight-read/synthetic-top.stdout.txt)新增默认`active-generation`和`generations`（设备时间06:32 UTC），与上次结束时默认账本不存在的证明冲突。[进程启动身份](m20-platform-closure/end-protection/new-process-start-identity.stdout.txt)、[安全停止记录](m20-platform-closure/end-protection/stop-proof.json)。

**源码支持的解释**：`LithicApplication.onCreate()`会启动默认AppContainer及ReminderRuntime，后者会安排reconcile；WorkManager开机组件或持久Job具备启动正常Application的路径。具体由哪个系统组件触发、是否已有新Worker执行，本次没有完整事件链，仍为UNKNOWN，不能将推理写成实测Worker结果。发现上述变化后停止依赖操作，没有读取或重开新默认账本，也没有为继续实验清除它。

## 固定对象与逐项结果

唯一允许对象仍为run `eb140516-9807-45be-b01b-781a9b76f6c0`、generation `f6b88b83-a4cc-4022-977e-80abfe975cd2`；周期`03c42993-0b37-444c-abd6-b8f67f4d43db`，候选`dfa1a7b3-836e-4d05-b8c6-1d226f1c215d`；完整通知键`RENEWAL:81babced-da72-4f36-a638-2f2045a74406:2026-10-24`。首次已完成的run没有重新操作。

| 检查 | 本次结果 |
|---|---|
| 原周期任务终止、原候选结束且没有新发布 | UNKNOWN；停止后未初始化WorkManager或读取/修改其数据库，未取消任何UUID |
| OS Job namespace及取消后的结束 | UNKNOWN；全局只读dumpsys仅在内存分析，目标记录提取未得到充分证据。保留原命令长度/SHA，不归档其他应用内容；0个提取块不能解释为0个Job。[限制](m20-platform-closure/end-protection/jobs-retention.json) |
| 指定通知不再活跃 | UNKNOWN；没有调用通知取消或重新发布，不用冷启动推断通知已消失 |
| 原平台结果字节不变 | PASS；2292B，设备回读SHA仍`418c2e6feeb6230566547b43aed056a3a9cde68281d306c26762e87cefb7e9a0`；[实际回读](m20-platform-closure/end-protection/original-result-sha.stdout.txt)、[原路径身份](m20-platform-closure/end-protection/original-root-identities.stdout.txt) |
| 合成项目/订阅/付款/Receipt/设置完整一致 | NOT_RUN；未重开第二次实验账本，不能由原结果或文件仍存在推定通过 |
| 原实验进程实例结束 | PASS（新Boot身份）；旧PID6998/startTicks209090属于此前Boot。仅证明旧进程实例不能继续持有资源，不把原`ownerClosed=false`改为显式关闭PASS |
| 本次重开Owner显式关闭 | NOT_RUN；Owner重开0次，旧Owner及应用级周期收口UNKNOWN仍保留 |
| 唯一新增收口测试 | NOT_RUN；在测试编译、Test更新及Instrumentation之前已停止。未编译草案仅本地忽略目录保留，不纳入源码或方法统计 |

历史第二次结果中的4条WorkSpec（3条SUCCEEDED＋上述ENQUEUED周期）只是此前记录，不是本次查询结果。旧首次3行模型没有复用；没有通过换UUID、删除数据库或取消不明任务绕过门禁。[原第二次结果](m20-android/platform-final-observation/retained-result.json)、[原文件索引](m20-android/platform-final-observation/retained-files.txt)、[首次独立收口](m20-android/platform-final-observation/old-completion.json)均原样保留。

## 安装身份、结束与交付边界

App/Test均未构建或更新，实际安装字节、UID及Debug证书前后相同；[身份证明](m20-platform-closure/preflight-read/installed-proof.json)、[结束原始命令](m20-platform-closure/exit/commands.json)。

- App `com.cooper.lithic.debug`，13,691,164B，UID10216，SHA `776c00167dd856d39791fca4c9331fdfaf313dab096b2835df6e40b3a66fda38`。
- Test `com.cooper.lithic.debug.test`，2,962,518B，UID10217，SHA `815608676350947f9e720af5e8c4647a8505a8f4540c86b07f3517b45776f408`。
- Debug证书SHA `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；未换签名、卸载、清数据或触及私人内容。

全部38条ADB命令均明确指定`-s emulator-5704`；冷启动期间6次get-state返回1原样保留。App/Test安装、测试方法、任务取消、通知取消、Owner重开均为0。只执行一次本轮原实例的正常`emu kill`，退出前核对原启动身份及APK，退出后进程和端口确实消失，[正常退出证明](m20-platform-closure/exit/exit-proof.json)PASS；AVD、合成数据和所有未知现场保留。不将模拟器退出替代应用收口。[统一结果](m20-platform-closure/results.json)。

没有重跑238 JVM、22 Android或通知窗口断言，没有改生产/测试代码、旧结果、Room、备份、算法或UI。M10 PENDING / DEFERRED、三JPEG历史FAIL、M13-T06延期、G1/G2、Q04及全部历史UNKNOWN/NOT_RUN/NOTES不变。**本次唯一阻塞是冷启动后的运行现场已发生自动App/默认代初始化，无法按原四任务、无新消费者前提安全收口；保留UNKNOWN等待独立审查，不自行再次启动、取消、恢复或进入M21。**

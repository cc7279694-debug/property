# M22 定向补验 — SystemUI 健康门禁阻塞

**BLOCKED — SYSTEMUI HEALTH GATE**。来源评审 `1130349e8382aae171aa9259d27bce86dfea4a2a`，分支 `codex/m22-experience-accessibility`。用户另行授权原5712最多冷启动一次；本轮没有安装、启动测试或操作其他设备。M22仍 **BLOCKED / CHANGES REQUIRED**，未冻结。

## 实际准入与停止

- 原AVD注册、配置、7项SDK/镜像文件SHA均与原证据一致；不下载、不升级、不新建。真实userdata/encryption仍在原专属`.avd`内，cache在`runtime-data`，未仅凭`-datadir`推断路径。[静态核验](m22-supplement/static-admission.json)、[本轮运行路径](m22-supplement/runtime-path-proof.json)。
- 2026-10-23 13:49:21（UTC+8）唯一冷启动：launcher **30120**、QEMU **24224**，完整路径/父子关系/创建时间/命令行/5712与5713端口一致。API36/x86_64、2560MiB/2核/headless/no-snapshot；新Boot ID **fca0224f-b56a-43da-b6da-86bb5a1f4936**。[启动claim](m22-supplement/start-claim.json)、[进程与端口](m22-supplement/runtime-host-proof.json)、[boot](m22-supplement/boot-proof.json)。启动早期一次ADB尚未在线exit1保留，不是第二次启动。
- **13:50:40第一轮**：焦点为Launcher，SystemUI PID960/UID10179；全屏仍是Launcher启动画面，未出现ANR对话框，**不据此判持续健康PASS**。[窗口](m22-supplement/commands/health-1-window/stdout)、[全屏原字节](m22-supplement/commands/health-1-frame/stdout.png)。
- **13:51:09第二轮**（窗口命令起点间隔29.019秒）：焦点及完整相关窗口行明确为`Application Not Responding: com.android.systemui`；全屏显示“System UI isn't responding”，有Close app/Wait遮挡。原始截图1080×1920、470362字节，SHA-256 **f856fa0ca267795f6a35a94370a36a0a4853d376315dce05fedd882590f03089**。[窗口](m22-supplement/commands/health-2-window/stdout)、[全屏原字节](m22-supplement/commands/health-2-frame/stdout.png)、[命令完整性](m22-supplement/commands/health-2-frame/command.json)。
- 两轮input dispatcher虽报告enabled/not frozen、队列无等待，也**不能推翻真实ANR遮挡**。健康门禁立即BLOCKED。未点击弹窗、注入触摸、强停SystemUI、重启、清数据或尝试系统修复；根因 **UNKNOWN**，不归因为砺石产品缺陷。[门禁结论](m22-supplement/systemui-health-gate.json)。

## 三项补验的真实结果

| 方法 | 本轮 | 原状态及边界 |
| --- | --- | --- |
| `CompleteJourneyTest#runtime_offline_journey` | **NOT_RUN** | 原三次FAIL原样保留；本轮未装包/未发Instrumentation，不能声称就绪等待修正有效。原金额、恢复、媒体和事实断言及超时不变。 |
| `LongHistoryPerformanceTest#full_twenty_year_dataset_reads_exact_global_facts_and_remains_responsive` | **NOT_RUN** | 1000项目/1000订阅/20000事件/10000付款、2006-10-23～2026-10-23、seed20261023及120秒计算预算未变；本轮实际Room数量、准备/分页/搜索/统计/7306日计算与渲染/选日/取消耗时、App内存及响应指标均 **NOT_CAPTURED**。 |
| `M22AccessibilityTest#system200_real_ime_keeps_production_form_input_and_save_reachable` | **NOT_RUN** | 启动前新增、编译并核对DEX；要求真实系统font_scale2.0、360dp、窗口焦点、实际IME可见、键盘显示时保存可达及关闭后输入保留。没有实际执行，不登记IME或大字PASS。 |

此前 **137 JVM PASS未重跑**；原38个Android身份的最新36 PASS/1 FAIL/1 NOT_RUN不改，原47次执行及7次FAIL不改。加入一个尚未运行的IME方法后，源码计划39个唯一方法，仍只实际执行过37个，最新36 PASS/1 FAIL/2 NOT_RUN；**本轮新增Android执行0、PASS0、重跑0**。[本轮独立结果](m22-supplement/results.json)、[历史逐方法原件](m22/android/results.json)。

## 启动前测试改动、构建与包身份

仅两个`androidTest`文件：旅程增加阶段status及失败前的全屏/受限语义树/本合成账本计数诊断（原异常重新抛出，诊断缺失或截断标UNKNOWN；不覆盖@Before/@After失败）；可访问性增加上述单方法。新IME方法使用**生产ProjectEditorScreen、合成UI状态、隔离ComponentActivity**，保存回调可达不冒充Room落盘；不是MainActivity、LocalDensity或TalkBack验证。没有生产源码、算法、Room、备份或依赖变化。

只执行`:app:assembleDebugAndroidTest :app:lintDebug`，**Gradle exit0，4m27s（命令268.156秒）**；Lint **0错误17警告**。AndroidTest分析实际执行，Main/unit分析和报告UP-TO-DATE；本轮XML与[原归档XML](m22/checks/lint.xml)字节SHA相同，没有重复归档或冒称全量分析。原SDK XML版本提示保留。[构建stdout](m22-supplement/commands/test-package-build/stdout)、[stderr](m22-supplement/commands/test-package-build/stderr)、[退出及SHA](m22-supplement/commands/test-package-build/command.json)、[Lint口径](m22-supplement/checks/lint.json)。

| 实物 | SHA-256 / 状态 |
| --- | --- |
| App `com.cooper.lithic.debug`，code17 / `0.1.0-m22-debug`，13822236字节 | `328e3dc2fbe4c2a503fc81f2750cd68a62e5bf2b2bf624620b21d63b3f9bf213`；原本地与本轮设备回读相同，UID10216；安装0。用户指令短写为59位，按评审提交原记录与实际同字节核对完整值，并非替换App。 |
| 原未运行Test候选 | `f671f7643ffe289f3f19440d84e03d0735f0d9b5be1f183b2c01ee3ea7658d1f`；构建前实物/SHA/证书/Manifest已核对，历史记录不改。 |
| 本轮最终本地Test，3764680字节 | `ce5f5d15abe3c6549f7b23777f2d11ea822a6d4a0417dda165dbfa16e3d6a614`；三个目标类的DEX declared methods已核对；**未安装、未运行**。 |
| 设备仍安装的原Test `com.cooper.lithic.debug.test` | `c9788ced5d20b80397adf04ec1ce87ef72d208716925a76cd072fd74e3e2eef7`，UID10217；未更新。 |

原App、本轮Test及构建前f671候选证书连续，SHA-256 **29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9**。设备App及原Test与此前验签的安装字节相同，未重新提取设备证书。Runner仍为`com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`，本轮未调用。[原候选核对](m22-supplement/checks/initial-artifacts.json)、[最终DEX/源码/SHA](m22-supplement/checks/final-artifacts.json)、[本轮Test验签](m22-supplement/commands/final-test-certificate/stdout)、[最终Manifest](m22-supplement/commands/final-test-manifest-tree/stdout)、[设备安装回读](m22-supplement/commands/end-installed-identities/stdout)。APK不入Git。

准备阶段的JAVA_HOME缺失、aapt绝对中文路径失败均保存原退出记录，后以既有JDK/相对路径完成静态核验；原构建记录exit0后出现本地GBK打印异常，未重跑构建。一次脚本文件执行策略拒绝发生在claim及模拟器启动前，随后以当前shell执行同一已审查脚本内容，没有改全局策略；**实际AVD启动仍只有一次**。另一次只读包查询因宿主引号导致shell语法exit1，未读取或改动包；后续结束保护查询取得真实SHA，未将错误查询当作PASS。见[准备说明](m22-supplement/checks/preparation-notes.md)。

## 保护与正常结束

- 原**29个零字节锁**（M21旧15＋此前M22新增14）UID/inode/nlink/大小/类型/路径完全匹配；跨Boot不要求设备号相同，**未取得锁、未确认锁状态、未删除**。[实际元数据](m22-supplement/commands/end-preserved-29-locks/stdout)。
- M21原9文件、M22原6张控件PNG及旧ANR原图SHA均匹配，未清理/改名/覆盖。[9文件](m22-supplement/commands/end-m21-nine-screenshots/stdout)、[6图及旧ANR](m22-supplement/commands/end-m22-six-plus-anr/stdout)。所有旧报告、三次旅程失败、旧claim及原始结果未改。
- 未观察到App/Test进程、默认`no_backup/active-generation`或`generations`、业务/WorkManager数据库文件，目标注册Job为0；不把全局UID库存当作Job。原始JobScheduler只在内存处理，归档目标记录数及整体长度/SHA，不归档无关应用数据。[账本/进程/设置](m22-supplement/commands/end-storage-and-settings/stdout)、[目标Job](m22-supplement/end-target-jobs.json)。
- 本轮无合成Owner、业务根、临时图片创建，无清理操作；Owner关闭要求 **N/A（未打开）**，不把模拟器退出推断为旧锁/Owner释放。系统设置未改；结束实际font1.0、默认density420、浅色、airplane0/Wi-Fi1/data1，未为未运行的测试提前断网或改字。[保护结论](m22-supplement/end-protection.json)。
- 仅一次官方emulator console正常退出，原launcher/QEMU及端口最终消失。首次退出观察保留了官方`emulator -kill 24224 -sleep 20`收尾helper，未对其发额外命令，后续只读确认helper也自然结束；不是第二次AVD启动或ADB杀App。[退出命令](m22-supplement/commands/normal-console-exit/stdout)、[第一次观察](m22-supplement/after-normal-exit-host.json)、[最终进程/端口](m22-supplement/final-exit-host.json)、[退出证明](m22-supplement/exit-proof.json)。AVD目录保留；其他设备操作0。
- Windows QEMU观测工作集3642122240字节、OS记录峰值3682226176字节（约3.43GiB），**不是App RSS/PSS、Guest硬上限或T03性能结果**；本轮新增磁盘占用NOT_CAPTURED。[资源原记录](m22-supplement/resource-observation.json)。

**当前阻塞：该原AVD冷启动后、安装或运行M22测试之前即出现SystemUI ANR，无法通过设备健康准入。** 不继续T06/T03/IME，不给设备问题编造产品根因，也不自授权修复、换设备或再次启动。提交本轮代码及证据后等待ChatGPT独立复验。

M10 PENDING/DEFERRED及三JPEG历史FAIL、M13-T06延期、M20额外平台FAIL/收口UNKNOWN与5704、Q03未执行项/Q04 PROPOSED/Q02完整跨模块及G1/G2/G3、所有历史NOTES保持。不合并main、不进入M23。

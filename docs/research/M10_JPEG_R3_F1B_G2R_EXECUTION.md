# M10-R3-F1B-G2-R 执行报告

2026-10-17（Asia/Shanghai）。**BLOCKED — M10-R3-F1B-G2-R**。

原AVD在一次获准冷启动后上线；唯一一次锁定G1预检在首轮进程解析处报告`unknown ps UID label`并停止。服务首轮实际分类为NO_TARGET_RECORD，但没有第二轮完整快照或组合资格。必要只读结束保护已完成；不改规则、不重试、不安装或运行应用。

## 契约与Git来源

- 仓库：`cc7279694-debug/property`；锁定G2来源`2de44c79173e7fa52841b55542e5ddcc5399a30a`，来源本地/远端一致且干净，新分支开始时不存在。
- 独立worktree：`E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1b-g2-resume/砺石新APP`；仅提交推送`research/m10-jpeg-strict-r3-f1b-g2-resume`。
- Goal：先核对宿主进程、ADB、原AVD身份；仅在未运行且身份一致时启动一次，再执行一次现有G1只读preflight及必要结束保护。
- Scope：一次原AVD冷启动、240秒有界等待、一次原监督器preflight、必要只读结束核查、新G2-R证据与文档、CURRENT_STATE和checkpoint。
- 禁止范围：安装/启动/停止/清理Android应用，Outsider/normal、故障注入、清数据、AVD重建/wipe/升级/snapshot加载保存、改G1规则、生产源码/测试、APK构建、JNI、allocator、M11。既有模拟器若运行但ADB失联须停止，不启动第二实例。

[契约与来源SHA](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2r-contract.json)记录上述边界。旧G2两条device-not-found记录及其他历史资料未修改。采用项目Resume/Checkpoint和独立worktree流程；本地辅助审阅只核对证据，不代替ChatGPT独立验收。

## 环境准备与一次启动

实际[环境检查](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/environment-before.json)：ADB设备列表无目标；Windows没有目标/未知模拟器进程；5596/5597无监听。启动前再次检查模拟器进程为空，先排他创建[启动attempt](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/launch-attempt.json)，再调用一次Start-Process。

| 身份 | 实际核对 |
|---|---|
| AVD入口 | `E:/Android/avd/Lithic_M01_API_36.ini`，SHA `a13c3fd2492d875a77a68262842b8912c3df6f67f4f2e329748774b465e3b902` |
| 既有数据目录 | `E:/Android/avd/Lithic_M01_API_36_fresh.avd`，入口明确指向此目录，userdata存在；只查元数据，未读userdata内容 |
| config.ini | 3,565B，SHA `555d10f2c3fe3bd8cd1c231ceea06c77532d132d39236a7f31c9fc839506bf54` |
| 原emulator.exe | `C:/Users/CDD/AppData/Local/MirraAndroid/sdk/emulator/emulator.exe`，SHA `dede05844d17902a7c74f2cee2bbe97ec9afaaf22ebc35a7cdc6d7c1eaa0b9dd` |

AVD入口/config哈希及程序路径与[F0启动记录](m10-jpeg-r3-f0/evidence/20261017T015123.092793Z/authorized-avd-start.json)一致；F0未记录程序SHA，上表程序SHA仅为本轮实测，不能称已完成其历史字节比对。本次继承的是F0锁定的现有AVD；不宣称从M01起现场或userdata字节持续未变。没有读取私人账本/图片或snapshot内容，也没有修AVD配置。

[唯一启动请求](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/launch-request.json)沿用F0参数：

`-avd Lithic_M01_API_36 -port 5596 -no-window -no-audio -no-snapshot-load -no-snapshot-save`

仅启动进程内设置`ANDROID_AVD_HOME=E:/Android/avd`，随后恢复调用方环境；窗口Hidden。launcher PID16988，结束时仍可见同PID及QEMU子进程20516。没有kill-server、终止模拟器或第二启动尝试。config默认仍有snapshot设置，本次由两个明确禁用参数覆盖；没有删除任何历史snapshot。原始宿主console只留本地，提交[范围/摘要记录](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/emulator-startup-scope.json)，不把无匹配日志行写成独立证明所有内部snapshot行为。

## 有界等待与唯一预检

- [等待结果](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/boot-wait-result.json)：上限240秒，实际循环44.766秒、13轮26条只读查询；24次退出0、2次早期offline退出1、无超时，全部[原返回值](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/boot-wait-commands.jsonl)保留。等待查询不计完整预检重试。
- 从启动请求07:44:11.982 UTC至07:45:17.787 UTC身份确认约65.805秒；这与等待循环44.766秒是不同口径。
- 实际确认serial=emulator-5596、AVD=Lithic_M01_API_36、API36、boot_completed=1，然后才启动预检。
- [预检](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2r-preflight-execution.json)只运行一次：原`docs/research/m10-jpeg-r3-f1b/verify.py preflight`，源码SHA `ad116cc3ab41a8974cf8a8d997319395fff7c81e6283c73fd35074fc811516e8`；G1分类器/组合器和全部旧源码保持锁定字节。
- 全新输出目录为`docs/research/m10-jpeg-r3-f1b/evidence/g2r-20261017-01`，遵守原监督器的输出根约束，不覆盖G2目录。
- 07:45:49～07:45:58 UTC，预检进程退出2：[阻塞](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/blocked.json)为`unknown ps UID label`。第一轮服务NO_TARGET_RECORD，进程检查BLOCKED，第二轮和组合门禁未运行。立即停止，不重新执行preflight。

## 必要结束保护与真实限度

仅调用一次锁定G2的`end_protection.py`，不调用旧finish、不改源。该入口只做设备/包/设置/私有元数据聚合、研究ps/Service/stage读取，不执行preflight。结束进程退出0仅表示收集完成；[内部结果](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2-ending-result.json)仍保留processStatus=BLOCKED，不能登记为门禁通过。

[保护比较](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2-protection-comparison.json)确认正式APK/UID、188条私有元数据聚合与设备设置在本次前后相同；私人字节完整性NOT_RUN。研究三包实际身份及其前后比较由真实命令离线登记，未复制APK或私有内容。结束stage ABSENT只是观察结果，不证明lease释放、worker死亡或授权谓词安全。

预检/结束共66条ADB读取，均退出0；加初始设备列表1条、启动等待26条，共**93条已归档ADB读取**。这些是命令数，不是93项测试PASS。[命令完整性登记](m10-jpeg-r3-f1b/evidence/g2r-20261017-01/g2r-command-integrity.json)区分源采集摘要与离线文本摘要，未为脱敏原文补造hash。

首轮服务完整capture为68B、退出0、complete=true/truncated=false，SHA `44009d1664d1b66d17ec4859a9e03642b0125d242c05eac9c101806e2397fb7c`；原文仅在G1内存分类，按原策略不归档。首轮ps失败路径只留21,913B，无SHA/未知标签原值；结束ps的21,919B及其SHA是另一时点，不能代替首轮输入。没有额外诊断读取去补造此缺口。

## 交付、验证与停止

只新增G2-R执行/设备结果/决策、证据及checkpoint，更新CURRENT_STATE；不改G1/G2原件、正式App、Java、WorkerService、StageStore、JPEG测试或APK。实际验证为来源SHA、环境/一次启动/有界等待、一次真实门禁及结束读取、离线结果与Git/链接/敏感信息审计。G1原81项宿主测试、Java/JVM、Gradle、Lint、构建及业务测试未重跑，原29场景仍0 PASS/1 UNKNOWN/28 NOT_RUN。

[设备结果](M10_JPEG_R3_F1B_G2R_DEVICE_RESULTS.md)、[决策与风险](M10_JPEG_R3_F1B_G2R_DECISION.md)、[checkpoint](../checkpoints/2026-10-17-M10-JPEG-R3-F1B-G2R.md)。仅推送新研究分支后停止，最终SHA取Git回执。模拟器保持运行，不自动关闭或操作应用；N01～N03、历史UNKNOWN、M10 PENDING/三JPEG FAIL、UI-C1冻结和M11未授权不变。

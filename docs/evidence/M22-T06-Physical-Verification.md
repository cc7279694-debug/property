# M22 真机T06正式补验

**READY_FOR_REVIEW — M22 T06 PHYSICAL VERIFICATION**（仅交付准入证据）。实际 **BLOCKED — ACTIVE_JOB_STATE_UNKNOWN_N_A**。过滤和独立未过滤系统任务输出均将Active标为`N/A`，无法证明该活动状态已知；**Test更新0次、T06执行0次**。不将N/A解释为零任务，也不反向推断已经有活动Worker或产品缺陷。M22继续BLOCKED / CHANGES REQUIRED。

来源`6bc13cdc057d1029ea57d97e12625fd4bacfbcf3`，分支`codex/m22-experience-accessibility`，2026-10-24（Asia/Shanghai）。开始时本地/远端HEAD一致、工作区干净。仅此前USB小米`23127PN0CC` / API36 / arm64-v8a，同Boot `a9edfd47-8cb5-40a5-92eb-c41925ab00ea`；实际命令均显式指定该USB序列，公开证据仅其SHA-256 `e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92`，不提交硬件序列或其他设备身份。

## 安装身份与桌面核验

| 对象 | 实际结果 |
| --- | --- |
| 原M10 | UID10376/code8；设备APK SHA `939b3b20d3c6cd02c800f320ccddc300736b75084b609c404f38b096270f7094`，前后安装身份/路径/SHA一致；无启动、更新、停止或私有账本读取 |
| M22 App | UID10420/code17；设备SHA `b749b771e4f6f9cebcb7355c95834b7ae2eb4ba4f7528536a92bf9e09eefb2d3`，前后一致；无更新或正常MainActivity启动 |
| 手机当前Test | UID10427；仍为 `c3791305aed28899f8df98dc7c89f27c986c022b84a0e528c68c59ebf9f2037f`，前后一致；未更新 |
| 桌面新Test | 3,403,324 bytes；SHA `12e17f01be03677501a753b1992badffdfaece0f4db1742ba27b498afc1dae26`。实际签名工具exit0，唯一证书SHA `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`；唯一Instrumentation仅由独立Test指向独立M22 App，Runner仍`com.cooper.lithic.app.M13IsolatedRunner` |

[桌面实物审计](m22-t06-physical-20261024-044110/artifact-audit.json)、[成功签名原文](m22-t06-physical-20261024-044110/commands/desktop-new-test-signature-relative/stdout)、[实际Manifest](m22-t06-physical-20261024-044110/commands/desktop-new-test-manifest-relative/stdout)、[前后身份及总结果](m22-t06-physical-20261024-044110/result.json)。没有重新构建、替换APK或创建密钥；桌面PASS不代表手机Test已更新或T06已经执行。

## 安全门禁与唯一阻塞

完整数字UID进程观察在开始/结束均未发现UID10420/10427或目标包进程；结束目标ServiceRecord为0。包过滤JobScheduler的目标Registered为`None.`、Pending为`None`，但完整Active section是`Active jobs:\n  N/A\n`。保守门禁停止后，只做一次独立未过滤JobScheduler观察：命令exit0、**1,436,022 bytes**，SHA **`c33d822bc690986fa8261628b94d7efcfae6c59c9dc64c1098f2f3db08dd122b`**，完整Active section仍为N/A。**语义和活动上下文未证明，不能用进程缺失、Registered/Pending为空或quota timer替代Active状态核验。**

[过滤任务回执](m22-t06-physical-20261024-044110/commands/before-target-jobs/command.json)、[独立完整输出的长度/SHA及目标section](m22-t06-physical-20261024-044110/commands/independent-unfiltered-job-state/command.json)、[隐私过滤后的独立观察](m22-t06-physical-20261024-044110/independent-job-observation.json)、[结束进程观察](m22-t06-physical-20261024-044110/commands/end-target-processes/command.json)。完整系统任务/进程输出仅在内存处理，不归档其他App名称、内容或日志。原API36命名空间匹配保留，不将空提取数组直接作为无任务结论。

## 方法、资源与保护结果

| 项目 | 状态 |
| --- | --- |
| Test更新 / 实际新SHA安装确认 | **NOT_RUN**；没有创建安装claim或发送install命令，旧Test保留 |
| 真实飞行模式及IPv4/IPv6离线门禁 | **NOT_RUN**；没有要求用户切换网络，不读取路由冒充离线通过 |
| T06唯一完整方法 / JUnit与异常堆栈 | **NOT_RUN**；没有Instrumentation命令，不生成虚假JUnit、阶段或截图 |
| plain Application、新UUID、journeyCompleted、合成Owner/数据库关闭 | **NOT_RUN**；没有本轮测试Owner或合成账本创建 |
| 新版失败现场保留 / 成功清理 | **NOT_RUN / PENDING_ANDROID_VERIFICATION**；不能由桌面源码、编译或此次未执行升级为真实PASS |
| 默认generation与WorkManager | 仅结束时读取必要路径/UID/inode/大小/时间元数据，与[此前已提交收口快照](m22-process-closure-resume-20261024-035729/commands/end-store-metadata/stdout)一致；本轮因先被阻断未取得其开始快照，**不声称本轮前后元数据验证**。未读取业务数据库内容、active指针内容或进行关闭/恢复/清理；历史正常Owner关闭NOT_PROVEN、账本完整性NOT_VERIFIED保持 |
| 三包及手机设置 | 三包前后包名/UID/版本/User0安装字段/唯一路径/SHA一致；font1.1、density480/1200×2670、airplane0/Wi-Fi1/mobile_data0、主题值0及Boot前后一致。无网络或其他设置修改，恢复操作不需要；不凭Runner或进程退出推断Owner关闭 |

[结束默认文件元数据](m22-t06-physical-20261024-044110/commands/end-store-metadata/stdout)、[结束设置](m22-t06-physical-20261024-044110/commands/end-settings/stdout)、[总结果](m22-t06-physical-20261024-044110/result.json)。没有采集手机私人屏幕；没有删除任何默认或历史目录。

## 原始失败及停止

保留首次桌面签名命令exit1：历史JSON用Windows默认编码读取导致中文绝对路径乱码；明确UTF-8并对同一锁定APK用相对路径后实际验签exit0，未重建。初次安全门禁因Active N/A而监督器exit1，原观察不改写。结束查询全部完成后，本地汇总再次因默认编码读JSON失败；仅在电脑用UTF-8重新生成汇总，没有重复手机查询。两次外层Python控制台原字节/SHA未另归档，保持**NOT_CAPTURED**；设备命令均有真实退出码、时间、完整长度/SHA及必要目标内容。[分层失败说明](m22-t06-physical-20261024-044110/control-layer-errors.json)。

本轮 **27次只读ADB**（1次主机发现＋26次定向手机查询）、3次桌面SDK静态命令；安装、Instrumentation、App正常启动、强停、任务取消、设置写入、数据库内容读取及监督器清理均 **0**。无产品/测试源码修改，无构建、JVM或旧Android套件重跑。原137 JVM/36 Android PASS、T06三次FAIL、历史UNKNOWN/NOT_RUN及M10/M13-T06/M20/Q03/Q04/G1/G2/G3全部保持。T03和真实IME本轮未获授权、未运行。

提交推送后停止，等待独立审查平台Active状态的可验证证据；不自行把N/A加入“安全空值”、更新Test、继续T06、冻结M22、合并main或进入M23。

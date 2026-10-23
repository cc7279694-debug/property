# M22 干净环境定向补验

**BLOCKED — SYSTEMUI HEALTH GATE**。来源`bf5e8c4f27efc9f036254d25ea4c241de70afedf`，分支`codex/m22-experience-accessibility`。用户另行同意：资源足够后新建一台隔离API36，只补T06、正式20年T03及真实系统IME。**本次干净空设备在安装砺石之前已出现SystemUI ANR，未进入任何测试。**

## 资源、创建与隔离

- 只读资源准入：可用物理内存 **6058926080字节，约5.64GiB**；E盘剩余 **102516019200字节，约95.48GiB**，分别高于本轮5GiB/10GiB保守余量。实际启动前内存5818716160字节，约5.42GiB。[原始测量](m22-clean-20261023-1523/resource-admission.json)、[启动claim](m22-clean-20261023-1523/start-claim.json)。不是把磁盘空间当内存，也不能据此认定ANR根因。
- 新AVD：**Lithic_M22_Clean_API36_20261023_1523 / emulator-5714**；专属根`E:/CODEX/.codex/research-env/lithic-m22-clean-20261023-1523`，独立注册、空userdata/cache、snapshot及临时目录；未克隆旧设备。复用API36 Google APIs/x86_64 r7、emulator37.1.11、既有SDK/JDK；7项已锁定工具/镜像元数据SHA核对，整张大镜像未重新hash。下载/升级/许可接受均0。
- 官方`avdmanager create` **exit0，实际创建1次**。新配置RAM2560MiB/2核、swiftshader、headless/no-snapshot。保留[官方原配置](m22-clean-20261023-1523/official-generated-config.ini)和[原注册](m22-clean-20261023-1523/official-generated-registration.ini)，新配置调整与隔离证明见[创建核验](m22-clean-20261023-1523/creation-proof.json)。创建后宿主校验脚本先有编码别名拼写错误、再有未定义局部变量；均仅修正后置校验，没有重发创建或覆盖原始生成证据。
- 2026-10-23 **15:26:50（UTC+8）仅启动1次**。launcher3760、QEMU31856的实际路径、父子关系、创建时间、启动参数及5714/5715端口一致；真实userdata/encryption在新`avd-data`，cache在新`runtime-data`，均属于新根，未仅凭`-datadir`推断。[进程/端口](m22-clean-20261023-1523/runtime-host-proof.json)、[运行路径](m22-clean-20261023-1523/runtime-path-proof.json)、[原hardware配置](m22-clean-20261023-1523/runtime-hardware-qemu.ini)。
- 实际boot完成、API36/x86_64、AVD名称确认；Boot ID **abedd018-8627-478b-aefc-59b6ce4310bf**。[boot原始观察](m22-clean-20261023-1523/boot-proof.json)。emulator记载启动136954ms，不把boot完成等同SystemUI可用。

## 真实健康阻塞

| 观察 | 实际结果 |
| --- | --- |
| 15:29:57第一轮 | SystemUI PID1073/UID10186，焦点为`Application Not Responding: com.android.systemui`；全屏有“System UI isn't responding”及Close app/Wait。 |
| 15:30:16第二轮 | 间隔约19.12秒，焦点和全屏仍是同一ANR遮挡；不是等待后恢复的PASS。 |
| 独立系统事件 | `executing service com.android.systemui/.SystemUIService, waited 20001ms`；这是直接超时点，**深层根因UNKNOWN**。没有ANR线程栈/内存归因证明。 |
| 砺石身份 | 首尾目标包列表为空，无App/Test或目标进程，**安装0、Instrumentation0**。没有M22代码在该设备运行，不能将本次系统ANR归因为M22测试或业务缺陷。 |

[第一轮窗口](m22-clean-20261023-1523/commands/health-1-window/stdout)、[第一轮全屏](m22-clean-20261023-1523/commands/health-1-frame/stdout.png)、[第二轮窗口](m22-clean-20261023-1523/commands/health-2-window/stdout)、[第二轮全屏](m22-clean-20261023-1523/commands/health-2-frame/stdout.png)、[目标ANR事件](m22-clean-20261023-1523/systemui-anr-events.json)、[首尾包缺席](m22-clean-20261023-1523/commands/end-package-absence/stdout)。

两张实际ADB全屏PNG均1080×1920；第一张462835字节/SHA **89d35878a95ce99953da69e5d29a87f5249b1aa6ea76d1ebd7502172c5386189**，第二张469669字节/SHA **e4926ba0b1074bcc01d088fb0a06c73c09b463af3cb8acdb1df763d767be4b92**。命令原始退出码/字节/SHA随各截图目录归档；不是控件层截图或MainActivity。事件缓冲只归档精确SystemUI ANR行及原输出长度/SHA，无关行仅在内存处理。未清logcat、修改容量、点击弹窗或尝试系统修复。

## 剩余三项与APK

| 方法 | 本轮真实状态 |
| --- | --- |
| `CompleteJourneyTest#runtime_offline_journey` | **NOT_RUN**；原三次FAIL不改。 |
| `LongHistoryPerformanceTest#full_twenty_year_dataset_reads_exact_global_facts_and_remains_responsive` | **NOT_RUN**；固定1000/1000记录、20000/10000事实、2006-10-23～2026-10-23、seed20261023及120秒预算不变；实际Room数量、阶段耗时/响应/App内存均NOT_CAPTURED。 |
| `M22AccessibilityTest#system200_real_ime_keeps_production_form_input_and_save_reachable` | **NOT_RUN**；没有实际IME显示或系统200%输入/保存的新证据。 |

本轮未改源码、未重编译、未运行JVM/Lint或任何旧套件。原137 JVM PASS、Android最新36 PASS/1 FAIL/2 NOT_RUN（39计划唯一、37已执行）及所有旧RED/UNKNOWN保持。[本轮结果](m22-clean-20261023-1523/results.json)。

复用原App code17/`0.1.0-m22-debug`：SHA **328e3dc2fbe4c2a503fc81f2750cd68a62e5bf2b2bf624620b21d63b3f9bf213**；Test SHA **ce5f5d15abe3c6549f7b23777f2d11ea822a6d4a0417dda165dbfa16e3d6a614**。本轮实际本地验签两者v2/同Debug证书 **29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9**，包名/Runner仍为`com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`。**均未安装，设备UID/安装回读N/A**，不伪造安装身份。[实物索引](m22-clean-20261023-1523/artifacts.json)、[App验签](m22-clean-20261023-1523/commands/app-certificate/stdout)、[Test验签](m22-clean-20261023-1523/commands/test-certificate/stdout)。原0错误17警告仅继承前轮Lint，不称本轮新执行。

## 安全结束与唯一阻塞

- 设备始终没有目标包/进程/注册Job，未开合成Owner、创建业务generation或触发生产WorkManager；无需关闭本轮未打开的Owner。没有读取私人账本。[目标Job观察](m22-clean-20261023-1523/end-target-jobs.json)。
- 系统设置未改；结束实际font1.0、默认density420、night=`custom_bedtime`、airplane0/Wi-Fi1/data1与新设备原值一致。没有为未运行测试切断网络/改字、没有清理数据。[结束设置](m22-clean-20261023-1523/commands/end-settings/stdout)。
- 只正常关闭新5714一次，最终宿主进程和5714/5715端口均消失，AVD/现场保留。不是仅以退出命令OK证明结束，也不由模拟器退出推断任何锁/Owner释放。[退出命令](m22-clean-20261023-1523/commands/normal-console-exit/stdout)、[最终宿主状态](m22-clean-20261023-1523/after-normal-exit-host.json)、[退出证明](m22-clean-20261023-1523/exit-proof.json)。
- Windows QEMU观测峰值 **3706683392字节，约3.45GiB**；新研究根关闭前文件逻辑长度合计 **4276598613字节，约3.98GiB**，实际分配占用NOT_CAPTURED。不是App RSS/PSS、T03测量或Guest内存硬上限。[资源口径](m22-clean-20261023-1523/resource-observation.json)。
- 原5712及29锁/截图、5704/H2/C2/M10等旧现场没有ADB、启动、修改、复制或清理；没有把未重新读取的旧锁/图片登记成一次新SHA验证。旧报告、原SystemUI失败、原claim及全部历史结果不改。

**当前唯一先决阻塞是模拟器SystemUI健康，而非磁盘不足或测试包安装问题。** 干净环境也在安装前阻塞，不能仅据此认定“多加内存即可解决”，也不继续自动新建/启动/重试。三项产品门禁仍未完成，M22不冻结；提交推送后停止等待ChatGPT独立复验。M10/M13-T06延期、M20平台FAIL/收口UNKNOWN、Q03/Q04/Q02与G1/G2/G3及其他历史NOTES全部保持，不合并main、不进入M23。

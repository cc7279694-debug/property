# C6-2D Android隔离限定执行：环境阻塞

2026-10-29；分支`codex/lithic-v1-product-completion`；起点`d2779276164f33df816949721be3b7fe74fa59cf`。用户本轮只批准检查既有独立、健康、可丢弃的Android测试环境，以及通过门禁后逐方法执行；不授权真机、小米14、真实数据、新建/修复故障环境或清场。

**C6-2D RUNTIME — ENVIRONMENT_BLOCKED**

第一关未取得安全可用的独立模拟器，已按授权停止设备执行。**安装0、Instrumentation0、App/Test/AVD启动0、重试0、清理0**；8项测试未运行，不判业务FAIL，也不判隔离安全PASS。

## 环境实际观察

- [主机原始资源/进程记录](c6-2d-runtime-20261029-195432/host-resources.json)：可用物理内存 **6.302GiB**（总15.877GiB）；Intel i5-8300H，4核8线程，采样CPU 23%；E盘可用49.054GiB。资源余量本身不是此次停止原因。
- 系统CIM枚举仅过滤QEMU/emulator/adb名称，**QEMU/emulator进程0**；仅见已有ADB服务。未终止任何进程。
- [ADB主机枚举](c6-2d-runtime-20261029-195432/adb-host-enumeration.json)：只对已有`127.0.0.1:5037`服务发送`host:version`和`host:devices-l`，协议回复OKAY、version0029；**设备列表0字节/0设备**。[真实空响应](c6-2d-runtime-20261029-195432/devices-l.stdout)的SHA为`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`。未启动/重启ADB服务，未向任何serial发命令，未查询手机。
- 仅列默认/环境声明AVD注册的10个`.ini`文件名，包含历史Lithic及其他项目；**注册名不等于已启动、健康、无业务数据或获准丢弃**，没有可证明归属本轮的C6独立环境。未打开这些AVD的配置/userdata/日志，未枚举受保护研究目录、读取账本或启动旧设备。此有限检查不声称机器全部自定义AVD均不存在。
- 因没有准入目标，实际设备API、ABI、Boot ID、UID、SystemUI/ANR与启动健康检查均 **NOT_RUN/NOT_AVAILABLE**，不能把没有观察到ANR写为系统健康PASS。5712/5714等旧故障环境不使用，其他保护现场不操作。

## 候选没有漂移

本轮仅读取既有实物，无重建、无替换、无新APK复制：

|候选|字节数|本轮重算SHA-256|
|---|---:|---|
|`com.cooper.lithic.c6isolation.debug`|15,976,838|`933387de7ce775d10d1f41bc438d650ea8ca273b1719911df51c091273d7627f`|
|`com.cooper.lithic.c6isolation.debug.test`|3,737,008|`e4831773c2397c07b49f6672348161918a5048b51c7563c2c12dcce57fed5d87`|

两包字节与[既审实物身份](c6-2d-preflight-20261029-172534/artifact/identity.json)一致，沿用已审原Debug证书`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`、M13 Runner、唯一独立targetPackage及默认目标进程。证书/Manifest/Runner DEX为相同字节绑定的既审静态证据，**本轮未重跑SDK签名工具，不是设备安装身份验证**。详见[本轮候选指纹](c6-2d-runtime-20261029-195432/candidate-check.json)；Preview12 15,977,934字节/SHA`4b6b3ed03674fc971b67a124171fedbb89fce155d2c6fd181ed597cde6250a1b`保持。

## 八项JUnit逐方法结果

|原方法|本轮结果|原始执行状态/原因|
|---|---|---|
|MediaSandboxStartupTest#manifest_declares_private_isolated_service_in_only_the_media_process|NOT_RUN|未取得安全可用的独立模拟器，JUnit未启动|
|MediaSandboxStartupTest#startup_providers_are_not_assigned_to_sandbox|NOT_RUN|未取得安全可用的独立模拟器，JUnit未启动|
|MediaSandboxStartupTest#unknown_unattached_application_cannot_create_owner_or_business_configuration|NOT_RUN|未取得安全可用的独立模拟器，JUnit未启动|
|MediaSandboxStartupTest#host_test_application_is_plain_and_has_not_run_normal_business_startup|NOT_RUN|未取得安全可用的独立模拟器，JUnit未启动|
|MediaIsolationChannelTest#echo_runs_under_independent_uid_with_read_only_input_and_separate_closure_evidence|NOT_RUN|未取得安全可用的独立模拟器，JUnit未启动|
|MediaIsolationChannelTest#cancelling_after_computing_keeps_revocation_and_preserves_the_session_files|NOT_RUN|未取得安全可用的独立模拟器，JUnit未启动|
|MediaIsolationChannelTest#nonreturning_synthetic_compute_times_out_without_promoting_late_success|NOT_RUN|未取得安全可用的独立模拟器，JUnit未启动|
|MediaIsolationChannelTest#admitted_record_after_host_state_loss_blocks_same_and_recreated_channel_without_binding|NOT_RUN|未取得安全可用的独立模拟器，JUnit未启动|

没有测试启动/完成状态、测试stdout/stderr、外层测试退出码或新UUID；不能制作空测试日志冒称已执行。API26/API36×arm64/x86_64没有任何新覆盖。独立UID/SANDBOX、输入拒写、FD/Guard、Binder死亡、RPC结束、PROBE_CLOSED、服务重建/拒访/取消/超时/Host状态丢失均 **NOT_RUN**；既有运行UNKNOWN不改判。

## 范围保护与下一步

- 本轮JVM、构建、AndroidTest编译、Lint均 **NOT_RUN**；原候选25 JVM PASS/0E16W仅为[原桌面记录](LITHIC-C6-2D-Isolation-Verification.md)，不算本次重跑。只增加本报告和脱敏环境元数据及当前状态登记，不改源代码、测试、版本、签名或任何APK。
- Room5、backup4、Owner/generation/恢复Token/指针、C6-1只读媒体、C9-R1/三主题/Preview12、冻结金额/订阅/通知规则保持。无JPEG/native/Picker/照片写入/替换/删除/GC；无main/M23/新体验版。
- M10三JPEG FAIL、媒体资源UNKNOWN、C6-1/2C/2D全部未执行边界、M20平台FAIL/UNKNOWN、M22/C3/M13-T06/Q03/Q04/G1～G4等历史门禁保持。**ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED**，不得进入严格JPEG整合或照片导入。
- 建议暂停本机隔离运行路线，优先转入V1其他稳定性工作；此为建议，不自动开始新任务。不重复探测/折腾旧电脑，也不要求继续释放内存。以后只有明确获授权的健康可丢弃环境及独立审查后，才重新申请一次限定执行；本次不申请或创建替代环境。

停止等待ChatGPT独立审查，不能把ENVIRONMENT_BLOCKED改成运行PASS。

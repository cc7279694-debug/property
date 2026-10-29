# C6-2D Android隔离安全验证：桌面执行准入

2026-10-29；分支`codex/lithic-v1-product-completion`；基线`af76a34250c36237388e1f4e2a9a8428ee4e82db`。C6-2C桌面实施已获用户转交的独立接受，运行未验收。**本轮无设备授权，仅完成独立安全候选审查点；不宣称Android运行通过。**

目标/范围：复核既有通道，最小独立构建、原8方法安全护栏和状态回执；不重写Service、原断言或冻结业务。桌面门禁已完成；候选须经独立审查及新的设备授权，不得据本报告直接安装/执行。没有APK用户交付、JNI或图片写入。

## 独立身份与启动合同

- 显式`-Pc6IsolationVerification=true`，输出仅`app/build-c6-isolation/`；新App `com.cooper.lithic.c6isolation.debug`、Test `.test`，code17/`0.1.0-c6isolation-debug`，不与原Debug、Preview、M22或C3身份重叠。互斥其他opt-in，拒绝无效值、M13=false、历史进程死亡参数，禁Release。
- 专属Manifest白名单：真实LithicApplication＋唯一非导出isolated `:media_sandbox` Service＋主进程无任何metadata的StartupProvider；无Activity/Launcher/Receiver/其他Service。App移除WAKE_LOCK、NETWORK_STATE、BOOT声明；共享通知源码所需POST仅声明，未来不请求/授予。无网络/存储权限。
- **不能把Manifest全局换成plain Application**：真实Service会拒绝非LithicApplication。M13IsolatedRunner只将Instrumentation默认目标进程换成plain Application；sandbox仍真实创建LithicApplication并调用onCreate，由SANDBOX角色提前返回，拒绝Owner/Room/DataStore/提醒/WorkManager配置。不是“Application从未启动”。
- 原`M13IsolatedRunner`及全部`app/src/main`保持字节不变；当前默认Main正常启动/三主题、Room5/backup4、C6-1、Owner/generation/恢复协议/业务规则不改。Preview12及本地已有历史APK保持。

## 候选实物（仅桌面准备，未安装）

|产物|本地绝对路径|字节数|SHA-256|
|---|---|---|---|
|独立App|`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/app/build-c6-isolation/outputs/apk/debug/app-debug.apk`|15,976,838|`933387de7ce775d10d1f41bc438d650ea8ca273b1719911df51c091273d7627f`|
|独立Test|`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/app/build-c6-isolation/outputs/apk/androidTest/debug/app-debug-androidTest.apk`|3,737,008|`e4831773c2397c07b49f6672348161918a5048b51c7563c2c12dcce57fed5d87`|

两包原Debug证书SHA-256均为`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。minSdk26/targetSdk36，签名验证exit0；Test唯一Instrumentation为`com.cooper.lithic.c6isolation.debug.test/com.cooper.lithic.app.M13IsolatedRunner`，targetPackage仅App，无targetProcesses（默认目标进程）。无替代签名、无用户体验版递增。

App的“无Activity/Launcher”等白名单结论不扩为Test：Test依赖仍声明REORDER_TASKS、3个exported AndroidX辅助Activity及LAUNCHER category（没有MAIN action），不是业务入口，8方法不使用它们；不为掩盖依赖而删测试Manifest。App只有POST及本包signature动态接收器权限，无INTERNET。实际未授权限/未安装/未启动。


## 八项Android真实状态

以下方法分别位于[启动测试](../../app/src/androidTest/java/com/cooper/lithic/app/MediaSandboxStartupTest.kt)和[通道测试](../../app/src/androidTest/java/com/cooper/lithic/data/media/isolation/MediaIsolationChannelTest.kt)。8个方法身份及全部原JUnit断言逐行保持，只有准确新包/Runner/plain宿主护栏和有限回执；本轮编译不计运行。

|顺序|类/方法|API26|API36|
|---|---|---|---|
|1|Startup：manifest_declares_private_isolated_service_in_only_the_media_process|NOT_RUN|NOT_RUN|
|2|Startup：startup_providers_are_not_assigned_to_sandbox|NOT_RUN|NOT_RUN|
|3|Startup：unknown_unattached_application_cannot_create_owner_or_business_configuration|NOT_RUN|NOT_RUN|
|4|Startup：host_test_application_is_plain_and_has_not_run_normal_business_startup|NOT_RUN|NOT_RUN|
|5|Channel：echo_runs_under_independent_uid_with_read_only_input_and_separate_closure_evidence|NOT_RUN|NOT_RUN|
|6|Channel：cancelling_after_computing_keeps_revocation_and_preserves_the_session_files|NOT_RUN|NOT_RUN|
|7|Channel：nonreturning_synthetic_compute_times_out_without_promoting_late_success|NOT_RUN|NOT_RUN|
|8|Channel：admitted_record_after_host_state_loss_blocks_same_and_recreated_channel_without_binding|NOT_RUN|NOT_RUN|

实际UID/SELinux、两ABI、API26进程名降级路径、真实Binder死亡、FD与锁、无重建/资源峰值均NOT_RUN/UNKNOWN。没有设备查询，不能声称某手机已无该新包或环境已可用；后续需含保留包记录的安装前准入。

## 安全边界与覆盖缺口

|边界|源码可确认|端侧仍须证明|
|---|---|---|
|启动/身份|MAIN精确准入；UNKNOWN/SANDBOX拒绝业务；Messenger真实sendingUid、首次HELLO固定UID、nonce/version核验|实际孤立UID、跨UID/SELinux、主目标真实noBackup根/默认generation/WorkManager与Job无初始化；原测试只检查合成子根，不能独自证明全无业务|
|FD/Guard|O_RDONLY输入；O_EXCL空输出/Guard；regular/UID/单链接/dev/ino/长度；Host独立open，不用dup冒充租约；持锁才COMPUTE|实际拒写、跨UID能力、持锁/归还；API26权限不靠API30 fcntl推断|
|结束|Binder死亡、曾持有后同inode锁归还、Host FD、Worker自报input/output关闭、RPC/解绑分别记录；仅完整组合可PROBE_CLOSED|各项真实值及无自动重建；PID/stopSelf/onDestroy/Runner结束均不能替代|
|撤销/恢复|取消/超时撤销，UNKNOWN封锁，旧ADMITTED/UNKNOWN乃至CLOSED日志不自动重试，不删除文件；任何结果均无媒体写入资格|timeout/cancel用永不返回latch，不是真正native取消或迟到RESULT注入；坏UID/nonce/重复回包主要JVM证据；Host丢状态只是合成ADMITTED，不是真实进程死亡|

新增状态回执`kind=OBSERVATION_NOT_JUNIT_COMPLETION`保留closure每个字段、null、撤销、UID/PID与当前UUID。先于原断言落盘，**不能作为JUnit PASS**。仅在本方法新UUID根写≤16KiB `observation-N.json`，不清理任何文件；只有读回已回传UUID的确切单文件才可后续提取，禁止枚举别的包/整机抓取。closure=false仅表示未取得该项证据，不能据此推断仍持有或已释放；缺回执/进程中断/关闭不明保持UNKNOWN，不假称finally必达。

## 未来一次执行的前置条件（本轮不执行）

1. 先ChatGPT独立审查候选，再另获用户明确设备、API/ABI和安装/单方法运行授权；原故障AVD、C3及用户既有App/真实账本均不使用。新包设备安装记录/UID/签名/健康也必须现场核对，不能仅凭新包名通过。
2. 每方法单独选择：`<独立Test包>/com.cooper.lithic.app.M13IsolatedRunner`，`-e class <完整类>#<完整方法>`；严禁整类/整包、自动循环或重试。依表先启动，再ECHO/FD/Guard，再撤销/恢复。原方法内10～30秒/取消join限制不改；建议桌面每方法外层≤90秒止等，超时只记UNKNOWN并停止，不自动杀进程。
3. 逐项核JUnit结束、每个关闭回执与目标进程/Service实际终态；启动异常、UID/文件权限不符、超时或关闭UNKNOWN即停依赖后续。timeout/cancel可JUnit PASS但closure UNKNOWN，不能继续。Host-state-loss应在已证明干净的独立测试宿主首次调用，避免进程级quarantine掩盖journal路径；不能为此清场、强停、换UUID绕过UNKNOWN。
4. 未来只可读取本新包已回传UUID里的`no_backup/c6-isolation-android-tests/<UUID>/observation-N.json`（≤16KiB）及必要标记/会话事实；保存原始JUnit/退出码。预期路径不是设备实物证据。未授设备/提取授权前不执行任何ADB命令。

## 本轮桌面验证与源码绑定

- **25唯一JVM PASS，0失败/错误/跳过**：AppProcessRole8＋IsolationSessionState17；本轮不是重跑全部历史套件。
- 本轮构建前可用RAM **4.979GiB**，离线、单worker；独立App/Test构建、AndroidTest源码编译、Lint组合exit0（84任务实际执行），**Lint 0错误16警告**，未禁用检查。SDK XML/历史编译warning保留，不升级SDK。
- 五种构建身份求值PASS，六种非法/冲突参数均EXPECTED_REJECTION，不算产品失败或Android测试。普通Debug/Release、Preview12/code28、M22、C3包名/版本/输出路径/Runner连续；新C6与原C3均无Release变体，C6无任何生产Launcher。
- 实际APK Manifest＋明文packaged Manifest＋签名＋DEX核对通过；测试DEX的newApplication明确const-class平台Application后调用原AndroidJUnitRunner。7个通道/启动生产类定义在App；原M13及245个main源文件哈希不变，测试框架不入App；8个既有native条目与Preview12逐字节一致，无JPEG新库。
- 原8方法/原断言逐行保持；**所有8 Android方法均NOT_RUN**，不把构建/语义/源码当跨UID/资源安全证据。11个历史deliveries APK字节/SHA保持，包括Preview12 `4b6b3ed03674fc971b67a124171fedbb89fce155d2c6fd181ed597cde6250a1b`。

[组合命令/退出码](c6-2d-preflight-20261029-172534/build-check/command.json)、[25 JVM逐方法](c6-2d-preflight-20261029-172534/build-check/jvm-summary.json)、[Lint原XML](c6-2d-preflight-20261029-172534/build-check/lint.xml)、[两APK身份](c6-2d-preflight-20261029-172534/artifact/identity.json)、[SDK命令](c6-2d-preflight-20261029-172534/artifact/commands.json)、[Runner实际反汇编](c6-2d-preflight-20261029-172534/artifact/runner-dex.txt)、[配置求值/预期拒绝](c6-2d-preflight-20261029-172534/configurations/results.json)、[原断言保持映射](c6-2d-preflight-20261029-172534/test-safety-map.json)、[源码绑定](c6-2d-preflight-20261029-172534/source-binding.json)。原始stdout/stderr/两份JVM XML/SDK stdout在对应目录；[前检](c6-2d-preflight-20261029-172534/preflight.json)锁定旧源码与APK。APK/临时DEX/密钥不入Git。


## JPEG只读复核与下一步建议

已有[C6-2A](LITHIC-C6-2A-JPEG-Admission.md)、[架构复用表](../design/LITHIC-C6-2B-Media-Import-Architecture.md)、[C6-2C版本研究](LITHIC-C6-2C-Isolation.md)及[原来源元数据](c6-isolation-20261028-113818/jpeg-version-review.json)保持。3.2.1只是待批准候选：记录tag `7b4993786d856cbccabfc0ed2f923ba000e326cc`/官方asset digest，不代表本地归档SHA或GPG已核验；本轮不联网研究、下载或native执行。旧LICENSE/IJG/BSD与实际组件分发义务须保留，不能只写“开源”。

旧五文件严格核心仍可作为薄适配输入，须保留warning拒绝、输入耗尽不补EOI、全行/finish及C局部longjmp；`jmem_budget.c`依赖3.2.0内部`JPEG_INTERNALS/jmemsys.h`，3.2.1不能直接换包/SHA即称兼容；同步FD核心没有取消回调/规范化输出合同。旧JNI、same-UID Worker、Stage V2、私有路径codec不能整包迁入，禁新Picker/媒体CAS/GC/主进程JNI/未知so。

**严格JPEG适配：NOT YET GO。** 需先通过独立隔离候选审查、明确一次设备授权及当前端侧安全门禁，再锁新版来源/许可/预算适配并另行授权；API26/36×arm64/x86_64、16KiB、完整baseline/progressive/同字节缺损、40MP Android全链内存/取消/异常恢复仍缺。旧host/旧设备证据不能替代。若没有安全可用且获授权的环境，运行阶段BLOCKED并转做V1其他稳定性工作，不重复环境研究。

M10三JPEG FAIL、C6-1原14 Android NOT_RUN、媒体资源UNKNOWN、M20平台FAIL/UNKNOWN、M22/C3及M13-T06/Q03/Q04/G1～G4全部历史状态不改。无设备/安装/清理，无新用户APK、Preview递增、main或M23。

状态：**C6-2D SAFE EXECUTION CANDIDATE — READY_FOR_INDEPENDENT_REVIEW**；**ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED**。提交后停止，等待独立审查，不自动进入运行或严格JPEG适配。

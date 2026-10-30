# LITHIC V1-A1 独立综合验收候选准备

2026-10-30；`codex/lithic-v1-product-completion`。开始时实际HEAD与远端均为已审基线`bc75c8c91a54d7715311829c6b475d24d620b3bd`，工作区干净；按[Release Readiness](LITHIC-V1-Release-Readiness.md)执行。**静态交付门禁满足，可提交ChatGPT独立候选审查；不是设备执行、安全运行或V1发行通过。**

构建源码提交：`b9697b4afb0b1302bae2b403f6878e9c4af0d156`；Git树：`73cf57a17808732e92e256c2517533bbfe1893f2`。构建前先提交四份必要源码/配置，随后编译、审计及归档；后继提交仅补本文、当前状态和原始证据，不修改构建源码。实际产物绑定见[identity.json](v1-a1-preparation-20261030/artifact/identity.json)。

## 一对稳定候选（不是用户体验版）

|APK|实际包名/版本|字节数|SHA-256|
|---|---|---:|---|
|App|`com.cooper.lithic.v1acceptance.debug`；code1 / `0.1.0-v1-a1-debug`|15,977,970|`626c9809b7566d56583d49e4dbb43abe90d44e151a5e9357d2ca90c62f35a97a`|
|AndroidTest|`com.cooper.lithic.v1acceptance.debug.test`；合并及APK Manifest**未声明versionCode/versionName**，为现有AGP测试默认，不套用App版号|3,741,308|`1fdfc6ae8b784b8041d8182089bd1bf7af00ccc0e5d1f047de9ad9358f7e1154`|

两包均经`apksigner verify --verbose --print-certs`实际验证exit0，证书SHA-256均为原Debug证书：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。未生成密钥，不是正式Release签名。

稳定归档实际绝对路径（仅本地，APK不入Git）：

- App：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/verification-candidates/v1-a1/Lithic-V1-A1-App-debug.apk`
- Test：`E:/CODEX/.codex/worktrees/lithic-m13/砺石新APP/verification-candidates/v1-a1/Lithic-V1-A1-AndroidTest.apk`

目录在复制前不存在；排除链接/目录身份冲突后新建，两个目标文件均以exclusive-create复制，**0覆盖、复制后大小及SHA与build实物一致**。目录有“独立验收，非用户体验版”README及身份元数据。[归档回执](v1-a1-preparation-20261030/archive.json)和[复制命令/退出码](v1-a1-preparation-20261030/archive-command.json)。不使用Preview deliveries路径，不依赖Gradle临时目录作为唯一候选原件。

## 必要代码差异与静态核验

- [app/build.gradle.kts](../../app/build.gradle.kts)：新增严格`true/false`的`v1AcceptanceVerification`；启用时独立`app/build-v1-acceptance/`、固定App/Test身份及M13 Runner、仅Debug；拒绝Private Trial/M22/C3/C6、任何历史`m12T04ProcessDeath`属性及显式`m13Isolated=false`。原Debug key必须已存在，不能自动生成替代签名。
- [最小Manifest覆盖](../../app/src/v1Acceptance/AndroidManifest.xml)：仅区分名称及登记**非导出的空ComponentActivity宿主**，完整生产Manifest继续合并；不使用C6无Activity清单。原T06/T03/IME依赖目标App内宿主，不能删掉后声称仍可执行。该类本来来自生产AndroidX依赖，V1构建排除`ui-test-manifest`运行依赖；JUnit、Runner及业务测试类仍只在Test APK。
- [只读配置探针](../../app/src/androidTest/java/com/cooper/lithic/app/V1AcceptanceConfigurationTest.kt)：先核对精确App/Test包、Runner、plain Application及预期UID关系，再用ActivityScenario读取**实际Activity**宽高dp、fontScale、densityDpi等并回传；不建Owner/Room、不截图/写文件、不改系统。阶段sendStatus不是JUnit完成；ActivityScenario关闭不等于进程退出或Owner关闭。**实际运行NOT_RUN**。
- [.gitignore](../../.gitignore)：忽略独立build及稳定候选目录；APK本身亦受已有规则保护。生产`app/src/main`、原Android测试及冻结数据/领域代码均未改。

|实物检查|实际结果与证据边界|
|---|---|
|正常生产入口|APK Manifest为`LithicApplication`、正常Launcher `MainActivity`；MainActivity实际DEX含真实Application/`getAppContainer`接线，Application DEX保留Owner及ReminderRuntime初始化。[App Manifest](v1-a1-preparation-20261030/artifact/app-manifest.stdout)、[Main DEX](v1-a1-preparation-20261030/artifact/main-activity-code.stdout)。正常启动未来会创建本包业务Owner/调度，**不是零业务初始化**|
|Test唯一目标及plain机制|唯一instrumentation=`com.cooper.lithic.app.M13IsolatedRunner`，targetPackage仅V1 App，未声明跨进程targetProcesses；实际Runner DEX用`android.app.Application.class.getName()`调用super.newApplication，而非构造LithicApplication。[Test Manifest](v1-a1-preparation-20261030/artifact/test-manifest.stdout)、[Runner DEX](v1-a1-preparation-20261030/artifact/runner-code.stdout)。只在Instrumentation宿主替换，不能在plain进程启动Main冒充正常体验|
|组件/Provider/权限|App为2 Activity、1 Provider、3 Service、4 Receiver；Main导出、空宿主不导出；Startup authority/process为独立包、非导出，WorkManagerInitializer已移除，保留原Lifecycle/Profile初始器。sandbox非导出/isolatedProcess。其余导出后台组件仍受BIND_JOB_SERVICE/DUMP权限保护，完整属性见identity.json|
|权限边界|App仅原POST_NOTIFICATIONS、WAKE_LOCK、ACCESS_NETWORK_STATE、RECEIVE_BOOT_COMPLETED及本包signature动态Receiver权限；无INTERNET/照片/相机/广泛存储权限。Test仅既有AndroidX测试REORDER_TASKS权限及框架辅助Activity；它们未进入App|
|DEX与native|实际App含导航、首页/回看、三主题Provider、备份服务/恢复协调器、generation、只读封面及原隔离Service；无JUnit、androidx.test/compose.ui.test实现、M13 Runner或旅程/探针类。Test包含原T06/T03/IME及M21备份恢复等测试。8项native条目逐SHA与Preview12一致，无新JPEG/JNI/Picker|
|数据隔离边界|独立包名、无sharedUserId、独立authority已静态确认；**实际安装UID、私有目录/进程归属、plain启动与资源关闭仍NOT_RUN**，不能以Manifest推定端侧已安全|

## 实际执行结果

构建使用已安装JDK17.0.20.1+1、SDK36/build-tools36.0.0及缓存Gradle8.14.3；构建前可用RAM6.929GiB，单worker、offline、no-daemon。无依赖/SDK/NDK下载或升级，无Gradle clean。

|检查|结果|
|---|---|
|`:app:assembleDebug :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest :app:lintDebug`，显式`-Pv1AcceptanceVerification=true`|一次组合构建exit0，8m59s；App/Test均成功打包，AndroidTest Kotlin编译成功。[命令/退出码/原始流SHA](v1-a1-preparation-20261030/build/command.json)、[stdout](v1-a1-preparation-20261030/build/stdout.txt)、[stderr](v1-a1-preparation-20261030/build/stderr.txt)|
|Lint|**0错误14警告**，[原XML](v1-a1-preparation-20261030/artifact/lint.xml)。原SDK XML版本提示、弃用/旧源码警告保留，不借此升级工具或改历史模块|
|Gradle配置矩阵|**15项符合预期**：7项身份成功（V1 true/false、无flag、Private Trial/M22/C3/C6），8项安全拒绝（非法布尔、4身份冲突、进程死亡属性true/false、plain=false）。拒绝命令exit1为预期保护，非产品测试FAIL；原始退出码/输出均保留。[逐项结果](v1-a1-preparation-20261030/parameter-results.json)|
|默认身份及Release|无flag/显式false：Debug仍`.debug`/code17/`0.1.0-m22-debug`，Release仍`com.cooper.lithic`/code17/`0.1.0-m22`；Private Trial仍`.preview.debug`/code28/preview12，其他验证身份保持。V1实际variant只有`debug`、没有assembleRelease任务；未构建任何Release或用户Preview|
|SDK实物审计|9条aapt2/apkanalyzer/apksigner命令均exit0；实际Manifest/签名/DEX已核对，[命令及流SHA](v1-a1-preparation-20261030/artifact/commands.json)。原始字节以局部.gitattributes禁止换行转换保存|
|JVM业务套件/Android执行|本轮未改业务逻辑，不重跑原180及其他历史JVM；**全部Android/Compose/隔离/备份恢复方法NOT_RUN**。仅配置断言及源码编译，不是JUnit或设备PASS|

## 历史保护、测试范围与停止点

[前检](v1-a1-preparation-20261030/preflight.json)及[后检](v1-a1-preparation-20261030/final-preservation.json)：426份既有生产/原Android测试/schema/Gradle边界文件SHA保持；11个用户交付APK大小/SHA全部保持。Preview12仍15,977,934字节、SHA`4b6b3ed03674fc971b67a124171fedbb89fce155d2c6fd181ed597cde6250a1b`，本轮先独立验签与原证书相同。C6 App临时build APK**仍缺失**，未重建或用旧SHA冒充实物；C6 Test仍3,737,008字节/SHA`e4831773c2397c07b49f6672348161918a5048b51c7563c2c12dcce57fed5d87`，未改。归档两包亦已重新核验；源码绑定与字节一致不代表运行通过。

未来仍须另获指定设备、逐方法及设置/恢复范围授权：原`CompleteJourneyTest#runtime_offline_journey`（UUID合成账本、失败保留/成功条件清理）、原`LongHistoryPerformanceTest#full_twenty_year_dataset_reads_exact_global_facts_and_remains_responsive`（1000/1000实体、20000/10000事实、seed20261023、120秒仅全量计算）、原系统200%/359～361dp IME方法、只读配置探针。本轮原方法/样本/超时/离线合同/历史失败均未改；这些方法各为**NOT_RUN**。

M13生命周期及现有备份恢复测试仅编译；T06受控文件往返不等于真实Main/SAF。旧`RealSafBackupExportTest`、`RealSafRestoreTest`及进程死亡测试固定原Debug身份/真实Application，**不可直接在本候选plain宿主运行**，不改其断言凑覆盖。完整Test APK仍含历史方法，未来只能选择独立审查后的白名单，不执行整类/整包。正常Main与plain阶段必须分开，关闭证据UNKNOWN时不强停、不转入下一阶段；详细执行单引用已审Readiness，不在本报告复制。

本轮ADB/设备探测、安装/Instrumentation、模拟器/手机/真实数据操作均0；无Preview升级或新用户包。Room5、backup4、Owner/generation、恢复Token/原子切换、金额/订阅/提醒/报告规则、C9-R1及C6冻结路径保持。M10 JPEG、M20平台FAIL/UNKNOWN、M22、C3、M13及G1～G4历史门禁不改。报告提交后停止，等待ChatGPT独立候选审查，不开启设备执行、JPEG整合、main或M23。

**V1-A1 CANDIDATE — READY_FOR_INDEPENDENT_REVIEW**

**V1_RELEASE_NO_GO / ANDROID_ISOLATION_RUNTIME_PENDING / MEDIA_IMPORT_BLOCKED**

# M22-T06 失败现场保留整改

**READY_FOR_REVIEW — M22 T06 FAILURE PRESERVATION**。仅桌面实现、编译及静态核验；M22整体仍 **BLOCKED / CHANGES REQUIRED**，T06未重新运行，修复为 **IMPLEMENTED / PENDING_ANDROID_VERIFICATION**。

来源`e90d288f089049abfa702a16c2cae1b1cbcb7cfc`，分支`codex/m22-experience-accessibility`。开始时本地HEAD匹配锁定来源、工作区干净；提交前核对远端仍一致。唯一测试源码改动为`CompleteJourneyTest.kt`；生产代码、Gradle/依赖、Room5/backup4、冻结算法、原Runner及其他测试源码均不改。历史三次T06 FAIL、原137 JVM/36 Android PASS和全部UNKNOWN/NOT_RUN保持。

## 关键修改与清理许可

原`@After`在测试失败后仍执行`root.deleteRecursively()`；这是静态源码确认的缺口，不伪称本轮执行过Android RED。[原输入SHA与实物基线](m22-t06-preservation-20261023-142053/baseline.json)。

1. 单一外层`RuleChain`包住Compose Rule，只有整个`base.evaluate()`成功返回后才考虑删除，覆盖`@Before`、原测试断言、`@After`以及Compose/Activity自身收口。初始化完成、原`COMPLETE`终点已到达、Owner关闭、teardown完整完成四个标记必须全部成立。相同类中的另一非T06方法不取得删除许可，默认保留。
2. `@After`自身不删除文件。逐项尝试撤UI、清ViewModelStore、等待捕获的协程Job结束，再关闭本轮Owner；恢复前捕获旧导航嵌套store Jobs，结束时捕获新store，避免只取消、不等待。核对初始代与关闭前后已观察代数据库均`!isOpen`，不再以`closing == null`当作关闭证明。关闭失败或无法核验Ready代时保留UNKNOWN。
3. 原断言/初始化/Compose异常原样重新抛出；关闭异常作为suppressed附加，不能替换原失败。诊断上报失败只附加/记录UNKNOWN，不能触发删除。进程在成功许可前直接终止时，不依赖`@After`必定执行，成功删除路径尚未到达，默认保留现场。
4. 清理仅接受本轮UUID的直属`noBackupFilesDir/m22-ui-journey-<UUID>`，精确Owner标记、原目录device/inode、当前UID、canonical路径都必须匹配。先以不跟随链接的遍历验证整棵树的归属和普通文件/目录身份，再开始删除；拒绝链接、跨界或替换。不开默认generation/WorkManager，不遍历兄弟目录，不删除截图、历史缓存锁或旧现场。

**真实边界**：逐项文件删除不是整树事务。仅在完整成功许可之后、已经开始清理时发生I/O失败或进程终止，可能留下部分清理残余；不能承诺该时点仍保留整棵树，也不能声称清理成功。原业务失败、初始化不全、`@After`部分失败、Owner未知均在首次unlink之前阻断。真实关闭、取消及文件系统行为本轮全部NOT_RUN。

## 静态分支核验（不是运行测试）

| 情况 | 静态核对结果 | Android运行 |
| --- | --- | --- |
| 完整成功 | 整体Rule返回＋四标记＋关闭/身份检查后才允许清理 | NOT_RUN |
| 业务断言、定位超时、备份/恢复/金额失败 | base失败直接保留，原异常重新抛出 | NOT_RUN |
| `@Before`未完成 | 初始化标记不成立/异常路径；已有Owner仍尽力关闭 | NOT_RUN |
| `@After`或Owner关闭部分失败/UNKNOWN | 后续释放仍尝试，teardown失败阻止删除 | NOT_RUN |
| Compose/Activity收口失败 | 最外层收到失败，不能取得清理许可 | NOT_RUN |
| UUID、marker、canonical、UID/inode异常或链接 | 首次unlink前拒绝 | NOT_RUN |
| 取消或成功许可前进程终止 | 无无条件finally删除；不声称After一定执行 | NOT_RUN |

[静态分支及源码比对](m22-t06-preservation-20261023-142053/static-review.json)确认：原T06正文仅新增“恢复前记录旧VM Jobs”和“原完整终点后设成功标记”，去除这两行后原业务正文逐字一致；金额、日期、媒体、备份恢复断言及超时不变，另一测试方法和原诊断/截图辅助正文也不变。T03源码、20年规模和120秒预算未修改。没有新增运行测试或把分支数量当作JUnit覆盖量。

## 构建与候选身份

仅执行`-Pm22DeviceVerification=true -Pm13Isolated=true :app:assembleDebugAndroidTest`，使用原JDK17/Gradle8.14.3/AGP8.13.2/Build Tools36.0.0、offline、单worker及1GiB Gradle堆，沿用已有身份核验init脚本；未运行App打包、clean、Lint、JVM或设备任务。**Gradle exit0，205.562秒，51任务中5执行/46已最新**。原SDK XML版本warning及编译warning原样保留。[构建回执](m22-t06-preservation-20261023-142053/commands/test-build/command.json)、[完整stdout](m22-t06-preservation-20261023-142053/commands/test-build/stdout)、[stderr](m22-t06-preservation-20261023-142053/commands/test-build/stderr)。

| 实物 | 桌面核验 |
| --- | --- |
| 新Test候选 | `app/build/m22-device-verification/outputs/apk/androidTest/debug/app-debug-androidTest.apk`；**3,403,324 bytes**；SHA **`12e17f01be03677501a753b1992badffdfaece0f4db1742ba27b498afc1dae26`** |
| 原独立App | **13,822,252 bytes**；SHA **`b749b771e4f6f9cebcb7355c95834b7ae2eb4ba4f7528536a92bf9e09eefb2d3`**，构建前后未变 |
| 历史已安装Test身份 | SHA **`c3791305aed28899f8df98dc7c89f27c986c022b84a0e528c68c59ebf9f2037f`**；原实物已按SHA本地保全，不入Git，不把新候选冒充同一字节；本轮未查询手机安装状态 |
| Test包/target/Runner | `com.cooper.lithic.m22verify.debug.test` → `com.cooper.lithic.m22verify.debug`；`com.cooper.lithic.app.M13IsolatedRunner`，plain Application源码未改 |
| 签名 | 新Test apksigner exit0，v2有效；原Debug证书SHA **`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`**，未创建密钥 |
| 打包静态核验 | 11份DEX中实际存在新清理/保留方法与原测试/Runner；T06全部declared descriptors无ImageDecoder引用；二进制Manifest及6个assets与旧Test逐字节一致。普通Debug App/Test亦保持原SHA。仅静态，不代表Android类加载或执行通过 |

[APK/DEX/旧产物对照](m22-t06-preservation-20261023-142053/artifact-audit.json)、[新Test签名](m22-t06-preservation-20261023-142053/commands/new-test-signature/stdout)、[实际二进制Manifest](m22-t06-preservation-20261023-142053/commands/new-test-manifest-relative/stdout)。

首次Manifest检查使用中文绝对路径，aapt报告Illegal byte sequence/exit1；换为同一APK的相对路径后exit0，不重建或替换APK。另一次外层查看日志遇GBK打印编码exit1；原始命令字节、非零回执和SHA保持，不将工具输出问题冒称业务FAIL或抹除。[首次aapt失败](m22-t06-preservation-20261023-142053/commands/new-test-manifest/stderr)、[分层说明](m22-t06-preservation-20261023-142053/inspection-notes.json)。

本轮ADB/手机/模拟器/安装/Instrumentation/设置/数据清理均 **0**。137 JVM/36 Android历史套件未重跑，T06/T03/真实字体IME均NOT_RUN。手机旧M10与M22默认generation/WorkManager、此前Owner/任务UNKNOWN完全未处理。后续必须先经ChatGPT独立审查，再取得新的明确设备授权，才能更新Test并恢复T06；不冻结M22、不合并main、不进入M23。

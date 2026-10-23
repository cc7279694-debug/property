# M21 — 隔离Android补验

2026-10-23；**READY_FOR_REVIEW — M21 / 独立PENDING**。来源`6c937ed7354b77d598bdeb55718a39708f572ee8`，分支`codex/m21-milestone-reminders`。**31个唯一方法首轮31 PASS，0 FAIL/UNKNOWN/NOT_RUN，执行31次、重跑0**；此前395 JVM PASS保持，本轮未重跑。没有产品代码修复，不授予冻结或G3通过。

## 测试及实际范围

| 独立Instrumentation顺序 | 方法数/实际结果 | 原始命令、stdout/stderr、退出码及Runner完成状态 |
|---|---:|---|
| M21MilestoneSchedulingTest | 1 PASS | [原始运行](m21-android/commands/test-00-instrument/result.json)、[逐方法](m21-android/tests/class-00.json) |
| M21MilestoneWatermarkTransactionTest | 10 PASS | [原始运行](m21-android/commands/test-01-instrument/result.json)、[逐方法](m21-android/tests/class-01.json) |
| M21MilestoneWorkerTest | 7 PASS | [原始运行](m21-android/commands/test-02-instrument/result.json)、[逐方法](m21-android/tests/class-02.json) |
| M21NoticeIntentTest | 4 PASS | [原始运行](m21-android/commands/test-03-instrument/result.json)、[逐方法](m21-android/tests/class-03.json) |
| M21MilestoneSourcesUiTest | 4 PASS | [原始运行](m21-android/commands/test-04-instrument/result.json)、[逐方法](m21-android/tests/class-04.json) |
| M21DatabaseMigrationTest | 3 PASS | [原始运行](m21-android/commands/test-05-instrument/result.json)、[逐方法](m21-android/tests/class-05.json) |
| M21BackupRestoreTest | 2 PASS | [原始运行](m21-android/commands/test-06-instrument/result.json)、[逐方法](m21-android/tests/class-06.json) |

[31项唯一结果及操作统计](m21-android/results.json)。每类外层exit0，JUnit分别`OK (N tests)`，Runner完成码−1；逐项成功状态0与预期class/method完整对应，非仅依据外层exit0。各运行目录的`stdout`/`stderr`为原始字节，`result.json`含长度与SHA。本轮没有首次JUnit FAIL或定向重跑；上轮所有RED和NOT_RUN原件不改写。

真实Android已验证：生产4→5迁移及原九表逐行保持、schema5重开、同UUID类型主键与删除隔离、原始超Int掩码拒绝；真实Room新建/纠错和水位共同回滚；历史初始化完整且无假Receipt、普通续费与补录分流；同日两类一条摘要与整批Receipt插入失败回滚；旧代/纠错/删除来源拒绝；format4十表/设置/图片原字节往返、真实旧schema4/proof3先认证后保护，以及构造SWITCHING状态的重开恢复。**后者不是实际杀进程或断电。**

调度类在第一个独立plain Application进程运行，先确认无既存WorkManager，再用内存TestDatabase/TestDriver/CountingWorker验证上海08:30→09:00的30分钟延迟及只取消自有里程碑候选，合成续费候选保留。其余Worker通过真实`doWork()`入口、真实Room、受控时钟及合成Publisher验证；没有生产24小时周期任务、系统通知发布或系统送达PASS。UI为隔离ComponentActivity/合成resolver，不是MainActivity或真实通知点击。

现有31项没有Android专门输入覆盖：D1000/所有日历周年/闰日；订阅预付首日与暂停/免费/停止的全部有效日边界；+6/+7补发/跨午夜/多时区；proof1/2实际恢复。这些保持此前JVM证据及本次Android **NOT_RUN**，不能由31 PASS扩大为所有12项核心核验的全部子边界均已设备验证。系统权限拒绝、真实系统发布/点击/后台重启、API26/OEM/完整TalkBack仍NOT_RUN，原Q03完整跨模块、Q04及G1/G2/G3不关闭。

## 设备、构建及身份

仅创建/启动/正常退出 **各1次**：`Lithic_M21_API36_20261023 / emulator-5712`，API36/x86_64，2核/headless/no-snapshot；独立注册、userdata、cache、临时和快照均在全新`E:/CODEX/.codex/research-env/lithic-m21-android-20261023`。初始没有任何运行中模拟器，新设备两目标包不存在；没有旧数据、账号或快照导入。全部59次设备命令均显式`-s emulator-5712`（57条原始命令＋2条只在内存处理、隐私过滤的JobScheduler观察），历史设备操作0。[主机身份](m21-android/device/host-admission.json)、[设备准入](m21-android/device/device-admission.json)、[安装回读](m21-android/device/installed.json)。

配置与启动参数请求2048MB，但既有官方Emulator37.1.11明确自动提高为 **2560MB**；不冒充2048MB实际运行，没有重启、换工具或扩大下载。[实际hardware](m21-android/device/hardware-qemu.ini)。官方创建exit0，首次本地取证误以为配置位于`-p avd-data`，只读真实注册后采用同一新根内实际生成的AVD路径，没有重复创建；另一次严格RAM断言被工具自动调整触发，均在任何ADB/安装前，见[创建说明](m21-android/device/creation-admission.json)、[RAM原日志](m21-android/device/emulator.stdout)。这些是取证接缝/实际资源偏差，不是JUnit RED或虚构旧PASS。

仅改一个androidTest文件：保留原4方法/断言，增加显式captureRun截图保存、具体ComponentActivity和同UUID场景的360dp/LocalDensity200%包装与准确行滚动。[源码范围核对](m21-android/test-source-scope.json)。App始终未重建/替换；Test一次构建、一次安装，无追加安装。定向Test构建＋Lint exit0 / **6m47s**，Lint **0错误25警告**，不运行JVM。[构建原件](m21-android/build/result.json)、[日志](m21-android/build/gradle.log)、[Lint计数](m21-android/build/lint.json)。

| 实际安装产物 | SHA-256 / 身份 |
|---|---|
| App，13,805,852B | `5f7c585e5e117bc7e77d5f2915429ac1eb8f4ac6b9480ad77aa8fa13529b1c62`；`com.cooper.lithic.debug`，code16 / `0.1.0-m21-debug`，UID10216 |
| 本轮Test，3,101,774B | `feab8f299a1e261731e197e2642f0ffa1f5ba136df9198551e6e655e80f7c5af`；`com.cooper.lithic.debug.test`，UID10217，Manifest未声明版本 |

两包v2签名成功，同Debug证书SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，实际设备回读与结束回读SHA/UID均一致。Runner `com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`。旧Test `5b75…`保留为上轮离线身份，不覆写原报告。APK/密钥不进Git。

## 截图、结束及限制

4张真实新设备截图：[51项来源](m21-android/screenshots/01-summary-sources.png)、[两种类型/360dp200%](m21-android/screenshots/02-typed-sources-local360-200.png)、[订阅行滚动后](m21-android/screenshots/02-subscription-local360-200.png)、[失效明确提示](m21-android/screenshots/03-stale-source.png)。[逐图尺寸/SHA/UID/PID/来源](m21-android/screenshots/index.json)；前后图1080×1920，中两图945×1920。**LocalDensity2模拟，不改变系统font_scale=1.0，不冒充系统大字、MainActivity或通知栏截图。**

所有测试Owner按finally/After关闭；13个已登记磁盘根通过capability/UID/inode/白名单检查后报告REMOVED，10个内存事务方法关闭数据库，App最终独立pidof无进程。没有默认active-generation/真实账本或WorkManager数据库；目标OS Job观察未见对应Job身份，包名的inactive accounting不误当活动Job。[结束Owner证据](m21-android/device/end-owner-proof.json)、[完整合成元数据](m21-android/commands/end-full-owned-metadata/stdout)、[目标Job观察](m21-android/device/job-observation-final.json)。

**保留15个零字节`cache/data/data/…/data.db.lck`文件**，UID10216、单链接，路径均带本轮合成根/代身份。它们不在清理登记内，来源机制及锁状态未单独认证，故保留、不删除，不宣称缓存已全清；也不凭文件名或单次PID缺失推断FD安全。另保留已关闭的截图文件和AndroidX `profileInstalled`。不存在未收口的测试业务根，不把缓存残留默写成CLOSED。

只正常关闭新5712；原launcher7600/QEMU12888及5712/5713端口均结束，[退出证明](m21-android/device/exit-proof.json)，保留整个新AVD。QEMU截至退出前OS累计PeakWorkingSet **3,923,120,128B≈3.654GiB**；退出尾段/整机同时峰值NOT_CAPTURED。新根NTFS文件物理约 **3.804GiB**（4,084,722,869B），逻辑4,148,554,933B；目录元数据与外部工具缓存开销NOT_CAPTURED。[资源口径](m21-android/device/resources.json)。

M20额外平台FAIL/第二次收口UNKNOWN及5704现场完全未接触；M10延期/三JPEG历史FAIL、M13-T06延期、Q04、G1/G2/G3及全部历史NOTES保持。原395 JVM与上轮31 NOT_RUN属于各自历史，不改写。本轮提交后停止，等待ChatGPT独立复验，不冻结、不合并main、不进入M22。

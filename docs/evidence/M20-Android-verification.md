# M20 — 低资源 Android 补验（2026-10-22）

状态：**READY_FOR_REVIEW — M20 / 独立PENDING**。候选来源`4353d71aecc23b169aab0f67aab7d4ccaaa845af`，分支`codex/m20-renewal-notifications`；原22项最终通过，额外平台方法FAIL及第二次平台任务显式收口UNKNOWN保留，不授予独立冻结结论。

## 范围与环境

仅新建`Lithic_M20_API36_20261022 / emulator-5704`，API36/x86_64、2048MB、2核、headless、`-no-snapshot`，复用既有Google APIs镜像。创建1次、启动1次；独立注册、数据、临时及快照目录位于`E:/CODEX/.codex/research-env/lithic-m20-android-20261022`。未向历史设备发送命令、未打开MainActivity默认账本、未读取私人内容。

完整启动身份、父子进程/创建时间、端口与可写路径见[主机证明](m20-android/device/host-instance-proof.json)、[设备准入](m20-android/device/device-admission.json)。[启动外层原记录](m20-android/device/start-outer-note.json)保持；最终取得[监督器终结元数据](m20-android/device/start-terminal-receipt.json)：exit1/TimeoutExpired、配置期限90秒，原stdout/stderr字节NOT_CAPTURED。设备独立准入PASS不改写该失败，也没有第二次启动。

## 改动与实际结果

四类测试增加本次独占根、nonce、UID/dev/inode及明确路径白名单；数据库、设置、存储Owner确认关闭后才清理，未知或身份变化保守保留。原22个测试方法和业务断言保留。首轮实际**21 PASS / 1 FAIL**：平台渠道诊断使用Test Context包名，而Instrumentation执行UID属于目标App；仅修正该测试的Context，不修改产品代码。[原22项逐方法记录](m20-android/tests/original/summary.json)及首轮失败保持。

| 原22项 | 方法数 | 最终执行结果与输入口径 |
|---|---:|---|
| 完整备份与恢复 | 4 | 首轮PASS：真实format1/2升级、format3九表/Receipt/偏好/媒体往返、截止水位与旧proof恢复 |
| 生产迁移及重开 | 2 | 首轮PASS：1/2/3→4、真实重开和Receipt唯一键 |
| 设置持久化 | 1 | 首轮PASS：两偏好、截止水位及同Owner重开 |
| Compose设置 | 7 | 首轮PASS；含拒绝提示、错误状态、返回和LocalDensity大字可达性 |
| 设置ViewModel | 2 | 首轮PASS：独立保存、旧代拒绝 |
| Worker与业务保护 | 6 | Context修正后6项定向复验PASS，含跨类型同UUID保护、去重、权限拒绝、旧代/旧revision拒绝及真实平台渠道读取；[逐方法记录](m20-android/tests/worker-recheck/summary.json) |

原22项最终唯一方法统计：**22 PASS / 0 FAIL / 0 NOT_RUN**。证据由首轮未受影响16项和Context修正后Worker6项组成，不能声称同一Test SHA一次跑完22项；首轮21 PASS/1 FAIL保持历史原状。[最终52个原测试编译类字节核验](m20-android/platform-fix-install/original-class-proof.json)全部不变。全部25个唯一Android方法的最新结果为**24 PASS / 1 FAIL**；实际32次执行为**29 PASS / 3 FAIL**，不把重复执行累加为唯一测试数。[统一结果](m20-android/results.json)。

额外三个唯一方法独立统计，不计入原22项：

| 额外方法 | 当前实际结果 |
|---|---|
| 截图旅程 | PASS；[真实执行](m20-android/tests/gallery/summary.json)，取得四张隔离Compose截图 |
| 真实平台队列及通知 | 两次JUnit均FAIL：末尾通知窗口可见性谓词10秒超时；未删除断言或延长期限。[初轮](m20-android/tests/platform/summary.json)、[修正多窗口查询/namespace后复验](m20-android/tests/platform-recheck/summary.json)原样保留。实际生产持久队列、独立OS namespace Job、通知发布、Receipt及业务事实不变子断言通过，且取得真实SystemUI截图；这些事实不能替代整方法PASS。第二次任务的显式Owner关闭和未来周期任务收口UNKNOWN，ENQUEUED周期及原现场保守保留 |
| 旧平台任务严格收口 | PASS；[独立执行](m20-android/tests/completion/summary.json)，只核验原确定UUID、取消精确通知/periodic、确认无新Worker并重开原合成账本核对后关闭，不重新发布或新建账本，不改写原平台FAIL |

真实平台证据见[初轮JSON](m20-android/platform-observation/platform-result.json)、[目标OS Job](m20-android/platform-observation/owned-jobs.txt)、[第二次保留结果](m20-android/platform-final-observation/retained-result.json)、[保留文件索引](m20-android/platform-final-observation/retained-files.txt)及[第一次任务收口记录](m20-android/platform-final-observation/old-completion.json)。原Worker中的模拟Publisher只证明相应业务决策，不代表系统通知送达。系统发布调用、通知栏可见与用户实际收到是不同结论；不将模拟器退出推断为应用Owner/周期任务已明确收口。

## 产物、构建与截图

App始终为`com.cooper.lithic.debug`，code15 / `0.1.0-m20-debug`，13,691,164B，SHA-256 **`776c00167dd856d39791fca4c9331fdfaf313dab096b2835df6e40b3a66fda38`**。未修改`app/src/main`。

首轮Test为`com.cooper.lithic.debug.test`，2,947,570B，SHA **`7b543b3bc7ea364d9ee6ecf6a6d991cd43180f31c092871f44f427816636c008`**；Context修正版2,947,710B，SHA **`95f31593c9ed81ea9c2372fa31ecd84ea86422bd936768397efcaef3c2848ec0`**，用于Worker6项复验及额外旅程/平台首轮。最终Test为2,962,518B，SHA **`815608676350947f9e720af5e8c4647a8505a8f4540c86b07f3517b45776f408`**；[最终安装回读](m20-android/device/installed-platform-correction-proof.json)验证App/Test UID10216/10217及原Debug证书`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`。App安装1次，Test首次安装1次＋同签更新2次，原身份不覆盖。使用`com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner`及plain Application隔离宿主。

此前238 JVM PASS原证据保留，本轮未重跑；新增监督器定向宿主12项PASS。[最终Test打包/Lint](m20-android/platform-fix-build/exit.json)退出0、构建4m30s、[Lint](m20-android/platform-fix-build/lint-results-debug.xml)0错误25警告；[宿主结果](m20-android/host-final/result.json)。早期CLI失败、中文路径AGP打包状态污染及全DEX重新打包证明均保留于[构建](m20-android/build/)、[首轮构建](m20-android/build-final/)、[DEX证明](m20-android/repackage/final-dex-proof.json)。aapt中文路径失败、缺包pm path退出1和结束核验UID子串解析失败等原始记录保留，未据此重复安装、创建或启动。

测试claim中的源码SHA是实际Windows构建工作树字节；Git文本会归一化CRLF→LF，八份源码的[双侧SHA与内容核对](m20-android/source-eol-proof.json)明确这一差异，不替换原校验值。原始取证目录采用`-text`，提交字节与本地原记录一致。

四张设置/拒绝提示/大字截图及SHA见[回读索引](m20-android/screenshots/gallery/source.json)、[宿主说明](m20-android/screenshots/gallery/gallery-source.json)。前两图同SHA，因此是3个独立画面；截图时实际未授予权限、生产Publisher拒绝发送，未主动打开系统权限对话框。设置截图为隔离ComponentActivity；360dp/200%是LocalDensity画布，不能称为MainActivity、系统字号或TalkBack。[最终真实SystemUI通知截图](m20-android/platform-final-observation/real-notification.png)178,879B，SHA-256`51c707998a0c986e50fb4d437db24471b06a669ba013e4562542dbf0ccb384ac`；合计最终5个PNG文件、4个独立画面，另保留旧平台截图。真实文案可见不改写可见性谓词FAIL。

## 结束保护与限制

最终App/Test字节、UID、原boot/AVD及设备设置复核见[结束命令索引](m20-android/end-final/commands.json)。只向新5704执行一次正常退出，原launcher/QEMU进程和5704/5705端口均结束，[退出证明PASS](m20-android/end-final/exit-proof.json)。AVD和第二次平台任务UNKNOWN现场保留；未停止或清理其他设备。创建/启动/正常退出各1次。

[实际资源](m20-android/end-final/resources.json)：QEMU生命周期PeakWorkingSet64 **4,012,032,000B（约3.74GiB）**，launcher峰值14,159,872B；两者同时合并峰值NOT_CAPTURED。新研究根NTFS文件物理占用4,336,395,219B（约4.04GiB），逻辑长度4,400,227,283B；目录元数据及系统其他开销NOT_CAPTURED。2048MB仅guest RAM配置，不是Windows进程或整机内存硬上限。

真机/API26/OEM、Release/稳定签名、真实重启后的任务执行、真实系统200%/完整TalkBack、真实午夜/断电/LMK及最坏后台送达时延仍NOT_RUN。成功发布后到Receipt持久化之间的崩溃重试窗口、系统节流与OEM行为保留NOTES。本轮不关闭Q04、G1/G2或16组完整跨模块门禁；M10 PENDING / DEFERRED及三JPEG历史FAIL、M13-T06 NOT_RUN / DEFERRED、所有旧RED/FAIL/UNKNOWN/NOT_RUN/NOTES不改，M21未授权。

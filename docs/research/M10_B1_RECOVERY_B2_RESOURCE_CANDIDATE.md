# M10 B1 实际收口修复与 B2 资源验收候选

2026-10-19。来源 `f1ca58b286381a053c551e6b0383d5141e3b9565`；分支 `codex/m10-b1-recovery-b2-resource-candidate`。本次仅离线代码、宿主测试及必要构建；**设备操作全部为0**。正式M10仍PENDING，原三JPEG历史FAIL不回写；18项自然流程不重跑，UI-C1 FROZEN、M11 NOT_AUTHORIZED及全部历史NOTE/UNKNOWN保持。候选未合并正式M10。

## 1. 实际改动及验证边界

- **原会话接续清理**：[Duty](../../app/src/main/java/com/cooper/lithic/data/media/jpegworker/JpegBindingDuty.java)关闭新RPC准入，只有真实bind返回/匹配RPC实际返回才能唤醒一次固定continuation，回调在短锁外执行。[Client](../../app/src/main/java/com/cooper/lithic/data/media/jpegworker/JpegWorkerClient.kt)保留原Context/ServiceConnection/attempt，派发到主线程解绑；迟到异常返回同样承担责任。取消Future、解绑、一次PID缺失均不替代消费者结束；主线程派发/解绑未知不释放责任。迟到收口只发布原失败，不接纳迟到SUCCESS或再次提交。
- **无结构化结果的独立非成功闭环**：[Stage](../../app/src/main/java/com/cooper/lithic/data/media/jpegstage/JpegStageV2.kt)仅在fresh create登记`supervisor.v2`，不回填旧源。恢复全程持同一namespace/consumer guard租约：先无扰动资格校验→持久revoke回读→再次核验进程/会话和完整文件身份→独立`abandoned.v2`→幂等已知payload清理。首次拒绝不撤销原授权；撤销后最终证明失败保pin/UNKNOWN。ABANDONED没有decode outcome，不能recordPrepared，不补造terminal/结果/Worker FD回执。owner/源SHA、device/inode、output/guard、未知子项、同代资源及v1保护保持。
- **可信恢复适配**：[Verifier](../../app/src/main/java/com/cooper/lithic/data/media/jpegstage/JpegRecoveryVerifier.kt)区分原Session的完整静默证明和重启后准确旧Host/Worker都结束；同时核对UID、PID/startTicks、boot及主进程角色。跨boot仅接受严格递增的[BOOT_COUNT（公开API24）](https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT)，编码成协议UUID形状；它不是随机OS boot UUID或授权nonce，不写全局设置。缺失/倒退/未知格式/权限错误均拒绝。[Store](../../app/src/main/java/com/cooper/lithic/data/media/LocalImageStore.kt)接入失败收口及新JPEG准入前恢复，证明未知仍映射RESULT_UNKNOWN。进程/FD的真实系统行为尚未由本轮验证。
- **B2可执行入口**：[三项Android测试](../../app/src/androidTest/java/com/cooper/lithic/data/media/M10JpegResourceEvidenceTest.kt)使用真实Stage→Client→native严格消费→原Android规范化→认证退休→terminal/清理，不写Room或正式封面。[Debug诊断](../../app/src/debug/java/com/cooper/lithic/data/media/jpegworker/JpegResourceDiagnostics.kt)须匹配独占合成permit，记录六个资源检查点、native16、Host/Worker身份/FD及CPU/RSS/PSS；[Release同名类](../../app/src/release/java/com/cooper/lithic/data/media/jpegworker/JpegResourceDiagnostics.kt)为空实现，无诊断解析/文件能力，无新权限/IPC/依赖。正常和异常操作结果与资源收口分别记录，清理成功不使解码失败变PASS。

### 尚不能自动恢复的准确条件

原永久未返回RPC、主线程无法派发/解绑、revoke或guard未知、准确Worker身份缺失（包括可能reserve但未pin）、旧V2无supervisor、源尚未完成发布、错代/损坏owner/inode/未知子项、boot证明未知、原Host仍存活但无同Session静默证明，均保留阻断。不按mtime猜删，不清用户数据，不补发第二消费者。迟到通知只尝试一次终结核验，未取得完整证明不承诺后台无限重试；新的进程恢复仍须满足全部门禁。新私有记录对旧候选是未知子项，降级会保守阻塞，不能称可透明降级。

## 2. 实际离线结果

| 范围 | RED / GREEN及统计口径 |
|---|---|
| B1真实Java责任组件 | [RED](m10-b1b2-candidate/evidence/worker/red/RESULTS.json)新增11断言失败，javac成功；[GREEN](m10-b1b2-candidate/evidence/worker/green-b1-final/RESULTS.json)原143＋新11＝154 PASS。最终又加强这11项的回调外错误检查，防止AssertionError只被记录；[最后154项重编回归PASS](m10-b1b2-candidate/evidence/worker/green-callback-assertions/RESULTS.json)，不新增方法/重复计数。不是Android解绑证明。 |
| Stage真实Kotlin/NTFS | [首批RED](m10-b1b2-candidate/evidence/stage/red/RESULTS.json)25中11失败；[GREEN](m10-b1b2-candidate/evidence/stage/green-25-before-preflight/RESULTS.json)25 PASS；两阶段门禁[RED 3失败](m10-b1b2-candidate/evidence/stage/preflight-red/RESULTS.json)→[GREEN 3 PASS](m10-b1b2-candidate/evidence/stage/preflight-green/RESULTS.json)。最终唯一数按后述统一回归，重复运行不相加。 |
| 新恢复授权谓词 | [16项](m10-b1b2-candidate/evidence/authority/green/RESULTS.json)，新增fail-closed stub RED 4失败→GREEN16；Probe事实为合成，Stage数据类型快照编译，不是OS死亡/磁盘恢复实证。 |
| B2解析/证据校验 | [21项真实Java](m10-b1b2-candidate/evidence/resource/RESULTS.json)，完整API stub RED21→GREEN21；最初stub SHA未登记如实保留。非40MP运行。 |
| 最终受影响JVM、Lint与构建 | [统一回归16份XML／产物审计](m10-b1b2-candidate/evidence/final/RESULTS.json)：**297唯一PASS＝旧Worker143＋旧Stage79＋新75（11＋27＋16＋21）**，0失败/错误/跳过。Lint **0错误/10警告**。首次统一构建32m59s成功；之后只补B2测试失败收口，App/JVM输入逐一不变，测试包重编与Lint3m13s成功，未重复297项。再后仅上述新JUnit11项加强断言并定向154 PASS，主程序/Debug/Release/androidTest及APK输入不变，原XML保留并单独登记补测，不冒充重新完整Gradle运行。Debug两APK与Release Kotlin编译PASS，未构建Release APK。 |

原Worker143文件、Stage79旧方法正文、四个原Android类及三JPEG断言保持；Room/schema/DAO/业务算法/UI/Manifest/Gradle/native源码无改动。仅重跑受影响JVM/Stage；旧86业务JVM、旧18项自然、native压测、sanitizer、所有设备异常及B2三项 **本轮NOT_RUN**。历史证据不重新计数。

工具链沿用JDK17.0.20.1+1、Gradle8.14.3、AGP8.13.2、Kotlin2.3.10、NDK28.2.13676358/CMake3.22.1，offline、max-workers1，无下载/升级。10条是本轮离线Lint实际输出（依赖更新信息有缓存边界），不改历史23警告记录；两项Exif、OldTarget、三项依赖提示、三项旧资源与UseKtx保留。实验宿主的NTFS身份/目录sync端口、合成Probe和静态DEX审计不等同Android真实证明。

**当前唯一候选产物（未安装/提交/发布）**，完整路径均以本worktree `E:/CODEX/.codex/worktrees/lithic-b1b2-candidate/砺石新APP/` 为根：

- `app/build/outputs/apk/debug/app-debug.apk`：13,900,723B；SHA `b7c90277fa6c27e6ab314aa52c293de8e445ba6a3deba9e984ab96f4280e8a4c`；包`com.cooper.lithic.debug`。
- 必要测试包 `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`：2,688,755B；SHA `bb00c0f6e34059170d20124eab739b43ccbc257ecc265def28906077fc0cb78c`；包`com.cooper.lithic.debug.test`；component `com.cooper.lithic.debug.test/com.cooper.lithic.data.media.M10ProcessBoundaryRunner`。
- 两者证书均`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，与旧App一致；min26/target36，Worker非导出/`:jpeg_worker`，无新增Manifest/权限。App仅原signature自定义permission，Test继承REORDER_TASKS，不扩到产品权限。原App`0ada…`/Test`73d9…`实物SHA保持。
- 四ABI APK内so＝本次AGP strip输出＝旧锁定App对应so逐字相同；不能把unstripped生成库SHA直接当APK内SHA。三份原native声明SHA核对，不提交许可正文。Debug诊断Release字节码确认为NOOP；六个相关测试/采样类的DEX declared metadata API26审计PASS，**反射/加载及方法体实际运行NOT_RUN**。

小型审计脚本初稿有括号语法和aapt2字段/CRLF/布尔输出格式假设错误，随后修正；首次将unstripped SHA与APK so直接比较的检查也停止，最终分别核对生成库、strip输出和旧APK字节。上述均为宿主审计工具整改，不是产品测试RED，不掩盖为首次审计即PASS；原始实际aapt/签名输出保留于final索引。

## 3. B2固定输入与测量口径

原件基址 `E:/CODEX/.codex/worktrees/m10-jpeg-strict-r1/砺石新APP/build/m10-jpeg-r1/`；仅校验/复用，不复制进Git/APK。

| 原相对路径 | 字节 / SHA-256 |
|---|---|
| `fixtures/large_40mp_baseline_420.jpg` | 17163380 / `c2c2fa54d49ab8155321175a66596ce8d35ed9927d9f3dfe2bbb6a00c5fbb5c7` |
| `fixtures/large_40mp_progressive_420.jpg` | 15458857 / `2b02f35f08a8cb36d31a0a4f01e8810f554be67619950c8e2b0d0d2be873c5c8` |
| **`single444/valid_40mp_progressive_444.jpg`** | 27445246 / `0555234207d5ddf84d236d812487a659f2dac34a9d82109a941628553c7b67e6` |

明确排除旧`fixtures/large_40mp_progressive_444.jpg`（SHA `ec0a7075f307f8702bd03080a8ece90ecbfafbc498162d4f1a9da05df22de700`，缺损）。新测试方法为`baseline_420_40mp_worker_resource_evidence`、`progressive_420_40mp_worker_resource_evidence`、`progressive_444_40mp_worker_resource_evidence`，全部属于`com.cooper.lithic.data.media.M10JpegResourceEvidenceTest`。

轮询100ms、最多1200轮，RSS/PSS峰值为**采样下界**；PSS可能被平台节流。proc VmHWM/native ru_maxrss是**进程出生至采集时刻**的高水位，不是退役后全生命周期峰值；最后RETIRING后尾部 **NOT_PROVEN**。包含Host/Worker PID/UID/startTicks、CPU/proc ticks、wall、诊断开销、native计费峰值/释放、FD与scratch/Stage状态。512MiB仅覆盖分配请求预算，不能宣称RSS、native堆或手机硬cap。系统Bitmap/ART/allocator驻留及采样开销另记；40MP、64MiB、合法progressive、1600px/2MiB不变。不预填任何手机安全阈值。

## 4. 下一轮精确设备授权提案（本轮未执行、不是已有许可）

**环境/包：**拟为API26/API36各创建一台全新专用合成AVD：`Lithic_M10B1B2_API26_<batch>` / `Lithic_M10B1B2_API36_<batch>`，独立`research-env/lithic-m10-b1b2-<batch>/api26|api36`；拟端口5664/5666（serial `emulator-5664` / `emulator-5666`，**未检查占用**，冲突STOP，不临时借旧设备）。只复用既有镜像/SDK，不下载。每台创建1、启动1、候选App/Test各安装1；无wipe/重试/旧AVD读取。真实API/ABI/配置/磁盘无链接越界、进程创建时间/完整路径/端口、APK字节/证书/UID和boot均匹配后才定向ADB。测试包`com.cooper.lithic.debug.test`，目标`com.cooper.lithic.debug`，instrumentation以上述实物核验值为准。

**自然资源：**上述3个方法每API各1次，串行、独占attempt；将固定三原件仅复制到新测试App私有`files/m10-b2-test-inputs`，重新核验长度/SHA/权限。采集现有nonce连续日志与私有合成诊断、独立进程/guard/terminal证据。不重跑旧18项。完整失败分类、测量完整性或retire未知即停，资源收口成功也不能把RESOURCE/LOAD等操作失败写成成功。arm64/32位、低RAM真机及HWM尾部均不由x86_64补证。

**B1 A/B/C/D/G/I异常族：**须另行批准隔离androidTest故障适配及具体门禁后才运行，当前没有冒充已存在/已运行的Android方法。A：一次调用方协程取消，验证撤销与迟到结果；B：一次可控RPC返回延迟（不得修改产品固定deadline），验证真实返回后UI清理，另一次主线程dispatch拒绝保持UNKNOWN；I：一次错session/重复回执只对同合成attempt，不能开第二消费者；G：一次active lease期间GC拒绝与终结后幂等重放。永久RPC仅维持UNKNOWN，不承诺有界结束。

**明确故障终止上限：**仅在新设备、合成owner/nonce和精确PID/UID/startTicks/boot反复核验后，C允许测试适配对自身`:jpeg_worker`执行一次`Process.killProcess(已认证Worker PID)`/API；D允许合成Host在“持久pin后、结果前”和“真实消费者结束后、receipt前”两个独占attempt分别执行一次`Process.killProcess(Process.myPid())`，共最多2次/API。执行前必须确认控制进程和目标不同、无私人/旧资源；执行后只读证明OS实例结束、Binder/FD/guard及重启恢复，不能靠kill命令成功推定PASS。正常协议Worker自退出另算自然行为。**不授权ADB kill/force-stop、包强停、外部任意PID、H2/C2/保留API36终止；本轮这些次数全部0。**B1故障适配/方法仍需下一次代码与设备范围批准，不将本报告当执行授权。

**停止/保留：**任何身份、文件归属、撤销、连接/FD、消费者结束、日志完整性FAIL/UNKNOWN立即停依赖链，无自动重跑、清数据、删除未知stage、修复设备或换串号。保存同attempt原始JUnit/退出码、连续日志/nonce、PID/UID/start/boot、FD/lease/pin、terminal/abandoned及元数据、资源采样和操作计数；不采集账本/私人图片。不要求另造正常流程数量，B1系统异常与B2数据充分后交ChatGPT决定能否冻结。

## 5. 交付结论

**READY_FOR_ANDROID_SAFETY_REVIEW**。离线代码与可构建入口已交付，证据见[小型结果索引](m10-b1b2-candidate/RESULTS.json)；本轮不授予ACCEPTED/FROZEN。B1 Android异常链与BOOT_COUNT实际可读性、B2三个40MP全链资源/失败释放仍**NOT_RUN**，M10两个系统安全门禁尚未关闭。H2 DELIVERY_UNKNOWN/pin=true、C2及保留API36状态仅继承历史，NOT_OBSERVED_THIS_TASK。只推候选分支后停止，等待ChatGPT独立审查及精确设备授权。

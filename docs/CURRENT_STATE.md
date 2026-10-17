# 当前项目状态

状态日期：2026-10-17（Asia/Shanghai；G2R-U1完成有限UID取证及最小研究兼容修复/宿主测试，等待独立审查）。本文件是当前分支的唯一实时状态；原件归档和 checkpoints 只记录历史。

## Current Stage

- **M10-R3-F1B-G2R-U1 — READY_FOR_REVIEW**：用户转交G2-R研究交付PASS WITH NOTES、真实双快照BLOCKED；本轮从锁定`f4de91342410b58d63526182a1402486e12e3990`建立独立worktree/分支`research/m10-jpeg-strict-r3-f1b-g2r-u1`。仅授权有限只读UID诊断、Python最小兼容修复、宿主测试和文档；不运行完整设备预检或任何App。
- **本轮实证与修改**：原AVD已在线，未启动/重启。API36/boot1与三包当前UID/APK SHA核对一致。一次legacy ps＋一次实际支持UID列的ps发现唯一未知ASCII标签uwb（1行），按USER/PID/PPID/NAME在内存关联数字UID1083；AOSP Android16 AID_UWB1083核对一致。仅将gate原UID规则提取共享函数并加精确uwb1083；Service/stage、verify采集协议、生产代码及原测试不变。完整进程数据仅内存，受限证据保留标签/计数/数值和命令字节/SHA；不能追认G2-R缺失的原token/SHA。
- **本轮验证与限度**：RED103唯一方法93 PASS/10 FAIL，GREEN103 PASS＝原81＋新增22，0 ERROR/SKIP，旧方法身份及断言保留；实际safe_idle仅通过FakeCollector宿主集成。受限真实观察的宿主审计PASS，不可重放私有完整行。三次取证脚本前置格式错误在ps之前停止，历史记录保留；共36次ADB读取，其中31条有逐条完整性元数据，另5身份显示无原始SHA。ps总2，安装/应用启动停止/清理/完整预检均0。数字采样研究行为空不是完整双快照或无Worker门禁证明；新Service/stage、私人元数据前后比较、私人字节、Android案例/构建均NOT_RUN。
- **U1交接与停止**：[执行](research/M10_JPEG_R3_F1B_G2R_U1_EXECUTION.md)、[测试](research/M10_JPEG_R3_F1B_G2R_U1_TEST_RESULTS.md)、[决策](research/M10_JPEG_R3_F1B_G2R_U1_DECISION.md)、[checkpoint](checkpoints/2026-10-17-M10-JPEG-R3-F1B-G2R-U1.md)。只提交推送U1分支后停止，等待ChatGPT独立审查；不因宿主全通过运行设备preflight。N01～N03、历史attach/R1超时UNKNOWN、R3原29项0 PASS/1 UNKNOWN/28 NOT_RUN、M10 PENDING/3 JPEG FAIL、UI-C1冻结PASS WITH NOTES及M11 NOT_AUTHORIZED不变。

以下为G2-R历史停止点；保留其原BLOCKED及缺失输入边界，本轮新观察不回填旧证据：

- **M10-R3-F1B-G2-R — BLOCKED，等待独立审查**：来源G2锁定`2de44c79173e7fa52841b55542e5ddcc5399a30a`，独立worktree/分支`research/m10-jpeg-strict-r3-f1b-g2-resume`。按本轮新授权先核对ADB/Windows进程/端口与F0原AVD路径、ini/config SHA及程序路径，另记录本轮工具SHA（F0无程序SHA基线）；确认未运行后只启动一次，禁snapshot加载/保存、不改配置或清数据。240秒有界等待循环实际44.766秒后确认原AVD/API36/serial/boot1（从启动请求到确认约65.805秒）。
- **唯一真实预检与停止**：原G1代码/入口未改，07:45:49～07:45:58 UTC运行一次。首轮完整服务输出分类NO_TARGET_RECORD；ps检查报`unknown ps UID label`，进程退出2并停止。第二轮、双快照一致性及PREFLIGHT_ELIGIBLE均NOT_RUN，没有改规则或重试。首ps失败路径未保留具体标签/SHA，不能将结束输入套用为首轮实证。
- **只读结束保护**：只执行一次锁定G2结束入口，收集完成退出0但进程检查仍BLOCKED；stage namespace ABSENT。正式APK/UID、188条私有元数据聚合及设置在本次前后相同，三包身份核对一致；未读私人账本/图片字节，字节完整性仍NOT_RUN。初始devices1＋启动等待26（2次早期offline非零保留）＋预检/结束66＝93条已归档ADB读取，不是93项测试。没有发出安装、应用启动/停止、清理或信号命令，没有构建，原模拟器保持运行。
- **G2-R交接**：[执行](research/M10_JPEG_R3_F1B_G2R_EXECUTION.md)、[设备结果](research/M10_JPEG_R3_F1B_G2R_DEVICE_RESULTS.md)、[风险/决策](research/M10_JPEG_R3_F1B_G2R_DECISION.md)、[checkpoint](checkpoints/2026-10-17-M10-JPEG-R3-F1B-G2R.md)。G2原失败与G1独立PASS WITH NOTES（仅监督器/宿主测试）保留；N01～N03、历史UNKNOWN、R3原29项0 PASS/1 UNKNOWN/28 NOT_RUN、M10 PENDING/三JPEG FAIL、UI-C1冻结及M11未授权不变。

以下为G2停止点历史；本轮环境恢复及新阻塞以上节为准，不改写旧失败：

- **M10-R3-F1B-G2 — BLOCKED，等待独立审查**：用户授权锁定G1入口的一次真实只读preflight；来源`7ff737ecd54746aaed580a15fac92126009d5c44`，独立worktree/分支`research/m10-jpeg-strict-r3-f1b-g2`。2026-10-17 07:19:16 UTC首条`emulator-5596 get-state`退出1并报告device not found，预检进程退出2；07:19:39 UTC必要只读结束核查首条同样失败，随后停止。没有修改G1源码/规则或恢复环境。
- **G2实际结果与限度**：总计2条只读状态查询，均非超时；0设备快照、0新增Android案例PASS。AVD/API36/boot、三包当前APK/UID、活跃Worker、ServiceRecord分类、stage/current/pin/receipt、双快照一致性与前后保护比较全部NOT_RUN。总体BLOCKED是连接环境，不是服务分类BLOCKED；不能由旧证据推定当前设备或私有数据状态。无安装/启动/清理/信号、无ADB预检重试、无私人账本或图片字节读取。
- **G1独立来源与继承**：本轮用户转交G1独立**PASS WITH NOTES，仅限研究监督器和宿主测试**，评审7ff737e。原81项HOST_ONLY PASS保留、本轮未重跑，不替代Android门禁。N01～N03、历史UNKNOWN、原29场景0 PASS/1 UNKNOWN/28 NOT_RUN、M10 PENDING/三JPEG FAIL、R3生命周期BLOCKED、UI-C1冻结PASS WITH NOTES与M11 NOT_AUTHORIZED均保留。
- **G2交接**：[执行](research/M10_JPEG_R3_F1B_G2_EXECUTION.md)、[设备门禁结果](research/M10_JPEG_R3_F1B_G2_DEVICE_RESULTS.md)、[风险与决策](research/M10_JPEG_R3_F1B_G2_DECISION.md)、[checkpoint](checkpoints/2026-10-17-M10-JPEG-R3-F1B-G2.md)。提交推送G2后立即停止，不自动安装APK或运行Outsider/normal。

以下为G1交付历史；其独立审查来源与当前G2结果以上节为准：

- **M10-R3-F1B-G1 — READY_FOR_REVIEW**：用户本轮仅授权研究Python、宿主测试、文档及提交推送。来源锁定`04561e14d5d5694f99dadf35d9a92e42d851e9bd`，独立worktree/分支`research/m10-jpeg-strict-r3-f1b-g1`。原监督器全局名称否决已替换为完整ServiceRecord候选分类、独立进程/已装身份/stage检查及双快照一致性组合；未知/冲突/活动/截断fail-closed。未修改WorkerService、StageStore、正式产品或历史证据。
- **本轮实际验证**：81个唯一Python方法PASS（新38分类＋27组合/原监督器FakeCollector集成＋原16 stage），0 FAIL/ERROR/SKIP；真实RED/GREEN及输入SHA保留。没有调用ADB、Android工具或构建，没有安装/启动任何App；Outsider/normal、FD/lease/死亡/真实清理仍NOT_RUN。宿主合成PREFLIGHT_ELIGIBLE不等于设备资格、租约或安装/启动授权。历史F1B裁剪资料无法组成完整双快照，保持其BLOCKED结论。
- **G1交接**：[执行](research/M10_JPEG_R3_F1B_G1_EXECUTION.md)、[测试](research/M10_JPEG_R3_F1B_G1_TEST_RESULTS.md)、[决策](research/M10_JPEG_R3_F1B_G1_DECISION.md)、[checkpoint](checkpoints/2026-10-17-M10-JPEG-R3-F1B-G1.md)。下一步仅等待ChatGPT独立审查。N01～N03、历史UNKNOWN和原29项0 PASS/1 UNKNOWN/28 NOT_RUN不变；M10 PENDING/三JPEG FAIL，R3生命周期BLOCKED，UI-C1冻结PASS WITH NOTES，M11 NOT_AUTHORIZED。

以下为继承的F1B停止点与历史事实，G1宿主结果未升级其Android状态：

- **M10-R3-F1B — BLOCKED，等待ChatGPT独立审查**：用户正式授权限定Outsider拒绝/销毁及一次normal，但安装前监督器发现旧WorkerService系统记录即保守停止。没有安装或启动任何新APK、未运行Outsider/normal；不能登记Android生命周期PASS。来源F1-A锁定`f23180a4c086d0fc01f944a83174d52e9ad62dbd`，新worktree/分支`research/m10-jpeg-strict-r3-f1b`，未覆盖原分支。交接：[执行](research/M10_JPEG_R3_F1B_EXECUTION.md)、[逐项结果](research/M10_JPEG_R3_F1B_ANDROID_RESULTS.md)、[资源生命周期](research/M10_JPEG_R3_F1B_RESOURCE_LIFECYCLE.md)、[决策](research/M10_JPEG_R3_F1B_DECISION.md)、[checkpoint](checkpoints/2026-10-17-M10-JPEG-R3-F1B.md)。
- **实际阻塞与边界**：目标ServiceRecord17f772c仍存在，recentCallingUid10219、app=null、无目标Bindings段；前后只观察到旧Host3928/UID10218与Outsider4062/UID10219，没有Worker进程。stage namespace只在停止后采集确认ABSENT。不能把“有服务记录”写成已证明有活跃消费者；初始门禁可能过严，残留安全分类仍UNKNOWN，不当场改门禁/清理/重跑。无关Last ANR前缀从发布派生证据排除，范围/SHA/原返回记录保留，不能归给Worker。
- **F1B实际验证**：原AVD/API36/boot=1实核；54次ADB全为读取、退出0，不是54测试PASS。正式APK/UID10216、188条私有元数据聚合及设备设置、旧研究已装APK/UID前后相同；私人内容字节NOT_RUN。本机锁定F1-A两APK的SHA/签名/Manifest与27输入核对通过，装机签名连续性/安装/Outsider新拒绝与泄漏消失/normal Binder/FD/退休/清理全部NOT_RUN。16项新宿主合成snapshot检查单测PASS，不计Android案例；本轮没有Gradle/Lint/构建或新的normal runId。
- **F1-A独立来源与NOTES**：用户转交`PASS WITH NOTES，仅限研究代码、JVM测试及构建`，评审f23180a。此前60项JVM/两研究APK构建为历史，未重跑；[三份报告](research/M10_JPEG_R3_F1A_EXECUTION.md)与原证据不改。N01仅持久REVOKED状态尚未被授权谓词完整拒绝：OPEN_NOT_TESTED，不修不测cancel；N02半清理保守阻塞保留；N03实际连接清理验证未运行。不能把stage缺失或JVM通过当作这些NOTE已关闭。
- **F0与跨阶段继承**：F0独立PASS WITH NOTES — D INTERMITTENT_RECOVERED历史有效，Host1572ms与跨UID拒绝不代替新APK验证。历史两次attach/R1 120秒UNKNOWN不关闭。原29场景仍0PASS/1UNKNOWN/28NOT_RUN，R3完整生命周期BLOCKED，B1生产NO-GO；M10 PENDING/三JPEG FAIL，UI-C1 ACCEPTED/FROZEN — PASS WITH NOTES，M11 NOT_AUTHORIZED。正式代码、研究APK源码/配置、原130案例、验收矩阵、R0～F1-A历史不改。

以下R3停止点及更早状态为继承事实；F0未替代其失败/UNKNOWN或扩大验收。

- **M10-R3历史交付停止点 — Worker生命周期BLOCKED（本轮已收到研究工程独立PASS WITH NOTES）**：用户正式授权隔离Android宿主/研究APK及受控实验，来源R2锁定`2912601899f0282ecc6aa32d5cf26a7b44c24c63`；新分支`research/m10-jpeg-strict-r3`，worktree `E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3/砺石新APP`。未改正式App/Manifest/Gradle/schema/媒体协议/原JPEG断言，未接JNI、allocator、spill或完整规范化，未运行正式App构建。
- **本轮实际完成**：独立Java/强类型AIDL工程、Host/私有worker及不同UID helper、合成stage/lease/receipt/防重放研究模型；17项唯一JVM测试PASS（保留15 FAIL/2 PASS的真实RED），最终研究构建退出0/日志16s、Lint0错误4警告。最终probe 42,621B/SHA `230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0`、helper 8,037B/SHA `214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358`均核对安装字节，UID10218/10219，与正式UID10216隔离。源码/构建不是生命周期通过。
- **Blocking**：既有API36两次启动研究Host（PID27893、28245），系统均记录failed to attach / start timeout并结束；无Application/worker事件和场景报告，最后无研究files目录。底层原因UNKNOWN。29计划Android场景中正常场景两次尝试UNKNOWN，其余28 NOT_RUN、0 PASS。已停止设备扩展，不重启/重建/清数据。正式APK/UID、188条私有文件元数据聚合、主题/字体/旋转/wm前后相同；私人内容未读/复制，字节级一致性NOT_RUN，不能从元数据推断七表/封面字节无损。
- 本轮交接：[R3执行](research/M10_JPEG_R3_EXECUTION.md)、[实际生命周期结果](research/M10_JPEG_R3_LIFECYCLE_RESULTS.md)、[FD恢复](research/M10_JPEG_R3_FD_RECOVERY.md)、[决策](research/M10_JPEG_R3_DECISION.md)、[checkpoint](checkpoints/2026-10-16-M10-JPEG-R3.md)。只推R3后停止；身份、FD、终止和恢复核心契约未证，不自授B1生产可行或授权下一阶段。
- **R2独立审查继承**：本次R3用户指令转交PASS WITH NOTES，**仅限研究架构**。R2首选B1＋A2仍是待证候选，生产NO_GO；A单独不采用，C为另批备选，D缩窄合法输入需PRODUCT_COMPATIBILITY_CHANGE_REQUIRED。R2[决策](research/M10_JPEG_R2_DECISION.md)、[架构](research/M10_JPEG_R2_ARCHITECTURE.md)、[预算](research/M10_JPEG_R2_RESOURCE_BUDGET.md)、[恢复](research/M10_JPEG_R2_FAILURE_RECOVERY.md)原件不改。R3的socket lease细化仅为未获设备验证的研究模型；不是正式存储协议或AppContainer门禁已完成。
- **R1继承实证，本轮未复跑**：固定NDK28.2.13676358/CMake3.22.1/Ninja1.10.2实际可用，官方3.2.0已验SHA/GPG；安装CLI异常退出保留。四ABI COMPILED、API26 triple/ELF16KiB与符号审计；实际仅API36 x86_64 CLI/shared运行。两锁样本A1/A2接受/拒绝，22份完整原创JPEG双路径44调用ACCEPT。累计122次decoder调用、121结构化返回、1监督超时（519次完成内部迭代），不是产品全PASS。
- **资源仍未证明**：40MP444 A2约232MiB、A1约259MiB进程RSS；RSS不是堆、MAXMEMORY不是总native硬cap。6.141秒检查空档及晚取消120秒无结果仍UNKNOWN，复跑成功不关闭。R1原不完整444生成事故和后续完整444验证均保留；总分配/Bitmap峰值、最坏CPU/取消闭环、API26进程身份/终止/恢复、真实OOM、其他ABI/真机/16KiB页/JNI/APK增量仍NOT_RUN。具体E01～E10需新授权，不以R2文档检查补成PASS。
- **模块状态继承**：本次用户明确确认、且已核对远端UI正式冻结文档提交`0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`；UI-C1 ACCEPTED / FROZEN — PASS WITH NOTES，未将UI代码合入研究分支。M00～M09原冻结不变；M10仍PENDING，原3项JPEG拒绝FAIL、原M10-T03/T04边界保持；Q05 USER_CONFIRMED，M11/M12完整往返NOT_RUN，M11 NOT_AUTHORIZED。UI-C1 N1～N4、旧超时UNKNOWN及平台NOT_RUN继续保留。
- R0研究材料独立PASS WITH NOTES，技术可行性当时BLOCKED/0 native实验；其[可行性原件](research/M10_JPEG_R0_FEASIBILITY.md)、[原矩阵](research/M10_JPEG_R0_TEST_MATRIX.md)、[原提案](research/M10_JPEG_R0_PRODUCTION_PROPOSAL.md)不改。R1[执行](research/M10_JPEG_R1_EXECUTION.md)、[实际结果](research/M10_JPEG_R1_RESULTS.md)、[资源报告](research/M10_JPEG_R1_RESOURCE_REPORT.md)、[生产建议](research/M10_JPEG_R1_PRODUCTION_RECOMMENDATION.md)及[R1 checkpoint](checkpoints/2026-10-16-M10-JPEG-R1.md)保持历史BLOCKED语境；[R2 checkpoint](checkpoints/2026-10-16-M10-JPEG-R2.md)保持当时交付语境。当前交接取R3，不改写旧报告/失败/NOT_RUN。

以下M10及更早模块状态、测试结果与限制保留当时语境；当前研究授权和停止边界以上节为准。UI-C1最终完整回归仍为86 JVM PASS＋629 Android PASS/3 FAIL＝718项715 PASS/3 FAIL、整体FAIL，冻结结论不替代该结果；N1～N4不自动关闭。

- **M10 — PENDING，JPEG补充验证已复现，修复BLOCKED_AT_SCOPE_DECISION**：复核起点本地/远端均为 `d7e9a92096b70d6e5976ebcc65dbdfda32b17296`，工作区原先干净；分支 `feat/m10-local-cover-media`，M09冻结来源 `57de897917473f255d43dfb306a8433b366c00eb`。使用原字节合成fixture，在已有API36 AVD通过真实ContentResolver → LocalImageStore确认缺损JPEG被错误接受。新增5方法实跑2 PASS /3 FAIL（退出1、3m13s），生产代码未改；ImageDecoder同样SUCCESS且partialErrors为空。152份完整输入同字节的Lint/assemble另跑成功（退出0、1m22s，0 errors/23 warnings），不是完整回归通过。见[补充报告](evidence/M10-JPEG-supplement-verification.md)、[JSON](evidence/M10-JPEG-supplement-verification.json)及[新checkpoint](checkpoints/2026-10-14-M10-JPEG-supplement.md)。
- 项目/订阅旧封面和完整资金/付款/状态事实在准备后保持原字节与元数据，测试未提交缺损图；但拒绝断言失败，失败清理断言未到达，M10-T03/T04该边界不能标PASS。有效原设备账本七表、两份私有封面及已安装APK字节实测不变；本轮未重建AVD/清数据/改时钟。完整严格JPEG校验需新增解码能力或显著扩展解析，按用户补充指令第5节提交证据和待授权方案后停止。正式ACCEPTANCE_MATRIX及历史652结果未改；新输入下完整JVM/Android回归NOT_RUN。Q05仍USER_CONFIRMED，M11/M12完整往返和全部平台NOT_RUN保持，不自授ACCEPTED/FROZEN，不进入M11。

- **M09 — ACCEPTED / FROZEN — PASS WITH NOTES**：依据用户在本会话正式转交的 ChatGPT 独立代码验收结论，评审 HEAD 为 `bfc25378927ca8d04eaecbb9d2b1455d72633da9`，评审及本次回写分支为 `feat/m09-subscription-lifecycle`，来源M08冻结回写 `0063ca504ca48ef9c7231d79dc155e9bf4bc8c82`。M09-T01～T06、原455＋新增94＝549项测试（86 JVM＋463 Android）及API36的20条实际路径等交付证据通过独立审查，未发现确定性阻塞问题。完整12项确认与全部NOTES见[正式验收登记](ACCEPTANCE_MATRIX.md#m09-独立代码验收与正式冻结记录2026-10-13)。本次仅文档回写，未重跑产品测试、构建、安装或核验APK字节；旧执行与checkpoint保留当时READY_FOR_REVIEW/PENDING，正式结论取代其当前效力。M11～M23未授权。
- **M08 — ACCEPTED / FROZEN — PASS WITH NOTES**：依据用户在本会话正式转交的 ChatGPT 独立代码验收结论，评审 HEAD 为 `a716bc5ad9e3ab49cbe858f2c966fecf93e09fbb`，评审分支 `feat/m08-subscription-renewals`。这是M08历史验收，历史交付状态与证据保留；当前M09正式冻结见上一行，M11～M23未授权。
- **M00～M07 — ACCEPTED / FROZEN — PASS WITH NOTES**，均依据用户转交的 ChatGPT 独立结论。M07 评审 HEAD 为 `4089428e7acdc0f2b8e2fd660f437b5bc7d29f61`，冻结回写 `fcb850f549285d310040318a5f7adfd51abb4c33` 是M08当时起点；全部确认项、评审 HEAD 和 NOTES 保留在[验收登记](ACCEPTANCE_MATRIX.md)。旧报告、XML、JSON、截图和 checkpoint 的当时状态不改写。

## M10 原d7e9交付的历史验证与保留边界（不证明新增JPEG边界）

- 本地受控静态JPEG/PNG、同代私有不可变资源、共享双引用清理及两类封面界面已接入；当前147项完整输入下已核对**652个唯一方法PASS＝86 JVM＋566 Android，原549＋新增103，0失败/错误/跳过，源码/XML EXACT MATCH**。覆盖来自分开执行：`green-full-runner-final`成功的JVM子命令（67.033秒、退出0），以及`green-android-systemui-clean-final`成功的Android/Lint/assemble阶段（639.901秒、退出0，日志10m39s）。第一阶段整体仍FAIL，其557 PASS/9 FAIL的Android结果全部排除；不是一次652项BUILD SUCCESSFUL。输入/ZIP原字节及逐方法依据见[严格覆盖核对](evidence/m10-tests/final-split-verification.json)。Lint0 errors/23 warnings，26完成阶段/44XML与1次中断的原始失败/日志保留。手工路径另有实际证据，不由自动化推定。
- 两处实际SIGKILL探针已PASS：文件落实后DB前（PID4239→4297）、DB提交后旧图清理前（PID4335→4375），旧进程实际消失、同合成generation重开、封面/金额/历史/时区和SQL完整性核对成功，安全清理已知孤儿。仅androidTest独立合成数据；不是断电、真实LMK或M12恢复证明。测试Runner登记失败的先前准备尝试保留；修正后的Runner无action时直接委托AndroidJUnitRunner，显式action才执行不计JUnit的探针，正式APK不包含它。
- 专用API36此前误重建，原AVD目录已不存在，原设备连续升级/保留证据中断。原M09合成DB/WAL/SHM及事实快照完整，严格重建fixture并保留已有空generation后，实际原M09 code7→最终M10 code8覆盖升级PASS：全部七表原字段、同代、GMT、pointer/ready/settings字节一致。不能称原设备连续保留或M12产品恢复。实际系统取消零写、A源删除冷重开、坏图保旧、两类独立导入/替换/移除、共享B及删除QA项目保护订阅、确切旧文件物理清理PASS。断网default network=none，360dp/200%字体、真实IME/raw错误、横竖屏/边缘手势及已保存Task616/PID17203→17927/同准备ID与字节恢复、明确一次保存PASS。早期加载截图和屏外就绪超时如实保留。全部guest设置实际复原，UTC差0.731秒，宿主和其他设备未改；见[最终设备核验](evidence/m10-device/manual/final-storage-device-proof.json)。
- 最终APK code8/0.1.0-m10-debug，12,755,115 bytes，SHA `939b3b20d3c6cd02c800f320ccddc300736b75084b609c404f38b096270f7094`，实际已安装字节与最终产物一致。v2签名/旧证书、无网络/相机/广泛存储权限、九域备份排除及90生产类/测试未打包核对；Room v2、原v1/v2导出、冻结引擎、唯一owner与运行依赖122→122保留。先文件fsync/核验后Room引用，不宣称跨存储原子；未知归属残留保留，真实LMK/断电/Release/真机/API26/OEM/完整TalkBack及完整备份恢复仍NOT_RUN。

## Verified / Completed — M09（独立验收认可的此前实际交付；本次未重跑）

- 复用schema v2七表、M08迁移/版本结构预检、唯一owner/coordinator、Money/BusinessClock及M03引擎。六种Room事务写入完整事实CAS；同日状态/原付款纠错保留身份，精确固定目标重放；无覆盖重开付款＋ENABLED同事务，取消计划/删除只确认CurrentAbsence，最后一笔拒绝。
- 用户A/B规则已实施并实测：STOPPED可同ID重开；覆盖内不付款、未来已付款生效前仍EXPIRED、无任何覆盖显式真实付款；默认停止固定全部已确认覆盖最晚边界与今天中的较晚日，预付/空档/全部到期/提前停止及固定计划冲突均验证，后续付款不会静默延长原计划。来源为用户M09完整指令SHA-256 `d83fb2d77190b0c49df8b5adf9a6e13c8cae8ca203513f9f5e98b0729350eaab`及[原决定记录](DECISIONS.md)、[M09契约](planning/M09_CONTRACT.md)；实施验证由原[M09报告](evidence/M09-verification.md)保留，本次独立审查认可。同身份重开及多期预付停止两项流程风险按模块证据关闭，Q03～Q06期限不变。
- 正式现在管理分订阅中/暂停中/已到期，已停止从回看进入；未来停止不提前归档。单一SavedState信封保存raw/基线/UUID/提交目标；保存不确定/读取错误保留输入与pending，COMMITTED只读；迟到读取不能覆盖后来详情，Nav3来源链有界/双VM守卫保护离开。
- 最终实际四任务 **Gradle日志13m 24s，命令墙钟805.631秒，549/549 PASS＝86 JVM＋463 Android，原455身份保留、新增94，0失败/错误/跳过**。所有测试方法与最终XML一一对应、128份构建输入哈希一致；Lint 0 errors/20 warnings、Debug assemble完成。11个此前真实阶段及最终阶段均归档，早期两次全量失败和旧四测试驱动最小调整不抹除，不把同源码限定PASS当根因证明。
- 最终APK **code7 /0.1.0-m09-debug，12,521,904 bytes**，SHA-256 `becd15b3bbf5de9425419504bde58837d0d6fa6e1cacc32d995f8dc11ec437f2`，与实际旅程使用及最终安装指纹相同；v2签名/旧证书兼容、无网络/禁止权限、九域备份排除、79指定生产类/无JUnit测试打包核对。运行依赖122→122完全相同，TOOLCHAIN_LOCK不改。
- 仅专用API36合成服务：150→预付180，固定3/1计划/边界STOPPED、修正/取消、暂停/恢复/提前停止、原ID重开、2/1真实6元每日、无覆盖空金额/付款日显式240原子启用、原付款纠错225及明确删除错误180，断网冷启仍按事实EXPIRED。实际设置/设备UTC恢复；旧七表原行逐字段相同，同generation/ready/settings/GMT。小屏200%/真实IME/raw错误/旋转/真实手势返回/系统已保存任务SIGKILL后同任务恢复单独留证，不称真实LMK。

13项报告及所有边界见[M09报告](evidence/M09-verification.md)、[结构化证据](evidence/M09-verification.json)、[文件清单](evidence/M09-files.md)与[M09 checkpoint](checkpoints/2026-10-13-M09.md)，均保留原交付字节和历史状态。本地辅助审阅不代替ChatGPT独立验收；本次正式来源是用户转交的独立结论，不将文档核查登记为新的产品测试PASS。

## Verified / Completed — M08（冻结继承的当时交付；M09实施回归另见上节）

- 新增订阅身份、独立续费及状态时间线三表，导出 schema v2；显式注册 1→2 迁移。原 v1 JSON、原四实体及旧业务事实保留；既有库在可写打开前核对版本、Room 身份、实际结构/索引/外键、WAL 和完整性，非法库拒绝并保留。字面内部对象前缀、PK BINARY/ASC 和保留表同名 trigger 的漏检均先复现再限定修复，无破坏性 fallback 或另一份空 generation。
- 首次身份与正额真实付款同一 Room 事务；追加续费复用唯一 owner/coordinator，固定 UUID/operationId，完整事实精确重放，不同内容冲突。全部已付覆盖参与重叠校验；逐笔保留 cycle、standardDays 和四个真实日期。复用冻结 Money/BusinessClock/M03，不复制成本公式、不自动付款或写入 M09 状态操作。
- 四个正式订阅页面、真实首页数量、paidOn 排序/SQL 分页/完整成本与写后刷新已接通。受控输入、可见周期选择、排他结束日说明及明确付款确认；读取失败不伪装空库。唯一 SavedState 信封与双 ViewModel 导航守卫覆盖 pending/dirty、隐藏表单可达、精确成功身份、COMMITTED 只重读及缺失目标安全返回。
- 最终四任务退出 0，**13m30s；86 JVM＋369 Android＝455 个唯一方法 PASS，0 失败/错误/跳过**。源码与 XML **EXACT MATCH**，旧 359 方法身份保留、新增 96，108 源码/schema 输入哈希与实际验证一致；八份最终 GREEN XML。六项原案例及本轮 20 项安全要求只在当前执行登记记录，不改原 130 案例。Lint **0 errors /20 warnings**，运行依赖 122→122 无新增/删除/升级。
- 真实 RED、编译/runner/匹配/准备及同步诊断完整保留。两个旧测试仅适配真实阶段，另两个旧测试加强驱动同步，保留方法身份、原业务断言和超时；不把同步调整声称为生产缺陷修复。此前 10m19s 的完整 455 PASS 已单独归档，之后只补周期可见提示及提醒日期当前不通知文案，再执行最终完整回归。
- 最终 Debug APK code6 / `0.1.0-m08-debug`，**12,386,935 bytes**，SHA-256 `6e04f28cc0998fa772cfeeedf99eb457b71852ee7549eeda39525c6155afbcf6`，与实际安装指纹一致。v2 签名及旧 M07 证书兼容、合并 Manifest/无网络及禁止权限、九域备份排除、63 个指定生产类及无测试框架/合成测试打包均已核对；Release 未构建。
- 仅专用 API36 `emulator-5596`：旧 M07 code5→中间 M08 code6 在原代真实升级，最终 APK 再同身份覆盖并核对，未卸载/清数据。旧四表所有事实与升级前完全相同：3 项目/6 事件；旧纠错结束项目仍净投入 ¥350、10 天、¥35/天。最终 APK 上完整创建合成服务并确认 ¥150→预付 ¥180，取消续费不写；实际 guest 日期 1/19 当前 ¥5/历史150/标准30，1/20 当前 ¥5/历史330/标准60/平均5.5/实际20，2/1 当前 ¥6。恢复10/13后断网冷启动 EXPIRED、当前价格 null/贡献0、历史330/标准60/实际59，无自动付款。保留此前候选合成服务，最终为 **2 订阅/3 续费/0 状态变更**，不删旧参考数据。
- 最终 APK 的 360dp/200% 字体、真实 IME、1.234 精度错误及 raw 保留、返回收键盘、横竖屏已操作。Task3038 已 STOPPED/HaveState=true/Bundle6360bytes 后 UID SIGKILL，旧 PID14365 消失，Recents 恢复同任务/新 PID15775/raw1.234；隐藏命令身份同一性由正式 Room/Nav3 仪器测试证明，不冒称手工界面读出了 UUID。最终只读 SQL integrity_check=ok、FK 无错误，generation/ready/settings SHA/记录时区 GMT 不变。全部临时 guest 网络、屏幕、字体、IME、旋转、明暗、时区及 auto_time/auto_time_zone 已恢复，guest UTC 与宿主相差少于10秒，未修改宿主时钟或其它设备。

详细文件、逐方法/输入/产物、失败诊断、实际设备及来源证据见 [M08 报告](evidence/M08-verification.md)、[JSON](evidence/M08-verification.json)、[文件清单](evidence/M08-files.md)和 [M08 checkpoint](checkpoints/2026-10-13-M08.md)。M08 正式独立验收的11项确认及全部 NOTES 见[验收登记](ACCEPTANCE_MATRIX.md#m08-独立代码验收与正式冻结记录2026-10-13)，来源为用户转交的 ChatGPT 结论，本地辅助审阅不等于独立验收。

## Pending / Current Problems / Technical Debt

- **M10 JPEG输入完整性：**保留EOI但图像数据缺失的锁定样本被扫描、BitmapFactory、normalize及真实prepareImport接受，产生18,820字节规范化图片和有效准备租约。新3项拒绝测试明确FAIL；尚未实施生产修复，后续需先由ChatGPT确认受控严格解码预检方案及新增依赖/构建边界。不得以输出可解码、日志警告或旧652 PASS掩盖缺口。正式登记不改，补测状态在本文件与新证据中如实记录。
- **M07 NOTES 继续有效：**ProjectAbsent 只确认当前不存在，不证明历史删除操作身份；多条旧 END_SETTLEMENT 明确冲突，没有自动修复；媒体数据/文件生命周期清理归 M10。原20条 Lint 提示及历史执行证据保留。M05 写后刷新与分页交接已在 M06 实施并独立验收，不改写历史 NOTES。
- Q01 CLOSED，Q02/Q07 USER_CONFIRMED；Q02 M17/M18 视图集成仍 NOT_RUN。Q03 长期100/1000日节点在 M21 前，Q04 提醒资格在 M20 前，Q05 图片往返在 M10/M12 前；Q06 **ENGINEERING_REQUIRED**，M12 完整导入、切换、验证后回退、中断、共同校验、清理及 Q06-V01～V03 完整流程仍 NOT_RUN。原 ZIP/43 原件/24 模块、130 案例及16组完整跨模块补测不改。
- [M09 两项流程风险](SPEC_ISSUES.md)由用户M09指令明确A/B产品规则：允许同身份重开，默认固定最晚已确认付费边界；保持USER_CONFIRMED / IMPLEMENTED / VERIFIED及原来源，两个流程风险已按模块证据关闭，现获独立审查认可。原全量失败及诊断保留，不由本次回写重新实施或验证。M04 首次初始化中断残留 generation 不得静默清除真实数据；M08 版本预检与无损升级已完成，不能代替 M12 完整恢复/故障回退。
- **CurrentAbsence：**只证明当前缺失，不提供永久删除操作日志；继续保留M07的ProjectAbsent和旧END_SETTLEMENT冲突边界。
- **空间不足：**真实 SQLite 满页可能被 Android/Room 回滚异常表现为普通 DATABASE；继续保留输入、事务原子性及同命令恢复，不按消息猜测或宣称所有磁盘满均准确分类。
- **性能与恢复：**全量存储事实预检在长期大数据下的性能仍待M22验证；1000 条分页是正确性/查询范围验证，不是长期性能基准，必要完整历史计算不保证无限历史恒定耗时。SavedState 不是持久业务草稿；已保存任务的 SIGKILL/Recents 不保证未保存状态、force-stop、断电或真实LMK后的未持久化草稿恢复。
- **数据保障：**完整备份/恢复仍归 M11/M12，未完成；Debug 暂不用于不可丢失的长期个人记录。M10当前本地封面见本轮执行记录；M13 汇总、M14 搜索/类别 CRUD、替换、多段复用、图表/排行、单独资金事件删除及后续集成保持原边界/NOT_RUN。
- **平台与独立执行：**真机、API26、OEM 备份/迁移、完整 TalkBack、真实内存压力 LMK、Release/正式签名升级仍 NOT_RUN；Debug 空 ComponentActivity 测试宿主不得未经 Release 验证正式交付。M01 独立未重装，M02/M03 独立未重跑本机 Gradle，M04～M07 独立未重跑本机 Gradle/安装等全部历史 NOTES 保留。M08 独立审查未在用户 Windows 本机重新运行 Gradle、重新安装 APK 或独立核验 APK 字节；超大 JSON 未整体重新解析，验收依据源码/XML/日志/迁移及设备证据。**M09 ChatGPT独立审查未在用户Windows本机重新运行Gradle或独立核验最终APK字节。**各次Codex验收回写均未重跑产品测试；549项PASS来自M09原实施执行证据，不把独立源码审查或本次文档核查视为重新执行。TypeScript/ESLint 不适用，NO-SOURCE 不计 PASS。

- **后续 schema 升级：**必须同步维护既有数据库版本预检及已知 Room 身份，不因 M08 通过就放过未知版本或结构不匹配库。

## Data / Architecture

当前为 Room schemaVersion=2、七张业务表，显式 Migration1→2。UI → 轻量 ViewModel → 同一 Repository/LocalWriteCoordinator → Room；Settings/DataStore、记录时区、AppContainer/ManagedGeneration、私有媒体及备份排除边界继承。pointer/ready 内部 formatVersion=1 不是 backupFormatVersion；不宣称 DB/DataStore/文件跨存储原子。真实结构与历史阶段区分见 [ARCHITECTURE](ARCHITECTURE.md)。

## Git

- 当前研究目录`E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1b-g2-resume/砺石新APP`，origin `https://github.com/cc7279694-debug/property.git`；只提交推送`research/m10-jpeg-strict-r3-f1b-g2-resume`，来源G2锁定`2de44c79173e7fa52841b55542e5ddcc5399a30a`，最终SHA/远端一致性取Git回执。G2原分支保持2de44c7、G1为7ff737e、F1B为04561e1、F1-A为f23180a、F0为f513725、R3为17b2cb4、R2为2912601、R1为2cdad4a、R0为c4cd90b；原M10工作区/分支4159682、UI-C1为0dce7dd、main远端145326d。没有合并、reset、force或修改这些分支/工作区。
- M09评审HEAD `bfc25378927ca8d04eaecbb9d2b1455d72633da9`与冻结回写57de897为历史依据。M10原交付及本次独立复核基准 `d7e9a92096b70d6e5976ebcc65dbdfda32b17296`；补充验证提交仅新增测试/必要合成资产/证据及当前交接，不是生产修复，最终SHA与推送结果由Git日志/最终回执核对，避免递归引用。main保护基线 `145326ddd4ce28abbf7c2706fe25f9d38c599c3a`；不合并main、不force、不自动PR/公开APK，不提交数据库、私钥、私人原图或真实个人数据。

## Next Recommended Task

**BLOCKED — M10-R3-F1B-G2-R。立即停止，等待ChatGPT独立审查未知ps UID标签阻塞。**原AVD现已上线，但只读门禁未通过；不改G1规则、不额外取证或重试，不安装或启动/停止Outsider/normal及正式App，不清理旧研究状态，不开展其他28项、取消/故障恢复、JNI或M11。模拟器保持运行；N01～N03、历史UNKNOWN、M10三FAIL与UI冻结等状态均保留。

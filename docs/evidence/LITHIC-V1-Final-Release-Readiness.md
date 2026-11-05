# LITHIC V1.0 最终发行差距审计

审计日期 2026-11-05。**建议：V1_RELEASE_NO_GO。** 当前已有可体验的核心源码及 Preview19，但当前默认 Release 关闭了已批准的照片写入能力；正式签名、体验版数据进入正式身份、当前代码真实 SAF/异常恢复和适用设备门禁尚无足够证据。本报告建议准备集中补验，不授权发行，也不把用户反馈、桌面测试或静态检查互相替代。

## 基线与事实边界

- 仓库 `cc7279694-debug/property`；开始时核心分支 `codex/v1-core-fasttrack-no-reminders` 的本地、远端均为 `e01b9368eabd967733d08ed307f3797ed61bc453`，工作区干净。审计分支 `codex/v1-final-release-readiness-audit` 从此提交建立。
- 本轮只读重算 Preview19 归档：`deliveries/lithic-preview-0.1.0-preview.19/Lithic-0.1.0-preview.19-debug.apk`，20,506,559 字节，SHA-256 `34ef14ac074fe5afafdf9a554518671e576ea8c0d4144537e39195f0947cf2f6`，与记录一致。实际包名 `com.cooper.lithic.preview.debug`、code35、`0.1.0-preview.19-debug` 和 Debug 证书 SHA-256 `29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9` 是既有 [Manifest/签名实物审计](v1-core-noop-20261105/artifact-2/identity.json)证据，本轮没有重新验签。构建输入为 `782f80ea5313742ae018da48a6eb534f0dc3bd7e`，App 树 `54e3ee06daf9cfec8b7c2e49ff121618f21b6946`，见[真实构建回执](v1-core-noop-20261105/preview19-build-receipt.json)。
- 本次用户任务书转交 Preview19 体验候选已接受；仓库既有[核心版记录](LITHIC-V1-CORE-FASTTRACK.md)本身登记的是 READY_FOR_INDEPENDENT_REVIEW，两种来源各自保留，不在本轮伪造新的独立签收。
- 用户本次反馈：小米14上 Preview18 直接覆盖到 Preview19，未卸载或清除数据，原有物品、订阅仍在，使用没有问题。记为 **USER_REPORTED_MANUAL_PASS_WITH_NOTES**；操作实际日期未提供，未取得逐表、照片字节或设备原始日志。该结果不覆盖失败恢复、真实 SAF、正式签名或全部设备兼容。
- 本次仅只读源码、Git、历史证据与本地 APK 哈希，并新增本报告；不修改生产/测试源码、配置或安装包，不运行测试、构建、ADB、手机、模拟器、真实账本、照片或备份。下述 PASS 均有明确证据层级，不是本轮新执行的 Android PASS。

## 已实现能力与当前范围核对

正常 MainActivity 接入同一 Owner：物品创建/编辑/结束/删除、投入/收回和替换链；订阅真实付款、续费、暂停/恢复/停止及历史；首页、列表、搜索、历史、项目/总趋势、类别及年度分析；手动完整备份、只读备份文件检查、恢复预览和确认；三主题及照片空间统计。路径分别可追溯至 [MainActivity](../../app/src/main/java/com/cooper/lithic/app/MainActivity.kt)、[导航](../../app/src/main/java/com/cooper/lithic/app/LithicNavigation.kt)、[备份导出](../../app/src/main/java/com/cooper/lithic/data/backup/BackupExportService.kt)、[恢复协调器](../../app/src/main/java/com/cooper/lithic/data/backup/RestoreCoordinator.kt)。维修按真实投入记录；无已承诺的独立 JSON/CSV 用户导出。桌面及历史组件验证不等于当前 APK 全链设备通过。

**确定性发行范围差异：**[构建配置](../../app/build.gradle.kts) 的默认 Release `PHOTO_LITE_EXPERIMENT=false`；Private Trial/B1-QA 的 Debug 覆盖为 true，而显式 PHOTO-LITE 实验还禁用 Release 变体。[LedgerUiStateHolder](../../app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt) 在 false 时不创建照片写入器。因此当前默认正式 Release 不能提供已批准的物品照片添加、更换、取消显示，不能把 Preview19 可用视为 Release 已具备。现有 [PhotoLiteThumbnailPipeline](../../app/src/main/java/com/cooper/lithic/feature/media/PhotoLiteThumbnailPipeline.kt) 仅在 **API 恰为 36 且系统 Photo Picker 可用**时开放；工程 minSdk 26 不代表照片支持其他 API。此为 A 类，不能自行将照片整体延期。

Preview19 的物品照片由系统缩略图转本地 PNG（最长边 640px、<2MiB），原始 JPEG 不作为认证成品保存；更换和取消显示保留旧媒体，不作永久删除。读取及备份覆盖已登记媒体，包括无当前引用的历史图。照片提供者可能由系统列出云来源，不能声称选择器只含本机文件。数据估算是登记长度，不是实际磁盘占用或可立即回收空间。

## 发行门禁矩阵

以下状态是**对应证据范围**；执行日期缺失写未提供。时间与提交为所引用证据的最近登记，不暗示当前审计重跑。执行方在设备操作前仍须取得用户当次授权。

### A — 正式发行前必须解决或完成的验证

| 编号 | 风险及对核心能力的影响 | 现有证据、提交/日期、状态 | 最小补验、执行方与通过条件 |
| --- | --- | --- | --- |
| A1 照片正式范围 | 默认 Release 关闭照片写入，与已批准的物品照片核心范围冲突；读取能力不等于添加/更换/取消可用。 | `app/build.gradle.kts`、`app/src/main/java/com/cooper/lithic/app/LedgerUiStateHolder.kt`；当前源码 `e01b9368`，2026-11-05：**FAIL（配置事实）**；正式 Release 实际构建/运行 **NOT_RUN**。 | Codex 仅在后续授权后完成最小正式配置与安全审查；在正式候选实际 Manifest/DEX 和 API36 设备上证明添加、更换、取消显示、重开和备份链可用，禁止直接拿实验标志绕过 Release 禁用；若调整支持范围须用户单独决定。 |
| A2 正式身份、签名与迁移 | Preview 包与正式包不同，直接覆盖不可能；错误的签名/身份或迁移可能丢失已有记录。 | `app/build.gradle.kts` 当前 Release 基础 ID `com.cooper.lithic`、默认 code17/name `0.1.0-m22`；Preview 为 `.preview.debug`/35。未见明确 release signingConfig；密钥在其他安全位置是否存在 **UNKNOWN**。`M13-T06-readiness.md`，`2c5e23a7`，2026-10-20：长期签名保管/升级 **NOT_RUN/BLOCKED**。 | Codex 制定经用户确认的正式 ID、递增版本、外部密钥访问和离线备份方案，勿输出秘密；用户保管密钥。先以独立合成数据验证正式同 ID/同签名更新、重开及备份恢复；Preview→正式需设计并验证跨包导出/恢复，不能称覆盖升级。签名/身份错误即停，不卸载旧包。 |
| A3 当前核心记录及用户升级 | 物品/订阅是数据核心；用户报告升级成功，但照片、全部表/设置、恢复和异常未逐项核对。 | 用户本次报告 Preview18→19 **USER_REPORTED_MANUAL_PASS_WITH_NOTES**，操作日期未提供；[C7](LITHIC-C7-Product-Integration.md) `1c97ff61`/2026-10-28 与 [Preview19](LITHIC-V1-CORE-FASTTRACK.md) `e01b9368`/2026-11-05 为桌面及静态证据。 | 用户在不接触唯一重要副本的前提下使用独立合成账本，核对项目投入/收回、负值、订阅真实续费及状态、退出重开。对已体验普通页面不重复全面巡检；事实、日期、无重复写入和重开一致才关闭对应项。 |
| A4 真实 SAF 备份/恢复 | 未确认的覆盖、文件损坏或新旧 Owner 混用会影响全部记录及照片。 | [B1-R1](LITHIC-V1-B1-R1-Classification.md) `08e11f20`/2026-11-02，168 JVM PASS；[B2](LITHIC-V1-B2-SAF-Acceptance-Preparation.md) `a782b23c`/2026-11-02；[B3](LITHIC-V1-B3-Photo-Space-Management.md) `0e9b3610`/2026-11-03 登记 B1-QA A–H **USER_REPORTED_MANUAL_PASS_WITH_NOTES**，未有逐项原始/图像字节证据。当前候选真实 SAF、逐项字节恢复 **NOT_RUN/UNKNOWN**。 | 用户只在独立可丢弃账本导出 B0→检查→修改→单次明确确认覆盖→重开，并核对可见记录、照片和统计，再导出 B-H。另获授权后 Codex 仅对这两份合成备份离线比对媒体 ID、长度、SHA、尺寸、内容和记录关联；没有合成文件或授权则强比较 NOT_RUN/UNKNOWN。VERIFIED 仅说明文件符合当时校验规则；实际恢复及对应比较通过才关闭该项。 |
| A5 中断及未知结果 | 在途恢复、Owner 换代、文件残留或未确认写入不能被误报成功或自动重试。 | [稳定性收口](LITHIC-V1-Stability-Closure.md) `d0e153fb`/2026-10-29 及 B1-R1 `08e11f20`/2026-11-02 的桌面合成测试；真实 SAF 关闭、进程中断与换代 **NOT_RUN/UNKNOWN**，不能由 A4 正常路径推定。 | Codex 在独立测试身份设计经审查的故障注入；经用户单独授权后在可丢弃数据上执行取消、坏/缺媒体及批准的中断点。结果未知时不得假成功、旧数据与现场须保留；终态不明立即停，不自动重试或清理。 |
| A6 已开放照片安全和备份 | 图片读取、主进程缩略解码、Owner 换代和资源不足影响已开放核心能力。 | [C6-1](LITHIC-C6-1-ReadOnly-Media.md) `88e7edb2`/2026-10-28：44 JVM PASS、14 Android **NOT_RUN**；[P3](LITHIC-PHOTO-LITE-P3-Integration.md) `dd31ce4b`/2026-11-01：56 JVM PASS，61 Android 方法仅编译；资源峰值 **UNKNOWN**。 | 用户/经授权测试者在当前生产同源的独立身份、API36 系统 Picker 上，用可丢弃图片验证取消、坏/不可读图、添加/替换/取消后的列表/详情/重开、Owner 换代与 backup4；目标设备上有界资源、失败反馈及原图保留均需可观察证据。 |
| A7 M22 离线、规模与输入 | 已开放长期记录需要离线与可操作性；规模和输入缺口影响长期使用。 | [M22](M22-verification.md) `5a446ee2`/2026-10-23：137 JVM、36 Android 局部 PASS，T06 三次 **FAIL/INCOMPLETE**，正式 T03 20年/1000项目/1000订阅/20000项目事件/10000续费 **NOT_RUN**，真实 IME/系统200% 未完成；[真机 T06](M22-T06-Physical-Verification.md) `3c3e293c`/2026-10-24 因原零默认路由前提不符 **NOT_RUN**。 | Codex 准备经审查的独立可丢弃测试身份；用户授权后按原方法与样本/超时合同分别完成真实离线旅程、正式长历史、系统大字与 IME，记录目标 API/OEM 和结果。环境前提不符为 NOT_RUN，不放宽断言；若支持范围要改变须用户明确批准。 |
| A8 停用后的旧任务与权限 | 旧任务冷启动若打开账本或发布通知会影响安全；权限声明与产品延期说明须一致。 | `ReminderWorker.kt`、`LithicApplication.kt`、[Worker DEX](v1-core-noop-20261105/artifact-2/core-worker-code.stdout.log)、[发布 DEX](v1-core-noop-20261105/publisher-dex.log) 和[调用检查](v1-core-noop-20261105/artifact-2/product-workmanager-invocations.json)，`e01b9368`/2026-11-05：静态 no-op；Android 真实唤醒/点击及旧任务状态 **NOT_RUN/UNKNOWN**。[实际 Manifest](v1-core-noop-20261105/artifact-2/app-manifest.stdout.log) 仍声明 `POST_NOTIFICATIONS`，但正常设置入口不请求，不能称权限未声明。 | Codex 后续审核正式合并 Manifest、说明和残留权限的必要性，由独立审查决定配置；用户批准的独立设备测试应证明旧模式无 Owner/Receipt/通知副作用及正常首页可用。不得通过取消旧任务或改系统设置取得通过；不能以静态扫描证明运行及反射行为。 |
| A9 发行门槛 | G1–G4 是原始门槛，单项 PASS 不自动关闭整体；G3 与已批准延期存在范围决策。 | 原定义 [MASTER_PLAN.md](../planning/original-v1/lithic_v1_plan/docs/MASTER_PLAN.md)，`daf96157`/2026-10-11；G1 稳定签名核心闭环、G2 浏览分析、G3 两种提醒验证、G4 升级/迁移/故障/小屏/长历史/签名及独立冻结。历史全门 **未关闭**。 | ChatGPT/用户明确 G3 随已批准范围延期后如何处理正式门槛；G1/G2/G4 对照实际证据逐项关闭，最后独立审查。不能将原 G3 标为 PASS，也不由本报告批准发行或修改原计划。 |

### B — 已批准延期，V1 不提供

| 能力 | 限定状态与保留事实 |
| --- | --- |
| 自动续费/里程碑提醒和 RC4 协调器 | 延期 V1.1。当前 [ReminderCapability](../../app/src/main/java/com/cooper/lithic/platform/notification/ReminderCapability.kt) 固定 false，正常调度为 no-op；旧 Worker 保留且源码/DEX 在读取 Owner 前返回成功。用户确认旧提醒点击可按普通首页打开，首页仍会正常读取账本，不解析旧提醒详情。旧任务**不主动取消**，可能继续唤醒及耗电；实际历史依赖图及任务终结 UNKNOWN。Manifest 仍声明通知权限（见 A8），非“权限已移除”。 |
| 严格 JPEG 原图导入 | **STRICT_JPEG_IMPORT_BLOCKED**。旧 M10 同一缺损 JPEG 的三个业务场景历史 FAIL 不改判。[C6-2A](LITHIC-C6-2A-JPEG-Admission.md) `35a616a7`/2026-10-28 复核的旧研究核心及 API26/36 限定归档曾拒绝锁定样本，不是当前生产修复；最新全链资源、取消和异常恢复仍 UNKNOWN/NOT_RUN。 |
| 历史照片永久删除/GC | 未开放。backup4 仍包含全部登记媒体；删除顺序、共享引用、并发快照与中断恢复尚未证明安全。B3 的历史登记字节为估算，不承诺可释放的物理空间。 |
| C6 严格隔离通道 | [C6-2B](../design/LITHIC-C6-2B-Media-Import-Architecture.md) `9f84c42a`、[C6-2C](LITHIC-C6-2C-Isolation.md) `267fec16`/2026-10-28 为架构/静态实现；[C6-2D](LITHIC-C6-2D-Runtime-Verification.md) `76ae1daf`/2026-10-29 因无准入独立模拟器 **ENVIRONMENT_BLOCKED**，8 项 Android **NOT_RUN**。该 Sandbox/FD/JPEG 通道未接入当前 Photo-Lite，不机械要求运行它才能证明当前 PNG 写入；共享进程角色及实际 PNG 主进程资源/读取/备份风险仍属 A6/A8，不得一起豁免。 |

### C — 可继承证据，避免无关重复

| 已有证据 | 继承边界 |
| --- | --- |
| [M13](M13-verification.md) `775951b9`、[M14](M14-verification.md) `730fd2a4`、[M15](M15-verification.md) `ec0b7286`、[M16](M16-verification.md) `d8ce0a8d`、[M17](M17-verification.md) `54360aa0`、[M18](M18-verification.md) `6dfc4a6c`、[M19](M19-verification.md) `21d62305`（2026-10-20～21） | 继承各自已验的金额、日期、付款事实、替换、分类、搜索、历史和分析合同及原环境结果；不改变公式，也不将历史隔离宿主 PASS 扩大为当前 Main/全设备/G2 通过。Room5、backup4、Owner/generation/恢复 Token 以当前源码和既有冻结合同为准。 |
| B1-R1 `08e11f20`、B3 `0e9b3610`、P3 `dd31ce4b` | 分别继承 168 JVM、62 JVM＋8 宿主 SQLite、56 JVM 的当时桌面证据；P3 人工体验及 B1-QA A–H 为用户报告，非逐图/异常恢复或自动化 PASS。无需重跑不受影响的全套。 |
| [C5](LITHIC-C5-Implementation.md) `82431c76`、[C9](LITHIC-C9-Visual-Theme-V2.md) `9c0956f8`、[C9-R1](LITHIC-C9-R1-Final-Visual-Refinement.md) `3aa4f30f` | 继承三主题、公共视觉与桌面测试；Preview17/18/19 用户反馈为 USER_REPORTED_MANUAL_PASS_WITH_NOTES，非全部页面/系统字体/IME/OEM 视觉验收。普通页面不因发行准备而重复大范围重设计。 |
| [Preview19 测试汇总](v1-core-noop-20261105/junit-summary.json)、[最终编译/Lint](v1-core-noop-20261105/verification-rerun.log)、[APK 审计](v1-core-noop-20261105/artifact-2/identity.json)，`e01b9368`/2026-11-05 | 171 JVM PASS，生产及 AndroidTest 源码编译通过，Lint 0 错误/20 警告，APK 身份与核心接线静态通过；AndroidTest 未运行，不能替代设备行为。首次编译/审计器错误和修正记录均保留。 |
| 用户报告 Preview18→19 | 继承用户实际观察到的同包覆盖及物品/订阅保留；不重复要求用户证明相同观察，不扩成照片字节、全库、正式跨包迁移或异常恢复通过。 |

## 历史 FAIL、UNKNOWN 与未找到的证据

- [M20 Android](M20-Android-verification.md) `d2be4cb2`/2026-10-22：原 22 个唯一方法最终 PASS；额外真实平台通知窗口方法两次 **FAIL**。发布调用等局部子断言不等于用户可见送达。[平台收口](M20-Platform-closure.md) `d3776e0b` 同日 **BLOCKED**，周期任务、OS Job、指定通知和新 Owner 收口 **UNKNOWN**。现延期不改历史结论。
- [M21 Android](M21-Android-verification.md) `a367b278`/2026-10-23：31 方法在 API36/x86_64 隔离宿主、合成 Publisher/TestDriver 范围 PASS；真实系统发布/点击、API26/OEM 和重启不由此通过。G3 仍未关闭。
- RC3、RC4 和 Repair 不在当前核心树，但并非全仓未找到：分别可只读查 `d8fa6049:docs/evidence/LITHIC-V1-RC3-Final-Reliability-Gates.md`、`354b99d2:docs/evidence/LITHIC-V1-RC4-Reminder-Coordinator.md`、`616b8821:docs/evidence/LITHIC-V1-RC4-Repair.md`。RC3 SOURCE_AUDIT_ONLY/BLOCKED；RC4 149 JVM、Repair 150 JVM 是其分支证据，跨进程 UNKNOWN 与恢复切换等待仍 BLOCKED，未合入核心分支。
- M22 T06 三次历史 FAIL 及 T03/真实 IME/系统大字 NOT_RUN 如 A7；[空模拟器记录](M22-Clean-Android-verification.md) `4fa4be13` 在装 App 前出现 SystemUI ANR，原因 **UNKNOWN**，是环境阻断而非已证实产品缺陷。旧故障 AVD 不作为补验环境。
- 本轮按最初猜测查找的 `docs/evidence/M22-Android-verification.md`、`docs/evidence/LITHIC-V1-B3-Photo-Storage.md`、核心树中的 RC3/RC4 报告为 **NOT_FOUND（这些路径/该树）**；相应实际来源分别为 [M22](M22-verification.md)、[B3](LITHIC-V1-B3-Photo-Space-Management.md) 和上述 Git 对象，不能把猜测路径当证据。

## 正式发行身份与权限

当前 Gradle 未发现正式 release signingConfig 的明确接线，未发现明确的 Release minify/R8 或资源压缩声明；实际插件最终配置、正式构建和签名 **NOT_RUN**，不能推定禁用或已就绪。密钥文件内容、密码、私钥、存放与备份情况均未读取，外部安全配置和访问控制 **UNKNOWN**。Debug 证书一致不代表正式签名、正式升级或密钥托管已通过。

Preview19 实际 Manifest 还包含 `ACCESS_NETWORK_STATE`、`POST_NOTIFICATIONS`、`RECEIVE_BOOT_COMPLETED`、`WAKE_LOCK` 及包专用非导出接收器权限；没有 `INTERNET`。这些是既有实物声明，不代表主动联网、已请求通知权限或允许发布通知。正式 Release 合并 Manifest、系统备份设置、权限用途及用户隐私说明需随正式候选重新核验；是否删除某项声明需单独审查，不在本任务改配置。

## 用户最小操作清单与最短路线

下一阶段所有设备安装、系统设置与覆盖恢复均需用户当次明确授权；优先使用与 Preview19 同源、正常 Application、独立包名/UID 的可丢弃验收候选。现有 B1-QA 是旧源码，不能当作 Preview19/正式 Release 的同源证据。不要覆盖现有 Preview 或把用户真实账本作为自动测试数据。仅分三个大批次：**批次 1 为步骤 1～3，批次 2 为步骤 4，批次 3 为步骤 5**；步骤是集中清单，不代表五次独立阶段。

1. **集中验收准备及一次正常流程：**独立候选及身份经审查后，由用户用合成记录新建一个物品和订阅，核对投入、收回、续费、状态、负值及退出重开；添加并更换可丢弃照片、取消另一封面的显示，查看引用及历史照片统计。
2. **合并一次 SAF 闭环：**导出唯一新合成备份 B0、用检查功能验证，再修改独立账本；仅在确认目标为该独立账本且单独授权覆盖后恢复、重开，对照可见数据、图片及统计，另导出 B-H。界面不展示逐图 SHA/尺寸；另获授权后 Codex 仅对这两份合成备份离线做逐图和关联比较，未提供或未授权则记 NOT_RUN。VERIFIED 本身不是恢复成功；出现 UNKNOWN、差异或崩溃立即停，不清理、不重复提交。
3. **授权范围内补关键异常与兼容：**用经审查的合成坏文件/缺图和中断注入检查原数据保护；按原 M22 合同完成离线、正式长历史、真实系统大字/IME 及适用设备检查。若自然前提不符记 NOT_RUN，不扩大操作权限求通过。实际步骤及首次结果记录在隔离账本证据，不采集私人内容。
4. **正式身份和迁移：**在上述问题定位后，Codex 取得新授权一次集中修复 A1 等确认缺陷，准备正式签名/包名/版本及 Preview→正式的备份迁移方案；先用合成数据验证。用户的正式数据迁移或任何真实覆盖均须另行明确许可。
5. **最终独立验收：**正式候选实际核验签名、Manifest、功能开关、数据迁移和 A 类结果，完成 G1/G2/G4 及 G3 延期处置决策后再由独立审查决定是否发行。已批准延期能力保持关闭；无需重复 C 类既有桌面测试。若批次 1 没有额外产品缺陷，批次 2 仍需解决已确认的 A1 和正式身份问题，不能跳过；批次 3 在门槛未齐前不启动。

本报告不自行进入设备测试、修复或正式构建。**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED / ANDROID_ISOLATION_RUNTIME_PENDING** 与历史 FAIL、UNKNOWN、NOT_RUN 保留。

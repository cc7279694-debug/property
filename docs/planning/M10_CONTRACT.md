# M10 本地封面与资源生命周期 Implementation Plan / Task Contract

**Goal：**长期项目与订阅各支持一张可查看、更换、移除的私有静态封面；失败保护旧图，共享引用与中断残留可安全管理。

**Architecture：**Compose/Nav3 → 单一封面ViewModel → MediaRepository → 当前ManagedGeneration的Room/coordinator及LocalImageStore。文件先完整落实，之后Room事务登记并更新引用；不宣称跨Room/文件原子。

**Tech Stack：**冻结Kotlin/Compose/Navigation3/Room/DataStore和Android内置图片API；minSdk26、targetSdk36。不增加图片处理依赖、表、索引或迁移。

**实施状态：**READY_FOR_REVIEW — M10；媒体、Repository、两详情及Nav3接口已接入，相同147输入的652项分开执行覆盖、Lint/assemble、两处真实SIGKILL、最终APK与实际系统旅程/设置复原已登记。文件清单/当前验收和本提交供审阅，Git回执按最终实际结果报告；独立验收PENDING，不自授冻结或进入M11。

**Spec / 来源：**[原M10计划](original-v1/lithic_v1_plan/docs/superpowers/plans/2026-10-10-m10.md)、原PRODUCT/UI/ARCH A02/A04/A07/A08、BACKUP B02～B05/B08及用户本轮完整M10执行指令。附件位于 `E:/CODEX/.codex/attachments/1d40c1e4-42a8-43a1-9960-baeec1c9292a/已粘贴的文本.txt`，SHA-256 `61638f3048268f9fcd2a2bddb050f6ffae7d863399288eacc077365cc502a8fc`。本契约细化已授权工程行为，不新增产品功能。

## Scope / Dependencies / Git

- 基线 `57de897917473f255d43dfb306a8433b366c00eb`，来源 `feat/m09-subscription-lifecycle`；只在用户指定工作目录的 `feat/m10-local-cover-media` 执行与推送。开工本地/远端一致、干净、目标分支此前不存在；549个旧测试方法与原XML完全对应。
- M00～M09已独立冻结；继承Room v2、v1/v2导出、唯一owner/coordinator、Money/BusinessClock/M03、记录时区和M09 A/B规则。
- 使用既有LocalMediaAssetEntity及两业务的coverAssetId。只更新coverAssetId与合法updatedAt；完整项目＋事件/订阅＋付款＋状态基线保护其他并发事实。
- Out of scope：创建表单提前选图、相册/相机/裁切/OCR/AI/云图片、广泛权限、备份导出/恢复/代切换、M11、main合并/force/公开APK、WHOCARE、依赖升级。

## Q05 用户确认

**USER_CONFIRMED：首次从相册导入规范化到安全私有资源；后续备份恢复验证已存资源的字节、SHA-256、尺寸、格式和上限，验证通过后保留原存储字节，不再次编码。**原相册字节无需等于规范化字节；完整恢复缺图/坏图不得静默完整成功。

M10只验证首次导入、失败保护、受控格式和多次读取字节稳定。Q05-V01～V03完整跨M11/M12往返仍NOT_RUN；不整体CLOSED。Q03/Q04/Q06与16组完整跨模块案例保持原期限，原43件/130案例/历史证据不改。

M12交接：已存资源读取只依赖Room元数据和实际受控文件，不需要M10导入凭据；恢复不重新编码。新generation须在完成路径、格式、尺寸、上限、SHA及业务关联验证后建立自己的有效归属/inode清理凭据，不能照搬旧generation、旧inode记录。该恢复协议与完整往返尚未实施或验证。

## 受控输入与编码

- 工程策略：仅接受完整静态JPEG/PNG，明确拒绝GIF、APNG、MPO、多帧/未知或不支持编码。检查真实容器/声明MIME，不仅依据扩展名。源读取与容器扫描有界，工程源流上限64MiB；超限明确失败。
- 先检查尺寸，Long安全比较原像素≤40,000,000，再采样解码；不会先完整加载超大Bitmap。输出最长边≤1600px、实际文件≤2,097,152字节。
- Android内置ExifInterface处理JPEG方向1～8；规范化不复制原EXIF/GPS。JPEG有限质量/尺寸阶梯；PNG保留alpha，只有限缩尺寸，不能静默JPEG化。重试有限，仍超限明确失败。
- 最终实际容器、实际解码尺寸、字节数与SHA核验后形成资源；已存读取永不重新编码。

## 共同类型与接口

`data/media/MediaContracts.kt`统一定义：

- `MediaResult<T>`：Success(value)或Failure(kind, message)，错误不暴露原始路径/堆栈；取消继续传播。
- `MediaFailureKind`：PATH、UNSUPPORTED、INVALID_IMAGE、TOO_LARGE、IO、DATABASE、CONFLICT、NOT_FOUND、IN_USE。
- `PreparedImage(asset: LocalMediaAssetEntity)`与`StoredImage(asset, bytes: ByteArray)`。
- `CoverTarget(type: CoverTargetType, id)`，type仅PROJECT/SUBSCRIPTION。
- `CoverBaseline.Project(project, events)` / `Subscription(facts: SubscriptionFacts)`；列表顺序不代表身份。
- `CoverCommand(operationId, expected: CoverBaseline, newAsset: LocalMediaAssetEntity?, writtenAt: Instant)`。newAsset=null为解除引用；精确目标保护后续修改，不宣称永久操作日志。
- `CoverCommitOutcome(baseline, replayed)`及`CleanupReport`，逐项保留删除失败/活动资源/未知文件边界。

`LocalImageStore(directory: File, dispatcher=IO, faults=NONE)`使用同代目录；故障注入仅为实际文件阶段验证端口，生产默认NONE。当前接口及受控边界：

- `importLocalImage(resolver, uri, assetId)` / `prepareImport(resolver, uri, assetId): MediaResult<PreparedImage>`。
- `restorePrepared(assetId): MediaResult<PreparedImage>`；按私有操作身份恢复，不持久保存源URI。
- `commitPreparedAsset(prepared): MediaResult<LocalMediaAssetEntity>`；不覆盖，完整核验后才可引用。
- `openAsset(asset): MediaResult<StoredImage>` / `verifyAsset(asset)`；有界读取，检查路径、尺寸、格式、长度、哈希。
- `releasePrepared(assetId)` / `isPreparedActive(assetId)` / `discardPreparedAsset(assetId)`；不推测删除已落实且可能被引用的新图。
- `managedAssetsForCleanup()`列出有证明的候选，清理无活动租约的已知未完成准备；未知文件保留。`removeUnreferenced(asset)`在生产owned实例经绑定守卫获取coordinator、核对两类引用及归属后才删除；独立文件测试实例没有Room，只测试底层文件协议。
- internal `bindRemovalGuard`只绑定一次；`validateOwnedRemoval`在元数据删除前校验归属；`removeAfterReferenceCheck`仅供已持同一协调器并核对双引用的Repository/守卫使用，避免再次获取非重入锁。整个生产generation只有一个images实例和活动租约集合。

`MediaRepository(database, coordinator, images, clock, resolver)`：

- `loadTarget(target): MediaResult<CoverBaseline>`，完整基线及关联事实。
- `prepareImport(uri, assetId)`、`restorePrepared(assetId)`、`discardPrepared(assetId)`，委托同一个images实例。
- internal `releasePreparedLease(assetId)`以NonCancellable释放取消回传后的内存租约，不删除可恢复字节；Store和Repository两层调度边界均处理这种取消。
- `commit(command): MediaResult<CoverCommitOutcome>`；先认精确已存目标，避免重放新文件/重复编码；否则完整CAS后提交。
- `verifyCommitted(command): MediaResult<CoverBaseline>`，COMMITTED只读核对；Conflict与读取失败均不盲写。
- `openAsset(id): MediaResult<StoredImage>`；在withSnapshot内完成校验和有界字节读取，不解锁后才开文件。
- `removeUnreferenced(): MediaResult<CleanupReport>`；与未来M11一致快照共用Mutex，不得嵌套非重入协调调用。

## 文件及中断协议

1. 最终仅 `media/<UUID>.jpg` 或 `.png`；严格canonical父子边界、逐层拒绝符号链接、绝对路径、反斜杠和穿越。当前代外不可读取/清理；最终资产不可覆盖。
2. `.m10-imports/<UUID>/`保留`owner.json`、`asset.json`及`owned-final.json`小型操作/元数据/inode归属凭据，属于中断核对/清理凭据，不是第二媒体真值、永久用户草稿或备份。活动lease保护准备，取消回传以NonCancellable释放lease、传播取消、保留可恢复字节。未知或无效凭据保留；仅有有效owner、没有asset登记的已知未完成准备可清理其已知临时文件，未知子文件仍保留。
3. 同一协调写保护下先复核完整业务基线，再以`O_CREAT|O_EXCL|O_NOFOLLOW`及0600排他创建最终文件。同步generation/UUID/设备号/inode所有权凭据后才复制有界准备字节；最终文件与media目录fsync及实际格式/尺寸/长度/SHA核验完成后，Room同事务insert metadata＋仅cover/updatedAt CAS，并核对真实行数与完整目标。
4. Android SELinux不必支持hard link，正式提交不依赖它。最终文件创建后、有效归属凭据形成前中断的残留无法证明归属，按unknown保守保留；不能只凭UUID名称清理。凭据已同步后的部分最终文件未获Room引用，可在双引用及归属校验通过后回收，不登记不完整文件为封面。
5. 已有同ID最终文件必须匹配归属凭据和实际字节；重试再次完成最终文件及media父目录fsync和核验，不因“文件已存在”跳过持久化，也不覆写最终字节。完成后仅删除已校验的冗余准备图片并同步准备目录，保留所有权/元数据/inode凭据及未知文件；此后同ID恢复使用最终文件。
6. DB失败保留旧引用/旧字节，允许留可识别孤儿；文件落实后DB前中断可按归属核对安全回收。DB后旧图清理前中断仍读新图；结果未收到以固定命令/资源身份及完整基线核对，不创建另一资源或盲删。COMMITTED仅`verifyCommitted`只读核对，不再次写入。
7. 清理先验证活动lease、完整归属凭据及最终inode，再事务重复核对project/subscription两类引用并精确CAS移除无引用metadata，最后物理删除与同步。未知或替换文件的metadata不能先删；物理失败保留凭据、逐项报告并可重试。锁顺序coordinator→文件mutex，GC不嵌套coordinator；快照读取完成前不可物理删除文件。
8. 封面保存/移除及原M07项目删除成功后分别进行媒体清理；媒体失败不改写业务成功。不在恢复活动准备身份前无条件启动GC，不删除整个media目录，不接触其他generation。完整备份恢复、代切换和Q06故障回退仍归M11/M12。

## 任务与文件归属

- [x] Task 1：新增两详情入口行为测试，已在旧生产代码上取得真实FAIL；正式测试Runner/编译错误单独保留诊断。
- [x] Task 2（源码实施）：LocalImageStore及容器/方向/文件边界实现与测试已接入；先取得有效行为RED，再最小实现，安全源URI provider/工具仅在androidTest。最终GREEN以当前执行登记为准。
- [x] Task 3（源码实施）：MediaRepository及三DAO受控接口已接入，真实Room/文件测试覆盖故障、共享引用、完整CAS、并发重放、快照/清理互斥、公共store引用守卫与取消租约。最终执行结果另登记。
- [x] Task 4（源码实施）：共用CoverViewModel/Display、CoverPicker及两详情/Nav3入口已接入；三者全局pending/dirty守卫、单SavedState信封、稳定身份、COMMITTED只读、准备损坏后显式重读及外部URI失效重选均已实现。只有原三个底部Tab，金额/状态层级继承；源码接入不代替设备验收。
- [x] Task 5：当前owner装配、Debug code8/0.1.0-m10-debug、完整549＋新增回归/Lint/build、最终APK核验、限定API36真系统选择器及媒体实际路径/断网/字体/旋转/已保存任务恢复、旧数据完整与设备设置恢复、交付文档；本提交与最终Git回执核对。独立验收仍PENDING。

Task 5结果见[14项实际报告](../evidence/M10-verification.md)：相同147项输入下652个唯一方法PASS（86 JVM与566 Android分开执行）、Lint/assemble、两处真实SIGKILL、最终APK及实际设备路径/设置复原核对。第一整体phase的9项Android失败和早期诊断保留，不能伪称一次652项BUILD SUCCESSFUL。原AVD误重建后使用受控M09合成fixture并实际M09→M10升级，不能称原设备连续保留或M12产品恢复。文档/索引核查不计产品测试；只交付READY_FOR_REVIEW，M11仍未授权。

## Acceptance / Verification / Review Focus

M10-T01～T05须按用户指令全部执行并留证：源删除/重开仍私有读、源/合并Manifest/实际APK无广泛权限与选择取消零写、六种替换故障保护旧引用/字节/成本状态、有界解码全类输入、严格私有相对路径/不可变ID。追加20项安全要求逐项映射到真实测试/设备证据；不按数量凑验收。

最重要补测：PNG CRC/截断与JPEG EOI/多帧拒绝；方向/alpha与最终元数据；文件先DB后的两崩溃边界；共享项目/订阅及未知文件/活动lease保护；回传丢失同身份核对与陈旧完整基线；系统选择器/旋转/隐藏pending离开守卫。

根代理负责实际执行并登记 `:app:testDebugUnitTest`、`:app:connectedDebugAndroidTest`、`:app:lintDebug :app:assembleDebug`；保持旧549方法身份/断言，逐方法核对源码、最终XML及构建输入。所有Gradle/ADB由root串行调度，构建期间冻结生产与测试输入，限定ANDROID_SERIAL=emulator-5596/Lithic_M01_API_36/API36；不接触其他AVD/真机或改宿主时钟。

API26实际运行、真机/OEM/Release/完整TalkBack/真实LMK若未运行则NOT_RUN；配置或模拟器不能冒充平台覆盖。原失败日志保留。未完成验证不得写PASS。只交付READY_FOR_REVIEW — M10后停止，独立验收PENDING，不进入M11。

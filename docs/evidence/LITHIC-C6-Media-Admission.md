# C6-0 真实物品封面安全准入

2026-10-28；`cc7279694-debug/property`；`codex/lithic-v1-product-completion`；审查基线`103535ec874dff3ad7faea6b67ebee626e4432e3`，开始时本地/远端一致、工作区干净。上一轮精修的独立接受仅限桌面实施。

**生产实施判定：BLOCKED（完整封面闭环）。** 已登记媒体存在可审查的只读接入方案，但当前没有生产读取/变更接口，新JPEG导入的严格拒绝及资源/异常收口门禁也未通过。不能把方案、旧UI冻结或备份成功当作完整媒体准入；本轮不开始下一阶段实现。

## 1. 当前事实与可复用范围

| 现有能力 | 缺口与证据 |
|---|---|
| Room5已有媒体元数据、两类`coverAssetId`及RESTRICT外键 | [媒体实体](../../app/src/main/java/com/cooper/lithic/data/local/ProjectEntities.kt#L32)只校UUID、路径语法、正尺寸、非负长度和SHA格式，**不认证文件字节**；[订阅引用](../../app/src/main/java/com/cooper/lithic/data/local/SubscriptionEntities.kt#L12)也已存在，无需新增字段。资产所属代由Owner的数据库和目录共同确定，不是全局assetId。 |
| 已有Owner、同代协调器、恢复退休隔离 | [ManagedGeneration](../../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt#L26)与[协调器](../../app/src/main/java/com/cooper/lithic/data/repository/LocalWriteCoordinator.kt#L7)可复用；[Ready](../../app/src/main/java/com/cooper/lithic/app/LedgerUiState.kt#L17)未暴露媒体facade。恢复回滚可[重开同一id的新Owner](../../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt#L334)，正常shutdown也不等于retire：仅检查generationId或持有旧协调器不足以取得永久读资格。 |
| 数据层能读媒体元数据 | [MediaDao](../../app/src/main/java/com/cooper/lithic/data/local/MediaDao.kt#L9)只有insert/list/find；没有MediaRepository、页面reader、封面CAS、跨类型引用计数或媒体GC。项目普通编辑CAS保护旧cover而不更新它；[删除项目](../../app/src/main/java/com/cooper/lithic/data/repository/ProjectWriteRepository.kt#L213)明确保留媒体。不能借普通表单直接修改封面。 |
| UI已有身份区及部分图片槽 | [Home](../../app/src/main/java/com/cooper/lithic/feature/home/HomeShell.kt#L99)只传引用有无；[EntryCard](../../app/src/main/java/com/cooper/lithic/core/design/EntryCard.kt#L64)、[项目详情](../../app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt#L85)有展示槽，[订阅详情](../../app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionDetailScreen.kt#L79)仍是不可用图标。真实添加、查看、替换、移除均未接线，继续MEDIA_DISPLAY_PENDING。 |

备份可复用的是**原有校验规则**：[BackupMediaSnapshot:21–102](../../app/src/main/java/com/cooper/lithic/data/backup/BackupMediaSnapshot.kt#L21)要求登记文件`0 < 长度 < 2MiB`、每边1～1600px（至多256万像素）、PNG/JPEG；这些是已登记成品限制，不是原始照片输入许可或导入峰值承诺。联合核对目录/文件UID、普通单链接文件、路径归属、O_NOFOLLOW/CLOEXEC、FD与路径前后dev/ino等身份、长度/SHA、格式和尺寸。其`copy()`会写导出暂存并全尺寸解码，**不能直接当页面reader**。[同代快照消费](../../app/src/main/java/com/cooper/lithic/data/backup/RoomBackupSnapshotSource.kt#L10)的Owner检查值得继承，禁止UI裸路径读取或自建/关闭存储Owner。

[format4清单](../../app/src/main/java/com/cooper/lithic/data/backup/BackupManifest.kt#L38)包含**全部登记媒体，含未引用资源**；[恢复校验](../../app/src/main/java/com/cooper/lithic/data/backup/RestoredGenerationWriter.kt#L224)在新代验证长度、SHA、格式、尺寸及可读性，保持原字节，不重新规范化。显示接入不得改变此合同或恢复Token/指针。

## 2. 旧代码与三个JPEG失败

旧UI-C1 `0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`的[CoverPreviewLoader](https://github.com/cc7279694-debug/property/blob/0dce7dd0725a54be4f0280870ef6ca47c48ea7ed/app/src/main/java/com/cooper/lithic/feature/media/CoverPreviewLoader.kt#L68)及同目录`CoverDisplay.kt`可参考验证后缓存、按目标尺寸采样、状态反馈和离页取消；不能直接复制其缺失的旧MediaRepository合同。8MiB只是LRU上限，**不是进程峰值上限**；decode前后ensureActive不证明native解码已被取消。旧Picker/ViewModel包含写入、租约及清理，也不是纯UI可迁入部分。

历史[原始补验报告:54–75](https://github.com/cc7279694-debug/property/blob/732333b7a2803ec1f4487e437a581979a270e3ee/docs/evidence/M10-JPEG-supplement-verification.md#L54)已通过精确ref读取，三个FAIL保持：

- `M10JpegIntegrityTest.jpeg_entropy_truncated_with_eoi_is_rejected`
- `M10JpegOldCoverProtectionTest.project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved`
- `M10JpegOldCoverProtectionTest.subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved`

均在缺损JPEG仍保留EOI、应拒绝却由prepareImport返回Success处失败。坏准备图未提交；不能推断已有金额破坏，也不能把未到达的失败租约/清理断言记为PASS。[当前JPEG检查器](../../app/src/main/java/com/cooper/lithic/data/backup/BackupJpegInspection.kt#L5)明确不认证熵流；SHA相符或BitmapFactory可解码不解决这些FAIL。

旧M10 [B1/B2候选:67](https://github.com/cc7279694-debug/property/blob/732333b7a2803ec1f4487e437a581979a270e3ee/docs/research/M10_B1_RECOVERY_B2_RESOURCE_CANDIDATE.md#L67)虽已有宿主修正，Android异常恢复及40MP全链资源/释放仍未通过；不得把早期已修问题当最新确定缺陷，也不得迁入未冻结Worker、JNI或Stage V2。已登记JPEG原字节备份的限定认可不等于新导入安全；也不能因新导入阻塞而拒绝所有合法登记JPEG备份。

## 3. 两条路径的准入门禁（尚未实施/验证）

**只读显示**：

1. 从现有Owner装配只读facade；在同一协调边界核对记录→coverAssetId→元数据及当前Owner有效性，再读取受限字节。复用[先验证Owner再碰Room](../../app/src/main/java/com/cooper/lithic/data/backup/RoomBackupSnapshotSource.kt#L17)的防护，关闭后不能经旧DAO重开库。校验应覆盖上述文件身份/摘要/格式/像素，不为显示调用导出或恢复写流程。
2. 锁内只捕获并验证有界字节，锁外后台按显示尺寸解码；限制并发、在途字节/像素和可见位图总预算。取消、关闭、解码失败须释放本请求资源；native硬取消时限和总峰值目前UNKNOWN，不以协程cancel或LRU容量冒称证明。
3. 请求绑定Owner实例、generation、记录类型/ID、asset身份/元数据及目标尺寸；发布前复核当前请求。换代、恢复和同id重开先撤旧图，拒绝迟到结果。首版可不建全局/磁盘缓存；若缓存，每次新展示仍验证当前文件，失败失效，不能用成功缓存长期掩盖坏图。
4. 无引用不读文件；元数据缺失、文件缺失、归属未知、校验失败、解码失败及旧代取消分态反馈，不伪装“未设置”，不自动删图、修元数据或修改业务记录。登记字节完整性、Android可读性、严格JPEG完整性分别记录；当前尚无生产显示安全通过证据。

**新增/替换/移除**：

1. 原始输入另设有界读取、格式/像素/资源和严格完整性门禁，必须能拒绝原三个失败案例且保持既定支持范围内合法静态输入的兼容，再按Q05首次受控规范化；不能先重编码将坏输入“洗成”合法输出，也不能未经批准缩成PNG-only。尚无可放行的验证器和Android异常/资源证据，**新增/替换阻塞**。
2. 需独立媒体Repository与封面CAS，复用现有同代协调器/Room事务：先取得已验证且归属明确的新文件，再一起提交元数据和引用；CAS失败保旧封面，提交结果未知不得重复写或贸然删准备文件。保持operationId、未确认写入及Nav3离开保护，不声称Room与文件跨存储原子。
3. 移除先只解除本记录引用、保留原元数据和文件；否则“删文件留登记”会使完整备份失败。这样不释放磁盘空间，也不算完成GC。物理清理另须证明跨项目/订阅零引用、提交结果、文件归属及成对元数据/文件收口；共享、未知、旧代资源保留，不能清扫目录兜底。
4. 门禁至少补同代/跨代及同id重开迟到、坏/缺/换文件、摘要尺寸不符、取消/失败释放、共享引用、替换失败保旧图、未知提交防重复和含未引用媒体的format4原字节往返。均待专项授权与独立审查；本轮未运行，不恢复M10旧设备实验。

## 4. 下一阶段最小文件范围

以下仅为建议，**未授权实施**：

- 读取阶段：拟新增`data/media`受控reader及`feature/media`缩略加载/显示组件；在`LedgerUiState`、`LedgerUiStateHolder`、`LithicNavigation`现有同代作用域注入；接Home、EntryCard、项目/订阅当前及历史列表、两类详情。现有槽可复用，搜索DTO不在本轮扩展；不自建Owner，不改生命周期、恢复及备份协议。若要抽共享验证器，必须另审备份回归影响。
- 变更阶段：安全输入验证器先独立通过，再拟增媒体Repository、必要的`MediaDao/ProjectDao/SubscriptionDao`引用CAS/查询、封面操作VM/系统选择入口和路由；对应最小定向测试。现有字段足够，不需要改Room5/backup4；当前不选择或迁入未验证处理器。

## 本轮核验与停止

仅读取当前源码、历史Git对象及已提交报告；静态检查引用、差异、敏感信息和Git一致性。**没有新测试、构建、APK、ADB、手机/模拟器、照片或账本读取**，不把历史测试当本轮重跑。唯一新增本报告；Room5、backup4、金额/订阅规则、Owner、恢复与原子切换、版本/证书均未改。M10延期/三FAIL、M20平台FAIL/UNKNOWN、M22与C3历史状态、M13-T06和G1～G4保留。

**C6-0 MEDIA ADMISSION — READY_FOR_INDEPENDENT_REVIEW**。完整生产准入仍BLOCKED，MEDIA_DISPLAY_PENDING；提交推送后停止等待ChatGPT独立审查，不开展媒体实现、不合并main、不进入M23。

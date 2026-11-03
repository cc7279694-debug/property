# LITHIC V1-B3 — 照片空间管理

基线 `a782b23c2f4b352953660b523ed74bc6e53202da`。用户反馈已亲自完成独立 B1-QA 的 A～H 人工旅程，结果正常，登记为 **USER_REPORTED_MANUAL_PASS_WITH_NOTES**。该登记来源是用户反馈；本阶段未取得逐步原始日志或独立复验，不能扩大为 Android 自动测试、异常中断、完整资源释放或正式发行通过。

## 本阶段结果

**SOURCE_PREPARED / BUILD_DEFERRED；永久删除 BLOCKED**。源码在设置原有照片存储区加入只读空间管理汇总，不提供单张历史图片浏览、删除入口、后台清理或 GC。历史照片仍可能占用空间，并继续参加完整备份。展示未引用媒体的登记字节数，作为潜在空间的参考；不是已验证的物理占用，也不承诺能够回收。

- [PhotoStorageReader](../../app/src/main/java/com/cooper/lithic/data/media/PhotoStorageReader.kt) 增加必填历史登记字节，拒绝负值、超过登记总额和与引用数矛盾的结果，不以零代替未知。[GenerationPhotoStorageReader](../../app/src/main/java/com/cooper/lithic/data/media/GenerationPhotoStorageReader.kt) 在现有同代Owner、快照和Room事务内，对项目及订阅的去重引用反集合求精确整数总和；取消、换代、闭库、异常和整数溢出仍保守处理，不向UI暴露路径。
- [DataConfidenceSections](../../app/src/main/java/com/cooper/lithic/feature/settings/DataConfidenceSections.kt) 将设置内现有区域作为轻量只读管理区，显示登记数、两类业务共享引用数、历史未引用数、历史登记体积和登记总体积，并明确清理与逐张查看暂不可用；无需进入新页面，无清理按钮。真实文件是否存在、物理分配及真正可释放数量仍 **UNKNOWN**。
- 已准备`PhotoStorageSummaryTest`（14项，含4项新增）、`PhotoStorageControllerTest`（11项）、`GenerationPhotoStorageReaderTest`（9项，含1项新增）及`DataConfidencePresentationTest`（4项），覆盖精确字节、跨两类引用去重、换图/取消、旧Owner、只读显示和禁用入口。原有断言保留，测试源码尚未编译或运行。
- 实际本轮仅做源码/边界静态审阅、`git diff --check`与19份历史APK大小/SHA核对。用户选择“先完成源码，构建暂缓”；因此Gradle、JVM RED/GREEN及所有定向测试、AndroidTest编译、构建、Lint均 **NOT_RUN / DEFERRED**，没有以源码检查冒充测试通过。参见[源码静态记录](v1-b3-20261103/desktop-1/static-review.json)。真实Android、SAF、异常中断与资源峰值均 **NOT_RUN/UNKNOWN**。
- 永久删除安全门禁未满足，按本任务约束不生成Preview17或其他用户APK；Preview16及既有独立候选均保留。用户恢复验证后须先重新核对至少4GiB可用内存，再运行上述定向JVM、AndroidTest源码编译、Debug构建与Lint；不自动进入设备验证或修改删除协议。

## 为什么不能现在开放永久删除

- [MediaDao](../../app/src/main/java/com/cooper/lithic/data/local/MediaDao.kt) 现仅提供插入、列表和查找，没有跨物品及订阅引用重验的条件删除，也没有可恢复的媒体删除账本。两类 `coverAssetId` 外键的 RESTRICT 可阻止仍被引用的行直接删除，但无法将文件操作与 Room 事务合为一个原子操作。
- [BackupManifest](../../app/src/main/java/com/cooper/lithic/data/backup/BackupManifest.kt) 和 [BackupMediaSnapshot](../../app/src/main/java/com/cooper/lithic/data/backup/BackupMediaSnapshot.kt) 的 format4 合同包含**所有已登记媒体**，包括没有当前业务引用的照片。若先删除文件、后删除数据库记录，进程在中间退出会留下指向缺失文件的登记项，使后续完整备份失败；若先删除记录、后删除文件，中断会留下失去登记关联的文件，现有协议没有证明其归属、恢复或安全回收资格的持久凭证。异常和未知结果不能以扫描或猜测删除来补救。
- [RoomBackupSnapshotSource](../../app/src/main/java/com/cooper/lithic/data/backup/RoomBackupSnapshotSource.kt) 的同代协调器锁覆盖数据快照及[备份媒体复制](../../app/src/main/java/com/cooper/lithic/data/backup/BackupExportService.kt)，但之后归档组装和 SAF 写入仍可能继续。[RestoreCoordinator](../../app/src/main/java/com/cooper/lithic/data/backup/RestoreCoordinator.kt) 预览校验、等待确认及候选准备也并非整体持有该锁。现有各页面 phase 是展示状态；单凭短时数据库锁或页面空闲不能证明整个备份、恢复、照片提交均已结束。
- [GenerationCoverReader](../../app/src/main/java/com/cooper/lithic/data/media/GenerationCoverReader.kt) 需要现存记录及匹配的当前封面身份，未被引用的历史照片没有已认证的单图读取入口；本阶段不绕过它拼接私有路径、不展示虚假缩略图。

## 后续专项设计及验收门禁（尚未批准或实施）

1. 在不更改 Room5、backup4 和原恢复 Token/代切换协议的前提下，设计受当前 Owner 管理的跨操作资格门禁；覆盖备份从准备到终结、恢复预览/确认、照片提交、代切换、关闭及 UNKNOWN。不能用 UI 状态代替存储层资格；未知或旧 Owner 一律拒绝。
2. 用户明确确认后，在同一可信事务内重新核对**所有物品与订阅（含历史记录）**的引用，核对媒体元数据及无引用条件。共享引用、旧选择、重复确认或发生换代都要保守拒绝；外键 RESTRICT 只是额外保险。
3. 候选顺序供独立审查：先将本代、操作 nonce、原媒体元数据及经验证的文件身份写入持久删除意图并确保持久化；在独占操作资格下将该文件隔离至本次专属位置并验证目录项及持久化；Room 事务中再重查两类引用及原元数据，条件满足才删除媒体记录；确认提交后再校验隔离文件身份并移除。**隔离至事务结束期间暂停备份及其他依赖媒体的操作**。任何一步结果不确定时保留账本与现场，不能报告释放成功。隔离/重命名、目录持久化和文件身份不被替换必须由目标 Android 文件系统及受控调用证实，不能把普通路径检查直接当作原子证明。不能向 UI 泄露私有路径，也不依赖 PID 或单一文件锁证明关闭。
4. 针对每个文件与数据库步骤前后注入崩溃、取消、磁盘错误和结果未知；在下一次开放读写及备份前，依据经认证的账本与实际 Room 状态恢复到一致状态。无法确认身份、提交结果或文件归属时冻结相关操作并保留现场，不自动重试或扫描删除陌生文件。只有证明该流程后，才能评估已确认操作的清理和恢复策略。
5. 以独立可丢弃账本验证项目与订阅共享引用、正常/失败/中断、连续备份与恢复、备份期间及恢复预览期间禁止操作、旧 Owner/迟到结果、不同系统与存储环境。JVM、编译和源码审查不能替代 Android 实际故障注入、真实 SAF 及重启后的证据；所有尚缺证据均保持 NOT_RUN/UNKNOWN。

本阶段不操作手机、模拟器或真实照片，不更改金额/订阅规则、Room5、backup4、Owner/generation 或恢复协议。**V1_RELEASE_NO_GO / STRICT_JPEG_IMPORT_BLOCKED** 及全部历史 FAIL、UNKNOWN、NOT_RUN 保持。

# M10 — 本地封面、图片导入与资源生命周期

**READY_FOR_REVIEW — M10，2026-10-14（Asia/Shanghai）；独立验收 PENDING。**本文件按用户执行指令的14点顺序登记实际证据。652项自动化覆盖来自相同147项输入的分开执行；两处真实进程边界和最终APK的设备路径另有实际证据，不替代独立验收。唯一实时状态仍为[CURRENT_STATE](../CURRENT_STATE.md)。

本轮完整授权附件 SHA-256：`61638f3048268f9fcd2a2bddb050f6ffae7d863399288eacc077365cc502a8fc`。基线 `57de897917473f255d43dfb306a8433b366c00eb`，来源 `feat/m09-subscription-lifecycle`，本轮 `feat/m10-local-cover-media`。范围见[M10契约](../planning/M10_CONTRACT.md)；原43份规划、24个模块、130条原案例及16组完整跨模块边界保持不可变。

## 1. 图片导入与受控编码方案

两类正式详情进入共同[CoverPicker](../../app/src/main/java/com/cooper/lithic/feature/project/CoverPicker.kt)，使用 `PickVisualMedia(ImageOnly)` 单图系统契约。Photo Picker 不可用时由既有AndroidX契约回退系统文档选择器；API26实际运行另记NOT_RUN。外部URI只用于本次受控准备，不入库、不写入SavedState，不依赖长期URI授权。取消结果不开始准备、不写业务事实。

[AndroidImageCodec](../../app/src/main/java/com/cooper/lithic/data/media/AndroidImageCodec.kt)检查声明MIME和真实容器，仅接受完整静态JPEG/PNG；拒绝GIF/APNG/MPO、多帧、未知或不支持编码。PNG检查CRC、IHDR、IDAT/IEND及动画标记；JPEG检查帧、扫描、终止和多图标记。JPEG方向1～8落实为实际像素方向，重新编码不复制EXIF/GPS；PNG保留alpha，不为压缩而静默转JPEG。JPEG使用有限质量及尺寸阶梯，PNG使用有限尺寸阶梯，超限返回明确失败。

## 2. 文件尺寸、40MP及2MiB边界

工程源流上限64MiB，读取和容器扫描有界。原图像素上限40,000,000；先检查正尺寸及Long安全比较，再采样解码，避免先完整分配超大Bitmap。输出最长边≤1600px，实际编码文件≤2,097,152字节；压缩重试次数有限，编码后再次检查实际容器、解码宽高和文件长度。

新增测试覆盖真实超过40MP的输入、超大/零尺寸、异常PNG CRC/截断、JPEG截断、不匹配MIME、非图片、动画/多帧、不受支持格式、64MiB源上限、编码仍超2MiB、PNG透明及八种EXIF方向。`green-final-lifecycle`中这些行为测试实际通过；源码方法/XML身份见第9节。测试拒绝结果不能用进程OOM崩溃代替。

## 3. 本地私有文件及SHA-256元数据

[LocalImageStore](../../app/src/main/java/com/cooper/lithic/data/media/LocalImageStore.kt)使用当前ManagedGeneration的私有 `media/`；最终只保存 `media/<canonical UUID>.jpg` 或 `.png`。替换使用新资源ID，正式文件不可覆盖。解析逐层拒绝链接、绝对路径、穿越、反斜杠和逃出当前代的目标。

复用Room v2既有 `LocalMediaAssetEntity` 七字段：id、relativePath、mime、width、height、byteLength、sha256；元数据来自实际规范化后的文件，不保存图片Blob、相册URI或派生成本。读取在同一协调快照内完成元数据、实际字节、尺寸、格式和SHA核验，再将独立字节交给界面；多次读取不重新编码。[最终设备私有字节核验](m10-device/manual/media-m10-device-final-restored.json)PASS：两份900×600图片，JPEG6005 bytes与PNG5645 bytes；实际SHA、格式、尺寸、无GPS及PNG alpha[0,255]逐项一致。仅发布受控合成metadata，不发布私有图片字节。

## 4. 跨Room/文件系统的一致性协议及残留风险

准备阶段在当前代 `.m10-imports/<UUID>/` 生成受控文件及owner/asset凭据，不修改业务coverAssetId。活动租约保护编辑中资源。Store、Repository、ViewModel取消路径以NonCancellable释放已经完成但未收到结果的内存租约，传播取消并保留可恢复字节；ViewModel清除不盲删可能已经引用的正式文件。

确认阶段复用唯一LocalWriteCoordinator，先比较完整项目＋事件或订阅＋付款＋状态基线。以 `O_CREAT|O_EXCL|O_NOFOLLOW`、0600排他创建最终文件，同步generation/UUID/设备号/inode归属凭据后复制准备字节。最终文件和media目录fsync、真实内容核验完成后，Room同事务插入元数据并仅CAS更新coverAssetId/updatedAt，末尾核对完整目标。正式提交不依赖Android SELinux可能禁止的hard link。

既有最终文件重试仍完成fsync和内容验证；精确已存目标重放不再分配资源。成功落实后只删除已校验的冗余准备图片，保留小型owner/asset/inode凭据及未知文件。COMMITTED恢复只读核对，不再次保存。固定命令和资源身份用于结果未知核对，不提供永久操作审计日志。

**Room和文件系统不构成跨资源原子事务。**DB失败保护旧引用与旧字节，可留下有证明的孤儿；有有效inode凭据的无引用部分文件可以回收。排他创建后、有效凭据形成前的中断残留无法证明归属，按unknown保守保留。凭据损坏、inode被替换、未知文件和其他generation也保留。实际两处SIGKILL已执行并核对，证据见第10节；不将故障注入/对象重开写成真实杀进程，也不把SIGKILL当作断电或真实LMK证明。

## 5. 长期项目和订阅封面操作

两类正式详情支持添加、查看、替换、移除单封面。[CoverDisplay](../../app/src/main/java/com/cooper/lithic/feature/media/CoverDisplay.kt)显示验证后的私有字节；缺失/损坏与尚未设置明确区分，错误不会清除引用。准备新图成功后仍须明确保存才替换旧图；删除封面先确认。原名称、金额、日期、类别、付款和状态受完整基线保护。

[CoverViewModel](../../app/src/main/java/com/cooper/lithic/feature/media/CoverViewModel.kt)使用单SavedState信封保存目标、current/history来源、固定操作/资源ID、时间、完整基线和提交阶段，不保存源URI。项目、订阅、封面共同参与Nav3全局pending/dirty守卫；未保存准备需明确弃改，隐藏待核对任务有恢复入口。损坏准备或陈旧事实须显式重读；COMMITTED只读。SavedState不是永久草稿，不保证force-stop、断电、系统未保存状态或真实LMK恢复。

Nav3规范化SideEffect只在背栈仍等于本次观察值时应用，避免旧渲染覆盖后来业务提交的导航。此缺陷有真实失败语义树和新增导航GREEN证据；不是通过延长超时规避。

## 6. 共享资源、引用解除与removeUnreferenced

[MediaRepository](../../app/src/main/java/com/cooper/lithic/data/media/MediaRepository.kt)与生产owned store公共删除入口共同检查项目、订阅两类真实引用。锁顺序coordinator→文件mutex，GC内部不嵌套非重入coordinator。先验证活动租约、归属及实际inode，再在Room事务中重复检查双引用并精确CAS删除无引用元数据，之后物理删除并同步。

任一对象仍引用的共享资源保留。物理删除失败保留凭据、逐项报告并可重试；未知/替换文件的元数据不能先删。封面提交/移除及原M07项目删除成功后独立执行清理，媒体失败不会把已成功业务写入改成失败。读取快照结束前不能删文件；恢复活动准备身份前不无条件启动GC；不删除整个media目录或其他代。

## 7. M10-T01～M10-T05真实执行结果

以下自动化行为现由第9节相同147项输入下的[652项严格覆盖核对](m10-tests/final-split-verification.json)支持。此前[103项GREEN摘要](m10-tests/green-final-lifecycle-summary.json)与[XML](m10-tests/green-final-lifecycle-android-0.xml)保留其当时限定运行历史；均不能代替最终设备路径或独立验收。

| 案例 | 已有实际证据 | 当前实际结果与边界 |
|---|---|---|
| M10-T01 copy_survives_source_removal | Store真实ContentResolver URI导入、删除源后私有读取、准备身份恢复及Repository代重开；实际字节/元数据保持 | **PASS**：最终APK实际系统选图；仅删除已核对哈希的合成A源，冷启动仍查看同一私有封面，SHA保持 |
| M10-T02 no_broad_storage_permission | null选择取消零业务写入测试；实际APK只含自定义非导出接收器权限，Manifest/九域备份排除核对 | **PASS**：实际系统取消前后七表/设置完全相同，0媒体写入；最终APK无广泛存储/相机/网络权限 |
| M10-T03 replacement_failure_keeps_old_cover | 六类解码/编码/准备写入/SQLite插入/完整CAS冲突/业务更新失败；旧引用、字节、成本和状态保护，触发器回滚及孤儿边界；当前完整输入自动化覆盖PASS | **PASS**：实际选择器缓存合成缩略图后将其源替换为34-byte无效输入，明确错误且旧B/七表/私有字节完全相同 |
| M10-T04 bounded_decode | 40MP前置拒绝、有界分配/输出、坏容器、MIME、方向、alpha等测试在当前完整输入覆盖中PASS | **PASS（API36实际自动化）**；API26实际运行NOT_RUN，不推广为API26通过 |
| M10-T05 private_relative_paths | UUID不可覆盖、越界/其他代/链接拒绝、无源URI、读取字节稳定；旧MetadataValidationTest覆盖绝对/穿越/反斜杠输入，当前完整旧路径回归PASS | **PASS**：实际DB相对UUID路径/metadata和私有字节一致，最终两资源路径无content URI或外部绝对路径 |

## 8. Q05当前实现及未来M11/M12未完成部分

Q05为**USER_CONFIRMED，非整体CLOSED**。用户确认首次相册导入规范化；后续完整备份恢复校验已存图片字节、SHA、尺寸、格式和上限，验证后保留原存储字节，不再次编码。原相册字节不要求与规范化后字节相同。本轮实现首次导入、已存严格验证和重复读取稳定。

M11完整导出、M12覆盖恢复/代切换/验证后回退及Q05-V01～V03完整往返仍**NOT_RUN**。已存封面读取不依赖M10导入凭据；M12须在新代完成资源及业务关联校验后建立新的generation/inode清理归属凭据，不能照搬旧凭据或重新编码已验证存储字节。缺图/坏图不能静默完整成功。Q03/Q04/Q06期限、历史NOTES及16组完整跨模块验收边界继承。

## 9. 原549项回归和所有新增测试

2026-10-14已严格核对当前源码与所选实际XML：**86 JVM＋566 Android＝652个唯一方法PASS，旧549＋新增103，0失败/错误/跳过，源码/XML EXACT MATCH。**新增为Store47、Repository36、ViewModel11、Routes4、Nav3守卫3、详情入口2。旧两份UI测试驱动仅作精确同步增强，15旧方法、117条完整断言及原点击次数保持；44份其他旧测试源码原字节不变。

最终覆盖来自**相同完整147项输入下的分开执行**，不是一次“652项BUILD SUCCESSFUL”：

- `green-full-runner-final`的forced JVM子命令实际退出0、67.033秒，7份XML共86 PASS；该阶段整体仍FAIL（退出1），其566项Android中的557 PASS/9 FAIL全部排除，不能借最终覆盖改写该阶段。
- `green-android-systemui-clean-final`实际Android＋Lint＋assemble命令退出0，命令墙钟639.900秒、完整阶段639.901秒，日志 **BUILD SUCCESSFUL in 10m 39s**；566 Android全部PASS，Lint0 errors/23 warnings，assemble完成。两处命令、独立时长、原始XML和所选方法身份见[严格覆盖核对](m10-tests/final-split-verification.json)与[652项逐方法登记](m10-tests/final-split-methods.json)。

两份147条完整ZIP、路径/原字节SHA及当前输入逐项相同，不能复用注册修复前的旧86 JVM。Lint [实际XML](m10-tests/lint-results-debug.xml) SHA为 `dbfa135058cf57adc037374cffbca7378d39095b61c8320441af44d2e3a64a58`，23条警告原样保留，未借此升级冻结工具链。

| 执行阶段 | 实际方法与结果 | 可证明的范围 |
|---|---|---|
| green-final-lifecycle | 103 Android PASS，0失败/错误/跳过，退出0 | 历史限定GREEN；后续旧驱动/测试Runner配置变化未在当次执行，不能当最终完整证据 |
| green-full-verified | 566 Android：562 PASS、4 FAIL；JVM0；退出1 | 全部463旧Android＋103新Android实际执行，完整GREEN失败 |
| green-legacy-ui-loading-sync | 8旧Android PASS，0失败/错误/跳过，退出0 | 两份有限调整后的驱动限定回归，属于测试Runner注册修复前的输入 |
| green-full-final | 日志只到359/566，无完成摘要 | 被中断执行不能登记完整PASS；不能把进度行当XML结果 |
| green-full-final-rerun | 86 JVM PASS，Android XML为0；总退出1 | JVM真实PASS；设备安装失败，非业务行为RED |
| green-android-final-rerun | Android XML为0，退出1 | ActivityManager启动错误；没有测试结果，不能登记PASS |
| green-full-restored-device | 86 JVM PASS；364 Android中355 PASS/9 FAIL；总退出1 | 实际局部结果及环境诊断保留，注册修复前的输入不纳入最终覆盖 |
| green-android-connected-final / green-android-final-clean | 均0 Android、退出1 | 不以无testcase结果或设备服务故障宣称PASS |
| green-full-runner-final | 86 JVM PASS；566 Android中557 PASS/9 FAIL；总退出1 | 只选择新输入下成功的JVM子命令，全部Android结果排除 |
| green-android-systemui-clean-final | 566 Android PASS，0失败/错误/跳过，退出0；Lint/assemble完成 | 清理环境干预有独立记录，同147项输入与所选JVM合成严格方法覆盖，非伪造单次全量phase |

已归档**26个完成summary、44份XML、122件完成阶段原始文件**，另保留中断`green-full-final`的[原日志](m10-tests/green-full-final.log)和[源码ZIP](m10-tests/green-full-final-source.zip)，没有伪造其summary。全部原始文件字节、SHA、XML节点和ZIP条目核对一致；[完成阶段原件登记](m10-tests/test-provenance.json)与[派生分类](m10-tests/phase-provenance.json)区分26完成／1未完成，共27次尝试。早期ZIP只含Kotlin子集（例如113/129、122/140、129/147），后续完整147项，不能把早期子集称为完整构建输入。

摘要的 `newDistinctTests` 是旧helper用当前结果数减549产生的粗字段：首次Android全量的17＝566−549、旧JVM单独通过的−463＝86−549，**不能解释为新增测试数或旧测试被删除**。真正新增103按class#method集合核对；历史不同输入的GREEN不参与最终覆盖。

第一次全量4个失败为三项M09Flow及大字体UI回调。后续语义树诊断确认旧详情驱动读取Loading；驱动只在实际仍加载时等待，真实缺失保持失败。大字体回调在限定执行未复现，有限同步不构成已证明根因。早期wrong-UID fixture、Runner缺stdlib、编译错误、SELinux诊断及失败原件保留，不冒充有效行为RED。

真实probe首次调用因AGP使用配置的AndroidJUnitRunner、未注册自定义组件而失败，**没有执行任何边界**。随后仅将构建的testInstrumentationRunner设为M10ProcessBoundaryRunner，并在无action时委托super执行正常JUnit；test APK重建/真实注册和失败原件见第10节。此两项输入变动要求重新验证，不能沿用旧split。

旧启动SystemUI ANR有独立[关闭对话框干预](m10-device/diagnostics/dialog-dismiss-intervention.json)及[最终执行前环境核对](m10-device/diagnostics/systemui-cleared-before-final.json)：限定本AVD，未清LITHIC数据或修改源码。原有失败不改写；不能声称已确定每项旧失败仅有同一个原因。新环境不能冒充原设备连续升级；实际用户旅程与重建来源见第11节已执行记录。

## 10. 文件失败、数据库失败、并发及进程中断证据

最终566项Android包含全部103项新增真实文件/Room测试，已验证失败保护、完整事实CAS、固定命令重放、快照/GC互斥、共享引用、取消回传及ViewModel清除租约。下表是本轮追加20项映射，**不是新增产品功能或独立验收结论**；95个原映射方法可定位，另8个准备/完整基线/未知文件/无写入方法补充S07、S16、S17，共103身份，执行身份与最终XML由第9节严格登记。

| 编号 | 要求与主要方法/证据 | 当前可证明与剩余边界 |
|---|---|---|
| S01 | two_projects_share_a_cover_and_replacement_keeps_other_reference | 最终Android双项目引用保护PASS |
| S02 | project_and_subscription_share_cover_until_last_reference_is_removed；production_owned_image_store_cannot_delete_project_or_subscription_cover | 最终Android及实际共享旅程PASS；同owner受控合成setup，两类真实引用核对 |
| S03 | two_projects_share_a_cover_and_replacement_keeps_other_reference | 最终Android替换后另一引用/旧字节PASS |
| S04 | project_and_subscription_share_cover_until_last_reference_is_removed；physical_delete_failure_keeps_proof_and_retries_without_metadata_reference | 最终Android最后引用解除/物理失败重试PASS |
| S05 | deleting_project_preserves_cover_still_used_by_subscription；project_delete_commit_triggers_independent_safe_media_cleanup_without_changing_delete_result | 最终Android业务删除＋独立GC/Nav PASS |
| S06 | system_picker_cancel_does_not_dirty_or_write_project_or_media | 最终Android及实际系统取消七表零写PASS |
| S07 | prepare_does_not_create_formal_file；remove_active_prepared_resource_is_rejected；discard_prepared_does_not_delete_final_with_uncertain_db_result；unknown_uuid_named_file_without_ownership_proof_is_retained，及故障/取消/快照测试 | 最终Android临时/孤儿/unknown/活动租约PASS |
| S08 | lost_result_replay_after_generation_reopen_does_not_import_again；committed_saved_envelope_performs_read_only_verification_without_import_or_commit | 最终Android回传丢失/固定身份/COMMITTED只读PASS |
| S09 | final_file_before_database_commit_is_reclaimable_after_reopen | 最终Android重开PASS；**真实file-before-DB SIGKILL PASS，PID4239→4297** |
| S10 | database_commit_before_old_cleanup_survives_generation_reopen | 最终Android新图重开PASS；**真实DB-before-GC SIGKILL PASS，PID4335→4375** |
| S11 | fixed_command_concurrency_creates_one_asset_and_replays_exact_target；duplicate_save_and_saved_task_recovery_keep_one_asset_and_frozen_operation | 最终Android并发/重复保存单资源PASS |
| S12 | stale_project_metadata_cannot_replace_newer_facts；new_subscription_payment_blocks_a_stale_cover_command；new_subscription_state_blocks_a_stale_cover_command | 最终Android完整事实/旧命令CAS冲突PASS |
| S13 | missing_cover_is_explicit_failure_without_deleting_metadata_or_business_reference；corrupt_cover_is_explicit_failure_without_reencoding_or_clearing_facts | 最终Android坏/缺已存字节PASS；实际坏导入源可见错误及旧图保护PASS，两类证据分开 |
| S14 | normalized_metadata_matches_real_private_bytes；repeated_verified_reads_preserve_bytes_and_real_metadata | 最终Android及实际私有字节独立解码/大小/尺寸/MIME/SHA PASS |
| S15 | repeated_reads_do_not_change_file_or_hash；repeated_verified_reads_preserve_bytes_and_real_metadata | 最终Android多次读取字节稳定PASS |
| S16 | project_cover_write_preserves_cost_and_all_money_facts；load_target_returns_complete_project_and_money_facts；project_cover_editor_retains_real_identity_and_history_origin_without_writing | 原项目事实/引擎保护及全部旧结束/纠错回归PASS |
| S17 | subscription_cover_write_preserves_payment_and_state_history；subscription_baseline_contains_original_payments_and_states；subscription_cover_editor_retains_identity_and_real_payment_without_writing | 原付款/状态保护及全部旧续费/暂停/停止/重开回归PASS |
| S18 | minSdk26/内置API/选择契约与Lint边界 | API26实际运行**NOT_RUN**；API36编码PASS不能代替API26 |
| S19 | 360dp/200%字体真实操作与错误可达 | **PASS**：真实系统键盘、raw1.234错误、滚动访问保存/选择、断网准备与明确保存；完整TalkBack仍NOT_RUN |
| S20 | SavedState固定身份、Nav3/home系统Back守卫；真实旋转/IME/手势/任务恢复 | **PASS**：横竖屏保留raw/准备，真实边缘手势触发弃改守卫；已保存同任务SIGKILL后同资源恢复并一次保存；非真实LMK |

两处**实际SIGKILL均PASS，单独补充证据，不计入652个JUnit方法**。首次[Runner未注册失败](m10-device/process/process-runner-unregistered-failed.json)原样保留；[test APK重建日志](m10-device/process/process-runner-build.log)为BUILD SUCCESSFUL in 6m 44s，随后[实际组件注册](m10-device/process/process-runner-registration.json)核对成功。常规JUnit无action时委托原runner路径，最终566项是在这个新注册配置下执行。

- [file-before-DB](m10-device/process/process-file-before-db.json)：最终文件已完成、DB新引用尚未登记时真实终止PID4239；旧PID实际消失，新PID4297打开相同孤立合成generation，保留旧封面、核对金额/历史及完整性/FK、只回收已知新孤儿。
- [DB-before-GC](m10-device/process/process-db-before-cleanup.json)：新引用已提交、旧文件尚未清理时真实终止PID4335；新PID4375核对新封面与已存字节，保留金额/历史及完整性/FK，只清理已无引用旧图。

[两边界汇总](m10-device/process/process-boundaries.json)记录checkpoint、前后PID、实际引用/尺寸/哈希、命令退出与安全清理；只公开合成metadata和原日志，不公开DB或图片字节。不清除正式数据、不切换正式generation；SIGKILL不是断电持久性、真实LMK或OEM证明。两次均使用目标APK SHA `939b3b20d3c6cd02c800f320ccddc300736b75084b609c404f38b096270f7094`。

## 11. API36实际系统选择器、断网、重启、字体与恢复验证

实际专用目标为 `emulator-5596 / Lithic_M01_API_36 / API36`。此前排查误重建了专用AVD，原目录已不存在；原设备连续升级/保留证据中断。保存的原M09合成DB/WAL/SHM及七表事实仍完整。经过严格来源/结构检查，在替代专用AVD只重建该合成fixture，保留已有空generation及指针；随后实际安装原M09 code7并冷启动，再覆盖安装最终M10 code8。[实际升级核验](m10-device/manual/reconstructed-upgrade-preservation.json)PASS：七表逐行逐字段完全相同，同generation、recordZone GMT及实际pointer/ready/settings字节不变。**这不是原设备连续升级、原现场自然保留或M12产品恢复。**

根代理实际系统选择器操作已PASS：取消七表零写；项目导入A、私有查看、删除确切合成A源、冷重开、替换B、缓存缩略图后的损坏源选择保留B。订阅导入透明PNG；测试Runner仅经同Application owner/生产MediaRepository为两个明确QA记录建立共享B，不以手动SQL冒充用户操作；随后正式UI移除项目封面、为项目导入独立Alpha、二次确认删除该QA项目，订阅B仍可读；正式UI移除订阅最后引用，再独立添加B并替换Alpha。金额/付款/状态只按业务原操作保持，原M09事实完整。[四份旧资源实际不存在](m10-device/manual/known-media-physical-cleanup.json)另有确切路径核验，最终被替换的订阅B也在[最终设备核验](m10-device/manual/final-storage-device-proof.json)确认实际不存在，不由空metadata推定删盘。

Wi-Fi与移动数据实际关闭、active default network=none时完成冷启动私有查看、系统选图/导入/保存。360dp/200%字体下实际键盘输入raw1.234并触发精度错误、滚动访问保存；横竖屏保留raw/准备，真实边缘返回触发弃改守卫。系统保存Task616为STOPPED/HaveState=true/Bundle6700 bytes后，确切自有PID17203 SIGKILL消失；手工Recents恢复同任务PID17927，同准备ID `6ef0c664-dffd-417e-b9d8-7bbbe824f6e2`及SHA不变，明确保存后只登记该资源一行，PNG alpha[0,255]。见[恢复凭证](m10-device/manual/saved-task-second-restored.json)、[实际就绪画面](m10-device/manual/saved-task-second-ready.png)。名称滚动前另截图核对；就绪凭证如实保留targetNameVisible=false。

首次恢复截图过早，仅记录加载画面；第二次初查保存按钮在屏外，50秒可见按钮检查超时。实际滚动后核对同任务/PID/资源/准备状态及可用保存按钮成功，不改业务代码或删除早期事实。两次入口/就绪失败与后续结果见[实际尝试说明](m10-device/manual/manual-attempt-notes.json)；不计入652个JUnit。

[最终存储核验](m10-device/manual/final-storage-device-proof.json)PASS：旧七表所有原行/字段保留、schema2、integrity_check=ok、FK无错误、同代、marker/settings字节及最终已安装APK SHA正确。全部临时网络、屏幕、字体、IME、导航、旋转、明暗及自动时间/时区设置已按新AVD操作前基线[实际恢复](m10-device/manual/guest-settings-final-restored.json)，差异0，guest/host UTC相差0.731秒；宿主时钟和其他设备未改。真机、API26、OEM、Release、完整TalkBack、真实LMK、断电仍**NOT_RUN**。

## 12. schema、generation、recordZone、权限和依赖保护

源码继续Room schema2七表，不新增表/索引/迁移，原v1/v2导出保留。实际APK结构检查记录v1 SHA `351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b`，v2 SHA `ceb49b458c4c5baa08066586f209f96842935f5456a0a53a01220f6d958f7131`，v2身份 `a8fd277b0ef08ce9cf59b704734d0c25`。DAO仅增封面CAS、双引用查询与精确无引用删除。领域引擎/Money/BusinessClock及唯一owner职责保留，媒体使用同一代/协调器，没有切换/回退或新时区规则。

实际runtime依赖解析PASS，**122→122，无新增/删除/改版**；原始日志 `runtime-dependencies-verified.log` SHA `6c77fc82505c350040eed8f2119b1b3d938ebf80e1cfbace15a52b7c5a816963`。冻结Gradle8.14.3、AGP8.13.2、Kotlin2.3.10、Room2.8.5、KSP2.3.6、Navigation3 1.0.1、Compose BOM2025.11.01、DataStore1.2.1，min26/target36；不新增图片依赖。

已核对实际APK和合并Manifest：只含 `com.cooper.lithic.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，没有网络/相机/广泛媒体/外部存储权限；`allowBackup=false`及legacy/cloud/device-transfer九域排除。90个指定生产类存在；测试框架、合成测试、URI provider及进程probe未打包到目标APK。实际设备存储/recordZone按第11节独立核对PASS，源码配置不替代运行证据。

## 13. APK版本、大小、SHA-256及签名

本次在最终新Runner输入和分开执行的测试/Lint/assemble完成后重新核对目标APK，字节仍与probe所用产物一致。已公开[APK核对JSON](m10-device/apk-final/apk-verification.json)、[生产类/测试排除/结构核对](m10-device/apk-final/extra-verification.json)及[实际签名输出](m10-device/apk-final/signature.txt)：

- 绝对路径：`E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`。
- applicationId：`com.cooper.lithic.debug`；versionCode：8；versionName：`0.1.0-m10-debug`。
- 大小：12,755,115 bytes。
- SHA-256：`939b3b20d3c6cd02c800f320ccddc300736b75084b609c404f38b096270f7094`；本次重新计算与实际verification JSON一致。
- v2签名验证PASS；Debug证书SHA：`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，与原M09相同。
- 90个生产类、schema、权限/备份排除及测试代码未打包已实际检查；wrapper JAR SHA `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`。

自定义Runner注册只在androidTest配置/源码中，目标APK不包含probe/provider/测试框架；测试APK的注册阶段SHA仅登记为测试证据，不上传APK或签名私钥。第11节实际设备操作及设置恢复后的已安装APK SHA再次核对为上述最终SHA，不由构建或孤立probe推定。APK不公开发布，数据库/私有图片原字节/真实相册与个人数据不提交。

## 14. Git文件清单、commit SHA、推送状态、Known Bugs及NOT_RUN

当前分支 `feat/m10-local-cover-media`，基线 `57de897917473f255d43dfb306a8433b366c00eb`。全部路径见[实际文件清单](M10-files.md)，结构化结果见[JSON](M10-verification.json)；[实际索引147输入核对](m10-tests/final-index-inputs.json)仅14项Git CRLF→LF转换，其余原字节相同，无测试后代码/配置变化。实际提交SHA及推送/远端一致回执在最终完成报告核对，不将提交自身SHA递归写入其内容。只推本轮功能分支，不合并main、不force、不修改WHOCARE、不发布APK或进入M11。

相同147项输入的652项分开执行覆盖、Lint/assemble、两处真实SIGKILL及实际设备旅程均已登记；第一整体失败阶段/9项focus失败、旧失败和诊断不改写。26个完成阶段/44XML/122完成原件，另1中断尝试的原日志/源码ZIP保留，未伪造summary。只发布源码、安全测试证据及根代理逐张审阅的合成截图/metadata白名单，原件/历史保护、链接/敏感信息与索引输入检查作为提交前文档核验，不算产品测试。设备事故及重建来源如实保留。当前未发现确定性未解决业务缺陷；未知归属中断残留保守保留，小型凭据不是备份或永久草稿；SQLite空间不足可能归DATABASE错误；长期性能仍归M22；Debug不用于无法丢失的重要记录。

提交前范围核查PASS：239新增/27修改/0删除，1606本地链接无失效；147构建输入与实际暂存blob一致（14项仅Git换行转换）。215份公开测试/设备证据暂存字节与本地原件完全一致。新增源码/当前文档的差异空白检查PASS；全档案检查会报告原始CRLF/工具输出空白，只发生在这两组原字节保留证据中，按哈希保留，不格式化证据消除诊断。

真机、API26、OEM、Release、完整TalkBack、真实LMK/断电、M11/M12完整备份恢复、Q05跨模块往返和Q06故障回退均**NOT_RUN**。Q03/Q04/Q06与原M09/M04/M08历史NOTES按原期限继承。原130条归档案例和16组完整跨模块补测不改写。本次只报告**READY_FOR_REVIEW — M10**，独立验收PENDING；立即停止，不进入M11。

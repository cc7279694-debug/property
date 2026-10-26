# UI-C2 B5：只读媒体接入核对

结论：**MEDIA_DISPLAY_PENDING**。本轮未实现媒体读取、解码、写入、Worker、JNI或GC，未运行测试、构建或任何设备命令。当前产品`data`/`domain`/schema相对`e24703b`的差异为空（只读git核对）。以下是源码事实与下一专项建议，不是设备安全或图片显示PASS。

## 当前可利用的身份事实

- `app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt:26–38`：ManagedGeneration持有id、directory、database、settings、coordinator及Owner close入口；没有旧代images/mediaResolver或页面MediaRepository。不得由Composable再开Room或自行拥有generation。
- `data/local/ProjectEntities.kt:32–50`：LocalMediaAssetEntity有id/relativePath/mime/width/height/byteLength/sha256，构造校验UUID、私有media路径、正尺寸、SHA格式；这是登记元数据，不等于文件存在或字节已核验。
- `data/local/MediaDao.kt:9–12`：find/list仅查当前数据库。可靠目标身份必须同时绑定本次generation与实体当前coverAssetId，不能只按asset UUID跨代缓存或直接从UI访问DAO。
- `data/backup/RoomBackupSnapshotSource.kt:10–22,37–54`：现有备份在确切coordinator.withSnapshot中验证settings Owner，并用Room事务读取媒体及所有关联事实。可复用“由既有Owner提供同代只读边界”的方法，不把整个全表备份当每张缩略图读取器。
- `data/repository/LocalWriteCoordinator.kt:6–20`：withSnapshot/withWrite与retire共用mutex，retire先标记isRetired再close，拒绝排队旧代工作。`app/LithicNavigation.kt:133–145`已按generationId和repository实例绑定GenerationUiScope；未来媒体也必须绑定同一会话，不另建存储Owner。
- `data/backup/BackupMediaSnapshot.kt:15–71,78–99`：现有完整备份读取有路径/父目录UID、regular file/nlink、lstat/fstat dev/inode/size/time、O_NOFOLLOW/O_CLOEXEC、复制前后身份、长度/SHA、容器/尺寸核验。它复制到本次备份临时目录后检查可读性（`BackupExportService.kt:46–51`仍在captureInto边界内调用）；不是现成页面read API，不能把其copy方法接到Compose。
- `data/backup/BackupEntryFiles.kt:16`与`BackupMediaSnapshot.kt:21`：当前已登记规范化封面复制范围是编码字节<2MiB、宽高1..1600。**这是存储封面口径，不是将M10原始输入40MP/64MiB规格缩小。**

## 旧UI-C1可复用什么，不能直接搬什么

参考冻结`0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`，以下路径均为该ref的`app/src/main/java/com/cooper/lithic/`：

- `feature/media/CoverDisplay.kt:50–88`：CompositionLocal provider随确切repository identity创建/销毁loader，LaunchedEffect以loader/asset/size为key，换代即重置Reading；这些生命周期与视觉分支可作为适配依据。
- `feature/media/CoverDisplay.kt:91–156`：64dp缩略图、详情16:9裁切及独立原比例查看、无引用与失败两种图标、文字/内容描述/testTag是纯呈现，可在后续已有安全read contract后选择性迁入；现有Failed状态太粗，需保持更明确的失败分类。
- `feature/media/CoverPreviewLoader.kt:30–49,68–108`：后台IO、并发信号量2、每次包括缓存命中先openAsset验证、cache key为repository实例/asset/目标尺寸、metadata变化使缓存失效，是可继承的原则；不是当前可用API。
- `feature/media/CoverPreviewLoader.kt:138–166`：8MiB像素LRU、close清空并取消scope、不得回收仍被Compose显示的bitmap；实际visible/evicted Bitmap可比LRU存活更久，所以8MiB不是进程内存硬上限。`decodeCoverPreview`使用BitmapFactory采样，不能因协程cancel就推断native decode已立即停止。
- 旧`data/media/MediaRepository.kt:134–138`依赖MediaRepository.snapshot和LocalImageStore.openAsset；旧`data/backup/DataGenerationStore.kt:27–43`还有images/removalGuard/mediaResolver，当前没有。旧`LocalImageStore.kt:321–328`verify依赖AndroidImageCodec.inspect/verifyDecode，不能只复制展示组件就绕开缺失存储协议。

## JPEG历史风险边界

当前继承文档`docs/evidence/M11B-final-verification.md:16`明确：JPEG登记字节完整性与严格熵流验证分开，Android Bitmap可读性不是严格解码证明。旧UI源码`AndroidImageCodec.kt:119–132`的verifyDecode依赖BitmapFactory；旧loader并未替代严格验证器。

三项历史JPEG断言继续保留：`M10JpegIntegrityTest.jpeg_entropy_truncated_with_eoi_is_rejected`（旧ref测试:60），`M10JpegOldCoverProtectionTest.project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved`（:68），`subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved`（:73）。EOI或SHA匹配并不能证明登记前已存在的熵流完整；宽松解码可能显示部分图像。只读展示不应修改旧封面/金额/付款，但仍有错误成功显示、坏图缓存以及native解码资源风险，不能因为无写入就宣称这些历史FAIL关闭。未来若要宣称严格拒绝坏JPEG，必须单独明确并验证熵流策略；不能因SHA与登记值一致且BitmapFactory返回非null就发放严格安全凭据，也不据此拒绝所有合法已登记JPEG的原字节备份。

## 下一专项最小建议（未授权实施）

1. 由现有generation Owner提供只读resolver，而非UI拼路径。一次请求绑定generation实例/id、coverAssetId、元数据身份；在同一协调器下完成有界FD读取及归属/前后身份/长度/SHA/格式/尺寸验证，关闭FD后才把不可变有界字节交给后台缩略解码；不要把native解码放在业务写锁内，也不把可取消协程冒充底层IO可立即终止。
2. 缩略图只按物理目标尺寸采样，限制单图输出像素、同时读取/解码数与LRU计费；缓存key必须包含generation/asset身份(SHA及尺寸/格式等)/目标尺寸。每次新呈现验证身份，失败使缓存失效，不永久缓存旧成功掩盖坏图。换代先清空可见状态、取消旧请求，发布前复核session。仅回收明确未被显示的像素；测量峰值与最坏取消不能由源码推理代替。
3. 只读结果区分：NO_COVER（无引用，不做IO）、MISSING_METADATA、MISSING_FILE、OWNERSHIP_UNKNOWN、INTEGRITY_FAILURE、DECODE_FAILURE、GENERATION_RETIRED/CANCELLED及READY。丢失/失败不能伪装未设置；未知归属不删除、不回写metadata、不触发媒体清理。UI可简洁提示并保留完整可访问原因。
4. 专项门禁至少覆盖：空封面零IO；同代登记JPEG/PNG；保留EOI的熵流截断及登记SHA也一致的坏JPEG（未有严格证据不能判安全PASS）；错SHA/长度/尺寸/MIME；目录逃逸/软硬链接/UID未知；读取时文件替换；旧generation迟到结果；同assetId跨代缓存隔离；解码失败/资源压力/取消后的真实资源状态。使用合成数据独立设备授权；不得迁入M10写入Worker/JNI/Stage协议来绕过审查，不得通过缩小产品规格取得PASS。

实现上述resolver将增加当前缺少的数据层/媒体安全API，超出本次B段UI授权，故本轮停在方案与门禁；其余UI可继续。真实封面、解码资源及Android显示仍NOT_RUN/PENDING。

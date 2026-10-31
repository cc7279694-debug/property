# PHOTO-LITE P1 — 物品封面限定实验

2026-10-31；来源 `b23d0f2aa968cb2c5eda6359b499da1b7fad5a74`；工作分支 `codex/photo-lite-p1`。原产品分支、V1-A1、Preview12和所有历史APK保留。本报告同时记录最小设计、实施计划及最终真实验证边界，不另建媒体架构或数据库。

**PHOTO_LITE_EXPERIMENT_ONLY / STRICT_JPEG_IMPORT_BLOCKED / V1_RELEASE_NO_GO**

## 契约与选择

目标仅为已有且尚无封面的长期物品添加一张本地封面：真实系统Photo Picker→瞬态缩略预览→用户确认→规范化PNG→当前Owner受保护登记→原读取与刷新链显示。没有订阅导入、替换、移除、GC、裁剪、后台自动重试或原JPEG永久副本。

- 原读取/备份合同严格小于2MiB，因此采用更严格的**PNG<2MiB、最长边640px**，满足用户<10MiB/≤1600要求，不扩大C6或backup4预算。
- 只在显式 `photoLiteExperiment=true`、独立 `.photolite.debug` 内开放。正常Debug/Release/Preview/V1-A1一律不注入writer，保留原身份/版本/签名。实验code1 / `0.1.0-photo-lite.p1-debug`；独立build与未来稳定归档目录。
- 初次实验只开放**API36+实际系统Photo Picker可用**；API26～35及未审未来版本明确不可用，不静默回退SAF。PhotoPicker无需广泛照片/存储权限，不持久化URI权限；不新增网络能力。系统Picker本身可包含云提供者，不能据此保证所有选项均为本机原件。
- `loadThumbnail`不是严格JPEG验证，也不是隔离进程。API29与API36采样实现不同，API36仍有额外/方向变换分配；后验检查不能冒称解码前硬峰值保证。原始JPEG完整性、系统codec内部峰值、即时取消及真机行为仍未证明。
- 复用C6串行槽与像素计费；取消先撤票据，实际IO返回前保留槽；未知/OOM停止提交，不声称协程取消即原生资源全部关闭。只由新sRGB画布像素编码PNG，额外拒绝文本/EXIF等非预期元数据，不改旧备份字节。
- 提交绑定完整项目快照、原cover、当前Owner**实例**与nonce，先独占文件/认证/fsync，再Room事务媒体insert+全行cover CAS。失败保旧记录，提交未知保文件且禁止自动重试；不删除旧媒体或未知文件。

## 最小实施计划

- [x] 新增不可变PNG成品认证/长度限额与纯JVM断言源码；原`BackupPngInspection`保持。
- [x] 新增受当前Owner管理的writer与专用cover CAS；合成Room/文件/backup4 Android断言源码。
- [x] 新增有界系统缩略图、瞬态预览与确认编码；取消/迟到/资源未知测试源码。
- [x] 详情入口、独立Controller、原导航/恢复守卫及现有封面显示/列表刷新接线。
- [x] 独立opt-in构建身份、最小源Manifest、历史APK与冻结边界静态核对（不是Gradle求值或合并Manifest核验）。
- [ ] 资源允许且用户恢复构建后，定向JVM、AndroidTest编译、构建/Lint、实物Manifest/签名/SHA与唯一归档；Android实际执行另授权。

## 本轮验证约束

开始时可用RAM约2.1GiB，低于既定4GiB构建门槛；用户明确回复“先完成源码，构建暂缓”。因此当前只做源码和静态准备，**没有运行Gradle/JVM/编译/Lint，没有生成或声称存在内部试验APK**。测试源码不记为RED/GREEN或Android PASS。

系统API来源：[API29实现](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-10.0.0_r1/core/java/android/content/ContentResolver.java)、[API36实现](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/core/java/android/content/ContentResolver.java?format=TEXT)、[loadThumbnail](https://developer.android.com/reference/android/content/ContentResolver#loadThumbnail(android.net.Uri,%20android.util.Size,%20android.os.CancellationSignal))、[PhotoPicker](https://developer.android.com/training/data-storage/shared/photo-picker)。这些是平台资料，不是本App已运行证据。

## 实际源码与复用

| 源码 | 本轮处理 |
| --- | --- |
| `data/media/photolite/PhotoLitePng.kt` | 不可变PNG、实际SHA、严格长度/尺寸、fresh 8-bit RGB/RGBA及标准色彩块结构；拒绝EXIF/文本/ICC，不改变旧备份媒体字节 |
| `PhotoLiteCoverWriter.kt` / `GenerationPhotoLiteCoverWriter.kt` | 单次nonce能力、Owner实例/旧全行保护、0600独占文件与路径/UID/dev/ino/长度/SHA读回、文件/目录sync、数据库事务；未知文件保留，无GC |
| `data/local/ProjectDao.kt` | 仅追加16行cover CAS，SET只有`coverAssetId/updatedAt`，原查询/写入方法保持，无表/字段/migration变化 |
| `feature/media/PhotoLiteThumbnailPipeline.kt` | 后台系统缩略→新sRGB画布→确认后受限PNG；实际工作串行、像素账本和编码引用持有；原生迟到OOM也封锁后续照片操作 |
| `PhotoLiteCoverController.kt` / `PhotoLiteCoverPanel.kt` | 明确预览确认/取消、双击保护、15秒读取/30秒保存看门狗；超时先撤票据，不等同原生终止；旧Picker回调/旧Owner不能重绑新提交，迟到成功不能清UNKNOWN |
| `LedgerUiState/Holder`、`LithicNavigation`、`ProjectDetailScreen` | 只在实验开关注入writer，原generation UI作用域及导航/恢复/备份保护；已有封面详情适度放大，原`RecordCover`受校验读取，普通版本没有添加入口 |

复用原`LocalMediaAssetEntity/MediaDao`、`ManagedGeneration`、coordinator、Room事务、`GenerationCoverReader/CoverPreviews`和`BackupPngInspection/BackupMediaSnapshot`。媒体登记后项目行失效信号沿原Repository刷新详情、列表及首页，不新增查询/缓存/Owner。原列表封面接线不改。已有封面不提供替换/移除；取消选图或预览没有存储提交。

SavedState仅为未确认布尔标记及nonce，无URI/Bitmap/PNG/外部路径。重建时保守UNKNOWN，但系统突然终止前是否已保存该标记**无法保证**；不自动重放提交。已发布Bitmap不能在Compose仍引用时强制recycle；16MiB只是活跃/在途像素记账，不含GC/GPU/codec/RSS保证。未知状态保留现场并阻止继续提交，不能冒称已全部释放。

## 真实验证结果

| 项目 | 本次结果 |
| --- | --- |
| 源码差异/边界、源XML解析、产物忽略规则 | 定向PowerShell静态审计exit0；8个旧文件改动均在许可范围，新文件14个；main Manifest未改；新增XML仅label+非导出空测试宿主，无新增权限。不是实际合并Manifest/DEX/安装隔离证明 |
| `git diff --check` | exit0；原DAO仅追加CAS，Room5 schema、backup4、金额/订阅/Owner/恢复/C6-1/C6-2C与C9-R1主题实现未修改 |
| 原11份用户APK＋V1-A1两归档 | 逐文件`Get-Item`/`Get-FileHash -Algorithm SHA256`相对既有`v1-a1-accelerated-20261031/retry-1/end-summary.json`核对，**13/13大小与SHA保持，exit0**。没有重建、覆盖或删除旧包 |
| 新JVM断言源码 | 14方法准备；有效PNG/防篡改、坏图/空/长度尺寸、元数据、API范围、不放大及限流；**未运行、未编译，NOT_RUN** |
| 新Android断言源码 | 38方法准备：writer10、pipeline8、Controller12、生产Compose8；全部**未编译/NOT_RUN**。包括重新打开受校验读取、旧Owner/双确认/CAS/链接、原format4导出验证和缺/坏图拒绝、迟到与UNKNOWN保留 |
| JVM/Gradle/AndroidTest编译/构建/Lint | **NOT_RUN — USER_BUILD_DEFERRED**，没有运行失败或通过结果，不宣称TDD RED/GREEN |
| 新APK/Manifest/DEX/证书/SHA/稳定归档 | **NOT_BUILT / NOT_VERIFIED**；仅预设独立包/原Debug签名配置，无新实物，无可交付路径或SHA |
| 实际Picker、系统API36编码块兼容、封面刷新/重开、备份恢复、取消和整体内存峰值 | **NOT_RUN / UNKNOWN**，不把源码或合成断言当Android证据 |
| 设备、ADB、模拟器、真实照片/账本、安装 | **0次** |

静态核验还包含`git check-ignore app/build-photo-lite/probe.apk verification-candidates/photo-lite-p1/probe.apk` exit0（只是路径规则查询，没有创建文件）。候选ID预设为`com.cooper.lithic.photolite.debug` / `.test`、code1、`0.1.0-photo-lite.p1-debug`，正常LithicApplication/MainActivity；Test配置M13 plain Application，不能把@Before拒绝误用当预启动隔离证明。原Debug证书预期仍为`29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9`，**新候选签名尚未实际生成/验证**。

## 下一次恢复构建与停止点

仅在用户恢复桌面构建、实测至少4GiB可用RAM后，以现有离线JDK/SDK执行定向测试/AndroidTest编译/构建/Lint，命令采用 `-PphotoLiteExperiment=true`（已强制M13测试Runner；不执行connected任务）。实际核对App/Test身份、权限、正常主入口、测试代码边界、证书和SHA后，才将**唯一内部App候选**归档到独立非build目录`verification-candidates/photo-lite-p1/`，不得覆盖未知同名文件。新APK和密钥均不入Git，设备运行仍须另行授权。

当前可供独立**源码**审查，尚不具备候选交付/生产安全验收条件。源代码准备阶段停止于 **SOURCE_PREPARED / BUILD_DEFERRED / APK_NOT_BUILT**；不解除旧M10三个JPEG FAIL、C6运行/资源UNKNOWN、正式MEDIA_IMPORT_BLOCKED或V1_RELEASE_NO_GO。此PNG实验不自动完成严格原JPEG认证、媒体替换/移除或下一阶段。

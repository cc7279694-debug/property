# M10 JPEG 补充验证报告

日期：2026-10-14，Asia/Shanghai。**模块PENDING；真实缺陷已复现，生产修复未实施，停在授权决策点。**本报告供ChatGPT独立复核，不是正式验收，也不是完整GREEN交付。

## 任务契约与来源

- Goal：用原始锁定JPEG验证M10-T04输入完整性及M10-T03旧封面保护，不重做M10。
- Scope：已有API36专用AVD、真实ContentResolver/provider → LocalImageStore、真实Room/私有文件的合成独立测试、构建核查、证据与当前交接、现有功能分支提交推送。
- Out of scope：M11、正式验收回写、main合并、WHOCARE、AVD重建、清除现有数据、修改时钟、依赖/工具链升级、公开APK或私人数据。
- Dependencies：d7e9原M10实现、冻结M00～M09、同代owner/coordinator、Room v2、既有测试provider。
- Acceptance：健康对照成功；缺损输入明确拒绝；两类旧封面及完整事实不变、失败安全清理。拒绝三项当前FAIL，不能冻结。
- Verification：先保持生产不变运行真实Android断言；核对完整输入、XML、日志、APK及数据保护；必要修复越界时停在方案决策点。

| 来源 | 实际校验 |
|---|---|
| 仓库/分支 | `cc7279694-debug/property` / `feat/m10-local-cover-media` |
| 起点/独立评审HEAD | `d7e9a92096b70d6e5976ebcc65dbdfda32b17296`，本地和远端一致，初始工作区干净 |
| M09冻结来源 | `57de897917473f255d43dfb306a8433b366c00eb` |
| main保护基准 | `145326ddd4ce28abbf7c2706fe25f9d38c599c3a` |
| `E:/CODEX/M10_Codex_follow_up.txt` | SHA-256 `a168222e260765fe807460d0da9fb48cc1fa2d2de084b980fdc7a3763d5a25af` |
| `E:/CODEX/M10_independent_review_JPEG_probe.zip` | 91,681字节；SHA-256 `a883941abcf87b93b583bc99e9c1a4b488e2c206a7db39cb22ce6e71f65c0770` |

ZIP实际验证14个文件的CRC、SHA、重复/绝对/父目录/盘符/链接/加密路径及大小边界，通过；README和指令实际读取。未执行ZIP内宿主探针、JAR或脚本。仅两份必要合成图片进入androidTest资产，来源及14项完整清单见[intake manifest](m10-jpeg-supplement/intake-manifest.json)，[原指令](m10-jpeg-supplement/intake/CODEX_FOLLOW_UP.md)和[原README](m10-jpeg-supplement/intake/README.md)按字节保留为资料。

| 原fixture | 字节数 | SHA-256 |
|---|---:|---|
| complete.jpg | 59,334 | `2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907` |
| entropy_cut_eoi_retained.jpg | 20,194 | `f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a` |

两者均256×256、末尾FF D9，缺损样本移除39,140字节图像数据。测试在资产读取及真实content URI读取后再次核对SHA；未再生成或替换样本。

## 真实Android结果：缺损输入被接受

既有AVD `Lithic_M01_API_36` / `emulator-5596`，Android API36。启动已有磁盘状态，未重建或wipe；本轮不修改宿主/guest时钟、不清App数据。每个测试使用随机独立no_backup目录；旧封面测试使用同代真实DataGenerationStore、Room及MediaRepository，不mock核心解码器。

| 观察层 | 健康对照 | 缺损JPEG |
|---|---|---|
| `AndroidImageCodec.inspect` | SUCCESS，JPEG 256×256 | SUCCESS，JPEG 256×256 |
| BitmapFactory bounds/bitmap | SUCCESS | SUCCESS，底部64行均为灰色128 |
| 生产`normalize` | SUCCESS | SUCCESS |
| 真实ContentResolver → `prepareImport` | SUCCESS | **错误SUCCESS**，准备租约有效 |
| ImageDecoder（software，partial listener返回false） | SUCCESS，partialErrors=[] | **SUCCESS，partialErrors=[]**，底部64行均灰色128 |

健康规范化文件51,882字节，SHA `673d7e89bf1f611b71203c183d533f175a1fbbb5cc0de6a222ae5cd567d8270a`；缺损规范化文件18,820字节，SHA `e6054776e4a0c956d2552e33adf71456834c636153d2b3c713b82518e3153570`。输出可解码不证明输入完整。六份实际观察及其SHA见[observations manifest](m10-jpeg-supplement/observations/manifest.json)。

实际logcat有`Corrupt JPEG data: premature end of data segment`，但API未抛拒绝异常。仅保留[相关警告摘录](m10-jpeg-supplement/jpeg-decoder-warnings.log)，完整logcat未提交。诊断测试PASS只代表健康断言和观察采集成功，不代表缺损拒绝PASS。

## 新增方法与真实RED

| 新增唯一方法 | 结果 |
|---|---|
| `M10JpegIntegrityTest.valid_control_jpeg_imports_successfully` | PASS |
| `M10JpegIntegrityTest.android_decoders_report_locked_fixture_outcomes` | PASS（诊断） |
| `M10JpegIntegrityTest.jpeg_entropy_truncated_with_eoi_is_rejected` | **FAIL：实际Success，不是INVALID_IMAGE** |
| `M10JpegOldCoverProtectionTest.project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved` | **FAIL：实际Success** |
| `M10JpegOldCoverProtectionTest.subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved` | **FAIL：实际Success** |

执行入口（同一冻结工具链，`ANDROID_SERIAL=emulator-5596`）：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.cooper.lithic.data.media.M10JpegIntegrityTest,com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest' --max-workers=2 --console=plain
.\gradlew.bat :app:lintDebug :app:assembleDebug --max-workers=2 --console=plain
```

第一命令**退出1，193.685秒，BUILD FAILED in 3m13s**；5项，2 PASS /3 FAIL /0 ERROR /0 SKIP。编译通过，三个失败均为真实拒绝断言，生产代码仍为d7e9；[RED XML](m10-jpeg-supplement/android-production-unchanged-android-0.xml)、[日志](m10-jpeg-supplement/android-production-unchanged.log)、[命令/输入指纹](m10-jpeg-supplement/android-production-unchanged-summary.json)及[实际输入ZIP](m10-jpeg-supplement/android-production-unchanged-source.zip)保留原字节。

第二命令另跑**退出0，82.984秒，BUILD SUCCESSFUL in1m22s**；Lint 0 Error /0 Fatal /23 Warning，Debug assemble成功。生产任务多数UP-TO-DATE，不能称新完整回归。[日志](m10-jpeg-supplement/lint-assemble-unchanged-production.log)、[输入指纹](m10-jpeg-supplement/lint-assemble-unchanged-production-summary.json)、[实际输入ZIP](m10-jpeg-supplement/lint-assemble-unchanged-production-source.zip)及[Lint XML](m10-jpeg-supplement/lint-results-debug.xml)保存。

两个阶段的**152份完整输入及ZIP原字节一致**；原147份输入不变，只增加3个Kotlin测试文件和2个合成fixture。源码方法总数657＝原652＋新增5；本轮只运行新增5。**原86 JVM＋566 Android的652项历史PASS保留，新152输入下完整JVM/Android回归NOT_RUN**，不合成一次完整成功。逐方法与输入/APK交叉核对见[document verification](m10-jpeg-supplement/document-verification.json)和[input/APK verification](m10-jpeg-supplement/input-apk-verification.json)。

## 旧封面保护实际到达的边界

项目及订阅均先经真实导入/Room提交正常旧封面，再对锁定缺损content URI调用生产prepareImport。检查完整项目/订阅详情、类别、所有金额事件/付款/状态、旧coverAssetId、完整媒体表及私有正式文件原字节/SHA：**在准备后全部相同**；没有新增正式媒体元数据/封面引用。然后“必须明确拒绝”的断言失败。

测试没有提交缺损准备图，**不能由此证明UI最终提交不会接受它**，也没有已证实的金额破坏。错误Success持有准备租约；后续“失败无租约/安全清理”断言未到达，**NOT_RUN，不能将M10-T03/T04新边界写为PASS**。各测试teardown只关闭/移除自己随机隔离的合成根目录，不能算生产失败协议已经通过。

现有设备活动账本也在本次前后独立只读核对：七表全部行与全部字段相同，2类别/4项目/7事件/2媒体/4订阅/6付款/5状态；generation、pointer/ready/settings、记录时区GMT及两份私有封面SHA不变；SQLite integrity=ok，FK无错误，安装APK字节相同。AVD descriptor/config原字节不变。[安全摘要](m10-jpeg-supplement/active-data-protection.json)提交；数据库/WAL/SHM、DataStore、现有封面原图及完整设备日志留在忽略目录，未推送。

## 根因与停止依据

当前`jpeg()`扫描SOF/SOS及EOI结构并跳过entropy字节，没有证明图像数据消费完整；BitmapFactory返回非空图且尺寸正确，随后重新编码并校验输出，无法发现本fixture的静默补全。实际ImageDecoder同样接受，故直接更换API无效；它也从API28才提供，API26/27尚未实测兼容路径。[官方ImageDecoder参考](https://developer.android.com/reference/android/graphics/ImageDecoder)。

阅读的固定版本源码显示：libjpeg在提前marker时可能警告并补零，Skia按输出行判断成功，平台回调按底层解码结果报告partial。这是对实测现象的机制解释，不是本AVD系统源码版本证明：[AOSP jdhuff.c](https://android.googlesource.com/platform/external/libjpeg-turbo/+/2ca61f042e0f5f0023941a3031b1bbb51c402879/jdhuff.c)、[SkJpegCodec.cpp](https://android.googlesource.com/platform/external/skia/+/55d7ef7cad065f1722e9f7f13aa521e80bb39b9d/src/codec/SkJpegCodec.cpp)、[ImageDecoder.cpp](https://android.googlesource.com/platform/frameworks/base/+/1cdfff555f4a21f71ccc978290e2e212e2f8b168/libs/hwui/jni/ImageDecoder.cpp)。这些提交不是新依赖版本选择。

用户补充指令第5节明确要求：

> 若最小可行修复需要增加依赖、改 minSdk 或显著扩张解析架构，先提供证据与最小方案并停在决策点，不自行扩大实现。

严格消费JPEG entropy涉及baseline/progressive、Huffman、MCU及restart等完整规则，已超出现有marker扫描的小改范围。当前没有经验证能拒绝该fixture的现有平台替换方案；本轮因此**不改生产代码、不加依赖、不假造GREEN**，提交真实RED及以下待授权方案。

## 最小修复提案（PROPOSED / NOT_APPROVED / NOT_IMPLEMENTED）

建议另行授权**固定版本、来源/许可证可追溯的libjpeg-turbo解码预检＋小型JNI**，所需NDK/CMake构建属于新增范围。保持minSdk26，不使用系统隐藏接口，不自写完整Kotlin entropy解析器。

1. 在受64MiB限制的私有JPEG源上完整消费baseline/progressive数据；解码检测到损坏/不足数据的警告即INVALID_IMAGE，禁止将补全当成功。通过后沿用原BitmapFactory规范化、EXIF方向、1600px/2MiB输出及有限重试；PNG alpha规则不变。
2. 继续40MP源图限制，不保存无界全分辨率像素；必须验证native工作内存、progressive工作量和输入消费的硬边界，不能只设置`max_memory_to_use`就宣称所有分配有硬上限。资源限制明确失败，不能绕过验证。具体版本/ABI/内存预算尚待方案冻结，未安装或选定依赖。
3. 后续验收锁定两份fixture和当前三项RED，补健康baseline/progressive、缺损progressive、资源上限/中途失败，验证两类旧封面拒绝及失败清理。保留原652方法及全部断言，在最终相同输入执行新增目标、完整JVM/Android、Lint、assemble，核对最终APK/ABI/权限；API36与API26真实兼容验证按下一轮授权执行，未执行不得标PASS。

JPEG没有通用内容校验和。此提案拒绝解码器检测出的损坏及不足数据补全，不能证明任意仍合法的像素变化与最初照片一致；不新增源内容真实性功能。

## 修改、产物与未执行状态

- 新增：3个androidTest Kotlin文件、2个原字节合成JPEG；本报告/JSON、新checkpoint及独立补充证据目录。
- 修改：唯一CURRENT_STATE、README入口及`.gitattributes`新增证据原字节保护。
- 删除：0。Android生产代码/UI/schema/依赖/工具链/原测试断言均未修改。正式ACCEPTANCE_MATRIX、原130案例、16组跨模块边界、原报告/XML/checkpoint不改，804项历史受保护文件已核对。
- 文档/Git核查：98个当前文档本地链接实际存在；对新增文本及源码ZIP共337个文件/条目执行凭据模式与禁止产物检查，未发现命中。原始资料摘录保持原字节，其包内相对说明仍以用户原ZIP为来源。检查只证明交接一致性，不将3项产品FAIL改成PASS。
- 数据层：只使用测试隔离的真实Room及私有文件；无schema/migration/生产数据变化，无备份恢复实现。
- 工具链仍为JDK17.0.20.1+1、Gradle8.14.3、AGP8.13.2、Kotlin2.3.10、Room2.8.5、min26/target36；其他锁定版本继承[TOOLCHAIN_LOCK](../TOOLCHAIN_LOCK.md)。
- 生产Debug APK：`E:/CODEX/砺石新APP/app/build/outputs/apk/debug/app-debug.apk`，12,755,115字节，SHA-256 `939b3b20d3c6cd02c800f320ccddc300736b75084b609c404f38b096270f7094`，**与原d7e9产物相同，非修复版**。测试APK SHA `26a81ee612740b230bc06512bffcc964f5c39e5ef8f7fb57d0a0be6801666d09`；两份fixture与探针类只在测试APK，生产APK不含，APK不推送/公开发布。
- NOT_RUN：新152输入完整652回归、拒绝后的失败清理、API26、真机/OEM/Release/完整TalkBack/真实LMK/断电、M11/M12完整往返。Q05仍USER_CONFIRMED；Q03/Q04/Q06及后续模块边界原样继承。
- Git：仅当前功能分支提交推送补测/证据，父提交d7e9；自身SHA和实际推送在最终Git回执核对，不递归写入本提交。没有main合并、force、PR或APK发布。

**READY_FOR_REVIEW — M10 supplemental verification**。下一步仅ChatGPT复核真实缺陷与修复提案、确认新增实现范围；M10未冻结，不进入M11。

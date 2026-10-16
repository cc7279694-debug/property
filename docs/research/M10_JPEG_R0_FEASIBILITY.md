# M10-R0 — 严格JPEG校验可行性研究

日期：2026-10-16。任务性质：技术研究和隔离预检，**没有修复或接入生产App**。

**本轮结论：BLOCKED — M10-R0 feasibility。**已检查的SDK/常规工具位置无NDK、CMake，PATH及常规位置无C/C++编译器；没有安装新工具。Android和Windows宿主的严格解码PoC均NOT_RUN，native内存/最坏工作量没有实测与硬上界证明，因此不能标GO。此阻塞不妨碍提交下面的来源核验、现有路径反证、资源分析及后续实施提案供ChatGPT独立技术审查。

| 路径 | 本轮建议 | 证据强度 |
|---|---|---|
| A1：libjpeg-turbo 3.2.0／TurboJPEG 3严格预检 | CONDITIONAL_GO仅指值得继续隔离PoC；**不批准生产接入** | 官方源码/发行签名已核对；锁样本实际严格解码NOT_RUN，内存/CPU/ABI/APK未证明 |
| A2：同版本libjpeg API、warning即停、逐行消费 | CONDITIONAL_GO仅指后续PoC的优先比较路径 | 能避免全幅像素输出、提供取消检查点；progressive系数缓存、allocator预算仍未证明 |
| B：BitmapFactory/ImageDecoder切换及最小头尾补强 | NO_GO，不能作为唯一严格完整性门禁 | 已有API36生产链接受锁定缺损样本；这不是R0新跑的解码结果 |
| 生产修复／M10-T03、T04冻结 | NOT_AUTHORIZED／PENDING | 原三项拒绝FAIL及原断言不改，R0预检不替代产品验收 |

## 1. Task Contract与基线

- Goal：找到可验证、资源受控、兼容minSdk26的严格JPEG路线，交付可审查证据和最小接入设计。
- Scope：独立研究文件、只读原代码/锁样本/官方源码、环境与供应链预检、静态预算计算及非敏感日志。
- Out of scope：生产Kotlin/Repository/Compose/测试断言、Gradle、Manifest、Room、签名/权限、PNG协议、M10修复、M11、旧工作区、main合并、安装未经批准工具。
- 实际研究工作区：`E:/CODEX/.codex/worktrees/m10-jpeg-strict-r0/砺石新APP`，仅`research/m10-jpeg-strict-r0`，由桌面原生worktree工具创建，从M10锁定`41596820145dc4a3fa6d041728a59540d828108a`开始，未合并UI分支。
- UI-C1正式冻结文档提交`0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`已实际核对远端；UI-C1 ACCEPTED / FROZEN — PASS WITH NOTES。M10仍PENDING；M11 NOT_AUTHORIZED。来源分支内的旧文档仅代表其当时状态，当前研究授权来自用户本次请求。
- 验收闸门：锁样本真实严格解码、正常/损坏progressive/restart/EXIF、受控资源、异常/取消/加载、四ABI/API26兼容与产物增量必须有证据；不满足不能GO。

## 2. 锁样本：实际读取，没有生成或替换

均直接读取[原测试assets](../../app/src/androidTest/assets/m10_jpeg_integrity/complete.jpg)，没有复制进正式记录、写回样本或联系设备。[输入原始结果](m10-jpeg-r0/evidence/locked-inputs.json)由[可重跑预检脚本](m10-jpeg-r0/scripts/verify_locked_inputs.py)产生。

| 文件 | 字节 | 实际SHA-256 | R0严格解码 |
|---|---:|---|---|
| complete.jpg | 59,334 | `2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907` | NOT_RUN |
| entropy_cut_eoi_retained.jpg | 20,194 | `f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a` | NOT_RUN |

两者SOF均为8bit baseline、256×256、3组件，均保留EOI。逐字节确认缺损文件恰等于`complete[:20192] + complete[59332:]`，删除半开范围`[20192,59332)`共39,140字节，剩余共同末尾仅`FF D9`。这证明样本身份与损伤方式，**不是压缩数据完整性或严格解码PASS**；预检脚本不解码entropy，严格返回值/警告/错误均记null／NOT_RUN。

## 3. 环境与实际执行

[环境JSON](m10-jpeg-r0/evidence/environment.json)和[实际输出](m10-jpeg-r0/evidence/environment-preflight.log)登记了检查路径与UTC时间。检查`MirraAndroid/sdk`及`Android/Sdk`中的ndk/ndk-bundle/cmake、NDK环境变量、PATH及VS/CMake/MSYS/常见E盘位置；均未找到可用NDK/CMake/C编译器。该结论限定于列出的位置，不声称扫描了整个磁盘。

现有Python3.12、GitHub CLI可用，Git自带GnuPG2.4.9可用。agent-reach/mcporter CLI不可用，按已读Skill采用GitHub CLI与内置web官方来源回退。没有安装Python包、NDK、CMake、编译器、解码库或升级工具链。没有运行Gradle、安装APK、调用ADB、启动/重建/清理AVD或读私有账本。

| 真正执行的步骤 | 结果 | 不代表什么 |
|---|---|---|
| Git本地/远端、两个原工作区及来源核对 | 起点匹配、原工作区干净；单独研究分支 | 不是M10修复 |
| 环境预检脚本 | 退出0，记录native PoC BLOCKED | 不把缺依赖算解码PASS |
| 锁样本SHA/字节/元数据/删除范围核查 | 退出0，身份一致 | 不消费Huffman/MCU，不算完整解码 |
| 官方源码包及签名资产SHA复算 | 与正式release digest均相同 | 不代表二进制性能/兼容性 |
| 独立临时公钥环GPG验签及指纹核对 | 退出0、VALIDSIG，见下一节 | 不修改系统信任、不等于解码测试 |
| 官方源码及既有生产/测试代码只读审查 | 支持下述比较与风险 | 不登记为运行测试PASS |
| 原M10最终APK ZIP/哈希只读ABI清单 | 4 ABI／8个原.so | 未构建或运行新native库 |
| A1/A2编译、锁样本真实解码、资源/故障/Android | **BLOCKED／NOT_RUN** | 无strict warning/error/return-code实测值 |

预检复跑命令与原输出见[命令/日志登记](m10-jpeg-r0/evidence/COMMANDS.md)，[机器可读研究结果](m10-jpeg-r0/evidence/r0-results.json)明确strictDecoderRuns=0；研究文件原字节见[SHA清单](m10-jpeg-r0/evidence/manifest.json)。所有实验及缺口见[测试矩阵](M10_JPEG_R0_TEST_MATRIX.md)。没有把静态计算或HOST_ONLY工具检查称为Android成功。

## 4. 方案A：来源、严格语义与许可证

实际查询的上游正式latest是[libjpeg-turbo 3.2.0](https://github.com/libjpeg-turbo/libjpeg-turbo/releases/tag/3.2.0)，发布于2026-06-30，非draft/prerelease；tag指向`c85e6b905bf237038faa936dab160ebfc5da0344`。使用正式release tarball，未采用GitHub自动生成Source code包、系统隐藏libjpeg或第三方预编译库。[来源/逐文件哈希](m10-jpeg-r0/evidence/upstream-provenance.json)登记11个源码/文档成员的哈希及644个TAR成员路径安全检查。

| 正式下载资产（只在忽略的build目录） | 实际字节 | 实际SHA-256 |
|---|---:|---|
| libjpeg-turbo-3.2.0.tar.gz | 2,537,858 | `6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e` |
| 对应.tar.gz.sig | 566 | `434aa81b514a0c7b94eb1b3e7784afe4d521b1a96a71de4a2d3476be3c595555` |

均与官方release API资产digest一致。按[官方签名说明](https://libjpeg-turbo.org/Downloads/DigitalSignatures)取得公钥，实际指纹`0338 C8D8 D9FD A62C F9C4 21BD 7EC2 DBB6 F4DB F434`相符；独立build目录公钥环验证得到GOODSIG/VALIDSIG、退出0。[原验签日志](m10-jpeg-r0/evidence/gpg-relative-2.log)保留TRUST_UNDEFINED提示：它指未建立本机ownertrust，不是签名错误；本次通过另外核对官方发布指纹建立此处来源核验。无下载的tar/.sig/公钥/可执行文件或keyring进入提交。

### A1：TurboJPEG 3参数不是万能完整性证明

在[固定版本turbojpeg.h](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/turbojpeg.h)与[turbojpeg.c](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/turbojpeg.c)已核对：

- `TJPARAM_STOPONWARNING=1`在解码器实际产生warning时中止；默认0允许非fatal继续。必须检查set/init/header/decompress的所有返回值，负返回即失败，不能因buffer已填部分像素继续。
- 必须实际执行完整` tj3Decompress8`并完成finish；`tj3DecompressHeader`成功不能代替。底层[Huffman恢复](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/jdhuff.c#L329-L346)遇提前marker可发warning并补数据，严格模式理论上可截断这条恢复路径；锁样本能否确实触发仍NOT_RUN。
- `MAXPIXELS=40000000`、正值`SCANLIMIT`、`MAXMEMORY`是需要显式设置的候选控制；扫描数与内存参数默认0不限。具体预算/扫描阈值尚未获准，不填写未经测量的“安全值”。
- 全幅packed输出仍需调用者分配；不应为了预检直接分配40MP RGBA。缩放/灰度输出可以比较，但要检查lossless不支持IDCT缩放等模式差异，并证明没有跳过应消费的扫描。不能不核对格式就按缩图容量分配。

### A2：同上游libjpeg逐行预检

自定义C `error_exit`及`emit_message`，负级别warning和fatal均跳到本调用的cleanup；`jpeg_read_header(TRUE)`后校验，再`jpeg_start_decompress`，循环`jpeg_read_scanlines`消费全部行，最后`jpeg_finish_decompress`。只保留一行/小条带输出，更适合预检及取消检查。官方[example.c](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/example.c)说明逐行模式，但默认错误示例不能原样当严格warning策略。

A2复用A的同一维护上游，不是新增另一种解码器；C错误出口、缓冲和JNI生命周期比A1需要更多自有代码，全部仍须编译、注入故障和审查。不能让longjmp越过有C++析构责任的栈，也不能在libjpeg回调里抛JNI异常后继续执行native。

### 许可证、安全维护

[3.2.0 LICENSE.md](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/LICENSE.md)及README.ijg已从已验签源码读取：libjpeg API适用IJG，TurboJPEG包装层还适用Modified BSD-3，整体使用需满足对应声明。后续接入应保存源码版本/来源、LICENSE/README.ijg/适用归属；修改上游时标明修改并保留原声明；仅打包所需库，避免误带libspng、命令行工具或Java包装等额外部分。R0只记录义务，未分发新解码二进制。

固定版本和依赖清单/SBOM、[上游安全政策](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/.github/SECURITY.md)及[版本维护状态](https://libjpeg-turbo.org/DeveloperInfo/Versioning)需要持续跟踪。安全政策不在正式tar中，已通过官方仓库单独读取，不能伪称其为tar成员。升级须独立评估、重新签名/hash验证和本矩阵回归；当前正式版本不等于零漏洞。

## 5. 方案B与候选比较

[既有补充验证](../evidence/M10-JPEG-supplement-verification.md)是在API36上通过真实ContentResolver→LocalImageStore执行：锁定缺损样本被BitmapFactory、normalize、prepareImport接受；ImageDecoder软件模式、partial listener返回false也SUCCESS，`partialErrors=[]`。原代码扫描[AndroidImageCodec.kt](../../app/src/main/java/com/cooper/lithic/data/media/AndroidImageCodec.kt)第236～287行仅验证marker结构，276～279行跳过entropy及restart，无法证明MCU数据完整。

[BitmapFactory](https://developer.android.com/reference/android/graphics/BitmapFactory)没有本任务需要的严格warning拒绝契约；[OnPartialImageListener](https://developer.android.com/reference/android/graphics/ImageDecoder.OnPartialImageListener)只在底层判为解码异常时参与，且API28起可用。[NDK AImageDecoder](https://developer.android.com/ndk/guides/image-decoder)也不能单独覆盖minSdk26或自动提供本样本的严格保证。现有反证不能被“系统API成功返回Bitmap”覆盖。

| 维度 | A1 TurboJPEG严格模式 | A2 libjpeg逐行严格模式 | B 平台API/最小补强 |
|---|---|---|---|
| 已复现缺损的确定性 | 源码路径有依据，实际NOT_RUN | 同上，实际NOT_RUN | 已有实测错误接受，NO_GO |
| 输出/工作内存 | 可缩放但仍有输出buffer；MAXMEMORY非全堆硬限 | 可消除整幅输出，仍有progressive系数/库分配 | 现有采样有界，不证明输入完整 |
| CPU/取消 | SCANLIMIT非deadline；需额外控制 | 可加行/进度检查点，最坏段仍须证明 | 不新增native维护，但拒绝缺口未解 |
| 误拒/漏拒 | 所有warning即拒可能拒绝宽松可恢复图片；比率未知 | 同上，须健康语料验证 | 锁样本已漏拒；灰行启发式又可误拒 |
| 兼容与体积 | 新四ABI库/JNI/许可证，增量NOT_MEASURED | 同上；自有桥接复杂度更高 | 小增量，但单独不能达标 |
| 维护 | 固定上游、安全跟踪及构建矩阵 | 同上＋错误/内存适配器维护 | 自写完整entropy解析会变成自有解码器 |

没有找到更简洁且已有证据足够的第三种路线。底部灰行检测、重新编码比较、日志字符串监听均没有可靠完整性语义；拒绝所有progressive会改变既有格式范围，不得作为悄悄降级。JPEG无原内容校验和，完整解码也不能检出所有仍合法的像素改变或丢失完整末尾渐进扫描；这里仅承诺经固定解码器/测试矩阵证明可拒绝的损伤，不增设照片真实性功能。

## 6. 资源预算：尚未达到GO门槛

继续64MiB原输入、40,000,000像素、最长边1600px、输出2MiB；本轮无任何修改。不能把64MiB文件限额直接当native内存限额。

| 条目 | 实际依据／待证明内容 |
|---|---|
| 40MP整幅RGBA | 仅算术为160,000,000字节，应避免；不是测量 |
| 8000×5000 progressive 4:4:4系数 | 8×8块×64个16位系数约240,000,000字节；尚不含开销 |
| 同图4:2:0含块行padding系数 | 约120,192,000字节，见脚本计算；不是普遍最大值 |
| 上游MAXMEMORY | [jmemnobs.c](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/jmemnobs.c)普通small/large调用malloc；max_memory_to_use参与虚拟数组可用量，非所有native分配硬cap |
| 合计预算 | 输入流缓冲/系数/行缓冲/标记/allocator开销/栈/JNI复制/后续Bitmap/并发必须合计；当前UNKNOWN |
| CPU | 输入长度/像素/扫描数有限仍非硬墙钟上限；MCU/扫描/进度检查、deadline与最坏中止粒度均NOT_RUN |

将系数cap简单设64MiB会拒绝一些40MP以内合法progressive，且本身不等于全堆上限。不能把这种兼容性改变写成坏图拒绝成功。需要比较受控allocator、单并发、逐行消费及必要进程隔离，并实测peak/native allocation/CPU/取消余额；若要修改上游内存管理或新增Android隔离服务，须另行批准。**本轮无法证明资源硬上限，故不得GO。**

所有`size_t`乘加、width/height/components、stride、系数块数、输入长度及JNI容量要先做checked arithmetic再分配，包含32位ABI。取消和资源不足须有明确cleanup；Kotlin withTimeout不能强停同步native。SIGABRT/SIGSEGV、进程OOM/链接失败不应伪装为普通INVALID_IMAGE成功，详见[生产提案](M10_JPEG_R0_PRODUCTION_PROPOSAL.md)。

## 7. Android、ABI、APK与环境方案（未实施）

[原APK只读清单](m10-jpeg-r0/evidence/existing-apk-abi.json)：原M10 APK SHA `939b3b20d3c6cd02c800f320ccddc300736b75084b609c404f38b096270f7094`，12,755,115字节，原8个.so共73,584字节，覆盖arm64-v8a、armeabi-v7a、x86、x86_64。真实用户手机ABI未采集；不能只保留两种64位ABI或拿模拟器代表真机。

[上游Android构建说明](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/BUILDING.md#building-libjpeg-turbo-for-android)有四ABI示例，CMake最低3.15，NDK toolchain应固定`ANDROID_PLATFORM=android-26`；上游不支持直接`add_subdirectory()`嵌入，需要独立构建/受控ExternalProject策略。A2可仅构建libjpeg，A1需TurboJPEG；实际ABI符号、STL依赖、SIMD、API下限与strip结果均未验证。禁止链接系统隐藏libjpeg或以当前APK内其他.so当作可调用的JPEG接口。

环境待审批提案：在保持Gradle8.14.3/AGP8.13.2/JDK17/Kotlin及项目版本不动的前提下，另行批准研究所需NDK/CMake/构建器。官方[AGP8.13兼容表](https://developer.android.com/build/releases/agp-8-13-0-release-notes)默认NDK27.0.12077973；[当前NDK下载页](https://developer.android.com/ndk/downloads)列r30 LTS 30.0.16248370（本次查询）。它们是待评审候选，不是已安装或已证明与PoC兼容的选择。应在安装前锁定一组具体NDK、SDK CMake/Ninja、官方下载哈希及许可证，确认磁盘/构建支持，再执行隔离构建。当前不运行安装命令，不接受新SDK许可，不为追新更换App工具链。

新增.so须遵守[16KiB页官方要求](https://developer.android.com/guide/practices/page-sizes)：r28+默认对齐、旧NDK需对应链接设置及最终ELF/APK核对；不能修改现有Manifest绕过。四ABI库大小、最终APK增量、峰值内存、CPU和真实加载均NOT_MEASURED／NOT_RUN。Windows发行安装包大小不能当Android增量估计。

## 8. 独立技术审查与停止

后续待批准范围：固定3.2.0官方源码、NDK/CMake/Ninja、研究专用native harness/JNI、四ABI测试，以及内存管理适配或额外隔离组件是否必要。此处只给ChatGPT审查，不要求用户人工判断专业参数，也不擅自安装。

原三项JPEG FAIL断言、M10-T03/T04和正式验收矩阵保持；UI-C1冻结与其四项NOTES不因R0改变，M11仍NOT_AUTHORIZED。只提交研究分支的文件和必要当前状态；不提交源码下载包/签名/公钥、二进制、缓存、私有账本或签名私钥。最终提交/推送SHA在实际Git回执报告。

**BLOCKED — M10-R0 feasibility**。等待ChatGPT独立技术审查和必要环境授权；不进入生产修复、M11或main合并。

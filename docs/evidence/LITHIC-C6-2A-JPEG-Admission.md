# C6-2A JPEG安全准入复核

日期：2026-10-28。起点`88e7edb201991f386b91b078f574a79f07ecde7b`，分支`codex/lithic-v1-product-completion`。

**结论：已有严格候选具备针对锁定缺损图的历史证明；本轮完成身份与归档复核，不批准生产导入。MEDIA_IMPORT_BLOCKED。** C6-1仅桌面实施的限定冻结已登记CURRENT_STATE；原14项Android NOT_RUN、媒体性能UNKNOWN及旧M10三项FAIL不改判。

## 本轮实际执行

- 独立[离线审计脚本](../research/c6-2a-jpeg-admission/verify_offline.py)退出0；固定图片、五个严格核心源码与历史JUnit字节校对通过。
- 只用既有JDK17、Kotlin stdlib及本地C6-1 Debug编译类，编译[容器对照入口](../research/c6-2a-jpeg-admission/ContainerProbe.java)，javac退出0；4次容器检查符合预期。没有像素解码，**不是4项严格JPEG安全PASS**。复用类文件的当前SHA单独记录；未与历史class清单逐字节比对，编译来源不作额外认证，也不宣称本轮重新构建了App。
- 本机PATH没有gcc/clang/cmake/ninja；只读`wsl --list --quiet`仅见Docker内部发行版，未进入它。没有核实可用的独立POSIX编译/运行环境及锁定libjpeg源码/静态库，因此**新严格native执行NOT_RUN / ENVIRONMENT_NOT_ADMITTED**；未安装、下载或扩建环境。
- 新Android执行0；未运行ADB、设备、Gradle/App构建、Lint或原44项JVM。没有生产修改、新依赖、native集成或新APK。

[执行汇总](c6-jpeg-admission-20261028-095202/summary.json)、[外层命令与退出码](c6-jpeg-admission-20261028-095202/driver.json)、[原始stdout](c6-jpeg-admission-20261028-095202/driver.stdout)、[原始stderr](c6-jpeg-admission-20261028-095202/driver.stderr)、[javac记录](c6-jpeg-admission-20261028-095202/javac.json)、[容器原始输出](c6-jpeg-admission-20261028-095202/container-probe.stdout)、[运行类绑定](c6-jpeg-admission-20261028-095202/container-runtime-binding.json)、[环境记录](c6-jpeg-admission-20261028-095202/environment.json)、[文件长度与SHA索引](c6-jpeg-admission-20261028-095202/evidence-index.json)。

## 固定输入与三个层次

| 输入 | 身份与本轮容器结果 | 严格熵流证据 |
|---|---|---|
| 原`complete.jpg` | 59,334字节；SHA `2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907`；baseline 256×256；容器接受 | 归档GCC/Clang均ACCEPT、读满256行、finish完成 |
| 原`entropy_cut_eoi_retained.jpg` | 20,194字节；SHA `f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a`；容器仍接受 | 同一归档核心GCC/Clang均IMAGE_REJECT，读到78行、finish未完成、codec errorCode117 |
| 现有小baseline对照 | 654字节、7×5；从冻结测试Base64提取；容器接受 | 本轮未做native全解码 |
| 现有小progressive对照 | 554字节、7×5；同上；容器接受 | 归档另有完整40MP progressive 4:2:0/4:4:4全解码通过；不是这份小图的新运行结果 |

[输入与源码SHA](c6-jpeg-admission-20261028-095202/inputs-and-source.json)。锁定缺损图恰为正常图前20,192字节再接EOI，删除39,140字节；**原三个FAIL是同一缺损输入的三个业务场景，不是三张不同坏图**。

历史R1小progressive的生成器和SHA记录仍在Git，但原始生成图片未在所查Git树中归档；不读取旧运行目录、不再生成替身。当前小对照来自`88e7edb:app/src/test/java/com/cooper/lithic/data/backup/BackupJpegInspectionTest.kt`，不冒称R1旧图片。

三层判断必须分开：容器检查确认marker/尺寸等结构；严格候选对声明支持的JPEG执行全行熵解码并要求无warning及finish完成；BitmapFactory/ImageDecoder能显示仅是平台容错解码。此次坏图仍通过第一层，不能用EOI或平台可显示代替第二层。即使全解码无warning，也不证明原图内容真实性或检出所有可能的合法渐进扫描删减。

## 可复用的候选及历史结果

研究定位到`732333b7a2803ec1f4487e437a581979a270e3ee`；五个核心文件`strict_probe.c / strict_core.h / budget.c / budget.h / jmem_budget.c`与成功CI源码`791d44e8ee2b3636031a1073c3df690585309853`逐字节相同。

[严格核心](https://github.com/cc7279694-debug/property/blob/732333b7a2803ec1f4487e437a581979a270e3ee/docs/research/m10-jpeg-e01-native/strict_probe.c)：warning即拒绝、输入耗尽不补EOI、逐行完整解码及finish检查；区分ACCEPT/IMAGE_REJECT/RESOURCE_REJECT/INFRA_ERROR。上述117是codec错误码，不是CLI进程退出码；IMAGE_REJECT的CLI退出约定为10。

候选固定libjpeg-turbo3.2.0原包SHA `6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e`。没有把其源码、库、JNI、Worker或Stage V2迁入生产，也没有执行会下载源码或扩展矩阵的旧CI驱动。

- 归档GCC/Clang对正常baseline及锁定坏图的区分、live0/badFrees0/fdClosed已复核；旧unsupported-SOF分类RED及后续修正均保留，不能把基础设施错误计成图片拒绝。
- 旧候选已有API26九项、API36九项限定Android证据，包含原缺损完整性断言和项目/订阅旧封面保护。API36三个对应方法的**归档原始JUnit**现按原字节复核并单独标ARCHIVED：[完整性](c6-jpeg-admission-20261028-095202/ARCHIVED-api36-method-03.txt)、[项目](c6-jpeg-admission-20261028-095202/ARCHIVED-api36-method-04.txt)、[订阅](c6-jpeg-admission-20261028-095202/ARCHIVED-api36-method-05.txt)。每份1test成功，均非本轮重跑。
- 因而“该严格研究候选能拒绝锁定缺损图，并在当时限定设备/场景保护旧封面与事实”已有证据；**当前生产M10三个历史FAIL不被覆盖，当前产品导入没有因此修复**。
- 最新输入supervisor路径别名修正在host19项通过；后续Android迁入未执行。旧候选18项自然流程证据不能覆盖最新B1异常恢复、B2资源及后续改动。

[归档摘要与来源SHA复核](c6-jpeg-admission-20261028-095202/archived-result-recheck.json)；原[host证据](https://github.com/cc7279694-debug/property/blob/732333b7a2803ec1f4487e437a581979a270e3ee/docs/research/m10-jpeg-accelerated/RESULTS.json)、[API26限定结论](https://github.com/cc7279694-debug/property/blob/732333b7a2803ec1f4487e437a581979a270e3ee/docs/research/M10_ANDROID_TEST_APK_COMPAT_FINAL.md)、[API36九项](https://github.com/cc7279694-debug/property/blob/732333b7a2803ec1f4487e437a581979a270e3ee/docs/research/M10_API36_UID_RESUME.md)、[后续B1/B2边界](https://github.com/cc7279694-debug/property/blob/732333b7a2803ec1f4487e437a581979a270e3ee/docs/research/M10_B1B2_MAINLINE_RESULTS.md)。

## 资源与尚缺门禁

| 项目 | 已有证据 | 尚未证明 |
|---|---|---|
| 正常baseline/progressive与锁定缺损 | 归档严格核心完整解码/拒绝；本轮身份复核 | 当前桌面native重执行NOT_RUN；不是任意JPEG安全认证 |
| API26/36 | 当时各9项自然流程限定PASS | 最新候选完整Android门禁NOT_RUN；其他ABI、真机/OEM/Release未覆盖 |
| 40MP | host三种8000×5000全部读满5000行；核心计量分配峰值333,126 / 120,535,371 / 240,265,637字节；归档退出live/liveBlocks/badFrees为0 | Android Stage→严格核验→规范化全链峰值、在途字节、进程/RSS与失败释放仍未关闭 |
| 预算、取消、异常恢复 | 核心分配计量、部分host超时/崩溃观察；B1健康smoke有旧限定证据 | 512MiB默认计量上限不是RSS或产品预算；取消/native终止、无界RPC、Worker/Host崩溃和中断恢复不能登记完整PASS；无结构化退出时清理仍UNKNOWN |

仅host统计：GCC三种RSS峰值30,296 / 119,264 / 236,212KiB；Clang含sanitizer时为30,424 / 159,368 / 289,288KiB。它们不等于Android整条媒体链的峰值。当前C6-1媒体性能UNKNOWN、14 Android NOT_RUN及全部历史NOTES继续保留。

进入C6-2实际新增/替换前仍须独立批准：

1. native依赖/ABI/隔离进程、支持范围与fail-closed分类的架构方案；不能把旧JNI/Worker整包合入。
2. 锁定输入字节与校验结果的可信交接、当前Owner/代实例资格、提交时原引用保护、并发替换及文件失败回滚方案；Room5/backup4原字节及恢复合同不变。
3. 对**最终同一候选**补齐API26/36取消/异常恢复和三种40MP全链资源证据，沿用原三项完整业务断言；设备或环境操作须另行明确授权。

本轮仅新增独立审计代码、有界原始记录与此报告，并登记C6-1限定冻结。Room5、backup4、金额/日期/订阅引擎、Owner/coordinator、恢复Token/原子指针、生产媒体读写及所有设备现场均未改；无媒体写入实现、用户APK、main合并或M23。

**C6-2A JPEG ADMISSION — READY_FOR_INDEPENDENT_REVIEW**

**MEDIA_IMPORT_BLOCKED**

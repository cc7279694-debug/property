# M10-R1 — 固定研究工具链与隔离 Native PoC 执行记录

日期：2026-10-16（Asia/Shanghai）。停止状态：**BLOCKED — M10-R1 isolated native PoC**，等待 ChatGPT 独立技术审查。严格拒绝锁定缺损的可行性已实测；完整生产资源/取消门槛仍未证明。本轮不修复正式 App。

## 任务契约与 Git 来源

- Goal：以固定官方 libjpeg-turbo 3.2.0 比较 A2 逐行 C API 与 A1 TurboJPEG3，取得实际 Android 解码、异常、资源及四 ABI 编译证据。
- Scope：仅研究源码/脚本/非敏感日志、此分支当前状态和 checkpoint；获准安装固定研究工具。
- Out of scope：正式 Kotlin/Compose/测试断言/Gradle/Manifest/Room/DAO/Repository/JNI入口、媒体协议、PNG路径、签名、APK、M10/UI-C1/main 分支和其他项目。没有执行生产 Gradle、安装 APK、清数据或创建/重置 AVD。
- 来源：R0 `c4cd90b148cce99ba497d864be1176c90408a4da`；分支 `research/m10-jpeg-strict-r1`；独立工作区 `E:/CODEX/.codex/worktrees/m10-jpeg-strict-r1/砺石新APP`。
- 原 M10 `41596820145dc4a3fa6d041728a59540d828108a`，UI-C1冻结文档 `0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`。它们没有合入或改写；M00～M09冻结，UI-C1冻结，M10 PENDING，M11 NOT_AUTHORIZED。
- 用户完整R1指令原件：[USER_INSTRUCTIONS.md](m10-jpeg-r1/evidence/USER_INSTRUCTIONS.md)，SHA-256 `20f04857a4e37ef42320d00e1bd2674ea5fdac07db484f60be24911c9839b0d1`。当前授权覆盖旧 AGENTS 中仅限 M10 分支的阶段性语句，不授权生产 native 集成。
- 恢复读取 R0 [可行性](M10_JPEG_R0_FEASIBILITY.md)、[矩阵](M10_JPEG_R0_TEST_MATRIX.md)、[提案](M10_JPEG_R0_PRODUCTION_PROPOSAL.md)及现行项目记忆。HR Project Memory 使用 resume/checkpoint，沿用唯一 CURRENT_STATE，不建立另一套实时状态。

## 工具安装与真实版本

配置SDK：`C:/Users/CDD/AppData/Local/MirraAndroid/sdk`。Windows Codex环境将实际路径解析到其 LocalCache 下同名SDK，真实解析路径、可执行文件SHA及属性见[构建审计](m10-jpeg-r1/evidence/build-audit.json)。没有改全局PATH或其他项目配置。

| 工具 | 实际核对 |
|---|---|
| NDK | r28c，`Pkg.Revision=28.2.13676358` |
| Clang | Android Clang 19.0.1 |
| SDK CMake | 包版本3.22.1，执行版本`3.22.1-g37088a8-dirty`，为官方包原有构建标识 |
| SDK Ninja | 1.10.2，未另外下载 |
| 既有 JDK | 17.0.20.1+1，仅子进程显式路径 |
| Android目标 | 四ABI均精确android26 target triple；实际运行仅既有API36 x86_64 |

官方稳定元数据实际列出指定版本，下载前C盘约28.84GB、E盘约91.33GB可用。包URL、大小和官方SHA-1见[官方包清单](m10-jpeg-r1/evidence/official-packages.json)。SDK包下载归档未保留，**没有独立重新计算这两份ZIP的下载字节哈希**，不将元数据预期哈希写成实测。

实际安装有失败：SDK Manager为新版官方Android CLI的兼容入口；首次调用把带分号的包名拆开，输出package not found。改用同一已安装官方`android.exe`及其列表实际展示的斜杠包ID，完成指定两包下载/解压，但该进程仍返回`3221226505 / 0xC0000409`。列表命令也异常退出。两次安装命令均保留为失败；之后source.properties、工具版本、真实四ABI编译独立证明组件可用，**不倒改安装器结果**。没有升级CLI或替换版本。

见[首次安装](m10-jpeg-r1/evidence/install-command.json)、[直接官方入口安装](m10-jpeg-r1/evidence/install-direct-command.json)及其stdout/stderr原件。[官方说明](https://developer.android.com/tools/agents/android-cli)确认该新入口；不依赖未授权工具替代。

许可：最初直接比较XML原文SHA，产生待确认闸门。随后实际调用SDK的`TrimStringAdapter`计算规范化SHA-1 `24333f8a63b6825ea9c5514f83c2829b004d1fee`，与已接受记录一致；原文SHA不能替代规范化标识。用户也明确回复“同意，仅限这两项研究工具”。保留[初步闸门](m10-jpeg-r1/evidence/preinstall-gate.json)与[更正/授权记录](m10-jpeg-r1/evidence/license-resolution.json)，未执行`--licenses`或批量yes接受；许可文件副本的CRLF哈希与官方XML文本哈希分开登记。

## 源码供应链、构建与隔离运行

实际重新核验[官方3.2.0发行资产](https://github.com/libjpeg-turbo/libjpeg-turbo/releases/tag/3.2.0)：tar.gz为2,537,858字节，SHA-256 `6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e`；独立GPG目录验签退出0、VALIDSIG，指纹`0338C8D8D9FDA62CF9C421BD7EC2DBB6F4DBF434`。见[本轮来源登记](m10-jpeg-r1/evidence/upstream-r1-provenance.json)、[签名原日志](m10-jpeg-r1/evidence/source-gpg-verify.log)。644个TAR成员路径检查后解包，无链接/特殊文件/越界；未修改官方源码，未使用不明预编译库或系统隐藏JPEG库。

四ABI：独立CMake/Ninja，Release，静态PIC，`WITH_SIMD=OFF`、API26、`-Wl,-z,max-page-size=16384`，不调用App构建。首次研究CMake错用`CMAKE_SYSTEM_VERSION=26`判断，配置失败；实际NDK将此变量设1，已改为核对`ANDROID_PLATFORM_LEVEL=26`，Clang triple另验证。原失败和后续成功都在[原始构建目录](m10-jpeg-r1/evidence/build-runs/)保留。完整命令/退出/输入SHA、16份配置、ELF和工具输出见[审计JSON](m10-jpeg-r1/evidence/build-audit.json)与[构建说明](m10-jpeg-r1/BUILD_NOTES.md)。

研究CLI：[strictjpeg_probe.c](m10-jpeg-r1/native/strictjpeg_probe.c)。A2自定义warning/fatal出口、8KiB输入缓冲、完整扫描行和finish；状态位于heap，longjmp不跨C++析构帧。A1实际设STOPONWARNING/MAXPIXELS/MAXMEMORY/SCANLIMIT并完整调用decompress，1/8输出。其row/finish是API契约推断，不伪装为直接逐行观测。

既有 `emulator-5596 / Lithic_M01_API_36`、API36、实际4KiB页、shell UID2000。所有可执行文件/样本仅置于新建`/data/local/tmp/lithic-m10-r1-20261016`。不安装测试宿主或正式APK，不访问/改写账本和私人图片。两锁样本push后读回SHA相同，补充原始读回见[supplement.json](m10-jpeg-r1/evidence/supplement.json)。

执行顺序：最小锁样本闸门4调用通过 → 四ABI/扩展原创与变异输入 → 异常/资源/取消/重复 → 单独完成40MP444 → 后续补充。脚本、实际argv/退出、输入/产物哈希随证据提交，构建缓存与二进制不提交。原始[驱动诊断](m10-jpeg-r1/evidence/driver-diagnostics.json)包括生成240秒超时、过早派生失败、取消120秒超时，均保留。

## 交付与停止

结果及D矩阵见[RESULTS](M10_JPEG_R1_RESULTS.md)，资源数据见[RESOURCE_REPORT](M10_JPEG_R1_RESOURCE_REPORT.md)，生产范围建议见[PRODUCTION_RECOMMENDATION](M10_JPEG_R1_PRODUCTION_RECOMMENDATION.md)。本地辅助审阅见[LOCAL_REVIEW](m10-jpeg-r1/LOCAL_REVIEW.md)，不代替ChatGPT独立技术验收。

本轮生产产品测试、Lint、Debug/Release APK构建与安装均**NOT_RUN／范围不适用**；不把隔离C实验计为JUnit或M10-T03/T04通过。原三项JPEG拒绝FAIL和UI-C1历史整体FAIL不改。提交仅研究分支，等待独立审查，禁止自行进入生产接入或M11。

提交前另实际执行记录一致性检查：Python源码AST、JSON/JSONL、锁样本与harness哈希、结果/退出/迭代数、caller/FD、合法语料、四ABI审计、文件链接、敏感模式及受保护路径Git差异。见[verification.json](m10-jpeg-r1/evidence/verification.json)与[逐文件SHA清单](m10-jpeg-r1/evidence/file-manifest.json)。这是研究记录核查PASS，不改变功能/资源BLOCKED；敏感模式扫描不等于任意秘密均不存在的绝对保证。原工作区干净及受保护远端HEAD见[Git基准](m10-jpeg-r1/evidence/git-baseline.json)；本地没有main分支，使用实际远端main核对，未创建或操作main。

清单SHA以工作区字节为准；CURRENT_STATE等当前记忆文档沿用仓库LF规范化，另列Git index哈希以区分CRLF。研究源/原始日志由研究目录`-text`规则保留原字节；首次“一律与index原字节相等”的检查因CURRENT_STATE换行差异失败，随后按真实属性核对，未改变证据或产品内容。

# M10-R2 — 资源安全决策与下一阶段闸门

日期：2026-10-16。**PROPOSED — 等待ChatGPT独立架构审查。**这是设计提案，不是生产ADR批准或修复完成。阅读：[架构与四路线](M10_JPEG_R2_ARCHITECTURE.md) · [资源预算](M10_JPEG_R2_RESOURCE_BUDGET.md) · [失败恢复](M10_JPEG_R2_FAILURE_RECOVERY.md) · [checkpoint](../checkpoints/2026-10-16-M10-JPEG-R2.md)。

## 1. 明确结论

**首选：CONDITIONAL_GO，仅建议下一阶段隔离验证B1＋A2；当前生产NO_GO。**

B1是同UID、非导出、按任务退休的bound JPEG worker，串行消费受控FD，先用固定libjpeg-turbo3.2.0 A2严格解码，销毁后在worker内复用现JPEG规范化语义；主进程只接小型结果和受限规范化输出，再按原CAS提交。A2内部研究最小低层计费backend；不重写完整allocator，不默认同时做spill或大量上游取消补丁。

选择独立进程的理由是已经观察到高RSS、6.141秒检查空档和120秒无结果，且同进程无法安全中止卡住的native线程或隔离native fatal。把规范化也移入worker，避免主进程第二次解码同一40MP原图。新增IPC/生命周期复杂度确实存在，只有下一阶段证明API26终止/恢复与资源模型能闭合才值得承担；不是凭“隔离”就宣布安全。

B1不限制同UID恶意代码访问私有账本，不提供内核RAM配额或设备绝对安全。若审查要求抵御worker任意native代码执行后访问账本，B1应判NO_GO，转审B2特殊UID沙箱及可证明终止路径。不能用B1的kill设想替代B2权限验证。

**备选：**若B1完整40MP仍无法给出可接受的实际资源模型，再单独研究C的私有有界spill，仍保留进程隔离。固定源码没有可用spill，需自有backend和更大磁盘预算，不默认实施。B2是安全隔离需求变化时的变体，不是当前已证明可互换组件。

**不采用为默认生产路线：**

- A单独同进程：计费不能解决fatal和无界取消，Android规范化内部内存仍未覆盖。
- A1 TurboJPEG：R1额外整文件输入、输出缓冲及缺协作取消不优于A2；不删除其历史实测或假定永远不可优化。
- D静态删progressive/降低有效像素/限制scan或单边：可能缩窄原规格，须`PRODUCT_COMPATIBILITY_CHANGE_REQUIRED`，本轮无产品授权。
- 只校验头尾、Bitmap成功、ImageDecoder无partial错误：已不足以拒绝锁定缺损输入，不能当完整性证明。
- 仅将strict放worker、主进程再规范化原图：保留原图高内存面，不满足本轮资源问题。

若B/C均无法通过完整资源及恢复闸门，结论保持NO_GO，不能以永久拒绝合法输入或放宽严格性制造通过。

## 2. 冻结事实与版本

设计来源R1 **`2cdad4aa40f9503aad13245670adc9a4d13db32b`**；M10来源`41596820145dc4a3fa6d041728a59540d828108a`，UI-C1冻结`0dce7dd0725a54be4f0280870ef6ca47c48ea7ed`。本轮独立worktree/分支见[架构事实基线](M10_JPEG_R2_ARCHITECTURE.md#1-任务契约与事实基线)。

| 锁定输入 | 字节 | SHA-256 | R1结果，本轮未复跑 |
|---|---:|---|---|
| `complete.jpg` | 59,334 | `2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907` | A1/A2 ACCEPT |
| `entropy_cut_eoi_retained.jpg` | 20,194 | `f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a` | A1/A2 REJECT |

原件在[R1沿用锁样本目录](../../app/src/androidTest/assets/m10_jpeg_integrity/)，本轮只读核对，不生成/替换样本。122调用中121结构化、1监督超时UNKNOWN；40MP444 A2约232MiB RSS、A1约259MiB；A2曾6.141秒检查空档，120秒无结果仍UNKNOWN。RSS非native堆；后续复跑不能关闭历史事件。严格decoder也不是防篡改的密码学完整性证明，R1无警告被接受的特定删scan/marker样本保持原语境。

| 项目 | 本轮实际评估/继承 | 下一阶段影响 |
|---|---|---|
| 解码源码 | libjpeg-turbo **3.2.0**，固定tar SHA `6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e`，R1已验GPG/成员哈希 | 自有budget backend需补丁/构建输入hash，跟踪漏洞与版本差异；不悄悄升级上游 |
| 许可证 | 核心IJG＋Modified BSD-3；R1 TurboJPEG研究构建还涉及bundled libspng/zlib的BSD-2/PNGRef2/zlib条款 | 仅A2生产链接对象须再审实际组成；随App声明必要版权/许可证，不因静态链接省略 |
| 工具 | R1已装NDK28.2.13676358、CMake3.22.1、Ninja1.10.2/Clang19.0.1 | 本轮未运行/升级/安装；正式App启用NDK/CMake仍待另行批准 |
| Android | minSdk26/target36与签名身份不改；四ABI研究已编译 | API36 x86_64才实跑，API26、arm64、armv7、x86、16KiB设备/JNI/正式APK均未证 |
| 体积 | R1 strip研究.so约650KB～1.14MB/ABI（见原资源表） | 含main/A1/A2且非JNI；不能作正式APK增量。实际链接、压缩/ABI打包/许可证、安装变化NOT_RUN |

详细来源：[R1构建/许可说明](m10-jpeg-r1/BUILD_NOTES.md)、[R1资源](M10_JPEG_R1_RESOURCE_REPORT.md)、[固定许可](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/LICENSE.md)。

## 3. 未来最小生产改动面（本轮零实现）

以下是可审查的预期接缝，不是批准改文件清单或已建立组件。Room v2七表、DAO/领域引擎、资金及订阅Repository、Q05已存媒体字节、Nav3安全守卫、PNG导入语义均不改。

| 真实现有位置/拟新增组件 | 为什么可能需要改 | 不允许借此扩张 |
|---|---|---|
| [LithicApplication](../../app/src/main/java/com/cooper/lithic/app/LithicApplication.kt)、[AppContainer](../../app/src/main/java/com/cooper/lithic/app/AppContainer.kt) | API26兼容主/worker身份门禁；主进程独占owner；app级Supervisor依赖注入 | worker不能开第二Room或新generation；身份不明不能显示正常空库 |
| [AndroidImageCodec](../../app/src/main/java/com/cooper/lithic/data/media/AndroidImageCodec.kt) | JPEG FD适配/worker搬移，原EXIF/尺寸/有限质量尝试保持；主进程不再解码原JPEG | 不重新定义PNG或宽松fallback，不改变1600px/2MiB/Q05 |
| [LocalImageStore](../../app/src/main/java/com/cooper/lithic/data/media/LocalImageStore.kt)、[MediaFileProtocol](../../app/src/main/java/com/cooper/lithic/data/media/MediaFileProtocol.kt) | v2 JPEG stage/消费者pin/有界结果receipt、异步准备与恢复/清理 | 不认领未知文件、不把v1准备asset当原图严格证明，不重编码已提交封面 |
| [MediaRepository](../../app/src/main/java/com/cooper/lithic/data/media/MediaRepository.kt)、[MediaContracts](../../app/src/main/java/com/cooper/lithic/data/media/MediaContracts.kt) | 分离长计算和短代锁、保持同代CAS；明确失败分类 | 不改资金/付款/状态事实、schema/索引或幂等身份 |
| [LocalWriteCoordinator](../../app/src/main/java/com/cooper/lithic/data/repository/LocalWriteCoordinator.kt)、[DataGenerationStore/ManagedGeneration](../../app/src/main/java/com/cooper/lithic/data/backup/DataGenerationStore.kt) | 只在必要接缝让消费者pin与close协调，不等待worker时持全局Mutex | 不新增第二提交锁，不实施M12切代/备份恢复 |
| [CoverViewModel](../../app/src/main/java/com/cooper/lithic/feature/media/CoverViewModel.kt)与现错误呈现 | 保持固定operation/asset、转交取消监督、明确资源/超时/能力失败 | 不动UI-C1冻结视觉、不绕过离开守卫；未知结果不能显示保存成功 |
| 拟新增私有bound Service、typed AIDL、Supervisor、FD codec adapter | 小型会话/结果/受控FD、单任务退休、death监管 | 非导出、不常驻、不加网络/相机/存储/通知或前台Service权限 |
| 拟新增C错误边界/JNI、最小budget backend、CMake输入 | 严格消费数据、计费/失败清理，异常不跨不安全native栈 | 不引入全局allocator或无关native功能；不下载未知预编译.so |
| 未来Manifest、app Gradle/NDK/CMake与许可资源 | 明确私有进程、固定native构建/ABI/载入/声明 | **本轮禁止修改**；正式加入必须另批并核对最终Manifest/APK/签名/无网络权限 |

可能影响私有stage协议，不需要改变Room schema或backupFormatVersion；这是待证设计，不提前宣称可无损迁移所有旧草稿。原v1未提交准备通常已无source，必须审查重选体验及保守残留策略；已提交封面继续原字节。

## 4. 下一阶段最小实验与必须通过的闸门

**以下全部NOT_RUN，未授权实施。**建议先独立测试宿主/研究目录验证关键门槛，再单独审批生产接入；不直接把研究.so复制进App。实验本身也需新授权，R2提交后停止。

| ID | 最小实验/目的 | 决定性证据及失败后果 |
|---|---|---|
| E01 | 逐入口native分配清单、创建前预算、所有malloc点失败注入、32/64位checked及清理 | 覆盖progressive池、caller和账本；无部分更新/泄漏、无越界。未计费内存未知→不能称总预算；abort不计普通拒绝 |
| E02 | API26与当前API36隔离宿主的进程初始化、FD握手、session/death、PID复用与自动重连 | worker零Room/owner；错会话拒绝；同步calling身份；超时终止不误杀；parent关FD不被当关闭证明。无法可靠闭合→B1 NO_GO |
| E03 | worker内严格＋原语义规范化的完整峰值/CPU采样 | 原健康/缺损锁、40MP420/444、窄长/多scan、EXIF、64MiB；与旧封面/UI/PNG并发，记录native/Java分配及RSS/PSS、基线/allocator残留。以实测提出预算，不捏造通用安全值 |
| E04 | 分阶段取消、无回调/无进展/线程失调度/清理卡住、host与worker各watchdog | request到UI撤销/检查点/FD关闭/进程死各时间；复现或定位6.141秒gap及120秒UNKNOWN。无法根因重现也保留UNKNOWN，需证明新架构在无结构化结果时安全终止/不提交 |
| E05 | native加载失败、JNI异常、主动abort/SEGV、实际可控分配不足及进程失联 | 分类LOAD_ERROR/NATIVE_CRASH/RESOURCE/UNKNOWN真实；主进程存活与业务无写；实际系统OOM/LMK单列，不用注入malloc失败冒充；不能承诺系统绝不杀host |
| E06 | worker持FD时取消/host SIGKILL/恢复、receipt原子写前后、旧v1草稿、新v2 stage、错代/未知子项 | pin先于FD，GC不早删；consumer终止后收敛；不能凭持久PID认领；与既有owner/close锁序无死锁。未知无限叠加或误清→NO_GO |
| E07 | 两类旧封面/共享引用/CAS/同操作重放，原两SIGKILL边界与新的prepare窗口 | 前后七表、旧图SHA/字节、引用及元数据；所有失败零正式引用/金额/付款/状态变化；租约可恢复/安全清理 |
| E08 | 四ABI JNI实际加载、API26/arm64/32位/16KiB支持及正式构建审计（各按另批环境） | 四ABI编译不替代运行；明确缺设备NOT_RUN；最终APK大小/签名/权限/测试未打包/许可证与补丁输入可追溯 |
| E09 | 原格式域兼容审计、合法/损坏baseline/progressive、restart/EXIF、特殊采样/2分量/超65500窄长、scan边界 | 固定3.2.0或预算拒原可用合法输入则`PRODUCT_COMPATIBILITY_CHANGE_REQUIRED`；不得静默缩40MP范围或拿输入重编码掩盖 |
| E10 | 经批准生产接入后运行原3项JPEG拒绝测试和完整适用回归 | 保留全部旧测试身份/失败历史、PNG与媒体生命周期；锁定缺损真实ContentResolver→Store必须INVALID_IMAGE且保旧/安全清理。只有此阶段实跑才能改M10-T03/T04实际结果 |

E01～E06是资源架构研究的优先验证项；E07～E10是后续生产准入/回归门槛，不代表授权进入生产或要求现在运行全App。C若启用另补spill容量、seek边界、ENOSPC、scan I/O放大、SIGKILL残留及page-cache压力实验，不能用磁盘容量公式替代实测。

## 5. 需要另行批准与NO_GO条件

另行批准事项：下一研究阶段/隔离Android宿主的代码与设备操作；固定3.2.0 native源码/最小backend补丁、JNI/NDK/CMake正式构建；私有Service/进程/AIDL/FD生命周期；JPEG stage v2及取消/清理/close协议；任何新增资源/scan/单边/时间准入规则或旧草稿兼容决策；若需B2/C，分别批准权限沙箱/终止设计或spill及磁盘预算。当前不向用户追加专业参数选择，先交ChatGPT独立技术审查；授权不能从研究成功自动推导。

出现以下任何情况，生产NO_GO：

- 不能覆盖libjpeg相关分配或不能测量worker规范化的剩余峰值；只拿MAXMEMORY/RSS当总堆上限。
- 长时间无native回调时无法阻止继续计算/分配，或API26终止/会话身份/重启恢复存在误杀、重复执行、GC早删风险。
- 必须依赖未经批准的合法图片拒绝、宽松fallback、降低40MP/64MiB或抛弃32位ABI才能通过。
- B1无法满足经确认的任意native代码执行威胁边界，而B2终止尚无证据。
- 旧图/业务事实/CAS/归属/共享资源受损，未知任务叠加失控，或失败被计为普通坏图成功。

R2文档可交独立审查，**不等于以上门槛已通过**。资源预算的UNKNOWN、R1历史UNKNOWN及所有NOT_RUN必须随方案传递。

## 6. 本轮文档验证与停止状态

本轮仅只读核对R1固定源码/报告/锁样本、现媒体与恢复协议、Android官方API；进行文档交叉检查、链接/来源、Git范围与敏感信息检查。最终实际检查范围和结果见[checkpoint](../checkpoints/2026-10-16-M10-JPEG-R2.md)，不登记为产品测试PASS。未运行native/ADB/Gradle、未安装工具、未生成APK。

正式模块状态：**UI-C1 ACCEPTED / FROZEN — PASS WITH NOTES；M10 PENDING，三项JPEG FAIL；Q05 USER_CONFIRMED，M11/M12完整往返NOT_RUN；M11 NOT_AUTHORIZED。**原R0/R1、130案例、历史checkpoint与失败保持原字节。R2等待ChatGPT独立架构审查，不自授ACCEPTED/FROZEN，不开始R3/生产修复/M11。

**READY_FOR_REVIEW — M10-R2 Resource Architecture**

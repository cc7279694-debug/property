# M10 JPEG 集中候选交付

2026-10-18；来源 `2d0a60aca9020fccfca192091b421412f2369316`；分支 `codex/m10-jpeg-accelerated-candidate`。

**结论：BLOCKED — M10-JPEG-ACCELERATED-CANDIDATE。**宿主资源验证和独立 JNI 组件已推进；真实 App 接入因 B1/持久资源协议门禁停止。没有 M10 验收升级或 Android 导入修复通过声明。

## 1. 实际差异及结果

- 复用 E01 backend、固定官方 libjpeg-turbo **3.2.0**（归档 SHA `6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e`），未改预算 backend 或重跑旧 114 次小图矩阵。将 C probe 提取成真实只读 FD 核心：自有 duplicate、`pread`、一行输出、C 内部 longjmp、逐出口 destroy/free/close；调用者 FD 和 offset 保持。
- 新增独立 JNI/Java 声明、四 ABI 构建/ELF 审计及 Linux JVM 实测；无 App 调用、自动加载、Worker 或 Gradle 接入。错误分开为 IMAGE / RESOURCE / INFRA；超时、进程崩溃由外部监督单列，不伪装为图片拒绝。合法但不支持的模式拒绝为能力缺口，不能称图片损坏。
- [最终 CI](https://github.com/cc7279694-debug/property/actions/runs/39109960477)（代码 `791d44e8ee2b3636031a1073c3df690585309853`）**PASS**：GCC13.3无sanitizer 134次＋Clang18.1.3 ASan/UBSan（detect_leaks=1）134次 probe、12项分配测试各657断言、40个FD用例各882断言；8次真实 JNI 调用及1次独立 JVM 载入错误检查通过。所有正常返回的计费/FD清理核对通过，未报告 sanitizer 错误。[分类 RED](https://github.com/cc7279694-debug/property/actions/runs/39109476240)实测 unsupported SOF 被误归 IMAGE/code60，修复后 GREEN；未删除失败记录。测试计数区分 probe 次数、真实 C 用例/断言、JNI 调用和 Python 方法，不合并为 Android/JUnit PASS。
- 新生成 26 份合成图：复用 R1 确定性算法，含 3×40MP、baseline/progressive、420/444、灰度、restart、8 个 EXIF 方向及新增 CMYK/YCCK；三份压力图字节 SHA 与先前 R1 锁相同，但来源明确为本轮重建，不冒称取得旧本地实物。仅临时 runner 生成，不提交图片。两份原锁样本始终直接取仓库实物并核对 SHA。
- 截断、错误 restart、中间熵数据删除明确拒绝。完全删除一个可选 progressive 细化扫描仍可形成合法 JPEG，本轮如实接受此探索样本；严格解码不等于拥有未知原件的字节完整性证明。尺寸/SOF header-only 变体仅测试分类，不当作完整合法图兼容证据。

## 2. 40MP 资源实测

以下为最终 GCC、标量、Linux 独立进程测量；Clang sanitizer 单独记录于结果索引。8000×5000，输出只保留一行；实验请求预算 512MiB **不是**产品上限。64MiB 只约束源文件；额外 64MiB 请求预算测试不改变产品兼容要求。

| 输入 | 计费峰值 B | 进程峰值 RSS KiB | wall ms | CPU ms |
|---|---:|---:|---:|---:|
| baseline 420 | 333,126 | 30,296 | 398.270 | 398.216 |
| progressive 420 | 120,535,371 | 119,264 | 701.964 | 701.836 |
| progressive 444 | 240,265,637 | 236,212 | 1115.860 | 1115.783 |

计费包含 caller context/行缓冲、账本以及 libjpeg manager/small/large/虚拟系数池的实际分配请求；RSS 是 `getrusage` 进程历史高水位，含运行时影响，不是 native heap。未覆盖 libc allocator 额外开销、栈、运行时/链接器、内核缓存、未调用 ICC 路径、后续 Android Bitmap/规范化与并发重叠。所有观测清理计数归零也不能证明任意输入、系统 OOM 或全设备内存上限。实验预算未作为手机安全阈值。

## 3. Android 组件编译，未运行

复用已安装 NDK **28.2.13676358** / Clang **19.0.1**、CMake **3.22.1**、Ninja **1.10.2**；实际编译宏核对 API26，无工具安装或升级。

| ABI | 未 strip 的 `.so` 字节数 | 结果 |
|---|---:|---|
| arm64-v8a | 1,607,640 | COMPILED_NOT_ANDROID_RUN |
| armeabi-v7a | 1,321,948 | COMPILED_NOT_ANDROID_RUN |
| x86 | 1,454,824 | COMPILED_NOT_ANDROID_RUN |
| x86_64 | 1,699,576 | COMPILED_NOT_ANDROID_RUN |

四份均实核 ELF 架构、LOAD ≥16KiB、依赖仅 libc/libm/libdl、无 main，唯一可见定义导出为指定 JNI 函数。加强后的导出审计覆盖 WEAK/PROTECTED/ABS；8 项宿主 Python 方法 GREEN，旧审计 RED 为 4 失败＋3 字段错误＋1 通过，原记录保留。首次构建因 NDK 的 `CMAKE_SYSTEM_VERSION=1` 被旧 guard 拒绝；改为核对实际 `__ANDROID_API__==26` 后，在新 v2 目录完成 31 条构建/审计命令，均 exit0。失败目录未删除。

本地构建目录：`E:/CODEX/.codex/research-builds/lithic-jpeg-candidate-20261018-v2`。8 份 codec/JNI/声明/CMake 输入与当前源码逐字节一致；第 9 份构建驱动随后只加强导出解析，使用新解析器对原四份 ELF 日志重新审计并复核二进制 SHA，没有冒称重新编译。完整 SHA/输入及原始日志索引见下方小型结果文件。这里不是 APK 增量；APK 压缩/打包大小、Android 加载、32 位实际解码均 NOT_RUN。沿用 [R0 许可证义务](M10_JPEG_R0_FEASIBILITY.md#许可证安全维护)，本轮不分发库二进制。

## 4. 为什么停止真实接入

1. [现有 MediaFileProtocol](../../app/src/main/java/com/cooper/lithic/data/media/MediaFileProtocol.kt) 仍为精确 owner v1；[LocalImageStore](../../app/src/main/java/com/cooper/lithic/data/media/LocalImageStore.kt) 仅内存 active，成功后删除 source（112），恢复准备资源只验证规范化 asset；重启 GC 无跨进程消费者证明。
2. [冻结 R2 恢复契约](M10_JPEG_R2_FAILURE_RECOVERY.md#4-stage所有权消费者pin及锁) 要求版本化 owner、持久 consumer pin/receipt、锁拆分和消费者恢复先于 GC。真实异步 Worker 接入必须改变本轮禁止修改的持久 stage/归属协议；不能靠旁路 sidecar 或同进程 JNI 绕过。旧已准备 v1 也不能补造严格源证明。
3. B1 原 normal 仍失败，H2 `DELIVERY_UNKNOWN / pin=true` 未触碰。Linux 合成 timeout/SIGABRT 仅证明本次 CI 自有进程的监督分类；native 清理仍 UNKNOWN，不能解决 R1 120秒 UNKNOWN、6.141秒取消检查空档或 Android Binder/退休/死亡安全。

因此未编写新的 Worker，也未生成接入 App 的 Debug APK。下一次先独立审查最小持久协议变更与 B1 终结门禁，再授权隔离 Android 验证；本轮不自动执行。

## 5. 保护、遗留及证据

| 检查 | 实际状态 |
|---|---|
| `jpeg_entropy_truncated_with_eoi_is_rejected` | 原 FAIL 保留；本轮 Android NOT_RUN |
| `project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved` | 原 FAIL 保留；本轮 Android NOT_RUN |
| `subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved` | 原 FAIL 保留；本轮 Android NOT_RUN |
| PNG/EXIF 展示、1600px/2MiB、Q05、旧封面/资金/付款/状态、共享资源/SIGKILL/恢复 | 生产源码/断言无差异；本轮 Android 集成回归 NOT_RUN，不由宿主结果替代 |
| App JVM/Android/Lint/Debug APK | NOT_RUN：无可安全接入的 App 候选；替代性实际执行为 C `-Werror`、sanitizer、宿主 JNI、Python 审计及四 ABI 编译 |
| Android JNI 异常/OOME/并发 FD、最坏取消、真实系统 OOM、特殊合法采样/2分量/窄长图兼容、SIMD | NOT_RUN / NOT_PROVEN；不得缩小原40MP/合法progressive范围后称通过 |
| 产品/Room/Gradle/Manifest、M10/UI-C1 分支、H2/C2/AVD/ADB/APK现场 | 无变更/无操作；生产 app 对正式 M10 锁定提交差异为空 |

保留 D2A-N01～N05、A1-N01～N04及全部历史 NOTE/FAIL/UNKNOWN。M00～M09与 UI-C1 原冻结不变；M10 PENDING／三 JPEG FAIL；R3/B1 BLOCKED；M11 NOT_AUTHORIZED。

证据入口：[小型机器结果与完整 SHA](m10-jpeg-accelerated/RESULTS.json)、[实际 CI/RED/GREEN 索引](m10-jpeg-accelerated/RESULTS.json)、[代码与可重复脚本](m10-jpeg-accelerated/)、[checkpoint](../checkpoints/2026-10-18-M10-JPEG-ACCELERATED.md)。不生成 ZIP、整仓快照或重复媒体归档。仅提交推送本研究分支后停止，等待 ChatGPT 独立验收。

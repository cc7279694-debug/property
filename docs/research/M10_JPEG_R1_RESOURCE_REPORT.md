# M10-R1 — 资源、取消与兼容性实测

日期：2026-10-16。资源门槛：**BLOCKED，生产NO_GO**。当前仅测API36 x86_64、4KiB页、标量`WITH_SIMD=OFF`。以下RSS不是native堆计量，单次数据不是通用最坏上限。

## 内存分项与实际数据

| 层次 | A2逐行 | A1 TurboJPEG3 |
|---|---|---|
| 输入文件 | 原64MiB上限；8KiB source小缓冲，不整文件复制 | 同64MiB上限；本PoC整文件malloc，实际计入caller预算 |
| 输出 | 一行RGB/灰度；8000宽为24,000字节，40000宽为120,000字节 | 固定1/8缩放；40MP420/444灰度输出625,000字节；RGB编码为3分量并检查容量 |
| progressive系数 | 库内整图DCT缓存仍存在 | 同样存在，缩小输出不能消除 |
| 库内存参数 | `max_memory_to_use=384MiB` | `TJPARAM_MAXMEMORY=384MiB` |
| 参数含义 | 中间数组/库策略，不是所有native分配的硬cap | 同样不是总堆硬cap |
| caller余额 | 只计context/input/output；不含库、stdio、allocator、栈 | 同左，输入buffer也计入 |
| JNI/Bitmap | 未接JNI/JVM/Bitmap，均NOT_MEASURED | 同左 |

以下来自[扩展原日志](m10-jpeg-r1/evidence/expanded.jsonl)、[完整444](m10-jpeg-r1/evidence/single-444.json)；每个测量为独立CLI进程，RSS为进程生命周期高水位。

| 已完成解码输入 | 方案 | native wall ms | native CPU ms | peak RSS KiB | 输出buffer bytes |
|---|---|---:|---:|---:|---:|
| 40MP baseline420 | A2 | 508.122 | 492.794 | 7,448 | 24,000 |
| 同上 | A1 | 222.977 | 216.170 | 20,736 | 625,000 |
| 40MP progressive420 | A2 | 25,141.390 | 18,752.995 | 121,088 | 24,000 |
| 同上 | A1 | 9,588.255 | 7,848.599 | 136,320 | 625,000 |
| 完整40MP progressive444 | A2 | 2,091.068 | 1,994.223 | 237,952 | 24,000 |
| 同上 | A1 | 21,325.863 | 21,127.089 | 265,344 | 625,000 |
| 64MiB尾填充长度样本 | A2 | 见原日志 | 见原日志 | 6,304 | 见原日志 |
| 同上 | A1 | 见原日志 | 见原日志 | 68,992 | 见原日志 |

444三次A2循环峰值238,080KiB，约232.5MiB；A1观测最高265,344KiB，约259.125MiB。尾填充A1 caller峰值67,120,353字节，A2仅11,435字节；该输入只用于长度边界，不当健康照片。首次不完整444拒绝曾耗时A2 64,001.901ms、A1 24,914.261ms；其作为不可信输入的实际成本保留，不混入合法图均值。

固定8000×5000的系数数组粗算：420约120,192,000字节；444约240,000,000字节，按8×8块、int16系数及分量采样计算，并按固定3.2.0 `jdcoefct.c`将420的Y分量625个block行补齐为626行。**这是静态算术，不是native堆实测或所有采样/内部对象的硬上限。** RSS还含链接器/库/栈/allocator等；不能以RSS差值反推出精确系数分配。源码默认不保存全部APPn元数据，但尚无全库allocation inventory、分配次数/大小追踪或每点失败注入。

32位编译与checked arithmetic已检查：输入/尺寸乘法、row/缩放容量、size_t及MiB转换均有边界；不是32位真实OOM验证。库参数限制1MiB时两API明确报告Memory limit exceeded并清理；caller分配故障也有实际返回。两者不等于所有malloc失败或allocator abort均安全。

## CPU、复杂度与取消

- A2真实读取全部行并finish；scan limit100，另以正常progressive与limit1实测RESOURCE_REJECT。A1同样显式限制并得到明确错误。没有合法100/101扫描边界、所有marker/采样恶意组合的最坏工作量证明。
- 4096个空COM标记实测可完成；64MiB上限约束输入长度，不给出通用CPU期限。多次计时差异大，部分原测试与诊断重叠；不能取最快值作为SLA或将内核/ADB耗时归咎于某一个已知原因。
- A2 deadline0/1/10/50ms实际CANCELLED；原批次相应lateness为0.063/0.152/0.007/0.247ms。完整444的300ms deadline，结束含清理wall324.114ms、检查点lateness0.024ms。**lateness不含全部cleanup时间**，应同时看wall。
- 完整444进度1000取消实际187.165ms；原不完整444相同参数首次supervisor120.063秒超时，无native结果。之后同原SHA、参数、库的持久化输出复核明确CANCELLED，367.216ms；不关闭首次UNKNOWN。
- 正常420的一次最大取消检查空档达到**6,141.244ms**；完整444初次为74.187ms；后续三次循环记录最后一次3.359ms。不能用后一小值覆盖前一大值，也不能将“支持取消标志”写成满足固定响应上界。
- A1无本PoC协作取消入口，带取消选项直接INFRA_ERROR、无成功回退。Kotlin Job取消→JNI标志→native停止→清租约的生产闭环**NOT_RUN**。

因此没有可执行、覆盖全部允许40MP progressive的总堆/CPU/最坏取消预算。不能随意降低40MP/64MiB支持范围来制造GO；受控allocator、上游补丁、spill或独立Android进程均只列待审方案，没有实施。

## 清理、故障与余量

所有121个结构化decoder结果中caller余额为0、可观测FD前后相同；四组100次健康/缺损重复caller平衡，RSS循环观察点稳定。合法444三次循环，RSS清理后首3,704KiB、最高3,712KiB。**8KiB变化既非确定泄漏也非全库无泄漏证明**，没有allocator级全量跟踪或长期压力保证。

[runtime helper](m10-jpeg-r1/evidence/runtime-edges.json)真实演示加载失败50、缺符号51、主动abort134/SEGV139；它们绝不计普通坏图REJECT。主动崩溃只发生于原创隔离helper，不是decoder自行崩溃。实际系统OOM、decoder SIGABRT、SIGKILL及JNI异常路径NOT_RUN；同进程Kotlin catch不能解决native进程崩溃，未建立生产隔离进程。

## ABI、16KiB和体积

[构建审计](m10-jpeg-r1/evidence/build-audit.json)核对四ABI的24份原/strip ELF：Machine/class正确，每LOAD声明align≥16384，offset/vaddr按16KiB和声明align同余；全部编译命令target为Android26。未解析到缺失的API26动态stub符号；这不能代替API26运行。

| ABI | 编译/ELF | 实际运行 | strip研究.so bytes |
|---|---|---|---:|
| arm64-v8a | COMPILED / PASS | NOT_RUN | 949,584 |
| armeabi-v7a | COMPILED / PASS | NOT_RUN | 649,844 |
| x86 | COMPILED / PASS | NOT_RUN | 1,046,008 |
| x86_64 | COMPILED / PASS | API36 CLI、dlopen及shared main调用VERIFIED | 1,137,136 |

原设备虽报告arm64 native bridge能力，x86_64进程dlopen arm64 ELF实际明确拒绝；不是arm64真机证据。API26、16KiB页设备、真机/OEM均NOT_RUN。四ABI均保留，没有牺牲32位。ELF16KiB对齐不等于最终APK的zipalign/打包/运行验证。

shared包含研究main和A1/A2两API，无生产JNI。各.so大小为实际产物，不能把总和或压缩估算当APK增量；生产JNI/ABI拆包/动态加载/许可证资源打包及最终APK**NOT_RUN**。动态依赖仅系统libm/libdl/libc；静态嵌入对象仍需许可证审阅。

本轮许可证为IJG＋Modified BSD-3；固定3.2.0的TurboJPEG构建还含bundled libspng/zlib对象，涉及BSD-2/PNGRef2/zlib条款。归档成员不等于全部进入最终ELF；详见[BUILD_NOTES](m10-jpeg-r1/BUILD_NOTES.md)与[许可文件哈希](m10-jpeg-r1/evidence/upstream-r1-provenance.json)。未来须维护固定源码、许可证声明、漏洞追踪、重现构建及升级回归。

结论：A2对baseline输出/输入预算更合适，但progressive仍有较高缓存和取消未知；A1额外复制输入且缺协作取消。本轮只支持继续独立资源方案审查，**不支持直接生产GO**。

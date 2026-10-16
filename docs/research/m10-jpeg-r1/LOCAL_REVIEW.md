# M10-R1 — 本地源码与执行证据复核

日期：2026-10-16。范围是隔离PoC源码和已有日志的交叉核对；复核者参与过harness实现，**不是ChatGPT独立技术验收，不授予M10 ACCEPTED/FROZEN**。本次复核不运行设备、不重新编译，不改变冻结源码或历史结果。

## 1. 已实际读取和核对

- [strictjpeg_probe.c](native/strictjpeg_probe.c)、[研究CMake](native/CMakeLists.txt)，以及固定3.2.0的`turbojpeg-mp.c`、`jcomapi.c`相关实现。
- [最小四案例](evidence/minimal.json)、[108条扩展日志](evidence/expanded.jsonl)、[独立完整4:4:4样本](evidence/single-444.json)、[补充结果](evidence/supplement.json)、[补充原始日志](evidence/supplement.jsonl)、[加载与故障边界](evidence/runtime-edges.json)。`supplement.json/jsonl`是同一组5次调用的登记与流水，不重复计数。
- `build/m10-jpeg-r1/build-runs/20261016T085225.922399Z-26740/`和`20261016T085401.834310Z-20644/`的manifest/源码输入。分别22、60条构建/检查命令均退出0，`inputsStable=true`，都绑定同一harness SHA-256：`954938adc6c2fcea91a5469d28ed864b3230abe520ac99561e88f69eee10ba58`。
- 直接读取四个本地CLI ELF头及计算产物SHA：x86_64/arm64为ELF64，x86/arm32为ELF32，machine分别62/183/3/40；所有PT_LOAD对齐16384，产物SHA与manifest一致。这是编译/ELF静态证据，不是四ABI运行或16KiB系统运行证明。
- 本地两份锁样本字节数及SHA与用户要求相同。补充证据中的远端`sha256sum`同时读回了两个锁样本及实际CLI，弥补最小日志最初只有本地SHA和push成功的证明范围。

## 2. 锁样本的真实最小结果

| 路径 | 输入 | 实际结果 | 压缩数据处理证据 | 清理观测 |
|---|---|---|---|---|
| A2 | complete.jpg，59,334字节 | ACCEPT，退出0，无warning/fatal | 直接读取256行，`jpeg_finish_decompress`成功；记录消费59,334字节 | FD 4→4，caller余额0，destroy/close已调用 |
| A2 | entropy_cut_eoi_retained.jpg，20,194字节 | REJECT，退出10，warning code117 | 第78行之后报`Corrupt JPEG data: premature end of data segment`；finish=false | 同上 |
| A1 | complete.jpg | ACCEPT，退出0，无warning/fatal | `tj3Decompress8`成功；1/8灰度输出32×32，1,024字节 | 同上 |
| A1 | entropy_cut_eoi_retained.jpg | REJECT，退出10，TJERR_WARNING=0 | 同一真实warning字符串，完整解压调用失败；finish=false | 同上 |

A1的`rowsRead=32`及`finishComplete=true`是对公开API成功路径的推导，源码/JSON已经明确标注，**不是直接scanline回调或独立finish返回观测**。固定3.2.0 `turbojpeg-mp.c`成功路径确实循环`jpeg_read_scanlines`并执行`jpeg_finish_decompress`。A1不暴露压缩消费游标，因此`compressedBytesConsumed=null`正确，不能填成本地输入长度。

## 3. 源码与清理边界

A2的warning和fatal回调均立即保存真实库消息并longjmp，不能落入ACCEPT；所有跳转只跨纯C帧，可变清理状态在heap context。ACCEPT还核对完整行数、无warning/fatal及finish成功。输出仅一行RGB/灰度，不分配全图RGBA。输入源按8KiB读取并受64MiB文件长度、40MP和checked尺寸乘法保护。

A1对STOPONWARNING/MAXPIXELS/MAXMEMORY/SCANLIMIT/SAVEMARKERS的设置及读回都核对；只在完整解压调用成功后ACCEPT。输出为1/8灰度，RGB-coded输入用1/8 RGB；输入是最多64MiB整份有界native缓冲。它没有公开协作取消回调，显式取消参数返回INFRA40，不能算取消成功。

四组`--repeat 100`日志均完成100轮，FD余额和caller余额正常；注入caller输出分配失败、低内存设置、scanlimit拒绝也有真实受控返回。**这些不等于所有decoder分配点失败注入或全native堆泄漏验证**。`trackedCallerBytesAfter=0`只是人工维护的context/input/output余额；库、stdio、分配器arena和JNI都不在跟踪范围。

## 4. 不能改写的证据差异与完整性能力限制

1. 扩展流水108次：56 ACCEPT、36 REJECT、10 RESOURCE_REJECT、5 CANCELLED、1 TIMEOUT。这是返回类别分布，包含52项EXPLORATORY，不是“107通过、1失败”的产品验收统计。
2. 原目录`large_40mp_progressive_444.jpg`虽保留`AUTHORED_HEALTHY/ACCEPT`原始标签，实际20,406,272字节且缺EOI；独立完整样本证明它是完整文件前缀。两次拒绝不能计作健康图误拒，也不能擦除原标签与失败历史。新的完整样本有独立名称、27,445,246字节、SHA `0555234207d5ddf84d236d812487a659f2dac34a9d82109a941628553c7b67e6`，两路径都真正ACCEPT。
3. 扩展中的`--cancel-after-progress 1000`在120.063秒后无JSON并由监督器记TIMEOUT。后续完整新样本同参数返回CANCELLED，**不关闭原超时根因**。后续“进程列表为空”只证明观察时没有匹配进程，不能补出原调用何时退出、为何无JSON或native清理是否执行。
4. `progressive_final_whole_scan_removed.jpg`、`baseline_restart_eight_bytes_after_removed.jpg`两路径均ACCEPT；严格warning门禁不是原始内容完整性/防篡改证明。语法仍可解码的修改可能没有warning，不能推广为“所有中间图像数据缺损都会被拒绝”。
5. 64MiB及64MiB−1尾部零填充输入仍ACCEPT；未知EXIF TIFF类型也ACCEPT。decoder并不代替原容器/EOI位置检查或EXIF元数据处理。SOF改到40MP的合成头不等于合法40MP图；其后entropy拒绝不能计作40MP兼容失败。65535极端维度实际触发固定库65500单边限制。

## 5. 资源与取消门槛仍阻塞生产GO

| 真实输入/模式 | wall / CPU，毫秒 | 进程峰值RSS，KiB | 说明 |
|---|---:|---:|---|
| 40MP baseline 4:2:0，A2 | 508.122 / 492.794 | 7,448 | 一行输出不代表progressive同样内存 |
| 40MP progressive 4:2:0，A2 | 25,141.390 / 18,752.995 | 121,088 | 该次最大取消检查间隔6,141.244ms |
| 新完整40MP progressive 4:4:4，A2 | 2,091.068 / 1,994.223 | 237,952 | 最大取消检查间隔74.187ms |
| 同一新完整4:4:4，A1 | 21,325.863 / 21,127.089 | 265,344 | 无公开协作取消检查点 |

单次运行受环境/调度影响，这些不是跨设备性能排名或最坏上界。RSS是整个CLI进程的生命周期高水位，不是decoder native heap。384MiB的MAXMEMORY/`max_memory_to_use`仅覆盖特定中间数组，不约束全部malloc、输入副本、行/指针、metadata、分配器或未来Android Bitmap/JNI。低内存设为1MiB得到受控失败，也不能证明384MiB是总内存硬上限。

补充完整4:4:4三次重复ACCEPT，晚进度取消及300ms deadline取消成功，caller/FD正常；但已有6秒级检查空档、120秒未解释超时、未覆盖全部分配点、缺少最坏工作量/总堆预算，足以阻止生产GO。没有将Kotlin Job取消、JNI异常或生产lease清理冒充本PoC已验证。

## 6. 平台和故障证明范围

四ABI是COMPILED；实际解码和shared `dlopen/dlsym`调用只覆盖现有API36 x86_64。shared入口仍是CLI main，**不是JNI**。API26、arm64/arm32/x86实际加载、真机、16KiB页系统及最终APK集成均未验证。

runtime-edges真实记录：缺库/错ABI退出50、缺符号退出51；独立诊断helper的SIGABRT退出134、SIGSEGV退出139，不按普通REJECT计数。x86_64 shared加载/符号关闭及两锁样本调用成功。故意崩溃的是诊断helper，不证明真实decoder崩溃隔离；真实OOM/系统杀进程、全部JNI异常与清理仍未覆盖。

同源shared同时含A1/A2且带main，并非最小生产wrapper。其strip体积、ELF对齐和普通压缩估计不能当最终APK增量、zipalign或16KiB安装证明。A1构建包含上游额外spng/zlib相关对象，未来裁剪、许可证与安全维护需独立审查；本次不能改源或新增生产依赖。

## 7. 复核建议

**锁样本拒绝技术路径有真实Android依据；生产资源/取消准入仍BLOCKED，不建议生产GO。**可以提交已有成功与失败供ChatGPT独立技术审查，优先确定总内存预算和最坏工作量/取消策略。若需要allocator替换、上游补丁或进程隔离，必须另行授权。M10原三项失败历史、UI-C1冻结状态及M11未授权保持不变。

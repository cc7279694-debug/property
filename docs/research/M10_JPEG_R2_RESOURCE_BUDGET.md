# M10-R2 — JPEG资源预算与待验证门槛

日期：2026-10-16。**设计，未执行新实验。**与[架构](M10_JPEG_R2_ARCHITECTURE.md)、[恢复](M10_JPEG_R2_FAILURE_RECOVERY.md)、[决策](M10_JPEG_R2_DECISION.md)共同审查。40MP＝40,000,000像素；MiB＝1,048,576字节。RSS、malloc请求量、Java/native堆、磁盘容量分别记录，禁止互换。

## 1. 证据标签与资源账本

- **MEASURED**：继承R1特定输入/环境的实际数据；R2没有复跑。
- **CALCULATED**：公开假设与公式下的算术，不是测量或设备安全阈值。
- **ESTIMATED**：架构估计，必须实测校准。
- **UNKNOWN**：当前没有足够证据；不是0，也不默认通过。

| 资源 | 数值/模型与标签 | 归属、限制及遗漏 |
|---|---|---|
| 原JPEG文件 | **CALCULATED** 产品上限64MiB磁盘；不是64MiB必驻内存 | 沿用受控stage；不走A1整文件malloc。内核page cache另计 |
| 源复制与读取缓冲 | **CALCULATED** 当前Store复制64KiB；R1 A2读取8KiB | 未来FD adapter需核对是否多余复制、stdio/JNI数组；库池不包含在这8KiB内 |
| native context/分配池 | **UNKNOWN** 总请求、池头、对齐、slop、stack及allocator物理开销 | 拟以A低层backend逐请求计费；现MAXMEMORY不足以约束全部分配 |
| progressive/multiscan DCT | **CALCULATED** 8000×5000：420为120,192,000B，444为240,000,000B | 完整系数payload；行指针、窗口、池头与其他工作区另计。缩小输出不消除此项 |
| marker/scan/元数据工作区 | **UNKNOWN** 完整上限；输入长度受64MiB约束 | A2当前不要求保存全部APPn，但JPEG规范化还有Exif/Android路径；scan数也影响CPU |
| 解码行缓冲 | **CALCULATED** R1 RGB宽8000为24,000B；限定宽≤65500、3分量时≤196,500B | 每次乘法先checked；PoC格式域不是批准后的完整产品域 |
| JNI/FD/Binder桥接 | **UNKNOWN** native/JVM引用、Parcel、FD数量与栈 | 设计不传整图ByteArray/Bitmap。小型结果、字段长度、最大FD数仍须明确并验证 |
| Android规范化Bitmap | **CALCULATED** 1600²×4＝10,240,000B（9.765625MiB）/幅；3幅payload合计30,720,000B（29.296875MiB） | 仅像素算术，非实测峰值。rowBytes、allocator、源解码缓存、Exif/旋转/缩放工作区均另计 |
| Android原图解码内部缓存 | **UNKNOWN** | 即使inSampleSize缩小Bitmap，progressive系数/codec内部内存仍未证明受1600²约束，故JPEG规范化也放worker |
| 压缩输出/主进程核验 | **CALCULATED** 输出文件≤2MiB；当前verify读入≤2MiB，位图≤1600² | ByteArrayOutputStream扩容、toByteArray副本、SHA/编码器内部缓冲及验证时重叠**UNKNOWN** |
| worker ART/系统组件 | **UNKNOWN** 实际基线和启动开销 | R1为CLI，不含Application、ART、Service或JNI；不得直接把R1 RSS当worker预算 |
| 主进程既有UI/数据库/封面 | **UNKNOWN** 实际基线与并行峰值 | 旧封面、列表缩略图、PNG任务仍可能共存；不宣称App总共只有一张Bitmap |
| 并发 | **ESTIMATED** 全App最多1个活动JPEG worker会话，包括取消未确证结束者 | 属调度提案，未实施。PNG/UI/系统并发不因此消失，下一阶段必须测重叠 |
| 失败/取消保留资源 | **UNKNOWN** 释放耗时及RSS回落 | 删除路径不释放仍打开的FD对应块；destroy也不保证allocator立即归还RSS；旧worker未确证终止不得启动下一JPEG |

当前codec的decoded/oriented/scaled引用存在同时存活路径；“3幅”是审查用payload包络，不是已证任意Android版本的3幅总内存硬上限。文件先后序列化也不代表物理内存完全无重叠。对应真实文件及固定源码定位见[架构S03～S07](M10_JPEG_R2_ARCHITECTURE.md#5-可追溯来源r2只读核对)。

## 2. DCT系数公式与格式域

对固定3.2.0支持的DCT路径，令输入宽高为W/H，各分量采样为hᵢ/vᵢ，Hmax/Vmax为最大采样。按上游分量块尺寸与MCU补齐计算：

```text
blocksWide_i = h_i × ceil(W / (8 × Hmax))
blocksHigh_i = v_i × ceil(H / (8 × Vmax))
Ccoef = 128 × Σ_i (blocksWide_i × blocksHigh_i)
```

128来自固定构建的64个int16系数；不是所有codec通用常量。实际native分配还受行对齐、virtual-array窗口与池开销影响。8000×5000的420中Y分量625块行补到626，因此不能只写3×W×H；444为6×W×H。baseline的多scan输入也可能需要完整系数，不能只按progressive标志决定缓存。[固定jdinput](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/jdinput.c)、[jdcoefct](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/jdcoefct.c)、[jdmaster](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/jdmaster.c)。

仅在W/H≤65500、分量数≤3、每方向采样≤4、W×H≤40,000,000的**显式限定域**内，可得很松的payload包络：

```text
Ccoef ≤ 6 × (W + 31) × (H + 31)
      ≤ 6 × (40,000,000 + 31 × 131,000 + 961)
      = 264,371,766 bytes ≈ 252.125 MiB
```

这不是最大真实malloc/RSS，也不是手机安全阈值。固定libjpeg的最大单边65500及R1仅1/3分量，与现M10扫描器的1～3分量、无65500单边检查存在潜在兼容差异。不能以限定域覆盖全部已承诺JPEG。须先核对原Android路径实际支持范围，再决定是否需产品批准；特殊采样、窄长图、2分量等逐项归类，不能默许新增拒绝。原codec已拒非8bit及非C0/C2编码，本提案不增加12bit/非DCT支持。

所有W×H、block数、Σ、×128、rowBytes×height、offset+length、预算加减和文件容量均需使用足够宽的checked运算，再验证size_t/jint/Java数组及32位size_t能否表示。不能先溢出再比较40MP。预算失败必须在写入分配台账/返回指针前拒绝，free只能减少已登记归属项。

## 3. R1实测保留及峰值模型

以下均为**MEASURED / R1继承**：API36 x86_64、4KiB页、标量CLI；原[资源报告](M10_JPEG_R1_RESOURCE_REPORT.md)、[扩展JSONL](m10-jpeg-r1/evidence/expanded.jsonl)、[完整444](m10-jpeg-r1/evidence/single-444.json)。没有测生产worker、JNI或Bitmap。

| 输入/方案 | wall ms | CPU ms | peak RSS KiB | 注 |
|---|---:|---:|---:|---|
| 40MP baseline420 / A2 | 508.122 | 492.794 | 7,448 | RGB一行24,000B |
| 40MP progressive420 / A2 | 25,141.390 | 18,752.995 | 121,088 | 最大检查空档6,141.244ms |
| 完整40MP progressive444 / A2 | 2,091.068 | 1,994.223 | 237,952 | 约232.375MiB；三轮最高238,080KiB |
| 同一完整444 / A1 | 21,325.863 | 21,127.089 | 265,344 | 约259.125MiB；不是A2堆实测 |

首选架构的待测峰值模型为：

```text
Pcombined(t) = MhostExisting(t) + MworkerART(t) + MworkerTask(t)
MworkerTask(t) = strictLive(t) + normalizationLive(t) + outputLive(t)
                + allocatorResidual(t) + bridge/stack/codecOther(t)
```

严格decoder销毁后才进入规范化，目的是减少活跃对象重叠；**不能未经测量直接把三个阶段求和改成max**，因为已free arena、Android内部codec和page cache可能仍驻留。成功worker退休后再由host最终验证/发布，减少跨阶段重叠；启动、FD和结果仍须在基线中测量。R1清理后RSS约3,704～3,712KiB只说明当时CLI行为，不预测ART进程。

不能把`:jpeg_worker`进程预算理解为设备RAM配额。OS低内存策略及其他App不在本项目控制内；需要针对支持设备测PSS/RSS、native/Java allocation、最大分配、host可用性与进程死亡，而不是只采单次RSS快照。[Android内存与绑定影响](https://developer.android.com/topic/performance/memory/guide/service-bindings)。

### A预算backend的验证要求

候选是替换固定`jmemnobs`低层接口，不改全局malloc、不重写整套manager。decoder创建前绑定per-attempt预算；覆盖manager自身、small/large池、progressive虚拟数组、窗口和caller缓冲，记录actual requested size及附加账本成本。源码内部静态allocator不能靠事后包装公开接口完全拦截。[固定jmemnobs](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/jmemnobs.c)、[jmemmgr](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/jmemmgr.c)。

预算变量Bnative、BworkerObserved、Bcombined、工作磁盘Dwork与时间Tsoft/Tgrace/Tsupervisor目前均为**UNKNOWN / PENDING_MEASUREMENT**。这不是可直接投产的配置：下一阶段必须完成分配清单、每点失败注入、32/64位实测、完整worker峰值及代表设备验证后，提交数值与兼容性影响供批准。不得临时填写256/384MiB或某台模拟器memoryClass作为全设备安全标准。

malloc请求硬cap只能约束计费分配；allocator开销、地址空间碎片、系统分配失败、Android Bitmap/codec及RSS不因此被覆盖。若清单不完整、异常清理失衡或无法界定未计费工作区，则该backend不能称为总native内存预算，更不能据此生产GO。

## 4. CPU、取消与硬中止

**已知结构原因**：R1 A2检查放在source refill、progress monitor和逐行调用边界；固定`jpeg_start_decompress`先进行master初始化，progress回调围绕输入消费，而非每个内部分配/系数初始化/IDCT指令。两个回调之间可能有大量工作、内存触页或线程未获调度。**6.141秒那一次究竟由哪个阶段/OS因素造成仍UNKNOWN**，不能仅凭源码把它断言为某一瓶颈。[固定jdapistd](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/jdapistd.c)、[R1 probe](m10-jpeg-r1/native/strictjpeg_probe.c)。

120.063秒监督超时仍是“无结构化结果”；没有证据证明native已响应取消、仍在运行、崩溃或成功。后续同输入成功取消不关闭此事件。wall含等待/cleanup，CPU只计执行；checkpoint lateness不等于从用户取消至FD全关/进程死亡的延迟。

| 候选机制 | 能解决什么 | 必须验证/不能声称 |
|---|---|---|
| 安全标志＋source/progress/row检查 | 正常路径尽快协作停止 | 不能承诺最坏间隔；记录所有阶段最大gap，不只最后一次 |
| 上游内部更细检查点 | 可能缩短长阶段 | 需专门补丁、重入/longjmp清理审查；仅在实测显示必要时考虑，非默认全面改源码 |
| 独立控制线程/host watchdog撤销attempt | deadline后立即禁止业务提交，不等native合作 | 撤销资格不释放活跃文件，不等于停止CPU；UI响应与worker结束分别测 |
| worker自退＋B1受控终止，等待匹配会话死亡 | 停止该进程计算并由OS回收句柄 | 内核调度不是硬实时保证；身份、PID复用、重连与API26必须实测；kill返回不等于死亡 |

最小测试需在header、master初始化、系数分配/置零、scan消费、行输出、finish、cleanup和Android规范化分别注入取消/卡住/分配失败，记录request/flag/checkpoint/terminal/FD闭合/death/host继续工作各时点。覆盖源停止供给、解码线程不再回调、控制线程仍活/失活、host死亡、最慢合法多scan、异常marker。同步观察CPU时间、wall及调度干扰，不能以平均值或成功最快复跑形成最坏承诺。禁止异步线程取消或从其他线程destroy解码器；无法确认则RESULT_UNKNOWN并保留pin。

## 5. C的磁盘预算与I/O

当前固定`jpeg_open_backing_store`总是报`JERR_NO_BACKING_STORE`，**C没有现成spill实现或实测**。virtual-array接口说明可自定义backend，不等于当前Android构建自动支持。[固定backend](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/jmemnobs.c)、[接口](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/src/jmemsys.h)、[上游说明](https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/doc/libjpeg.txt)。

条件算术：

```text
Dpeak >= 64MiB source + Ccoef spill + 2MiB工作输出 + 2MiB准备副本
         + backend元数据/对齐 + 恢复保留 + 安全余量
```

40MP444典型payload对应前四项约296.9MiB；上述限定域松包络约320.1MiB。均为**CALCULATED**，并非已测文件容量或可用空间要求；若优化为顺序复用输出，可少一副本，须验证再调整。实际可用空间、预留/稀疏文件、ENOSPC、inode、并发PNG和未知残留全部另计。不得把输入64MiB额度解释为可覆盖工作文件。

多scan可能反复读取/写回系数窗口，I/O≈多轮窗口读写＋最终输出；准确放大系数、闪存延迟、page-cache压力为**UNKNOWN**。容量有界不证明CPU/延迟有界。实现须用原owner创建受控私有FD，checked offset/length和总容量、禁路径注入/外部临时目录、限制窗口，失败/取消先确证消费者结束再按[恢复契约](M10_JPEG_R2_FAILURE_RECOVERY.md)清理。SIGKILL后不能仅按文件龄删除；现有未知残留策略继续有效。若需要C，须另行批准上游backend和更大的磁盘预算，不与本轮B自动捆绑。

## 6. D的兼容性影响与用户成本

以下**都未批准、未实施**。凡新规则拒绝原契约内可用的合法输入，标记 **PRODUCT_COMPATIBILITY_CHANGE_REQUIRED**，即使仍写40MP/64MiB。

| 可能规则 | 可量化受影响类型 | 用户成本、提示及替代 |
|---|---|---|
| 禁止progressive或只准baseline | 所有原支持progressive；占真实图片比例UNKNOWN | “此编码暂不支持严格校验”；重新选择其他图片或在外部明确转换；不能偷偷用宽松解码后生成一张合格图 |
| 低于完整系数的预算硬拒绝 | Bcoef<120,192,000B可拒40MP420；<240,000,000B可拒40MP444；其他尺寸/采样按公式 | “此图片超出当前校验资源能力，原封面未更改”；换图/外部裁剪后作为新输入重试；不得称文件损坏 |
| 固定scan数/时间上限 | 大量scan、慢但合法输入；合法100/101scan边界尚NOT_RUN，R1参数100不是规格 | “校验超时/未完成”；同字节重试不保证成功。永久cap或降低覆盖范围需批准，不能假定所有手机相同速度 |
| 固定65500单边、仅1/3分量、额外采样约束 | 原scanner没有该单边检查且允许2分量；65501～65535窄长图仍可能≤40MP | 是否原Android能处理及规范是否允许仍UNKNOWN；先造合法样本审计再决定，不能把PoC限制当已确认产品要求 |
| 静态按memoryClass/低内存机型拒绝 | 部分设备上的原合法输入；数量与机型范围UNKNOWN | 不冒称设备安全阈值；给出明确资源原因和保旧结果，另审设备支持/兼容承诺 |

真实malloc/系统资源不足应安全拒绝并保旧，这与新设永久格式cap不是同一事实；两者都需如实分类。资源预检只做保守估算，不替代消费全部压缩数据的严格解码。严禁自动降级为BitmapFactory成功或只验首尾成功。PNG原路径及Q05已有封面字节不变。

## 7. 预算结论

R2足以推荐“B1隔离＋A2计费＋JPEG全链worker”进入下一轮隔离验证，**不足以给出任何手机通用安全预算或生产GO**。所有新数值配置、worker峰值、规范化内部工作量、最坏中止、32位/真机及兼容审计仍NOT_RUN。下一阶段硬闸门及另行批准范围见[DECISION](M10_JPEG_R2_DECISION.md)。

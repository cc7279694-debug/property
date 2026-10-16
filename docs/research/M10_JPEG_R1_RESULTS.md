# M10-R1 — 实验结果与R0矩阵对照

日期：2026-10-16。**BLOCKED — M10-R1 isolated native PoC**。锁定缺损的严格拒绝已证明；生产资源与取消闭环仍不满足GO。安装/源码/范围见[执行记录](M10_JPEG_R1_EXECUTION.md)。

## 结果口径

[实际计数](m10-jpeg-r1/evidence/result-counts.json)：122次登记的decoder调用，121次有结构化返回、1次supervisor超时；累计519次明确完成的内部迭代。结构化结果为62 ACCEPT、39 REJECT、10 RESOURCE_REJECT、8 CANCELLED、2 INFRA_ERROR。另6次加载/故障诊断不算图像调用。**这些是结果分类，不是122项产品测试PASS**；被接受的变异、未知样本及超时均未隐藏。

原始证据：[最小4调用](m10-jpeg-r1/evidence/minimal.json)、[扩展108调用逐项JSONL](m10-jpeg-r1/evidence/expanded.jsonl)、[完整40MP444两调用](m10-jpeg-r1/evidence/single-444.json)、[补充5调用](m10-jpeg-r1/evidence/supplement.json)、[晚取消原输入复核](m10-jpeg-r1/evidence/late-cancel-recheck.json)、[shared及故障](m10-jpeg-r1/evidence/runtime-edges.json)。扩展驱动遇超时即退出1，未生成其整体成功summary；不能把文件缺失解释为全部成功。

## D01～D03：先运行的锁样本

| 固定原件 | SHA-256 / bytes | A2 | A1 |
|---|---|---|---|
| complete.jpg | `2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907` /59,334 | ACCEPT，exit0，256行+finish，0 warning，消费59,334字节 | ACCEPT，exit0，完整decompress，0 warning |
| entropy_cut_eoi_retained.jpg | `f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a` /20,194 | REJECT，exit10，warning117：premature end of data segment，78行后停止，finish=false | REJECT，exit10，TJERR_WARNING=0，警告同义，finish=false |

均为真实API36 /固定3.2.0，没有用此前2.1.5宿主结果替代。A2两次行缓冲768字节；FD4→4、caller余额0、decoder销毁、文件关闭。A1不公开压缩消费位置，记录null；不能把成功header或缩略输出存在作为完整性证明。

## 原创语料和样本事故

原创算法/参数见[fixture说明](m10-jpeg-r1/FIXTURE_PLAN.md)、[生成源](m10-jpeg-r1/native/generate_fixtures.c)、[生成及SHA清单](m10-jpeg-r1/evidence/generated.json)。十九小图含灰度、RGB/YCbCr、420/444、baseline/progressive、restart、1×1、1×40000、40000×1、EXIF1～8；加完整40MP baseline420、progressive420、单独完成的progressive444，共**22份已验证合法原创JPEG，两方案44次全部ACCEPT**。另有锁定健康原件单列。语料来自一个固定编码器，不代表真实相机误拒率或无漏检证明。

首次catalogue生成超过240秒；恢复得到22个候选输出，但最后`large_40mp_progressive_444.jpg`仅20,406,272字节、无EOI，两decoder均报告Premature end of JPEG file。历史category曾写AUTHORED_HEALTHY，仅表示当时预期，**该文件现在明确为GENERATION_INCOMPLETE，不是合法图片误拒**。

为消除缺口，新目录用相同原创算法单独生成`valid_40mp_progressive_444.jpg`，生成exit0、56.697秒、27,445,246字节，SHA-256 `0555234207d5ddf84d236d812487a659f2dac34a9d82109a941628553c7b67e6`；前一文件是完整文件的精确前缀，原字节未覆盖。两decoder均ACCEPT。见[生成原记录](m10-jpeg-r1/evidence/single-444-generation.json)和[实际结果](m10-jpeg-r1/evidence/single-444.json)。不将先前未知退出码补成0。

## D04～D18逐项边界

| R0 ID | 实际结果 | 限制／未执行 |
|---|---|---|
| D04 baseline | PASS于选定原创灰度、RGB、采样和长宽变体 | 不是全格式或相机兼容认证 |
| D05 progressive | PASS于小图、40MP420/444完整文件 | 大图高RSS；全部允许采样/扫描组合未覆盖 |
| D06 progressive损坏 | entropy切除、尾部切除保留EOI明确REJECT | 整个最后scan删除仍ACCEPT；严格解码不是防篡改证明 |
| D07 restart | 正常成功；RST缺失/乱序、前段删除等明确REJECT | baseline一个RST后8字节删除仍ACCEPT；不得声称任何删字节必被发现 |
| D08 EXIF | 方向1～8字节不变并被接受；异常TIFF类型亦可能接受 | 严格解码不验证全部EXIF语义；正式旋转/GPS剥除链本轮NOT_RUN |
| D09 范围外编码 | 源码拒绝非8bit/Huffman/允许分量外范围 | 真实12bit/lossless/算术/CMYK等输入NOT_RUN；PNG正式路径不改 |
| D10 长度/像素/复杂度 | 64MiB−1/正好可进decode，+1分配前RESOURCE_REJECT；8000×5001超40MP拒绝；零宽高/极端SOF拒绝；4096 COM实测 | 尾部padding仅测长度，不是健康语料；40MP精确合法图已测，±1像素无法以两个uint16因子编码；100扫描与所有恶意组合未测 |
| D11 资源失败 | 两方案caller输出分配故障注入、1MiB中间内存限制均明确RESOURCE_REJECT/清理 | 不是每个库分配点或真实系统OOM注入；JNI分配异常NOT_RUN |
| D12 取消/重复 | A2 deadline0/1/10/50/300ms、早/晚进度点有明确CANCELLED及清理；四组100次循环、合法444三次循环有余额证据 | 一次晚取消120秒超时未定位；A1无协作取消；Kotlin Job→JNI取消NOT_RUN |
| D13 加载/符号 | x86_64 shared加载/调用；缺库50、错误ABI50、缺符号51，不回退 | JNI/Java加载异常映射NOT_RUN；其他ABI真实加载NOT_RUN |
| D14 crash/监督 | 原创helper主动abort→134、SEGV→139，均非REJECT；真实driver TIMEOUT单列 | 两崩溃是监督诊断，不是decoder自行崩溃；真实系统OOM/SIGKILL NOT_RUN |
| D15 资源/CPU | baseline、40MP420/444、64MiB、重复/取消均有实测 | 全native堆硬上限、最坏CPU/扫描与并发预算仍BLOCKED |
| D16 ABI/平台 | 四ABI COMPILED，精确API26目标；全部ELF对齐/同余PASS；API36 x86_64 CLI/shared实跑 | API26/真机/32位runtime/arm64设备/16KiB页设备NOT_RUN；当前设备4KiB页 |
| D17 体积/许可 | 各ABI strip.so、符号和系统依赖实际读取；许可证检查 | 研究.so含main和两API，不是生产JNI；APK增量/打包对齐NOT_RUN |
| D18 误拒/漏检 | 22份完整原创JPEG无误拒；明确展示两种未检出的变异 | 单一编码器小语料，真实相机、广泛编码器语料和误拒率NOT_RUN |

派生的24文件来自原创父图，精确编辑区间、父/子SHA及预期不确定性见[mutation-manifest](m10-jpeg-r1/evidence/mutation-manifest.json)、[原始JSONL](m10-jpeg-r1/evidence/mutation-records.jsonl)。没有改变两份锁样本；header尺寸变异不冒称已重新编码健康图。

## 保留的失败和未解决项

首次晚取消：原不完整444、`--cancel-after-progress 1000`，host等待120.063秒，exit=null、stdout空；不能判断为普通坏图拒绝、成功取消或native崩溃。后续检查无该进程；后续相同原字节参数在带远端结果文件的独立复核中明确CANCELLED（367.216ms），完整444相同进度点亦成功。**没有取得首次native返回或确切根因，原超时仍UNKNOWN，不用复跑通过关闭。** 部分故障诊断与原扩展运行重叠，不能排除设备/ADB争用；原耗时不是受控最坏CPU证明。

原三项正式JPEG拒绝断言、M10-T03/T04生产链仍FAIL历史/PENDING；完整App回归本轮NOT_RUN。UI-C1的86 JVM PASS＋629 Android PASS／3 FAIL＝718项、715 PASS／3 FAIL、整体FAIL及N1～N4保留；研究分支没有合入UI代码或改写原正式登记。

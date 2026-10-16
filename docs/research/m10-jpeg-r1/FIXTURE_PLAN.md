# M10-R1 — 原创隔离 JPEG fixture 规范

本文件描述生成器输入算法及预期集合，不是成功执行或解码结果。实际生成命令、字节数、SHA-256、解码返回值及设备结果须由本轮执行登记提供。两张锁定 JPEG 不由此程序生成，不改字节、不替换。

## 生成与来源

生成器：[native/generate_fixtures.c](native/generate_fixtures.c)。只通过固定官方 libjpeg-turbo 3.2.0 的 C compression API 编译为独立研究可执行文件；不连接正式 App，不新增正式测试资源。程序要求已存在的输出目录，以 C11 `wbx` 独占创建每个文件，拒绝覆盖已有同名文件。输出只能放在 ignored 的本轮 `build/m10-jpeg-r1/` 或设备本轮独立目录。

```text
generate_fixtures <existing-empty-output-dir>
generate_fixtures <existing-empty-output-dir> --large
```

每个成功关闭的文件向 stdout 写一条 JSON，标记 `GENERATED_NOT_DECODE_VERIFIED`；字段包含固定 seed、尺寸、颜色空间、采样、质量、progressive、restart MCU 间隔、EXIF 方向、行缓冲字节数及文件字节数。生成器不会计算 SHA，执行器应对实际文件另外计算 SHA-256。错误写 stderr 并非零退出；未完成文件尝试删除，已经完整生成的文件保留并有记录，不能把部分批次写成全批次完成。

## 像素算法 lithic-coordinate-v1

所有样本固定 seed `0x13579bdf`（十进制 324508639）。坐标 `(x,y)` 的哈希运算均为明确的 uint32 模 2^32 算术：

```text
v = seed XOR (x * 0x9e3779b9) XOR (y * 0x85ebca6b)
v = v XOR (v >> 16)
v = v * 0x7feb352d
v = v XOR (v >> 15)
v = v * 0x846ca68b
h = v XOR (v >> 16)
R = (3*x + 5*y + (h AND 15)) AND 255
G = (x + 7*y + ((h >> 8) AND 15)) AND 255
B = (11*x + 2*y + ((h >> 16) AND 15)) AND 255
```

灰度只用 R。逐行生成，不分配整张 RGB 源图。哈希乘法的回绕属于固定图案算法，和长度计算不同；路径相加、行宽乘组件数、JPEG 最大尺寸均在分配前检查。所有样本固定 `JDCT_ISLOW`、`jpeg_set_quality(..., TRUE)`，baseline `optimize_coding=FALSE`。Progressive 使用固定 3.2.0 的 `jpeg_simple_progression`；该库可在 progressive 内部启用所需的优化，不能将生成器的请求字段冒充最终编码扫描脚本或内存特性。

同一固定库、ABI、编译参数及 SIMD 路径下重复生成应逐字节核对；不得未经实测宣布不同 ABI/SIMD 也必然字节相同。

## 默认集合（19 张健康候选）

| 文件 | 尺寸 | JPEG 编码 | 参数 |
|---|---|---|---|
| baseline_rgb_420.jpg | 257×193 | 8-bit baseline YCbCr 4:2:0 | Q90 |
| baseline_rgb_444.jpg | 257×193 | baseline YCbCr 4:4:4 | Q90 |
| baseline_native_rgb_444.jpg | 257×193 | baseline 真正 RGB 4:4:4 | `jpeg_set_colorspace(JCS_RGB)`，Q90 |
| baseline_gray.jpg | 259×197 | baseline 单组件灰度 | Q90 |
| baseline_1x1.jpg | 1×1 | baseline YCbCr 4:2:0 | Q90 |
| baseline_1x40000.jpg | 1×40000 | baseline YCbCr 4:2:0 | 合法极长尺寸，Q90 |
| baseline_40000x1.jpg | 40000×1 | baseline YCbCr 4:2:0 | 合法极宽尺寸，Q90 |
| progressive_rgb_420.jpg | 257×193 | progressive YCbCr 4:2:0 | Q90 |
| progressive_rgb_444.jpg | 257×193 | progressive YCbCr 4:4:4 | Q90 |
| baseline_restart_420.jpg | 257×193 | baseline YCbCr 4:2:0 | restart_interval=7 MCUs，Q90 |
| progressive_restart_420.jpg | 257×193 | progressive YCbCr 4:2:0 | restart_interval=7 MCUs，Q90 |
| exif_orientation_1.jpg … exif_orientation_8.jpg | 各257×193 | baseline YCbCr 4:2:0 | Q90，APP1 Exif 最小 Little-endian TIFF |

EXIF 每张写一个合法 SHORT orientation tag，值为 1～8；没有 GPS、缩略图或其他私人元数据。JPEG 压缩数据相同而方向标签不同，严格预检不应修改字节/方向。这些输入不证明正式规范化链已经正确执行方向变换。

## `--large` 额外集合（3 张健康候选）

| 文件 | 尺寸 / 像素数 | 编码 |
|---|---|---|
| large_40mp_baseline_420.jpg | 8000×5000 = 40,000,000 | baseline YCbCr 4:2:0，Q85 |
| large_40mp_progressive_420.jpg | 同上 | progressive YCbCr 4:2:0，Q85 |
| large_40mp_progressive_444.jpg | 同上 | progressive YCbCr 4:4:4，Q85 |

生成器只保留 24,000 字节输入行，但 **压缩器** 的 progressive 系数缓存仍可能很大，不能据此宣称生成或解码总 native 内存受 24KB 限制。这组固定图案不是所有相机图、最坏熵或所有 progressive 扫描复杂度的替代证明。

## 损坏与边界派生输入

生成器故意只产生健康编码候选，不在编码过程中植入不可靠坏图。执行器可另用可审阅的 byte mutation 脚本，从本组新图派生 progressive 中段 entropy 删除但保留 EOI、RST marker 缺失/乱序、RST 前后局部截断、异常 SOF 尺寸及 metadata/scan 压力输入。每个派生文件必须登记父 SHA、精确字节偏移/删除或替换规则、输出字节数与 SHA，不覆盖父图。

已提供 [scripts/derive_fixtures.py](scripts/derive_fixtures.py)。它只读取 allowlist 中五个原创小图名称，拒绝原件同名输出，默认不生成大文件。示例命令（不是执行记录）：

```text
python docs/research/m10-jpeg-r1/scripts/derive_fixtures.py --generated-dir <authored-small-fixtures> --output-dir <new-empty-derived-dir>
python docs/research/m10-jpeg-r1/scripts/derive_fixtures.py --generated-dir <authored-small-fixtures> --output-dir <another-new-empty-dir> --include-size-boundaries
```

每项派生登记原始父 SHA、零基源字节区间、替换 pattern/repeat_count、移除和替换 SHA、输出区间/大小/SHA及预期分类；二次读取校验输出 SHA，立即追加并 flush `mutation-records.jsonl`，整批结束另写 `mutation-manifest.json`，并复查父文件不变。中途失败保留已完成输入及逐项记录，不计整批成功。可产生 progressive entropy 和整扫描删减、baseline/progressive restart 前后删除/错序、缺失 EOI、APP1 声明长度/EXIF语义问题、4096空 COM、SOF 尺寸/乘法边界。`REJECT_EXPECTED_NOT_PROVEN` 和 `UNKNOWN` 都不能自动计为应拒绝坏图；真实解码结果须独立登记。

64MiB−1、64MiB、64MiB+1 的 opt-in 样本由 baseline EOI 后追加确定性零字节构成，只验证输入长度门禁；前两者是否拒绝仍受其他策略影响，不称健康图。尺寸精确乘积为 39,999,999 和 40,000,001 时，不存在两个均不超过65535的整数宽高，因此无法直接编码为 JPEG SOF 的16位宽高；脚本明确登记该数学限制，另以40,008,000和65535²覆盖超过上限及乘法风险，不伪称执行了精确±1像素图。40,000,000的派生 SOF 同样不是重编码的健康压缩数据；真正40MP健康输入来自 C 生成器的大图集合。

JPEG 中某些删减仍可能形成语法合法且可完整解码的不同图像；测试必须按实际语义分类，不能因为视觉变化就假定所有严格解码器都能证明原图被篡改。64MiB±1 文件大小边界也应另作明确构造，不把 padding 当成新的健康相机 JPEG。

## 清理与结论边界

生成器使用 C `setjmp/longjmp`，跨越的栈没有 C++ 析构责任；所有可变 decoder/file/row 状态都在 heap state 内，异常统一销毁 compressor、关闭句柄、释放行与路径，删除未完成的新文件。它不测试 JNI、取消或正式图片导入租约。生成与解码是不同证据；未执行编译/生成/解码的部分维持 NOT_RUN。

# M10 R2-E01 — 宿主分配覆盖核查与预算前置检查

2026-10-18。**HOST_ONLY / PARTIAL；E01 BLOCKED，等待 ChatGPT 限定审查。**

本轮转回产品 JPEG 资源问题，没有继续 Android 环境排障。新增的是可执行的输入身份校验、SOF 元数据读取及系数 payload 算术检查；**没有实现或验证 native 计费 backend**。32 个宿主测试 PASS 不能填补真实 malloc 故障注入、释放归零及总内存预算的缺口。

## 来源、范围和复用

- 正式 M10：`feat/m10-local-cover-media` / `41596820145dc4a3fa6d041728a59540d828108a`，开工本地/远端一致，原工作区干净。
- R1：`2cdad4aa40f9503aad13245670adc9a4d13db32b`；R2：`2912601899f0282ecc6aa32d5cf26a7b44c24c63`；R3：`17b2cb40d8f3b95136f3d43e5c91cd7aee7b8e5d`。继承 [R2 E01 闸门](../M10_JPEG_R2_DECISION.md)、[资源模型](../M10_JPEG_R2_RESOURCE_BUDGET.md)及 [R1 原生源码](../m10-jpeg-r1/native/strictjpeg_probe.c)。
- 新分支：`codex/m10-r2-e01-host`；独立 worktree：`E:/CODEX/.codex/worktrees/lithic-e01/砺石新APP`。从累计研究提交 `bc3b4e3ce533246ccd1a06f738c4a871d3f263d1` 建立，保留 C1 许可交付整改及全部历史。此来源与正式 M10 的 `app/`、Gradle 输入无差异；R1 native 研究文件也与 R1 冻结提交无差异。
- Goal：确认分配覆盖接缝及可复用物，先使资源计算与输入来源可重复核查。Scope：本目录研究 Python / 测试 / 小型证据、唯一 CURRENT_STATE 和新 checkpoint。验收：真实 RED/GREEN、锁样本不变、数值区分模型与实测、native 未执行如实阻塞。
- 不改正式代码、旧测试、R1～R3 原件、UI-C1、JNI、Worker、Room 或依赖。不操作 AVD / ADB / APK、H2/C2 现场，不重跑既有 decoder 实验，不进入 M11。无下载、安装、许可接受、ZIP 或新源代码/图片归档。

固定源码来自 R1 已认证的 `libjpeg-turbo-3.2.0.tar.gz`，2,537,858 字节，SHA-256 `6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e`。原 GPG/版本/许可来源沿用 [R1 provenance](../m10-jpeg-r1/evidence/upstream-r1-provenance.json)，本轮未重新签名验证。8 个选定源码文件再次与该 tar 的原始成员逐字节哈希比对一致，**未提取或修改原件**。加上 archive、2 份原研究文件和 5 份样本，共 16 项输入见 [source-lock.json](source-lock.json)，不是全源码或构建覆盖 Manifest。

## 分配路径：已核实源码，尚未实测覆盖

下表行号指上述固定源码；只支持 A2 libjpeg 解码路径的后续实施审查，不宣称覆盖整个 libjpeg-turbo。

| 接缝 | 确认事实 | E01 尚缺的真实验证 |
|---|---|---|
| `jdapimin.c:59–69`；`jmemmgr.c:1226` | `client_data` 在 create 清零时被保留；初始 memory manager 经 `jpeg_get_small` 分配 | 创建前关联外部预算；第一次分配失败、半初始化 destroy、上下文生命期 |
| `jmemmgr.c:318 / 392` | 小池/大池最终经 get_small/large；池头、对齐、slop 一起进入低层请求 | 低层 backend 预算 checked 预留/失败无部分更新；自身 header/ledger 计费；不能只包装创建后的公开 allocator |
| `jmemmgr.c:578 / 585 / 806`；`jdcoefct.c:895` 附近 | 虚拟系数数组通过 alloc_barray，经小池行指针及大池数据块；默认 backend 无 spill | progressive 及 baseline 多 scan 实际分配轨迹、所有池/请求覆盖、全路径失败释放 |
| `jmemmgr.c:1143 / 1156 / 1182` | 大池、小池、manager 分别释放，随后 mem_term | 对称释放、坏归属/重复 free、longjmp 及部分创建失败后的计数归零；RSS 不能替代 |
| `jmemnobs.c:32 / 49` | 当前 get_small/large 直接 MALLOC，free 直接 free，未计费 | 现有实现不能直接当资源受控 backend；MAXMEMORY 不是总内存上限 |
| `jdicc.c:143` | `jpeg_read_icc_profile` 存在绕过上述 backend 的直接 malloc | R1 A2 不调用此 API，不能泛称全库分配均已覆盖；未来调用图变化必须重新审核 |
| R1 `caller_allocate`、context、8KiB source、输出行及 stdio | 原 trackedCaller 仅调用方；不含库内部/stdio/allocator arena | 调用方、预算上下文本身、ledger、行缓冲也须纳入请求账本；FILE/栈/系统 allocator 物理开销继续单列 |

**失败注入的重要语义：**`alloc_small` 在失败后会减半 slop 重试（`jmemmgr.c:316–323`）。因此后续至少区分 fail-at-N 和 fail-from-N：单次注入后成功重试可合法 ACCEPT，但必须证明注入命中且最终无泄漏；持续失败才要求资源拒绝与安全释放。不能为让每点失败测试通过而删除上游 fallback，也不能把真实 crash / abort / 无结果当普通 RESOURCE_REJECT。

## 新增代码和实际验证

[e01_audit.py](e01_audit.py) 只读运行，不调用外部工具、不解码、不改输入：

1. 校验 16 项锁定输入的长度、SHA 和限定路径，拒绝缺失、变更、重复和越界。
2. 读取到第一个 SOF0/SOF2；不消费 entropy、不验证 EOI。APP 元数据按声明长度跳过，不能把元数据里的伪 SOF 当帧头。
3. 计算完整系数数组路径的 **CALCULATED payload**：`128 × Σ[(hᵢ × ceil(W/(8Hmax))) × (vᵢ × ceil(H/(8Vmax)))]`，另列行指针 payload。32/64 位是 Python 的检查算术模型，**不是 native ABI/OOM 验证**。

公式只在完整系数数组路径适用；baseline 头无法证明单 scan，故同样输出条件预算，不能报实际系数占用为零。池头、slop、对齐、元数据、context、stdio、栈、分配器开销、后续 Bitmap/worker 重叠均不包含，`totalNativeBytes=null / UNKNOWN`。不提供或批准任何手机安全 cap。

数学模型可读取 65535 单边及 2/4 分量，用于避免偷偷缩窄产品讨论；固定 native 的 `JPEG_MAX_DIMENSION` 实为 65500，R1 A2 仅处理 1/3 分量。这些额外模型输入**不是 decoder 兼容性证据或产品准入规则**。非 SOF0/SOF2、非 8bit 等模型不支持也不能当产品 INVALID_IMAGE。

| 实际检查 | 结果 | 原始证据 |
|---|---|---|
| TDD RED：尚未实现的接口 | 32 唯一方法，33 断言失败（一个参数化方法有两条失败），exit1；无导入/语法错误冒充 RED | [red.txt](evidence/red.txt) |
| GREEN：完整本目录测试 | 32 唯一方法 PASS，exit0；算术溢出/边界、头部截断/元数据、条件预算、源身份拒绝 | [green-initial.txt](evidence/green-initial.txt)、[测试源码](test_e01_audit.py) |
| 原 archive 与 8 选定源码成员 | MATCH；原实物未重建、未重新提取 | [source-archive-check.json](evidence/source-archive-check.json) |
| 16 项现有输入、5 真实文件头与双位宽模型 | 已实核运行，exit0；只是本次只读审计脚本成功，**E01 仍 BLOCKED** | [input-and-payload-results.json](evidence/input-and-payload-results.json) |
| Windows native toolchain 最小编译 | **BLOCKED**：clang exit1，`vcruntime.h file not found`，未生成 executable、未进入 native allocator 实验 | [host-toolchain-check.json](evidence/host-toolchain-check.json) |
| native 分配计费、逐点 OOM、释放归零、ASAN/泄漏、CPU/取消、32位 native | **NOT_RUN / BLOCKED** | 缺完整 host CRT/链接环境，未安装或另造 CRT |
| Android / APK / 全 App JVM/Lint/构建 / 原三 JPEG 断言重跑 | **NOT_RUN** | 本轮不授权；原 FAIL 保持，不由 Python PASS 替代 |

实际输入观察（详含字节/SHA见原 JSON，不复制图片）：

| 既有文件 | 读取到的尺寸 | 完整系数 payload | 行指针 payload 32/64 |
|---|---:|---:|---:|
| `complete.jpg` | 256×256 / baseline 420 | 条件 196,608 B | 256 / 512 B |
| `entropy_cut_eoi_retained.jpg` | 同上 | 同上 | 同上 |
| `large_40mp_baseline_420.jpg` | 8000×5000 | 条件 120,192,000 B | 5,008 / 10,016 B |
| `large_40mp_progressive_420.jpg` | 8000×5000 | 120,192,000 B | 5,008 / 10,016 B |
| `single444/valid_40mp_progressive_444.jpg` | 8000×5000 | 240,000,000 B | 7,500 / 15,000 B |

健康锁样本 SHA `2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907`、缺损锁 SHA `f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a` 均不变。**两者头部/模型相同正说明此脚本不能验证 JPEG 完整性**。R1 中不完整的 `fixtures/large_40mp_progressive_444.jpg` 未被冒充健康样本；本轮使用原完整的 single444 实物。

复现：在本目录运行 `python -B -m unittest discover -s . -p test_e01_audit.py -v`；输入审计运行 `python -B e01_audit.py --r1-worktree "E:/CODEX/.codex/worktrees/m10-jpeg-strict-r1/砺石新APP"`。不自动探测或操作 Android。原研究物不存在时应报缺失，不能下载/生成替代物。

交付核查：12 项变更均在本研究目录、CURRENT_STATE 和新 checkpoint；32 唯一方法与 32 条 GREEN 逐项计数一致；新文档本地链接、常见密钥模式及禁止二进制后缀检查通过。源码/文档空白检查通过；原始 RED 的参数化失败标题有 1 处行尾空格，`git diff --check` 对该原始输出报出提示，原样保留而不清洗测试证据。正式 App/Gradle 差异为空。

## 具体阻塞和最小续接

现有 NDK r28c 含 Windows 可运行的 clang，不能据此断言 Windows 原生编译环境完整。本轮单次最小 `stdio/stdlib/malloc/free` 编译明确在旧 UCRT 的 `corecrt.h` 缺 **vcruntime.h** 处失败，尚未进入链接，所以完整 host runtime/link 链也未证明。PATH 和常规 VS/MinGW 盘点未找到可用完整链；不把旧注册表路径或只含 docker-desktop 的 WSL 列表当可执行环境。没有安装工具、接受新许可、用自造 CRT 或借 Android 执行绕过。

下一步先由独立审查决定如何提供受支持的既有/获准宿主 C 编译运行环境；不得推断本轮已授权安装。环境就绪后，沿用上述固定源码与原 A2 逐行严格路径，只替换低层 get/free backend，创建前注入预算上下文，逐请求计费池头/对齐/账本及 caller。完成真实 fail-at/from-N、首分配/半创建/longjmp/OOM/正常 destroy 的归零、32/64 native 和 sanitizer 证据，才能推进 E01。**本轮未提交未经运行验证的 allocator 实现，未降低原安全、40MP/64MiB 或兼容要求。**

R1 232MiB RSS、不明 6.141秒检查空档和 120秒 UNKNOWN；R3 完整生命周期 BLOCKED；N01～N03、D2A-N01～N05、A1-N01～N04及全部历史 NOTE/FAIL/UNKNOWN/NOT_RUN 均保留。H2 `DELIVERY_UNKNOWN/pin=true` 及 C2 现场完全未访问。M10 **PENDING / 原三项 JPEG FAIL**；UI-C1 **ACCEPTED / FROZEN — PASS WITH NOTES**；M11 **NOT_AUTHORIZED**。本轮只推研究分支后停止，不自行进入 native/Android/生产修复下一阶段。

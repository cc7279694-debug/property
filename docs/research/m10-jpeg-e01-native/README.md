# M10 E01 — Linux native 宿主研究

来源 `dafb7bc031bd67f44ca7d62e251785f8905d0ab7`；继续 `codex/m10-r2-e01-host`，独立研究目录。用户授权临时 Ubuntu24.04 Actions 验证，不安装本机工具，不操作 Android 或运行现场。

## 代码与验证边界

- `budget.c/h`：每个 decoder 独立账本；真实 malloc/free，payload、对齐头部与 intrusive ledger 一起计费。容量/计数器 checked 或饱和，失败不修改 live ledger；错误 owner/size/kind、未知/重复 free 拒绝后保留合法清理能力。
- `jmem_budget.c`：仅在临时 runner 提取目录替换固定 3.2.0 的 `src/jmemnobs.c`。创建前通过 `client_data` 关联预算，覆盖初始 manager 和 get_small/large/free；原 jmemmgr 小池重试不改，默认无 spill。
- `strict_probe.c`：budgeted heap context 含8KiB源缓冲，单行输出，不申请整幅 RGBA。严格消费真实压缩数据，warning/fatal 用 C 错误边界退出，半创建也 destroy，随后逐项实际释放并核对账本。进程 crash、超时、INFRA/清理失败均不算普通图片拒绝。
- `budget_test.c`：12个真实C测试；`E01_RED_BACKEND` 仅作显式 mutation RED（故意去掉容量判断），默认构建启用检查。此 RED 是测试敏感度验证，**不是此前生产代码出现过相同失败的证明**。
- `run_ci.py`：已存在 GCC普通构建和 Clang ASan/UBSan（启用 leak detection），无 apt/pip/工具安装。按两样本正常轨迹的每个分配序号分别注入 fail-at-N/fail-from-N；单次失败后允许真实 slop fallback 成功，但必须命中注入且完成清理。
- [workflow](../../../.github/workflows/m10-e01-native.yml)：仅该研究分支 push、仅代码路径触发，`contents:read`，Ubuntu24.04 单job15分钟。公开仓库按精确commit匿名fetch；没有secret引用、第三方Action、cache、artifact上传、部署或产品构建。报告提交不触发重跑。

固定官方源码只在临时 runner 下载/提取，URL沿用原 [R1 provenance](../m10-jpeg-r1/evidence/upstream-r1-provenance.json)，3.2.0归档必须为2,537,858字节及SHA `6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e`，不符即停止。替换前也核对原backend SHA；不提交上游源码、归档、二进制或重复图片。原核心IJG/BSD许可来源不变，本轮不发布任何native产品。

只使用仓库原有 `complete.jpg`（59,334B，SHA `2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907`）和缺损 `entropy_cut_eoi_retained.jpg`（20,194B，SHA `f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a`）。两者逐字节核对；不向CI补传本地未提交的40MP样本，不生成替代图片。

## 实际结果

**HOST_LIMITED_PASS / E01全量仍 NOT_PROVEN。**这是 C 实际运行，不沿用之前32项Python模型PASS作为证据。

[CI首轮运行及完整日志](https://github.com/cc7279694-debug/property/actions/runs/39105247179) · [单job](https://github.com/cc7279694-debug/property/actions/runs/39105247179/job/117609557501) · 执行代码 `a4075344cc667c1f6795442f883cde75f01d2ea8`。`push`触发，run_attempt=1，2026-10-18 06:46:47～06:47:28 UTC，41秒，所有workflow步骤成功，无重跑。最终文档提交未改变这份已测试C/CI输入。

| 项目 | GCC普通构建 | Clang sanitizer构建 |
|---|---|---|
| 运行器/工具 | Ubuntu24.04 x86_64 / GCC13.3.0 | 同runner / Clang18.1.3 |
| native编译 | 固定3.2.0静态libjpeg、unit、probe成功 | 同样成功，libjpeg与研究C均启用ASan/UBSan，`detect_leaks=1` |
| 显式mutation RED | 12方法，659断言，6失败，exit1（预期） | 同结果（预期）；没有将原始失败删去或冒充正常拒绝 |
| 默认GREEN | 12方法、657断言，0失败，exit0 | 12方法、657断言，0失败，exit0；无ASan/UBSan/LSan诊断 |
| native probe | 57次 | 57次 |
| fail-at / fail-from | 各26次（两样本各序号1～13） | 各26次（同参数） |
| slop重试接受健康图 | 3次（真实命中单次注入后完成解码） | 3次 |
| 全部probe清理 | 57次live=0 / liveBlocks=0 / badFrees=0；已取得的FD已关（未取得者不算close）、decoder.mem=NULL | 同57次核对通过 |

统计口径：**12个唯一C方法，在两种构建各执行一次，共24次GREEN方法执行；657断言/构建。**114次probe是参数化native进程运行，不是114个不同JUnit方法。114次结果共10 ACCEPT / 96 RESOURCE_REJECT / 8 IMAGE_REJECT，均为预期类别、退出码与结构化状态一致；没有INFRA、crash或timeout被计为普通图片拒绝。CMake3.31.6/Ninja1.13.2均是runner既有工具，未安装/升级。

- 健康锁图：两构建均消费59,334字节，256/256行＋finish成功、0警告；13次申请（caller2 / small5 / large6）；**计费请求峰值46,382B**，实际正常destroy后归零。
- 缺损锁图：两构建均消费20,194字节，到第78行产生警告code117，IMAGE_REJECT/exit10，finish=false；destroy及实际释放归零，没有宽松回退。
- 创建前/半初始化：`fail-from=2`使首次libjpeg manager分配返回NULL；phase=create、smallCalls=1、error54，createAttempted/destroyCalled均true，清理后归零。调用方context分配本身`fail-at/from=1`也被真实注入覆盖。
- 预算边界：limit=0明确RESOURCE；limit=46,382B完成健康图；limit=23,191B在start阶段RESOURCE，峰值23,124B，未超过cap。**这些都是实验参数及请求量，不是推荐手机预算。**unit另覆盖header计费、对齐、SIZE_MAX/计数器饱和、wrong owner/size/kind、未知/重复free保守拒绝及合法后续释放、64轮非LIFO释放。

原始GitHub单job日志已取回到本地忽略目录 `build/m10-e01-native-ci/run-39105247179.raw.log`，138,545B，SHA `a9b1a252c45eb9077a8f7f645d0b9d7429bb06face474f39b45d48c310a0977e`。本轮不向Git重复归档整份CI输出、源码、图片或二进制；独立复核可按上方run/job与代码SHA对应。首次`gh api`文本提取因终端转义保护退出1，空输出原样留在同目录；随后显式允许转义并仅重取日志到文件，exit0，未重跑实验；读取时未执行日志中的转义内容。

## 尚未覆盖与停止边界

计费是**受覆盖 malloc 请求字节**（包括自己的头/ledger及上游池头/对齐/slop），不是 malloc usable size、总 native 堆或 RSS 上限。预算根/codec栈、libc/printf、系统 allocator bookkeeping/arena、OS/FD、Android/JVM/Bitmap、SIMD等不在此证明内。当前使用单线程独立decoder，未验证共享账本并发。

两样本均为256×256 baseline。原40MP/progressive完整样本只在本地忽略目录，runner无法取得，相关规模/虚拟系数池压力、复杂scan及格式兼容 **NOT_RUN**；不借小图 PASS 关闭这些边界。fail-at/from只扫两样本基线轨迹序号，不能称全部图像及所有失败扩展路径覆盖。直接 malloc 的 ICC API未调用，不宣称全库统一预算。

Linux x86_64、关闭SIMD的研究构建不能替代Android/四ABI/32位运行、CPU/取消最坏时延、真实系统OOM/LMK、SIGKILL、Worker/恢复或整个E01通过。64MiB默认堆预算及probe编码范围只是小样本实验控制，不是新产品门槛或手机安全值；40MP/64MiB原规格不改。

M10 **PENDING / 原三 JPEG FAIL**、UI-C1 **FROZEN**、M11 **NOT_AUTHORIZED**；R3 BLOCKED、H2/C2现场、全部历史NOTE/FAIL/UNKNOWN/NOT_RUN保留。研究提交推送后停止，等待ChatGPT限定验收，不自动接入生产或开展下一实验。

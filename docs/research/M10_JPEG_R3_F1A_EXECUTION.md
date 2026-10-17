# M10-R3-F1A — 最小研究修复与交付

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1A Research Fixes**。仅研究代码/JVM/静态审阅/构建完成，等待 ChatGPT 独立验收；不是 Android 生命周期通过，不授予 ACCEPTED/FROZEN 或 F1-B。

[代码审阅](M10_JPEG_R3_F1A_CODE_REVIEW.md) · [测试结果](M10_JPEG_R3_F1A_TEST_RESULTS.md) · [checkpoint](../checkpoints/2026-10-17-M10-JPEG-R3-F1A.md)。

## 契约与来源

- Goal：修复 Outsider 连接释放责任，以及旧 attempt 未收口就发布新会话的两项研究缺口。
- Scope：`docs/research/m10-jpeg-r3/` 的相关 Java/测试/研究脚本，新 F1A 报告/证据、唯一 CURRENT_STATE 和新 checkpoint。
- Out of scope：安装/启动新 APK、normal 或其余 Android 场景、ADB 故障注入、正式产品/GC、JNI/native/allocator、Manifest/AIDL/WorkerService 生命周期改造、工具链升级、F1-B、M11。
- 基线：F0 `f513725b862d3422b371883536557c4233bed2e6`；原工作区与远端相符且干净。原生 worktree 工具从此提交建立独立目录，随后新建 `research/m10-jpeg-strict-r3-f1`，没有 reset、覆盖或从 main 开始。
- 工作区：`E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1/砺石新APP`。当前用户授权覆盖归档/AGENTS 中旧 M10/F0 阶段限制；产品和数据保护约束继续有效。
- 用户转交 F0 独立结论：**PASS WITH NOTES — D INTERMITTENT_RECOVERED**。Host 显示 1572ms、跨 UID 拒绝为 F0 历史实证；Outsider 泄漏和 guard-only 为本轮修复输入。历史两次 attach UNKNOWN 不关闭。
- 已读当前 AGENTS/PROJECT/CURRENT_STATE、工具链、F0 三报告及补充 checkpoint、R3 四报告及原始失败、R2 架构/资源/恢复材料和实际研究源码、Manifest、AIDL、测试/构建脚本。原件不改。

## 最小改动

| 范围 | 新增/修改与目的 |
|---|---|
| Outsider | 修改 `OutsiderActivity.java`；新增 `BindingAttempt.java`、12项 `BindingAttemptTest`；研究 outsider 配置增加已有缓存 JUnit4.13.2 测试依赖 |
| 旧 attempt | 修改 `StageStore.java`、`ProbeRunner.java`；新增 `StageAdmission`、`StageIdentity`、`AuthorizationFence` 及三份测试（14+12+5项） |
| 研究工具 | 新增 `run_f1a_jvm.py`、`build_f1a.py`、`audit_f1a.py`；沿用 R3 受控 ASCII 投影方式，保留原构建脚本 |
| 交接 | 本报告、代码审阅、测试报告、新 checkpoint、CURRENT_STATE、F1A 原始日志/输入 SHA/Manifest/DEX/签名核对；证据目录 `-text` 保留原字节 |

未删旧测试、未改旧17项方法/断言，未改 AIDL、WorkerService、两个研究 Manifest 或正式 App。没有新增运行库、权限、组件或 native 后端。生产 Room/Repository/媒体协议与原三项 JPEG 断言没有变动。

## 真实执行

- JVM：**60个唯一方法 PASS，0 FAIL/ERROR/SKIP**；原17项保留，新增43项。源码方法身份与五份最终 XML 精确一致。
- 原始 RED/GREEN 保留：Outsider 12项 RED10FAIL→GREEN12PASS；准入14项 RED13FAIL；身份比较12项 RED10FAIL；回调围栏连同原17项共22项 RED4FAIL→GREEN22PASS。独立分组含重复回归，不能相加为唯一总数。
- RED 来源明确：旧策略被提取为可执行 JVM 边界；身份比较为测试先行初始 seam。不是在 JVM 执行未修改 Android Activity/StageStore，也不是本轮 Android 复现。快照及执行哈希均保留。
- 独立研究 Gradle 六任务退出0：**BUILD SUCCESSFUL in 1m 7s**，99 actionable tasks 全执行。进程 wall 67.600s。正式 `:app` 未构建。
- Lint：probe 0错误/4警告；outsider 0错误/3警告，共 **0错误/7警告**。原 probe4条保留；本轮首次同时检查 outsider。未压制、未升级依赖。
- APK 实物审计：包名/min26/target36、无权限、私有 Worker、无 native 库和 JUnit/Hamcrest 打包；旧锁定研究 APK 的证书指纹与新产物一致。
- 构建输入27份逐字节投影、SHA一致；最终源码与投影输入相符。最终 Git 源码/测试/产物对应核查见测试报告和交付审计。

## 研究 APK（只在本机交付，未安装/未公开推送）

| APK | applicationId | 字节数 | SHA-256 |
|---|---|---:|---|
| probe | `com.cooper.lithic.m10r3.probe` | 50205 | `a3eea12dfe05736a69c473fa812d0b52a43c8100bbed2cffd0ba9c8f5f0ea684` |
| outsider | `com.cooper.lithic.m10r3.outsider` | 10473 | `a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879` |

probe 绝对路径：`E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1/砺石新APP/docs/research/m10-jpeg-r3/host/probe/build/outputs/apk/debug/probe-debug.apk`

outsider 绝对路径：`E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1/砺石新APP/docs/research/m10-jpeg-r3/host/outsider/build/outputs/apk/debug/outsider-debug.apk`

构建/审计原始依据：[Gradle日志](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/gradle.log)、[27份输入](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/inputs.json)、[构建产物](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/result.json)、[逐方法/Manifest/签名/DEX审计](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/artifact-audit.json)、[旧证书比对](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/signing-continuity.json)。APK及临时 ASCII 构建缓存保持忽略，不进入 Git。

## 设备与保护

实际只读检查确认 `emulator-5596` / `Lithic_M01_API_36` / API36 在线。原始[身份和Git检查](m10-jpeg-r3-f1a/evidence/preflight/read-only-checks.json)是在工作区建立、修复开始后保留的快照，不将其含有本轮改动的 status 宣称为初始干净状态。初始清洁检查来自创建分支前的实际 Git 输出。

本轮没有安装、启动、force-stop、kill、清数据、重启/wipe/重建 AVD，没有修改时钟/字体/主题/密度/旋转。未读取私人账本/图片或重新核对其字节，不用“不执行写操作”代替私人数据实测 PASS。F0 安装 UID/元数据/设置保护仅为历史证据，本轮不重复登记为 PASS。

## 限制与停止点

- 纯 JVM 证明实际共享策略/比较/控制器；Android ServiceConnectionLeaked 是否消失、真实 FD/内核 lease/fsync/IPC/进程行为均 **NOT_RUN**。
- 不自动修复半清理：删除中断且没有完整终结证明时，保留 current 和剩余资源，拒绝新授权。旧版简化撤销/清理标记也不自动认领或迁移。
- 未知 stage/子项阻止新会话。原 stage_model 仍可验证已知资源清理且未知项保留；随后新会话会因该刻意残留而阻塞，不自动清除以运行更多案例。
- 这是受控合作研究消费者模型；取得 lease 不证明进程已死，更不证明同 UID 任意 native 代码安全，B1生产 NO-GO 保留。
- **原R3：0 PASS /1 UNKNOWN /28 NOT_RUN；历史 attach 两次 UNKNOWN、R1 120秒 UNKNOWN 原样保留。**
- **M10 PENDING/三项JPEG FAIL；UI-C1 ACCEPTED/FROZEN — PASS WITH NOTES；M11 NOT_AUTHORIZED。**正式验收矩阵/原130案例和 R0～R3/F0 历史证据不改。

代码、JVM、Lint、构建和产物证据已具备提交 F1-B 独立验证申请的条件；这不代表设备预检已通过或 F1-B 已获授权。须先等待 ChatGPT 对 F1-A 的独立审查，之后另行申请一次拒绝路径及一次 normal 的限定验证。本轮停止，不运行它们。

仅提交推送 `research/m10-jpeg-strict-r3-f1`，最终提交 SHA 与远端一致性以 Git 回执/最终回复为准，避免自引用。不合并、不 force、不发布 APK。

## 提交前检查

[交付审计](m10-jpeg-r3-f1a/evidence/delivery/precommit-audit.json)核对范围、27份Git暂存输入与构建SHA、原17方法、文件链接及敏感模式。正式产品/配置、验收矩阵、原规划与R0～R3/F0历史证据不改。审计会记录建立自身前后的文件数量口径；最终以Git提交树为准。

源码/脚本/当前文档 whitespace检查退出0。完整含原始日志的检查退出2，仅7份JUnit原始输出有末尾空行；按保留真实输出要求不修改、不压制，具体路径和返回码保留在审计JSON。这是日志格式记录，不是产品或研究测试失败。

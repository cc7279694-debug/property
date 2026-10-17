# M10-R3-F1A — 实际测试与产物

2026-10-17。**研究 JVM/构建结果 PASS，Android系统行为 NOT_RUN**。不是 M10 全模块通过；原三项 JPEG FAIL、R3 UNKNOWN 不变。

[执行报告](M10_JPEG_R3_F1A_EXECUTION.md) · [代码审阅](M10_JPEG_R3_F1A_CODE_REVIEW.md)。

## 最终唯一方法结果

| 测试类 | 唯一方法 | 实际结果 | 范围 |
|---|---:|---|---|
| SessionGateTest | 17 | PASS | 原源码/方法/断言字节相同，全部回归 |
| BindingAttemptTest | 12 | PASS | 拒绝、true/false返回、异常、销毁重试、意外解绑与幂等 |
| StageAdmissionTest | 14 | PASS | 全命名空间预检、同 lease 内收口、繁忙/失败保留 current、终结幂等及新指针门禁 |
| StageIdentityTest | 12 | PASS | 实际共享的完整键/值/attempt/nonce/generation/inode/device/类型比较 |
| AuthorizationFenceTest | 5 | PASS | 持久撤销/旧 current/损坏状态拒绝、SessionGate防重放、收口与回调串行 |
| **总计** | **60** | **60 PASS /0 FAIL /0 ERROR /0 SKIP** | **原17 +新增43；无重复计数** |

Gradle 的 `testDebugUnitTest` 是 **Windows宿主 JVM 测试**，不是 Android 设备测试。probe48、outsider12。五份 XML 与源码方法身份精确匹配，源测试输入在构建清单中逐字节核对。原17项源码与 F0 Git blob 相同。[逐方法结果](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/artifact-audit.json)。

## RED/GREEN原始证据与可重复性

| 分组 | 实际RED | GREEN | 原件 |
|---|---|---|---|
| Outsider控制器 | 编译0，12运行/10FAIL/2PASS | 12PASS | [说明/RED快照](m10-jpeg-r3-f1a/evidence/outsider-unit/README.md)、[RED](m10-jpeg-r3-f1a/evidence/outsider-unit/red.log)、[GREEN](m10-jpeg-r3-f1a/evidence/outsider-unit/green.log) |
| 旧attempt准入 | 编译0，14运行/13FAIL/1PASS | 与身份12及原17合计43PASS | [RED](m10-jpeg-r3-f1a/evidence/stage-unit/RED-junit.txt)、[源码快照](m10-jpeg-r3-f1a/evidence/stage-unit/RED-StageAdmission.java.txt) |
| 共享身份比较 | 编译0，12运行/10FAIL/2PASS | 同上43PASS | [RED](m10-jpeg-r3-f1a/evidence/stage-unit/identity-RED-junit.txt)、[源码快照](m10-jpeg-r3-f1a/evidence/stage-unit/RED-StageIdentity.java.txt)、[GREEN](m10-jpeg-r3-f1a/evidence/stage-unit/GREEN-junit.txt) |
| 回调围栏+原17 | 编译0，22运行/4FAIL/18PASS | 22PASS | [RED说明/快照](m10-jpeg-r3-f1a/evidence/authorization-red/README.md)、[RED](m10-jpeg-r3-f1a/evidence/authorization-red/junit.log)、[GREEN](m10-jpeg-r3-f1a/evidence/authorization-green/junit.log) |

RED 不含编译失败冒充：均编译成功后断言失败。为宿主执行而抽取的旧策略（guard-only/只在true时解绑/无持久回调围栏）及初始身份比较 seam 不是未修改 Android 组件本身，明确保留快照、哈希和边界。原始日志保持进程输出字节，包含 Windows 原有 CR/CRLF，不为美化改写。

StageAdmission 中 owner/receipt/inode 损坏场景由合成 backend 拒绝，证明预检失败后不删除、不发布的**编排**；实际共享 StageIdentity 的12项再证明键值/inode 比较。真实 JSON/lstat/socket/fsync 适配不因两者相加而成为 Android PASS。stage-unit GREEN-inputs 的 StageStore 快照早于最终 JSON 整数标准化/receipt 比较小修，不能当最终适配器哈希；最终27份构建输入为唯一最终源码/产物依据。

重跑最终研究构建：在 F1 工作区执行 `python docs/research/m10-jpeg-r3/scripts/build_f1a.py`。它只创建新时间戳 ASCII 投影和新证据目录，offline 构建研究 probe/outsider，不安装、不调用 ADB、不构建正式 App。宿主边界测试可用 `run_f1a_jvm.py`，phase 必须新名称，保留旧证据。每组 RED 可用保留快照替换临时编译输入后执行相同 JUnit 类，不能替换实际已提交主源码或改旧日志。

## 构建、Lint与APK实物

实际六任务：probe/outsider 两模块分别 testDebugUnitTest、lintDebug、assembleDebug；`--offline --max-workers=2 --console=plain --stacktrace`。最终 **BUILD SUCCESSFUL in 1m 7s**，退出0，99任务实际执行；NO-SOURCE/SKIPPED不计测试。

工具链复用 JDK17.0.20.1+1、Gradle8.14.3、AGP8.13.2、Build Tools36.0.0、min26/target36。实际 [JDK版本](m10-jpeg-r3-f1a/evidence/preflight/jdk-versions.json)及 build/Lint XML 版本可核对；无安装/升级/下载、新native或生产依赖。研究 outsider 只增加已缓存的 JUnit4.13.2 测试声明。

Lint probe0错误/4警告（OldTargetApi、DataExtractionRules、StaticFieldLeak、MissingApplicationIcon）；outsider0错误/3警告（无 StaticFieldLeak）。合计0错误/7警告，未压制。本轮首次完整检查 outsider，不宣称其3条为新代码引入或旧零警告回归。

[构建日志](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/gradle.log)、[输入清单](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/inputs.json)、[产物大小/SHA/绝对路径](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/result.json)、[实物审计](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/artifact-audit.json)。两APK详情及绝对路径亦见执行报告。实际 Manifest print、badging、签名、DEX 八次审计均退出0；包名保持研究ID、min26/target36、无权限、probe Worker非导出且`:jpeg_worker`、无 native 或 JUnit/Hamcrest 打包。旧锁定两APK哈希先核对后比证书，SHA256证书一致：[签名连续性](m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/signing-continuity.json)。不提交APK/私钥/缓存。

## 明确未执行

| 项目 | 状态与原因 |
|---|---|
| 新研究APK安装、启动、跨UID拒绝及ServiceConnectionLeaked消失 | **NOT_RUN**，F1-A明确禁止 |
| 真实normal、FD交付、consumer lease、结果/退休/收口 | **NOT_RUN**，等待F1-A独立审查后另行授权F1-B |
| 原29项R3 | **原0PASS/1UNKNOWN/28NOT_RUN不变**，没有运行任何场景 |
| cancel/SIGKILL/恢复/原Android故障路径/API26/真机/OOM/LMK | **NOT_RUN**，不在本轮范围 |
| 正式App/JPEG三FAIL重跑、生产APK构建、私人字节保护验证 | **NOT_RUN**，没有产品修改或授权 |
| R1超时及历史attach根因 | **UNKNOWN保留**，不因本轮构建/模型通过关闭 |

当前只具备申请下一限定验证的代码与构建证据；不是系统生命周期验证结果。M10 PENDING/三JPEG FAIL，UI-C1冻结PASS WITH NOTES，M11未授权；本轮不改正式 ACCEPTANCE_MATRIX/原130案例。

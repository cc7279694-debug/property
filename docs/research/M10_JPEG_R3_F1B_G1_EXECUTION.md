# M10-R3-F1B-G1 执行记录

日期：2026-10-17。交付状态：**READY_FOR_REVIEW — M10-R3-F1B-G1**。这是宿主研究门禁修正，等待ChatGPT独立验收，不授予Android实验许可。

## 契约与来源

- Goal：修正仅因WorkerService名称存在而阻塞的判断，区分历史ServiceRecord与活跃Worker/消费者。
- Scope：研究Python监督器、纯分类器/组合器、宿主测试、当前状态与新checkpoint。
- Out of scope：本轮没有调用ADB或Android工具，没有安装/启动App、运行normal/Outsider、清理服务/文件、构建APK或运行29场景。生产App、Java/AIDL、WorkerService、StageStore、Gradle、Manifest、签名不改。
- 仓库：`cc7279694-debug/property`；来源锁定`04561e14d5d5694f99dadf35d9a92e42d851e9bd`；来源分支`research/m10-jpeg-strict-r3-f1b`。
- 独立worktree：`E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1b-g1/砺石新APP`；新分支`research/m10-jpeg-strict-r3-f1b-g1`。开始时来源本地/远端一致、各原工作区干净，新分支未存在；未reset、合并或覆盖。
- 验收：名称本身不拒绝；完整目标记录、独立进程/UID/已装APK身份、stage事实及两次快照必须共同安全。缺失、冲突、活动、未知、截断均BLOCKED。

## 实际修改

| 文件 | 用途与验证 |
|---|---|
| [原监督器verify.py](m10-jpeg-r3-f1b/verify.py) | 删除全局名称否决，真实`safe_idle()`调用新分类/组合函数；仍仅有只读preflight/finish入口，没有安装/启动入口。宿主FakeCollector直接调用该函数，禁止子进程。 |
| [service_record.py](m10-jpeg-r3-f1b-g1/service_record.py) | 完整capture/API36已知格式、精确记录边界与安全字段分类，只产生候选/无目标/阻塞。 |
| [gate.py](m10-jpeg-r3-f1b-g1/gate.py) | 同时验证身份、严格ps解析、原stage检查、两次独立快照一致性，才产生研究范围的PREFLIGHT_ELIGIBLE。 |
| [test_service_record.py](m10-jpeg-r3-f1b-g1/test_service_record.py)、[test_gate.py](m10-jpeg-r3-f1b-g1/test_gate.py) | 38＋27项新宿主测试，涵盖原监督器调用、正反边界与历史资料不能升级通过。 |
| [run_host_tests.py](m10-jpeg-r3-f1b-g1/run_host_tests.py) | 记录唯一测试身份、输入SHA、原始日志；运行期间对子进程调用设失败闸，不能触发ADB。 |
| [.gitattributes](m10-jpeg-r3-f1b-g1/.gitattributes) | 保存研究脚本、fixture快照及日志原字节；不改变历史资料。 |

inventory仅增加与该门禁直接相关的完整性约束：命令成功/无诊断、JSON读取前后stat完全一致、缺失/异常即停止。仍复用原`stage_check.validate_snapshot()`，没有修改清理逻辑、所有权模型或租约协议。

## 锁定真实输入与证据限度

[F1B原目标派生文本](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked-service-record-detail.txt)：**1,413 bytes**，SHA-256 `4080746899e8fcfb71f63c77599f5ff10c972ae3ab70983126155679b9add571`，保持原字节。

它有32行（说明标题＋目标body），`app=null`、`startForegroundCount=0`、背景启动与restart边界`--`、无目标Bindings段。`recentCallingUid=10219`和头部`c:Outsider`是调用来源，不是Worker实际UID或活跃Binding；`mAllowWiu_*Binding*=DENIED`也不是Bindings段。

该文件是裁剪派生材料：[范围登记](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked-service-detail-scope.json)记录排除37行前缀和10行外部包内容；不能证明完整dumpsys末端。F1B原预检在服务门禁停止，未采集preflight stage，只有after-files。因此**历史F1B仍BLOCKED，不能拼成两轮完整预检**。

所有阳性完整capture均明确为**HOST_SYNTHETIC**：保留真实锁定body、添加已知官方格式的合成外壳和捕获元数据；进程、包身份与两轮时间也是宿主模型。它们不是新Android运行结果，不将假包路径、合成time或任意complete标志当实际设备证据。

## 实际流程与测试

1. 直接调用原`safe_idle()`，完整合成闲置输入被旧名称断言阻止：1方法FAIL/退出1，保留[RED](m10-jpeg-r3-f1b-g1/evidence/red-supervisor.log)。旧监督器来源文件SHA `0969e99675d7419f251a4ab2116386c3009d4a6b9b77a0bf7c57c51dc3557756`。
2. 分类器和组合器测试先暴露边界再最小修正；失败/成功及修复前代码、测试快照均保留。未通过删断言、换成功fixture或改Android事实规避。
3. 最终执行`python -B docs/research/m10-jpeg-r3-f1b-g1/run_host_tests.py --tag green-final`：**81个唯一Python方法PASS，0 FAIL/ERROR/SKIP**，Python3.12.4，日志0.558秒。新65项＋原F1B16项；不重复累计多次运行。见[最终JSON](m10-jpeg-r3-f1b-g1/evidence/green-final.json)和[完整日志](m10-jpeg-r3-f1b-g1/evidence/green-final.log)。
4. 本地辅助审查发现的非目标body身份冲突、EMPTY终态、正式UID误归研究、包路径关联和空ps等问题均有对应宿主回归；本地审查不代替ChatGPT独立验收。

## 状态与停止

G1 READY_FOR_REVIEW仅指研究门禁与宿主证据。F1B两项Android验证仍NOT_RUN；原29场景0 PASS/1 UNKNOWN/28 NOT_RUN，R3生命周期BLOCKED，历史attach/R1超时UNKNOWN保留。

N01持久REVOKED授权语义**OPEN_NOT_TESTED**，本轮没有修StageStore；N02半清理保守阻塞保留；N03真实Outsider连接清理仍NOT_RUN。M10 PENDING/三项JPEG FAIL；UI-C1 ACCEPTED/FROZEN — PASS WITH NOTES；M11 NOT_AUTHORIZED。

其他结果和风险见[测试](M10_JPEG_R3_F1B_G1_TEST_RESULTS.md)、[决策](M10_JPEG_R3_F1B_G1_DECISION.md)、[checkpoint](../checkpoints/2026-10-17-M10-JPEG-R3-F1B-G1.md)。仅提交推送G1分支；最终SHA和远端一致性由Git回执报告，不在提交文件中自引用。

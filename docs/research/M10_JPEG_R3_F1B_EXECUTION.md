# M10-R3-F1B — 限定Android验证执行报告

2026-10-17。**BLOCKED — M10-R3-F1B Android Normal**。

**在安装前安全门禁停止；研究APK安装0次、Outsider启动0次、normal启动0次。**这不是新APK泄漏/normal测试失败，也不是活跃Worker已经确诊。

[逐项结果](M10_JPEG_R3_F1B_ANDROID_RESULTS.md) · [资源与生命周期](M10_JPEG_R3_F1B_RESOURCE_LIFECYCLE.md) · [决策](M10_JPEG_R3_F1B_DECISION.md) · [checkpoint](../checkpoints/2026-10-17-M10-JPEG-R3-F1B.md)。

## Goal / scope / source

只验证一次Outsider拒绝及销毁清理，并在通过后验证一次真实normal，前提是旧消费者/任务已经安全结束。禁止其余28场景、cancel/故障注入/恢复、生产修改、JNI或新工具安装。

锁定F1-A `f23180a4c086d0fc01f944a83174d52e9ad62dbd` 与本地/远端一致、源工作区干净；原生worktree工具从该提交创建独立目录后，新建 `research/m10-jpeg-strict-r3-f1b`。工作区 `E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1b/砺石新APP`。没有合并、reset或覆盖别的分支。

已读AGENTS/PROJECT/CURRENT_STATE、相关决策/架构、F1-A三报告及实际研究源码、Manifest/AIDL、R3原失败/F0证据。用户转交F1-A独立 **PASS WITH NOTES，仅限研究代码/JVM测试/构建**；不是Android生命周期通过。N01 REVOKED状态语义、N02半清理保守阻塞、N03真实连接清理待验证均保留。

## 实际过程

1. 实核 `emulator-5596 / Lithic_M01_API_36 /API36`、boot_completed=1。
2. 正式APK SHA与预期相符，UID10216；188条私有文件/目录元数据仅在内存排序聚合，不读取账本/图片内容。记录设备设置基线。
3. 研究已装仍为旧R3 APK，probe UID10218/Host PID3928，Outsider UID10219/PID4062；没有观察到Worker进程。
4. `dumpsys activity services com.cooper.lithic.m10r3.probe` 仍列出目标ServiceRecord。监督器按“出现WorkerService记录即拒绝”的保守条件停止，preflight进程退出2；原始[停止记录](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked.json)保留。
5. 停止后只补齐只读残留/保护取证，未尝试安装、启动、解绑、kill、清理或恢复。目标记录为`app=null`，recentCallingUid=10219，目标段没有Bindings段。stage命名空间不存在，只有旧合成owner marker与Host trace。**这些事实不能被写成正在运行的Worker，也没有证明仍有活跃绑定**；门禁可能过严，保留未知并交独立审查，不在本轮修改门禁继续执行。
6. 只在宿主核对两份锁定F1-A APK的字节、SHA、签名及Manifest，27份研究构建输入仍一致；没有构建或安装。旧已装APK的当前签名连续性检查因门禁停止而NOT_RUN。
7. 再次只读比较正式APK/UID、私有元数据聚合、设备设置和研究已装身份，实际一致。54次ADB命令均退出0，均为读取；**54不是测试通过数**。16项合成宿主snapshot检查器单测PASS，不计Android验收。

## 阻塞证据及局限

- [初始仅目标匹配行](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/preflight-services.json)、[停止后仅目标记录](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked-service-record-detail.txt)、[采集范围](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked-service-detail-scope.json)。
- ServiceRecord `17f772c`：目标`probe/.WorkerService`、`c:...outsider`、进程名`:jpeg_worker`、`app=null`、recentCallingUid10219。采样[进程](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/preflight-processes.json)与[结束进程](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/after-processes.json)都只有原Host/Outsider。
- [stage只读清单](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/after-files.json)和[严格snapshot检查](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/after-stage-check.json)：`ABSENT`，无current/pin/receipt、无source/output。没有调用canAcquireGuard、cleanup、recover或创建测试目录。
- 初始监督器只按Service名称拒绝，**不能把这个谓词输出直接当作“待完成绑定/活跃消费者存在”的实证**。目标无进程/无stage是已观察事实；残留记录是否足以安全忽略仍未在本轮确定。不能把F0 ServiceConnectionLeaked或拒绝访问与此记录建立已证因果。
- 原始dumpsys还包含无关的全局Last ANR服务段；发布证据只保留目标ServiceRecord，原命令退出码/字节计数不改。更宽的初始派生文本只留本机忽略目录，截取规则、前后SHA及排除行数都有登记；无关ANR的Bindings/执行字段不能归给研究Worker。

## 本轮APK输入（仅宿主审计）

| 输入 | 字节 | SHA-256 |
|---|---:|---|
| probe | 50,205 | `a3eea12dfe05736a69c473fa812d0b52a43c8100bbed2cffd0ba9c8f5f0ea684` |
| outsider | 10,473 | `a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879` |

原F1-A工作区APK仍存在，包名分别为`com.cooper.lithic.m10r3.probe`与`.outsider`，min26/target36、无权限、probe Worker非导出且`:jpeg_worker`，签名校验退出0。详情、绝对路径、原始Manifest/证书和四条宿主命令见[本地实物审计](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/local-apk-audit/result.json)。没有声称它们已安装。

## 保护与交付文件

[保护比较](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/protection-comparison.json) PASS仅限实际比较：正式APK/UID、元数据、设置、研究已装身份。私有聚合 `326e7f5576fdb03ba0b57012ada3c12b06adf147726946e9f8df2b856d4a8a83` /188条，与开始和F0历史值一致；私人字节一致性 **NOT_RUN**。

新增本轮四报告、只读监督/宿主snapshot检查器及16项合成测试、输入快照/命令/受限系统记录/保护与宿主APK证据、新checkpoint；更新唯一CURRENT_STATE。正式代码、研究APK源码/测试/配置、原130案例、验收矩阵与R0～F1-A历史证据不改。最终监督器精简为只读preflight/finish，不能执行install或Activity；实际执行时脚本快照及SHA均保留，不能用最后版本覆盖当时输入。

没有安装/启动/force-stop/kill任何App，未重启/wipe/重建AVD，未改时间/字体/主题/旋转/密度，没有删除研究残留、构建正式App或安装工具。未运行normal，未生成用于执行normal的runId。退出前原Host/Outsider PID和旧研究APK均保留。

仅提交推送`research/m10-jpeg-strict-r3-f1b`，最终SHA/远端一致性取Git回执。停止等待独立审查残留记录和门禁，不自动放宽/重跑/清理。UI-C1冻结、M10 PENDING/三JPEG FAIL、R3完整生命周期BLOCKED、M11 NOT_AUTHORIZED；原29仍0PASS/1UNKNOWN/28NOT_RUN，历史attach/R1超时UNKNOWN不关闭。

## 提交前检查

[交付审计](m10-jpeg-r3-f1b/evidence/f1b-20261017-01/delivery-audit.json)记录：43文件（42新增、1修改、0删除），范围仅本轮研究资料/CURRENT_STATE/checkpoint；全部本地文件链接有效，敏感模式扫描零命中，Git whitespace检查退出0。八个受保护工作区干净且HEAD未变；27份F1-A研究APK构建输入未变，执行脚本快照哈希及原始证据暂存字节一致。完整Android实验仍未运行，审计通过不改变BLOCKED。

# M10 Android 日志整改与集中收尾

**BLOCKED — ORIGINAL_API26_INSTANCE_ABSENT**。来源 `31a85a9c1e0ecbba373cce0b29d6ad82767aa7c3`；仅提交 `codex/m10-jpeg-android-final-natural-validation`。本轮交付有效的监督器整改，没有取得新的 Android PASS。

| 检查 | 实际结果 |
|---|---|
| 原 API26 实例 | **BLOCKED**。2026-10-18 15:08:31 UTC，唯一只读 CIM 查询 exit0：`targets=[]`、`ports=[]`、`emulatorProcessCount=0`。原 launcher32880/QEMU32032 不存在；原因 **UNKNOWN**。本轮最后设备环境观察即此结果，没有重启、替代或向设备查询。 |
| 当前设备身份/两份 Stage | **NOT_RUN**。实例门禁失败后未发 ADB；不能用历史记录推定当前 boot、APK、UID、Stage 或私有字节状态。 |
| 健康自然生命周期 | 继承用户确认的历史限定补验 **PASS**，本轮未重跑。 |
| 旧缺损生命周期 | **UNKNOWN 保留**。已记录的 JUnit、六阶段及终结链可分别核验；既有字节不能证明原完整采集窗口。旧 before/after/result 未修改。 |
| 新缺损、剩余七项、API36 | 全部 **NOT_RUN**；API26 实例门禁失败即停止，没有重试或创建新 Stage。 |
| 宿主回归 | **322 唯一方法：320 PASS / 2 SKIP / 0 FAIL / 0 ERROR**。原234身份全部保留，新增88全PASS；输入前后SHA一致。两SKIP是原符号链接权限不足，未冒充执行。 |

**实际操作次数**：只读宿主 CIM **1**；ADB、API26 创建/启动/安装/测试/退出、API36 创建/启动/安装/测试、H2/C2操作均 **0**。未重新构建、替换或操作 APK，也未安装工具、下载镜像或接受许可。

## 必要代码差异

- [连续采集器](m10-jpeg-android-isolated/continuous_log.py)：固定新设备serial、main缓冲与LITHIC_JPEG tag；测试前开始，双通道原始字节及SHA，stdout≤2MiB、stderr≤64KiB，总期限360秒。真实header与自有Host reader生存共同作为就绪条件；异常退出、可见丢失、溢出、断流、截断、未确认EOF均UNKNOWN。结束仅处理该采集器自己创建的**宿主 adb reader**，不发送Android停止命令，无kill升级或重试。
- [流解释器](m10-jpeg-android-isolated/continuous_evidence.py)：重新核对持久proof、起止顺序、进程归属、两路EOF和原字节SHA。只把before中精确已知旧行识别为初始backlog，不要求旧全局前缀一直存在；完整流保留、不排序或挑选有利六行。重复/未知事件、错误设备或进程身份、缺阶段继续拒绝，六阶段仍采用原允许偏序。
- [方法协调器](m10-jpeg-android-isolated/natural_tests.py)与[只读取证层](m10-jpeg-android-isolated/device_evidence.py)接通采集前置、方法起止记录和一次结束责任。采集UNKNOWN覆盖JUnit成功，任何后置异常仍尝试记录自有采集器收口。真实[Supervisor](m10-jpeg-android-isolated/validate.py)强制连续路径，证据使用新的独占根。旧snapshot差分仅为历史/宿主兼容保留，不能成为真实执行的降级旁路。

宿主真实Python子进程验证管道、EOF及有界退出；真实历史日志仅作回放fixture。新增35采集器＋35解释器＋18协调器测试，不算Android实证。中间Windows低粒度单调时钟问题改用`perf_counter_ns`，完整CRLF header边界补测；原RED及中间失败保留。没有重跑JVM/native全量、Lint或Gradle。

## 证据与剩余边界

[结果](m10-jpeg-android-final-natural/evidence/RESULTS.json)、[CIM原始命令及退出码](m10-jpeg-android-final-natural/evidence/commands.jsonl)、[实际环境观察](m10-jpeg-android-final-natural/evidence/initial-windows.json)、[宿主完整回归](m10-jpeg-android-final-natural/evidence/host-tests.txt)、[输入与统计](m10-jpeg-android-final-natural/evidence/host-test-results.json)、[旧缺损离线核查](m10-jpeg-android-final-natural/evidence/offline-prior-audit.json)、[小型SHA索引](m10-jpeg-android-final-natural/evidence/INDEX.json)。所有原R0～当前历史证据保持。

采集器PASS只证明其可观察窗口，不保证Android logd绝无不可见丢失；必须同时取得完整方法/唯一attempt/session/Worker身份、退休、Stage和业务事实证据。本轮真实logcat就绪/流传输/自然退休全部NOT_RUN。没有实现跨轮准入旁路来跳过旧UNKNOWN。

**唯一当前执行阻塞：原授权 API26 实例已不存在，不能在本轮重启或替代。**后续需新的明确环境授权并独立审查，不能自动推进API36、Worker故障场景或生产集成。私人账本及图片字节未读取/复制，当前私人字节完整性NOT_RUN。

App子树仍为`89679a188c2b6e6793962041383a9f92337e126f`，未改产品代码、原三JPEG断言、Room、备份、算法或UI。M10 **PENDING**、三个历史JPEG **FAIL**、UI-C1 **FROZEN**、M11 **NOT_AUTHORIZED**；H2 DELIVERY_UNKNOWN/pin=true、C2及全部历史NOTE保持。推送本候选分支后停止等待ChatGPT独立验收。

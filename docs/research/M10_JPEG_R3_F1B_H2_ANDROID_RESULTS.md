# M10-R3-F1B-H2 Android结果

2026-10-17。**BLOCKED — M10-R3-F1B-H2**。本轮单一normal失败，未完成生命周期；原R3统计不改。

| 检查 | 状态 | 实际证据与边界 |
|---|---|---|
| 授权环境、新完整G1/U1双快照 | PASS | API36/boot1，两轮闲置服务、无研究进程、stage ABSENT，PREFLIGHT_ELIGIBLE。 |
| 三包SHA/UID与来源 | PASS | 正式10216、Probe10218、Outsider10219；7脚本/27输入相同，无安装。 |
| 唯一normal发送及命令身份 | PASS | 标记先fsync，只有1次am start，正确runId/command/case。 |
| am启动结果 | UNKNOWN | 退出0但Status timeout/LaunchState UNKNOWN；晚Displayed不改写首次UNKNOWN。 |
| Host进入 | PASS，限身份 | PID6423/UID10218、HOST/syntheticOwner=true、唯一CASE_BEGIN；不是会话成功。 |
| bind及run报告 | FAIL | CASE_FAIL_STOP及结构化FAIL均为bind timeout。 |
| Host ANR | FAIL观察；根因UNKNOWN | 系统Input dispatching timed out，没有另行诊断。 |
| 迟到Worker角色 | PASS，限角色 | PID6610/UID10218、独立`:jpeg_worker`、syntheticOwner=false。 |
| 源与pin创建 | PASS，限元数据 | 同attempt、源预期SHA/source8192、DELIVERY_UNKNOWN pin；Worker未验证源字节。 |
| 同步hello/grant、nonce/代授权闭环 | NOT_RUN | bind期限内失败，未进入reserve；owner存在不代替握手。 |
| FD交付/pread/写拒绝/双方FD关闭及EBADF | NOT_RUN | 未进入start/work。 |
| consumer lease及活跃GC拒绝 | NOT_RUN | 未reserve；pin/guard存在不证明内核lease。 |
| 唯一SUCCESS/结果接受计数 | FAIL目标未达；计数NOT_RUN | 实际run FAIL；不假定缺失的acceptedResults字段值。 |
| 正常认证退休 | NOT_RUN | 无AUTHENTICATED_RETIRE，实际UNRESERVED_DEADLINE。 |
| 迟到Worker死亡 | PASS，限该死亡 | Binder＋ActivityManager/am_proc_died＋Zygote signal9匹配6610；不是normal退休。 |
| receipt/terminal清理 | BLOCKED | current存在、DELIVERY_UNKNOWN/pin=true/source保留，无成功receipt/终结proof。 |
| 首次结束研究进程 | BLOCKED / UNKNOWN | 原门禁unknown process under research identity；原行未归档，无法回溯身份。 |
| 最终进程/服务 | PASS，限末次快照 | Host6423仍在，无Worker、NO_TARGET_RECORD；不回写首次BLOCKED。 |
| 正式APK/UID、188元数据及设置 | PASS | 最终保护比较实际一致。 |
| 私人业务行、账本/封面字节 | NOT_RUN | 未读取、复制或归档，不由元数据推断。 |
| 其他28场景/cancel/恢复/API26/JNI、产品重跑 | NOT_RUN | 当前未授权；历史失败与UNKNOWN不改。 |

## 时间与身份

- runId：`h2_e22caecf8afc4a72957afc061c68d5a3`。
- attempt：`586d0904-eaac-403a-895a-48c34cd98810`；nonce：`80e16f4e-f17c-47a7-92ba-ce4ccaed751f`；generation：`synthetic-generation-r3`。
- epoch1792229881.382：START；9881.659：Host6423/UID10218启动；9884.472：HOST角色。
- 9889.482：CASE_BEGIN；9894.214：STAGE_CREATED；9897.657：PIN_DURABLE；9903.069：bind timeout FAIL。
- 9905.772：Displayed（系统报告+24s415ms）；9936.776：Host Input-dispatch ANR。
- 9961.449：Worker6610启动，比Host失败晚58.380秒；9965.398：SERVICE_CREATED；9965.403：SERVICE_BOUND。
- 9995.403：UNRESERVED_DEADLINE；9995.408：自身signal9；9995.420：OS死亡；9995.421：Host Binder死亡；9995.422：Zygote退出。

来源：[Host trace](m10-jpeg-r3-f1b/evidence/h2-20261017-01/normal-host-trace-6423.jsonl)、[Worker trace](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-ending-worker-trace-6610.jsonl)、[范围日志及完整性](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-ending-log-result.json)、[FAIL](m10-jpeg-r3-f1b/evidence/h2-20261017-01/normal-failure-result.json)。没有握手，不能伪造Worker已接收本attempt envelope。

## 计数及保护

本轮1个normal尝试，FAIL/BLOCKED，启动返回另有UNKNOWN；没有完整normal PASS。153条ADB不是153项测试。原R3 29项仍0 PASS/1 UNKNOWN/28 NOT_RUN，本轮另记，不改原矩阵。

最终正式APK SHA `4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46`；Probe `a3eea12dfe05736a69c473fa812d0b52a43c8100bbed2cffd0ba9c8f5f0ea684`；Outsider `a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879`，UID与基线相同。188→188元数据，聚合SHA `326e7f5576fdb03ba0b57012ada3c12b06adf147726946e9f8df2b856d4a8a83`，见[最终保护](m10-jpeg-r3-f1b/evidence/h2-20261017-01/h2-final-protection-result.json)。私人字节NOT_RUN。

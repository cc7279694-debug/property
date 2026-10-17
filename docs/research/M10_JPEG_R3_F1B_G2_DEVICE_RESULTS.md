# M10-R3-F1B-G2 真实设备门禁结果

日期：2026-10-17。结论：**BLOCKED — M10-R3-F1B-G2**，阻塞发生在设备连接检查，未进入ServiceRecord分类或组合门禁。

锁定G1来源：`7ff737ecd54746aaed580a15fac92126009d5c44`。本轮仅尝试原有`Lithic_M01_API_36 / emulator-5596`，没有切换设备、启动AVD或重试预检。结果汇总见[g2-results.json](m10-jpeg-r3-f1b/evidence/g2-20261017-01/g2-results.json)。

## 实际执行与原始证据

| 阶段 | 实际结果 | 精确来源 |
|---|---|---|
| 原G1只读preflight，唯一一次 | 第一条`get-state`返回1；监督器返回2并停止，未采集快照 | [入口及时间](m10-jpeg-r3-f1b/evidence/g2-20261017-01/g2-preflight-execution.json)、[原阻塞记录](m10-jpeg-r3-f1b/evidence/g2-20261017-01/blocked.json) |
| 必要只读结束保护，唯一一次 | 同样在第一条`get-state`返回1；结束helper返回2，保护比较未执行 | [入口及时间](m10-jpeg-r3-f1b/evidence/g2-20261017-01/g2-ending-execution.json)、[结束错误](m10-jpeg-r3-f1b/evidence/g2-20261017-01/g2-ending-error.json) |
| 实际ADB读取 | 共2条，均为指定serial的`get-state`；均未超时，stdout均0字节、stderr均41字节 | [原始命令、时间和退出码](m10-jpeg-r3-f1b/evidence/g2-20261017-01/adb-commands.jsonl)、[字节数与SHA-256](m10-jpeg-r3-f1b/evidence/g2-20261017-01/g2-command-integrity.json) |

两次错误均为`error: device 'emulator-5596' not found`。UTC时间分别为`07:19:16.353479`和`07:19:39.467548`。这只能证明当时指定ADB serial不可用，不能据此认定AVD已经关闭、被删除、存在危险消费者或运行在其他位置。

命令完整性SHA由已归档的无损ASCII命令文本离线计算，来源已在JSON注明；不是额外设备读取。第二条命令仅用于必要结束保护，不是第二次preflight。**2条失败读取不是2项测试通过，也不是2轮设备快照。**

## 逐项结果

| 检查 | 状态 | 证据边界 |
|---|---|---|
| 指定serial连接 | BLOCKED | 两次`get-state`均明确返回1，未取得`device`状态 |
| AVD身份、API36、ABI、boot完成 | NOT_RUN | 连接检查即停止，没有取得当前属性 |
| 正式APK及Probe、Outsider现有APK身份/UID | NOT_RUN | 未读取当前包路径、APK SHA或UID |
| 当前研究Host/Worker进程与关联UID | NOT_RUN | 未执行ps，不能宣称有或没有活跃Worker |
| 两轮完整ServiceRecord与返回完整性 | NOT_RUN | 真实dumpsys采集次数为0 |
| stage、current、pin、receipt及未知残留 | NOT_RUN | 未执行研究目录inventory或JSON读取 |
| 两轮独立快照一致性 | NOT_RUN | 完整快照数量为0 |
| `NO_TARGET_RECORD` | NOT_RUN | 分类器未取得设备输入 |
| `INACTIVE_RECORD_CANDIDATE` | NOT_RUN | 分类器未取得设备输入 |
| Service分类`BLOCKED` | NOT_RUN | 本次环境阻塞不能代替Service分类结果 |
| `PREFLIGHT_ELIGIBLE` | NOT_RUN | 组合门禁未运行，不授予任何后续实验资格 |
| 正式APK/UID、私有元数据与设置前后比较 | NOT_RUN | before/after保护快照均未取得，不能宣称一致 |
| 私人账本、图片字节完整性 | NOT_RUN | 未读取、复制或比较私人内容 |

## 操作与保留状态

实际安装、App启动、Outsider、normal、清理、信号、重启/启动AVD均为**0**；没有归档无关应用日志。原G1的81个宿主测试本轮未重跑，不作为本次设备测试PASS。

F1-A N01持久REVOKED语义保持`OPEN_NOT_TESTED`；N02半清理保守阻塞保持原边界；N03真实Outsider连接清理仍`NOT_RUN`。历史attach与R1超时UNKNOWN不变；R3原29场景保持`0 PASS / 1 UNKNOWN / 28 NOT_RUN`，完整生命周期仍BLOCKED。M10仍PENDING、原三项JPEG FAIL；UI-C1仍ACCEPTED / FROZEN — PASS WITH NOTES；M11仍NOT_AUTHORIZED。

已停止设备操作。环境恢复、后续设备预检或任何Android实验均不能由本次失败结果自动授权。

# M10-R3-F1B-H1 Android结果

2026-10-17。**READY_FOR_REVIEW — M10-R3-F1B-H1**。范围仅两研究APK更新及一次Outsider；不是完整R3生命周期或M10验收。

| 核对项 | 实际状态 | 证据与限制 |
|---|---|---|
| 新完整安全预检 | PASS | 本轮原G1/U1双快照实际PREFLIGHT_ELIGIBLE，未复用U2结果。 |
| 两APK身份/签名/Manifest | PASS | 原F1-A产物SHA正确、27构建输入相同，新旧证书一致，无权限；Worker非导出且`:jpeg_worker`。 |
| Probe保留数据更新 | PASS | 一次install-r Success；已装目标SHA、UID10218不变，立即进程/服务/stage及文件元数据检查通过。 |
| Outsider保留数据更新 | PASS | 一次install-r Success；已装目标SHA、UID10219不变；已知旧合成结果更新前后原字节保留。 |
| 测试前再次确认 | PASS，限快照 | 目标APK已装，无研究活动进程或危险stage；没有清理旧文件来通过。 |
| 独立UID及系统实际拒绝 | PASS | PID5352/UID10219访问Probe UID10218，ActivityManager明确not exported Permission Denial。 |
| App绑定结果与清理 | PASS | 本轮DENIED、bindingDenied=true、cleanupStatus=RELEASED，无首次意外清理错误。 |
| Activity销毁及新持久结果 | PASS | 新PID/新SHA、onCreate→onDestroy，系统wm_on_destroy_called，同activity token。 |
| ServiceConnectionLeaked | PASS，限本次有界窗口未见 | 4buffer完整采集，目标PID及对应系统事件覆盖销毁后10.869993秒，没有仅凭日志缺失推定。 |
| 越权Worker/不明消费者 | PASS，限观察与既有协议 | 未见SAFETY_OUTSIDER_BOUND/Worker启动，结束Worker app=null，无Bindings，stage ABSENT；不是永久无消费者证明。 |
| 正式APK/UID、元数据及设置保护 | PASS | 实际188→188且聚合SHA相同，APK/UID/设置一致。 |
| 正式业务记录 | 未执行正式业务写入口 | 研究APK分UID、无生产业务/Room入口，未请求正式业务写入；私人业务行/字节核查NOT_RUN，不能由元数据等同推定。 |
| 私人账本/图片字节完整性 | NOT_RUN | 未读取、复制或归档私人内容。 |
| normal、cancel、杀进程/恢复及29场景 | NOT_RUN | 本轮均禁止，原R3统计不增加PASS。 |
| 构建/Lint/JVM/正式JPEG重跑 | NOT_RUN | 使用锁定产物，未改产品或研究APK源码。 |

## 唯一测试身份与实际时序

- 外部编号：`989aa7a9-ba21-422e-bc5a-77e858ac7d87`，Intent action为`com.cooper.lithic.m10r3.H1.<该编号>`。只发出一次am start；源代码不消费该编号。
- 实际Outsider：PID5352、UID10219；Probe UID10218、正式UID10216，均互异。系统Activity token为254818546。
- 日志窗口：设备epoch **1792226955.655449194～1792226968.107993278**。
- 系统START 1792226955.817；am_proc_start 1792226956.073；系统Permission Denial 1792226956.624。
- onCreate结果1792226956.661；App onDestroy结果1792226957.237；系统wm_on_destroy_called **1792226957.238**。
- 窗口结束距系统销毁 **10.869993秒**。F0原泄漏在其销毁后约9ms出现，原失败和堆栈保留，本次覆盖这一检查时段。

最终`files/result.json`为274字节，SHA `575e3a8e964c21f78dd0f78d4c1da3e465326530dbf5ef1bda4f3a184558ca1e`，phase=onDestroy、PID5352/UID10219、DENIED/RELEASED，与本轮日志相同；旧结果PID4062，未误用。原SecurityException为期望的系统拒绝，完整研究堆栈保留；onDestroy再次发布同一拒绝异常不是第二次绑定。

来源：[生命周期结构化结果](m10-jpeg-r3-f1b/evidence/h1-20261017-01/outsider-lifecycle-result.json)、[system范围日志](m10-jpeg-r3-f1b/evidence/h1-20261017-01/outsider-window-system.log)、[events范围日志](m10-jpeg-r3-f1b/evidence/h1-20261017-01/outsider-window-events.log)、[main范围日志](m10-jpeg-r3-f1b/evidence/h1-20261017-01/outsider-window-main.log)。

## 结束现场与保护

Probe/Outsider最终SHA为本轮锁定目标，UID仍10218/10219；正式APK SHA `4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46`、UID10216不变。正式私有元数据前后188条、聚合SHA `326e7f5576fdb03ba0b57012ada3c12b06adf147726946e9f8df2b856d4a8a83`；字体、旋转、主题、尺寸、密度及时间设置一致。只读取元数据，不归档私人路径或内容。

结束ps保留已知Outsider PID5352，系统先前记录freezing；没有终止它来清场。Worker进程未见；目标ServiceRecord `9879f0e`，app=null，无Bindings/pending/executing/restarting，原分类器判为 **INACTIVE_RECORD_CANDIDATE**。该记录保留不清理；它不是Worker已正常运行或退休证明。研究stage命名空间ABSENT，无current/pin/receipt；既有owner/trace文件元数据保留，未知文件不删除。

来源：[结束身份/进程/stage](m10-jpeg-r3-f1b/evidence/h1-20261017-01/g2-ending-result.json)、[完整目标ServiceRecord块](m10-jpeg-r3-f1b/evidence/h1-20261017-01/g2-after-target-ServiceRecord.txt)、[服务分类及完整性](m10-jpeg-r3-f1b/evidence/h1-20261017-01/g2-after-services.json)、[保护比较](m10-jpeg-r3-f1b/evidence/h1-20261017-01/g2-protection-comparison.json)。完整服务命令1419字节/SHA `f425db4f42e0b1270f7f7601232693bf530b12fb0d3760583e859817e346878f`，分类基于同次完整输入，归档目标块明确为范围派生。

N01～N03不自动CLOSED；本轮补充N03相关单次API36系统证据，等待ChatGPT判断。原R3仍0 PASS/1 UNKNOWN/28 NOT_RUN，M10 PENDING/原三JPEG FAIL，UI-C1冻结PASS WITH NOTES，M11 NOT_AUTHORIZED。
